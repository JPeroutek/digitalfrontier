package com.partatoes.digitalfrontier.entity.custom;

/**
 * What a companion Bool says: "no" when danger is near its owner, "yes" when its owner looks toward where they
 * should go, otherwise nothing. Pure, so the geometry can be tested without a world.
 */
public final class BoolSignals {
    /** How far either side of the goal the owner can look and still get a "yes", in degrees. */
    public static final float YES_CONE = 15;

    public enum Signal {
        IDLE, YES, NO;

        static Signal byId(int id) {
            Signal[] values = values();
            return id >= 0 && id < values.length ? values[id] : IDLE;
        }
    }

    private BoolSignals() {
    }

    /**
     * @param threatNear whether something hostile is near the owner; it takes priority
     * @param dx         the goal's offset from the owner along X, or NaN with no goal
     * @param dz         the goal's offset along Z
     * @param yaw        where the owner is looking, as Minecraft yaw (0 is +Z, 90 is -X)
     */
    public static Signal signal(boolean threatNear, double dx, double dz, float yaw) {
        if (threatNear) {
            return Signal.NO;
        }
        if (Double.isNaN(dx) || Double.isNaN(dz) || dx == 0 && dz == 0) {
            return Signal.IDLE;
        }
        return Math.abs(offBy(bearing(dx, dz), yaw)) <= YES_CONE ? Signal.YES : Signal.IDLE;
    }

    /** The yaw that faces along this offset. */
    static double bearing(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    // The signed difference between two yaws, in [-180, 180)
    static double offBy(double bearing, double yaw) {
        double difference = (bearing - yaw) % 360;
        if (difference < -180) {
            difference += 360;
        } else if (difference >= 180) {
            difference -= 360;
        }
        return difference;
    }
}
