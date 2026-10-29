package com.partatoes.digitalfrontier.item;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.item.custom.DimensionTeleporterItem;
import com.partatoes.digitalfrontier.item.custom.EscapeKeyItem;
import com.partatoes.digitalfrontier.item.custom.LightCycleBaton;
import com.partatoes.digitalfrontier.item.custom.LuminanceOreDetector;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ModItems {
    public static final Identifier BINARY_0_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "binary_0");
    public static final Item BINARY_0 = registerItem(BINARY_0_ID, new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, BINARY_0_ID))));
    public static final Identifier BINARY_1_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "binary_1");
    public static final Item BINARY_1 = registerItem(BINARY_1_ID, new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, BINARY_1_ID))));
    public static final Identifier PIXEL_DUST_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "pixel_dust");
    public static final Item PIXEL_DUST = registerItem(PIXEL_DUST_ID, new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, PIXEL_DUST_ID))));
    public static final Identifier LUMINANCE_ORE_DETECTOR_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "luminance_detector");
    public static final Item LUMINANCE_ORE_DETECTOR = registerItem(LUMINANCE_ORE_DETECTOR_ID,
            new LuminanceOreDetector(new Item.Properties()
                    .durability(128)
                    .setId(ResourceKey.create(Registries.ITEM, LUMINANCE_ORE_DETECTOR_ID))));
    public static final Identifier LUMINESSENCE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "luminessence");
    public static final Item LUMINESSENCE = registerItem(LUMINESSENCE_ID, new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, LUMINESSENCE_ID))));
    public static final Identifier BYTE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "byte");
    public static final Item BYTE = registerItem(BYTE_ID,
            new Item(new Item.Properties()
                    .food(ModFoodComponents.BYTE)
                    .setId(ResourceKey.create(Registries.ITEM, BYTE_ID))));
    public static final Identifier COAL_DUST_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "coal_dust");
    public static final Item COAL_DUST = registerItem(COAL_DUST_ID, new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, COAL_DUST_ID))));
    public static final Identifier LIGHTCYCLE_BATON_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "lightcycle_baton");
    public static final Item LIGHTCYCLE_BATON = registerItem(LIGHTCYCLE_BATON_ID, new LightCycleBaton(new Item.Properties()
            .stacksTo(1)
            .setId(ResourceKey.create(Registries.ITEM, LIGHTCYCLE_BATON_ID))));
    public static final Identifier DIMENSION_TELEPORTER_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "dimension_teleporter");
    public static final Item DIMENSION_TELEPORTER = registerItem(DIMENSION_TELEPORTER_ID, new DimensionTeleporterItem(new Item.Properties()
            .stacksTo(1)
            .setId(ResourceKey.create(Registries.ITEM, DIMENSION_TELEPORTER_ID))));
    public static final Identifier LASER_TUNER_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "laser_tuner");
    public static final Item LASER_TUNER = registerItem(LASER_TUNER_ID, new Item(new Item.Properties()
            .stacksTo(1)
            .setId(ResourceKey.create(Registries.ITEM, LASER_TUNER_ID))));
    public static final Identifier ESCAPE_KEY_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "escape_key");
    public static final Item ESCAPE_KEY = registerItem(ESCAPE_KEY_ID, new EscapeKeyItem(new Item.Properties()
            .stacksTo(16)
            .rarity(Rarity.RARE)
            .setId(ResourceKey.create(Registries.ITEM, ESCAPE_KEY_ID))));
    public static final Identifier MEMORY_PAGING_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "memory_paging");
    public static final Item MEMORY_PAGING = registerItem(MEMORY_PAGING_ID, new Item(new Item.Properties()
            .stacksTo(16)
            .rarity(Rarity.RARE)
            .setId(ResourceKey.create(Registries.ITEM, MEMORY_PAGING_ID))));
    // Memory Paging that also keeps the player's inventory and experience
    public static final Identifier CORE_DUMP_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "core_dump");
    public static final Item CORE_DUMP = registerItem(CORE_DUMP_ID, new Item(new Item.Properties()
            .stacksTo(16)
            .rarity(Rarity.EPIC)
            .setId(ResourceKey.create(Registries.ITEM, CORE_DUMP_ID))));

    private static Item registerItem(Identifier id, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void registerModItems() {
        DigitalFrontier.LOGGER.info("Registering Mod Items for " + DigitalFrontier.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(ModItems::addItemsToIngredientTabItemGroup);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(ModItems::addItemsToFoodTabItemGroup);
    }

    private static void addItemsToIngredientTabItemGroup(FabricItemGroupEntries entries) {
        entries.accept(BINARY_0);
        entries.accept(BINARY_1);
        entries.accept(PIXEL_DUST);
        entries.accept(LUMINESSENCE);
        entries.accept(COAL_DUST);
    }

    private static void addItemsToFoodTabItemGroup(FabricItemGroupEntries entries) {
        entries.accept(BYTE);
    }
}
