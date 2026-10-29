package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.grid.stronghold.LieutenantDef;
import com.partatoes.digitalfrontier.grid.stronghold.Lieutenants;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * A stronghold's lieutenant: a named boss spawned by {@link StrongholdManager} from a {@link LieutenantDef}. Its
 * name, skin and health come from the definition; how it fights from the definition's archetype. It stays near its
 * stronghold. What it drops when derezzed is its definition's loot table, or {@code digitalfrontier:entities/lieutenant}.
 */
public class LieutenantEntity extends Monster {
    private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(LieutenantEntity.class, EntityDataSerializers.STRING);
    private static final int LEASH = 24;
    private static final int MAX_ENFORCERS = 4;

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    @Nullable
    private Identifier lieutenantId;
    @Nullable
    private String strongholdId;
    private LieutenantDef.Archetype archetype = LieutenantDef.Archetype.RULER;
    private BlockPos home = BlockPos.ZERO;
    private int abilityCooldown = 100;

    public LieutenantEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 100;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createLieutenantAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    /** Called once when the stronghold spawns it. */
    public void setup(Lieutenants.Lieutenant lieutenant, String strongholdId, BlockPos home) {
        LieutenantDef def = lieutenant.def();
        this.lieutenantId = lieutenant.id();
        this.strongholdId = strongholdId;
        this.archetype = def.archetype();
        this.home = home;
        setCustomName(def.name());
        setCustomNameVisible(true);
        entityData.set(TEXTURE, def.textureOrDefault().toString());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(def.healthOrDefault());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.archetype().speed());
        setHealth(getMaxHealth());
    }

    @Nullable
    public String strongholdId() {
        return strongholdId;
    }

    @Nullable
    public Identifier texture() {
        String texture = entityData.get(TEXTURE);
        return texture.isEmpty() ? null : Identifier.tryParse(texture);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TEXTURE, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(3, new ReturnHomeGoal());
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        // A copy the stronghold no longer expects (defeated, respawned elsewhere, duplicated) removes itself
        if (tickCount % 20 == 0 && !StrongholdManager.isCurrent(this)) {
            discard();
            return;
        }
        bossBar.setName(getDisplayName());
        bossBar.setProgress(getHealth() / getMaxHealth());

        LivingEntity target = getTarget();
        if (target == null || --abilityCooldown > 0) {
            return;
        }
        switch (archetype) {
            case RULER -> summonEnforcers(world);
            case DUELIST -> dashAt(target);
        }
    }

    // The ruler calls its guard: two enforcers at a time, never more than four around it
    private void summonEnforcers(ServerLevel world) {
        abilityCooldown = 300;
        int nearby = world.getEntitiesOfClass(EnforcerEntity.class, getBoundingBox().inflate(24), entity -> true).size();
        for (int i = 0; i < 2 && nearby + i < MAX_ENFORCERS; i++) {
            EnforcerEntity enforcer = ModEntities.ENFORCER.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (enforcer == null) {
                return;
            }
            enforcer.snapTo(getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3, getYRot(), 0);
            enforcer.setTarget(getTarget());
            world.addFreshEntity(enforcer);
        }
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    // The duelist closes distance in a sudden leap
    private void dashAt(LivingEntity target) {
        double distance = distanceTo(target);
        if (distance < 4 || distance > 14) {
            abilityCooldown = 10;
            return;
        }
        abilityCooldown = 80;
        Vec3 toward = target.position().subtract(position()).normalize();
        setDeltaMovement(toward.x * 1.6, 0.35, toward.z * 1.6);
        needsSync = true;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.HOSTILE, 1.0f, 1.4f);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (level() instanceof ServerLevel) {
            StrongholdManager.onDefeated(this);
        }
    }

    // A definition can name its own loot table; vanilla's death loot would always use the entity type's
    @Override
    protected void dropFromLootTable(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        Lieutenants.Lieutenant lieutenant = lieutenantId == null ? null : Lieutenants.get(lieutenantId);
        Optional<ResourceKey<LootTable>> table = lieutenant == null ? Optional.empty() : lieutenant.def().lootTable();
        if (table.isEmpty()) {
            super.dropFromLootTable(world, source, causedByPlayer);
            return;
        }
        LootParams.Builder context = new LootParams.Builder(world)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
        Player attackingPlayer = getLastHurtByPlayer();
        if (causedByPlayer && attackingPlayer != null) {
            context = context.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, attackingPlayer).withLuck(attackingPlayer.getLuck());
        }
        world.getServer().reloadableRegistries().getLootTable(table.get())
                .getRandomItems(context.create(LootContextParamSets.ENTITY), getLootTableSeed(), stack -> spawnAtLocation(world, stack));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        if (lieutenantId != null) {
            view.putString("Lieutenant", lieutenantId.toString());
        }
        if (strongholdId != null) {
            view.putString("Stronghold", strongholdId);
        }
        view.putString("Archetype", archetype.getSerializedName());
        view.store("Home", BlockPos.CODEC, home);
        view.putString("Texture", entityData.get(TEXTURE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        lieutenantId = view.getString("Lieutenant").map(Identifier::tryParse).orElse(null);
        strongholdId = view.getString("Stronghold").orElse(null);
        for (LieutenantDef.Archetype candidate : LieutenantDef.Archetype.values()) {
            if (candidate.getSerializedName().equals(view.getStringOr("Archetype", ""))) {
                archetype = candidate;
            }
        }
        view.read("Home", BlockPos.CODEC).ifPresent(pos -> home = pos);
        entityData.set(TEXTURE, view.getStringOr("Texture", ""));
        if (hasCustomName()) {
            bossBar.setName(getDisplayName());
        }
    }

    private class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && !home.equals(BlockPos.ZERO) && home.distToCenterSqr(position()) > LEASH * LEASH;
        }

        @Override
        public void start() {
            getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && !getNavigation().isDone();
        }
    }
}
