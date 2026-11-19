package com.partatoes.digitalfrontier.mixin.client;

import com.google.common.cache.LoadingCache;
import net.minecraft.client.resources.palette.PaletteMapping;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * What a paletted texture (an armour trim) was made from, so a derez statue can recolor it on the CPU: the trims
 * themselves only exist on the GPU. See {@link PalettedSlotKeyAccessor} and {@link PalettedBaseTextureAccessor}.
 */
@Mixin(PalettedTextureManager.class)
public interface PalettedTextureManagerAccessor {
    @Accessor("handles")
    Map<Object, PalettedTextureManager.Handle> digitalfrontier$handles();

    @Accessor("baseTextureCache")
    LoadingCache<Identifier, Optional<Object>> digitalfrontier$baseTextures();

    @Accessor("paletteMappings")
    BiFunction<Identifier, Identifier, PaletteMapping> digitalfrontier$paletteMappings();
}
