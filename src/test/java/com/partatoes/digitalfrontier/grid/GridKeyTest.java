package com.partatoes.digitalfrontier.grid;

import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridKeyTest {
    private static GridLayoutSettings withSeed(long seed) {
        GridLayoutSettings d = GridLayoutSettings.DEFAULT;
        return new GridLayoutSettings(seed, d.corruption(), d.coreRadius(), d.cityRadius(), d.blockSize(),
                d.outskirtsRadius(), d.waypointRingRadius(), d.waypointCount(), d.outlandsRadius(), d.districtColors());
    }

    @Test
    void defaultSettingsMapToDefault() {
        assertEquals(Identifier.of("digitalfrontier", "default"), GridKey.of(GridLayoutSettings.DEFAULT));
    }

    @Test
    void tunedGridIdsAreStableAndDistinct() {
        Identifier first = GridKey.of(withSeed(42L));
        assertEquals(first, GridKey.of(withSeed(42L)));
        assertTrue(first.getPath().matches("0x[0-9a-f]{8}"), first.toString());
        assertNotEquals(first, GridKey.of(withSeed(43L)));
    }
}
