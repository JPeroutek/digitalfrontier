package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.block.entity.ModBlockEntities;
import com.partatoes.digitalfrontier.compat.ModCompat;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.grid.GridCommands;
import com.partatoes.digitalfrontier.grid.GridManager;
import com.partatoes.digitalfrontier.grid.GridRegistries;
import com.partatoes.digitalfrontier.grid.tuning.GridTuningNetworking;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.exit.MemoryPaging;
import com.partatoes.digitalfrontier.item.ModFuelItems;
import com.partatoes.digitalfrontier.item.ModItemGroups;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.world.gen.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigitalFrontier implements ModInitializer {
	public static final String MOD_ID = "digitalfrontier";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("digitalfrontier");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		// Basic Registries
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerModBlockEntities();
		ModFuelItems.registerModFuelItems();

		// Vehicle Registration
		ModVehicles.registerModVehicles();

		// Entity Registration
		FabricDefaultAttributeRegistry.register(ModEntities.PROGRAM, ProgramEntity.createProgramAttributes());
		ModEntities.registerModEntities();

		ModWorldGeneration.generateModWorldGen();
		GridRegistries.register();
		GridArrival.register();
		MemoryPaging.register();
		GridManager.register();
		GridCommands.register();
		GridTuningNetworking.register();
		ModCompat.register();
	}
}