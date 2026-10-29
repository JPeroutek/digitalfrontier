package com.partatoes.digitalfrontier.grid;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.grid.stronghold.GridEvent;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdState;
import com.partatoes.digitalfrontier.grid.stronghold.Trigger;
import com.partatoes.digitalfrontier.grid.tuning.GridTuning;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import org.jetbrains.annotations.Nullable;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * {@code /grid list}, {@code /grid code} and {@code /grid lieutenant status <grid>} for everyone;
 * {@code /grid open <code>}, {@code /grid delete <id>}, {@code /grid lieutenant trigger <grid> [signal]} and
 * {@code /grid lieutenant respawn <grid> <stronghold>} for operators.
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
                        .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                        .then(argument("code", StringArgumentType.word()).executes(GridCommands::open)))
                .then(literal("delete")
                        .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                        .then(argument("grid", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                        GridManager.tunedGrids(context.getSource().getServer()).keySet(), builder))
                                .executes(GridCommands::delete)))
                .then(literal("lieutenant")
                        .then(literal("status")
                                .then(gridArgument().executes(GridCommands::lieutenantStatus)))
                        .then(literal("trigger")
                                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                                .then(gridArgument()
                                        .executes(context -> lieutenantTrigger(context, null))
                                        .then(argument("signal", StringArgumentType.word())
                                                .executes(context -> lieutenantTrigger(context, StringArgumentType.getString(context, "signal"))))))
                        .then(literal("respawn")
                                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                                .then(gridArgument()
                                        .then(strongholdArgument().executes(GridCommands::lieutenantRespawn))))
                        .then(literal("defeat")
                                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                                .then(gridArgument()
                                        .then(strongholdArgument().executes(GridCommands::lieutenantDefeat))))));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> strongholdArgument() {
        return argument("stronghold", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        gridWorld(context).map(grid -> StrongholdManager.strongholds(grid).stream()
                                .map(view -> view.site().id()).toList()).orElse(List.of()), builder));
    }

    private static int lieutenantDefeat(CommandContext<ServerCommandSource> context) {
        Optional<ServerWorld> grid = gridWorld(context);
        String stronghold = StringArgumentType.getString(context, "stronghold");
        if (grid.isEmpty() || !StrongholdManager.forceDefeat(grid.get(), stronghold)) {
            context.getSource().sendError(Text.translatable("commands.digitalfrontier.grid.lieutenant.defeat.failed", stronghold));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("commands.digitalfrontier.grid.lieutenant.defeat", stronghold), true);
        return 1;
    }

    // Every loaded grid: the default one and the tuned ones
    private static RequiredArgumentBuilder<ServerCommandSource, Identifier> gridArgument() {
        return argument("grid", IdentifierArgumentType.identifier())
                .suggests((context, builder) -> {
                    MinecraftServer server = context.getSource().getServer();
                    List<Identifier> grids = new ArrayList<>(GridManager.tunedGrids(server).keySet());
                    grids.add(ModDimensions.DEFAULT_GRID_ID);
                    return CommandSource.suggestIdentifiers(grids, builder);
                });
    }

    private static Optional<ServerWorld> gridWorld(CommandContext<ServerCommandSource> context) {
        return GridArrival.gridWorld(context.getSource().getServer(), IdentifierArgumentType.getIdentifier(context, "grid"));
    }

    private static int lieutenantStatus(CommandContext<ServerCommandSource> context) {
        Optional<ServerWorld> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendError(Text.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgumentType.getIdentifier(context, "grid").toString()));
            return 0;
        }
        List<StrongholdManager.StrongholdView> strongholds = StrongholdManager.strongholds(grid.get());
        long now = grid.get().getTime();
        for (StrongholdManager.StrongholdView view : strongholds) {
            StrongholdState.Stronghold stronghold = view.stronghold();
            Text name = view.lieutenant() != null ? view.lieutenant().def().name() : Text.translatable("commands.digitalfrontier.grid.lieutenant.none");
            Text state = stronghold.alive()
                    ? Text.translatable("commands.digitalfrontier.grid.lieutenant.alive").formatted(Formatting.RED)
                    : Text.translatable("commands.digitalfrontier.grid.lieutenant.defeated", (now - stronghold.defeatedAt()) / 20).formatted(Formatting.AQUA);
            context.getSource().sendFeedback(() -> Text.translatable("commands.digitalfrontier.grid.lieutenant.status",
                    view.site().id(), view.center().toShortString(), name, state, stronghold.respawns()), false);
            if (!stronghold.alive() && view.lieutenant() != null) {
                List<Trigger> leaves = view.lieutenant().respawn().leaves();
                for (int i = 0; i < leaves.size(); i++) {
                    int count = i < stronghold.progress().size() ? stronghold.progress().get(i) : 0;
                    String leaf = leaves.get(i).type();
                    context.getSource().sendFeedback(() -> Text.literal("   " + leaf + ": " + count).formatted(Formatting.GRAY), false);
                }
            }
        }
        return strongholds.size();
    }

    private static int lieutenantTrigger(CommandContext<ServerCommandSource> context, @Nullable String signal) {
        Optional<ServerWorld> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendError(Text.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgumentType.getIdentifier(context, "grid").toString()));
            return 0;
        }
        boolean counted = StrongholdManager.fire(grid.get(), new GridEvent.Function(signal)).counted();
        context.getSource().sendFeedback(() -> Text.translatable(counted
                ? "commands.digitalfrontier.grid.lieutenant.trigger.counted"
                : "commands.digitalfrontier.grid.lieutenant.trigger.ignored"), true);
        return counted ? 1 : 0;
    }

    private static int lieutenantRespawn(CommandContext<ServerCommandSource> context) {
        Optional<ServerWorld> grid = gridWorld(context);
        String stronghold = StringArgumentType.getString(context, "stronghold");
        if (grid.isEmpty() || !StrongholdManager.forceRespawn(grid.get(), stronghold)) {
            context.getSource().sendError(Text.translatable("commands.digitalfrontier.grid.lieutenant.respawn.failed", stronghold));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("commands.digitalfrontier.grid.lieutenant.respawn", stronghold), true);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        context.getSource().sendFeedback(() -> Text.translatable("commands.digitalfrontier.grid.list",
                GridManager.tunedGrids(server).size(), GridManager.maxTunedGrids(server)), false);
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
                    GridManager.maxTunedGrids(source.getServer())));
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
                .withClickEvent(new ClickEvent.CopyToClipboard(code))
                .withHoverEvent(new HoverEvent.ShowText(Text.translatable("chat.copy.click"))));
    }
}
