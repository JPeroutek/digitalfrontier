package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;

/**
 * What a single chunk of the grid contains. Computed purely from the chunk position and grid settings.
 *
 * @param streetX true if a street runs along the X axis through this chunk
 * @param streetZ true if a street runs along the Z axis through this chunk
 * @param avenue  true if this street lies on one of the main X/Z axes through the city center
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
        float corruption,
        CorruptionTier tier) {
}
