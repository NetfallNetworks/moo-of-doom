package com.github.netfallnetworks.mooofdoom.cow.combat;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.cow.Cow;

import java.util.EnumSet;

/**
 * Cow shoots milk projectiles at targets that are too far for melee.
 */
public class MilkProjectileGoal extends Goal {
    private final Cow cow;
    private int cooldown;

    public MilkProjectileGoal(Cow cow) {
        this.cow = cow;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!ModConfig.MILK_PROJECTILE_ENABLED.getAsBoolean()) return false;
        LivingEntity target = cow.getTarget();
        if (target == null || !target.isAlive()) return false;
        // Use ranged when target is 6+ blocks away
        return cow.distanceToSqr(target) > 36.0;
    }

    @Override
    public void tick() {
        LivingEntity target = cow.getTarget();
        if (target == null) return;

        cow.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (cooldown <= 0) {
            MilkProjectile projectile = new MilkProjectile(cow.level(), cow);
            double dx = target.getX() - cow.getX();
            double dy = target.getEyeY() - cow.getEyeY();
            double dz = target.getZ() - cow.getZ();
            // Lead the target: offset aim by target's velocity * estimated travel time
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double travelTime = dist / 1.8; // estimate based on projectile speed
            dx += target.getDeltaMovement().x * travelTime;
            dy += target.getDeltaMovement().y * travelTime;
            dz += target.getDeltaMovement().z * travelTime;
            projectile.shoot(dx, dy, dz, 1.8F, 1.0F);
            cow.level().addFreshEntity(projectile);
            cooldown = 40; // 2 second cooldown
        } else {
            cooldown--;
        }
    }
}
