package com.partatoes.digitalfrontier.grid.layout;

/**
 * Stateless, position-keyed randomness. Every random decision in the grid is a pure function of
 * (grid seed, coordinates, salt), so chunks can generate in any order and always agree.
 */
public final class GridRandom {
    private GridRandom() {
    }

    // splitmix64 finalizer over the combined inputs
    public static long hash(long seed, int x, int z, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /** Uniform double in [0, 1). */
    public static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    /** Uniform int in [0, bound). */
    public static int nextInt(long hash, int bound) {
        return (int) (unit(hash) * bound);
    }
}
