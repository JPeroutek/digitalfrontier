package com.partatoes.digitalfrontier.grid.layout;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Zone implements StringRepresentable {
    CITY_CORE("city_core"),
    CITY("city"),
    OUTSKIRTS("outskirts"),
    WAYPOINT("waypoint"),
    OUTLANDS("outlands"),
    UNALLOCATED("unallocated"),
    /** Islands of abandoned grid out over the Sea of Simulation, past unallocated space (see {@link DerelictSector}). */
    DERELICT("derelict");

    public static final Codec<Zone> CODEC = StringRepresentable.fromEnum(Zone::values);

    private final String name;

    Zone(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
