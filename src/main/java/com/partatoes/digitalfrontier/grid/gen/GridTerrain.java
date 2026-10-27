package com.partatoes.digitalfrontier.grid.gen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.PlanType;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.DyeColor;

import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_HEIGHT;
import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_Y;

/**
 * Phase 1 placeholder terrain: the block at any position of a chunk, derived only from its {@link ChunkPlan}.
 * Shared by chunk generation and column sampling so the two can never disagree.
 */
final class GridTerrain {
    // Placeholder buildings leave a 1-block gap around the chunk edge so neighbours read as separate towers
    private static final int FOOTPRINT_MIN = 1;
    private static final int FOOTPRINT_MAX = 14;

    private GridTerrain() {
    }

    /** Lowest Y with a block in this column. Unallocated space is only a thin floor. */
    static int columnBottom(ChunkPlan plan, int worldBottomY) {
        return plan.zone() == Zone.UNALLOCATED ? FLOOR_Y : worldBottomY;
    }

    /** Highest Y with a block in this column. */
    static int columnTop(ChunkPlan plan, int localX, int localZ) {
        if (plan.type() == PlanType.BUILDING && inFootprint(localX, localZ)) {
            return FLOOR_Y + plan.floors() * FLOOR_HEIGHT;
        }
        return FLOOR_Y;
    }

    static BlockState blockAt(ChunkPlan plan, int localX, int y, int localZ) {
        if (y < FLOOR_Y) {
            return ModBlocks.GRIDSTONE_BLOCK.getDefaultState();
        }
        if (y == FLOOR_Y) {
            return surface(plan, localX, localZ);
        }
        if (y <= columnTop(plan, localX, localZ)) {
            // A lit band marks the top of every floor
            return (y - FLOOR_Y) % FLOOR_HEIGHT == 0
                    ? pattern("full", plan.district())
                    : ModBlocks.GRIDSTONE_BRICKS_BLOCK.getDefaultState();
        }
        return Blocks.AIR.getDefaultState();
    }

    private static BlockState surface(ChunkPlan plan, int localX, int localZ) {
        return switch (plan.type()) {
            case STREET -> isStreetLight(plan, localX, localZ)
                    ? pattern("full", plan.district())
                    : ModBlocks.GRIDSTONE_BLOCK.getDefaultState();
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

    // Light lines along both edges of each street; avenues also get a double center line
    private static boolean isStreetLight(ChunkPlan plan, int localX, int localZ) {
        if (plan.streetX() && isLightLine(localZ, plan.avenue())) {
            return true;
        }
        return plan.streetZ() && isLightLine(localX, plan.avenue());
    }

    private static boolean isLightLine(int across, boolean avenue) {
        return across == 1 || across == 14 || (avenue && (across == 7 || across == 8));
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
