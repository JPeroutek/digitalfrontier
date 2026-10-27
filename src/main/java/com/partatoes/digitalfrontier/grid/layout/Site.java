package com.partatoes.digitalfrontier.grid.layout;

/** A named landmark occupying the chunk at (chunkX, chunkZ). */
public record Site(Type type, String id, int chunkX, int chunkZ) {
    public enum Type {
        ARRIVAL_TERMINAL,
        WAYPOINT,
        STRONGHOLD,
        PORTAL
    }

    public double distanceTo(int otherChunkX, int otherChunkZ) {
        return Math.hypot(otherChunkX - chunkX, otherChunkZ - chunkZ);
    }
}
