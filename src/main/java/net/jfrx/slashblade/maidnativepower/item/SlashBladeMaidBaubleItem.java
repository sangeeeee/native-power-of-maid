package net.jfrx.slashblade.maidnativepower.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class SlashBladeMaidBaubleItem extends Item {
    public SlashBladeMaidBaubleItem(Properties pProperties) {
        super(pProperties.stacksTo(1));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.native_power_of_maid.tooltips"));
        } else {
            int index = 1;
            while (true) {
                String key = this.getDescriptionId() + ".tooltips." + index;
                String translated = Component.translatable(key).getString();
                if (!translated.toLowerCase(Locale.ENGLISH).equals(key)) {
                    tooltip.add(Component.translatable(key));
                    index++;
                } else {
                    return;
                }
            }
        }
    }
}
