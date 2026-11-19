package com.partatoes.digitalfrontier.grid.jobs;

/** How a job program prices its trades for one player. Pure, so it can be tested without a world. */
public final class Pricing {
    /** The most a price moves either way: a quarter. */
    public static final float SWING = 0.25f;

    private Pricing() {
    }

    /**
     * The price multiplier: 1 - {@link #SWING} for a player exactly as aligned with the regime as the program is, rising
     * evenly to 1 + {@link #SWING} for one on the opposite extreme. A loyal program overcharges rebels, and a rebel
     * program its regime's friends.
     *
     * @param alignment the player's alignment with the regime, -1 to 1
     * @param sentiment the program's sentiment toward the regime, -1 to 1
     */
    public static float multiplier(float alignment, float sentiment) {
        float distance = Math.abs(alignment - sentiment) / 2;
        return 1 - SWING + 2 * SWING * Math.clamp(distance, 0f, 1f);
    }

    /** A price in items after the multiplier: never less than one. */
    public static int price(int base, float multiplier) {
        return Math.max(1, Math.round(base * multiplier));
    }
}
