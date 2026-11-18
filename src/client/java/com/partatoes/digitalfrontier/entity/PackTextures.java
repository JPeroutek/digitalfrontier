package com.partatoes.digitalfrontier.entity;

import com.partatoes.digitalfrontier.DigitalFrontier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Textures named by data rather than shipped with the mod, such as a lore pack's lieutenant skins. A datapack on the
 * server can name a texture whose resource pack this client doesn't have; drawing it would show the missing-texture
 * checkerboard, so it's swapped for a fallback instead. Whether each texture exists is remembered until the resource
 * packs reload.
 */
public final class PackTextures {
    private static final Map<Identifier, Boolean> EXISTS = new ConcurrentHashMap<>();

    private PackTextures() {
    }

    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "pack_textures"),
                (ResourceManagerReloadListener) manager -> EXISTS.clear());
    }

    /** The texture if the client's resource packs have it, otherwise the fallback. */
    public static Identifier orFallback(Identifier texture, Identifier fallback) {
        boolean exists = EXISTS.computeIfAbsent(texture,
                id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent());
        return exists ? texture : fallback;
    }
}
