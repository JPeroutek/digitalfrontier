package com.partatoes.digitalfrontier.grid.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GridLayoutTest {
    private static final GridLayoutSettings DEFAULT = GridLayoutSettings.DEFAULT;
    private static final GridLayoutSettings UNIFORM = withStreetStyle(DEFAULT, StreetAxis.StreetStyle.UNIFORM);

    private static GridLayoutSettings withStreetStyle(GridLayoutSettings d, StreetAxis.StreetStyle style) {
        return new GridLayoutSettings(d.gridSeed(), d.corruption(), d.coreRadius(), d.cityRadius(), d.blockSize(),
                d.outskirtsRadius(), d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors(), style);
    }

    private static GridLayoutSettings withSeed(long seed) {
        GridLayoutSettings d = DEFAULT;
        return new GridLayoutSettings(seed, d.corruption(), d.coreRadius(), d.cityRadius(), d.blockSize(),
                d.outskirtsRadius(), d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors(), d.streetStyle());
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
    void uniformStreetsFollowTheBlockGrid() {
        GridLayout layout = new GridLayout(UNIFORM);
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
    void avenueConnectsTheCityToTheMainArrivalTerminal() {
        GridLayout layout = new GridLayout(DEFAULT);
        Site terminal = layout.mainArrival();
        for (int z = 0; z < terminal.chunkZ(); z++) {
            ChunkPlan plan = layout.planAt(terminal.chunkX(), z);
            assertEquals(PlanType.STREET, plan.type(), "street at " + terminal.chunkX() + "," + z);
            assertTrue(plan.streetZ(), "street along Z at " + terminal.chunkX() + "," + z);
        }
        ChunkPlan beside = layout.planAt(terminal.chunkX() + 1, DEFAULT.cityRadius() + 1);
        assertNotEquals(PlanType.STREET, beside.type());
    }

    @Test
    void terminalsAreTheMainArrivalAndTheWaypoints() {
        GridLayout layout = new GridLayout(DEFAULT);
        List<Site> terminals = layout.terminals();
        assertEquals(1 + DEFAULT.waypointCount(), terminals.size());
        assertEquals(GridLayout.MAIN_ARRIVAL_ID, terminals.getFirst().id());
        assertEquals(terminals.size(), terminals.stream().map(Site::id).distinct().count(), "terminal ids are unique");
        assertTrue(terminals.stream().allMatch(Site::hasTerminal));
        assertTrue(layout.terminal(GridLayout.MAIN_ARRIVAL_ID).isPresent());
        assertTrue(layout.terminal("stronghold_0").isEmpty(), "strongholds aren't terminals");
    }

    @Test
    void nearestTerminalPicksTheClosestOne() {
        GridLayout layout = new GridLayout(DEFAULT);
        assertEquals(GridLayout.MAIN_ARRIVAL_ID, layout.nearestTerminal(0, 0).id());
        for (Site terminal : layout.terminals()) {
            assertEquals(terminal, layout.nearestTerminal(terminal.chunkX(), terminal.chunkZ()));
        }
    }

    @Test
    void portalSitsInTheOutlandsOppositeTheStronghold() {
        for (long seed = 0; seed < 16; seed++) {
            GridLayout layout = new GridLayout(withSeed(seed));
            Site portal = layout.sites().stream().filter(site -> site.type() == Site.Type.PORTAL).findFirst().orElseThrow();
            Site stronghold = layout.sites().stream().filter(site -> site.type() == Site.Type.STRONGHOLD).findFirst().orElseThrow();
            assertEquals(Zone.OUTLANDS, layout.zoneAt(portal.chunkX(), portal.chunkZ()), "seed " + seed);
            assertEquals(PlanType.SITE, layout.planAt(portal.chunkX(), portal.chunkZ()).type(), "seed " + seed);
            assertTrue(portal.distanceTo(stronghold.chunkX(), stronghold.chunkZ()) > DEFAULT.outlandsRadius(), "seed " + seed);
        }
    }

    @Test
    void highwaysRunFromTheCityEdgeToEveryWaypoint() {
        // Seeds and waypoint counts that between them put waypoints on both axes and both diagonals
        for (long seed = 0; seed < 8; seed++) {
            GridLayout layout = new GridLayout(withSeed(seed));
            for (Site waypoint : layout.sites()) {
                if (waypoint.type() != Site.Type.WAYPOINT) {
                    continue;
                }
                int signX = Integer.signum(waypoint.chunkX());
                int signZ = Integer.signum(waypoint.chunkZ());
                int steps = Math.max(Math.abs(waypoint.chunkX()), Math.abs(waypoint.chunkZ()));
                for (int step = 1; step < steps; step++) {
                    int x = signX * step;
                    int z = signZ * step;
                    if (layout.zoneAt(x, z) == Zone.CITY || layout.zoneAt(x, z) == Zone.CITY_CORE) {
                        continue;
                    }
                    ChunkPlan plan = layout.planAt(x, z);
                    String where = "seed " + seed + ", " + waypoint.id() + " at " + x + "," + z;
                    if (plan.type() == PlanType.SITE) {
                        // A highway on +Z passes through the main arrival terminal
                        continue;
                    }
                    if (signX == 0 || signZ == 0) {
                        assertEquals(PlanType.STREET, plan.type(), where);
                        assertEquals(signZ == 0, plan.streetX(), where);
                    } else {
                        assertNotNull(plan.diagonal(), where);
                        assertTrue(plan.diagonal().contains(x * 16 + 7, z * 16 + 7), where);
                        assertEquals(0.0, layout.highwayDistance(x * 16 + 7, z * 16 + 7), where);
                    }
                }
            }
        }
    }

    @Test
    void waypointSettlementsHaveAStreetCrossThroughTheTerminal() {
        GridLayout layout = new GridLayout(DEFAULT);
        for (Site waypoint : layout.sites()) {
            if (waypoint.type() != Site.Type.WAYPOINT) {
                continue;
            }
            for (int d = 1; d < GridLayout.SITE_RADIUS; d++) {
                for (int[] arm : new int[][]{{d, 0}, {-d, 0}, {0, d}, {0, -d}}) {
                    ChunkPlan plan = layout.planAt(waypoint.chunkX() + arm[0], waypoint.chunkZ() + arm[1]);
                    assertEquals(PlanType.STREET, plan.type(), waypoint.id() + " arm " + arm[0] + "," + arm[1]);
                }
            }
        }
    }

    @Test
    void highwayDistanceIsZeroOnlyNearHighways() {
        GridLayout layout = new GridLayout(DEFAULT);
        Site stronghold = layout.sites().stream().filter(site -> site.type() == Site.Type.STRONGHOLD).findFirst().orElseThrow();
        // Strongholds sit between the waypoint directions, so no highway passes them
        assertTrue(layout.highwayDistance(stronghold.chunkX() * 16, stronghold.chunkZ() * 16) > 100);
    }

    @Test
    void variedCitiesSplitAroundACentralBoulevard() {
        GridLayout layout = new GridLayout(DEFAULT);
        // Along both axes, chunks -1 and 0 are the two halves of the central boulevard (except where they cross)
        for (int along = 3; along < 30; along++) {
            if (layout.planAt(along, -1).streetZ() || layout.planAt(along, 0).streetZ()) {
                continue;
            }
            assertEquals(new StreetAxis.Road(StreetAxis.RoadClass.BOULEVARD, 0, true), layout.planAt(along, -1).roadX());
            assertEquals(new StreetAxis.Road(StreetAxis.RoadClass.BOULEVARD, 1, true), layout.planAt(along, 0).roadX());
        }
        assertEquals(new StreetAxis.Road(StreetAxis.RoadClass.BOULEVARD, 0, true), layout.planAt(-1, 20).roadZ());
        assertEquals(new StreetAxis.Road(StreetAxis.RoadClass.BOULEVARD, 1, true), layout.planAt(0, 20).roadZ());
        assertFalse(layout.planAt(0, 20).avenue(), "the boulevard has a median, not an avenue center line");
    }

    @Test
    void variedAxesAlternateBlocksAndRoadsOfEveryWidth() {
        java.util.Set<StreetAxis.RoadClass> seen = java.util.EnumSet.noneOf(StreetAxis.RoadClass.class);
        for (long seed = 0; seed < 8; seed++) {
            StreetAxis axis = new StreetAxis(StreetAxis.StreetStyle.VARIED, seed, 10, 3, 60);
            for (int c = -55; c <= 55; c++) {
                StreetAxis.Road road = axis.roadAt(c);
                String where = "seed " + seed + ", chunk " + c;
                if (road == null) {
                    StreetAxis.Block block = axis.blockAt(c);
                    assertTrue(c >= block.start() && c < block.start() + block.size(), where);
                    assertTrue(block.size() >= 2 && block.size() <= 5, where + ": block of " + block.size());
                    // A block ends at a road (or runs off the computed range)
                    assertNotNull(axis.roadAt(block.start() - 1), where);
                    assertNotNull(axis.roadAt(block.start() + block.size()), where);
                    continue;
                }
                seen.add(road.roadClass());
                if (road.roadClass() == StreetAxis.RoadClass.BOULEVARD) {
                    // Each half has its partner on the right side
                    StreetAxis.Road partner = axis.roadAt(road.half() == 0 ? c + 1 : c - 1);
                    assertNotNull(partner, where);
                    assertEquals(StreetAxis.RoadClass.BOULEVARD, partner.roadClass(), where);
                    assertEquals(1 - road.half(), partner.half(), where);
                } else {
                    assertNull(axis.roadAt(c - 1), where + ": one-chunk roads stand alone");
                    assertNull(axis.roadAt(c + 1), where + ": one-chunk roads stand alone");
                }
            }
        }
        assertEquals(java.util.EnumSet.allOf(StreetAxis.RoadClass.class), seen);
    }

    @Test
    void uniformAxesMatchTheOriginalModuloLayout() {
        StreetAxis axis = new StreetAxis(StreetAxis.StreetStyle.UNIFORM, 0, 10, 3, 60);
        for (int c = -50; c <= 50; c++) {
            assertEquals(Math.floorMod(c, 4) == 0, axis.roadAt(c) != null, "chunk " + c);
            if (axis.roadAt(c) == null) {
                assertEquals(Math.floorDiv(c, 4) * 4 + 1, axis.blockAt(c).start(), "chunk " + c);
            }
        }
    }

    private static GridLayoutSettings withSeedAndCorruption(long seed, float corruption) {
        GridLayoutSettings d = DEFAULT;
        return new GridLayoutSettings(seed, corruption, d.coreRadius(), d.cityRadius(), d.blockSize(),
                d.outskirtsRadius(), d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors(), d.streetStyle());
    }

    private static List<Site> sitesOfType(GridLayout layout, Site.Type type) {
        return layout.sites().stream().filter(site -> site.type() == type).toList();
    }

    @Test
    void defaultGridKeepsItsOneStrongholdAndPortal() {
        // Existing worlds were generated with these; more strongholds must not move them
        GridLayout layout = new GridLayout(DEFAULT);
        List<Site> strongholds = sitesOfType(layout, Site.Type.STRONGHOLD);
        assertEquals(1, strongholds.size());
        assertEquals(new Site(Site.Type.STRONGHOLD, "stronghold_0", 89, -37), strongholds.getFirst());
        assertEquals(new Site(Site.Type.PORTAL, "portal_0", -111, 46), sitesOfType(layout, Site.Type.PORTAL).getFirst());
    }

    @Test
    void moreCorruptedGridsHaveMoreStrongholds() {
        assertEquals(1, GridLayout.strongholdCount(0f));
        assertEquals(1, GridLayout.strongholdCount(0.5f));
        assertEquals(2, GridLayout.strongholdCount(0.65f));
        assertEquals(3, GridLayout.strongholdCount(0.85f));
        for (long seed = 0; seed < 32; seed++) {
            for (float corruption : new float[]{0.5f, 0.7f, 0.9f}) {
                GridLayout layout = new GridLayout(withSeedAndCorruption(seed, corruption));
                List<Site> strongholds = sitesOfType(layout, Site.Type.STRONGHOLD);
                String where = "seed " + seed + ", corruption " + corruption;
                assertEquals(GridLayout.strongholdCount(corruption), strongholds.size(), where);
                assertEquals(strongholds.size(), strongholds.stream().map(Site::id).distinct().count(), where);
                Site portal = sitesOfType(layout, Site.Type.PORTAL).getFirst();
                for (Site stronghold : strongholds) {
                    assertEquals(CorruptionTier.SEVERE, layout.planAt(stronghold.chunkX(), stronghold.chunkZ()).tier(), where);
                    assertEquals(PlanType.SITE, layout.planAt(stronghold.chunkX(), stronghold.chunkZ()).type(), where);
                    // Never in the portal's direction
                    double angle = Math.abs(Math.atan2(stronghold.chunkZ(), stronghold.chunkX()) - Math.atan2(portal.chunkZ(), portal.chunkX()));
                    assertTrue(Math.min(angle, 2 * Math.PI - angle) > Math.PI / 8, where + ", " + stronghold.id());
                }
                // The first stronghold doesn't depend on corruption, so the portal stays opposite it
                assertEquals(sitesOfType(new GridLayout(withSeed(seed)), Site.Type.STRONGHOLD).getFirst(), strongholds.getFirst(), where);
            }
        }
    }

    @Test
    void sitesNeverOverlapInCorruptedGrids() {
        for (long seed = 0; seed < 64; seed++) {
            List<Site> sites = new GridLayout(withSeedAndCorruption(seed, 1f)).sites();
            for (int i = 0; i < sites.size(); i++) {
                for (int j = i + 1; j < sites.size(); j++) {
                    assertTrue(sites.get(i).distanceTo(sites.get(j).chunkX(), sites.get(j).chunkZ()) > 2 * GridLayout.SITE_RADIUS,
                            "seed " + seed + ": " + sites.get(i).id() + " overlaps " + sites.get(j).id());
                }
            }
        }
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
