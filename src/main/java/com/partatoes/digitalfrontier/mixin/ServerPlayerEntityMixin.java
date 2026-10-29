package com.partatoes.digitalfrontier.mixin;

import com.partatoes.digitalfrontier.grid.exit.MemoryPaging;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {
    // Memory Paging: respawn straight at the grid terminal instead of respawning in the real world first
    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    public void respawnAtPagedTerminal(boolean alive, TeleportTransition.PostTeleportTransition postDimensionTransition,
                                       CallbackInfoReturnable<TeleportTransition> cir) {
        TeleportTransition target = MemoryPaging.respawnTarget((ServerPlayer) (Object) this);
        if (target != null) {
            cir.setReturnValue(target);
        }
    }
}
