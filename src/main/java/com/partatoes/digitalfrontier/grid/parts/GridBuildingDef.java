package com.partatoes.digitalfrontier.grid.parts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

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

    public enum Kind implements StringRepresentable {
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
        STRONGHOLD("stronghold"),

        // The city's levels: one part (the base), authored for a line along X, placed with its Y = 0 at baseY.
        /** One half of a skyroad deck: the median at Z = 15, the outer edge at Z = 8; pillars come down to Y 65. */
        SKYROAD("skyroad", GridLayout.FLOOR_Y + 1),
        /** A quarter of the junction where two skyroads cross: the medians meet at X = 15, Z = 15. */
        SKYROAD_JUNCTION("skyroad_junction", GridLayout.FLOOR_Y + 1),
        /** The first chunk of an on-ramp: Z = 2..7, rising from the street at X = 0 to half way at X = 15. */
        SKYROAD_RAMP_LOWER("skyroad_ramp_lower", GridLayout.FLOOR_Y + 1),
        /** The second chunk of an on-ramp, reaching the deck (Z = 8) at the far end. */
        SKYROAD_RAMP_UPPER("skyroad_ramp_upper", GridLayout.FLOOR_Y + 1),
        /** An elevated rail guideway with its supports, tracks along X. */
        GUIDEWAY("guideway", GridLayout.FLOOR_Y + 1),
        /** A guideway over an intersection: no supports in the crossing road. */
        GUIDEWAY_CROSSING("guideway_crossing", GridLayout.FLOOR_Y + 1),
        /** An elevated rail station over an intersection, with its way down to the street. */
        GUIDEWAY_STATION("guideway_station", GridLayout.FLOOR_Y + 1),
        /** A subway tunnel, tracks along X; Y = 0 is the tunnel floor. Carve the tunnel with air. */
        SUBWAY("subway", 47),
        /**
         * A subway station under an intersection, with its way up to the street (Y = 17 is the street surface). Leave
         * X = 7..8, Y = 3 clear on the walls at Z = 1 and 14: the station's name signs go there.
         */
        SUBWAY_STATION("subway_station", 47),
        /** A bend in an express subway line: in from the south (Z = 15), out to the east (X = 15). */
        SUBWAY_CORNER("subway_corner", 47),
        /**
         * A maintenance room beside a subway tunnel, toward Z = 0, with a doorway through the tunnel wall at Z = 3.
         * Placed before the tunnel, so its blocks win; mirrored for rooms on the other side.
         */
        SUBWAY_PASSAGE("subway_passage", 47),
        /** A skyway bridge across a road along X (so the bridge runs along Z); Y = 0 is its floor. */
        SKYWAY("skyway", -1);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;
        // Where a level part's Y = 0 goes; 0 for kinds stacked from the street, -1 where it varies (skyways)
        private final int baseY;

        Kind(String name) {
            this(name, 0);
        }

        Kind(String name, int baseY) {
            this.name = name;
            this.baseY = baseY;
        }

        /** Whether this kind is one of the city's levels (placed by the levels, not stacked on a chunk). */
        public boolean isLevel() {
            return baseY != 0;
        }

        /** The Y a level part's Y = 0 is placed at; -1 if it depends on the piece (skyways, at their floor). */
        public int baseY() {
            return baseY;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
