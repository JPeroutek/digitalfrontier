package com.partatoes.digitalfrontier.entity.projectile;

import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The ring game's ball: it flies light and bounces hard off floors and walls, losing a little each time. Caught with
 * a Cesta (or by a champion), it rides just in front of its holder until thrown, and is thrown for them if they hold
 * it too long (see {@link #MAX_HOLD}).
 */
public class RingBall extends Entity implements ItemSupplier {
    /** How long it can be held before it's thrown anyway, in ticks. */
    public static final int MAX_HOLD = 60;
    /** How hard it's thrown. */
    public static final double THROW_SPEED = 1.1;
    private static final double GRAVITY = 0.03;
    private static final double BOUNCE = 0.85;

    @Nullable
    private UUID holder;
    private int held;

    public RingBall(EntityType<? extends RingBall> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.RING_BALL);
    }

    @Nullable
    public LivingEntity holder() {
        return holder != null && level() instanceof ServerLevel world && world.getEntity(holder) instanceof LivingEntity living
                && living.isAlive() ? living : null;
    }

    public boolean heldBy(Entity entity) {
        return entity.getUUID().equals(holder);
    }

    /** Whether it's loose: flying or rolling, nobody holding it. */
    public boolean free() {
        return holder() == null;
    }

    /** Caught: it rides in front of the catcher until thrown. */
    public void catchBy(LivingEntity catcher) {
        holder = catcher.getUUID();
        held = 0;
        setDeltaMovement(Vec3.ZERO);
        playSound(SoundEvents.ITEM_PICKUP, 0.8f, 1.4f);
    }

    /** Thrown from its holder (or anyone) along a direction. */
    public void throwAlong(Vec3 direction) {
        holder = null;
        held = 0;
        setDeltaMovement(direction.normalize().scale(THROW_SPEED));
        playSound(SoundEvents.TRIDENT_THROW.value(), 1.0f, 1.4f);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity holding = holder();
        if (holding != null) {
            Vec3 front = holding.getEyePosition().add(holding.getLookAngle().scale(1.2)).subtract(0, 0.4, 0);
            setPos(front);
            setDeltaMovement(Vec3.ZERO);
            if (++held > MAX_HOLD) {
                throwAlong(holding.getLookAngle());
            }
            return;
        }
        holder = null;
        Vec3 velocity = getDeltaMovement().add(0, -GRAVITY, 0).scale(0.995);
        move(MoverType.SELF, velocity);
        Vec3 moved = getDeltaMovement();
        // Bounce off whatever stopped it, a little softer each time
        double vx = Math.abs(moved.x) < Math.abs(velocity.x) * 0.5 ? -velocity.x * BOUNCE : velocity.x;
        double vz = Math.abs(moved.z) < Math.abs(velocity.z) * 0.5 ? -velocity.z * BOUNCE : velocity.z;
        double vy = velocity.y;
        if (verticalCollision) {
            vy = Math.abs(velocity.y) > 0.08 ? -velocity.y * BOUNCE : 0;
            if (vy == 0) {
                vx *= 0.9;
                vz *= 0.9;
            }
        }
        if ((vx != velocity.x || vz != velocity.z || verticalCollision && Math.abs(velocity.y) > 0.08) && level() instanceof ServerLevel world) {
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 0.6f, 1.8f);
        }
        setDeltaMovement(vx, vy, vz);
        if (level() instanceof ServerLevel world && tickCount % 2 == 0) {
            world.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        if (holder != null) {
            view.store("Holder", UUIDUtil.CODEC, holder);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        holder = view.read("Holder", UUIDUtil.CODEC).orElse(null);
    }
}
