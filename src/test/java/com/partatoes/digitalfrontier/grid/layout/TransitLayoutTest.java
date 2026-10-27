package com.partatoes.digitalfrontier.grid.layout;

import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.grid.GridKey;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransitLayoutTest {
    private static final GridLayoutSettings DEFAULT = GridLayoutSettings.DEFAULT;

    private static GridLayoutSettings with(long seed, Set<GridLevel> levels) {
        GridLayoutSettings d = DEFAULT;
        return new GridLayoutSettings(seed, d.corruption(), d.coreRadius(), d.cityRadius(), d.blockSize(), d.outskirtsRadius(),
                d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors(), d.streetStyle(), levels);
    }

    // Every boulevard line through the city, as {first row/column, along X?}
    private static List<int[]> boulevards(GridLayout layout) {
        List<int[]> lines = new ArrayList<>();
        for (int c = -45; c <= 45; c++) {
            ChunkPlan alongX = layout.planAt(30, c);
            if (alongX.roadX() != null && alongX.roadX().roadClass() == RoadClass.BOULEVARD && alongX.roadX().half() == 0) {
                lines.add(new int[]{c, 1});
            }
            ChunkPlan alongZ = layout.planAt(c, 30);
            if (alongZ.roadZ() != null && alongZ.roadZ().roadClass() == RoadClass.BOULEVARD && alongZ.roadZ().half() == 0) {
                lines.add(new int[]{c, 0});
            }
        }
        return lines;
    }

    @Test
    void skyroadDecksRunTheLengthOfEveryBoulevard() {
        for (long seed = 0; seed < 6; seed++) {
            GridLayout layout = new GridLayout(with(seed, Set.of(GridLevel.SKYROADS)));
            TransitLayout transit = layout.transit();
            List<int[]> lines = boulevards(layout);
            assertFalse(lines.isEmpty(), "seed " + seed + " has boulevards (at least the central ones)");
            for (int[] line : lines) {
                // Across the line, the median sits between the two rows; check the blocks either side of it
                int median = (line[0] + 1) * 16;
                for (int along = -16 * 20; along < 16 * 20; along++) {
                    int level0 = line[1] == 1 ? transit.deckLevel(along, median - 1) : transit.deckLevel(median - 1, along);
                    int level1 = line[1] == 1 ? transit.deckLevel(along, median) : transit.deckLevel(median, along);
                    String where = "seed " + seed + ", line " + line[0] + (line[1] == 1 ? " along X" : " along Z") + " at " + along;
                    if (Math.hypot(along / 16.0, line[0]) < DEFAULT.cityRadius() - 4) {
                        assertEquals(TransitLayout.DECK_LEVEL, level0, where);
                        assertEquals(TransitLayout.DECK_LEVEL, level1, where);
                    }
                }
            }
        }
    }

    @Test
    void decksRampDownToTheStreetAtTheCityEdge() {
        GridLayout layout = new GridLayout(with(0, Set.of(GridLevel.SKYROADS)));
        TransitLayout transit = layout.transit();
        // The central boulevard along X has its median at block row 0
        int previous = TransitLayout.DECK_LEVEL;
        int along = 0;
        while (transit.deckLevel(along, 0) >= 0) {
            int level = transit.deckLevel(along, 0);
            assertTrue(previous - level <= 1, "the ramp descends half a block per block at " + along);
            previous = level;
            along++;
        }
        assertEquals(0, previous, "the deck reaches the street");
        assertTrue(Math.hypot(along / 16.0, 0) <= DEFAULT.cityRadius() + 1, "and ends at the city edge");
    }

    @Test
    void onRampsClimbTwoChunksOfABlockStretchAndNeverAnIntersection() {
        int ramps = 0;
        for (long seed = 0; seed < 6; seed++) {
            GridLayout layout = new GridLayout(with(seed, Set.of(GridLevel.SKYROADS)));
            TransitLayout transit = layout.transit();
            for (int x = -30 * 16; x < 30 * 16; x++) {
                for (int z = -30 * 16; z < 30 * 16; z += 3) {
                    int level = transit.rampLevel(x, z);
                    if (level < 0) {
                        continue;
                    }
                    ramps++;
                    ChunkPlan plan = layout.planAt(x >> 4, z >> 4);
                    String where = "seed " + seed + " at " + x + "," + z;
                    assertEquals(PlanType.STREET, plan.type(), where);
                    assertFalse(plan.streetX() && plan.streetZ(), where + ": ramps aren't on intersections");
                    assertTrue(transit.deckLevel(x, z) < 0, where + ": ramps run beside the deck, not on it");
                    assertTrue(level <= TransitLayout.DECK_LEVEL, where);
                }
            }
        }
        assertTrue(ramps > 0, "some on-ramps exist");
    }

    @Test
    void railRunsAlongZAndSubwayAlongXWithSpacedStations() {
        for (long seed = 0; seed < 6; seed++) {
            GridLayout layout = new GridLayout(with(seed, Set.of(GridLevel.ELEVATED_RAIL, GridLevel.SUBWAY)));
            TransitLayout transit = layout.transit();
            List<Integer> railColumns = new ArrayList<>();
            List<Integer> subwayRows = new ArrayList<>();
            for (int c = -40; c <= 40; c++) {
                if (transit.elevatedRail(c, 0) || transit.elevatedRail(c, 5)) {
                    railColumns.add(c);
                    ChunkPlan plan = layout.planAt(c, 7);
                    assertNotNull(plan.roadZ(), "seed " + seed + ": rail column " + c + " is a road along Z");
                    assertEquals(RoadClass.STREET, plan.roadZ().roadClass());
                }
                if (transit.subway(0, c) || transit.subway(5, c)) {
                    subwayRows.add(c);
                    ChunkPlan plan = layout.planAt(7, c);
                    assertNotNull(plan.roadX(), "seed " + seed + ": subway row " + c + " is a road along X");
                    assertEquals(RoadClass.STREET, plan.roadX().roadClass());
                }
            }
            assertFalse(railColumns.isEmpty() && subwayRows.isEmpty(), "seed " + seed + " has some rail");
            for (int i = 1; i < railColumns.size(); i++) {
                assertTrue(railColumns.get(i) - railColumns.get(i - 1) >= TransitLayout.MIN_LINE_SPACING, "seed " + seed);
            }
            for (int column : railColumns) {
                int last = Integer.MIN_VALUE / 2;
                for (int z = -40; z <= 40; z++) {
                    if (transit.railStation(column, z)) {
                        assertTrue(layout.planAt(column, z).streetX(), "seed " + seed + ": stations are at intersections");
                        assertTrue(z - last >= TransitLayout.MIN_STATION_SPACING, "seed " + seed + ": stations are spaced");
                        last = z;
                    }
                }
                assertTrue(last > Integer.MIN_VALUE / 2, "seed " + seed + ": rail column " + column + " has a station");
            }
        }
    }

    @Test
    void skywaysOnlyCrossSingleRoadsBetweenBlocks() {
        int skyways = 0;
        for (long seed = 0; seed < 6; seed++) {
            GridLayout layout = new GridLayout(with(seed, Set.of(GridLevel.SKYWAYS)));
            for (int x = -35; x <= 35; x++) {
                for (int z = -35; z <= 35; z++) {
                    TransitLayout.Skyway skyway = layout.transit().skywayAt(x, z);
                    if (skyway == null) {
                        continue;
                    }
                    skyways++;
                    ChunkPlan plan = layout.planAt(x, z);
                    assertNotEquals(plan.streetX(), plan.streetZ(), "a single road");
                    assertEquals(skyway.roadAlongX(), plan.streetX());
                    ChunkPlan side = skyway.roadAlongX() ? layout.planAt(x, z + 1) : layout.planAt(x + 1, z);
                    assertNotEquals(PlanType.STREET, side.type(), "the bridge lands on a block, not a road");
                    assertTrue(skyway.standY() >= 101 && (skyway.standY() - 65) % GridLayout.FLOOR_HEIGHT == 0);
                }
            }
        }
        assertTrue(skyways > 0);
    }

    @Test
    void noLevelsMeansNothingAndOldGridsKeepTheirIds() {
        GridLayout layout = new GridLayout(with(0, Set.of()));
        for (int x = -400; x < 400; x += 7) {
            assertEquals(-1, layout.transit().deckLevel(x, 0));
            assertEquals(-1, layout.transit().rampLevel(x, -5));
        }
        GridLayoutSettings none = with(42, Set.of());
        assertFalse(GridLayoutSettings.CODEC.encodeStart(JsonOps.INSTANCE, none).getOrThrow().getAsJsonObject().has("levels"));
        assertNotEquals(GridKey.of(none), GridKey.of(with(42, Set.of(GridLevel.SUBWAY))));
    }
}
