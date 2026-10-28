package com.partatoes.digitalfrontier.grid.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.layout.CorruptionTier;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.stream.Stream;

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
    protected MapCodec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return biomes.stream();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int biomeX, int biomeY, int biomeZ, MultiNoiseUtil.MultiNoiseSampler noise) {
        int chunkX = BiomeCoords.toChunk(biomeX);
        int chunkZ = BiomeCoords.toChunk(biomeZ);
        // Only strong corruption changes the biome; lighter corruption just changes blocks
        boolean corrupted = CorruptionTier.fromIntensity(layout.corruptionAt(chunkX, chunkZ)).compareTo(CorruptionTier.CORRUPTED) >= 0;
        return biomes.forZone(layout.zoneAt(chunkX, chunkZ), corrupted);
    }
}
