# AGENTS.md

NeoForge 1.21.1 mod (Gensokyo Legacy, Touhou characters/structures). Java 21, Gradle 8.8, `net.neoforged.moddev` 2.0.80. Java sources in `src/main/java/dev/xkmc/gensokyolegacy/`.

## Commands
- `./gradlew runClient` / `./gradlew runServer` — run the game.
- `./gradlew runData` — datagen. Writes to `src/generated/resources/`, which is committed to git and merged into the jar as main resources (`build.gradle` line 68). Generated output must be committed alongside code changes.
- `./gradlew build` — full build (jar + sources jar).
- `gradle.properties` sets `org.gradle.daemon=false` and `-Xmx3G`: every Gradle invocation is a slow cold JVM. Batch independent Gradle work into one command.
- `src/test/java/organize/*` are `main()` utilities run from the IDE, **not JUnit tests**. `organize.ResourceOrganizer` merges the split zh_cn lang files (below). `./gradlew test` runs no real tests.
- `python3 tools/export_dialogs.py` — reads the generated `dialog` / `dialog_starter` / `quest` JSON plus the lang files and writes `out/en/<char>.txt` and `out/zh_cn/<char>.txt`: greetings, chats and quest dialogs as review text (gates, actions, requirements, rewards spelled out, dialog trees inlined). Use it to proofread dialogs or the zh_cn translations without launching the game. `--lang en|zh_cn|both`, `--character alice`. Each file opens with a DIALOG ANIMATIONS section listing every `animations` trigger, resolved per character to the clip it really plays (trigger -> `YoukaiAnim` slot -> `getAnim` clip -> the clip's length/bones in `assets/gensokyolegacy/animations/<char>.animation.json`), and each `[anim]` line in the body carries the same resolution. The rig is parsed out of the Java sources, so the table follows a renumbered enum or a renamed clip on its own.
- `python3 tools/stripjarjar.py <jar> [--dry-run] [--backup]` — removes the jar-in-jar payload (`META-INF/jarjar/`) from a mod jar in `libs/`, then verifies the rewrite. Needed because the published `youkaisfeasts` build bundles copies of Registrate / `l2core` / `l2serial` / `l2damagetracker` / `l2modularblocks` / `l2harvester` / TerraBlender that this project already declares as `jarJar(...)`, and the duplicate copies cause a JPMS split-package failure in the dev launcher. Re-run after every re-export from the upstream project. See `doc/tools.md`.
- `python3 tools/checkconfig.py <structure> <template.nbt>` — validates the **generated** `structure_config.json` (house box, interior rooms, nodes) against a structure template: no outdoor air inside a room, rooms disjoint and inside the house box, node cells are standable air in the room they are declared for, the bed is in a room, stair nodes one block off each end of the run with the two rooms touching. Run it after `runData` whenever a structure `.nbt` or `StructureConfigBuilder` changes, and over the untouched structures too — it is structure-agnostic. See `doc/tools.md`.
- `python3 tools/terrain.py <dimension> [--seed N]` — writes a biome map and a height map of a dimension, one pixel per chunk, 512x512 by default, plus a legend `.txt`. Reads the dimension json and the noise router it points at (vanilla density functions come from the client-extra jar, found automatically), so it renders the terrain the game would generate. Use it to see the effect of an offset spline or biome grid change. `--voids` instead reports the caves (carved volume, surface openings, void sizes) from a vertical walk of `final_density`; see `doc/tools.md`.

## Registration (two systems)
- `GensokyoLegacy.REGISTRATE` (`L2Registrate`, dev.xkmc's fork of Registrate) — blocks, items, entities, creative tabs, with datagen. Register classes in `init/registrate/GL*.java` and call `register()` from the `GensokyoLegacy()` constructor (order matters).
- `GensokyoLegacy.REG` (`dev.xkmc.l2core...Reg`) — vanilla/built-in registries (sensors/memories/activities via `SR`, see `GLBrains`) and custom datapack registries via `dataReg` + `CdcVal` (see `content/rpg/core/CodecRegistry.java`).

## Key conventions
- Networking: single `GensokyoLegacy.HANDLER` (l2serial `PacketHandler`); its registration lives in `init/GLPackets.java` (`GLPackets.create(version)`, one chained `.toClient(X.class)` / `.toServer(X.class)` line per packet). Packets use l2serial codecs.
- Custom JSON codecs use l2serial `CodecHandler`/`Handlers` (see the `FluidIngredient` registration in the mod constructor).
- Datapack registries: define a `ResourceKey` in `CodecRegistry.Keys`, register with `REG.dataReg("name", CODEC)`. Runtime JSON lives at `data/gensokyolegacy/gensokyolegacy/<registry>/...` (currently `dialog`, `dialog_starter`, `quest`, `trade`). New option/action/condition/requirement/reward subclasses must be registered in `CodecRegistry`.
- Config: `GLModConfig.SERVER` via `REGISTRATE.registerSynced(...)` (server-synced), built with l2core `ConfigInit` (pattern in `init/data/GLModConfig.java`).
- Optional-mod compat lives in `compat/` (touhoulittlemaid, curios, jei) and must be guarded with `ModList.get().isLoaded(...)`. TLM compat is the most elaborate; always guard new TLM references.
- Mixins go in `mixin/` and must be declared in `src/main/resources/gensokyolegacy.mixins.json` (`defaultRequire: 1` — unlisted/unmatched mixins fail hard).
- Access transformer: `src/main/resources/META-INF/accesstransformer.cfg`.

## Code style
- Never use fully-qualified names (FQNs) inline — always add an `import` and use the simple name. This applies to all Java sources.
- Use package-info.java for all packages with nullability annotation.
- Comments must be concise. A Javadoc block is one line of summary plus at most one more line for the non-obvious *why*; a `//` run is at most two lines. Keep every line within ~120 cols — cut words, not line length.
- Long information does not belong in code. Design rationale, engine quirks, layout tables, tuning tables and worked examples go in `doc/design/<feature>.md`, and the code keeps a pointer: `@see "doc/design/<feature>.md"` on the type, and a bare `party/send.md §3` next to the line it explains. Quote the path — `@see "doc/..."` — so the reference reads as a string and not as a dangling symbol.
- Keep the load-bearing fact when condensing. If a comment explains why the code is shaped oddly (a constraint, a GeckoLib or vanilla quirk), that sentence stays; what goes is restatement of the code, repetition and preamble.
- Preserve every `@param` / `@return` / `@throws` / `@see` and meaningful `{@link}` / `{@code}`. Never add a comment where there is none, never split one comment into two, and never drop a comment entirely to shorten it — merge duplicates instead, keeping the one that matches the code.

## Code layout
- `content/entity/characters/<character>/` — per-character Entity/Model/Renderer (fairy, maiden, rumia, merchant, boss); shared youkai base classes in `content/entity/youkai/` (with `impl/` for the per-character facets — base impl, eating, combat, hurt, the anim enums and the GeckoLib rig — and `renderer/` for renderers and render layers); AI in `content/entity/behavior/` (brain/sensor/task/move/combat), with `module/` (per-entity modules) and `visit/` (the visit scheduler) under it.
- `content/attachment/` — player/entity data, one subpackage per feature: `character/` (character data, `CharacterConfig`), `home/` (`core/`, `custom/`, `structure/`, and its `datamap/` records), `party/` (parties: `core/`, `food/`, `item/`), plus `area/`, `gap/`, `doll/`, `glove/`, `index/`, `misc/`, `storage/`.
- `content/rpg/` — quest/dialog/trade system (condition/requirement/action/reward); `core/` holds `DialogConfig` and the codec registry. Actively developed.
- `content/spell/` — danmaku/spell cards. `content/ui/` — menus and screens.

## Lang
- English: generated from the `GLLang` enum (`init/data/GLLang.java`); `GLLang.FOO$BAR` → key `gensokyolegacy.foo.bar`. Output lands in `src/generated/.../lang/{en_us,en_ud}.json`.
- zh_cn is hand-authored, not generated: split per-category JSON under `src/test/resources/gensokyolegacy/lang/zh_cn/<category>.json`, merged into `src/main/resources/assets/gensokyolegacy/lang/zh_cn.json` by running the `organize.ResourceOrganizer` main. Supports `-cartesian` block expansion. After adding zh_cn strings, rerun the organizer.

## Git
- Two remotes: `base` = upstream `Minecraft-LightLand/GensokyoLegacy`, `origin` = fork `Signifier-Dollhouse`. `main` tracks `base/main`.
- Feature branches in use: `quest`, `multiplex_worldgen`, `youkaishomecoming`.
- Commit style: short lowercase one-liners ("quest tab", "dialog init").
- Bump version via `mod_version` in `gradle.properties` (scheme like `0.3.2+1`).

## Dependencies
- Dev.xkmc L2 ecosystem libs (l2core, l2serial, l2library, l2damagetracker, l2menustacker, l2tabs, l2modularblocks, danmaku_api, fast_projectile_api) are jarJar'd. Dev jars can be pulled from `mavenLocal()` or the flatDir `libs/` (sources jars included) — when bumping a library version, confirm the jar exists locally first. Also GeckoLib, Curios, JEI, Touhou Little Maid, Sodium, Jade.

## Neoform patched sources (MC 1.21.1 + NeoForge 21.1.217)
- Patched sources jar: `build/moddev/artifacts/neoforge-21.1.217-sources.jar`
- Neoform runtime cache (zip, decompiled+patched+named): `~/.gradle/caches/neoformruntime/intermediate_results/sourcesWithNeoForge_58ddab5b2b626895fcf3a4b832c7d56e848354bd_output.zip`
- Neoform runtime metadata: `~/.gradle/caches/neoformruntime/intermediate_results/sourcesWithNeoForge_58ddab5b2b626895fcf3a4b832c7d56e848354bd.txt`
