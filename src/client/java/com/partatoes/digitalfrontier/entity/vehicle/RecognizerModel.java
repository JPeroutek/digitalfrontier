package com.partatoes.digitalfrontier.entity.vehicle;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Recognizer: two tall legs joined by a heavy top beam, with the cockpit on top and a glowing eye under the beam.
 * Built like vanilla models: y = 24 is its feet, and up is -Y. Drawn at twice size by {@link RecognizerRenderer}.
 */
public class RecognizerModel extends EntityModel<RecognizerRenderState> {
    public RecognizerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Legs, splaying slightly outward toward the feet
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2, -20, -3, 4, 20, 6),
                PartPose.offsetAndRotation(-10, 24, 0, 0, 0, -0.12f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(20, 0).addBox(-2, -20, -3, 4, 20, 6),
                PartPose.offsetAndRotation(10, 24, 0, 0, 0, 0.12f));
        root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(0, 26).addBox(-14, -4, -4, 28, 4, 8),
                PartPose.offset(0, 4, 0));
        root.addOrReplaceChild("cockpit", CubeListBuilder.create().texOffs(0, 40).addBox(-4, -3, -3, 8, 3, 6),
                PartPose.offset(0, 0, 0));
        root.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(28, 40).addBox(-3, 0, -2, 6, 1, 4),
                PartPose.offset(0, 4, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
