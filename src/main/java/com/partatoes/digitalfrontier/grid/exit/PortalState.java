package com.partatoes.digitalfrontier.grid.exit;

import com.mojang.serialization.Codec;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.world.ModSavedData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Which of a grid's Portals have had their toll paid (see {@link PortalGate}), saved with that grid's world. */
public final class PortalState extends SavedData {
    private static final Codec<PortalState> CODEC = Codec.STRING.listOf().xmap(PortalState::new, state -> List.copyOf(state.unlocked));
    private static final SavedDataType<PortalState> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "portals"),
            PortalState::new, CODEC, null);

    private final Set<String> unlocked = new HashSet<>();

    private PortalState() {
    }

    private PortalState(List<String> unlocked) {
        this.unlocked.addAll(unlocked);
    }

    public static PortalState of(ServerLevel grid) {
        return ModSavedData.computeIfAbsent(grid.getDataStorage(), TYPE);
    }

    public boolean isUnlocked(String portalId) {
        return unlocked.contains(portalId);
    }

    /** Opens or reseals a Portal; returns whether that changed anything. */
    public boolean setUnlocked(String portalId, boolean open) {
        boolean changed = open ? unlocked.add(portalId) : unlocked.remove(portalId);
        if (changed) {
            setDirty();
        }
        return changed;
    }
}
