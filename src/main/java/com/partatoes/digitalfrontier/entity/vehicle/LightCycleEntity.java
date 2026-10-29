//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.partatoes.digitalfrontier.entity.vehicle;

import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
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

public class LightCycleEntity extends VehicleEntity {
    // Used for controlling the vehicle
    private boolean pressingLeft;
    private boolean pressingRight;
    private boolean pressingForward;
    private boolean pressingBack;

    // Velocity and positions
    private float yawVelocity;
    // Smooths the position updates from the server, as for boats
    private final InterpolationHandler interpolator = new InterpolationHandler(this, 3);

    public LightCycleEntity(EntityType<? extends  LightCycleEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.LIGHTCYCLE_BATON;
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

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolator;
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
        this.interpolator.interpolate();
        if (this.isLocalInstanceAuthoritative()) {
            this.updateVelocity();

            if (this.level().isClientSide()) {
                this.updateStateFromInput();
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        this.applyEffectsFromBlocks();
        this.applyEffectsFromBlocks();

        // Here is where you could add code for causing Programs to enter the vehicle
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
