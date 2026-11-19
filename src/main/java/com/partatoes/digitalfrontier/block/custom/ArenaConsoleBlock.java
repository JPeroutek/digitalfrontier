package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.grid.arena.ArenaManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The arena's console, by the gate: use it to see the games on offer and step in (see {@link ArenaManager}). */
public class ArenaConsoleBlock extends Block {
    public ArenaConsoleBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            ArenaManager.showMenu(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
