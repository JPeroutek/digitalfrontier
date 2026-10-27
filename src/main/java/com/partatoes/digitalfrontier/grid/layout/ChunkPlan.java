package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;

/**
 * What a single chunk of the grid contains. Computed purely from the chunk position and grid settings.
 *
 * @param streetX    true if a street runs along the X axis through this chunk
 * @param streetZ    true if a street runs along the Z axis through this chunk
 * @param avenue     true if this street lies on one of the main X/Z axes through the city center
 * @param multiChunk set when this chunk is part of a 2×2-chunk building
 */
public record ChunkPlan(
        Zone zone,
        PlanType type,
        int floors,
        DyeColor district,
        boolean streetX,
        boolean streetZ,
        boolean avenue,
        @Nullable Site site,
        @Nullable MultiChunk multiChunk,
        float corruption,
        CorruptionTier tier) {

    /** The chunk whose position seeds this chunk's building choice: the multi-chunk origin, or the chunk itself. */
    public int originX(int chunkX) {
        return multiChunk != null ? multiChunk.originX() : chunkX;
    }

    public int originZ(int chunkZ) {
        return multiChunk != null ? multiChunk.originZ() : chunkZ;
    }
}
