package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.item.ModItemGroups;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModEnglishLangProvider extends FabricLanguageProvider {
    public ModEnglishLangProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup, TranslationBuilder translationBuilder) {
        translationBuilder.add(ModItems.BINARY_0, "Binary 0");
        translationBuilder.add(ModItems.BINARY_1, "Binary 1");
        translationBuilder.add(ModItems.PIXEL_DUST, "Pixel Dust");
        translationBuilder.add(ModItems.COAL_DUST, "Coal Dust");
        translationBuilder.add(ModItems.LUMINESSENCE, "Luminessence");
        translationBuilder.add(ModItems.LUMINANCE_ORE_DETECTOR, "Luminessence Detector");
        translationBuilder.add("item.digitalfrontier.luminance_detector.tooltip", "Right click a block to detect luminance ore below it.");
        translationBuilder.add(ModItems.BYTE, "Byte");
        translationBuilder.add(ModItems.LIGHTCYCLE_BATON, "Lightcycle Baton");
        translationBuilder.add(ModItems.DIMENSION_TELEPORTER, "Grid Teleporter (Debug)");

        translationBuilder.add(ModBlocks.PIXEL_BLOCK, "Pixel");
        translationBuilder.add(ModBlocks.PIXEL_LAMP, "Pixel Lamp");
        translationBuilder.add(ModBlocks.GRIDSTONE_BLOCK, "Gridstone");
        translationBuilder.add(ModBlocks.CRACKED_GRIDSTONE_BLOCK, "Cracked Gridstone");
        translationBuilder.add(ModBlocks.GRIDSTONE_BRICKS_BLOCK, "Gridstone Bricks");
        translationBuilder.add(ModBlocks.LUMINANCE_ORE, "Luminance Ore");
        translationBuilder.add(ModBlocks.BOOSTER_BLOCK, "Boost Block");
        translationBuilder.add(ModBlocks.LIGHT_BARRIER, "Light Barrier");
        translationBuilder.add(ModBlocks.LIGHT_BARRIER_EMITTER, "Light Barrier Emitter");
        translationBuilder.add(ModBlocks.ARRIVAL_PAD, "Arrival Pad");
        translationBuilder.add(ModBlocks.DIGITIZING_LASER, "Digitizing Laser");
        translationBuilder.add(ModItems.LASER_TUNER, "Laser Tuner");
        translationBuilder.add(ModBlocks.GRID_PORTAL, "Portal");
        translationBuilder.add(ModItems.ESCAPE_KEY, "Escape Key");
        translationBuilder.add(ModItems.MEMORY_PAGING, "Memory Paging");
        translationBuilder.add("message.digitalfrontier.exited_grid", "Left the grid");
        translationBuilder.add("message.digitalfrontier.escape_key_outside_grid", "The Escape Key only works inside the grid");
        translationBuilder.add("message.digitalfrontier.memory_paged", "Memory paged back in at %s");
        translationBuilder.add(ModItems.CORE_DUMP, "Core Dump");

        // Corruption
        translationBuilder.add(ModBlocks.STATIC_BLOCK, "Static");
        ModBlocks.GLITCHED_GRIDSTONE.forEach((color, block) ->
                translationBuilder.add(block, String.format("%s Glitched Gridstone", toTitle(color.asString()))));

        // Inhabitants and strongholds
        translationBuilder.add(ModEntities.CORRUPTED_PROGRAM, "Corrupted Program");
        translationBuilder.add(ModEntities.ENFORCER, "Enforcer");
        translationBuilder.add(ModEntities.LIEUTENANT, "Lieutenant");
        translationBuilder.add("lieutenant.digitalfrontier.the_sovereign", "The Sovereign");
        translationBuilder.add("lieutenant.digitalfrontier.the_warden", "The Warden");
        translationBuilder.add(ModBlocks.STRONGHOLD_UPLINK, "Stronghold Uplink");
        translationBuilder.add("message.digitalfrontier.lieutenant_defeated", "%s has been derezzed");
        translationBuilder.add("message.digitalfrontier.lieutenant_returned", "%s has returned to its stronghold");
        translationBuilder.add("commands.digitalfrontier.grid.unknown", "There's no loaded grid %s");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.none", "no lieutenant");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.alive", "alive");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.defeated", "defeated %ss ago");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.status", "%s at %s: %s, %s, respawned %s times");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.trigger.counted", "The signal reached a stronghold");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.trigger.ignored", "No defeated lieutenant listens for that signal");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.respawn", "The lieutenant of %s has returned");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.defeat", "The lieutenant of %s is marked defeated");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.defeat.failed", "Couldn't defeat the lieutenant of %s: it isn't alive, or there's no such stronghold");
        translationBuilder.add("commands.digitalfrontier.grid.lieutenant.respawn.failed","Couldn't bring back the lieutenant of %s: it isn't defeated, or there's no such stronghold");

        // Biomes
        translationBuilder.add("biome.digitalfrontier.grid_city_core", "City Core");
        translationBuilder.add("biome.digitalfrontier.grid_city", "City");
        translationBuilder.add("biome.digitalfrontier.outskirts", "Outskirts");
        translationBuilder.add("biome.digitalfrontier.outlands", "Outlands");
        translationBuilder.add("biome.digitalfrontier.unallocated_space", "Unallocated Space");
        translationBuilder.add("biome.digitalfrontier.corrupted_city", "Corrupted City");
        translationBuilder.add("biome.digitalfrontier.corrupted_outlands", "Corrupted Outlands");
        translationBuilder.add("biome.digitalfrontier.corrupted_unallocated", "Corrupted Void");

        // Tuned grids
        translationBuilder.add(ModBlocks.GRID_TUNING_LASER, "Grid Tuning Laser");
        translationBuilder.add("gamerule.digitalfrontierMaxTunedGrids", "Digital Frontier: maximum tuned grids");
        translationBuilder.add("message.digitalfrontier.invalid_grid_code", "That isn't a valid grid code");
        translationBuilder.add("message.digitalfrontier.grid_limit", "This server can't hold more than %s tuned grids");
        translationBuilder.add("message.digitalfrontier.laser_tuned_grid", "Laser tuned to grid %s");
        translationBuilder.add("screen.digitalfrontier.grid_tuning", "Grid Tuning");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.seed", "Seed");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.random", "Random");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.corruption", "Corruption: %s%% (strongholds: %s)");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.city_radius", "City radius: %s chunks (%s blocks)");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.block_size", "Blocks");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.waypoints", "Waypoints");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.street_style", "Streets");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.street_style.uniform", "Uniform");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.street_style.varied", "Varied");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.levels", "Levels");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.levels.off", "Streets only");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.levels.skyroads", "Skyroads");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.levels.transit", "Rail and subway");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.levels.all", "Skyroads, rail, subway and skyways");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.districts", "District colors (%s of up to %s)");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.code", "Grid code");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.copy", "Copy");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.paste", "Paste");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.grid_id", "Grid: %s");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.tune", "Tune");
        translationBuilder.add("commands.digitalfrontier.grid.list", "%s of %s tuned grids:");
        translationBuilder.add("commands.digitalfrontier.grid.list.entry", "(city radius %s, corruption %s%%, %s players)");
        translationBuilder.add("commands.digitalfrontier.grid.code", "Grid %s has code %s");
        translationBuilder.add("commands.digitalfrontier.grid.code.untunable", "This grid's settings can't be written as a grid code");
        translationBuilder.add("commands.digitalfrontier.grid.open", "Opened grid %s");
        translationBuilder.add("commands.digitalfrontier.grid.delete", "Deleted grid %s");
        translationBuilder.add("commands.digitalfrontier.grid.delete.unknown", "There's no tuned grid %s");
        translationBuilder.add("message.digitalfrontier.core_dumped", "Core dump restored at %s, inventory intact");

        // Grid terminals and arrival messages
        translationBuilder.add("terminal.digitalfrontier.city_main", "City Terminal");
        translationBuilder.add("terminal.digitalfrontier.waypoint", "Waypoint %s");
        translationBuilder.add("message.digitalfrontier.digitizing", "Digitizing... destination: %s");
        translationBuilder.add("message.digitalfrontier.arrived", "Arrived at %s");
        translationBuilder.add("message.digitalfrontier.terminal_discovered", "Terminal discovered: %s");
        translationBuilder.add("message.digitalfrontier.laser_tuned", "Laser tuned to %s (%s/%s)");
        translationBuilder.add("message.digitalfrontier.scan_interrupted", "Scan interrupted: stay close to the laser");
        translationBuilder.add("message.digitalfrontier.terminal_unreachable", "No signal from the grid terminal");
        translationBuilder.add("message.digitalfrontier.laser_inside_grid", "The laser can't digitize from inside the grid");

        translationBuilder.add(RegistryKey.of(RegistryKeys.ITEM_GROUP, ModItemGroups.DIGITAL_FRONTIER_GROUP_ID), "Digital Frontier");


        translationBuilder.add(ModTags.Items.BIT, "Bit");

        translationBuilder.add(ModEntities.PROGRAM, "Program");
        translationBuilder.add(ModVehicles.LIGHTCYCLE, "Light Cycle");

        // Funky business for the colored gridstone variants
        ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS
                .forEach((patternName, dyeBlockMap) -> {
                    dyeBlockMap.forEach((color, block) -> {
                        translationBuilder.add(block, String.format("%s %s Patterned Gridstone", toTitle(color.asString()), toTitle(patternName)));
                    });
                });

