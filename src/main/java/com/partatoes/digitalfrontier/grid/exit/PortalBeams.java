package com.partatoes.digitalfrontier.grid.exit;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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
        public static final StreamCodec<RegistryFriendlyByteBuf, Beam> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Beam::minX,
                ByteBufCodecs.VAR_INT, Beam::minZ,
                ByteBufCodecs.VAR_INT, Beam::maxX,
                ByteBufCodecs.VAR_INT, Beam::maxZ,
                ByteBufCodecs.VAR_INT, Beam::baseY,
                Beam::new);

        static Beam of(BoundingBox box) {
            return new Beam(box.minX(), box.minZ(), box.maxX(), box.maxZ(), box.minY());
        }
    }

    /** Server to client: the beams in {@code world}; none outside a grid. */
    public record BeamsPayload(Identifier world, List<Beam> beams) implements CustomPacketPayload {
        public static final Type<BeamsPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "portal_beams"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BeamsPayload> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, BeamsPayload::world,
                Beam.CODEC.apply(ByteBufCodecs.list()), BeamsPayload::beams,
                BeamsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(BeamsPayload.ID, BeamsPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> send(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> send(newPlayer));
    }

    private static void send(ServerPlayer player) {
        ServerLevel world = player.level();
        GridChunkGenerator generator = GridArrival.generator(world);
        List<Beam> beams = generator == null ? List.of() : generator.portalBeams().stream().map(Beam::of).toList();
        ServerPlayNetworking.send(player, new BeamsPayload(world.dimension().identifier(), beams));
    }
}
