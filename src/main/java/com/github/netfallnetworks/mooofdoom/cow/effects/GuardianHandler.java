package com.github.netfallnetworks.mooofdoom.cow.effects;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import com.github.netfallnetworks.mooofdoom.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public class GuardianHandler {

    private static final String GUARDIAN_TAG = "MooOfDoom_Guardian";

    /**
     * Trigger: Player feeds wheat to any cow -> apply Guardian buff.
     */
    @SubscribeEvent
    public static void onFeedCow(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!ModConfig.GUARDIAN_ENABLED.getAsBoolean()) return;
        if (!(event.getTarget() instanceof Cow)) return;
        if (!event.getItemStack().is(Items.WHEAT)) return;

        Player player = event.getEntity();
        int duration = ModConfig.GUARDIAN_DURATION_TICKS.getAsInt();
        player.addEffect(new MobEffectInstance(ModEffects.GUARDIAN, duration, 0, false, true, true));
        MooOfDoom.LOGGER.debug("Applied Guardian to player {}", player.getName().getString());

        // Advancement: got guardian
        if (player instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.GET_GUARDIAN.get().trigger(serverPlayer);
        }
    }

    /**
     * Tick behavior: cows near guardian players follow them and attack hostiles.
     */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!ModConfig.GUARDIAN_ENABLED.getAsBoolean()) return;

        // Only check every 20 ticks (1 second) for performance
        if (player.tickCount % 20 != 0) return;

        boolean hasGuardian = player.hasEffect(ModEffects.GUARDIAN);

        if (hasGuardian) {
            int range = ModConfig.GUARDIAN_RANGE.getAsInt();
            AABB searchBox = player.getBoundingBox().inflate(range);
            List<Cow> nearbyCows = player.level().getEntitiesOfClass(Cow.class, searchBox);

            for (Cow cow : nearbyCows) {
                // Tag the cow as a guardian follower if not already
                if (!cow.getTags().contains(GUARDIAN_TAG)) {
                    cow.addTag(GUARDIAN_TAG);
                    // Add combat capability for vanilla cows
                    if (!OpCowManager.isOpCow(cow)) {
                        cow.goalSelector.addGoal(1, new MeleeAttackGoal(cow, 1.2, true));
                        cow.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(cow, Monster.class, true));
                    }
                }

                // Navigate cow toward the player
                double distSq = cow.distanceToSqr(player);
                if (distSq > 9.0) { // Only move if more than 3 blocks away
                    cow.getNavigation().moveTo(player, 1.2);
                }

                // Find and target nearby hostile mobs threatening the player
                List<Monster> nearbyMonsters = player.level().getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(range));
                if (!nearbyMonsters.isEmpty()) {
                    // Target the closest monster
                    Monster closest = null;
                    double closestDist = Double.MAX_VALUE;
                    for (Monster monster : nearbyMonsters) {
                        double d = cow.distanceToSqr(monster);
                        if (d < closestDist) {
                            closestDist = d;
                            closest = monster;
                        }
                    }
                    if (closest != null) {
                        cow.setTarget(closest);
                    }
                }
            }
        } else {
            // Clean up guardian cows near this player
            int range = ModConfig.GUARDIAN_RANGE.getAsInt() + 8;
            AABB searchBox = player.getBoundingBox().inflate(range);
            List<Cow> nearbyCows = player.level().getEntitiesOfClass(Cow.class, searchBox);

            for (Cow cow : nearbyCows) {
                if (cow.getTags().contains(GUARDIAN_TAG)) {
                    // Check if any nearby player still has guardian
                    boolean anyGuardian = false;
                    List<Player> nearbyPlayers = cow.level().getEntitiesOfClass(Player.class,
                            cow.getBoundingBox().inflate(ModConfig.GUARDIAN_RANGE.getAsInt()));
                    for (Player p : nearbyPlayers) {
                        if (p.hasEffect(ModEffects.GUARDIAN)) {
                            anyGuardian = true;
                            break;
                        }
                    }
                    if (!anyGuardian) {
                        cow.removeTag(GUARDIAN_TAG);
                        cow.setTarget(null);
                        cow.getNavigation().stop();
                    }
                }
            }
        }
    }
}
