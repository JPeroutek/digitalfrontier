package com.partatoes.digitalfrontier.grid.layout;

import java.util.List;

/**
 * Corruption intensity in [0, 1] per chunk: a background that rises with distance from the city,
 * low-frequency blobs on top, and strongholds forced to SEVERE at their core.
 */
public final class CorruptionField {
    public static final int STRONGHOLD_RADIUS = 12;
    private static final int SALT_BLOBS = 101;
    private static final int SALT_DETAIL = 102;

    private final GridLayoutSettings settings;
    private final List<Site> strongholds;

    CorruptionField(GridLayoutSettings settings, List<Site> strongholds) {
        this.settings = settings;
        this.strongholds = strongholds;
    }

    public float intensityAt(int chunkX, int chunkZ) {
        double distance = Math.hypot(chunkX, chunkZ);
        double radial = smoothstep(settings.cityRadius() * 0.5, settings.outlandsRadius(), distance);
        double noise = 0.7 * valueNoise(chunkX, chunkZ, 24, SALT_BLOBS) + 0.3 * valueNoise(chunkX, chunkZ, 9, SALT_DETAIL);
        // Fade the noise out toward the city center so the core is always clean
        double noiseWeight = Math.min(1.0, distance / settings.coreRadius());
        double value = (0.65 * radial + 0.5 * (noise - 0.35) * noiseWeight) * (0.5 + settings.corruption());

        for (Site stronghold : strongholds) {
            double falloff = 1.0 - stronghold.distanceTo(chunkX, chunkZ) / STRONGHOLD_RADIUS;
            if (falloff > 0) {
                value = Math.max(value, falloff >= 0.6 ? 0.95 : 0.95 * falloff / 0.6);
            }
        }
        return (float) Math.clamp(value, 0.0, 1.0);
    }

    private double valueNoise(int x, int z, int cellSize, int salt) {
        int cellX = Math.floorDiv(x, cellSize);
        int cellZ = Math.floorDiv(z, cellSize);
        double fx = fade((x - cellX * cellSize) / (double) cellSize);
        double fz = fade((z - cellZ * cellSize) / (double) cellSize);
        double a = corner(cellX, cellZ, salt);
        double b = corner(cellX + 1, cellZ, salt);
        double c = corner(cellX, cellZ + 1, salt);
        double d = corner(cellX + 1, cellZ + 1, salt);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    private double corner(int cellX, int cellZ, int salt) {
        return GridRandom.unit(GridRandom.hash(settings.gridSeed(), cellX, cellZ, salt));
    }

    private static double fade(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        return fade(Math.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0));
    }
}
