package com.partatoes.digitalfrontier.mixin.client;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Which base texture and palette a paletted texture (an armour trim) was made from. */
@Mixin(targets = "net.minecraft.client.resources.palette.PalettedTextureManager$SlotKey")
public interface PalettedSlotKeyAccessor {
    @Accessor("baseTexture")
    Identifier digitalfrontier$baseTexture();

    @Accessor("paletteId")
    Identifier digitalfrontier$paletteId();
}
