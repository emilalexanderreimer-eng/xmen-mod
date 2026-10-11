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
- Press **R** to use your power (you can change the key in Options → Controls).

The full list of powers is in [tools/powers.psv](tools/powers.psv).

## Building it yourself

GitHub builds the jar automatically on every push. To build locally you need Java 21: run `./gradlew build` and the jar lands in `build/libs/`.
