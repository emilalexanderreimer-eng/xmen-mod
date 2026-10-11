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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Tech, Matter & Evolution. */
public final class TechPowers {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    public static final String SUMMONED_TAG = "xmen_summoned";

    private TechPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    public static boolean isSummoned(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(SUMMONED_TAG);
    }

    private static int evolutionStage(ServerPlayer player) {
        return Math.min(6, player.experienceLevel / 5);
    }

    public static void register() {
        // Use a lever, button, door or other machine you look at from up to 48 blocks away.
        reg("technopathy", b().active(0.5, player -> {
            BlockHitResult hit = lookedAtBlock(player, 48);
            if (hit.getType() == HitResult.Type.MISS) {
                return false;
            }
            ServerLevel level = level(player);
            BlockState state = level.getBlockState(hit.getBlockPos());
            if (state.hasBlockEntity()) {
                tell(player, "You can't work that from here");
                return false;
            }
            var result = state.useWithoutItem(level, player, hit);
            if (!result.consumesAction()) {
                tell(player, "Nothing to switch there");
                return false;
            }
            particleLine(player, player.getEyePosition().add(player.getLookAngle()), hit.getLocation(),
                    ParticleTypes.ELECTRIC_SPARK, 0.6);
            return true;
        }));

        // Eat iron, gold or copper to repair your armor. Each metal meal also gives extra armor for a while.
        reg("techno_organic_body", b()
                .attribute(Attributes.ARMOR, 2, ADD)
                .active(3, player -> {
                    ItemStack held = player.getMainHandItem();
                    int value;
                    if (held.is(Items.IRON_INGOT) || held.is(Items.COPPER_INGOT)) {
                        value = 2;
                    } else if (held.is(Items.GOLD_INGOT)) {
                        value = 3;
                    } else if (held.is(Items.IRON_BLOCK)) {
                        value = 12;
                    } else if (held.is(Items.NETHERITE_SCRAP)) {
                        value = 10;
                    } else {
                        tell(player, "Hold an iron, gold or copper ingot to eat it");
                        return false;
                    }
                    held.shrink(1);
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack armor = player.getItemBySlot(slot);
                        if (slot.isArmor() && armor.isDamaged()) {
                            armor.setDamageValue(Math.max(0, armor.getDamageValue() - armor.getMaxDamage() * value / 20));
                        }
                    }
                    timedModifier(player, Attributes.ARMOR, "metal_meal", Math.min(10, value * 2), ADD, 60);
                    player.getFoodData().eat(2, 0.2f);
                    sound(player, SoundEvents.GENERIC_EAT, 1, 0.6f);
                    sound(player, SoundEvents.ANVIL_USE, 0.4f, 1.6f);
                    return true;
                }));

        // You learn from everything: 50% more XP.
        reg("super_genius", b().onXp((player, amount) -> {
            int bonus = amount / 2 + (player.getRandom().nextBoolean() ? amount % 2 : 0);
            if (bonus > 0) {
                Scheduler.later(1, () -> player.giveExperiencePoints(bonus));
            }
        }));

