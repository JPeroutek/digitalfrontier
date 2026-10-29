package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.grid.stronghold.LieutenantDef;
import com.partatoes.digitalfrontier.grid.stronghold.Lieutenants;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * A stronghold's lieutenant: a named boss spawned by {@link StrongholdManager} from a {@link LieutenantDef}. Its
 * name, skin and health come from the definition; how it fights from the definition's archetype. It stays near its
 * stronghold. What it drops when derezzed is its definition's loot table, or {@code digitalfrontier:entities/lieutenant}.
 */
public class LieutenantEntity extends HostileEntity {
    private static final TrackedData<String> TEXTURE = DataTracker.registerData(LieutenantEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final int LEASH = 24;
    private static final int MAX_ENFORCERS = 4;

    private final ServerBossBar bossBar = new ServerBossBar(Text.empty(), BossBar.Color.RED, BossBar.Style.NOTCHED_10);
    @Nullable
    private Identifier lieutenantId;
    @Nullable
    private String strongholdId;
    private LieutenantDef.Archetype archetype = LieutenantDef.Archetype.RULER;
    private BlockPos home = BlockPos.ORIGIN;
    private int abilityCooldown = 100;

    public LieutenantEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100;
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createLieutenantAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 300)
                .add(EntityAttributes.ATTACK_DAMAGE, 10)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.ARMOR, 8)
                .add(EntityAttributes.FOLLOW_RANGE, 40)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.8);
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
        dataTracker.set(TEXTURE, def.textureOrDefault().toString());
        getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(def.healthOrDefault());
        getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(def.archetype().speed());
        setHealth(getMaxHealth());
    }

    @Nullable
    public String strongholdId() {
        return strongholdId;
    }

    @Nullable
    public Identifier texture() {
        String texture = dataTracker.get(TEXTURE);
        return texture.isEmpty() ? null : Identifier.tryParse(texture);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TEXTURE, "");
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.add(3, new ReturnHomeGoal());
        this.goalSelector.add(5, new WanderAroundGoal(this, 0.6));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);
        // A copy the stronghold no longer expects (defeated, respawned elsewhere, duplicated) removes itself
        if (age % 20 == 0 && !StrongholdManager.isCurrent(this)) {
            discard();
            return;
        }
        bossBar.setName(getDisplayName());
        bossBar.setPercent(getHealth() / getMaxHealth());

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
    private void summonEnforcers(ServerWorld world) {
        abilityCooldown = 300;
        int nearby = world.getEntitiesByClass(EnforcerEntity.class, getBoundingBox().expand(24), entity -> true).size();
        for (int i = 0; i < 2 && nearby + i < MAX_ENFORCERS; i++) {
            EnforcerEntity enforcer = ModEntities.ENFORCER.create(world, SpawnReason.MOB_SUMMONED);
            if (enforcer == null) {
                return;
            }
            enforcer.refreshPositionAndAngles(getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3, getYaw(), 0);
            enforcer.setTarget(getTarget());
            world.spawnEntity(enforcer);
        }
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.5f, 0.5f);
    }

    // The duelist closes distance in a sudden leap
    private void dashAt(LivingEntity target) {
        double distance = distanceTo(target);
        if (distance < 4 || distance > 14) {
            abilityCooldown = 10;
            return;
        }
        abilityCooldown = 80;
        Vec3d toward = target.getEntityPos().subtract(getEntityPos()).normalize();
        setVelocity(toward.x * 1.6, 0.35, toward.z * 1.6);
        velocityModified = true;
        getEntityWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BREEZE_WIND_BURST, SoundCategory.HOSTILE, 1.0f, 1.4f);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (getEntityWorld() instanceof ServerWorld) {
            StrongholdManager.onDefeated(this);
        }
    }

    // A definition can name its own loot table; vanilla's death loot would always use the entity type's
    @Override
    protected void dropLoot(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        Lieutenants.Lieutenant lieutenant = lieutenantId == null ? null : Lieutenants.get(lieutenantId);
        Optional<RegistryKey<LootTable>> table = lieutenant == null ? Optional.empty() : lieutenant.def().lootTable();
        if (table.isEmpty()) {
            super.dropLoot(world, source, causedByPlayer);
            return;
        }
        LootWorldContext.Builder context = new LootWorldContext.Builder(world)
                .add(LootContextParameters.THIS_ENTITY, this)
                .add(LootContextParameters.ORIGIN, getEntityPos())
                .add(LootContextParameters.DAMAGE_SOURCE, source)
                .addOptional(LootContextParameters.ATTACKING_ENTITY, source.getAttacker())
                .addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, source.getSource());
        PlayerEntity attackingPlayer = getAttackingPlayer();
        if (causedByPlayer && attackingPlayer != null) {
            context = context.add(LootContextParameters.LAST_DAMAGE_PLAYER, attackingPlayer).luck(attackingPlayer.getLuck());
        }
        world.getServer().getReloadableRegistries().getLootTable(table.get())
                .generateLoot(context.build(LootContextTypes.ENTITY), getLootTableSeed(), stack -> dropStack(world, stack));
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        if (lieutenantId != null) {
            view.putString("Lieutenant", lieutenantId.toString());
        }
        if (strongholdId != null) {
            view.putString("Stronghold", strongholdId);
        }
        view.putString("Archetype", archetype.asString());
        view.put("Home", BlockPos.CODEC, home);
        view.putString("Texture", dataTracker.get(TEXTURE));
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        lieutenantId = view.getOptionalString("Lieutenant").map(Identifier::tryParse).orElse(null);
        strongholdId = view.getOptionalString("Stronghold").orElse(null);
        for (LieutenantDef.Archetype candidate : LieutenantDef.Archetype.values()) {
            if (candidate.asString().equals(view.getString("Archetype", ""))) {
                archetype = candidate;
            }
        }
        view.read("Home", BlockPos.CODEC).ifPresent(pos -> home = pos);
        dataTracker.set(TEXTURE, view.getString("Texture", ""));
        if (hasCustomName()) {
            bossBar.setName(getDisplayName());
        }
    }

    private class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() {
            setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return getTarget() == null && !home.equals(BlockPos.ORIGIN) && home.getSquaredDistance(getEntityPos()) > LEASH * LEASH;
        }

        @Override
        public void start() {
            getNavigation().startMovingTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean shouldContinue() {
            return getTarget() == null && !getNavigation().isIdle();
        }
    }
}
