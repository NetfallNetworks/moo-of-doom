package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.rarity.TieredRandom;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles feeding a Doom Apple to entities via right-click interaction.
 * - Feed to non-OP cow: transforms to OP cow (no RNG)
 * - Feed to OP cow: wasted (consumed, no effect)
 * - Feed to hostile mob: tiered conversion (protector, explosion, cow, OP cow)
 *
 * Player eating the apple is handled by DoomAppleItem via the food/consumable system.
 */
public class DoomAppleUseHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!event.getItemStack().is(ModItems.DOOM_APPLE.get())) return;

        if (event.getTarget() instanceof Cow cow) {
            handleCowFeed(event, cow);
        } else if (event.getTarget() instanceof Monster monster) {
            handleHostileFeed(event, monster);
        }
    }

    private static void handleCowFeed(PlayerInteractEvent.EntityInteract event, Cow cow) {
        event.setCanceled(true);

        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }

        if (OpCowManager.isOpCow(cow)) {
            // Already OP — wasted apple
            cow.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.8F);
            MooOfDoom.LOGGER.debug("Doom Apple wasted on already-OP cow");
            return;
        }

        // Transform to OP cow
        OpCowManager.makeOpCow(cow);

        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                cow.getX(), cow.getY() + 1, cow.getZ(),
                50, 0.5, 1.0, 0.5, 0.2);
        cow.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);

        // Advancement: created first OP cow
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.CREATE_OP_COW.get().trigger(serverPlayer);
        }
    }

    private static void handleHostileFeed(PlayerInteractEvent.EntityInteract event, Monster monster) {
        event.setCanceled(true);

        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }

        RarityTier tier = TieredRandom.roll(event.getEntity().getRandom());
        MobConversionHandler.applyHostileConversion(monster, event.getEntity(), tier);

        // Advancement: converted a hostile mob
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.CONVERT_HOSTILE.get().trigger(serverPlayer);
        }
    }
}
