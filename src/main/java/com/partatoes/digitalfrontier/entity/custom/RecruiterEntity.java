package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.regime.RegimeRules;
import com.partatoes.digitalfrontier.grid.regime.RegimeState;
import com.partatoes.digitalfrontier.grid.regime.Sentiment;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * The regime's recruitment agent. Unarmed, it goes from program to program: any that stays close for long enough may
 * be conscripted (see {@link ProgramEntity#expose}), and every so often it offers one a bribe. How likely either is to
 * work depends on the program's sentiment and on how badly the regime needs members (see
 * {@link com.partatoes.digitalfrontier.grid.regime.Conscription}). Players can pay it tribute in Luminessence to win
 * the regime's favour. A recruiter is one of the regime's lasting members: it doesn't despawn, and counts toward the
 * regime's numbers.
 */
public class RecruiterEntity extends PathfinderMob {
    private int bribeIn;
    // Whether it has been counted on the regime's roster, which it is once it's in a grid
    private boolean rostered;

    public RecruiterEntity(EntityType<? extends PathfinderMob> entityType, Level world) {
        super(entityType, world);
        setPersistenceRequired();
        // Not straight away: a new recruiter works the street a while before its first offer
        int interval = Regime.rules().recruiting().bribeInterval();
        bribeIn = interval / 2 + random.nextInt(interval);
    }

    public static AttributeSupplier.Builder createRecruiterAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(3, new CanvassGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public boolean rostered() {
        return rostered;
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (GridArrival.generator(world) == null) {
            return;
        }
        if (!rostered) {
            rostered = true;
            RegimeState.of(world).changeRoster(1);
        }
        RegimeRules.Recruiting recruiting = Regime.rules().recruiting();
        if (tickCount % 20 == 0) {
            world.getEntitiesOfClass(ProgramEntity.class, getBoundingBox().inflate(recruiting.exposureRadius()), ProgramEntity::isAlive)
                    .forEach(program -> program.expose(world, 20));
        }
        if (--bribeIn <= 0) {
            bribeIn = recruiting.bribeInterval();
            bribe(world, recruiting.bribeRadius());
        }
    }

    // The nearest program it isn't already working on gets an offer
    private void bribe(ServerLevel world, int radius) {
        world.getEntitiesOfClass(ProgramEntity.class, getBoundingBox().inflate(radius), program -> program.isAlive() && !program.recentlyExposed())
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .ifPresent(program -> {
                    world.sendParticles(ParticleTypes.ELECTRIC_SPARK, program.getX(), program.getEyeY(), program.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                    world.playSound(null, getX(), getY(), getZ(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 1.0f, 0.7f);
                    program.offerBribe(world);
                });
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        return !SystemCoreEntity.firedBy(source) && super.hurtServer(world, source, amount);
    }

    // Tribute: Luminessence for the regime's favour
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.LUMINESSENCE)) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel world) {
            Regime.shiftAlignment(player, Regime.rules().alignment().tribute());
            stack.consume(1, player);
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 0.7f);
            player.sendOverlayMessage(Component.translatable("entity.digitalfrontier.recruiter.tribute"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putBoolean("Rostered", rostered);
        view.putInt("BribeIn", bribeIn);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        rostered = view.getBooleanOr("Rostered", false);
        bribeIn = view.getIntOr("BribeIn", bribeIn);
    }

    // Goes to the nearest program that isn't against the regime, and stays with it
    private class CanvassGoal extends Goal {
        @Nullable
        private ProgramEntity mark;

        CanvassGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (random.nextInt(20) != 0) {
                return false;
            }
            mark = level().getEntitiesOfClass(ProgramEntity.class, getBoundingBox().inflate(16),
                            program -> program.isAlive() && program.stance() != Sentiment.Stance.REBEL
                                    && program.stance() != Sentiment.Stance.RESISTANCE).stream()
                    .min(Comparator.comparingDouble(RecruiterEntity.this::distanceToSqr))
                    .orElse(null);
            return mark != null;
        }

        @Override
        public void start() {
            getNavigation().moveTo(mark, 0.7);
        }

        @Override
        public boolean canContinueToUse() {
            return mark != null && mark.isAlive();
        }

        @Override
        public void tick() {
            if (mark != null && distanceToSqr(mark) > 4 && tickCount % 10 == 0) {
                getNavigation().moveTo(mark, 0.7);
            }
        }

        @Override
        public void stop() {
            mark = null;
        }
    }
}
