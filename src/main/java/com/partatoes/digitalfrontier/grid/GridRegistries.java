package com.partatoes.digitalfrontier.grid;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.gen.GridBiomeSource;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.parts.GridAssetsLoader;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class GridRegistries {
    public static final Identifier GRID_ID = Identifier.of(DigitalFrontier.MOD_ID, "grid");

    public static void register() {
        DigitalFrontier.LOGGER.info("Registering grid world generation for " + DigitalFrontier.MOD_ID);
        Registry.register(Registries.CHUNK_GENERATOR, GRID_ID, GridChunkGenerator.CODEC);
        Registry.register(Registries.BIOME_SOURCE, GRID_ID, GridBiomeSource.CODEC);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GridAssetsLoader());
    }
}
