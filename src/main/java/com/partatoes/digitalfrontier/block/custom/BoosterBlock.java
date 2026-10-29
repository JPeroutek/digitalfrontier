package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BoosterBlock extends Block {
    public BoosterBlock(Properties settings) {
        super(settings);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (entity.showVehicleHealth() && !entity.isSteppingCarefully()) {
            ((LivingEntity) entity).addEffect(new MobEffectInstance(MobEffects.SPEED, 1));
        }
        super.stepOn(world, pos, state, entity);
    }
}
