package com.github.netfallnetworks.mooofdoom.cow.combat;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class MilkProjectile extends ThrowableItemProjectile {

    public MilkProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public MilkProjectile(Level level, LivingEntity shooter) {
        super(ModEntityTypes.MILK_PROJECTILE.get(), shooter, level, new ItemStack(Items.MILK_BUCKET));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.MILK_BUCKET;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target) {
            target.hurt(damageSources().thrown(this, getOwner()),
                    ModConfig.COW_ATTACK_DAMAGE.getAsInt() / 2.0F);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
        }
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.DRIPPING_WATER,
                    getX(), getY(), getZ(), 0, 0, 0);
        }
    }

}
