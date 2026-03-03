package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public class AuraHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.PASSIVE_AURA_ENABLED.getAsBoolean()) return;

        // Only check every 40 ticks (2 seconds) for performance
        if (cow.tickCount % 40 != 0) return;

        int range = ModConfig.AURA_RANGE.getAsInt();
        AABB auraBox = cow.getBoundingBox().inflate(range);
        List<Player> nearbyPlayers = cow.level().getEntitiesOfClass(Player.class, auraBox);

        for (Player player : nearbyPlayers) {
            // Duration slightly longer than check interval to avoid flickering
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, true));
        }
    }
}
