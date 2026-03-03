package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles hostile mob conversion when fed a Doom Apple.
 * Also manages the "following protector" tick behavior.
 */
public class MobConversionHandler {

    // Track protector mobs: mob entity ID -> protector data
    private static final Map<Integer, ProtectorData> protectors = new HashMap<>();

    record ProtectorData(UUID ownerUUID, long expireTime) {}

    public static void applyHostileConversion(Mob mob, Player player, RarityTier tier) {
        MooOfDoom.LOGGER.info("Doom Apple fed to {} — rolled {}", mob.getType().getDescriptionId(), tier);
        switch (tier) {
            case COMMON, UNCOMMON -> applyFollowingProtector(mob, player);
            case RARE -> applyExplosion(mob);
            case LEGENDARY -> convertToCow(mob, false);
            case MYTHIC -> convertToCow(mob, true);
        }
    }

    private static void applyFollowingProtector(Mob mob, Player player) {
        // Cancel hostility toward the player
        mob.setTarget(null);

        // Add combat goals for protecting the player
        if (mob instanceof PathfinderMob pathfinderMob) {
            mob.goalSelector.addGoal(1, new MeleeAttackGoal(pathfinderMob, 1.2, true));
        }
        mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Monster.class, true));

        // Track as protector with 30s expiry
        protectors.put(mob.getId(), new ProtectorData(player.getUUID(), mob.tickCount + 600));

        // Visual effects
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    mob.getX(), mob.getY() + 1, mob.getZ(),
                    15, 0.5, 0.5, 0.5, 0.1);
        }
        mob.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    private static void applyExplosion(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;

        // Visual explosion (no block damage)
        level.explode(mob, mob.getX(), mob.getY() + 0.5, mob.getZ(),
                2.0F, Level.ExplosionInteraction.NONE);

        // Kill the mob (triggers natural death drops)
        mob.kill(level);
    }

    private static void convertToCow(Mob mob, boolean makeOp) {
        if (!(mob.level() instanceof ServerLevel level)) return;

        Cow cow = mob.convertTo(EntityType.COW,
                ConversionParams.single(mob, false, false),
                newCow -> {
                    if (makeOp) {
                        OpCowManager.makeOpCow(newCow);
                    }
                });

        if (cow != null) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    cow.getX(), cow.getY() + 1, cow.getZ(),
                    30, 0.5, 1.0, 0.5, 0.2);
            cow.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        }
    }

    /**
     * Tick handler: make protector mobs follow their owner and expire after 30s.
     */
    @SubscribeEvent
    public static void onMobTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;

        ProtectorData data = protectors.get(mob.getId());
        if (data == null) return;

        // Check expiry
        if (mob.tickCount >= data.expireTime || !mob.isAlive()) {
            protectors.remove(mob.getId());
            mob.setTarget(null);
            return;
        }

        // Navigate toward owner (every 20 ticks for performance)
        if (mob.tickCount % 20 == 0) {
            ServerLevel level = (ServerLevel) mob.level();
            Player owner = level.getServer().getPlayerList().getPlayer(data.ownerUUID);
            if (owner != null && owner.level() == mob.level()) {
                double distSq = mob.distanceToSqr(owner);
                if (distSq > 9.0) {
                    mob.getNavigation().moveTo(owner, 1.2);
                }

                // Target nearby hostile mobs threatening the owner
                List<Monster> nearbyMonsters = mob.level().getEntitiesOfClass(Monster.class,
                        owner.getBoundingBox().inflate(16));
                Monster closest = null;
                double closestDist = Double.MAX_VALUE;
                for (Monster monster : nearbyMonsters) {
                    if (monster == mob) continue;
                    if (protectors.containsKey(monster.getId())) continue; // Don't target other protectors
                    double d = mob.distanceToSqr(monster);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = monster;
                    }
                }
                if (closest != null) {
                    mob.setTarget(closest);
                }
            }
        }
    }
}
