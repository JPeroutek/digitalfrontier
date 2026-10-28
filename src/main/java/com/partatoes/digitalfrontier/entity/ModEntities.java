package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.EnforcerEntity;
import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final Identifier PROGRAM_ID = Identifier.of(DigitalFrontier.MOD_ID, "program");
    public static final Identifier CORRUPTED_PROGRAM_ID = Identifier.of(DigitalFrontier.MOD_ID, "corrupted_program");
    public static final Identifier ENFORCER_ID = Identifier.of(DigitalFrontier.MOD_ID, "enforcer");
    public static final Identifier LIEUTENANT_ID = Identifier.of(DigitalFrontier.MOD_ID, "lieutenant");

    public static final EntityType<ProgramEntity> PROGRAM = register(PROGRAM_ID,
            EntityType.Builder.create(ProgramEntity::new, SpawnGroup.MISC));
    public static final EntityType<CorruptedProgramEntity> CORRUPTED_PROGRAM = register(CORRUPTED_PROGRAM_ID,
            EntityType.Builder.create(CorruptedProgramEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.8f));
    public static final EntityType<EnforcerEntity> ENFORCER = register(ENFORCER_ID,
            EntityType.Builder.create(EnforcerEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.8f));
    public static final EntityType<LieutenantEntity> LIEUTENANT = register(LIEUTENANT_ID,
            EntityType.Builder.create(LieutenantEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.95f).maxTrackingRange(10));

    private static <T extends Entity> EntityType<T> register(Identifier id, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, id, builder.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id)));
    }

    public static void registerModEntities() {
        DigitalFrontier.LOGGER.info("Registering ModEntities for " + DigitalFrontier.MOD_ID);
        FabricDefaultAttributeRegistry.register(PROGRAM, ProgramEntity.createProgramAttributes());
        FabricDefaultAttributeRegistry.register(CORRUPTED_PROGRAM, CorruptedProgramEntity.createCorruptedProgramAttributes());
        FabricDefaultAttributeRegistry.register(ENFORCER, EnforcerEntity.createEnforcerAttributes());
        FabricDefaultAttributeRegistry.register(LIEUTENANT, LieutenantEntity.createLieutenantAttributes());
    }
}
