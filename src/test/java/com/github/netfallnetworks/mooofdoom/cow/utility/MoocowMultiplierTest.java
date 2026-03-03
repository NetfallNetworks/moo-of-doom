package com.github.netfallnetworks.mooofdoom.cow.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MoocowMultiplierTest {

    // Default MOOCOW_MULTIPLIER = 10

    @Test
    void oneHitKillGivesMaxMultiplier() {
        assertEquals(10, CombatLootHandler.calculateMultiplier(1, 10));
    }

    @Test
    void tenHitsGivesMinMultiplier() {
        assertEquals(1, CombatLootHandler.calculateMultiplier(10, 10));
    }

    @Test
    void moreThanTenHitsStillGivesOne() {
        assertEquals(1, CombatLootHandler.calculateMultiplier(15, 10));
        assertEquals(1, CombatLootHandler.calculateMultiplier(100, 10));
    }

    @Test
    void zeroHitsTreatedAsOneHit() {
        // Edge case: if somehow hits = 0, should still give max
        assertEquals(10, CombatLootHandler.calculateMultiplier(0, 10));
    }

    @ParameterizedTest
    @CsvSource({
            "1,  10, 10",
            "2,  10, 9",
            "3,  10, 8",
            "4,  10, 7",
            "5,  10, 6",
            "6,  10, 5",
            "7,  10, 4",
            "8,  10, 3",
            "9,  10, 2",
            "10, 10, 1"
    })
    void linearInterpolationWithDefault10(int hits, int moocow, int expected) {
        assertEquals(expected, CombatLootHandler.calculateMultiplier(hits, moocow),
                "hits=" + hits + " moocow=" + moocow);
    }

    @ParameterizedTest
    @CsvSource({
            "1,  20, 20",
            "5,  20, 12",
            "10, 20, 1"
    })
    void worksWithCustomMultiplier(int hits, int moocow, int expected) {
        assertEquals(expected, CombatLootHandler.calculateMultiplier(hits, moocow),
                "hits=" + hits + " moocow=" + moocow);
    }

    @Test
    void multiplierAlwaysAtLeastOne() {
        for (int hits = 1; hits <= 20; hits++) {
            for (int moocow = 1; moocow <= 100; moocow++) {
                int result = CombatLootHandler.calculateMultiplier(hits, moocow);
                assertTrue(result >= 1,
                        "Multiplier must be >= 1, got " + result + " for hits=" + hits + " moocow=" + moocow);
            }
        }
    }

    @Test
    void multiplierNeverExceedsMax() {
        for (int hits = 1; hits <= 20; hits++) {
            int moocow = 10;
            int result = CombatLootHandler.calculateMultiplier(hits, moocow);
            assertTrue(result <= moocow,
                    "Multiplier must be <= moocow, got " + result + " for hits=" + hits);
        }
    }

    @Test
    void multiplierDecreasesMonotonicallyWithHits() {
        int moocow = 10;
        int prev = CombatLootHandler.calculateMultiplier(1, moocow);
        for (int hits = 2; hits <= 10; hits++) {
            int current = CombatLootHandler.calculateMultiplier(hits, moocow);
            assertTrue(current <= prev,
                    "Multiplier should decrease: hits=" + hits + " gave " + current + " > prev " + prev);
            prev = current;
        }
    }
}
