package com.partatoes.digitalfrontier.grid.layout;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DecayGradientTest {
    private static GridLayoutSettings grid(long seed, float corruption, float regime) {
        GridLayoutSettings d = GridLayoutSettings.DEFAULT;
        return new GridLayoutSettings(seed, corruption, d.coreRadius(), d.cityRadius(), d.blockSize(), d.outskirtsRadius(),
                d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors(), d.streetStyle(), d.levels(),
                Optional.of(regime), d.regimeColor());
    }

    /**
     * Walking out from a stronghold, the chunks between the last clean one and the first with static (CORRUPTED), for
     * every ray that reaches static at all.
     */
    static List<Integer> bands(float corruption, float regime) {
        List<Integer> bands = new ArrayList<>();
        for (long seed = 0; seed < 8; seed++) {
            GridLayout layout = new GridLayout(grid(seed, corruption, regime));
            for (Site stronghold : layout.sites()) {
                if (stronghold.type() != Site.Type.STRONGHOLD) {
                    continue;
                }
                for (int ray = 0; ray < 16; ray++) {
                    double angle = ray * Math.PI / 8;
                    int lastClean = -1;
                    for (int step = 0; step < 60; step++) {
                        int x = stronghold.chunkX() + (int) Math.round(step * Math.cos(angle));
                        int z = stronghold.chunkZ() + (int) Math.round(step * Math.sin(angle));
                        ChunkPlan plan = layout.planAt(x, z);
                        if (plan.road() || plan.zone() == Zone.DERELICT) {
                            continue;
                        }
                        if (plan.tier() == CorruptionTier.CLEAN) {
                            lastClean = step;
                        } else if (plan.tier().compareTo(CorruptionTier.CORRUPTED) >= 0) {
                            if (lastClean >= 0) {
                                bands.add(step - lastClean);
                            }
                            break;
                        }
                    }
                }
            }
        }
        Collections.sort(bands);
        return bands;
    }

    @Test
    void decayBuildsUpGraduallyAwayFromTheRegime() {
        List<Integer> bands = bands(0.5f, 0.5f);
        assertFalse(bands.isEmpty());
        // At least four chunks (64 blocks) from clean ground to static everywhere, and usually twice that. An S-curve
        // suppression made it as little as three, which looked like a sudden switch
        assertTrue(bands.getFirst() >= 4, "narrowest border " + bands);
        assertTrue(bands.get(bands.size() / 4) >= 8, "lower quartile " + bands);
    }
}
