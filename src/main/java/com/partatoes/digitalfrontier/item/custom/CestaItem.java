package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.entity.projectile.RingBall;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * The Cesta: a scoop for the ring game's ball. Use it to catch a loose ball within reach, and again to throw the ball
 * you hold where you're looking (see {@link RingBall}).
 */
public class CestaItem extends Item {
    private static final double REACH = 3.5;

    public CestaItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.SUCCESS;
        }
        RingBall nearest = serverWorld.getEntitiesOfClass(RingBall.class, user.getBoundingBox().inflate(REACH + 2)).stream()
                .min(Comparator.comparingDouble(ball -> ball.distanceToSqr(user))).orElse(null);
        if (nearest == null) {
            return InteractionResult.PASS;
        }
        if (nearest.heldBy(user)) {
            Vec3 look = user.getLookAngle();
            nearest.throwAlong(look.add(0, 0.08, 0));
            return InteractionResult.SUCCESS;
        }
        if (nearest.free() && nearest.distanceTo(user) <= REACH) {
            nearest.catchBy(user);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
