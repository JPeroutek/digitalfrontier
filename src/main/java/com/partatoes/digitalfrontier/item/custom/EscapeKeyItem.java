package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.grid.exit.GridExit;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** The emergency exit: single use, takes the player out of the grid from anywhere in it. */
public class EscapeKeyItem extends Item {
    public EscapeKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!ModDimensions.isGrid(world)) {
            if (user instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable("message.digitalfrontier.escape_key_outside_grid").formatted(Formatting.RED), true);
            }
            return ActionResult.FAIL;
        }
        if (user instanceof ServerPlayerEntity player) {
            player.getStackInHand(hand).decrementUnlessCreative(1, player);
            GridExit.toRealWorld(player);
        }
        return ActionResult.SUCCESS;
    }
}
