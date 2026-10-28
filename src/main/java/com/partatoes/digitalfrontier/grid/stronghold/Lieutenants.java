package com.partatoes.digitalfrontier.grid.stronghold;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
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

    public record Lieutenant(Identifier id, LieutenantDef def, CompiledTrigger respawn) {
    }

    private static volatile Map<Identifier, Lieutenant> loaded = Map.of();

    @Nullable
    public static Lieutenant get(Identifier id) {
        return loaded.get(id);
    }

    /** Lieutenants a grid tuned to this corruption setting can have, sorted by id so weighted picks are stable. */
    public static List<Lieutenant> eligible(float gridCorruption) {
        return loaded.values().stream()
                .filter(lieutenant -> gridCorruption >= lieutenant.def().minGridCorruption()
                        && gridCorruption <= lieutenant.def().maxGridCorruption())
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of(DigitalFrontier.MOD_ID, "grid_lieutenants");
    }

    @Override
    public void reload(ResourceManager manager) {
        Map<Identifier, Lieutenant> lieutenants = new HashMap<>();
        manager.findResources(FOLDER, path -> path.getPath().endsWith(".json")).forEach((file, resource) -> {
            Identifier id = file.withPath(path -> path.substring(FOLDER.length() + 1, path.length() - ".json".length()));
            try (Reader reader = resource.getReader()) {
                LieutenantDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                        .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Invalid grid lieutenant {}: {}", id, error))
                        .ifPresent(def -> lieutenants.put(id, new Lieutenant(id, def, new CompiledTrigger(def.respawn().trigger()))));
            } catch (Exception e) {
                DigitalFrontier.LOGGER.error("Failed to read grid lieutenant {}", id, e);
            }
        });
        loaded = Map.copyOf(lieutenants);
        DigitalFrontier.LOGGER.info("Loaded {} grid lieutenants", lieutenants.size());
    }
}
