package com.partatoes.digitalfrontier.item;

import net.fabricmc.fabric.api.registry.FuelRegistry;

public class ModFuelItems {
    public static void registerModFuelItems() {
        FuelRegistry.INSTANCE.add(ModItems.COAL_DUST,200);
    }

}
