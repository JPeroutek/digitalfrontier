package com.partatoes.digitalfrontier.grid.parts;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads grid parts ({@code structure/grid/**.nbt}) and building definitions ({@code grid/building/*.json})
 * from server data whenever datapacks (re)load. Definitions that reference missing or wrongly sized parts
 * are skipped with an error in the log.
 */
public class GridAssetsLoader implements SimpleSynchronousResourceReloadListener {
    private static final String PART_FOLDER = "structure/grid";
    private static final String BUILDING_FOLDER = "grid/building";

    @Override
    public Identifier getFabricId() {
        return Identifier.of(DigitalFrontier.MOD_ID, "grid_assets");
    }

    @Override
    public void reload(ResourceManager manager) {
        Map<Identifier, GridPart> parts = new HashMap<>();
        manager.findResources(PART_FOLDER, path -> path.getPath().endsWith(".nbt")).forEach((file, resource) -> {
            // structure/grid/tower/floor.nbt -> grid/tower/floor, the same id a structure block or /place template uses
            Identifier id = file.withPath(path -> path.substring("structure/".length(), path.length() - ".nbt".length()));
            try (InputStream input = resource.getInputStream()) {
                parts.put(id, GridPart.fromNbt(NbtIo.readCompressed(input, NbtSizeTracker.ofUnlimitedBytes())));
            } catch (Exception e) {
                DigitalFrontier.LOGGER.error("Failed to load grid part {} from {}", id, file, e);
            }
        });

        Map<Identifier, GridBuildingDef> buildings = new HashMap<>();
        manager.findResources(BUILDING_FOLDER, path -> path.getPath().endsWith(".json")).forEach((file, resource) -> {
            Identifier id = file.withPath(path -> path.substring(BUILDING_FOLDER.length() + 1, path.length() - ".json".length()));
            parseBuilding(id, resource).ifPresent(def -> {
                List<String> problems = validate(def, parts);
                if (problems.isEmpty()) {
                    buildings.put(id, def);
                } else {
                    DigitalFrontier.LOGGER.error("Skipping grid building {}: {}", id, String.join("; ", problems));
                }
            });
        });

        GridAssets.set(new GridAssets(parts, buildings));
        DigitalFrontier.LOGGER.info("Loaded {} grid parts and {} grid buildings", parts.size(), buildings.size());
    }

    private static Optional<GridBuildingDef> parseBuilding(Identifier id, Resource resource) {
        try (Reader reader = resource.getReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            return GridBuildingDef.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Invalid grid building {}: {}", id, error));
        } catch (Exception e) {
            DigitalFrontier.LOGGER.error("Failed to read grid building {}", id, e);
            return Optional.empty();
        }
    }

    private static List<String> validate(GridBuildingDef def, Map<Identifier, GridPart> parts) {
        List<String> problems = new ArrayList<>();
        if (def.base().isEmpty() && def.floors().isEmpty() && def.roof().isEmpty()) {
            problems.add("no base, floors, or roof");
        }
        if (def.minFloors() > def.maxFloors()) {
            problems.add("min_floors is greater than max_floors");
        }
        int width = def.footprint() * 16;
        def.allParts().distinct().forEach(partId -> {
            GridPart part = parts.get(partId);
            if (part == null) {
                problems.add("missing part " + partId);
            } else if (part.sizeX() != width || part.sizeZ() != width) {
                problems.add(String.format("part %s is %d×%d but footprint %d needs %d×%d",
                        partId, part.sizeX(), part.sizeZ(), def.footprint(), width, width));
            }
        });
        return problems;
    }
}
