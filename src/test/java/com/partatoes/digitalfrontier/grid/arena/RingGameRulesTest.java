package com.partatoes.digitalfrontier.grid.arena;

import org.junit.jupiter.api.Test;

import static com.partatoes.digitalfrontier.grid.arena.RingGameRules.Goal;
import static com.partatoes.digitalfrontier.grid.arena.RingGameRules.scored;
import static org.junit.jupiter.api.Assertions.*;

class RingGameRulesTest {
    // Rings at z = -14 (north) and 14 (south), centered on x = 0, y = 63, an opening of 1.7
    private static Goal through(double fromZ, double toZ, double x, double y) {
        return scored(fromZ, toZ, x, y, -14, 14, 0, 63, 1.7);
    }

    @Test
    void throughTheSouthRingIsTheChallengers() {
        assertEquals(Goal.CHALLENGER, through(13.6, 14.4, 0.5, 63.5));
    }

    @Test
    void throughTheNorthRingIsTheProgramS() {
        assertEquals(Goal.PROGRAM, through(-13.5, -14.5, 0, 62));
    }

    @Test
    void wideOrShortIsNoGoal() {
        assertEquals(Goal.NONE, through(13.6, 14.4, 3, 63), "beside the ring");
        assertEquals(Goal.NONE, through(13.6, 14.4, 0, 60), "under it");
        assertEquals(Goal.NONE, through(10, 13, 0, 63), "short of it");
    }

    @Test
    void itCountsEitherWayThrough() {
        assertEquals(Goal.CHALLENGER, through(14.5, 13.5, 0, 63));
    }

    @Test
    void threeWins() {
        assertFalse(RingGameRules.won(2));
        assertTrue(RingGameRules.won(3));
    }
}
