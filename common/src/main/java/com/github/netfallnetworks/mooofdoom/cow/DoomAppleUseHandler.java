package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.config.ModConfigValues;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Handles feeding a Doom Apple to entities via right-click interaction.
 * - Feed to non-OP cow: transforms to OP cow (no RNG)
 * - Feed to OP cow: wasted (consumed, no effect)
 * - Feed to hostile mob: tiered conversion (protector, explosion, cow, OP cow)
 *
 * Player eating the apple is handled by DoomAppleItem via the food/consumable system.
 */
public class DoomAppleUseHandler {


    /**
     * @return true if the interaction was handled and the event should be canceled
     */
    public static boolean onPlayerInteractEntity(Player player, Entity target, InteractionHand hand, Level level) {
        if (level.isClientSide()) return false;
        if (!player.getItemInHand(hand).is(ModItems.DOOM_APPLE.get())) return false;

        if (target instanceof Cow cow) {
            handleCowFeed(player, hand, cow);
            return true;
        } else if (target instanceof Monster monster) {
            handleHostileFeed(player, hand, monster);
            return true;
        }
        return false;
    }

    private static void handleCowFeed(Player player, InteractionHand hand, Cow cow) {
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }

        if (OpCowManager.isOpCow(cow)) {
            // Already OP — wasted apple
            cow.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.8F);
            MooOfDoom.LOGGER.debug("Doom Apple wasted on already-OP cow");
            return;
        }

        // Transform to OP cow
        OpCowManager.makeOpCow(cow);

        ServerLevel serverLevel = (ServerLevel) cow.level();
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                cow.getX(), cow.getY() + 1, cow.getZ(),
                50, 0.5, 1.0, 0.5, 0.2);
        cow.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);

        // Advancement: created first OP cow
        if (player instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.CREATE_OP_COW.get().trigger(serverPlayer);
        }
    }

    private static void handleHostileFeed(Player player, InteractionHand hand, Monster monster) {
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }

        RarityTier tier = ModConfigValues.rollRarity(player.getRandom().nextInt(ModConfigValues.rarityTotalWeight()));
        MobConversionHandler.applyHostileConversion(monster, player, tier);

        // Advancement: converted a hostile mob
        if (player instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.CONVERT_HOSTILE.get().trigger(serverPlayer);
        }
    }
}
