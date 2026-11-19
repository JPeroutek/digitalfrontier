package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.core.BlockPos;

/**
 * Where the System Core sleeps: a chamber deep under the city center, below the subway, reached by a shaft down from
 * the central intersection. The shaft's foot is closed by a core seal until the regime's last lieutenant falls. Every
 * grid has one in the same place, so this is fixed geometry, in block coordinates.
 */
public final class CoreChamber {
    /** The chamber's interior spans these X and Z, inclusive: the three chunks around chunk (0, 0) each way. */
    public static final int MIN = -16;
    public static final int MAX = 31;
    /** Its floor is the layer below this; its interior runs up to {@link #TOP}. */
    public static final int FLOOR = 8;
    public static final int TOP = 23;
    /** The shaft's interior spans these X and Z, inclusive, from the chamber's ceiling up through the street. */
    public static final int SHAFT_MIN = 6;
    public static final int SHAFT_MAX = 9;
    /** The seal fills the shaft at this Y, in the chamber's ceiling. */
    public static final int SEAL_Y = TOP + 1;
    /** The ladder runs down the shaft's west wall, at this X, from the street to the chamber floor. */
    public static final int LADDER_X = SHAFT_MIN;
    /** And the pillar it leans on in the chamber, at this X. */
    public static final int PILLAR_X = SHAFT_MIN - 1;

    private CoreChamber() {
    }

    /** Where the core stands: on the chamber floor, 16 blocks south of the foot of the shaft. */
    public static BlockPos core() {
        return new BlockPos((SHAFT_MIN + SHAFT_MAX + 1) / 2, FLOOR, (SHAFT_MIN + SHAFT_MAX + 1) / 2 + 16);
    }

    public static boolean inChamber(int x, int y, int z) {
        return x >= MIN && x <= MAX && z >= MIN && z <= MAX && y >= FLOOR && y <= TOP;
    }

    public static boolean inShaft(int x, int z) {
        return x >= SHAFT_MIN && x <= SHAFT_MAX && z >= SHAFT_MIN && z <= SHAFT_MAX;
    }

    /** Whether a chunk holds any part of the chamber or shaft. */
    public static boolean touches(int chunkX, int chunkZ) {
        return chunkX >= MIN >> 4 && chunkX <= MAX >> 4 && chunkZ >= MIN >> 4 && chunkZ <= MAX >> 4;
    }
}
