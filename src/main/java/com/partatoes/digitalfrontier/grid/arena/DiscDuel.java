package com.partatoes.digitalfrontier.grid.arena;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.block.custom.ArenaTileBlock;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.ChampionEntity;
import com.partatoes.digitalfrontier.grid.layout.ArenaGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The disc duel: the challenger and the champion face off across the tile field, and only Data Discs count. Every
 * five seconds the outermost ring of tiles flashes and drops into the pit, until only the middle is left. Falling in
 * loses; so does a finishing blow. Beating the champion wins.
 */
public class DiscDuel extends Match {
    // Every so often the next ring of tiles goes, a second after it starts to flash
    private static final int RING_EVERY = 100;
    private static final int WARNING = 20;
    // The middle this many rings in never falls
    private static final int KEPT_RINGS = 1;

    private final List<BlockPos> tiles;
    @Nullable
    private ChampionEntity champion;
    private int ringsDropped;

    public DiscDuel(ServerLevel world, ArenaGeometry arena, ServerPlayer challenger, int level, boolean conscripted) {
        super(world, arena, ArenaGame.DISC_DUEL, challenger, level, conscripted);
        this.tiles = arena.tilesOutsideIn();
    }

    @Override
    protected void setUp(ServerPlayer player) {
        restoreTiles();
        BlockPos north = new BlockPos(arena.centerX(), ArenaGeometry.FLOOR + 1, arena.centerZ() - (ArenaGeometry.TILES - 2));
        BlockPos south = new BlockPos(arena.centerX(), ArenaGeometry.FLOOR + 1, arena.centerZ() + (ArenaGeometry.TILES - 2));
        player.teleportTo(north.getX() + 0.5, north.getY(), north.getZ() + 0.5);
        champion = ModEntities.CHAMPION.create(world, EntitySpawnReason.EVENT);
        if (champion != null) {
            champion.snapTo(south.getX() + 0.5, south.getY(), south.getZ() + 0.5, 180, 0);
            champion.prepare(level, ChampionEntity.Mode.DUEL);
            champion.setTarget(player);
            world.addFreshEntity(champion);
        }
    }

    @Override
    protected void play(ServerPlayer player) {
        if (champion == null || !champion.isAlive() || champion.getY() < ArenaGeometry.FLOOR - 1) {
            finish(true);
            return;
        }
        if (player.getY() < ArenaGeometry.FLOOR - 1) {
            ArenaManager.announce(world, arena, Component.translatable("arena.digitalfrontier.fell", player.getDisplayName()));
            finish(false);
            return;
        }
        champion.setTarget(player);
        int sincePlay = ticks - COUNTDOWN;
        int phase = sincePlay % RING_EVERY;
        int ring = ArenaGeometry.TILES - ringsDropped;
        if (ring <= KEPT_RINGS) {
            return;
        }
        if (phase == RING_EVERY - WARNING) {
            setRing(ring, true);
        } else if (phase == 0 && sincePlay > 0) {
            dropRing(ring);
            ringsDropped++;
        }
    }

    private void setRing(int ring, boolean warning) {
        for (BlockPos tile : tiles) {
            if (ringOf(tile) == ring && world.getBlockState(tile).is(ModBlocks.ARENA_TILE)) {
                world.setBlockAndUpdate(tile, ModBlocks.ARENA_TILE.defaultBlockState().setValue(ArenaTileBlock.WARNING, warning));
            }
        }
    }

    private void dropRing(int ring) {
        for (BlockPos tile : tiles) {
            if (ringOf(tile) == ring) {
                world.removeBlock(tile, false);
            }
        }
        world.playSound(null, arena.centerX(), ArenaGeometry.FLOOR, arena.centerZ(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.5f, 0.6f);
    }

    private int ringOf(BlockPos tile) {
        return Math.max(Math.abs(tile.getX() - arena.centerX()), Math.abs(tile.getZ() - arena.centerZ()));
    }

    private void restoreTiles() {
        for (BlockPos tile : tiles) {
            world.setBlockAndUpdate(tile, ModBlocks.ARENA_TILE.defaultBlockState());
        }
    }

    @Override
    protected void cleanUp() {
        restoreTiles();
        if (champion != null && champion.isAlive()) {
            champion.discard();
        }
    }
}
