package net.jfrx.slashblade.maidnativepower.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.mrqx.sbr_core.events.StunEvent;
import net.jfrx.slashblade.maidnativepower.item.SlashBladeMaidBauble;

@EventBusSubscriber(modid = "native_power_of_maid")
public class StunHandler {
    @SubscribeEvent
    public static void onStunEvent(StunEvent event) {
        if (event.getEntity() instanceof EntityMaid maid && SlashBladeMaidBauble.NativePower.checkBauble(maid)) {
            event.setCanceled(true);
        }
    }
}
