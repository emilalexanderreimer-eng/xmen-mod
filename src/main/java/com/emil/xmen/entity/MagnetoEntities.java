package com.emil.xmen.entity;

import java.util.function.Supplier;

import com.emil.xmen.XMenMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Magneto (the Asteroid M boss), the metal he throws, and his spawn egg. */
public final class MagnetoEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, XMenMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(XMenMod.MOD_ID);

    public static final Supplier<EntityType<MagnetoBoss>> MAGNETO = ENTITY_TYPES.register("magneto",
            () -> EntityType.Builder.of(MagnetoBoss::new, MobCategory.MONSTER).sized(0.6f, 1.95f)
                    .clientTrackingRange(10).build("magneto"));
    public static final Supplier<EntityType<MagnetoDebris>> DEBRIS = ENTITY_TYPES.register("magneto_debris",
            () -> EntityType.Builder.<MagnetoDebris>of(MagnetoDebris::new, MobCategory.MISC).sized(0.9f, 0.9f)
                    .clientTrackingRange(8).updateInterval(1).noSummon().build("magneto_debris"));

    public static final DeferredItem<Item> MAGNETO_SPAWN_EGG = ITEMS.register("magneto_spawn_egg",
            () -> new DeferredSpawnEggItem(MAGNETO, 0x7a1420, 0x6a2a8a, new Item.Properties()));

    private MagnetoEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(MagnetoEntities::registerAttributes);
        modEventBus.addListener(MagnetoEntities::addToCreativeTabs);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MAGNETO.get(), MagnetoBoss.createAttributes().build());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(MAGNETO_SPAWN_EGG.get());
        }
    }
}
