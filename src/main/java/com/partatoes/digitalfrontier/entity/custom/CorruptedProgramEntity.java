package com.partatoes.digitalfrontier.entity.custom;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A program the corruption has gotten into. Not hostile, but wrong: each has one or two quirks (see {@link Quirk}),
 * and all of them stutter, snap their heads round and repeat their own sounds. Where corruption is strong they
 * spawn "provokable" and fight back when hit; elsewhere they glitch away instead.
 */
public class CorruptedProgramEntity extends PathAwareEntity {
    private static final int ECHO_DELAY = 30;
    private static final int ANGER_TICKS = 600;

    public enum Quirk {
        /** Freezes while a player looks at it, and moves only when nobody does. */
        FREEZE_WHEN_WATCHED,
        /** Copies a nearby player's movements a moment late. */
        ECHO,
        /** Walks backwards. */
        BACKWARDS,
        /** Skips short distances when players are near. */
        GLITCH_TELEPORT
    }

    private int quirks;
    private boolean provokable;
    private int angerTicks;
    private int stutterTicks;
    private int repeatSoundIn = -1;
    @Nullable
    private PlayerEntity watcher;
    // A nearby player's recent positions, oldest first, for the echo quirk
    private final Vec3d[] echoTrail = new Vec3d[ECHO_DELAY + 1];
    private int echoTrailSize;

