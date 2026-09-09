package net.jfrx.slashblade.maidnativepower.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;

public class MaidPowerCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NativePowerOfMaid.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NATIVE_POWER_OF_MAID_GROUP = CREATIVE_MODE_TABS.register(NativePowerOfMaid.MODID, () -> CreativeModeTab.builder()
            .title(Component.translatable("item_group.native_power_of_maid"))
            .icon(() -> new ItemStack(MaidPowerItems.SOUL_OF_COMBO_B.get()))
            .displayItems((features, output) -> {
                output.accept(MaidPowerItems.UNAWAKENED_SOUL.get());
                output.accept(MaidPowerItems.SOUL_OF_COMBO_B.get());
                output.accept(MaidPowerItems.SOUL_OF_COMBO_C.get());
                output.accept(MaidPowerItems.SOUL_OF_RAPID_SLASH.get());
                output.accept(MaidPowerItems.SOUL_OF_AIR_COMBO.get());
                output.accept(MaidPowerItems.SOUL_OF_MIRAGE_BLADE.get());
                output.accept(MaidPowerItems.SOUL_OF_TRICK.get());
                output.accept(MaidPowerItems.SOUL_OF_POWER.get());
                output.accept(MaidPowerItems.SOUL_OF_JUDGEMENT_CUT.get());
                output.accept(MaidPowerItems.SOUL_OF_JUST_JUDGEMENT_CUT.get());
                output.accept(MaidPowerItems.SOUL_OF_GUARD.get());
                output.accept(MaidPowerItems.SOUL_OF_HEALTH.get());
                output.accept(MaidPowerItems.SOUL_OF_EXP.get());
                output.accept(MaidPowerItems.SOUL_OF_NATIVE_POWER.get());
            }).build());
}
