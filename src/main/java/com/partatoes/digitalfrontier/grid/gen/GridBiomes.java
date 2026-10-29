package com.partatoes.digitalfrontier.grid.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/** The biome used for each zone of a grid, and for strongly corrupted ground in it. */
public record GridBiomes(
        Holder<Biome> cityCore,
        Holder<Biome> city,
        Holder<Biome> outskirts,
        Holder<Biome> outlands,
        Holder<Biome> unallocated,
        Holder<Biome> corruptedCity,
        Holder<Biome> corruptedOutlands,
        Holder<Biome> corruptedUnallocated) {

    public static final Codec<GridBiomes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.CODEC.fieldOf("city_core").forGetter(GridBiomes::cityCore),
            Biome.CODEC.fieldOf("city").forGetter(GridBiomes::city),
            Biome.CODEC.fieldOf("outskirts").forGetter(GridBiomes::outskirts),
            Biome.CODEC.fieldOf("outlands").forGetter(GridBiomes::outlands),
            Biome.CODEC.fieldOf("unallocated").forGetter(GridBiomes::unallocated),
            Biome.CODEC.fieldOf("corrupted_city").forGetter(GridBiomes::corruptedCity),
            Biome.CODEC.fieldOf("corrupted_outlands").forGetter(GridBiomes::corruptedOutlands),
            Biome.CODEC.fieldOf("corrupted_unallocated").forGetter(GridBiomes::corruptedUnallocated)
    ).apply(instance, GridBiomes::new));

    public Holder<Biome> forZone(Zone zone, boolean corrupted) {
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

    public Stream<Holder<Biome>> stream() {
        return Stream.of(cityCore, city, outskirts, outlands, unallocated, corruptedCity, corruptedOutlands, corruptedUnallocated);
    }
}
