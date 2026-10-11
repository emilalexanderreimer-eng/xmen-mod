package com.emil.xmen.network;

import com.emil.xmen.XMenMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: player entityId now looks like entityType ("" for normal). */
public record MorphPayload(int entityId, String entityType) implements CustomPacketPayload {
    public static final Type<MorphPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "morph"));
    public static final StreamCodec<ByteBuf, MorphPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MorphPayload::entityId, ByteBufCodecs.STRING_UTF8, MorphPayload::entityType,
            MorphPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