//        "block.digitalfrontier.lime_gridstone": "Lime Gridstone",
//        "block.digitalfrontier.blue_gridstone": "Blue Gridstone",
//        "block.digitalfrontier.red_gridstone": "Red Gridstone",
//        "block.digitalfrontier.white_gridstone": "White Gridstone",
//        "block.digitalfrontier.orange_gridstone": "Orange Gridstone",
//
//
//        "block.digitalfrontier.gridstone_stairs": "Gridstone Stairs",
//        "block.digitalfrontier.gridstone_slab": "Gridstone Slab",
//        "block.digitalfrontier.gridstone_wall": "Gridstone Wall",
//        "block.digitalfrontier.gridstone_pressure_plate": "Gridstone Pressure Plate",
//
//        "block.digitalfrontier.cracked_gridstone_stairs": "Cracked Gridstone Stairs",
//        "block.digitalfrontier.cracked_gridstone_slab": "Cracked Gridstone Slab",
//        "block.digitalfrontier.cracked_gridstone_wall": "Cracked Gridstone Wall",
//
//        "block.digitalfrontier.gridstone_bricks_stairs": "Gridstone Brick Stairs",
//        "block.digitalfrontier.gridstone_bricks_slab": "Gridstone Brick Slab",
//        "block.digitalfrontier.gridstone_bricks_wall": "Gridstone Brick Wall",
    }

    private String toTitle(String name) {
        return name.replaceFirst(".", name.substring(0, 1).toUpperCase());
    }
}
