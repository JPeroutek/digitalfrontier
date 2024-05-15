package com.partatoes.digitalfrontier.entity.vehicle;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModModelLayers;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class LightCycleRenderer extends EntityRenderer<LightCycleEntity> {
    private final LightCycleModel<LightCycleEntity> MODEL;
    private static final Identifier TEXTURE = new Identifier(DigitalFrontier.MOD_ID, "textures/entity/lightcycle_blue.png");
    public LightCycleRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.MODEL = createModel(ctx);
    }
    private LightCycleModel<LightCycleEntity> createModel(EntityRendererFactory.Context ctx) {
        EntityModelLayer entityModelLayer = ModModelLayers.LIGHTCYCLE;
        ModelPart modelPart = ctx.getPart(entityModelLayer);
        return new LightCycleModel<>(modelPart);
    }
    @Override
    public Identifier getTexture(LightCycleEntity entity) {
        return TEXTURE;
    }
    @Override
    public void render(LightCycleEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));

        // Flip it rightside-up
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));
        // Shift it back into position
        matrices.translate(0f, -1.5f, 0f);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.MODEL.getLayer(TEXTURE));
        this.MODEL.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
