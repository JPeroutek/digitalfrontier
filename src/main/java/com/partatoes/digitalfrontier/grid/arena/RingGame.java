package com.partatoes.digitalfrontier.grid.arena;

import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.ChampionEntity;
import com.partatoes.digitalfrontier.entity.projectile.RingBall;
import com.partatoes.digitalfrontier.grid.layout.ArenaGeometry;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The ring game: the challenger and the champion, a Cesta each, and one bouncing ball. Throw it through the far ring
 * (south) to score, and keep it out of your own (north); first to three (see {@link RingGameRules}). The champion
 * chases the ball, catches it, and throws for the challenger's ring, sooner and truer at higher levels.
 */
public class RingGame extends Match {
    // How close a pass must be to a ring's center to go through
    private static final double OPENING = ArenaGeometry.RING_RADIUS - 0.3;

    @Nullable
    private ChampionEntity champion;
    @Nullable
    private RingBall ball;
    private Vec3 lastBall = Vec3.ZERO;
    private int challengerGoals;
    private int programGoals;
    private int holding;

    public RingGame(ServerLevel world, ArenaGeometry arena, ServerPlayer challenger, int level, boolean conscripted) {
        super(world, arena, ArenaGame.RING_GAME, challenger, level, conscripted);
    }

    @Override
    protected void setUp(ServerPlayer player) {
        BlockPos start = arena.challengerStart();
        player.teleportTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
        if (!player.getInventory().contains(new ItemStack(ModItems.CESTA))) {
            ItemStack cesta = new ItemStack(ModItems.CESTA);
            if (!player.getInventory().add(cesta)) {
                player.spawnAtLocation(world, cesta);
            }
        }
        BlockPos other = arena.opponentStart();
        champion = ModEntities.CHAMPION.create(world, EntitySpawnReason.EVENT);
        if (champion != null) {
            champion.snapTo(other.getX() + 0.5, other.getY(), other.getZ() + 0.5, 180, 0);
            champion.prepare(level, ChampionEntity.Mode.RING);
            champion.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.CESTA));
            world.addFreshEntity(champion);
        }
        ball = ModEntities.RING_BALL.create(world, EntitySpawnReason.EVENT);
        if (ball != null) {
            world.addFreshEntity(ball);
            serve(0);
        }
    }

    // The ball back to the middle, nudged toward whoever just conceded
    private void serve(double toward) {
        if (ball == null) {
            return;
        }
        ball.throwAlong(new Vec3(0, 1, toward));
        ball.setPos(arena.centerX() + 0.5, ArenaGeometry.FLOOR + 2, arena.centerZ() + 0.5);
        ball.setDeltaMovement(new Vec3(0, 0.4, toward * 0.2));
        lastBall = ball.position();
    }

    @Override
    protected void play(ServerPlayer player) {
        if (ball == null || ball.isRemoved() || champion == null || !champion.isAlive()) {
            finish(champion == null || !champion.isAlive());
            return;
        }
        Vec3 now = ball.position();
        BlockPos north = arena.ring(true);
        BlockPos south = arena.ring(false);
        RingGameRules.Goal goal = RingGameRules.scored(lastBall.z, now.z, now.x, now.y, north.getZ() + 0.5, south.getZ() + 0.5,
                arena.centerX() + 0.5, north.getY() + 0.5, OPENING);
        lastBall = now;
        if (goal != RingGameRules.Goal.NONE) {
            boolean challengerScored = goal == RingGameRules.Goal.CHALLENGER;
            if (challengerScored) {
                challengerGoals++;
            } else {
                programGoals++;
            }
            world.playSound(null, now.x, now.y, now.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 2.0f, challengerScored ? 1.4f : 0.8f);
            ArenaManager.announce(world, arena, Component.translatable("arena.digitalfrontier.goal",
                    challengerScored ? player.getDisplayName() : champion.getDisplayName(), challengerGoals, programGoals));
            if (RingGameRules.won(challengerGoals) || RingGameRules.won(programGoals)) {
                finish(RingGameRules.won(challengerGoals));
                return;
            }
            serve(challengerScored ? 1 : -1);
            return;
        }
        // A ball off the floor (in the stands, say) comes back to the middle
        if (!arena.onFloor(Mth.floor(now.x), Mth.floor(now.z)) || now.y < ArenaGeometry.FLOOR) {
            serve(0);
            return;
        }
        playChampion();
    }

    // The champion chases a loose ball, catches it, and after a moment throws for the challenger's ring
    private void playChampion() {
        if (champion == null || ball == null) {
            return;
        }
        if (ball.heldBy(champion)) {
            champion.getNavigation().stop();
            BlockPos target = arena.ring(true);
            Vec3 aim = Vec3.atCenterOf(target).subtract(champion.getEyePosition());
            champion.getLookControl().setLookAt(Vec3.atCenterOf(target));
            if (++holding >= 25 - 5 * level) {
                holding = 0;
                double spread = 0.12 / level;
                Vec3 jitter = new Vec3(champion.getRandom().nextGaussian() * spread, champion.getRandom().nextGaussian() * spread, 0);
                ball.throwAlong(aim.normalize().add(jitter).add(0, aim.horizontalDistance() * 0.012, 0));
            }
            return;
        }
        holding = 0;
        if (ball.free()) {
            champion.getNavigation().moveTo(ball.getX(), ball.getY(), ball.getZ(), 1.0 + 0.15 * level);
            if (champion.distanceTo(ball) < 2.2) {
                ball.catchBy(champion);
            }
        }
    }

    @Override
    protected void cleanUp() {
        if (ball != null) {
            ball.discard();
        }
        if (champion != null && champion.isAlive()) {
            champion.discard();
        }
    }
}
