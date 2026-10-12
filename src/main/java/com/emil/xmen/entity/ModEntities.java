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

    public static final Supplier<EntityType<MutantNpc>> MUTANT = ENTITY_TYPES.register("mutant",
            () -> EntityType.Builder.of(MutantNpc::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .clientTrackingRange(10).build("mutant"));

    public static final Supplier<EntityType<Blackbird>> BLACKBIRD = ENTITY_TYPES.register("blackbird",
            () -> EntityType.Builder.<Blackbird>of(Blackbird::new, MobCategory.MISC).sized(3.0f, 1.6f)
                    .clientTrackingRange(10).updateInterval(1).build("blackbird"));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CLONE.get(), PowerClone.createAttributes().build());
        event.put(MUTANT.get(), MutantNpc.createAttributes().build());
    }
}
