package com.partatoes.digitalfrontier.grid.regime;

/** The odds of a program joining the regime when it's recruited. */
public final class Conscription {
    private Conscription() {
    }

    /**
     * The chance one recruitment attempt succeeds.
     *
     * @param base       the method's base chance (a recruiter's company, or a bribe)
     * @param sentiment  the program's sentiment toward the regime: loyalists join readily, rebels almost never
     * @param population the regime's members in the grid
     * @param ideal      how many members the regime wants; with none (no stronghold standing) nobody is recruited
     */
    public static double chance(double base, float sentiment, int population, int ideal) {
        if (ideal <= 0) {
            return 0;
        }
        double willing = (sentiment + 1) / 2;
        return base * willing * willing * need(population, ideal);
    }

    /**
     * How keenly the regime recruits: 1 with no members, falling as it fills up and faster once past its ideal, but
     * never quite to 0.
     */
    static double need(int population, int ideal) {
        return ideal / (ideal + 2.0 * Math.max(0, population - ideal) + 0.25 * population);
    }

    /** The regime's ideal size: a number of members per standing stronghold, more under a stronger regime. */
    public static int ideal(int strongholdsStanding, int perStronghold, float regime) {
        return Math.round(strongholdsStanding * perStronghold * (0.5f + regime));
    }
}
