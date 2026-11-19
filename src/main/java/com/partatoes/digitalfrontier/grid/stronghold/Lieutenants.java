package com.partatoes.digitalfrontier.grid.stronghold;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The loaded lieutenant definitions ({@code grid/lieutenant/*.json}), reloaded with datapacks. A datapack can
 * replace a shipped lieutenant by using the same file path, e.g. to give it a lore name.
 */
public final class Lieutenants implements SimpleSynchronousResourceReloadListener {
    private static final String FOLDER = "grid/lieutenant";

    /** A loaded definition with its triggers compiled; {@code restore} is null for one that can't be redeemed. */
    public record Lieutenant(Identifier id, LieutenantDef def, CompiledTrigger respawn, @Nullable CompiledTrigger restore) {
    }

    private static volatile Map<Identifier, Lieutenant> loaded = Map.of();

    @Nullable
    public static Lieutenant get(Identifier id) {
        return loaded.get(id);
    }

    /** Lieutenants a grid of these settings can have, sorted by id so weighted picks are stable. */
    public static List<Lieutenant> eligible(GridLayoutSettings grid) {
        return loaded.values().stream()
                .filter(lieutenant -> grid.corruption() >= lieutenant.def().minGridCorruption()
                        && grid.corruption() <= lieutenant.def().maxGridCorruption()
                        && grid.regime() >= lieutenant.def().minGridRegime()
                        && grid.regime() <= lieutenant.def().maxGridRegime())
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid_lieutenants");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<Identifier, Lieutenant> lieutenants = new HashMap<>();
        manager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).forEach((file, resource) -> {
            Identifier id = file.withPath(path -> path.substring(FOLDER.length() + 1, path.length() - ".json".length()));
            try (Reader reader = resource.openAsReader()) {
                LieutenantDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                        .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Invalid grid lieutenant {}: {}", id, error))
                        .ifPresent(def -> lieutenants.put(id, new Lieutenant(id, def, new CompiledTrigger(def.respawn().trigger()),
                                def.restore().map(CompiledTrigger::new).orElse(null))));
            } catch (Exception e) {
                DigitalFrontier.LOGGER.error("Failed to read grid lieutenant {}", id, e);
            }
        });
        loaded = Map.copyOf(lieutenants);
        DigitalFrontier.LOGGER.info("Loaded {} grid lieutenants", lieutenants.size());
    }
}
