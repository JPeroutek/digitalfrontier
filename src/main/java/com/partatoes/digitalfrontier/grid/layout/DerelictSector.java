package com.partatoes.digitalfrontier.grid.layout;

import org.jetbrains.annotations.Nullable;

/**
 * A derelict sector: an island of old, abandoned grid floating over the Sea of Simulation, out past where unallocated
 * space has crumbled away. Unallocated space is divided into square cells; each cell may hold one island, more rarely
 * the further out it is. Islands are heavily corrupted, and stand well inside their cell, so a chunk only ever needs
 * to look at its own cell.
 *
 * @param centerX    block X of the island's center
 * @param centerZ    block Z of the island's center
 * @param radius     the island's radius in blocks; its edge is ragged inside this
 * @param corruption the least corruption intensity anywhere on the island
 */
public record DerelictSector(int centerX, int centerZ, int radius, float corruption) {
    /** Cell size in chunks. */
    static final int CELL = 12;
    /** Chunks past the outlands edge before the first islands, about where the last fragments give out. */
    static final int START = 12;

    private static final int SALT_ISLAND = 7001;
    private static final int SALT_CENTER_X = 7002;
    private static final int SALT_CENTER_Z = 7003;
    private static final int SALT_RADIUS = 7004;
    private static final int SALT_CORRUPTION = 7005;

    /** The island covering any part of this chunk, or null. */
    @Nullable
    public static DerelictSector at(GridLayoutSettings settings, int chunkX, int chunkZ) {
        DerelictSector sector = inCell(settings, Math.floorDiv(chunkX, CELL), Math.floorDiv(chunkZ, CELL));
        if (sector == null) {
            return null;
        }
        // The island's edge is ragged within its radius, so take in every chunk any part of the circle could reach
        return sector.distanceTo(chunkX * 16 + 8, chunkZ * 16 + 8) <= sector.radius + 12 ? sector : null;
    }

    @Nullable
    static DerelictSector inCell(GridLayoutSettings settings, int cellX, int cellZ) {
        double past = Math.hypot(cellX * CELL + CELL / 2.0, cellZ * CELL + CELL / 2.0) - settings.outlandsRadius();
        if (past < START) {
            return null;
        }
        long seed = settings.gridSeed();
        // Common just past the fragments, thinning out to a few far out, but never none
        double chance = 0.08 + 0.32 * Math.exp(-(past - START) / 150);
        if (GridRandom.unit(GridRandom.hash(seed, cellX, cellZ, SALT_ISLAND)) >= chance) {
            return null;
        }
        int radiusChunks = 1 + GridRandom.nextInt(GridRandom.hash(seed, cellX, cellZ, SALT_RADIUS), 3);
        // Center chunk 3..8 into the cell: with at most 3 chunks of radius (and the edge chunk), the island stays inside
        int centerChunkX = cellX * CELL + 3 + GridRandom.nextInt(GridRandom.hash(seed, cellX, cellZ, SALT_CENTER_X), 6);
        int centerChunkZ = cellZ * CELL + 3 + GridRandom.nextInt(GridRandom.hash(seed, cellX, cellZ, SALT_CENTER_Z), 6);
        float corruption = 0.5f + 0.4f * (float) GridRandom.unit(GridRandom.hash(seed, cellX, cellZ, SALT_CORRUPTION));
        return new DerelictSector(centerChunkX * 16 + 8, centerChunkZ * 16 + 8, radiusChunks * 16 + 8, corruption);
    }

    public double distanceTo(double blockX, double blockZ) {
        return Math.hypot(blockX - centerX, blockZ - centerZ);
    }

    /** Whether the whole chunk is well inside the island, where a building can stand. */
    public boolean holdsBuilding(int chunkX, int chunkZ) {
        return distanceTo(chunkX * 16 + 8, chunkZ * 16 + 8) + 12 <= radius * 0.75;
    }
}
