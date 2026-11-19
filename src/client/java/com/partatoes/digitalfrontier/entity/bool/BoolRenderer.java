package com.partatoes.digitalfrontier.entity.bool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModModelLayers;
import com.partatoes.digitalfrontier.entity.custom.BoolEntity;
import com.partatoes.digitalfrontier.entity.custom.BoolSignals;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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
    }

    @Override
    public void submit(BoolRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        matrices.pushPose();
        float bob = state.sitting ? 0 : (float) Math.sin(state.ageInTicks * 0.1) * 0.08f;
        matrices.translate(0, 0.25 + bob, 0);
        // The frame (rims) cut out, then the faint panes inside them. The eyes pass blends without writing depth, so
        // no pane hides another: every face of both cubes shows through every other whatever order they're drawn in
        queue.submitModel(model, state, matrices, RenderTypes.entityCutout(TEXTURES.get(state.signal)), LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        queue.submitModel(model, state, matrices, RenderTypes.eyes(PANES.get(state.signal)), LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0);
        // The glow: a larger, fainter copy around it, breathing in and out
        float glow = 1.6f + (float) Math.sin(state.ageInTicks * 0.15) * 0.15f;
        matrices.scale(glow, glow, glow);
        queue.submitModel(model, state, matrices, RenderTypes.eyes(HALOS.get(state.signal)), LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();
        super.submit(state, matrices, queue, cameraState);
    }
}
