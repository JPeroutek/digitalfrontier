package com.partatoes.digitalfrontier.grid.layout;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TerritoryTest {
    private static GridLayoutSettings grid(long seed, float corruption, float regime) {
        GridLayoutSettings d = GridLayoutSettings.DEFAULT;
        return new GridLayoutSettings(seed, corruption, d.coreRadius(), d.cityRadius(), d.blockSize(), d.outskirtsRadius(),
                d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors(), d.streetStyle(), d.levels(),
                Optional.of(regime), d.regimeColor());
    }

    @Test
    void theSeatIsNeverDecayed() {
        for (double roll = 0; roll < 1; roll += 0.1) {
            assertEquals(0f, Territory.decay(1f, ControlTier.SEAT.minIntensity(), roll, false));
        }
    }

    @Test
    void patrolledRoadsAreNeverDecayedButOtherRoadsAre() {
        assertEquals(0f, Territory.decay(1f, ControlTier.PATROLLED.minIntensity(), 0.5, true));
        assertTrue(Territory.decay(1f, ControlTier.PATROLLED.minIntensity(), 0.5, false) > 0.5f, "off the road, pockets survive");
        assertTrue(Territory.decay(1f, ControlTier.WATCHED.minIntensity(), 0.5, true) > 0.8f, "a watched road is barely kept");
    }

    @Test
    void freeGroundKeepsItsDecay() {
        for (float raw = 0; raw <= 1; raw += 0.1f) {
            assertEquals(raw, Territory.decay(raw, 0f, 0.3, false), 1e-6);
        }
    }

    @Test
    void suppressionGrowsWithControlAndVariesByChunk() {
        float previous = 1f;
        for (float control = 0; control < ControlTier.SEAT.minIntensity(); control += 0.05f) {
            float decay = Territory.decay(1f, control, 0.5, false);
            assertTrue(decay <= previous, "control " + control);
            previous = decay;
        }
        float control = 0.5f;
        assertTrue(Territory.decay(1f, control, 0.0, false) > Territory.decay(1f, control, 0.99, false),
                "a poorly kept chunk keeps more decay");
    }

    @Test
    void underStrongControlDecayIsAlmostNeverSignificant() {
        int controlled = 0;
        int decayed = 0;
        for (long seed = 0; seed < 6; seed++) {
            for (float regime : new float[]{0.3f, 0.6f, 0.95f}) {
                GridLayout layout = new GridLayout(grid(seed, 1f, regime));
                int extent = layout.settings().outlandsRadius();
                for (int x = -extent; x <= extent; x += 2) {
                    for (int z = -extent; z <= extent; z += 2) {
                        ChunkPlan plan = layout.planAt(x, z);
                        if (plan.zone() == Zone.DERELICT || plan.controlTier().compareTo(ControlTier.CONTROLLED) < 0) {
                            continue;
                        }
                        controlled++;
                        if (plan.tier().compareTo(CorruptionTier.UNSTABLE) >= 0) {
                            decayed++;
                        }
                    }
                }
            }
        }
        assertTrue(controlled > 1000, "enough controlled chunks to judge: " + controlled);
        assertTrue(decayed < controlled * 0.02, decayed + " of " + controlled + " controlled chunks decayed");
    }

    @Test
    void freeGroundStillDecaysInADecayedGrid() {
        GridLayout layout = new GridLayout(grid(3, 1f, 0.5f));
        int free = 0;
        int decayed = 0;
        int extent = layout.settings().outlandsRadius();
        for (int x = -extent; x <= extent; x += 2) {
            for (int z = -extent; z <= extent; z += 2) {
                ChunkPlan plan = layout.planAt(x, z);
                if (plan.controlTier() == ControlTier.FREE && Math.hypot(x, z) > layout.settings().outskirtsRadius()) {
                    free++;
                    if (plan.tier().compareTo(CorruptionTier.UNSTABLE) >= 0) {
                        decayed++;
                    }
                }
            }
        }
        assertTrue(decayed > free / 2, decayed + " of " + free + " free outland chunks decayed");
    }
}
