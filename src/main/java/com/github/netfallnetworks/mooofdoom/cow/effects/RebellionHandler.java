package com.github.netfallnetworks.mooofdoom.cow.effects;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import com.github.netfallnetworks.mooofdoom.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public class RebellionHandler {

    private static final String REBEL_TAG = "MooOfDoom_Rebel";

    /**
     * Trigger rebellion when a player attacks any cow.
     */
    @SubscribeEvent
    public static void onCowHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!ModConfig.REBELLION_ENABLED.getAsBoolean()) return;

        if (event.getSource().getEntity() instanceof Player player) {
            applyRebellion(player);
        }
    }

    /**
     * Trigger rebellion when a player kills a cow:
     * - Always if the killed cow is OP
     * - If a vanilla cow dies within range of an OP cow
     */
    @SubscribeEvent
    public static void onCowDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Cow deadCow)) return;
        if (deadCow.level().isClientSide()) return;
        if (!ModConfig.REBELLION_ENABLED.getAsBoolean()) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        if (OpCowManager.isOpCow(deadCow)) {
            // Killing an OP cow always triggers rebellion
            applyRebellion(player);
            return;
        }

        // Killing a vanilla cow near an OP cow triggers rebellion
        int range = ModConfig.REBELLION_RANGE.getAsInt();
        AABB searchBox = deadCow.getBoundingBox().inflate(range);
        List<Cow> nearbyCows = deadCow.level().getEntitiesOfClass(Cow.class, searchBox);
        for (Cow nearby : nearbyCows) {
            if (OpCowManager.isOpCow(nearby)) {
                applyRebellion(player);
                return;
            }
        }
    }

    /**
     * Tick behavior: make cows near rebellious players aggressive toward them.
     */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!ModConfig.REBELLION_ENABLED.getAsBoolean()) return;

        // Only check every 20 ticks (1 second) for performance
        if (player.tickCount % 20 != 0) return;

        boolean hasRebellion = player.hasEffect(ModEffects.REBELLION);

        if (hasRebellion) {
            int range = ModConfig.REBELLION_RANGE.getAsInt();
            AABB searchBox = player.getBoundingBox().inflate(range);
            List<Cow> nearbyCows = player.level().getEntitiesOfClass(Cow.class, searchBox);

            for (Cow cow : nearbyCows) {
                if (OpCowManager.isOpCow(cow)) {
                    // OP cows: just set target to the player
                    cow.setTarget(player);
                } else {
                    // Vanilla cows: add temporary combat goals and target the player
                    if (!cow.getTags().contains(REBEL_TAG)) {
                        cow.addTag(REBEL_TAG);
                        cow.goalSelector.addGoal(1, new MeleeAttackGoal(cow, 1.2, true));
                        cow.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(cow, Player.class, true));
                    }
                    cow.setTarget(player);
                }
            }
        } else {
            // Clean up rebel cows near this player that no longer need to be aggressive
            // We do a wider range check to catch cows that were made rebels
            int range = ModConfig.REBELLION_RANGE.getAsInt() + 8;
            AABB searchBox = player.getBoundingBox().inflate(range);
            List<Cow> nearbyCows = player.level().getEntitiesOfClass(Cow.class, searchBox);

            for (Cow cow : nearbyCows) {
                if (cow.getTags().contains(REBEL_TAG) && !OpCowManager.isOpCow(cow)) {
                    // Check if any nearby player still has rebellion
                    boolean anyRebellion = false;
                    List<Player> nearbyPlayers = cow.level().getEntitiesOfClass(Player.class,
                            cow.getBoundingBox().inflate(ModConfig.REBELLION_RANGE.getAsInt()));
                    for (Player p : nearbyPlayers) {
                        if (p.hasEffect(ModEffects.REBELLION)) {
                            anyRebellion = true;
                            break;
                        }
                    }
                    if (!anyRebellion) {
                        cow.removeTag(REBEL_TAG);
                        cow.setTarget(null);
                        // Goals will naturally stop when target is null
                    }
                }
            }
        }
    }

    public static void applyRebellion(Player player) {
        if (!ModConfig.REBELLION_ENABLED.getAsBoolean()) return;
        int duration = ModConfig.REBELLION_DURATION_TICKS.getAsInt();
        player.addEffect(new MobEffectInstance(ModEffects.REBELLION, duration, 0, false, true, true));
        MooOfDoom.LOGGER.debug("Applied Rebellion to player {}", player.getName().getString());

        // Advancement: got rebellion
        if (player instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.GET_REBELLION.get().trigger(serverPlayer);
        }
    }
}
