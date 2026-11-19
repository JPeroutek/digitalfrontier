package com.partatoes.digitalfrontier.entity.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModModelLayers;
import com.partatoes.digitalfrontier.entity.custom.SystemCoreEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/**
 * Draws the System Core at full brightness, twice the model's size, so it fills its 2×4-block body. A living entity
 * renderer, so it shows its name and the red flash when hit.
 */
public class SystemCoreRenderer extends LivingEntityRenderer<SystemCoreEntity, SystemCoreRenderState, SystemCoreModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/system_core.png");

    public SystemCoreRenderer(EntityRendererProvider.Context context) {
        super(context, new SystemCoreModel(context.bakeLayer(ModModelLayers.SYSTEM_CORE)), 1.0f);
    }

    @Override
    public SystemCoreRenderState createRenderState() {
        return new SystemCoreRenderState();
    }

    @Override
    public void extractRenderState(SystemCoreEntity entity, SystemCoreRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.phase = entity.phase();
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public Identifier getTextureLocation(SystemCoreRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(SystemCoreRenderState state, PoseStack matrices) {
        matrices.scale(2, 2, 2);
    }
}
