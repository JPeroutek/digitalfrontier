package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModVehicles {
    public static final Identifier LIGHTCYCLE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "lightcycle");

    public static final EntityType<LightCycleEntity> LIGHTCYCLE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            LIGHTCYCLE_ID,
            EntityType.Builder.of(LightCycleEntity::new, MobCategory.MISC)
                    .sized(1f, 1f)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, LIGHTCYCLE_ID)));

    public static void registerModVehicles() {
        DigitalFrontier.LOGGER.info("Registering ModVehicles for " + DigitalFrontier.MOD_ID);
    }
}
