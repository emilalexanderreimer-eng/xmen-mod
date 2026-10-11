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
import java.util.ArrayDeque;
import java.util.Deque;
import com.emil.xmen.power.Powers;
import com.emil.xmen.power.Power;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.CropBlock;

/** Reality, Time & Space. */
public final class RealityPowers {
    private static final List<Block> WARP_BLOCKS = List.of(Blocks.MOSS_BLOCK, Blocks.AMETHYST_BLOCK, Blocks.GLASS,
            Blocks.PRISMARINE, Blocks.PURPUR_BLOCK, Blocks.HONEYCOMB_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.END_STONE,
            Blocks.PINK_WOOL, Blocks.SLIME_BLOCK, Blocks.SEA_LANTERN, Blocks.CHERRY_LEAVES, Blocks.CALCITE);
    private static final List<EntityType<? extends Mob>> WARP_ANIMALS = List.of(EntityType.CHICKEN, EntityType.PIG,
            EntityType.SHEEP, EntityType.RABBIT, EntityType.COW, EntityType.FROG, EntityType.AXOLOTL);
    private static final List<Block> TRANSMUTE = List.of(Blocks.STONE, Blocks.IRON_ORE, Blocks.GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.EMERALD_ORE, Blocks.DIAMOND_ORE);
    private static final DustParticleOptions VOID = new DustParticleOptions(new Vector3f(0.1f, 0.0f, 0.2f), 2.0f);

    private record Snapshot(Vec3 pos, float health, int food, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
    }

