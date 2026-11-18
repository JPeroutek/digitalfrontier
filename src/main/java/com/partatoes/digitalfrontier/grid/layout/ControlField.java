package com.partatoes.digitalfrontier.grid.layout;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The regime's control in [0, 1] per chunk. Each stronghold holds the ground around it, out to a reach that grows
 * with the grid's regime setting, with ragged borders from low-frequency noise. A strong regime also patrols the
 * roads: above {@link #STREET_PRESENCE_FROM} a faint presence spreads along every road outside the city core.
 */
public final class ControlField {
    /** The regime setting above which roads are patrolled. */
    public static final float STREET_PRESENCE_FROM = 0.6f;
    private static final int SALT_BORDER = 201;

    private final GridLayoutSettings settings;
    private final List<Site> strongholds;

    ControlField(GridLayoutSettings settings, List<Site> strongholds) {
        this.settings = settings;
        this.strongholds = strongholds;
    }

    /** How far, in chunks, a stronghold's control reaches: 10 chunks with no regime, 24 at full strength. */
    public double reach() {
        return 10 + 14 * settings.regime();
    }

    /**
     * @param patrolledRoad whether the chunk is a road the regime could patrol (any road outside the city core)
     */
    public float intensityAt(int chunkX, int chunkZ, boolean patrolledRoad) {
        double value = 0;
        Site source = strongestSource(chunkX, chunkZ);
        if (source != null) {
            value = fromStronghold(source, chunkX, chunkZ);
        }
        if (patrolledRoad && settings.regime() > STREET_PRESENCE_FROM) {
            // Up to PATROLLED at full regime strength
            double presence = (settings.regime() - STREET_PRESENCE_FROM) / (1 - STREET_PRESENCE_FROM) * 0.5;
            value = Math.max(value, presence);
        }
        return (float) Math.clamp(value, 0.0, 1.0);
    }

    /** The stronghold whose control is strongest at this chunk, or null if none reaches it. */
    @Nullable
    public Site strongestSource(int chunkX, int chunkZ) {
        Site best = null;
        double bestValue = 0;
        for (Site stronghold : strongholds) {
            double value = fromStronghold(stronghold, chunkX, chunkZ);
            if (value > bestValue) {
                best = stronghold;
                bestValue = value;
            }
        }
        return best;
    }

    private double fromStronghold(Site stronghold, int chunkX, int chunkZ) {
        double distance = stronghold.distanceTo(chunkX, chunkZ) / reach();
        if (distance > 1.5) {
            return 0;
        }
        // Push the border in and out by up to a third of the reach, so it isn't a circle; the seat itself stays solid
        double noise = CorruptionField.valueNoise(settings.gridSeed(), chunkX, chunkZ, 8, SALT_BORDER) - 0.5;
        double ragged = distance * (1 + 0.6 * noise * Math.min(1, distance * 2));
        return 1 - CorruptionField.smoothstep(0.2, 1.0, ragged);
    }
}
