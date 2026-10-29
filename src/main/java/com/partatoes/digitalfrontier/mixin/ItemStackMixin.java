package com.partatoes.digitalfrontier.mixin;

import com.partatoes.digitalfrontier.grid.stronghold.StrongholdEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fabric has no crafting event; this is where vanilla handles a player taking a crafted item
@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "onCraftedBy", at = @At("HEAD"))
    public void fireItemCrafted(Player player, int amount, CallbackInfo ci) {
        StrongholdEvents.onItemCrafted((ItemStack) (Object) this, player.level(), player);
    }
}
