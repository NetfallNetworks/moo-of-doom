package com.github.netfallnetworks.mooofdoom.rarity;

/**
 * Pure weighted random selection across the 5-tier rarity system.
 * No Minecraft dependencies — safe for unit testing without Minecraft on the classpath.
 *
 * <p>For convenience rolls using configured weights, see
 * {@link com.github.netfallnetworks.mooofdoom.config.ModConfigValues#rollRarity(int)}.
 */
public class TieredRandom {

    /**
     * Roll a tier from explicit weights.
     */
    public static RarityTier roll(int rollValue, int common, int uncommon, int rare, int legendary, int mythic) {
        if (rollValue < common) return RarityTier.COMMON;
        rollValue -= common;
        if (rollValue < uncommon) return RarityTier.UNCOMMON;
        rollValue -= uncommon;
        if (rollValue < rare) return RarityTier.RARE;
        rollValue -= rare;
        if (rollValue < legendary) return RarityTier.LEGENDARY;
        return RarityTier.MYTHIC;
    }

    /**
     * Roll a tier using a Random and explicit weights.
     */
    public static RarityTier roll(java.util.Random random, int common, int uncommon, int rare, int legendary, int mythic) {
        int total = common + uncommon + rare + legendary + mythic;
        return roll(random.nextInt(total), common, uncommon, rare, legendary, mythic);
    }

    private TieredRandom() {}
}
