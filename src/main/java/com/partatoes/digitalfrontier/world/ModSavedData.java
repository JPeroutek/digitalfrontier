package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the mod's saved data, first moving it from where it was saved before 26.1. Until then a saved data file was
 * named data/digitalfrontier_<name>.dat; since 26.1 it's data/digitalfrontier/<name>.dat. Minecraft's own world
 * upgrade only moves vanilla's files, so without this an old world would lose its tuned grids and strongholds.
 */
public final class ModSavedData {
    // Each file is only looked for once per run
    private static final Set<Path> CHECKED = ConcurrentHashMap.newKeySet();

    private ModSavedData() {
    }

    public static <T extends SavedData> T computeIfAbsent(SavedDataStorage storage, SavedDataType<T> type) {
        Path file = type.id().withSuffix(".dat").resolveAgainst(storage.dataFolder);
        if (CHECKED.add(file)) {
            Path legacy = storage.dataFolder.resolve(type.id().getNamespace() + "_" + type.id().getPath() + ".dat");
            if (Files.exists(legacy) && !Files.exists(file)) {
                try {
                    Files.createDirectories(file.getParent());
                    Files.move(legacy, file);
                    DigitalFrontier.LOGGER.info("Moved {} from before 26.1 to {}", legacy, file);
                } catch (IOException e) {
                    DigitalFrontier.LOGGER.error("Couldn't move {} from before 26.1 to {}", legacy, file, e);
                }
            }
        }
        return storage.computeIfAbsent(type);
    }
}
