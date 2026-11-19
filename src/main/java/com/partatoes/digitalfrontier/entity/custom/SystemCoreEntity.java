package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.block.custom.MovingLightBlock;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.grid.core.CoreManager;
import com.partatoes.digitalfrontier.grid.layout.CoreChamber;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The System Core: the regime's heart, under the city center (see {@link CoreChamber}). It never moves. Whole, it
 * calls up decayed programs from the chamber's edges and draws every program within reach toward it, absorbing any
 * that touch it to heal, so the fight is partly about keeping them away (Disinfection Pads in the chamber cure the
 * decayed ones before they arrive). Below half health it fires homing bolts that freeze, throw or lift what they hit
 * (see {@link CoreBolt}) and calls enforcer squads; below a fifth, faster, and some bolts black out the view. Its life and death are run by {@link CoreManager}.
 */
public class SystemCoreEntity extends Monster implements MovingLightBlock.Emitter {
    // The core floods its chamber with light, from its middle
    private static final int LIGHT = 15;
    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(SystemCoreEntity.class, EntityDataSerializers.BYTE);
    private static final double PULL_RANGE = 24;
    private static final double ABSORB_RANGE = 2.5;
    private static final float ABSORB_HEAL = 15;
    private static final int MAX_FED = 6;

    private final ServerBossEvent bossBar = new ServerBossEvent(Mth.createInsecureUUID(this.random), Component.empty(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);

    public SystemCoreEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 500;
        setPersistenceRequired();
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createSystemCoreAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, (byte) SystemCorePhase.ABSORBING.ordinal());
    }

    /** Its phase, for the renderer: the shell comes apart once it's exposed. */
    public SystemCorePhase phase() {
        return SystemCorePhase.values()[Math.clamp(entityData.get(PHASE), 0, SystemCorePhase.values().length - 1)];
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        setDeltaMovement(Vec3.ZERO);
        if (tickCount % 2 == 0) {
            MovingLightBlock.shine(world, blockPosition().above(2), LIGHT);
        }
        if (tickCount % 20 == 0 && !CoreManager.isCurrent(this)) {
            discard();
            return;
        }
        SystemCorePhase phase = SystemCorePhase.of(getHealth() / getMaxHealth());
        if (phase.ordinal() != entityData.get(PHASE)) {
            entityData.set(PHASE, (byte) phase.ordinal());
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 0.6f + phase.ordinal() * 0.3f);
        }
        bossBar.setName(getDisplayName());
        bossBar.setProgress(getHealth() / getMaxHealth());

        if (phase.absorbs()) {
            absorb(world);
        }
        if (phase.feedEvery() > 0 && tickCount % phase.feedEvery() == 0) {
            feed(world);
        }
        LivingEntity target = getTarget();
        if (target != null && phase.boltEvery() > 0 && tickCount % phase.boltEvery() == 0) {
            world.addFreshEntity(new CoreBolt(world, this, target, phase.bolt(random.nextDouble())));
            world.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 2.0f, 0.6f);
        }
        if (target != null && phase.squadEvery() > 0 && tickCount % phase.squadEvery() == 0) {
            callSquad(world, target);
        }
    }

    // Draws in every program within reach; any that touches it is absorbed and heals it
    private void absorb(ServerLevel world) {
        for (PathfinderMob program : world.getEntitiesOfClass(PathfinderMob.class, getBoundingBox().inflate(PULL_RANGE),
                mob -> mob instanceof ProgramEntity || mob instanceof CorruptedProgramEntity)) {
            Vec3 toward = position().subtract(program.position());
            if (toward.horizontalDistance() < ABSORB_RANGE + getBbWidth() / 2) {
                world.sendParticles(ParticleTypes.REVERSE_PORTAL, program.getX(), program.getY() + 1, program.getZ(), 30, 0.3, 0.6, 0.3, 0.1);
                world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.5f);
                program.discard();
                heal(ABSORB_HEAL);
            } else if (tickCount % 10 == 0) {
                Vec3 pull = toward.horizontal().normalize().scale(0.25);
                program.setDeltaMovement(program.getDeltaMovement().add(pull));
                program.needsSync = true;
            }
        }
    }

    // Calls up a decayed program at a random point on the chamber's edge, to walk in and be absorbed
    private void feed(ServerLevel world) {
        if (world.getEntitiesOfClass(CorruptedProgramEntity.class, getBoundingBox().inflate(PULL_RANGE * 2), mob -> true).size() >= MAX_FED) {
            return;
        }
        CorruptedProgramEntity program = ModEntities.CORRUPTED_PROGRAM.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (program == null) {
            return;
        }
        BlockPos edge = chamberEdge();
        program.snapTo(edge.getX() + 0.5, CoreChamber.FLOOR, edge.getZ() + 0.5, random.nextFloat() * 360, 0);
        program.setPersistenceRequired();
        world.addFreshEntity(program);
        world.sendParticles(ParticleTypes.SMOKE, program.getX(), program.getY() + 1, program.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
    }

    private BlockPos chamberEdge() {
        int span = CoreChamber.MAX - CoreChamber.MIN;
        int along = CoreChamber.MIN + 1 + random.nextInt(span - 1);
        return switch (random.nextInt(4)) {
            case 0 -> new BlockPos(CoreChamber.MIN + 1, CoreChamber.FLOOR, along);
            case 1 -> new BlockPos(CoreChamber.MAX - 1, CoreChamber.FLOOR, along);
            case 2 -> new BlockPos(along, CoreChamber.FLOOR, CoreChamber.MIN + 1);
            default -> new BlockPos(along, CoreChamber.FLOOR, CoreChamber.MAX - 1);
        };
    }

    // Two enforcers from the chamber's edges, sent at its target
    private void callSquad(ServerLevel world, LivingEntity target) {
        for (int i = 0; i < 2; i++) {
            EnforcerEntity enforcer = ModEntities.ENFORCER.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (enforcer == null) {
                return;
            }
            BlockPos edge = chamberEdge();
            enforcer.snapTo(edge.getX() + 0.5, CoreChamber.FLOOR, edge.getZ() + 0.5, random.nextFloat() * 360, 0);
            enforcer.setTarget(target);
            world.addFreshEntity(enforcer);
        }
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0f, 0.4f);
    }

    @Override
    public int emittedLight() {
        return LIGHT;
    }

    /** Whether damage came from the core or its bolts: the regime's own mobs shrug it off, so they never turn on it. */
    public static boolean firedBy(DamageSource source) {
        return source.getEntity() instanceof SystemCoreEntity;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (level() instanceof ServerLevel) {
            CoreManager.onDefeated(this);
        }
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
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
