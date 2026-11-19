package com.partatoes.digitalfrontier.entity.projectile;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** How a thrown disc comes off a surface. Pure, so it can be tested without a world. */
public final class Ricochet {
    /** The share of its speed a disc keeps through each bounce. */
    public static final double KEPT = 0.9;

    private Ricochet() {
    }

    /**
     * The velocity after bouncing off a block face: mirrored across the face (the part along the face's axis flips),
     * and a little slower.
     */
    public static Vec3 reflect(Vec3 velocity, Direction face) {
        Vec3 mirrored = switch (face.getAxis()) {
            case X -> new Vec3(-velocity.x, velocity.y, velocity.z);
            case Y -> new Vec3(velocity.x, -velocity.y, velocity.z);
            case Z -> new Vec3(velocity.x, velocity.y, -velocity.z);
        };
        return mirrored.scale(KEPT);
    }
}
