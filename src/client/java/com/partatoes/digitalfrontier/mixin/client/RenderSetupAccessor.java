package com.partatoes.digitalfrontier.mixin.client;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** A render setup's textures by sampler name ("Sampler0" is the main one); see {@link TextureBindingAccessor}. */
@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    @Accessor("textures")
    Map<String, ?> digitalfrontier$textures();
}
