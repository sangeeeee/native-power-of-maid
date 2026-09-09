package net.jfrx.slashblade.maidnativepower;

import com.mojang.logging.LogUtils;
import net.jfrx.slashblade.maidnativepower.config.NativePowerOfMaidClientConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.jfrx.slashblade.maidnativepower.init.MaidPowerDataComponents;
import net.jfrx.slashblade.maidnativepower.config.NativePowerOfMaidCommonConfig;
import net.jfrx.slashblade.maidnativepower.init.MaidPowerCreativeTab;
import net.jfrx.slashblade.maidnativepower.init.MaidPowerItems;
import net.jfrx.slashblade.maidnativepower.network.NetworkManager;
import org.slf4j.Logger;

@Mod(NativePowerOfMaid.MODID)
public class NativePowerOfMaid {
    public static final String MODID = "native_power_of_maid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NativePowerOfMaid(IEventBus modEventBus, ModContainer container) {
        try {
            Class.forName("com.github.tartaricacid.touhoulittlemaid.compat.slashblade.SlashBladeCompat", false, getClass().getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Native POWER of Maid requires the Touhou Little Maid 1.5.3 NeoForge 1.21.1 snapshot with SlashBlade compatibility, not the public release.", exception);
        }
        MaidPowerDataComponents.COMPONENTS.register(modEventBus);
        MaidPowerItems.ITEMS.register(modEventBus);
        MaidPowerCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(NetworkManager::register);

        container.registerConfig(ModConfig.Type.COMMON, NativePowerOfMaidCommonConfig.COMMON_CONFIG);
        container.registerConfig(ModConfig.Type.CLIENT, NativePowerOfMaidClientConfig.CLIENT_CONFIG);
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
