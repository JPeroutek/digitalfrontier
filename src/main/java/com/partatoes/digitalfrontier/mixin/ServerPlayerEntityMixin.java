package com.partatoes.digitalfrontier.mixin;

import com.partatoes.digitalfrontier.grid.exit.MemoryPaging;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    // Memory Paging: respawn straight at the grid terminal instead of respawning in the real world first
    @Inject(method = "getRespawnTarget", at = @At("HEAD"), cancellable = true)
    public void respawnAtPagedTerminal(boolean alive, TeleportTarget.PostDimensionTransition postDimensionTransition,
                                       CallbackInfoReturnable<TeleportTarget> cir) {
        TeleportTarget target = MemoryPaging.respawnTarget((ServerPlayerEntity) (Object) this);
        if (target != null) {
            cir.setReturnValue(target);
        }
    }
}
