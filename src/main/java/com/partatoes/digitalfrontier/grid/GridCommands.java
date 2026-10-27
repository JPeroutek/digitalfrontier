package com.partatoes.digitalfrontier.grid;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.grid.tuning.GridTuning;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * {@code /grid list} and {@code /grid code} for everyone; {@code /grid open <code>} and {@code /grid delete <id>}
 * for operators.
 */
public final class GridCommands {
    private GridCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("grid")
                .then(literal("list").executes(GridCommands::list))
                .then(literal("code").executes(GridCommands::code))
                .then(literal("open")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("code", StringArgumentType.word()).executes(GridCommands::open)))
                .then(literal("delete")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("grid", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                        GridManager.tunedGrids(context.getSource().getServer()).keySet(), builder))
                                .executes(GridCommands::delete))));
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        context.getSource().sendFeedback(() -> Text.translatable("commands.digitalfrontier.grid.list",
                GridManager.tunedGrids(server).size(), server.getGameRules().getInt(GridManager.MAX_TUNED_GRIDS)), false);
        context.getSource().sendFeedback(() -> gridLine(server, ModDimensions.DEFAULT_GRID_ID, GridLayoutSettings.DEFAULT), false);
        GridManager.tunedGrids(server).forEach((id, settings) ->
                context.getSource().sendFeedback(() -> gridLine(server, id, settings), false));
        return GridManager.tunedGrids(server).size() + 1;
    }

    private static int code(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        GridLayoutSettings settings = ModDimensions.isGrid(world) && GridArrival.generator(world) != null
                ? GridArrival.generator(world).layout().settings()
                : GridLayoutSettings.DEFAULT;
        Optional<GridTuning> tuning = GridTuning.fromSettings(settings);
        if (tuning.isEmpty()) {
            context.getSource().sendError(Text.translatable("commands.digitalfrontier.grid.code.untunable"));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("commands.digitalfrontier.grid.code",
                GridKey.of(settings).toString(), copyable(tuning.get().code())), false);
        return 1;
    }

    private static int open(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Optional<GridTuning> tuning = GridTuning.fromCode(StringArgumentType.getString(context, "code"));
        if (tuning.isEmpty()) {
            source.sendError(Text.translatable("message.digitalfrontier.invalid_grid_code"));
            return 0;
        }
        Optional<ServerWorld> grid = GridManager.getOrCreate(source.getServer(), tuning.get().toSettings());
        if (grid.isEmpty()) {
            source.sendError(Text.translatable("message.digitalfrontier.grid_limit",
                    source.getServer().getGameRules().getInt(GridManager.MAX_TUNED_GRIDS)));
            return 0;
        }
        Identifier id = grid.get().getRegistryKey().getValue();
        source.sendFeedback(() -> Text.translatable("commands.digitalfrontier.grid.open", id.toString()), true);
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            GridArrival.teleport(player, new TerminalRef(id, GridLayout.MAIN_ARRIVAL_ID));
        }
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> context) {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "grid");
        if (!GridManager.delete(context.getSource().getServer(), id)) {
            context.getSource().sendError(Text.translatable("commands.digitalfrontier.grid.delete.unknown", id.toString()));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("commands.digitalfrontier.grid.delete", id.toString()), true);
        return 1;
    }

    private static Text gridLine(MinecraftServer server, Identifier id, GridLayoutSettings settings) {
        int players = GridArrival.gridWorld(server, id).map(world -> world.getPlayers().size()).orElse(0);
        MutableText line = Text.literal(" - " + id + " ").formatted(Formatting.WHITE)
                .append(Text.translatable("commands.digitalfrontier.grid.list.entry", settings.cityRadius(),
                        Math.round(settings.corruption() * 100), players).formatted(Formatting.GRAY));
        GridTuning.fromSettings(settings).ifPresent(tuning -> line.append(" ").append(copyable(tuning.code())));
        return line;
    }

    private static Text copyable(String code) {
        return Text.literal(code).styled(style -> style
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("chat.copy.click"))));
    }
}
