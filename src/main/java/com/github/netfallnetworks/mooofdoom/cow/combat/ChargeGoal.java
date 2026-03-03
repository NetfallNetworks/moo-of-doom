package com.github.netfallnetworks.mooofdoom.cow.combat;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ChargeGoal extends Goal {
    private final Cow cow;
    private LivingEntity target;
    private int cooldownTicks;
    private boolean charging;

    public ChargeGoal(Cow cow) {
        this.cow = cow;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!ModConfig.CHARGE_ATTACK_ENABLED.getAsBoolean()) return false;
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }
        target = cow.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = cow.distanceToSqr(target);
        return dist >= 16.0 && dist <= 256.0;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && charging && cow.distanceToSqr(target) > 2.0;
    }

    @Override
    public void start() {
        charging = true;
    }

    @Override
    public void stop() {
        charging = false;
        cooldownTicks = ModConfig.CHARGE_COOLDOWN_TICKS.getAsInt();
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) return;

        cow.getLookControl().setLookAt(target, 30.0F, 30.0F);

        Vec3 direction = target.position().subtract(cow.position()).normalize();
        cow.setDeltaMovement(direction.x * 0.8, cow.getDeltaMovement().y, direction.z * 0.8);

        if (cow.distanceToSqr(target) < 4.0) {
            target.hurt(cow.damageSources().mobAttack(cow), ModConfig.COW_ATTACK_DAMAGE.getAsInt());
            Vec3 kb = direction.scale(3.0);
            target.push(kb.x, 0.5, kb.z);
            charging = false;
        }
    }
}
