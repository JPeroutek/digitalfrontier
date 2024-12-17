package com.partatoes.digitalfrontier.item;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.item.custom.LightCycleBaton;
import com.partatoes.digitalfrontier.item.custom.LuminanceOreDetector;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Identifier BINARY_0_ID = Identifier.of(DigitalFrontier.MOD_ID, "binary_0");
    public static final Item BINARY_0 = registerItem(BINARY_0_ID, new Item(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, BINARY_0_ID))));
    public static final Identifier BINARY_1_ID = Identifier.of(DigitalFrontier.MOD_ID, "binary_1");
    public static final Item BINARY_1 = registerItem(BINARY_1_ID, new Item(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, BINARY_1_ID))));
    public static final Identifier PIXEL_DUST_ID = Identifier.of(DigitalFrontier.MOD_ID, "pixel_dust");
    public static final Item PIXEL_DUST = registerItem(PIXEL_DUST_ID, new Item(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, PIXEL_DUST_ID))));
    public static final Identifier LUMINANCE_ORE_DETECTOR_ID = Identifier.of(DigitalFrontier.MOD_ID, "luminance_detector");
    public static final Item LUMINANCE_ORE_DETECTOR = registerItem(LUMINANCE_ORE_DETECTOR_ID,
            new LuminanceOreDetector(new Item.Settings()
                    .maxDamage(128)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, LUMINANCE_ORE_DETECTOR_ID))));
    public static final Identifier LUMINESSENCE_ID = Identifier.of(DigitalFrontier.MOD_ID, "luminessence");
    public static final Item LUMINESSENCE = registerItem(LUMINESSENCE_ID, new Item(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, LUMINESSENCE_ID))));
    public static final Identifier BYTE_ID = Identifier.of(DigitalFrontier.MOD_ID, "byte");
    public static final Item BYTE = registerItem(BYTE_ID,
            new Item(new Item.Settings()
                    .food(ModFoodComponents.BYTE)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, BYTE_ID))));
    public static final Identifier COAL_DUST_ID = Identifier.of(DigitalFrontier.MOD_ID, "coal_dust");
    public static final Item COAL_DUST = registerItem(COAL_DUST_ID, new Item(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, COAL_DUST_ID))));
    public static final Identifier LIGHTCYCLE_BATON_ID = Identifier.of(DigitalFrontier.MOD_ID, "lightcycle_baton");
    public static final Item LIGHTCYCLE_BATON = registerItem(LIGHTCYCLE_BATON_ID, new LightCycleBaton(new Item.Settings()
            .maxCount(1)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, LIGHTCYCLE_BATON_ID))));

    private static Item registerItem(Identifier id, Item item) {
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void registerModItems() {
        DigitalFrontier.LOGGER.info("Registering Mod Items for " + DigitalFrontier.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::addItemsToIngredientTabItemGroup);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(ModItems::addItemsToFoodTabItemGroup);
    }

    private static void addItemsToIngredientTabItemGroup(FabricItemGroupEntries entries) {
        entries.add(BINARY_0);
        entries.add(BINARY_1);
        entries.add(PIXEL_DUST);
        entries.add(LUMINESSENCE);
        entries.add(COAL_DUST);
    }

    private static void addItemsToFoodTabItemGroup(FabricItemGroupEntries entries) {
        entries.add(BYTE);
    }
}
