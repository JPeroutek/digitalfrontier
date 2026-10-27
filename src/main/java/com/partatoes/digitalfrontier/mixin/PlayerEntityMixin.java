package com.partatoes.digitalfrontier.mixin;

import com.partatoes.digitalfrontier.grid.exit.MemoryPaging;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Core Dump: the same two checks vanilla makes for the keepInventory game rule, for a single death
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    public void keepInventoryAfterCoreDump(ServerWorld world, CallbackInfo ci) {
        if (MemoryPaging.keepsInventory((PlayerEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "getExperienceToDrop", at = @At("HEAD"), cancellable = true)
    public void keepExperienceAfterCoreDump(ServerWorld world, CallbackInfoReturnable<Integer> cir) {
        if (MemoryPaging.keepsInventory((PlayerEntity) (Object) this)) {
            cir.setReturnValue(0);
        }
    }
}
