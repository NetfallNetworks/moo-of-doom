package com.github.netfallnetworks.mooofdoom;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Activation ---
    public static final ModConfigSpec.EnumValue<ActivationMode> ACTIVATION_MODE;
    public static final ModConfigSpec.DoubleValue RARE_SPAWN_CHANCE;
    public static final ModConfigSpec.DoubleValue MYTHIC_SPAWN_CHANCE;

    // --- Combat ---
    public static final ModConfigSpec.BooleanValue CHARGE_ATTACK_ENABLED;
    public static final ModConfigSpec.BooleanValue MILK_PROJECTILE_ENABLED;
    public static final ModConfigSpec.IntValue COW_HEALTH;
    public static final ModConfigSpec.IntValue COW_ATTACK_DAMAGE;
    public static final ModConfigSpec.IntValue DETECTION_RANGE;
    public static final ModConfigSpec.IntValue CHARGE_COOLDOWN_TICKS;

    // --- Utility ---
    public static final ModConfigSpec.BooleanValue ENCHANTED_MILK_ENABLED;
    public static final ModConfigSpec.BooleanValue RARE_DROPS_ENABLED;
    public static final ModConfigSpec.BooleanValue PASSIVE_AURA_ENABLED;
    public static final ModConfigSpec.IntValue AURA_RANGE;
    public static final ModConfigSpec.IntValue DROP_INTERVAL_TICKS;

    // --- Chaos ---
    public static final ModConfigSpec.BooleanValue SIZE_CHANGE_ENABLED;
    public static final ModConfigSpec.BooleanValue EXPLOSION_ENABLED;
    public static final ModConfigSpec.BooleanValue MOON_JUMP_ENABLED;
    public static final ModConfigSpec.IntValue SIZE_CHANGE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue EXPLOSION_INTERVAL_TICKS;
    public static final ModConfigSpec.DoubleValue EXPLOSION_POWER;
    public static final ModConfigSpec.IntValue MOON_JUMP_INTERVAL_TICKS;

    // --- Loot ---
    public static final ModConfigSpec.IntValue MOOCOW_MULTIPLIER;
    public static final ModConfigSpec.IntValue VANILLA_COW_LOOT_MULTIPLIER;

    // --- Rebellion & Guardian ---
    public static final ModConfigSpec.BooleanValue REBELLION_ENABLED;
    public static final ModConfigSpec.IntValue REBELLION_DURATION_TICKS;
    public static final ModConfigSpec.IntValue REBELLION_RANGE;
    public static final ModConfigSpec.BooleanValue GUARDIAN_ENABLED;
    public static final ModConfigSpec.IntValue GUARDIAN_DURATION_TICKS;
    public static final ModConfigSpec.IntValue GUARDIAN_RANGE;

    // --- Rarity Weights ---
    public static final ModConfigSpec.IntValue RARITY_COMMON_WEIGHT;
    public static final ModConfigSpec.IntValue RARITY_UNCOMMON_WEIGHT;
    public static final ModConfigSpec.IntValue RARITY_RARE_WEIGHT;
    public static final ModConfigSpec.IntValue RARITY_LEGENDARY_WEIGHT;
    public static final ModConfigSpec.IntValue RARITY_MYTHIC_WEIGHT;

    static final ModConfigSpec SPEC;

    static {
        BUILDER.push("activation");
        ACTIVATION_MODE = BUILDER
                .comment("How cows become OP: ALL_COWS, ITEM_ACTIVATED, or RARE_SPAWN.",
                         "NOTE: Config files persist between mod updates. If you changed this default",
                         "in a previous version, delete mooofdoom-common.toml to pick up new defaults.")
                .defineEnum("mode", ActivationMode.ITEM_ACTIVATED);
        RARE_SPAWN_CHANCE = BUILDER
                .comment("Chance (0.0-1.0) for a cow to spawn as OP in RARE_SPAWN mode")
                .defineInRange("rareSpawnChance", 0.05, 0.0, 1.0);
        MYTHIC_SPAWN_CHANCE = BUILDER
                .comment("Chance (0.0-1.0) for a cow to naturally spawn as OP regardless of mode")
                .defineInRange("mythicSpawnChance", 0.01, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("combat");
        CHARGE_ATTACK_ENABLED = BUILDER
                .comment("Enable charge attack")
                .define("chargeAttackEnabled", true);
        MILK_PROJECTILE_ENABLED = BUILDER
                .comment("Enable ranged milk projectile attack")
                .define("milkProjectileEnabled", true);
        COW_HEALTH = BUILDER
                .comment("OP cow max health (vanilla cow is 10)")
                .defineInRange("health", 100, 10, 1000);
        COW_ATTACK_DAMAGE = BUILDER
                .comment("OP cow melee attack damage")
                .defineInRange("attackDamage", 10, 1, 100);
        DETECTION_RANGE = BUILDER
                .comment("Range in blocks to detect hostile mobs")
                .defineInRange("detectionRange", 24, 8, 64);
        CHARGE_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown between charge attacks in ticks (20 ticks = 1 second)")
                .defineInRange("chargeCooldownTicks", 100, 20, 600);
        BUILDER.pop();

        BUILDER.push("utility");
        ENCHANTED_MILK_ENABLED = BUILDER
                .comment("Enable enchanted milk from OP cows")
                .define("enchantedMilkEnabled", true);
        RARE_DROPS_ENABLED = BUILDER
                .comment("Enable periodic rare item drops")
                .define("rareDropsEnabled", true);
        PASSIVE_AURA_ENABLED = BUILDER
                .comment("Enable passive aura buffs for nearby players")
                .define("passiveAuraEnabled", true);
        AURA_RANGE = BUILDER
                .comment("Range in blocks for passive aura buffs")
                .defineInRange("auraRange", 10, 3, 32);
        DROP_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between rare item drops (6000 = ~5 minutes)")
                .defineInRange("dropIntervalTicks", 6000, 200, 72000);
        BUILDER.pop();

        BUILDER.push("chaos");
        SIZE_CHANGE_ENABLED = BUILDER
                .comment("Enable random size changes")
                .define("sizeChangeEnabled", true);
        EXPLOSION_ENABLED = BUILDER
                .comment("Enable random explosions")
                .define("explosionEnabled", true);
        MOON_JUMP_ENABLED = BUILDER
                .comment("Enable random moon jumps")
                .define("moonJumpEnabled", true);
        SIZE_CHANGE_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between size changes (2400 = ~2 minutes)")
                .defineInRange("sizeChangeIntervalTicks", 2400, 200, 12000);
        EXPLOSION_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between random explosions (3600 = ~3 minutes)")
                .defineInRange("explosionIntervalTicks", 3600, 200, 72000);
        EXPLOSION_POWER = BUILDER
                .comment("Explosion power (TNT is 4.0, creeper is 3.0)")
                .defineInRange("explosionPower", 2.0, 0.5, 6.0);
        MOON_JUMP_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between moon jumps (6000 = ~5 minutes)")
                .defineInRange("moonJumpIntervalTicks", 6000, 200, 12000);
        BUILDER.pop();

        BUILDER.push("loot");
        MOOCOW_MULTIPLIER = BUILDER
                .comment("Max loot multiplier for 1-hit OP cow kills (10x HP = 10x loot)")
                .defineInRange("moocowMultiplier", 10, 1, 100);
        VANILLA_COW_LOOT_MULTIPLIER = BUILDER
                .comment("Loot multiplier for vanilla (non-OP) cow kills")
                .defineInRange("vanillaCowLootMultiplier", 2, 1, 10);
        BUILDER.pop();

        BUILDER.push("effects");
        REBELLION_ENABLED = BUILDER
                .comment("Enable Rebellion of the Cows debuff")
                .define("rebellionEnabled", true);
        REBELLION_DURATION_TICKS = BUILDER
                .comment("Duration of Rebellion debuff in ticks (2400 = 2 minutes)")
                .defineInRange("rebellionDurationTicks", 2400, 200, 12000);
        REBELLION_RANGE = BUILDER
                .comment("Range in blocks for Rebellion/Guardian effects")
                .defineInRange("rebellionRange", 16, 4, 64);
        GUARDIAN_ENABLED = BUILDER
                .comment("Enable Guardian of the Cows buff")
                .define("guardianEnabled", true);
        GUARDIAN_DURATION_TICKS = BUILDER
                .comment("Duration of Guardian buff in ticks (2400 = 2 minutes)")
                .defineInRange("guardianDurationTicks", 2400, 200, 12000);
        GUARDIAN_RANGE = BUILDER
                .comment("Range in blocks for Guardian buff cow follow/defend")
                .defineInRange("guardianRange", 16, 4, 64);
        BUILDER.pop();

        BUILDER.push("rarity");
        RARITY_COMMON_WEIGHT = BUILDER
                .comment("Weight for Common tier rolls")
                .defineInRange("commonWeight", 50, 0, 1000);
        RARITY_UNCOMMON_WEIGHT = BUILDER
                .comment("Weight for Uncommon tier rolls")
                .defineInRange("uncommonWeight", 30, 0, 1000);
        RARITY_RARE_WEIGHT = BUILDER
                .comment("Weight for Rare tier rolls")
                .defineInRange("rareWeight", 15, 0, 1000);
        RARITY_LEGENDARY_WEIGHT = BUILDER
                .comment("Weight for Legendary tier rolls")
                .defineInRange("legendaryWeight", 4, 0, 1000);
        RARITY_MYTHIC_WEIGHT = BUILDER
                .comment("Weight for Mythic tier rolls")
                .defineInRange("mythicWeight", 1, 0, 1000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public enum ActivationMode {
        ALL_COWS,
        ITEM_ACTIVATED,
        RARE_SPAWN
    }
}
