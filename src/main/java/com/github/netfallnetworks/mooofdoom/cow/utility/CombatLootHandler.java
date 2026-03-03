package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.rarity.TieredRandom;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks hits on OP cows and calculates MOOCOW-multiplied drops on death.
 * Also buffs vanilla cow drops by VANILLA_COW_LOOT_MULTIPLIER.
 *
 * MOOCOW formula: 1-hit kill = max multiplier, 10+ hits = 1x.
 * One additional roll on the tiered rare loot table on OP cow death.
 */
public class CombatLootHandler {

    // Track hit count per OP cow (entity ID -> hit count)
    private static final Map<Integer, Integer> hitCounts = new HashMap<>();

    @SubscribeEvent
    public static void onCowHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;

        hitCounts.merge(cow.getId(), 1, Integer::sum);
    }

    @SubscribeEvent
    public static void onCowDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;

        if (OpCowManager.isOpCow(cow)) {
            handleOpCowDeath(cow);
        } else {
            handleVanillaCowDeath(cow);
        }
    }

    private static void handleOpCowDeath(Cow cow) {
        int hits = hitCounts.getOrDefault(cow.getId(), 1);
        hitCounts.remove(cow.getId());

        int moocow = ModConfig.MOOCOW_MULTIPLIER.getAsInt();
        int multiplier = calculateMultiplier(hits, moocow);

        // Drop extra base drops (vanilla loot table provides 1x, we add multiplier-1 extra)
        int extra = multiplier - 1;
        if (extra > 0) {
            int beef = 1 + cow.getRandom().nextInt(3);
            int leather = cow.getRandom().nextInt(3);
            dropItem(cow, new ItemStack(Items.BEEF, beef * extra));
            if (leather > 0) {
                dropItem(cow, new ItemStack(Items.LEATHER, leather * extra));
            }
        }

        // One roll on the tiered rare loot table
        RarityTier tier = TieredRandom.roll(cow.getRandom());
        dropTieredLoot(cow, tier);

        // Visual feedback
        if (cow.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cow.getX(), cow.getY() + 1, cow.getZ(),
                    20, 1.0, 1.0, 1.0, 0.0);
        }
    }

    private static void handleVanillaCowDeath(Cow cow) {
        int multiplier = ModConfig.VANILLA_COW_LOOT_MULTIPLIER.getAsInt();
        int extra = multiplier - 1;
        if (extra <= 0) return;

        int beef = 1 + cow.getRandom().nextInt(3);
        int leather = cow.getRandom().nextInt(3);
        dropItem(cow, new ItemStack(Items.BEEF, beef * extra));
        if (leather > 0) {
            dropItem(cow, new ItemStack(Items.LEATHER, leather * extra));
        }
    }

    /**
     * Drops a tiered rare loot item. Used by both death drops and alive-drops.
     */
    static void dropTieredLoot(Cow cow, RarityTier tier) {
        switch (tier) {
            case COMMON -> dropItem(cow, new ItemStack(Items.IRON_INGOT, 8));
            case UNCOMMON -> {
                dropItem(cow, new ItemStack(Items.GOLD_INGOT, 5));
                dropItem(cow, new ItemStack(Items.EMERALD, 3));
            }
            case RARE -> dropItem(cow, new ItemStack(Items.DIAMOND, 1));
            case LEGENDARY -> dropItem(cow, new ItemStack(Items.NETHERITE_SCRAP, 1));
            case MYTHIC -> dropItem(cow, new ItemStack(ModItems.DOOM_APPLE.get()));
        }
    }

    /**
     * Calculate the MOOCOW loot multiplier based on hit count.
     * 1-hit kill = max multiplier, 10+ hits = 1x, linear interpolation between.
     */
    static int calculateMultiplier(int hits, int moocowMax) {
        if (hits >= 10) return 1;
        if (hits <= 1) return moocowMax;
        return (int) Math.round(moocowMax - (hits - 1.0) * (moocowMax - 1.0) / 9.0);
    }

    static void dropItem(Cow cow, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(cow.level(),
                cow.getX(), cow.getY() + 0.5, cow.getZ(), stack);
        itemEntity.setDefaultPickUpDelay();
        cow.level().addFreshEntity(itemEntity);
    }
}
