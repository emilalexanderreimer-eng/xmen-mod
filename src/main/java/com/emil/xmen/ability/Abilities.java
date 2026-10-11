package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.emil.xmen.XMenMod;
import com.emil.xmen.entity.PowerClone;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared building blocks that many powers use: aiming, beams, particles, damage, buffs, teleporting. */
public final class Abilities {
    private static boolean dealingPowerDamage;
    private static UUID blastOwner;

    private Abilities() {
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.serverLevel();
    }

    public static long now(ServerPlayer player) {
        return player.level().getGameTime();
    }

    public static PowerState state(ServerPlayer player) {
        return PowerState.of(player);
    }

    // ------------------------------------------------------------------ aiming

    /** The block the player is looking at, up to range blocks away (type MISS if nothing). */
    public static BlockHitResult lookedAtBlock(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        return player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
    }

    public static BlockHitResult lookedAtBlockOrFluid(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        return player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                player));
    }

    /** Where the player's view ends: the block hit, or range blocks away. */
    public static Vec3 lookTarget(ServerPlayer player, double range) {
        return lookedAtBlock(player, range).getLocation();
    }

    /** The block position the player looks at, or null if they look at the sky. */
    public static BlockPos lookedAtPos(ServerPlayer player, double range) {
        BlockHitResult hit = lookedAtBlock(player, range);
        return hit.getType() == HitResult.Type.MISS ? null : hit.getBlockPos();
    }

    /** The first entity in the player's view, not hidden behind blocks. */
    public static Entity lookedAtEntity(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double max = lookTarget(player, range).distanceTo(start);
        Vec3 end = start.add(look.scale(max));
        AABB box = player.getBoundingBox().expandTowards(look.scale(max)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box,
                e -> e != player && !e.isSpectator() && e.isPickable(), max * max);
        return hit == null ? null : hit.getEntity();
    }

    /** The first living thing in the player's view, not hidden behind blocks. */
    public static LivingEntity lookedAtLiving(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double max = lookTarget(player, range).distanceTo(start);
        Vec3 end = start.add(look.scale(max));
        AABB box = player.getBoundingBox().expandTowards(look.scale(max)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box,
                e -> e instanceof LivingEntity && e != player && !e.isSpectator() && e.isPickable(), max * max);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /** Like lookedAtLiving, but tells the player to aim at a mob if they missed. */
    public static LivingEntity target(ServerPlayer player, double range) {
        LivingEntity target = lookedAtLiving(player, range);
        if (target == null) {
            tell(player, "Look at a mob first");
        }
        return target;
    }

    /** Every living thing along a straight beam from the player's eyes until it hits a block. */
    public static List<LivingEntity> beam(ServerPlayer player, double range, double width) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double length = lookTarget(player, range).distanceTo(start);
        Set<LivingEntity> hits = new LinkedHashSet<>();
        AABB area = player.getBoundingBox().expandTowards(look.scale(length)).inflate(width + 1);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator());
        for (double d = 0; d <= length; d += 0.5) {
            Vec3 point = start.add(look.scale(d));
            for (LivingEntity candidate : candidates) {
                if (candidate.getBoundingBox().inflate(width).contains(point)) {
                    hits.add(candidate);
                }
            }
        }
        return new ArrayList<>(hits);
    }

    /** Living things within range in front of the player, inside a cone of the given half-angle. */
    public static List<LivingEntity> cone(ServerPlayer player, double range, double halfAngleDegrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double minDot = Math.cos(Math.toRadians(halfAngleDegrees));
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity e : livingAround(player, range)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() <= range && to.normalize().dot(look) >= minDot && player.hasLineOfSight(e)) {
                result.add(e);
            }
        }
        return result;
    }

    public static List<LivingEntity> livingAround(ServerPlayer player, double radius) {
        return livingNear(player, player.position(), radius);
    }

    /** Living things within radius of a point, not counting the player. */
    public static List<LivingEntity> livingNear(ServerPlayer player, Vec3 center, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator()
                        && e.getBoundingBox().getCenter().distanceTo(center) <= radius + 0.5);
    }

    /** Monsters and other hostile mobs within radius. */
    public static List<LivingEntity> enemiesAround(ServerPlayer player, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity e : livingAround(player, radius)) {
            if (isEnemy(player, e)) {
                result.add(e);
            }
        }
        return result;
    }

    public static List<Entity> entitiesAround(ServerPlayer player, double radius) {
        return player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                e -> e.isAlive() && !e.isSpectator());
    }

    /** Other players, the player's pets, clones and allied mobs, and villagers. */
    public static boolean isFriend(ServerPlayer player, Entity entity) {
        if (entity == player) {
            return true;
        }
        if (entity instanceof Player || entity instanceof AbstractVillager) {
            return true;
        }
        if (entity instanceof TamableAnimal pet && pet.isTame() && player.getUUID().equals(pet.getOwnerUUID())) {
            return true;
        }
        if (entity instanceof PowerClone clone && player.getUUID().equals(clone.getOwnerId())) {
            return true;
        }
        return Allies.isAllyOf(entity, player.getUUID());
    }

    public static boolean isEnemy(ServerPlayer player, Entity entity) {
        return entity instanceof Enemy && !isFriend(player, entity);
    }

    // ------------------------------------------------------------------ effects

    public static void particleLine(ServerPlayer player, Vec3 from, Vec3 to, ParticleOptions particle, double spacing) {
        ServerLevel level = level(player);
        double length = from.distanceTo(to);
        if (length < 1.0E-3) {
            return;
        }
        Vec3 step = to.subtract(from).normalize();
        for (double d = 0; d <= length; d += spacing) {
            Vec3 p = from.add(step.scale(d));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    public static void particles(ServerPlayer player, Vec3 at, ParticleOptions particle, int count, double spread) {
        level(player).sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
    }

    public static void particlesAround(ServerPlayer player, Entity entity, ParticleOptions particle, int count) {
        Vec3 c = entity.getBoundingBox().getCenter();
        level(player).sendParticles(particle, c.x, c.y, c.z, count, entity.getBbWidth() / 2, entity.getBbHeight() / 2,
                entity.getBbWidth() / 2, 0.02);
    }

    /** Particles only this player can see. */
    public static void privateParticles(ServerPlayer player, Vec3 at, ParticleOptions particle, int count,
            double spread) {
        level(player).sendParticles(player, particle, true, at.x, at.y, at.z, count, spread, spread, spread, 0);
    }

    /** A ring of particles around a point. */
    public static void ring(ServerPlayer player, Vec3 center, double radius, ParticleOptions particle) {
        ServerLevel level = level(player);
        int points = (int) Math.max(12, radius * 10);
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    public static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                volume, pitch);
    }

    public static void sound(ServerPlayer player, Holder<SoundEvent> sound, float volume, float pitch) {
        sound(player, sound.value(), volume, pitch);
    }

    public static void soundAt(ServerPlayer player, Vec3 at, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void soundAt(ServerPlayer player, Vec3 at, Holder<SoundEvent> sound, float volume, float pitch) {
        soundAt(player, at, sound.value(), volume, pitch);
    }

    public static void tell(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GRAY), true);
    }

    public static void chat(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.AQUA));
    }

    // ------------------------------------------------------------------ damage

    /** Damage from a power, counted as the player's attack. Doesn't trigger melee-hit powers. */
    public static void hurt(ServerPlayer player, LivingEntity target, float damage) {
        hurt(player, target, damage, player.damageSources().playerAttack(player));
    }

    /** Damage that ignores armor. */
    public static void hurtMagic(ServerPlayer player, LivingEntity target, float damage) {
        hurt(player, target, damage, player.damageSources().indirectMagic(player, player));
    }

    public static void hurt(ServerPlayer player, LivingEntity target, float damage, DamageSource source) {
        if (target == player) {
            return;
        }
        if (isFriend(player, target) && !(target instanceof Player && player.server.isPvpAllowed())) {
            return;
        }
        boolean before = dealingPowerDamage;
        dealingPowerDamage = true;
        try {
            target.hurt(source, damage);
        } finally {
            dealingPowerDamage = before;
        }
    }

    /** True while a power is dealing damage, so melee-hit powers can skip it. */
    public static boolean isPowerDamage() {
        return dealingPowerDamage;
    }

    /** An explosion that never hurts the player who made it. */
    public static void explode(ServerPlayer player, Vec3 at, float power, boolean breakBlocks) {
        UUID before = blastOwner;
        blastOwner = player.getUUID();
        try {
            player.level().explode(player, at.x, at.y, at.z, power,
                    breakBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        } finally {
            blastOwner = before;
        }
    }

    static boolean isOwnBlast(ServerPlayer player) {
        return player.getUUID().equals(blastOwner);
    }

    // ------------------------------------------------------------------ buffs

    /** Gives a short effect that a ticking power keeps topping up. */
    public static void buff(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, 60, amplifier, true, false, true));
    }

    public static void effect(LivingEntity entity, Holder<MobEffect> effect, double seconds, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, (int) (seconds * 20), amplifier));
    }

    public static void glow(LivingEntity entity, double seconds) {
        entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, (int) (seconds * 20), 0,
                false, false));
    }

    /** Adds an attribute modifier that the power removes when it's switched off or changed. */
    public static void setModifier(ServerPlayer player, Holder<Attribute> attribute, String name, double amount,
            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, name);
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        PowerState state = PowerState.of(player);
        String key = "modifier:" + name + ":" + attribute.getRegisteredName();
        if (!state.flag(key)) {
            state.setFlag(key, true);
            state.onClear(() -> instance.removeModifier(id));
        }
    }

    public static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, String name) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, name));
        }
    }

    /** An attribute modifier that removes itself after seconds. */
    public static void timedModifier(ServerPlayer player, Holder<Attribute> attribute, String name, double amount,
            AttributeModifier.Operation operation, double seconds) {
        setModifier(player, attribute, name, amount, operation);
        Scheduler.later((int) (seconds * 20), () -> removeModifier(player, attribute, name));
    }

    // ------------------------------------------------------------------ movement

    /** Pushes an entity in the player's look direction. */
    public static void push(ServerPlayer player, Entity target, double strength, double lift) {
        Vec3 look = player.getLookAngle();
        target.setDeltaMovement(look.x * strength, Math.max(look.y * strength, 0) + lift, look.z * strength);
        target.hurtMarked = true;
    }

    /** Pushes an entity straight away from a point. */
    public static void pushFrom(Vec3 from, Entity target, double strength, double lift) {
        Vec3 away = target.position().subtract(from);
        away = new Vec3(away.x, 0, away.z);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 0) : away.normalize().scale(strength);
        target.setDeltaMovement(away.x, lift, away.z);
        target.hurtMarked = true;
    }

    /** Pulls an entity toward a point. */
    public static void pull(Vec3 to, Entity target, double strength) {
        Vec3 toward = to.subtract(target.position());
        if (toward.lengthSqr() < 1.0E-4) {
            return;
        }
        target.setDeltaMovement(target.getDeltaMovement().add(toward.normalize().scale(strength)));
        target.hurtMarked = true;
    }

    public static void setVelocity(Entity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
    }

    /** Throws the player in the direction they look. */
    public static void launchSelf(ServerPlayer player, double strength) {
        setVelocity(player, player.getLookAngle().scale(strength));
        player.resetFallDistance();
    }

    public static boolean hasRoom(ServerPlayer player, BlockPos feet) {
        return hasRoom(level(player), feet);
    }

    public static boolean hasRoom(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    public static void teleport(ServerPlayer player, Vec3 to) {
        player.teleportTo(to.x, to.y, to.z);
        player.resetFallDistance();
    }

    /** The open spot right in front of the block the player looks at, or null. */
    public static BlockPos teleportSpot(ServerPlayer player, double range) {
        BlockHitResult hit = lookedAtBlock(player, range);
        BlockPos spot;
        if (hit.getType() == HitResult.Type.MISS) {
            spot = BlockPos.containing(hit.getLocation());
        } else {
            spot = hit.getBlockPos().relative(hit.getDirection());
            if (!hasRoom(player, spot) && hasRoom(player, spot.below())) {
                spot = spot.below();
            }
        }
        return hasRoom(player, spot) ? spot : null;
    }

    /** Finds a safe place to stand near pos (searching up and down), or null. */
    public static BlockPos safeSpotNear(ServerLevel level, BlockPos pos) {
        for (int dy = 0; dy <= 12; dy++) {
            for (int sign : new int[] { 1, -1 }) {
                BlockPos candidate = pos.offset(0, dy * sign, 0);
                if (hasRoom(level, candidate)
                        && !level.getBlockState(candidate.below()).getCollisionShape(level, candidate.below())
                                .isEmpty()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ world checks

    public static boolean inSunlight(LivingEntity entity) {
        Level level = entity.level();
        BlockPos pos = BlockPos.containing(entity.getEyePosition());
        return level.isDay() && !level.isRaining() && level.canSeeSky(pos);
    }

    public static boolean isNightOutside(LivingEntity entity) {
        Level level = entity.level();
        return level.isNight() && level.canSeeSky(BlockPos.containing(entity.getEyePosition()));
    }

    public static int light(LivingEntity entity) {
        return entity.level().getMaxLocalRawBrightness(entity.blockPosition());
    }

    public static int blockLight(LivingEntity entity) {
        return entity.level().getBrightness(LightLayer.BLOCK, entity.blockPosition());
    }

    public static boolean isStill(ServerPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        return motion.x * motion.x + motion.z * motion.z < 0.0004 && !player.isSprinting();
    }

    public static String direction(ServerPlayer player, Vec3 to) {
        Vec3 d = to.subtract(player.position());
        String ns = d.z < -Math.abs(d.x) / 2 ? "north" : d.z > Math.abs(d.x) / 2 ? "south" : "";
        String ew = d.x > Math.abs(d.z) / 2 ? "east" : d.x < -Math.abs(d.z) / 2 ? "west" : "";
        String dir = ns.isEmpty() || ew.isEmpty() ? ns + ew : ns + "-" + ew;
        String vertical = d.y > 4 ? ", above" : d.y < -4 ? ", below" : "";
        return (int) d.length() + " blocks " + (dir.isEmpty() ? "here" : dir) + vertical;
    }

    // ------------------------------------------------------------------ world helpers

    private static final java.util.Map<String, Long> DAILY = new java.util.HashMap<>();

    /** True (and remembered) if the player hasn't used key yet in this Minecraft day. */
    public static boolean onceADay(ServerPlayer player, String key) {
        long day = player.level().getDayTime() / 24000L;
        String id = player.getUUID() + ":" + key;
        Long last = DAILY.get(id);
        if (last != null && last == day) {
            return false;
        }
        DAILY.put(id, day);
        return true;
    }

    /** Like bone meal on the block. Returns true if something grew. */
    public static boolean grow(ServerLevel level, BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock plant
                && !(state.getBlock() instanceof net.minecraft.world.level.block.GrassBlock)
                && plant.isValidBonemealTarget(level, pos, state)) {
            plant.performBonemeal(level, level.getRandom(), pos, state);
            level.levelEvent(1505, pos, 0);
            return true;
        }
        return false;
    }

    /** What a furnace would make from stack (one item), or empty. */
    public static net.minecraft.world.item.ItemStack smeltResult(ServerLevel level,
            net.minecraft.world.item.ItemStack stack) {
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack);
        return level.getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level)
                .map(r -> r.value().assemble(input, level.registryAccess()))
                .orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    /** Spawns a copy of the player that lasts seconds. */
    public static PowerClone spawnClone(ServerPlayer player, Vec3 at, int variant, double seconds) {
        PowerClone clone = com.emil.xmen.entity.ModEntities.CLONE.get().create(player.level());
        if (clone == null) {
            return null;
        }
        clone.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
        clone.setup(player, variant, (int) (seconds * 20));
        player.level().addFreshEntity(clone);
        return clone;
    }

    /** A lightning bolt that counts as the player's and never hurts them. */
    public static void lightning(ServerPlayer player, Vec3 at) {
        net.minecraft.world.entity.LightningBolt bolt =
                net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(player.level());
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setCause(player);
            player.level().addFreshEntity(bolt);
        }
    }

    /** The block under the player's feet. */
    public static net.minecraft.world.level.block.state.BlockState below(LivingEntity entity) {
        return entity.level().getBlockState(entity.getOnPos());
    }

    /** Does the entity wear any armor? */
    public static boolean wearsArmor(LivingEntity entity) {
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.isArmor() && !entity.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Freezes a mob solid for a while. */
    public static void freeze(LivingEntity target, double seconds) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + (int) (seconds * 20)));
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                (int) (seconds * 20), 4));
    }

    /** Makes a mob forget the player. */
    public static void forget(ServerPlayer player, LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player) {
            mob.setTarget(null);
        }
        if (entity.getLastHurtByMob() == player) {
            entity.setLastHurtByMob(null);
        }
        if (entity instanceof net.minecraft.world.entity.NeutralMob neutral) {
            neutral.stopBeingAngry();
        }
    }
}
