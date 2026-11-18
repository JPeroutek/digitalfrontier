package com.partatoes.digitalfrontier.grid;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.partatoes.digitalfrontier.grid.stronghold.GridEvent;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdState;
import com.partatoes.digitalfrontier.grid.stronghold.Trigger;
import com.partatoes.digitalfrontier.grid.tuning.GridTuning;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import org.jetbrains.annotations.Nullable;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /grid list}, {@code /grid code} and {@code /grid lieutenant status <grid>} for everyone;
 * {@code /grid open <code>}, {@code /grid delete <id>}, {@code /grid locate <grid> <site>},
 * {@code /grid lieutenant trigger <grid> [signal]} and
 * {@code /grid lieutenant respawn <grid> <stronghold>} for operators.
 */
public final class GridCommands {
    private GridCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("grid")
                .then(literal("list").executes(GridCommands::list))
                .then(literal("code").executes(GridCommands::code))
                .then(literal("open")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(argument("code", StringArgumentType.word()).executes(GridCommands::open)))
                .then(literal("delete")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(argument("grid", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        GridManager.tunedGrids(context.getSource().getServer()).keySet(), builder))
                                .executes(GridCommands::delete)))
                .then(literal("locate")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(gridArgument()
                                .then(argument("site", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                gridWorld(context).map(GridCommands::sites).orElse(List.of()).stream()
                                                        .map(Site::id).toList(), builder))
                                        .executes(GridCommands::locate))))
                .then(literal("lieutenant")
                        .then(literal("status")
                                .then(gridArgument().executes(GridCommands::lieutenantStatus)))
                        .then(literal("trigger")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(gridArgument()
                                        .executes(context -> lieutenantTrigger(context, null))
                                        .then(argument("signal", StringArgumentType.word())
                                                .executes(context -> lieutenantTrigger(context, StringArgumentType.getString(context, "signal"))))))
                        .then(literal("respawn")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(gridArgument()
                                        .then(strongholdArgument().executes(GridCommands::lieutenantRespawn))))
                        .then(literal("defeat")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(gridArgument()
                                        .then(strongholdArgument().executes(GridCommands::lieutenantDefeat))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> strongholdArgument() {
        return argument("stronghold", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        gridWorld(context).map(grid -> StrongholdManager.strongholds(grid).stream()
                                .map(view -> view.site().id()).toList()).orElse(List.of()), builder));
    }

    private static int lieutenantDefeat(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        String stronghold = StringArgumentType.getString(context, "stronghold");
        if (grid.isEmpty() || !StrongholdManager.forceDefeat(grid.get(), stronghold)) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.lieutenant.defeat.failed", stronghold));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.lieutenant.defeat", stronghold), true);
        return 1;
    }

    // Every loaded grid: the default one and the tuned ones
    private static RequiredArgumentBuilder<CommandSourceStack, Identifier> gridArgument() {
        return argument("grid", IdentifierArgument.id())
                .suggests((context, builder) -> {
                    MinecraftServer server = context.getSource().getServer();
                    List<Identifier> grids = new ArrayList<>(GridManager.tunedGrids(server).keySet());
                    grids.add(ModDimensions.DEFAULT_GRID_ID);
                    return SharedSuggestionProvider.suggestResource(grids, builder);
                });
    }

    private static Optional<ServerLevel> gridWorld(CommandContext<CommandSourceStack> context) {
        return GridArrival.gridWorld(context.getSource().getServer(), IdentifierArgument.getId(context, "grid"));
    }

    private static List<Site> sites(ServerLevel grid) {
        GridChunkGenerator generator = GridArrival.generator(grid);
        return generator == null ? List.of() : generator.layout().sites();
    }

    // Where a layout site is, with a click to teleport there: for testing, and for finding things in a big grid
    private static int locate(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        String id = StringArgumentType.getString(context, "site");
        Optional<Site> site = grid.flatMap(world -> sites(world).stream().filter(s -> s.id().equals(id)).findFirst());
        if (site.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.locate.unknown", id));
            return 0;
        }
        int x = site.get().chunkX() * 16 + 8;
        int z = site.get().chunkZ() * 16 + 8;
        // On top of whatever is there, generating the chunk if it hasn't been. The grid's generator doesn't fill
        // in heightmaps, so the column is searched instead
        ServerLevel world = grid.get();
        world.getChunk(site.get().chunkX(), site.get().chunkZ());
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos(x, world.getMaxY(), z);
        while (column.getY() > world.getMinY() && world.getBlockState(column).getCollisionShape(world, column).isEmpty()) {
            column.move(Direction.DOWN);
        }
        int y = column.getY() + 1;
        String position = x + " " + y + " " + z;
        String teleport = "/execute in " + grid.get().dimension().identifier() + " run tp @s " + position;
        Component coordinates = Component.literal("[" + position + "]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent.SuggestCommand(teleport))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))));
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.locate", id, coordinates), false);
        return 1;
    }

    private static int lieutenantStatus(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        List<StrongholdManager.StrongholdView> strongholds = StrongholdManager.strongholds(grid.get());
        long now = grid.get().getGameTime();
        for (StrongholdManager.StrongholdView view : strongholds) {
            StrongholdState.Stronghold stronghold = view.stronghold();
            Component name = view.lieutenant() != null ? view.lieutenant().def().name() : Component.translatable("commands.digitalfrontier.grid.lieutenant.none");
            Component state = stronghold.alive()
                    ? Component.translatable("commands.digitalfrontier.grid.lieutenant.alive").withStyle(ChatFormatting.RED)
                    : Component.translatable("commands.digitalfrontier.grid.lieutenant.defeated", (now - stronghold.defeatedAt()) / 20).withStyle(ChatFormatting.AQUA);
            context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.lieutenant.status",
                    view.site().id(), view.center().toShortString(), name, state, stronghold.respawns()), false);
            if (!stronghold.alive() && view.lieutenant() != null) {
                List<Trigger> leaves = view.lieutenant().respawn().leaves();
                for (int i = 0; i < leaves.size(); i++) {
                    int count = i < stronghold.progress().size() ? stronghold.progress().get(i) : 0;
                    String leaf = leaves.get(i).type();
                    context.getSource().sendSuccess(() -> Component.literal("   " + leaf + ": " + count).withStyle(ChatFormatting.GRAY), false);
                }
            }
        }
        return strongholds.size();
    }

    private static int lieutenantTrigger(CommandContext<CommandSourceStack> context, @Nullable String signal) {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        boolean counted = StrongholdManager.fire(grid.get(), new GridEvent.Function(signal)).counted();
        context.getSource().sendSuccess(() -> Component.translatable(counted
                ? "commands.digitalfrontier.grid.lieutenant.trigger.counted"
                : "commands.digitalfrontier.grid.lieutenant.trigger.ignored"), true);
        return counted ? 1 : 0;
    }

    private static int lieutenantRespawn(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        String stronghold = StringArgumentType.getString(context, "stronghold");
        if (grid.isEmpty() || !StrongholdManager.forceRespawn(grid.get(), stronghold)) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.lieutenant.respawn.failed", stronghold));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.lieutenant.respawn", stronghold), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.list",
                GridManager.tunedGrids(server).size(), GridManager.maxTunedGrids(server)), false);
        context.getSource().sendSuccess(() -> gridLine(server, ModDimensions.DEFAULT_GRID_ID, GridLayoutSettings.DEFAULT), false);
        GridManager.tunedGrids(server).forEach((id, settings) ->
                context.getSource().sendSuccess(() -> gridLine(server, id, settings), false));
        return GridManager.tunedGrids(server).size() + 1;
    }

    private static int code(CommandContext<CommandSourceStack> context) {
        ServerLevel world = context.getSource().getLevel();
        GridLayoutSettings settings = ModDimensions.isGrid(world) && GridArrival.generator(world) != null
                ? GridArrival.generator(world).layout().settings()
                : GridLayoutSettings.DEFAULT;
        Optional<GridTuning> tuning = GridTuning.fromSettings(settings);
        if (tuning.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.code.untunable"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.code",
                GridKey.of(settings).toString(), copyable(tuning.get().code())), false);
        return 1;
    }

    private static int open(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<GridTuning> tuning = GridTuning.fromCode(StringArgumentType.getString(context, "code"));
        if (tuning.isEmpty()) {
            source.sendFailure(Component.translatable("message.digitalfrontier.invalid_grid_code"));
            return 0;
        }
        Optional<ServerLevel> grid = GridManager.getOrCreate(source.getServer(), tuning.get().toSettings());
        if (grid.isEmpty()) {
            source.sendFailure(Component.translatable("message.digitalfrontier.grid_limit",
                    GridManager.maxTunedGrids(source.getServer())));
            return 0;
        }
        Identifier id = grid.get().dimension().identifier();
        source.sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.open", id.toString()), true);
        if (source.getEntity() instanceof ServerPlayer player) {
            GridArrival.teleport(player, new TerminalRef(id, GridLayout.MAIN_ARRIVAL_ID));
        }
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        Identifier id = IdentifierArgument.getId(context, "grid");
        if (!GridManager.delete(context.getSource().getServer(), id)) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.delete.unknown", id.toString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.delete", id.toString()), true);
        return 1;
    }

    private static Component gridLine(MinecraftServer server, Identifier id, GridLayoutSettings settings) {
        int players = GridArrival.gridWorld(server, id).map(world -> world.players().size()).orElse(0);
        MutableComponent line = Component.literal(" - " + id + " ").withStyle(ChatFormatting.WHITE)
                .append(Component.translatable("commands.digitalfrontier.grid.list.entry", settings.cityRadius(),
                        Math.round(settings.corruption() * 100), Math.round(settings.regime() * 100), players).withStyle(ChatFormatting.GRAY));
        GridTuning.fromSettings(settings).ifPresent(tuning -> line.append(" ").append(copyable(tuning.code())));
        return line;
    }

    private static Component copyable(String code) {
        return Component.literal(code).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.CopyToClipboard(code))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click"))));
    }
}
