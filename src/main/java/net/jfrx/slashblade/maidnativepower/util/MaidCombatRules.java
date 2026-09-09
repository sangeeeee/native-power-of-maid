package net.jfrx.slashblade.maidnativepower.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.entity.IShootable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Shared protection for target selection, melee hits and delayed blade projectiles. */
public final class MaidCombatRules {
    private MaidCombatRules() {
    }

    public static Entity rootTarget(Entity target) {
        while (target instanceof PartEntity<?> part) {
            target = part.getParent();
        }
        return target;
    }

    public static boolean canHarm(EntityMaid maid, Entity target) {
        target = rootTarget(target);
        if (target instanceof LivingEntity living) {
            if (isAlly(maid, living)) return false;
            if (!SlashBladeConfig.PVP_ENABLE.get() && living instanceof Player) return false;
            // A maid's custom attack list or revenge target must not override a disabled safety switch.
            return SlashBladeConfig.FRIENDLY_ENABLE.get() || living instanceof Enemy;
        }
        // Keep interception/extinguishing, but never add dropped items or decorations as attack targets.
        if (target instanceof Projectile projectile) {
            Entity owner = projectile instanceof IShootable shootable ? shootable.getShooter() : projectile.getOwner();
            return !(owner instanceof LivingEntity living) || canHarm(maid, living);
        }
        return target instanceof PrimedTnt;
    }

    private static boolean isAlly(EntityMaid maid, LivingEntity target) {
        if (target == maid || target.getUUID().equals(maid.getOwnerUUID()) || maid.isAlliedTo(target) || target.isAlliedTo(maid)) {
            return true;
        }
        if (target instanceof OwnableEntity ownable) {
            UUID owner = ownable.getOwnerUUID();
            if (owner != null && (owner.equals(maid.getUUID()) || owner.equals(maid.getOwnerUUID()))) return true;
        }
        LivingEntity owner = maid.getOwner();
        return owner != null && owner.isAlliedTo(target);
    }

    @Nullable
    public static EntityMaid bladeUser(@Nullable Entity source) {
        // Projectile ownership survives switching the maid's item or task.
        if (source instanceof IShootable shootable && shootable.getShooter() instanceof EntityMaid maid) {
            return maid;
        }
        if (source instanceof Projectile projectile && projectile.getOwner() instanceof EntityMaid maid
                && MaidSlashBladeAttackUtils.isHoldingSlashBlade(maid)) {
            return maid;
        }
        return source instanceof EntityMaid maid && MaidSlashBladeAttackUtils.isHoldingSlashBlade(maid) ? maid : null;
    }
}
