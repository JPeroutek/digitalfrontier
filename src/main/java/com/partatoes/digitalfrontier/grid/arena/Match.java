package com.partatoes.digitalfrontier.grid.arena;

import com.partatoes.digitalfrontier.grid.layout.ArenaGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One round in the arena: a challenger against the grid's champion at one game. A three-second countdown, then the
 * game runs until someone wins; the challenger forfeits by leaving the arena or the grid. Losing costs only the round
 * (see {@link ArenaManager}). Each game is a subclass.
 */
public abstract class Match {
    /** Ticks of countdown before play starts. */
    public static final int COUNTDOWN = 60;

    protected final ServerLevel world;
    protected final ArenaGeometry arena;
    protected final ArenaGame game;
    protected final int level;
    protected final boolean conscripted;
    private final UUID challenger;
    protected int ticks;
    private boolean over;

    protected Match(ServerLevel world, ArenaGeometry arena, ArenaGame game, ServerPlayer challenger, int level, boolean conscripted) {
        this.world = world;
        this.arena = arena;
        this.game = game;
        this.challenger = challenger.getUUID();
        this.level = level;
        this.conscripted = conscripted;
    }

    public ArenaGame game() {
        return game;
    }

    public int level() {
        return level;
    }

    public boolean conscripted() {
        return conscripted;
    }

    public boolean over() {
        return over;
    }

    public boolean isChallenger(ServerPlayer player) {
        return player.getUUID().equals(challenger);
    }

    @Nullable
    protected ServerPlayer challenger() {
        return world.getServer().getPlayerList().getPlayer(challenger);
    }

    /** Sets the floor and the opponents up, and puts the challenger in place. */
    protected abstract void setUp(ServerPlayer player);

    /** A tick of play, after the countdown. */
    protected abstract void play(ServerPlayer player);

    /** Clears the floor and the opponents away, win or lose. */
    protected abstract void cleanUp();

    final void start(ServerPlayer player) {
        setUp(player);
    }

    final void tick() {
        if (over) {
            return;
        }
        ServerPlayer player = challenger();
        if (player == null || player.level() != world || !player.isAlive() || !arena.inside(player.getBlockX(), player.getBlockZ())) {
            finish(false);
            return;
        }
        ticks++;
        if (ticks <= COUNTDOWN) {
            if (ticks % 20 == 1) {
                ArenaManager.announce(world, arena, Component.translatable("arena.digitalfrontier.countdown", 3 - ticks / 20));
            }
            if (ticks == COUNTDOWN) {
                ArenaManager.announce(world, arena, Component.translatable("arena.digitalfrontier.go"));
            }
            return;
        }
        play(player);
    }

    /** Ends the round: the challenger has won or lost it. */
    protected final void finish(boolean won) {
        if (over) {
            return;
        }
        over = true;
        cleanUp();
        ServerPlayer player = challenger();
        if (player != null && player.level() == world) {
            BlockPos stands = arena.stands();
            player.teleportTo(stands.getX() + 0.5, stands.getY(), stands.getZ() + 0.5);
            player.resetFallDistance();
        }
        ArenaManager.concluded(this, player, won);
    }

    /** The challenger lost: called when a blow would have finished them (they're left on 1 health). */
    final void knockedOut() {
        finish(false);
    }
}
