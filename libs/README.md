# Touhou Little Maid snapshot

This branch uses `touhoulittlemaid-1.5.3-neoforge+mc1.21.1-snapshot.jar`, supplied by the project owner. The jar is a local compile/runtime dependency and is **not embedded** in Native POWER of Maid's release jar. Install it separately in Minecraft's `mods` directory.

The public Modrinth 1.5.3 release does not contain the SlashBlade compatibility classes used by this branch. Both jars declare `1.5.3-neoforge+mc1.21.1` in their mod metadata, so Gradle checks the snapshot's SHA-256 rather than relying on its version string:

```
ac7c07068be61216180a75e6845dc1e91f8c95a7c2e19ad241b81a127e7802dd
```

The default path is configured as `maid_snapshot_jar` in `gradle.properties`. To use the same jar at another location, pass `-Pmaid_snapshot_jar=/absolute/path/to/the/snapshot.jar`.

Upstream: <https://github.com/TartaricAcid/TouhouLittleMaid>. The supplied jar declares `MIT / CC BY-NC-SA 4.0`; upstream copyright and licensing remain applicable. The GameTest structure in `src/gameTest/resources` is copied from this snapshot for development tests and is not shipped in the mod jar.
