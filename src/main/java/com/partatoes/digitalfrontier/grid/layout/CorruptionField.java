package com.partatoes.digitalfrontier.grid.layout;

/**
 * Raw decay intensity in [0, 1] per chunk: a background that rises with distance from the city, and low-frequency
 * blobs on top. This is decay before the regime's control suppresses it (see {@link Territory}); strongholds are
 * no longer decayed, but the opposite: the regime keeps its ground clean.
 */
public final class CorruptionField {
    private static final int SALT_BLOBS = 101;
    private static final int SALT_DETAIL = 102;

    private final GridLayoutSettings settings;

    CorruptionField(GridLayoutSettings settings) {
        this.settings = settings;
    }

    public float intensityAt(int chunkX, int chunkZ) {
        double distance = Math.hypot(chunkX, chunkZ);
        double radial = smoothstep(settings.cityRadius() * 0.5, settings.outlandsRadius(), distance);
        long seed = settings.gridSeed();
        double noise = 0.7 * valueNoise(seed, chunkX, chunkZ, 24, SALT_BLOBS) + 0.3 * valueNoise(seed, chunkX, chunkZ, 9, SALT_DETAIL);
        // Fade the noise out toward the city center so the core is always clean
        double noiseWeight = Math.min(1.0, distance / settings.coreRadius());
        double value = (0.65 * radial + 0.5 * (noise - 0.35) * noiseWeight) * (0.5 + settings.corruption());
        return (float) Math.clamp(value, 0.0, 1.0);
    }

    /** Smooth seeded noise in [0, 1], varying over cells of {@code cellSize} chunks. */
    static double valueNoise(long seed, int x, int z, int cellSize, int salt) {
        int cellX = Math.floorDiv(x, cellSize);
        int cellZ = Math.floorDiv(z, cellSize);
        double fx = fade((x - cellX * cellSize) / (double) cellSize);
        double fz = fade((z - cellZ * cellSize) / (double) cellSize);
        double a = corner(seed, cellX, cellZ, salt);
        double b = corner(seed, cellX + 1, cellZ, salt);
        double c = corner(seed, cellX, cellZ + 1, salt);
        double d = corner(seed, cellX + 1, cellZ + 1, salt);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    private static double corner(long seed, int cellX, int cellZ, int salt) {
        return GridRandom.unit(GridRandom.hash(seed, cellX, cellZ, salt));
    }

    private static double fade(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double smoothstep(double edge0, double edge1, double x) {
        return fade(Math.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0));
    }
}
