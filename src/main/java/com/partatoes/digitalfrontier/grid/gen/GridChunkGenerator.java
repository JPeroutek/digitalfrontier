package com.partatoes.digitalfrontier.grid.gen;

import com.mojang.serialization.MapCodec;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.CorruptionTier;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import com.partatoes.digitalfrontier.grid.layout.PlanType;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import com.partatoes.digitalfrontier.grid.parts.GridAssets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generates a grid chunk by chunk from its {@link GridLayout}. Each chunk only ever writes its own blocks,
 * so generation order doesn't matter.
 */
public class GridChunkGenerator extends ChunkGenerator {
    // The generator's settings live on its biome source, so the dimension JSON holds them only once
    public static final MapCodec<GridChunkGenerator> CODEC = GridBiomeSource.CODEC.xmap(GridChunkGenerator::new, GridChunkGenerator::gridBiomeSource);

    private static final int MIN_Y = GridTerrain.WORLD_BOTTOM;
    private static final double PROGRAM_CHANCE = 0.15;
    private static final int SALT_PROGRAMS = 4000;
    private static final int WORLD_HEIGHT = 384;

    private final GridBiomeSource gridBiomeSource;

    public GridChunkGenerator(GridBiomeSource biomeSource) {
        super(biomeSource);
        this.gridBiomeSource = biomeSource;
    }

    public GridBiomeSource gridBiomeSource() {
        return gridBiomeSource;
    }

