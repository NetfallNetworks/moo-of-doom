package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.combat.ChargeGoal;
import com.github.netfallnetworks.mooofdoom.cow.combat.HostileTargetGoal;
import com.github.netfallnetworks.mooofdoom.cow.combat.MilkProjectileGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class OpCowManager {

    public static final String OP_TAG = "MooOfDoom";
    private static boolean loggedActivationMode = false;

    public static boolean isOpCow(Cow cow) {
        return cow.getTags().contains(OP_TAG);
    }

    public static void makeOpCow(Cow cow) {
        cow.addTag(OP_TAG);
        boostAttributes(cow);
        cow.setGlowingTag(true);
        addCombatGoals(cow);
    }

    private static void boostAttributes(Cow cow) {
        AttributeInstance healthAttr = cow.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(ModConfig.COW_HEALTH.getAsInt());
            cow.setHealth(cow.getMaxHealth());
        }

        AttributeInstance speedAttr = cow.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.35);
        }

        AttributeInstance kbAttr = cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbAttr.setBaseValue(0.8);
        }

        // Set follow range so targeting goals can detect hostiles at the configured distance
        AttributeInstance followRange = cow.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(ModConfig.DETECTION_RANGE.getAsInt());
        }
    }

    public static void addCombatGoals(Cow cow) {
        cow.targetSelector.addGoal(1, new HostileTargetGoal(cow));

        if (ModConfig.CHARGE_ATTACK_ENABLED.getAsBoolean()) {
            cow.goalSelector.addGoal(2, new ChargeGoal(cow));
        }

        if (ModConfig.MILK_PROJECTILE_ENABLED.getAsBoolean()) {
            cow.goalSelector.addGoal(3, new MilkProjectileGoal(cow));
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Cow cow)) return;
        if (event.getLevel().isClientSide()) return;

        // Log the activation mode once for diagnostics (helps catch stale config files)
        if (!loggedActivationMode) {
            loggedActivationMode = true;
            ModConfig.ActivationMode mode = ModConfig.ACTIVATION_MODE.get();
            MooOfDoom.LOGGER.info("[Moo of Doom] Activation mode: {}. If this is unexpected, delete " +
                    "config/mooofdoom-common.toml to regenerate with current defaults.", mode);
            if (mode == ModConfig.ActivationMode.ALL_COWS) {
                MooOfDoom.LOGGER.warn("[Moo of Doom] ALL_COWS mode is active — every cow will become OP! " +
                        "Set mode = \"ITEM_ACTIVATED\" in mooofdoom-common.toml for normal gameplay.");
            }
        }

        // If already OP, re-apply attributes (they reset on load)
        if (isOpCow(cow)) {
            boostAttributes(cow);
            cow.setGlowingTag(true);
            addCombatGoals(cow);
            return;
        }

        // Determine if this cow should become OP
        ModConfig.ActivationMode mode = ModConfig.ACTIVATION_MODE.get();
        switch (mode) {
            case ALL_COWS -> makeOpCow(cow);
            case RARE_SPAWN -> {
                if (cow.getRandom().nextDouble() < ModConfig.RARE_SPAWN_CHANCE.get()) {
                    makeOpCow(cow);
                }
            }
            case ITEM_ACTIVATED -> {
                // Mythic natural spawns: small chance for OP cows regardless of mode
                if (cow.getRandom().nextDouble() < ModConfig.MYTHIC_SPAWN_CHANCE.get()) {
                    makeOpCow(cow);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (!isOpCow(cow)) return;

        // Immune to explosion damage
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
            return;
        }

        // Immune to fall damage
        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
        }
    }
}
