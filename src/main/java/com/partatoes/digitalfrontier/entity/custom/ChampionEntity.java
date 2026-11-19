package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.entity.projectile.ThrownDataDisc;
import com.partatoes.digitalfrontier.grid.arena.ArenaState;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * An arena champion: the program a challenger faces in the arena's games (see
 * {@link com.partatoes.digitalfrontier.grid.arena.ArenaManager}). Its level (1 to {@value ArenaState#TITLE_LEVEL})
 * rises each time a game's champion is beaten, making it tougher and quicker; at the top it's the title champion, with
 * a boss bar. In a disc duel it fights only with Data Discs, and only discs hurt it. It belongs to a match: the arena
 * clears it away when the round ends.
 */
public class ChampionEntity extends Monster {
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(ChampionEntity.class, EntityDataSerializers.INT);

    /** What it's doing in its match. */
    public enum Mode {
        /** Throwing discs across the duel field. */
        DUEL,
        /** Riding a light cycle, steered by the match. */
        RIDER,
        /** Chasing, catching and throwing the ring game's ball, run by the match. */
        RING
    }

    private final ServerBossEvent bossBar = new ServerBossEvent(Mth.createInsecureUUID(this.random), Component.empty(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private Mode mode = Mode.DUEL;

    public ChampionEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
        setPersistenceRequired();
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createChampionAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEVEL, 1);
    }

    public int championLevel() {
        return entityData.get(LEVEL);
    }

    public Mode mode() {
        return mode;
    }

    /** Readies it for a match: its level sets its health; a duellist holds a disc. */
    public void prepare(int level, Mode mode) {
        this.mode = mode;
        entityData.set(LEVEL, level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(30 + 15 * (level - 1));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28 + 0.03 * level);
        setHealth(getMaxHealth());
        if (mode == Mode.DUEL) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DATA_DISC));
        }
        setDropChance(EquipmentSlot.MAINHAND, 0);
    }

    public boolean titleChampion() {
        return championLevel() >= ArenaState.TITLE_LEVEL;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DuelGoal());
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24));
    }

    // In a duel only discs hurt it, and it never falls for the ordinary blows
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (mode == Mode.DUEL && !(source.getDirectEntity() instanceof ThrownDataDisc) && !source.isCreativePlayer()) {
            return false;
        }
        return super.hurtServer(world, source, amount);
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        bossBar.setName(getDisplayName());
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setVisible(titleChampion());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // A duellist circles its opponent and throws discs at it, faster the higher its level
    private class DuelGoal extends Goal {
        private int cooldown = 40;
        private int strafe = 1;

        DuelGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mode == Mode.DUEL && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            if (random.nextInt(60) == 0) {
                strafe = -strafe;
            }
            // Keep a throwing distance, sidestepping
            double distance = distanceTo(target);
            float forward = distance > 9 ? 0.5f : distance < 5 ? -0.5f : 0;
            getMoveControl().strafe(forward, 0.4f * strafe);
            if (--cooldown <= 0 && hasLineOfSight(target) && level() instanceof ServerLevel world) {
                cooldown = 50 - 12 * championLevel();
                ThrownDataDisc disc = new ThrownDataDisc(ChampionEntity.this, world, new ItemStack(ModItems.DATA_DISC));
                Vec3 aim = target.getEyePosition().subtract(disc.position());
                disc.shoot(aim.x, aim.y, aim.z, 1.4f, 3 - championLevel());
                world.addFreshEntity(disc);
                world.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.0f, 1.2f);
            }
        }
    }
}
