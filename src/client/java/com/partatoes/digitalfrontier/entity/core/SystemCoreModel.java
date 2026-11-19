package com.partatoes.digitalfrontier.entity.core;

import com.partatoes.digitalfrontier.entity.custom.SystemCorePhase;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The System Core: a stacked column of three blocks, each turning on its own. Once its shell breaks (below half health)
 * the middle block swells and the column turns faster; enraged, faster still. Built like vanilla models: y = 24 is
 * its feet, and up is -Y.
 */
public class SystemCoreModel extends EntityModel<SystemCoreRenderState> {
    private final ModelPart base;
    private final ModelPart middle;
    private final ModelPart top;

    public SystemCoreModel(ModelPart root) {
        super(root);
        this.base = root.getChild("base");
        this.middle = root.getChild("middle");
        this.top = root.getChild("top");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-8, -12, -8, 16, 12, 16), PartPose.offset(0, 24, 0));
        root.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 32).addBox(-6, -6, -6, 12, 12, 12),
                PartPose.offset(0, 5, 0));
        root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 64).addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(0, -5, 0));
        return LayerDefinition.create(mesh, 64, 128);
    }

    @Override
    public void setupAnim(SystemCoreRenderState state) {
        super.setupAnim(state);
        float speed = 1 + state.phase.ordinal();
        base.yRot = state.ageInTicks * 0.01f * speed;
        middle.yRot = -state.ageInTicks * 0.04f * speed;
        middle.xRot = (float) Math.PI / 4;
        middle.zRot = (float) Math.PI / 4;
        top.yRot = state.ageInTicks * 0.07f * speed;
        float swell = state.phase == SystemCorePhase.ABSORBING ? 1 : 1.3f;
        middle.xScale = swell;
        middle.yScale = swell;
        middle.zScale = swell;
        top.y = -5 + (float) Math.sin(state.ageInTicks * 0.08) * 1.5f;
    }
}
