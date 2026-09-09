package net.jfrx.slashblade.maidnativepower.event;

import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AdvancementHelper;
import net.jfrx.slashblade.maidnativepower.task.TaskSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.jfrx.slashblade.maidnativepower.item.SlashBladeMaidBauble;
import net.jfrx.slashblade.maidnativepower.util.JustSlashArtManager;

@EventBusSubscriber(modid = "native_power_of_maid")
public class ChargeActionHandler {
    @SubscribeEvent
    public static void onPerformSlashArtEvent(SlashBladeEvent.PerformSlashArtEvent event) {
        if (event.getEntityLiving() instanceof EntityMaid maid) {
            BladeStateAccess.of(maid.getMainHandItem())
                    .ifPresent(state -> onPerformSlashArt(event, maid, state));
        }
    }

    private static void onPerformSlashArt(SlashBladeEvent.PerformSlashArtEvent event, EntityMaid maid, ISlashBladeState state) {
        // Only check if in 拔刀剑攻击
        if (!TaskSlashBlade.UID.equals(maid.getTask().getUid())) {
            return;
        }
        if (!SlashBladeMaidBauble.JudgementCut.checkBauble(maid) && !SlashBladeMaidBauble.JustJudgementCut.checkBauble(maid)) {
            event.setCanceled(true);
            return;
        }
        if (isJudgementCut(event.getComboState())) {
            int count = JustSlashArtManager.addJustCount(maid);
            int maxCount = SlashBladeMaidBauble.JustJudgementCut.checkBauble(maid) ? (SlashBladeMaidBauble.NativePower.checkBauble(maid) ? 5 : 3) : 1;
            if (count > maxCount) {
                JustSlashArtManager.setJustCooldown(maid, 240);
                event.setCanceled(true);
            }
            if (event.getType() == SlashArts.ArtsType.Jackpot) {
                AdvancementHelper.grantedIf(maid.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SOUL_SPEED).value(), maid);
            }
        }
    }

    public static boolean isJudgementCut(ResourceLocation combo) {
        return combo.equals(ComboStateRegistry.JUDGEMENT_CUT.getId())
                || combo.equals(ComboStateRegistry.JUDGEMENT_CUT_SLASH.getId())
                || combo.equals(ComboStateRegistry.JUDGEMENT_CUT_SLASH_AIR.getId())
                || combo.equals(ComboStateRegistry.JUDGEMENT_CUT_SLASH_JUST.getId())
                || combo.equals(ComboStateRegistry.JUDGEMENT_CUT_SLASH_JUST2.getId())
                || combo.equals(ComboStateRegistry.JUDGEMENT_CUT_END.getId());
    }
}
