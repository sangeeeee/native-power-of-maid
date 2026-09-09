package net.jfrx.slashblade.maidnativepower.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "native_power_of_maid")
public class DoSlashHandler {
    public static final String LAST_DO_SLASH_TIME = "nativePowerOfMaid.lastDoSlashTime";

    @SubscribeEvent
    public static void onDoSlashEvent(SlashBladeEvent.DoSlashEvent event) {
        if (event.getUser() instanceof EntityMaid maid) {
            CompoundTag data = maid.getPersistentData();
            data.putLong(LAST_DO_SLASH_TIME, maid.level().getGameTime());
        }
    }
}
