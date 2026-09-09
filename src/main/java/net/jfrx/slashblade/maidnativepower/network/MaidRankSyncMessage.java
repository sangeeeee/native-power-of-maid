package net.jfrx.slashblade.maidnativepower.network;

import io.netty.buffer.ByteBuf;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MaidRankSyncMessage(long rawPoint, int entityId) implements CustomPacketPayload {
    public static final Type<MaidRankSyncMessage> TYPE = new Type<>(NativePowerOfMaid.prefix("maid_rank"));
    public static final StreamCodec<ByteBuf, MaidRankSyncMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, MaidRankSyncMessage::rawPoint,
            ByteBufCodecs.VAR_INT, MaidRankSyncMessage::entityId,
            MaidRankSyncMessage::new);

    @Override
    public Type<MaidRankSyncMessage> type() {
        return TYPE;
    }
}
