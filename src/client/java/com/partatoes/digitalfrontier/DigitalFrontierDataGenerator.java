package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.datagen.*;
import com.partatoes.digitalfrontier.datagen.grid.ModGridStarterProvider;
import com.partatoes.digitalfrontier.world.ModBiomes;
import com.partatoes.digitalfrontier.world.ModConfiguredFeatures;
import com.partatoes.digitalfrontier.world.ModDimensions;
import com.partatoes.digitalfrontier.world.ModPlacedFeatures;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;

public class DigitalFrontierDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(ModBlockTagProvider::new);
		pack.addProvider(ModItemTagProvider::new);
		pack.addProvider(ModLootTableProvider::new);
		pack.addProvider(ModModelProvider::new);
		pack.addProvider(ModRecipeProvider::new);
		pack.addProvider(ModWorldGenerator::new);
		pack.addProvider(ModDimensionProvider::new);
		pack.addProvider(ModGridStarterProvider::new);
		pack.addProvider(ModEnglishLangProvider::new);
		pack.addProvider(ModTextureProvider::new);
	}

	@Override
	public void buildRegistry(RegistryBuilder registryBuilder) {
		registryBuilder.addRegistry(RegistryKeys.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap);
		registryBuilder.addRegistry(RegistryKeys.PLACED_FEATURE, ModPlacedFeatures::bootstrap);
		registryBuilder.addRegistry(RegistryKeys.BIOME, ModBiomes::bootstrap);
		registryBuilder.addRegistry(RegistryKeys.DIMENSION_TYPE, ModDimensions::bootstrapType);
		DataGeneratorEntrypoint.super.buildRegistry(registryBuilder);
	}
}
