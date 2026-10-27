package com.partatoes.digitalfrontier.grid.gen;

import com.mojang.serialization.MapCodec;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.parts.GridAssets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

    private static final int MIN_Y = -64;
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
        int bottom = GridTerrain.columnBottom(build.plan(), chunk.getBottomY());
        int top = Math.min(build.maxY(), chunk.getTopYInclusive());
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int y = bottom; y <= top; y++) {
                    BlockState state = build.blockAt(localX, y, localZ);
                    if (state.isAir()) {
                        continue;
                    }
                    chunk.setBlockState(pos.set(chunkPos.getStartX() + localX, y, chunkPos.getStartZ() + localZ), state, false);
                    oceanFloor.trackUpdate(localX, y, localZ, state);
                    worldSurface.trackUpdate(localX, y, localZ, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
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
        for (int y = GridTerrain.columnBottom(build.plan(), worldBottom); y <= top; y++) {
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

    @Override
    public void populateEntities(ChunkRegion region) {
    }
}
