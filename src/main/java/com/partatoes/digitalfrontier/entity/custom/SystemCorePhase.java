package com.partatoes.digitalfrontier.entity.custom;

import java.util.List;

/**
 * The System Core's fight, by how much health it has left. Each phase has its own cadence of attacks, in ticks
 * (0 for none), and its own mix of bolts. Pure, so the thresholds can be tested without a world.
 */
public enum SystemCorePhase {
    /** Whole: it draws programs in and absorbs them to heal, and calls up decayed ones to feed on. */
    ABSORBING(0, 0, 100, List.of()),
    /** Below half, its shell broken: homing bolts, and enforcer squads. */
    EXPOSED(60, 300, 0, List.of(CoreBoltEffect.FREEZE, CoreBoltEffect.KNOCKBACK, CoreBoltEffect.LEVITATE)),
    /** Below a fifth: everything faster, and bolts that black out the view. */
    ENRAGED(25, 200, 0, List.of(CoreBoltEffect.FREEZE, CoreBoltEffect.KNOCKBACK, CoreBoltEffect.LEVITATE, CoreBoltEffect.GLITCH));

    public static final float EXPOSED_BELOW = 0.5f;
    public static final float ENRAGED_BELOW = 0.2f;

    private final int boltEvery;
    private final int squadEvery;
    private final int feedEvery;
    private final List<CoreBoltEffect> bolts;

    SystemCorePhase(int boltEvery, int squadEvery, int feedEvery, List<CoreBoltEffect> bolts) {
        this.boltEvery = boltEvery;
        this.squadEvery = squadEvery;
        this.feedEvery = feedEvery;
        this.bolts = bolts;
    }

    public static SystemCorePhase of(float healthFraction) {
        if (healthFraction < ENRAGED_BELOW) {
            return ENRAGED;
        }
        return healthFraction < EXPOSED_BELOW ? EXPOSED : ABSORBING;
    }

    public boolean absorbs() {
        return this == ABSORBING;
    }

    public int boltEvery() {
        return boltEvery;
    }

    public int squadEvery() {
        return squadEvery;
    }

    public int feedEvery() {
        return feedEvery;
    }

    /** The bolts it fires in this phase, picked from evenly. */
    public List<CoreBoltEffect> bolts() {
        return bolts;
    }

    /** The bolt to fire, from a roll in [0, 1). */
    public CoreBoltEffect bolt(double roll) {
        return bolts.get(Math.min(bolts.size() - 1, (int) (roll * bolts.size())));
    }
}
