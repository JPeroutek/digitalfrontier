//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.partatoes.digitalfrontier.entity.vehicle;

import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class LightCycleEntity extends VehicleEntity {
    // Used for controlling the vehicle
    private boolean pressingLeft;
    private boolean pressingRight;
    private boolean pressingForward;
    private boolean pressingBack;

    // Velocity and positions
    private float yawVelocity;
    private double x;
    private double y;
    private double z;
    private double pitch;
    private double roll;
    private double yaw;
    private int lerpTicks;

    public LightCycleEntity(EntityType<? extends  LightCycleEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected Item asItem() {
        return ModItems.LIGHTCYCLE_BATON;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {

    }

    @Override
    public void onPassengerLookAround(Entity passenger) {
        this.clampPassengerYaw(passenger);
    }

    protected void clampPassengerYaw(Entity passenger) {
        passenger.setBodyYaw(this.getYaw());
        float f = MathHelper.wrapDegrees(passenger.getYaw() - this.getYaw());
        float g = MathHelper.clamp(f, -105.0F, 105.0F);
        passenger.prevYaw += g - f;
        passenger.setYaw(passenger.getYaw() + g - f);
        passenger.setHeadYaw(passenger.getYaw());
    }

    public ActionResult interact(PlayerEntity player, Hand hand) {
        ActionResult actionResult = super.interact(player, hand);
        if (actionResult != ActionResult.PASS) {
            return actionResult;
        } else {
            return (player.shouldCancelInteraction() || !this.getWorld().isClient && !player.startRiding(this) ? ActionResult.PASS : ActionResult.SUCCESS);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().size() < this.getMaxPassengers() && !this.isSubmergedIn(FluidTags.WATER);
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
    protected double getGravity() {
        return .04;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return canCollide(this, other);
    }

    public static boolean canCollide(Entity entity, Entity other) {
        return (other.isCollidable() || other.isPushable()) && !entity.isConnectedThroughVehicle(other);
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.pushAwayFrom(entity);
        }
    }

    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
        if (!this.hasVehicle()) {
            if (onGround) {
                this.onLanding();
            } else if (!this.getWorld().getFluidState(this.getBlockPos().down()).isIn(FluidTags.WATER) && heightDifference < 0.0) {
                this.fallDistance -= (float)heightDifference;
            }

        }
    }

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lerpTicks = interpolationSteps;
    }

    private void updatePositionAndRotation() {
        if (this.lerpTicks > 0) {
            this.lerpPosAndRotation(this.lerpTicks, this.x, this.y, this.z, this.yaw, this.pitch);
            --this.lerpTicks;
        }
    }

    @Override
    public void tick() {
        if (this.getDamageWobbleTicks() > 0) {
            this.setDamageWobbleTicks(this.getDamageWobbleTicks() - 1);
        }

        if (this.getDamageWobbleStrength() > 0.0F) {
            this.setDamageWobbleStrength(this.getDamageWobbleStrength() - 1.0F);
        }

        super.tick();
        this.updatePositionAndRotation();
        if (this.isLogicalSideForUpdatingMovement()) {
            this.updateVelocity();

            if (this.getWorld().isClient) {
                this.updateStateFromInput();
            }

            this.move(MovementType.SELF, this.getVelocity());
        } else {
            this.setVelocity(Vec3d.ZERO);
        }

        this.tickBlockCollision();
        this.tickBlockCollision();

        // Here is where you could add code for causing Programs to enter the vehicle
    }

    private void updateVelocity() {
        double d = -this.getFinalGravity();
        float velocityDecay = 0.9F;  // Typical values for boats are .9 for Water/Air, with different values on ice etc

        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.x * (double)velocityDecay, vec3d.y + d, vec3d.z * (double)velocityDecay);
        this.yawVelocity = this.yawVelocity * velocityDecay;
    }

    private void updateStateFromInput() {
        if (this.hasPassengers()) {
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

            this.setYaw(this.getYaw() + this.yawVelocity);
            if (this.pressingForward) {
                f += 0.04F;
            }

            if (this.pressingBack) {
                f -= 0.005F;
            }

            this.setVelocity(
                    this.getVelocity()
                            .add(
                                    (MathHelper.sin(-this.getYaw() * (float) (Math.PI / 180.0)) * f),
                                    0.0,
                                    (MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0)) * f)
                            )
            );
        }
    }
}
