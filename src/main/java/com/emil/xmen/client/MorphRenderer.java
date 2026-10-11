package com.emil.xmen.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Draws a morphed player as their mob instead of a player body. */
final class MorphRenderer {
    private record Look(String type, Entity entity) {
    }

    private static final Map<Integer, Look> CACHE = new HashMap<>();

    private MorphRenderer() {
    }

    static void clear() {
        CACHE.clear();
    }

    private static Entity lookFor(Player player, String typeId) {
        Look look = CACHE.get(player.getId());
        if (look != null && look.type().equals(typeId) && look.entity().level() == player.level()) {
            return look.entity();
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(typeId));
        Entity entity = type.create(player.level());
        if (entity == null) {
            return null;
        }
        CACHE.put(player.getId(), new Look(typeId, entity));
        return entity;
    }

    static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        String typeId = ClientMorphs.get(player.getId());
        if (typeId == null) {
            return;
        }
        Entity entity = lookFor(player, typeId);
        if (entity == null) {
            return;
        }
        event.setCanceled(true);
        entity.setPos(player.getX(), player.getY(), player.getZ());
        entity.xo = player.xo;
        entity.yo = player.yo;
        entity.zo = player.zo;
        entity.xOld = player.xOld;
        entity.yOld = player.yOld;
        entity.zOld = player.zOld;
        entity.setYRot(player.getYRot());
        entity.yRotO = player.yRotO;
        entity.setXRot(player.getXRot());
        entity.xRotO = player.xRotO;
        entity.tickCount = player.tickCount;
        entity.setOnGround(player.onGround());
        entity.setInvisible(player.isInvisible());
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = player.yBodyRot;
            living.yBodyRotO = player.yBodyRotO;
            living.yHeadRot = player.yHeadRot;
            living.yHeadRotO = player.yHeadRotO;
            living.hurtTime = player.hurtTime;
            living.deathTime = player.deathTime;
            living.swinging = player.swinging;
            living.attackAnim = player.attackAnim;
            living.oAttackAnim = player.oAttackAnim;
        }
        float scale = Mth.clamp(player.getBbHeight() / Math.max(0.2f, entity.getBbHeight()), 0.05f, 4f);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).render(entity,
                Mth.lerp(event.getPartialTick(), player.yRotO, player.getYRot()), event.getPartialTick(), pose,
                event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    /** Keeps the mob's legs moving like the player's. */
    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            CACHE.clear();
            return;
        }
        for (var it = CACHE.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            Entity owner = minecraft.level.getEntity(entry.getKey());
            if (!(owner instanceof Player player) || ClientMorphs.get(entry.getKey()) == null) {
                it.remove();
                continue;
            }
            if (entry.getValue().entity() instanceof LivingEntity living) {
                float moved = (float) Mth.length(player.getX() - player.xo, 0, player.getZ() - player.zo) * 4;
                living.walkAnimation.update(Math.min(moved, 1), 0.4f);
            }
        }
    }
}
