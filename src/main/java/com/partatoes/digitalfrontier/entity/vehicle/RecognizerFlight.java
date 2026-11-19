package com.partatoes.digitalfrontier.entity.vehicle;

import net.minecraft.world.phys.Vec3;

/**
 * How a Recognizer flies (see {@link RecognizerEntity}): heavy and slow, it turns its velocity toward where it wants
 * to be a little at a time, eases off as it arrives, and hangs in the air when it has nowhere to go. Pure, so the
 * flight can be tested.
 */
public final class RecognizerFlight {
    /** Top speed, in blocks a tick. */
    public static final double MAX_SPEED = 0.5;
    /** How much its velocity can change in a tick. */
    public static final double ACCEL = 0.04;
    // Within this distance of its goal it starts slowing, reaching zero on top of it
    private static final double EASE_DISTANCE = 6;
    private static final double HOVER_DAMPING = 0.85;

    private RecognizerFlight() {
    }

    /** The next velocity, flying from where it is toward {@code toGoal} (the goal minus its position). */
    public static Vec3 steer(Vec3 velocity, Vec3 toGoal) {
        double distance = toGoal.length();
        Vec3 desired = distance < 1.0e-3 ? Vec3.ZERO : toGoal.scale(Math.min(MAX_SPEED, MAX_SPEED * distance / EASE_DISTANCE) / distance);
        return accelerate(velocity, desired);
    }

    /**
     * The next velocity under a pilot's hands: thrust along its heading, climb or dive, or hang still with nothing
     * pressed.
     *
     * @param thrust  -1 (back) to 1 (forward)
     * @param climb   -1 (down) to 1 (up)
     * @param heading the direction it faces, flat
     */
    public static Vec3 pilot(Vec3 velocity, Vec3 heading, double thrust, double climb) {
        if (thrust == 0 && climb == 0) {
            return hover(velocity);
        }
        Vec3 desired = heading.scale(thrust * MAX_SPEED).add(0, climb * MAX_SPEED * 0.6, 0);
        return accelerate(velocity, desired);
    }

    /** The next velocity with nowhere to go: it bleeds off speed and hangs in place. */
    public static Vec3 hover(Vec3 velocity) {
        Vec3 slowed = velocity.scale(HOVER_DAMPING);
        return slowed.lengthSqr() < 1.0e-6 ? Vec3.ZERO : slowed;
    }

    private static Vec3 accelerate(Vec3 velocity, Vec3 desired) {
        Vec3 change = desired.subtract(velocity);
        double length = change.length();
        if (length > ACCEL) {
            change = change.scale(ACCEL / length);
        }
        return velocity.add(change);
    }
}
