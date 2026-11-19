package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.regime.Sentiment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The regime's sentries, patrolling the ground it controls (see {@link com.partatoes.digitalfrontier.grid.layout.ControlField}):
 * disciplined, not decayed. They patrol in groups (the spawner places them together) and come to each other's aid.
 * They attack players the regime doesn't tolerate (see {@link com.partatoes.digitalfrontier.grid.regime.RegimeRules.Alignment}),
 * hunting the ones it wants from twice as far, and rebel programs. Conscripts are programs the regime recruited: they
 * stay for good, and count toward its numbers.
 */
public class EnforcerEntity extends Monster {
    private static final int FOLLOW_RANGE = 32;

    private boolean conscripted;

    public EnforcerEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (target, world) -> Regime.alignment(target) < Regime.rules().alignment().toleratedFrom()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ProgramEntity.class, true,
                (target, world) -> target instanceof ProgramEntity program && program.sentiment() <= Sentiment.REBEL));
        // And lieutenants that have turned on it
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LieutenantEntity.class, true,
                (target, world) -> target instanceof LieutenantEntity lieutenant && lieutenant.redeemed()));
    }

    public static AttributeSupplier.Builder createEnforcerAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    // The System Core's stray bolts pass harmlessly, or the squads it calls would turn on it
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        return !SystemCoreEntity.firedBy(source) && super.hurtServer(world, source, amount);
    }

    public boolean conscripted() {
        return conscripted;
    }

    public void setConscripted(boolean conscripted) {
        this.conscripted = conscripted;
    }

    // Players the regime wants are hunted from twice the usual range, seen or not
    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (tickCount % 20 != 0 || getTarget() != null) {
            return;
        }
        float wantedBelow = Regime.rules().alignment().wantedBelow();
        Player wanted = world.getNearestPlayer(getX(), getY(), getZ(), FOLLOW_RANGE * 2,
                player -> player instanceof Player p && !p.isSpectator() && !p.isCreative() && Regime.alignment(p) < wantedBelow);
        if (wanted != null) {
            setTarget(wanted);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putBoolean("Conscripted", conscripted);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        conscripted = view.getBooleanOr("Conscripted", false);
    }
}
