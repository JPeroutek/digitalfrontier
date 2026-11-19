package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArenaGeometryTest {
    private final ArenaGeometry arena = new ArenaGeometry(new Site(Site.Type.ARENA, "arena_0", 10, -4));

    @Test
    void itsCenteredOnItsSite() {
        assertEquals(10 * 16 + 8, arena.centerX());
        assertEquals(-4 * 16 + 8, arena.centerZ());
        assertTrue(arena.touches(10, -4));
        assertTrue(arena.touches(11, -3));
        assertFalse(arena.touches(13, -4), "three chunks wide, no more");
    }

    @Test
    void theStartsAreOnTheFloorAndTheTilesHoldThemUp() {
        BlockPos start = arena.challengerStart();
        assertEquals(ArenaGeometry.Part.AIR, arena.partAt(start.getX(), start.getY(), start.getZ()));
        assertNotNull(arena.partAt(start.getX(), start.getY() - 1, start.getZ()));
        assertNotEquals(ArenaGeometry.Part.AIR, arena.partAt(start.getX(), start.getY() - 1, start.getZ()));
        assertEquals(ArenaGeometry.Part.TILE, arena.partAt(arena.centerX(), ArenaGeometry.FLOOR, arena.centerZ()));
        assertEquals(ArenaGeometry.Part.AIR, arena.partAt(arena.centerX(), ArenaGeometry.FLOOR - 1, arena.centerZ()), "the pit under the tiles");
        assertEquals(13 * 13, arena.tilesOutsideIn().size());
    }

    @Test
    void theConsoleAndTheStandsAreWhereAPlayerCanStand() {
        BlockPos console = arena.console();
        assertEquals(ArenaGeometry.Part.CONSOLE, arena.partAt(console.getX(), console.getY(), console.getZ()));
        assertEquals(ArenaGeometry.Part.STAND, arena.partAt(console.getX(), console.getY() - 1, console.getZ()));
        BlockPos stands = arena.stands();
        assertEquals(ArenaGeometry.Part.AIR, arena.partAt(stands.getX(), stands.getY(), stands.getZ()));
        assertEquals(ArenaGeometry.Part.STAND, arena.partAt(stands.getX(), stands.getY() - 1, stands.getZ()));
    }

    @Test
    void theGateIsOpenAndTheWallElsewhereIsNot() {
        int wallZ = arena.centerZ() - ArenaGeometry.WALL;
        assertEquals(ArenaGeometry.Part.AIR, arena.partAt(arena.centerX(), GridLayout.FLOOR_Y + 1, wallZ));
        assertEquals(ArenaGeometry.Part.WALL, arena.partAt(arena.centerX() + 5, GridLayout.FLOOR_Y + 1, wallZ));
    }

    @Test
    void eachEndHasAGoalRing() {
        BlockPos north = arena.ring(true);
        assertEquals(ArenaGeometry.Part.RING, arena.partAt(north.getX() + ArenaGeometry.RING_RADIUS, north.getY(), north.getZ()));
        assertEquals(ArenaGeometry.Part.AIR, arena.partAt(north.getX(), north.getY(), north.getZ()), "open through the middle");
        BlockPos south = arena.ring(false);
        assertEquals(ArenaGeometry.Part.RING, arena.partAt(south.getX(), south.getY() + ArenaGeometry.RING_RADIUS, south.getZ()));
    }
}
