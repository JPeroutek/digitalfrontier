package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleEntity;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LightCycleBaton extends Item {

    private static final Predicate<Entity> RIDERS;

    public LightCycleBaton(Properties settings) {
        super(settings);
    }

    private LightCycleEntity createEntity(Level world, HitResult hitResult, ItemStack stack, Player player) {
        Vec3 vec = hitResult.getLocation();
        LightCycleEntity lightCycle = new LightCycleEntity(ModVehicles.LIGHTCYCLE, world);
        lightCycle.setPos(vec);
        if (world instanceof ServerLevel serverWorld) {
            EntityType.createDefaultStackConfig(serverWorld, stack, player).accept(lightCycle);
        }
        return lightCycle;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        HitResult hitResult = getPlayerPOVHitResult(world, user, ClipContext.Fluid.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        } else {
            Vec3 vec3d = user.getViewVector(1.0F);
            double d = 5.0;
            List<Entity> list = world.getEntities(user, user.getBoundingBox().expandTowards(vec3d.scale(5.0)).inflate(1.0), RIDERS);
            if (!list.isEmpty()) {
                Vec3 vec3d2 = user.getEyePosition();
                Iterator var11 = list.iterator();

                while(var11.hasNext()) {
                    Entity entity = (Entity)var11.next();
                    AABB box = entity.getBoundingBox().inflate((double)entity.getPickRadius());
                    if (box.contains(vec3d2)) {
                        return InteractionResult.PASS;
                    }
                }
            }

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                LightCycleEntity lightCyle = this.createEntity(world, hitResult, itemStack, user);
//                lightCyle.setVariant(this.type);
                lightCyle.setYRot(user.getYRot());
                if (!world.noCollision(lightCyle, lightCyle.getBoundingBox())) {
                    return InteractionResult.PASS;
                } else {
                    if (!world.isClientSide()) {
                        world.addFreshEntity(lightCyle);
                        world.gameEvent(user, GameEvent.ENTITY_PLACE, hitResult.getLocation());
                        itemStack.consume(1, user);
                    }

                    user.awardStat(Stats.ITEM_USED.get(this));
                    return InteractionResult.SUCCESS;
                }
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    static {
        RIDERS = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
    }
}
