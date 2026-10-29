// Made with Blockbench 4.9.4
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports

package com.partatoes.digitalfrontier.entity.vehicle;

import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class LightCycleModel<T extends EntityRenderState> extends EntityModel<T> {
	private final ModelPart LightCycle;
	private final ModelPart FrontTire;
	private final ModelPart MainBody;
	private final ModelPart RearTire;
	public LightCycleModel(ModelPart root) {
		super(root.getChild("LightCycle"));
		this.LightCycle = root.getChild("LightCycle");
		this.FrontTire = root.getChild("LightCycle").getChild("FrontTire");
		this.MainBody = root.getChild("LightCycle").getChild("MainBody");
		this.RearTire = root.getChild("LightCycle").getChild("RearTire");
	}
	public static LayerDefinition getTexturedModelData() {
		MeshDefinition modelData = new MeshDefinition();
		PartDefinition modelPartData = modelData.getRoot();
		PartDefinition LightCycle = modelPartData.addOrReplaceChild("LightCycle", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition FrontTire = LightCycle.addOrReplaceChild("FrontTire", CubeListBuilder.create().texOffs(40, 12).addBox(-8.0F, -16.0F, -28.0F, 16.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 32).addBox(-8.0F, -4.0F, -24.0F, 16.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-8.0F, -16.0F, -24.0F, 16.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(0, 12).addBox(-8.0F, -16.0F, -16.0F, 16.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition MainBody = LightCycle.addOrReplaceChild("MainBody", CubeListBuilder.create().texOffs(56, 8).addBox(-6.0F, -14.0F, -12.0F, 12.0F, 12.0F, 24.0F, new CubeDeformation(0.0F))
				.texOffs(40, 0).addBox(-5.0F, -16.0F, 11.0F, 10.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(80, 62).addBox(-6.0F, -15.0F, -12.0F, 12.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
				.texOffs(58, 0).addBox(-5.0F, -16.0F, -10.0F, 10.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(3.0F, -18.0F, -8.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-4.0F, -18.0F, -8.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(84, 1).addBox(4.0F, -18.0F, -8.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(90, 4).addBox(-9.0F, -18.0F, -8.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition RearTire = LightCycle.addOrReplaceChild("RearTire", CubeListBuilder.create().texOffs(0, 45).addBox(-8.0F, -16.0F, 12.0F, 16.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(40, 56).addBox(-8.0F, -16.0F, 24.0F, 16.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(40, 44).addBox(-8.0F, -4.0F, 16.0F, 16.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(80, 49).addBox(-8.0F, -16.0F, 16.0F, 16.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		return LayerDefinition.create(modelData, 128, 128);
	}

//	@Override
//	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
//		LightCycle.render(matrices, vertices, light, overlay, color);
//	}

//	@Override
//	public ModelPart getPart() {
//		return this.LightCycle;
//	}
//
//	@Override
//	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
//
//	}
}