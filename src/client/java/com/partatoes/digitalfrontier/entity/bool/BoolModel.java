package com.partatoes.digitalfrontier.entity.bool;

import com.partatoes.digitalfrontier.entity.custom.BoolSignals;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * A Bool: two cubes through each other, one turned 45° on two axes, which together read as a many-sided solid. The
 * two spin against each other, each on its own pivot. "Yes" drops the turned cube and leaves a clean diamond; "no"
 * pushes it out into spikes.
 */
public class BoolModel extends EntityModel<BoolRenderState> {
    private static final float SHELL_SCALE = 0.93f;

    private final ModelPart body;
    private final ModelPart coreSpin;
    private final ModelPart core;
    private final ModelPart shellSpin;
    private final ModelPart shell;

    public BoolModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.coreSpin = body.getChild("core_spin");
        this.core = coreSpin.getChild("core");
        this.shellSpin = body.getChild("shell_spin");
        this.shell = shellSpin.getChild("shell");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        // Each cube sits in its own spinning pivot, so the shell keeps its tilt while it turns. They differ in size so
        // their faces never coincide and flicker as they pass through each other
        body.addOrReplaceChild("core_spin", CubeListBuilder.create(), PartPose.ZERO)
                .addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.ZERO);
        body.addOrReplaceChild("shell_spin", CubeListBuilder.create(), PartPose.ZERO)
                .addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 12).addBox(-3.5f, -3.5f, -3.5f, 7, 7, 7),
                        PartPose.rotation((float) Math.PI / 4, 0, (float) Math.PI / 4));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(BoolRenderState state) {
        super.setupAnim(state);
        body.xRot = (float) Math.sin(state.ageInTicks * 0.05) * 0.3f;
        coreSpin.yRot = state.ageInTicks * 0.104f;
        shellSpin.yRot = -state.ageInTicks * 0.143f;
        shell.visible = state.signal != BoolSignals.Signal.YES;
        core.xRot = state.signal == BoolSignals.Signal.YES ? (float) Math.PI / 4 : 0;
        core.zRot = state.signal == BoolSignals.Signal.YES ? (float) Math.PI / 4 : 0;
        // Drawn a touch under its 7 pixels (about 6.5), still clear of the core's faces; "no" pushes it out into spikes
        float spikes = SHELL_SCALE * (state.signal == BoolSignals.Signal.NO ? 1.35f : 1);
        shell.xScale = spikes;
        shell.yScale = spikes;
        shell.zScale = spikes;
    }
}
