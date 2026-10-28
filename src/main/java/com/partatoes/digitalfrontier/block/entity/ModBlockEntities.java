package com.partatoes.digitalfrontier.block.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<DigitizingLaserBlockEntity> DIGITIZING_LASER = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(DigitalFrontier.MOD_ID, "digitizing_laser"),
            FabricBlockEntityTypeBuilder.create(DigitizingLaserBlockEntity::new, ModBlocks.DIGITIZING_LASER, ModBlocks.GRID_TUNING_LASER).build());

    public static void registerModBlockEntities() {
        DigitalFrontier.LOGGER.info("Registering block entities for " + DigitalFrontier.MOD_ID);
    }
}
