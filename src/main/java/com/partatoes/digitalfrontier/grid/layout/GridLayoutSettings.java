package com.partatoes.digitalfrontier.grid.layout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.StreetStyle;
import net.minecraft.util.DyeColor;

import java.util.List;

/**
 * Everything that defines a grid. Two grids with identical settings are the same grid,
 * which is why the grid id is a hash of these values (see {@link com.partatoes.digitalfrontier.grid.GridKey}).
 * Radii are in chunks, measured from the city center at chunk (0, 0).
 */
public record GridLayoutSettings(
        long gridSeed,
        float corruption,
        int coreRadius,
        int cityRadius,
        int blockSize,
        int outskirtsRadius,
        int waypointRingRadius,
        int waypointCount,
        int outlandsRadius,
        List<DyeColor> districtColors,
        StreetStyle streetStyle) {

    public static final GridLayoutSettings DEFAULT = new GridLayoutSettings(
            0L, 0.5f, 10, 40, 3, 64, 80, 4, 128,
            List.of(DyeColor.LIGHT_BLUE, DyeColor.CYAN, DyeColor.BLUE, DyeColor.ORANGE), StreetStyle.VARIED);

    public static final Codec<GridLayoutSettings> CODEC = RecordCodecBuilder.<GridLayoutSettings>create(instance -> instance.group(
            Codec.LONG.fieldOf("grid_seed").forGetter(GridLayoutSettings::gridSeed),
            Codec.floatRange(0f, 1f).fieldOf("corruption").forGetter(GridLayoutSettings::corruption),
            Codec.intRange(1, 64).fieldOf("core_radius").forGetter(GridLayoutSettings::coreRadius),
            Codec.intRange(2, 256).fieldOf("city_radius").forGetter(GridLayoutSettings::cityRadius),
            Codec.intRange(1, 8).fieldOf("block_size").forGetter(GridLayoutSettings::blockSize),
            Codec.intRange(3, 512).fieldOf("outskirts_radius").forGetter(GridLayoutSettings::outskirtsRadius),
            Codec.intRange(4, 512).fieldOf("waypoint_ring_radius").forGetter(GridLayoutSettings::waypointRingRadius),
            Codec.intRange(0, 8).fieldOf("waypoint_count").forGetter(GridLayoutSettings::waypointCount),
            Codec.intRange(5, 1024).fieldOf("outlands_radius").forGetter(GridLayoutSettings::outlandsRadius),
            DyeColor.CODEC.listOf(1, 16).fieldOf("district_colors").forGetter(GridLayoutSettings::districtColors),
            // Left out of the JSON when uniform, so grids from before varied streets keep their encoding (and GridKey)
            StreetStyle.CODEC.optionalFieldOf("street_style", StreetStyle.UNIFORM).forGetter(GridLayoutSettings::streetStyle)
    ).apply(instance, GridLayoutSettings::new)).validate(GridLayoutSettings::validate);

    private static DataResult<GridLayoutSettings> validate(GridLayoutSettings settings) {
        if (!(settings.coreRadius < settings.cityRadius
                && settings.cityRadius < settings.outskirtsRadius
                && settings.outskirtsRadius < settings.waypointRingRadius
                && settings.waypointRingRadius < settings.outlandsRadius)) {
            return DataResult.error(() -> "Grid radii must increase: core < city < outskirts < waypoint ring < outlands");
        }
        // Waypoints sit on the 8 compass directions so highways to them stay chunk-aligned
        if (settings.waypointCount != 0 && 8 % settings.waypointCount != 0) {
            return DataResult.error(() -> "waypoint_count must be 0, 1, 2, 4 or 8");
        }
        return DataResult.success(settings);
    }
}
