package net.jfrx.slashblade.maidnativepower.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.jfrx.slashblade.maidnativepower.NativePowerOfMaid;
import net.jfrx.slashblade.maidnativepower.item.SlashBladeMaidBaubleItem;
import net.jfrx.slashblade.maidnativepower.item.NativePowerBaubleItem;

public class MaidPowerItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, NativePowerOfMaid.MODID);

    public static final DeferredHolder<Item, Item> UNAWAKENED_SOUL = ITEMS.register("unawakened_soul", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, Item> SOUL_OF_COMBO_B = ITEMS.register("soul_of_combo_b", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_COMBO_C = ITEMS.register("soul_of_combo_c", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_RAPID_SLASH = ITEMS.register("soul_of_rapid_slash", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_AIR_COMBO = ITEMS.register("soul_of_air_combo", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_MIRAGE_BLADE = ITEMS.register("soul_of_mirage_blade", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_TRICK = ITEMS.register("soul_of_trick", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_POWER = ITEMS.register("soul_of_power", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_JUDGEMENT_CUT = ITEMS.register("soul_of_judgement_cut", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_JUST_JUDGEMENT_CUT = ITEMS.register("soul_of_just_judgement_cut", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_GUARD = ITEMS.register("soul_of_guard", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_HEALTH = ITEMS.register("soul_of_health", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_EXP = ITEMS.register("soul_of_exp", () -> new SlashBladeMaidBaubleItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SOUL_OF_NATIVE_POWER = ITEMS.register("soul_of_native_power", NativePowerBaubleItem::new);
}
