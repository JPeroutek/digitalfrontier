package com.partatoes.digitalfrontier.mixin.client;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.derez.Derez;
import com.partatoes.digitalfrontier.derez.DerezClient;
import com.partatoes.digitalfrontier.derez.DerezStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.derez.DerezStatue;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Derezzing bodies (see {@link Derez}): in place of vanilla's tip-over and red flash, a dying body that derezzes is
 * drawn by its style. A sweep dissolves its skin cell by cell through the dragon's dissolve render type, its tint's
 * alpha driving the dissolve; a burst stops drawing it; a freeze flashes it white first; a statue is rebuilt in cubes
 * from the model on the first frame (see {@link DerezStatue}). Armour and held items go as soon as it starts to come
 * apart. Loose cubes are shed by {@link DerezClient}.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererDerezMixin {
    @Unique
    private static final RenderStateDataKey<Float> DEREZ = RenderStateDataKey.create(() -> "digitalfrontier:derez");
    @Unique
    private static final Identifier MASK = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/derez_mask.png");

    // Set on the frame a body's statue should be raised: the color for cubes of a skin that can't be read
    @Unique
    private static final RenderStateDataKey<Integer> RAISE = RenderStateDataKey.create(() -> "digitalfrontier:derez_raise");

    @Shadow
    protected EntityModel<?> model;

    @Shadow
    public abstract Identifier getTextureLocation(LivingEntityRenderState state);

    // How far through derezzing the body is, or null if it isn't derezzing
    @Unique
    private static Float digitalfrontier$progress(LivingEntityRenderState state) {
        return state.getData(DEREZ);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void digitalfrontier$extractDerez(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        state.setData(RAISE, null);
        if (state.deathTime > 0 && DerezClient.derezzes(entity)) {
            state.setData(DEREZ, Derez.progress(state.deathTime));
            // No tipping over, no red flash
            state.deathTime = 0;
            state.hasRedOverlay = false;
            if (DerezClient.raiseOnce(entity)) {
                state.setData(RAISE, DerezClient.glow(entity));
            }
        } else {
            state.setData(DEREZ, null);
        }
    }

    // A statue is raised from the model exactly as it's about to be drawn: posed, and placed where the body stands
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private void digitalfrontier$raiseStatue(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector queue,
                                             CameraRenderState camera, CallbackInfo ci) {
        Integer glow = state.getData(RAISE);
        ClientLevel level = Minecraft.getInstance().level;
        if (glow == null || level == null) {
            return;
        }
        state.setData(RAISE, null);
        ((EntityModel) model).setupAnim(state);
        DerezStatue.raise(level, model.root(), pose, camera.pos, getTextureLocation(state), glow, state.x, state.z);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void digitalfrontier$derezRenderType(LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing,
                                                 CallbackInfoReturnable<RenderType> cir) {
        Float progress = digitalfrontier$progress(state);
        if (progress == null) {
            return;
        }
        DerezStyle style = DerezClient.style();
        if (!style.bodyVisible(progress)) {
            cir.setReturnValue(null);
        } else if (style == DerezStyle.SWEEP) {
            cir.setReturnValue(RenderTypes.entityCutoutDissolve(getTextureLocation(state), MASK));
        }
    }

    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void digitalfrontier$derezTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        Float progress = digitalfrontier$progress(state);
        if (progress != null && DerezClient.style() == DerezStyle.SWEEP) {
            // The dissolve drops every texel whose mask is above this alpha
            cir.setReturnValue(ARGB.multiplyAlpha(cir.getReturnValue(), DerezClient.style().bodyAlpha(progress)));
        }
    }

    @Inject(method = "getWhiteOverlayProgress", at = @At("RETURN"), cancellable = true)
    private void digitalfrontier$derezFlash(LivingEntityRenderState state, CallbackInfoReturnable<Float> cir) {
        Float progress = digitalfrontier$progress(state);
        if (progress != null) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), DerezClient.style().whiteFlash(progress)));
        }
    }

    @Inject(method = "shouldRenderLayers", at = @At("HEAD"), cancellable = true)
    private void digitalfrontier$derezLayers(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
        Float progress = digitalfrontier$progress(state);
        if (progress != null && (DerezClient.style() != DerezStyle.FREEZE || !DerezClient.style().bodyVisible(progress))) {
            cir.setReturnValue(false);
        }
    }
}
