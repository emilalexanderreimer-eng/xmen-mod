# X-Men Mutants (Minecraft mod)

A NeoForge mod for **Minecraft 1.21.1** where every player is a mutant.

## Install it with CurseForge

1. Download the newest `xmen-*.jar` from the [Latest build](../../releases/tag/latest) release.
2. In the CurseForge app, go to **Minecraft → Create Custom Profile**, pick **Minecraft 1.21.1** and the **NeoForge** loader, and create it.
3. On the profile, click the **⋮** menu → **Open Folder**, then open the `mods` folder (create it if it isn't there).
4. Drop the `.jar` into `mods` and press **Play**.

## Playing

The first time you join a world you get a random power from a pool of 300, shown in the top-left corner.

- `/power` shows your power and what it does.
- `/power list` shows how many powers each level has; `/power list omega` lists the Omega ones.
- `/power set <power>` and `/power random` change your power, but only in creative mode.
- `/power list working` shows the powers that already do something.
- Press **R** to use your power (you can change the key in Options → Controls). Your cooldown shows under the power name.

### Powers that work so far

| Power | How to use it |
| --- | --- |
| Optic Blasts | R: red beam that hurts everything in a line |
| Pyrokinesis | R: throw three fireballs |
| Fireproof | Always on: no fire or lava damage |
| Cryokinesis | R: ice blast freezes a mob, or turns water to ice |
| Gills | Always on: breathe underwater |
| Weather Control | R: lightning where you look. Sneak + R: change the weather. Lightning can't hurt you |
| Feather Fall | Always on: no fall damage |
| Magnetism | Always on: fly (double-tap jump). R: throw metal mobs and items. Sneak + R: pull them in |
| Teleportation | R: teleport where you look |
| Super Speed | Always on: run very fast and step up blocks |
| Wall Crawling | Always on: walk into a wall to climb it, sneak to hold still |
| Super Jump | Always on: jump very high, less fall damage |
| Phasing | R: walk through the wall in front of you |
| Flight | Always on: fly (double-tap jump). R: rocket forward |
| Super Strength | Always on: hit much harder, mine faster |
| Claws | R: claws out (big melee damage) or back in |
| Healing Factor | Always on: fast healing, poison and wither wear off |
| Night Vision | Always on: see in the dark |
| Telekinesis | R: lift the mob you look at. R again: throw it |
| Invisibility | R: invisible for 10 seconds, mobs lose track of you |

The full list of powers is in [tools/powers.psv](tools/powers.psv).

## Building it yourself

GitHub builds the jar automatically on every push. To build locally you need Java 21: run `./gradlew build` and the jar lands in `build/libs/`.
