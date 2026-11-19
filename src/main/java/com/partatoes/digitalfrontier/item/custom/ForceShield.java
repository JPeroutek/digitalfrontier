package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The Force Shield: blocks like a shield (its blocks_attacks component), and while raised, sends any projectile that
 * strikes its face straight back, Data Discs and grenades included. Each reflection wears it a little.
 */
public final class ForceShield {
    private ForceShield() {
    }

    /** Whether this holder is blocking with a Force Shield facing the projectile; wears the shield if so. */
    public static boolean reflects(LivingEntity holder, Projectile projectile) {
        if (!holder.isBlocking()) {
            return false;
        }
        ItemStack shield = holder.getItemBlockingWith();
        if (shield == null || !shield.is(ModItems.FORCE_SHIELD)) {
            return false;
        }
        // Only what comes at its face
        Vec3 facing = holder.getViewVector(1).horizontal();
        Vec3 toward = projectile.position().subtract(holder.position()).horizontal();
        if (facing.lengthSqr() < 1.0e-4 || toward.lengthSqr() < 1.0e-4 || facing.normalize().dot(toward.normalize()) < 0.2) {
            return false;
        }
        if (holder.level() instanceof ServerLevel world) {
            world.playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.0f, 1.6f);
            EquipmentSlot slot = holder.getOffhandItem() == shield ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            shield.hurtAndBreak(1, holder, slot);
        }
        return true;
    }
}
