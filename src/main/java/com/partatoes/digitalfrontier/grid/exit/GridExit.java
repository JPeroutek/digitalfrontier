package com.partatoes.digitalfrontier.grid.exit;

import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Leaving the grid: back to the player's respawn point in the real world, as if they'd respawned there. */
public final class GridExit {
    private GridExit() {
    }

    public static void toRealWorld(ServerPlayer player) {
        // Treat the player as alive so leaving doesn't use up a respawn anchor charge
        TeleportTransition target = player.findRespawnPositionAndUseSpawnBlock(true, TeleportTransition.DO_NOTHING);
        if (ModDimensions.isGrid(target.newLevel())) {
            // Spawn points can't normally be set in a grid (no beds or anchors), but commands can
            target = worldSpawn(player);
        }
        player.teleport(target);
        player.resetFallDistance();
        target.newLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 1.0f, 1.2f);
        player.sendOverlayMessage(Component.translatable("message.digitalfrontier.exited_grid"));
    }

    /** The world spawn, or the overworld at the same spot if a command has put the world spawn in a grid. */
    public static TeleportTransition worldSpawn(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel world = server.findRespawnDimension();
        if (ModDimensions.isGrid(world)) {
            world = server.overworld();
        }
        Vec3 spawn = player.adjustSpawnLocation(world, server.getRespawnData().pos()).getBottomCenter();
        return new TeleportTransition(world, spawn, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING);
    }
}
