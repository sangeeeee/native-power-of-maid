# Native POWER of Maid

This branch targets **Minecraft 1.21.1 / NeoForge / Java 21**.

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

On Windows use `gradlew.bat`. The installable mod is `build/libs/Native_POWER_of_Maid-1.21.1-1.0.2.jar`. Install it alongside the required mods on both client and server.

`runClient` starts the development client. `runClientSmoke` runs the GameTests, copies their generated world into the isolated `run/client-smoke/` directory, renders Bedrock and Gecko maids, saves a screenshot, and exits automatically. This check needs a graphical desktop. GameTest classes and structures live in `src/gameTest/` and are excluded from the release jar.

Validation covers eight GameTests: melee combat, soul component persistence/network encoding, anvil embedding, altar recipes, attribute cleanup, cooldown/resurrection behavior, combo and friendly-target rules, and rank payload encoding. The client smoke test checks task synchronization and actual blade drawing for both Bedrock and Gecko models. Its screenshot also allows visual checking of the rank billboard and model positioning.

The migration uses NeoForge event registration and custom payloads, item data components for embedded souls, and the 1.21.1 `recipe/` data directory and altar recipe codec. It retains the local combo logic and permits other maid tasks to use Slash Arts without requiring the Judgement Cut souls.

Migration references: [NeoForge data components](https://docs.neoforged.net/docs/1.21.1/items/datacomponents/), [NeoForge payload registration](https://docs.neoforged.net/docs/1.21.1/networking/payload/), and the supplied jars. The original 1.21.1 attachment was used for reference; the result is built from this repository's source.

This is a fork of the "TLM: True POWER" mod. It removes the dependency on the "True POWER" mod, enabling its integration into modpacks without compatibility issues with "True POWER." "TLM: True POWER" is a mod specifically designed to enhance a maid's ability to utilize SlashBlade for combat purposes. Full credit is attributed to the author of the original mod.


Minor Changes relative to original version:
1. Remove Void Slash (时空裂闪), which is implemented in the "True POWER" mod.
2. Remove a feature that the 【Judgement Cut (裂空审判)】or【Judgement Cut EX (裂空审判 EX)】 is required in all task to use SA (e.g., when play with Touhou Little Maid: Spell).
