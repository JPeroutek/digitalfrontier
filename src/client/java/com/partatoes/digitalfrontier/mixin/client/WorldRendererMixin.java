package com.partatoes.digitalfrontier.mixin.client;

import com.partatoes.digitalfrontier.grid.PortalBeamRenderer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fabric API has no world render events on 1.21.9: draw the Portal beams in the main pass, once entities are drawn
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "method_62214", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/RenderDispatcher;render()V", shift = At.Shift.AFTER))
    private void renderPortalBeams(CallbackInfo ci) {
        PortalBeamRenderer.render();
    }
}
