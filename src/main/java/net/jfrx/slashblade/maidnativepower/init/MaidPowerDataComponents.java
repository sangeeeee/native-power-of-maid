package net.jfrx.slashblade.maidnativepower.init;

import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MaidPowerDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, NativePowerOfMaid.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> NATIVE_POWER_SOULS =
            COMPONENTS.register("native_power_souls", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    private MaidPowerDataComponents() {}
}
