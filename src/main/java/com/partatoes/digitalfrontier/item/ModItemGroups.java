package com.partatoes.digitalfrontier.item;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    public static final Identifier DIGITAL_FRONTIER_GROUP_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "digitalfrontier");
    public static final CreativeModeTab DIGITAL_FRONTIER_GROUP = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DIGITAL_FRONTIER_GROUP_ID,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemgroup.digitalfrontier"))
                    .icon(() -> new ItemStack(ModItems.BINARY_1))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(ModItems.BINARY_0);
                        entries.accept(ModItems.BINARY_1);
                        entries.accept(ModItems.PIXEL_DUST);
                        entries.accept(ModItems.COAL_DUST);
                        entries.accept(ModItems.LUMINESSENCE);
                        entries.accept(ModItems.LUMINANCE_ORE_DETECTOR);
                        entries.accept(ModItems.BYTE);
                        entries.accept(ModItems.LIGHTCYCLE_BATON);
                        entries.accept(ModItems.DIMENSION_TELEPORTER);
                        entries.accept(ModItems.LASER_TUNER);
                        entries.accept(ModBlocks.DIGITIZING_LASER);
                        entries.accept(ModBlocks.GRID_TUNING_LASER);
                        entries.accept(ModBlocks.ARRIVAL_PAD);
                        entries.accept(ModItems.ESCAPE_KEY);
                        entries.accept(ModItems.MEMORY_PAGING);
                        entries.accept(ModItems.CORE_DUMP);
                        entries.accept(ModBlocks.GRID_PORTAL);

                        entries.accept(ModBlocks.PIXEL_BLOCK);
                        entries.accept(ModBlocks.PIXEL_LAMP);
                        entries.accept(ModBlocks.GRIDSTONE_BLOCK);
                        entries.accept(ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                        entries.accept(ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                        entries.accept(ModBlocks.LUMINANCE_ORE);
                        entries.accept(ModBlocks.BOOSTER_BLOCK);
                        entries.accept(ModBlocks.KEYBOARD_BLOCK);

                        entries.accept(ModBlocks.GRIDSTONE_STAIRS);
                        entries.accept(ModBlocks.GRIDSTONE_SLAB);
                        entries.accept(ModBlocks.GRIDSTONE_WALL);
                        entries.accept(ModBlocks.GRIDSTONE_PRESSURE_PLATE);

                        entries.accept(ModBlocks.CRACKED_GRIDSTONE_STAIRS);
                        entries.accept(ModBlocks.CRACKED_GRIDSTONE_SLAB);
                        entries.accept(ModBlocks.CRACKED_GRIDSTONE_WALL);

                        entries.accept(ModBlocks.GRIDSTONE_BRICKS_STAIRS);
                        entries.accept(ModBlocks.GRIDSTONE_BRICKS_SLAB);
                        entries.accept(ModBlocks.GRIDSTONE_BRICKS_WALL);
                        entries.accept(ModBlocks.LIGHT_BARRIER_EMITTER);

                        ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.values()
                                .forEach(colorMapping -> colorMapping.values().forEach(entries::accept));

                        entries.accept(ModBlocks.STATIC_BLOCK);
                        entries.accept(ModBlocks.STRONGHOLD_UPLINK);
                        ModBlocks.GLITCHED_GRIDSTONE.values().forEach(entries::accept);
                    })
                    .build());

    public static void registerItemGroups() {
        DigitalFrontier.LOGGER.info("Registering Item Groups for " + DigitalFrontier.MOD_ID);
    }
}
