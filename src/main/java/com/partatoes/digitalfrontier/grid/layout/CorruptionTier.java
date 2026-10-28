package com.partatoes.digitalfrontier.grid.layout;

public enum CorruptionTier {
    CLEAN(0.0f),
    LOW(0.2f),
    UNSTABLE(0.45f),
    CORRUPTED(0.7f),
    SEVERE(0.9f);

    private final float minIntensity;

    CorruptionTier(float minIntensity) {
        this.minIntensity = minIntensity;
    }

    public static CorruptionTier fromIntensity(float intensity) {
        CorruptionTier[] tiers = values();
        for (int i = tiers.length - 1; i > 0; i--) {
            if (intensity >= tiers[i].minIntensity) {
                return tiers[i];
            }
        }
        return CLEAN;
    }
}
