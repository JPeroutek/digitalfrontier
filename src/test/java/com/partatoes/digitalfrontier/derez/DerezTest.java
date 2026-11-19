package com.partatoes.digitalfrontier.derez;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DerezTest {
    @Test
    void ownMobsDerezAnywhereAndEverythingDerezzesInAGrid() {
        assertTrue(Derez.derezzes(true, false, false));
        assertTrue(Derez.derezzes(false, true, false));
        assertFalse(Derez.derezzes(false, false, false), "a zombie in the overworld dies as usual");
        assertFalse(Derez.derezzes(false, true, true), "the Ender Dragon keeps its own death");
    }

    @Test
    void everyStyleSpendsItsWholeBudget() {
        for (DerezStyle style : DerezStyle.values()) {
            if (style.statue()) {
                continue;
            }
            for (int budget : new int[]{Derez.MIN_CUBES, 61, Derez.MAX_CUBES}) {
                int total = 0;
                for (int tick = 0; tick <= Derez.TICKS + 5; tick++) {
                    total += style.cubesThisTick(tick, budget);
                }
                assertEquals(budget, total, style + " with " + budget);
            }
        }
    }

    @Test
    void burstsComeAllAtOnce() {
        assertEquals(100, DerezStyle.INSTANT.cubesThisTick(1, 100));
        assertEquals(0, DerezStyle.INSTANT.cubesThisTick(2, 100));
        assertEquals(0, DerezStyle.FREEZE.cubesThisTick(9, 100));
        assertEquals(100, DerezStyle.FREEZE.cubesThisTick(10, 100));
        // A sweep sheds a few every tick
        for (int tick = 1; tick <= Derez.TICKS; tick++) {
            int cubes = DerezStyle.SWEEP.cubesThisTick(tick, 100);
            assertTrue(cubes == 5, "tick " + tick + ": " + cubes);
        }
    }

    @Test
    void theSweepMovesDownTheBody() {
        float previousTop = 2;
        for (int tick = 1; tick <= Derez.TICKS; tick++) {
            float[] band = DerezStyle.SWEEP.band(tick);
            assertTrue(band[0] >= 0 && band[0] < band[1] && band[1] <= 1);
            assertTrue(band[1] < previousTop);
            previousTop = band[1];
        }
        assertEquals(1, DerezStyle.SWEEP.band(1)[1], 1e-6);
        assertEquals(0, DerezStyle.SWEEP.band(Derez.TICKS)[0], 1e-6);
        assertArrayEquals(new float[]{0, 1}, DerezStyle.INSTANT.band(5));
    }

    @Test
    void bodiesGoWhenTheirStyleSays() {
        assertTrue(DerezStyle.SWEEP.bodyVisible(0.9f));
        assertEquals(0.25f, DerezStyle.SWEEP.bodyAlpha(0.75f), 1e-6);
        assertFalse(DerezStyle.INSTANT.bodyVisible(0.05f));
        assertTrue(DerezStyle.FREEZE.bodyVisible(0.4f));
        assertFalse(DerezStyle.FREEZE.bodyVisible(0.5f));
        assertTrue(DerezStyle.FREEZE.whiteFlash(0.2f) > 0);
        assertEquals(0, DerezStyle.FREEZE.whiteFlash(0.6f));
        assertEquals(0, DerezStyle.SWEEP.whiteFlash(0.2f));
        assertEquals(1, DerezStyle.INSTANT.bodyAlpha(0.5f));
    }

    @Test
    void aStatueStandsThenComesApartFromTheHeadDown() {
        assertTrue(DerezStyle.VOXEL.statue());
        assertEquals(0, DerezStyle.VOXEL.cubesThisTick(1, 100), "a statue sheds no loose cubes");
        assertTrue(DerezStyle.VOXEL.bodyVisible(0), "the body shows until its statue forms");
        assertFalse(DerezStyle.VOXEL.bodyVisible(0.1f));
        assertEquals(Derez.STATUE_HOLD, Derez.releaseDelay(1));
        assertEquals(Derez.STATUE_HOLD + Derez.STATUE_WAVE, Derez.releaseDelay(0));
        assertTrue(Derez.releaseDelay(0.8f) < Derez.releaseDelay(0.3f), "the head goes before the feet");
    }

    @Test
    void statueCubesGrowWithTheModel() {
        // A humanoid has about 6.5 square blocks of surface: two-pixel cubes, about 400 of them
        assertEquals(Derez.MIN_STATUE_CUBE, Derez.statueCube(6.5));
        // The System Core's 2× model would need too many at that size
        float core = Derez.statueCube(60);
        assertTrue(core > Derez.MIN_STATUE_CUBE);
        assertTrue(60 / (core * core) <= Derez.MAX_STATUE_CUBES + 1);
    }

    @Test
    void biggerBodiesShedMoreCubes() {
        int program = Derez.budget(0.6f, 1.8f);
        assertTrue(program > 40 && program < 70, "program: " + program);
        assertEquals(Derez.MAX_CUBES, Derez.budget(2, 4), "the System Core is capped");
        assertEquals(Derez.MIN_CUBES, Derez.budget(0.4f, 0.4f), "even a Bool sheds some");
        assertEquals(1, Derez.progress(30));
        assertEquals(0.5f, Derez.progress(10), 1e-6);
    }
}
