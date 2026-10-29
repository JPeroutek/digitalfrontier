package com.partatoes.digitalfrontier.entity.custom;

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

/**
 * The regime's sentries, found where corruption runs high. Hostile, they patrol in groups (the spawner places
 * them together), come to each other's aid, and hunt clean programs as well as players.
 */
public class EnforcerEntity extends Monster {
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
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ProgramEntity.class, true));
    }

    public static AttributeSupplier.Builder createEnforcerAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 32);
    }
}
