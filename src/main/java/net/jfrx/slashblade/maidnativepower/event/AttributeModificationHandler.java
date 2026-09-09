package net.jfrx.slashblade.maidnativepower.event;

import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import mods.flammpfeil.slashblade.registry.ModAttributes;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "native_power_of_maid", bus = EventBusSubscriber.Bus.MOD)
public class AttributeModificationHandler {
    @SubscribeEvent
    public static void onEntityAttributeModificationEvent(EntityAttributeModificationEvent event) {
        event.add(InitEntities.MAID.get(), ModAttributes.SLASHBLADE_DAMAGE);
    }
}
