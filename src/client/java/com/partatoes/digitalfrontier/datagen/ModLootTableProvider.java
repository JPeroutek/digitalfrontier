package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootSubProvider {
    public ModLootTableProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        // Self-dropping blocks
        dropSelf(ModBlocks.CRACKED_GRIDSTONE_BLOCK);
        dropSelf(ModBlocks.GRIDSTONE_BRICKS_BLOCK);
        dropSelf(ModBlocks.PIXEL_LAMP);
        dropSelf(ModBlocks.BOOSTER_BLOCK);
        dropSelf(ModBlocks.KEYBOARD_BLOCK);

        dropSelf(ModBlocks.GRIDSTONE_STAIRS);
        dropSelf(ModBlocks.GRIDSTONE_WALL);
        dropSelf(ModBlocks.GRIDSTONE_PRESSURE_PLATE);
        add(ModBlocks.GRIDSTONE_SLAB, createSlabItemTable(ModBlocks.GRIDSTONE_SLAB));

        dropSelf(ModBlocks.CRACKED_GRIDSTONE_STAIRS);
        dropSelf(ModBlocks.CRACKED_GRIDSTONE_WALL);
        add(ModBlocks.CRACKED_GRIDSTONE_SLAB, createSlabItemTable(ModBlocks.CRACKED_GRIDSTONE_SLAB));

        dropSelf(ModBlocks.GRIDSTONE_BRICKS_STAIRS);
        dropSelf(ModBlocks.GRIDSTONE_BRICKS_WALL);
        add(ModBlocks.GRIDSTONE_BRICKS_SLAB, createSlabItemTable(ModBlocks.GRIDSTONE_BRICKS_SLAB));

        dropSelf(ModBlocks.LIGHT_BARRIER_EMITTER);
        dropSelf(ModBlocks.ARRIVAL_PAD);
        dropSelf(ModBlocks.DIGITIZING_LASER);
        dropSelf(ModBlocks.GRID_TUNING_LASER);
        dropSelf(ModBlocks.STRONGHOLD_UPLINK);

        ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.values()
                .forEach(colorMapping -> colorMapping.values().forEach(this::dropSelf));

        // Blocks that drop others if not using silk
        add(ModBlocks.GRIDSTONE_BLOCK, createSingleItemTableWithSilkTouch(ModBlocks.GRIDSTONE_BLOCK, ModBlocks.CRACKED_GRIDSTONE_BLOCK));

        // Blocks that support silk and fortune
        createOreDrop(ModBlocks.LUMINANCE_ORE, ModItems.LUMINESSENCE);
        createOreDrop(ModBlocks.PIXEL_BLOCK, ModItems.PIXEL_DUST);
//        addDrop(ModBlocks.LUMINANCE_ORE,
//                SilkAndFortuneDrops(ModBlocks.LUMINANCE_ORE, ModItems.LUMINESSENCE, 4.0f, 5.0f));
//        addDrop(ModBlocks.PIXEL_BLOCK,
//                SilkAndFortuneDrops(ModBlocks.PIXEL_BLOCK, ModItems.PIXEL_DUST, 3.0f, 6.0f));
    }

//    public LootTable.Builder SilkAndFortuneDrops(Block silkTouchDrop, Item regularAndFortuneDrop, float minDrop, float maxDrop) {
////        return BlockLootTableGenerator.dropsWithSilkTouch(
////                silkTouchDrop,
////                (LootPoolEntry.Builder)
////                    this.applyExplosionDecay(
////                            silkTouchDrop,
////                        ((LeafEntry.Builder)
////                            ItemEntry.builder(regularAndFortuneDrop)
////                                     .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(minDrop, maxDrop))))
////                                     .apply(ApplyBonusLootFunction.uniformBonusCount(Enchantments.FORTUNE))));
//        RegistryWrapper.Impl<Enchantment> enchantmentsWrapper = this.registries.getOrThrow(RegistryKeys.ENCHANTMENT);
//        return this.dropsWithSilkTouch(
//                silkTouchDrop,
//                (LootPoolEntry.Builder) this.applyExplosionDecay(
//                        silkTouchDrop,
//                        ((LeafEntry.Builder) ItemEntry.builder(regularAndFortuneDrop)
//                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(minDrop, maxDrop))))
//                                .apply(ApplyBonusLootFunction.oreDrops(enchantmentsWrapper.getOrThrow(Enchantments.FORTUNE)))));
//
//    }
}
