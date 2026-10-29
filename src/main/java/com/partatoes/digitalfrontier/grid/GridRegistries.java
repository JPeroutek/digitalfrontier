package com.partatoes.digitalfrontier.grid;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.gen.GridBiomeSource;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.parts.GridAssetsLoader;
import com.partatoes.digitalfrontier.grid.stronghold.Lieutenants;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class GridRegistries {
    public static final Identifier GRID_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid");

    public static void register() {
        DigitalFrontier.LOGGER.info("Registering grid world generation for " + DigitalFrontier.MOD_ID);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, GRID_ID, GridChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.BIOME_SOURCE, GRID_ID, GridBiomeSource.CODEC);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new GridAssetsLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Lieutenants());
    }
}
