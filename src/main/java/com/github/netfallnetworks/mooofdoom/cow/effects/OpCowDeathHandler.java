package com.github.netfallnetworks.mooofdoom.cow.effects;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class OpCowDeathHandler {

    @SubscribeEvent
    public static void onOpCowDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;

        ServerLevel serverLevel = (ServerLevel) cow.level();

        // Dramatic death effects: totem-pop style particles (50+ particles)
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                cow.getX(), cow.getY() + 1.0, cow.getZ(),
                60, 0.8, 1.5, 0.8, 0.3);

        // Additional particle burst for extra drama
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                cow.getX(), cow.getY() + 0.5, cow.getZ(),
                3, 0.5, 0.5, 0.5, 0.0);

        // Layered sounds audible to ~48 blocks
        // Explosion sound (loud, long range)
        serverLevel.playSound(null, cow.getX(), cow.getY(), cow.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
                4.0F, 0.7F);

        // Cow death sound — louder and higher pitch so it's audible over the explosion
        serverLevel.playSound(null, cow.getX(), cow.getY(), cow.getZ(),
                SoundEvents.COW_DEATH, SoundSource.HOSTILE,
                4.0F, 0.8F);

        // Totem sound for the dramatic flair
        serverLevel.playSound(null, cow.getX(), cow.getY(), cow.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.HOSTILE,
                3.0F, 1.0F);

        // Trigger rebellion on the killer and grant kill advancement
        if (event.getSource().getEntity() instanceof Player killer) {
            RebellionHandler.applyRebellion(killer);
            if (killer instanceof ServerPlayer serverPlayer) {
                ModCriteriaTriggers.KILL_OP_COW.get().trigger(serverPlayer);
            }
            MooOfDoom.LOGGER.info("OP cow killed by {}! Rebellion triggered.",
                    killer.getName().getString());
        }
    }
}
