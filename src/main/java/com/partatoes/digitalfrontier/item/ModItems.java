package com.partatoes.digitalfrontier.item;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.item.custom.DataDiscItem;
import com.partatoes.digitalfrontier.item.custom.DimensionTeleporterItem;
import com.partatoes.digitalfrontier.item.custom.EscapeKeyItem;
import com.partatoes.digitalfrontier.item.custom.LightCycleBaton;
import com.partatoes.digitalfrontier.item.custom.LuminanceOreDetector;
import com.partatoes.digitalfrontier.item.custom.LightGrenadeItem;
import com.partatoes.digitalfrontier.item.custom.CestaItem;
import com.partatoes.digitalfrontier.effect.Overclock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
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
            .cookingFuel(ModFuelItems.COAL_DUST_BURN_TIME)
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

    // A throwing disc that ricochets and returns; in the offhand it stands in for a Core Dump once
    public static final Identifier DATA_DISC_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "data_disc");
    public static final Item DATA_DISC = registerItem(DATA_DISC_ID, new DataDiscItem(new Item.Properties()
            .durability(250)
            .repairable(LUMINESSENCE)
            .rarity(Rarity.UNCOMMON)
            .setId(ResourceKey.create(Registries.ITEM, DATA_DISC_ID))));

    // Gear. The staff hits from further off and blocks from the front; the shield also reflects what hits its face
    // (see ForceShield); the chute glides and opens by itself in a fall (see Chute)
    public static final Identifier LIGHT_STAFF_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "light_staff");
    public static final Item LIGHT_STAFF = registerItem(LIGHT_STAFF_ID, new Item(new Item.Properties()
            .sword(ToolMaterial.IRON, 3, -3.0f)
            .component(DataComponents.ATTACK_RANGE, new AttackRange(0, 4.5f, 0, 6.5f, 0.125f, 0.5f))
            .component(DataComponents.BLOCKS_ATTACKS, blocking(0.6f, 0.4f))
            .repairable(LUMINESSENCE)
            .setId(ResourceKey.create(Registries.ITEM, LIGHT_STAFF_ID))));
    public static final Identifier LIGHT_GRENADE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "light_grenade");
    public static final Item LIGHT_GRENADE = registerItem(LIGHT_GRENADE_ID, new LightGrenadeItem(new Item.Properties()
            .stacksTo(16)
            .setId(ResourceKey.create(Registries.ITEM, LIGHT_GRENADE_ID))));
    public static final Identifier FORCE_SHIELD_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "force_shield");
    public static final Item FORCE_SHIELD = registerItem(FORCE_SHIELD_ID, new Item(new Item.Properties()
            .durability(400)
            .component(DataComponents.BLOCKS_ATTACKS, blocking(1.0f, 0.25f))
            .equippableUnswappable(EquipmentSlot.OFFHAND)
            .repairable(LUMINESSENCE)
            .setId(ResourceKey.create(Registries.ITEM, FORCE_SHIELD_ID))));
    public static final Identifier CHUTE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "chute");
    public static final Item CHUTE = registerItem(CHUTE_ID, new Item(new Item.Properties()
            .durability(120)
            .component(DataComponents.GLIDER, Unit.INSTANCE)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                    .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
                    .setAsset(ResourceKey.create(equipmentAssets(), CHUTE_ID))
                    .setDamageOnHurt(false)
                    .build())
            .repairable(LUMINESSENCE)
            .setId(ResourceKey.create(Registries.ITEM, CHUTE_ID))));
    // A minute of strength and speed, paid for in decay (see Overclock)
    public static final Identifier OVERCLOCK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "overclock");
    public static final Item OVERCLOCK = registerItem(OVERCLOCK_ID, new Item(new Item.Properties()
            .stacksTo(16)
            .component(DataComponents.CONSUMABLE, Consumables.defaultDrink()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(Overclock.OVERCLOCKED, 1200)))
                    .build())
            .rarity(Rarity.UNCOMMON)
            .setId(ResourceKey.create(Registries.ITEM, OVERCLOCK_ID))));

    // The ring game (see RingGame): the scoop that catches and throws its ball, and the ball itself
    public static final Identifier CESTA_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "cesta");
    public static final Item CESTA = registerItem(CESTA_ID, new CestaItem(new Item.Properties()
            .stacksTo(1)
            .setId(ResourceKey.create(Registries.ITEM, CESTA_ID))));
    public static final Identifier RING_BALL_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "ring_ball");
    public static final Item RING_BALL = registerItem(RING_BALL_ID, new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, RING_BALL_ID))));

    // Blocks from the front like a shield, taking this share of the damage off (all of it, for the shield)
    private static BlocksAttacks blocking(float share, float delaySeconds) {
        return new BlocksAttacks(delaySeconds, 1.0f, List.of(new BlocksAttacks.DamageReduction(90, Optional.empty(), 0, share)),
                new BlocksAttacks.ItemDamageFunction(3, 1, 1), Optional.empty(),
                Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK));
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<Registry<EquipmentAsset>> equipmentAssets() {
        return (ResourceKey<Registry<EquipmentAsset>>) EquipmentAssets.ROOT_ID;
    }

    private static Item registerItem(Identifier id, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void registerModItems() {
        DigitalFrontier.LOGGER.info("Registering Mod Items for " + DigitalFrontier.MOD_ID);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(ModItems::addItemsToIngredientTabItemGroup);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(ModItems::addItemsToFoodTabItemGroup);
    }

    private static void addItemsToIngredientTabItemGroup(FabricCreativeModeTabOutput entries) {
        entries.accept(BINARY_0);
        entries.accept(BINARY_1);
        entries.accept(PIXEL_DUST);
        entries.accept(LUMINESSENCE);
        entries.accept(COAL_DUST);
    }

    private static void addItemsToFoodTabItemGroup(FabricCreativeModeTabOutput entries) {
        entries.accept(BYTE);
    }
}
