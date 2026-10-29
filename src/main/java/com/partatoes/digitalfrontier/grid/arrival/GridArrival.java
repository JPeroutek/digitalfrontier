package com.partatoes.digitalfrontier.grid.arrival;

import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Getting players to grid terminals, and noticing which terminals they've found. */
public final class GridArrival {
    /** How close, in chunks, a player has to come to a terminal to discover it. */
    private static final int DISCOVERY_RADIUS = 2;
    private static final int DISCOVERY_INTERVAL_TICKS = 20;
    private static final String WAYPOINT_PREFIX = "waypoint_";

    private GridArrival() {
    }

    public static void register() {
        GridMemory.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % DISCOVERY_INTERVAL_TICKS == 0) {
                server.getPlayerList().getPlayers().forEach(GridArrival::discoverNearby);
            }
        });
    }

    /** The world of a grid, if it's loaded and really is a grid. */
    public static Optional<ServerLevel> gridWorld(MinecraftServer server, Identifier grid) {
        ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, grid));
        return world != null && generator(world) != null ? Optional.of(world) : Optional.empty();
    }

    @Nullable
    public static GridChunkGenerator generator(ServerLevel world) {
        return world.getChunkSource().getGenerator() instanceof GridChunkGenerator grid ? grid : null;
    }

    /** The terminal nearest a position, with the position's block coordinates read as grid block coordinates. */
    public static Optional<TerminalRef> nearestTerminal(MinecraftServer server, Identifier grid, BlockPos pos) {
        return gridWorld(server, grid).map(world -> {
            Site terminal = generator(world).layout().nearestTerminal(pos.getX() >> 4, pos.getZ() >> 4);
            return new TerminalRef(grid, terminal.id());
        });
    }

    /**
     * Sends a player to a terminal and remembers it as their last arrival. Fails (returns false) if the grid
     * isn't loaded or no longer has that terminal.
     */
    public static boolean teleport(ServerPlayer player, TerminalRef ref) {
        Optional<TeleportTransition> target = arrivalTarget(player.level().getServer(), ref, player.getYRot(), player.getXRot());
        if (target.isEmpty()) {
            return false;
        }
        player.teleport(target.get());
        player.resetFallDistance();
        recordArrival(player, ref, Component.translatable("message.digitalfrontier.arrived", terminalName(ref.terminal())));
        return true;
    }

    /** Where arriving at a terminal puts a player, if the grid is loaded and still has that terminal. */
    public static Optional<TeleportTransition> arrivalTarget(MinecraftServer server, TerminalRef ref, float yaw, float pitch) {
        return gridWorld(server, ref.grid()).flatMap(world -> {
            GridChunkGenerator generator = generator(world);
            return generator.layout().terminal(ref.terminal()).map(terminal -> new TeleportTransition(world,
                    generator.arrivalPos(terminal).getBottomCenter(), Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING));
        });
    }

    /** Everything that happens once a player is standing at a terminal: sound, message and their memory of it. */
    public static void recordArrival(ServerPlayer player, TerminalRef ref, Component message) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0f, 1.5f);
        GridMemory memory = player.getAttachedOrCreate(GridMemory.TYPE);
        player.setAttached(GridMemory.TYPE, memory.withArrival(ref));
        player.sendOverlayMessage(message);
    }

    public static Component terminalName(String terminalId) {
        if (terminalId.startsWith(WAYPOINT_PREFIX)) {
            try {
                int index = Integer.parseInt(terminalId.substring(WAYPOINT_PREFIX.length()));
                return Component.translatable("terminal.digitalfrontier.waypoint", index + 1);
            } catch (NumberFormatException ignored) {
                // Fall through to the generic name
            }
        }
        return Component.translatable("terminal.digitalfrontier." + terminalId);
    }

    private static void discoverNearby(ServerPlayer player) {
        ServerLevel world = player.level();
        if (!ModDimensions.isGrid(world) || player.isSpectator()) {
            return;
        }
        GridChunkGenerator generator = generator(world);
        if (generator == null) {
            return;
        }
        int chunkX = player.getBlockX() >> 4;
        int chunkZ = player.getBlockZ() >> 4;
        Site terminal = generator.layout().nearestTerminal(chunkX, chunkZ);
        if (terminal.distanceTo(chunkX, chunkZ) > DISCOVERY_RADIUS) {
            return;
        }
        TerminalRef ref = new TerminalRef(world.dimension().identifier(), terminal.id());
        GridMemory memory = player.getAttachedOrCreate(GridMemory.TYPE);
        if (!memory.hasDiscovered(ref)) {
            player.setAttached(GridMemory.TYPE, memory.withDiscovered(ref));
            player.sendOverlayMessage(Component.translatable("message.digitalfrontier.terminal_discovered", terminalName(terminal.id()))
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
