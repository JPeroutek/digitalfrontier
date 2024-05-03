// Made with Blockbench 4.9.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class LightCycle<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "lightcycle"), "main");
	private final ModelPart FrontTire;
	private final ModelPart MainBody;
	private final ModelPart RearTire;

	public LightCycle(ModelPart root) {
		this.FrontTire = root.getChild("FrontTire");
		this.MainBody = root.getChild("MainBody");
		this.RearTire = root.getChild("RearTire");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition FrontTire = partdefinition.addOrReplaceChild("FrontTire", CubeListBuilder.create().texOffs(40, 12).addBox(-16.0F, -16.0F, 0.0F, 16.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 32).addBox(-16.0F, -4.0F, 4.0F, 16.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-16.0F, -16.0F, 4.0F, 16.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 12).addBox(-16.0F, -16.0F, 12.0F, 16.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition MainBody = partdefinition.addOrReplaceChild("MainBody", CubeListBuilder.create().texOffs(56, 8).addBox(-14.0F, -14.0F, 16.0F, 12.0F, 12.0F, 24.0F, new CubeDeformation(0.0F))
		.texOffs(40, 0).addBox(-13.0F, -16.0F, 39.0F, 10.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(80, 62).addBox(-14.0F, -15.0F, 16.0F, 12.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(58, 0).addBox(-13.0F, -16.0F, 18.0F, 10.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.0F, -18.0F, 20.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-12.0F, -18.0F, 20.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(84, 1).addBox(-4.0F, -18.0F, 20.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(90, 4).addBox(-17.0F, -18.0F, 20.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition RearTire = partdefinition.addOrReplaceChild("RearTire", CubeListBuilder.create().texOffs(0, 45).addBox(-16.0F, -16.0F, 40.0F, 16.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(40, 56).addBox(-16.0F, -16.0F, 52.0F, 16.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(40, 44).addBox(-16.0F, -4.0F, 44.0F, 16.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(80, 49).addBox(-16.0F, -16.0F, 44.0F, 16.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		FrontTire.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		MainBody.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		RearTire.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}