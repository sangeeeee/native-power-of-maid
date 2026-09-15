# Native POWER of Maid

This branch targets **Minecraft 1.21.1 / NeoForge / Java 21**.

This is a NeoForge 1.21.1 port of [astro-jingtao/native-power-of-maid](https://github.com/astro-jingtao/native-power-of-maid).

Required mods:

| Dependency | Version used for development and testing |
| --- | --- |
| NeoForge | 21.1.248 |
| SlashBlade: Resharped | 2.0.7-1.21.1 |
| mrqx's Slashblade Core | 1.4.4 |
| Touhou Little Maid | **The supplied 1.5.3 NeoForge 1.21.1 snapshot** in `libs/` |

The public Touhou Little Maid 1.5.3 release is not a substitute for this snapshot: it lacks the required SlashBlade compatibility. Both jars report the same version, so the build verifies the exact snapshot's SHA-256. See [libs/README.md](libs/README.md) for details. The dependency jar is not bundled into this mod's output.

Build with JDK 21:

```sh
./gradlew build
./gradlew runGameTestServer
```

On Windows use `gradlew.bat`. The installable mod is `build/libs/Native_POWER_of_Maid-neoforge-1.21.1-1.0.2.jar`. Install it alongside the required mods on both client and server.

`runClient` starts the development client. `runClientSmoke` runs the GameTests, copies their generated world into the isolated `run/client-smoke/` directory, renders Bedrock and Gecko maids, saves a screenshot, and exits automatically. This check needs a graphical desktop. GameTest classes and structures live in `src/gameTest/` and are excluded from the release jar.

Validation covers fourteen GameTests: the original eight cover melee combat, soul component persistence/network encoding, anvil embedding, altar recipes, attribute cleanup, cooldown/resurrection behavior, combo rules, and rank payload encoding. Six combat safety tests cover friendly/PVP switches, stale AI and blade targets, melee sweeps and direct hits, Judgement Cut, forced and flying summoned swords after weapon changes, same-owner allies, and preservation of plants and dropped items. The client smoke test checks task synchronization and actual blade drawing for both Bedrock and Gecko models. Its screenshot also allows visual checking of the rank billboard and model positioning.

Maid blade attacks respect SlashBlade's friendly-fire and PVP safety switches. A custom maid attack list or retaliation target cannot override a disabled switch. Target selection filters the native SlashBlade result, preserving its range and blacklist checks, and excludes the maid, its owner, same-owner pets/maids and allies. Melee hits, projectile impacts and incoming damage share these protections; summoned swords and slashes remain attributed to their maid after it switches weapons. Enabling friendly fire still permits attacks on non-allied animals selected by the maid's attack list.

With the dependency versions above, the inspected blade attacks do not destroy blocks: ground-hit fragments are block particles. Server regression tests exercise melee, slashes and Super Judgement Cut around short grass, flowers, mature wheat and a dropped item, and verify that they survive. This check covers the current attacks and dependencies; it does not establish the behavior of additional mods' custom skills.

Blade placement uses the snapshot's existing held-item rendering entry points. The old extra layers combined a maid locator with the player motion rig's absolute height and fixed offsets, which displaced blades on differently sized models. The renderer now anchors the blade at the model's waist locator (or hand locator when the waist locator is absent), retaining the snapshot's coordinate conventions and model scale. Models without either locator keep the snapshot's fallback placement.

SlashBlade-task animations apply `inverse(rest bone) * animated bone` in blade model units, so the player rig's absolute height is removed while combo motion is retained. Other tasks use the snapshot's original renderer. No model names or individual model corrections are used. The client regression check compares blade/sheath position and normal matrices with the snapshot at rest, tests active combo motion and return to rest, and checks missing-locator fallbacks. It covers Reimu, Cirno, Winefox, and mini Winefox.

The migration uses NeoForge event registration and custom payloads, item data components for embedded souls, and the 1.21.1 `recipe/` data directory and altar recipe codec. It retains the local combo logic and permits other maid tasks to use Slash Arts without requiring the Judgement Cut souls.

Migration references: [NeoForge data components](https://docs.neoforged.net/docs/1.21.1/items/datacomponents/), [NeoForge payload registration](https://docs.neoforged.net/docs/1.21.1/networking/payload/), and the supplied jars. The original 1.21.1 attachment was used for reference; the result is built from this repository's source.

This is a fork of the "TLM: True POWER" mod. It removes the dependency on the "True POWER" mod, enabling its integration into modpacks without compatibility issues with "True POWER." "TLM: True POWER" is a mod specifically designed to enhance a maid's ability to utilize SlashBlade for combat purposes. Full credit is attributed to the author of the original mod.


Minor Changes relative to original version:
1. Remove Void Slash (时空裂闪), which is implemented in the "True POWER" mod.
2. Remove a feature that the 【Judgement Cut (裂空审判)】or【Judgement Cut EX (裂空审判 EX)】 is required in all task to use SA (e.g., when play with Touhou Little Maid: Spell).
