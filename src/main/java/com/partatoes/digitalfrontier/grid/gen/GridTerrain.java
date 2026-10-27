package com.partatoes.digitalfrontier.grid.gen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.DiagonalRoad;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import com.partatoes.digitalfrontier.grid.layout.PlanType;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.Road;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.DyeColor;

import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_HEIGHT;
import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_Y;

/**
 * The ground of the grid, and the placeholder buildings used where no building template applies. Everything is
 * derived from the chunk's {@link ChunkPlan} and block coordinates, so chunk generation and column sampling agree.
 *
 * <ul>
 *   <li>Most of the grid is solid gridstone up to the street surface at {@link GridLayout#FLOOR_Y}.</li>
 *   <li>The outlands rise into terraced mesas, leveling off beside highways and around sites.</li>
 *   <li>Unallocated space breaks up into floating fragments that thin out into the void.</li>
 * </ul>
 */
final class GridTerrain {
    /** The bottom of the grid dimension type. */
    static final int WORLD_BOTTOM = -64;
    /** The most ground can rise above {@link GridLayout#FLOOR_Y}: outlands mesas, raised fragments. */
    static final int MAX_GROUND_RISE = 40;

    // Placeholder buildings leave a 1-block gap around the chunk edge so neighbours read as separate towers
    private static final int FOOTPRINT_MIN = 1;
    private static final int FOOTPRINT_MAX = 14;

    // A lane's 6-block roadway, across the chunk; one block of sidewalk either side, then the wall strips
    private static final int LANE_FIRST = 5;
    private static final int LANE_LAST = 10;

    private static final int MESA_HEIGHT = 36;
    private static final int TERRACE_STEP = 3;
    private static final int FRAGMENT_SIZE = 4;
    /** Chunks past the outlands edge after which unallocated space is empty. */
    private static final int FRAGMENT_FADE_CHUNKS = 24;

    private static final int SALT_MESA_LARGE = 3001;
    private static final int SALT_MESA_SMALL = 3002;
    private static final int SALT_FRAGMENT = 3003;
    private static final int SALT_FRAGMENT_HEIGHT = 3004;
    private static final int SALT_FRAGMENT_THICKNESS = 3005;

    private GridTerrain() {
    }

    /**
     * One column of ground: blocks from {@code bottom} to {@code top} inclusive, {@code surface} on top and
     * {@code body} below it. Empty (void) when {@code top < bottom}.
     */
    record Column(int bottom, int top, BlockState surface, BlockState body) {
        static final Column VOID = new Column(WORLD_BOTTOM, WORLD_BOTTOM - 1, Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState());

        boolean isEmpty() {
            return top < bottom;
        }
    }

    static Column column(GridLayout layout, ChunkPlan plan, int blockX, int blockZ) {
        if (plan.zone() == Zone.UNALLOCATED) {
            return fragment(layout.settings(), blockX, blockZ);
        }
        int localX = blockX & 15;
        int localZ = blockZ & 15;
        BlockState surface = plan.diagonal() != null && plan.diagonal().contains(blockX, blockZ)
                ? diagonalSurface(plan, plan.diagonal(), blockX, blockZ)
                : surface(plan, localX, localZ);
        int rise = mesaHeight(layout, plan, blockX, blockZ);
        if (rise > 0) {
            surface = ModBlocks.CRACKED_GRIDSTONE_BLOCK.getDefaultState();
        }
        return new Column(WORLD_BOTTOM, FLOOR_Y + rise, surface, ModBlocks.GRIDSTONE_BLOCK.getDefaultState());
    }

    /** Highest Y with a block in this column, including placeholder buildings and lane walls. */
    static int columnTop(ChunkPlan plan, Column column, int localX, int localZ) {
        if (plan.type() == PlanType.BUILDING && inFootprint(localX, localZ)) {
            return FLOOR_Y + plan.floors() * FLOOR_HEIGHT;
        }
        if (plan.type() == PlanType.STREET && isLaneWall(plan, localX, localZ)) {
            return FLOOR_Y + 1;
        }
        return column.top();
    }

    static BlockState blockAt(ChunkPlan plan, Column column, int localX, int y, int localZ) {
        if (y < column.bottom()) {
            return Blocks.AIR.getDefaultState();
        }
        if (y < column.top()) {
            return column.body();
        }
        if (y == column.top()) {
            return column.surface();
        }
        if (plan.type() == PlanType.STREET) {
            return y == FLOOR_Y + 1 && isLaneWall(plan, localX, localZ)
                    ? ModBlocks.GRIDSTONE_BRICKS_WALL.getDefaultState()
                    : Blocks.AIR.getDefaultState();
        }
        if (y <= columnTop(plan, column, localX, localZ)) {
            // A lit band marks the top of every floor
            return (y - FLOOR_Y) % FLOOR_HEIGHT == 0
                    ? pattern("full", plan.district())
                    : ModBlocks.GRIDSTONE_BRICKS_BLOCK.getDefaultState();
        }
        return Blocks.AIR.getDefaultState();
    }

