package com.partatoes.digitalfrontier.mixin;

import com.partatoes.digitalfrontier.grid.exit.MemoryPaging;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Core Dump: the same two checks vanilla makes for the keepInventory game rule, for a single death
@Mixin(Player.class)
public class PlayerEntityMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    public void keepInventoryAfterCoreDump(ServerLevel world, CallbackInfo ci) {
        if (MemoryPaging.keepsInventory((Player) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    public void keepExperienceAfterCoreDump(ServerLevel world, CallbackInfoReturnable<Integer> cir) {
        if (MemoryPaging.keepsInventory((Player) (Object) this)) {
            cir.setReturnValue(0);
        }
    }
}
