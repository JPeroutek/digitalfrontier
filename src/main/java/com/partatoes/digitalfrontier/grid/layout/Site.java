package com.partatoes.digitalfrontier.grid.layout;

/** A named landmark occupying the chunk at (chunkX, chunkZ). */
public record Site(Type type, String id, int chunkX, int chunkZ) {
    public enum Type {
        ARRIVAL_TERMINAL,
        WAYPOINT,
        STRONGHOLD,
        PORTAL
    }

    /** Arrival terminals and waypoints both have a terminal players can arrive at. */
    public boolean hasTerminal() {
        return type == Type.ARRIVAL_TERMINAL || type == Type.WAYPOINT;
    }

    public double distanceTo(int otherChunkX, int otherChunkZ) {
        return Math.hypot(otherChunkX - chunkX, otherChunkZ - chunkZ);
    }
}
