package com.partatoes.digitalfrontier.grid.tuning;

import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.StreetStyle;
import net.minecraft.util.DyeColor;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * The settings a Grid Tuning Laser lets players change, each within bounds. Every other layout setting is derived
 * from these, scaled from the default grid, so a tuning always makes a sensible grid. The default tuning makes
 * exactly {@link GridLayoutSettings#DEFAULT}.
 *
 * <p>A tuning can be shared as a short grid code (a prefix plus URL-safe base64), which decodes back to the
 * identical tuning and therefore the identical grid. Codes are {@code DF2.}; the older {@code DF1.} codes, from
 * before street styles, still decode, as uniform streets.
 *
 * @param corruptionPercent 0 to 100
 * @param cityRadius        in chunks; the core, outskirts, waypoint ring and outlands scale with it
 * @param blockSize         city block size in chunks; with varied streets, blocks range around it
 */
public record GridTuning(long seed, int corruptionPercent, int cityRadius, int blockSize, int waypointCount,
                         List<DyeColor> districtColors, StreetStyle streetStyle) {
    public static final int MIN_CITY_RADIUS = 16;
    public static final int MAX_CITY_RADIUS = 96;
    public static final int MIN_BLOCK_SIZE = 2;
    public static final int MAX_BLOCK_SIZE = 6;
    public static final List<Integer> WAYPOINT_COUNTS = List.of(0, 1, 2, 4, 8);
    public static final int MAX_DISTRICT_COLORS = 8;

    public static final GridTuning DEFAULT = new GridTuning(0L, 50, 40, 3, 4,
            List.of(DyeColor.LIGHT_BLUE, DyeColor.CYAN, DyeColor.BLUE, DyeColor.ORANGE), StreetStyle.VARIED);

    private static final String V1_PREFIX = "DF1.";
    private static final String V2_PREFIX = "DF2.";

    public GridTuning {
        districtColors = List.copyOf(districtColors);
    }

    public boolean isValid() {
        return corruptionPercent >= 0 && corruptionPercent <= 100
                && cityRadius >= MIN_CITY_RADIUS && cityRadius <= MAX_CITY_RADIUS
                && blockSize >= MIN_BLOCK_SIZE && blockSize <= MAX_BLOCK_SIZE
                && WAYPOINT_COUNTS.contains(waypointCount)
                && !districtColors.isEmpty() && districtColors.size() <= MAX_DISTRICT_COLORS
                && new HashSet<>(districtColors).size() == districtColors.size();
    }

    // Ratios of the default grid: core 10, city 40, outskirts 64, waypoint ring 80, outlands 128
    public GridLayoutSettings toSettings() {
        return new GridLayoutSettings(seed, corruptionPercent / 100f,
                Math.max(1, Math.round(cityRadius * 0.25f)),
                cityRadius,
                blockSize,
                Math.round(cityRadius * 1.6f),
                cityRadius * 2,
                waypointCount,
                Math.round(cityRadius * 3.2f),
                districtColors,
                streetStyle);
    }

    /** The tuning that makes these settings, if any does (settings written by hand may not fit the scaling). */
    public static Optional<GridTuning> fromSettings(GridLayoutSettings settings) {
        GridTuning tuning = new GridTuning(settings.gridSeed(), Math.round(settings.corruption() * 100), settings.cityRadius(),
                settings.blockSize(), settings.waypointCount(), settings.districtColors(), settings.streetStyle());
        return tuning.isValid() && tuning.toSettings().equals(settings) ? Optional.of(tuning) : Optional.empty();
    }

    public String code() {
        ByteBuffer buffer = ByteBuffer.allocate(15 + districtColors.size());
        buffer.put((byte) 2)
                .putLong(seed)
                .put((byte) corruptionPercent)
                .put((byte) cityRadius)
                .put((byte) blockSize)
                .put((byte) waypointCount)
                .put((byte) districtColors.size());
        districtColors.forEach(color -> buffer.put((byte) color.getId()));
        buffer.put((byte) streetStyle.ordinal());
        return V2_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    /** Decodes a grid code; empty if it's malformed or out of bounds. */
    public static Optional<GridTuning> fromCode(String code) {
        String trimmed = code.trim();
        int version;
        if (trimmed.startsWith(V2_PREFIX)) {
            version = 2;
        } else if (trimmed.startsWith(V1_PREFIX)) {
            version = 1;
        } else {
            return Optional.empty();
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(trimmed.substring(V2_PREFIX.length())));
            if (buffer.get() != version) {
                return Optional.empty();
            }
            long seed = buffer.getLong();
            int corruption = buffer.get();
            int city = buffer.get() & 0xFF;
            int block = buffer.get();
            int waypoints = buffer.get();
            int colorCount = buffer.get();
            if (colorCount < 0 || colorCount > MAX_DISTRICT_COLORS) {
                return Optional.empty();
            }
            List<DyeColor> colors = new ArrayList<>();
            for (int i = 0; i < colorCount; i++) {
                int id = buffer.get();
                if (id < 0 || id >= DyeColor.values().length) {
                    return Optional.empty();
                }
                colors.add(DyeColor.byId(id));
            }
            // Version 1 codes predate street styles: their grids have uniform streets
            StreetStyle style = StreetStyle.UNIFORM;
            if (version == 2) {
                int styleIndex = buffer.get();
                if (styleIndex < 0 || styleIndex >= StreetStyle.values().length) {
                    return Optional.empty();
                }
                style = StreetStyle.values()[styleIndex];
            }
            if (buffer.hasRemaining()) {
                return Optional.empty();
            }
            GridTuning tuning = new GridTuning(seed, corruption, city, block, waypoints, colors, style);
            return tuning.isValid() ? Optional.of(tuning) : Optional.empty();
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            return Optional.empty();
        }
    }

    /** A seed from whatever a player typed: a number is used as-is, anything else is hashed, like world seeds. */
    public static long parseSeed(String text) {
        String trimmed = text.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return trimmed.hashCode();
        }
    }

    public GridTuning withSeed(long seed) {
        return new GridTuning(seed, corruptionPercent, cityRadius, blockSize, waypointCount, districtColors, streetStyle);
    }

    public GridTuning withCorruptionPercent(int corruptionPercent) {
        return new GridTuning(seed, corruptionPercent, cityRadius, blockSize, waypointCount, districtColors, streetStyle);
    }

    public GridTuning withCityRadius(int cityRadius) {
        return new GridTuning(seed, corruptionPercent, cityRadius, blockSize, waypointCount, districtColors, streetStyle);
    }

    public GridTuning withBlockSize(int blockSize) {
        return new GridTuning(seed, corruptionPercent, cityRadius, blockSize, waypointCount, districtColors, streetStyle);
    }

    public GridTuning withWaypointCount(int waypointCount) {
        return new GridTuning(seed, corruptionPercent, cityRadius, blockSize, waypointCount, districtColors, streetStyle);
    }

    public GridTuning withDistrictColors(List<DyeColor> districtColors) {
        return new GridTuning(seed, corruptionPercent, cityRadius, blockSize, waypointCount, districtColors, streetStyle);
    }

    public GridTuning withStreetStyle(StreetStyle streetStyle) {
        return new GridTuning(seed, corruptionPercent, cityRadius, blockSize, waypointCount, districtColors, streetStyle);
    }
}
