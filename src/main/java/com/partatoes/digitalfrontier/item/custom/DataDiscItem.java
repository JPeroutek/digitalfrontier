package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.entity.projectile.ThrownDataDisc;
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

/**
 * A Data Disc: thrown, it ricochets and comes back (see {@link ThrownDataDisc}); the disc leaves the hand while it
 * flies. Carried in the offhand into a fatal blow in a grid, it stands in for a Core Dump once (see
 * {@link com.partatoes.digitalfrontier.grid.exit.MemoryPaging}).
 */
public class DataDiscItem extends Item {
    private static final int COOLDOWN = 10;

    public DataDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (world instanceof ServerLevel serverWorld) {
            ItemStack disc = stack.copyWithCount(1);
            Projectile.spawnProjectileFromRotation((level, owner, item) -> new ThrownDataDisc(owner, level, item),
                    serverWorld, disc, player, 0, 1.6f, 0.5f);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 1.3f);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (world instanceof ServerLevel) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
