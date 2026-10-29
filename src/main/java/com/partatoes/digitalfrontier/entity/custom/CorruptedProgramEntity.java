package com.partatoes.digitalfrontier.entity.custom;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A program the corruption has gotten into. Not hostile, but wrong: each has one or two quirks (see {@link Quirk}),
 * and all of them stutter, snap their heads round and repeat their own sounds. Where corruption is strong they
 * spawn "provokable" and fight back when hit; elsewhere they glitch away instead.
 */
public class CorruptedProgramEntity extends PathfinderMob {
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
    private Player watcher;
    // A nearby player's recent positions, oldest first, for the echo quirk
    private final Vec3[] echoTrail = new Vec3[ECHO_DELAY + 1];
    private int echoTrailSize;

    public CorruptedProgramEntity(EntityType<? extends PathfinderMob> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createCorruptedProgramAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FreezeWhenWatchedGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(3, new EchoGoal());
        this.goalSelector.addGoal(4, new StalkGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData) {
        Quirk[] all = Quirk.values();
        quirks = 1 << random.nextInt(all.length);
        if (random.nextBoolean()) {
            quirks |= 1 << random.nextInt(all.length);
        }
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData);
    }

    public void setProvokable(boolean provokable) {
        this.provokable = provokable;
    }

    public boolean hasQuirk(Quirk quirk) {
        return (quirks & 1 << quirk.ordinal()) != 0;
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        boolean damaged = super.hurtServer(world, source, amount);
        if (damaged && source.getEntity() instanceof Player player && !player.isCreative()) {
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
        if (level().isClientSide()) {
            return;
        }
        if (angerTicks > 0 && --angerTicks == 0) {
            setTarget(null);
        }
        if (repeatSoundIn > 0 && --repeatSoundIn == 0) {
            playAmbientSound();
            repeatSoundIn = -1;
        }
        Player nearest = level().getNearestPlayer(this, 16);
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
                lookAt(nearest, 360, 360);
                setYHeadRot(getYRot());
            }
            if (hasQuirk(Quirk.GLITCH_TELEPORT) && random.nextFloat() < 0.005f) {
                glitchAway(4);
            }
        }
        if (hasQuirk(Quirk.BACKWARDS)) {
            Vec3 velocity = getDeltaMovement();
            if (velocity.horizontalDistanceSqr() > 1.0E-4) {
                float facing = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) + 90f;
                setYRot(facing);
                setYBodyRot(facing);
                setYHeadRot(facing);
            }
        }
    }

    // A short, silent-film skip: gone from one spot, standing on another
    private void glitchAway(int range) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = getX() + (random.nextDouble() - 0.5) * 2 * range;
            double z = getZ() + (random.nextDouble() - 0.5) * 2 * range;
            BlockPos target = BlockPos.containing(x, getY(), z);
            if (randomTeleport(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, true, state -> false)) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.4f, 2.0f);
                return;
            }
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
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
    public float getVoicePitch() {
        return 0.4f + random.nextFloat() * 1.4f;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putInt("Quirks", quirks);
        view.putBoolean("Provokable", provokable);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        quirks = view.getIntOr("Quirks", 0);
        provokable = view.getBooleanOr("Provokable", false);
    }

    @Nullable
    private Player findWatcher() {
        for (Player player : level().players()) {
            if (player.isSpectator() || player.distanceToSqr(this) > 24 * 24) {
                continue;
            }
            Vec3 look = player.getViewVector(1f).normalize();
            Vec3 toThis = getEyePosition().subtract(player.getEyePosition()).normalize();
            if (look.dot(toThis) > 0.96 && player.hasLineOfSight(this)) {
                return player;
            }
        }
        return null;
    }

    private class FreezeWhenWatchedGoal extends Goal {
        FreezeWhenWatchedGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!hasQuirk(Quirk.FREEZE_WHEN_WATCHED) || getTarget() != null) {
                return false;
            }
            watcher = findWatcher();
            return watcher != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            getNavigation().stop();
            // Perfectly still, staring back
            if (watcher != null) {
                getLookControl().setLookAt(watcher, 360, 360);
            }
        }
    }

    private class EchoGoal extends Goal {
        @Nullable
        private Player subject;

        EchoGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!hasQuirk(Quirk.ECHO) || getTarget() != null) {
                return false;
            }
            subject = level().getNearestPlayer(CorruptedProgramEntity.this, 10);
            return subject != null && !subject.isSpectator();
        }

        @Override
        public boolean canContinueToUse() {
            return subject != null && subject.isAlive() && subject.distanceToSqr(CorruptedProgramEntity.this) < 14 * 14;
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
                echoTrail[echoTrailSize++] = subject.position();
                return;
            }
            System.arraycopy(echoTrail, 1, echoTrail, 0, echoTrail.length - 1);
            echoTrail[echoTrail.length - 1] = subject.position();
            // Move the way the player moved ECHO_DELAY ticks ago
            Vec3 step = echoTrail[1].subtract(echoTrail[0]);
            if (step.horizontalDistanceSqr() > 4.0E-4) {
                getMoveControl().setWantedPosition(getX() + step.x * 4, getY(), getZ() + step.z * 4, 1.0);
            }
            if (step.y > 0.2) {
                getJumpControl().jump();
            }
        }
    }

    // Drifts toward the nearest player and stops a few blocks short
    private class StalkGoal extends Goal {
        @Nullable
        private Player subject;

        StalkGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || random.nextInt(60) != 0) {
                return false;
            }
            subject = level().getNearestPlayer(CorruptedProgramEntity.this, 20);
            return subject != null && !subject.isSpectator() && subject.distanceToSqr(CorruptedProgramEntity.this) > 16;
        }

        @Override
        public boolean canContinueToUse() {
            return subject != null && subject.isAlive() && !getNavigation().isDone()
                    && subject.distanceToSqr(CorruptedProgramEntity.this) > 16;
        }

        @Override
        public void start() {
            if (subject != null) {
                getNavigation().moveTo(subject, 0.7);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
