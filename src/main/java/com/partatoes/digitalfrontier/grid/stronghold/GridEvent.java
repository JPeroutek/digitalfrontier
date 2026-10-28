package com.partatoes.digitalfrontier.grid.stronghold;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Something that happened in (or to) a grid, which event triggers can count toward a lieutenant's respawn.
 * Events with a position can be limited to a radius around the stronghold.
 */
public sealed interface GridEvent {
    /** Where it happened, for triggers limited to a radius around the stronghold; null if it has no position. */
    @Nullable
    default BlockPos pos() {
        return null;
    }

    record GridEntered(boolean newPlayer) implements GridEvent {
    }

    /** A player came near a stronghold after nobody had been near it for {@code absence} ticks. */
    record StrongholdEntered(long absence) implements GridEvent {
    }

    record ItemCrafted(ItemStack stack) implements GridEvent {
    }

    record ItemUsed(ItemStack stack, BlockPos pos) implements GridEvent {
    }

    record BlockPlaced(BlockState state, BlockPos pos) implements GridEvent {
    }

    record BlockBroken(BlockState state, BlockPos pos) implements GridEvent {
    }

    /** A Stronghold Uplink was powered. */
    record Redstone(BlockPos pos) implements GridEvent {
    }

    record LieutenantDefeated(Identifier lieutenant, String stronghold) implements GridEvent {
    }

    record PlayerDerezzed(BlockPos pos) implements GridEvent {
    }

    record PortalUsed() implements GridEvent {
    }

    record ProgramDerezzed(BlockPos pos) implements GridEvent {
    }

    record MemoryPaged() implements GridEvent {
    }

    /** Fired by {@code /grid lieutenant trigger}, e.g. from a datapack function. */
    record Function(@Nullable String signal) implements GridEvent {
    }
}
