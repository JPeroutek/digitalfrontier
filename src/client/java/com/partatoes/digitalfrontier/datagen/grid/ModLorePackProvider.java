package com.partatoes.digitalfrontier.datagen.grid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.stronghold.LieutenantDef;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The built-in lore pack (resourcepacks/lore, off by default): the films' names for the mod's characters, and the
 * project's character skins for them. The data half is the shipped definitions with only their skins changed, so
 * it can't drift from them; the assets half renames them through the same translation keys the defaults use.
 * The skins themselves are hand-made files under src/main/resources/resourcepacks/lore.
 */
public class ModLorePackProvider implements DataProvider {
    // Shipped lieutenant → its lore skin, under textures/entity/lore
    private static final Map<String, String> LIEUTENANT_SKINS = Map.of(
            "the_sovereign", "clu64",
            "the_warden", "rinzler64");
    // Shipped lieutenant → its lore skin once redeemed: the duelist was a good program before the regime took it
    private static final Map<String, String> REDEEMED_SKINS = Map.of(
            "the_warden", "tron64");

    private final PackOutput.PathProvider lieutenants;
    private final PackOutput.PathProvider lang;

    public ModLorePackProvider(FabricPackOutput output) {
        PackOutput lore = new PackOutput(output.getOutputFolder().resolve("resourcepacks/" + DigitalFrontier.LORE_PACK));
        this.lieutenants = lore.createPathProvider(PackOutput.Target.DATA_PACK, "grid/lieutenant");
        this.lang = lore.createPathProvider(PackOutput.Target.RESOURCE_PACK, "lang");
    }

    /** Translation key → the lore name, overriding en_us. */
    static Map<String, String> names() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("lieutenant.digitalfrontier.the_sovereign", "Clu");
        names.put("lieutenant.digitalfrontier.the_warden", "Rinzler");
        names.put("entity.digitalfrontier.bool", "Bit");
        names.put("item.digitalfrontier.data_disc", "Identity Disc");
        names.put("entity.digitalfrontier.data_disc", "Identity Disc");
        names.put("entity.digitalfrontier.program.portal_keeper", "Tower Guardian");
        names.put("entity.digitalfrontier.system_core", "Master Control Program");
        return names;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput writer) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        ModLieutenantProvider.definitions().forEach((name, def) -> {
            String skin = LIEUTENANT_SKINS.get(name);
            LieutenantDef lore = skin == null ? def
                    : def.withTexture(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/lore/" + skin + ".png"));
            String redeemedSkin = REDEEMED_SKINS.get(name);
            if (redeemedSkin != null) {
                lore = lore.withRestoredTexture(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/lore/" + redeemedSkin + ".png"));
            }
            JsonElement json = LieutenantDef.CODEC.encodeStart(JsonOps.INSTANCE, lore).getOrThrow();
            futures.add(DataProvider.saveStable(writer, json, lieutenants.json(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, name))));
        });
        JsonObject english = new JsonObject();
        names().forEach(english::addProperty);
        futures.add(DataProvider.saveStable(writer, english, lang.json(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "en_us"))));
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Lore pack";
    }
}
