package com.emil.xmen.apocalypse;

import java.util.List;
import java.util.function.Supplier;

import com.emil.xmen.XMenMod;
import com.emil.xmen.world.ModContent;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Apocalypse, his Four Horsemen, Clan Akkaba and the glowing veins of his pyramid. */
public final class ApocalypseContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, XMenMod.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(XMenMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(XMenMod.MOD_ID);

    public static final Supplier<EntityType<ApocalypseBoss>> APOCALYPSE = ENTITIES.register("apocalypse",
            () -> EntityType.Builder.of(ApocalypseBoss::new, MobCategory.MONSTER).sized(0.6f, 1.95f).fireImmune()
                    .clientTrackingRange(10).build("apocalypse"));
    public static final Supplier<EntityType<ApocalypseServant>> STORM = servant("storm");
    public static final Supplier<EntityType<ApocalypseServant>> PSYLOCKE = servant("psylocke");
    public static final Supplier<EntityType<ApocalypseServant>> ARCHANGEL = servant("archangel");
    public static final Supplier<EntityType<ApocalypseServant>> HORSEMAN_MAGNETO = servant("horseman_magneto");
    public static final Supplier<EntityType<ApocalypseServant>> CULTIST = servant("akkaba_cultist");

    /** Gold lines in the pyramid walls. They glow brighter when Apocalypse wakes up. */
    public static final DeferredBlock<VeinBlock> VEIN = BLOCKS.register("celestial_vein",
            () -> new VeinBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0f, 9.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 8)));

    public static final DeferredItem<Item> APOCALYPSE_EGG = egg("apocalypse", APOCALYPSE, 0x2c3e6b, 0xc9a227);
    public static final DeferredItem<Item> STORM_EGG = egg("storm", STORM, 0x1d1d24, 0xf0f0f0);
    public static final DeferredItem<Item> PSYLOCKE_EGG = egg("psylocke", PSYLOCKE, 0x5a1f78, 0xd070ff);
    public static final DeferredItem<Item> ARCHANGEL_EGG = egg("archangel", ARCHANGEL, 0x2a3d6e, 0x9aa7b8);
    public static final DeferredItem<Item> MAGNETO_EGG = egg("horseman_magneto", HORSEMAN_MAGNETO, 0x3b3f47,
            0x8a2030);
    public static final DeferredItem<Item> CULTIST_EGG = egg("akkaba_cultist", CULTIST, 0x6b4a2a, 0xc9a227);

    private ApocalypseContent() {
    }

    private static Supplier<EntityType<ApocalypseServant>> servant(String name) {
        return ENTITIES.register(name, () -> EntityType.Builder.<ApocalypseServant>of(ApocalypseServant::new,
                MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10).build(name));
    }

    private static DeferredItem<Item> egg(String name, Supplier<? extends EntityType<? extends net.minecraft.world
            .entity.Mob>> type, int background, int highlight) {
        return ITEMS.register(name + "_spawn_egg",
                () -> new DeferredSpawnEggItem(type, background, highlight, new Item.Properties()));
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ApocalypseContent::registerAttributes);
        modEventBus.addListener(ApocalypseContent::addToTabs);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(APOCALYPSE.get(), ApocalypseBoss.createAttributes().build());
        for (Supplier<EntityType<ApocalypseServant>> type : List.of(STORM, PSYLOCKE, ARCHANGEL, HORSEMAN_MAGNETO,
                CULTIST)) {
            event.put(type.get(), ApocalypseServant.createAttributes(ServantKind.of(type.get())).build());
        }
    }

    private static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS || event.getTab() == ModContent.TAB.get()) {
            for (var egg : ITEMS.getEntries()) {
                event.accept(egg.get());
            }
        }
    }
}
