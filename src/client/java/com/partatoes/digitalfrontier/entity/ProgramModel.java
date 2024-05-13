package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

public class ProgramModel <T extends ProgramEntity> extends PlayerEntityModel <T> {
    private final ModelPart program;
    public ProgramModel(ModelPart root) {
        super(root, false);
        this.program = root;
    }


    public static TexturedModelData getTexturedModelData() {
        return TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, false), 64, 64);
    }
}
