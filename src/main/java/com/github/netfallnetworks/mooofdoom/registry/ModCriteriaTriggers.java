package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers custom advancement criterion triggers for mod-specific events.
 */
public class ModCriteriaTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, MooOfDoom.MODID);

    public static final Supplier<ModSimpleTrigger> CREATE_OP_COW =
            TRIGGERS.register("create_op_cow", ModSimpleTrigger::new);

    public static final Supplier<ModSimpleTrigger> GET_REBELLION =
            TRIGGERS.register("get_rebellion", ModSimpleTrigger::new);

    public static final Supplier<ModSimpleTrigger> GET_GUARDIAN =
            TRIGGERS.register("get_guardian", ModSimpleTrigger::new);

    public static final Supplier<ModSimpleTrigger> KILL_OP_COW =
            TRIGGERS.register("kill_op_cow", ModSimpleTrigger::new);

    public static final Supplier<ModSimpleTrigger> COW_MORPH =
            TRIGGERS.register("cow_morph", ModSimpleTrigger::new);

    public static final Supplier<ModSimpleTrigger> CONVERT_HOSTILE =
            TRIGGERS.register("convert_hostile", ModSimpleTrigger::new);
}
