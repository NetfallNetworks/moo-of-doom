package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class MoonJumpHandler {

    private static final String MOON_JUMP_TAG = "MooOfDoom_MoonJump";

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.MOON_JUMP_ENABLED.getAsBoolean()) return;

        // If currently in a moon jump, apply slow fall
        if (cow.getTags().contains(MOON_JUMP_TAG)) {
            if (cow.onGround()) {
                // Landed
                cow.removeTag(MOON_JUMP_TAG);
                ServerLevel level = (ServerLevel) cow.level();
                level.sendParticles(ParticleTypes.CLOUD,
                        cow.getX(), cow.getY(), cow.getZ(),
                        20, 1.0, 0.2, 1.0, 0.0);
            } else {
                // Slow fall + particles
                Vec3 motion = cow.getDeltaMovement();
                if (motion.y < -0.1) {
                    cow.setDeltaMovement(motion.x * 0.95, -0.1, motion.z * 0.95);
                }
                cow.fallDistance = 0;
                if (cow.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.END_ROD,
                            cow.getX(), cow.getY(), cow.getZ(),
                            2, 0.3, 0.0, 0.3, 0.02);
                }
            }
            return;
        }

        // Random chance to start a moon jump
        if (cow.getRandom().nextInt(ModConfig.MOON_JUMP_INTERVAL_TICKS.getAsInt()) != 0) return;

        cow.addTag(MOON_JUMP_TAG);
        cow.setDeltaMovement(cow.getDeltaMovement().add(0, 2.0, 0));
        cow.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 0.5F);

        ServerLevel level = (ServerLevel) cow.level();
        level.sendParticles(ParticleTypes.FIREWORK,
                cow.getX(), cow.getY(), cow.getZ(),
                15, 0.3, 0.1, 0.3, 0.1);
    }
}
