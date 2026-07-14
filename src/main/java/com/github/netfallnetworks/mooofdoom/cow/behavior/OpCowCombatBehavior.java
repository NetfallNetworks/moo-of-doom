package com.github.netfallnetworks.mooofdoom.cow.behavior;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.combat.ChargeGoal;
import com.github.netfallnetworks.mooofdoom.cow.combat.HostileTargetGoal;
import com.github.netfallnetworks.mooofdoom.cow.combat.MilkProjectileGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;

import java.util.ArrayList;
import java.util.List;

/** OP cow combat AI (charge, milk projectile, hostile targeting), gated on the OP tag. */
public final class OpCowCombatBehavior extends TemporaryBehavior {

    public static final OpCowCombatBehavior INSTANCE = new OpCowCombatBehavior();
    public static final String TAG = "MooOfDoom"; // OpCowManager.OP_TAG

    private OpCowCombatBehavior() {}

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    protected List<GoalSpec> createGoals(Mob mob) {
        Cow cow = (Cow) mob;
        List<GoalSpec> goals = new ArrayList<>();
        goals.add(new GoalSpec(true, 1, new HostileTargetGoal(cow)));
        if (ModConfig.CHARGE_ATTACK_ENABLED.getAsBoolean()) {
            goals.add(new GoalSpec(false, 2, new ChargeGoal(cow)));
        }
        if (ModConfig.MILK_PROJECTILE_ENABLED.getAsBoolean()) {
            goals.add(new GoalSpec(false, 3, new MilkProjectileGoal(cow)));
        }
        return goals;
    }
}
