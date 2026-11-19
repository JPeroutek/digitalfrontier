package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.entity.vehicle.RecognizerEntity;
import com.partatoes.digitalfrontier.effect.Overclock;
import com.partatoes.digitalfrontier.entity.projectile.ThrownLightGrenade;
import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.regime.Sentiment;
import com.partatoes.digitalfrontier.grid.squad.SquadRoles;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

/**
 * The regime's sentries, patrolling the ground it controls (see {@link com.partatoes.digitalfrontier.grid.layout.ControlField}):
 * disciplined, not decayed. They patrol in squads (formed by {@code GridSpawner}), each
 * with a leader the others keep close to; kill the leader and the rest scatter. A squad member has a role
 * ({@link SquadRoles}): plain, a staff that reaches and blocks, a slow armoured heavy, or a grenadier who keeps its
 * distance and throws Light Grenades. They attack players the regime doesn't tolerate (see
 * {@link com.partatoes.digitalfrontier.grid.regime.RegimeRules.Alignment}), hunting the ones it wants from twice as
 * far, and rebel programs. Conscripts are programs the regime recruited: they stay for good, and count toward its
 * numbers.
 */
public class EnforcerEntity extends Monster {
    private static final EntityDataAccessor<Byte> ROLE = SynchedEntityData.defineId(EnforcerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> LEADER = SynchedEntityData.defineId(EnforcerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int FOLLOW_RANGE = 32;
    // How close squad members keep to their leader, and how far a leader's fall is felt
    private static final double FOLLOW_LEADER = 10;
    private static final double SCATTER_RANGE = 24;
    private static final int SCATTER_TICKS = 160;
    private static final int GRENADE_EVERY = 60;
    private static final int BLOCK_TICKS = 30;

    private boolean conscripted;
    // Whether its role's gear and strength have been given (a role set by hand, as with /summon, is applied on load)
    private boolean kitted;
    @Nullable
    private UUID squad;
    private int scatterTicks;
    @Nullable
    private Vec3 scatterFrom;
    private int blockTicks;

    public EnforcerEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, (byte) SquadRoles.PLAIN.ordinal());
        builder.define(LEADER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ScatterGoal());
        this.goalSelector.addGoal(2, new ThrowGrenadeGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(4, new FollowLeaderGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (target, world) -> Regime.alignment(target) < Regime.rules().alignment().toleratedFrom() || Overclock.marked(target)
                        // A player in a Recognizer's cockpit has stolen it
                        || target.getVehicle() instanceof RecognizerEntity recognizer && recognizer.getControllingPassenger() == target));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ProgramEntity.class, true,
                (target, world) -> target instanceof ProgramEntity program && program.sentiment() <= Sentiment.REBEL));
        // And lieutenants that have turned on it
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LieutenantEntity.class, true,
                (target, world) -> target instanceof LieutenantEntity lieutenant && lieutenant.redeemed()));
    }

    public static AttributeSupplier.Builder createEnforcerAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    public SquadRoles role() {
        return SquadRoles.values()[Math.clamp(entityData.get(ROLE), 0, SquadRoles.values().length - 1)];
    }

    public boolean leader() {
        return entityData.get(LEADER);
    }

    /**
     * Makes this enforcer a member of a squad, in a role, perhaps its leader: equips it and sets its strength to
     * match. Called once, as the squad forms; what it sets is saved with the enforcer.
     */
    public void joinSquad(@Nullable UUID squad, SquadRoles role, boolean leader) {
        this.squad = squad;
        this.kitted = true;
        entityData.set(ROLE, (byte) role.ordinal());
        entityData.set(LEADER, leader);
        switch (role) {
            case STAFF -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.LIGHT_STAFF));
            case GRENADIER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.LIGHT_GRENADE));
            case HEAVY -> {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(50);
                getAttribute(Attributes.ARMOR).setBaseValue(10);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22);
                getAttribute(Attributes.SCALE).setBaseValue(1.15);
            }
            default -> {
            }
        }
        if (leader) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(getAttribute(Attributes.MAX_HEALTH).getBaseValue() + 10);
        }
        setDropChance(EquipmentSlot.MAINHAND, 0.05f);
        setHealth(getMaxHealth());
    }

    @Nullable
    public UUID squad() {
        return squad;
    }

    // The System Core's stray bolts pass harmlessly, or the squads it calls would turn on it. A staff raises its guard
    // when struck
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (SystemCoreEntity.firedBy(source)) {
            return false;
        }
        boolean hurt = super.hurtServer(world, source, amount);
        if (hurt && role() == SquadRoles.STAFF && blockTicks == 0 && random.nextFloat() < 0.5f) {
            startUsingItem(InteractionHand.MAIN_HAND);
            blockTicks = BLOCK_TICKS;
        }
        return hurt;
    }

    // A fallen leader scatters its squad
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (leader() && squad != null && level() instanceof ServerLevel world) {
            Vec3 from = damageSource.getEntity() != null ? damageSource.getEntity().position() : position();
            for (EnforcerEntity member : world.getEntitiesOfClass(EnforcerEntity.class, getBoundingBox().inflate(SCATTER_RANGE),
                    other -> other != this && squad.equals(other.squad))) {
                member.scatter(from);
            }
        }
    }

    private void scatter(Vec3 from) {
        scatterTicks = SCATTER_TICKS;
        scatterFrom = from;
        setTarget(null);
        squad = null;
    }

    public boolean conscripted() {
        return conscripted;
    }

    public void setConscripted(boolean conscripted) {
        this.conscripted = conscripted;
    }

    // Players the regime wants are hunted from twice the usual range, seen or not
    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (blockTicks > 0 && --blockTicks == 0) {
            stopUsingItem();
        }
        if (scatterTicks > 0) {
            scatterTicks--;
            setTarget(null);
            return;
        }
        if (tickCount % 20 != 0 || getTarget() != null) {
            return;
        }
        float wantedBelow = Regime.rules().alignment().wantedBelow();
        Player wanted = world.getNearestPlayer(getX(), getY(), getZ(), FOLLOW_RANGE * 2,
                player -> player instanceof Player p && !p.isSpectator() && !p.isCreative() && Regime.alignment(p) < wantedBelow);
        if (wanted != null) {
            setTarget(wanted);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putBoolean("Conscripted", conscripted);
        view.putString("Role", role().name().toLowerCase(Locale.ROOT));
        view.putBoolean("Leader", leader());
        view.putBoolean("Kitted", kitted);
        if (squad != null) {
            view.store("Squad", UUIDUtil.CODEC, squad);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        conscripted = view.getBooleanOr("Conscripted", false);
        String role = view.getStringOr("Role", "plain");
        for (SquadRoles value : SquadRoles.values()) {
            if (value.name().equalsIgnoreCase(role)) {
                entityData.set(ROLE, (byte) value.ordinal());
            }
        }
        entityData.set(LEADER, view.getBooleanOr("Leader", false));
        squad = view.read("Squad", UUIDUtil.CODEC).orElse(null);
        if (!view.getBooleanOr("Kitted", false) && (role() != SquadRoles.PLAIN || leader())) {
            joinSquad(squad, role(), leader());
        }
    }

    // After its leader falls: runs from where the blow came from, for a while
    private class ScatterGoal extends Goal {
        ScatterGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return scatterTicks > 0 && scatterFrom != null;
        }

        @Override
        public void start() {
            Vec3 away = DefaultRandomPos.getPosAway(EnforcerEntity.this, 16, 7, scatterFrom);
            if (away != null) {
                getNavigation().moveTo(away.x, away.y, away.z, 1.3);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return scatterTicks > 0 && !getNavigation().isDone();
        }
    }

    // Grenadiers keep their distance and lob Light Grenades; up close, they fall back on their fists
    private class ThrowGrenadeGoal extends Goal {
        private int cooldown;

        ThrowGrenadeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return role() == SquadRoles.GRENADIER && target != null && target.isAlive() && distanceToSqr(target) > 16;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            double distance = distanceTo(target);
            if (distance < 8) {
                Vec3 away = DefaultRandomPos.getPosAway(EnforcerEntity.this, 8, 4, target.position());
                if (away != null) {
                    getNavigation().moveTo(away.x, away.y, away.z, 1.1);
                }
            } else if (distance > 14) {
                getNavigation().moveTo(target, 1.0);
            } else {
                getNavigation().stop();
            }
            if (--cooldown <= 0 && hasLineOfSight(target) && level() instanceof ServerLevel world) {
                cooldown = GRENADE_EVERY;
                ThrownLightGrenade grenade = new ThrownLightGrenade(EnforcerEntity.this, world, new ItemStack(ModItems.LIGHT_GRENADE));
                double dx = target.getX() - getX();
                double dz = target.getZ() - getZ();
                double dy = target.getY() - grenade.getY();
                grenade.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.25, dz, 0.9f, 4);
                world.addFreshEntity(grenade);
                world.playSound(null, getX(), getY(), getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 0.6f, 0.6f);
            }
        }
    }

    // Squad members keep near their leader
    private class FollowLeaderGoal extends Goal {
        @Nullable
        private EnforcerEntity leader;

        FollowLeaderGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (leader() || squad == null || getTarget() != null || tickCount % 20 != 0) {
                return false;
            }
            leader = level().getEntitiesOfClass(EnforcerEntity.class, getBoundingBox().inflate(FOLLOW_RANGE),
                    other -> other.leader() && squad.equals(other.squad)).stream().findFirst().orElse(null);
            return leader != null && distanceToSqr(leader) > FOLLOW_LEADER * FOLLOW_LEADER;
        }

        @Override
        public void start() {
            if (leader != null) {
                getNavigation().moveTo(leader, 1.0);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return leader != null && leader.isAlive() && getTarget() == null && !getNavigation().isDone()
                    && distanceToSqr(leader) > 16;
        }
    }
}
