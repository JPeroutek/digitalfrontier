package com.partatoes.digitalfrontier.grid.arrival;

import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
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
            if (server.getTicks() % DISCOVERY_INTERVAL_TICKS == 0) {
                server.getPlayerManager().getPlayerList().forEach(GridArrival::discoverNearby);
            }
        });
    }

    /** The world of a grid, if it's loaded and really is a grid. */
    public static Optional<ServerWorld> gridWorld(MinecraftServer server, Identifier grid) {
        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, grid));
        return world != null && generator(world) != null ? Optional.of(world) : Optional.empty();
    }

    @Nullable
    public static GridChunkGenerator generator(ServerWorld world) {
        return world.getChunkManager().getChunkGenerator() instanceof GridChunkGenerator grid ? grid : null;
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
    public static boolean teleport(ServerPlayerEntity player, TerminalRef ref) {
        Optional<TeleportTarget> target = arrivalTarget(player.getServer(), ref, player.getYaw(), player.getPitch());
        if (target.isEmpty()) {
            return false;
        }
        player.teleportTo(target.get());
        player.onLanding();
        recordArrival(player, ref, Text.translatable("message.digitalfrontier.arrived", terminalName(ref.terminal())));
        return true;
    }

    /** Where arriving at a terminal puts a player, if the grid is loaded and still has that terminal. */
    public static Optional<TeleportTarget> arrivalTarget(MinecraftServer server, TerminalRef ref, float yaw, float pitch) {
        return gridWorld(server, ref.grid()).flatMap(world -> {
            GridChunkGenerator generator = generator(world);
            return generator.layout().terminal(ref.terminal()).map(terminal -> new TeleportTarget(world,
                    generator.arrivalPos(terminal).toBottomCenterPos(), Vec3d.ZERO, yaw, pitch, TeleportTarget.NO_OP));
        });
    }

    /** Everything that happens once a player is standing at a terminal: sound, message and their memory of it. */
    public static void recordArrival(ServerPlayerEntity player, TerminalRef ref, Text message) {
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 1.0f, 1.5f);
        GridMemory memory = player.getAttachedOrCreate(GridMemory.TYPE);
        player.setAttached(GridMemory.TYPE, memory.withArrival(ref));
        player.sendMessage(message, true);
    }

    public static Text terminalName(String terminalId) {
        if (terminalId.startsWith(WAYPOINT_PREFIX)) {
            try {
                int index = Integer.parseInt(terminalId.substring(WAYPOINT_PREFIX.length()));
                return Text.translatable("terminal.digitalfrontier.waypoint", index + 1);
            } catch (NumberFormatException ignored) {
                // Fall through to the generic name
            }
        }
        return Text.translatable("terminal.digitalfrontier." + terminalId);
    }

    private static void discoverNearby(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
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
        TerminalRef ref = new TerminalRef(world.getRegistryKey().getValue(), terminal.id());
        GridMemory memory = player.getAttachedOrCreate(GridMemory.TYPE);
        if (!memory.hasDiscovered(ref)) {
            player.setAttached(GridMemory.TYPE, memory.withDiscovered(ref));
            player.sendMessage(Text.translatable("message.digitalfrontier.terminal_discovered", terminalName(terminal.id()))
                    .formatted(Formatting.AQUA), true);
        }
    }
}
