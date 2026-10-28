package com.partatoes.digitalfrontier.compat;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.fabricmc.loader.api.FabricLoader;

/** Integrations with optional mods. Each one's classes are only touched when that mod is installed. */
public final class ModCompat {
    private ModCompat() {
    }

    public static void register() {
        if (FabricLoader.getInstance().isModLoaded("accessories")) {
            DigitalFrontier.LOGGER.info("Accessories found: Core Dumps will keep equipped accessories");
            AccessoriesCompat.register();
        }
    }
}
