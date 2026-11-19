package com.partatoes.digitalfrontier.mixin.client;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

/**
 * A model part's cubes and children, so a derez statue can walk only the parts that are showing (vanilla's
 * {@code ModelPart.visit} walks hidden ones too, and armour models hide all but the worn pieces).
 */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("cubes")
    List<ModelPart.Cube> digitalfrontier$cubes();

    @Accessor("children")
    Map<String, ModelPart> digitalfrontier$children();
}
