# Backport plan

Two workstreams run in parallel:

- **A — mod port.** Forge 1.20.1, L2 2.x plus backported deltas, data-component shim.
- **B — danmaku libraries.** `FastProjectileAPI` then `DanmakuAPI`. Both are
  standalone repos and are now built and copied to `libs/`.

Phase A0 (library backports) gates every mod phase that imports L2 code. A1
(bootstrap) is done; the source port starts at A2. The concrete compile
baseline, rewrite table and compat surface are in
[`source-port.md`](source-port.md).

## Current state

| phase | state |
|---|---|
| A0 — L2 libraries | done. L2Library 2.5.6 (slim/full/sources), L2Serial 1.2.7, L2Tabs 0.4.0, L2ScreenTracker 0.1.5, L2ModularBlock 1.2.0, L2ItemSelector 0.2.1, L2DamageTracker 0.4.4 built and in `libs/` |
| B0 — FastProjectileAPI | done. 3.1.2+1.20.1 |
| B1 — DanmakuAPI | done. 3.0.6+1.20.1 |
| A1 — bootstrap | done. Forge 47.1.3 / Java 17 / Gradle 8.10; classpaths resolve, `prepareRuns` accepts the AT and mixin config |
| A2 — L2 wiring and shim | surface frozen, implementation not started |
| A3–A10 | not started |

