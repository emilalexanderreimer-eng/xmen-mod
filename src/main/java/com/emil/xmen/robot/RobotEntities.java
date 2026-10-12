package com.emil.xmen.robot;

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

/** Sentinels and the Danger Room's training robots and drones, with their spawn eggs. */
public final class RobotEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, XMenMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(XMenMod.MOD_ID);

    public static final Supplier<EntityType<Sentinel>> SENTINEL = ENTITY_TYPES.register("sentinel",
            () -> EntityType.Builder.of(Sentinel::new, MobCategory.MONSTER).sized(1.6f, 4.3f).eyeHeight(3.85f)
                    .fireImmune().clientTrackingRange(12).build("sentinel"));
    public static final Supplier<EntityType<TrainingRobot>> TRAINING_ROBOT = ENTITY_TYPES.register("training_robot",
            () -> EntityType.Builder.of(TrainingRobot::new, MobCategory.MONSTER).sized(0.7f, 2.0f).eyeHeight(1.75f)
                    .clientTrackingRange(10).build("training_robot"));
    public static final Supplier<EntityType<LaserDrone>> LASER_DRONE = ENTITY_TYPES.register("laser_drone",
            () -> EntityType.Builder.of(LaserDrone::new, MobCategory.MONSTER).sized(0.9f, 0.6f).eyeHeight(0.3f)
                    .clientTrackingRange(10).build("laser_drone"));

    public static final DeferredItem<Item> SENTINEL_EGG = ITEMS.register("sentinel_spawn_egg",
            () -> new DeferredSpawnEggItem(SENTINEL, 0x6b2a7a, 0xb0b4bb, new Item.Properties()));
    public static final DeferredItem<Item> TRAINING_ROBOT_EGG = ITEMS.register("training_robot_spawn_egg",
            () -> new DeferredSpawnEggItem(TRAINING_ROBOT, 0xb9c0c9, 0x3fa9ff, new Item.Properties()));
    public static final DeferredItem<Item> LASER_DRONE_EGG = ITEMS.register("laser_drone_spawn_egg",
            () -> new DeferredSpawnEggItem(LASER_DRONE, 0x3a3f47, 0xff3030, new Item.Properties()));

    private RobotEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(RobotEntities::registerAttributes);
        modEventBus.addListener(RobotEntities::addToCreativeTabs);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SENTINEL.get(), Sentinel.createAttributes().build());
        event.put(TRAINING_ROBOT.get(), TrainingRobot.createAttributes().build());
        event.put(LASER_DRONE.get(), LaserDrone.createAttributes().build());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(SENTINEL_EGG.get());
            event.accept(TRAINING_ROBOT_EGG.get());
            event.accept(LASER_DRONE_EGG.get());
        }
    }
}
