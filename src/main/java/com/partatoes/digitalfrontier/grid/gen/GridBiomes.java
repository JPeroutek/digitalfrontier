package com.partatoes.digitalfrontier.grid.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.util.stream.Stream;

/** The biome used for each zone of a grid. */
public record GridBiomes(
        RegistryEntry<Biome> cityCore,
        RegistryEntry<Biome> city,
        RegistryEntry<Biome> outskirts,
        RegistryEntry<Biome> outlands,
        RegistryEntry<Biome> unallocated) {

    public static final Codec<GridBiomes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.REGISTRY_CODEC.fieldOf("city_core").forGetter(GridBiomes::cityCore),
            Biome.REGISTRY_CODEC.fieldOf("city").forGetter(GridBiomes::city),
            Biome.REGISTRY_CODEC.fieldOf("outskirts").forGetter(GridBiomes::outskirts),
            Biome.REGISTRY_CODEC.fieldOf("outlands").forGetter(GridBiomes::outlands),
            Biome.REGISTRY_CODEC.fieldOf("unallocated").forGetter(GridBiomes::unallocated)
    ).apply(instance, GridBiomes::new));

    public RegistryEntry<Biome> forZone(Zone zone) {
        return switch (zone) {
            case CITY_CORE -> cityCore;
            case CITY -> city;
            // Waypoints are small settlements out past the city; until they get their own biome they read as outskirts
            case OUTSKIRTS, WAYPOINT -> outskirts;
            case OUTLANDS -> outlands;
            case UNALLOCATED -> unallocated;
        };
    }

    public Stream<RegistryEntry<Biome>> stream() {
        return Stream.of(cityCore, city, outskirts, outlands, unallocated);
    }
}
