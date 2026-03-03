package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class SizeChangeHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.SIZE_CHANGE_ENABLED.getAsBoolean()) return;

        if (cow.getRandom().nextInt(ModConfig.SIZE_CHANGE_INTERVAL_TICKS.getAsInt()) != 0) return;

        // Random scale between 0.5 and 3.0
        float newScale = 0.5F + cow.getRandom().nextFloat() * 2.5F;

        // Use the SCALE attribute (available in 1.21+)
        AttributeInstance scaleAttr = cow.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(newScale);
        }

        ServerLevel level = (ServerLevel) cow.level();
        level.sendParticles(ParticleTypes.POOF,
                cow.getX(), cow.getY() + 0.5, cow.getZ(),
                15, 0.5, 0.5, 0.5, 0.05);
        cow.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F,
                newScale > 1.5F ? 0.5F : 1.5F);
    }
}
