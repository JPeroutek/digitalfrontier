package com.partatoes.digitalfrontier.derez;

/**
 * How a body derezzes over its second of dying (see {@link Derez}). Progress runs from 0 at the moment of death to 1
 * when vanilla removes the body, {@value Derez#TICKS} ticks later. Pure, so the timings can be tested without a game.
 */
public enum DerezStyle {
    /** The body dissolves cell by cell while a band sweeps down it, shedding cubes as it goes. */
    SWEEP,
    /** The body bursts into cubes at once. */
    INSTANT,
    /** The body freezes upright and flashes white, then bursts at the halfway mark. */
    FREEZE,
    /**
     * The body turns into a statue of cubes in its own shape and colors, which holds for a moment and then comes apart
     * from the head down, its cubes flying off and falling (see {@link Derez#releaseDelay}).
     */
    VOXEL;

    /** When FREEZE bursts, as progress. */
    public static final float BURST = 0.5f;

    /** The body's dissolve alpha: 1 is whole, 0 is gone. Only SWEEP dissolves; the others stay whole until they go. */
    public float bodyAlpha(float progress) {
        return this == SWEEP ? Math.clamp(1 - progress, 0, 1) : 1;
    }

    /** Whether the body is still drawn at all. */
    public boolean bodyVisible(float progress) {
        return switch (this) {
            case SWEEP -> progress < 1;
            case INSTANT -> progress <= 0;
            case FREEZE -> progress < BURST;
            // Its statue takes its place after the first tick
            case VOXEL -> progress < 1f / Derez.TICKS;
        };
    }

    /** How white the body flashes, 0 to 1: FREEZE pulses until it bursts. */
    public float whiteFlash(float progress) {
        if (this != FREEZE || progress >= BURST) {
            return 0;
        }
        return 0.6f + 0.4f * (float) Math.sin(progress * 40);
    }

    /** The tick (1 to {@value Derez#TICKS}) on which a style bursts all its cubes, or 0 if it sheds them over time. */
    public int burstTick() {
        return switch (this) {
            case SWEEP, VOXEL -> 0;
            case INSTANT -> 1;
            case FREEZE -> Math.round(BURST * Derez.TICKS);
        };
    }

    /** Whether the body is rebuilt as a statue of cubes from its own model, rather than shedding loose cubes. */
    public boolean statue() {
        return this == VOXEL;
    }

    /**
     * How many loose cubes a body sheds on a tick of its death (1 to {@value Derez#TICKS}); they add up to the budget.
     * A statue sheds none: its cubes are its model's.
     */
    public int cubesThisTick(int tick, int budget) {
        if (tick < 1 || tick > Derez.TICKS || statue()) {
            return 0;
        }
        if (this != SWEEP) {
            return tick == burstTick() ? budget : 0;
        }
        // Spread evenly, the remainder going to the first ticks
        return budget * tick / Derez.TICKS - budget * (tick - 1) / Derez.TICKS;
    }

    /**
     * The slice of the body (as fractions of its height, bottom to top) that cubes come from on a tick: for SWEEP, a
     * band moving from the head down to the feet; for the others, the whole body.
     */
    public float[] band(int tick) {
        if (this != SWEEP) {
            return new float[]{0, 1};
        }
        float step = 1f / Derez.TICKS;
        float top = 1 - (Math.clamp(tick, 1, Derez.TICKS) - 1) * step;
        return new float[]{Math.max(0, top - step * 1.5f), top};
    }

    /** How hard its cubes fly out from the body, in blocks a tick. */
    public double kick() {
        return this == SWEEP ? 0.04 : 0.12;
    }
}
