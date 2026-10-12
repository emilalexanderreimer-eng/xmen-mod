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

/** Mister Sinister (the boss of his lab), his clones, his energy blasts, and their spawn eggs. */
public final class SinisterEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, XMenMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(XMenMod.MOD_ID);

    public static final Supplier<EntityType<SinisterBoss>> SINISTER = ENTITY_TYPES.register("mister_sinister",
            () -> EntityType.Builder.of(SinisterBoss::new, MobCategory.MONSTER).sized(0.6f, 1.95f)
                    .clientTrackingRange(10).build("mister_sinister"));
    public static final Supplier<EntityType<SinisterClone>> CLONE = ENTITY_TYPES.register("sinister_clone",
            () -> EntityType.Builder.of(SinisterClone::new, MobCategory.MONSTER).sized(0.6f, 1.95f)
                    .clientTrackingRange(10).build("sinister_clone"));
    public static final Supplier<EntityType<SinisterBlast>> BLAST = ENTITY_TYPES.register("sinister_blast",
            () -> EntityType.Builder.<SinisterBlast>of(SinisterBlast::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .clientTrackingRange(8).updateInterval(1).noSummon().build("sinister_blast"));

    public static final DeferredItem<Item> SINISTER_SPAWN_EGG = ITEMS.register("mister_sinister_spawn_egg",
            () -> new DeferredSpawnEggItem(SINISTER, 0x16161c, 0xc4141e, new Item.Properties()));
    public static final DeferredItem<Item> CLONE_SPAWN_EGG = ITEMS.register("sinister_clone_spawn_egg",
            () -> new DeferredSpawnEggItem(CLONE, 0x2a2d33, 0x8a9a8c, new Item.Properties()));

    private SinisterEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(SinisterEntities::registerAttributes);
        modEventBus.addListener(SinisterEntities::addToCreativeTabs);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SINISTER.get(), SinisterBoss.createAttributes().build());
        event.put(CLONE.get(), SinisterClone.createAttributes().build());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(SINISTER_SPAWN_EGG.get());
            event.accept(CLONE_SPAWN_EGG.get());
        }
    }
}
