package com.partatoes.digitalfrontier.grid.gen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.block.custom.PatternBlock;
import com.partatoes.digitalfrontier.grid.layout.CorruptionTier;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_Y;

/**
 * The last pass over a chunk's blocks: corruption, applied to whatever the chunk would otherwise be, so any zone or
 * building can be corrupted without corrupted copies of anything. Every decision is a hash of the grid seed and the
 * block or cell position, so chunks agree at their borders and regenerate identically.
 *
 * <p>Effects build up with intensity (see {@link CorruptionTier} for the thresholds):
 * <ul>
 *   <li>LOW: barely anything; a few pattern lines flicker, their glow dropping out now and then, so lightly
 *       corrupted buildings keep their clean look.</li>
 *   <li>UNSTABLE: pattern lines stray from their district color, to its neighbours on the color wheel at first and
 *       further around it as intensity rises, or glitch; gridstone cracks; holes through walls, floors and
 *       streets; columns of buildings shifted up or down ("misaligned rows").</li>
 *   <li>CORRUPTED and up: blocks dissolve into static; buildings break off above a cut and float apart.</li>
 * </ul>
 * Structural effects (holes, shifts, breaks) are skipped on site chunks so terminals and strongholds stay intact;
 * their materials still corrupt. The Portal's chunk isn't corrupted at all (see {@link GridChunkBuild}), and
 * Arrival Pads, Portal blocks and Stronghold Uplinks are never touched.
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
    private static final int SALT_RECOLOR_JUMP = 5014;
    private static final int SALT_FLICKER = 5015;
    private static final int SALT_FLICKER_TIMING = 5016;
    /** Missing spans of the city's levels are this many blocks each way. */
    private static final int SPAN = 8;

    // The dye colors as two color wheels: the hues in order, and the neutrals from light to dark
    private static final List<DyeColor> HUES = List.of(DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN,
            DyeColor.CYAN, DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK);
    private static final List<DyeColor> NEUTRALS = List.of(DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK, DyeColor.BROWN);
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
                return Blocks.AIR.defaultBlockState();
            }
            if (y < FLOOR_Y && source.blockAt(y).isAir() && roll(seed, blockX, y, blockZ, SALT_CAVE_IN) < (intensity - CORRUPTED) * 0.6) {
                return ModBlocks.STATIC_BLOCK.defaultBlockState();
            }
        }
        int sourceY = y;
        if (structural && intensity >= UNSTABLE && y > FLOOR_Y) {
            sourceY = y - shift(seed, intensity, blockX, blockZ);
            if (sourceY <= FLOOR_Y) {
                return Blocks.AIR.defaultBlockState();
            }
            if (intensity >= CORRUPTED && brokenOff(seed, intensity, top, blockX, sourceY, blockZ)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        BlockState state = source.blockAt(sourceY);
        if (state.isAir() || state.is(ModBlocks.ARRIVAL_PAD) || state.is(ModBlocks.GRID_PORTAL) || state.is(ModBlocks.STRONGHOLD_UPLINK)) {
            return state;
        }
        if (structural && intensity >= UNSTABLE && hole(seed, intensity, blockX, y, blockZ)) {
            return Blocks.AIR.defaultBlockState();
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
            return ModBlocks.STATIC_BLOCK.defaultBlockState();
        }
        Pattern pattern = patterns().get(state.getBlock());
        if (pattern != null) {
            if (intensity >= UNSTABLE && roll(seed, blockX, y, blockZ, SALT_GLITCH) < (intensity - UNSTABLE) * 0.8) {
                return ModBlocks.GLITCHED_GRIDSTONE.get(pattern.color()).defaultBlockState();
            }
            if (intensity >= UNSTABLE && roll(seed, blockX, y, blockZ, SALT_RECOLOR) < (intensity - UNSTABLE) * 0.8) {
                DyeColor wrong = strayColor(pattern.color(), intensity, roll(seed, blockX, y, blockZ, SALT_RECOLOR_PICK),
                        roll(seed, blockX, y, blockZ, SALT_RECOLOR_JUMP));
                return ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get(pattern.name()).get(wrong).defaultBlockState();
            }
            if (state.hasProperty(PatternBlock.FLICKER) && roll(seed, blockX, y, blockZ, SALT_FLICKER) < Math.min(0.25, (intensity - LOW) * 0.5)) {
                return state.setValue(PatternBlock.FLICKER, roll(seed, blockX, y, blockZ, SALT_FLICKER_TIMING) < 0.5 ? 1 : 2);
            }
            return state;
        }
        if ((state.is(ModBlocks.GRIDSTONE_BLOCK) || state.is(ModBlocks.GRIDSTONE_BRICKS_BLOCK))
                && intensity >= UNSTABLE && roll(seed, blockX, y, blockZ, SALT_CRACK) < (intensity - UNSTABLE) * 0.6) {
            return ModBlocks.CRACKED_GRIDSTONE_BLOCK.defaultBlockState();
        }
        return state;
    }

    /**
     * The wrong color a corrupted pattern line shows: a neighbour of its own color on the color wheel when lines first
     * stray (at UNSTABLE), reaching further around the wheel as intensity rises, and at high intensity sometimes
     * jumping between the hues and the neutrals.
     *
     * @param pick a roll in [0, 1) choosing the step around the wheel
     * @param jump a roll in [0, 1) deciding whether to leave the color's own wheel
     */
    static DyeColor strayColor(DyeColor color, float intensity, double pick, double jump) {
        double severity = Math.clamp((intensity - UNSTABLE) / (1 - UNSTABLE), 0, 1);
        boolean hue = HUES.contains(color);
        if (jump < severity * severity * 0.3) {
            List<DyeColor> other = hue ? NEUTRALS : HUES;
            return other.get((int) (pick * other.size()));
        }
        List<DyeColor> wheel = hue ? HUES : NEUTRALS;
        // Steps of 1 at the start, up to half way round the wheel at full intensity, either way
        int reach = 1 + (int) Math.round(severity * (wheel.size() / 2 - 1));
        int choice = (int) (pick * reach * 2);
        int step = (choice / 2 + 1) * (choice % 2 == 0 ? 1 : -1);
        return wheel.get(Math.floorMod(wheel.indexOf(color) + step, wheel.size()));
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