    private RealityPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static boolean breakable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0
                && state.getFluidState().isEmpty();
    }

    public static void register() {
        // Rewrite reality: the blocks around the spot you look at turn into strange new blocks, and a mob you
        // look at turns into a harmless animal.
        reg("reality_warping", b().active(5, player -> {
            ServerLevel level = level(player);
            LivingEntity target = lookedAtLiving(player, 32);
            if (target instanceof Mob mob && !(target instanceof Player) && !isFriend(player, target)) {
                EntityType<? extends Mob> type = WARP_ANIMALS.get(player.getRandom().nextInt(WARP_ANIMALS.size()));
                Mob animal = type.create(level);
                if (animal != null) {
                    animal.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0);
                    level.addFreshEntity(animal);
                    particlesAround(player, mob, ParticleTypes.WITCH, 30);
                    mob.discard();
                    sound(player, SoundEvents.EVOKER_PREPARE_WOLOLO, 1, 1.5f);
                    return true;
                }
            }
            BlockPos center = lookedAtPos(player, 32);
            if (center == null) {
                tell(player, "Look at a block or a mob");
                return false;
            }
            Block block = WARP_BLOCKS.get(player.getRandom().nextInt(WARP_BLOCKS.size()));
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
                BlockState s = level.getBlockState(pos);
                if (pos.distSqr(center) <= 6 && breakable(level, pos, s) && s.getDestroySpeed(level, pos) < 30) {
                    level.setBlockAndUpdate(pos, block.defaultBlockState());
                }
            }
            particles(player, Vec3.atCenterOf(center), ParticleTypes.WITCH, 40, 1.5);
            sound(player, SoundEvents.EVOKER_PREPARE_WOLOLO, 1, 0.6f);
            return true;
        }));

        // Freeze time for 5 seconds: mobs and arrows around you stop dead.
        reg("time_stop", b().active(45, player -> {
            ServerLevel level = level(player);
            List<Mob> frozen = new ArrayList<>();
            List<Projectile> stopped = new ArrayList<>();
            List<Vec3> speeds = new ArrayList<>();
            for (Entity e : entitiesAround(player, 32)) {
                if (e instanceof Mob mob && !isFriend(player, mob) && !mob.isNoAi()) {
                    mob.setNoAi(true);
                    setVelocity(mob, Vec3.ZERO);
                    frozen.add(mob);
                } else if (e instanceof Projectile p && !p.isNoGravity()) {
                    speeds.add(p.getDeltaMovement());
                    stopped.add(p);
                    p.setNoGravity(true);
                    setVelocity(p, Vec3.ZERO);
                }
            }
            Scheduler.repeat(1, 99, () -> {
                for (Projectile p : stopped) {
                    if (p.isAlive()) {
                        p.setDeltaMovement(Vec3.ZERO);
                    }
                }
            });
            Scheduler.later(100, () -> {
                frozen.forEach(mob -> mob.setNoAi(false));
                for (int i = 0; i < stopped.size(); i++) {
                    Projectile p = stopped.get(i);
                    if (p.isAlive()) {
                        p.setNoGravity(false);
                        setVelocity(p, speeds.get(i));
                    }
                }
                sound(player, SoundEvents.BELL_RESONATE, 1, 1.5f);
            });
            for (int r = 4; r <= 32; r += 7) {
                ring(player, player.position().add(0, 1, 0), r, ParticleTypes.REVERSE_PORTAL);
            }
            sound(player, SoundEvents.BELL_BLOCK, 1, 0.5f);
            tell(player, "Time stopped for 5 seconds");
            return true;
        }));

        // For 8 seconds everything near you moves in slow motion: mobs crawl and arrows drift.
        reg("slow_motion", b().active(25, player -> {
            Zones.following(player, 10, 8, 1, null, (zone, inside) -> {
                for (Entity e : zone.entities()) {
                    if (e instanceof Projectile p && p.getOwner() != zone.ownerPlayer()) {
                        Vec3 v = p.getDeltaMovement();
                        if (v.length() > 0.15) {
                            setVelocity(p, v.scale(0.8));
                        }
                    }
                }
                if (zone.age % 10 == 0) {
                    for (LivingEntity e : inside) {
                        if (!isFriend(player, e)) {
                            effect(e, MobEffects.MOVEMENT_SLOWDOWN, 1, 4);
                            effect(e, MobEffects.DIG_SLOWDOWN, 1, 3);
                        }
                    }
                }
            });
            sound(player, SoundEvents.BEACON_DEACTIVATE, 1, 0.5f);
            return true;
        }));

        // Jump back to where (and how healthy) you were 10 seconds ago.
        reg("rewind", b()
                .every(20, player -> {
                    Deque<Snapshot> history = state(player).get("rewind");
                    if (history == null) {
                        history = new ArrayDeque<>();
                        state(player).put("rewind", history);
                    }
                    history.addLast(new Snapshot(player.position(), player.getHealth(), player.getFoodData().getFoodLevel(),
                            player.level().dimension()));
                    while (history.size() > 10) {
                        history.removeFirst();
                    }
                })
                .active(30, player -> {
                    Deque<Snapshot> history = state(player).get("rewind");
                    if (history == null || history.isEmpty()) {
                        tell(player, "Nothing to rewind yet");
                        return false;
                    }
                    Snapshot past = history.peekFirst();
                    if (past.dimension() != player.level().dimension()) {
                        tell(player, "You can't rewind across dimensions");
                        return false;
                    }
                    particles(player, player.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 40, 0.4);
                    teleport(player, past.pos());
                    player.setHealth(Math.max(player.getHealth(), past.health()));
                    player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), past.food()));
                    player.clearFire();
                    history.clear();
                    particles(player, player.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 40, 0.4);
                    sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 0.5f);
                    return true;
                }));

        // Mobs around you float away. Sneak: slam them all into the ground.
        reg("gravity_control", b().active(8, player -> {
            int moved = 0;
            for (LivingEntity e : livingAround(player, 12)) {
                if (isFriend(player, e)) {
                    continue;
                }
                if (player.isShiftKeyDown()) {
                    setVelocity(e, new Vec3(0, -3, 0));
                    hurt(player, e, 5);
                } else {
                    effect(e, MobEffects.LEVITATION, 4, 2);
                }
                moved++;
            }
            if (moved == 0) {
                tell(player, "No mobs nearby");
                return false;
            }
            ring(player, player.position().add(0, 0.2, 0), 6, ParticleTypes.REVERSE_PORTAL);
            sound(player, SoundEvents.SHULKER_BULLET_HIT, 1, 0.6f);
            return true;
        }));

        // Luck: better loot, one hit in five does triple damage, and mobs sometimes miss you.
        reg("luck", b()
                .effect(MobEffects.LUCK, 1)
                .onAttack((player, target, event) -> {
                    if (player.getRandom().nextFloat() < 0.2f) {
                        event.setAmount(event.getAmount() * 3);
                        particlesAround(player, target, ParticleTypes.HAPPY_VILLAGER, 10);
                        tell(player, "Lucky hit!");
                    }
                })
                .onHurt((player, event) -> {
                    if (event.getSource().getEntity() != null && player.getRandom().nextFloat() < 0.15f) {
                        event.setCanceled(true);
                        tell(player, "Lucky! It missed");
                    }
                }));

        // Turn the block you look at into something more valuable: stone into iron ore, iron into gold, and on.
        reg("transmutation", b().active(5, player -> {
            ServerLevel level = level(player);
            BlockPos pos = lookedAtPos(player, 8);
            if (pos == null) {
                tell(player, "Look at a block");
                return false;
            }
            BlockState s = level.getBlockState(pos);
            Block next = null;
            if (s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK)) {
                next = Blocks.CLAY;
            } else if (s.is(Blocks.COBBLESTONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.ANDESITE)
                    || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE)) {
                next = Blocks.STONE;
            } else {
                int i = TRANSMUTE.indexOf(s.getBlock());
                if (i >= 0 && i < TRANSMUTE.size() - 1) {
                    next = TRANSMUTE.get(i + 1);
                }
            }
            if (next == null) {
                tell(player, "That can't be transmuted");
                return false;
            }
            level.setBlockAndUpdate(pos, next.defaultBlockState());
            particles(player, Vec3.atCenterOf(pos), ParticleTypes.ENCHANT, 30, 0.5);
            tell(player, "Turned into " + next.getName().getString());
            sound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 1, 1.4f);
            return true;
        }));

        // Open your ender chest anywhere.
        reg("pocket_space", b().active(1, player -> {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory,
                    player.getEnderChestInventory()), Component.translatable("container.enderchest")));
            sound(player, SoundEvents.ENDER_CHEST_OPEN, 1, 1);
            return true;
        }));

        // Two copies of you appear and fight for 30 seconds (up to 4 at once).
        reg("duplication", b().active(15, player -> {
            int alive = level(player).getEntitiesOfClass(PowerClone.class, player.getBoundingBox().inflate(64),
                    c -> player.getUUID().equals(c.getOwnerId()) && c.getVariant() == PowerClone.FIGHTER).size();
            int make = Math.min(2, 4 - alive);
            if (make <= 0) {
                tell(player, "You already have 4 copies");
                return false;
            }
            for (int i = 0; i < make; i++) {
                Vec3 side = new Vec3(-player.getLookAngle().z, 0, player.getLookAngle().x).normalize().scale(i == 0 ? 1.5 : -1.5);
                PowerClone clone = clone(player, player.position().add(side), PowerClone.FIGHTER, 30);
                if (clone != null) {
                    particlesAround(player, clone, ParticleTypes.POOF, 10);
                }
            }
            sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, 0.8f);
            return true;
        }));

        // Break the block you look at instantly, even obsidian. You get the block.
        reg("molecular_breakdown", b().active(0.5, player -> {
            ServerLevel level = level(player);
            BlockPos pos = lookedAtPos(player, 8);
            if (pos == null) {
                return false;
            }
            BlockState s = level.getBlockState(pos);
            if (s.getDestroySpeed(level, pos) < 0) {
                tell(player, "Even you can't break that");
                return false;
            }
            level.destroyBlock(pos, false, player);
            ItemStack item = new ItemStack(s.getBlock().asItem());
            if (!item.isEmpty()) {
                Block.popResource(level, pos, item);
            }
            particles(player, Vec3.atCenterOf(pos), ParticleTypes.WHITE_ASH, 20, 0.4);
            return true;
        }));

        // Other players near you lose their powers while they stay close.
        reg("power_negation", b().every(20, player -> {
            for (ServerPlayer other : level(player).players()) {
                if (other != player && other.distanceTo(player) <= 10) {
                    if (!PowerRuntime.isSuppressed(other)) {
                        tell(other, player.getName().getString() + " is blocking your powers");
                    }
                    PowerRuntime.suppress(other, 30);
                }
            }
        }));

        // Supercharge every player near you for 30 seconds: cooldowns are halved and hits are stronger.
        reg("power_boost", b().active(60, player -> {
            int boosted = 0;
            for (ServerPlayer other : level(player).players()) {
                if (other.distanceTo(player) <= 10) {
                    PowerRuntime.boost(other, 600);
                    effect(other, MobEffects.DAMAGE_BOOST, 30, 0);
                    particlesAround(player, other, ParticleTypes.TOTEM_OF_UNDYING, 20);
                    if (other != player) {
                        chat(other, player.getName().getString() + " boosted your power for 30 seconds!");
                        boosted++;
                    }
                }
            }
            tell(player, "Boosted " + boosted + " friends (and yourself) for 30 seconds");
            sound(player, SoundEvents.BEACON_POWER_SELECT, 1, 1.2f);
            return true;
        }));

        // Give the player you look at a random second power for 5 minutes.
        reg("power_granting", b().active(120, player -> {
            LivingEntity target = lookedAtLiving(player, 16);
            if (!(target instanceof ServerPlayer other)) {
                tell(player, "Look at another player");
                return false;
            }
            Power power = null;
            for (int i = 0; i < 50 && power == null; i++) {
                Power candidate = Powers.roll(player.getRandom());
                if (PowerBehaviors.get(candidate.id()) != null && !candidate.id().equals("power_granting")) {
                    power = candidate;
                }
            }
            if (power == null) {
                return false;
            }
            PowerRuntime.grantExtra(other, power.id(), 6000);
            other.sendSystemMessage(Component.literal(player.getName().getString() + " gave you a second power for 5 minutes: ")
                    .append(power.title()));
            player.sendSystemMessage(Component.literal("You gave " + other.getName().getString() + " ").append(power.title()));
            particlesAround(player, other, ParticleTypes.TOTEM_OF_UNDYING, 40);
            sound(player, SoundEvents.TOTEM_USE, 0.6f, 1.5f);
            return true;
        }));

        // Touch something to age it: babies grow up, crops ripen, and grown-up mobs get old and weak.
        reg("aging_touch", b().active(4, player -> {
            LivingEntity target = lookedAtLiving(player, 5);
            if (target instanceof AgeableMob ageable && ageable.isBaby()) {
                ageable.setAge(0);
                particlesAround(player, target, ParticleTypes.HAPPY_VILLAGER, 10);
                return true;
            }
            if (target != null && !isFriend(player, target)) {
                effect(target, MobEffects.WEAKNESS, 20, 1);
                effect(target, MobEffects.MOVEMENT_SLOWDOWN, 20, 1);
                hurtMagic(player, target, 3);
                particlesAround(player, target, ParticleTypes.WHITE_ASH, 15);
                return true;
            }
            BlockPos pos = lookedAtPos(player, 5);
            if (pos != null) {
                ServerLevel level = level(player);
                int ripened = 0;
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s)) {
                        level.setBlockAndUpdate(p, crop.getStateForAge(crop.getMaxAge()));
                        ripened++;
                    }
                }
                if (ripened > 0) {
                    particles(player, Vec3.atCenterOf(pos), ParticleTypes.HAPPY_VILLAGER, 20, 1.5);
                    return true;
                }
            }
            tell(player, "Nothing to age");
            return false;
        }));

        // Swallow every mob and item around the spot you look at and spit them out far away.
        reg("wormhole", b().active(20, player -> {
            ServerLevel level = level(player);
            Vec3 at = lookTarget(player, 32);
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double dist = 30 + player.getRandom().nextInt(30);
            BlockPos far = BlockPos.containing(at.x + Math.cos(angle) * dist, at.y, at.z + Math.sin(angle) * dist);
            BlockPos spot = safeSpotNear(level, level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, far));
            if (spot == null) {
                tell(player, "The wormhole collapsed");
                return true;
            }
            int sent = 0;
            for (Entity e : level.getEntities(player, new AABB(at, at).inflate(4))) {
                if ((e instanceof LivingEntity living && !isFriend(player, living)) || e instanceof ItemEntity) {
                    e.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                    sent++;
                }
            }
            level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1, at.z, 80, 2, 1, 2, 1);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, spot.getX(), spot.getY() + 1, spot.getZ(), 80, 1, 1, 1, 0.5);
            tell(player, sent == 0 ? "Nothing was swallowed" : "Sent " + sent + " things " + direction(player, Vec3.atCenterOf(spot)));
            soundAt(player, at, SoundEvents.PORTAL_TRAVEL, 0.3f, 1.5f);
            return true;
        }));

        // A bubble around you for 8 seconds: you and friends inside take far less damage.
        reg("force_field", b()
                .onHurt((player, event) -> {
                    if (state(player).active("field", now(player))) {
                        event.setAmount(event.getAmount() * 0.3f);
                    }
                })
                .active(25, player -> {
                    state(player).start("field", now(player), 160);
                    Zones.following(player, 4, 8, 10, null, (zone, inside) -> {
                        for (LivingEntity e : inside) {
                            if (isFriend(player, e)) {
                                buff(e, MobEffects.DAMAGE_RESISTANCE, 2);
                            }
                        }
                        ring(player, zone.center, 4, ParticleTypes.END_ROD);
                    });
                    sound(player, SoundEvents.BEACON_ACTIVATE, 1, 1.5f);
                    return true;
                }));

        // Friends near you get Luck.
        reg("lucky_aura", b().every(20, player -> {
            buff(player, MobEffects.LUCK, 0);
            for (LivingEntity e : livingAround(player, 10)) {
                if (isFriend(player, e)) {
                    buff(e, MobEffects.LUCK, 0);
                }
            }
        }));

        // A hex bolt: the mob it hits suffers a random misfortune.
        PowerProjectiles.register("hex", (owner, projectile, hit) -> {
            if (owner == null || !(hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target)) {
                return;
            }
            hurtMagic(owner, target, 2);
            switch (owner.getRandom().nextInt(6)) {
                case 0 -> {
                    lightning(owner, target.position());
                    tell(owner, "Hex: struck by lightning");
                }
                case 1 -> {
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack item = target.getItemBySlot(slot);
                        if (!item.isEmpty()) {
                            target.spawnAtLocation(item.copy());
                            target.setItemSlot(slot, ItemStack.EMPTY);
                        }
                    }
                    tell(owner, "Hex: dropped everything");
                }
                case 2 -> {
                    setVelocity(target, new Vec3(0, 1.5, 0));
                    tell(owner, "Hex: thrown into the sky");
                }
                case 3 -> {
                    target.igniteForSeconds(6);
                    tell(owner, "Hex: burst into flames");
                }
                case 4 -> {
                    effect(target, MobEffects.LEVITATION, 3, 0);
                    effect(target, MobEffects.UNLUCK, 30, 0);
                    tell(owner, "Hex: floating off");
                }
                default -> {
                    effect(target, MobEffects.WEAKNESS, 10, 2);
                    effect(target, MobEffects.MOVEMENT_SLOWDOWN, 10, 2);
                    tell(owner, "Hex: cursed with weakness");
                }
            }
            particlesAround(owner, target, ParticleTypes.WITCH, 20);
        });
        reg("hex_bolts", b().active(3, player -> {
            PowerProjectiles.shoot(player, "hex", new ItemStack(Items.ENDER_EYE), 1.8f, 0, false);
            sound(player, SoundEvents.EVOKER_CAST_SPELL, 1, 1.6f);
            return true;
        }));

        // A black hole where you look for 8 seconds: it pulls in everything around it and crushes it.
        reg("black_hole", b().active(60, player -> {
            Vec3 at = lookTarget(player, 32).add(0, 1.5, 0);
            Zones.add(player, at, 12, 8, 1, null, (zone, inside) -> {
                for (Entity e : zone.entities()) {
                    if (e instanceof LivingEntity living && isFriend(player, living)) {
                        continue;
                    }
                    pull(zone.center, e, 0.12);
                    if (e instanceof LivingEntity living && zone.age % 10 == 0
                            && living.getBoundingBox().getCenter().distanceTo(zone.center) < 2) {
                        hurtMagic(player, living, 4);
                    }
                }
                if (zone.age % 2 == 0) {
                    level(player).sendParticles(VOID, zone.center.x, zone.center.y, zone.center.z, 10, 0.4, 0.4, 0.4, 0);
                    level(player).sendParticles(ParticleTypes.REVERSE_PORTAL, zone.center.x, zone.center.y,
                            zone.center.z, 15, 4, 4, 4, 0.2);
                }
            });
            soundAt(player, at, SoundEvents.WARDEN_SONIC_CHARGE, 2, 0.3f);
            return true;
        }));

        // Everything decays around you: stone crumbles to cobble and gravel, and mobs' armor falls apart.
        reg("decay", b().active(15, player -> {
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 3, 4))) {
                if (player.getRandom().nextFloat() > 0.5f) {
                    continue;
                }
                BlockState s = level.getBlockState(pos);
                if (s.is(Blocks.STONE) || s.is(Blocks.STONE_BRICKS)) {
                    level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                } else if (s.is(Blocks.COBBLESTONE)) {
                    level.setBlockAndUpdate(pos, Blocks.GRAVEL.defaultBlockState());
                } else if (s.is(Blocks.GRASS_BLOCK)) {
                    level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
                }
            }
            for (LivingEntity e : livingAround(player, 6)) {
                if (isFriend(player, e)) {
                    continue;
                }
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack item = e.getItemBySlot(slot);
                    if (slot.isArmor() && !item.isEmpty() && item.isDamageableItem()) {
                        item.hurtAndBreak(60, e, slot);
                    }
                }
                effect(e, MobEffects.WITHER, 3, 0);
            }
            particles(player, player.position().add(0, 1, 0), ParticleTypes.WHITE_ASH, 80, 3);
            sound(player, SoundEvents.WITHER_AMBIENT, 0.5f, 0.5f);
            return true;
        }));
    }
}