        // Open every door, trapdoor and gate and light every lamp around you for 5 seconds.
        reg("redstone_pulse", b().active(8, player -> {
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            List<BlockPos> changed = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-8, -4, -8), c.offset(8, 4, 8))) {
                BlockState s = level.getBlockState(pos);
                if (s.is(Blocks.IRON_DOOR) || s.is(Blocks.IRON_TRAPDOOR)) {
                    continue;
                }
                if (s.hasProperty(BlockStateProperties.OPEN) && !s.getValue(BlockStateProperties.OPEN)) {
                    level.setBlock(pos, s.setValue(BlockStateProperties.OPEN, true), 10);
                    changed.add(pos.immutable());
                } else if (s.is(Blocks.REDSTONE_LAMP) && !s.getValue(BlockStateProperties.LIT)) {
                    level.setBlock(pos, s.setValue(BlockStateProperties.LIT, true), 2);
                    changed.add(pos.immutable());
                }
            }
            if (changed.isEmpty()) {
                tell(player, "No doors or lamps nearby");
                return false;
            }
            Scheduler.later(100, () -> {
                for (BlockPos pos : changed) {
                    BlockState s = level.getBlockState(pos);
                    if (s.hasProperty(BlockStateProperties.OPEN)) {
                        level.setBlock(pos, s.setValue(BlockStateProperties.OPEN, false), 10);
                    } else if (s.hasProperty(BlockStateProperties.LIT)) {
                        level.setBlock(pos, s.setValue(BlockStateProperties.LIT, false), 2);
                    }
                }
            });
            ring(player, player.position().add(0, 0.2, 0), 8, new DustParticleOptions(new Vector3f(1, 0, 0), 1));
            sound(player, SoundEvents.LEVER_CLICK, 1, 0.8f);
            return true;
        }));

        // Dropped items and XP fly to you from 10 blocks away.
        reg("item_magnet", b().every(2, player -> {
            for (Entity e : entitiesAround(player, 10)) {
                if (e instanceof ItemEntity || e instanceof ExperienceOrb) {
                    Vec3 to = player.position().add(0, 0.5, 0).subtract(e.position());
                    setVelocity(e, to.normalize().scale(0.45));
                }
            }
        }));

        // Repair the tool in your hand and the armor you wear by a quarter.
        reg("repair_touch", b().active(30, player -> {
            int fixed = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = player.getItemBySlot(slot);
                if (item.isDamaged()) {
                    item.setDamageValue(Math.max(0, item.getDamageValue() - item.getMaxDamage() / 4));
                    fixed++;
                }
            }
            if (fixed == 0) {
                tell(player, "Nothing to repair");
                return false;
            }
            tell(player, "Repaired " + fixed + " items");
            sound(player, SoundEvents.ANVIL_USE, 1, 1.2f);
            return true;
        }));

        // Create a glowing energy sword out of nothing. It vanishes after 60 seconds.
        reg("weapon_summon", b()
                .every(20, player -> {
                    long now = now(player);
                    var inventory = player.getInventory();
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        ItemStack stack = inventory.getItem(i);
                        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                        if (data != null && data.contains(SUMMONED_TAG) && data.copyTag().getLong(SUMMONED_TAG) < now) {
                            inventory.setItem(i, ItemStack.EMPTY);
                        }
                    }
                })
                .active(60, player -> {
                    ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                    sword.set(DataComponents.CUSTOM_NAME, Component.literal("Energy Sword"));
                    sword.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
                    var enchantments = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                    sword.enchant(enchantments.getHolderOrThrow(Enchantments.SHARPNESS), 5);
                    CompoundTag tag = new CompoundTag();
                    tag.putLong(SUMMONED_TAG, now(player) + 1200);
                    sword.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    if (!player.getInventory().add(sword)) {
                        tell(player, "Your inventory is full");
                        return false;
                    }
                    state(player).onClear(() -> {
                        var inventory = player.getInventory();
                        for (int i = 0; i < inventory.getContainerSize(); i++) {
                            if (isSummoned(inventory.getItem(i))) {
                                inventory.setItem(i, ItemStack.EMPTY);
                            }
                        }
                    });
                    sound(player, SoundEvents.BEACON_POWER_SELECT, 1, 2);
                    return true;
                }));

        // Explosions can't hurt you.
        reg("blast_proof", b().immuneTo((player, source) -> source.is(DamageTypeTags.IS_EXPLOSION)));

        // Explode like a bomb without getting hurt.
        reg("self_detonation", b().active(60, player -> {
            explode(player, player.position(), 4, false);
            return true;
        }));

        // You take on the traits of what you stand on: stone gives armor, iron strength, gold speed,
        // wood healing, obsidian toughness, diamond everything.
        reg("matter_absorption", b().every(20, player -> {
            BlockState s = below(player);
            if (s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.STONE_BRICKS)) {
                timedModifier(player, Attributes.ARMOR, "matter_stone", 6, ADD, 1.5);
            } else if (s.is(Blocks.IRON_BLOCK) || s.is(Blocks.IRON_ORE) || s.is(Blocks.DEEPSLATE_IRON_ORE)) {
                buff(player, MobEffects.DAMAGE_BOOST, 1);
            } else if (s.is(Blocks.GOLD_BLOCK) || s.is(Blocks.GOLD_ORE)) {
                buff(player, MobEffects.MOVEMENT_SPEED, 2);
            } else if (s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS)) {
                buff(player, MobEffects.REGENERATION, 0);
            } else if (s.is(Blocks.OBSIDIAN) || s.is(Blocks.CRYING_OBSIDIAN)) {
                buff(player, MobEffects.DAMAGE_RESISTANCE, 2);
            } else if (s.is(Blocks.DIAMOND_BLOCK)) {
                buff(player, MobEffects.DAMAGE_BOOST, 1);
                buff(player, MobEffects.DAMAGE_RESISTANCE, 1);
                buff(player, MobEffects.MOVEMENT_SPEED, 1);
            }
        }));

        // Always see your coordinates and which way you face.
        reg("built_in_compass", b().every(10, player -> {
            tell(player, "X " + player.getBlockX() + "  Y " + player.getBlockY() + "  Z " + player.getBlockZ()
                    + "  facing " + player.getDirection().getName());
        }));

        // Keep your XP when you die.
        reg("perfect_memory", b().keepXp());

        // Metal wings: R launches you into the sky; hold jump to glide (XMenClient). Flying fast through mobs cuts them.
        reg("metal_wings", b()
                .onFall((player, distance) -> true)
                .every(4, player -> {
                    if (player.onGround()) {
                        state(player).setFlag("wings_launch", false);
                        return;
                    }
                    if (player.getDeltaMovement().length() > 0.6) {
                        for (LivingEntity e : livingAround(player, 1.8)) {
                            if (!isFriend(player, e)) {
                                hurt(player, e, 5);
                            }
                        }
                    }
                })
                .active(4, player -> {
                    setVelocity(player, player.getLookAngle().scale(0.6).add(0, 1.4, 0));
                    state(player).setFlag("wings_launch", true);
                    particles(player, player.position(), ParticleTypes.CRIT, 15, 0.4);
                    sound(player, SoundEvents.ARMOR_EQUIP_IRON, 1, 0.6f);
                    return true;
                }));

        // Lightning charges you up instead of hurting, and standing near powered redstone makes you strong.
        reg("living_battery", b()
                .immuneTo((player, source) -> {
                    if (source.is(DamageTypeTags.IS_LIGHTNING)) {
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1));
                        player.clearFire();
                        return true;
                    }
                    return false;
                })
                .every(20, player -> {
                    ServerLevel level = level(player);
                    if (level.getBestNeighborSignal(player.blockPosition()) > 0
                            || level.getBestNeighborSignal(player.blockPosition().below()) > 0) {
                        buff(player, MobEffects.DAMAGE_BOOST, 0);
                        buff(player, MobEffects.MOVEMENT_SPEED, 0);
                        particlesAround(player, player, ParticleTypes.ELECTRIC_SPARK, 4);
                    }
                }));

        // Open a crafting grid anywhere.
        reg("instant_crafting", b().active(1, player -> {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CraftingMenu(id, inventory,
                    ContainerLevelAccess.create(player.level(), player.blockPosition())) {
                @Override
                public boolean stillValid(Player who) {
                    return true;
                }
            }, Component.translatable("container.crafting")));
            return true;
        }));

        // R: energy armor for 20 seconds that gets stronger every time you're hit.
        reg("psionic_armor", b()
                .onHurt((player, event) -> {
                    PowerState st = state(player);
                    if (st.active("psi_armor", now(player))) {
                        double armor = Math.min(20, st.add("psi_points", 2));
                        setModifier(player, Attributes.ARMOR, "psi_armor", armor, ADD);
                        setModifier(player, Attributes.ARMOR_TOUGHNESS, "psi_tough", armor / 2, ADD);
                    }
                })
                .every(10, player -> {
                    PowerState st = state(player);
                    if (st.number("psi_points") > 0 && !st.active("psi_armor", now(player))) {
                        st.put("psi_points", 0.0);
                        removeModifier(player, Attributes.ARMOR, "psi_armor");
                        removeModifier(player, Attributes.ARMOR_TOUGHNESS, "psi_tough");
                    } else if (st.active("psi_armor", now(player))) {
                        particlesAround(player, player, ParticleTypes.ENCHANTED_HIT, 3);
                    }
                })
                .active(40, player -> {
                    PowerState st = state(player);
                    st.start("psi_armor", now(player), 400);
                    st.put("psi_points", 4.0);
                    setModifier(player, Attributes.ARMOR, "psi_armor", 4, ADD);
                    sound(player, SoundEvents.ARMOR_EQUIP_NETHERITE, 1, 1.5f);
                    return true;
                }));

        // Shoot sticky goo that glues mobs in place.
        PowerProjectiles.register("goo", (owner, projectile, hit) -> {
            if (owner == null) {
                return;
            }
            for (LivingEntity e : livingNear(owner, hit.getLocation(), 1.5)) {
                if (!isFriend(owner, e)) {
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 5, 3);
                    effect(e, MobEffects.JUMP, 5, 128);
                }
            }
            particles(owner, hit.getLocation(), ParticleTypes.ITEM_SLIME, 15, 0.4);
            soundAt(owner, hit.getLocation(), SoundEvents.SLIME_SQUISH, 1, 0.8f);
        });
        reg("sticky_goo", b().active(2, player -> {
            PowerProjectiles.shoot(player, "goo", new ItemStack(Items.SLIME_BALL), 1.4f, 0, true);
            sound(player, SoundEvents.SLIME_ATTACK, 1, 1);
            return true;
        }));

        // A planted bomb blinks until its owner sets it off.
        PowerProjectiles.register("time_bomb", (owner, projectile, hit) -> {
            if (owner == null) {
                return;
            }
            Vec3 at = hit.getLocation();
            List<Vec3> bombs = state(owner).get("bombs");
            if (bombs == null) {
                return;
            }
            bombs.add(at);
            tell(owner, bombs.size() + " bombs planted. Sneak + R to set them off");
            Scheduler.repeat(20, 60, () -> {
                if (bombs.contains(at)) {
                    level(owner).sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 2, 0.1, 0.1, 0.1, 0.01);
                }
            });
        });
        // Throw up to 3 sticky bombs. Sneak + R sets all of them off at once.
        reg("time_bombs", b().active(1, player -> {
            PowerState st = state(player);
            List<Vec3> bombs = st.get("bombs");
            if (bombs == null) {
                bombs = new ArrayList<>();
                st.put("bombs", bombs);
            }
            if (player.isShiftKeyDown()) {
                if (bombs.isEmpty()) {
                    tell(player, "No bombs planted");
                    return false;
                }
                for (Vec3 at : bombs) {
                    explode(player, at, 2.5f, false);
                }
                bombs.clear();
                return true;
            }
            if (bombs.size() >= 3) {
                tell(player, "3 bombs planted. Sneak + R to set them off");
                return false;
            }
            PowerProjectiles.shoot(player, "time_bomb", new ItemStack(Items.FIRE_CHARGE), 1.2f, 0, true);
            sound(player, SoundEvents.TNT_PRIMED, 0.6f, 1.6f);
            return false;
        }));

        // After something hurts you, your body adapts: that kind of damage can't hurt you for 30 seconds.
        reg("adaptation", b()
                .immuneTo((player, source) -> {
                    Object type = state(player).get("adapted");
                    return type != null && type.equals(source.typeHolder().getRegisteredName())
                            && state(player).active("adapt", now(player));
                })
                .onHurt((player, event) -> {
                    String type = event.getSource().typeHolder().getRegisteredName();
                    if (!type.equals(state(player).get("adapted")) || !state(player).active("adapt", now(player))) {
                        state(player).put("adapted", type);
                        state(player).start("adapt", now(player), 600);
                        tell(player, "Adapting... immune to " + event.getSource().type().msgId() + " for 30 seconds");
                    }
                }));

        // Start weak and evolve every 5 XP levels: speed, then strength, toughness, healing, fire and night
        // sight, and finally flight at level 30.
        reg("evolution", b().every(20, player -> {
            int stage = evolutionStage(player);
            if (stage >= 1) {
                buff(player, MobEffects.MOVEMENT_SPEED, 0);
            }
            if (stage >= 2) {
                buff(player, MobEffects.DAMAGE_BOOST, 0);
            }
            if (stage >= 3) {
                buff(player, MobEffects.DAMAGE_RESISTANCE, 0);
            }
            if (stage >= 4) {
                buff(player, MobEffects.REGENERATION, 0);
            }
            if (stage >= 5) {
                buff(player, MobEffects.FIRE_RESISTANCE, 0);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
            }
            boolean flies = stage >= 6;
            if (flies != state(player).flag("flight")) {
                PowerRuntime.setTemporaryFlight(player, flies);
            }
            int last = (int) state(player).number("evo_stage");
            if (stage > last) {
                String[] names = { "", "speed", "strength", "toughness", "healing", "fire and night sight", "flight" };
                chat(player, "You evolved! New ability: " + names[stage]);
                sound(player, SoundEvents.PLAYER_LEVELUP, 1, 0.6f);
            }
            state(player).put("evo_stage", (double) stage);
        }));
    }
}