The dependency set is settled: L2Library slim and the six jars it ships are
`implementation` (D24); l2damagetracker, FastProjectileAPI, DanmakuAPI,
l2harvester, GeckoLib and TerraBlender are `jarJar`'d. `youkaisfeasts
0.5.2-slim` replaces `youkaishomecoming` as the food-mod dependency — both
share the `dev.xkmc.youkaishomecoming` package and the feast build is the
subset carrying every class the mod imports; l2harvester is bundled because
the slim feast build does not ship it.

Compile baseline: `compileJava` stops at 11 Java 21 record-pattern parse
errors in 4 files (`QuestRewardCategory`, `PartyGiftReader`, `LootReader`,
`GLCommands`). No dependency or symbol errors are hidden behind them. The
source port has not started.

Next: clear the four record patterns, land the A2 compat layer in the order
recorded in [`source-port.md`](source-port.md), then do the loader swap
(`net.neoforged.*` → `net.minecraftforge.*`, 125 files) and the
non-mechanical deltas before A3.

## Phases

### A0 — L2 library backports (done)

Adopt the legacy 1.20.1 API and class location, and add only the contents the
mod needs (D20–D23). Rebuild `L2Library`, `L2Serial`, `L2ModularBlock`,
`L2ItemSelector`, `L2DamageTracker`, `L2Tabs` and `L2ScreenTracker` and publish
the jars to `libs/`. `L2DamageTracker` and `L2Tabs` are rebuilt at
their legacy API (`L2Tabs` from the `entity` branch); the 1.21.1 refactor is
not ported. The jar-by-jar breakdown,
build order and target versions are in [`library-plan.md`](library-plan.md).

Exit: each library builds on 1.20.1; `L2Tabs` is rebuilt from `entity` so
the legacy `TabRegistry.registerTab` API is present; every class GensokyoLegacy
imports exists; the 1.20.1 surface matches what the mod is rewritten against.

All library decisions are closed (O2–O7); the jar order and outputs are in
[`library-plan.md`](library-plan.md).

### B0 — FastProjectileAPI (done)

Lift the vendored 1.20.1 implementation out of `Youkai-Homecoming-1.20` `main`
(46 files under `src/main/java/dev/xkmc/fastprojectileapi`), move the six
`render/core` classes to `render` so the package names match 3.x, and build the
`FastProjectileAPI-1.20` repo.

Exit: builds on Forge 1.20.1 and publishes a jar exposing the 12 classes the mod
uses.

### B1 — DanmakuAPI (done)

Port from the reference's in-mod code (`content/entity/danmaku`,
`content/spell`, `content/particle`, `content/item/danmaku`,
`init/registrate/YHDanmaku`) into the library's package layout. The 3.x `api/`
package has no counterpart and is rebuilt on top of `IYHDanmaku` and
`YHBaseDanmakuEntity`.

Exit: builds against B0 and publishes a jar exposing the 42 classes the mod
uses.

### A1 — Bootstrap (done)

ForgeGradle 6, Java 17, Forge 47.1.3, Gradle 8.10, `mods.toml`, `pack.mcmeta`
15, Forge event bus, mixin `JAVA_17`, access-transformer audit. L2Library is
an explicit external dependency (D24): the slim jar and the jars it ships are
`implementation`, not `jarJar`'d.

Exit: `compileClasspath` and `runtimeClasspath` resolve, and `prepareRuns`
accepts the AT and mixin config. `runClient` waits for the source port.

### A2 — L2 wiring and data-component shim (surface frozen)

Declare the backported libraries, add the `DataComponents` shim over NBT, and
enumerate the exact API surface the 27 affected files use before writing it.
The surface is frozen in [`source-port.md`](source-port.md): `Val`,
`SimpleEntry`, `LegacyHolder`, `GLRegistrate`, `SR` / `CdcReg` / `DCVal`,
`AttReg` / `AttVal`, `compat.components`, and the remaining helpers.

Implement in that order — the registration wrappers unblock the most call
sites, and `AttReg` / `AttVal` is the only piece that needs a design pass.

Exit: shim surface frozen; `PotionUtils`, `DiggerItem`/`Tier`,
`hasCustomHoverName`, `getDefaultAttributeModifiers` and `EquipmentSlot`
replacements identified per call site.

### A3 — Registration

`GLItems`, `GLBlocks`, `GLEntities`, fluids, menus, particles, effects.
`DeferredHolder` → `RegistryObject`; fill the `L2Registrate` gaps (`particle`,
`registerSynced`, `registerUnsynced`, `dataReg`).

Exit: all registries compile and the game reaches the main menu with blocks and
items registered.

### A4 — Data layer

Attachments → Forge capabilities, data maps → `BaseConfig` / `ConfigTypeEntry`
(`MenuLayoutConfig` is the worked example), datapack registries →
`DataPackRegistryEvent`, `ConfigInit` → `ForgeConfigSpec`.

Exit: attachments round-trip in a world; dialog/quest/trade datapacks load.

### A5 — Networking

All 49 packet classes onto l2serial `SerialPacketBase` and
`BasePacketHandler`: 44 serial records and 5 `SimplePacketBase` classes, of
which `GLPackets` registers 43.

Exit: every packet registers and a client/server round trip succeeds.

### A6 — Worldgen

Dimension, biomes, structures, placements, features, `FlatCheckStructure`,
`forge/biome_modifier`, TerraBlender, datagen.

Exit: `runData` emits worldgen JSON; `tools/terrain.py` renders the dimension;
`tools/checkconfig.py` passes.

### A7 — Entities and AI

`SyncedData`, attributes, brains/sensors/tasks, GeckoLib models and renderers.

Exit: entities spawn, animate and fight.

### A8 — Client and UI

Events, menus, screens, overlays, tabs, item selector, menu stacker.

Exit: all screens open and the quest tab renders.

### A9 — Compat

Curios 5.10, JEI 15.20, Patchouli, youkaisfeasts 0.5.2, farmersdelight family.
Touhou Little Maid is decision O3.

Exit: optional-mod classes load only when the mod is present.

### A10 — Datagen, tools, full build

Regenerate all 2,971 generated files, update `tools/*.py` paths, commit
generated output, run `./gradlew build`.

Exit: clean build and a loadable jar.

## Ordering rationale

- A0 before A2–A9: every mod phase imports L2 code.
- B0 before B1: DanmakuAPI depends on FastProjectileAPI.
- B in parallel with A: independent repos, no shared build.
- A1 before A3: registration cannot compile without the 1.20.1 loader.
- A2 before A3: the component shim changes item and block constructors.

## Risks

| risk | impact | mitigation |
|---|---|---|
| L2Tabs 0.3.3 jar is a different API generation | mod cannot compile | resolved: rebuilt 0.4.0 from `entity`, legacy `TabRegistry.registerTab` present |
| `DoubleBlockImpl` missing everywhere | 5 furniture registrations blocked | resolved: restored in L2ModularBlock 1.20 (A0) |
| Data-component shim grows without bound | permanent abstraction debt | freeze the surface in A2 |
| 1.21 → 1.20.1 worldgen API differences | dimension does not generate | use the reference `FlatStructure` as the guide, verify with `tools/terrain.py` |
| Danmaku renderer port | projectiles invisible or crash | resolved: FastProjectileAPI rendering ported first (B0) |
| Access-transformer and mixin targets changed | startup crash | audited in A1; all 21 mixins checked, 5 need target changes (listed in [`source-port.md`](source-port.md)) |
| 2,971 generated files | large, error-prone diff | regenerate once at A10, never hand-edit |

## Definition of done

- `./gradlew build` succeeds on Java 17 / Forge 1.20.1.
- `runClient` and `runServer` load with the mod and all required dependencies.
- `runData` output is committed.
- `tools/terrain.py` and `tools/checkconfig.py` pass over the generated data.
- No `net.neoforged` imports remain in `src/main/java`.
