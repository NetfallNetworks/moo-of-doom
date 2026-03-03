package com.github.netfallnetworks.mooofdoom.cow.combat;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;

/**
 * AI goal that causes OP cows to automatically target nearby hostile mobs.
 * Detection range is controlled by the cow's FOLLOW_RANGE attribute,
 * which is set by OpCowManager based on {@code ModConfig.DETECTION_RANGE}.
 */
public class HostileTargetGoal extends NearestAttackableTargetGoal<Monster> {
    public HostileTargetGoal(Mob mob) {
        super(mob, Monster.class, 10, true, false, null);
    }
}
