package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.effect.Overclock;
import com.partatoes.digitalfrontier.item.custom.Chute;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.block.entity.ModBlockEntities;
import com.partatoes.digitalfrontier.compat.ModCompat;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.grid.GridCommands;
import com.partatoes.digitalfrontier.grid.GridManager;
import com.partatoes.digitalfrontier.grid.GridRegistries;
import com.partatoes.digitalfrontier.grid.GridSpawner;
import com.partatoes.digitalfrontier.grid.core.CoreManager;
import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdEvents;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import com.partatoes.digitalfrontier.grid.tuning.GridTuningNetworking;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.exit.MemoryPaging;
import com.partatoes.digitalfrontier.grid.exit.PortalBeams;
import com.partatoes.digitalfrontier.item.ModItemGroups;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.world.gen.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigitalFrontier implements ModInitializer {
	public static final String MOD_ID = "digitalfrontier";
	/** The built-in lore pack's folder under resourcepacks/ (see docs/lieutenants.md). */
	public static final String LORE_PACK = "lore";

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

		// Vehicle Registration
		ModVehicles.registerModVehicles();

		// Entity Registration
		ModEntities.registerModEntities();

		ModWorldGeneration.generateModWorldGen();
		GridRegistries.register();
		GridArrival.register();
		MemoryPaging.register();
		PortalBeams.register();
		GridManager.register();
		GridCommands.register();
		GridTuningNetworking.register();
		StrongholdManager.register();
		StrongholdEvents.register();
		GridSpawner.register();
		Regime.register();
		CoreManager.register();
		Overclock.register();
		Chute.register();
		ModCompat.register();

		// The films' names and the project's character skins, off unless a player or server owner turns it on.
		// One folder serves both halves: data (enabled per world) and assets (enabled per client)
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(mod -> ResourceLoader.registerBuiltinPack(
				Identifier.fromNamespaceAndPath(MOD_ID, LORE_PACK), mod,
				Component.translatable("pack.digitalfrontier.lore"), PackActivationType.NORMAL));
	}
}