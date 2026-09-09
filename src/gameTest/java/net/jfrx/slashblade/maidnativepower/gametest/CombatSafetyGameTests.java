package net.jfrx.slashblade.maidnativepower.gametest;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.data.inner.AttackListData;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitTaskData;
import mods.flammpfeil.slashblade.RegistryEvents;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.slasharts.JudgementCut;
import mods.flammpfeil.slashblade.util.AttackHelper;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.jfrx.slashblade.maidnativepower.task.TaskSlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(NativePowerOfMaid.MODID)
@PrefixGameTestTemplate(false)
public final class CombatSafetyGameTests {
    private static EntityMaid maid(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID.get(), 2, 2, 2);
        maid.setNoAi(true);
        maid.setNoGravity(true);
        maid.setItemSlot(EquipmentSlot.MAINHAND, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        maid.setTask(new TaskSlashBlade());
        // Deliberately broaden the maid's attack list: this must not override SlashBlade's safety switches.
        maid.setData(InitTaskData.ATTACK_LIST, new AttackListData(Map.of(ResourceLocation.withDefaultNamespace("cow"), MonsterType.HOSTILE)));
        maid.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(9);
        return maid;
    }

    private static <T extends Mob> T durable(GameTestHelper helper, EntityType<T> type, int x, int z) {
        T mob = helper.spawn(type, x, 2, z);
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        mob.setHealth(1000);
        return mob;
    }

    private static void withFriendlyFire(boolean enabled, Runnable action) {
        boolean friendly = SlashBladeConfig.FRIENDLY_ENABLE.get();
        boolean pvp = SlashBladeConfig.PVP_ENABLE.get();
        try {
            SlashBladeConfig.FRIENDLY_ENABLE.set(enabled);
            SlashBladeConfig.PVP_ENABLE.set(false);
            action.run();
        } finally {
            SlashBladeConfig.FRIENDLY_ENABLE.set(friendly);
            SlashBladeConfig.PVP_ENABLE.set(pvp);
        }
    }

