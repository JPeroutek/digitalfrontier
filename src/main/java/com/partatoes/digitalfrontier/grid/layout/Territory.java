package com.partatoes.digitalfrontier.grid.layout;

/**
 * How the regime's control and decay share the grid. They're separate axes, but correlated: the regime maintains
 * what it holds, so decay is suppressed under control. The suppression is statistical, a per-chunk roll, so moderate
 * control leaves the odd neglected pocket while strong control leaves almost none, and a few hard rules sit on top:
 * <ul>
 *   <li>the seat of a stronghold is never decayed;</li>
 *   <li>roads through patrolled ground are never decayed, since the regime keeps its roads clear.</li>
 * </ul>
 */
public final class Territory {
    /** Control at which suppression reaches its full strength (still short of total, by the roll). */
    static final float SUPPRESSION_FULL = 0.85f;

    private Territory() {
    }

    /**
     * Decay after the regime's suppression.
     *
     * @param raw     decay before suppression, 0 to 1
     * @param control the regime's control, 0 to 1
     * @param roll    a per-chunk seeded value in [0, 1): how thoroughly this chunk is kept
     * @param road    whether the chunk is a road
     */
    public static float decay(float raw, float control, double roll, boolean road) {
        if (control >= ControlTier.SEAT.minIntensity()
                || road && control >= ControlTier.PATROLLED.minIntensity()) {
            return 0;
        }
        // In step with control, not an S-curve: that held decay back almost fully and then let go within a couple of
        // chunks, so static appeared all at once at the border
        double held = Math.min(1.0, control / SUPPRESSION_FULL);
        double suppression = held * (0.75 + 0.25 * roll);
        return (float) (raw * (1 - suppression));
    }
}
