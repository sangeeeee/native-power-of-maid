package net.jfrx.slashblade.maidnativepower.gametest;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidDeathEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskIdle;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import io.netty.buffer.Unpooled;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.jfrx.slashblade.maidnativepower.config.NativePowerOfMaidCommonConfig;
import net.jfrx.slashblade.maidnativepower.event.MaidTickHandler;
import net.jfrx.slashblade.maidnativepower.event.api.MaidProgressComboEvent;
import net.jfrx.slashblade.maidnativepower.init.MaidPowerItems;
import net.jfrx.slashblade.maidnativepower.item.NativePowerBaubleItem;
import net.jfrx.slashblade.maidnativepower.network.MaidRankSyncMessage;
import net.jfrx.slashblade.maidnativepower.task.TaskSlashBlade;
import net.jfrx.slashblade.maidnativepower.util.JustSlashArtManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(NativePowerOfMaid.MODID)
@PrefixGameTestTemplate(false)
public final class NativePowerGameTests {
    private static EntityMaid createMaid(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID.get(), 2, 2, 2);
        maid.setSchedule(MaidSchedule.ALL);
        maid.setItemSlot(EquipmentSlot.MAINHAND, SlashBladeItems.SLASHBLADE.get().getDefaultInstance());
        maid.setTask(new TaskSlashBlade());
        return maid;
    }

    @GameTest(template = "game_test", timeoutTicks = 200)
    public static void maidAcquiresAndAttacks(GameTestHelper helper) {
        helper.setNight();
        EntityMaid maid = createMaid(helper);
        Zombie target = helper.spawn(EntityType.ZOMBIE, 2, 2, 6);
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        helper.succeedWhen(() -> {
            helper.assertTrue(maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == target,
                    "Maid did not acquire the hostile target");
            helper.assertTrue(target.getHealth() < 200, "Maid acquired the target but dealt no damage");
        });
    }

    @GameTest(template = "game_test")
    public static void soulsSurviveSaveAndNetwork(GameTestHelper helper) {
        ItemStack power = MaidPowerItems.SOUL_OF_NATIVE_POWER.get().getDefaultInstance();
        ItemStack soul = MaidPowerItems.SOUL_OF_GUARD.get().getDefaultInstance();
        NativePowerBaubleItem.addSoul(power, soul);
        ItemStack saved = ItemStack.parse(helper.getLevel().registryAccess(), power.save(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(NativePowerBaubleItem.getSouls(saved).getFirst().is(soul.getItem()), "Soul lost on save/load");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, saved);
            ItemStack synced = ItemStack.STREAM_CODEC.decode(buffer);
            ItemStack extracted = NativePowerBaubleItem.getSouls(synced).getFirst();
            helper.assertTrue(extracted.is(soul.getItem()), "Soul lost during network sync");
            extracted.setCount(0);
            helper.assertTrue(NativePowerBaubleItem.getSouls(synced).getFirst().getCount() == 1, "Soul accessor leaked mutable contents");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void anvilPreservesInputsAndSoulLimit(GameTestHelper helper) {
        ItemStack base = MaidPowerItems.SOUL_OF_NATIVE_POWER.get().getDefaultInstance();
        ItemStack material = MaidPowerItems.SOUL_OF_COMBO_B.get().getDefaultInstance();
        AnvilUpdateEvent event = new AnvilUpdateEvent(base, material, "", 0, helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.getOutput().isEmpty() && event.getCost() == 30 && event.getMaterialCost() == 1, "Anvil recipe did not register");
        helper.assertTrue(NativePowerBaubleItem.getSouls(base).isEmpty() && material.getCount() == 1, "Anvil preview mutated inputs");
        int limit = NativePowerOfMaidCommonConfig.NATIVE_POWER_MAX_SOUL_COUNT.get();
        for (int i = 0; i < limit; i++) {
            NativePowerBaubleItem.addSoul(event.getOutput(), MaidPowerItems.SOUL_OF_GUARD.get().getDefaultInstance());
        }
        List<ItemStack> souls = NativePowerBaubleItem.getSouls(event.getOutput());
        helper.assertTrue(souls.size() == limit && souls.stream().allMatch(s -> s.is(MaidPowerItems.SOUL_OF_GUARD.get())), "Soul capacity did not discard oldest entry");
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void altarRecipesLoad(GameTestHelper helper) {
        for (String name : List.of("unawakened_soul", "soul_of_power", "soul_of_native_power")) {
            var recipe = helper.getLevel().getRecipeManager().byKey(NativePowerOfMaid.prefix("altar/" + name)).orElseThrow();
            helper.assertTrue(recipe.value() instanceof AltarRecipe, "Wrong altar recipe type: " + name);
            AltarRecipe altar = (AltarRecipe) recipe.value();
            helper.assertTrue(altar.getIngredients().size() == 6 && !altar.getResult().isEmpty(), "Invalid altar recipe: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void taskAndWeaponChangesClearBonuses(GameTestHelper helper) {
        EntityMaid maid = createMaid(helper);
        double initial = maid.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        NeoForge.EVENT_BUS.post(new MaidTickEvent(maid));
        helper.assertTrue(maid.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) > initial, "Reach bonus missing");
        maid.setTask(new TaskIdle());
        NeoForge.EVENT_BUS.post(new MaidTickEvent(maid));
        helper.assertTrue(maid.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == initial, "Reach bonus remained after task change");
        maid.setTask(new TaskSlashBlade());
        NeoForge.EVENT_BUS.post(new MaidTickEvent(maid));
        maid.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new MaidTickEvent(maid));
        helper.assertTrue(maid.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == initial, "Reach bonus remained after weapon removal");
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void cooldownAndResurrection(GameTestHelper helper) {
        EntityMaid maid = createMaid(helper);
        maid.setFavorability(10000);
        maid.getMaidBauble().setStackInSlot(0, MaidPowerItems.SOUL_OF_NATIVE_POWER.get().getDefaultInstance());
        JustSlashArtManager.addJustCount(maid);
        JustSlashArtManager.setJustCooldown(maid, 1);
        MaidTickHandler.maidTickCounter(maid, true);
        helper.assertTrue(JustSlashArtManager.getJustCooldown(maid) == 0 && JustSlashArtManager.getJustCount(maid) == 0, "Accelerated cooldown overshot zero");
        maid.getPersistentData().putLong(MaidTickHandler.NATIVE_POWER_RANK, 300);
        MaidDeathEvent death = new MaidDeathEvent(maid, maid.damageSources().generic());
        NeoForge.EVENT_BUS.post(death);
        helper.assertTrue(death.isCanceled() && maid.getPersistentData().getLong(MaidTickHandler.NATIVE_POWER_RANK) == 0, "Native power resurrection handler did not run");
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void comboGatingAndFriendlyTargets(GameTestHelper helper) {
        EntityMaid maid = createMaid(helper);
        EntityMaid friend = helper.spawn(InitEntities.MAID.get(), 3, 2, 2);
        UUID owner = UUID.randomUUID();
        maid.setOwnerUUID(owner);
        friend.setOwnerUUID(owner);
        var targets = TargetSelector.getTargettableEntitiesWithinAABB(helper.getLevel(), maid, maid.getBoundingBox().inflate(8), 8);
        helper.assertTrue(!targets.contains(maid) && !targets.contains(friend), "Attack includes maid or allied maid");
        var combo = new MaidProgressComboEvent(maid, friend, ComboStateRegistry.COMBO_A2.getId(), ComboStateRegistry.COMBO_C.getId());
        NeoForge.EVENT_BUS.post(combo);
        helper.assertTrue(combo.isCanceled(), "Combo C activated without its soul");
        friend.setPos(maid.position().add(0.25, 0, 0));
        helper.assertTrue(maid.isWithinMeleeAttackRange(friend), "Melee range mixin rejected a nearby target");
        friend.setPos(maid.position().add(20, 0, 0));
        helper.assertTrue(!maid.isWithinMeleeAttackRange(friend), "Melee range mixin accepted a distant target");
        helper.succeed();
    }

    @GameTest(template = "game_test")
    public static void rankPayloadRoundTrip(GameTestHelper helper) {
        var buffer = Unpooled.buffer();
        try {
            var payload = new MaidRankSyncMessage(2400, 12345);
            MaidRankSyncMessage.STREAM_CODEC.encode(buffer, payload);
            helper.assertTrue(payload.equals(MaidRankSyncMessage.STREAM_CODEC.decode(buffer)), "Rank payload codec mismatch");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
