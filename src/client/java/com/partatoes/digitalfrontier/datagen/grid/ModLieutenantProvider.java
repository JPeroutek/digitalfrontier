package com.partatoes.digitalfrontier.datagen.grid;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.datagen.ModEntityLootProvider;
import com.partatoes.digitalfrontier.grid.stronghold.LieutenantDef;
import com.partatoes.digitalfrontier.grid.stronghold.RegistryMatcher;
import com.partatoes.digitalfrontier.grid.stronghold.Trigger;
import com.partatoes.digitalfrontier.grid.stronghold.Triggers;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The lieutenants the mod ships with (see docs/lieutenants.md). Their names are translation keys, so a resource
 * pack can rename them; a datapack can replace a whole file to change anything else.
 */
public class ModLieutenantProvider implements DataProvider {
    private static final long DAY = 24000;

    private final DataOutput.PathResolver lieutenants;

    public ModLieutenantProvider(FabricDataOutput output) {
        this.lieutenants = output.getResolver(DataOutput.OutputType.DATA_PACK, "grid/lieutenant");
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        definitions().forEach((name, def) -> {
            JsonElement json = LieutenantDef.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow();
            futures.add(DataProvider.writeToPath(writer, json, lieutenants.resolveJson(Identifier.of(DigitalFrontier.MOD_ID, name))));
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static Map<String, LieutenantDef> definitions() {
        Map<String, LieutenantDef> defs = new LinkedHashMap<>();

        // The ruler returns on its own after a week, or sooner if the grid feeds the regime
        defs.put("the_sovereign", new LieutenantDef(
                Text.translatable("lieutenant.digitalfrontier.the_sovereign"),
                LieutenantDef.Archetype.RULER, Optional.empty(), Optional.empty(), 10, 0f, 1f,
                Optional.of(ModEntityLootProvider.SOVEREIGN_LOOT),
                new LieutenantDef.Respawn(DAY, -1, anyOf(
                        new Triggers.Time(7 * DAY),
                        new Triggers.Count(3, new Triggers.PlayerDerezzed(-1)),
                        new Triggers.Count(5, new Triggers.ProgramDerezzed(-1)),
                        new Triggers.ItemUsed(Optional.of(matcher("minecraft:echo_shard")), 16, true),
                        new Triggers.ItemCrafted(Optional.of(matcher("digitalfrontier:grid_tuning_laser"))),
                        new Triggers.Redstone(32),
                        new Triggers.Chance(0.5f, new Triggers.LieutenantDefeated(Optional.empty(), true)),
                        new Triggers.AllOf(List.of(new Triggers.Thunder(), new Triggers.Time(2 * DAY))),
                        // Once the whole regime has fallen, the ruler is quickest to rebuild it
                        new Triggers.AllOf(List.of(new Triggers.AllDefeated(), new Triggers.Time(3 * DAY))),
                        new Triggers.Function(Optional.empty())))));

        // The duelist waits for its stronghold to be deserted, then for someone to come back, or for fresh blood
        defs.put("the_warden", new LieutenantDef(
                Text.translatable("lieutenant.digitalfrontier.the_warden"),
                LieutenantDef.Archetype.DUELIST, Optional.empty(), Optional.empty(), 10, 0f, 1f, Optional.empty(),
                new LieutenantDef.Respawn(DAY / 2, -1, anyOf(
                        new Triggers.StrongholdEntered(DAY),
                        new Triggers.GridEntered(true),
                        new Triggers.Chance(0.25f, new Triggers.BlockBroken(Optional.of(matcher("#digitalfrontier:grid_block")), 32)),
                        new Triggers.Count(16, new Triggers.BlockPlaced(Optional.empty(), 32)),
                        new Triggers.AllOf(List.of(new Triggers.MoonPhase(List.of(0)), new Triggers.Time(DAY))),
                        new Triggers.Count(2, new Triggers.MemoryPaged()),
                        new Triggers.Count(3, new Triggers.PortalUsed()),
                        new Triggers.Function(Optional.of("reboot_warden"))))));
        return defs;
    }

    private static Trigger anyOf(Trigger... triggers) {
        return new Triggers.AnyOf(List.of(triggers));
    }

    private static RegistryMatcher matcher(String text) {
        boolean tag = text.startsWith("#");
        return new RegistryMatcher(tag, Identifier.of(tag ? text.substring(1) : text));
    }

    @Override
    public String getName() {
        return "Grid lieutenants";
    }
}
