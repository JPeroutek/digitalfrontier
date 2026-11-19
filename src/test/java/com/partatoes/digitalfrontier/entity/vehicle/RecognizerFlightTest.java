package com.partatoes.digitalfrontier.entity.vehicle;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecognizerFlightTest {
    @Test
    void itFliesToItsGoalAndStopsThere() {
        Vec3 position = Vec3.ZERO;
        Vec3 velocity = Vec3.ZERO;
        Vec3 goal = new Vec3(40, 10, -20);
        for (int tick = 0; tick < 600; tick++) {
            velocity = RecognizerFlight.steer(velocity, goal.subtract(position));
            assertTrue(velocity.length() <= RecognizerFlight.MAX_SPEED + 1.0e-9, "never faster than its top speed");
            position = position.add(velocity);
        }
        assertTrue(position.distanceTo(goal) < 1, "arrived: " + position);
        assertTrue(velocity.length() < 0.05, "and settled: " + velocity);
    }

    @Test
    void itTurnsHeavily() {
        Vec3 velocity = new Vec3(RecognizerFlight.MAX_SPEED, 0, 0);
        Vec3 next = RecognizerFlight.steer(velocity, new Vec3(-100, 0, 0));
        assertEquals(RecognizerFlight.ACCEL, velocity.subtract(next).length(), 1.0e-9);
    }

    @Test
    void withNothingPressedItHangsInTheAir() {
        Vec3 velocity = new Vec3(0.4, -0.2, 0.1);
        for (int tick = 0; tick < 100; tick++) {
            velocity = RecognizerFlight.pilot(velocity, new Vec3(1, 0, 0), 0, 0);
        }
        assertEquals(Vec3.ZERO, velocity);
    }

    @Test
    void aPilotFliesWhereItFaces() {
        Vec3 velocity = Vec3.ZERO;
        for (int tick = 0; tick < 50; tick++) {
            velocity = RecognizerFlight.pilot(velocity, new Vec3(0, 0, 1), 1, 1);
        }
        assertEquals(RecognizerFlight.MAX_SPEED, velocity.z, 1.0e-6);
        assertTrue(velocity.y > 0);
        assertEquals(0, velocity.x, 1.0e-9);
    }
}
