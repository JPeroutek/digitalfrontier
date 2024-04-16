package com.partatoes.digitalfrontier.item;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup DIGITAL_FRONTIER_GROUP = Registry.register(Registries.ITEM_GROUP, new Identifier(DigitalFrontier.MOD_ID, "digitalfrontier"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.digitalfrontier"))
                    .icon(() -> new ItemStack(ModItems.BINARY_1))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.BINARY_0);
                        entries.add(ModItems.BINARY_1);
                        entries.add(ModItems.PIXEL_DUST);
                        entries.add(ModItems.LUMINESSENCE);
                        entries.add(ModItems.LUMINANCE_ORE_DETECTOR);
                        entries.add(ModItems.BYTE);

                        entries.add(ModBlocks.PIXEL_BLOCK);
                        entries.add(ModBlocks.GRIDSTONE_BLOCK);
                        entries.add(ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                        entries.add(ModBlocks.LUMINANCE_ORE);
                    })
                    .build());

    public static void registerItemGroups() {
        DigitalFrontier.LOGGER.info("Registering Item Groups for " + DigitalFrontier.MOD_ID);
    }
}
