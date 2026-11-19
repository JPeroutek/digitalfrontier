package com.partatoes.digitalfrontier.grid.squad;

import com.partatoes.digitalfrontier.grid.layout.ControlTier;

/**
 * What an enforcer in a squad does, rolled when the squad forms: the deeper into the regime's
 * land, the likelier the heavier and longer-ranged roles. Pure, so the odds can be tested.
 */
public enum SquadRoles {
    /** Fists, as enforcers always fought. */
    PLAIN,
    /** A Light Staff: reach, and blocking. */
    STAFF,
    /** Slow, armoured and bigger. */
    HEAVY,
    /** Keeps its distance and throws Light Grenades. */
    GRENADIER;

    // Weights for PLAIN, STAFF, HEAVY, GRENADIER at each tier that fields squads
    private static final int[] PATROLLED = {70, 20, 5, 5};
    private static final int[] CONTROLLED = {50, 25, 12, 13};
    private static final int[] SEAT = {35, 25, 20, 20};

    /** A role for an enforcer of a squad formed in land of this tier, from a roll in [0, 1). */
    public static SquadRoles roll(ControlTier tier, double roll) {
        int[] weights = weights(tier);
        int total = 0;
        for (int weight : weights) {
            total += weight;
        }
        double pick = roll * total;
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick < 0) {
                return values()[i];
            }
        }
        return values()[weights.length - 1];
    }

    /** The odds of each role (PLAIN, STAFF, HEAVY, GRENADIER) at a tier, as weights. */
    public static int[] weights(ControlTier tier) {
        return switch (tier) {
            case SEAT -> SEAT.clone();
            case CONTROLLED -> CONTROLLED.clone();
            default -> PATROLLED.clone();
        };
    }

    /** How many enforcers a squad of this tier has, leader included, from a roll in [0, 1). */
    public static int size(ControlTier tier, double roll) {
        return switch (tier) {
            case SEAT -> 3 + (int) (roll * 2);
            case CONTROLLED -> 2 + (int) (roll * 2);
            default -> 2;
        };
    }
}
