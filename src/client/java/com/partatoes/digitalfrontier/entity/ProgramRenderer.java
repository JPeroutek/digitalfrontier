package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;

public class ProgramRenderer extends BipedEntityRenderer<ProgramEntity, PlayerEntityRenderState, ProgramModel<PlayerEntityRenderState>> {
    private static final Identifier TEXTURE = Identifier.of(DigitalFrontier.MOD_ID, "textures/entity/quorra64.png");

    public ProgramRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, createModel(ctx), 0.5f);
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        return new PlayerEntityRenderState();
    }

    private static ProgramModel<PlayerEntityRenderState> createModel(EntityRendererFactory.Context ctx) {
        EntityModelLayer entityModelLayer = ModModelLayers.PROGRAM;
        ModelPart modelPart = ctx.getPart(entityModelLayer);
        return new ProgramModel<>(modelPart);
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState state) {
        return TEXTURE;
    }
}
