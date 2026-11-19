package com.partatoes.digitalfrontier.grid.regime;

/**
 * A program's sentiment toward the regime, from -1 (it would fight the regime) to 1 (it would join it). Pure
 * functions, so the numbers can be tested without a world.
 */
public final class Sentiment {
    /** At or above this a program is loyal: it seeks out the regime and reports rebels. */
    public static final float LOYAL = 0.5f;
    /** At or below this a program is a rebel: it avoids the regime, and enforcers hunt it. */
    public static final float REBEL = -0.5f;
    /** At or below this a rebel fights back: it attacks recruiters and enforcers that attack others. */
    public static final float RESISTANCE = -0.8f;
    /** Sentiment that backlashes against a recruiter's pitch instead of warming to it. */
    public static final float BACKLASH = -0.6f;
    /** The least any act by a player moves a program, so even a player who hasn't picked a side matters. */
    public static final float MIN_PLAYER_EFFECT = 0.02f;
    static final float SPREAD = 0.35f;

    public enum Stance {
        RESISTANCE, REBEL, NEUTRAL, LOYAL
    }

    private Sentiment() {
    }

    /**
     * A new program's sentiment: roughly normal around a mean that leans loyal where the regime holds the ground, and
     * with the strength of the grid's regime.
     *
     * @param a       a roll in [0, 1); three of them, summed, make the bell curve
     * @param control the regime's control where the program lives, 0 to 1
     * @param regime  the grid's regime setting, 0 to 1
     */
    public static float initial(double a, double b, double c, float control, float regime) {
        double mean = 0.3 * control + 0.2 * (regime - 0.5);
        // The sum of three uniform rolls has a standard deviation of 0.5
        double normal = (a + b + c - 1.5) / 0.5;
        return clamp(mean + SPREAD * normal);
    }

    public static Stance stance(float sentiment) {
        if (sentiment <= RESISTANCE) {
            return Stance.RESISTANCE;
        }
        if (sentiment <= REBEL) {
            return Stance.REBEL;
        }
        return sentiment >= LOYAL ? Stance.LOYAL : Stance.NEUTRAL;
    }

    /**
     * An act by a player toward a program: kindness (a positive {@code strength}) pulls the program toward the player's
     * side, harm (negative) pushes it away. The effect scales with how firmly the player has picked a side, but is
     * never less than {@link #MIN_PLAYER_EFFECT}. Players are outsiders: until one sides with the regime (a positive
     * alignment), their side counts as against it.
     */
    public static float byPlayer(float sentiment, float alignment, float strength) {
        int side = alignment > 0 ? 1 : -1;
        double amount = Math.max(MIN_PLAYER_EFFECT, Math.abs(strength) * Math.abs(alignment));
        return clamp(sentiment + side * Math.signum(strength) * amount);
    }

    /** A second near a recruiter: it warms to the regime, unless it's already set against it, when it hardens. */
    public static float exposed(float sentiment, float perSecond) {
        return clamp(sentiment <= BACKLASH ? sentiment - perSecond : sentiment + perSecond);
    }

    public static float clamp(double sentiment) {
        return (float) Math.clamp(sentiment, -1.0, 1.0);
    }
}
