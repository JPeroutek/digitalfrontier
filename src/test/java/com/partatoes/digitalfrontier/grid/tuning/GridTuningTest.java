package com.partatoes.digitalfrontier.grid.tuning;

import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.grid.GridKey;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.StreetStyle;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.DyeColor;

import static org.junit.jupiter.api.Assertions.*;

class GridTuningTest {
    @Test
    void defaultTuningMakesTheDefaultGrid() {
        assertEquals(GridLayoutSettings.DEFAULT, GridTuning.DEFAULT.toSettings());
        assertEquals(GridTuning.DEFAULT, GridTuning.fromSettings(GridLayoutSettings.DEFAULT).orElseThrow());
        assertEquals("default", GridKey.of(GridTuning.DEFAULT.toSettings()).getPath());
    }

    @Test
    void everyTuningWithinBoundsMakesValidSettings() {
        for (int city = GridTuning.MIN_CITY_RADIUS; city <= GridTuning.MAX_CITY_RADIUS; city++) {
            for (int block = GridTuning.MIN_BLOCK_SIZE; block <= GridTuning.MAX_BLOCK_SIZE; block++) {
                for (int waypoints : GridTuning.WAYPOINT_COUNTS) {
                    StreetStyle style = StreetStyle.values()[(city + block) % StreetStyle.values().length];
                    GridTuning.Levels levels = GridTuning.Levels.values()[city % GridTuning.Levels.values().length];
                    GridTuning tuning = new GridTuning(city * 31L, city % 101, city, block, waypoints, List.of(DyeColor.RED), style, levels);
                    assertTrue(tuning.isValid());
                    GridLayoutSettings settings = tuning.toSettings();
                    // The codec's validation is what the dimension loader applies
                    assertTrue(GridLayoutSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).isSuccess(), "city " + city);
                    assertEquals(tuning, GridTuning.fromSettings(settings).orElseThrow(), "city " + city);
                }
            }
        }
    }

    @Test
    void codesRoundTrip() {
        GridTuning tuning = new GridTuning(-8_123_456_789L, 73, 96, 6, 8,
                List.of(DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.WHITE, DyeColor.BLACK, DyeColor.LIME), StreetStyle.UNIFORM, GridTuning.Levels.TRANSIT);
        String code = tuning.code();
        assertTrue(code.startsWith("DF3."));
        assertEquals(tuning, GridTuning.fromCode(code).orElseThrow());
        assertEquals(tuning, GridTuning.fromCode("  " + code + "\n").orElseThrow(), "pasted codes are trimmed");
        assertEquals(GridTuning.DEFAULT, GridTuning.fromCode(GridTuning.DEFAULT.code()).orElseThrow());
    }

    @Test
    void badCodesAreRejected() {
        String code = GridTuning.DEFAULT.code();
        assertTrue(GridTuning.fromCode("").isEmpty());
        assertTrue(GridTuning.fromCode("hello").isEmpty());
        assertTrue(GridTuning.fromCode("DF1.!!!").isEmpty());
        assertTrue(GridTuning.fromCode(code.substring(0, code.length() - 3)).isEmpty(), "truncated");
        assertTrue(GridTuning.fromCode(code + "AAAA").isEmpty(), "trailing bytes");
        String outOfBounds = GridTuning.DEFAULT.withCityRadius(200).code();
        assertTrue(GridTuning.fromCode(outOfBounds).isEmpty(), "city radius out of bounds");
        String repeatedColor = GridTuning.DEFAULT.withDistrictColors(List.of(DyeColor.RED, DyeColor.RED)).code();
        assertTrue(GridTuning.fromCode(repeatedColor).isEmpty(), "repeated district color");
    }

    @Test
    void differentTuningsGiveDifferentGrids() {
        Set<String> ids = new HashSet<>();
        for (long seed = 0; seed < 50; seed++) {
            ids.add(GridKey.of(GridTuning.DEFAULT.withSeed(seed).toSettings()).toString());
        }
        assertEquals(50, ids.size());
    }

    @Test
    void typedSeedsAreNumbersOrHashes() {
        assertEquals(12345L, GridTuning.parseSeed(" 12345 "));
        assertEquals("tron".hashCode(), GridTuning.parseSeed("tron"));
    }

    @Test
    void codesFromBeforeStreetStylesStillOpenTheSameGrids() {
        // A DF1 code shared before street styles existed, and the grid id a server gave it then
        GridTuning old = GridTuning.fromCode("DF1.AQAAAAAAAAAqUBgECAIOBQ").orElseThrow();
        assertEquals(StreetStyle.UNIFORM, old.streetStyle());
        assertEquals(GridTuning.Levels.OFF, old.levels());
        assertEquals(24, old.cityRadius());
        assertEquals("0x2e7aae69", GridKey.of(old.toSettings()).getPath());
    }

    @Test
    void uniformStreetsAreLeftOutOfTheSettingsJson() {
        // So settings saved before street styles existed encode, and hash, exactly as they did
        GridLayoutSettings uniform = GridTuning.DEFAULT.withStreetStyle(StreetStyle.UNIFORM).toSettings();
        assertFalse(GridLayoutSettings.CODEC.encodeStart(JsonOps.INSTANCE, uniform).getOrThrow().getAsJsonObject().has("street_style"));
        assertTrue(GridLayoutSettings.CODEC.encodeStart(JsonOps.INSTANCE, GridLayoutSettings.DEFAULT).getOrThrow().getAsJsonObject().has("street_style"));
        assertNotEquals(GridKey.of(uniform), GridKey.of(GridLayoutSettings.DEFAULT));
    }
}
