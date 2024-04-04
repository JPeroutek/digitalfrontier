package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItemGroups;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
*	TODO: Add the rest of the blocks (possibly with variants to account for colors?
*   TODO: Gridstone other blocks, stairs, walls, slabs, etc
*   TODO: Luminessence and Luminessence Ore and deepslate, nether, end, grid variants
*   TODO: Luminessence + binary_1 = Pixel Dust
*   TODO: Luminessence + binary_0 = Coal (or charcoal, some kind of fuel)
*   TODO: Luminessence + Redstone = 2 redstone
*	TODO: Add "Bit" tag to binary items
*   TODO: Add "Byte" item, crafted with random assortment of bits
*   TODO: Make Byte edible
*   TODO: dimension, biomes, etc
* */
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
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
	}
}