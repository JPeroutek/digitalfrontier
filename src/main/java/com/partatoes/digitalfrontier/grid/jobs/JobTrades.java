package com.partatoes.digitalfrontier.grid.jobs;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.io.Reader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What each job trades, from {@code data/<namespace>/grid/job/<job>.json}, reloaded with datapacks. A datapack can
 * replace a job's offers by using the same file path:
 * <pre>{"offers": [{"buy": {"item": "digitalfrontier:luminessence", "count": 4}, "sell": {"item": "...", "count": 1}, "max_uses": 8}]}</pre>
 */
public final class JobTrades implements SimpleSynchronousResourceReloadListener {
    private static final String FOLDER = "grid/job";

    /** One kind of item, so many of them. */
    public record Stack(Item item, int count) {
        public static final Codec<Stack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Stack::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Stack::count)
        ).apply(instance, Stack::new));
    }

    /**
     * @param buy     what the player pays, before the job program's pricing (see {@link Pricing})
     * @param buyB    an optional second payment, not repriced
     * @param maxUses trades before it's out of stock until the next day
     */
    public record Offer(Stack buy, Optional<Stack> buyB, Stack sell, int maxUses) {
        public static final Codec<Offer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Stack.CODEC.fieldOf("buy").forGetter(Offer::buy),
                Stack.CODEC.optionalFieldOf("buy_b").forGetter(Offer::buyB),
                Stack.CODEC.fieldOf("sell").forGetter(Offer::sell),
                Codec.intRange(1, 1000).optionalFieldOf("max_uses", 8).forGetter(Offer::maxUses)
        ).apply(instance, Offer::new));

        /** As a vanilla offer, priced for one player, with {@code uses} already made today. */
        public MerchantOffer toMerchantOffer(float priceMultiplier, int uses) {
            MerchantOffer offer = new MerchantOffer(new ItemCost(buy.item(), Pricing.price(buy.count(), priceMultiplier)),
                    buyB.map(b -> new ItemCost(b.item(), b.count())), new ItemStack(sell.item(), sell.count()), uses, maxUses, 0, 0);
            return offer;
        }
    }

    public record Trades(List<Offer> offers) {
        public static final Codec<Trades> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Offer.CODEC.listOf().fieldOf("offers").forGetter(Trades::offers)
        ).apply(instance, Trades::new));
    }

    private static volatile Map<ProgramJob, List<Offer>> loaded = Map.of();

    public static List<Offer> offers(ProgramJob job) {
        return loaded.getOrDefault(job, List.of());
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid_job_trades");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<ProgramJob, List<Offer>> trades = new EnumMap<>(ProgramJob.class);
        manager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).forEach((file, resource) -> {
            String name = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".json".length());
            ProgramJob job = ProgramJob.byName(name);
            if (job == null) {
                DigitalFrontier.LOGGER.error("Grid job trades for unknown job {}", file);
                return;
            }
            try (Reader reader = resource.openAsReader()) {
                Trades.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                        .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Invalid grid job trades {}: {}", file, error))
                        .ifPresent(parsed -> trades.put(job, List.copyOf(parsed.offers())));
            } catch (Exception e) {
                DigitalFrontier.LOGGER.error("Failed to read grid job trades {}", file, e);
            }
        });
        loaded = Map.copyOf(trades);
    }
}
