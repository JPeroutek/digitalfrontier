package com.partatoes.digitalfrontier.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * The player's shape (with its overlay layers, which hang off the limbs) on a plain biped model. Not a
 * PlayerEntityModel: since 1.21.9 any player render state is drawn by the player renderer with a player skin.
 */
public class ProgramModel extends HumanoidModel<HumanoidRenderState> {
    public ProgramModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition getTexturedModelData() {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);
    }
}
