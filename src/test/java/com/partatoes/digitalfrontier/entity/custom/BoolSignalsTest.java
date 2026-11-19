package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.entity.custom.BoolSignals.Signal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoolSignalsTest {
    @Test
    void bearingsFollowMinecraftYaw() {
        // Yaw 0 faces +Z, 90 faces -X, 180 faces -Z, -90 faces +X
        assertEquals(0, BoolSignals.bearing(0, 10), 1e-9);
        assertEquals(90, BoolSignals.bearing(-10, 0), 1e-9);
        assertEquals(180, Math.abs(BoolSignals.bearing(0, -10)), 1e-9);
        assertEquals(-90, BoolSignals.bearing(10, 0), 1e-9);
    }

    @Test
    void yesWithinTheConeAroundTheGoal() {
        // The goal lies along +Z
        assertEquals(Signal.YES, BoolSignals.signal(false, 0, 100, 0));
        assertEquals(Signal.YES, BoolSignals.signal(false, 0, 100, 14));
        assertEquals(Signal.YES, BoolSignals.signal(false, 0, 100, -14));
        assertEquals(Signal.IDLE, BoolSignals.signal(false, 0, 100, 16));
        assertEquals(Signal.IDLE, BoolSignals.signal(false, 0, 100, 180));
    }

    @Test
    void yawsWrapAround() {
        // Facing -Z is yaw 180, -180 or 540, all the same
        assertEquals(Signal.YES, BoolSignals.signal(false, 0, -100, 180));
        assertEquals(Signal.YES, BoolSignals.signal(false, 0, -100, -180));
        assertEquals(Signal.YES, BoolSignals.signal(false, 0, -100, 540));
        assertEquals(Signal.YES, BoolSignals.signal(false, 0, -100, 170));
        assertEquals(Signal.YES, BoolSignals.signal(false, 0, -100, -170));
        assertEquals(Signal.IDLE, BoolSignals.signal(false, 0, -100, 160));
    }

    @Test
    void dangerOutranksDirections() {
        assertEquals(Signal.NO, BoolSignals.signal(true, 0, 100, 0));
        assertEquals(Signal.NO, BoolSignals.signal(true, Double.NaN, Double.NaN, 0));
    }

    @Test
    void withoutAGoalThereIsNothingToSay() {
        assertEquals(Signal.IDLE, BoolSignals.signal(false, Double.NaN, Double.NaN, 0));
        assertEquals(Signal.IDLE, BoolSignals.signal(false, 0, 0, 0));
    }
}
