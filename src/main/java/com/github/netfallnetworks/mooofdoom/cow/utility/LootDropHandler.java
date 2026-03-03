package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.rarity.TieredRandom;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.cow.Cow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Periodic alive-drops from OP cows. Uses the same tiered rarity table
 * as death drops — keeping your OP cow alive yields a steady trickle.
 */
public class LootDropHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.RARE_DROPS_ENABLED.getAsBoolean()) return;

        if (cow.getRandom().nextInt(ModConfig.DROP_INTERVAL_TICKS.getAsInt()) != 0) return;

        // Roll tiered loot (same table as death rare drops)
        RarityTier tier = TieredRandom.roll(cow.getRandom());
        CombatLootHandler.dropTieredLoot(cow, tier);

        // Sparkle effect
        if (cow.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cow.getX(), cow.getY() + 1, cow.getZ(),
                    10, 0.5, 0.5, 0.5, 0.0);
        }
        cow.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);
    }
}
