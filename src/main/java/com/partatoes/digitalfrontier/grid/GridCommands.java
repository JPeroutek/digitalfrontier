package com.partatoes.digitalfrontier.grid;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.exit.PortalState;
import com.partatoes.digitalfrontier.grid.arena.ArenaGame;
import com.partatoes.digitalfrontier.grid.arena.ArenaManager;
import com.partatoes.digitalfrontier.grid.arena.ArenaState;
import com.partatoes.digitalfrontier.grid.arena.Match;
import com.partatoes.digitalfrontier.grid.layout.ArenaGeometry;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.core.CoreManager;
import com.partatoes.digitalfrontier.grid.core.CoreState;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.regime.RegimeState;
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
import net.minecraft.commands.arguments.EntityArgument;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /grid list}, {@code /grid code}, {@code /grid lieutenant status <grid>} and
 * {@code /grid regime status <grid> [player]} and {@code /grid core status <grid>} for everyone;
 * {@code /grid regime alignment <grid> <player> <value>}, {@code /grid core reset <grid>},
 * {@code /grid open <code>}, {@code /grid delete <id>}, {@code /grid locate <grid> <site>},
 * {@code /grid lieutenant trigger <grid> [signal]} and
 * {@code /grid lieutenant respawn <grid> <stronghold>} for operators.
 */
