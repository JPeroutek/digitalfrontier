package com.partatoes.digitalfrontier.grid.gen;

import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import com.partatoes.digitalfrontier.grid.layout.MultiChunk;
import com.partatoes.digitalfrontier.grid.layout.PlanType;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.Road;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import com.partatoes.digitalfrontier.grid.layout.TransitLayout;
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
    private final GridAssets assets;
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
    @Nullable
    private float[] corruptionGrid;
    @Nullable
    private GridTransit.ChunkTransit transit;

    private record Layer(GridPart part, int bottomY) {
        boolean contains(int y) {
            return y >= bottomY && y < bottomY + part.sizeY();
        }
    }

    private GridChunkBuild(GridLayout layout, GridAssets assets, ChunkPlan plan, int chunkX, int chunkZ, @Nullable Identifier buildingId, List<Layer> layers,
                           int footprintChunkX, int footprintChunkZ, int footprintChunks, BlockRotation rotation) {
        this.layout = layout;
        this.assets = assets;
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

        GridAssets.Candidate candidate;
        if (plan.type() == PlanType.STREET) {
            candidate = streetFurniture(plan, assets, seed, chunkX, chunkZ);
        } else {
            GridBuildingDef.Kind kind = switch (plan.type()) {
                case BUILDING -> GridBuildingDef.Kind.BUILDING;
                case PLAZA -> GridBuildingDef.Kind.PLAZA;
                case SITE -> siteKind(plan.site());
                case STREET, OPEN -> null;
            };
            candidate = kind == null ? null : assets.pick(kind, 1, plan.zone(), roll(seed, chunkX, chunkZ, 0));
        }
        if (candidate == null) {
            return new GridChunkBuild(layout, assets, plan, chunkX, chunkZ, null, List.of(), chunkX, chunkZ, 1, BlockRotation.NONE);
        }
        return stacked(layout, plan, chunkX, chunkZ, assets, candidate, chunkX, chunkZ, 1, seed);
    }

    // Furniture goes on straight roads, never intersections. Lanes always get theirs (their walls and planters are
    // what make them lanes); streets and boulevards only sometimes.
    @Nullable
    private static GridAssets.Candidate streetFurniture(ChunkPlan plan, GridAssets assets, long seed, int chunkX, int chunkZ) {
        if (plan.streetX() == plan.streetZ()) {
            return null;
        }
        Road road = plan.streetX() ? plan.roadX() : plan.roadZ();
        double chance = road.roadClass() == RoadClass.LANE ? 1.0 : STREET_FURNITURE_CHANCE;
        if (roll(seed, chunkX, chunkZ, 1) >= chance) {
            return null;
        }
        return assets.pickStreet(road.roadClass(), plan.zone(), roll(seed, chunkX, chunkZ, 0));
    }

    /**
     * Street furniture is authored for a road along X, with a boulevard's outer edge at Z = 0 and its median at
     * Z = 15. Turn it for roads along Z, and so each boulevard half faces outward.
     */
    private static BlockRotation streetRotation(ChunkPlan plan) {
        boolean alongX = plan.streetX();
        Road road = alongX ? plan.roadX() : plan.roadZ();
        if (road.roadClass() == RoadClass.BOULEVARD) {
            if (alongX) {
                return road.half() == 0 ? BlockRotation.NONE : BlockRotation.CLOCKWISE_180;
            }
            return road.half() == 0 ? BlockRotation.COUNTERCLOCKWISE_90 : BlockRotation.CLOCKWISE_90;
        }
        return alongX ? BlockRotation.NONE : BlockRotation.CLOCKWISE_90;
    }

    @Nullable
    private static GridBuildingDef.Kind siteKind(@Nullable Site site) {
        if (site == null) {
            return null;
        }
        return switch (site.type()) {
            case ARRIVAL_TERMINAL, WAYPOINT -> GridBuildingDef.Kind.TERMINAL;
            case PORTAL -> GridBuildingDef.Kind.PORTAL;
            case STRONGHOLD -> GridBuildingDef.Kind.STRONGHOLD;
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

        BlockRotation rotation = def.kind() == GridBuildingDef.Kind.STREET
                ? streetRotation(plan)
                : ROTATIONS[(int) (roll(seed, originX, originZ, 2) * ROTATIONS.length)];
        return new GridChunkBuild(layout, assets, plan, chunkX, chunkZ, candidate.id(), List.copyOf(layers), originX, originZ, footprintChunks, rotation);
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
        // Corruption can push columns up a few blocks past the top
        return Math.min(MAX_Y, uncorruptedMaxY() + GridCorruption.MAX_SHIFT);
    }

    private int uncorruptedMaxY() {
        GridTransit.ChunkTransit levels = transit();
        return levels.any() ? Math.max(structureTop(), levels.maxY()) : structureTop();
    }

    // The ground and building alone, without the city's levels; what a skyway needs to know about a tower
    private int structureTop() {
        if (layers.isEmpty()) {
            return FLOOR_Y + Math.max(plan.floors() * GridLayout.FLOOR_HEIGHT, GridTerrain.MAX_GROUND_RISE);
        }
        Layer top = layers.getLast();
        return top.bottomY() + top.part().sizeY() - 1;
    }

    private GridTransit.ChunkTransit transit() {
        if (transit == null) {
            transit = GridTransit.forChunk(layout.transit(), assets, layout.settings().gridSeed(), plan, chunkX, chunkZ, this::skywayFits);
        }
        return transit;
    }

    // A skyway only goes up where there are buildings on both sides of the road, tall enough to meet it
    private boolean skywayFits(TransitLayout.Skyway skyway, int roadChunkX, int roadChunkZ) {
        int[][] sides = skyway.roadAlongX()
                ? new int[][]{{roadChunkX, roadChunkZ - 1}, {roadChunkX, roadChunkZ + 1}}
                : new int[][]{{roadChunkX - 1, roadChunkZ}, {roadChunkX + 1, roadChunkZ}};
        for (int[] side : sides) {
            GridChunkBuild building = side[0] == chunkX && side[1] == chunkZ ? this : resolve(layout, assets, side[0], side[1]);
            if (building.plan.type() != PlanType.BUILDING || building.structureTop() < skyway.standY() + 3) {
                return false;
            }
        }
        return true;
    }

    /** What the city's levels put in this chunk, for the debug screen. */
    String describeLevels() {
        GridTransit.ChunkTransit levels = transit();
        List<String> parts = new ArrayList<>();
        if (levels.deckChunk()) {
            parts.add("skyroad");
        }
        if (levels.elevatedRail()) {
            parts.add(levels.railStation() ? "elevated rail station" : "elevated rail");
        }
        if (levels.subway()) {
            parts.add(levels.subwayStation() ? "subway station" : "subway");
        }
        if (levels.skyway() != null) {
            parts.add("skyway at Y " + levels.skyway().standY());
        }
        levels.doors().forEach(door -> parts.add("skyway door at Y " + door.standY()));
        return String.join(", ", parts);
    }

    /** Lowest Y with a block in a column. Above the world bottom only in unallocated space. */
    int columnBottom(int localX, int localZ) {
        return column(localX, localZ).bottom();
    }

    /** Highest non-air Y in a column, or the ground if nothing stands on it (below the world bottom for void). */
    int columnTop(int localX, int localZ) {
        if (layers.isEmpty() && !transit().any()) {
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
        if (!corrupted()) {
            return uncorruptedBlockAt(localX, y, localZ);
        }
        GridTransit.ChunkTransit levels = transit();
        boolean level = levels.any() && GridTransit.blockAt(levels, localX, y, localZ) != null;
        return GridCorruption.apply(layout.settings().gridSeed(), corruptionAt(localX, localZ), plan.type() != PlanType.SITE, level,
                uncorruptedMaxY(), chunkX * 16 + localX, y, chunkZ * 16 + localZ, sourceY -> uncorruptedBlockAt(localX, sourceY, localZ));
    }

    // Corruption intensity of the 3×3 chunks around this one, so it can be blended smoothly across chunk borders
    private float[] corruptionGrid() {
        if (corruptionGrid == null) {
            float[] grid = new float[9];
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    grid[(dz + 1) * 3 + dx + 1] = layout.corruptionAt(chunkX + dx, chunkZ + dz);
                }
            }
            corruptionGrid = grid;
        }
        return corruptionGrid;
    }

    private boolean corrupted() {
        for (float intensity : corruptionGrid()) {
            if (intensity >= GridCorruption.MIN_INTENSITY) {
                return true;
            }
        }
        return false;
    }

    // Bilinear between the centers of the four nearest chunks
    private float corruptionAt(int localX, int localZ) {
        float[] grid = corruptionGrid();
        float gx = (localX + 0.5f) / 16 - 0.5f;
        float gz = (localZ + 0.5f) / 16 - 0.5f;
        int x0 = gx < 0 ? -1 : 0;
        int z0 = gz < 0 ? -1 : 0;
        float tx = gx - x0;
        float tz = gz - z0;
        float a = grid[(z0 + 1) * 3 + x0 + 1];
        float b = grid[(z0 + 1) * 3 + x0 + 2];
        float c = grid[(z0 + 2) * 3 + x0 + 1];
        float d = grid[(z0 + 2) * 3 + x0 + 2];
        return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz;
    }

    private BlockState uncorruptedBlockAt(int localX, int y, int localZ) {
        // The city's levels win wherever they put something
        GridTransit.ChunkTransit levels = transit();
        if (levels.any()) {
            BlockState level = GridTransit.blockAt(levels, localX, y, localZ);
            if (level != null) {
                return level;
            }
        }
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
