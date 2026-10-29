package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.Identifier;

public class ProgramRenderer extends BipedEntityRenderer<ProgramEntity, BipedEntityRenderState, ProgramModel> {
    private static final Identifier TEXTURE = Identifier.of(DigitalFrontier.MOD_ID, "textures/entity/quorra64.png");

    public ProgramRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, createModel(ctx), 0.5f);
    }

    @Override
    public BipedEntityRenderState createRenderState() {
        return new BipedEntityRenderState();
    }

    private static ProgramModel createModel(EntityRendererFactory.Context ctx) {
        EntityModelLayer entityModelLayer = ModModelLayers.PROGRAM;
        ModelPart modelPart = ctx.getPart(entityModelLayer);
        return new ProgramModel(modelPart);
    }

    @Override
    public Identifier getTexture(BipedEntityRenderState state) {
        return TEXTURE;
    }
}
