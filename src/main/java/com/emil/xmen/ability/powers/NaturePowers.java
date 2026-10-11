package com.emil.xmen.ability.powers;

import static com.emil.xmen.ability.Abilities.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import com.emil.xmen.ability.Allies;
import com.emil.xmen.ability.Highlights;
import com.emil.xmen.ability.Morphs;
import com.emil.xmen.ability.PowerBehavior;
import com.emil.xmen.ability.PowerBehaviors;
import com.emil.xmen.ability.PowerProjectiles;
import com.emil.xmen.ability.PowerRuntime;
import com.emil.xmen.ability.PowerState;
import com.emil.xmen.ability.Scheduler;
import com.emil.xmen.ability.TempBlocks;
import com.emil.xmen.ability.Zones;
import com.emil.xmen.entity.ModEntities;
import com.emil.xmen.entity.PowerClone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;

/** Nature & Animals. */
public final class NaturePowers {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final BlockParticleOption VINES = new BlockParticleOption(ParticleTypes.BLOCK,
            Blocks.VINE.defaultBlockState());
    private static final List<Block> FLOWERS = List.of(Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER,
            Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.ALLIUM);

    private NaturePowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    /** Holds a mob still for seconds. */
    private static void root(ServerPlayer player, LivingEntity target, double seconds) {
        effect(target, MobEffects.MOVEMENT_SLOWDOWN, seconds, 10);
        effect(target, MobEffects.JUMP, seconds, 128);
        Scheduler.repeat(5, (int) (seconds * 4), () -> {
            if (target.isAlive()) {
                setVelocity(target, new Vec3(0, Math.min(0, target.getDeltaMovement().y), 0));
                particlesAround(player, target, VINES, 3);
            }
        });
    }

