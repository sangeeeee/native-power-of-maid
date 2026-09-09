package net.jfrx.slashblade.maidnativepower.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.jfrx.slashblade.maidnativepower.util.MaidCombatRules;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = NativePowerOfMaid.MODID)
public final class MaidCombatProtection {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        EntityMaid maid = MaidCombatRules.bladeUser(event.getProjectile());
        if (maid != null && event.getRayTraceResult() instanceof EntityHitResult hit
                && !MaidCombatRules.canHarm(maid, hit.getEntity())) {
            // Cancel before impact handlers apply stun, fire, potions or damage.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        EntityMaid maid = MaidCombatRules.bladeUser(event.getSource().getDirectEntity());
        if (maid == null) maid = MaidCombatRules.bladeUser(event.getSource().getEntity());
        if (maid != null && !MaidCombatRules.canHarm(maid, event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
