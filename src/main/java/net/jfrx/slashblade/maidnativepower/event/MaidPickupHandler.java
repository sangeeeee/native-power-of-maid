package net.jfrx.slashblade.maidnativepower.event;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidPickupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.jfrx.slashblade.maidnativepower.item.SlashBladeMaidBauble;

@EventBusSubscriber(modid = "native_power_of_maid")
public class MaidPickupHandler {
    @SubscribeEvent
    public static void onMaidPickupExperience(MaidPickupEvent.ExperienceResult event) {
        mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess.of(event.getMaid().getMainHandItem()).ifPresent(state -> {
            if (SlashBladeMaidBauble.Exp.checkBauble(event.getMaid())) {
                state.setDamage(state.getDamage() - event.getExperienceOrb().getValue());
            }
        });
    }

    @SubscribeEvent
    public static void onMaidPickupPowerPoint(MaidPickupEvent.PowerPointResult event) {
        mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess.of(event.getMaid().getMainHandItem()).ifPresent(state -> {
            if (SlashBladeMaidBauble.Exp.checkBauble(event.getMaid())) {
                state.setDamage(state.getDamage() - event.getPowerPoint().getValue());
            }
        });
    }
}
