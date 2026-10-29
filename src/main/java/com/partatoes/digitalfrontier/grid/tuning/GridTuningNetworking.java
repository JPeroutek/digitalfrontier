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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
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
    public record OpenScreenPayload(BlockPos pos, String code) implements CustomPacketPayload {
        public static final Type<OpenScreenPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "open_grid_tuning"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreenPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenScreenPayload::pos,
                ByteBufCodecs.STRING_UTF8, OpenScreenPayload::code,
                OpenScreenPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    /** Client to server: tune the laser at {@code pos} to the grid with this code. */
    public record TunePayload(BlockPos pos, String code) implements CustomPacketPayload {
        public static final Type<TunePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "tune_laser"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TunePayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TunePayload::pos,
                ByteBufCodecs.stringUtf8(64), TunePayload::code,
                TunePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TunePayload.ID, TunePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TunePayload.ID, (payload, context) -> tune(context.player(), payload));
    }

    public static void openScreen(ServerPlayer player, DigitizingLaserBlockEntity laser) {
        GridTuning current = Optional.ofNullable(laser.link())
                .flatMap(link -> GridArrival.gridWorld(player.level().getServer(), link.grid()))
                .flatMap(world -> GridTuning.fromSettings(GridArrival.generator(world).layout().settings()))
                .orElse(GridTuning.DEFAULT);
        ServerPlayNetworking.send(player, new OpenScreenPayload(laser.getBlockPos(), current.code()));
    }

    private static void tune(ServerPlayer player, TunePayload payload) {
        ServerLevel world = player.level();
        BlockPos pos = payload.pos();
        if (!(world.getBlockState(pos).getBlock() instanceof DigitizingLaserBlock block) || !block.isTunable()
                || !(world.getBlockEntity(pos) instanceof DigitizingLaserBlockEntity laser)
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE * MAX_DISTANCE) {
            return;
        }
        Optional<GridTuning> tuning = GridTuning.fromCode(payload.code());
        if (tuning.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.digitalfrontier.invalid_grid_code").withStyle(ChatFormatting.RED));
            return;
        }
        Optional<ServerLevel> grid = GridManager.getOrCreate(player.level().getServer(), tuning.get().toSettings());
        if (grid.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.digitalfrontier.grid_limit",
                    GridManager.maxTunedGrids(player.level().getServer())).withStyle(ChatFormatting.RED));
            return;
        }
        Identifier gridId = grid.get().dimension().identifier();
        laser.setLink(new TerminalRef(gridId, GridLayout.MAIN_ARRIVAL_ID));
        world.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 0.8f);
        player.sendOverlayMessage(Component.translatable("message.digitalfrontier.laser_tuned_grid", gridId.toString()).withStyle(ChatFormatting.AQUA));
    }
}
