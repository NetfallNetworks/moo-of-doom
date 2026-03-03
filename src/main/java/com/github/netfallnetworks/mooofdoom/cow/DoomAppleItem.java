package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.rarity.TieredRandom;
import com.github.netfallnetworks.mooofdoom.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Doom Apple — consumable item with tiered RNG outcomes when eaten by a player.
 * Entity feeding (cow/hostile mob) is handled by DoomAppleUseHandler.
 */
public class DoomAppleItem extends Item {

    public DoomAppleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            RarityTier tier = TieredRandom.roll(player.getRandom());
            applyPlayerEffect(player, tier);
            MooOfDoom.LOGGER.info("Player {} ate Doom Apple — rolled {}", player.getName().getString(), tier);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    private void applyPlayerEffect(Player player, RarityTier tier) {
        switch (tier) {
            case COMMON, UNCOMMON -> applyGodMode(player, 600); // 30s
            case RARE -> {
                player.addEffect(new MobEffectInstance(ModEffects.GUARDIAN, 2400, 0, false, true, true));
            }
            case LEGENDARY -> {
                applyGodMode(player, 600);
                player.addEffect(new MobEffectInstance(ModEffects.GUARDIAN, 2400, 0, false, true, true));
            }
            case MYTHIC -> {
                applyGodMode(player, 600);
                player.addEffect(new MobEffectInstance(ModEffects.GUARDIAN, 600, 0, false, true, true));
                CowMorphHandler.startMorph(player, 600); // 30s
            }
        }
    }

    private void applyGodMode(Player player, int duration) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 2));        // Strength III
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 1));      // Resistance II
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1));            // Speed II
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));          // Glowing
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0)); // Fire Resistance
    }
}
