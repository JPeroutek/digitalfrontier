package com.partatoes.digitalfrontier.grid.parts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A building, plaza, or piece of street furniture, loaded from {@code data/<namespace>/grid/building/<name>.json}.
 * It is a vertical stack of parts: an optional base, a number of floors, and an optional roof. Part ids are
 * structure ids, e.g. {@code digitalfrontier:grid/tower/floor} for {@code data/digitalfrontier/structure/grid/tower/floor.nbt}.
 *
 * @param footprint size in chunks: 1 for 16×16 parts, 2 for 32×32 parts
 * @param floors    candidate floor parts; each floor picks one at random
 * @param road      for street furniture: the kind of road it's for (default {@code street})
 */
public record GridBuildingDef(
        Kind kind,
        List<Zone> zones,
        int weight,
        int footprint,
        Optional<Identifier> base,
        List<Identifier> floors,
        Optional<Identifier> roof,
        int minFloors,
        int maxFloors,
        Optional<RoadClass> road) {

    public static final Codec<GridBuildingDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.fieldOf("kind").forGetter(GridBuildingDef::kind),
            Zone.CODEC.listOf().fieldOf("zones").forGetter(GridBuildingDef::zones),
            Codec.intRange(1, 1000).optionalFieldOf("weight", 10).forGetter(GridBuildingDef::weight),
            Codec.intRange(1, 2).optionalFieldOf("footprint", 1).forGetter(GridBuildingDef::footprint),
            Identifier.CODEC.optionalFieldOf("base").forGetter(GridBuildingDef::base),
            Identifier.CODEC.listOf().optionalFieldOf("floors", List.of()).forGetter(GridBuildingDef::floors),
            Identifier.CODEC.optionalFieldOf("roof").forGetter(GridBuildingDef::roof),
            Codec.intRange(0, 64).optionalFieldOf("min_floors", 0).forGetter(GridBuildingDef::minFloors),
            Codec.intRange(0, 64).optionalFieldOf("max_floors", 64).forGetter(GridBuildingDef::maxFloors),
            RoadClass.CODEC.optionalFieldOf("road").forGetter(GridBuildingDef::road)
    ).apply(instance, GridBuildingDef::new));

    // Everything but street furniture is the same for every road
    public GridBuildingDef(Kind kind, List<Zone> zones, int weight, int footprint, Optional<Identifier> base, List<Identifier> floors,
                           Optional<Identifier> roof, int minFloors, int maxFloors) {
        this(kind, zones, weight, footprint, base, floors, roof, minFloors, maxFloors, Optional.empty());
    }

    /** The road class street furniture is for; null for every other kind. */
    @Nullable
    public RoadClass roadOrNull() {
        return kind == Kind.STREET ? road.orElse(RoadClass.STREET) : null;
    }

    public Stream<Identifier> allParts() {
        return Stream.of(base.stream(), floors.stream(), roof.stream()).flatMap(stream -> stream);
    }

    public enum Kind implements StringIdentifiable {
        /** Placed on building chunks. */
        BUILDING("building"),
        /** The 2×2 tower at the center of the city. */
        LANDMARK("landmark"),
        /** Placed on plaza chunks (open squares in the city, and waypoints). */
        PLAZA("plaza"),
        /** Decorations placed on straight road chunks; authored for a road running along the X axis (see {@code road}). */
        STREET("street"),
        /** The building at an arrival terminal or waypoint; contains the Arrival Pad players arrive on. */
        TERMINAL("terminal"),
        /** The building at a Portal site; contains the Portal blocks players leave the grid through. */
        PORTAL("portal"),
        /** The building at a stronghold site, deep in corrupted territory. */
        STRONGHOLD("stronghold");

        public static final Codec<Kind> CODEC = StringIdentifiable.createCodec(Kind::values);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