    @GameTest(template = "game_test")
    public static void disabledFriendlyFireFiltersTargets(GameTestHelper helper) {
        withFriendlyFire(false, () -> {
            EntityMaid maid = maid(helper);
            var cow = durable(helper, EntityType.COW, 3, 2);
            var enemy = durable(helper, EntityType.ZOMBIE, 4, 2);
            var farEnemy = durable(helper, EntityType.ZOMBIE, 15, 2);
            var targets = TargetSelector.getTargettableEntitiesWithinAABB(helper.getLevel(), maid, maid.getBoundingBox().inflate(20), 8);
            helper.assertTrue(!targets.contains(cow), "Maid re-added a friendly entity excluded by SlashBlade");
            helper.assertTrue(targets.contains(enemy), "Hostile target was lost");
            helper.assertTrue(!targets.contains(farEnemy), "Maid bypassed SlashBlade's reach limit");
            helper.assertTrue(!maid.canAttack(cow), "Maid AI still selects protected friendly entities");
            maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, cow);
            var state = BladeStateAccess.of(maid.getMainHandItem()).orElseThrow();
            state.setTargetEntityId(cow);
            NeoForge.EVENT_BUS.post(new MaidTickEvent(maid));
            helper.assertTrue(maid.getTarget() == null && state.getTargetEntity(helper.getLevel()) == null,
                    "Maid retained a protected entity in its AI or blade lock-on target");
        });
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void meleeAndJudgementCutProtectFriendlies(GameTestHelper helper) {
        withFriendlyFire(false, () -> {
            EntityMaid maid = maid(helper);
            var cow = durable(helper, EntityType.COW, 3, 2);
            var enemy = durable(helper, EntityType.ZOMBIE, 4, 2);
            AttackManager.areaAttack(maid, target -> {}, 1, true, true, true);
            helper.assertTrue(cow.getHealth() == 1000, "Melee sweep damaged a friendly entity");
            helper.assertTrue(enemy.getHealth() < 1000, "Melee sweep stopped damaging enemies");
            AttackHelper.attack(maid, cow, 1);
            helper.assertTrue(cow.getHealth() == 1000, "Direct melee bypassed friendly-fire protection");
            EntityJudgementCut cut = new EntityJudgementCut(RegistryEvents.JudgementCut, helper.getLevel());
            cut.setOwner(maid);
            cut.setPos(cow.position());
            cut.setDamage(1);
            helper.getLevel().addFreshEntity(cut);
            try {
                cut.tick();
                cut.tick();
                helper.assertTrue(cow.getHealth() == 1000, "Judgement Cut splash damaged a friendly entity");
                helper.assertTrue(cow.getActiveEffects().isEmpty(), "Judgement Cut applied an effect to a friendly entity");
                JudgementCut.doJudgementCutSuper(maid);
                helper.assertTrue(cow.getActiveEffects().isEmpty(), "Super Judgement Cut slowed a friendly entity");
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(EntityJudgementCut.class, cow.getBoundingBox().inflate(0.1)).stream()
                        .noneMatch(other -> other != cut && other.getShooter() == maid && other.position().distanceToSqr(cow.position()) < 0.01),
                        "Super Judgement Cut selected a friendly target");
            } finally {
                helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, maid.getBoundingBox().inflate(64),
                        entity -> entity instanceof IShootable shootable && shootable.getShooter() == maid).forEach(net.minecraft.world.entity.Entity::discard);
            }
        });
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void summonedSwordProtectsFriendliesAfterWeaponSwitch(GameTestHelper helper) {
        withFriendlyFire(false, () -> {
            EntityMaid maid = maid(helper);
            var cow = durable(helper, EntityType.COW, 3, 2);
            EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, helper.getLevel());
            sword.setOwner(maid);
            sword.setDamage(8);
            sword.setPos(maid.position());
            helper.getLevel().addFreshEntity(sword);
            try {
                maid.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                sword.doForceHitEntity(cow);
                helper.assertTrue(cow.getHealth() == 1000 && cow.getActiveEffects().isEmpty(), "Old summoned sword hurt a friendly entity after weapon switch");
                boolean hit = cow.hurt(cow.damageSources().indirectMagic(sword, maid), 8);
                helper.assertTrue(!hit && cow.getHealth() == 1000, "Indirect damage bypassed final friendly-fire check");
                var enemy = durable(helper, EntityType.ZOMBIE, 4, 2);
                sword.doForceHitEntity(enemy);
                helper.assertTrue(enemy.getHealth() < 1000, "Summoned sword stopped damaging enemies");
            } finally {
                sword.discard();
            }
        });
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void flyingSwordPassesFriendliesAndHitsEnemies(GameTestHelper helper) {
        withFriendlyFire(false, () -> {
            EntityMaid maid = maid(helper);
            var cow = durable(helper, EntityType.COW, 3, 2);
            var enemy = durable(helper, EntityType.ZOMBIE, 6, 2);
            // Native SlashBlade lets this retaliation tag bypass FRIENDLY_ENABLE.
            cow.addTag("RevengeAttacker");
            EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, helper.getLevel());
            sword.setOwner(maid);
            sword.setDamage(8);
            sword.setNoGravity(true);
            sword.setPos(maid.position().add(0, 0.75, 0));
            sword.setDeltaMovement(1.2, 0, 0);
            helper.getLevel().addFreshEntity(sword);
            try {
                maid.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                for (int tick = 0; tick < 5; tick++) sword.tick();
                helper.assertTrue(cow.getHealth() == 1000 && cow.getActiveEffects().isEmpty(),
                        "Flying sword hurt a friendly retaliation target");
                helper.assertTrue(enemy.getHealth() < 1000, "Protected entity blocked a sword from hitting the enemy behind it");
            } finally {
                sword.discard();
            }
        });
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void enabledFriendlyFireStillProtectsAllies(GameTestHelper helper) {
        withFriendlyFire(true, () -> {
            EntityMaid maid = maid(helper);
            var cow = durable(helper, EntityType.COW, 3, 2);
            UUID owner = UUID.randomUUID();
            maid.setOwnerUUID(owner);
            EntityMaid friend = helper.spawn(InitEntities.MAID.get(), 3, 2, 3);
            friend.setNoAi(true);
            friend.setOwnerUUID(owner);
            float health = friend.getHealth();
            AttackManager.areaAttack(maid, target -> {}, 1, true, true, true);
            helper.assertTrue(cow.getHealth() < 1000, "Enabled friendly-fire setting was ignored");
            AttackHelper.attack(maid, friend, 1);
            helper.assertTrue(friend.getHealth() == health, "Allied maid was damaged");
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            float playerHealth = player.getHealth();
            AttackHelper.attack(maid, player, 1);
            helper.assertTrue(player.getHealth() == playerHealth, "PVP disabled but maid damaged a player");
        });
        helper.succeed();
    }

    @GameTest(template = "game_test", timeoutTicks = 80)
    public static void attacksPreservePlantsAndDroppedItems(GameTestHelper helper) {
        EntityMaid maid = maid(helper);
        durable(helper, EntityType.ZOMBIE, 4, 2);
        List<BlockPos> plants = List.of(new BlockPos(1, 2, 3), new BlockPos(2, 2, 3), new BlockPos(3, 2, 3));
        for (BlockPos pos : plants) helper.setBlock(pos.below(), Blocks.DIRT);
        helper.setBlock(plants.get(0), Blocks.SHORT_GRASS);
        helper.setBlock(plants.get(1), Blocks.DANDELION);
        helper.setBlock(plants.get(2).below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        helper.setBlock(plants.get(2), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        BlockPos dropPos = helper.absolutePos(new BlockPos(2, 2, 4));
        ItemEntity drop = new ItemEntity(helper.getLevel(), dropPos.getX() + 0.5, dropPos.getY(), dropPos.getZ() + 0.5, new ItemStack(Items.DIAMOND));
        drop.setNoGravity(true);
        drop.setPickUpDelay(32767);
        helper.getLevel().addFreshEntity(drop);
        for (int tick : List.of(1, 5, 10)) {
            helper.runAtTickTime(tick, () -> {
                AttackManager.areaAttack(maid, target -> {}, 1, true, true, true);
                AttackManager.doSlash(maid, 0);
                JudgementCut.doJudgementCutSuper(maid);
            });
        }
        helper.runAtTickTime(35, () -> {
            helper.assertTrue(helper.getBlockState(plants.get(0)).is(Blocks.SHORT_GRASS), "Slash destroyed grass");
            helper.assertTrue(helper.getBlockState(plants.get(1)).is(Blocks.DANDELION), "Slash destroyed a flower");
            helper.assertTrue(helper.getBlockState(plants.get(2)).is(Blocks.WHEAT), "Slash destroyed a crop");
            helper.assertTrue(drop.isAlive() && drop.getItem().is(Items.DIAMOND) && drop.getItem().getCount() == 1, "Slash destroyed a dropped item");
            helper.succeed();
        });
    }
}
