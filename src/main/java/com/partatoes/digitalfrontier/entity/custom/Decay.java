package com.partatoes.digitalfrontier.entity.custom;

/**
 * How a program drifts into decay: the decayed ground it stands on and the corrupted programs around it wear it down,
 * a little every {@link #INTERVAL} ticks, and clean ground lets it recover. At 1 it's a corrupted program. Pure, so
 * the rates can be tested without a world.
 */
public final class Decay {
    /** Ticks between steps. */
    public static final int INTERVAL = 40;
    /** Ground decay below this does nothing to a program. */
    static final float SAFE_GROUND = 0.3f;
    static final float GROUND_RATE = 0.04f;
    static final float PER_CORRUPTED = 0.005f;
    static final float RECOVERY = 0.005f;
    /** From here on a program shows it: it jitters, and its circuitry frays. */
    public static final float VISIBLE = 0.3f;
    /** From here on its skin is the fraying one. */
    public static final float FRAYING = 0.6f;

    private Decay() {
    }

    /**
     * @param current         the program's decay, 0 to 1
     * @param groundDecay     the decay of the chunk it's in, 0 to 1
     * @param nearbyCorrupted corrupted programs within 8 blocks
     */
    public static float step(float current, float groundDecay, int nearbyCorrupted) {
        double pressure = Math.max(0, groundDecay - SAFE_GROUND) * GROUND_RATE + nearbyCorrupted * PER_CORRUPTED;
        double next = pressure > 0 ? current + pressure : current - RECOVERY;
        return (float) Math.clamp(next, 0.0, 1.0);
    }

    /** How hard a program at this decay jitters, for {@code GridMobRenderer}: none until it shows, 0.6 when gone. */
    public static float jitter(float decay) {
        return decay < VISIBLE ? 0 : (decay - VISIBLE) / (1 - VISIBLE) * 0.6f;
    }
}
