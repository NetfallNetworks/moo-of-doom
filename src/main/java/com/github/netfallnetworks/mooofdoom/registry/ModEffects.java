package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MooOfDoom.MODID);

    // Rebellion: harmful debuff - cows attack the player (red/orange color: 0xCC4400)
    public static final DeferredHolder<MobEffect, MobEffect> REBELLION =
            MOB_EFFECTS.register("rebellion",
                    () -> new MobEffect(MobEffectCategory.HARMFUL, 0xCC4400) {});

    // Guardian: beneficial buff - cows follow and defend the player (blue color: 0x3399FF)
    public static final DeferredHolder<MobEffect, MobEffect> GUARDIAN =
            MOB_EFFECTS.register("guardian",
                    () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x3399FF) {});
}
