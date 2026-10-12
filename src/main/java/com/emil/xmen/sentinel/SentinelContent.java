package com.emil.xmen.sentinel;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import com.emil.xmen.XMenMod;
import com.emil.xmen.world.ModContent;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Sentinel Mark X boss and everything you can build from Sentinel parts. (The normal Mark I Sentinel is
 * {@link com.emil.xmen.robot.Sentinel}, shared with the Danger Room.)
 */
public final class SentinelContent {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, XMenMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(XMenMod.MOD_ID);
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, XMenMod.MOD_ID);

    /** The black future Sentinel, the factory's boss (and, reprogrammed, your own Sentinel). */
    public static final Supplier<EntityType<SentinelMarkX>> MARK_X = ENTITY_TYPES.register("sentinel_mark_x",
            () -> EntityType.Builder.of(SentinelMarkX::new, MobCategory.MONSTER).sized(2.0f, 6.3f).eyeHeight(5.65f)
                    .clientTrackingRange(12).build("sentinel_mark_x"));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "sentinel", () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 8);
            }), 12, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(ModContent.SENTINEL_PART.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "sentinel"))),
                    2.5f, 0.1f));

    public static final DeferredItem<Item> SENTINEL_CORE = ITEMS.register("sentinel_core",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> BLASTER = ITEMS.register("sentinel_blaster",
            () -> new SentinelBlaster(new Item.Properties().durability(250).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> CONTROL_CHIP = ITEMS.register("sentinel_control_chip",
            () -> new ControlChipItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> HELMET = armor("sentinel_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> CHESTPLATE = armor("sentinel_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> LEGGINGS = armor("sentinel_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> BOOTS = armor("sentinel_boots", ArmorItem.Type.BOOTS);

    public static final DeferredItem<Item> MARK_X_SPAWN_EGG = ITEMS.register("sentinel_mark_x_spawn_egg",
            () -> new DeferredSpawnEggItem(MARK_X, 0x1f2124, 0xff8a1a, new Item.Properties()));

    private SentinelContent() {
    }

    private static DeferredItem<Item> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new SentinelArmorItem(ARMOR_MATERIAL, type,
                new Item.Properties().rarity(Rarity.RARE).durability(type.getDurability(35))));
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        modEventBus.addListener(SentinelContent::registerAttributes);
        modEventBus.addListener(SentinelContent::addToCreativeTabs);
        SentinelArmorItem.registerEvents(NeoForge.EVENT_BUS);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MARK_X.get(), SentinelMarkX.createAttributes().build());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(MARK_X_SPAWN_EGG.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(BLASTER.get());
            event.accept(HELMET.get());
            event.accept(CHESTPLATE.get());
            event.accept(LEGGINGS.get());
            event.accept(BOOTS.get());
        } else if (event.getTabKey() == ModContent.TAB.getKey()) {
            event.accept(SENTINEL_CORE.get());
            event.accept(CONTROL_CHIP.get());
            event.accept(BLASTER.get());
            event.accept(HELMET.get());
            event.accept(CHESTPLATE.get());
            event.accept(LEGGINGS.get());
            event.accept(BOOTS.get());
        }
    }
}
