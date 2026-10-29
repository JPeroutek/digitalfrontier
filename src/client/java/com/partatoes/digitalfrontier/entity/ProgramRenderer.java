package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class ProgramRenderer extends HumanoidMobRenderer<ProgramEntity, HumanoidRenderState, ProgramModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/quorra64.png");

    public ProgramRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, createModel(ctx), 0.5f);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    private static ProgramModel createModel(EntityRendererProvider.Context ctx) {
        ModelLayerLocation entityModelLayer = ModModelLayers.PROGRAM;
        ModelPart modelPart = ctx.bakeLayer(entityModelLayer);
        return new ProgramModel(modelPart);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
