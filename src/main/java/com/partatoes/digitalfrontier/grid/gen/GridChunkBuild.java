package com.partatoes.digitalfrontier.grid.gen;

import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import com.partatoes.digitalfrontier.grid.layout.MultiChunk;
import com.partatoes.digitalfrontier.grid.layout.PlanType;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.parts.DistrictPalette;
import com.partatoes.digitalfrontier.grid.parts.GridAssets;
import com.partatoes.digitalfrontier.grid.parts.GridBuildingDef;
import com.partatoes.digitalfrontier.grid.parts.GridPart;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_Y;

/**
 * Everything needed to produce one chunk's blocks: its plan plus, when a matching building definition exists,
 * the stack of parts placed on it. Without a definition the chunk falls back to {@link GridTerrain}'s placeholders.
 * Resolution only reads the plan and the asset snapshot, so every chunk of a 2×2 building resolves identically.
 */
final class GridChunkBuild {
    private static final int MAX_Y = 319;
    private static final double STREET_FURNITURE_CHANCE = 0.35;
    private static final BlockRotation[] ROTATIONS = BlockRotation.values();

    private final GridLayout layout;
    private final ChunkPlan plan;
    private final int chunkX;
    private final int chunkZ;
    @Nullable
    private final Identifier buildingId;
    private final List<Layer> layers;
    private final int footprintX;
    private final int footprintZ;
    private final int footprintSize;
    private final BlockRotation rotation;
    // Ground columns, worked out on first use: a height lookup only needs one of them
    private final GridTerrain.Column[] columns = new GridTerrain.Column[256];

    private record Layer(GridPart part, int bottomY) {
        boolean contains(int y) {
            return y >= bottomY && y < bottomY + part.sizeY();
        }
    }

    private GridChunkBuild(GridLayout layout, ChunkPlan plan, int chunkX, int chunkZ, @Nullable Identifier buildingId, List<Layer> layers,
                           int footprintChunkX, int footprintChunkZ, int footprintChunks, BlockRotation rotation) {
        this.layout = layout;
        this.plan = plan;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.buildingId = buildingId;
        this.layers = layers;
        this.footprintX = footprintChunkX * 16;
        this.footprintZ = footprintChunkZ * 16;
        this.footprintSize = footprintChunks * 16;
        this.rotation = rotation;
    }

    static GridChunkBuild resolve(GridLayout layout, GridAssets assets, int chunkX, int chunkZ) {
        ChunkPlan plan = layout.planAt(chunkX, chunkZ);
        long seed = layout.settings().gridSeed();
        MultiChunk multi = plan.multiChunk();

        if (plan.type() == PlanType.BUILDING && multi != null) {
            GridBuildingDef.Kind kind = multi.landmark() ? GridBuildingDef.Kind.LANDMARK : GridBuildingDef.Kind.BUILDING;
            GridAssets.Candidate candidate = assets.pick(kind, MultiChunk.SIZE, plan.zone(), roll(seed, multi.originX(), multi.originZ(), 0));
            if (candidate == null && multi.landmark()) {
                candidate = assets.pick(GridBuildingDef.Kind.BUILDING, MultiChunk.SIZE, plan.zone(), roll(seed, multi.originX(), multi.originZ(), 0));
            }
            if (candidate != null) {
                return stacked(layout, plan, chunkX, chunkZ, assets, candidate, multi.originX(), multi.originZ(), MultiChunk.SIZE, seed);
            }
            // No 2×2 buildings available: fall through and treat this quarter as an ordinary building chunk
        }

        GridBuildingDef.Kind kind = switch (plan.type()) {
            case BUILDING -> GridBuildingDef.Kind.BUILDING;
            case PLAZA -> GridBuildingDef.Kind.PLAZA;
            case STREET -> plan.streetX() != plan.streetZ() && roll(seed, chunkX, chunkZ, 1) < STREET_FURNITURE_CHANCE
                    ? GridBuildingDef.Kind.STREET : null;
            case SITE -> siteKind(plan.site());
            case OPEN -> null;
        };
        GridAssets.Candidate candidate = kind == null ? null : assets.pick(kind, 1, plan.zone(), roll(seed, chunkX, chunkZ, 0));
        if (candidate == null) {
            return new GridChunkBuild(layout, plan, chunkX, chunkZ, null, List.of(), chunkX, chunkZ, 1, BlockRotation.NONE);
        }
        return stacked(layout, plan, chunkX, chunkZ, assets, candidate, chunkX, chunkZ, 1, seed);
    }

    @Nullable
    private static GridBuildingDef.Kind siteKind(@Nullable Site site) {
        if (site == null) {
            return null;
        }
        return switch (site.type()) {
            case ARRIVAL_TERMINAL, WAYPOINT -> GridBuildingDef.Kind.TERMINAL;
            case PORTAL -> GridBuildingDef.Kind.PORTAL;
            // Strongholds get their buildings with corruption (phase 6)
            case STRONGHOLD -> null;
        };
    }

