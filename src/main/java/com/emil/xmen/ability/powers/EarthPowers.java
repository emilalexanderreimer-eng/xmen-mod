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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.neoforged.neoforge.common.Tags;

/** Earth, Stone & Metal. */
public final class EarthPowers {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final DustParticleOptions MAGNET = new DustParticleOptions(new Vector3f(0.7f, 0.2f, 0.9f), 1.2f);

    private EarthPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        return look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
    }

    private static boolean isMetal(Entity entity) {
        if (entity instanceof ItemEntity || entity instanceof IronGolem || entity instanceof AbstractMinecart) {
            return true;
        }
        return entity instanceof LivingEntity living && wearsArmor(living);
    }

    private static boolean isMetalBlock(BlockState state) {
        return state.is(Blocks.IRON_BLOCK) || state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL) || state.is(Blocks.IRON_BARS) || state.is(Blocks.IRON_DOOR)
                || state.is(Blocks.IRON_TRAPDOOR) || state.is(Blocks.CHAIN) || state.is(Blocks.GOLD_BLOCK)
                || state.is(Blocks.RAW_IRON_BLOCK) || state.is(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
                || state.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE) || state.is(Blocks.CAULDRON)
                || state.is(Blocks.LANTERN);
    }

    /** Dirt, stone, sand and other natural ground that powers may move or break. */
    static boolean isNatural(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0
                || state.getDestroySpeed(level, pos) > 5) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.COBBLESTONE) || state.is(Blocks.CLAY) || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE) || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.END_STONE)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUD) || state.is(Blocks.SNOW_BLOCK);
    }

    private static boolean onStone(ServerPlayer player) {
        BlockState s = below(player);
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER) || s.is(Blocks.COBBLESTONE)
                || s.is(Blocks.STONE_BRICKS) || s.is(Blocks.SMOOTH_STONE) || s.is(Blocks.DEEPSLATE)
                || s.is(Blocks.COBBLED_DEEPSLATE) || s.is(Blocks.END_STONE);
    }

    public static void register() {
        // Throw away everything metal around you. Sneak: rip out the metal block you look at and hurl it.
        // Arrows and tridents always stop in the air before they reach you.
        reg("magnetism", b()
                .stopsProjectiles((player, projectile) -> {
                    if (projectile instanceof AbstractArrow arrow) {
                        if (arrow.pickup == AbstractArrow.Pickup.ALLOWED) {
                            ItemEntity drop = new ItemEntity(arrow.level(), arrow.getX(), arrow.getY(), arrow.getZ(),
                                    arrow.getPickupItemStackOrigin().copy());
                            drop.setDeltaMovement(Vec3.ZERO);
                            arrow.level().addFreshEntity(drop);
                        }
                        particles(player, arrow.position(), MAGNET, 6, 0.1);
                        arrow.discard();
                        return true;
                    }
                    return false;
                })
                .active(3, player -> {
                    ServerLevel level = level(player);
                    if (player.isShiftKeyDown()) {
                        BlockPos pos = lookedAtPos(player, 24);
                        BlockState state = pos == null ? null : level.getBlockState(pos);
                        if (state == null || !isMetalBlock(state)) {
                            tell(player, "Look at a metal block (iron, gold, anvil, bars...)");
                            return false;
                        }
                        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
                        block.setHurtsEntities(3, 40);
                        setVelocity(block, player.getLookAngle().scale(1.5).add(0, 0.5, 0));
                        particleLine(player, player.position().add(0, 1, 0), Vec3.atCenterOf(pos), MAGNET, 0.6);
                        sound(player, SoundEvents.ANVIL_LAND, 0.6f, 1.5f);
                        return true;
                    }
                    int moved = 0;
                    for (Entity entity : entitiesAround(player, 16)) {
                        if (!isMetal(entity) || isFriend(player, entity)) {
                            continue;
                        }
                        push(player, entity, 2.0, 0.6);
                        if (entity instanceof LivingEntity living) {
                            hurt(player, living, 4);
                        }
                        particleLine(player, player.position().add(0, 1, 0), entity.position(), MAGNET, 0.8);
                        moved++;
                    }
                    if (moved == 0) {
                        tell(player, "No metal nearby");
                        return false;
                    }
                    sound(player, SoundEvents.BEACON_POWER_SELECT, 1, 0.6f);
                    return true;
                }));

        // Organic steel: very tough and strong, but you sink like a stone in water.
        reg("steel_skin", b()
                .attribute(Attributes.ARMOR, 10, ADD)
                .attribute(Attributes.ARMOR_TOUGHNESS, 4, ADD)
                .attribute(Attributes.ATTACK_DAMAGE, 3, ADD)
                .tick(player -> {
                    if (player.isInWater() && !player.onGround()) {
                        player.setDeltaMovement(player.getDeltaMovement().add(0, -0.04, 0));
                        player.hurtMarked = true;
                    }
                }));

        // Raise a pillar of earth where you look (lifting mobs on it). Sneak: sink a pit instead.
        reg("earth_shaping", b().active(2, player -> {
            ServerLevel level = level(player);
            BlockHitResult hit = lookedAtBlock(player, 24);
            if (hit.getType() == HitResult.Type.MISS) {
                tell(player, "Look at the ground");
                return false;
            }
            BlockPos base = hit.getBlockPos();
            BlockState ground = level.getBlockState(base);
            if (!isNatural(level, base, ground)) {
                tell(player, "You can only shape dirt, sand and stone");
                return false;
            }
            if (player.isShiftKeyDown()) {
                for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, -2, -1), base.offset(1, 0, 1))) {
                    if (isNatural(level, pos, level.getBlockState(pos))) {
                        TempBlocks.replace(level, pos.immutable(), Blocks.AIR.defaultBlockState(), 600);
                    }
                }
            } else {
                for (int h = 1; h <= 3; h++) {
                    for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, h, -1), base.offset(1, h, 1))) {
                        TempBlocks.place(level, pos.immutable(), ground, 600);
                    }
                }
                for (LivingEntity e : livingNear(player, Vec3.atCenterOf(base.above()), 2)) {
                    e.teleportTo(e.getX(), base.getY() + 4.1, e.getZ());
                }
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), base.getX() + 0.5,
                    base.getY() + 1, base.getZ() + 0.5, 40, 1, 1, 1, 0.1);
            sound(player, SoundEvents.ROOTED_DIRT_BREAK, 1, 0.6f);
            return true;
        }));

        // Stomp: every mob around you is knocked off its feet.
        reg("ground_stomp", b().active(6, player -> {
            if (!player.onGround()) {
                tell(player, "Stand on the ground to stomp");
                return false;
            }
            for (LivingEntity e : livingAround(player, 6)) {
                if (!isFriend(player, e) && e.onGround()) {
                    hurt(player, e, 4);
                    setVelocity(e, new Vec3(0, 0.7, 0));
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 2, 2);
                }
            }
            BlockState ground = below(player);
            level(player).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(),
                    player.getY() + 0.1, player.getZ(), 80, 3, 0.1, 3, 0.2);
            sound(player, SoundEvents.GENERIC_EXPLODE, 0.6f, 0.5f);
            return true;
        }));

        // Stone skin: a lot of armor, but you're a bit slower.
        reg("stone_skin", b()
                .attribute(Attributes.ARMOR, 8, ADD)
                .attribute(Attributes.MOVEMENT_SPEED, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        // Bore a tunnel 8 blocks forward through stone and dirt (you get the blocks).
        reg("tunneling", b().active(2, player -> {
            ServerLevel level = level(player);
            Vec3 look = player.getLookAngle();
            Vec3 start = player.position().add(0, 0.5, 0);
            int dug = 0;
            for (int i = 1; i <= 8; i++) {
                BlockPos feet = BlockPos.containing(start.add(look.scale(i)));
                for (BlockPos pos : new BlockPos[] { feet, feet.above() }) {
                    BlockState s = level.getBlockState(pos);
                    if (isNatural(level, pos, s) || s.is(Tags.Blocks.ORES)) {
                        level.destroyBlock(pos, true, player);
                        dug++;
                    }
                }
            }
            if (dug == 0) {
                tell(player, "Nothing to dig through");
                return false;
            }
            sound(player, SoundEvents.STONE_BREAK, 1, 0.6f);
            return true;
        }));

        // Sand body: arrows and falls pass harmlessly through you. R: a sandstorm blinds mobs around you.
        reg("sand_form", b()
                .immuneTo((player, source) -> source.is(DamageTypeTags.IS_PROJECTILE)
                        || source.is(DamageTypeTags.IS_FALL))
                .active(10, player -> {
                    BlockParticleOption sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
                    Zones.following(player, 5, 5, 10, sand, (zone, inside) -> {
                        for (LivingEntity e : inside) {
                            if (!isFriend(player, e)) {
                                effect(e, MobEffects.BLINDNESS, 2, 0);
                                hurt(player, e, 1);
                            }
                        }
                    });
                    sound(player, SoundEvents.SAND_BREAK, 1, 0.5f);
                    return true;
                }));

        // Grow sharp amethyst crystals where you look. Mobs standing in them get cut.
        reg("crystal_growth", b().active(10, player -> {
            ServerLevel level = level(player);
            BlockPos center = lookedAtPos(player, 24);
            if (center == null) {
                tell(player, "Look at the ground");
                return false;
            }
            int grown = 0;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
                if (player.getRandom().nextFloat() < 0.5f && level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                    TempBlocks.place(level, pos.immutable(), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 300);
                    grown++;
                }
            }
            if (grown == 0) {
                return false;
            }
            Zones.add(player, Vec3.atCenterOf(center.above()), 3.5, 15, 20, null, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        hurt(player, e, 3);
                    }
                }
            });
            sound(player, SoundEvents.AMETHYST_CLUSTER_PLACE, 1, 0.8f);
            return true;
        }));

        // Ores close to you glow through the stone.
        reg("ore_sense", b().every(60, player -> {
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            int shown = 0;
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-5, -5, -5), c.offset(5, 5, 5))) {
                if (shown < 24 && level.getBlockState(pos).is(Tags.Blocks.ORES)) {
                    Highlights.block(level, pos.immutable(), 0xFFD700, 3.1);
                    shown++;
                }
            }
        }));

        // A big earthquake for 5 seconds: the ground cracks and mobs are thrown around.
        reg("earthquake", b().active(40, player -> {
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            Scheduler.repeat(5, 20, () -> {
                for (int i = 0; i < 4; i++) {
                    BlockPos p = c.offset(player.getRandom().nextInt(21) - 10, 0, player.getRandom().nextInt(21) - 10);
                    BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p).below();
                    if (top.distSqr(c) > 9 && isNatural(level, top, level.getBlockState(top))) {
                        level.destroyBlock(top, true, player);
                    }
                }
                for (LivingEntity e : livingNear(player, Vec3.atCenterOf(c), 10)) {
                    if (!isFriend(player, e) && e.onGround()) {
                        setVelocity(e, new Vec3(player.getRandom().triangle(0, 0.4), 0.35,
                                player.getRandom().triangle(0, 0.4)));
                        hurt(player, e, 1.5f);
                    }
                }
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                        c.getX() + 0.5, c.getY() + 0.2, c.getZ() + 0.5, 60, 8, 0.1, 8, 0.1);
            });
            sound(player, SoundEvents.WARDEN_EMERGE, 2, 0.5f);
            return true;
        }));

        // Pull every mob in armor, iron golem, minecart and dropped item toward you.
        reg("iron_pull", b().active(4, player -> {
            int pulled = 0;
            for (Entity e : entitiesAround(player, 32)) {
                if (isMetal(e) && !isFriend(player, e)) {
                    Vec3 to = player.position().subtract(e.position());
                    setVelocity(e, to.normalize().scale(Math.min(2.0, to.length() * 0.15)).add(0, 0.3, 0));
                    pulled++;
                }
            }
            if (pulled == 0) {
                tell(player, "No metal nearby");
                return false;
            }
            sound(player, SoundEvents.CHAIN_PLACE, 1, 0.5f);
            return true;
        }));

        // R: turn to diamond (or back). As diamond you're almost unbreakable and hit very hard, but slow.
        reg("diamond_form", b()
                .every(10, player -> {
                    if (state(player).flag("diamond")) {
                        setModifier(player, Attributes.ARMOR, "diamond_armor", 20, ADD);
                        setModifier(player, Attributes.ARMOR_TOUGHNESS, "diamond_tough", 12, ADD);
                        setModifier(player, Attributes.ATTACK_DAMAGE, "diamond_attack", 6, ADD);
                        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, "diamond_kb", 1, ADD);
                        setModifier(player, Attributes.MOVEMENT_SPEED, "diamond_slow", -0.35,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                        level(player).sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1,
                                player.getZ(), 1, 0.3, 0.6, 0.3, 0);
                    }
                })
                .active(2, player -> {
                    boolean on = state(player).toggle("diamond");
                    if (!on) {
                        removeModifier(player, Attributes.ARMOR, "diamond_armor");
                        removeModifier(player, Attributes.ARMOR_TOUGHNESS, "diamond_tough");
                        removeModifier(player, Attributes.ATTACK_DAMAGE, "diamond_attack");
                        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, "diamond_kb");
                        removeModifier(player, Attributes.MOVEMENT_SPEED, "diamond_slow");
                    }
                    tell(player, on ? "Diamond form" : "Back to normal");
                    sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1, on ? 0.6f : 1.4f);
                    return true;
                }));

        // Rip a chunk of the ground out and throw it.
        PowerProjectiles.register("boulder", (owner, projectile, hit) -> {
            if (owner == null) {
                return;
            }
            for (LivingEntity e : livingNear(owner, hit.getLocation(), 2)) {
                hurt(owner, e, 8);
            }
            BlockState look = projectile instanceof net.minecraft.world.entity.projectile.ThrowableItemProjectile t
                    && t.getItem().getItem() instanceof net.minecraft.world.item.BlockItem bi
                            ? bi.getBlock().defaultBlockState()
                            : Blocks.STONE.defaultBlockState();
            level(owner).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, look), hit.getLocation().x,
                    hit.getLocation().y, hit.getLocation().z, 40, 0.5, 0.5, 0.5, 0.2);
            soundAt(owner, hit.getLocation(), SoundEvents.STONE_BREAK, 1.5f, 0.5f);
        });
        reg("boulder_throw", b().active(3, player -> {
            ServerLevel level = level(player);
            BlockPos pos = player.getOnPos();
            BlockState ground = level.getBlockState(pos);
            if (!isNatural(level, pos, ground)) {
                tell(player, "Stand on dirt, sand or stone");
                return false;
            }
            PowerProjectiles.shoot(player, "boulder", new ItemStack(ground.getBlock().asItem()), 1.4f, 0, true);
            sound(player, SoundEvents.STONE_BREAK, 1, 0.6f);
            return true;
        }));

        // The ground under a mob falls away for 10 seconds.
        reg("sinkhole", b().active(8, player -> {
            LivingEntity target = target(player, 24);
            if (target == null) {
                return false;
            }
            ServerLevel level = level(player);
            BlockPos base = target.blockPosition();
            int removed = 0;
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, -3, -1), base.offset(1, -1, 1))) {
                if (isNatural(level, pos, level.getBlockState(pos))) {
                    TempBlocks.replace(level, pos.immutable(), Blocks.AIR.defaultBlockState(), 200);
                    removed++;
                }
            }
            if (removed == 0) {
                tell(player, "The ground there is too hard");
                return false;
            }
            sound(player, SoundEvents.GRAVEL_BREAK, 1, 0.5f);
            return true;
        }));

        // Standing on stone makes you strong and tough.
        reg("mountain_strength", b().every(20, player -> {
            if (onStone(player)) {
                buff(player, MobEffects.DAMAGE_BOOST, 0);
                buff(player, MobEffects.DAMAGE_RESISTANCE, 0);
            }
        }));

        // The ground where you look turns to quicksand for 8 seconds: mobs sink, slow down and choke.
        reg("quicksand", b().active(12, player -> {
            Vec3 at = lookTarget(player, 24);
            BlockParticleOption sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
            Zones.add(player, at.add(0, 0.5, 0), 4, 8, 5, sand, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 1, 4);
                        setVelocity(e, new Vec3(e.getDeltaMovement().x * 0.2, -0.1, e.getDeltaMovement().z * 0.2));
                        if (zone.age % 20 == 0) {
                            hurt(player, e, 1.5f, player.damageSources().inWall());
                        }
                    }
                }
            });
            sound(player, SoundEvents.SAND_BREAK, 1, 0.4f);
            return true;
        }));

        // Ores sometimes drop twice as much.
        reg("lucky_miner", b().onBreak((player, pos, state) -> {
            if (state.is(Tags.Blocks.ORES) && player.getRandom().nextFloat() < 0.4f) {
                Block.dropResources(state, player.level(), pos);
                particles(player, Vec3.atCenterOf(pos), ParticleTypes.HAPPY_VILLAGER, 8, 0.4);
            }
        }));

        // R: stand on a floating rock for 20 seconds. Jump to rise, sneak to sink.
        reg("floating_rock", b()
                .every(4, player -> {
                    PowerState st = state(player);
                    ServerLevel level = level(player);
                    Integer y = st.get("rock_y");
                    if (y == null) {
                        return;
                    }
                    List<BlockPos> old = st.get("rock_blocks");
                    if (!st.active("rock", now(player))) {
                        if (old != null) {
                            old.forEach(p -> TempBlocks.restore(level, p));
                        }
                        st.remove("rock_y");
                        st.remove("rock_blocks");
                        return;
                    }
                    int wanted = player.isShiftKeyDown() ? y - 1 : Math.max(y, (int) Math.floor(player.getY()) - 1);
                    BlockPos feet = BlockPos.containing(player.getX(), wanted, player.getZ());
                    List<BlockPos> now = new ArrayList<>();
                    for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 0, 1))) {
                        now.add(pos.immutable());
                    }
                    if (old != null) {
                        for (BlockPos p : old) {
                            if (!now.contains(p)) {
                                TempBlocks.restore(level, p);
                            }
                        }
                    }
                    for (BlockPos p : now) {
                        TempBlocks.place(level, p, Blocks.COBBLESTONE.defaultBlockState(), 40);
                    }
                    st.put("rock_y", wanted);
                    st.put("rock_blocks", now);
                    player.resetFallDistance();
                    level.sendParticles(ParticleTypes.POOF, player.getX(), wanted, player.getZ(), 2, 1, 0, 1, 0);
                })
                .active(30, player -> {
                    PowerState st = state(player);
                    st.start("rock", now(player), 400);
                    st.put("rock_y", (int) Math.floor(player.getY()) - 1);
                    st.onClear(() -> {
                        List<BlockPos> old = st.get("rock_blocks");
                        if (old != null) {
                            old.forEach(p -> TempBlocks.restore(level(player), p));
                        }
                    });
                    tell(player, "Jump to rise, sneak to sink");
                    sound(player, SoundEvents.STONE_PLACE, 1, 0.6f);
                    return true;
                }));

        // Build a shell of obsidian around yourself for 6 seconds. Hits that still reach you are tiny.
        reg("obsidian_shell", b()
                .onHurt((player, event) -> {
                    if (state(player).active("shell", now(player))) {
                        event.setAmount(event.getAmount() * 0.2f);
                    }
                })
                .active(30, player -> {
                    ServerLevel level = level(player);
                    BlockPos c = player.blockPosition();
                    for (BlockPos pos : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 2, 1))) {
                        boolean inside = pos.getX() == c.getX() && pos.getZ() == c.getZ() && pos.getY() >= c.getY()
                                && pos.getY() <= c.getY() + 1;
                        if (!inside) {
                            TempBlocks.place(level, pos.immutable(), Blocks.OBSIDIAN.defaultBlockState(), 120);
                        }
                    }
                    state(player).start("shell", now(player), 120);
                    sound(player, SoundEvents.STONE_PLACE, 1, 0.4f);
                    return true;
                }));

        // While you stand still, you feel every mob walking on the ground near you.
        reg("tremor_sense", b().every(20, player -> {
            if (isStill(player) && player.onGround()) {
                for (LivingEntity e : livingAround(player, 20)) {
                    if (e.onGround()) {
                        glow(e, 1.5);
                    }
                }
            }
        }));
    }
}
