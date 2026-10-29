package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.exit.GridExit;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

// Creative/debug tool: jumps to the default grid's main arrival terminal and back, skipping the Digitizing Laser
public class DimensionTeleporterItem extends Item {
    private static final int COOLDOWN_TICKS = 20;

    public DimensionTeleporterItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        if (ModDimensions.isGrid(world)) {
            player.teleport(GridExit.worldSpawn(player));
        } else if (!GridArrival.teleport(player, new TerminalRef(ModDimensions.DEFAULT_GRID_ID, GridLayout.MAIN_ARRIVAL_ID))) {
            return InteractionResult.FAIL;
        }

        player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
