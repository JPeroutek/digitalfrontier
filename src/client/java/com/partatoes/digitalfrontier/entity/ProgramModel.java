package com.partatoes.digitalfrontier.entity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class ProgramModel <T extends PlayerEntityRenderState> extends PlayerEntityModel {
    private final ModelPart program;
    public ProgramModel(ModelPart root) {
        super(root, false);
        this.program = root;
    }


    public static TexturedModelData getTexturedModelData() {
        return TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, false), 64, 64);
    }
}
