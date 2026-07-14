package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.List;

/** Converted "protector" mobs fight monsters while the protector tag is present. */
public final class ProtectorBehavior extends TemporaryBehavior {

    public static final ProtectorBehavior INSTANCE = new ProtectorBehavior();
    public static final String TAG = "MooOfDoom_Protector";

    private ProtectorBehavior() {}

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    protected List<GoalSpec> createGoals(Mob mob) {
        List<GoalSpec> goals = new ArrayList<>();
        if (mob instanceof PathfinderMob pathfinderMob) {
            goals.add(new GoalSpec(false, 1, new MeleeAttackGoal(pathfinderMob, 1.2, true)));
        }
        goals.add(new GoalSpec(true, 1, new NearestAttackableTargetGoal<>(mob, Monster.class, true)));
        return goals;
    }
}
