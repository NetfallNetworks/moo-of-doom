package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Rebel cows melee-attack players while the rebel tag is present. */
public final class RebellionBehavior extends TemporaryBehavior {

    public static final RebellionBehavior INSTANCE = new RebellionBehavior();
    public static final String TAG = "MooOfDoom_Rebel";

    private RebellionBehavior() {}

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    protected List<GoalSpec> createGoals(Mob mob) {
        return List.of(
                new GoalSpec(false, 1, new MeleeAttackGoal((PathfinderMob) mob, 1.2, true)),
                new GoalSpec(true, 1, new NearestAttackableTargetGoal<>(mob, Player.class, true)));
    }
}
