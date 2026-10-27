package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ModEntityLootProvider extends SimpleFabricLootTableProvider {
    // Named by the_sovereign.json's "loot_table"; any lieutenant definition can name a table like this
    public static final RegistryKey<LootTable> SOVEREIGN_LOOT = RegistryKey.of(RegistryKeys.LOOT_TABLE,
            Identifier.of(DigitalFrontier.MOD_ID, "entities/lieutenant/the_sovereign"));

    public ModEntityLootProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup, LootContextTypes.ENTITY);
    }

    @Override
    public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> exporter) {
        // Lieutenants without a loot_table of their own: always a way out, sometimes a way back in
        exporter.accept(ModEntities.LIEUTENANT.getLootTableKey().orElseThrow(), lieutenantLoot(ModItems.MEMORY_PAGING));
        // The ruler guards something better
        exporter.accept(SOVEREIGN_LOOT, lieutenantLoot(ModItems.CORE_DUMP));
    }

    private static LootTable.Builder lieutenantLoot(Item rareDrop) {
        return LootTable.builder()
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(ModItems.ESCAPE_KEY)))
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.25f))
                        .with(ItemEntry.builder(rareDrop)))
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(ModItems.LUMINESSENCE)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 5)))));
    }
}
