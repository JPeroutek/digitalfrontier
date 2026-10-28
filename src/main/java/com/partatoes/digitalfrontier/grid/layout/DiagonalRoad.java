package com.partatoes.digitalfrontier.grid.layout;

/**
 * A highway running at 45° to a diagonal waypoint. It isn't tied to one chunk: the road is a band of blocks
 * around a line through chunk centers, so it crosses each chunk corner to corner and continues seamlessly into
 * the next. Rising roads follow x = z, falling roads follow x = -z.
 */
public record DiagonalRoad(boolean rising) {
    /** Blocks either side of the center line that are road. */
    public static final int HALF_WIDTH = 7;

    /** Signed distance, in blocks measured along an axis, of a block from the road's center line. */
    public int offset(int blockX, int blockZ) {
        // The center line passes through the middle of chunk (0, 0), between blocks 7 and 8
        return rising ? blockX - blockZ : blockX + blockZ - 15;
    }

    public boolean contains(int blockX, int blockZ) {
        return Math.abs(offset(blockX, blockZ)) <= HALF_WIDTH;
    }
}
