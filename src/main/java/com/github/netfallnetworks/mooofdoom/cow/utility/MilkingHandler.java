package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.rarity.TieredRandom;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class MilkingHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!ModConfig.ENCHANTED_MILK_ENABLED.getAsBoolean()) return;
        if (!(event.getTarget() instanceof Cow cow)) return;
        if (!event.getItemStack().is(Items.BUCKET)) return;

        // Check if this is an OP cow or a companion cow (morphed player)
        boolean isOpCow = OpCowManager.isOpCow(cow);
        boolean isCompanionCow = cow.getTags().contains("MooOfDoom_Companion");

        if (!isOpCow && !isCompanionCow) return;

        // Replace normal milking with tiered buff bucket
        event.setCanceled(true);

        Item bucketItem = rollBuffBucket(cow);

        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        event.getEntity().getInventory().add(new ItemStack(bucketItem));
        cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);

        // Extra sparkle effect when milking a morphed player
        if (isCompanionCow && cow.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cow.getX(), cow.getY() + 1, cow.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private static Item rollBuffBucket(Cow cow) {
        RarityTier tier = TieredRandom.roll(cow.getRandom());
        return switch (tier) {
            case COMMON -> ModItems.BUCKET_OF_SPEED.get();
            case UNCOMMON -> ModItems.BUCKET_OF_REGENERATION.get();
            case RARE -> ModItems.BUCKET_OF_STRENGTH.get();
            case LEGENDARY -> ModItems.BUCKET_OF_FIRE_RESISTANCE.get();
            case MYTHIC -> ModItems.BUCKET_OF_LUCK.get();
        };
    }
}
