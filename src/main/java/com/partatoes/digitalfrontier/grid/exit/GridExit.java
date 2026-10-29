package com.partatoes.digitalfrontier.grid.exit;

import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/** Leaving the grid: back to the player's respawn point in the real world, as if they'd respawned there. */
public final class GridExit {
    private GridExit() {
    }

    public static void toRealWorld(ServerPlayerEntity player) {
        // Treat the player as alive so leaving doesn't use up a respawn anchor charge
        TeleportTarget target = player.getRespawnTarget(true, TeleportTarget.NO_OP);
        if (ModDimensions.isGrid(target.world())) {
            // Spawn points can't normally be set in a grid (no beds or anchors), but commands can
            target = worldSpawn(player);
        }
        player.teleportTo(target);
        player.onLanding();
        target.world().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.PLAYERS, 1.0f, 1.2f);
        player.sendMessage(Text.translatable("message.digitalfrontier.exited_grid"), true);
    }

    /** The world spawn, or the overworld at the same spot if a command has put the world spawn in a grid. */
    public static TeleportTarget worldSpawn(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        ServerWorld world = server.getSpawnWorld();
        if (ModDimensions.isGrid(world)) {
            world = server.getOverworld();
        }
        Vec3d spawn = player.getWorldSpawnPos(world, server.getSpawnPoint().getPos()).toBottomCenterPos();
        return new TeleportTarget(world, spawn, Vec3d.ZERO, player.getYaw(), player.getPitch(), TeleportTarget.NO_OP);
    }
}
