package com.emil.xmen.world;

import java.util.EnumMap;
import java.util.List;

import com.emil.xmen.XMenMod;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All new blocks, items and armor that come with the structures. */
public final class ModContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(XMenMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(XMenMod.MOD_ID);
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, XMenMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, XMenMod.MOD_ID);

    // ------------------------------------------------------------------ blocks
    public static final DeferredBlock<ElevatorBlock> ELEVATOR = BLOCKS.register("elevator",
            () -> new ElevatorBlock(metal().strength(3.0f, 6.0f)));
    public static final DeferredBlock<CerebroBlock> CEREBRO = BLOCKS.register("cerebro",
            () -> new CerebroBlock(metal().strength(5.0f, 1200.0f).lightLevel(state -> 10)));
    public static final DeferredBlock<DangerRoomConsoleBlock> DANGER_ROOM_CONSOLE = BLOCKS.register(
            "danger_room_console", () -> new DangerRoomConsoleBlock(metal().strength(5.0f, 1200.0f)
                    .lightLevel(state -> 7)));
    public static final DeferredBlock<KrakoaGateBlock> KRAKOA_GATE = BLOCKS.register("krakoa_gate",
            () -> new KrakoaGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK)
                    .strength(2.0f, 1200.0f).sound(SoundType.MOSS).lightLevel(state -> 12)));

    // ------------------------------------------------------------------ armor
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ADAMANTIUM_MATERIAL = armorMaterial("adamantium",
            3, 6, 8, 3, 15, 3.5f, 0.15f, () -> Ingredient.of(ModContent.ADAMANTIUM_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> XMEN_SUIT_MATERIAL = armorMaterial("xmen_suit",
            2, 5, 6, 2, 15, 1.0f, 0.0f, () -> Ingredient.of(Items.LEATHER));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MAGNETO_MATERIAL = armorMaterial("magneto",
            3, 6, 8, 3, 10, 2.0f, 0.1f, () -> Ingredient.of(Items.IRON_INGOT));

    public static final Tier ADAMANTIUM_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2600, 10.0f,
            5.0f, 15, () -> Ingredient.of(ModContent.ADAMANTIUM_INGOT.get()));

    // ------------------------------------------------------------------ items
    public static final DeferredItem<Item> MUTANT_SERUM = ITEMS.register("mutant_serum",
            () -> new RerollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), false));
    public static final DeferredItem<Item> CELESTIAL_ORB = ITEMS.register("celestial_orb",
            () -> new RerollItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant(), true));
    public static final DeferredItem<Item> ADAMANTIUM_INGOT = ITEMS.register("adamantium_ingot",
            () -> new Item(new Item.Properties().fireResistant()));
    public static final DeferredItem<Item> SENTINEL_PART = ITEMS.register("sentinel_part",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ADAMANTIUM_SWORD = ITEMS.register("adamantium_sword",
            () -> new SwordItem(ADAMANTIUM_TIER, new Item.Properties().fireResistant()
                    .attributes(SwordItem.createAttributes(ADAMANTIUM_TIER, 3, -2.4f))));
    public static final DeferredItem<Item> ADAMANTIUM_PICKAXE = ITEMS.register("adamantium_pickaxe",
            () -> new PickaxeItem(ADAMANTIUM_TIER, new Item.Properties().fireResistant()
                    .attributes(DiggerItem.createAttributes(ADAMANTIUM_TIER, 1.0f, -2.8f))));
    public static final DeferredItem<Item> ADAMANTIUM_HELMET = armor("adamantium_helmet", ADAMANTIUM_MATERIAL,
            ArmorItem.Type.HELMET, 45);
    public static final DeferredItem<Item> ADAMANTIUM_CHESTPLATE = armor("adamantium_chestplate",
            ADAMANTIUM_MATERIAL, ArmorItem.Type.CHESTPLATE, 45);
    public static final DeferredItem<Item> ADAMANTIUM_LEGGINGS = armor("adamantium_leggings", ADAMANTIUM_MATERIAL,
            ArmorItem.Type.LEGGINGS, 45);
    public static final DeferredItem<Item> ADAMANTIUM_BOOTS = armor("adamantium_boots", ADAMANTIUM_MATERIAL,
            ArmorItem.Type.BOOTS, 45);
    public static final DeferredItem<Item> XMEN_MASK = armor("xmen_mask", XMEN_SUIT_MATERIAL, ArmorItem.Type.HELMET,
            25);
    public static final DeferredItem<Item> XMEN_SUIT = armor("xmen_suit", XMEN_SUIT_MATERIAL,
            ArmorItem.Type.CHESTPLATE, 25);
    public static final DeferredItem<Item> XMEN_PANTS = armor("xmen_pants", XMEN_SUIT_MATERIAL,
            ArmorItem.Type.LEGGINGS, 25);
    public static final DeferredItem<Item> XMEN_BOOTS = armor("xmen_boots", XMEN_SUIT_MATERIAL, ArmorItem.Type.BOOTS,
            25);
    public static final DeferredItem<Item> MAGNETO_HELMET = ITEMS.register("magneto_helmet",
            () -> new ArmorItem(MAGNETO_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC)
                    .durability(ArmorItem.Type.HELMET.getDurability(40))));

    public static final DeferredItem<BlockItem> ELEVATOR_ITEM = ITEMS.registerSimpleBlockItem(ELEVATOR);
    public static final DeferredItem<BlockItem> CEREBRO_ITEM = ITEMS.registerSimpleBlockItem(CEREBRO);
    public static final DeferredItem<BlockItem> DANGER_ROOM_CONSOLE_ITEM =
            ITEMS.registerSimpleBlockItem(DANGER_ROOM_CONSOLE);
    public static final DeferredItem<BlockItem> KRAKOA_GATE_ITEM = ITEMS.registerSimpleBlockItem(KRAKOA_GATE);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("xmen",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.xmen"))
                    .icon(() -> new ItemStack(CELESTIAL_ORB.get()))
                    .displayItems((params, output) -> {
                        for (DeferredHolder<Item, ? extends Item> item : ITEMS.getEntries()) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    private ModContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        TABS.register(modEventBus);
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private static DeferredItem<Item> armor(String name, DeferredHolder<ArmorMaterial, ArmorMaterial> material,
            ArmorItem.Type type, int durability) {
        return ITEMS.register(name, () -> new ArmorItem(material, type,
                new Item.Properties().durability(type.getDurability(durability))));
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> armorMaterial(String name, int boots, int legs,
            int chest, int helmet, int enchantability, float toughness, float knockbackResistance,
            java.util.function.Supplier<Ingredient> repair) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                    map.put(ArmorItem.Type.BOOTS, boots);
                    map.put(ArmorItem.Type.LEGGINGS, legs);
                    map.put(ArmorItem.Type.CHESTPLATE, chest);
                    map.put(ArmorItem.Type.HELMET, helmet);
                    map.put(ArmorItem.Type.BODY, chest);
                }),
                enchantability, SoundEvents.ARMOR_EQUIP_IRON, repair,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, name))),
                toughness, knockbackResistance));
    }
}
