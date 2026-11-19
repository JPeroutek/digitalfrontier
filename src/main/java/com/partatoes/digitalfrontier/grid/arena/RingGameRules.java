package com.partatoes.digitalfrontier.grid.arena;

/**
 * Scoring in the ring game (see {@link RingGame}): the challenger defends the north ring and scores through the
 * south one; the program the reverse. A goal is the ball passing through a ring's opening in the plane of the ring.
 * The first to {@link #TO_WIN} wins. Pure, so the scoring can be tested.
 */
public final class RingGameRules {
    /** Goals needed to win. */
    public static final int TO_WIN = 3;

    /** Who scored as the ball moved from one point to the next. */
    public enum Goal {
        NONE, CHALLENGER, PROGRAM
    }

    private RingGameRules() {
    }

    /**
     * Whether the ball's move scored, crossing a ring's plane inside its opening.
     *
     * @param northZ  the north ring's Z (the challenger's own)
     * @param southZ  the south ring's Z (the program's own)
     * @param centerX the rings' X
     * @param ringY   the rings' center height
     * @param opening how far from a ring's center still counts as through it
     */
    public static Goal scored(double fromZ, double toZ, double x, double y, double northZ, double southZ, double centerX, double ringY,
                              double opening) {
        boolean inOpening = Math.hypot(x - centerX, y - ringY) <= opening;
        if (!inOpening) {
            return Goal.NONE;
        }
        if (crossed(fromZ, toZ, southZ)) {
            return Goal.CHALLENGER;
        }
        if (crossed(fromZ, toZ, northZ)) {
            return Goal.PROGRAM;
        }
        return Goal.NONE;
    }

    private static boolean crossed(double from, double to, double plane) {
        return from < plane && to >= plane || from > plane && to <= plane;
    }

    public static boolean won(int goals) {
        return goals >= TO_WIN;
    }
}
