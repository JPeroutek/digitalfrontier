package com.partatoes.digitalfrontier.grid.layout;

/** How firmly the regime holds a chunk, from its control intensity (see {@link ControlField}). */
public enum ControlTier {
    /** Nobody answers to the regime here. */
    FREE(0.0f),
    /** The regime's eye: the odd patrol passes through. */
    WATCHED(0.15f),
    /** Patrolled ground: decay is held back, and the regime's roads are kept clean. */
    PATROLLED(0.4f),
    /** Regime territory: orderly, pristine, in the regime's color. */
    CONTROLLED(0.65f),
    /** The stronghold and the ground around it. */
    SEAT(0.85f);

    private final float minIntensity;

    ControlTier(float minIntensity) {
        this.minIntensity = minIntensity;
    }

    public float minIntensity() {
        return minIntensity;
    }

    public static ControlTier fromIntensity(float intensity) {
        ControlTier[] tiers = values();
        for (int i = tiers.length - 1; i > 0; i--) {
            if (intensity >= tiers[i].minIntensity) {
                return tiers[i];
            }
        }
        return FREE;
    }

    /** The tier one step lower, or FREE. */
    public ControlTier weaker() {
        return this == FREE ? FREE : values()[ordinal() - 1];
    }
}
