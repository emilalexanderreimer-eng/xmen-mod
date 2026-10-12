package com.emil.xmen.world;

import java.util.List;
import java.util.Optional;

import com.emil.xmen.XMenMod;
import com.emil.xmen.robot.Sentinel;
import com.emil.xmen.sentinel.SentinelMarkX;
import com.emil.xmen.apocalypse.ApocalypseBoss;
import com.emil.xmen.apocalypse.ApocalypseServant;
import com.emil.xmen.entity.MagnetoBoss;
import com.emil.xmen.entity.MutantNpc;
import com.emil.xmen.entity.SinisterBoss;
import com.emil.xmen.entity.SinisterClone;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import com.emil.xmen.entity.JasonStryker;
import com.emil.xmen.entity.LadyDeathstrike;
import com.emil.xmen.entity.WeaponXI;
import com.emil.xmen.entity.WeaponXLogan;
import com.emil.xmen.entity.WeaponXMob;
import com.emil.xmen.entity.WeaponXScientist;
import com.emil.xmen.entity.WeaponXSoldier;
import com.emil.xmen.entity.WilliamStryker;
import com.emil.xmen.entity.X24;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Checks that every structure loads and can be built, and that its people get set up. Runs in CI. */
@GameTestHolder(XMenMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class StructureTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyStructureBuilds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withSuppressedOutput();
        int index = 0;
        for (ResourceKey<Structure> key : ModStructures.ALL) {
            Optional<StructureTemplate> template = level.getStructureManager().get(key.location());
            if (template.isEmpty() || template.get().getSize().getX() <= 0) {
                helper.fail("Missing building for " + key.location());
                return;
            }
            Holder.Reference<Structure> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .getHolder(key).orElse(null);
            if (holder == null) {
                helper.fail("Structure not registered: " + key.location());
                return;
            }
            // each one gets its own spot far away from the others
            int cx = 2000 + (index % 4) * 16;
            int cz = 2000 + (index / 4) * 16;
            index++;
            for (int x = cx - 7; x <= cx + 7; x++) {
                for (int z = cz - 7; z <= cz + 7; z++) {
                    level.setChunkForced(x, z, true);
                    level.getChunk(x, z);
                }
            }
            BlockPos pos = new BlockPos(cx * 16, 64, cz * 16);
            try {
                PlaceCommand.placeStructure(source, holder, pos);
            } catch (Exception e) {
                helper.fail("Couldn't build " + key.location() + ": " + e.getMessage());
                return;
            }
            XMenMod.LOGGER.info("Test: built {}", key.location());
        }
        List<Mob> people = level.getEntitiesOfClass(Mob.class, new AABB(2000 * 16 - 200, -64, 2000 * 16 - 200,
                2064 * 16 + 200, 320, 2064 * 16 + 200), mob -> mob.getPersistentData().contains("xmen_kind")
                        || mob instanceof Sentinel || mob instanceof SentinelMarkX);
        if (people.size() < 40) {
            helper.fail("Only " + people.size() + " structure mobs were set up");
            return;
        }
        boolean magneto = !level.getEntitiesOfClass(MagnetoBoss.class, new AABB(2000 * 16 - 200, -64,
                2000 * 16 - 200, 2064 * 16 + 200, 320, 2064 * 16 + 200)).isEmpty();
        if (!magneto) {
            helper.fail("Magneto isn't on Asteroid M");
            return;
        }
        AABB area = new AABB(2000 * 16 - 200, -64, 2000 * 16 - 200, 2064 * 16 + 200, 320, 2064 * 16 + 200);
        // the X-Mansion: the X-Men are real characters (no villagers) and Professor X sits.
        // (The lower level with the X-Jet ends up below the bottom of the flat test world, so it isn't checked.)
        List<MutantNpc> xmen = level.getEntitiesOfClass(MutantNpc.class, area, npc -> !npc.isHostile());
        if (xmen.stream().noneMatch(npc -> npc.getCharacter().equals("professor_x") && npc.isSitting())) {
            helper.fail("Professor X isn't sitting in his study");
            return;
        }
        if (xmen.stream().noneMatch(npc -> npc.getCharacter().equals("wolverine"))) {
            helper.fail("Wolverine isn't in the mansion");
            return;
        }
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, area).isEmpty()) {
            helper.fail("There are still villagers in the structures");
            return;
        }
        if (level.getEntitiesOfClass(ApocalypseBoss.class, area).isEmpty()) {
            helper.fail("Apocalypse isn't in his pyramid");
            return;
        }
        int horsemen = level.getEntitiesOfClass(ApocalypseServant.class, area, s -> s.kind().horseman()).size();
        if (horsemen < 4) {
            helper.fail("Only " + horsemen + " of Apocalypse's Four Horsemen are in the pyramid");
            return;
        }
        // the Sentinel factory: real Sentinels (no iron golems) and the Mark X boss asleep in its cradle
        if (level.getEntitiesOfClass(SentinelMarkX.class, area, s -> s.isBoss() && !s.isAwake()).isEmpty()) {
            helper.fail("The Sentinel Mark X isn't asleep in the factory");
            return;
        }
        int sentinels = level.getEntitiesOfClass(Sentinel.class, area, s -> !s.isBoss()).size();
        if (sentinels < 6) {
            helper.fail("The Sentinel factory has only " + sentinels + " Sentinels");
            return;
        }
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, area).isEmpty()) {
            helper.fail("There are still iron golems in the structures");
            return;
        }
        XMenMod.LOGGER.info("Test: {} structure mobs set up, {} X-Men", people.size(), xmen.size());
        helper.succeed();
    }

    /**
     * Builds Mister Sinister's lab by itself (the test world is too flat and low for its underground level) and
     * checks that Sinister is in it, his clones are asleep in their tanks, and both elevator stops are there.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void sinisterLabHasItsPeople(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager()
                .get(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "sinister_lab")).orElse(null);
        if (template == null) {
            helper.fail("Missing building for xmen:sinister_lab");
            return;
        }
        BlockPos corner = new BlockPos(3100 * 16, -60, 3100 * 16);
        for (int x = 3099; x <= 3106; x++) {
            for (int z = 3099; z <= 3106; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
        }
        template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.getRandom(), 2);
        AABB area = new AABB(corner).expandTowards(template.getSize().getX(), template.getSize().getY(),
                template.getSize().getZ()).inflate(4);
        if (level.getEntitiesOfClass(SinisterBoss.class, area).isEmpty()) {
            helper.fail("Mister Sinister isn't in his lab");
            return;
        }
        int sleeping = level.getEntitiesOfClass(SinisterClone.class, area, SinisterClone::isDormant).size();
        if (sleeping < 12) {
            helper.fail("Only " + sleeping + " of Sinister's clones are asleep in their tanks");
            return;
        }
        // the freight elevator: one stop in the Essex Corp lobby, one on the lab level, straight below
        BlockPos top = corner.offset(37, 40, 11);
        BlockPos bottom = corner.offset(37, 6, 11);
        if (!level.getBlockState(top).is(ModContent.ELEVATOR.get())
                || !level.getBlockState(bottom).is(ModContent.ELEVATOR.get())) {
            helper.fail("The elevator down to Sinister's lab is missing");
            return;
        }
        XMenMod.LOGGER.info("Test: Sinister's lab has {} sleeping clones", sleeping);
        helper.succeed();
    }

    /**
     * Builds the Weapon X lab by itself (the test world is too flat and low for its deep levels) and checks that
     * everybody is in it and the three in tanks are still locked up.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void weaponXHasItsPeople(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager()
                .get(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "weapon_x")).orElse(null);
        if (template == null) {
            helper.fail("Missing building for xmen:weapon_x");
            return;
        }
        BlockPos corner = new BlockPos(3000 * 16, -50, 3000 * 16);
        for (int x = 2999; x <= 3008; x++) {
            for (int z = 2999; z <= 3008; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
        }
        template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.getRandom(), 2);
        AABB area = new AABB(corner).inflate(0, 0, 0).expandTowards(template.getSize().getX(),
                template.getSize().getY(), template.getSize().getZ()).inflate(4);
        int soldiers = level.getEntitiesOfClass(WeaponXSoldier.class, area).size();
        int scientists = level.getEntitiesOfClass(WeaponXScientist.class, area).size();
        if (soldiers < 15 || scientists < 6) {
            helper.fail("Weapon X has only " + soldiers + " soldiers and " + scientists + " scientists");
            return;
        }
        for (Class<? extends WeaponXMob> boss : List.of(WilliamStryker.class, LadyDeathstrike.class,
                JasonStryker.class)) {
            if (level.getEntitiesOfClass(boss, area).isEmpty()) {
                helper.fail(boss.getSimpleName() + " isn't in the Weapon X lab");
                return;
            }
        }
        for (Class<? extends WeaponXMob> tank : List.of(WeaponXLogan.class, X24.class, WeaponXI.class)) {
            if (level.getEntitiesOfClass(tank, area, WeaponXMob::isContained).isEmpty()) {
                helper.fail(tank.getSimpleName() + " isn't locked in his tank");
                return;
            }
        }
        XMenMod.LOGGER.info("Test: Weapon X has {} soldiers and {} scientists", soldiers, scientists);
        helper.succeed();
    }

    /**
     * Builds the reworked smaller structures by themselves (the Morlock tunnels are too deep for the flat test world)
     * and checks that their people are real characters, not renamed pillagers, vindicators or husks.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void smallStructuresHaveRealPeople(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String[] names = {"morlock_tunnels", "brotherhood_hideout", "hellfire_club", "genosha", "detection_tower"};
        AABB all = null;
        AABB tunnels = null;
        for (int i = 0; i < names.length; i++) {
            StructureTemplate template = level.getStructureManager()
                    .get(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, names[i])).orElse(null);
            if (template == null) {
                helper.fail("Missing building for xmen:" + names[i]);
                return;
            }
            int cx = 4000 + i * 8;
            for (int x = cx - 1; x <= cx + 6; x++) {
                for (int z = 3999; z <= 4006; z++) {
                    level.setChunkForced(x, z, true);
                    level.getChunk(x, z);
                }
            }
            BlockPos corner = new BlockPos(cx * 16, -55, 4000 * 16);
            template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.getRandom(), 2);
            AABB box = new AABB(corner).expandTowards(template.getSize().getX(), template.getSize().getY(),
                    template.getSize().getZ()).inflate(4);
            all = all == null ? box : all.minmax(box);
            if (i == 0) {
                tunnels = box;
            }
        }
        List<Mob> vanilla = level.getEntitiesOfClass(Mob.class, all,
                mob -> mob instanceof AbstractIllager || mob instanceof Zombie || mob instanceof AbstractVillager);
        if (!vanilla.isEmpty()) {
            helper.fail("There are still vanilla mobs in the small structures: " + vanilla.get(0).getType());
            return;
        }
        List<MutantNpc> people = level.getEntitiesOfClass(MutantNpc.class, all);
        for (String character : List.of("callisto", "caliban", "healer", "masque", "sabretooth", "juggernaut",
                "multiple_man", "sebastian_shaw", "azazel", "riptide", "hellfire_guard", "selene", "genoshan",
                "anti_mutant_soldier")) {
            if (people.stream().noneMatch(npc -> npc.getCharacter().equals(character))) {
                helper.fail(character + " isn't in the small structures");
                return;
            }
        }
        if (people.stream().anyMatch(npc -> npc.getCharacter().equals("callisto") && npc.isHostile())) {
            helper.fail("Callisto should only fight once she's challenged");
            return;
        }
        int carts = level.getEntitiesOfClass(AbstractMinecart.class, tunnels).size();
        if (carts < 2) {
            helper.fail("The Morlock minecart line has " + carts + " carts instead of 2");
            return;
        }
        XMenMod.LOGGER.info("Test: the small structures have {} real characters", people.size());
        helper.succeed();
    }
}
