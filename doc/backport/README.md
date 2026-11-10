# 1.21.1 → 1.20.1 port notes

The Forge 1.20.1 port is complete (`mod_version=1.0.7-beta.2+1.20.1`). This
directory is no longer a work plan: it keeps the mapping tables that a future
merge of 1.21.1 work into the 1.20.1 branch needs.

- [`api.md`](api.md) — what changes between the 1.21.1 code and 1.20.1, by area.
- [`libraries.md`](libraries.md) — the library versions the branch is frozen
  at, and which 1.21 refactors are deliberately not ported.

## Target toolchain

| | 1.21.1 source | 1.20.1 branch |
|---|---|---|
| loader | NeoForge 21.1.x | Forge 47.1.3 |
| Java | 21 | 17 |
| Gradle | 8.8 | 8.10 |
| build plugin | `net.neoforged.moddev` 2.0.80 | ForgeGradle `[6.0,6.2)` |
| mappings | official | official |
| access transformer | official names | SRG names |
| mixin | `JAVA_21` | `JAVA_17` + refmap |
| mod descriptor | `META-INF/neoforge.mods.toml` | `META-INF/mods.toml` |
| `pack.mcmeta` | — | `pack_format` 15 |
| Registrate | `MC1.21-1.3.0+50` | `MC1.20-1.3.11` |
| L2 base | `l2core` 3.x | `l2library` 2.5.7 + `compat/l2lib` shims |

## Merging a 1.21.1 change

1. Bring the source change over with a normal merge, then compile. Do not
   merge generated resources by hand; regenerate them (step 5).
2. Rewrite the imports with the tables in [`api.md`](api.md) §1: `l2core`,
   `l2serial`, `l2modularblock`, `l2menustacker`, `l2itemselector.overlay` and
   `net.neoforged`. Java 21-only syntax is listed in §2.
3. Adapt the constructs that are not import swaps: registration (§3),
   attachments/capabilities (§4), packets (§5), data components (§6), data
   maps (§7), events (§11) and client/rendering (§12).
4. Watch the mixin and access-transformer traps in §15. `defaultRequire: 1`
   makes a missing target a hard failure, so a renamed vanilla method shows up
   as a startup crash, not a compile error.
5. `./gradlew compileJava`, then `./gradlew runData` once the code compiles.
6. Run `python3 tools/export_dialogs.py` if dialogs or quests changed, and the
   `organize.ResourceOrganizer` main if zh_cn changed. Commit generated output
   with the source change.
7. `./gradlew build` and one `runClient` before calling the merge done.

## Build notes

- `java.toolchain.languageVersion = 17`; Gradle itself runs on JDK 17–21.
  The library repos still on Gradle 7.3 must run on JDK 17
  (`Unsupported class file major version 65` otherwise).
- `systemProp.fg.repo.recompile=false` in `gradle.properties` is load-bearing:
  ForgeGradle must use the AT-applied mapped jar, not recompile it.
- ForgeGradle's certificate check fails against `maven.minecraftforge.net` on
  this machine; pass `-Dnet.minecraftforge.gradle.check.certs=false` when a
  dependency download fails. `--offline` is safe after the first resolve.
- A clean build of a project that declares an AT can race two configurations
  resolving the generated AT variant (`imtd is null`, missing `..._at_...`
  variant). Wipe `build/`, resolve one configuration, then build.
- `libs/` is `flatDir`: sources jars are not resolved automatically. Attach the
  matching `-sources.jar` manually in the IDE.
- `python3 tools/stripjarjar.py <jar>` after every re-export of
  `youkaisfeasts`: its jar-in-jar payload duplicates the L2 jars on the dev
  classpath and splits packages. See `doc/tools.md`.
- Java 21 source constructs: record patterns (`instanceof X(...)`) occur in the
  1.21.1 tree. `Math.clamp` and `List.getFirst/getLast` are the other common
  ones.
