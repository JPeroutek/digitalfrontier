package com.partatoes.digitalfrontier.grid.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.CorruptionTier;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** Picks each position's biome from the zone the grid layout puts it in. */
public class GridBiomeSource extends BiomeSource {
    public static final MapCodec<GridBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            GridLayoutSettings.CODEC.fieldOf("settings").forGetter(GridBiomeSource::settings),
            GridBiomes.CODEC.fieldOf("biomes").forGetter(GridBiomeSource::biomes)
    ).apply(instance, GridBiomeSource::new));

    private final GridLayoutSettings settings;
    private final GridBiomes biomes;
    private final GridLayout layout;

    public GridBiomeSource(GridLayoutSettings settings, GridBiomes biomes) {
        this.settings = settings;
        this.biomes = biomes;
        this.layout = new GridLayout(settings);
    }

    public GridLayoutSettings settings() {
        return settings;
    }

    public GridBiomes biomes() {
        return biomes;
    }

    public GridLayout layout() {
        return layout;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler noise) {
        // A resolver serves one chunk at a time, many times over, so remember the last chunk's biome
        long[] lastChunk = {Long.MIN_VALUE};
        List<Holder<Biome>> lastBiome = new ArrayList<>(List.of(biomes.forZone(Zone.CITY, false)));
        return (biomeX, biomeY, biomeZ) -> {
            int chunkX = QuartPos.toSection(biomeX);
            int chunkZ = QuartPos.toSection(biomeZ);
            long chunk = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
            if (chunk != lastChunk[0]) {
                // Only strong decay changes the biome; lighter decay just changes blocks
                ChunkPlan plan = layout.planAt(chunkX, chunkZ);
                lastBiome.set(0, biomes.forZone(plan.zone(), plan.tier().compareTo(CorruptionTier.CORRUPTED) >= 0));
                lastChunk[0] = chunk;
            }
            return lastBiome.getFirst();
        };
    }
}
