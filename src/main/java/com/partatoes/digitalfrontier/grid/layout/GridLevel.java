package com.partatoes.digitalfrontier.grid.layout;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** The levels a city can have above and below its streets (see {@link TransitLayout}). */
public enum GridLevel implements StringIdentifiable {
    /** Light-cycle decks over the boulevards, at Y 80, with ramps up from the street. */
    SKYROADS("skyroads"),
    /** Minecart guideways at Y 88 over some north–south streets. */
    ELEVATED_RAIL("elevated_rail"),
    /** Minecart tunnels at Y 48 under some east–west streets, with stations. */
    SUBWAY("subway"),
    /** Bridges between towers across streets, at their floor heights (Y 101 and up). */
    SKYWAYS("skyways");

    public static final Codec<GridLevel> CODEC = StringIdentifiable.createCodec(GridLevel::values);
    /** A set of levels as a JSON list, always in declaration order so the encoding (and grid id) is stable. */
    public static final Codec<Set<GridLevel>> SET_CODEC = CODEC.listOf().xmap(GridLevel::setOf, GridLevel::sortedList);

    private final String name;

    GridLevel(String name) {
        this.name = name;
    }

    public static Set<GridLevel> setOf(List<GridLevel> levels) {
        return levels.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(levels));
    }

    private static List<GridLevel> sortedList(Set<GridLevel> levels) {
        return levels.isEmpty() ? List.of() : List.copyOf(EnumSet.copyOf(levels));
    }

    @Override
    public String asString() {
        return name;
    }
}
