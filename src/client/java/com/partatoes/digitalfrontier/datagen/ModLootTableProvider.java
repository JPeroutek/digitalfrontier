package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
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
        dropSelf(ModBlocks.DISINFECTION_PAD);
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

        // Blocks that support silk and fortune. Luminance Ore also gives up a few loose bits, the real world's only
        // source of them (the laser into the grid is built from them)
        add(ModBlocks.LUMINANCE_ORE, createOreDrop(ModBlocks.LUMINANCE_ORE, ModItems.LUMINESSENCE)
                .withPool(bits(ContextIntProviders.between(0, 2)).when(doesNotHaveSilkTouch())));
        add(ModBlocks.PIXEL_BLOCK, createOreDrop(ModBlocks.PIXEL_BLOCK, ModItems.PIXEL_DUST));

        // Static, the stuff of decayed ground, breaks down into a loose bit
        add(ModBlocks.STATIC_BLOCK, LootTable.lootTable().withPool(bits(ContextIntProviders.exactly(1))));
//        addDrop(ModBlocks.LUMINANCE_ORE,
//                SilkAndFortuneDrops(ModBlocks.LUMINANCE_ORE, ModItems.LUMINESSENCE, 4.0f, 5.0f));
//        addDrop(ModBlocks.PIXEL_BLOCK,
//                SilkAndFortuneDrops(ModBlocks.PIXEL_BLOCK, ModItems.PIXEL_DUST, 3.0f, 6.0f));
    }

    /** A pool of loose bits: each roll is a Binary 0 or a Binary 1, evenly. */
    static LootPool.Builder bits(Holder<ContextIntProvider> rolls) {
        return LootPool.lootPool()
                .setRolls(rolls)
                .add(LootItem.lootTableItem(ModItems.BINARY_0))
                .add(LootItem.lootTableItem(ModItems.BINARY_1));
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
