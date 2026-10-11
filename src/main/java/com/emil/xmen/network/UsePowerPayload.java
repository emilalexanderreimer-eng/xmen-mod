package com.emil.xmen.network;

import com.emil.xmen.XMenMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the player pressed the power key. */
public record UsePowerPayload() implements CustomPacketPayload {
    public static final UsePowerPayload INSTANCE = new UsePowerPayload();
    public static final Type<UsePowerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "use_power"));
    public static final StreamCodec<ByteBuf, UsePowerPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
