package com.partatoes.digitalfrontier.grid.gen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.layout.CorruptionTier;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.DyeColor;

import java.util.HashMap;
import java.util.Map;

import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_Y;

/**
 * The last pass over a chunk's blocks: corruption, applied to whatever the chunk would otherwise be, so any zone or
 * building can be corrupted without corrupted copies of anything. Every decision is a hash of the grid seed and the
 * block or cell position, so chunks agree at their borders and regenerate identically.
 *
 * <p>Effects build up with intensity (see {@link CorruptionTier} for the thresholds):
 * <ul>
 *   <li>LOW: pattern lines show the wrong district color; gridstone cracks.</li>
 *   <li>UNSTABLE: holes through walls, floors and streets; columns of buildings shifted up or down
 *       ("misaligned rows"); pattern lines glitch.</li>
 *   <li>CORRUPTED and up: blocks dissolve into static; buildings break off above a cut and float apart.</li>
 * </ul>
 * Structural effects (holes, shifts, breaks) are skipped on site chunks so terminals, the Portal and strongholds
 * stay intact; their materials still corrupt. Arrival Pads, Portal blocks and Stronghold Uplinks are never touched.
 */
final class GridCorruption {
    /** Below this intensity nothing changes. */
    static final float MIN_INTENSITY = 0.2f;
    /** The furthest a shifted column moves up. */
    static final int MAX_SHIFT = 5;

    private static final float LOW = MIN_INTENSITY;
    private static final float UNSTABLE = 0.45f;
    private static final float CORRUPTED = 0.7f;
    /** Ground deeper than this below the street is left alone: nobody sees it, and it keeps generation cheap. */
    private static final int DEPTH = 4;
    private static final int HOLE_CELL = 3;
    private static final int SHIFT_GROUP = 4;
    private static final int FRAGMENT_CELL = 4;

    private static final int SALT_RECOLOR = 5001;
    private static final int SALT_RECOLOR_PICK = 5002;
    private static final int SALT_CRACK = 5003;
    private static final int SALT_HOLE = 5004;
    private static final int SALT_SHIFT = 5005;
    private static final int SALT_SHIFT_AMOUNT = 5006;
    private static final int SALT_GLITCH = 5007;
    private static final int SALT_STATIC = 5008;
    private static final int SALT_BREAK = 5009;
    private static final int SALT_BREAK_HEIGHT = 5010;
    private static final int SALT_FRAGMENT = 5011;
    private static final int SALT_SPAN = 5012;
    private static final int SALT_CAVE_IN = 5013;
    /** Missing spans of the city's levels are this many blocks each way. */
    private static final int SPAN = 8;

    private static final DyeColor[] COLORS = DyeColor.values();
    private static volatile Map<Block, Pattern> patterns;

    private GridCorruption() {
    }

    /** Where a pattern block came from, so it can be recolored or glitched in the same color. */
    private record Pattern(String name, DyeColor color) {
    }

    /** Looks up blocks of a column, before corruption. */
    @FunctionalInterface
    interface Source {
        BlockState blockAt(int y);
    }

    /**
     * The corrupted block at a position.
     *
     * @param structural whether holes, shifted columns and broken-off buildings may apply here
     * @param level      whether the block belongs to one of the city's levels (see {@link GridTransit})
     * @param top        the highest Y the uncorrupted chunk has blocks at; nothing generates above it
     */
    static BlockState apply(long seed, float intensity, boolean structural, boolean level, int top, int blockX, int y, int blockZ,
                            Source source) {
        // The city's levels (decks, guideways, tunnels, skyways) keep their shape until corruption is strong
        if (level) {
            structural &= intensity >= CORRUPTED;
        }
        if (intensity < LOW || y < FLOOR_Y - DEPTH && !level) {
            return source.blockAt(y);
        }
        if (level && intensity >= CORRUPTED) {
            if (y > FLOOR_Y && roll(seed, Math.floorDiv(blockX, SPAN), Math.floorDiv(y, SPAN), Math.floorDiv(blockZ, SPAN), SALT_SPAN)
                    < (intensity - CORRUPTED) * 1.5) {
                // A whole span of the deck, guideway or bridge is gone
                return Blocks.AIR.getDefaultState();
            }
            if (y < FLOOR_Y && source.blockAt(y).isAir() && roll(seed, blockX, y, blockZ, SALT_CAVE_IN) < (intensity - CORRUPTED) * 0.6) {
                return ModBlocks.STATIC_BLOCK.getDefaultState();
            }
        }
        int sourceY = y;
        if (structural && intensity >= UNSTABLE && y > FLOOR_Y) {
            sourceY = y - shift(seed, intensity, blockX, blockZ);
            if (sourceY <= FLOOR_Y) {
                return Blocks.AIR.getDefaultState();
            }
            if (intensity >= CORRUPTED && brokenOff(seed, intensity, top, blockX, sourceY, blockZ)) {
                return Blocks.AIR.getDefaultState();
            }
        }
        BlockState state = source.blockAt(sourceY);
        if (state.isAir() || state.isOf(ModBlocks.ARRIVAL_PAD) || state.isOf(ModBlocks.GRID_PORTAL) || state.isOf(ModBlocks.STRONGHOLD_UPLINK)) {
            return state;
        }
        if (structural && intensity >= UNSTABLE && hole(seed, intensity, blockX, y, blockZ)) {
            return Blocks.AIR.getDefaultState();
        }
        return material(seed, intensity, blockX, y, blockZ, state);
    }

