package net.jfrx.slashblade.maidnativepower.event;

import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import mods.flammpfeil.slashblade.ability.ArrowReflector;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.ModAttributes;
import mods.flammpfeil.slashblade.slasharts.JudgementCut;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.jfrx.slashblade.maidnativepower.entity.ai.MaidMirageBladeBehavior;
import net.jfrx.slashblade.maidnativepower.entity.ai.MaidSlashBladeMove;
import net.jfrx.slashblade.maidnativepower.item.SlashBladeMaidBauble;
import net.jfrx.slashblade.maidnativepower.network.MaidRankSyncMessage;
import net.jfrx.slashblade.maidnativepower.task.TaskSlashBlade;
import net.jfrx.slashblade.maidnativepower.util.MaidSlashBladeAttackUtils;
import net.jfrx.slashblade.maidnativepower.util.MaidCombatRules;
import net.jfrx.slashblade.maidnativepower.util.MaidSlashBladeMovementUtils;
import net.jfrx.slashblade.maidnativepower.util.JustSlashArtManager;

import java.util.Map;
import java.util.Objects;

@EventBusSubscriber(modid = "native_power_of_maid")
public class MaidTickHandler {
    public static final String NATIVE_POWER_RANK = "nativePowerOfMaid.nativepowerRank";
    private static final ResourceLocation FOLLOW_RANGE_BONUS = net.jfrx.slashblade.maidnativepower.NativePowerOfMaid.prefix("maid_slashblade_radius_bonus");
    private static final ResourceLocation DAMAGE_BONUS = net.jfrx.slashblade.maidnativepower.NativePowerOfMaid.prefix("maid_slashblade_unawakened_soul_bonus");
    private static final ResourceLocation REACH_BONUS = net.jfrx.slashblade.maidnativepower.NativePowerOfMaid.prefix("maid_slashblade_true_power_bonus");

