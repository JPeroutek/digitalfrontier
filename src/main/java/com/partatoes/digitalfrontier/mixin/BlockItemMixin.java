package com.partatoes.digitalfrontier.mixin;

import com.partatoes.digitalfrontier.grid.stronghold.StrongholdEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Fabric has no block placement event; every block item places through here
@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"))
    public void fireBlockPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction()) {
            StrongholdEvents.onBlockPlaced(context.getLevel(), context.getClickedPos());
        }
    }
}