    public CorruptedProgramEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createCorruptedProgramAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.ATTACK_DAMAGE, 4)
                .add(EntityAttributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FreezeWhenWatchedGoal());
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.add(3, new EchoGoal());
        this.goalSelector.add(4, new StalkGoal());
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16));
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        Quirk[] all = Quirk.values();
        quirks = 1 << random.nextInt(all.length);
        if (random.nextBoolean()) {
            quirks |= 1 << random.nextInt(all.length);
        }
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    public void setProvokable(boolean provokable) {
        this.provokable = provokable;
    }

    public boolean hasQuirk(Quirk quirk) {
        return (quirks & 1 << quirk.ordinal()) != 0;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean damaged = super.damage(world, source, amount);
        if (damaged && source.getAttacker() instanceof PlayerEntity player && !player.isCreative()) {
            if (provokable) {
                setTarget(player);
                angerTicks = ANGER_TICKS;
            } else {
                glitchAway(6);
            }
        }
        return damaged;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            return;
        }
        if (angerTicks > 0 && --angerTicks == 0) {
            setTarget(null);
        }
        if (repeatSoundIn > 0 && --repeatSoundIn == 0) {
            playAmbientSound();
            repeatSoundIn = -1;
        }
        PlayerEntity nearest = getWorld().getClosestPlayer(this, 16);
        if (nearest != null) {
            // Stutter: stop dead for half a second
            if (stutterTicks > 0) {
                stutterTicks--;
                getNavigation().stop();
            } else if (random.nextFloat() < 0.01f) {
                stutterTicks = 10;
            }
            // Head snap: no turning, the head is just suddenly facing you
            if (random.nextFloat() < 0.02f) {
                lookAtEntity(nearest, 360, 360);
                setHeadYaw(getYaw());
            }
            if (hasQuirk(Quirk.GLITCH_TELEPORT) && random.nextFloat() < 0.005f) {
                glitchAway(4);
            }
        }
        if (hasQuirk(Quirk.BACKWARDS)) {
            Vec3d velocity = getVelocity();
            if (velocity.horizontalLengthSquared() > 1.0E-4) {
                float facing = (float) (MathHelper.atan2(velocity.z, velocity.x) * MathHelper.DEGREES_PER_RADIAN) + 90f;
                setYaw(facing);
                setBodyYaw(facing);
                setHeadYaw(facing);
            }
        }
    }

    // A short, silent-film skip: gone from one spot, standing on another
    private void glitchAway(int range) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = getX() + (random.nextDouble() - 0.5) * 2 * range;
            double z = getZ() + (random.nextDouble() - 0.5) * 2 * range;
            BlockPos target = BlockPos.ofFloored(x, getY(), z);
            if (teleport(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, true)) {
                getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.4f, 2.0f);
                return;
            }
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        // Sometimes a sound repeats a moment later, as if the program glitched mid-word
        if (repeatSoundIn < 0 && random.nextFloat() < 0.3f) {
            repeatSoundIn = 4 + random.nextInt(4);
        }
    }

    @Override
    public float getSoundPitch() {
        return 0.4f + random.nextFloat() * 1.4f;
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putInt("Quirks", quirks);
        view.putBoolean("Provokable", provokable);
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        quirks = view.getInt("Quirks", 0);
        provokable = view.getBoolean("Provokable", false);
    }

    @Nullable
    private PlayerEntity findWatcher() {
        for (PlayerEntity player : getWorld().getPlayers()) {
            if (player.isSpectator() || player.squaredDistanceTo(this) > 24 * 24) {
                continue;
            }
            Vec3d look = player.getRotationVec(1f).normalize();
            Vec3d toThis = getEyePos().subtract(player.getEyePos()).normalize();
            if (look.dotProduct(toThis) > 0.96 && player.canSee(this)) {
                return player;
            }
        }
        return null;
    }

    private class FreezeWhenWatchedGoal extends Goal {
        FreezeWhenWatchedGoal() {
            setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
        }

        @Override
        public boolean canStart() {
            if (!hasQuirk(Quirk.FREEZE_WHEN_WATCHED) || getTarget() != null) {
                return false;
            }
            watcher = findWatcher();
            return watcher != null;
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
        }

        @Override
        public void tick() {
            getNavigation().stop();
            // Perfectly still, staring back
            if (watcher != null) {
                getLookControl().lookAt(watcher, 360, 360);
            }
        }
    }

    private class EchoGoal extends Goal {
        @Nullable
        private PlayerEntity subject;

        EchoGoal() {
            setControls(EnumSet.of(Control.MOVE, Control.JUMP));
        }

        @Override
        public boolean canStart() {
            if (!hasQuirk(Quirk.ECHO) || getTarget() != null) {
                return false;
            }
            subject = getWorld().getClosestPlayer(CorruptedProgramEntity.this, 10);
            return subject != null && !subject.isSpectator();
        }

        @Override
        public boolean shouldContinue() {
            return subject != null && subject.isAlive() && subject.squaredDistanceTo(CorruptedProgramEntity.this) < 14 * 14;
        }

        @Override
        public void start() {
            echoTrailSize = 0;
        }

        @Override
        public void tick() {
            if (subject == null) {
                return;
            }
            if (echoTrailSize < echoTrail.length) {
                echoTrail[echoTrailSize++] = subject.getPos();
                return;
            }
            System.arraycopy(echoTrail, 1, echoTrail, 0, echoTrail.length - 1);
            echoTrail[echoTrail.length - 1] = subject.getPos();
            // Move the way the player moved ECHO_DELAY ticks ago
            Vec3d step = echoTrail[1].subtract(echoTrail[0]);
            if (step.horizontalLengthSquared() > 4.0E-4) {
                getMoveControl().moveTo(getX() + step.x * 4, getY(), getZ() + step.z * 4, 1.0);
            }
            if (step.y > 0.2) {
                getJumpControl().setActive();
            }
        }
    }

    // Drifts toward the nearest player and stops a few blocks short
    private class StalkGoal extends Goal {
        @Nullable
        private PlayerEntity subject;

        StalkGoal() {
            setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (getTarget() != null || random.nextInt(60) != 0) {
                return false;
            }
            subject = getWorld().getClosestPlayer(CorruptedProgramEntity.this, 20);
            return subject != null && !subject.isSpectator() && subject.squaredDistanceTo(CorruptedProgramEntity.this) > 16;
        }

        @Override
        public boolean shouldContinue() {
            return subject != null && subject.isAlive() && !getNavigation().isIdle()
                    && subject.squaredDistanceTo(CorruptedProgramEntity.this) > 16;
        }

        @Override
        public void start() {
            if (subject != null) {
                getNavigation().startMovingTo(subject, 0.7);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
