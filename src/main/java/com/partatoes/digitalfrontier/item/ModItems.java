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
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item BINARY_0 = registerItem("binary_0", new Item(new Item.Settings()));
    public static final Item BINARY_1 = registerItem("binary_1", new Item(new Item.Settings()));
    public static final Item PIXEL_DUST = registerItem("pixel_dust", new Item(new Item.Settings()));
    public static final Item LUMINANCE_ORE_DETECTOR = registerItem("luminance_detector",
            new LuminanceOreDetector(new Item.Settings().maxDamage(128)));
    public static final Item LUMINESSENCE = registerItem("luminessence", new Item(new Item.Settings()));

    public static final Item BYTE = registerItem("byte",
            new Item(new Item.Settings().food(ModFoodComponents.BYTE)));

    public static final Item COAL_DUST = registerItem("coal_dust", new Item(new Item.Settings()));

    public static final Item LIGHTCYCLE_BATON = registerItem("lightcycle_baton", new LightCycleBaton(new Item.Settings().maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(DigitalFrontier.MOD_ID, name), item);
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
