//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.partatoes.digitalfrontier.entity.vehicle;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.block.custom.MovingLightBlock;
import com.partatoes.digitalfrontier.block.custom.TrailWallBlock;
import com.partatoes.digitalfrontier.entity.custom.ChampionEntity;
import com.partatoes.digitalfrontier.grid.arena.CycleMatch;
import com.partatoes.digitalfrontier.grid.arena.CycleSteering;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class LightCycleEntity extends VehicleEntity implements MovingLightBlock.Emitter {
    // In an arena match (see CycleMatch): 0 for an ordinary cycle, then lined up waiting, then racing
    private static final EntityDataAccessor<Byte> ARENA = SynchedEntityData.defineId(LightCycleEntity.class, EntityDataSerializers.BYTE);
    public static final byte ARENA_WAITING = 1;
    public static final byte ARENA_RACING = 2;
    /** An arena cycle's speed, in blocks a tick. */
    public static final double ARENA_SPEED = 0.4;
    private static final int TURN_COOLDOWN = 4;
    // How brightly a light cycle lights the road around it
    private static final int LIGHT = 12;
    private boolean wasLeft;
    private boolean wasRight;
    private int turnCooldown;
    @Nullable
    private BlockPos lastCell;
    // Used for controlling the vehicle
    private boolean pressingLeft;
    private boolean pressingRight;
    private boolean pressingForward;
    private boolean pressingBack;

    // Velocity and positions
    private float yawVelocity;

    public LightCycleEntity(EntityType<? extends  LightCycleEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.LIGHTCYCLE_BATON;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARENA, (byte) 0);
    }

    /** Its arena state: 0 for an ordinary cycle, {@link #ARENA_WAITING} or {@link #ARENA_RACING}. */
    public byte arena() {
        return entityData.get(ARENA);
    }

    public void setArena(byte state) {
        entityData.set(ARENA, state);
    }

    // An arena cycle can't be broken up (and drop a baton)
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        return arena() == 0 && super.hurtServer(world, source, amount);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {

    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        this.clampPassengerYaw(passenger);
    }

    protected void clampPassengerYaw(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float g = Mth.clamp(f, -105.0F, 105.0F);
        passenger.yRotO += g - f;
        passenger.setYRot(passenger.getYRot() + g - f);
        passenger.setYHeadRot(passenger.getYRot());
    }

    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult actionResult = super.interact(player, hand, location);
        if (actionResult != InteractionResult.PASS) {
            return actionResult;
        } else {
            return (player.isSecondaryUseActive() || !this.level().isClientSide() && !player.startRiding(this) ? InteractionResult.PASS : InteractionResult.SUCCESS);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.getMaxPassengers() && !this.isEyeInFluid(FluidTags.WATER);
    }
    protected int getMaxPassengers() {
        return 1;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        LivingEntity le;
        if (passenger instanceof LivingEntity livingEntity) {
            le = livingEntity;
        } else {
            le = super.getControllingPassenger();
        }

        return le;
    }

    public void setInputs(boolean pressingLeft, boolean pressingRight, boolean pressingForward, boolean pressingBack) {
        this.pressingLeft = pressingLeft;
        this.pressingRight = pressingRight;
        this.pressingForward = pressingForward;
        this.pressingBack = pressingBack;
    }

    @Override
    protected double getDefaultGravity() {
        return .04;
    }

    // Enough to ride up slab ramps, like the grid's skyroad ramps, without hopping full blocks
    @Override
    public float maxUpStep() {
        return 0.6f;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return canCollide(this, other);
    }

    public static boolean canCollide(Entity entity, Entity other) {
        return (other.canBeCollidedWith(entity) || other.isPushable()) && !entity.isPassengerOfSameVehicle(other);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void push(Entity entity) {
        if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.push(entity);
        }
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
        if (!this.isPassenger()) {
            if (onGround) {
                this.resetFallDistance();
            } else if (!this.level().getFluidState(this.blockPosition().below()).is(FluidTags.WATER) && heightDifference < 0.0) {
                this.fallDistance -= (float)heightDifference;
            }

        }
    }

    // Smooths the position updates from the server, as for boats
    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    @Override
    public void tick() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        super.tick();
        if (this.isLocalInstanceAuthoritative()) {
            if (arena() != 0) {
                // An arena cycle runs flat out on a grid, turning a quarter at a time
                if (!level().isClientSide() && getControllingPassenger() instanceof ChampionEntity && arena() == ARENA_RACING) {
                    CycleMatch.steer(this);
                }
                arenaDrive();
            } else {
                this.updateVelocity();

                if (this.level().isClientSide()) {
                    this.updateStateFromInput();
                }
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        if (arena() == ARENA_RACING && level() instanceof ServerLevel world) {
            layTrail(world);
        }

        this.applyEffectsFromBlocks();
        this.applyEffectsFromBlocks();

        if (tickCount % 2 == 0) {
            MovingLightBlock.shine(level(), blockPosition(), LIGHT);
        }

        // Here is where you could add code for causing Programs to enter the vehicle
    }

    @Override
    public int emittedLight() {
        return LIGHT;
    }

    // Turns a quarter left or right as a turn key goes down, and otherwise runs straight on at speed
    private void arenaDrive() {
        if (turnCooldown > 0) {
            turnCooldown--;
        }
        if (arena() == ARENA_RACING && turnCooldown == 0) {
            if (pressingLeft && !wasLeft) {
                turn(CycleSteering.Turn.LEFT);
            } else if (pressingRight && !wasRight) {
                turn(CycleSteering.Turn.RIGHT);
            }
        }
        wasLeft = pressingLeft;
        wasRight = pressingRight;
        int heading = CycleSteering.headingOf(getYRot());
        setYRot(CycleSteering.yawOf(heading));
        double speed = arena() == ARENA_RACING ? ARENA_SPEED : 0;
        setDeltaMovement(CycleSteering.dx(heading) * speed, -getGravity(), CycleSteering.dz(heading) * speed);
    }

    /** Turns a quarter, lining up on the middle of the block so trails stay straight. */
    public void turn(CycleSteering.Turn turn) {
        int heading = turn.apply(CycleSteering.headingOf(getYRot()));
        setYRot(CycleSteering.yawOf(heading));
        BlockPos cell = blockPosition();
        setPos(cell.getX() + 0.5, getY(), cell.getZ() + 0.5);
        turnCooldown = TURN_COOLDOWN;
    }

    // Leaves a wall in each block it leaves behind, and crashes on hitting one (or the arena's edge)
    private void layTrail(ServerLevel world) {
        BlockPos cell = blockPosition();
        if (lastCell != null && !lastCell.equals(cell)) {
            boolean program = getControllingPassenger() instanceof ChampionEntity;
            BlockState wall = ModBlocks.TRAIL_WALL.defaultBlockState().setValue(TrailWallBlock.PROGRAM, program);
            for (int dy = 0; dy < 2; dy++) {
                BlockState there = world.getBlockState(lastCell.above(dy));
                if (there.isAir() || there.is(ModBlocks.MOVING_LIGHT)) {
                    world.setBlockAndUpdate(lastCell.above(dy), wall);
                }
            }
        }
        lastCell = cell;
        int heading = CycleSteering.headingOf(getYRot());
        BlockPos ahead = BlockPos.containing(getX() + CycleSteering.dx(heading) * 0.7, getY() + 0.2, getZ() + CycleSteering.dz(heading) * 0.7);
        BlockState next = world.getBlockState(ahead);
        if (next.is(ModBlocks.TRAIL_WALL) || next.isSolid() || CycleMatch.offFloor(world, this)) {
            CycleMatch.crashed(world, this);
        }
    }

    private void updateVelocity() {
        double d = -this.getGravity();
        float velocityDecay = 0.9F;  // Typical values for boats are .9 for Water/Air, with different values on ice etc

        Vec3 vec3d = this.getDeltaMovement();
        this.setDeltaMovement(vec3d.x * (double)velocityDecay, vec3d.y + d, vec3d.z * (double)velocityDecay);
        this.yawVelocity = this.yawVelocity * velocityDecay;
    }

    private void updateStateFromInput() {
        if (this.isVehicle()) {
            float f = 0.0F;
            if (this.pressingLeft) {
                this.yawVelocity--;
            }

            if (this.pressingRight) {
                this.yawVelocity++;
            }

            if (this.pressingRight != this.pressingLeft && !this.pressingForward && !this.pressingBack) {
                f += 0.005F;
            }

            this.setYRot(this.getYRot() + this.yawVelocity);
            if (this.pressingForward) {
                f += 0.04F;
            }

            if (this.pressingBack) {
                f -= 0.005F;
            }

            this.setDeltaMovement(
                    this.getDeltaMovement()
                            .add(
                                    (Mth.sin(-this.getYRot() * (float) (Math.PI / 180.0)) * f),
                                    0.0,
                                    (Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * f)
                            )
            );
        }
    }
}
