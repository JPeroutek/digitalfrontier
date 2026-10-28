package com.partatoes.digitalfrontier.grid.exit;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;

import java.util.List;

/**
 * The Portal's beam of light, like a beacon's but seen from anywhere in the grid: chunks with the Portal in needn't
 * be loaded. The server tells each player where the beams of the world they're in rise from, whenever they arrive in
 * a world; the client draws them.
 */
public final class PortalBeams {
    /** The beam's color, as ARGB. */
    public static final int COLOR = 0xFFC8F4FF;

    private PortalBeams() {
    }

    /** A beam rising from the block bounds {@code minX..maxX}, {@code minZ..maxZ}, from {@code baseY} up. */
    public record Beam(int minX, int minZ, int maxX, int maxZ, int baseY) {
        public static final PacketCodec<RegistryByteBuf, Beam> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, Beam::minX,
                PacketCodecs.VAR_INT, Beam::minZ,
                PacketCodecs.VAR_INT, Beam::maxX,
                PacketCodecs.VAR_INT, Beam::maxZ,
                PacketCodecs.VAR_INT, Beam::baseY,
                Beam::new);

        static Beam of(BlockBox box) {
            return new Beam(box.getMinX(), box.getMinZ(), box.getMaxX(), box.getMaxZ(), box.getMinY());
        }
    }

    /** Server to client: the beams in {@code world}; none outside a grid. */
    public record BeamsPayload(Identifier world, List<Beam> beams) implements CustomPayload {
        public static final Id<BeamsPayload> ID = new Id<>(Identifier.of(DigitalFrontier.MOD_ID, "portal_beams"));
        public static final PacketCodec<RegistryByteBuf, BeamsPayload> CODEC = PacketCodec.tuple(
                Identifier.PACKET_CODEC, BeamsPayload::world,
                Beam.CODEC.collect(PacketCodecs.toList()), BeamsPayload::beams,
                BeamsPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(BeamsPayload.ID, BeamsPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> send(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> send(newPlayer));
    }

    private static void send(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        GridChunkGenerator generator = GridArrival.generator(world);
        List<Beam> beams = generator == null ? List.of() : generator.portalBeams().stream().map(Beam::of).toList();
        ServerPlayNetworking.send(player, new BeamsPayload(world.getRegistryKey().getValue(), beams));
    }
}
