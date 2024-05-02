package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class LuminanceOreDetector extends Item {

    public LuminanceOreDetector(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getPlayer() == null) {
            return ActionResult.PASS;
        }
        if(!context.getWorld().isClient()) {
            BlockPos positionClicked = context.getBlockPos();
            PlayerEntity player = context.getPlayer();

            for(int i = 0; i <= positionClicked.getY() + 64; i++) {
                BlockState state = context.getWorld().getBlockState(positionClicked.down(i));

                if (state.isOf(ModBlocks.LUMINANCE_ORE)) {
                    player.sendMessage(Text.literal("Found Luminance Ore at Y level " + positionClicked.down(i).getY()), false);
                    break;
                }
            }
        }

//        context.getStack().damage(1, context.getPlayer(), playerEntity -> playerEntity.sendToolBreakStatus(playerEntity.getActiveHand()));
        context.getStack().damage(1, context.getPlayer(), context.getHand().equals(Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return ActionResult.SUCCESS;
    }


    //ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.digitalfrontier.luminance_detector.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
//    @Override
//    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
//        tooltip.add(Text.translatable("item.digitalfrontier.luminance_detector.tooltip"));
//        super.appendTooltip(stack, world, tooltip, context);
//    }
}
