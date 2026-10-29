package com.partatoes.digitalfrontier.grid;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * The digitalfrontier:grid F3 section: what the grid generates where the camera is (zone, corruption, building, site
 * and levels). Enabled from the debug options (F3 + F6). Like vanilla's chunk_generation_stats it asks the server's
 * chunk generator, so it only shows in single player.
 */
public final class GridDebugEntry implements DebugScreenEntry {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid");

    // Worked out again only when the camera moves to another block or level
    private List<String> lines = List.of();
    @Nullable
    private BlockPos lastPos;
    @Nullable
    private Level lastLevel;

    public static void register() {
        DebugScreenEntries.register(ID, new GridDebugEntry());
    }

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk,
                        @Nullable LevelChunk serverChunk) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null || !(serverOrClientLevel instanceof ServerLevel level)
                || !(level.getChunkSource().getGenerator() instanceof GridChunkGenerator generator)) {
            return;
        }
        BlockPos pos = camera.blockPosition();
        if (!pos.equals(lastPos) || level != lastLevel) {
            lines = generator.debugLines(pos);
            lastPos = pos;
            lastLevel = level;
        }
        displayer.addToGroup(ID, lines);
    }
}
