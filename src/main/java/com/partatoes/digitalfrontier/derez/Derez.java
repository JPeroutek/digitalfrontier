package com.partatoes.digitalfrontier.derez;

/**
 * Derezzing: in place of vanilla's death (tipping over, a red flash, a puff of smoke), a body comes apart into
 * glowing cubes. It's purely visual and drawn by the client; the server still removes the body after
 * {@value #TICKS} ticks as usual. Everything that dies in a grid derezzes, and the mod's own mobs derez anywhere.
 * See {@link DerezStyle} for the ways it can look.
 */
public final class Derez {
    /** How long dying lasts before vanilla removes the body. */
    public static final int TICKS = 20;
    /** The fewest and most cubes a body comes apart into. */
    public static final int MIN_CUBES = 24;
    public static final int MAX_CUBES = 300;
    // Cubes per cubic block of body
    private static final double CUBES_PER_VOLUME = 80;

    private Derez() {
    }

    /**
     * Whether a dying entity derezzes.
     *
     * @param ownMob   whether it's one of the mod's own mobs
     * @param inGrid   whether it's in a grid
     * @param excluded whether it has a death of its own that must be left alone (the Ender Dragon, the Wither)
     */
    public static boolean derezzes(boolean ownMob, boolean inGrid, boolean excluded) {
        return !excluded && (ownMob || inGrid);
    }

    /** How far through derezzing a body is, 0 to 1, from vanilla's death time (with partial ticks). */
    public static float progress(float deathTime) {
        return Math.clamp(deathTime / TICKS, 0, 1);
    }

    /** The most cubes a statue (see {@link DerezStyle#VOXEL}) is built from; bigger models get bigger cubes. */
    public static final int MAX_STATUE_CUBES = 700;
    /** The smallest a statue's cube is, in blocks: two model pixels. */
    public static final float MIN_STATUE_CUBE = 2 / 16f;
    /** How long a statue stands whole, and how long it takes to come apart from head to feet, in ticks. */
    public static final int STATUE_HOLD = 4;
    public static final int STATUE_WAVE = 10;
    /** How opaque a statue's cubes are while it stands; once they break off they drop to {@link #LOOSE_OPACITY}. */
    public static final float STATUE_OPACITY = 0.8f;
    /** How opaque loose cubes are: shed by the other styles, or broken off a statue. */
    public static final float LOOSE_OPACITY = 0.6f;

    /**
     * The size of a statue's cubes, in blocks, for a model with this much surface (in square blocks): as small as
     * {@link #MIN_STATUE_CUBE}, but large enough that the surface takes at most {@link #MAX_STATUE_CUBES}.
     */
    public static float statueCube(double surface) {
        return (float) Math.max(MIN_STATUE_CUBE, Math.sqrt(surface / MAX_STATUE_CUBES));
    }

    /**
     * When one of a statue's cubes breaks off, in ticks after the statue forms, by its height up the statue (0 at the
     * feet, 1 at the head): it stands whole for {@link #STATUE_HOLD} ticks, then comes apart from the head down.
     */
    public static int releaseDelay(float heightFraction) {
        return STATUE_HOLD + Math.round((1 - Math.clamp(heightFraction, 0, 1)) * STATUE_WAVE);
    }

    /** How many cubes a body of this size comes apart into. */
    public static int budget(float width, float height) {
        return (int) Math.clamp(Math.round(width * width * height * CUBES_PER_VOLUME), MIN_CUBES, MAX_CUBES);
    }
}
