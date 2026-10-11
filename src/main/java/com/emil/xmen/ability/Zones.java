package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An area that does something to what's inside it for a while: poison clouds, storms, black holes,
 * walls of fire. A zone can follow an entity or drift in a direction.
 */
public final class Zones {
    @FunctionalInterface
    public interface ZoneEffect {
        /** inside holds the living things in the zone, not counting the owner. */
        void apply(Zone zone, List<LivingEntity> inside);
    }

    public static final class Zone {
        public final ServerLevel level;
        public final UUID owner;
        public final double radius;
        public final int period;
        public final ParticleOptions particle;
        public final ZoneEffect effect;
        public Vec3 center;
        public Vec3 velocity = Vec3.ZERO;
        public Entity follow;
        public int age;
        final int lifetime;

        Zone(ServerLevel level, UUID owner, Vec3 center, double radius, int lifetime, int period,
                ParticleOptions particle, ZoneEffect effect) {
            this.level = level;
            this.owner = owner;
            this.center = center;
            this.radius = radius;
            this.lifetime = lifetime;
            this.period = Math.max(1, period);
            this.particle = particle;
            this.effect = effect;
        }

        public ServerPlayer ownerPlayer() {
            return level.getServer().getPlayerList().getPlayer(owner);
        }

        /** All entities in the zone (not just living ones), not counting the owner. */
        public List<Entity> entities() {
            return level.getEntities((Entity) null, new AABB(center, center).inflate(radius),
                    e -> e.isAlive() && !e.getUUID().equals(owner) && e.position().distanceTo(center) <= radius + 0.5);
        }

        public void end() {
            age = lifetime;
        }
    }

    private static final List<Zone> ZONES = new ArrayList<>();

    private Zones() {
    }

    /** Adds a zone. effect runs every period ticks for seconds seconds. particle may be null. */
    public static Zone add(ServerPlayer owner, Vec3 center, double radius, double seconds, int period,
            ParticleOptions particle, ZoneEffect effect) {
        Zone zone = new Zone(owner.serverLevel(), owner.getUUID(), center, radius, (int) (seconds * 20), period,
                particle, effect);
        ZONES.add(zone);
        return zone;
    }

    public static Zone following(ServerPlayer owner, double radius, double seconds, int period,
            ParticleOptions particle, ZoneEffect effect) {
        Zone zone = add(owner, owner.position(), radius, seconds, period, particle, effect);
        zone.follow = owner;
        return zone;
    }

    static void tick() {
        if (ZONES.isEmpty()) {
            return;
        }
        List<Zone> current = new ArrayList<>(ZONES);
        for (Zone zone : current) {
            zone.age++;
            if (zone.follow != null) {
                if (!zone.follow.isAlive()) {
                    zone.end();
                } else {
                    zone.center = zone.follow.position().add(0, zone.follow.getBbHeight() / 2, 0);
                }
            }
            zone.center = zone.center.add(zone.velocity);
            if (zone.particle != null && zone.age % 4 == 0) {
                RandomSource random = zone.level.getRandom();
                int count = (int) Math.min(40, 4 + zone.radius * zone.radius);
                for (int i = 0; i < count; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double dist = Math.sqrt(random.nextDouble()) * zone.radius;
                    double y = (random.nextDouble() - 0.5) * Math.min(zone.radius, 3);
                    zone.level.sendParticles(zone.particle, zone.center.x + Math.cos(angle) * dist,
                            zone.center.y + y, zone.center.z + Math.sin(angle) * dist, 1, 0, 0, 0, 0);
                }
            }
            if (zone.age % zone.period == 0) {
                List<LivingEntity> inside = zone.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(zone.center, zone.center).inflate(zone.radius),
                        e -> e.isAlive() && !e.isSpectator() && !e.getUUID().equals(zone.owner)
                                && e.getBoundingBox().getCenter().distanceTo(zone.center) <= zone.radius + 0.5);
                zone.effect.apply(zone, inside);
            }
        }
        ZONES.removeIf(zone -> zone.age >= zone.lifetime);
    }
}
