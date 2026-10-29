package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.EnforcerEntity;
import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final Identifier PROGRAM_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "program");
    public static final Identifier CORRUPTED_PROGRAM_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "corrupted_program");
    public static final Identifier ENFORCER_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "enforcer");
    public static final Identifier LIEUTENANT_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "lieutenant");

    public static final EntityType<ProgramEntity> PROGRAM = register(PROGRAM_ID,
            EntityType.Builder.of(ProgramEntity::new, MobCategory.MISC));
    public static final EntityType<CorruptedProgramEntity> CORRUPTED_PROGRAM = register(CORRUPTED_PROGRAM_ID,
            EntityType.Builder.of(CorruptedProgramEntity::new, MobCategory.MISC).sized(0.6f, 1.8f));
    public static final EntityType<EnforcerEntity> ENFORCER = register(ENFORCER_ID,
            EntityType.Builder.of(EnforcerEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f));
    public static final EntityType<LieutenantEntity> LIEUTENANT = register(LIEUTENANT_ID,
            EntityType.Builder.of(LieutenantEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10));

    private static <T extends Entity> EntityType<T> register(Identifier id, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    public static void registerModEntities() {
        DigitalFrontier.LOGGER.info("Registering ModEntities for " + DigitalFrontier.MOD_ID);
        FabricDefaultAttributeRegistry.register(PROGRAM, ProgramEntity.createProgramAttributes());
        FabricDefaultAttributeRegistry.register(CORRUPTED_PROGRAM, CorruptedProgramEntity.createCorruptedProgramAttributes());
        FabricDefaultAttributeRegistry.register(ENFORCER, EnforcerEntity.createEnforcerAttributes());
        FabricDefaultAttributeRegistry.register(LIEUTENANT, LieutenantEntity.createLieutenantAttributes());
    }
}
