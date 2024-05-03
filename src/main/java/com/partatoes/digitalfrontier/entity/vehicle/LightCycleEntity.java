package com.partatoes.digitalfrontier.entity.vehicle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class LightCycleEntity extends Entity {

    public LightCycleEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // Not currently needing to track any data
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        // Will be needed in the future to track different "Types" of lightcycles.
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        // Will be needed in the future to track different "Types" of lightcycles.
    }
}
