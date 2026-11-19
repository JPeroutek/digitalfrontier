package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.block.custom.MovingLightBlock;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.core.CoreManager;
import com.partatoes.digitalfrontier.grid.core.CoreState;
import com.partatoes.digitalfrontier.grid.layout.CoreChamber;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Bool: a small floating program that can only say yes or no. Befriended with Luminessence, it follows its owner
 * and answers the one question that matters: "no" (red, spiky, a low buzz) when something hostile is within 16
 * blocks of its owner, "yes" (bright, a chime) when its owner faces the nearest stronghold still standing. Wild ones
 * drift about the clean streets of the city. It keeps out of fights: while its owner is in one it hovers above, out
 * of reach, and only players other than its owner can hurt it.
 */
public class BoolEntity extends TamableAnimal implements MovingLightBlock.Emitter {
    // How brightly a Bool lights the space around it
    private static final int LIGHT = 10;
    private static final EntityDataAccessor<Byte> SIGNAL = SynchedEntityData.defineId(BoolEntity.class, EntityDataSerializers.BYTE);
    private static final double THREAT_RANGE = 16;
    // How recently its owner must have hit or been hit for it to keep clear
    private static final int COMBAT_TICKS = 100;

    public BoolEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createBoolAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, world);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SIGNAL, (byte) BoolSignals.Signal.IDLE.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new KeepClearGoal());
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, 6, 2));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
    }

    public BoolSignals.Signal signal() {
        return BoolSignals.Signal.byId(entityData.get(SIGNAL));
    }

    @Override
    public int emittedLight() {
        return LIGHT;
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (tickCount % 2 == 0) {
            MovingLightBlock.shine(world, blockPosition(), LIGHT);
        }
        if (tickCount % 10 != 0) {
            return;
        }
        BoolSignals.Signal signal = isTame() && getOwner() instanceof Player owner && owner.level() == world
                ? signalFor(world, owner)
                : BoolSignals.Signal.IDLE;
        if (signal != signal()) {
            entityData.set(SIGNAL, (byte) signal.ordinal());
            switch (signal) {
                case YES -> world.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 1.4f);
                case NO -> world.playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.NEUTRAL, 1.0f, 0.5f);
                default -> {
                }
            }
        }
    }

    private BoolSignals.Signal signalFor(ServerLevel world, Player owner) {
        boolean threat = !world.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(THREAT_RANGE),
                entity -> entity.isAlive() && (entity instanceof CorruptedProgramEntity || entity instanceof EnforcerEntity
                        || entity instanceof RecruiterEntity || entity instanceof LieutenantEntity)).isEmpty();
        BlockPos goal = goal(world, owner);
        double dx = goal == null ? Double.NaN : goal.getX() + 0.5 - owner.getX();
        double dz = goal == null ? Double.NaN : goal.getZ() + 0.5 - owner.getZ();
        return BoolSignals.signal(threat, dx, dz, owner.getYHeadRot());
    }

    // Where its owner should head: the nearest stronghold whose lieutenant still stands, or once the regime's
    // lieutenants are gone and its core is awake, the core
    @Nullable
    private static BlockPos goal(ServerLevel world, Player owner) {
        if (GridArrival.generator(world) == null) {
            return null;
        }
        if (CoreManager.phase(world) == CoreState.Phase.AWAKE) {
            return CoreChamber.core();
        }
        return StrongholdManager.strongholds(world).stream()
                .filter(view -> view.stronghold().alive())
                .map(StrongholdManager.StrongholdView::center)
                .min((a, b) -> Double.compare(a.distToCenterSqr(owner.position()), b.distToCenterSqr(owner.position())))
                .orElse(null);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isTame() && stack.is(ModItems.LUMINESSENCE)) {
            if (level() instanceof ServerLevel) {
                stack.consume(1, player);
                // A chance per piece, like taming a wolf
                if (random.nextInt(3) == 0) {
                    tame(player);
                    navigation.stop();
                    setOrderedToSit(false);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(player) && stack.isEmpty()) {
            if (level() instanceof ServerLevel) {
                setOrderedToSit(!isOrderedToSit());
                navigation.stop();
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // Only players other than its owner can hurt it (and /kill and the void): mobs never go for it, and it can't be
    // caught in its owner's swings
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        boolean byStranger = source.getEntity() instanceof Player player && !isOwnedBy(player);
        if (!byStranger && !source.is(DamageTypes.GENERIC_KILL) && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        return super.hurtServer(world, source, amount);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob partner) {
        return null;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    // While its owner is fighting, it hangs back above them
    private class KeepClearGoal extends Goal {
        KeepClearGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean ownerFighting() {
            if (!isTame() || isOrderedToSit() || !(getOwner() instanceof Player owner)) {
                return false;
            }
            return owner.tickCount - owner.getLastHurtByMobTimestamp() < COMBAT_TICKS && owner.getLastHurtByMob() != null
                    || owner.tickCount - owner.getLastHurtMobTimestamp() < COMBAT_TICKS && owner.getLastHurtMob() != null;
        }

        @Override
        public boolean canUse() {
            return ownerFighting();
        }

        @Override
        public void tick() {
            if (getOwner() instanceof Player owner && tickCount % 10 == 0) {
                Vec3 behind = owner.position().subtract(owner.getLookAngle().multiply(3, 0, 3)).add(0, 4, 0);
                getNavigation().moveTo(behind.x, behind.y, behind.z, 1.2);
            }
        }
    }
}
