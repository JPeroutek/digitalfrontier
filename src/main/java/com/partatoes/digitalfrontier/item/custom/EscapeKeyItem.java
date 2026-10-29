package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.grid.exit.GridExit;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The emergency exit: single use, takes the player out of the grid from anywhere in it. */
public class EscapeKeyItem extends Item {
    public EscapeKeyItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (!ModDimensions.isGrid(world)) {
            if (user instanceof ServerPlayer player) {
                player.sendOverlayMessage(Component.translatable("message.digitalfrontier.escape_key_outside_grid").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        if (user instanceof ServerPlayer player) {
            player.getItemInHand(hand).consume(1, player);
            GridExit.toRealWorld(player);
        }
        return InteractionResult.SUCCESS;
    }
}
