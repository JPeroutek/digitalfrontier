package com.partatoes.digitalfrontier.mixin.client;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.partatoes.digitalfrontier.grid.PortalBeamRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Portal's beams (see PortalBeamRenderer): prepared alongside vanilla's own frame, then drawn into the world's render
 * pass just before translucent terrain, as vanilla orders its own beacon beams, so water between the camera and a beam
 * is drawn over it rather than hiding it.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    // Vertices can only be uploaded outside a render pass, and only once the frame has started
    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"))
    private void digitalfrontier$preparePortalBeams(CallbackInfo ci) {
        PortalBeamRenderer.prepare();
    }

    @Inject(method = "executeClassicTransparency", at = @At("HEAD"))
    private void digitalfrontier$drawPortalBeams(ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame frame,
                                                 RenderPass pass, CallbackInfo ci) {
        PortalBeamRenderer.draw(pass);
    }
}
