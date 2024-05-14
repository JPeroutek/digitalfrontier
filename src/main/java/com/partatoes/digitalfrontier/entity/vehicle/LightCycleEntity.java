package com.partatoes.digitalfrontier.entity.vehicle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class LightCycleEntity extends Entity {
    private static final double MODEL_WIDTH = 1;
    private static final double MODEL_LENGTH = 3.5;
    private static final double MODEL_HEIGHT = 1.5;

    public LightCycleEntity(EntityType<?> type, World world) {
        super(type, world);
    }

//    @Override
//    public void setPosition(double x, double y, double z) {
//        this.setPos(x, y + .2, z);
////        this.setBoundingBox(this.createBoundingBox(x, y, z));
//    }
//
//    private Box createBoundingBox(double x, double y, double z) {
//        return new Box(x, y + .2, z, x - LightCycleEntity.MODEL_WIDTH, y + LightCycleEntity.MODEL_HEIGHT, z + LightCycleEntity.MODEL_LENGTH);
//    }

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
