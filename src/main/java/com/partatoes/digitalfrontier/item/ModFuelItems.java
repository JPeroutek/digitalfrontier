package com.partatoes.digitalfrontier.item;


import net.fabricmc.fabric.api.registry.FuelValueEvents;

public class ModFuelItems {
    public static void registerModFuelItems() {
        FuelValueEvents.BUILD.register((builder, context) -> {
            builder.add(ModItems.COAL_DUST, context.baseSmeltTime() / 8);
        });
    }

}
