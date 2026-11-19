package com.partatoes.digitalfrontier.grid.arena;

import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;

import static com.partatoes.digitalfrontier.grid.arena.CycleSteering.*;
import static org.junit.jupiter.api.Assertions.*;

class CycleSteeringTest {
    // A 21×21 floor, walled round
    private static final BiPredicate<Integer, Integer> FLOOR = (x, z) -> Math.abs(x) > 10 || Math.abs(z) > 10;

    @Test
    void inTheOpenItRidesOn() {
        assertEquals(Turn.STRAIGHT, choose(FLOOR, 0, 0, NORTH, 0, -8, 8, 0.5));
    }

    @Test
    void itTurnsAwayFromAWallAhead() {
        Turn turn = choose(FLOOR, 0, -10, NORTH, 0, 0, 8, 0);
        assertNotEquals(Turn.STRAIGHT, turn);
    }

    @Test
    void itTakesTheSideWithMoreRoom() {
        // A trail runs along x = -1 next to it: left is shut
        BiPredicate<Integer, Integer> trail = FLOOR.or((x, z) -> x == -1);
        assertEquals(Turn.RIGHT, choose(trail, 0, -10, NORTH, 0, 0, 8, 0));
    }

    @Test
    void aKeenRiderTurnsTowardItsTarget() {
        assertEquals(Turn.RIGHT, choose(FLOOR, 0, 0, NORTH, 9, 0, 8, 1));
        assertEquals(Turn.LEFT, choose(FLOOR, 0, 0, NORTH, -9, 0, 8, 1));
    }

    @Test
    void headingsAndYawsAgree() {
        for (int heading = 0; heading < 4; heading++) {
            assertEquals(heading, headingOf(yawOf(heading)));
        }
        assertEquals(1, dz(SOUTH));
        assertEquals(-1, dx(WEST));
        assertEquals(EAST, Turn.RIGHT.apply(NORTH));
        assertEquals(WEST, Turn.LEFT.apply(NORTH));
        assertEquals(3, room(FLOOR, 0, 7, SOUTH, 8));
    }
}
