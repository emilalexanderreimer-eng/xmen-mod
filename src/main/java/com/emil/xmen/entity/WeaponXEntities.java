package com.emil.xmen.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.emil.xmen.XMenMod;
import com.emil.xmen.world.ModContent;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The people of the Weapon X facility, their spawn eggs, the soldiers' rifle and the tank console. */
public final class WeaponXEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, XMenMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(XMenMod.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(XMenMod.MOD_ID);
    private static final List<DeferredItem<? extends Item>> EGGS = new ArrayList<>();

    public static final Supplier<EntityType<WeaponXSoldier>> SOLDIER = person("weapon_x_soldier",
            WeaponXSoldier::new, 0x1c1f1c, 0x4a5240);
    public static final Supplier<EntityType<WeaponXScientist>> SCIENTIST = person("weapon_x_scientist",
            WeaponXScientist::new, 0xe8eaec, 0x6d9bc3);
    public static final Supplier<EntityType<WilliamStryker>> STRYKER = person("william_stryker",
            WilliamStryker::new, 0x4b5320, 0xb9b39a);
    public static final Supplier<EntityType<LadyDeathstrike>> DEATHSTRIKE = person("lady_deathstrike",
            LadyDeathstrike::new, 0x111111, 0x4fc3f7);
    public static final Supplier<EntityType<JasonStryker>> JASON = person("jason_stryker",
            JasonStryker::new, 0x9ea3a8, 0xc58bd6);
    public static final Supplier<EntityType<X24>> X_24 = person("x24", X24::new, 0x3b3b3b, 0xc9c9c9);
    public static final Supplier<EntityType<WeaponXI>> WEAPON_XI = person("weapon_xi", WeaponXI::new,
            0xd9b9a0, 0x8b0000);
    public static final Supplier<EntityType<WeaponXLogan>> LOGAN = person("weapon_x_logan", WeaponXLogan::new,
            0xc89a74, 0x9aa0a6);

    public static final DeferredItem<Item> RIFLE = ITEMS.register("weapon_x_rifle",
            () -> new WeaponXRifle(new Item.Properties().durability(300)));
    public static final DeferredItem<Item> CLAWS = ITEMS.register("adamantium_claws",
            () -> new WeaponXItems.Claws(ModContent.ADAMANTIUM_TIER, new Item.Properties().fireResistant()
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .attributes(SwordItem.createAttributes(ModContent.ADAMANTIUM_TIER, 4, -1.6f))));
    public static final DeferredItem<Item> SKELETON = ITEMS.register("adamantium_skeleton",
            () -> new WeaponXItems.Skeleton(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Item> HEALING_SERUM = ITEMS.register("healing_factor_serum",
            () -> new WeaponXItems.Serum(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> COLLAR = ITEMS.register("inhibitor_collar",
            () -> new WeaponXItems.Collar(new Item.Properties().durability(8).rarity(Rarity.RARE)));
    public static final DeferredBlock<TankConsoleBlock> TANK_CONSOLE = BLOCKS.register("weapon_x_tank_console",
            () -> new TankConsoleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f)
                    .sound(SoundType.METAL).lightLevel(state -> 7).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> TANK_CONSOLE_ITEM = ITEMS.registerSimpleBlockItem(TANK_CONSOLE);

    private WeaponXEntities() {
    }

    private static <T extends WeaponXMob> Supplier<EntityType<T>> person(String name, EntityType.EntityFactory<T> make,
            int color, int spots) {
        Supplier<EntityType<T>> type = ENTITY_TYPES.register(name,
                () -> EntityType.Builder.of(make, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10)
                        .build(name));
        EGGS.add(ITEMS.register(name + "_spawn_egg",
                () -> new DeferredSpawnEggItem(type, color, spots, new Item.Properties())));
        return type;
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(WeaponXEntities::registerAttributes);
        modEventBus.addListener(WeaponXEntities::addToCreativeTabs);
        NeoForge.EVENT_BUS.addListener(WeaponXItems::onClone);
        NeoForge.EVENT_BUS.addListener(WeaponXItems::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, WeaponXItems::onCollarInteract);
        NeoForge.EVENT_BUS.addListener(WeaponXItems::onCollaredTick);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        put(event, SOLDIER, WeaponXSoldier.createAttributes());
        put(event, SCIENTIST, WeaponXScientist.createAttributes());
        put(event, STRYKER, WilliamStryker.createAttributes());
        put(event, DEATHSTRIKE, LadyDeathstrike.createAttributes());
        put(event, JASON, JasonStryker.createAttributes());
        put(event, X_24, X24.createAttributes());
        put(event, WEAPON_XI, WeaponXI.createAttributes());
        put(event, LOGAN, WeaponXLogan.createAttributes());
    }

    private static void put(EntityAttributeCreationEvent event, Supplier<? extends EntityType<? extends WeaponXMob>> type,
            AttributeSupplier.Builder attributes) {
        event.put(type.get(), attributes.build());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            for (DeferredItem<? extends Item> egg : EGGS) {
                event.accept(egg.get());
            }
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(RIFLE.get());
            event.accept(CLAWS.get());
        } else if (event.getTabKey() == ModContent.TAB.getKey()) {
            for (DeferredItem<Item> item : List.of(RIFLE, CLAWS, SKELETON, HEALING_SERUM, COLLAR)) {
                event.accept(item.get());
            }
            event.accept(TANK_CONSOLE_ITEM.get());
        }
    }
}
