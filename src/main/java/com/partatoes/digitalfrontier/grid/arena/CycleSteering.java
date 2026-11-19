package com.partatoes.digitalfrontier.grid.arena;

import java.util.function.BiPredicate;

/**
 * How a program rides a light cycle in the arena's light cycle match (see {@link CycleMatch}): at each block it
 * weighs carrying on against turning left or right, by how far it could go each way before hitting a wall, and, the
 * keener it is, by how much a way closes in on its opponent. Pure, so riding can be tested without a world.
 */
public final class CycleSteering {
    /** The four headings, clockwise from north (-Z). */
    public static final int NORTH = 0;
    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DZ = {-1, 0, 1, 0};

    public enum Turn {
        LEFT(-1), STRAIGHT(0), RIGHT(1);

        private final int step;

        Turn(int step) {
            this.step = step;
        }

        /** The heading after this turn from another. */
        public int apply(int heading) {
            return Math.floorMod(heading + step, 4);
        }
    }

    private CycleSteering() {
    }

    public static int dx(int heading) {
        return DX[heading];
    }

    public static int dz(int heading) {
        return DZ[heading];
    }

    /**
     * Which way to go from a block, heading somewhere.
     *
     * @param blocked    whether a block (x, z) is a wall, a trail or off the floor
     * @param lookahead  how far ahead it looks
     * @param aggression 0 to 1: how much it weighs closing in on its target against keeping clear room
     */
    public static Turn choose(BiPredicate<Integer, Integer> blocked, int x, int z, int heading, int targetX, int targetZ,
                              int lookahead, double aggression) {
        Turn best = Turn.STRAIGHT;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Turn turn : new Turn[]{Turn.STRAIGHT, Turn.LEFT, Turn.RIGHT}) {
            int way = turn.apply(heading);
            int room = room(blocked, x, z, way, lookahead);
            double score = room;
            if (room > 1) {
                // Closer to the target one step on is better, by as much as it cares
                double now = Math.abs(targetX - x) + Math.abs(targetZ - z);
                double after = Math.abs(targetX - (x + DX[way])) + Math.abs(targetZ - (z + DZ[way]));
                score += aggression * (now - after) * 2;
            }
            if (score > bestScore) {
                bestScore = score;
                best = turn;
            }
        }
        return best;
    }

    /** How many clear blocks lie ahead in a heading, up to the lookahead. */
    public static int room(BiPredicate<Integer, Integer> blocked, int x, int z, int heading, int lookahead) {
        int free = 0;
        for (int step = 1; step <= lookahead; step++) {
            if (blocked.test(x + DX[heading] * step, z + DZ[heading] * step)) {
                break;
            }
            free++;
        }
        return free;
    }

    /** The heading nearest a yaw, in degrees (yaw 0 faces south, as entities do). */
    public static int headingOf(float yaw) {
        int quarter = Math.floorMod(Math.round(yaw / 90f), 4);
        // Yaw 0 is south, 90 west, 180 north, 270 east
        return switch (quarter) {
            case 0 -> SOUTH;
            case 1 -> WEST;
            case 2 -> NORTH;
            default -> EAST;
        };
    }

    /** The yaw that faces a heading. */
    public static float yawOf(int heading) {
        return switch (heading) {
            case SOUTH -> 0;
            case WEST -> 90;
            case NORTH -> 180;
            default -> 270;
        };
    }
}
