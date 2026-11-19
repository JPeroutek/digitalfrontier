package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.item.ModItemGroups;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import java.util.concurrent.CompletableFuture;

public class ModEnglishLangProvider extends FabricLanguageProvider {
    public ModEnglishLangProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider wrapperLookup, TranslationBuilder translationBuilder) {
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
        translationBuilder.add(ModBlocks.DISINFECTION_PAD, "Disinfection Pad");
        translationBuilder.add(ModBlocks.LIGHT_BARRIER, "Light Barrier");
        translationBuilder.add(ModBlocks.LIGHT_BARRIER_EMITTER, "Light Barrier Emitter");
        translationBuilder.add(ModBlocks.ARRIVAL_PAD, "Arrival Pad");
        translationBuilder.add(ModBlocks.DIGITIZING_LASER, "Digitizing Laser");
        translationBuilder.add(ModItems.LASER_TUNER, "Laser Tuner");
        translationBuilder.add(ModBlocks.GRID_PORTAL, "Portal");
        translationBuilder.add(ModBlocks.MOVING_LIGHT, "Moving Light");
        translationBuilder.add(ModItems.ESCAPE_KEY, "Escape Key");
        translationBuilder.add(ModItems.MEMORY_PAGING, "Memory Paging");
        translationBuilder.add("message.digitalfrontier.exited_grid", "Left the grid");
        translationBuilder.add("message.digitalfrontier.portal_sealed", "The Portal is sealed: step in holding an Escape Key to open it for good");
        translationBuilder.add("message.digitalfrontier.portal_opened", "%s paid the Portal's toll: it's open to everyone now");
        translationBuilder.add("message.digitalfrontier.escape_key_outside_grid", "The Escape Key only works inside the grid");
        translationBuilder.add("message.digitalfrontier.memory_paged", "Memory paged back in at %s");
        translationBuilder.add(ModItems.CORE_DUMP, "Core Dump");
        translationBuilder.add(ModItems.DATA_DISC, "Data Disc");
        translationBuilder.add(ModEntities.DATA_DISC, "Data Disc");
        translationBuilder.add(ModItems.LIGHT_STAFF, "Light Staff");
        translationBuilder.add(ModEntities.RECOGNIZER, "Recognizer");
        // The arena
        translationBuilder.add(ModEntities.CHAMPION, "Champion");
        translationBuilder.add(ModBlocks.ARENA_TILE, "Arena Tile");
        translationBuilder.add(ModBlocks.ARENA_CONSOLE, "Arena Console");
        translationBuilder.add(ModBlocks.TROPHY, "Trophy");
        translationBuilder.add(ModBlocks.TRAIL_WALL, "Trail Wall");
        translationBuilder.add(ModItems.CESTA, "Cesta");
        translationBuilder.add(ModItems.RING_BALL, "Ring Ball");
        translationBuilder.add(ModEntities.RING_BALL, "Ring Ball");
        translationBuilder.add("arena.digitalfrontier.goal", "Goal to %s! %s to %s.");
        translationBuilder.add("arena.digitalfrontier.derezzed", "%s derezzes!");
        translationBuilder.add("arena.digitalfrontier.disc_duel", "Disc Duel");
        translationBuilder.add("arena.digitalfrontier.light_cycle", "Light Cycle Match");
        translationBuilder.add("arena.digitalfrontier.ring_game", "Ring Game");
        translationBuilder.add("arena.digitalfrontier.announcer", "[Announcer] %s");
        translationBuilder.add("arena.digitalfrontier.menu", "The arena's games (click one to step in):");
        translationBuilder.add("arena.digitalfrontier.champion_level", "champion level %s");
        translationBuilder.add("arena.digitalfrontier.busy", "A match is under way: wait for it to finish");
        translationBuilder.add("arena.digitalfrontier.none", "There's no arena here");
        translationBuilder.add("arena.digitalfrontier.come_closer", "Step into the arena to play");
        translationBuilder.add("arena.digitalfrontier.unknown_game", "No arena game %s");
        translationBuilder.add("arena.digitalfrontier.intro", "%s takes on the champion at the %s! Champion level %s.");
        translationBuilder.add("arena.digitalfrontier.conscripted_intro", "The regime brings us %s, to face the champion at the %s! Champion level %s.");
        translationBuilder.add("arena.digitalfrontier.countdown", "%s...");
        translationBuilder.add("arena.digitalfrontier.go", "Begin!");
        translationBuilder.add("arena.digitalfrontier.fell", "%s falls!");
        translationBuilder.add("arena.digitalfrontier.won", "%s wins the %s!");
        translationBuilder.add("arena.digitalfrontier.lost", "%s is beaten, and leaves the floor.");
        translationBuilder.add("arena.digitalfrontier.title", "%s has beaten the title champion at the %s!");
        translationBuilder.add("commands.digitalfrontier.grid.arena.where", "The arena is at %s; %s titles won there");
        translationBuilder.add("commands.digitalfrontier.grid.arena.game", "%s: champion level %s");
        translationBuilder.add("commands.digitalfrontier.grid.arena.playing", "A %s is under way");
        translationBuilder.add("commands.digitalfrontier.grid.arena.reset", "Arena reset: champions back to level 1, titles cleared");
        translationBuilder.add(ModItems.LIGHT_GRENADE, "Light Grenade");
        translationBuilder.add(ModEntities.LIGHT_GRENADE, "Light Grenade");
        translationBuilder.add(ModItems.FORCE_SHIELD, "Force Shield");
        translationBuilder.add(ModItems.CHUTE, "Chute");
        translationBuilder.add(ModItems.OVERCLOCK, "Overclock");
        translationBuilder.add("effect.digitalfrontier.overclocked", "Overclocked");
        translationBuilder.add(ModBlocks.FADING_BARRIER, "Fading Barrier");

        // Corruption
        translationBuilder.add(ModBlocks.STATIC_BLOCK, "Static");
        ModBlocks.GLITCHED_GRIDSTONE.forEach((color, block) ->
                translationBuilder.add(block, String.format("%s Glitched Gridstone", toTitle(color.getSerializedName()))));

        // Inhabitants and strongholds
        translationBuilder.add(ModEntities.CORRUPTED_PROGRAM, "Corrupted Program");
        translationBuilder.add(ModEntities.ENFORCER, "Enforcer");
        translationBuilder.add(ModEntities.RECRUITER, "Recruiter");
        translationBuilder.add(ModEntities.BOOL, "Bool");
        translationBuilder.add(ModEntities.SYSTEM_CORE, "System Core");
        translationBuilder.add(ModBlocks.CORE_SEAL, "Core Seal");
        translationBuilder.add("message.digitalfrontier.core_awake", "The regime's last lieutenant has fallen, and the System Core stirs beneath the city center. The shaft at %s is open.");
        translationBuilder.add("message.digitalfrontier.core_defeated", "The System Core is derezzed. The regime is broken.");
        translationBuilder.add("commands.digitalfrontier.derez.style", "Bodies derez with style: %s");
        translationBuilder.add("commands.digitalfrontier.derez.unknown", "No derez style %s (try sweep, instant, freeze or voxel)");
        translationBuilder.add("commands.digitalfrontier.grid.core.status","System Core: %s (the shaft down to it is at %s)");
        translationBuilder.add("commands.digitalfrontier.grid.core.reset", "System Core reset: dormant, its seal closed, and its fallen lieutenants back");
        translationBuilder.add("commands.digitalfrontier.grid.portal.status", "%s: %s");
        translationBuilder.add("commands.digitalfrontier.grid.portal.open", "open");
        translationBuilder.add("commands.digitalfrontier.grid.portal.sealed", "sealed");
        translationBuilder.add("commands.digitalfrontier.grid.portal.opened", "Opened %s Portals");
        translationBuilder.add("commands.digitalfrontier.grid.portal.resealed", "Sealed %s Portals again");
        translationBuilder.add("entity.digitalfrontier.program.mechanic", "Mechanic");
        translationBuilder.add("entity.digitalfrontier.program.portal_keeper", "Portal Keeper");
        translationBuilder.add("entity.digitalfrontier.program.portal_keeper.line0", "The Portal opens on the real world, but it's sealed until someone pays its toll: one Escape Key. I keep one for those who can pay.");
        translationBuilder.add("entity.digitalfrontier.program.portal_keeper.line1", "Step in with a key in hand and the Portal opens, for you and everyone after you, for good. You wake where you last slept, out there.");
        translationBuilder.add("entity.digitalfrontier.program.portal_keeper.line2", "The regime watches this place. Whatever you are to them, the Portal doesn't care.");
        translationBuilder.add("entity.digitalfrontier.recruiter.tribute", "The regime notes your tribute");
        translationBuilder.add(ModEntities.LIEUTENANT, "Lieutenant");
        translationBuilder.add("lieutenant.digitalfrontier.the_sovereign", "The Sovereign");
        translationBuilder.add("lieutenant.digitalfrontier.the_warden", "The Warden");
        translationBuilder.add("pack.digitalfrontier.lore", "Digital Frontier: Lore Names");
        translationBuilder.add("pack.digitalfrontier.lore.description", "The films' names and character skins");
        translationBuilder.add("commands.digitalfrontier.grid.locate", "%s is at %s");
        translationBuilder.add("message.digitalfrontier.lieutenant_redeemed", "%s has been redeemed and turns on the regime");
        translationBuilder.add("commands.digitalfrontier.grid.regime.status", "The regime has %s lasting members; it wants %s");
        translationBuilder.add("commands.digitalfrontier.grid.regime.alignment", "%s's alignment with the regime: %s");
        translationBuilder.add("commands.digitalfrontier.grid.locate.unknown", "This grid has no site %s");
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
        translationBuilder.add("gamerule.digitalfrontier.max_tuned_grids", "Digital Frontier: maximum tuned grids");
        translationBuilder.add("message.digitalfrontier.invalid_grid_code", "That isn't a valid grid code");
        translationBuilder.add("message.digitalfrontier.grid_limit", "This server can't hold more than %s tuned grids");
        translationBuilder.add("message.digitalfrontier.laser_tuned_grid", "Laser tuned to grid %s");
        translationBuilder.add("screen.digitalfrontier.grid_tuning", "Grid Tuning");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.seed", "Seed");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.random", "Random");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.corruption", "Decay: %s%%");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.regime", "Regime: %s%% (strongholds: %s)");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.city_radius", "City radius: %s chunks (%s blocks)");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.block_size", "Blocks");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.waypoints", "Waypoints");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.street_style", "Streets");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.street_style.uniform", "Uniform");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.street_style.varied", "Varied");
        translationBuilder.add("screen.digitalfrontier.grid_tuning.levels", "Levels");
        translationBuilder.add("sign.digitalfrontier.line", "Line");
        translationBuilder.add("sign.digitalfrontier.subway", "Subway");
        translationBuilder.add("sign.digitalfrontier.elevated", "Elevated");
        translationBuilder.add("sign.digitalfrontier.express", "Express");
        translationBuilder.add("sign.digitalfrontier.express_to", "to Waypoint %s");
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
        translationBuilder.add("commands.digitalfrontier.grid.list.entry", "(city radius %s, decay %s%%, regime %s%%, %s players)");
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

        translationBuilder.add(ResourceKey.create(Registries.CREATIVE_MODE_TAB, ModItemGroups.DIGITAL_FRONTIER_GROUP_ID), "Digital Frontier");


        translationBuilder.add(ModTags.Items.BIT, "Bit");

        translationBuilder.add(ModEntities.PROGRAM, "Program");
        translationBuilder.add(ModVehicles.LIGHTCYCLE, "Light Cycle");

        // Funky business for the colored gridstone variants
        ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS
                .forEach((patternName, dyeBlockMap) -> {
                    dyeBlockMap.forEach((color, block) -> {
                        translationBuilder.add(block, String.format("%s %s Patterned Gridstone", toTitle(color.getSerializedName()), toTitle(patternName)));
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
