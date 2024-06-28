package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {
    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        // Self-dropping blocks
        addDrop(ModBlocks.CRACKED_GRIDSTONE_BLOCK);
        addDrop(ModBlocks.GRIDSTONE_BRICKS_BLOCK);
        addDrop(ModBlocks.LIME_GRIDSTONE_BLOCK);
        addDrop(ModBlocks.BLUE_GRIDSTONE_BLOCK);
        addDrop(ModBlocks.RED_GRIDSTONE_BLOCK);
        addDrop(ModBlocks.WHITE_GRIDSTONE_BLOCK);
        addDrop(ModBlocks.ORANGE_GRIDSTONE_BLOCK);
        addDrop(ModBlocks.PIXEL_LAMP);
        addDrop(ModBlocks.BOOSTER_BLOCK);
        addDrop(ModBlocks.KEYBOARD_BLOCK);

        addDrop(ModBlocks.GRIDSTONE_STAIRS);
        addDrop(ModBlocks.GRIDSTONE_WALL);
        addDrop(ModBlocks.GRIDSTONE_PRESSURE_PLATE);
        addDrop(ModBlocks.GRIDSTONE_SLAB, slabDrops(ModBlocks.GRIDSTONE_SLAB));

        addDrop(ModBlocks.CRACKED_GRIDSTONE_STAIRS);
        addDrop(ModBlocks.CRACKED_GRIDSTONE_WALL);
        addDrop(ModBlocks.CRACKED_GRIDSTONE_SLAB, slabDrops(ModBlocks.CRACKED_GRIDSTONE_SLAB));

        // Blocks that drop others if not using silk
        addDrop(ModBlocks.GRIDSTONE_BLOCK, drops(ModBlocks.GRIDSTONE_BLOCK, ModBlocks.CRACKED_GRIDSTONE_BLOCK));

        // Blocks that support silk and fortune
        addDrop(ModBlocks.LUMINANCE_ORE,
                SilkAndFortuneDrops(ModBlocks.LUMINANCE_ORE, ModItems.LUMINESSENCE, 4.0f, 5.0f));
        addDrop(ModBlocks.PIXEL_BLOCK,
                SilkAndFortuneDrops(ModBlocks.PIXEL_BLOCK, ModItems.PIXEL_DUST, 3.0f, 6.0f));
    }

    public LootTable.Builder SilkAndFortuneDrops(Block silkTouchDrop, Item regularAndFortuneDrop, float minDrop, float maxDrop) {
//        return BlockLootTableGenerator.dropsWithSilkTouch(
//                silkTouchDrop,
//                (LootPoolEntry.Builder)
//                    this.applyExplosionDecay(
//                            silkTouchDrop,
//                        ((LeafEntry.Builder)
//                            ItemEntry.builder(regularAndFortuneDrop)
//                                     .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(minDrop, maxDrop))))
//                                     .apply(ApplyBonusLootFunction.uniformBonusCount(Enchantments.FORTUNE))));
        RegistryWrapper.Impl<Enchantment> enchantmentsWrapper = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        return this.dropsWithSilkTouch(
                silkTouchDrop,
                (LootPoolEntry.Builder) this.applyExplosionDecay(
                        silkTouchDrop,
                        ((LeafEntry.Builder) ItemEntry.builder(regularAndFortuneDrop)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(minDrop, maxDrop))))
                                .apply(ApplyBonusLootFunction.oreDrops(enchantmentsWrapper.getOrThrow(Enchantments.FORTUNE)))));

    }
}