    private static BlockState surface(ChunkPlan plan, int localX, int localZ) {
        return switch (plan.type()) {
            case STREET -> {
                if (isStreetLight(plan, localX, localZ)) {
                    yield pattern("full", plan.district());
                }
                // Outside every road's pavement: a lane's walled strips
                boolean paved = isPaved(plan.roadX(), localZ) || isPaved(plan.roadZ(), localX);
                yield paved ? ModBlocks.GRIDSTONE_BLOCK.getDefaultState() : ModBlocks.GRIDSTONE_BRICKS_BLOCK.getDefaultState();
            }
            case BUILDING -> ModBlocks.GRIDSTONE_BRICKS_BLOCK.getDefaultState();
            case PLAZA -> isCenter(localX, localZ, 1)
                    ? pattern("thin_target", plan.district())
                    : ModBlocks.GRIDSTONE_BRICKS_BLOCK.getDefaultState();
            case SITE -> isCenter(localX, localZ, 2)
                    ? pattern("thick_target", siteColor(plan.site()))
                    : ModBlocks.GRIDSTONE_BRICKS_BLOCK.getDefaultState();
            case OPEN -> plan.zone() == Zone.OUTLANDS
                    ? ModBlocks.CRACKED_GRIDSTONE_BLOCK.getDefaultState()
                    : ModBlocks.GRIDSTONE_BLOCK.getDefaultState();
        };
    }

    // Light lines along both edges, two blocks wide so the stair-stepped diagonal reads as one line
    private static BlockState diagonalSurface(ChunkPlan plan, DiagonalRoad road, int blockX, int blockZ) {
        int across = Math.abs(road.offset(blockX, blockZ));
        return across >= DiagonalRoad.HALF_WIDTH - 1
                ? pattern("full", plan.district())
                : ModBlocks.GRIDSTONE_BLOCK.getDefaultState();
    }

    /**
     * Terraced mesas across the open outlands: two octaves of value noise, cut off below a threshold so plenty of
     * flat ground remains, and stepped. They fade in past the outskirts, out toward unallocated space, and away
     * near highways and sites, so roads and the Portal stay reachable.
     */
    private static int mesaHeight(GridLayout layout, ChunkPlan plan, int blockX, int blockZ) {
        if (plan.zone() != Zone.OUTLANDS || plan.type() != PlanType.OPEN) {
            return 0;
        }
        GridLayoutSettings settings = layout.settings();
        double chunkDistance = Math.hypot(blockX + 0.5, blockZ + 0.5) / 16;
        double amplitude = ramp(chunkDistance, settings.outskirtsRadius(), settings.outskirtsRadius() + 8)
                * ramp(settings.outlandsRadius() - chunkDistance, 0, 6)
                * ramp(layout.highwayDistance(blockX, blockZ), 1, 12);
        for (Site site : layout.sites()) {
            amplitude *= ramp(Math.hypot(blockX - (site.chunkX() * 16 + 7.5), blockZ - (site.chunkZ() * 16 + 7.5)), 20, 56);
        }
        if (amplitude <= 0) {
            return 0;
        }
        long seed = settings.gridSeed();
        double noise = 0.65 * valueNoise(seed, blockX, blockZ, 48, SALT_MESA_LARGE)
                + 0.35 * valueNoise(seed, blockX, blockZ, 16, SALT_MESA_SMALL);
        double ridge = Math.clamp((noise - 0.45) / 0.55, 0.0, 1.0);
        int height = (int) (Math.pow(ridge, 1.2) * MESA_HEIGHT * amplitude);
        return height / TERRACE_STEP * TERRACE_STEP;
    }

    /**
     * Unallocated space: 4×4 tiles of floor that go missing more often the further out they are, and drift up or
     * down a little, until nothing is left.
     */
    private static Column fragment(GridLayoutSettings settings, int blockX, int blockZ) {
        double past = Math.hypot(blockX + 0.5, blockZ + 0.5) / 16 - settings.outlandsRadius();
        int tileX = Math.floorDiv(blockX, FRAGMENT_SIZE);
        int tileZ = Math.floorDiv(blockZ, FRAGMENT_SIZE);
        long seed = settings.gridSeed();
        if (GridRandom.unit(GridRandom.hash(seed, tileX, tileZ, SALT_FRAGMENT)) < 0.08 + past / FRAGMENT_FADE_CHUNKS) {
            return Column.VOID;
        }
        double drift = Math.min(1.0, past / 8);
        int top = FLOOR_Y + (int) Math.round((GridRandom.unit(GridRandom.hash(seed, tileX, tileZ, SALT_FRAGMENT_HEIGHT)) - 0.5) * 6 * drift);
        int thickness = 1 + GridRandom.nextInt(GridRandom.hash(seed, tileX, tileZ, SALT_FRAGMENT_THICKNESS), 3);
        return new Column(top - thickness + 1, top, ModBlocks.GRIDSTONE_BLOCK.getDefaultState(),
                ModBlocks.CRACKED_GRIDSTONE_BLOCK.getDefaultState());
    }

