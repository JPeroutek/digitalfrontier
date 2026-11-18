package com.partatoes.digitalfrontier.grid.layout;

import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import net.minecraft.world.item.DyeColor;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.Road;
import org.jetbrains.annotations.Nullable;

/**
 * What a single chunk of the grid contains. Computed purely from the chunk position and grid settings.
 *
 * @param roadX      the road running along the X axis through this chunk, if any
 * @param roadZ      the road running along the Z axis through this chunk, if any
 * @param multiChunk set when this chunk is part of a 2×2-chunk building
 * @param diagonal   set when a diagonal highway crosses this (open) chunk
 * @param corruption decay intensity, after the regime's control has suppressed it (see {@link Territory})
 * @param control    the regime's control (see {@link ControlField})
 */
public record ChunkPlan(
        Zone zone,
        PlanType type,
        int floors,
        DyeColor district,
        @Nullable Road roadX,
        @Nullable Road roadZ,
        @Nullable Site site,
        @Nullable MultiChunk multiChunk,
        @Nullable DiagonalRoad diagonal,
        float corruption,
        CorruptionTier tier,
        float control,
        ControlTier controlTier) {

    /** True if this chunk is a road: a street, or open ground a diagonal highway crosses. */
    public boolean road() {
        return type == PlanType.STREET || diagonal != null;
    }

    ChunkPlan withTerritory(float corruption, float control) {
        return new ChunkPlan(zone, type, floors, district, roadX, roadZ, site, multiChunk, diagonal,
                corruption, CorruptionTier.fromIntensity(corruption), control, ControlTier.fromIntensity(control));
    }

    /** True if a road runs along the X axis through this chunk. */
    public boolean streetX() {
        return roadX != null;
    }

    /** True if a road runs along the Z axis through this chunk. */
    public boolean streetZ() {
        return roadZ != null;
    }

    /** True if this is a one-chunk street through the city center (or a highway): those get a lit center line. */
    public boolean avenue() {
        return isCentralStreet(roadX) || isCentralStreet(roadZ);
    }

    private static boolean isCentralStreet(@Nullable Road road) {
        return road != null && road.central() && road.roadClass() == RoadClass.STREET;
    }

    /** The chunk whose position seeds this chunk's building choice: the multi-chunk origin, or the chunk itself. */
    public int originX(int chunkX) {
        return multiChunk != null ? multiChunk.originX() : chunkX;
    }

    public int originZ(int chunkZ) {
        return multiChunk != null ? multiChunk.originZ() : chunkZ;
    }
}
