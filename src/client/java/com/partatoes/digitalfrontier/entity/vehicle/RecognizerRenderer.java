package com.partatoes.digitalfrontier.entity.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws a Recognizer at twice its model's size, full bright, and while it searches, a cone of red light from its eye
 * down to the ground: faint as it sweeps, brighter once it has caught someone.
 */
public class RecognizerRenderer extends EntityRenderer<RecognizerEntity, RecognizerRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/recognizer.png");
    private static final RenderType BEAM = RenderTypes.entityTranslucentEmissive(
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/derez_cube.png"));
    private static final float SCALE = 2;
    // Where the eye sits, in blocks above its feet
    private static final float EYE = 2.4f;
    private static final int SEGMENTS = 16;

    private final RecognizerModel model;

    public RecognizerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RecognizerModel(context.bakeLayer(ModModelLayers.RECOGNIZER));
        this.shadowRadius = 1.5f;
    }

    @Override
    public RecognizerRenderState createRenderState() {
        return new RecognizerRenderState();
    }

    @Override
    public void extractRenderState(RecognizerEntity entity, RecognizerRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.yaw = entity.getYRot(tickDelta);
        state.searching = entity.searching();
        state.spotting = entity.spotting();
    }

    @Override
    public void submit(RecognizerRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        matrices.pushPose();
        matrices.rotate(Axis.YP.rotationDegrees(180 - state.yaw));
        matrices.scale(-SCALE, -SCALE, SCALE);
        matrices.translate(0, -1.501, 0);
        queue.submitModel(model, state, matrices, RenderTypes.entityCutout(TEXTURE), LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        matrices.popPose();
        if (state.searching) {
            int color = state.spotting ? 0x60FF3030 : 0x2AFF4040;
            matrices.pushPose();
            matrices.translate(0, EYE, 0);
            queue.submitCustomGeometry(matrices, BEAM, (pose, vertices) -> cone(pose.pose(), vertices, color));
            matrices.popPose();
        }
        super.submit(state, matrices, queue, cameraState);
    }

    // A cone from the eye (a small disc) down to a wide circle on the ground, both faces drawn
    private static void cone(Matrix4f pose, VertexConsumer vertices, int color) {
        float top = 0.5f;
        float bottom = RecognizerEntity.BEAM_RADIUS;
        float drop = -RecognizerEntity.BEAM_LENGTH;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS;
            float a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float[][] quad = {
                    {Mth.cos(a0) * top, 0, Mth.sin(a0) * top},
                    {Mth.cos(a0) * bottom, drop, Mth.sin(a0) * bottom},
                    {Mth.cos(a1) * bottom, drop, Mth.sin(a1) * bottom},
                    {Mth.cos(a1) * top, 0, Mth.sin(a1) * top}};
            emit(pose, vertices, color, quad, false);
            emit(pose, vertices, color, quad, true);
        }
    }

    private static void emit(Matrix4f pose, VertexConsumer vertices, int color, float[][] quad, boolean reversed) {
        for (int i = 0; i < 4; i++) {
            float[] corner = quad[reversed ? 3 - i : i];
            org.joml.Vector3f at = pose.transformPosition(corner[0], corner[1], corner[2], new org.joml.Vector3f());
            vertices.addVertex(at.x, at.y, at.z, color, 0.5f, 0.5f, OverlayTexture.NO_OVERLAY, LightCoordsUtil.FULL_BRIGHT, 0, 1, 0);
        }
    }
}