public final class GridCommands {
    // Not a layout site, but locatable: the building chunk the city core sets aside for a garage
    private static final String GARAGE = "garage";

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
                                                Stream.concat(gridWorld(context).map(GridCommands::sites).orElse(List.of()).stream()
                                                        .map(Site::id), Stream.of(GARAGE)).toList(), builder))
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
                                        .then(strongholdArgument().executes(GridCommands::lieutenantDefeat)))))
                .then(literal("core")
                        .then(literal("status")
                                .then(gridArgument().executes(GridCommands::coreStatus)))
                        .then(literal("reset")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(gridArgument().executes(GridCommands::coreReset))))
                .then(literal("arena")
                        // Anyone in the arena can step in; the console's menu runs this
                        .then(literal("join")
                                .then(argument("game", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(ArenaGame.values()).map(ArenaGame::id), builder))
                                        .executes(GridCommands::arenaJoin)))
                        .then(literal("status")
                                .then(gridArgument().executes(GridCommands::arenaStatus)))
                        .then(literal("reset")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(gridArgument().executes(GridCommands::arenaReset))))
                .then(literal("portal")
                        .then(literal("status")
                                .then(gridArgument().executes(GridCommands::portalStatus)))
                        .then(literal("open")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(gridArgument().executes(context -> portalSet(context, true))))
                        .then(literal("seal")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(gridArgument().executes(context -> portalSet(context, false)))))
                .then(literal("regime")
                        .then(literal("status")
                                .then(gridArgument()
                                        .executes(context -> regimeStatus(context, false))
                                        .then(argument("player", EntityArgument.player())
                                                .executes(context -> regimeStatus(context, true)))))
                        .then(literal("alignment")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(gridArgument()
                                        .then(argument("player", EntityArgument.player())
                                                .then(argument("value", FloatArgumentType.floatArg(-1, 1))
                                                        .executes(GridCommands::regimeAlignment)))))));
    }

    private static int coreStatus(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        CoreState.Phase phase = CoreManager.phase(grid.get());
        BlockPos shaft = CoreManager.shaftTop();
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.core.status",
                phase.getSerializedName(), shaft.getX() + ", " + shaft.getY() + ", " + shaft.getZ()), false);
        return phase.ordinal();
    }

    private static int coreReset(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        CoreManager.reset(grid.get());
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.core.reset"), true);
        return 1;
    }

    private static int arenaJoin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(context, "game");
        Optional<ArenaGame> game = ArenaGame.byId(id);
        if (game.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("arena.digitalfrontier.unknown_game", id));
            return 0;
        }
        return ArenaManager.start(player, game.get(), false) ? 1 : 0;
    }

    // Each game's champion level, the titles won, and any match under way
    private static int arenaStatus(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        ArenaState state = ArenaState.of(grid.get());
        ArenaManager.arena(grid.get()).ifPresent(arena -> context.getSource().sendSuccess(() -> Component.translatable(
                "commands.digitalfrontier.grid.arena.where", arena.centerX() + ", " + ArenaGeometry.FLOOR + ", " + arena.centerZ(), state.titles()), false));
        for (ArenaGame game : ArenaGame.values()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.arena.game",
                    Component.translatable(game.translationKey()), state.championLevel(game)), false);
        }
        Match match = ArenaManager.match(grid.get());
        if (match != null) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.arena.playing",
                    Component.translatable(match.game().translationKey())), false);
        }
        return state.titles();
    }

    private static int arenaReset(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        ArenaManager.abort(grid.get());
        ArenaState.of(grid.get()).reset();
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.arena.reset"), true);
        return 1;
    }

    // Every Portal of the grid, open or sealed (see PortalGate)
    private static int portalStatus(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        PortalState state = PortalState.of(grid.get());
        List<String> portals = portals(grid.get());
        portals.forEach(portal -> context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.portal.status",
                portal, Component.translatable(state.isUnlocked(portal)
                        ? "commands.digitalfrontier.grid.portal.open" : "commands.digitalfrontier.grid.portal.sealed")), false));
        return (int) portals.stream().filter(state::isUnlocked).count();
    }

    // Opens every Portal of the grid as if its toll were paid, or seals them again
    private static int portalSet(CommandContext<CommandSourceStack> context, boolean open) {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        PortalState state = PortalState.of(grid.get());
        List<String> portals = portals(grid.get());
        portals.forEach(portal -> state.setUnlocked(portal, open));
        context.getSource().sendSuccess(() -> Component.translatable(open
                ? "commands.digitalfrontier.grid.portal.opened" : "commands.digitalfrontier.grid.portal.resealed", portals.size()), true);
        return portals.size();
    }

    private static List<String> portals(ServerLevel grid) {
        GridChunkGenerator generator = GridArrival.generator(grid);
        return generator == null ? List.of() : generator.layout().sites().stream()
                .filter(site -> site.type() == Site.Type.PORTAL).map(Site::id).toList();
    }

    // The regime's size against what it wants, and a player's alignment: the given player's, or the caller's own
    private static int regimeStatus(CommandContext<CommandSourceStack> context, boolean named) throws CommandSyntaxException {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        int population = Regime.population(grid.get());
        int ideal = Regime.ideal(grid.get());
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.regime.status", population, ideal), false);
        ServerPlayer player = named ? EntityArgument.getPlayer(context, "player") : context.getSource().getPlayer();
        if (player != null) {
            float alignment = RegimeState.of(grid.get()).alignment(player.getUUID());
            context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.regime.alignment",
                    player.getDisplayName(), String.format("%.2f", alignment)), false);
        }
        return population;
    }

    private static int regimeAlignment(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Optional<ServerLevel> grid = gridWorld(context);
        if (grid.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.unknown", IdentifierArgument.getId(context, "grid").toString()));
            return 0;
        }
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        float value = FloatArgumentType.getFloat(context, "value");
        RegimeState.of(grid.get()).setAlignment(player.getUUID(), value);
        context.getSource().sendSuccess(() -> Component.translatable("commands.digitalfrontier.grid.regime.alignment",
                player.getDisplayName(), String.format("%.2f", value)), true);
        return 1;
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

    // Where a layout site (or the city's reserved garage) is, with a click to teleport there: for testing, and for
    // finding things in a big grid
    private static int locate(CommandContext<CommandSourceStack> context) {
        Optional<ServerLevel> grid = gridWorld(context);
        String id = StringArgumentType.getString(context, "site");
        Optional<GridLayout.ChunkSpot> spot = GARAGE.equals(id)
                ? grid.map(GridArrival::generator).flatMap(generator -> generator.layout().garage())
                : grid.flatMap(world -> sites(world).stream().filter(s -> s.id().equals(id)).findFirst())
                        .map(s -> new GridLayout.ChunkSpot(s.chunkX(), s.chunkZ()));
        if (spot.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.digitalfrontier.grid.locate.unknown", id));
            return 0;
        }
        int x = spot.get().chunkX() * 16 + 8;
        int z = spot.get().chunkZ() * 16 + 8;
        // On top of whatever is there, generating the chunk if it hasn't been. The grid's generator doesn't fill
        // in heightmaps, so the column is searched instead
        ServerLevel world = grid.get();
        world.getChunk(spot.get().chunkX(), spot.get().chunkZ());
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
