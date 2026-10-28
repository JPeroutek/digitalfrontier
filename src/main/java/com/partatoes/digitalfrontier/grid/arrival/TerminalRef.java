package com.partatoes.digitalfrontier.grid.arrival;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

/**
 * A terminal in a particular grid. The grid id is the grid's dimension id, e.g. {@code digitalfrontier:default},
 * and the terminal id is a {@link com.partatoes.digitalfrontier.grid.layout.Site} id such as {@code city_main}.
 */
public record TerminalRef(Identifier grid, String terminal) {
    public static final Codec<TerminalRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("grid").forGetter(TerminalRef::grid),
            Codec.STRING.fieldOf("terminal").forGetter(TerminalRef::terminal)
    ).apply(instance, TerminalRef::new));
}
