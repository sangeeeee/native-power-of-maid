package net.jfrx.slashblade.maidnativepower.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.jfrx.slashblade.maidnativepower.util.MaidCombatRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Mixin(TargetSelector.class)
public abstract class MixinTargetSelector {
    @Inject(method = "getTargettableEntitiesWithinAABB(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/AABB;D)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void filterMaidTargets(Level world, LivingEntity attacker, AABB aabb, double reach, CallbackInfoReturnable<List<Entity>> cir) {
        if (attacker instanceof EntityMaid maid) {
            // Only remove from the native result: appending maid.canAttack targets bypassed
            // SlashBlade's friendly/PVP switches, blacklist and distance checks.
            // Native multipart traversal can return the same part more than once.
            List<Entity> targets = new ArrayList<>(new LinkedHashSet<>(cir.getReturnValue()));
            targets.removeIf(target -> !MaidCombatRules.canHarm(maid, target)
                    || MaidCombatRules.rootTarget(target) instanceof LivingEntity living && !maid.canAttack(living));
            cir.setReturnValue(targets);
        }
    }

    @Inject(method = "getTargettableEntitiesWithinAABB(Lnet/minecraft/world/level/Level;DLnet/minecraft/world/entity/Entity;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void filterMaidProjectileTargets(Level world, double reach, Entity projectile, CallbackInfoReturnable<List<Entity>> cir) {
        EntityMaid maid = MaidCombatRules.bladeUser(projectile);
        if (maid != null) {
            List<Entity> targets = new ArrayList<>(new LinkedHashSet<>(cir.getReturnValue()));
            targets.removeIf(target -> !MaidCombatRules.canHarm(maid, target));
            cir.setReturnValue(targets);
        }
    }
}
