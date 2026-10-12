package com.emil.xmen.power;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.RandomSource;

/**
 * All 300 mutant powers. Generated from the power list Emil approved
 * (15 categories x 20 powers). Each power's behaviour is added in later updates.
 */
public final class Powers {
    private static final Map<String, Power> BY_ID = new LinkedHashMap<>();
    private static final Map<PowerLevel, List<Power>> BY_LEVEL = new EnumMap<>(PowerLevel.class);

    static {
        add("optic_blasts", "Optic Blasts", "Cyclops", PowerLevel.BETA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Red eye beam that damages and knocks back everything in a line");
        add("kinetic_charging", "Kinetic Charging", "Gambit", PowerLevel.BETA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Charge the item in your hand and throw it so it explodes");
        add("energy_absorption", "Energy Absorption", "Bishop", PowerLevel.ALPHA, PowerCategory.ENERGY_AND_BLASTS, PowerType.PASSIVE,
                "Soak up part of all damage, then release it as a shockwave");
        add("plasma_bolts", "Plasma Bolts", "", PowerLevel.GAMMA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Slow, heavy plasma orbs that burst on impact");
        add("sonic_scream", "Sonic Scream", "Banshee", PowerLevel.GAMMA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Scream in a cone that damages and stuns mobs");
        add("chain_lightning", "Chain Lightning", "", PowerLevel.GAMMA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Electric shock that jumps between nearby mobs");
        add("concussive_punch", "Concussive Punch", "", PowerLevel.DELTA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Punch the air to knock mobs far back");
        add("hard_light_constructs", "Hard-Light Constructs", "", PowerLevel.ALPHA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Make temporary glowing blocks to stand on or hide behind");
        add("photon_beam", "Photon Beam", "", PowerLevel.BETA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Long-range light beam that makes mobs glow");
        add("radiation_aura", "Radiation Aura", "", PowerLevel.ALPHA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Aura that poisons and weakens everything near you");
        add("microwave_pulse", "Microwave Pulse", "", PowerLevel.DELTA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Cooks raw food in your inventory and heats nearby mobs");
        add("bio_electric_touch", "Bio-Electric Touch", "", PowerLevel.EPSILON, PowerCategory.ENERGY_AND_BLASTS, PowerType.PASSIVE,
                "Your punches deal a little extra shock damage");
        add("energy_shield", "Energy Shield", "", PowerLevel.GAMMA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Bubble that blocks arrows and fireballs for a few seconds");
        add("explosive_touch", "Explosive Touch", "", PowerLevel.BETA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "A block or mob you touch explodes a few seconds later");
        add("disintegration_ray", "Disintegration Ray", "", PowerLevel.ALPHA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Beam that destroys the blocks in a line");
        add("static_shock", "Static Shock", "", PowerLevel.EPSILON, PowerCategory.ENERGY_AND_BLASTS, PowerType.PASSIVE,
                "Mobs you hit are slowed for a moment");
        add("solar_beams", "Solar Beams", "Sunfire", PowerLevel.BETA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Beams that get stronger in sunlight");
        add("cosmic_energy_control", "Cosmic Energy Control", "", PowerLevel.OMEGA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "One huge energy beam, or sneak for a cosmic shield of extra hearts");
        add("palm_blasts", "Palm Blasts", "", PowerLevel.DELTA, PowerCategory.ENERGY_AND_BLASTS, PowerType.ACTIVE,
                "Short-range energy bolts from your hands");
        add("reactive_charge", "Reactive Charge", "", PowerLevel.GAMMA, PowerCategory.ENERGY_AND_BLASTS, PowerType.PASSIVE,
                "Getting hit charges your next attack to double damage");
        add("pyrokinesis", "Pyrokinesis", "Pyro", PowerLevel.BETA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Throw fireballs and shape nearby fire");
        add("fireproof", "Fireproof", "", PowerLevel.EPSILON, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "Immune to fire and lava damage");
        add("living_flame", "Living Flame", "", PowerLevel.ALPHA, PowerCategory.FIRE_AND_HEAT, PowerType.BOTH,
                "Turn into fire: fly, burn what you touch, ignore heat");
        add("body_heat", "Body Heat", "", PowerLevel.DELTA, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "Melt snow and ice around you; never freeze");
        add("lava_walker", "Lava Walker", "", PowerLevel.DELTA, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "Lava under your feet cools into basalt as you walk");
        add("fire_breath", "Fire Breath", "", PowerLevel.GAMMA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Breathe a cone of fire");
        add("smoke_form", "Smoke Form", "", PowerLevel.BETA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Vanish in smoke: mobs nearby are blinded and you are invisible and fast for 6 seconds");
        add("ember_trail", "Ember Trail", "", PowerLevel.EPSILON, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "Leave a short trail of fire while sprinting");
        add("blast_punch", "Blast Punch", "", PowerLevel.GAMMA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Your next punch explodes in a small blast");
        add("molten_skin", "Molten Skin", "", PowerLevel.GAMMA, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "Mobs that hit you catch fire");
        add("heat_vision", "Heat Vision", "", PowerLevel.GAMMA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Stare at a block to smelt it (sand to glass, ore to ingot)");
        add("wall_of_fire", "Wall of Fire", "", PowerLevel.BETA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Raise a wall of fire in front of you");
        add("phoenix_rebirth", "Phoenix Rebirth", "", PowerLevel.OMEGA, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "When you die, rise again from flames at full health (once a day)");
        add("fire_healing", "Fire Healing", "", PowerLevel.DELTA, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "Standing in fire heals you instead of hurting");
        add("meteor_call", "Meteor Call", "", PowerLevel.ALPHA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Call down a burning meteor where you look");
        add("heat_sense", "Heat Sense", "", PowerLevel.EPSILON, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "See heat: burning and fire mobs glow from far away, warm mobs close by");
        add("sun_battery", "Sun Battery", "", PowerLevel.DELTA, PowerCategory.FIRE_AND_HEAT, PowerType.PASSIVE,
                "Sunlight gives you Strength and Regeneration");
        add("flame_dash", "Flame Dash", "", PowerLevel.GAMMA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Rocket forward leaving fire behind");
        add("volcano", "Volcano", "", PowerLevel.ALPHA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "The ground around you erupts with lava and fire");
        add("blue_flame", "Blue Flame", "", PowerLevel.BETA, PowerCategory.FIRE_AND_HEAT, PowerType.ACTIVE,
                "Hotter fire that burns longer and ignores fire resistance");
        add("cryokinesis", "Cryokinesis", "Iceman", PowerLevel.BETA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Shoot ice that freezes mobs in place");
        add("ice_form", "Ice Form", "", PowerLevel.ALPHA, PowerCategory.ICE_AND_WATER, PowerType.BOTH,
                "Become living ice: armor, freezing touch, immune to cold");
        add("ice_slide", "Ice Slide", "", PowerLevel.GAMMA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Build an ice ramp where you look and slide along it");
        add("frost_walker", "Frost Walker", "", PowerLevel.EPSILON, PowerCategory.ICE_AND_WATER, PowerType.PASSIVE,
                "Water freezes under your feet");
        add("cold_proof", "Cold Proof", "", PowerLevel.EPSILON, PowerCategory.ICE_AND_WATER, PowerType.PASSIVE,
                "Powder snow never freezes you");
        add("snowball_barrage", "Snowball Barrage", "", PowerLevel.DELTA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Throw a burst of hard-hitting snowballs");
        add("hydrokinesis", "Hydrokinesis", "", PowerLevel.BETA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Water jets that push mobs and put out fire");
        add("gills", "Gills", "", PowerLevel.EPSILON, PowerCategory.ICE_AND_WATER, PowerType.PASSIVE,
                "Breathe underwater forever");
        add("aquatic_body", "Aquatic Body", "", PowerLevel.DELTA, PowerCategory.ICE_AND_WATER, PowerType.PASSIVE,
                "Swim fast and see clearly underwater");
        add("tidal_wave", "Tidal Wave", "", PowerLevel.ALPHA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Summon a wave that sweeps mobs away");
        add("ice_armor", "Ice Armor", "", PowerLevel.GAMMA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Coat yourself in ice for strong protection");
        add("blizzard", "Blizzard", "", PowerLevel.ALPHA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Snowstorm around you that slows and freezes mobs");
        add("water_form", "Water Form", "", PowerLevel.BETA, PowerCategory.ICE_AND_WATER, PowerType.BOTH,
                "Become water: no fall damage, slip through gaps");
        add("ice_spikes", "Ice Spikes", "", PowerLevel.GAMMA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Spikes of ice shoot up from the ground");
        add("rain_healing", "Rain Healing", "", PowerLevel.DELTA, PowerCategory.ICE_AND_WATER, PowerType.PASSIVE,
                "Regenerate in water or rain");
        add("absolute_zero", "Absolute Zero", "", PowerLevel.OMEGA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Freeze everything in a huge radius: water, lava and mobs");
        add("mist", "Mist", "", PowerLevel.DELTA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Thick fog that hides you from mobs");
        add("whirlpool", "Whirlpool", "", PowerLevel.GAMMA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Pull mobs in water toward one point");
        add("ice_wall", "Ice Wall", "", PowerLevel.DELTA, PowerCategory.ICE_AND_WATER, PowerType.ACTIVE,
                "Wall of ice blocks attacks in front of you");
        add("boiling_touch", "Boiling Touch", "", PowerLevel.GAMMA, PowerCategory.ICE_AND_WATER, PowerType.BOTH,
                "Your hits scald mobs (double if wet), and R boils the water around you");
        add("weather_control", "Weather Control", "Storm", PowerLevel.OMEGA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Change the weather at will and call lightning anywhere");
        add("lightning_strike", "Lightning Strike", "", PowerLevel.BETA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Lightning finds the three closest monsters by itself");
        add("wind_gust", "Wind Gust", "", PowerLevel.GAMMA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Blast of wind pushes mobs and items away");
        add("wind_flight", "Wind Flight", "", PowerLevel.BETA, PowerCategory.WEATHER_AND_AIR, PowerType.BOTH,
                "Ride a gust of wind where you look and blow mobs out of your way");
        add("tornado", "Tornado", "", PowerLevel.ALPHA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Spawn a tornado that throws mobs around");
        add("rain_maker", "Rain Maker", "", PowerLevel.DELTA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Start rain (good for crops and fishing)");
        add("clear_skies", "Clear Skies", "", PowerLevel.DELTA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Stop rain and thunder");
        add("air_bubble", "Air Bubble", "", PowerLevel.EPSILON, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Breathable bubble of air underwater");
        add("lightning_proof", "Lightning Proof", "", PowerLevel.EPSILON, PowerCategory.WEATHER_AND_AIR, PowerType.PASSIVE,
                "Immune to lightning; being struck charges you up, storms make you faster and tougher");
        add("cyclone_spin", "Cyclone Spin", "", PowerLevel.GAMMA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Spin fast to deflect arrows");
        add("vacuum", "Vacuum", "", PowerLevel.ALPHA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Pull the air away from mobs so they suffocate");
        add("wind_jump", "Wind Jump", "", PowerLevel.DELTA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "A gust launches you high into the air");
        add("storm_power", "Storm Power", "", PowerLevel.BETA, PowerCategory.WEATHER_AND_AIR, PowerType.PASSIVE,
                "Faster and stronger during rain and thunder");
        add("feather_fall", "Feather Fall", "", PowerLevel.EPSILON, PowerCategory.WEATHER_AND_AIR, PowerType.PASSIVE,
                "You float down like a feather instead of falling");
        add("hailstorm", "Hailstorm", "", PowerLevel.GAMMA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Hail falls on an area and hurts mobs");
        add("air_pressure", "Air Pressure", "", PowerLevel.BETA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Squeeze the air around one mob: rising damage for 3 seconds, then it pops into the air");
        add("thunderclap", "Thunderclap", "", PowerLevel.GAMMA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Clap to stun all mobs near you");
        add("wind_wall", "Wind Wall", "", PowerLevel.DELTA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Wall of wind that stops arrows");
        add("jet_stream", "Jet Stream", "", PowerLevel.ALPHA, PowerCategory.WEATHER_AND_AIR, PowerType.ACTIVE,
                "Fly extremely fast in a straight line");
        add("static_field", "Static Field", "", PowerLevel.DELTA, PowerCategory.WEATHER_AND_AIR, PowerType.PASSIVE,
                "Mobs touching you get zapped");
        add("magnetism", "Magnetism", "Magneto", PowerLevel.OMEGA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Throw metal mobs and items away, rip out metal blocks and hurl them, arrows stop before they hit you");
        add("steel_skin", "Steel Skin", "Colossus", PowerLevel.BETA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.PASSIVE,
                "Organic steel body: high armor and strength, sink in water");
        add("earth_shaping", "Earth Shaping", "", PowerLevel.BETA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Raise and move dirt and stone");
        add("ground_stomp", "Ground Stomp", "", PowerLevel.GAMMA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Stomp to shake the ground and knock mobs down");
        add("stone_skin", "Stone Skin", "", PowerLevel.DELTA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.PASSIVE,
                "Extra armor like stone");
        add("tunneling", "Tunneling", "", PowerLevel.GAMMA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Dig through stone extremely fast");
        add("sand_form", "Sand Form", "", PowerLevel.BETA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.BOTH,
                "Become sand: arrows and falls don't hurt you");
        add("crystal_growth", "Crystal Growth", "", PowerLevel.GAMMA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Grow crystal walls and spikes");
        add("ore_sense", "Ore Sense", "", PowerLevel.EPSILON, PowerCategory.EARTH_STONE_AND_METAL, PowerType.PASSIVE,
                "Nearby ores glow through stone");
        add("earthquake", "Earthquake", "", PowerLevel.ALPHA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Big quake that breaks blocks around you");
        add("iron_pull", "Iron Pull", "", PowerLevel.DELTA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Pull iron items and dropped items toward you");
        add("diamond_form", "Diamond Form", "Emma Frost", PowerLevel.ALPHA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.BOTH,
                "Turn to diamond: almost unbreakable and very strong");
        add("boulder_throw", "Boulder Throw", "", PowerLevel.DELTA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Rip out a chunk of ground and throw it");
        add("sinkhole", "Sinkhole", "", PowerLevel.EPSILON, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "The ground swallows a mob and closes over its head for 10 seconds");
        add("mountain_strength", "Mountain Strength", "", PowerLevel.GAMMA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.PASSIVE,
                "The more stone around you, the stronger and tougher you get");
        add("quicksand", "Quicksand", "", PowerLevel.GAMMA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Ground turns soft so mobs sink and slow down");
        add("lucky_miner", "Lucky Miner", "", PowerLevel.EPSILON, PowerCategory.EARTH_STONE_AND_METAL, PowerType.PASSIVE,
                "Better chance of extra drops from ores");
        add("floating_rock", "Floating Rock", "", PowerLevel.ALPHA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Fly standing on a floating rock platform");
        add("obsidian_shell", "Obsidian Shell", "", PowerLevel.BETA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.ACTIVE,
                "Wrap yourself in obsidian: nearly invulnerable for a moment");
        add("tremor_sense", "Tremor Sense", "", PowerLevel.DELTA, PowerCategory.EARTH_STONE_AND_METAL, PowerType.PASSIVE,
                "Feel mobs walking on the ground near you (they glow)");
        add("teleportation", "Teleportation", "Nightcrawler", PowerLevel.BETA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Teleport to where you look, with a puff of smoke");
        add("super_speed", "Super Speed", "Quicksilver", PowerLevel.BETA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Run extremely fast");
        add("wall_crawling", "Wall Crawling", "", PowerLevel.DELTA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Climb any wall");
        add("super_jump", "Super Jump", "", PowerLevel.DELTA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Jump much higher");
        add("phasing", "Phasing", "Shadowcat", PowerLevel.ALPHA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Walk through walls");
        add("flight", "Flight", "", PowerLevel.GAMMA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.BOTH,
                "Fly freely; R makes you fly three times faster for 5 seconds");
        add("portals", "Portals", "Blink", PowerLevel.ALPHA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Open a portal between two places");
        add("dimension_hop", "Dimension Hop", "", PowerLevel.ALPHA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Teleport between the Overworld, Nether and End");
        add("blink_step", "Blink Step", "", PowerLevel.DELTA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Short instant dash forward");
        add("glider", "Glider", "", PowerLevel.EPSILON, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Hold jump in the air to glide: look down to dive and speed up, look up to climb");
        add("water_runner", "Water Runner", "", PowerLevel.EPSILON, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Run across water while sprinting");
        add("fast_swimmer", "Fast Swimmer", "", PowerLevel.EPSILON, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Swim very fast");
        add("home_recall", "Home Recall", "", PowerLevel.GAMMA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Teleport to your bed");
        add("group_teleport", "Group Teleport", "", PowerLevel.BETA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Call every player and every pet of yours in this world to your side");
        add("rocket_launch", "Rocket Launch", "Cannonball", PowerLevel.GAMMA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Blast off and explode at the top of the flight, knocking mobs away");
        add("bounce", "Bounce", "", PowerLevel.DELTA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Bounce off the ground; no fall damage");
        add("grapple", "Grapple", "", PowerLevel.DELTA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Pull yourself to the block you look at");
        add("wall_run", "Wall Run", "", PowerLevel.EPSILON, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Run along walls for a short time");
        add("low_gravity", "Low Gravity", "", PowerLevel.EPSILON, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.PASSIVE,
                "Jump higher and fall slower");
        add("instant_travel", "Instant Travel", "", PowerLevel.ALPHA, PowerCategory.MOVEMENT_AND_TRAVEL, PowerType.ACTIVE,
                "Sneak + R saves a spot, R takes you back to it, or type /power travel x y z");
        add("super_strength", "Super Strength", "", PowerLevel.BETA, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Hit much harder and break blocks faster");
        add("invulnerability", "Invulnerability", "", PowerLevel.ALPHA, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Take much less damage from everything");
        add("giant_growth", "Giant Growth", "", PowerLevel.GAMMA, PowerCategory.BODY_AND_STRENGTH, PowerType.ACTIVE,
                "Grow huge: more reach and strength, but slower");
        add("shrinking", "Shrinking", "", PowerLevel.GAMMA, PowerCategory.BODY_AND_STRENGTH, PowerType.ACTIVE,
                "Become tiny: fit through 1-block gaps, harder to hit");
        add("claws", "Claws", "Wolverine", PowerLevel.GAMMA, PowerCategory.BODY_AND_STRENGTH, PowerType.ACTIVE,
                "Pop out claws for strong melee damage");
        add("bone_spikes", "Bone Spikes", "Marrow", PowerLevel.DELTA, PowerCategory.BODY_AND_STRENGTH, PowerType.BOTH,
                "Mobs that hit you take spike damage, and R shoots a volley of bone spikes");
        add("density_control", "Density Control", "", PowerLevel.BETA, PowerCategory.BODY_AND_STRENGTH, PowerType.ACTIVE,
                "Become heavy (no knockback) or light (float)");
        add("rubber_body", "Rubber Body", "", PowerLevel.GAMMA, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "No fall damage and longer reach");
        add("extra_arms", "Extra Arms", "", PowerLevel.DELTA, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Mine and attack faster");
        add("endless_stamina", "Endless Stamina", "", PowerLevel.EPSILON, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Sprinting never costs hunger");
        add("thick_hide", "Thick Hide", "", PowerLevel.DELTA, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Arrows and other projectiles only do half damage");
        add("acid_spit", "Acid Spit", "", PowerLevel.DELTA, PowerCategory.BODY_AND_STRENGTH, PowerType.ACTIVE,
                "Spit acid that hurts mobs and wears down armor");
        add("venom_strike", "Venom Strike", "", PowerLevel.GAMMA, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Your hits poison");
        add("tail", "Tail", "", PowerLevel.EPSILON, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Extra reach and better climbing");
        add("wings", "Wings", "Angel", PowerLevel.BETA, PowerCategory.BODY_AND_STRENGTH, PowerType.BOTH,
                "Feathered wings let you fly");
        add("quick_reflexes", "Quick Reflexes", "", PowerLevel.DELTA, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Chance to dodge attacks");
        add("berserker_rage", "Berserker Rage", "", PowerLevel.GAMMA, PowerCategory.BODY_AND_STRENGTH, PowerType.ACTIVE,
                "Huge strength for a while, but less defense");
        add("tough_bones", "Tough Bones", "", PowerLevel.EPSILON, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Much less fall damage");
        add("metal_skeleton", "Metal Skeleton", "", PowerLevel.ALPHA, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "No knockback and no single hit can do more than 6 hearts, but you sink in water");
        add("iron_stomach", "Iron Stomach", "", PowerLevel.EPSILON, PowerCategory.BODY_AND_STRENGTH, PowerType.PASSIVE,
                "Eat rotten flesh and raw food with no bad effects");
        add("healing_factor", "Healing Factor", "Wolverine", PowerLevel.BETA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Heal fast from any wound");
        add("extreme_regeneration", "Extreme Regeneration", "", PowerLevel.ALPHA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Come back from almost dead; poison and wither barely work on you");
        add("healing_touch", "Healing Touch", "", PowerLevel.GAMMA, PowerCategory.HEALING_AND_LIFE, PowerType.ACTIVE,
                "Heal friends and pets");
        add("life_drain", "Life Drain", "", PowerLevel.BETA, PowerCategory.HEALING_AND_LIFE, PowerType.ACTIVE,
                "Steal health from mobs around you");
        add("poison_proof", "Poison Proof", "", PowerLevel.EPSILON, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Immune to poison");
        add("sickness_proof", "Sickness Proof", "", PowerLevel.EPSILON, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Immune to hunger, nausea and wither effects");
        add("slow_metabolism", "Slow Metabolism", "", PowerLevel.DELTA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Hunger drains much slower");
        add("resurrection", "Resurrection", "", PowerLevel.OMEGA, PowerCategory.HEALING_AND_LIFE, PowerType.ACTIVE,
                "Bring a dead friend back to life where they fell");
        add("biokinesis", "Biokinesis", "Elixir", PowerLevel.ALPHA, PowerCategory.HEALING_AND_LIFE, PowerType.ACTIVE,
                "Heal or badly hurt any living thing with a touch");
        add("cure_touch", "Cure Touch", "", PowerLevel.DELTA, PowerCategory.HEALING_AND_LIFE, PowerType.ACTIVE,
                "Remove bad effects and cure zombie villagers");
        add("second_life", "Second Life", "", PowerLevel.GAMMA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Survive a killing blow once a day");
        add("healing_aura", "Healing Aura", "", PowerLevel.GAMMA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Friends near you regenerate");
        add("grass_healing", "Grass Healing", "", PowerLevel.EPSILON, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Heal while standing on grass");
        add("adrenaline", "Adrenaline", "", PowerLevel.DELTA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Low health gives you speed and strength");
        add("toxic_cloud", "Toxic Cloud", "", PowerLevel.GAMMA, PowerCategory.HEALING_AND_LIFE, PowerType.ACTIVE,
                "A poison cloud that drifts where you look and keeps growing");
        add("plague", "Plague", "", PowerLevel.BETA, PowerCategory.HEALING_AND_LIFE, PowerType.ACTIVE,
                "Sickness that weakens mobs and spreads between them");
        add("life_sense", "Life Sense", "", PowerLevel.EPSILON, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "See the health of mobs you look at");
        add("pain_eater", "Pain Eater", "", PowerLevel.DELTA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Taking damage fills your hunger");
        add("undying", "Undying", "", PowerLevel.ALPHA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Damage drains your hunger instead of health while you have food");
        add("vampire_bite", "Vampire Bite", "", PowerLevel.GAMMA, PowerCategory.HEALING_AND_LIFE, PowerType.PASSIVE,
                "Your melee hits heal you a little");
        add("night_vision", "Night Vision", "", PowerLevel.EPSILON, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "See in the dark");
        add("x_ray_vision", "X-Ray Vision", "", PowerLevel.GAMMA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "See through blocks for a few seconds");
        add("super_hearing", "Super Hearing", "", PowerLevel.EPSILON, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "You hear hidden mobs and players: sound markers show where they are, only you see them");
        add("danger_sense", "Danger Sense", "", PowerLevel.DELTA, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "Warning when a mob starts targeting you");
        add("eagle_eyes", "Eagle Eyes", "", PowerLevel.EPSILON, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "Zoom in like a spyglass");
        add("tracker", "Tracker", "", PowerLevel.DELTA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "See a trail of a player's or mob's recent steps");
        add("mutant_detection", "Mutant Detection", "Cerebro", PowerLevel.GAMMA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "See every player's power and where they are");
        add("super_smell", "Super Smell", "", PowerLevel.EPSILON, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "Animals and dropped items nearby glow");
        add("precognition", "Precognition", "Blindfold", PowerLevel.BETA, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "See attacks coming and dodge some of them");
        add("radar_sense", "Radar Sense", "", PowerLevel.GAMMA, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "See all mobs around you, even when blinded");
        add("structure_sense", "Structure Sense", "", PowerLevel.DELTA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "Point toward the nearest village or structure");
        add("treasure_sense", "Treasure Sense", "", PowerLevel.GAMMA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "Unopened loot chests glow gold through walls for 30 seconds");
        add("echolocation", "Echolocation", "", PowerLevel.DELTA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "A ping that outlines mobs and caves around you");
        add("weak_spot_sight", "Weak Spot Sight", "", PowerLevel.DELTA, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "See where mobs are weak: hits from behind do double damage");
        add("time_sense", "Time Sense", "", PowerLevel.EPSILON, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "Always know the time and when night falls");
        add("cosmic_awareness", "Cosmic Awareness", "", PowerLevel.OMEGA, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "Sense everything near you: mobs, ores, chests and players");
        add("remote_viewing", "Remote Viewing", "", PowerLevel.BETA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "Look from a faraway spot for a few seconds");
        add("postcognition", "Postcognition", "", PowerLevel.GAMMA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "See where you died and where players were recently");
        add("aura_reading", "Aura Reading", "", PowerLevel.DELTA, PowerCategory.SENSES_AND_DETECTION, PowerType.ACTIVE,
                "Mobs glow in colors showing if they're hostile or friendly");
        add("perfect_aim", "Perfect Aim", "", PowerLevel.GAMMA, PowerCategory.SENSES_AND_DETECTION, PowerType.PASSIVE,
                "Arrows and thrown items fly straight and hit harder");
        add("telepathy", "Telepathy", "Professor X", PowerLevel.BETA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "See what mobs are targeting and talk privately to any player");
        add("telekinesis", "Telekinesis", "Jean Grey", PowerLevel.BETA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Lift and throw mobs and blocks with your mind");
        add("mind_control", "Mind Control", "", PowerLevel.ALPHA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Take control of a mob and give it orders: R makes it attack what you look at");
        add("psychic_blast", "Psychic Blast", "", PowerLevel.GAMMA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Mental blast that hurts and confuses");
        add("illusions", "Illusions", "Mirage", PowerLevel.GAMMA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "You vanish while three copies of you run off, and mobs chase the copies");
        add("psychic_shield", "Psychic Shield", "", PowerLevel.DELTA, PowerCategory.MIND_AND_PSYCHIC, PowerType.PASSIVE,
                "Immune to nausea, blindness and darkness; resist mind control");
        add("memory_wipe", "Memory Wipe", "", PowerLevel.DELTA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "A mob forgets you and stops chasing");
        add("fear", "Fear", "", PowerLevel.GAMMA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Mobs run away in panic");
        add("calm", "Calm", "", PowerLevel.EPSILON, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Calming aura: mobs stop attacking you, friends and villagers, creepers stop fizzing");
        add("psychic_blade", "Psychic Blade", "Psylocke", PowerLevel.BETA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Psychic blade that ignores armor");
        add("astral_projection", "Astral Projection", "", PowerLevel.ALPHA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Leave your body and fly around as a ghost");
        add("phoenix_force", "Phoenix Force", "", PowerLevel.OMEGA, PowerCategory.MIND_AND_PSYCHIC, PowerType.BOTH,
                "Cosmic fire: flight, huge fire blasts and rebirth");
        add("mind_link", "Mind Link", "", PowerLevel.DELTA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Share your location and health with friends");
        add("hypnosis", "Hypnosis", "", PowerLevel.DELTA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Put a mob to sleep for a while");
        add("telekinetic_flight", "Telekinetic Flight", "", PowerLevel.GAMMA, PowerCategory.MIND_AND_PSYCHIC, PowerType.BOTH,
                "Float with your mind and carry friends and pets next to you along");
        add("telekinetic_barrier", "Telekinetic Barrier", "", PowerLevel.GAMMA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Psychic wall that pushes everything away");
        add("possession", "Possession", "", PowerLevel.ALPHA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Take over a mob and walk around in its body");
        add("psychic_weapons", "Psychic Weapons", "", PowerLevel.BETA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Create glowing psychic weapons that hit hard");
        add("hive_mind", "Hive Mind", "", PowerLevel.OMEGA, PowerCategory.MIND_AND_PSYCHIC, PowerType.ACTIVE,
                "Control many mobs at once and see through their eyes");
        add("emotion_sense", "Emotion Sense", "", PowerLevel.EPSILON, PowerCategory.MIND_AND_PSYCHIC, PowerType.PASSIVE,
                "See which mobs are angry at you");
        add("shapeshifting", "Shapeshifting", "Mystique", PowerLevel.BETA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Look like any mob you've hit");
        add("wolf_form", "Wolf Form", "Wolfsbane", PowerLevel.DELTA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Become a wolf: fast, strong bite");
        add("bird_form", "Bird Form", "", PowerLevel.GAMMA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Become a bird and fly");
        add("fish_form", "Fish Form", "", PowerLevel.DELTA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Turn into a small tropical fish: super fast underwater, slow on land");
        add("beast_form", "Beast Form", "Beast", PowerLevel.GAMMA, PowerCategory.SHAPESHIFTING, PowerType.PASSIVE,
                "Blue furry body: strong, agile, climbs walls");
        add("ghost_form", "Ghost Form", "", PowerLevel.BETA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Become see-through and untouchable");
        add("statue_form", "Statue Form", "", PowerLevel.DELTA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Turn to stone: can't move but almost nothing hurts you");
        add("power_mimic", "Power Mimic", "Synch", PowerLevel.ALPHA, PowerCategory.SHAPESHIFTING, PowerType.PASSIVE,
                "Copy the power of the last mutant player near you for a while");
        add("power_absorption", "Power Absorption", "Rogue", PowerLevel.ALPHA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Steal a player's power for 60 seconds, or drain a mob to use its special move");
        add("baby_form", "Baby Form", "", PowerLevel.EPSILON, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Become small like a baby mob");
        add("slime_body", "Slime Body", "", PowerLevel.DELTA, PowerCategory.SHAPESHIFTING, PowerType.BOTH,
                "Bouncy body, no fall damage, split into small slimes when hurt");
        add("bee_swarm_form", "Bee Swarm Form", "", PowerLevel.BETA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Become a swarm of bees");
        add("dragon_form", "Dragon Form", "", PowerLevel.ALPHA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Become a dragon: fly and breathe fire");
        add("werewolf", "Werewolf", "", PowerLevel.GAMMA, PowerCategory.SHAPESHIFTING, PowerType.PASSIVE,
                "Stronger and faster at night");
        add("elemental_form", "Elemental Form", "", PowerLevel.ALPHA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Switch between fire, water, earth and air bodies");
        add("enderman_form", "Enderman Form", "", PowerLevel.GAMMA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Teleport away when hit and pick up blocks");
        add("undead_body", "Undead Body", "", PowerLevel.DELTA, PowerCategory.SHAPESHIFTING, PowerType.PASSIVE,
                "Zombies and skeletons ignore you; healing potions hurt you");
        add("golem_form", "Golem Form", "", PowerLevel.BETA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Become an iron golem: huge strength and health, slow");
        add("liquid_metal_body", "Liquid Metal Body", "", PowerLevel.ALPHA, PowerCategory.SHAPESHIFTING, PowerType.PASSIVE,
                "Arrows pass through you and wounds close instantly");
        add("total_transformation", "Total Transformation", "", PowerLevel.OMEGA, PowerCategory.SHAPESHIFTING, PowerType.ACTIVE,
                "Become any mob in the game with all its abilities");
        add("plant_growth", "Plant Growth", "", PowerLevel.DELTA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Crops and trees around you grow instantly");
        add("vine_grab", "Vine Grab", "", PowerLevel.GAMMA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Vines grab mobs and hold them still");
        add("animal_friend", "Animal Friend", "", PowerLevel.EPSILON, PowerCategory.NATURE_AND_ANIMALS, PowerType.PASSIVE,
                "Animals follow you and are easier to tame");
        add("animal_command", "Animal Command", "", PowerLevel.GAMMA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Animals and pets attack your target");
        add("sleep_pollen", "Sleep Pollen", "", PowerLevel.DELTA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Pollen cloud that slows and blinds mobs");
        add("photosynthesis", "Photosynthesis", "", PowerLevel.EPSILON, PowerCategory.NATURE_AND_ANIMALS, PowerType.PASSIVE,
                "Sunlight fills your hunger");
        add("thorn_skin", "Thorn Skin", "", PowerLevel.DELTA, PowerCategory.NATURE_AND_ANIMALS, PowerType.PASSIVE,
                "Thorns hurt mobs that hit you");
        add("leaf_camouflage", "Leaf Camouflage", "", PowerLevel.EPSILON, PowerCategory.NATURE_AND_ANIMALS, PowerType.PASSIVE,
                "Invisible while crouching in leaves or tall grass, mobs that come close get tangled");
        add("tree_form", "Tree Form", "", PowerLevel.GAMMA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Root in place: big armor and regeneration, can't move");
        add("bee_call", "Bee Call", "", PowerLevel.DELTA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Angry bees attack your target");
        add("mushroom_spores", "Mushroom Spores", "", PowerLevel.GAMMA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Spores poison mobs and grow mushrooms");
        add("wolf_pack", "Wolf Pack", "", PowerLevel.GAMMA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Summon a pack of wolves that fight with you");
        add("bird_flock", "Bird Flock", "", PowerLevel.GAMMA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "A flock of birds snatches what mobs hold and carries small mobs into the air");
        add("harvest_bounty", "Harvest Bounty", "", PowerLevel.EPSILON, PowerCategory.NATURE_AND_ANIMALS, PowerType.PASSIVE,
                "More drops from crops and animals");
        add("gaia_link", "Gaia Link", "", PowerLevel.ALPHA, PowerCategory.NATURE_AND_ANIMALS, PowerType.PASSIVE,
                "Crops grow as you walk, animals obey you, trees sprout");
        add("insect_command", "Insect Command", "", PowerLevel.BETA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Bees, spiders and silverfish nearby fight for you");
        add("spider_powers", "Spider Powers", "", PowerLevel.GAMMA, PowerCategory.NATURE_AND_ANIMALS, PowerType.BOTH,
                "Climb walls, R shoots webs, spiders leave you alone and your hits sometimes trap mobs");
        add("fish_caller", "Fish Caller", "", PowerLevel.EPSILON, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Fish come to you; much better fishing luck");
        add("monster_tamer", "Monster Tamer", "", PowerLevel.ALPHA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Tame hostile mobs as pets");
        add("seed_bomb", "Seed Bomb", "", PowerLevel.DELTA, PowerCategory.NATURE_AND_ANIMALS, PowerType.ACTIVE,
                "Throw seeds that burst into grabbing vines");
        add("invisibility", "Invisibility", "", PowerLevel.GAMMA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Turn invisible until you attack something, but it makes you hungry");
        add("light_bending", "Light Bending", "Dazzler", PowerLevel.BETA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "A dazzling light show: mobs inside are blinded, dizzy and attack each other");
        add("darkness_control", "Darkness Control", "", PowerLevel.BETA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Cover an area in darkness that only you can see through");
        add("shadow_walk", "Shadow Walk", "", PowerLevel.ALPHA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Teleport from one dark spot to another");
        add("flash", "Flash", "", PowerLevel.DELTA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Blind all mobs around you");
        add("glow", "Glow", "", PowerLevel.EPSILON, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Light up the area around you like a torch");
        add("camouflage", "Camouflage", "", PowerLevel.DELTA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.PASSIVE,
                "Invisible while standing still");
        add("silent_steps", "Silent Steps", "", PowerLevel.EPSILON, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.PASSIVE,
                "Mobs don't hear you and sculk sensors don't trigger");
        add("shadow_clones", "Shadow Clones", "", PowerLevel.GAMMA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "In the dark, shadow copies rise behind monsters and fight; R again swaps you with one");
        add("hologram", "Hologram", "", PowerLevel.DELTA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Project a fake image of yourself to distract mobs");
        add("darkforce", "Darkforce", "Cloak", PowerLevel.ALPHA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Swallow mobs into a shadow dimension that hurts them");
        add("laser", "Laser", "", PowerLevel.GAMMA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Thin focused laser beam");
        add("starlight_power", "Starlight Power", "", PowerLevel.DELTA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.PASSIVE,
                "Stronger at night");
        add("shadow_cloak", "Shadow Cloak", "", PowerLevel.GAMMA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Monsters can't see you for 10 seconds and your first hit does triple damage");
        add("rainbow_beam", "Rainbow Beam", "", PowerLevel.BETA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Rainbow beam that gives mobs random effects");
        add("light_speed", "Light Speed", "", PowerLevel.ALPHA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Turn into light and shoot far in a straight line");
        add("blinding_touch", "Blinding Touch", "", PowerLevel.EPSILON, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Touching a mob blinds it");
        add("eclipse", "Eclipse", "", PowerLevel.OMEGA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.ACTIVE,
                "Turn day into night or night into day");
        add("light_eater", "Light Eater", "", PowerLevel.DELTA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.BOTH,
                "Bright light heals you, and R eats the torches around you for a big heal");
        add("unnoticed", "Unnoticed", "", PowerLevel.GAMMA, PowerCategory.LIGHT_SHADOW_AND_STEALTH, PowerType.PASSIVE,
                "Mobs only notice you once you attack");
        add("reality_warping", "Reality Warping", "Legion", PowerLevel.OMEGA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Rewrite the world: change blocks into other blocks and mobs into animals");
        add("time_stop", "Time Stop", "", PowerLevel.OMEGA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Freeze all mobs and arrows for a few seconds");
        add("slow_motion", "Slow Motion", "", PowerLevel.ALPHA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Everything around you moves in slow motion");
        add("rewind", "Rewind", "", PowerLevel.ALPHA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Jump back to where and how you were 10 seconds ago");
        add("gravity_control", "Gravity Control", "Polaris", PowerLevel.ALPHA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Make mobs float away or slam them to the ground");
        add("luck", "Luck", "Domino", PowerLevel.BETA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.PASSIVE,
                "Better loot, more critical hits, mobs miss you more");
        add("transmutation", "Transmutation", "", PowerLevel.ALPHA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Turn blocks into other blocks (stone into iron)");
        add("pocket_space", "Pocket Space", "", PowerLevel.BETA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Open your ender chest anywhere");
        add("duplication", "Duplication", "Multiple Man", PowerLevel.BETA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Make copies of yourself that fight");
        add("molecular_breakdown", "Molecular Breakdown", "", PowerLevel.ALPHA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Break any block instantly, even obsidian");
        add("power_negation", "Power Negation", "Leech", PowerLevel.BETA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.PASSIVE,
                "Mutants near you lose their powers");
        add("power_boost", "Power Boost", "", PowerLevel.GAMMA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Make nearby friends' powers stronger for a while");
        add("power_granting", "Power Granting", "", PowerLevel.OMEGA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Give another player a second power for a while");
        add("aging_touch", "Aging Touch", "", PowerLevel.GAMMA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Grow baby animals and crops instantly; weaken old mobs");
        add("wormhole", "Wormhole", "", PowerLevel.ALPHA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Swallow mobs or items and spit them out somewhere else");
        add("force_field", "Force Field", "", PowerLevel.GAMMA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Bubble that protects you and friends");
        add("lucky_aura", "Lucky Aura", "", PowerLevel.DELTA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.PASSIVE,
                "Friends near you get Luck");
        add("hex_bolts", "Hex Bolts", "Scarlet Witch", PowerLevel.GAMMA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Bolts that cause random bad luck to mobs");
        add("black_hole", "Black Hole", "", PowerLevel.OMEGA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Create a black hole that pulls everything in");
        add("decay", "Decay", "", PowerLevel.BETA, PowerCategory.REALITY_TIME_AND_SPACE, PowerType.ACTIVE,
                "Blocks crumble and mobs' armor breaks around you");
        add("technopathy", "Technopathy", "Forge", PowerLevel.BETA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Control redstone, doors and machines from far away");
        add("techno_organic_body", "Techno-Organic Body", "Warlock", PowerLevel.ALPHA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Hold iron, gold or copper and press R to eat it: repairs your armor and gives extra armor");
        add("super_genius", "Super Genius", "", PowerLevel.GAMMA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "More XP from everything and cheaper enchanting");
        add("redstone_pulse", "Redstone Pulse", "", PowerLevel.DELTA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Power all redstone around you");
        add("item_magnet", "Item Magnet", "", PowerLevel.EPSILON, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "Items fly to you from far away");
        add("repair_touch", "Repair Touch", "", PowerLevel.DELTA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Repair your tools and armor");
        add("weapon_summon", "Weapon Summon", "", PowerLevel.GAMMA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Create a glowing weapon from nothing");
        add("blast_proof", "Blast Proof", "", PowerLevel.EPSILON, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "Immune to explosions");
        add("self_detonation", "Self-Detonation", "Nitro", PowerLevel.GAMMA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Explode without dying (long cooldown)");
        add("matter_absorption", "Matter Absorption", "", PowerLevel.BETA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "Touch a material to take its traits (stone = armor, iron = strength)");
        add("built_in_compass", "Built-in Compass", "", PowerLevel.EPSILON, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "Always see your coordinates and direction");
        add("perfect_memory", "Perfect Memory", "", PowerLevel.EPSILON, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "Keep your XP when you die");
        add("metal_wings", "Metal Wings", "", PowerLevel.GAMMA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.BOTH,
                "Fly with wings made of metal");
        add("living_battery", "Living Battery", "", PowerLevel.DELTA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "Lightning and redstone charge you with Strength");
        add("instant_crafting", "Instant Crafting", "", PowerLevel.DELTA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Craft anything without a crafting table");
        add("psionic_armor", "Psionic Armor", "Armor", PowerLevel.BETA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Energy armor that grows stronger the more you get hit");
        add("sticky_goo", "Sticky Goo", "", PowerLevel.EPSILON, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Shoot goo that slows mobs");
        add("time_bombs", "Time Bombs", "Boom-Boom", PowerLevel.GAMMA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.ACTIVE,
                "Throw up to 3 sticky bombs and set them all off with Sneak + R");
        add("adaptation", "Adaptation", "Darwin", PowerLevel.ALPHA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "Become immune to whatever hurt you last");
        add("evolution", "Evolution", "", PowerLevel.OMEGA, PowerCategory.TECH_MATTER_AND_SPECIAL, PowerType.PASSIVE,
                "Start weak and unlock new abilities as you gain XP levels");
    }

    private Powers() {
    }

    private static void add(String id, String name, String basedOn, PowerLevel level, PowerCategory category,
            PowerType type, String description) {
        Power power = new Power(id, name, basedOn, level, category, type, description);
        BY_ID.put(id, power);
        BY_LEVEL.computeIfAbsent(level, l -> new ArrayList<>()).add(power);
    }

    public static Power get(String id) {
        return BY_ID.get(id);
    }

    public static Iterable<String> ids() {
        return Collections.unmodifiableSet(BY_ID.keySet());
    }

    public static List<Power> all() {
        return List.copyOf(BY_ID.values());
    }

    public static List<Power> ofLevel(PowerLevel level) {
        return BY_LEVEL.getOrDefault(level, List.of());
    }

    public static int count() {
        return BY_ID.size();
    }

    /** Like roll, but only from this level and stronger ones (still weighted by their chances). */
    public static Power rollAtLeast(RandomSource random, PowerLevel minimum) {
        int total = 0;
        for (PowerLevel level : PowerLevel.values()) {
            if (level.ordinal() >= minimum.ordinal()) {
                total += level.chancePercent();
            }
        }
        int pick = random.nextInt(total);
        PowerLevel chosen = minimum;
        for (PowerLevel level : PowerLevel.values()) {
            if (level.ordinal() < minimum.ordinal()) {
                continue;
            }
            pick -= level.chancePercent();
            if (pick < 0) {
                chosen = level;
                break;
            }
        }
        List<Power> pool = ofLevel(chosen);
        return pool.get(random.nextInt(pool.size()));
    }

    /** Picks a level by its roll chance first, then a random power within that level. */
    public static Power roll(RandomSource random) {
        PowerLevel level = PowerLevel.roll(random);
        List<Power> pool = ofLevel(level);
        return pool.get(random.nextInt(pool.size()));
    }
}
