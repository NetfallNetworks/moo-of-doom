package com.github.netfallnetworks.mooofdoom.cow.effects;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.cow.behavior.RebellionBehavior;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import com.github.netfallnetworks.mooofdoom.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public class RebellionHandler {

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
                    // Vanilla cows: activate tag-gated rebel combat AI
                    RebellionBehavior.INSTANCE.activate(cow);
                    cow.setTarget(player);
                }
            }
        } else {
            // Clean up rebel cows near this player that no longer need to be aggressive
            // We do a wider range check to catch cows that were made rebels
            int range = ModConfig.REBELLION_RANGE.getAsInt();
            int searchRange = range + 8;
            AABB searchBox = player.getBoundingBox().inflate(searchRange);
            List<Cow> nearbyCows = player.level().getEntitiesOfClass(Cow.class, searchBox);
            if (nearbyCows.isEmpty()) return;

            // One query per pass instead of one per cow (issue #22): every player with
            // Rebellion who could be within `range` of any cow in the search box
            List<Player> rebelliousPlayers = player.level().getEntitiesOfClass(Player.class,
                    player.getBoundingBox().inflate(searchRange + range),
                    p -> p.hasEffect(ModEffects.REBELLION));

            for (Cow cow : nearbyCows) {
                if (RebellionBehavior.INSTANCE.isActive(cow) && !OpCowManager.isOpCow(cow)) {
                    AABB cowRange = cow.getBoundingBox().inflate(range);
                    boolean anyRebellion = false;
                    for (Player p : rebelliousPlayers) {
                        if (cowRange.intersects(p.getBoundingBox())) {
                            anyRebellion = true;
                            break;
                        }
                    }
                    if (!anyRebellion) {
                        RebellionBehavior.INSTANCE.deactivate(cow);
                        cow.setTarget(null);
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
