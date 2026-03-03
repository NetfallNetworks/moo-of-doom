package com.github.netfallnetworks.mooofdoom;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import com.github.netfallnetworks.mooofdoom.cow.CowMorphHandler;
import com.github.netfallnetworks.mooofdoom.cow.DoomAppleUseHandler;
import com.github.netfallnetworks.mooofdoom.cow.MobConversionHandler;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.cow.effects.GuardianHandler;
import com.github.netfallnetworks.mooofdoom.cow.effects.OpCowDeathHandler;
import com.github.netfallnetworks.mooofdoom.cow.effects.RebellionHandler;
import com.github.netfallnetworks.mooofdoom.cow.utility.CombatLootHandler;
import com.github.netfallnetworks.mooofdoom.cow.utility.MilkingHandler;
import com.github.netfallnetworks.mooofdoom.cow.utility.LootDropHandler;
import com.github.netfallnetworks.mooofdoom.cow.utility.AuraHandler;
import com.github.netfallnetworks.mooofdoom.cow.chaos.SizeChangeHandler;
import com.github.netfallnetworks.mooofdoom.cow.chaos.ExplosionHandler;
import com.github.netfallnetworks.mooofdoom.cow.chaos.MoonJumpHandler;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import com.github.netfallnetworks.mooofdoom.registry.ModEffects;
import com.github.netfallnetworks.mooofdoom.registry.ModEntityTypes;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MooOfDoom.MODID)
public class MooOfDoom {
    public static final String MODID = "mooofdoom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Moo of Doom loading...");

        // Register deferred registers
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModCriteriaTriggers.TRIGGERS.register(modEventBus);

        // Register mod configuration
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);

        // Add ATTACK_DAMAGE attribute to cows so MeleeAttackGoal doesn't crash
        modEventBus.addListener(MooOfDoom::onAttributeModify);

        // Register game event handlers
        NeoForge.EVENT_BUS.register(OpCowManager.class);
        NeoForge.EVENT_BUS.register(DoomAppleUseHandler.class);

        // Utility handlers
        NeoForge.EVENT_BUS.register(MilkingHandler.class);
        NeoForge.EVENT_BUS.register(LootDropHandler.class);
        NeoForge.EVENT_BUS.register(CombatLootHandler.class);
        NeoForge.EVENT_BUS.register(AuraHandler.class);

        // Chaos handlers
        NeoForge.EVENT_BUS.register(SizeChangeHandler.class);
        NeoForge.EVENT_BUS.register(ExplosionHandler.class);
        NeoForge.EVENT_BUS.register(MoonJumpHandler.class);

        // Effect handlers (Rebellion, Guardian, OP Cow Death)
        NeoForge.EVENT_BUS.register(RebellionHandler.class);
        NeoForge.EVENT_BUS.register(GuardianHandler.class);
        NeoForge.EVENT_BUS.register(OpCowDeathHandler.class);

        // Doom Apple conversion handlers
        NeoForge.EVENT_BUS.register(CowMorphHandler.class);
        NeoForge.EVENT_BUS.register(MobConversionHandler.class);
    }

    private static void onAttributeModify(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.COW, Attributes.ATTACK_DAMAGE)) {
            event.add(EntityType.COW, Attributes.ATTACK_DAMAGE);
        }
    }
}
