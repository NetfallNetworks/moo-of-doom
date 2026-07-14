package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

/** Guardian cows melee-attack monsters while the guardian tag is present. */
public final class GuardianBehavior extends TemporaryBehavior {

    public static final GuardianBehavior INSTANCE = new GuardianBehavior();
    public static final String TAG = "MooOfDoom_Guardian";

    private GuardianBehavior() {}

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    protected List<GoalSpec> createGoals(Mob mob) {
        return List.of(
                new GoalSpec(false, 1, new MeleeAttackGoal((PathfinderMob) mob, 1.2, true)),
                new GoalSpec(true, 1, new NearestAttackableTargetGoal<>(mob, Monster.class, true)));
    }
}
