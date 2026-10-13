package com.emil.xmen.world;

import com.emil.xmen.entity.WeaponXItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.joml.Vector3f;

import com.emil.xmen.entity.ModEntities;
import com.emil.xmen.entity.MutantNpc;
import com.emil.xmen.entity.WeaponXEntities;
import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.Power;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The people and enemies that live in the X-Men structures. Structures place normal Minecraft mobs with a tag
 * like "xmen.pyro"; this class turns them into that character: name, health, gear and special moves.
 */
public final class MutantMobs {
    private static final String KIND = "xmen_kind";
    private static final String COOLDOWN = "xmen_cooldown";
    private static final String STEP = "xmen_step";
    private static final String HOME = "xmen_home";
    private static final Map<String, Kind> KINDS = new HashMap<>();
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private static final DustParticleOptions RED_LASER = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 1.2f);

    private MutantMobs() {
    }

    /** What a tag turns a mob into. */
    private static final class Kind {
        final String id;
        String[] names;
        ChatFormatting color = ChatFormatting.WHITE;
        double health;
        double damage;
        double speed = 1.0;
        double scale = 1.0;
        double knockbackResistance;
        BossEvent.BossBarColor bar;
        String ability = "";
        boolean hostile = true;
        boolean showName;
        boolean sits;
        String[] lines;
        Consumer<Mob> gear = mob -> {
        };

        Kind(String id, String... names) {
            this.id = id;
            this.names = names;
        }

        Kind color(ChatFormatting value) {
            color = value;
            return this;
        }

        Kind health(double value) {
            health = value;
            return this;
        }

        Kind damage(double value) {
            damage = value;
            return this;
        }

        Kind speed(double value) {
            speed = value;
            return this;
        }

        Kind scale(double value) {
            scale = value;
            return this;
        }

        Kind heavy() {
            knockbackResistance = 1.0;
            return this;
        }

        Kind boss(BossEvent.BossBarColor value) {
            bar = value;
            showName = true;
            return this;
        }

        Kind ability(String value) {
            ability = value;
            return this;
        }

        Kind friendly(String... value) {
            hostile = false;
            showName = true;
            lines = value;
            return this;
        }

        Kind sits() {
            sits = true;
            return this;
        }

        Kind gear(Consumer<Mob> value) {
            gear = value;
            return this;
        }
    }

    private static Kind kind(String id, String... names) {
        Kind kind = new Kind(id, names);
        KINDS.put(id, kind);
        return kind;
    }

    static {
        // --- friendly people
        kind("student", "Jubilee", "Kitty Pryde", "Bobby Drake", "Rogue", "Kurt", "Piotr", "Laura", "Hope", "Sam",
                "Dani Moonstar", "Rahne", "Roberto", "Jean", "Scott", "Hank", "Warren", "Ororo", "Remy", "Logan")
                .color(ChatFormatting.AQUA)
                .friendly("Welcome to Xavier's School for Gifted Youngsters!", "Have you tried the Danger Room yet? "
                        + "The elevator next to the lounge goes down.", "Professor X is upstairs in his study.",
                        "Cerebro can find every mutant in the world.", "The flower gate in the garden goes to "
                                + "Krakoa!", "Don't tell anyone where the X-Jet is.");
        kind("professor_x", "Professor X").color(ChatFormatting.LIGHT_PURPLE).health(60).sits()
                .friendly("Welcome, young mutant. Let me look into your mind...",
                        "Cerebro is downstairs. Take the elevator in the hall and look for the round door.",
                        "Mutants are not to be feared. We are simply the next step.");

        // --- the X-Men (movie team), living in the mansion
        kind("wolverine", "Wolverine").color(ChatFormatting.YELLOW).health(80).damage(10).ability("regenerate")
                .friendly("What are you lookin' at, bub?", "Don't touch my bike.",
                        "If you see Magneto, tell him I said hi. With these.", "The Danger Room's no fun anymore.");
        kind("cyclops", "Cyclops").color(ChatFormatting.BLUE).health(60).ability("laser")
                .friendly("Stay in formation.", "The X-Jet is ready if we need it. The hangar is under the "
                        + "basketball court.", "These glasses are ruby quartz. Without them I'd blow a hole in the "
                        + "wall.");
        kind("storm", "Storm").color(ChatFormatting.WHITE).health(60).ability("lightning")
                .friendly("Do you know what happens to a toad when it's struck by lightning?",
                        "I teach the students when the Professor is busy.", "The weather is lovely today. I made it "
                                + "that way.");
        kind("jean_grey", "Jean Grey").color(ChatFormatting.RED).health(60).ability("telekinesis")
                .friendly("I'm the doctor here. The med lab is downstairs if you get hurt.",
                        "Sometimes I can hear what everyone is thinking. It's very loud.",
                        "Cerebro amplifies the mind. It can be dangerous for anyone but the Professor.");
        kind("beast", "Beast").color(ChatFormatting.DARK_BLUE).health(70).damage(8).speed(1.2)
                .friendly("Oh my stars and garters!", "I'm working on a new formula in the lab.",
                        "Have you read the books in the library? Simply marvelous.");
        kind("rogue", "Rogue").color(ChatFormatting.DARK_GREEN).health(50).ability("drain")
                .friendly("Don't touch my skin, sugar. Trust me.", "This place was the first real home I ever had.");
        kind("captive_rogue", "Rogue").color(ChatFormatting.DARK_GREEN).health(50)
                .friendly("Get me out of here! Magneto wants to use my power for his machine.",
                        "Break the glass, sugar. Just don't touch my skin.",
                        "His machine turns people into mutants... and it would kill me.");
        kind("iceman", "Iceman").color(ChatFormatting.AQUA).health(50).ability("freeze")
                .friendly("Want a snow cone? I can make one.", "Pyro and I used to be friends...");
        kind("nightcrawler", "Nightcrawler").color(ChatFormatting.DARK_PURPLE).health(50).ability("teleport")
                .friendly("Guten Tag! Do not be afraid of how I look.", "*BAMF*",
                        "I like the circus. And the church. And this school.");
        kind("colossus", "Colossus").color(ChatFormatting.GRAY).health(110).damage(10).speed(0.85).heavy()
                .friendly("Hello, comrade.", "I am drawing a picture of the garden.", "I am made of steel. "
                        + "Hit me if you like, it will not hurt.");
        kind("kitty_pryde", "Kitty Pryde").color(ChatFormatting.LIGHT_PURPLE).health(40).ability("phase")
                .friendly("I just walk through walls. Doors are for other people.", "Don't tell Logan I ate his "
                        + "pudding.");
        kind("jubilee", "Jubilee").color(ChatFormatting.GOLD).health(40).ability("fireworks")
                .friendly("Wanna see some fireworks?", "The mall is way more fun than class.");
        kind("gambit", "Gambit").color(ChatFormatting.DARK_RED).health(55).ability("cards")
                .friendly("Remy LeBeau, at your service.", "Wanna play cards, mon ami?");
        kind("morlock", "Sunder", "Tommy", "Berto", "Erg", "Ape", "Plague", "Scaleface", "Skids", "Annalee",
                "Cybelle", "Beautiful Dreamer", "Tar Baby")
                .color(ChatFormatting.GRAY)
                .friendly("Surface folk don't come down here often.", "We Morlocks keep to the tunnels.",
                        "Leave us in peace and we'll leave you in peace.", "Take the cart at the station, it goes "
                                + "down to the Alley.", "Healer can fix you up. His hut is next to the cart stop.",
                        "Callisto leads us. Anyone can challenge her... nobody ever wins.",
                        "Caliban can feel every mutant in the world. Ask him where to go.");
        // --- the Morlock leaders (0.6.2)
        kind("callisto", "Callisto").color(ChatFormatting.DARK_PURPLE).health(160).damage(9).speed(1.25)
                .boss(BossEvent.BossBarColor.PURPLE).ability("knife")
                .friendly("I'm Callisto. I lead the Morlocks.");
        kind("caliban", "Caliban").color(ChatFormatting.WHITE).health(50)
                .friendly("Caliban feels mutants. Caliban feels you.");
        kind("healer", "Healer").color(ChatFormatting.GREEN).health(60)
                .friendly("Healer helps.");
        kind("masque", "Masque").color(ChatFormatting.GRAY).health(45)
                .friendly("I can change any face. Want a new one? Heh. Didn't think so.",
                        "Up there they call us freaks. Down here we're family.");
        kind("leech", "Leech").color(ChatFormatting.GREEN).health(25).scale(0.7)
                .friendly("When I'm close, other mutants' powers just... turn off. Sorry.",
                        "Kitty Pryde played with me once. She was nice.");
        kind("marrow", "Marrow").color(ChatFormatting.LIGHT_PURPLE).health(55).damage(8)
                .friendly("I grow my own knives. Bones, I mean.", "This station is ours. Callisto's orders.",
                        "Don't touch the train. Masque lives in it sometimes.");
        kind("krakoa_mutant", "Cypher", "Wolfsbane", "Magik", "Sunspot", "Mirage", "Karma", "Armor", "Pixie")
                .color(ChatFormatting.GREEN)
                .friendly("Welcome to Krakoa. Every mutant is safe here.", "No monsters can spawn on Krakoa.",
                        "The gate in the cave goes to the X-Mansion.");
        kind("refugee", "Mutant Refugee").color(ChatFormatting.YELLOW)
                .friendly("Please don't tell the Sentinels I'm here.", "I'm just trying to stay hidden.",
                        "Help yourself to some food.");
        kind("moira", "Moira MacTaggert").color(ChatFormatting.GOLD)
                .friendly("Welcome to Muir Island. We study mutant genes here.",
                        "Check the chests, I left a map to the X-Mansion somewhere.");
        kind("scientist", "Muir Island Researcher").color(ChatFormatting.YELLOW)
                .friendly("Fascinating... your genes are very unusual.", "Moira knows more than I do.");

        // --- the Brotherhood
        kind("pyro", "Pyro").color(ChatFormatting.RED).health(40).ability("fire")
                .gear(mob -> hold(mob, Items.BLAZE_ROD));
        kind("avalanche", "Avalanche").color(ChatFormatting.GOLD).health(45).ability("quake");
        kind("blob", "The Blob").color(ChatFormatting.GOLD).health(90).damage(7).speed(0.8).scale(1.6).heavy();
        kind("toad", "Toad").color(ChatFormatting.DARK_GREEN).health(30).speed(1.2).ability("tongue");
        kind("mystique", "Mystique").color(ChatFormatting.BLUE).health(45).ability("shift");
        kind("thug", "Brotherhood Thug").color(ChatFormatting.DARK_RED).health(30)
                .gear(mob -> wear(mob, EquipmentSlot.HEAD, Items.IRON_HELMET));

        // --- Sentinels and Trask
        kind("sentinel", "Sentinel").color(ChatFormatting.LIGHT_PURPLE).health(120).scale(1.25).ability("laser");
        kind("giant_sentinel", "Sentinel Mark I").color(ChatFormatting.LIGHT_PURPLE).health(500).damage(20)
                .scale(3.0).heavy().boss(BossEvent.BossBarColor.PINK).ability("giant_laser");
        kind("trask_guard", "Trask Guard").color(ChatFormatting.GRAY).health(30)
                .gear(mob -> wear(mob, EquipmentSlot.HEAD, Items.IRON_HELMET));
        kind("tower_guard", "Anti-Mutant Guard").color(ChatFormatting.GRAY).health(30)
                .gear(mob -> wear(mob, EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET));

        // --- Danger Room
        kind("training_robot", "Training Robot").color(ChatFormatting.GRAY).health(24).speed(1.1)
                .gear(mob -> {
                    wear(mob, EquipmentSlot.HEAD, Items.IRON_HELMET);
                    wear(mob, EquipmentSlot.CHEST, Items.IRON_CHESTPLATE);
                });
        kind("laser_drone", "Laser Drone").color(ChatFormatting.GRAY).health(20)
                .gear(mob -> wear(mob, EquipmentSlot.HEAD, Items.IRON_HELMET));
        kind("training_sentinel", "Sentinel Simulation").color(ChatFormatting.LIGHT_PURPLE).health(90).scale(1.3)
                .ability("laser");

        // --- Weapon X
        kind("weapon_x_guard", "Weapon X Guard").color(ChatFormatting.GRAY).health(32)
                .gear(mob -> wear(mob, EquipmentSlot.HEAD, Items.IRON_HELMET));
        kind("weapon_x_scientist", "Weapon X Scientist").color(ChatFormatting.WHITE).health(30);
        kind("feral_subject", "Feral Subject").color(ChatFormatting.DARK_RED).health(40).speed(1.35).damage(6)
                .ability("regenerate").gear(mob -> hold(mob, Items.IRON_SWORD));

        // --- Hellfire Club
        kind("white_queen", "The White Queen").color(ChatFormatting.WHITE).health(200)
                .boss(BossEvent.BossBarColor.WHITE).ability("psychic");
        kind("hellfire_guard", "Hellfire Guard").color(ChatFormatting.DARK_RED).health(35);

        // --- Genosha
        kind("scavenger", "Genoshan Scavenger").color(ChatFormatting.GRAY).health(26);
        // --- Magneto's people on Genosha (X-Men: Dark Phoenix, 0.6.2)
        kind("selene", "Selene").color(ChatFormatting.DARK_PURPLE).health(70).damage(7).ability("drain");
        kind("ariki", "Ariki").color(ChatFormatting.DARK_GRAY).health(55).ability("hair");
        kind("red_lotus", "Red Lotus").color(ChatFormatting.RED).health(50).speed(1.25).ability("pounce");
        kind("genoshan", "Ana", "Kofi", "Mira", "Tomas", "Leni", "Jun", "Amara").color(ChatFormatting.YELLOW)
                .friendly("Erik gave us this island. A home for every mutant.", "The Sentinels came at night. "
                        + "Most of the city is gone now.", "Watch out for Selene and her friends. They don't like "
                                + "visitors.", "The Magneto statue still stands. Mostly.");

        // --- the movie Brotherhood (0.6.2)
        kind("sabretooth", "Sabretooth").color(ChatFormatting.GOLD).health(90).damage(9).speed(1.15)
                .ability("pounce");
        kind("juggernaut", "Juggernaut").color(ChatFormatting.DARK_RED).health(160).damage(12).speed(0.9).scale(1.3)
                .heavy().ability("charge");
        kind("multiple_man", "Multiple Man").color(ChatFormatting.GOLD).health(40).ability("copies");
        kind("multiple_man_copy", "Multiple Man").color(ChatFormatting.GOLD).health(10).damage(4);

        // --- the Hellfire Club (X-Men: First Class, 0.6.2)
        kind("sebastian_shaw", "Sebastian Shaw").color(ChatFormatting.DARK_RED).health(220).damage(9)
                .boss(BossEvent.BossBarColor.RED).ability("shockwave");
        kind("azazel", "Azazel").color(ChatFormatting.RED).health(60).damage(8).ability("teleport")
                .gear(mob -> hold(mob, Items.IRON_SWORD));
        kind("riptide", "Riptide").color(ChatFormatting.GRAY).health(55).ability("whirlwind");

        // --- the Detection Tower's soldiers (0.6.2)
        kind("anti_mutant_soldier", "Anti-Mutant Soldier").color(ChatFormatting.GRAY).health(30).ability("rifle")
                .gear(mob -> hold(mob, WeaponXEntities.RIFLE.get()));

    }

    public static void register() {
        MutantNpc.professorOffers = MutantMobs::professorOffers;
        NeoForge.EVENT_BUS.addListener(MutantMobs::onJoin);
        NeoForge.EVENT_BUS.addListener(MutantMobs::onLeave);
        NeoForge.EVENT_BUS.addListener(MutantMobs::onTick);
        NeoForge.EVENT_BUS.addListener(MutantMobs::onDeath);
        NeoForge.EVENT_BUS.addListener(MutantMobs::onDrops);
        NeoForge.EVENT_BUS.addListener(MutantMobs::onExperience);
        NeoForge.EVENT_BUS.addListener(MutantMobs::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(MutantMobs::onInteract);
    }

    private static Kind kindOf(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(KIND) ? KINDS.get(data.getString(KIND)) : null;
    }

    public static boolean hasPower(Player player) {
        return !player.getData(PlayerPowers.POWER).isEmpty();
    }

    // ------------------------------------------------------------------ setup
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Kind kind = kindOf(mob);
        if (kind == null) {
            for (String tag : mob.getTags()) {
                if (tag.startsWith("xmen.") && KINDS.containsKey(tag.substring(5))) {
                    kind = KINDS.get(tag.substring(5));
                    setup(mob, kind);
                    break;
                }
            }
        }
        if (kind != null) {
            addGoals(mob, kind);
        }
    }

    /** First time only: name, health, gear. Saved with the mob. */
    private static void setup(Mob mob, Kind kind) {
        mob.getPersistentData().putString(KIND, kind.id);
        if (mob instanceof MutantNpc npc) {
            if (npc.getCharacter().isEmpty()) {
                npc.setCharacter(kind.id);
            }
            npc.setHostile(kind.hostile);
            npc.setSitting(kind.sits);
        }
        String name = kind.names[mob.getRandom().nextInt(kind.names.length)];
        mob.setCustomName(Component.literal(name).withStyle(kind.color));
        mob.setCustomNameVisible(kind.showName);
        mob.setPersistenceRequired();
        if (kind.health > 0) {
            setBase(mob, Attributes.MAX_HEALTH, kind.health);
            mob.setHealth((float) kind.health);
        }
        if (kind.damage > 0) {
            setBase(mob, Attributes.ATTACK_DAMAGE, kind.damage);
        }
        if (kind.speed != 1.0) {
            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * kind.speed);
            }
        }
        if (kind.scale != 1.0) {
            setBase(mob, Attributes.SCALE, kind.scale);
        }
        if (kind.knockbackResistance > 0) {
            setBase(mob, Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance);
        }
        kind.gear.accept(mob);
        if (kind.id.equals("callisto")) {
            mob.getPersistentData().putLong(HOME, mob.blockPosition().asLong());
        }
        if (kind.id.equals("professor_x") && mob instanceof WanderingTrader trader) {
            trader.setDespawnDelay(0);
            MerchantOffers offers = trader.getOffers();
            offers.clear();
            offers.addAll(professorOffers());
        }
    }

    /** What Professor X sells. */
    public static MerchantOffers professorOffers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(Items.EMERALD, 6, ModContent.XMEN_MASK.get(), 1, 99));
        offers.add(offer(Items.EMERALD, 10, ModContent.XMEN_SUIT.get(), 1, 99));
        offers.add(offer(Items.EMERALD, 8, ModContent.XMEN_PANTS.get(), 1, 99));
        offers.add(offer(Items.EMERALD, 6, ModContent.XMEN_BOOTS.get(), 1, 99));
        offers.add(offer(Items.EMERALD, 12, ModContent.ELEVATOR_ITEM.get(), 4, 99));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 64), java.util.Optional.of(
                new ItemCost(Items.DIAMOND, 8)), new ItemStack(ModContent.MUTANT_SERUM.get()), 3, 10, 0.0f));
        return offers;
    }

    private static MerchantOffer offer(ItemLike cost, int count, ItemLike result, int resultCount, int uses) {
        return new MerchantOffer(new ItemCost(cost, count), new ItemStack(result, resultCount), uses, 5, 0.0f);
    }

    /** Every time the mob is loaded: AI that isn't saved. */
    private static void addGoals(Mob mob, Kind kind) {
        if (mob instanceof IronGolem golem && kind.hostile) {
            golem.targetSelector.removeAllGoals(goal -> true);
            golem.targetSelector.addGoal(1, new HurtByTargetGoal(golem));
            boolean training = kind.id.startsWith("training");
            golem.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(golem, Player.class, 10, true, false,
                    target -> target instanceof Player player && !player.isCreative() && !player.isSpectator()
                            && (training || hasPower(player))));
        }
    }

    private static void setBase(LivingEntity mob, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static void wear(Mob mob, EquipmentSlot slot, ItemLike item) {
        mob.setItemSlot(slot, new ItemStack(item));
        mob.setDropChance(slot, 0.0f);
    }

    private static void hold(Mob mob, ItemLike item) {
        wear(mob, EquipmentSlot.MAINHAND, item);
    }

    // ------------------------------------------------------------------ ticking
    private static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.tickCount % 10 != 0 || !(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level)
                || !mob.isAlive()) {
            return;
        }
        Kind kind = kindOf(mob);
        if (kind == null) {
            return;
        }
        if (kind.bar != null) {
            if (!(mob instanceof MutantNpc npc) || npc.isHostile()) {
                updateBar(level, mob, kind);
            } else {
                removeBar(mob);    // Callisto only shows her bar once she's been challenged
            }
        }
        if (kind.ability.isEmpty() || WeaponXItems.isSuppressed(mob)) {
            return;  // an inhibitor collar switches the special moves (and the healing) off
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target.level() != level) {
            if (kind.ability.equals("regenerate")) {
                mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, true, false));
            }
            return;
        }
        long now = level.getGameTime();
        CompoundTag data = mob.getPersistentData();
        if (now < data.getLong(COOLDOWN)) {
            return;
        }
        int step = data.getInt(STEP);
        int wait = useAbility(level, mob, kind, target, step);
        if (wait > 0) {
            data.putLong(COOLDOWN, now + wait);
            data.putInt(STEP, step + 1);
        }
    }

    /** Does the mob's special move. Returns how many ticks to wait before the next one (0 = didn't do anything). */
    private static int useAbility(ServerLevel level, Mob mob, Kind kind, LivingEntity target, int step) {
        double distance = mob.distanceTo(target);
        boolean sees = mob.hasLineOfSight(target);
        switch (kind.ability) {
            case "fire" -> {
                if (distance > 18 || !sees) {
                    return 0;
                }
                for (int i = 0; i < 3; i++) {
                    Vec3 dir = target.getEyePosition().subtract(mob.getEyePosition()).normalize();
                    SmallFireball ball = new SmallFireball(level, mob, dir.add(mob.getRandom().nextGaussian() * 0.05,
                            0, mob.getRandom().nextGaussian() * 0.05));
                    ball.setPos(mob.getX(), mob.getEyeY() - 0.1, mob.getZ());
                    level.addFreshEntity(ball);
                }
                sound(level, mob, SoundEvents.BLAZE_SHOOT);
                return 60;
            }
            case "quake" -> {
                if (distance > 10) {
                    return 0;
                }
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox()
                        .inflate(8), e -> e != mob && kindOf(e) == null && e.onGround())) {
                    victim.hurt(mob.damageSources().mobAttack(mob), 5);
                    victim.push(0, 0.7, 0);
                    victim.hurtMarked = true;
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
                level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY(), mob.getZ(), 6, 3, 0.2, 3, 0);
                sound(level, mob, SoundEvents.GENERIC_EXPLODE.value());
                return 100;
            }
            case "tongue" -> {
                if (distance > 12 || distance < 3 || !sees) {
                    return 0;
                }
                beam(level, mob, target, new DustParticleOptions(new Vector3f(0.9f, 0.4f, 0.5f), 1.0f));
                Vec3 pull = mob.position().subtract(target.position()).normalize().scale(1.4);
                target.setDeltaMovement(pull.x, 0.4, pull.z);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                sound(level, mob, SoundEvents.SLIME_JUMP);
                return 80;
            }
            case "shift" -> {
                if (distance > 20) {
                    return 0;
                }
                Vec3 behind = target.position().subtract(target.getLookAngle().multiply(2, 0, 2));
                mob.teleportTo(behind.x, target.getY(), behind.z);
                mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0));
                level.sendParticles(ParticleTypes.WITCH, mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.4, 0.8, 0.4, 0);
                sound(level, mob, SoundEvents.ILLUSIONER_MIRROR_MOVE);
                return 120;
            }
            case "laser" -> {
                if (distance > 20 || !sees) {
                    return 0;
                }
                beam(level, mob, target, RED_LASER);
                target.hurt(mob.damageSources().mobAttack(mob), 6);
                sound(level, mob, SoundEvents.BEACON_DEACTIVATE);
                return 60;
            }
            case "giant_laser" -> {
                if (step % 3 == 2 && distance < 7) {
                    level.explode(mob, mob.getX(), mob.getY(), mob.getZ(), 3.0f, Level.ExplosionInteraction.NONE);
                    return 60;
                }
                if (distance > 32 || !sees) {
                    return 0;
                }
                beam(level, mob, target, RED_LASER);
                target.hurt(mob.damageSources().mobAttack(mob), 10);
                target.igniteForSeconds(3);
                sound(level, mob, SoundEvents.BEACON_DEACTIVATE);
                return 40;
            }
            case "psychic" -> {
                if (distance > 16) {
                    return 0;
                }
                if (target instanceof Player player && CerebroBlock.hiddenFromTelepaths(player)) {
                    say(level, mob, "The White Queen: \"That helmet... I can't get into your head!\"");
                    return 100;
                }
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                if (mob.getHealth() < mob.getMaxHealth() / 2) {
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                    level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.4, 0.8,
                            0.4, 0.02);
                }
                beam(level, mob, target, new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 1.2f));
                sound(level, mob, SoundEvents.ILLUSIONER_CAST_SPELL);
                return 90;
            }
            case "lightning" -> {
                if (distance > 20 || !sees) {
                    return 0;
                }
                net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                target.hurt(mob.damageSources().lightningBolt(), 8);
                return 80;
            }
            case "telekinesis" -> {
                if (distance > 16 || !sees) {
                    return 0;
                }
                Vec3 push = target.position().subtract(mob.position()).normalize().scale(1.8);
                target.setDeltaMovement(push.x, 0.7, push.z);
                target.hurtMarked = true;
                target.hurt(mob.damageSources().mobAttack(mob), 6);
                beam(level, mob, target, new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.1f), 1.2f));
                sound(level, mob, SoundEvents.ILLUSIONER_CAST_SPELL);
                return 70;
            }
            case "freeze" -> {
                if (distance > 14 || !sees) {
                    return 0;
                }
                beam(level, mob, target, ParticleTypes.SNOWFLAKE);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), 200));
                target.hurt(mob.damageSources().freeze(), 5);
                sound(level, mob, SoundEvents.GLASS_BREAK);
                return 60;
            }
            case "drain" -> {
                if (distance > 3) {
                    return 0;
                }
                target.hurt(mob.damageSources().mobAttack(mob), 4);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                mob.heal(4);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + 1, mob.getZ(), 8, 0.3,
                        0.6, 0.3, 0);
                return 60;
            }
            case "teleport" -> {
                if (distance > 20 || distance < 3) {
                    return 0;
                }
                level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.3, 0.6,
                        0.3, 0.01);
                Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1.5, 0, 1.5));
                mob.teleportTo(behind.x, target.getY(), behind.z);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.3, 0.6,
                        0.3, 0.01);
                level.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0f,
                        1.2f);
                return 80;
            }
            case "phase" -> {
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, true, false));
                return 120;
            }
            case "fireworks" -> {
                if (distance > 12 || !sees) {
                    return 0;
                }
                level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + 1, target.getZ(), 40, 0.4,
                        0.6, 0.4, 0.15);
                target.hurt(mob.damageSources().mobAttack(mob), 4);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                sound(level, mob, SoundEvents.FIREWORK_ROCKET_BLAST);
                return 50;
            }
            case "cards" -> {
                if (distance > 16 || !sees) {
                    return 0;
                }
                beam(level, mob, target, new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.8f), 1.0f));
                level.explode(mob, target.getX(), target.getY() + 0.5, target.getZ(), 1.2f,
                        Level.ExplosionInteraction.NONE);
                return 70;
            }
            case "regenerate" -> {
                mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false));
                return 40;
            }
            case "pounce" -> {
                if (distance < 3 || distance > 12 || !mob.onGround()) {
                    return 0;
                }
                Vec3 dir = target.position().subtract(mob.position()).normalize();
                mob.setDeltaMovement(dir.x * 1.3, 0.55, dir.z * 1.3);
                mob.hurtMarked = true;
                if (kind.id.equals("sabretooth")) {
                    mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
                    sound(level, mob, SoundEvents.RAVAGER_ROAR);
                } else {
                    sound(level, mob, SoundEvents.PLAYER_ATTACK_SWEEP);
                }
                return 70;
            }
            case "charge" -> {
                if (distance < 2.8) {
                    target.hurt(mob.damageSources().mobAttack(mob), 10);
                    Vec3 push = target.position().subtract(mob.position()).normalize().scale(2.2);
                    target.setDeltaMovement(push.x, 0.7, push.z);
                    target.hurtMarked = true;
                    level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 2,
                            0.3, 0.3, 0.3, 0);
                    sound(level, mob, SoundEvents.IRON_GOLEM_ATTACK);
                    return 50;
                }
                if (distance > 18) {
                    return 0;
                }
                Vec3 dir = target.position().subtract(mob.position()).normalize();
                mob.setDeltaMovement(dir.x * 1.1, mob.getDeltaMovement().y, dir.z * 1.1);
                mob.hurtMarked = true;
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 3, true, false));
                level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.2, mob.getZ(), 6, 0.4, 0.1, 0.4,
                        0.02);
                if (step % 4 == 0) {
                    say(level, mob, "Juggernaut: \"Don't you know who I am? I'm the Juggernaut!\"");
                }
                sound(level, mob, SoundEvents.RAVAGER_STEP);
                return 15;
            }
            case "copies" -> {
                if (distance > 16) {
                    return 0;
                }
                int copies = level.getEntitiesOfClass(MutantNpc.class, mob.getBoundingBox().inflate(16),
                        n -> "multiple_man_copy".equals(n.getPersistentData().getString(KIND))).size();
                if (copies >= 4) {
                    return 40;
                }
                for (int i = 0; i < 2; i++) {
                    MutantNpc copy = ModEntities.MUTANT.get().create(level);
                    if (copy == null) {
                        return 100;
                    }
                    double angle = mob.getRandom().nextDouble() * Math.PI * 2;
                    copy.setCharacter("multiple_man");
                    copy.moveTo(mob.getX() + Math.cos(angle) * 1.5, mob.getY(), mob.getZ() + Math.sin(angle) * 1.5,
                            mob.getYRot(), 0);
                    copy.addTag("xmen.multiple_man_copy");
                    copy.addTag("xmen.no_loot");
                    level.addFreshEntity(copy);
                    copy.setTarget(target);
                    level.sendParticles(ParticleTypes.POOF, copy.getX(), copy.getY() + 1, copy.getZ(), 12, 0.3, 0.6,
                            0.3, 0.02);
                }
                say(level, mob, "Multiple Man: \"Meet the rest of me!\"");
                sound(level, mob, SoundEvents.ILLUSIONER_MIRROR_MOVE);
                return 140;
            }
            case "shockwave" -> {
                if (distance > 7) {
                    return 0;
                }
                // he soaks up every hit and gives it back: the more hurt he is, the harder it hits
                float missing = 1 - mob.getHealth() / mob.getMaxHealth();
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox()
                        .inflate(7), e -> e != mob && kindOf(e) == null)) {
                    victim.hurt(mob.damageSources().mobAttack(mob), 4 + missing * 10);
                    Vec3 push = victim.position().subtract(mob.position()).normalize().scale(1.6);
                    victim.setDeltaMovement(push.x, 0.5, push.z);
                    victim.hurtMarked = true;
                }
                level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 2.5, 0.5,
                        2.5, 0);
                if (missing > 0.3 && step % 2 == 0) {
                    say(level, mob, "Sebastian Shaw: \"Every hit you land only makes me stronger!\"");
                }
                sound(level, mob, SoundEvents.GENERIC_EXPLODE.value());
                return 100;
            }
            case "whirlwind" -> {
                if (distance > 12 || !sees) {
                    return 0;
                }
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox()
                        .inflate(3), e -> e != mob && kindOf(e) == null)) {
                    victim.setDeltaMovement((mob.getRandom().nextDouble() - 0.5) * 1.2, 1.1,
                            (mob.getRandom().nextDouble() - 0.5) * 1.2);
                    victim.hurtMarked = true;
                }
                for (int i = 0; i < 6; i++) {
                    level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + i * 0.6, target.getZ(),
                            10, 0.4 + i * 0.15, 0.2, 0.4 + i * 0.15, 0.05);
                }
                sound(level, mob, SoundEvents.ELYTRA_FLYING);
                return 90;
            }
            case "hair" -> {
                if (distance > 10 || distance < 2.5 || !sees) {
                    return 0;
                }
                beam(level, mob, target, new DustParticleOptions(new Vector3f(0.12f, 0.08f, 0.06f), 1.4f));
                Vec3 pull = mob.position().subtract(target.position()).normalize().scale(1.2);
                target.setDeltaMovement(pull.x, 0.3, pull.z);
                target.hurtMarked = true;
                target.hurt(mob.damageSources().mobAttack(mob), 3);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                sound(level, mob, SoundEvents.FISHING_BOBBER_THROW);
                return 70;
            }
            case "rifle" -> {
                if (distance > 22 || !sees) {
                    return 0;
                }
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                beam(level, mob, target, ParticleTypes.CRIT);
                if (mob.getRandom().nextFloat() < 0.65f) {
                    target.hurt(mob.damageSources().mobAttack(mob), 4);
                }
                level.playSound(null, mob.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE,
                        1.2f, 1.8f);
                return 30;
            }
            case "knife" -> {
                if (!(mob instanceof MutantNpc npc) || !npc.isHostile()) {
                    return 0;
                }
                if (distance < 2.5) {
                    mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    target.hurt(mob.damageSources().mobAttack(mob), 5);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(),
                            1, 0, 0, 0, 0);
                    sound(level, mob, SoundEvents.PLAYER_ATTACK_SWEEP);
                    return 25;
                }
                if (distance > 14) {
                    return 0;
                }
                // Callisto's super-fast dash
                Vec3 dir = target.position().subtract(mob.position()).normalize();
                mob.setDeltaMovement(dir.x * 1.6, 0.25, dir.z * 1.6);
                mob.hurtMarked = true;
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 2, true, false));
                level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.3, mob.getZ(), 8, 0.3, 0.1, 0.3,
                        0.02);
                sound(level, mob, SoundEvents.PHANTOM_SWOOP);
                return 30;
            }
            default -> {
                return 0;
            }
        }
    }

    private static void summon(ServerLevel level, Mob mob, EntityType<? extends Mob> type, String kind, int count,
            LivingEntity target) {
        for (int i = 0; i < count; i++) {
            Mob minion = type.create(level);
            if (minion == null) {
                return;
            }
            double angle = mob.getRandom().nextDouble() * Math.PI * 2;
            minion.moveTo(mob.getX() + Math.cos(angle) * 2, mob.getY(), mob.getZ() + Math.sin(angle) * 2,
                    mob.getYRot(), 0);
            minion.addTag("xmen." + kind);
            minion.addTag("xmen.no_loot");
            level.addFreshEntity(minion);
            minion.setTarget(target);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, minion.getX(), minion.getY() + 1, minion.getZ(), 15, 0.3,
                    0.6, 0.3, 0.02);
        }
    }

    private static void beam(ServerLevel level, LivingEntity from, LivingEntity to, ParticleOptions particle) {
        Vec3 a = from.getEyePosition();
        Vec3 b = to.position().add(0, to.getBbHeight() / 2, 0);
        int steps = (int) (a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) Math.max(steps, 1));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void sound(ServerLevel level, Entity mob, SoundEvent sound) {
        level.playSound(null, mob.blockPosition(), sound, SoundSource.HOSTILE, 1.0f, 1.0f);
    }

    private static void say(ServerLevel level, Mob mob, String text) {
        broadcast(level, mob, Component.literal(text).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    private static void broadcast(ServerLevel level, Mob mob, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(mob) < 48 * 48) {
                player.sendSystemMessage(message);
            }
        }
    }

    // ------------------------------------------------------------------ boss bars
    private static void updateBar(ServerLevel level, Mob mob, Kind kind) {
        ServerBossEvent bar = BARS.computeIfAbsent(mob.getUUID(), id -> new ServerBossEvent(
                mob.getDisplayName(), kind.bar, BossEvent.BossBarOverlay.NOTCHED_10));
        bar.setProgress(Math.max(0, mob.getHealth() / mob.getMaxHealth()));
        for (ServerPlayer player : level.players()) {
            boolean near = player.distanceToSqr(mob) < 40 * 40;
            if (near && !bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            } else if (!near && bar.getPlayers().contains(player)) {
                bar.removePlayer(player);
            }
        }
    }

    private static void removeBar(Entity entity) {
        ServerBossEvent bar = BARS.remove(entity.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            removeBar(event.getEntity());
        }
    }

    public static void clearBars() {
        for (ServerBossEvent bar : BARS.values()) {
            bar.removeAllPlayers();
        }
        BARS.clear();
    }

    // ------------------------------------------------------------------ death, loot, fighting
    private static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Kind kind = kindOf(entity);
        if (kind == null) {
            return;
        }
        removeBar(entity);
        switch (kind.id) {
            case "giant_sentinel" -> {
                drop(entity, new ItemStack(ModContent.SENTINEL_PART.get(), 6));
                drop(entity, new ItemStack(Items.IRON_BLOCK, 4));
                drop(entity, new ItemStack(Items.DIAMOND, 2));
            }
            case "white_queen" -> {
                drop(entity, new ItemStack(Items.DIAMOND, 4));
                drop(entity, new ItemStack(Items.EMERALD, 12));
            }
            case "sebastian_shaw" -> {
                drop(entity, new ItemStack(Items.DIAMOND, 3));
                drop(entity, new ItemStack(Items.GOLD_INGOT, 8));
            }
            case "juggernaut" -> drop(entity, new ItemStack(Items.IRON_BLOCK, 2));
            case "callisto" -> {
                drop(entity, new ItemStack(Items.DIAMOND, 3));
                drop(entity, new ItemStack(Items.EMERALD, 12));
                if (entity instanceof Mob mob) {
                    broadcast(level, mob, Component.literal("Callisto: ...You win. The Morlocks follow you now. "
                            + "The hoard is yours.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                }
                CompoundTag data = entity.getPersistentData();
                openIronDoors(level, data.contains(HOME) ? BlockPos.of(data.getLong(HOME)) : entity.blockPosition());
                level.playSound(null, entity.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f,
                        0.8f);
            }
            case "sentinel" -> {
                if (entity.getRandom().nextBoolean()) {
                    drop(entity, new ItemStack(ModContent.SENTINEL_PART.get()));
                }
            }
            default -> {
            }
        }
    }

    /** Opens the iron doors around a spot (the Morlock hoard behind Callisto's pit). */
    private static void openIronDoors(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-12, -4, -12), center.offset(12, 4, 12))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.IRON_DOOR) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && state.getBlock() instanceof DoorBlock door) {
                door.setOpen(null, level, state, pos.immutable(), true);
            }
        }
    }

    private static void drop(LivingEntity entity, ItemStack stack) {
        entity.spawnAtLocation(stack);
    }

    private static boolean noLoot(Entity entity) {
        return entity.getTags().contains("xmen.no_loot") || entity.getPersistentData().getBoolean("xmen_no_loot");
    }

    private static void onDrops(LivingDropsEvent event) {
        if (noLoot(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onExperience(LivingExperienceDropEvent event) {
        if (noLoot(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Villains in the same base don't fight each other (Sentinels vs. Trask's guards and so on). */
    private static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) {
            return;
        }
        Kind attacker = kindOf(event.getEntity());
        Kind victim = kindOf(target);
        if (attacker != null && victim != null && attacker.hostile && victim.hostile) {
            event.setCanceled(true);
        }
    }

    /** Friendly characters say something when you right-click them. Professor X reads your mind. */
    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        Kind kind = kindOf(event.getTarget());
        if (kind == null || kind.lines == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (morlockInteract(kind, event.getTarget(), player)) {
            return;
        }
        String line = kind.lines[player.getRandom().nextInt(kind.lines.length)];
        player.sendSystemMessage(event.getTarget().getDisplayName().copy().append(Component.literal(": " + line)
                .withStyle(ChatFormatting.WHITE)));
        if (kind.id.equals("professor_x")) {
            Power power = PlayerPowers.get(player);
            if (CerebroBlock.hiddenFromTelepaths(player)) {
                player.sendSystemMessage(Component.literal("Professor X: Hm. That helmet blocks my telepathy. Take it "
                        + "off and I'll tell you about your gift.").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (power != null) {
                player.sendSystemMessage(Component.literal("Professor X: Your gift is ").withStyle(
                        ChatFormatting.LIGHT_PURPLE).append(power.title()).append(Component.literal(". "
                                + power.description()).withStyle(ChatFormatting.GRAY)));
                player.sendSystemMessage(Component.literal("Professor X: If you ever want a different gift, I can "
                        + "trade you a Mutant Serum. It isn't cheap.").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** Callisto can be challenged, Healer heals, Caliban senses mutants. Returns true if it handled the click. */
    private static boolean morlockInteract(Kind kind, Entity target, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        CompoundTag data = target.getPersistentData();
        switch (kind.id) {
            case "callisto" -> {
                if (!(target instanceof MutantNpc npc) || npc.isHostile()) {
                    return true;
                }
                boolean again = data.hasUUID("xmen_challenger") && data.getUUID("xmen_challenger")
                        .equals(player.getUUID()) && now - data.getLong("xmen_challenge_time") < 200;
                if (!again) {
                    data.putUUID("xmen_challenger", player.getUUID());
                    data.putLong("xmen_challenge_time", now);
                    tell(player, "Callisto: So you want to lead the Morlocks? Down here the leader is whoever beats "
                            + "me. Right-click me again if you dare.", ChatFormatting.DARK_PURPLE);
                    return true;
                }
                if (player.isCreative()) {
                    tell(player, "Callisto: Come back without your god powers (leave creative mode).",
                            ChatFormatting.DARK_PURPLE);
                    return true;
                }
                npc.setHostile(true);
                npc.clearRestriction();
                npc.setTarget(player);
                broadcast(level, npc, Component.literal("Callisto: Then fight me, surface dweller! Morlocks, give us "
                        + "room!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                level.playSound(null, npc.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0f, 1.3f);
                return true;
            }
            case "healer" -> {
                if (now < data.getLong("xmen_heal_ready")) {
                    tell(player, "Healer: Healer is tired. Come back soon.", ChatFormatting.GREEN);
                    return true;
                }
                data.putLong("xmen_heal_ready", now + 400);
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.clearFire();
                for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        player.removeEffect(effect.getEffect());
                    }
                }
                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.4,
                        0.4, 0.4, 0);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL,
                        1.5f, 1.2f);
                tell(player, "Healer puts his hand on you. Your wounds close.", ChatFormatting.GREEN);
                return true;
            }
            case "caliban" -> {
                if (CerebroBlock.hiddenFromTelepaths(player)) {
                    tell(player, "Caliban: Caliban can't feel you... the helmet hides you.", ChatFormatting.WHITE);
                    return true;
                }
                Power power = PlayerPowers.get(player);
                if (power != null) {
                    player.sendSystemMessage(Component.literal("Caliban feels your power: ").withStyle(
                            ChatFormatting.WHITE).append(power.title()));
                }
                BlockPos here = player.blockPosition();
                ResourceKey<Structure> best = null;
                BlockPos bestPos = null;
                double bestDistance = Double.MAX_VALUE;
                for (ResourceKey<Structure> key : ModStructures.ALL) {
                    if (key == ModStructures.MORLOCK_TUNNELS) {
                        continue;
                    }
                    BlockPos at = ModStructures.findNearest(level, key, here, 32);
                    if (at == null) {
                        continue;
                    }
                    double distance = Math.sqrt(at.distSqr(new BlockPos(here.getX(), at.getY(), here.getZ())));
                    if (distance < bestDistance) {
                        best = key;
                        bestPos = at;
                        bestDistance = distance;
                    }
                }
                if (best == null) {
                    tell(player, "Caliban: Caliban feels no other mutants near. Only Morlocks.", ChatFormatting.WHITE);
                } else {
                    tell(player, "Caliban: Caliban feels mutants at the " + ModStructures.displayName(best) + ". "
                            + (int) bestDistance + " blocks " + compass(here, bestPos) + ".", ChatFormatting.WHITE);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static void tell(ServerPlayer player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    private static String compass(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        String ns = Math.abs(dz) > Math.abs(dx) / 2 ? (dz < 0 ? "north" : "south") : "";
        String ew = Math.abs(dx) > Math.abs(dz) / 2 ? (dx < 0 ? "west" : "east") : "";
        String dir = ns + (ns.isEmpty() || ew.isEmpty() ? "" : "-") + ew;
        return dir.isEmpty() ? "right here" : dir;
    }

    /** Used by the game tests. */
    public static List<String> kindIds() {
        return List.copyOf(KINDS.keySet());
    }
}
