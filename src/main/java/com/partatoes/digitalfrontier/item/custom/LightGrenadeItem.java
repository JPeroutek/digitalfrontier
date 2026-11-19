package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.entity.projectile.ThrownLightGrenade;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A Light Grenade: lobbed like a snowball, it bursts into a damaging flash and a ring of fading light walls. */
public class LightGrenadeItem extends Item {
    private static final int COOLDOWN = 20;

    public LightGrenadeItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world instanceof ServerLevel serverWorld) {
            Projectile.spawnProjectileFromRotation((level, owner, item) -> new ThrownLightGrenade(owner, level, item),
                    serverWorld, stack.copyWithCount(1), user, 0, 0.9f, 1.0f);
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.6f);
        user.getCooldowns().addCooldown(stack, COOLDOWN);
        stack.consume(1, user);
        return InteractionResult.SUCCESS;
    }
}
