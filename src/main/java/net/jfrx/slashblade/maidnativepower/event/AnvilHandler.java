package net.jfrx.slashblade.maidnativepower.event;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.jfrx.slashblade.maidnativepower.init.MaidPowerItems;
import net.jfrx.slashblade.maidnativepower.item.SlashBladeMaidBauble;
import net.jfrx.slashblade.maidnativepower.item.NativePowerBaubleItem;

@EventBusSubscriber(modid = "native_power_of_maid")
public class AnvilHandler {
    @SubscribeEvent
    public static void onAnvilUpdateEvent(AnvilUpdateEvent event) {
        if (!event.getOutput().isEmpty()) {
            return;
        }
        ItemStack base = event.getLeft();
        ItemStack material = event.getRight();
        if (base.isEmpty() || material.isEmpty()) {
            return;
        }

        if (base.is(MaidPowerItems.SOUL_OF_NATIVE_POWER.get())) {
            IMaidBauble bauble = BaubleManager.getBauble(material);
            if (bauble instanceof SlashBladeMaidBauble && !(bauble instanceof SlashBladeMaidBauble.NativePower)) {
                ItemStack output = base.copy();

                NativePowerBaubleItem.addSoul(output, material);

                event.setMaterialCost(1);
                event.setCost(30);
                event.setOutput(output);
            }
        }
    }
}