    @SubscribeEvent
    public static void onMaidTickEvent(MaidTickEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide()) {
            return;
        }
        if (!TaskSlashBlade.UID.equals(maid.getTask().getUid()) || !MaidSlashBladeAttackUtils.isHoldingSlashBlade(maid)) {
            clearBonuses(maid);
            return;
        }
        BladeStateAccess.of(maid.getMainHandItem())
                .ifPresent(state -> handleMaidTick(maid, state));
    }

    private static void handleMaidTick(EntityMaid maid, ISlashBladeState state) {
        boolean hasNativePower = SlashBladeMaidBauble.NativePower.checkBauble(maid);
        CompoundTag data = maid.getPersistentData();

        maidTickCounter(maid, hasNativePower);
        maidBonus(maid, hasNativePower);
        // Revalidate saved targets before ticking combos (e.g. after a safety setting changes).
        LivingEntity attackTarget = maid.getTarget();
        if (attackTarget != null && !maid.canAttack(attackTarget)) {
            maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            maid.setTarget(null);
        }
        Entity lockedTarget = state.getTargetEntity(maid.level());
        if (lockedTarget != null && (!MaidCombatRules.canHarm(maid, lockedTarget)
                || MaidCombatRules.rootTarget(lockedTarget) instanceof LivingEntity living && !maid.canAttack(living))) {
            state.setTargetEntityId(null);
        }
        maid.getMainHandItem().inventoryTick(maid.level(), maid, 0, true);

        java.util.Optional.of(maid.getData(CapabilityConcentrationRank.RANK_POINT))
                .ifPresent(rank -> {
                    if (hasNativePower) {
                        long rankPoint = Math.min(Math.max(rank.getRankPoint(maid.level().getGameTime()), data.getLong(NATIVE_POWER_RANK)), rank.getMaxCapacity());
                        rank.setRawRankPoint(rankPoint);
                        rank.setLastUpdte(maid.level().getGameTime());
                        data.putLong(NATIVE_POWER_RANK, rankPoint);
                    }

                    if (maid.level().getGameTime() % 20 == 0) {
                        PacketDistributor.sendToPlayersTrackingEntity(maid, new MaidRankSyncMessage(
                                Math.min(rank.getRankPoint(maid.level().getGameTime()), rank.getMaxCapacity()), maid.getId()));
                    }
                });

        boolean canTrick = MaidSlashBladeMovementUtils.canTrick(maid);
        Entity target = state.getTargetEntity(maid.level());
        boolean canAirTrick = canTrick && SlashBladeMaidBauble.MirageBlade.checkBauble(maid) && target != null;

        if (target != null) {
            canAirTrick &= target.isAlive();
            if (target instanceof LivingEntity living) {
                canAirTrick &= living.getHealth() > 0;
                maid.setTarget(living);
            }
        }

        if (canTrick && !canAirTrick) {
            MaidSlashBladeMovementUtils.TRICK_DOWN_CHECK.accept(maid);
        }
        if (MaidGuardHandler.isGuarding(maid)) {
            if (data.getInt(MaidGuardHandler.GUARD_ESCAPE_COUNTER) <= 0) {
                MaidSlashBladeMovementUtils.TRY_TRICK_DODGE.accept(maid);
                MaidGuardHandler.guardRefreshMaidTickCounter(maid);
            }
            state.setFallDecreaseRate(1);
        }
        if (data.getBoolean(MaidGuardHandler.IS_PRE_ESCAPING) && data.getInt(MaidGuardHandler.PRE_ESCAPE_COUNTER) <= 0) {
            MaidSlashBladeMovementUtils.TRY_TRICK_DODGE.accept(maid);
            data.putBoolean(MaidGuardHandler.IS_PRE_ESCAPING, false);
        }
        handleHealthAndExp(maid, state, hasNativePower);
        handleSuperJudgementCut(maid, state, hasNativePower, data);
    }

    private static void handleHealthAndExp(EntityMaid maid, ISlashBladeState state, boolean hasNativePower) {
        int favorabilityLevel = Math.max(1, maid.getFavorabilityManager().getLevel() + 1);
        int cost = Math.max(1, Math.max(0, 4 - favorabilityLevel) / (hasNativePower ? 2 : 1));
        if (SlashBladeMaidBauble.Health.checkBauble(maid) && maid.getHealth() < maid.getMaxHealth()) {
            boolean isGuarding = MaidGuardHandler.isGuarding(maid);
            if ((hasNativePower || isGuarding || maid.level().getGameTime() % 10 == 0) && state.getProudSoulCount() >= cost) {
                state.setProudSoulCount(state.getProudSoulCount() - cost);
                if (hasNativePower) {
                    maid.setHealth(Math.min(maid.getHealth() + favorabilityLevel * (isGuarding ? 2 : 1), maid.getMaxHealth()));
                } else {
                    maid.heal(favorabilityLevel * 0.5F);
                }
            }
        }
        if (state.isBroken() && SlashBladeMaidBauble.Exp.checkBauble(maid)) {
            if (maid.getExperience() >= cost) {
                maid.setExperience(maid.getExperience() - cost);
                state.setDamage(Math.max(0, state.getDamage() - favorabilityLevel));
                if (state.getDamage() <= 0) {
                    state.setBroken(false);
                }
            }
        }
    }

    private static void handleSuperJudgementCut(EntityMaid maid, ISlashBladeState state, boolean hasNativePower, CompoundTag data) {
        if (hasNativePower && data.getInt(MaidSlashBladeAttackUtils.SUPER_JUDGEMENT_CUT_COUNTER_KEY) <= 0) {
            Map.Entry<Integer, ResourceLocation> currentLoc = state.resolvCurrentComboStateTicks(maid);
            ResourceLocation csLoc = state.getSlashArts().doArts(SlashArts.ArtsType.Super, maid);
            if (!csLoc.equals(ComboStateRegistry.NONE.getId()) && !currentLoc.getValue().equals(csLoc)) {
                data.putInt(MaidSlashBladeAttackUtils.SUPER_JUDGEMENT_CUT_COUNTER_KEY, 2400);

                AttributeInstance entityReachAttributeInstance = maid.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                if (entityReachAttributeInstance == null) {
                    return;
                }

                double radius = TaskSlashBlade.getRadius(maid);
                int rank = maid.getData(CapabilityConcentrationRank.RANK_POINT).getRank(maid.level().getGameTime()).level;
                double bonus = radius / Math.max(TargetSelector.getResolvedReach(maid), 1) * rank / 7;

                AttributeModifier entityReachBonus = new AttributeModifier(
                        net.jfrx.slashblade.maidnativepower.NativePowerOfMaid.prefix("maid_superjudgementcut_transient_bonus"), bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

                entityReachAttributeInstance.addOrUpdateTransientModifier(entityReachBonus);
                try {
                    JudgementCut.doJudgementCutSuper(maid);

                    AABB aabb = maid.getBoundingBox().inflate(48.0F);
                    double reach = TargetSelector.getResolvedReach(maid) + 32.0;

                    maid.level().getEntitiesOfClass(Projectile.class, aabb).stream()
                            .filter(e -> {
                                Entity owner = (e instanceof IShootable iShootable) ?
                                        iShootable.getShooter() : e.getOwner();
                                if (owner != null) {
                                    return !owner.equals(maid) && !owner.equals(maid.getOwner()) && !maid.isAlliedTo(owner)
                                            && !(owner instanceof OwnableEntity ownable
                                            && (Objects.equals(ownable.getOwnerUUID(), maid.getUUID())
                                            || maid.getOwnerUUID() != null && Objects.equals(ownable.getOwnerUUID(), maid.getOwnerUUID())));
                                } else {
                                    return true;
                                }
                            })
                            .filter(e -> (e.distanceToSqr(maid) < (reach * reach)))
                            .forEach(e -> ArrowReflector.doReflect(e, maid));

                } finally {
                    entityReachAttributeInstance.removeModifier(entityReachBonus);
                }
            }
        }
    }

    public static void maidTickCounter(EntityMaid maid, boolean hasNativePower) {
        CompoundTag data = maid.getPersistentData();
        int nativePower = hasNativePower ? 2 : 1;
        int favorabilityLevel = maid.getFavorabilityManager().getLevel() + 1;
        int decrement = favorabilityLevel * nativePower;

        // 随好感度加快的冷却
        data.putInt(MaidMirageBladeBehavior.HEAVY_RAIN_SWORD_COUNTER_KEY,
                Math.max(0, data.getInt(MaidMirageBladeBehavior.HEAVY_RAIN_SWORD_COUNTER_KEY) - decrement));
        data.putInt(MaidMirageBladeBehavior.BLISTERING_SWORD_COUNTER_KEY,
                Math.max(0, data.getInt(MaidMirageBladeBehavior.BLISTERING_SWORD_COUNTER_KEY) - decrement));
        data.putInt(MaidMirageBladeBehavior.SPIRAL_SWORD_COUNTER_KEY,
                Math.max(0, data.getInt(MaidMirageBladeBehavior.SPIRAL_SWORD_COUNTER_KEY) - decrement));
        data.putInt(MaidMirageBladeBehavior.STORM_SWORD_COUNTER_KEY,
                Math.max(0, data.getInt(MaidMirageBladeBehavior.STORM_SWORD_COUNTER_KEY) - decrement));
        data.putInt(MaidMirageBladeBehavior.BASE_SUMMONED_SWORD_COUNTER_KEY,
                Math.max(0, data.getInt(MaidMirageBladeBehavior.BASE_SUMMONED_SWORD_COUNTER_KEY) - decrement));
        data.putInt(MaidSlashBladeAttackUtils.SUPER_JUDGEMENT_CUT_COUNTER_KEY,
                Math.max(0, data.getInt(MaidSlashBladeAttackUtils.SUPER_JUDGEMENT_CUT_COUNTER_KEY) - decrement));

        // 不随好感度加快的冷却
        data.putInt(MaidSlashBladeMove.TRICK_COOL_DOWN,
                Math.max(0, data.getInt(MaidSlashBladeMove.TRICK_COOL_DOWN) - nativePower));
        data.putInt(MaidGuardHandler.GUARD_DAMAGE_COUNTER,
                Math.max(0, data.getInt(MaidGuardHandler.GUARD_DAMAGE_COUNTER) - nativePower));
        data.putInt(MaidGuardHandler.GUARD_ESCAPE_COUNTER,
                Math.max(0, data.getInt(MaidGuardHandler.GUARD_ESCAPE_COUNTER) - nativePower));
        data.putInt(MaidGuardHandler.PRE_ESCAPE_COUNTER,
                Math.max(0, data.getInt(MaidGuardHandler.PRE_ESCAPE_COUNTER) - nativePower));
        data.putInt(MaidGuardHandler.GUARD_COOL_DOWN,
                Math.max(0, data.getInt(MaidGuardHandler.GUARD_COOL_DOWN) - nativePower));

        if (!data.contains(NATIVE_POWER_RANK)) {
            data.putLong(NATIVE_POWER_RANK, 2300);
        }
        data.putLong(NATIVE_POWER_RANK, Math.min(2400, data.getLong(NATIVE_POWER_RANK) + decrement));

        long cooldown = JustSlashArtManager.getJustCooldown(maid);
        if (cooldown > 0) {
            cooldown = Math.max(0, cooldown - nativePower);
            JustSlashArtManager.setJustCooldown(maid, cooldown);
            if (cooldown == 0) {
                JustSlashArtManager.resetJustCount(maid);
            }
        }
    }

    public static void maidBonus(EntityMaid maid, boolean hasNativePower) {
        double radius = TargetSelector.getResolvedReach(maid) * 2;
        radius *= radius;
        if (SlashBladeMaidBauble.MirageBlade.checkBauble(maid) || SlashBladeMaidBauble.JudgementCut.checkBauble(maid)) {
            radius *= 3;
        }
        AttributeModifier followRangeBonus = new AttributeModifier(
                FOLLOW_RANGE_BONUS, radius, AttributeModifier.Operation.ADD_VALUE);
        AttributeInstance followRangeAttributeInstance = maid.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRangeAttributeInstance == null) {
            return;
        }
        followRangeAttributeInstance.removeModifier(followRangeBonus);
        followRangeAttributeInstance.addTransientModifier(followRangeBonus);

        AttributeModifier slashBladeDamageBonus = new AttributeModifier(
                DAMAGE_BONUS, SlashBladeMaidBauble.getBaubleCountForClass(maid, SlashBladeMaidBauble.UnawakenedSoul.class) * 0.1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        AttributeInstance slashBladeDamageInstance = maid.getAttribute(ModAttributes.SLASHBLADE_DAMAGE);
        if (slashBladeDamageInstance == null) {
            return;
        }
        slashBladeDamageInstance.removeModifier(slashBladeDamageBonus);
        slashBladeDamageInstance.addTransientModifier(slashBladeDamageBonus);

        AttributeModifier entityReachBonus = new AttributeModifier(
                REACH_BONUS, hasNativePower ? 2.5 : 0.5, AttributeModifier.Operation.ADD_VALUE);
        AttributeInstance entityReachAttributeInstance = maid.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityReachAttributeInstance == null) {
            return;
        }
        entityReachAttributeInstance.removeModifier(entityReachBonus);
        entityReachAttributeInstance.addTransientModifier(entityReachBonus);
    }

    private static void clearBonuses(EntityMaid maid) {
        removeBonus(maid.getAttribute(Attributes.FOLLOW_RANGE), FOLLOW_RANGE_BONUS);
        removeBonus(maid.getAttribute(ModAttributes.SLASHBLADE_DAMAGE), DAMAGE_BONUS);
        removeBonus(maid.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), REACH_BONUS);
    }

    private static void removeBonus(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }
}
