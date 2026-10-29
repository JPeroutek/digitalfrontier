package com.partatoes.digitalfrontier.entity.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModModelLayers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class LightCycleRenderer extends EntityRenderer<LightCycleEntity, LightCycleEntityRenderState> {
    private final LightCycleModel<LightCycleEntityRenderState> MODEL;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/lightcycle_blue.png");


    public LightCycleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.MODEL = createModel(ctx);
    }

    @Override
    public LightCycleEntityRenderState createRenderState() {
        return new LightCycleEntityRenderState();
    }

    @Override
    public void extractRenderState(LightCycleEntity entity, LightCycleEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.yaw = entity.getYRot();
    }

    private LightCycleModel<LightCycleEntityRenderState> createModel(EntityRendererProvider.Context ctx) {
        ModelLayerLocation entityModelLayer = ModModelLayers.LIGHTCYCLE;
        ModelPart modelPart = ctx.bakeLayer(entityModelLayer);
        return new LightCycleModel<>(modelPart);
    }

    @Override
    public void submit(LightCycleEntityRenderState renderState, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(-renderState.yaw));

        // Flip it rightside-up
        matrices.mulPose(Axis.XP.rotationDegrees(180f));
        // Shift it back into position
        matrices.translate(0f, -1.5f, 0f);
        queue.submitModel(this.MODEL, renderState, matrices, this.MODEL.renderType(TEXTURE), renderState.lightCoords, OverlayTexture.NO_OVERLAY,
                renderState.outlineColor, null);

        matrices.popPose();
        super.submit(renderState, matrices, queue, cameraState);
    }
}
