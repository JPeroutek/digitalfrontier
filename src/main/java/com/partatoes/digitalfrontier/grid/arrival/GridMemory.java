package com.partatoes.digitalfrontier.grid.arrival;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a player remembers about the grids: the terminals they've discovered in each grid, and the terminal they
 * last arrived at (where Memory Paging will respawn them). Stored on the player and kept through death.
 */
public record GridMemory(Map<Identifier, List<String>> discovered, Optional<TerminalRef> lastArrival) {
    public static final GridMemory EMPTY = new GridMemory(Map.of(), Optional.empty());

    public static final Codec<GridMemory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, Codec.STRING.listOf()).optionalFieldOf("discovered", Map.of()).forGetter(GridMemory::discovered),
            TerminalRef.CODEC.optionalFieldOf("last_arrival").forGetter(GridMemory::lastArrival)
    ).apply(instance, GridMemory::new));

    public static final AttachmentType<GridMemory> TYPE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid_memory"),
            builder -> builder.persistent(CODEC).copyOnDeath().initializer(() -> EMPTY));

    /** Terminal ids discovered in a grid, in the order they were found. */
    public List<String> discovered(Identifier grid) {
        return discovered.getOrDefault(grid, List.of());
    }

    public boolean hasDiscovered(TerminalRef terminal) {
        return discovered(terminal.grid()).contains(terminal.terminal());
    }

    public GridMemory withDiscovered(TerminalRef terminal) {
        if (hasDiscovered(terminal)) {
            return this;
        }
        Map<Identifier, List<String>> grids = new HashMap<>(discovered);
        List<String> terminals = new ArrayList<>(discovered(terminal.grid()));
        terminals.add(terminal.terminal());
        grids.put(terminal.grid(), List.copyOf(terminals));
        return new GridMemory(Map.copyOf(grids), lastArrival);
    }

    public GridMemory withArrival(TerminalRef terminal) {
        GridMemory memory = withDiscovered(terminal);
        return new GridMemory(memory.discovered, Optional.of(terminal));
    }

    /** Called once at startup so the attachment type is registered before any player data loads. */
    public static void register() {
    }
}
