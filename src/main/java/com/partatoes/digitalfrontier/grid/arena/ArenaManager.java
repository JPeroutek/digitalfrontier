package com.partatoes.digitalfrontier.grid.arena;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.ArenaGeometry;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The arena (see {@link ArenaGeometry}): one match at a time per grid, a challenger against the grid's champion at one
 * of its games ({@link ArenaGame}). The Announcer calls it all to everyone in the arena. Losing costs only the round:
 * a finishing blow leaves the challenger on one health in the stands, items and all. Winning pays out, and raises the
 * champion's level; beating the title champion places a trophy on the stands and is announced to the whole grid.
 */
public final class ArenaManager {
    private static final Map<ResourceKey<Level>, Match> MATCHES = new HashMap<>();

    private ArenaManager() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            Match match = MATCHES.get(world.dimension());
            if (match != null) {
                match.tick();
                if (match.over()) {
                    MATCHES.remove(world.dimension());
                }
            }
        });
        // A finishing blow in a match only ends the round
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                Match match = MATCHES.get(player.level().dimension());
                if (match != null && !match.over() && match.isChallenger(player)) {
                    player.setHealth(1);
                    match.knockedOut();
                    return false;
                }
            }
            return true;
        });
    }

    /** The arena of the grid a world is, if it is one. */
    public static Optional<ArenaGeometry> arena(ServerLevel world) {
        GridChunkGenerator generator = GridArrival.generator(world);
        return generator == null ? Optional.empty() : Optional.of(generator.layout().arena());
    }

    @Nullable
    public static Match match(ServerLevel world) {
        return MATCHES.get(world.dimension());
    }

    /** The console's menu: each game, with its champion's level, to click and step in. */
    public static void showMenu(ServerPlayer player) {
        ServerLevel world = player.level();
        if (MATCHES.containsKey(world.dimension())) {
            player.sendSystemMessage(Component.translatable("arena.digitalfrontier.busy").withStyle(ChatFormatting.RED));
            return;
        }
        ArenaState state = ArenaState.of(world);
        player.sendSystemMessage(Component.translatable("arena.digitalfrontier.menu").withStyle(ChatFormatting.GOLD));
        for (ArenaGame game : ArenaGame.values()) {
            MutableComponent line = Component.literal("  [").append(Component.translatable(game.translationKey())).append("] ")
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent.RunCommand("/grid arena join " + game.id())));
            line.append(Component.translatable("arena.digitalfrontier.champion_level", state.championLevel(game)).withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(line);
        }
    }

    /**
     * Steps a player into a game. Fails (with a message) outside an arena, or while another match is on.
     *
     * @param conscripted whether the regime brought them here (see the Recognizer)
     */
    public static boolean start(ServerPlayer player, ArenaGame game, boolean conscripted) {
        ServerLevel world = player.level();
        Optional<ArenaGeometry> found = arena(world);
        if (found.isEmpty()) {
            player.sendSystemMessage(Component.translatable("arena.digitalfrontier.none").withStyle(ChatFormatting.RED));
            return false;
        }
        if (MATCHES.containsKey(world.dimension())) {
            player.sendSystemMessage(Component.translatable("arena.digitalfrontier.busy").withStyle(ChatFormatting.RED));
            return false;
        }
        ArenaGeometry arena = found.get();
        if (!conscripted && !arena.inside(player.getBlockX(), player.getBlockZ())) {
            player.sendSystemMessage(Component.translatable("arena.digitalfrontier.come_closer").withStyle(ChatFormatting.RED));
            return false;
        }
        int level = ArenaState.of(world).championLevel(game);
        Match match = switch (game) {
            case DISC_DUEL -> new DiscDuel(world, arena, player, level, conscripted);
            case LIGHT_CYCLE -> new CycleMatch(world, arena, player, level, conscripted);
            case RING_GAME -> new RingGame(world, arena, player, level, conscripted);
        };
        MATCHES.put(world.dimension(), match);
        match.start(player);
        announce(world, arena, Component.translatable(conscripted ? "arena.digitalfrontier.conscripted_intro" : "arena.digitalfrontier.intro",
                player.getDisplayName(), Component.translatable(game.translationKey()), level));
        return true;
    }

    /** Ends any match in a grid without a result, clearing the floor. */
    public static void abort(ServerLevel world) {
        Match match = MATCHES.remove(world.dimension());
        if (match != null && !match.over()) {
            match.cleanUp();
        }
    }

    // A round's result: the announcement, and for a win the prize, the champion's rise, and perhaps a title
    static void concluded(Match match, @Nullable ServerPlayer player, boolean won) {
        ServerLevel world = match.world;
        ArenaGeometry arena = match.arena;
        Component name = player != null ? player.getDisplayName() : Component.literal("?");
        if (!won) {
            announce(world, arena, Component.translatable("arena.digitalfrontier.lost", name));
            return;
        }
        ArenaState state = ArenaState.of(world);
        announce(world, arena, Component.translatable("arena.digitalfrontier.won", name, Component.translatable(match.game().translationKey())));
        if (player != null) {
            reward(player, match.level());
        }
        if (match.level() >= ArenaState.TITLE_LEVEL) {
            state.recordTitle();
            List<BlockPos> spots = arena.trophySpots();
            world.setBlockAndUpdate(spots.get((state.titles() - 1) % spots.size()), ModBlocks.TROPHY.defaultBlockState());
            Component title = Component.translatable("arena.digitalfrontier.title", name, Component.translatable(match.game().translationKey()))
                    .withStyle(ChatFormatting.GOLD);
            world.players().forEach(other -> other.sendSystemMessage(title));
            world.playSound(null, arena.centerX(), ArenaGeometry.FLOOR + 2, arena.centerZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundSource.PLAYERS, 2.0f, 1.0f);
        }
        state.recordWin(match.game());
    }

    // Luminessence and bits, more for a tougher champion; a backup of memory for the title
    private static void reward(ServerPlayer player, int level) {
        give(player, new ItemStack(ModItems.LUMINESSENCE, 2 + 3 * level));
        for (int i = 0; i < 2 * level; i++) {
            give(player, new ItemStack(player.getRandom().nextBoolean() ? ModItems.BINARY_0 : ModItems.BINARY_1));
        }
        if (level >= ArenaState.TITLE_LEVEL) {
            give(player, new ItemStack(ModItems.MEMORY_PAGING));
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(player.level(), stack);
        }
    }

    /** The Announcer: a line to everyone in the arena. */
    public static void announce(ServerLevel world, ArenaGeometry arena, Component line) {
        Component said = Component.translatable("arena.digitalfrontier.announcer", line).withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer player : world.players()) {
            if (arena.inside(player.getBlockX(), player.getBlockZ())) {
                player.sendSystemMessage(said);
            }
        }
    }
}
