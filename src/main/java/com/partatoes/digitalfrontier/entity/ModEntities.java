package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final Identifier PROGRAM_ID = Identifier.of(DigitalFrontier.MOD_ID, "program");

    public static final EntityType<ProgramEntity> PROGRAM = Registry.register(
            Registries.ENTITY_TYPE,
            PROGRAM_ID,
            EntityType.Builder.create(ProgramEntity::new, SpawnGroup.MISC).build());

    public static void registerModEntities() {
        DigitalFrontier.LOGGER.info("Registering ModEntities for " + DigitalFrontier.MOD_ID);
    }
}
