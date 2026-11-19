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

    @Test
    void staticCreepsInGradually() {
        // A chunk runs about 1500 blocks through the roll; count the static each step of decay leaves
        int chunkBlocks = 1500;
        assertEquals(0, GridCorruption.staticChance(0.45f));
        assertTrue(GridCorruption.staticChance(0.5f) * chunkBlocks < 2);
        double previous = 0;
        for (float intensity = 0.5f; intensity <= 1.0001f; intensity += 0.05f) {
            double blocks = GridCorruption.staticChance(intensity) * chunkBlocks;
            // No step of 0.05 adds more than about a tenth of a chunk's blocks at once
            assertTrue(blocks - previous < 0.11 * chunkBlocks, "jump at " + intensity);
            assertTrue(blocks >= previous);
            previous = blocks;
        }
        assertEquals(0.36, GridCorruption.staticChance(1f), 1e-6);
    }
}
