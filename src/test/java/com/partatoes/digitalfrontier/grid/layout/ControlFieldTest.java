package com.partatoes.digitalfrontier.grid.layout;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ControlFieldTest {
    private static GridLayoutSettings grid(long seed, float regime) {
        GridLayoutSettings d = GridLayoutSettings.DEFAULT;
        return new GridLayoutSettings(seed, d.corruption(), d.coreRadius(), d.cityRadius(), d.blockSize(), d.outskirtsRadius(),
                d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors(), d.streetStyle(), d.levels(),
                Optional.of(regime), d.regimeColor());
    }

    private static List<Site> strongholds(GridLayout layout) {
        return layout.sites().stream().filter(site -> site.type() == Site.Type.STRONGHOLD).toList();
    }

    private static int heldChunks(GridLayout layout) {
        int held = 0;
        int extent = layout.settings().outlandsRadius() + 30;
        for (int x = -extent; x <= extent; x++) {
            for (int z = -extent; z <= extent; z++) {
                if (layout.planAt(x, z).controlTier().compareTo(ControlTier.CONTROLLED) >= 0) {
                    held++;
                }
            }
        }
        return held;
    }

    @Test
    void controlFallsAwayFromTheSeat() {
        for (long seed = 0; seed < 16; seed++) {
            GridLayout layout = new GridLayout(grid(seed, 0.5f));
            Site stronghold = strongholds(layout).getFirst();
            ControlField field = new ControlField(layout.settings(), strongholds(layout));
            assertTrue(field.intensityAt(stronghold.chunkX(), stronghold.chunkZ(), false) > 0.95f, "seed " + seed);
            double angle = Math.atan2(stronghold.chunkZ(), stronghold.chunkX());
            int far = (int) Math.ceil(field.reach() * 1.6);
            int x = stronghold.chunkX() + (int) Math.round(far * Math.cos(angle));
            int z = stronghold.chunkZ() + (int) Math.round(far * Math.sin(angle));
            assertEquals(0f, field.intensityAt(x, z, false), "seed " + seed + ", well past the reach");
            assertSame(stronghold, field.strongestSource(stronghold.chunkX(), stronghold.chunkZ()));
        }
    }

    @Test
    void aStrongerRegimeHoldsMoreGround() {
        // The same single stronghold (regime under 65%), reaching further
        int weak = heldChunks(new GridLayout(grid(5, 0f)));
        int middling = heldChunks(new GridLayout(grid(5, 0.6f)));
        assertTrue(middling > weak * 1.5, weak + " vs " + middling);
    }

    @Test
    void theCityCoreIsAlwaysFree() {
        for (float regime : new float[]{0f, 0.5f, 1f}) {
            GridLayout layout = new GridLayout(grid(9, regime));
            int core = layout.settings().coreRadius();
            for (int x = -core; x <= core; x++) {
                for (int z = -core; z <= core; z++) {
                    if (Math.hypot(x, z) <= core) {
                        assertEquals(ControlTier.FREE, layout.planAt(x, z).controlTier(), "regime " + regime + " at " + x + "," + z);
                    }
                }
            }
        }
    }

    @Test
    void onlyAStrongRegimePatrolsTheRoads() {
        GridLayout weak = new GridLayout(grid(2, 0.5f));
        GridLayout strong = new GridLayout(grid(2, 1f));
        // The avenue at x = 0 runs out to the main terminal past the city edge
        int z = strong.settings().cityRadius() + 1;
        assertTrue(weak.planAt(0, z).road());
        assertEquals(ControlTier.FREE, weak.planAt(0, z).controlTier());
        assertEquals(ControlTier.PATROLLED, strong.planAt(0, z).controlTier());
        assertEquals(CorruptionTier.CLEAN, strong.planAt(0, z).tier(), "patrolled roads are kept clean");
    }
}
