package com.partatoes.digitalfrontier.entity.projectile;

import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * A thrown Data Disc. It flies straight, ricochets off up to {@link #BOUNCES} surfaces, and hurts the first time it
 * passes through each creature on each leg of its flight. Then it comes back to its thrower, through walls if it must.
 * If the thrower is gone (logged out, dead, in another dimension) it drops where it is. The disc it carries is the
 * thrower's own item, worn by each hit.
 */
public class ThrownDataDisc extends ThrowableItemProjectile {
    public static final int BOUNCES = 3;
    private static final float DAMAGE = 7;
    // How long it flies out before turning back, in ticks
    private static final int REACH = 25;
    private static final double RETURN_SPEED = 1.4;

    private int bouncesLeft = BOUNCES;
    private boolean returning;
    private int flying;
    // Who it has already hit on this leg; cleared at each bounce and on the way back
    private final Set<Integer> hitThisLeg = new HashSet<>();

    public ThrownDataDisc(EntityType<? extends ThrownDataDisc> entityType, Level world) {
        super(entityType, world);
    }

    public ThrownDataDisc(LivingEntity owner, Level world, ItemStack disc) {
        super(ModEntities.DATA_DISC, owner, world, disc);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.DATA_DISC;
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        if (returning) {
            noPhysics = true;
        }
        super.tick();
        if (!(level() instanceof ServerLevel world)) {
            return;
        }
        if (!returning && ++flying > REACH) {
            turnBack();
        }
        if (!returning) {
            return;
        }
        Entity owner = getOwner();
        if (!(owner instanceof Player player) || !player.isAlive() || player.level() != world) {
            spawnAtLocation(world, getItem());
            discard();
            return;
        }
        Vec3 toOwner = player.getEyePosition().subtract(position());
        if (toOwner.lengthSqr() < 2.25) {
            catchBy(player);
            return;
        }
        setDeltaMovement(toOwner.normalize().scale(RETURN_SPEED));
        needsSync = true;
    }

    private void turnBack() {
        returning = true;
        hitThisLeg.clear();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (returning) {
            return;
        }
        super.onHitBlock(hit);
        if (bouncesLeft-- > 0) {
            setDeltaMovement(Ricochet.reflect(getDeltaMovement(), hit.getDirection()));
            needsSync = true;
            hitThisLeg.clear();
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 1.6f);
        } else {
            turnBack();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        if (target == getOwner()) {
            if (returning && target instanceof Player player) {
                catchBy(player);
            }
            return;
        }
        if (!(level() instanceof ServerLevel world) || !hitThisLeg.add(target.getId())) {
            return;
        }
        Entity owner = getOwner();
        if (target.hurtServer(world, damageSources().thrown(this, owner == null ? this : owner), DAMAGE)) {
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8f, 1.5f);
            ItemStack disc = getItem().copy();
            disc.hurtAndBreak(1, world, owner instanceof ServerPlayer player ? player : null, item -> {
                world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                discard();
            });
            setItem(disc);
        }
    }

    // Back in its thrower's hand if that's free, otherwise anywhere in their inventory, otherwise at their feet
    private void catchBy(Player player) {
        if (!isRemoved()) {
            ItemStack disc = getItem();
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, disc);
            } else if (!player.getInventory().add(disc) && level() instanceof ServerLevel world) {
                player.spawnAtLocation(world, disc);
            }
            level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0f, 1.2f);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putInt("BouncesLeft", bouncesLeft);
        view.putBoolean("Returning", returning);
        view.putInt("Flying", flying);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        bouncesLeft = view.getIntOr("BouncesLeft", BOUNCES);
        returning = view.getBooleanOr("Returning", false);
        flying = view.getIntOr("Flying", 0);
    }
}
