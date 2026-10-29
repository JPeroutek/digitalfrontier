package com.partatoes.digitalfrontier.grid.stronghold;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TriggerTest {
    // Text components (lieutenant names) need the vanilla registries
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Trigger parse(String json) {
        return Trigger.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void everyTriggerTypeRoundTrips() {
        String json = """
                {"type": "any_of", "triggers": [
                  {"type": "time", "ticks": 100},
                  {"type": "overworld_moon_phase", "phases": [0, 4]},
                  {"type": "overworld_thunder"},
                  {"type": "scoreboard", "objective": "regime", "holder": "#power", "min": 3},
                  {"type": "all_lieutenants_defeated"},
                  {"type": "grid_entered", "new_players_only": true},
                  {"type": "stronghold_entered", "after_absence": 600},
                  {"type": "item_crafted", "item": "minecraft:diamond"},
                  {"type": "item_used_at_stronghold", "item": "#minecraft:logs", "radius": 8, "consume": false},
                  {"type": "block_placed", "block": "minecraft:stone", "radius": -1},
                  {"type": "block_broken"},
                  {"type": "redstone", "radius": 16},
                  {"type": "lieutenant_defeated", "lieutenant": "digitalfrontier:the_warden", "others_only": false},
                  {"type": "player_derezzed"},
                  {"type": "portal_used"},
                  {"type": "program_derezzed", "radius": 64},
                  {"type": "memory_paged"},
                  {"type": "function", "signal": "reboot"},
                  {"type": "all_of", "triggers": [{"type": "not", "trigger": {"type": "overworld_thunder"}}]},
                  {"type": "chance", "chance": 0.5, "trigger": {"type": "portal_used"}},
                  {"type": "count", "count": 3, "trigger": {"type": "player_derezzed"}}
                ]}""";
        Trigger trigger = parse(json);
        JsonElement encoded = Trigger.CODEC.encodeStart(JsonOps.INSTANCE, trigger).getOrThrow();
        assertEquals(trigger, parse(encoded.toString()));

        Set<String> used = new HashSet<>();
        collectTypes(JsonParser.parseString(json), used);
        assertEquals(Triggers.types(), used, "the example should cover every trigger type");
    }

    private static void collectTypes(JsonElement json, Set<String> types) {
        if (json.isJsonObject()) {
            if (json.getAsJsonObject().has("type")) {
                types.add(json.getAsJsonObject().get("type").getAsString());
            }
            json.getAsJsonObject().entrySet().forEach(entry -> collectTypes(entry.getValue(), types));
        } else if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(child -> collectTypes(child, types));
        }
    }

    @Test
    void badTriggersAreRejected() {
        assertTrue(Trigger.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"type\": \"nonsense\"}")).isError());
        assertTrue(Trigger.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"type\": \"count\", \"count\": 2, \"trigger\": {\"type\": \"time\", \"ticks\": 5}}")).isError(),
                "count needs an event trigger");
        assertTrue(Trigger.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"type\": \"chance\", \"chance\": 2, \"trigger\": {\"type\": \"portal_used\"}}")).isError());
    }

    @Test
    void identicalLeavesKeepSeparateCounters() {
        CompiledTrigger compiled = new CompiledTrigger(parse(
                "{\"type\": \"all_of\", \"triggers\": [{\"type\": \"portal_used\"}, {\"type\": \"portal_used\"}]}"));
        assertEquals(2, compiled.leaves().size());
        assertEquals(0, compiled.index(compiled.leaves().get(0)));
        assertEquals(1, compiled.index(compiled.leaves().get(1)));
    }

    @Test
    void chancesMultiplyThroughNesting() {
        CompiledTrigger compiled = new CompiledTrigger(parse("""
                {"type": "any_of", "triggers": [
                  {"type": "chance", "chance": 0.5, "trigger": {"type": "chance", "chance": 0.5, "trigger": {"type": "portal_used"}}},
                  {"type": "memory_paged"}
                ]}"""));
        assertEquals(0.25, compiled.chance(compiled.leaves().get(0)), 1e-6);
        assertEquals(1.0, compiled.chance(compiled.leaves().get(1)), 1e-6);
    }

    @Test
    void combinatorsEvaluateTheirCounters() {
        CompiledTrigger compiled = new CompiledTrigger(parse("""
                {"type": "any_of", "triggers": [
                  {"type": "count", "count": 3, "trigger": {"type": "player_derezzed"}},
                  {"type": "all_of", "triggers": [
                    {"type": "portal_used"},
                    {"type": "not", "trigger": {"type": "memory_paged"}}
                  ]}
                ]}"""));
        FakeContext context = new FakeContext(compiled);
        Trigger derezzed = compiled.leaves().get(0);
        Trigger portal = compiled.leaves().get(1);
        Trigger paged = compiled.leaves().get(2);

        assertFalse(compiled.root().test(context));
        context.counts.put(derezzed, 2);
        assertFalse(compiled.root().test(context), "two of three deaths");
        context.counts.put(derezzed, 3);
        assertTrue(compiled.root().test(context), "three deaths");

        context.counts.clear();
        context.counts.put(portal, 1);
        assertTrue(compiled.root().test(context), "portal used, nobody paged");
        context.counts.put(paged, 1);
        assertFalse(compiled.root().test(context), "not memory_paged fails once someone paged");
    }

    @Test
    void eventTriggersMatchTheirEvents() {
        FakeContext context = new FakeContext(new CompiledTrigger(new Triggers.PortalUsed()));
        assertTrue(new Triggers.Function(Optional.of("reboot")).matches(new GridEvent.Function("reboot"), context));
        assertFalse(new Triggers.Function(Optional.of("reboot")).matches(new GridEvent.Function("other"), context));
        assertTrue(new Triggers.Function(Optional.empty()).matches(new GridEvent.Function(null), context));
        assertTrue(new Triggers.GridEntered(true).matches(new GridEvent.GridEntered(true), context));
        assertFalse(new Triggers.GridEntered(true).matches(new GridEvent.GridEntered(false), context));
        assertTrue(new Triggers.StrongholdEntered(100).matches(new GridEvent.StrongholdEntered(150), context));
        assertFalse(new Triggers.StrongholdEntered(100).matches(new GridEvent.StrongholdEntered(50), context));
        // Radius is measured from the stronghold center (0, 65, 0) in this fake context
        assertTrue(new Triggers.Redstone(10).matches(new GridEvent.Redstone(new BlockPos(5, 65, 5)), context));
        assertFalse(new Triggers.Redstone(10).matches(new GridEvent.Redstone(new BlockPos(20, 65, 0)), context));
        assertTrue(new Triggers.Redstone(-1).matches(new GridEvent.Redstone(new BlockPos(2000, 65, 0)), context));
        // "Others" means other strongholds: two strongholds can have the same lieutenant
        Identifier sovereign = context.lieutenant();
        Identifier warden = Identifier.fromNamespaceAndPath("digitalfrontier", "the_warden");
        Triggers.LieutenantDefeated others = new Triggers.LieutenantDefeated(Optional.empty(), true);
        assertFalse(others.matches(new GridEvent.LieutenantDefeated(sovereign, context.stronghold()), context), "this stronghold");
        assertTrue(others.matches(new GridEvent.LieutenantDefeated(sovereign, "stronghold_1"), context), "same lieutenant elsewhere");
        assertTrue(others.matches(new GridEvent.LieutenantDefeated(warden, "stronghold_1"), context));
        Triggers.LieutenantDefeated onlyWarden = new Triggers.LieutenantDefeated(Optional.of(warden), false);
        assertTrue(onlyWarden.matches(new GridEvent.LieutenantDefeated(warden, "stronghold_1"), context));
        assertFalse(onlyWarden.matches(new GridEvent.LieutenantDefeated(sovereign, "stronghold_1"), context));
        assertFalse(new Triggers.PortalUsed().matches(new GridEvent.MemoryPaged(), context));
    }

    // Counters only; world-dependent states aren't evaluated here
    private static final class FakeContext implements TriggerContext {
        final CompiledTrigger compiled;
        final Map<Trigger, Integer> counts = new HashMap<>();

        FakeContext(CompiledTrigger compiled) {
            this.compiled = compiled;
        }

        public ServerLevel grid() {
            throw new UnsupportedOperationException();
        }

        public BlockPos center() {
            return new BlockPos(0, 65, 0);
        }

        public Identifier lieutenant() {
            return Identifier.fromNamespaceAndPath("digitalfrontier", "the_sovereign");
        }

        public String stronghold() {
            return "stronghold_0";
        }

        public long defeatedAt() {
            return 0;
        }

        public boolean allDefeated() {
            return false;
        }

        public int count(Trigger leaf) {
            // Leaves are compared by identity, as in the real context
            return counts.entrySet().stream().filter(entry -> entry.getKey() == leaf).mapToInt(Map.Entry::getValue).findFirst().orElse(0);
        }

        public double chance(Trigger leaf) {
            return compiled.chance(leaf);
        }
    }

    @Test
    void shippedStyleDefinitionParses() {
        String json = """
                {"name": "Clu", "archetype": "ruler", "texture": "digitalfrontier:textures/entity/clu64.png",
                 "respawn": {"min_delay": 24000, "max_respawns": 3, "trigger": {"type": "time", "ticks": 168000}}}""";
        LieutenantDef def = LieutenantDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertEquals("Clu", def.name().getString());
        assertEquals(LieutenantDef.Archetype.RULER, def.archetype());
        assertEquals(300, def.healthOrDefault());
        assertEquals(3, def.respawn().maxRespawns());
        assertTrue(def.lootTable().isEmpty(), "no loot_table: the entity type's default table");
    }

    @Test
    void definitionsCanNameALootTable() {
        String json = """
                {"name": "Rinzler", "archetype": "duelist", "loot_table": "mypack:entities/rinzler",
                 "respawn": {"trigger": {"type": "function"}}}""";
        LieutenantDef def = LieutenantDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertEquals(Identifier.fromNamespaceAndPath("mypack", "entities/rinzler"), def.lootTable().orElseThrow().identifier());
    }
}
