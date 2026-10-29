package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ModEntityLootProvider extends SimpleFabricLootTableProvider {
    // Named by the_sovereign.json's "loot_table"; any lieutenant definition can name a table like this
    public static final ResourceKey<LootTable> SOVEREIGN_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "entities/lieutenant/the_sovereign"));

    public ModEntityLootProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup, LootContextParamSets.ENTITY);
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> exporter) {
        // Lieutenants without a loot_table of their own: always a way out, sometimes a way back in
        exporter.accept(ModEntities.LIEUTENANT.getDefaultLootTable().orElseThrow(), lieutenantLoot(ModItems.MEMORY_PAGING));
        // The ruler guards something better
        exporter.accept(SOVEREIGN_LOOT, lieutenantLoot(ModItems.CORE_DUMP));
    }

    private static LootTable.Builder lieutenantLoot(Item rareDrop) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(ModItems.ESCAPE_KEY)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.25f))
                        .add(LootItem.lootTableItem(rareDrop)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(ModItems.LUMINESSENCE)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5)))));
    }
}
