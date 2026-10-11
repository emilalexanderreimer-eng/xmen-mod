package com.emil.xmen.entity;

import java.util.function.Supplier;

import com.emil.xmen.XMenMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, XMenMod.MOD_ID);

    public static final Supplier<EntityType<PowerClone>> CLONE = ENTITY_TYPES.register("clone",
            () -> EntityType.Builder.of(PowerClone::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10)
                    .build("clone"));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CLONE.get(), PowerClone.createAttributes().build());
    }
}