    // Smoothly interpolated random values on a lattice of the given cell size
    private static double valueNoise(long seed, int blockX, int blockZ, int cell, int salt) {
        int cellX = Math.floorDiv(blockX, cell);
        int cellZ = Math.floorDiv(blockZ, cell);
        double fx = smooth((blockX - cellX * cell) / (double) cell);
        double fz = smooth((blockZ - cellZ * cell) / (double) cell);
        double a = GridRandom.unit(GridRandom.hash(seed, cellX, cellZ, salt));
        double b = GridRandom.unit(GridRandom.hash(seed, cellX + 1, cellZ, salt));
        double c = GridRandom.unit(GridRandom.hash(seed, cellX, cellZ + 1, salt));
        double d = GridRandom.unit(GridRandom.hash(seed, cellX + 1, cellZ + 1, salt));
        double top = a + (b - a) * fx;
        double bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    /** 0 at or below {@code from}, 1 at or above {@code to}, linear between. */
    private static double ramp(double value, double from, double to) {
        return Math.clamp((value - from) / (to - from), 0.0, 1.0);
    }

    // A road's light lines, by where a block sits across it (0-15); roads crossing at an intersection both draw theirs
    private static boolean isStreetLight(ChunkPlan plan, int localX, int localZ) {
        return isLightLine(plan.roadX(), localZ) || isLightLine(plan.roadZ(), localX);
    }

    private static boolean isLightLine(@Nullable Road road, int across) {
        if (road == null) {
            return false;
        }
        return switch (road.roadClass()) {
            // Both edges; the street through the city center also gets a double center line
            case STREET -> across == 1 || across == 14 || (road.central() && (across == 7 || across == 8));
            case LANE -> across == LANE_FIRST || across == LANE_LAST;
            // The outer edge, and the median where the two halves meet
            case BOULEVARD -> road.half() == 0 ? across == 1 || across == 15 : across == 0 || across == 14;
        };
    }

    /**
     * The placeholder walls of a lane chunk: 1-high planter walls along both outer strips, with a gap every 8 blocks
     * so the buildings behind stay reachable. Where another road crosses, its pavement wins.
     */
    static boolean isLaneWall(ChunkPlan plan, int localX, int localZ) {
        boolean wall = isLaneStrip(plan.roadX(), localZ, localX) || isLaneStrip(plan.roadZ(), localX, localZ);
        return wall && !isPaved(plan.roadX(), localZ) && !isPaved(plan.roadZ(), localX);
    }

    private static boolean isLaneStrip(@Nullable Road road, int across, int along) {
        return road != null && road.roadClass() == RoadClass.LANE
                && (across < LANE_FIRST - 1 || across > LANE_LAST + 1) && along % 8 != 3 && along % 8 != 4;
    }

    // Lanes are paved from sidewalk to sidewalk; streets and boulevards all the way across
    private static boolean isPaved(@Nullable Road road, int across) {
        if (road == null) {
            return false;
        }
        return road.roadClass() != RoadClass.LANE || (across >= LANE_FIRST - 1 && across <= LANE_LAST + 1);
    }

    private static boolean isCenter(int localX, int localZ, int halfWidth) {
        return localX >= 8 - halfWidth && localX < 8 + halfWidth && localZ >= 8 - halfWidth && localZ < 8 + halfWidth;
    }

    private static boolean inFootprint(int localX, int localZ) {
        return localX >= FOOTPRINT_MIN && localX <= FOOTPRINT_MAX && localZ >= FOOTPRINT_MIN && localZ <= FOOTPRINT_MAX;
    }

    private static DyeColor siteColor(Site site) {
        return switch (site.type()) {
            case ARRIVAL_TERMINAL -> DyeColor.WHITE;
            case WAYPOINT -> DyeColor.LIGHT_BLUE;
            case STRONGHOLD -> DyeColor.RED;
            case PORTAL -> DyeColor.LIME;
        };
    }

    private static BlockState pattern(String name, DyeColor color) {
        return ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get(name).get(color).getDefaultState();
    }
}
