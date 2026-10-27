package com.partatoes.digitalfrontier.mixin;

import com.partatoes.digitalfrontier.grid.stronghold.StrongholdEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fabric has no crafting event; this is where vanilla handles a player taking a crafted item
@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "onCraftByPlayer", at = @At("HEAD"))
    public void fireItemCrafted(World world, PlayerEntity player, int amount, CallbackInfo ci) {
        StrongholdEvents.onItemCrafted((ItemStack) (Object) this, world, player);
    }
}
