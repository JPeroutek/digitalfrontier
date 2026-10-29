package com.partatoes.digitalfrontier.block.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final BlockEntityType<DigitizingLaserBlockEntity> DIGITIZING_LASER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "digitizing_laser"),
            FabricBlockEntityTypeBuilder.create(DigitizingLaserBlockEntity::new, ModBlocks.DIGITIZING_LASER, ModBlocks.GRID_TUNING_LASER).build());

    public static void registerModBlockEntities() {
        DigitalFrontier.LOGGER.info("Registering block entities for " + DigitalFrontier.MOD_ID);
    }
}
