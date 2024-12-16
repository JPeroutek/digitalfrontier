package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModVehicles {
    public static final Identifier LIGHTCYCLE_ID = Identifier.of(DigitalFrontier.MOD_ID, "lightcycle");

    public static final EntityType<LightCycleEntity> LIGHTCYCLE = Registry.register(
            Registries.ENTITY_TYPE,
            LIGHTCYCLE_ID,
            EntityType.Builder.create(LightCycleEntity::new, SpawnGroup.MISC)
                    .dimensions(1f, 1f)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, LIGHTCYCLE_ID)));

    public static void registerModVehicles() {
        DigitalFrontier.LOGGER.info("Registering ModVehicles for " + DigitalFrontier.MOD_ID);
    }
}
