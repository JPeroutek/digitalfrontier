package com.partatoes.digitalfrontier.grid.parts;

import com.partatoes.digitalfrontier.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * A station's spiral staircase: a 4×4 shaft with stairs climbing clockwise around a lit 2×2 pillar, one block per
 * step. Shared by the code-built stations and the starter templates, so both look the same.
 *
 * <p>Positions are within the shaft (X and Z 0..3, Y 0 for the bottom step). The bottom and top steps are always
 * on the shaft's +X or +Z side (cells (3, 1..3) and (1..2, 3)), so you walk on and off toward the middle of the
 * chunk when the shaft sits in its (0, 0) corner. A railing at the top keeps you from falling down the well.
 */
public final class SpiralStair {
    // The ring of steps around the pillar, clockwise seen from above (east, then south, west and north)
    private static final int[][] RING = {{1, 0}, {2, 0}, {3, 0}, {3, 1}, {3, 2}, {3, 3}, {2, 3}, {1, 3}, {0, 3}, {0, 2}, {0, 1}, {0, 0}};
    // Ring cells you can step on and off from: the ones facing the chunk, away from the shaft's outer edges
    private static final int FIRST_INNER = 3;
    private static final int LAST_INNER = 7;

    private static final BlockState WALL = ModBlocks.GRIDSTONE_BRICKS_WALL.getDefaultState();
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private SpiralStair() {
    }

    /**
     * The smallest rise of at least {@code rise} that a staircase can have; a few rises would put the top or bottom
     * step against the shaft's outer edge.
     */
    public static int fit(int rise) {
        while (topCell(rise) < 0) {
            rise++;
        }
        return rise;
    }

    // The ring index of the top step, or -1 if no placement gives inner cells at both ends
    private static int topCell(int rise) {
        for (int top = FIRST_INNER; top <= LAST_INNER; top++) {
            int bottom = Math.floorMod(top - rise, RING.length);
            if (bottom >= FIRST_INNER && bottom <= LAST_INNER) {
                return top;
            }
        }
        return -1;
    }

    /**
     * The staircase's block at a position in the shaft, or null where it leaves things as they are.
     *
     * @param rise  how far the top step is above the bottom one; use {@link #fit} for one that works
     * @param light the pillar's lit block
     */
    @Nullable
    public static BlockState blockAt(int x, int y, int z, int rise, BlockState light) {
        if (x < 0 || x > 3 || z < 0 || z > 3 || y < 0 || y > rise + 3) {
            return null;
        }
        if (x >= 1 && x <= 2 && z >= 1 && z <= 2) {
            return y <= rise + 1 ? light : null;
        }
        int top = topCell(rise);
        int cell = cell(x, z);
        if (y > rise) {
            // Railing round the well, open at the top two steps where you come up
            boolean open = cell == top || cell == Math.floorMod(top - 1, RING.length);
            return y == rise + 1 && !open ? WALL : AIR;
        }
        // Step i is at height i, in cell (top - rise + i) of the ring
        int step = Math.floorMod(cell - (top - rise), RING.length);
        if (Math.floorMod(y - step, RING.length) == 0) {
            // Each step faces the next one, so you climb toward it
            int[] next = RING[(cell + 1) % RING.length];
            Direction facing = next[0] > x ? Direction.EAST : next[0] < x ? Direction.WEST : next[1] > z ? Direction.SOUTH : Direction.NORTH;
            return ModBlocks.GRIDSTONE_BRICKS_STAIRS.getDefaultState()
                    .with(StairsBlock.FACING, facing).with(StairsBlock.HALF, BlockHalf.BOTTOM);
        }
        return AIR;
    }

    private static int cell(int x, int z) {
        for (int i = 0; i < RING.length; i++) {
            if (RING[i][0] == x && RING[i][1] == z) {
                return i;
            }
        }
        throw new IllegalArgumentException("not a ring cell: " + x + ", " + z);
    }
}
