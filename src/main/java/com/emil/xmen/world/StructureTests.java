package com.emil.xmen.world;

import java.util.List;
import java.util.Optional;

import com.emil.xmen.XMenMod;
import com.emil.xmen.apocalypse.ApocalypseBoss;
import com.emil.xmen.apocalypse.ApocalypseServant;
import com.emil.xmen.entity.MagnetoBoss;
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
                2064 * 16 + 200, 320, 2064 * 16 + 200), mob -> mob.getPersistentData().contains("xmen_kind"));
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
        if (level.getEntitiesOfClass(SinisterBoss.class, area).isEmpty()) {
            helper.fail("Mister Sinister isn't in his lab");
            return;
        }
        if (level.getEntitiesOfClass(SinisterClone.class, area, SinisterClone::isDormant).size() < 8) {
            helper.fail("Sinister's clones aren't asleep in their tanks");
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
        XMenMod.LOGGER.info("Test: {} structure mobs set up", people.size());
        helper.succeed();
    }
}
