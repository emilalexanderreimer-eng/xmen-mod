package com.emil.xmen.client;

import java.util.UUID;

import com.emil.xmen.entity.PowerClone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Draws power clones with their owner's skin. */
public class CloneRenderer extends HumanoidMobRenderer<PowerClone, PlayerModel<PowerClone>> {
    public CloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(PowerClone clone) {
        UUID owner = clone.getOwnerId();
        if (owner == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(owner);
        return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(owner).texture();
    }
}
