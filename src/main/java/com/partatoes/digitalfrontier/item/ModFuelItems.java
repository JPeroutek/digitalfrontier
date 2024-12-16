package com.partatoes.digitalfrontier.item;


import net.fabricmc.fabric.api.registry.FuelRegistryEvents;

public class ModFuelItems {
    public static void registerModFuelItems() {
        FuelRegistryEvents.BUILD.register((builder, context) -> {
            builder.add(ModItems.COAL_DUST, context.baseSmeltTime() / 8);
        });
    }

}
