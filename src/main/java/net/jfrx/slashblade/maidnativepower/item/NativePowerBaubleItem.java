package net.jfrx.slashblade.maidnativepower.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.jfrx.slashblade.maidnativepower.config.NativePowerOfMaidCommonConfig;
import net.jfrx.slashblade.maidnativepower.init.MaidPowerDataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NativePowerBaubleItem extends SlashBladeMaidBaubleItem {
    public static final String NATIVE_POWER_SOULS_KEY = NativePowerOfMaid.MODID + "." + "nativePowerSouls";

    public NativePowerBaubleItem() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (!Screen.hasShiftDown() && !Screen.hasAltDown()) {
            tooltip.add(Component.translatable("item.native_power_of_maid.tooltips"));
            tooltip.add(Component.translatable("item.native_power_of_maid.soul_of_native_power.tooltips.alt"));
        } else if (Screen.hasShiftDown()) {
            int index = 1;
            while (true) {
                String key = this.getDescriptionId() + ".tooltips." + index;
                String translated = Component.translatable(key).getString();
                if (!translated.toLowerCase(Locale.ENGLISH).equals(key)) {
                    tooltip.add(index == 3 ? Component.translatable(key,
                            Component.literal(String.valueOf(NativePowerOfMaidCommonConfig.NATIVE_POWER_MAX_SOUL_COUNT.get())).withStyle(ChatFormatting.GOLD)) : Component.translatable(key));
                    index++;
                } else {
                    return;
                }
            }
        } else if (Screen.hasAltDown()) {
            NativePowerBaubleItem.getSouls(stack).forEach(itemStack -> tooltip.add(itemStack.getDisplayName()));
        }
    }

    public static void addSoul(ItemStack itemStack, ItemStack soul) {
        int limit = NativePowerOfMaidCommonConfig.NATIVE_POWER_MAX_SOUL_COUNT.get();
        List<ItemStack> souls = new ArrayList<>(getSouls(itemStack));
        if (limit > 0 && !soul.isEmpty()) {
            souls.add(soul.copyWithCount(1));
        }
        if (souls.size() > limit) {
            souls = new ArrayList<>(souls.subList(souls.size() - limit, souls.size()));
        }
        itemStack.set(MaidPowerDataComponents.NATIVE_POWER_SOULS, ItemContainerContents.fromItems(souls));
    }

    public static List<ItemStack> getSouls(ItemStack itemStack) {
        return itemStack.getOrDefault(MaidPowerDataComponents.NATIVE_POWER_SOULS, ItemContainerContents.EMPTY)
                .stream().toList();
    }
}
