package com.emil.xmen.network;

import com.emil.xmen.XMenMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: tells the player which power they have, for the on-screen display. */
public record SyncPowerPayload(String powerId) implements CustomPacketPayload {
    public static final Type<SyncPowerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "sync_power"));
    public static final StreamCodec<ByteBuf, SyncPowerPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SyncPowerPayload::powerId, SyncPowerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
