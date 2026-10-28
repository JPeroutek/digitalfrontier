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
import net.minecraft.entity.SpawnReason;
import com.partatoes.digitalfrontier.grid.parts.GridAssets;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignText;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        GridChunkBuild build = build(chunkPos.x, chunkPos.z);
        Heightmap oceanFloor = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        int top = Math.min(build.maxY(), chunk.getTopYInclusive());
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int y = Math.max(build.columnBottom(localX, localZ), chunk.getBottomY()); y <= top; y++) {
                    BlockState state = build.blockAt(localX, y, localZ);
                    if (state.isAir()) {
                        continue;
                    }
                    chunk.setBlockState(pos.set(chunkPos.getStartX() + localX, y, chunkPos.getStartZ() + localZ), state, 0);
                    oceanFloor.trackUpdate(localX, y, localZ, state);
                    worldSurface.trackUpdate(localX, y, localZ, state);
                }
            }
        }
        placeSigns(build, chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    // Station signs need their text as block entities, loaded with the chunk (a chunk being generated has no world to
    // update); skipped wherever corruption replaced the sign
    private static void placeSigns(GridChunkBuild build, Chunk chunk) {
        for (GridTransit.Sign sign : build.signs()) {
            BlockPos pos = chunk.getPos().getStartPos().add(sign.localPos());
            if (!(chunk.getBlockState(pos).getBlock() instanceof AbstractSignBlock)) {
                continue;
            }
            NbtCompound nbt = new NbtCompound();
            nbt.putString("id", Registries.BLOCK_ENTITY_TYPE.getId(BlockEntityType.SIGN).toString());
            nbt.putInt("x", pos.getX());
            nbt.putInt("y", pos.getY());
            nbt.putInt("z", pos.getZ());
            nbt.putBoolean("is_waxed", true);
            SignText.CODEC.encodeStart(NbtOps.INSTANCE, sign.text()).ifSuccess(text -> nbt.put("front_text", text));
            chunk.addPendingBlockEntityNbt(nbt);
        }
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        // Close enough for spawning and structure checks: the first non-air block counts for every heightmap type
        return build(x >> 4, z >> 4).columnTop(x & 15, z & 15) + 1;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        GridChunkBuild build = build(x >> 4, z >> 4);
        int worldBottom = world.getBottomY();
        BlockState[] states = new BlockState[world.getHeight()];
        Arrays.fill(states, Blocks.AIR.getDefaultState());
        int top = Math.min(build.maxY(), worldBottom + states.length - 1);
        for (int y = Math.max(build.columnBottom(x & 15, z & 15), worldBottom); y <= top; y++) {
            states[y - worldBottom] = build.blockAt(x & 15, y, z & 15);
        }
        return new VerticalBlockSample(worldBottom, states);
    }

    @Override
    public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        GridChunkBuild build = build(pos.getX() >> 4, pos.getZ() >> 4);
        ChunkPlan plan = build.plan();
        text.add(String.format("Grid: %s %s floors=%d district=%s", plan.zone(), plan.type(), plan.floors(), plan.district().asString()));
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
    }

    /**
     * Where a player arriving at a terminal stands: on top of the Arrival Pad in the terminal's template, or the
     * middle of the site chunk if it has none. Worked out from the layout alone, so the chunk needn't be loaded.
     */
    public BlockPos arrivalPos(Site terminal) {
        GridChunkBuild build = build(terminal.chunkX(), terminal.chunkZ());
        BlockPos pad = build.arrivalPad();
        if (pad != null) {
            return pad.up();
        }
        return new BlockPos(terminal.chunkX() * 16 + 8, build.columnTop(8, 8) + 1, terminal.chunkZ() * 16 + 8);
    }

    /** Where each Portal's beam of light rises from: block bounds of the beam's foot, worked out from the layout alone. */
    public List<BlockBox> portalBeams() {
        List<BlockBox> beams = new ArrayList<>();
        for (Site site : layout().sites()) {
            if (site.type() == Site.Type.PORTAL) {
                BlockBox beam = build(site.chunkX(), site.chunkZ()).portalBeam();
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
    public int getSpawnHeight(HeightLimitView world) {
        return GridLayout.FLOOR_Y + 1;
    }

    @Override
    public int getSeaLevel() {
        return GridLayout.FLOOR_Y;
    }

    @Override
    public int getMinimumY() {
        return MIN_Y;
    }

    @Override
    public int getWorldHeight() {
        return WORLD_HEIGHT;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk) {
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
    }

    /**
     * Programs live on the streets and plazas of the city and the waypoints, where corruption is clean or low.
     * They're placed once, when the chunk generates, and don't despawn.
     */
    @Override
    public void populateEntities(ChunkRegion region) {
        ChunkPos chunkPos = region.getCenterPos();
        GridChunkBuild build = build(chunkPos.x, chunkPos.z);
        ChunkPlan plan = build.plan();
        spawnInPassage(region, build, plan);
        boolean inhabited = plan.zone() == Zone.CITY_CORE || plan.zone() == Zone.CITY || plan.zone() == Zone.WAYPOINT;
        boolean open = plan.type() == PlanType.STREET || plan.type() == PlanType.PLAZA;
        if (!inhabited || !open || plan.tier().compareTo(CorruptionTier.LOW) > 0) {
            return;
        }
        long seed = layout().settings().gridSeed();
        if (GridRandom.unit(GridRandom.hash(seed, chunkPos.x, chunkPos.z, SALT_PROGRAMS)) >= PROGRAM_CHANCE) {
            return;
        }
        int count = 1 + GridRandom.nextInt(GridRandom.hash(seed, chunkPos.x, chunkPos.z, SALT_PROGRAMS + 1), 2);
        for (int i = 0; i < count; i++) {
            long spot = GridRandom.hash(seed, chunkPos.x, chunkPos.z, SALT_PROGRAMS + 2 + i);
            int localX = 2 + GridRandom.nextInt(spot, 12);
            int localZ = 2 + GridRandom.nextInt(spot >>> 20, 12);
            ProgramEntity program = ModEntities.PROGRAM.create(region.toServerWorld(), SpawnReason.CHUNK_GENERATION);
            if (program == null) {
                return;
            }
            program.refreshPositionAndAngles(chunkPos.getStartX() + localX + 0.5, build.columnTop(localX, localZ) + 1,
                    chunkPos.getStartZ() + localZ + 0.5, GridRandom.nextInt(spot >>> 40, 360), 0);
            program.setPersistent();
            region.spawnEntityAndPassengers(program);
        }
    }

    // Subway maintenance passages in unstable ground shelter a corrupted program, one that fights back where it's worse
    private static void spawnInPassage(ChunkRegion region, GridChunkBuild build, ChunkPlan plan) {
        BlockPos spot = build.passageSpot();
        if (spot == null || plan.tier().compareTo(CorruptionTier.UNSTABLE) < 0) {
            return;
        }
        CorruptedProgramEntity program = ModEntities.CORRUPTED_PROGRAM.create(region.toServerWorld(), SpawnReason.CHUNK_GENERATION);
        if (program == null) {
            return;
        }
        BlockPos pos = region.getCenterPos().getStartPos().add(spot);
        program.setProvokable(plan.tier().compareTo(CorruptionTier.CORRUPTED) >= 0);
        program.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        program.setPersistent();
        region.spawnEntityAndPassengers(program);
    }
}
