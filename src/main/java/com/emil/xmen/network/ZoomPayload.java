package com.emil.xmen.network;

import com.emil.xmen.XMenMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: zoom the view in (Eagle Eyes) or back out. */
public record ZoomPayload(boolean zoomed) implements CustomPacketPayload {
    public static final Type<ZoomPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "zoom"));
    public static final StreamCodec<ByteBuf, ZoomPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ZoomPayload::zoomed, ZoomPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
