package com.emil.xmen.robot.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.robot.RobotMob;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws a robot with its texture plus a glowing layer for the eyes and lights (textures from tools/robots.py). */
public class RobotRenderer<T extends RobotMob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;

    public RobotRenderer(EntityRendererProvider.Context context, M model, float shadow, String name) {
        super(context, model, shadow);
        texture = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/robot/" + name + ".png");
        RenderType glow = RenderType.eyes(
                ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/robot/" + name + "_glow.png"));
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return glow;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
