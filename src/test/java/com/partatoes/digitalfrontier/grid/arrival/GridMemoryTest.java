package com.partatoes.digitalfrontier.grid.arrival;

import com.mojang.serialization.JsonOps;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GridMemoryTest {
    private static final Identifier DEFAULT = Identifier.of("digitalfrontier", "default");
    private static final Identifier TUNED = Identifier.of("digitalfrontier", "0x12cbf34a");

    @Test
    void discoveriesAreKeptPerGridInOrder() {
        GridMemory memory = GridMemory.EMPTY
                .withDiscovered(new TerminalRef(DEFAULT, "waypoint_2"))
                .withDiscovered(new TerminalRef(DEFAULT, "city_main"))
                .withDiscovered(new TerminalRef(DEFAULT, "waypoint_2"))
                .withDiscovered(new TerminalRef(TUNED, "city_main"));
        assertEquals(List.of("waypoint_2", "city_main"), memory.discovered(DEFAULT));
        assertEquals(List.of("city_main"), memory.discovered(TUNED));
        assertTrue(memory.lastArrival().isEmpty());
    }

    @Test
    void arrivingDiscoversAndRemembersTheTerminal() {
        TerminalRef terminal = new TerminalRef(DEFAULT, "waypoint_0");
        GridMemory memory = GridMemory.EMPTY.withArrival(terminal);
        assertTrue(memory.hasDiscovered(terminal));
        assertEquals(terminal, memory.lastArrival().orElseThrow());
    }

    @Test
    void codecRoundTrips() {
        GridMemory memory = GridMemory.EMPTY
                .withDiscovered(new TerminalRef(TUNED, "waypoint_1"))
                .withArrival(new TerminalRef(DEFAULT, "city_main"));
        GridMemory decoded = GridMemory.CODEC.parse(JsonOps.INSTANCE,
                GridMemory.CODEC.encodeStart(JsonOps.INSTANCE, memory).getOrThrow()).getOrThrow();
        assertEquals(memory, decoded);
    }
}
