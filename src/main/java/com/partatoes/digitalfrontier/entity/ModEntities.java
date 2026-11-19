package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.entity.projectile.ThrownLightGrenade;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.custom.BoolEntity;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.EnforcerEntity;
import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.RecruiterEntity;
import com.partatoes.digitalfrontier.entity.custom.SystemCoreEntity;
import com.partatoes.digitalfrontier.entity.projectile.ThrownDataDisc;
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
    public static final Identifier RECRUITER_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "recruiter");
    public static final Identifier BOOL_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "bool");
    public static final Identifier DATA_DISC_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "data_disc");
    public static final Identifier SYSTEM_CORE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "system_core");

    public static final EntityType<ProgramEntity> PROGRAM = register(PROGRAM_ID,
            EntityType.Builder.of(ProgramEntity::new, MobCategory.MISC));
    public static final EntityType<CorruptedProgramEntity> CORRUPTED_PROGRAM = register(CORRUPTED_PROGRAM_ID,
            EntityType.Builder.of(CorruptedProgramEntity::new, MobCategory.MISC).sized(0.6f, 1.8f));
    public static final EntityType<EnforcerEntity> ENFORCER = register(ENFORCER_ID,
            EntityType.Builder.of(EnforcerEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f));
    public static final EntityType<LieutenantEntity> LIEUTENANT = register(LIEUTENANT_ID,
            EntityType.Builder.of(LieutenantEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10));
    public static final EntityType<RecruiterEntity> RECRUITER = register(RECRUITER_ID,
            EntityType.Builder.of(RecruiterEntity::new, MobCategory.MISC).sized(0.6f, 1.8f));
    public static final EntityType<BoolEntity> BOOL = register(BOOL_ID,
            EntityType.Builder.of(BoolEntity::new, MobCategory.CREATURE).sized(0.5f, 0.5f).clientTrackingRange(10));
    public static final EntityType<ThrownDataDisc> DATA_DISC = register(DATA_DISC_ID,
            EntityType.Builder.<ThrownDataDisc>of(ThrownDataDisc::new, MobCategory.MISC).sized(0.5f, 0.25f).clientTrackingRange(4).updateInterval(2));
    public static final Identifier LIGHT_GRENADE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "light_grenade");
    public static final EntityType<ThrownLightGrenade> LIGHT_GRENADE = register(LIGHT_GRENADE_ID,
            EntityType.Builder.<ThrownLightGrenade>of(ThrownLightGrenade::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10));
    public static final EntityType<SystemCoreEntity> SYSTEM_CORE = register(SYSTEM_CORE_ID,
            EntityType.Builder.of(SystemCoreEntity::new, MobCategory.MONSTER).sized(2.0f, 4.0f).fireImmune().clientTrackingRange(10));

    private static <T extends Entity> EntityType<T> register(Identifier id, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    public static void registerModEntities() {
        DigitalFrontier.LOGGER.info("Registering ModEntities for " + DigitalFrontier.MOD_ID);
        FabricDefaultAttributeRegistry.register(PROGRAM, ProgramEntity.createProgramAttributes());
        FabricDefaultAttributeRegistry.register(CORRUPTED_PROGRAM, CorruptedProgramEntity.createCorruptedProgramAttributes());
        FabricDefaultAttributeRegistry.register(ENFORCER, EnforcerEntity.createEnforcerAttributes());
        FabricDefaultAttributeRegistry.register(LIEUTENANT, LieutenantEntity.createLieutenantAttributes());
        FabricDefaultAttributeRegistry.register(RECRUITER, RecruiterEntity.createRecruiterAttributes());
        FabricDefaultAttributeRegistry.register(BOOL, BoolEntity.createBoolAttributes());
        FabricDefaultAttributeRegistry.register(SYSTEM_CORE, SystemCoreEntity.createSystemCoreAttributes());
    }
}
