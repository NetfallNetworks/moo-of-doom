package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.config.ModConfigValues;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MilkingHandler {


    /**
     * @return true if the interaction was handled and the event should be canceled
     */
    public static boolean onPlayerInteractEntity(Player player, Entity target, InteractionHand hand, Level level) {
        if (level.isClientSide()) return false;
        if (!ModConfigValues.enchantedMilkEnabled) return false;
        if (!(target instanceof Cow cow)) return false;
        if (!player.getItemInHand(hand).is(Items.BUCKET)) return false;

        // Check if this is an OP cow or a companion cow (morphed player)
        boolean isOpCow = OpCowManager.isOpCow(cow);
        boolean isCompanionCow = cow.getTags().contains("MooOfDoom_Companion");

        if (!isOpCow && !isCompanionCow) return false;

        // Replace normal milking with tiered buff bucket
        Item bucketItem = rollBuffBucket(cow);

        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        player.getInventory().add(new ItemStack(bucketItem));
        cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);

        // Extra sparkle effect when milking a morphed player
        if (isCompanionCow && cow.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cow.getX(), cow.getY() + 1, cow.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
        }
        return true;
    }

    private static Item rollBuffBucket(Cow cow) {
        RarityTier tier = ModConfigValues.rollRarity(cow.getRandom().nextInt(ModConfigValues.rarityTotalWeight()));
        return switch (tier) {
            case COMMON -> ModItems.BUCKET_OF_SPEED.get();
            case UNCOMMON -> ModItems.BUCKET_OF_REGENERATION.get();
            case RARE -> ModItems.BUCKET_OF_STRENGTH.get();
            case LEGENDARY -> ModItems.BUCKET_OF_FIRE_RESISTANCE.get();
            case MYTHIC -> ModItems.BUCKET_OF_LUCK.get();
        };
    }
}
