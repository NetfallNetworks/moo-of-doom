package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class ExplosionHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.EXPLOSION_ENABLED.getAsBoolean()) return;

        if (cow.getRandom().nextInt(ModConfig.EXPLOSION_INTERVAL_TICKS.getAsInt()) != 0) return;

        if (cow.getTarget() != null && cow.getTarget().isAlive()) {
            // Combat explosion: deals damage to the target
            cow.level().explode(
                    cow,
                    cow.getX(),
                    cow.getY() + 0.5,
                    cow.getZ(),
                    (float) ModConfig.EXPLOSION_POWER.get().doubleValue(),
                    Level.ExplosionInteraction.NONE
            );
        } else {
            // Random explosion: visual + sound only, no damage
            if (cow.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        cow.getX(), cow.getY() + 0.5, cow.getZ(),
                        1, 0, 0, 0, 0);
                cow.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F,
                        0.8F + cow.getRandom().nextFloat() * 0.4F);
            }
        }
    }
}
