package com.github.netfallnetworks.mooofdoom.rarity;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TieredRandomTest {

    // Default weights: 50/30/15/4/1 = 100 total
    private static final int W_COMMON = 50;
    private static final int W_UNCOMMON = 30;
    private static final int W_RARE = 15;
    private static final int W_LEGENDARY = 4;
    private static final int W_MYTHIC = 1;

    // --- Boundary tests (deterministic, using rollValue directly) ---

    @Test
    void rollValue0ReturnsCommon() {
        assertEquals(RarityTier.COMMON,
                TieredRandom.roll(0, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue49ReturnsCommon() {
        // Last value in Common range (0-49)
        assertEquals(RarityTier.COMMON,
                TieredRandom.roll(49, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue50ReturnsUncommon() {
        // First value in Uncommon range (50-79)
        assertEquals(RarityTier.UNCOMMON,
                TieredRandom.roll(50, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue79ReturnsUncommon() {
        assertEquals(RarityTier.UNCOMMON,
                TieredRandom.roll(79, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue80ReturnsRare() {
        // First value in Rare range (80-94)
        assertEquals(RarityTier.RARE,
                TieredRandom.roll(80, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue94ReturnsRare() {
        assertEquals(RarityTier.RARE,
                TieredRandom.roll(94, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue95ReturnsLegendary() {
        // First value in Legendary range (95-98)
        assertEquals(RarityTier.LEGENDARY,
                TieredRandom.roll(95, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue98ReturnsLegendary() {
        assertEquals(RarityTier.LEGENDARY,
                TieredRandom.roll(98, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue99ReturnsMythic() {
        // Only value in Mythic range (99)
        assertEquals(RarityTier.MYTHIC,
                TieredRandom.roll(99, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    // --- Zero-weight tests ---

    @Test
    void zeroWeightMythicNeverReturnsMythic() {
        Random rng = new Random(42L);
        for (int i = 0; i < 10_000; i++) {
            RarityTier tier = TieredRandom.roll(rng, 50, 30, 15, 5, 0);
            assertNotEquals(RarityTier.MYTHIC, tier);
        }
    }

    @Test
    void zeroWeightCommonNeverReturnsCommon() {
        Random rng = new Random(42L);
        for (int i = 0; i < 10_000; i++) {
            RarityTier tier = TieredRandom.roll(rng, 0, 30, 15, 4, 1);
            assertNotEquals(RarityTier.COMMON, tier);
        }
    }

    @Test
    void singleTierAlwaysReturnsThatTier() {
        Random rng = new Random(77L);
        for (int i = 0; i < 100; i++) {
            assertEquals(RarityTier.LEGENDARY,
                    TieredRandom.roll(rng, 0, 0, 0, 1, 0));
        }
    }

    // --- Distribution test ---

    @Test
    void distributionMatchesWeightsWithinTolerance() {
        Random rng = new Random(12345L);
        Map<RarityTier, Integer> counts = new EnumMap<>(RarityTier.class);
        for (RarityTier t : RarityTier.values()) counts.put(t, 0);

        int trials = 100_000;
        for (int i = 0; i < trials; i++) {
            RarityTier t = TieredRandom.roll(rng, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC);
            counts.merge(t, 1, Integer::sum);
        }

        // Within 3% absolute tolerance
        assertWithinTolerance("COMMON", counts.get(RarityTier.COMMON), trials, 0.50, 0.03);
        assertWithinTolerance("UNCOMMON", counts.get(RarityTier.UNCOMMON), trials, 0.30, 0.03);
        assertWithinTolerance("RARE", counts.get(RarityTier.RARE), trials, 0.15, 0.03);
        assertWithinTolerance("LEGENDARY", counts.get(RarityTier.LEGENDARY), trials, 0.04, 0.02);
        assertWithinTolerance("MYTHIC", counts.get(RarityTier.MYTHIC), trials, 0.01, 0.01);
    }

    // --- All enum values covered ---

    @Test
    void allTiersReachableWithDefaultWeights() {
        Random rng = new Random(99L);
        Map<RarityTier, Boolean> seen = new EnumMap<>(RarityTier.class);

        for (int i = 0; i < 100_000; i++) {
            seen.put(TieredRandom.roll(rng, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC), true);
            if (seen.size() == RarityTier.values().length) break;
        }

        for (RarityTier tier : RarityTier.values()) {
            assertTrue(seen.containsKey(tier),
                    tier + " was never rolled in 100k trials");
        }
    }

    private void assertWithinTolerance(String name, int count, int total, double expected, double tolerance) {
        double actual = count / (double) total;
        assertTrue(actual > expected - tolerance && actual < expected + tolerance,
                name + " ratio should be ~" + expected + " (±" + tolerance + "), got: " + actual);
    }
}
