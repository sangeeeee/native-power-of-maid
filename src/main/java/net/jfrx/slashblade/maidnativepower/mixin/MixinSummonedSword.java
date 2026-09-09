package net.jfrx.slashblade.maidnativepower.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.jfrx.slashblade.maidnativepower.util.MaidCombatRules;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityAbstractSummonedSword.class)
public abstract class MixinSummonedSword {
    @Inject(method = "doForceHitEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void protectForcedHit(Entity target, CallbackInfo ci) {
        EntityMaid maid = MaidCombatRules.bladeUser((Entity) (Object) this);
        if (maid != null && !MaidCombatRules.canHarm(maid, target)) {
            ci.cancel();
        }
    }
}
