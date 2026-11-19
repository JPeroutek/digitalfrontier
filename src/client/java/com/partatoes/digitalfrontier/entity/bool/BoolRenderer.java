package com.partatoes.digitalfrontier.entity.bool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModModelLayers;
import com.partatoes.digitalfrontier.entity.custom.BoolEntity;
import com.partatoes.digitalfrontier.entity.custom.BoolSignals;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.partatoes.digitalfrontier.derez.Derez;
import com.partatoes.digitalfrontier.derez.DerezClient;
import com.partatoes.digitalfrontier.derez.DerezStatue;
import com.partatoes.digitalfrontier.derez.DerezStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

import java.util.EnumMap;
import java.util.Map;

/** Draws a Bool glowing (full brightness, inside a soft halo) in its signal's color, bobbing as it floats. */
public class BoolRenderer extends EntityRenderer<BoolEntity, BoolRenderState> {
    private static final Map<BoolSignals.Signal, Identifier> TEXTURES = new EnumMap<>(Map.of(
            BoolSignals.Signal.IDLE, texture("bool_idle"),
            BoolSignals.Signal.YES, texture("bool_yes"),
            BoolSignals.Signal.NO, texture("bool_no")));
    private static final Map<BoolSignals.Signal, Identifier> PANES = new EnumMap<>(Map.of(
            BoolSignals.Signal.IDLE, texture("bool_idle_panes"),
            BoolSignals.Signal.YES, texture("bool_yes_panes"),
            BoolSignals.Signal.NO, texture("bool_no_panes")));
    private static final Map<BoolSignals.Signal, Identifier> HALOS = new EnumMap<>(Map.of(
            BoolSignals.Signal.IDLE, texture("bool_idle_halo"),
            BoolSignals.Signal.YES, texture("bool_yes_halo"),
            BoolSignals.Signal.NO, texture("bool_no_halo")));

    private static final Identifier DEREZ_MASK = texture("derez_mask");

    private final BoolModel model;

    public BoolRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BoolModel(context.bakeLayer(ModModelLayers.BOOL));
        this.shadowRadius = 0.2f;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/" + name + ".png");
    }

    @Override
    public BoolRenderState createRenderState() {
        return new BoolRenderState();
    }

    @Override
    public void extractRenderState(BoolEntity entity, BoolRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.signal = entity.signal();
        state.sitting = entity.isOrderedToSit();
        state.derez = entity.deathTime > 0 && DerezClient.derezzes(entity) ? Derez.progress(entity.deathTime + tickDelta) : -1;
        state.raise = state.derez >= 0 && DerezClient.raiseOnce(entity) ? DerezClient.glow(entity) : -1;
    }

    @Override
    public void submit(BoolRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        // Derezzing (see Derez): drawn as its style says, the frame dissolving and the light fading with it
        DerezStyle style = DerezClient.style();
        boolean derezzing = state.derez >= 0;
        if (derezzing && !style.bodyVisible(state.derez)) {
            super.submit(state, matrices, queue, cameraState);
            return;
        }
        float fade = derezzing ? style.bodyAlpha(state.derez) : 1;
        matrices.pushPose();
        float bob = state.sitting || derezzing ? 0 : (float) Math.sin(state.ageInTicks * 0.1) * 0.08f;
        matrices.translate(0, 0.25 + bob, 0);
        ClientLevel level = Minecraft.getInstance().level;
        if (state.raise >= 0 && level != null) {
            // Its statue, from the model as posed this frame
            model.setupAnim(state);
            DerezStatue.Builder statue = new DerezStatue.Builder(cameraState.pos, state.raise);
            statue.addModel(model.root(), matrices, TEXTURES.get(state.signal), -1);
            statue.raise(level, state.x, state.z);
            state.raise = -1;
        }
        // The frame (rims) cut out, then the faint panes inside them. The eyes pass blends without writing depth, so
        // no pane hides another: every face of both cubes shows through every other whatever order they're drawn in
        RenderType frame = derezzing && style == DerezStyle.SWEEP
                ? RenderTypes.entityCutoutDissolve(TEXTURES.get(state.signal), DEREZ_MASK)
                : RenderTypes.entityCutout(TEXTURES.get(state.signal));
        int tint = derezzing && style == DerezStyle.FREEZE ? 0xFFFFFFFF : ARGB.white(fade);
        queue.submitModel(model, state, matrices, frame, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint, null,
                state.outlineColor);
        queue.submitModel(model, state, matrices, RenderTypes.eyes(PANES.get(state.signal)), LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, ARGB.white(fade), null, 0);
        // The glow: a larger, fainter copy around it, breathing in and out
        float glow = 1.6f + (float) Math.sin(state.ageInTicks * 0.15) * 0.15f;
        matrices.scale(glow, glow, glow);
        queue.submitModel(model, state, matrices, RenderTypes.eyes(HALOS.get(state.signal)), LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, ARGB.white(fade), null, 0);
        matrices.popPose();
        super.submit(state, matrices, queue, cameraState);
    }
}
