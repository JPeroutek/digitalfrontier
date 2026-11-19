package com.partatoes.digitalfrontier.mixin.client;

import com.partatoes.digitalfrontier.derez.DerezClient;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A body that derezzed has already come apart into cubes: no puff of smoke when it's removed. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDerezMixin {
    @Inject(method = "makePoofParticles", at = @At("HEAD"), cancellable = true)
    private void digitalfrontier$noPoof(CallbackInfo ci) {
        if (DerezClient.derezzes((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
