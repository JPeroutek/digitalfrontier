package com.partatoes.digitalfrontier.grid.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GridLayoutTest {
    private static final GridLayoutSettings DEFAULT = GridLayoutSettings.DEFAULT;

    private static GridLayoutSettings withSeed(long seed) {
        GridLayoutSettings d = DEFAULT;
        return new GridLayoutSettings(seed, d.corruption(), d.coreRadius(), d.cityRadius(), d.blockSize(),
                d.outskirtsRadius(), d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors());
    }

    @Test
    void sameSettingsGiveIdenticalPlans() {
        GridLayout a = new GridLayout(DEFAULT);
        GridLayout b = new GridLayout(DEFAULT);
        for (int x = -150; x <= 150; x += 7) {
            for (int z = -150; z <= 150; z += 7) {
                assertEquals(a.planAt(x, z), b.planAt(x, z), "plan at " + x + "," + z);
            }
        }
        assertEquals(a.sites(), b.sites());
    }

    @Test
    void differentSeedChangesPlansButNotCityZones() {
        GridLayout a = new GridLayout(DEFAULT);
        GridLayout b = new GridLayout(withSeed(12345L));
        boolean anyPlanDiffers = false;
        for (int x = -60; x <= 60; x++) {
            for (int z = -60; z <= 60; z++) {
                // Zones out to the outskirts don't depend on the seed (waypoints sit further out)
                assertEquals(a.zoneAt(x, z), b.zoneAt(x, z), "zone at " + x + "," + z);
                anyPlanDiffers |= !a.planAt(x, z).equals(b.planAt(x, z));
            }
        }
        assertTrue(anyPlanDiffers, "a different seed should change some chunk plans");
    }

    @Test
    void zonesFollowDistanceBands() {
        GridLayout layout = new GridLayout(DEFAULT);
        assertEquals(Zone.CITY_CORE, layout.zoneAt(0, 0));
        assertEquals(Zone.CITY, layout.zoneAt(DEFAULT.cityRadius() - 1, 0));
        assertEquals(Zone.UNALLOCATED, layout.zoneAt(DEFAULT.outlandsRadius() + 5, 0));
    }

    @Test
    void cityStreetsFollowTheBlockGrid() {
        GridLayout layout = new GridLayout(DEFAULT);
        int period = DEFAULT.blockSize() + 1;
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                ChunkPlan plan = layout.planAt(x, z);
                if (plan.site() != null || (plan.zone() != Zone.CITY && plan.zone() != Zone.CITY_CORE)) {
                    continue;
                }
                boolean onStreetLine = Math.floorMod(x, period) == 0 || Math.floorMod(z, period) == 0;
                assertEquals(onStreetLine, plan.type() == PlanType.STREET, "street at " + x + "," + z);
            }
        }
        assertTrue(layout.planAt(0, 8).avenue());
        assertFalse(layout.planAt(4, 8).avenue());
    }

    @Test
    void multiChunkBuildingsAreConsistentTwoByTwoGroups() {
        GridLayout layout = new GridLayout(DEFAULT);
        int groups = 0;
        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                ChunkPlan plan = layout.planAt(x, z);
                MultiChunk multi = plan.multiChunk();
                if (multi == null || multi.originX() != x || multi.originZ() != z) {
                    continue;
                }
                groups++;
                for (int dx = 0; dx < MultiChunk.SIZE; dx++) {
                    for (int dz = 0; dz < MultiChunk.SIZE; dz++) {
                        ChunkPlan member = layout.planAt(x + dx, z + dz);
                        assertEquals(PlanType.BUILDING, member.type(), "member of " + x + "," + z);
                        assertEquals(multi, member.multiChunk(), "member of " + x + "," + z);
                        assertEquals(plan.floors(), member.floors(), "floors of " + x + "," + z);
                        assertEquals(plan.district(), member.district(), "district of " + x + "," + z);
                    }
                }
            }
        }
        assertTrue(groups > 10, "expected a reasonable number of 2x2 buildings, got " + groups);
    }

    @Test
    void landmarkSitsNextToTheCityCenter() {
        MultiChunk landmark = new GridLayout(DEFAULT).planAt(1, 1).multiChunk();
        assertNotNull(landmark);
        assertTrue(landmark.landmark());
        assertEquals(1, landmark.originX());
        assertEquals(1, landmark.originZ());
    }

    @Test
    void sitesNeverOverlap() {
        for (long seed = 0; seed < 64; seed++) {
            List<Site> sites = new GridLayout(withSeed(seed)).sites();
            for (int i = 0; i < sites.size(); i++) {
                for (int j = i + 1; j < sites.size(); j++) {
                    Site a = sites.get(i);
                    Site b = sites.get(j);
                    assertTrue(a.distanceTo(b.chunkX(), b.chunkZ()) > 2 * GridLayout.SITE_RADIUS,
                            "seed " + seed + ": " + a.id() + " overlaps " + b.id());
                }
            }
        }
    }

    @Test
    void corruptionIsCleanAtTheCoreAndSevereAtTheStronghold() {
        GridLayout layout = new GridLayout(DEFAULT);
        assertEquals(CorruptionTier.CLEAN, layout.planAt(0, 0).tier());
        Site stronghold = layout.sites().stream().filter(site -> site.type() == Site.Type.STRONGHOLD).findFirst().orElseThrow();
        assertEquals(CorruptionTier.SEVERE, layout.planAt(stronghold.chunkX(), stronghold.chunkZ()).tier());
    }
}
