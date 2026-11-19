package com.partatoes.digitalfrontier.entity.custom;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A System Core bolt: a homing shulker bullet that does one of several things to what it hits (see
 * {@link CoreBoltEffect}), trailing particles that show which. It is a plain shulker bullet to the client; one saved
 * and reloaded mid-flight comes back as an ordinary levitating one.
 */
public class CoreBolt extends ShulkerBullet {
    private static final float DAMAGE = 4;
    private static final int FREEZE_TICKS = 20;
    private static final int LEVITATE_TICKS = 60;
    private static final int GLITCH_TICKS = 60;
    private static final double THROW = 1.6;

    private final CoreBoltEffect effect;

    public CoreBolt(ServerLevel world, LivingEntity core, Entity target, CoreBoltEffect effect) {
        super(world, core, target, Direction.Axis.Y);
        this.effect = effect;
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive() && level() instanceof ServerLevel world && tickCount % 2 == 0) {
            world.sendParticles(trail(), getX(), getY(), getZ(), 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private ParticleOptions trail() {
        return switch (effect) {
            case FREEZE -> ParticleTypes.SNOWFLAKE;
            case KNOCKBACK -> ParticleTypes.GUST;
            case LEVITATE -> ParticleTypes.END_ROD;
            case GLITCH -> ParticleTypes.SQUID_INK;
        };
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        Entity owner = getOwner();
        LivingEntity attacker = owner instanceof LivingEntity living ? living : null;
        DamageSource source = damageSources().mobProjectile(this, attacker);
        if (!(level() instanceof ServerLevel world) || !target.hurtServer(world, source, DAMAGE) || !(target instanceof LivingEntity living)) {
            return;
        }
        Entity cause = owner != null ? owner : this;
        switch (effect) {
            case FREEZE -> {
                // Slowed to a standstill, with the powder-snow frost creeping off over a few seconds
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FREEZE_TICKS, 9), cause);
                living.setTicksFrozen(living.getTicksRequiredToFreeze() + FREEZE_TICKS);
                living.setDeltaMovement(Vec3.ZERO);
                living.needsSync = true;
                world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.0f, 0.8f);
            }
            case KNOCKBACK -> {
                Vec3 away = living.position().subtract(cause.position()).horizontal();
                away = away.lengthSqr() < 1.0e-4 ? getDeltaMovement().horizontal() : away;
                double resist = 1 - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                Vec3 push = away.normalize().scale(THROW * resist).add(0, 0.45 * resist, 0);
                living.setDeltaMovement(living.getDeltaMovement().add(push));
                living.needsSync = true;
                world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.0f, 0.8f);
            }
            case LEVITATE -> living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATE_TICKS), cause);
            case GLITCH -> {
                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, GLITCH_TICKS), cause);
                world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0f, 0.6f);
            }
        }
    }
}
