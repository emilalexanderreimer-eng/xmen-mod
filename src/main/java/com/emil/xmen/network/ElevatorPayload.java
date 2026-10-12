package com.emil.xmen.network;

import com.emil.xmen.XMenMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the player jumped (up) or sneaked (down) while standing on an elevator. */
public record ElevatorPayload(boolean up) implements CustomPacketPayload {
    public static final Type<ElevatorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "elevator"));
    public static final StreamCodec<ByteBuf, ElevatorPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ElevatorPayload::new, ElevatorPayload::up);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
