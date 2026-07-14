package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.util.List;

/**
 * Template base for tag-gated AI behaviors (Rebellion, Guardian, Protector, OP combat).
 *
 * Lifecycle invariants are implemented final so subclasses cannot get them wrong:
 * - ensureInstalled: idempotent by scanning the live selector for this behavior's
 *   GatedGoals — no external state, self-heals across chunk reload (goal selectors are
 *   never serialized; tags are).
 * - deactivate: strips the tag. The GatedGoal gate goes false and the goals are inert
 *   immediately. Nothing is ever removed, so nothing can leak (issue #19).
 *
 * Subclasses declare only tag() and createGoals(). This class is the ONLY place in the
 * mod allowed to call GoalSelector.addGoal — enforced by BehaviorArchitectureTest.
 */
public abstract class TemporaryBehavior {

    /** One goal to install: which selector, at what priority. */
    public record GoalSpec(boolean targetSelector, int priority, Goal goal) {}

    /** Persistent entity tag that marks this behavior active. */
    public abstract String tag();

    /** The goals this behavior needs. Called at most once per entity per session. */
    protected abstract List<GoalSpec> createGoals(Mob mob);

    public final boolean isActive(Mob mob) {
        return mob.getTags().contains(tag());
    }

    public final void activate(Mob mob) {
        ensureInstalled(mob);
        mob.addTag(tag());
    }

    public final void deactivate(Mob mob) {
        mob.removeTag(tag());
    }

    public final void ensureInstalled(Mob mob) {
        if (isInstalled(mob.goalSelector) || isInstalled(mob.targetSelector)) return;
        for (GoalSpec spec : createGoals(mob)) {
            GatedGoal gated = new GatedGoal(tag(), () -> isActive(mob), spec.goal());
            GoalSelector selector = spec.targetSelector() ? mob.targetSelector : mob.goalSelector;
            selector.addGoal(spec.priority(), gated);
        }
    }

    private boolean isInstalled(GoalSelector selector) {
        for (WrappedGoal wrapped : selector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof GatedGoal gated
                    && gated.getBehaviorId().equals(tag())) {
                return true;
            }
        }
        return false;
    }
}
