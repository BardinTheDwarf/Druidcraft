package com.vulp.druidcraft.entities.AI.goals;

import com.vulp.druidcraft.entities.TameableMonsterEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;

import java.util.EnumSet;

public class OwnerHurtTargetGoalMonster extends TargetGoal {
    private final TameableMonsterEntity tameable;
    private LivingEntity attacker;
    private int timestamp;

    public OwnerHurtTargetGoalMonster(TameableMonsterEntity theEntityTameableIn) {
        super(theEntityTameableIn, false);
        this.tameable = theEntityTameableIn;
        this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
	public boolean shouldExecute() {
        if (this.tameable.isTamed() && !this.tameable.isSitting()) {
            LivingEntity livingentity = this.tameable.getOwner();
            if (livingentity == null) {
                return false;
            } else {
                this.attacker = livingentity.getLastAttackedEntity();
                int i = livingentity.getLastAttackedEntityTime();
                return i != this.timestamp && this.isSuitableTarget(this.attacker, EntityPredicate.DEFAULT) && this.tameable.shouldAttackEntity(this.attacker, livingentity);
            }
        } else {
            return false;
        }
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
	public void startExecuting() {
        this.goalOwner.setAttackTarget(this.attacker);
        LivingEntity livingentity = this.tameable.getOwner();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastAttackedEntityTime();
        }

        super.startExecuting();
    }
}