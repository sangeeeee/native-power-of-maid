package net.jfrx.slashblade.maidnativepower.task;

import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler;
import com.github.tartaricacid.touhoulittlemaid.util.SoundUtil;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.data.builtin.SlashBladeBuiltInRegistry;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.ItemStack;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.jfrx.slashblade.maidnativepower.entity.ai.MaidMirageBladeBehavior;
import net.jfrx.slashblade.maidnativepower.entity.ai.MaidSlashBladeAttack;
import net.jfrx.slashblade.maidnativepower.entity.ai.MaidSlashBladeMove;
import net.jfrx.slashblade.maidnativepower.item.SlashBladeMaidBauble;
import net.jfrx.slashblade.maidnativepower.util.MaidSlashBladeAttackUtils;
import net.jfrx.slashblade.maidnativepower.util.MaidCombatRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class TaskSlashBlade implements IAttackTask {
    public static final ResourceLocation UID = NativePowerOfMaid.prefix("slashblade_attack");

    @Override
    public @NotNull ResourceLocation getUid() {
        return UID;
    }

    @Override
    public @NotNull ItemStack getIcon() {
        if (Minecraft.getInstance().player != null) {
            Registry<SlashBladeDefinition> bladeRegistry = SlashBlade.getSlashBladeDefinitionRegistry(Minecraft.getInstance().player.level());
            if (bladeRegistry.containsKey(SlashBladeBuiltInRegistry.YAMATO)) {
                return Objects.requireNonNull(bladeRegistry.get(SlashBladeBuiltInRegistry.YAMATO)).getBlade(Minecraft.getInstance().player.registryAccess());
            }
        }
        return SlashBladeItems.SLASHBLADE.get().getDefaultInstance();
    }

    @Override
    public @Nullable SoundEvent getAmbientSound(@NotNull EntityMaid maid) {
        return SoundUtil.attackSound(maid, InitSounds.MAID_ATTACK.get(), 0.5F);
    }

    @Override
    public @NotNull List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(@NotNull EntityMaid maid) {
        BehaviorControl<EntityMaid> supplementedTask = StartAttacking.create(MaidSlashBladeAttackUtils::isHoldingSlashBlade, IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<EntityMaid> findTargetTask = StopAttackingIfTargetInvalid.create((target) -> !MaidSlashBladeAttackUtils.isHoldingSlashBlade(maid) || farAway(target, maid));
        BehaviorControl<Mob> moveToTargetTask = MaidSlashBladeMove.create(0.6F);
        BehaviorControl<Mob> attackTargetTask = MaidSlashBladeAttack.create();
        BehaviorControl<EntityMaid> mirageBladeTask = new MaidMirageBladeBehavior();
        return Lists.newArrayList(
                Pair.of(5, supplementedTask),
                Pair.of(5, findTargetTask),
                Pair.of(5, moveToTargetTask),
                Pair.of(5, attackTargetTask),
                Pair.of(5, mirageBladeTask)
        );
    }

    @Override
    public @NotNull List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(@NotNull EntityMaid maid) {
        BehaviorControl<EntityMaid> supplementedTask = StartAttacking.create(MaidSlashBladeAttackUtils::isHoldingSlashBlade, IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<EntityMaid> findTargetTask = StopAttackingIfTargetInvalid.create((target) -> !MaidSlashBladeAttackUtils.isHoldingSlashBlade(maid) || farAway(target, maid));
        BehaviorControl<Mob> attackTargetTask = MaidSlashBladeAttack.create();
        BehaviorControl<EntityMaid> mirageBladeTask = new MaidMirageBladeBehavior();
        return Lists.newArrayList(
                Pair.of(5, supplementedTask),
                Pair.of(5, findTargetTask),
                Pair.of(5, attackTargetTask),
                Pair.of(5, mirageBladeTask)
        );
    }

    @Override
    public boolean isWeapon(@NotNull EntityMaid maid, ItemStack stack) {
        return BladeStateAccess.of(stack).isPresent();
    }

    @Override
    public boolean canAttack(EntityMaid maid, LivingEntity target) {
        return MaidCombatRules.canHarm(maid, target) && IAttackTask.super.canAttack(maid, target);
    }

    private boolean hasSouls(EntityMaid maid) {
        BaubleItemHandler handler = maid.getMaidBauble();
        for (int i = 0; i < handler.getSlots(); ++i) {
            IMaidBauble baubleIn = handler.getBaubleInSlot(i);
            if (baubleIn instanceof SlashBladeMaidBauble) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(@NotNull EntityMaid maid) {
        return Lists.newArrayList(Pair.of("has_slashblade", MaidSlashBladeAttackUtils::isHoldingSlashBlade), Pair.of("souls", this::hasSouls));
    }

    public static boolean farAway(LivingEntity target, EntityMaid maid) {
        if (!target.isAlive() || !maid.canAttack(target)) {
            return true;
        } else {
            if (SlashBladeMaidBauble.MirageBlade.checkBauble(maid) && SlashBladeMaidBauble.Trick.checkBauble(maid)) {
                return false;
            }
            boolean enable = maid.isHomeModeEnable();
            double radius = getRadius(maid);
            if (!enable && maid.getOwner() != null) {
                return maid.getOwner().distanceTo(target) > radius;
            } else {
                return maid.distanceTo(target) > radius;
            }
        }
    }

    public static double getRadius(EntityMaid maid) {
        double radius = TargetSelector.getResolvedReach(maid) * 2;
        radius *= radius;
        if (SlashBladeMaidBauble.MirageBlade.checkBauble(maid) || SlashBladeMaidBauble.JudgementCut.checkBauble(maid)) {
            radius *= 3;
        }
        return radius;
    }
}