    private static GridChunkBuild stacked(GridLayout layout, ChunkPlan plan, int chunkX, int chunkZ, GridAssets assets, GridAssets.Candidate candidate,
                                          int originX, int originZ, int footprintChunks, long seed) {
        GridBuildingDef def = candidate.def();
        List<Layer> layers = new ArrayList<>();
        int y = FLOOR_Y + 1;
        y = addLayer(layers, assets, def.base().orElse(null), y);

        int floorCount = def.floors().isEmpty() ? 0 : Math.clamp(plan.floors(), def.minFloors(), def.maxFloors());
        int reservedForRoof = def.roof().map(assets::part).map(GridPart::sizeY).orElse(0);
        for (int floor = 0; floor < floorCount; floor++) {
            Identifier partId = def.floors().get((int) (roll(seed, originX, originZ, 10 + floor) * def.floors().size()));
            GridPart part = assets.part(partId);
            if (part == null || y + part.sizeY() + reservedForRoof - 1 > MAX_Y) {
                break;
            }
            y = addLayer(layers, assets, partId, y);
        }
        addLayer(layers, assets, def.roof().orElse(null), y);

        // Street furniture is authored for streets running along X; turn it for streets along Z
        BlockRotation rotation = def.kind() == GridBuildingDef.Kind.STREET
                ? (plan.streetX() ? BlockRotation.NONE : BlockRotation.CLOCKWISE_90)
                : ROTATIONS[(int) (roll(seed, originX, originZ, 2) * ROTATIONS.length)];
        return new GridChunkBuild(layout, plan, chunkX, chunkZ, candidate.id(), List.copyOf(layers), originX, originZ, footprintChunks, rotation);
    }

    private static int addLayer(List<Layer> layers, GridAssets assets, @Nullable Identifier partId, int bottomY) {
        GridPart part = partId == null ? null : assets.part(partId);
        if (part == null || bottomY + part.sizeY() - 1 > MAX_Y) {
            return bottomY;
        }
        layers.add(new Layer(part, bottomY));
        return bottomY + part.sizeY();
    }

    private static double roll(long seed, int x, int z, int salt) {
        // Salted well apart from the layout's own salts
        return GridRandom.unit(GridRandom.hash(seed, x, z, 1000 + salt));
    }

    ChunkPlan plan() {
        return plan;
    }

    @Nullable
    Identifier buildingId() {
        return buildingId;
    }

    /** Highest Y any block of this chunk can occupy. */
    int maxY() {
        if (layers.isEmpty()) {
            return FLOOR_Y + Math.max(plan.floors() * GridLayout.FLOOR_HEIGHT, GridTerrain.MAX_GROUND_RISE);
        }
        Layer top = layers.getLast();
        return top.bottomY() + top.part().sizeY() - 1;
    }

    /** Lowest Y with a block in a column. Above the world bottom only in unallocated space. */
    int columnBottom(int localX, int localZ) {
        return column(localX, localZ).bottom();
    }

    /** Highest non-air Y in a column, or the ground if nothing stands on it (below the world bottom for void). */
    int columnTop(int localX, int localZ) {
        if (layers.isEmpty()) {
            return GridTerrain.columnTop(plan, column(localX, localZ), localX, localZ);
        }
        for (int y = maxY(); y > FLOOR_Y; y--) {
            if (!blockAt(localX, y, localZ).isAir()) {
                return y;
            }
        }
        return column(localX, localZ).top();
    }

    private GridTerrain.Column column(int localX, int localZ) {
        int index = localZ * 16 + localX;
        GridTerrain.Column column = columns[index];
        if (column == null) {
            column = GridTerrain.column(layout, plan, chunkX * 16 + localX, chunkZ * 16 + localZ);
            columns[index] = column;
        }
        return column;
    }

    /** World position of the first Arrival Pad in this chunk's stack, or null if its parts have none. */
    @Nullable
    BlockPos arrivalPad() {
        int last = footprintSize - 1;
        for (Layer layer : layers) {
            BlockPos pad = layer.part().arrivalPad();
            if (pad == null) {
                continue;
            }
            // The forward version of the rotation partBlock undoes
            int px = pad.getX();
            int pz = pad.getZ();
            int fx;
            int fz;
            switch (rotation) {
                case CLOCKWISE_90 -> { fx = last - pz; fz = px; }
                case CLOCKWISE_180 -> { fx = last - px; fz = last - pz; }
                case COUNTERCLOCKWISE_90 -> { fx = pz; fz = last - px; }
                default -> { fx = px; fz = pz; }
            }
            return new BlockPos(footprintX + fx, layer.bottomY() + pad.getY(), footprintZ + fz);
        }
        return null;
    }

    BlockState blockAt(int localX, int y, int localZ) {
        if (layers.isEmpty() || y <= FLOOR_Y) {
            return GridTerrain.blockAt(plan, column(localX, localZ), localX, y, localZ);
        }
        for (Layer layer : layers) {
            if (layer.contains(y)) {
                return partBlock(layer, localX, y, localZ);
            }
        }
        return Blocks.AIR.getDefaultState();
    }

    private BlockState partBlock(Layer layer, int localX, int y, int localZ) {
        // Position within the (possibly 2×2) footprint, then undo the rotation to find the template position
        int fx = chunkX * 16 + localX - footprintX;
        int fz = chunkZ * 16 + localZ - footprintZ;
        int last = footprintSize - 1;
        int px;
        int pz;
        switch (rotation) {
            case CLOCKWISE_90 -> { px = fz; pz = last - fx; }
            case CLOCKWISE_180 -> { px = last - fx; pz = last - fz; }
            case COUNTERCLOCKWISE_90 -> { px = last - fz; pz = fx; }
            default -> { px = fx; pz = fz; }
        }
        BlockState state = layer.part().get(px, y - layer.bottomY(), pz);
        if (state == null) {
            return Blocks.AIR.getDefaultState();
        }
        return DistrictPalette.remap(state, plan.district()).rotate(rotation);
    }
}
