package net.jfrx.slashblade.maidnativepower.mixin;

import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.util.AttackHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.jfrx.slashblade.maidnativepower.util.MaidCombatRules;
import net.jfrx.slashblade.maidnativepower.item.SlashBladeMaidBauble;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AttackHelper.class)
public abstract class MixinAttackHelper {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void protectMaidMelee(LivingEntity attacker, Entity target, float comboRatio, CallbackInfo ci) {
        if (attacker instanceof EntityMaid maid && !MaidCombatRules.canHarm(maid, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "getRankBonus(Lnet/minecraft/world/entity/LivingEntity;)F", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectGetRankBonus(LivingEntity attacker, CallbackInfoReturnable<Float> cir) {
        if (attacker instanceof EntityMaid maid) {
            boolean hasNativePower = SlashBladeMaidBauble.NativePower.checkBauble(maid);
            IConcentrationRank.ConcentrationRanks rankBonus = maid.getData(CapabilityConcentrationRank.RANK_POINT).getRank(attacker.getCommandSenderWorld().getGameTime());
            double rankDamageBonus = rankBonus.level / 2.0;
            if (IConcentrationRank.ConcentrationRanks.S.level <= rankBonus.level) {
                int refine = BladeStateAccess.of(maid.getMainHandItem())
                        .map(ISlashBladeState::getRefine).orElse(0);
                int expLevel = (int) Math.floor((double) maid.getExperience() / 120);

                rankDamageBonus = (float) Math.max(rankDamageBonus,
                        (hasNativePower ? refine : Math.min(expLevel, refine)) * SlashBladeConfig.REFINE_DAMAGE_MULTIPLIER.get());
            }

            cir.setReturnValue((float) rankDamageBonus);
        }
    }
}
