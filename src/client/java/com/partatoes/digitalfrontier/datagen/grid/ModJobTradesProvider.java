package com.partatoes.digitalfrontier.datagen.grid;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.jobs.JobTrades;
import com.partatoes.digitalfrontier.grid.jobs.ProgramJob;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** The shipped job trades, {@code data/digitalfrontier/grid/job/<job>.json}; all priced in Luminessence. */
public class ModJobTradesProvider implements DataProvider {
    private final PackOutput.PathProvider jobs;

    public ModJobTradesProvider(FabricPackOutput output) {
        this.jobs = output.createPathProvider(PackOutput.Target.DATA_PACK, "grid/job");
    }

    static Map<ProgramJob, List<JobTrades.Offer>> trades() {
        Map<ProgramJob, List<JobTrades.Offer>> trades = new EnumMap<>(ProgramJob.class);
        // The garage: what a rider needs, a baton above all
        trades.put(ProgramJob.MECHANIC, List.of(
                offer(4, ModBlocks.BOOSTER_BLOCK, 4, 8),
                offer(6, ModBlocks.LIGHT_BARRIER_EMITTER, 2, 8),
                offer(2, ModItems.COAL_DUST, 8, 12),
                offer(16, ModItems.LIGHTCYCLE_BATON, 1, 2)));
        // The way out, one a day
        trades.put(ProgramJob.PORTAL_KEEPER, List.of(
                offer(32, ModItems.ESCAPE_KEY, 1, 1)));
        return trades;
    }

    private static JobTrades.Offer offer(int luminessence, ItemLike sold, int count, int maxUses) {
        Item item = sold.asItem();
        return new JobTrades.Offer(new JobTrades.Stack(ModItems.LUMINESSENCE, luminessence), Optional.empty(),
                new JobTrades.Stack(item, count), maxUses);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput writer) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        trades().forEach((job, offers) -> {
            JsonElement json = JobTrades.Trades.CODEC.encodeStart(JsonOps.INSTANCE, new JobTrades.Trades(offers)).getOrThrow();
            futures.add(DataProvider.saveStable(writer, json,
                    jobs.json(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, job.getSerializedName()))));
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Grid job trades";
    }
}