    public GridLayout layout() {
        return gridBiomeSource.layout();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState noiseConfig, StructureManager structureAccessor,
                                                    BiomeManager biomeAccess, WorldGenRegion region, Set<Holder<Biome>> biomes) {
        ChunkPos chunkPos = chunk.getPos();
        GridChunkBuild build = build(chunkPos.x(), chunkPos.z());
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int top = Math.min(build.maxY(), chunk.getMaxY());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int y = Math.max(build.columnBottom(localX, localZ), chunk.getMinY()); y <= top; y++) {
                    BlockState state = build.blockAt(localX, y, localZ);
                    if (state.isAir()) {
                        continue;
                    }
                    chunk.setBlockState(pos.set(chunkPos.getMinBlockX() + localX, y, chunkPos.getMinBlockZ() + localZ), state, 0);
                    if (connectsToNeighbours(state)) {
                        chunk.markPosForPostProcessing(pos);
                    }
                    oceanFloor.update(localX, y, localZ, state);
                    worldSurface.update(localX, y, localZ, state);
                }
            }
        }
        placeSigns(build, chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    // Walls, fences, panes and bars take their shape from block updates, which a chunk being generated doesn't get: marked
    // blocks are shaped against their neighbours when the chunk is finished, as vanilla does for structure pieces
    private static boolean connectsToNeighbours(BlockState state) {
        return state.getBlock() instanceof WallBlock || state.getBlock() instanceof CrossCollisionBlock;
    }

    // Station signs need their text as block entities, loaded with the chunk (a chunk being generated has no world to
    // update); skipped wherever corruption replaced the sign
    private static void placeSigns(GridChunkBuild build, ChunkAccess chunk) {
        for (GridTransit.Sign sign : build.signs()) {
            BlockPos pos = chunk.getPos().getWorldPosition().offset(sign.localPos());
            if (!(chunk.getBlockState(pos).getBlock() instanceof SignBlock)) {
                continue;
            }
            CompoundTag nbt = new CompoundTag();
            nbt.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityTypes.SIGN).toString());
            nbt.putInt("x", pos.getX());
            nbt.putInt("y", pos.getY());
            nbt.putInt("z", pos.getZ());
            nbt.putBoolean("is_waxed", true);
            SignText.CODEC.encodeStart(NbtOps.INSTANCE, sign.text()).ifSuccess(text -> nbt.put("front_text", text));
            chunk.setBlockEntityNbt(nbt);
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        // Close enough for spawning and structure checks: the first non-air block counts for every heightmap type
        return build(x >> 4, z >> 4).columnTop(x & 15, z & 15) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
        GridChunkBuild build = build(x >> 4, z >> 4);
        int worldBottom = world.getMinY();
        BlockState[] states = new BlockState[world.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        int top = Math.min(build.maxY(), worldBottom + states.length - 1);
        for (int y = Math.max(build.columnBottom(x & 15, z & 15), worldBottom); y <= top; y++) {
            states[y - worldBottom] = build.blockAt(x & 15, y, z & 15);
        }
        return new NoiseColumn(worldBottom, states);
    }

    // The grid's lines have their own F3 section, digitalfrontier:grid, rather than joining chunk_generation_stats
    @Override
    public void addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos, SamplerContext samplerContext) {
    }

    /** What the grid generates at a position, for the digitalfrontier:grid F3 section. */
    public List<String> debugLines(BlockPos pos) {
        List<String> text = new ArrayList<>();
        GridChunkBuild build = build(pos.getX() >> 4, pos.getZ() >> 4);
        ChunkPlan plan = build.plan();
        text.add(String.format("Grid: %s %s floors=%d district=%s", plan.zone(), plan.type(), plan.floors(), plan.district().getSerializedName()));
        text.add(String.format("Grid corruption: %.2f (%s)", plan.corruption(), plan.tier()));
        if (build.buildingId() != null) {
            text.add("Grid building: " + build.buildingId());
        }
        if (plan.multiChunk() != null) {
            text.add(String.format("Grid multi-chunk origin: %d, %d%s", plan.multiChunk().originX(), plan.multiChunk().originZ(),
                    plan.multiChunk().landmark() ? " (landmark)" : ""));
        }
        if (plan.site() != null) {
            text.add("Grid site: " + plan.site().id());
        }
        String levels = build.describeLevels();
        if (!levels.isEmpty()) {
            text.add("Grid levels: " + levels);
        }
        return text;
    }

    /**
     * Where a player arriving at a terminal stands: on top of the Arrival Pad in the terminal's template, or the
     * middle of the site chunk if it has none. Worked out from the layout alone, so the chunk needn't be loaded.
     */
    public BlockPos arrivalPos(Site terminal) {
        GridChunkBuild build = build(terminal.chunkX(), terminal.chunkZ());
        BlockPos pad = build.arrivalPad();
        if (pad != null) {
            return pad.above();
        }
        return new BlockPos(terminal.chunkX() * 16 + 8, build.columnTop(8, 8) + 1, terminal.chunkZ() * 16 + 8);
    }

    /** Where each Portal's beam of light rises from: block bounds of the beam's foot, worked out from the layout alone. */
    public List<BoundingBox> portalBeams() {
        List<BoundingBox> beams = new ArrayList<>();
        for (Site site : layout().sites()) {
            if (site.type() == Site.Type.PORTAL) {
                BoundingBox beam = build(site.chunkX(), site.chunkZ()).portalBeam();
                if (beam != null) {
                    beams.add(beam);
                }
            }
        }
        return beams;
    }

    private GridChunkBuild build(int chunkX, int chunkZ) {
        return GridChunkBuild.resolve(layout(), GridAssets.current(), chunkX, chunkZ);
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor world) {
        return GridLayout.FLOOR_Y + 1;
    }

    @Override
    public int getSeaLevel() {
        return GridLayout.FLOOR_Y;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return WORLD_HEIGHT;
    }

    /**
     * Programs live on the streets and plazas of the city and the waypoints, where corruption is clean or low.
     * They're placed once, when the chunk generates, and don't despawn.
     */
    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos chunkPos = region.getCenter();
        GridChunkBuild build = build(chunkPos.x(), chunkPos.z());
        ChunkPlan plan = build.plan();
        spawnInPassage(region, build, plan);
        boolean inhabited = plan.zone() == Zone.CITY_CORE || plan.zone() == Zone.CITY || plan.zone() == Zone.WAYPOINT;
        boolean open = plan.type() == PlanType.STREET || plan.type() == PlanType.PLAZA;
        if (!inhabited || !open || plan.tier().compareTo(CorruptionTier.LOW) > 0) {
            return;
        }
        long seed = layout().settings().gridSeed();
        if (GridRandom.unit(GridRandom.hash(seed, chunkPos.x(), chunkPos.z(), SALT_PROGRAMS)) >= PROGRAM_CHANCE) {
            return;
        }
        int count = 1 + GridRandom.nextInt(GridRandom.hash(seed, chunkPos.x(), chunkPos.z(), SALT_PROGRAMS + 1), 2);
        for (int i = 0; i < count; i++) {
            long spot = GridRandom.hash(seed, chunkPos.x(), chunkPos.z(), SALT_PROGRAMS + 2 + i);
            int localX = 2 + GridRandom.nextInt(spot, 12);
            int localZ = 2 + GridRandom.nextInt(spot >>> 20, 12);
            ProgramEntity program = ModEntities.PROGRAM.create(region.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
            if (program == null) {
                return;
            }
            program.snapTo(chunkPos.getMinBlockX() + localX + 0.5, build.columnTop(localX, localZ) + 1,
                    chunkPos.getMinBlockZ() + localZ + 0.5, GridRandom.nextInt(spot >>> 40, 360), 0);
            program.setPersistenceRequired();
            region.addFreshEntityWithPassengers(program);
        }
    }

    // Subway maintenance passages in unstable ground shelter a corrupted program, one that fights back where it's worse
    private static void spawnInPassage(WorldGenRegion region, GridChunkBuild build, ChunkPlan plan) {
        BlockPos spot = build.passageSpot();
        if (spot == null || plan.tier().compareTo(CorruptionTier.UNSTABLE) < 0) {
            return;
        }
        CorruptedProgramEntity program = ModEntities.CORRUPTED_PROGRAM.create(region.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
        if (program == null) {
            return;
        }
        BlockPos pos = region.getCenter().getWorldPosition().offset(spot);
        program.setProvokable(plan.tier().compareTo(CorruptionTier.CORRUPTED) >= 0);
        program.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        program.setPersistenceRequired();
        region.addFreshEntityWithPassengers(program);
    }
}
