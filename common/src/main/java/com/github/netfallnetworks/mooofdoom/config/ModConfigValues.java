package com.github.netfallnetworks.mooofdoom.config;

import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.rarity.TieredRandom;

public final class ModConfigValues {
    // --- Activation ---
    public static ActivationMode activationMode = ActivationMode.ITEM_ACTIVATED;
    public static double rareSpawnChance = 0.05;
    public static double mythicSpawnChance = 0.01;

    // --- Combat ---
    public static boolean chargeAttackEnabled = true;
    public static boolean milkProjectileEnabled = true;
    public static int cowHealth = 100;
    public static int cowAttackDamage = 10;
    public static int detectionRange = 24;
    public static int chargeCooldownTicks = 100;

    // --- Utility ---
    public static boolean enchantedMilkEnabled = true;
    public static boolean rareDropsEnabled = true;
    public static boolean passiveAuraEnabled = true;
    public static int auraRange = 10;
    public static int dropIntervalTicks = 6000;

    // --- Chaos ---
    public static boolean sizeChangeEnabled = true;
    public static boolean explosionEnabled = true;
    public static boolean moonJumpEnabled = true;
    public static int sizeChangeIntervalTicks = 2400;
    public static int explosionIntervalTicks = 3600;
    public static double explosionPower = 2.0;
    public static int moonJumpIntervalTicks = 6000;

    // --- Loot ---
    public static int moocowMultiplier = 10;
    public static int vanillaCowLootMultiplier = 2;

    // --- Rebellion & Guardian ---
    public static boolean rebellionEnabled = true;
    public static int rebellionDurationTicks = 2400;
    public static int rebellionRange = 16;
    public static boolean guardianEnabled = true;
    public static int guardianDurationTicks = 2400;
    public static int guardianRange = 16;

    // --- Rarity Weights ---
    public static int rarityCommonWeight = 50;
    public static int rarityUncommonWeight = 30;
    public static int rarityRareWeight = 15;
    public static int rarityLegendaryWeight = 4;
    public static int rarityMythicWeight = 1;

    /** Sum of all rarity weights. */
    public static int rarityTotalWeight() {
        return rarityCommonWeight + rarityUncommonWeight + rarityRareWeight
                + rarityLegendaryWeight + rarityMythicWeight;
    }

    /** Roll a rarity tier using the configured weights. */
    public static RarityTier rollRarity(int randomValue) {
        return TieredRandom.roll(randomValue,
                rarityCommonWeight, rarityUncommonWeight,
                rarityRareWeight, rarityLegendaryWeight, rarityMythicWeight);
    }

    private ModConfigValues() {}

    public enum ActivationMode {
        ALL_COWS,
        ITEM_ACTIVATED,
        RARE_SPAWN
    }
}
