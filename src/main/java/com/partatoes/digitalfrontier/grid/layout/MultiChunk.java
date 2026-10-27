package com.partatoes.digitalfrontier.grid.layout;

/**
 * Marks a chunk as one quarter of a 2×2-chunk building whose north-west chunk is (originX, originZ).
 * All four chunks share the same origin, so they all pick the same building.
 *
 * @param landmark true for the city's central landmark tower
 */
public record MultiChunk(int originX, int originZ, boolean landmark) {
    public static final int SIZE = 2;
}
