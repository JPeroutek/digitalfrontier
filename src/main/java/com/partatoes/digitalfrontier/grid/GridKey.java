package com.partatoes.digitalfrontier.grid;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/**
 * Derives a grid's dimension id from its settings: {@code digitalfrontier:default} for the default settings,
 * otherwise {@code digitalfrontier:0x} plus the first 8 hex digits of a SHA-256 over the canonical settings JSON.
 */
public final class GridKey {
    public static final String DEFAULT_PATH = "default";

    private GridKey() {
    }

    public static Identifier of(GridLayoutSettings settings) {
        if (settings.equals(GridLayoutSettings.DEFAULT)) {
            return Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, DEFAULT_PATH);
        }
        JsonElement json = GridLayoutSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();
        String hash = Hashing.sha256().hashString(canonicalize(json).toString(), StandardCharsets.UTF_8).toString();
        return Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "0x" + hash.substring(0, 8));
    }

    // Sort object keys so the hash doesn't depend on field encoding order
    private static JsonElement canonicalize(JsonElement element) {
        if (element.isJsonObject()) {
            TreeMap<String, JsonElement> sorted = new TreeMap<>();
            element.getAsJsonObject().entrySet().forEach(entry -> sorted.put(entry.getKey(), canonicalize(entry.getValue())));
            JsonObject result = new JsonObject();
            sorted.forEach(result::add);
            return result;
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            element.getAsJsonArray().forEach(child -> result.add(canonicalize(child)));
            return result;
        }
        return element;
    }
}
