package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class LightCycleBaton extends Item {

    private static final Predicate<Entity> RIDERS;

    public LightCycleBaton(Settings settings) {
        super(settings);
    }

    private LightCycleEntity createEntity(World world, HitResult hitResult, ItemStack stack, PlayerEntity player) {
        Vec3d vec = hitResult.getPos();
        LightCycleEntity lightCycle = new LightCycleEntity(ModVehicles.LIGHTCYCLE, world);
        lightCycle.setPosition(vec);
        if (world instanceof ServerWorld serverWorld) {
            EntityType.copier(serverWorld, stack, player).accept(lightCycle);
        }
        return lightCycle;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        HitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return ActionResult.PASS;
        } else {
            Vec3d vec3d = user.getRotationVec(1.0F);
            double d = 5.0;
            List<Entity> list = world.getOtherEntities(user, user.getBoundingBox().stretch(vec3d.multiply(5.0)).expand(1.0), RIDERS);
            if (!list.isEmpty()) {
                Vec3d vec3d2 = user.getEyePos();
                Iterator var11 = list.iterator();

                while(var11.hasNext()) {
                    Entity entity = (Entity)var11.next();
                    Box box = entity.getBoundingBox().expand((double)entity.getTargetingMargin());
                    if (box.contains(vec3d2)) {
                        return ActionResult.PASS;
                    }
                }
            }

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                LightCycleEntity lightCyle = this.createEntity(world, hitResult, itemStack, user);
//                lightCyle.setVariant(this.type);
                lightCyle.setYaw(user.getYaw());
                if (!world.isSpaceEmpty(lightCyle, lightCyle.getBoundingBox())) {
                    return ActionResult.PASS;
                } else {
                    if (!world.isClient) {
                        world.spawnEntity(lightCyle);
                        world.emitGameEvent(user, GameEvent.ENTITY_PLACE, hitResult.getPos());
                        itemStack.decrementUnlessCreative(1, user);
                    }

                    user.incrementStat(Stats.USED.getOrCreateStat(this));
                    return ActionResult.SUCCESS;
                }
            } else {
                return ActionResult.PASS;
            }
        }
    }

    static {
        RIDERS = EntityPredicates.EXCEPT_SPECTATOR.and(Entity::canHit);
    }
}
