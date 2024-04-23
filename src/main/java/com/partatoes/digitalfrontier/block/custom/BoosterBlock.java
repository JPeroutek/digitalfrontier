package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BoosterBlock extends Block {
    public BoosterBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (entity.isLiving() && !entity.bypassesSteppingEffects()) {
            ((LivingEntity) entity).addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1));
        }
        super.onSteppedOn(world, pos, state, entity);
    }
}
