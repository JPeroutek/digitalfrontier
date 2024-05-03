// Made with Blockbench 4.9.4
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports

package com.partatoes.digitalfrontier.entity.vehicle;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class LightCycleModel<T extends LightCycleEntity> extends SinglePartEntityModel<T> {

	private final ModelPart LightCycle;
	private final ModelPart FrontTire;
	private final ModelPart MainBody;
	private final ModelPart RearTire;
	public LightCycleModel(ModelPart root) {
		this.LightCycle = root;
		this.FrontTire = root.getChild("FrontTire");
		this.MainBody = root.getChild("MainBody");
		this.RearTire = root.getChild("RearTire");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData FrontTire = modelPartData.addChild("FrontTire", ModelPartBuilder.create().uv(40, 12).cuboid(-16.0F, -16.0F, 0.0F, 16.0F, 16.0F, 4.0F, new Dilation(0.0F))
		.uv(0, 32).cuboid(-16.0F, -4.0F, 4.0F, 16.0F, 4.0F, 8.0F, new Dilation(0.0F))
		.uv(0, 0).cuboid(-16.0F, -16.0F, 4.0F, 16.0F, 4.0F, 8.0F, new Dilation(0.0F))
		.uv(0, 12).cuboid(-16.0F, -16.0F, 12.0F, 16.0F, 16.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData MainBody = modelPartData.addChild("MainBody", ModelPartBuilder.create().uv(56, 8).cuboid(-14.0F, -14.0F, 16.0F, 12.0F, 12.0F, 24.0F, new Dilation(0.0F))
		.uv(40, 0).cuboid(-13.0F, -16.0F, 39.0F, 10.0F, 2.0F, 1.0F, new Dilation(0.0F))
		.uv(80, 62).cuboid(-14.0F, -15.0F, 16.0F, 12.0F, 1.0F, 9.0F, new Dilation(0.0F))
		.uv(58, 0).cuboid(-13.0F, -16.0F, 18.0F, 10.0F, 1.0F, 5.0F, new Dilation(0.0F))
		.uv(0, 0).cuboid(-5.0F, -18.0F, 20.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F))
		.uv(0, 0).cuboid(-12.0F, -18.0F, 20.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F))
		.uv(84, 1).cuboid(-4.0F, -18.0F, 20.0F, 5.0F, 1.0F, 1.0F, new Dilation(0.0F))
		.uv(90, 4).cuboid(-17.0F, -18.0F, 20.0F, 5.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData RearTire = modelPartData.addChild("RearTire", ModelPartBuilder.create().uv(0, 45).cuboid(-16.0F, -16.0F, 40.0F, 16.0F, 16.0F, 4.0F, new Dilation(0.0F))
		.uv(40, 56).cuboid(-16.0F, -16.0F, 52.0F, 16.0F, 16.0F, 4.0F, new Dilation(0.0F))
		.uv(40, 44).cuboid(-16.0F, -4.0F, 44.0F, 16.0F, 4.0F, 8.0F, new Dilation(0.0F))
		.uv(80, 49).cuboid(-16.0F, -16.0F, 44.0F, 16.0F, 4.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));
		return TexturedModelData.of(modelData, 128, 128);
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		FrontTire.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		MainBody.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		RearTire.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart getPart() {
		return this.LightCycle;
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

	}
}