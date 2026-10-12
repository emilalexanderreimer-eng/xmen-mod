package com.emil.xmen.robot.client;

import com.emil.xmen.robot.RobotEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup for the Sentinels and the Danger Room robots. */
public final class RobotClient {
    private RobotClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RobotClient::registerRenderers);
        modEventBus.addListener(RobotClient::registerLayers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RobotEntities.SENTINEL.get(), context -> new RobotRenderer<>(context,
                new SentinelModel(context.bakeLayer(SentinelModel.LAYER)), 1.2f, "sentinel"));
        event.registerEntityRenderer(RobotEntities.TRAINING_ROBOT.get(), context -> new RobotRenderer<>(context,
                new TrainingRobotModel(context.bakeLayer(TrainingRobotModel.LAYER)), 0.5f, "training_robot"));
        event.registerEntityRenderer(RobotEntities.LASER_DRONE.get(), context -> new RobotRenderer<>(context,
                new LaserDroneModel(context.bakeLayer(LaserDroneModel.LAYER)), 0.4f, "laser_drone"));
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SentinelModel.LAYER, SentinelModel::createLayer);
        event.registerLayerDefinition(TrainingRobotModel.LAYER, TrainingRobotModel::createLayer);
        event.registerLayerDefinition(LaserDroneModel.LAYER, LaserDroneModel::createLayer);
    }
}
