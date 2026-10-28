package com.partatoes.digitalfrontier.grid.layout;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public enum Zone implements StringIdentifiable {
    CITY_CORE("city_core"),
    CITY("city"),
    OUTSKIRTS("outskirts"),
    WAYPOINT("waypoint"),
    OUTLANDS("outlands"),
    UNALLOCATED("unallocated"),
    /** Islands of abandoned grid out over the Sea of Simulation, past unallocated space (see {@link DerelictSector}). */
    DERELICT("derelict");

    public static final Codec<Zone> CODEC = StringIdentifiable.createCodec(Zone::values);

    private final String name;

    Zone(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }
}
