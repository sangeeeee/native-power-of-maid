package net.jfrx.slashblade.maidnativepower.mixin;

import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityMaid.class)
public abstract class MixinEntityMaid extends TamableAnimal implements CrossbowAttackMob, IMaid {
    private MixinEntityMaid(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "isWithinMeleeAttackRange(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void injectIsWithinMeleeAttackRange(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        BladeStateAccess.of(this.getMainHandItem()).ifPresent(state -> {
            double reach = TargetSelector.getResolvedReach(this);
            cir.setReturnValue(this.distanceToSqr(entity) < reach * reach);
        });
    }
}
