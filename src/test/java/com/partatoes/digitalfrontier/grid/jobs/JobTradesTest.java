package com.partatoes.digitalfrontier.grid.jobs;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobTradesTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void offersParseWithDefaults() {
        JobTrades.Trades trades = JobTrades.Trades.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"offers": [
                  {"buy": {"item": "minecraft:emerald", "count": 4}, "sell": {"item": "minecraft:diamond"}},
                  {"buy": {"item": "minecraft:emerald"}, "buy_b": {"item": "minecraft:book"}, "sell": {"item": "minecraft:stone", "count": 8}, "max_uses": 2}
                ]}""")).getOrThrow();
        JobTrades.Offer first = trades.offers().getFirst();
        assertEquals(Items.EMERALD, first.buy().item());
        assertEquals(1, first.sell().count(), "count defaults to 1");
        assertEquals(8, first.maxUses(), "max_uses defaults to 8");
        assertTrue(trades.offers().get(1).buyB().isPresent());
        assertTrue(JobTrades.Trades.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"offers\": [{\"buy\": {\"item\": \"minecraft:emerald\", \"count\": 99}, \"sell\": {\"item\": \"minecraft:stone\"}}]}")).isError());
    }
}
