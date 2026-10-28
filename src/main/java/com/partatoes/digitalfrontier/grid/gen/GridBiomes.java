package com.partatoes.digitalfrontier.grid.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.util.stream.Stream;

/** The biome used for each zone of a grid, and for strongly corrupted ground in it. */
public record GridBiomes(
        RegistryEntry<Biome> cityCore,
        RegistryEntry<Biome> city,
        RegistryEntry<Biome> outskirts,
        RegistryEntry<Biome> outlands,
        RegistryEntry<Biome> unallocated,
        RegistryEntry<Biome> corruptedCity,
        RegistryEntry<Biome> corruptedOutlands,
        RegistryEntry<Biome> corruptedUnallocated) {

    public static final Codec<GridBiomes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.REGISTRY_CODEC.fieldOf("city_core").forGetter(GridBiomes::cityCore),
            Biome.REGISTRY_CODEC.fieldOf("city").forGetter(GridBiomes::city),
            Biome.REGISTRY_CODEC.fieldOf("outskirts").forGetter(GridBiomes::outskirts),
            Biome.REGISTRY_CODEC.fieldOf("outlands").forGetter(GridBiomes::outlands),
            Biome.REGISTRY_CODEC.fieldOf("unallocated").forGetter(GridBiomes::unallocated),
            Biome.REGISTRY_CODEC.fieldOf("corrupted_city").forGetter(GridBiomes::corruptedCity),
            Biome.REGISTRY_CODEC.fieldOf("corrupted_outlands").forGetter(GridBiomes::corruptedOutlands),
            Biome.REGISTRY_CODEC.fieldOf("corrupted_unallocated").forGetter(GridBiomes::corruptedUnallocated)
    ).apply(instance, GridBiomes::new));

    public RegistryEntry<Biome> forZone(Zone zone, boolean corrupted) {
        if (corrupted) {
            return switch (zone) {
                case CITY_CORE, CITY -> corruptedCity;
                case OUTSKIRTS, WAYPOINT, OUTLANDS -> corruptedOutlands;
                case UNALLOCATED, DERELICT -> corruptedUnallocated;
            };
        }
        return switch (zone) {
            case CITY_CORE -> cityCore;
            case CITY -> city;
            // Waypoints are small settlements out past the city; until they get their own biome they read as outskirts
            case OUTSKIRTS, WAYPOINT -> outskirts;
            case OUTLANDS -> outlands;
            case UNALLOCATED, DERELICT -> unallocated;
        };
    }

    public Stream<RegistryEntry<Biome>> stream() {
        return Stream.of(cityCore, city, outskirts, outlands, unallocated, corruptedCity, corruptedOutlands, corruptedUnallocated);
    }
}
