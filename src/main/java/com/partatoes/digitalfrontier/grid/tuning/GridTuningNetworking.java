package com.partatoes.digitalfrontier.grid.tuning;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.custom.DigitizingLaserBlock;
import com.partatoes.digitalfrontier.block.entity.DigitizingLaserBlockEntity;
import com.partatoes.digitalfrontier.grid.GridManager;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * The Grid Tuning Laser's screen: the server opens it with the laser's current grid code, and the client sends
 * back the code to tune to. Everything the client sends is checked again here.
 */
public final class GridTuningNetworking {
    private static final double MAX_DISTANCE = 8.0;

    private GridTuningNetworking() {
    }

    /** Server to client: open the tuning screen for the laser at {@code pos}, showing {@code code}. */
    public record OpenScreenPayload(BlockPos pos, String code) implements CustomPayload {
        public static final Id<OpenScreenPayload> ID = new Id<>(Identifier.of(DigitalFrontier.MOD_ID, "open_grid_tuning"));
        public static final PacketCodec<RegistryByteBuf, OpenScreenPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, OpenScreenPayload::pos,
                PacketCodecs.STRING, OpenScreenPayload::code,
                OpenScreenPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Client to server: tune the laser at {@code pos} to the grid with this code. */
    public record TunePayload(BlockPos pos, String code) implements CustomPayload {
        public static final Id<TunePayload> ID = new Id<>(Identifier.of(DigitalFrontier.MOD_ID, "tune_laser"));
        public static final PacketCodec<RegistryByteBuf, TunePayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, TunePayload::pos,
                PacketCodecs.string(64), TunePayload::code,
                TunePayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TunePayload.ID, TunePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TunePayload.ID, (payload, context) -> tune(context.player(), payload));
    }

    public static void openScreen(ServerPlayerEntity player, DigitizingLaserBlockEntity laser) {
        GridTuning current = Optional.ofNullable(laser.link())
                .flatMap(link -> GridArrival.gridWorld(player.getServer(), link.grid()))
                .flatMap(world -> GridTuning.fromSettings(GridArrival.generator(world).layout().settings()))
                .orElse(GridTuning.DEFAULT);
        ServerPlayNetworking.send(player, new OpenScreenPayload(laser.getPos(), current.code()));
    }

    private static void tune(ServerPlayerEntity player, TunePayload payload) {
        ServerWorld world = player.getWorld();
        BlockPos pos = payload.pos();
        if (!(world.getBlockState(pos).getBlock() instanceof DigitizingLaserBlock block) || !block.isTunable()
                || !(world.getBlockEntity(pos) instanceof DigitizingLaserBlockEntity laser)
                || player.squaredDistanceTo(Vec3d.ofCenter(pos)) > MAX_DISTANCE * MAX_DISTANCE) {
            return;
        }
        Optional<GridTuning> tuning = GridTuning.fromCode(payload.code());
        if (tuning.isEmpty()) {
            player.sendMessage(Text.translatable("message.digitalfrontier.invalid_grid_code").formatted(Formatting.RED), true);
            return;
        }
        Optional<ServerWorld> grid = GridManager.getOrCreate(player.getServer(), tuning.get().toSettings());
        if (grid.isEmpty()) {
            player.sendMessage(Text.translatable("message.digitalfrontier.grid_limit",
                    player.getServer().getGameRules().getInt(GridManager.MAX_TUNED_GRIDS)).formatted(Formatting.RED), true);
            return;
        }
        Identifier gridId = grid.get().getRegistryKey().getValue();
        laser.setLink(new TerminalRef(gridId, GridLayout.MAIN_ARRIVAL_ID));
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0f, 0.8f);
        player.sendMessage(Text.translatable("message.digitalfrontier.laser_tuned_grid", gridId.toString()).formatted(Formatting.AQUA), true);
    }
}
