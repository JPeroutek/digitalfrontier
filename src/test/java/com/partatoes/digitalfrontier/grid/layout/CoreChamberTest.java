package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreChamberTest {
    @Test
    void theShaftComesDownFromTheCityCenterIntoTheChamber() {
        // Inside chunk (0, 0), the crossing of the central avenues
        assertEquals(0, CoreChamber.SHAFT_MIN >> 4);
        assertEquals(0, CoreChamber.SHAFT_MAX >> 4);
        assertTrue(CoreChamber.inChamber(CoreChamber.SHAFT_MIN, CoreChamber.TOP, CoreChamber.SHAFT_MIN), "the shaft opens into the chamber");
        assertEquals(CoreChamber.TOP + 1, CoreChamber.SEAL_Y, "the seal sits in the chamber's ceiling");
        assertTrue(CoreChamber.TOP < GridLayout.FLOOR_Y - 20, "below the subway");
        assertTrue(CoreChamber.FLOOR > -64, "above the world's bottom");
    }

    @Test
    void theCoreStandsInTheChamberClearOfTheLadder() {
        BlockPos core = CoreChamber.core();
        assertTrue(CoreChamber.inChamber(core.getX(), core.getY(), core.getZ()));
        assertEquals(CoreChamber.FLOOR, core.getY());
        // Its 2-block-wide body, and room to fight around it, stay in the chamber and away from the shaft's foot
        assertTrue(CoreChamber.inChamber(core.getX() - 6, core.getY(), core.getZ() + 6));
        assertTrue(core.getZ() - CoreChamber.SHAFT_MAX > 10);
        assertTrue(CoreChamber.touches(core.getX() >> 4, core.getZ() >> 4));
        assertFalse(CoreChamber.touches(5, 0));
    }
}
