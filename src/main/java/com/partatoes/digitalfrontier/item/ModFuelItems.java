package com.partatoes.digitalfrontier.item;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

/**
 * Fuel burn times, which since 26.3 are data (context int providers) that items point to with
 * {@link net.minecraft.world.item.Item.Properties#cookingFuel}. Each is a file in
 * data/digitalfrontier/context_int_provider, written like vanilla's so fast furnaces burn it quicker; they're not
 * generated, since datagen can't reference the fast_cooking predicate they use.
 */
public class ModFuelItems {
    // An eighth of an item's smelt (200 ticks)
    public static final ResourceKey<ContextIntProvider> COAL_DUST_BURN_TIME = ResourceKey.create(Registries.CONTEXT_INT_PROVIDER,
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "cooking/time_coal_dust"));
}