    private static int growAround(ServerPlayer player, int radius, int tries) {
        ServerLevel level = level(player);
        BlockPos c = player.blockPosition();
        int grown = 0;
        for (int i = 0; i < tries; i++) {
            BlockPos p = c.offset(player.getRandom().nextInt(radius * 2 + 1) - radius, player.getRandom().nextInt(5) - 2,
                    player.getRandom().nextInt(radius * 2 + 1) - radius);
            BlockState s = level.getBlockState(p);
            if ((s.getBlock() instanceof CropBlock || s.getBlock() instanceof SaplingBlock || s.is(BlockTags.CROPS)
                    || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.BAMBOO_SAPLING) || s.is(Blocks.SUGAR_CANE)
                    || s.is(Blocks.CACTUS)) && grow(level, p)) {
                grown++;
            }
        }
        return grown;
    }

    public static void register() {
        // Everything growing around you grows up right away.
        reg("plant_growth", b().active(10, player -> {
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            int grown = 0;
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-6, -2, -6), c.offset(6, 2, 6))) {
                BlockState s = level.getBlockState(pos);
                if (s.is(BlockTags.CROPS) || s.getBlock() instanceof SaplingBlock || s.is(Blocks.SWEET_BERRY_BUSH)) {
                    for (int i = 0; i < 6 && grow(level, pos.immutable()); i++) {
                        grown++;
                    }
                }
            }
            if (grown == 0) {
                tell(player, "Nothing is growing near you");
                return false;
            }
            sound(player, SoundEvents.BONE_MEAL_USE, 1, 1);
            return true;
        }));

        // Vines grab the mob you look at and hold it still for 5 seconds.
        reg("vine_grab", b().active(6, player -> {
            LivingEntity target = target(player, 24);
            if (target == null) {
                return false;
            }
            root(player, target, 5);
            hurt(player, target, 2);
            particleLine(player, player.position().add(0, 0.2, 0), target.position(), VINES, 0.5);
            sound(player, SoundEvents.CAVE_VINES_PLACE, 1, 0.8f);
            return true;
        }));

        // Animals follow you around, and tameable ones near you slowly become your pets.
        reg("animal_friend", b().every(40, player -> {
            for (LivingEntity e : livingAround(player, 10)) {
                if (!(e instanceof Animal animal)) {
                    continue;
                }
                if (animal.distanceTo(player) > 3) {
                    animal.getNavigation().moveTo(player, 1.1);
                }
                if (animal instanceof TamableAnimal pet && !pet.isTame() && player.getRandom().nextFloat() < 0.08f) {
                    pet.tame(player);
                    particlesAround(player, pet, ParticleTypes.HEART, 7);
                    chat(player, "A " + pet.getName().getString() + " became your pet!");
                }
            }
        }));

        // Your pets and the animals around you attack the mob you look at.
        reg("animal_command", b().active(10, player -> {
            LivingEntity target = target(player, 32);
            if (target == null) {
                return false;
            }
            int sent = 0;
            List<Animal> pack = new ArrayList<>();
            for (LivingEntity e : livingAround(player, 20)) {
                if (e instanceof Animal animal && e != target) {
                    if (!(animal instanceof TamableAnimal pet && pet.isTame() && !player.getUUID().equals(pet.getOwnerUUID()))) {
                        Allies.control(player, animal, 20);
                        animal.setTarget(target);
                        pack.add(animal);
                        sent++;
                    }
                }
            }
            if (sent == 0) {
                tell(player, "No animals nearby");
                return false;
            }
            // Animals that can't really fight still ram the target.
            Scheduler.repeat(10, 40, () -> {
                for (Animal animal : pack) {
                    if (animal.isAlive() && target.isAlive()) {
                        animal.getNavigation().moveTo(target, 1.4);
                        if (animal.distanceTo(target) < 2) {
                            hurt(player, target, 1);
                        }
                    }
                }
            });
            tell(player, sent + " animals attack!");
            sound(player, SoundEvents.WOLF_GROWL, 1, 1);
            return true;
        }));

        // A cloud of pollen where you look makes mobs drowsy: slow and blind.
        reg("sleep_pollen", b().active(12, player -> {
            Vec3 at = lookTarget(player, 24);
            Zones.add(player, at.add(0, 1, 0), 4, 8, 10, ParticleTypes.SPORE_BLOSSOM_AIR, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 2, 3);
                        effect(e, MobEffects.BLINDNESS, 2, 0);
                        effect(e, MobEffects.DIG_SLOWDOWN, 2, 2);
                        if (e instanceof Mob mob) {
                            mob.setTarget(null);
                        }
                    }
                }
                level(player).sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, zone.center.x, zone.center.y + 2,
                        zone.center.z, 12, 3, 1, 3, 0);
            });
            soundAt(player, at, SoundEvents.AZALEA_LEAVES_BREAK, 1, 0.6f);
            return true;
        }));

        // Sunlight fills your hunger.
        reg("photosynthesis", b().every(100, player -> {
            if (inSunlight(player)) {
                player.getFoodData().eat(1, 0.3f);
            }
        }));

        // Mobs that hit you get poisoned by your thorns.
        reg("thorn_skin", b().onHurt((player, event) -> {
            if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != player) {
                effect(attacker, MobEffects.POISON, 3, 0);
                hurt(player, attacker, 1, player.damageSources().thorns(player));
            }
        }));

        // Sneak in leaves, tall grass or flowers to vanish. Mobs that walk into your bush get tangled.
        reg("leaf_camouflage", b().every(10, player -> {
            if (!player.isShiftKeyDown()) {
                return;
            }
            ServerLevel level = level(player);
            BlockPos feet = player.blockPosition();
            boolean hidden = false;
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 1, 1))) {
                BlockState s = level.getBlockState(pos);
                if (s.is(BlockTags.LEAVES) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.FERN)
                        || s.is(Blocks.LARGE_FERN) || s.is(BlockTags.FLOWERS)) {
                    hidden = true;
                    break;
                }
            }
            if (hidden) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 15, 0, false, false, true));
                for (LivingEntity e : livingAround(player, 24)) {
                    forget(player, e);
                }
                for (LivingEntity e : livingAround(player, 2.5)) {
                    if (!isFriend(player, e)) {
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 2, 4);
                        particlesAround(player, e, ParticleTypes.SPORE_BLOSSOM_AIR, 4);
                    }
                }
            }
        }));

        // R: take root (or pull them up). Rooted, you can't move but gain lots of armor and heal fast.
        reg("tree_form", b()
                .every(20, player -> {
                    if (state(player).flag("rooted")) {
                        buff(player, MobEffects.REGENERATION, 1);
                        level(player).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                                Blocks.OAK_LEAVES.defaultBlockState()), player.getX(), player.getY() + 2, player.getZ(),
                                4, 0.5, 0.3, 0.5, 0);
                    }
                })
                .active(2, player -> {
                    boolean on = state(player).toggle("rooted");
                    if (on) {
                        setModifier(player, Attributes.ARMOR, "tree_armor", 12, ADD);
                        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, "tree_kb", 1, ADD);
                        setModifier(player, Attributes.MOVEMENT_SPEED, "tree_root", -1,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                    } else {
                        removeModifier(player, Attributes.ARMOR, "tree_armor");
                        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, "tree_kb");
                        removeModifier(player, Attributes.MOVEMENT_SPEED, "tree_root");
                    }
                    tell(player, on ? "Rooted" : "Uprooted");
                    sound(player, SoundEvents.ROOTS_PLACE, 1, on ? 0.6f : 1.2f);
                    return true;
                }));

        // Five angry bees attack the mob you look at.
        reg("bee_call", b().active(15, player -> {
            LivingEntity target = target(player, 24);
            if (target == null) {
                return false;
            }
            for (int i = 0; i < 5; i++) {
                Bee bee = EntityType.BEE.create(player.level());
                if (bee == null) {
                    continue;
                }
                bee.moveTo(player.getX() + player.getRandom().triangle(0, 1.5), player.getY() + 1.5,
                        player.getZ() + player.getRandom().triangle(0, 1.5));
                Allies.summon(player, bee, 20);
                bee.setTarget(target);
                bee.setRemainingPersistentAngerTime(400);
                bee.setPersistentAngerTarget(target.getUUID());
            }
            sound(player, SoundEvents.BEE_LOOP_AGGRESSIVE, 1, 1);
            return true;
        }));

        // A burst of spores: mobs around you are poisoned, and mushrooms sprout.
        reg("mushroom_spores", b().active(10, player -> {
            for (LivingEntity e : livingAround(player, 6)) {
                if (!isFriend(player, e)) {
                    effect(e, MobEffects.POISON, 5, 1);
                    effect(e, MobEffects.CONFUSION, 5, 0);
                }
            }
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            for (int i = 0; i < 12; i++) {
                BlockPos p = c.offset(player.getRandom().nextInt(11) - 5, player.getRandom().nextInt(3) - 1,
                        player.getRandom().nextInt(11) - 5);
                Block mushroom = player.getRandom().nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM;
                if (level.getBlockState(p).isAir() && mushroom.defaultBlockState().canSurvive(level, p)) {
                    level.setBlockAndUpdate(p, mushroom.defaultBlockState());
                }
            }
            particles(player, player.position().add(0, 1, 0), ParticleTypes.CRIMSON_SPORE, 60, 3);
            sound(player, SoundEvents.FUNGUS_BREAK, 1, 0.6f);
            return true;
        }));

        // Three wolves come to fight by your side for 40 seconds.
        reg("wolf_pack", b().active(45, player -> {
            for (int i = 0; i < 3; i++) {
                Wolf wolf = EntityType.WOLF.create(player.level());
                if (wolf == null) {
                    continue;
                }
                wolf.moveTo(player.getX() + player.getRandom().triangle(0, 2), player.getY(),
                        player.getZ() + player.getRandom().triangle(0, 2));
                wolf.tame(player);
                Allies.summon(player, wolf, 40);
                particlesAround(player, wolf, ParticleTypes.POOF, 10);
            }
            sound(player, SoundEvents.WOLF_HOWL, 1, 1);
            return true;
        }));

        // A flock of birds swarms the spot you look at for 8 seconds: they snatch what mobs are holding,
        // carry small mobs up into the air and drop them.
        reg("bird_flock", b().active(15, player -> {
            Vec3 at = lookTarget(player, 32);
            ItemParticleOption feather = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FEATHER));
            Zones.add(player, at.add(0, 1, 0), 4, 8, 10, feather, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (isFriend(player, e)) {
                        continue;
                    }
                    ItemStack held = e.getMainHandItem();
                    if (!held.isEmpty() && !(e instanceof Player)) {
                        e.spawnAtLocation(held.copy());
                        e.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    }
                    if (e.getBbHeight() < 1.2 && zone.age % 40 == 0) {
                        setVelocity(e, new Vec3(0, 1.4, 0));
                    }
                    if (zone.age % 20 == 0) {
                        hurt(player, e, 1);
                    }
                }
                if (zone.age % 20 == 0) {
                    soundAt(player, zone.center, SoundEvents.PARROT_FLY, 1, 1.2f);
                }
            });
            return true;
        }));

        // More drops from crops and animals.
        reg("harvest_bounty", b()
                .onBreak((player, pos, state) -> {
                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                        Block.dropResources(state, player.level(), pos);
                    }
                })
                .onKill((player, victim, drops) -> {
                    if (victim instanceof Animal) {
                        List<ItemEntity> extra = new ArrayList<>();
                        for (ItemEntity drop : drops) {
                            extra.add(new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(),
                                    drop.getItem().copy()));
                        }
                        drops.addAll(extra);
                    }
                }));

        // Nature answers you: crops and saplings grow as you walk by, and flowers spring up behind you.
        reg("gaia_link", b().every(20, player -> {
            growAround(player, 5, 8);
            ServerLevel level = level(player);
            BlockPos feet = player.blockPosition();
            if (player.getRandom().nextFloat() < 0.3f && level.getBlockState(feet).isAir()
                    && level.getBlockState(feet.below()).is(Blocks.GRASS_BLOCK)) {
                Block flower = FLOWERS.get(player.getRandom().nextInt(FLOWERS.size()));
                level.setBlockAndUpdate(feet, flower.defaultBlockState());
            }
        }));

        // Spiders, silverfish, endermites and bees nearby fight for you for a minute.
        reg("insect_command", b().active(30, player -> {
            int joined = 0;
            for (LivingEntity e : livingAround(player, 24)) {
                if (e instanceof Spider || e instanceof Silverfish || e instanceof Endermite || e instanceof Bee) {
                    Allies.control(player, (Mob) e, 60);
                    particlesAround(player, e, ParticleTypes.HAPPY_VILLAGER, 5);
                    joined++;
                }
            }
            if (joined == 0) {
                tell(player, "No insects or spiders nearby");
                return false;
            }
            tell(player, joined + " creepy-crawlies fight for you");
            return true;
        }));

        // Climb walls (see XMenClient), spiders leave you alone, your punches web mobs in place,
        // and R shoots a web.
        PowerProjectiles.register("web", (owner, projectile, hit) -> {
            if (owner == null) {
                return;
            }
            BlockPos at = hit instanceof EntityHitResult e ? e.getEntity().blockPosition()
                    : BlockPos.containing(hit.getLocation());
            if (hit instanceof BlockHitResult block) {
                at = block.getBlockPos().relative(block.getDirection());
            }
            TempBlocks.place(level(owner), at, Blocks.COBWEB.defaultBlockState(), 160);
        });
        reg("spider_powers", b()
                .ignoredBy((player, mob) -> mob instanceof Spider)
                .onAttack((player, target, event) -> {
                    if (player.getRandom().nextFloat() < 0.3f) {
                        TempBlocks.place(level(player), target.blockPosition(), Blocks.COBWEB.defaultBlockState(), 100);
                    }
                })
                .active(3, player -> {
                    PowerProjectiles.shoot(player, "web", new ItemStack(Items.COBWEB), 1.6f, 0, true);
                    sound(player, SoundEvents.SPIDER_AMBIENT, 1, 1.4f);
                    return true;
                }));

        // Much better luck when fishing. R pulls a fish out of the water you look at.
        reg("fish_caller", b()
                .attribute(Attributes.LUCK, 3, ADD)
                .active(30, player -> {
                    BlockHitResult hit = lookedAtBlockOrFluid(player, 16);
                    if (!level(player).getFluidState(hit.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER)) {
                        tell(player, "Look at water");
                        return false;
                    }
                    Item[] fish = { Items.COD, Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH };
                    Vec3 at = hit.getLocation();
                    ItemEntity drop = new ItemEntity(level(player), at.x, at.y + 0.5, at.z,
                            new ItemStack(fish[player.getRandom().nextInt(fish.length)]));
                    Vec3 to = player.position().subtract(at);
                    drop.setDeltaMovement(to.x * 0.1, 0.4, to.z * 0.1);
                    level(player).addFreshEntity(drop);
                    particles(player, at, ParticleTypes.SPLASH, 15, 0.3);
                    sound(player, SoundEvents.FISHING_BOBBER_SPLASH, 1, 1);
                    return true;
                }));

        // Tame a hostile mob for good. It follows and fights for you. Up to 5 at a time.
        reg("monster_tamer", b().active(10, player -> {
            LivingEntity target = target(player, 16);
            if (!(target instanceof Mob mob) || !isEnemy(player, target)) {
                if (target != null) {
                    tell(player, "Look at a hostile mob");
                }
                return false;
            }
            if (Allies.countOwned(player.getUUID(), false) >= 5) {
                tell(player, "You already have 5 tamed monsters");
                return false;
            }
            Allies.control(player, mob, -1);
            mob.setCustomName(Component.literal(player.getName().getString() + "'s " + mob.getName().getString()));
            particlesAround(player, mob, ParticleTypes.HEART, 10);
            chat(player, "You tamed a " + mob.getType().getDescription().getString() + "!");
            sound(player, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.5f, 1.4f);
            return true;
        }));

        // Throw seeds that burst into grabbing vines.
        PowerProjectiles.register("seed_bomb", (owner, projectile, hit) -> {
            if (owner == null) {
                return;
            }
            Vec3 at = hit.getLocation();
            for (LivingEntity e : livingNear(owner, at, 3)) {
                if (!isFriend(owner, e)) {
                    root(owner, e, 4);
                }
            }
            level(owner).sendParticles(VINES, at.x, at.y + 0.5, at.z, 40, 1.5, 0.5, 1.5, 0.1);
            soundAt(owner, at, SoundEvents.CAVE_VINES_PLACE, 1, 0.6f);
        });
        reg("seed_bomb", b().active(4, player -> {
            PowerProjectiles.shoot(player, "seed_bomb", new ItemStack(Items.WHEAT_SEEDS), 1.3f, 0, true);
            sound(player, SoundEvents.SNOWBALL_THROW, 1, 0.8f);
            return true;
        }));
    }
}
