package net.jfrx.slashblade.maidnativepower.network;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkManager {
    private NetworkManager() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(MaidRankSyncMessage.TYPE, MaidRankSyncMessage.STREAM_CODEC,
                (message, context) -> {
                    Entity entity = context.player().level().getEntity(message.entityId());
                    if (entity instanceof EntityMaid maid) {
                        IConcentrationRank rank = maid.getData(CapabilityConcentrationRank.RANK_POINT);
                        long time = maid.level().getGameTime();
                        int oldRank = rank.getRank(time).level;
                        rank.setRawRankPoint(message.rawPoint());
                        rank.setLastUpdte(time);
                        if (oldRank < rank.getRank(time).level) {
                            rank.setLastRankRise(time);
                        }
                    }
                });
    }
}
