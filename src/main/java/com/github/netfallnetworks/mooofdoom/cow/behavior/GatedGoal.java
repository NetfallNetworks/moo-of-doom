package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.function.BooleanSupplier;

/**
 * Decorator that wraps any vanilla Goal and gates its activation on a predicate
 * (typically "entity has behavior tag X"). When the gate is false the goal is inert.
 * Nothing is ever removed from a goal selector — deactivation just flips the gate —
 * so goal leaks are structurally impossible. Carries a behaviorId so installers can
 * detect an existing install by scanning the selector.
 */
public class GatedGoal extends Goal {

    private final String behaviorId;
    private final BooleanSupplier gate;
    private final Goal delegate;

    public GatedGoal(String behaviorId, BooleanSupplier gate, Goal delegate) {
        this.behaviorId = behaviorId;
        this.gate = gate;
        this.delegate = delegate;
        this.setFlags(delegate.getFlags());
    }

    public String getBehaviorId() {
        return behaviorId;
    }

    @Override
    public boolean canUse() {
        return gate.getAsBoolean() && delegate.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return gate.getAsBoolean() && delegate.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return delegate.isInterruptable();
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return delegate.requiresUpdateEveryTick();
    }

    @Override
    public void tick() {
        delegate.tick();
    }
}
