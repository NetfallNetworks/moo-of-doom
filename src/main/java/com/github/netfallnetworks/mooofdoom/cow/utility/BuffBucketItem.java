package com.github.netfallnetworks.mooofdoom.cow.utility;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Holder;

/**
 * A milk bucket that grants a specific buff when consumed.
 * Clears negative effects (like vanilla milk) then applies the buff.
 */
public class BuffBucketItem extends Item {

    private final Holder<MobEffect> effect;
    private final int durationTicks;
    private final int amplifier;

    public BuffBucketItem(Properties properties, Holder<MobEffect> effect, int durationTicks, int amplifier) {
        super(properties);
        this.effect = effect;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // Clear negative effects like regular milk
            player.removeAllEffects();

            // Apply the specific buff
            player.addEffect(new MobEffectInstance(effect, durationTicks, amplifier));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.getInventory().add(new ItemStack(Items.BUCKET));
            }
        }
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
}
