package com.emil.xmen.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import com.emil.xmen.XMenMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The powers that already work. Powers missing here exist but don't do anything yet. */
public final class PowerBehaviors {
    private static final Map<String, PowerBehavior> BEHAVIORS = new HashMap<>();

    private static final ResourceLocation CLAWS_ID = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "claws");
    private static final DustParticleOptions RED_BEAM = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 1.6f);
    private static final DustParticleOptions MAGNET = new DustParticleOptions(new Vector3f(0.7f, 0.2f, 0.9f), 1.2f);

    private static final Set<UUID> CLAWS_OUT = new HashSet<>();
    private static final Map<UUID, Held> HELD = new HashMap<>();

    private record Held(int entityId, long since) {
    }

    static {
        // ---- Energy & Blasts ----
        register("optic_blasts", PowerBehavior.builder().active(2, PowerBehaviors::opticBlast));

        // ---- Fire & Heat ----
        register("fireproof", PowerBehavior.builder().effect(MobEffects.FIRE_RESISTANCE, 0));
        register("pyrokinesis", PowerBehavior.builder().active(1.5, PowerBehaviors::fireballs));

        // ---- Ice & Water ----
        register("cryokinesis", PowerBehavior.builder().active(1.5, PowerBehaviors::iceBlast));
        register("gills", PowerBehavior.builder().effect(MobEffects.WATER_BREATHING, 0));

        // ---- Weather & Air ----
        register("weather_control", PowerBehavior.builder()
                .immuneTo((player, source) -> source.is(DamageTypeTags.IS_LIGHTNING))
                .active(1, PowerBehaviors::weatherControl));
        register("feather_fall", PowerBehavior.builder()
                .attribute(Attributes.FALL_DAMAGE_MULTIPLIER, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        // ---- Earth, Stone & Metal ----
        register("magnetism", PowerBehavior.builder().flight().active(3, PowerBehaviors::magnetism));

        // ---- Movement & Travel ----
        register("teleportation", PowerBehavior.builder().active(1.5, PowerBehaviors::teleport));
        register("super_speed", PowerBehavior.builder()
                .attribute(Attributes.MOVEMENT_SPEED, 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .attribute(Attributes.STEP_HEIGHT, 0.5, AttributeModifier.Operation.ADD_VALUE));
        register("wall_crawling", PowerBehavior.builder()); // climbing runs on the client, see XMenClient
        register("super_jump", PowerBehavior.builder()
                .attribute(Attributes.JUMP_STRENGTH, 0.4, AttributeModifier.Operation.ADD_VALUE)
                .attribute(Attributes.SAFE_FALL_DISTANCE, 10, AttributeModifier.Operation.ADD_VALUE));
        register("phasing", PowerBehavior.builder().active(2, PowerBehaviors::phase));
        register("flight", PowerBehavior.builder().flight().active(3, player -> {
            Abilities.launchSelf(player, 2.2);
            Abilities.sound(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1, 1);
            return true;
        }));

        // ---- Body & Strength ----
        register("super_strength", PowerBehavior.builder()
                .attribute(Attributes.ATTACK_DAMAGE, 5, AttributeModifier.Operation.ADD_VALUE)
                .attribute(Attributes.ATTACK_KNOCKBACK, 1, AttributeModifier.Operation.ADD_VALUE)
                .attribute(Attributes.BLOCK_BREAK_SPEED, 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .attribute(Attributes.KNOCKBACK_RESISTANCE, 0.5, AttributeModifier.Operation.ADD_VALUE));
        register("claws", PowerBehavior.builder().active(0.5, PowerBehaviors::toggleClaws));

        // ---- Healing & Life ----
        register("healing_factor", PowerBehavior.builder()
                .effect(MobEffects.REGENERATION, 1)
                .tick(player -> {
                    if (player.tickCount % 20 == 0) {
                        player.removeEffect(MobEffects.POISON);
                        player.removeEffect(MobEffects.WITHER);
                    }
                }));

        // ---- Senses & Detection ----
        register("night_vision", PowerBehavior.builder().effect(MobEffects.NIGHT_VISION, 0));

        // ---- Mind & Psychic ----
        register("telekinesis", PowerBehavior.builder()
                .tick(PowerBehaviors::holdTelekinesis)
                .active(2, PowerBehaviors::telekinesis));

        // ---- Light, Shadow & Stealth ----
        register("invisibility", PowerBehavior.builder().active(30, player -> {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false, true));
            for (LivingEntity nearby : Abilities.livingAround(player, 32)) {
                if (nearby instanceof Mob mob && mob.getTarget() == player) {
                    mob.setTarget(null);
                }
            }
            Abilities.particles(player, player.position().add(0, 1, 0), ParticleTypes.CLOUD, 20, 0.4);
            Abilities.sound(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1, 1.2f);
            Abilities.tell(player, "You are invisible for 10 seconds");
            return true;
        }));
    }

    private PowerBehaviors() {
    }

    private static void register(String id, PowerBehavior.Builder builder) {
        PowerBehavior behavior = builder.build();
        BEHAVIORS.put(id, behavior);
        behavior.attributes().forEach(bonus -> PowerRuntime.trackAttribute(bonus.attribute()));
    }

    public static PowerBehavior get(String id) {
        return BEHAVIORS.get(id);
    }

    public static boolean isImplemented(String id) {
        return BEHAVIORS.containsKey(id);
    }

    public static int count() {
        return BEHAVIORS.size();
    }

    /** Forgets temporary things like held mobs and drawn claws. */
    static void clearState(ServerPlayer player) {
        HELD.remove(player.getUUID());
        if (CLAWS_OUT.remove(player.getUUID())) {
            AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) {
                damage.removeModifier(CLAWS_ID);
            }
        }
    }

    // ---------------------------------------------------------------- abilities

    private static boolean opticBlast(ServerPlayer player) {
        Vec3 start = player.getEyePosition().add(0, -0.1, 0);
        Vec3 end = Abilities.lookTarget(player, 32);
        Abilities.particleLine(player, start.add(player.getLookAngle()), end, RED_BEAM, 0.4);
        for (LivingEntity target : Abilities.beam(player, 32, 0.4)) {
            Abilities.hurt(player, target, 8);
            Vec3 look = player.getLookAngle();
            target.knockback(1.2, -look.x, -look.z);
        }
        Abilities.sound(player, SoundEvents.BLAZE_SHOOT, 1, 1.6f);
        return true;
    }

    private static boolean fireballs(ServerPlayer player) {
        ServerLevel level = Abilities.level(player);
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < 3; i++) {
            Vec3 movement = look.add(player.getRandom().triangle(0, 0.08), player.getRandom().triangle(0, 0.08),
                    player.getRandom().triangle(0, 0.08));
            SmallFireball fireball = new SmallFireball(level, player, movement);
            Vec3 at = player.getEyePosition().add(look.scale(1.2));
            fireball.setPos(at.x, at.y, at.z);
            level.addFreshEntity(fireball);
        }
        Abilities.sound(player, SoundEvents.BLAZE_SHOOT, 1, 1);
        return true;
    }

    private static boolean iceBlast(ServerPlayer player) {
        ServerLevel level = Abilities.level(player);
        LivingEntity target = Abilities.lookedAtLiving(player, 30);
        Vec3 start = player.getEyePosition().add(player.getLookAngle());
        Vec3 end = target != null ? target.getBoundingBox().getCenter() : Abilities.lookTarget(player, 30);
        Abilities.particleLine(player, start, end, ParticleTypes.SNOWFLAKE, 0.4);
        if (target != null) {
            Abilities.hurt(player, target, 4);
            target.setTicksFrozen(target.getTicksRequiredToFreeze() + 120);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
        } else {
            // Freeze the water around where the blast lands.
            BlockPos center = BlockPos.containing(end);
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
                if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource()) {
                    level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                }
            }
        }
        Abilities.particles(player, end, ParticleTypes.SNOWFLAKE, 25, 0.5);
        Abilities.sound(player, SoundEvents.PLAYER_HURT_FREEZE, 1, 1.4f);
        return true;
    }

    // Look somewhere and press R: lightning strikes there. Sneak + R: change the weather.
    private static boolean weatherControl(ServerPlayer player) {
        ServerLevel level = Abilities.level(player);
        if (player.isShiftKeyDown()) {
            String now;
            if (level.isThundering()) {
                level.setWeatherParameters(12000, 0, false, false);
                now = "Clear skies";
            } else if (level.isRaining()) {
                level.setWeatherParameters(0, 12000, true, true);
                now = "Thunderstorm";
            } else {
                level.setWeatherParameters(0, 12000, true, false);
                now = "Rain";
            }
            Abilities.tell(player, "Weather: " + now);
            return true;
        }
        Vec3 target = Abilities.lookTarget(player, 64);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return false;
        }
        bolt.moveTo(target.x, target.y, target.z);
        bolt.setCause(player);
        level.addFreshEntity(bolt);
        return true;
    }

    private static boolean isMetal(Entity entity) {
        if (entity instanceof ItemEntity || entity instanceof IronGolem) {
            return true;
        }
        if (entity instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor() && !living.getItemBySlot(slot).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    // R: throw everything metal around you where you look. Sneak + R: pull it all to you.
    private static boolean magnetism(ServerPlayer player) {
        boolean pull = player.isShiftKeyDown();
        int moved = 0;
        for (Entity entity : Abilities.entitiesAround(player, 16)) {
            if (!isMetal(entity)) {
                continue;
            }
            if (pull) {
                Vec3 toPlayer = player.position().subtract(entity.position()).normalize().scale(1.4);
                entity.setDeltaMovement(toPlayer.add(0, 0.3, 0));
                entity.hurtMarked = true;
            } else {
                Abilities.push(player, entity, 2.0, 0.6);
                if (entity instanceof LivingEntity living) {
                    Abilities.hurt(player, living, 4);
                }
            }
            Abilities.particleLine(player, player.position().add(0, 1, 0), entity.position(), MAGNET, 0.8);
            moved++;
        }
        if (moved == 0) {
            Abilities.tell(player, "No metal nearby");
            return false;
        }
        Abilities.sound(player, SoundEvents.BEACON_POWER_SELECT, 1, 0.6f);
        return true;
    }

    private static boolean teleport(ServerPlayer player) {
        BlockPos spot = Abilities.teleportSpot(player, 48);
        if (spot == null) {
            Abilities.tell(player, "No room to teleport there");
            return false;
        }
        Abilities.particles(player, player.position().add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 30, 0.3);
        Abilities.sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 0.8f);
        Abilities.teleport(player, Vec3.atBottomCenterOf(spot));
        Abilities.particles(player, player.position().add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 30, 0.3);
        Abilities.sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 0.8f);
        return true;
    }

    // Walks you through the wall in front of you, up to 10 blocks thick.
    private static boolean phase(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            Abilities.tell(player, "Look at a wall to walk through it");
            return false;
        }
        flat = flat.normalize();
        Vec3 origin = player.position();
        boolean passedWall = false;
        for (double d = 0.8; d <= 12; d += 0.5) {
            BlockPos feet = BlockPos.containing(origin.add(flat.scale(d)));
            if (!Abilities.hasRoom(player, feet)) {
                passedWall = true;
            } else if (passedWall) {
                Abilities.particles(player, player.position().add(0, 1, 0), ParticleTypes.PORTAL, 40, 0.4);
                Abilities.teleport(player, Vec3.atBottomCenterOf(feet));
                Abilities.sound(player, SoundEvents.CHORUS_FRUIT_TELEPORT, 1, 1.2f);
                return true;
            }
        }
        Abilities.tell(player, "No wall to walk through (or it's too thick)");
        return false;
    }

    private static boolean toggleClaws(ServerPlayer player) {
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage == null) {
            return false;
        }
        if (CLAWS_OUT.remove(player.getUUID())) {
            damage.removeModifier(CLAWS_ID);
            Abilities.tell(player, "Claws in");
        } else {
            CLAWS_OUT.add(player.getUUID());
            damage.addOrUpdateTransientModifier(
                    new AttributeModifier(CLAWS_ID, 7, AttributeModifier.Operation.ADD_VALUE));
            Abilities.tell(player, "Claws out! Your punches hit much harder");
        }
        Abilities.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.6f);
        return true;
    }

    // First R grabs the mob you look at and holds it in front of you. Second R throws it.
    private static boolean telekinesis(ServerPlayer player) {
        Held held = HELD.remove(player.getUUID());
        if (held != null) {
            Entity entity = player.level().getEntity(held.entityId());
            if (entity != null) {
                entity.setDeltaMovement(player.getLookAngle().scale(2.5));
                entity.hurtMarked = true;
                Abilities.sound(player, SoundEvents.EVOKER_CAST_SPELL, 1, 0.8f);
            }
            return true;
        }
        LivingEntity target = Abilities.lookedAtLiving(player, 24);
        if (target == null) {
            Abilities.tell(player, "Look at a mob to lift it");
            return false;
        }
        HELD.put(player.getUUID(), new Held(target.getId(), player.level().getGameTime()));
        Abilities.sound(player, SoundEvents.EVOKER_CAST_SPELL, 1, 1.4f);
        Abilities.tell(player, "Press R again to throw it");
        return false;
    }

    private static void holdTelekinesis(ServerPlayer player) {
        Held held = HELD.get(player.getUUID());
        if (held == null) {
            return;
        }
        Entity entity = player.level().getEntity(held.entityId());
        long heldFor = player.level().getGameTime() - held.since();
        if (entity == null || !entity.isAlive() || entity.distanceTo(player) > 32 || heldFor > 200) {
            HELD.remove(player.getUUID());
            return;
        }
        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(4)).subtract(0, entity.getBbHeight() / 2,
                0);
        entity.setDeltaMovement(target.subtract(entity.position()).scale(0.4));
        entity.hurtMarked = true;
        entity.resetFallDistance();
        if (player.tickCount % 4 == 0) {
            Abilities.particles(player, entity.getBoundingBox().getCenter(), ParticleTypes.WITCH, 3, 0.3);
        }
    }

    static List<String> ids() {
        return List.copyOf(BEHAVIORS.keySet());
    }
}
