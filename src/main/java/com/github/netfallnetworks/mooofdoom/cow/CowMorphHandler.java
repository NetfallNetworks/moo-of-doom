package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the Mythic cow morph: player becomes invisible with a companion cow that
 * follows their position. Other players see a cow instead of the player.
 */
public class CowMorphHandler {

    // Track morphed players: player UUID -> companion cow entity ID
    private static final Map<UUID, Integer> morphedPlayers = new HashMap<>();

    public static final String COMPANION_TAG = "MooOfDoom_Companion";

    public static void startMorph(Player player, int durationTicks) {
        if (!(player.level() instanceof ServerLevel level)) return;

        // Discard any previous companion (e.g. orphan from a re-morph) before spawning
        Integer existingId = morphedPlayers.remove(player.getUUID());
        if (existingId != null) {
            Entity existing = level.getEntity(existingId);
            if (existing != null) {
                existing.discard();
            }
        }

        // Spawn companion cow at player position
        Cow cow = new Cow(EntityType.COW, level);
        cow.setPos(player.position());
        cow.setYRot(player.getYRot());
        cow.setInvulnerable(true);
        cow.setNoAi(true);
        cow.setSilent(true);
        cow.addTag(COMPANION_TAG);

        // Register BEFORE addFreshEntity: spawning fires EntityJoinLevelEvent synchronously,
        // and OpCowManager's companion guard discards companions it can't find here.
        morphedPlayers.put(player.getUUID(), cow.getId());
        level.addFreshEntity(cow);

        // Particle effects
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1, player.getZ(),
                50, 0.5, 1.0, 0.5, 0.2);
        player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);

        // Advancement: mythic cow morph
        if (player instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.COW_MORPH.get().trigger(serverPlayer);
        }

        MooOfDoom.LOGGER.info("Player {} morphed into a cow!", player.getName().getString());
    }

    /** True if this entity ID is a companion cow currently managed by a live morph. */
    public static boolean isManagedCompanion(int entityId) {
        return morphedPlayers.containsValue(entityId);
    }

    /**
     * End the morph on disconnect — otherwise the player entity stops ticking and the
     * invulnerable companion cow is stranded in the world forever (issue #20).
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (isMorphed(player)) {
            endMorph(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        Integer cowId = morphedPlayers.get(player.getUUID());
        if (cowId == null) return;

        // Check if morph ended (invisibility expired)
        if (!player.hasEffect(MobEffects.INVISIBILITY)) {
            endMorph(player);
            return;
        }

        // Sync companion cow position to player
        Entity cow = player.level().getEntity(cowId);
        if (cow == null || !cow.isAlive()) {
            endMorph(player);
            return;
        }

        cow.setPos(player.position());
        cow.setYRot(player.getYRot());
        cow.setXRot(player.getXRot());
    }

    public static void endMorph(Player player) {
        Integer cowId = morphedPlayers.remove(player.getUUID());
        if (cowId != null && player.level() instanceof ServerLevel level) {
            Entity cow = level.getEntity(cowId);
            if (cow != null && cow.isAlive()) {
                cow.discard();
            }
        }
        player.removeEffect(MobEffects.INVISIBILITY);

        // Re-appearance effects
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 1, player.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
        }
        player.playSound(SoundEvents.COW_AMBIENT, 1.0F, 0.5F);
        MooOfDoom.LOGGER.info("Player {} morph ended.", player.getName().getString());
    }

    public static boolean isMorphed(Player player) {
        return morphedPlayers.containsKey(player.getUUID());
    }
}
