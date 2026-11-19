package com.partatoes.digitalfrontier.entity.custom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecayTest {
    // How many steps until a program at 0 decay turns, or -1 if it never does within a day
    private static int stepsToTurn(float ground, int corrupted) {
        float decay = 0;
        for (int step = 1; step <= 24000 / Decay.INTERVAL; step++) {
            decay = Decay.step(decay, ground, corrupted);
            if (decay >= 1) {
                return step;
            }
        }
        return -1;
    }

    @Test
    void cleanAndLightlyDecayedGroundIsSafe() {
        assertEquals(-1, stepsToTurn(0f, 0));
        assertEquals(-1, stepsToTurn(0.3f, 0), "up to the edge of LOW");
        assertEquals(0f, Decay.step(0.5f - 0.5f, 0.1f, 0));
        assertTrue(Decay.step(0.5f, 0.1f, 0) < 0.5f, "clean ground lets a program recover");
    }

    @Test
    void worseGroundTurnsProgramsFaster() {
        int unstable = stepsToTurn(0.5f, 0);
        int corruptedGround = stepsToTurn(0.8f, 0);
        assertTrue(unstable > 0 && corruptedGround > 0);
        assertTrue(corruptedGround < unstable);
        // In minutes, not seconds: a player passing through has time to lead programs out
        assertTrue(corruptedGround * Decay.INTERVAL > 20 * 60, "at least a minute on corrupted ground");
        assertTrue(unstable * Decay.INTERVAL < 24000, "within a day on unstable ground");
    }

    @Test
    void corruptedCompanyIsCatching() {
        assertTrue(stepsToTurn(0f, 3) > 0, "even on clean ground, enough corrupted programs turn one");
        assertTrue(stepsToTurn(0.5f, 2) < stepsToTurn(0.5f, 0));
    }

    @Test
    void itShowsOnlyOnceItsVisible() {
        assertEquals(0f, Decay.jitter(0.2f));
        assertTrue(Decay.jitter(0.5f) > 0);
        assertEquals(0.6f, Decay.jitter(1f), 1e-6);
    }
}
