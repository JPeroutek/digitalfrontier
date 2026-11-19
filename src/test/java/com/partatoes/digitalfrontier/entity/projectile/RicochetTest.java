package com.partatoes.digitalfrontier.entity.projectile;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RicochetTest {
    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 1e-9, "x");
        assertEquals(expected.y, actual.y, 1e-9, "y");
        assertEquals(expected.z, actual.z, 1e-9, "z");
    }

    @Test
    void mirrorsAcrossTheFaceItHits() {
        Vec3 velocity = new Vec3(1, 0.2, -0.5);
        double k = Ricochet.KEPT;
        assertVec(new Vec3(-k, 0.2 * k, -0.5 * k), Ricochet.reflect(velocity, Direction.WEST));
        assertVec(new Vec3(-k, 0.2 * k, -0.5 * k), Ricochet.reflect(velocity, Direction.EAST));
        assertVec(new Vec3(k, -0.2 * k, -0.5 * k), Ricochet.reflect(velocity, Direction.UP));
        assertVec(new Vec3(k, 0.2 * k, 0.5 * k), Ricochet.reflect(velocity, Direction.SOUTH));
    }

    @Test
    void headOnItComesStraightBack() {
        Vec3 back = Ricochet.reflect(new Vec3(0, 0, 2), Direction.NORTH);
        assertVec(new Vec3(0, 0, -2 * Ricochet.KEPT), back);
    }

    @Test
    void itSlowsWithEveryBounceButNeverSpeedsUp() {
        Vec3 velocity = new Vec3(0.7, -0.3, 1.1);
        for (Direction face : Direction.values()) {
            assertTrue(Ricochet.reflect(velocity, face).length() < velocity.length(), face.toString());
        }
    }
}
