package com.partatoes.digitalfrontier.entity.vehicle;

import com.partatoes.digitalfrontier.block.custom.MovingLightBlock;
import com.partatoes.digitalfrontier.effect.Overclock;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.EnforcerEntity;
import com.partatoes.digitalfrontier.grid.layout.ControlTier;
import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.squad.SquadRoles;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Recognizer: a great flying arch, the regime's eye in the sky. Its pilot, an enforcer in the cockpit, flies it in
 * loops over the regime's land at a height, sweeping a searchlight below. A player caught in the light whom the regime
 * doesn't tolerate is spotted: the Recognizer hangs over them and drops enforcers, two at a time, drifting down on
 * Chutes. Kill the pilot and it settles to the ground, and anyone can climb into the cockpit and fly it (forward and
 * back, turning, jump to climb, sprint to dive); the regime treats a stolen Recognizer as an enemy. It flies like a
 * heavy thing (see {@link RecognizerFlight}), and lights the ground beneath it.
 */
public class RecognizerEntity extends Entity implements MovingLightBlock.Emitter {
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(RecognizerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SEARCHING = SynchedEntityData.defineId(RecognizerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPOTTING = SynchedEntityData.defineId(RecognizerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final float MAX_HEALTH = 120;
    /** How far below it the searchlight reaches, and how wide it is at the bottom. */
    public static final float BEAM_LENGTH = 30;
    public static final float BEAM_RADIUS = 6;
    private static final int CRUISE = 24;
    private static final int PATROL_RADIUS = 48;
    private static final int DROP_EVERY = 600;
    private static final int MAX_DROPPED = 4;
    private static final int LIGHT = 12;

    private boolean crewed;
    @Nullable
    private Vec3 home;
    @Nullable
    private Vec3 patrolGoal;
    private int dropCooldown;
    // Whoever its light has caught, rechecked every half second
    @Nullable
    private Player caught;
    private final List<UUID> dropped = new ArrayList<>();
    // A pilot's controls, from its client
    private boolean left;
    private boolean right;
    private boolean forward;
    private boolean back;
    private boolean up;
    private boolean down;

    public RecognizerEntity(EntityType<?> entityType, Level world) {
        super(entityType, world);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEALTH, MAX_HEALTH);
        builder.define(SEARCHING, false);
        builder.define(SPOTTING, false);
    }

    public float health() {
        return entityData.get(HEALTH);
    }

    /** Whether its searchlight is on: it has a regime pilot. */
    public boolean searching() {
        return entityData.get(SEARCHING);
    }

    /** Whether it has someone in its light right now. */
    public boolean spotting() {
        return entityData.get(SPOTTING);
    }

    /** A player pilot's controls; called on their client every tick they fly it. */
    public void setInputs(boolean left, boolean right, boolean forward, boolean back, boolean up, boolean down) {
        this.left = left;
        this.right = right;
        this.forward = forward;
        this.back = back;
        this.up = up;
        this.down = down;
    }

    @Override
    public int emittedLight() {
        return LIGHT;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity controller = getControllingPassenger();
        if (level() instanceof ServerLevel world) {
            if (!crewed) {
                crew(world);
            }
            entityData.set(SEARCHING, controller instanceof Mob);
            if (controller instanceof Mob) {
                patrol(world);
            } else if (controller == null) {
                settle();
            }
            if (tickCount % 2 == 0) {
                MovingLightBlock.shine(world, blockPosition(), LIGHT);
            }
        } else if (controller instanceof Player && isLocalInstanceAuthoritative()) {
            fly();
        }
    }

    // First time in the world: an enforcer takes the controls, and this spot becomes its beat
    private void crew(ServerLevel world) {
        crewed = true;
        home = position();
        EnforcerEntity pilot = ModEntities.ENFORCER.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (pilot != null) {
            pilot.snapTo(getX(), getY(), getZ(), getYRot(), 0);
            pilot.joinSquad(null, SquadRoles.PLAIN, false);
            pilot.setPersistenceRequired();
            world.addFreshEntity(pilot);
            pilot.startRiding(this, true, false);
        }
    }

    // Under a regime pilot: loops over its beat, and hangs over whoever its light catches
    private void patrol(ServerLevel world) {
        if (home == null) {
            home = position();
        }
        if (tickCount % 10 == 0) {
            caught = caught(world);
            entityData.set(SPOTTING, caught != null);
            if (caught != null && --dropCooldown <= 0) {
                dropCooldown = DROP_EVERY / 10;
                dropEnforcers(world, caught);
            }
        }
        Vec3 goal;
        if (caught != null && caught.isAlive()) {
            goal = caught.position().add(0, CRUISE * 0.75, 0);
        } else {
            if (patrolGoal == null || position().distanceTo(patrolGoal) < 3) {
                double angle = random.nextDouble() * Math.PI * 2;
                double reach = PATROL_RADIUS * (0.4 + random.nextDouble() * 0.6);
                int x = Mth.floor(home.x + Math.cos(angle) * reach);
                int z = Mth.floor(home.z + Math.sin(angle) * reach);
                int ground = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                patrolGoal = new Vec3(x + 0.5, Math.max(ground, world.getMinY()) + CRUISE, z + 0.5);
            }
            goal = patrolGoal;
        }
        Vec3 velocity = RecognizerFlight.steer(getDeltaMovement(), goal.subtract(position()));
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        if (velocity.horizontalDistanceSqr() > 1.0e-4) {
            setYRot((float) (Mth.atan2(-velocity.x, velocity.z) * Mth.RAD_TO_DEG));
        }
    }

    // A player in the searchlight, below it, in sight, whom the regime doesn't tolerate
    @Nullable
    private Player caught(ServerLevel world) {
        for (Player player : world.players()) {
            if (player.isSpectator() || player.isCreative() || player.getVehicle() == this) {
                continue;
            }
            double drop = getY() - player.getY();
            double across = Math.sqrt(Mth.square(player.getX() - getX()) + Mth.square(player.getZ() - getZ()));
            if (drop < 0 || drop > BEAM_LENGTH || across > BEAM_RADIUS * drop / BEAM_LENGTH + 1) {
                continue;
            }
            boolean hostile = Regime.alignment(player) < Regime.rules().alignment().toleratedFrom() || Overclock.marked(player);
            if (hostile && world.clip(new ClipContext(position(), player.getEyePosition(), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                return player;
            }
        }
        return null;
    }

    // Two enforcers, out of its belly, drifting down on Chutes onto the one it caught
    private void dropEnforcers(ServerLevel world, Player caught) {
        dropped.removeIf(id -> !(world.getEntity(id) instanceof EnforcerEntity enforcer) || !enforcer.isAlive());
        UUID squad = UUID.randomUUID();
        for (int i = 0; i < 2 && dropped.size() < MAX_DROPPED; i++) {
            EnforcerEntity enforcer = ModEntities.ENFORCER.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (enforcer == null) {
                return;
            }
            enforcer.snapTo(getX() + (i == 0 ? -1.2 : 1.2), getY() - 1, getZ(), getYRot(), 0);
            enforcer.joinSquad(squad, SquadRoles.roll(ControlTier.CONTROLLED, random.nextDouble()), i == 0);
            enforcer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CHUTE));
            enforcer.setDropChance(EquipmentSlot.CHEST, 0);
            enforcer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400));
            enforcer.setTarget(caught);
            world.addFreshEntity(enforcer);
            dropped.add(enforcer.getUUID());
        }
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    // Without a pilot it sinks gently to the ground and waits
    private void settle() {
        Vec3 velocity = onGround() ? RecognizerFlight.hover(getDeltaMovement()) : new Vec3(getDeltaMovement().x * 0.9,
                Math.max(-0.12, getDeltaMovement().y - 0.01), getDeltaMovement().z * 0.9);
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
    }

    // Under a player's hands: turn with left and right, thrust forward and back, climb and dive
    private void fly() {
        if (left) {
            setYRot(getYRot() - 3);
        }
        if (right) {
            setYRot(getYRot() + 3);
        }
        Vec3 heading = Vec3.directionFromRotation(0, getYRot());
        double thrust = (forward ? 1 : 0) - (back ? 1 : 0);
        double climb = (up ? 1 : 0) - (down ? 1 : 0);
        Vec3 velocity = RecognizerFlight.pilot(getDeltaMovement(), heading, thrust, climb);
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity pilot ? pilot : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 3;
    }

    // The pilot sits on top; anyone else hangs beneath the arch
    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        int seat = getPassengers().indexOf(passenger);
        Vec3 offset = seat <= 0 ? new Vec3(0, 3.0, 0) : new Vec3(seat == 1 ? -0.8 : 0.8, 0.4, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
        move.accept(passenger, getX() + offset.x, getY() + offset.y, getZ() + offset.z);
    }

    // Anyone can take an empty cockpit
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive() || getControllingPassenger() != null) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (isInvulnerableToBase(source) || source.getEntity() != null && hasPassenger(source.getEntity())) {
            return false;
        }
        entityData.set(HEALTH, health() - amount);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 2, getZ(), 6, 1, 1, 1, 0.1);
        if (health() <= 0) {
            breakApart(world);
        }
        return true;
    }

    // Comes apart into salvage
    private void breakApart(ServerLevel world) {
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5, getZ(), 1, 0, 0, 0, 0);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.8f);
        ejectPassengers();
        for (int i = 0; i < 3 + random.nextInt(4); i++) {
            spawnAtLocation(world, random.nextBoolean() ? ModItems.BINARY_0 : ModItems.BINARY_1);
        }
        spawnAtLocation(world, new ItemStack(ModItems.LUMINESSENCE, 4 + random.nextInt(5)));
        discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        view.putFloat("Health", health());
        view.putBoolean("Crewed", crewed);
        if (home != null) {
            view.store("Home", Vec3.CODEC, home);
        }
        view.store("Dropped", UUIDUtil.CODEC.listOf(), dropped);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        entityData.set(HEALTH, view.getFloatOr("Health", MAX_HEALTH));
        crewed = view.getBooleanOr("Crewed", false);
        home = view.read("Home", Vec3.CODEC).orElse(null);
        dropped.clear();
        view.read("Dropped", UUIDUtil.CODEC.listOf()).ifPresent(dropped::addAll);
    }

    /** Where a spotted, hovering Recognizer should be relative to the ground below a block, for spawning. */
    public static BlockPos cruiseAbove(BlockPos ground) {
        return ground.above(CRUISE);
    }
}
