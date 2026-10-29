package com.partatoes.digitalfrontier.grid.parts;

import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * An immutable snapshot of the loaded parts and building definitions. Worldgen threads read {@link #current()};
 * a datapack reload swaps in a new snapshot without disturbing chunks already generating.
 */
public final class GridAssets {
    public static final GridAssets EMPTY = new GridAssets(Map.of(), Map.of());

    private static volatile GridAssets current = EMPTY;

    private final Map<Identifier, GridPart> parts;
    private final Map<Identifier, GridBuildingDef> buildings;
    private final Map<CandidateKey, List<Candidate>> candidates = new HashMap<>();

    public GridAssets(Map<Identifier, GridPart> parts, Map<Identifier, GridBuildingDef> buildings) {
        this.parts = Map.copyOf(parts);
        this.buildings = Map.copyOf(buildings);
        // Sort so weighted picks don't depend on map iteration order
        buildings.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            GridBuildingDef def = entry.getValue();
            for (Zone zone : def.zones()) {
                candidates.computeIfAbsent(new CandidateKey(def.kind(), def.footprint(), zone, def.roadOrNull()), key -> new ArrayList<>())
                        .add(new Candidate(entry.getKey(), def));
            }
        });
    }

    public static GridAssets current() {
        return current;
    }

    static void set(GridAssets assets) {
        current = assets;
    }

    @Nullable
    public GridPart part(Identifier id) {
        return parts.get(id);
    }

    public int partCount() {
        return parts.size();
    }

    public int buildingCount() {
        return buildings.size();
    }

    /** Weighted pick among the definitions matching kind, footprint and zone; {@code roll} is uniform in [0, 1). */
    @Nullable
    public Candidate pick(GridBuildingDef.Kind kind, int footprint, Zone zone, double roll) {
        return pick(new CandidateKey(kind, footprint, zone, null), roll);
    }

    /** Street furniture for one kind of road. */
    @Nullable
    public Candidate pickStreet(RoadClass road, Zone zone, double roll) {
        return pick(new CandidateKey(GridBuildingDef.Kind.STREET, 1, zone, road), roll);
    }

    @Nullable
    private Candidate pick(CandidateKey key, double roll) {
        List<Candidate> options = candidates.get(key);
        if (options == null) {
            return null;
        }
        int total = options.stream().mapToInt(candidate -> candidate.def().weight()).sum();
        int target = (int) (roll * total);
        for (Candidate candidate : options) {
            target -= candidate.def().weight();
            if (target < 0) {
                return candidate;
            }
        }
        return options.getLast();
    }

    public record Candidate(Identifier id, GridBuildingDef def) {
    }

    // road is set only for street furniture
    private record CandidateKey(GridBuildingDef.Kind kind, int footprint, Zone zone, @Nullable RoadClass road) {
    }
}
