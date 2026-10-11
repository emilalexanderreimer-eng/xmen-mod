package com.emil.xmen.network;

import com.emil.xmen.XMenMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the power key is on cooldown for this many ticks. */
public record CooldownPayload(int ticks) implements CustomPacketPayload {
    public static final Type<CooldownPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "cooldown"));
    public static final StreamCodec<ByteBuf, CooldownPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, CooldownPayload::ticks, CooldownPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
