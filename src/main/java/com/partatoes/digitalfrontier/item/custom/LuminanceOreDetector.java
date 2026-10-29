package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.block.ModBlocks;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

public class LuminanceOreDetector extends Item {

    public LuminanceOreDetector(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if(!context.getLevel().isClientSide()) {
            BlockPos positionClicked = context.getClickedPos();
            Player player = context.getPlayer();

            for(int i = 0; i <= positionClicked.getY() + 64; i++) {
                BlockState state = context.getLevel().getBlockState(positionClicked.below(i));

                if (state.is(ModBlocks.LUMINANCE_ORE)) {
                    player.displayClientMessage(Component.literal("Found Luminance Ore at Y level " + positionClicked.below(i).getY()), false);
                    break;
                }
            }
        }

        context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand().equals(InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.translatable("item.digitalfrontier.luminance_detector.tooltip"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
