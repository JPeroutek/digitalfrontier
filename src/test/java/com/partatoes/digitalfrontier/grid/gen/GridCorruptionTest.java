package com.partatoes.digitalfrontier.grid.gen;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.world.item.DyeColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridCorruptionTest {
    // Every color a corrupted cyan pattern line can show at an intensity, never jumping wheels
    private static Set<DyeColor> straysFromCyan(float intensity) {
        Set<DyeColor> colors = EnumSet.noneOf(DyeColor.class);
        for (int i = 0; i < 1000; i++) {
            colors.add(GridCorruption.strayColor(DyeColor.CYAN, intensity, i / 1000.0, 0.99));
        }
        return colors;
    }

    @Test
    void firstStraysAreToNeighbouringColors() {
        assertEquals(EnumSet.of(DyeColor.GREEN, DyeColor.LIGHT_BLUE), straysFromCyan(0.45f));
    }

    @Test
    void higherCorruptionReachesFurtherRoundTheWheel() {
        Set<DyeColor> unstable = straysFromCyan(0.7f);
        assertTrue(unstable.size() > 2 && unstable.size() < 10, unstable::toString);
        Set<DyeColor> full = straysFromCyan(1f);
        assertEquals(10, full.size(), full::toString);
        assertTrue(!full.contains(DyeColor.CYAN) && !full.contains(DyeColor.WHITE), full::toString);
    }

    @Test
    void onlyHighCorruptionJumpsBetweenHuesAndNeutrals() {
        assertEquals(DyeColor.LIGHT_BLUE, GridCorruption.strayColor(DyeColor.CYAN, 0.45f, 0, 0));
        assertEquals(DyeColor.WHITE, GridCorruption.strayColor(DyeColor.CYAN, 1f, 0, 0));
        assertEquals(DyeColor.RED, GridCorruption.strayColor(DyeColor.GRAY, 1f, 0, 0));
    }
}
