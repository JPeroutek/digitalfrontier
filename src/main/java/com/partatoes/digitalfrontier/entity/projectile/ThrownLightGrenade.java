package com.partatoes.digitalfrontier.entity.projectile;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown Light Grenade: lobbed, it bursts where it lands (or after two seconds in flight), hurting and throwing back
 * everything near, and leaves a ring of light walls that fade after a few seconds (see
 * {@link com.partatoes.digitalfrontier.block.custom.FadingBarrierBlock}).
 */
public class ThrownLightGrenade extends ThrowableItemProjectile {
    private static final int FUSE = 40;
    private static final double RADIUS = 3;
    private static final float DAMAGE = 6;
    // The ring of light walls left by the burst
    private static final int RING = 2;

    public ThrownLightGrenade(EntityType<? extends ThrownLightGrenade> entityType, Level world) {
        super(entityType, world);
    }

    public ThrownLightGrenade(LivingEntity owner, Level world, ItemStack grenade) {
        super(ModEntities.LIGHT_GRENADE, owner, world, grenade);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.LIGHT_GRENADE;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isRemoved() && tickCount >= FUSE && level() instanceof ServerLevel world) {
            burst(world);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!isRemoved() && level() instanceof ServerLevel world) {
            burst(world);
        }
    }

    private void burst(ServerLevel world) {
        Vec3 center = position();
        world.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.5, center.z, 40, 1.2, 0.6, 1.2, 0.2);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.8f, 1.6f);
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIUS))) {
            double distance = target.position().distanceTo(center);
            if (distance > RADIUS || !target.isAlive()) {
                continue;
            }
            float falloff = (float) (1 - distance / (RADIUS + 1));
            target.hurtServer(world, damageSources().thrown(this, getOwner()), DAMAGE * falloff);
            Vec3 away = target.position().subtract(center).horizontal();
            if (away.lengthSqr() > 1.0e-4) {
                target.push(away.normalize().scale(0.8 * falloff).add(0, 0.3 * falloff, 0));
                target.needsSync = true;
            }
        }
        // A ring of light walls, two high, wherever there's room
        BlockPos middle = BlockPos.containing(center);
        for (int dx = -RING; dx <= RING; dx++) {
            for (int dz = -RING; dz <= RING; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != RING) {
                    continue;
                }
                for (int dy = 0; dy < 2; dy++) {
                    BlockPos at = middle.offset(dx, dy, dz);
                    if (world.getBlockState(at).canBeReplaced()) {
                        world.setBlockAndUpdate(at, ModBlocks.FADING_BARRIER.defaultBlockState());
                    }
                }
            }
        }
        discard();
    }
}
