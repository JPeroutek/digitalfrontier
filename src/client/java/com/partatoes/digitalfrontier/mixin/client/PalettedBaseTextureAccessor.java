package com.partatoes.digitalfrontier.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.texture.PaletteMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A paletted texture's greyscale source image, and the palette its greys are keyed to. */
@Mixin(targets = "net.minecraft.client.resources.palette.PalettedTextureManager$BaseTexture")
public interface PalettedBaseTextureAccessor {
    @Accessor("image")
    NativeImage digitalfrontier$image();

    @Accessor("paletteMetadata")
    PaletteMetadataSection digitalfrontier$paletteMetadata();
}