    // Columns of a building, in groups of 4×4, slide up or down a few blocks
    private static int shift(long seed, float intensity, int blockX, int blockZ) {
        int groupX = Math.floorDiv(blockX, SHIFT_GROUP);
        int groupZ = Math.floorDiv(blockZ, SHIFT_GROUP);
        if (GridRandom.unit(GridRandom.hash(seed, groupX, groupZ, SALT_SHIFT)) >= (intensity - UNSTABLE) * 1.5) {
            return 0;
        }
        int maxShift = 1 + (int) ((intensity - UNSTABLE) / (1 - UNSTABLE) * 4);
        int amount = 1 + GridRandom.nextInt(GridRandom.hash(seed, groupX, groupZ, SALT_SHIFT_AMOUNT), maxShift);
        return (GridRandom.hash(seed, groupX, groupZ, SALT_SHIFT_AMOUNT + 1) & 1) == 0 ? amount : -amount;
    }

    // Above a cut partway up, a building only survives in scattered 4×4×4 fragments that seem to float apart
    private static boolean brokenOff(long seed, float intensity, int top, int blockX, int y, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        if (GridRandom.unit(GridRandom.hash(seed, chunkX, chunkZ, SALT_BREAK)) >= (intensity - CORRUPTED) * 2) {
            return false;
        }
        int height = top - FLOOR_Y;
        int cut = FLOOR_Y + 4 + (int) (GridRandom.unit(GridRandom.hash(seed, chunkX, chunkZ, SALT_BREAK_HEIGHT)) * Math.max(1, height - 4));
        if (y <= cut) {
            return false;
        }
        return roll(seed, Math.floorDiv(blockX, FRAGMENT_CELL), Math.floorDiv(y, FRAGMENT_CELL), Math.floorDiv(blockZ, FRAGMENT_CELL),
                SALT_FRAGMENT) >= 0.25;
    }

    private static boolean hole(long seed, float intensity, int blockX, int y, int blockZ) {
        double chance = (intensity - UNSTABLE) * 0.5;
        return roll(seed, Math.floorDiv(blockX, HOLE_CELL), Math.floorDiv(y, HOLE_CELL), Math.floorDiv(blockZ, HOLE_CELL), SALT_HOLE) < chance;
    }

    private static BlockState material(long seed, float intensity, int blockX, int y, int blockZ, BlockState state) {
        if (intensity >= CORRUPTED && roll(seed, blockX, y, blockZ, SALT_STATIC) < (intensity - CORRUPTED) * 1.2) {
            return ModBlocks.STATIC_BLOCK.getDefaultState();
        }
        Pattern pattern = patterns().get(state.getBlock());
        if (pattern != null) {
            if (intensity >= UNSTABLE && roll(seed, blockX, y, blockZ, SALT_GLITCH) < (intensity - UNSTABLE) * 0.8) {
                return ModBlocks.GLITCHED_GRIDSTONE.get(pattern.color()).getDefaultState();
            }
            if (roll(seed, blockX, y, blockZ, SALT_RECOLOR) < (intensity - LOW) * 0.8) {
                DyeColor wrong = COLORS[(int) (roll(seed, blockX, y, blockZ, SALT_RECOLOR_PICK) * COLORS.length)];
                return ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get(pattern.name()).get(wrong).getDefaultState();
            }
            return state;
        }
        if ((state.isOf(ModBlocks.GRIDSTONE_BLOCK) || state.isOf(ModBlocks.GRIDSTONE_BRICKS_BLOCK))
                && roll(seed, blockX, y, blockZ, SALT_CRACK) < (intensity - LOW) * 0.6) {
            return ModBlocks.CRACKED_GRIDSTONE_BLOCK.getDefaultState();
        }
        return state;
    }

    private static double roll(long seed, int x, int y, int z, int salt) {
        // Fold Y into the seed: GridRandom hashes are 2D
        return GridRandom.unit(GridRandom.hash(seed ^ (y * 0x632BE59BD9B4E019L), x, z, salt));
    }

    private static Map<Block, Pattern> patterns() {
        Map<Block, Pattern> result = patterns;
        if (result == null) {
            Map<Block, Pattern> built = new HashMap<>();
            ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.forEach((name, colors) ->
                    colors.forEach((color, block) -> built.put(block, new Pattern(name, color))));
            result = Map.copyOf(built);
            patterns = result;
        }
        return result;
    }
}
