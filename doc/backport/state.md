# Work state

As of 2026-11-09. The L2 / FastProjectileAPI / DanmakuAPI library ports are
done and committed in their own repos. The mod repo's A1 bootstrap, dependency
set and backport docs are committed as `1.20.1 port init`; the source port has
not started.

## Library repos

| repo / branch | version | artifact | state |
|---|---|---|---|
| `~/git/l2lib/L2Serial-1.20` @ `1.20.1` | `1.2.7` | `build/libs/l2serial-1.2.7.jar` | committed, builds |
| `~/git/l2lib/L2ModularBlock-1.20` @ `1.20` | `1.2.1` | `build/libs/l2modularblock-1.2.0.jar` (source is 1.2.1; jar built as 1.2.0) | committed, builds |
| `~/git/l2lib/L2ScreenTracker-1.20` @ `main` | `0.1.5` | `build/libs/l2screentracker-0.1.5.jar` | committed, builds |
| `~/git/l2lib/L2ItemSelector-1.20` @ `main` | `0.2.1` | `build/libs/l2itemselector-0.2.1.jar` | committed, builds |
| `~/git/l2lib/L2DamageTracker-1.20` @ `main` | `0.4.4` | `build/libs/l2damagetracker-0.4.4.jar` | committed, builds |
| `~/git/l2lib/L2Library-1.20` @ `1.20` | `2.5.6` | `build/libs/l2library-2.5.6-slim.jar`, `l2library-2.5.6.jar` | committed, builds |
| `~/git/l2lib/L2Tabs-1.20` @ `entity` | `0.4.0` | `build/libs/l2tabs-0.4.0.jar` | committed, builds |
| `~/git/l2lib/L2ModularBlock-1.21.1` @ `1.20.4` | `3.0.0+8` | — | committed; restore only, build unverified |
| `~/git/l2lib/FastProjectileAPI-1.20` @ `1.20.1` | `3.1.2+1.20.1` | `build/libs/fast_projectile_api-3.1.2+1.20.1.jar` | committed, builds |
| `~/git/l2lib/DanmakuAPI-1.20` @ `1.20` | `3.0.6+1.20.1` | `build/libs/danmaku_api-3.0.6+1.20.1.jar` | committed, builds |

`L2DamageTracker-1.20` has one uncommitted change: its `libs/l2tabs-0.4.0.jar`
was replaced by the rebuilt L2Tabs jar so it compiles against the `entity`
branch API. The committed jar is the upstream 0.4.0 build.

## Mod repo (A1 bootstrap, committed)

Branch `backport`. The Forge 1.20.1 build is in place; Java sources are
untouched.

- `build.gradle`: ForgeGradle 6, `mappings channel: official`, Gradle 8.10,
  Java 17, Forge 47.1.3, `mixin { add ... refmap }`.
- `gradle.properties`: 1.20.1 versions, mod version
  `1.0.7-beta.1+1.20.1`, l2/library versions.
- `META-INF/mods.toml` replaces `neoforge.mods.toml`; `pack.mcmeta` is
  15/15/15; mixin config is `JAVA_17` with
  `"refmap": "gensokyolegacy.refmap.json"`.
- `libs/` holds the 1.20.1 jars: `l2library-2.5.6-slim` (plus full and
  sources), the six jars it ships, `l2damagetracker-0.4.4`,
  `fast_projectile_api-3.1.2+1.20.1`, `danmaku_api-3.0.6+1.20.1`,
  `l2harvester-0.1.2`, `Registrate-MC1.20-1.3.11`, `mixinextras-*`, and
  `youkaisfeasts-0.5.2-slim`. The old 1.21.1 jars are still in `libs/`
  (tracked); only the 1.20.1 names are referenced by `build.gradle`.
- Every library has a matching `-sources.jar` in `libs/` for IDE attachment;
  attach it manually (flatDir dependencies do not resolve sources). The
  `l2modularblock` sources jar is `1.2.1` while the binary in use is `1.2.0`,
  the documented version gap above.
- `youkaisfeasts` replaces `youkaishomecoming` as the food-mod dependency: both
  1.20.1 builds share the `dev.xkmc.youkaishomecoming` package, and the feast
  build is the subset that carries every class the mod imports. Its modid also
  matches the existing `BlockFoods` guard.
- Dependency policy (D24): L2Library slim and the jars it ships are
  `implementation`, not `jarJar`'d. Only l2damagetracker, FastProjectileAPI,
  DanmakuAPI, l2harvester, GeckoLib and TerraBlender are `jarJar`'d.
- Verified: `compileClasspath` and `runtimeClasspath` resolve;
  `prepareRuns` accepts the AT and mixin config. `compileJava` stops at 11
  Java 21 record-pattern parse errors in 4 files (see
  [`source-port.md`](source-port.md)).

## Changes per repo

### L2Serial

- `ArrayCodec`: reallocate when `ans == null || Array.getLength(ans) < n`
  (`fix arr len`).
- `Handlers`: `JSON_MAP`, `NBT_MAP`, `PACKET_MAP`, `MAP` are
  `ConcurrentHashMap`.
- `GenericCodec`: `Handlers.LIST.add(this)` inside `synchronized
  (Handlers.LIST)`.
- `RecordCache` already uses the thread-safe `LazyExc`; the 1.21 record-codec
  additions do not apply to 1.20.1.

### L2ModularBlock 1.20

- New: `BlockTemplates`, `VoxelBuilder`, `DelegateBlockSimpleWaterImpl`,
  `DelegateEntityBlockSimpleWaterImpl`, `impl/SimpleWaterloggedImpl`,
  `impl/DoubleBlockImpl`, `type/WaterloggedMethodMarker`, and
  `mult/{StepOn, ChangeStateExtra, PathFind, UseItemOn, UseWithoutItem,
  ToolModify, PlayerDestory}BlockMethod`.
- Modified: `BlockImplementor` (`first`), `DelegateBlock` (waterlogged/entity
  subclass selection), `DelegateBlockImpl` (placement null-propagation,
  `hasCollision`, `use` dispatch chain, `stepOn`, `isPathfindable`,
  `playerDestroy`, `playerWillDestroy`, `getToolModifiedState`),
  `BlockEntityBlockMethodImpl` (`UseWithoutItemBlockMethod`),
  `AllDireBlockMethodImpl` / `HorizontalBlockMethodImpl`
  (`BlockProxy` → `BlockTemplates`), `PlacementBlockMethod` (`@Nullable`).
- Platform adaptations: `ItemInteractionResult` → `InteractionResult`,
  `ItemAbility` → Forge `ToolAction`, 4-arg `isPathfindable`,
  `void playerWillDestroy`; legacy `OnClickBlockMethod` kept as fallback.

### L2ModularBlock 1.21.1

- Restored the five files missing from `origin/1.20.4`; the tree matches
  `libs/l2modularblocks-3.0.0+8-sources.jar` file-for-file.
- `mod_version` bumped `3.0.0+7` → `3.0.0+8`.
- Not compiled: the 1.21.1 NeoForm cache is absent.

### L2ScreenTracker

- Added `init/MouseCache`, `screen/packets/CacheMouseToClient`,
  `mixin/MinecraftMixin`.
- `CacheMouseToClient` is registered as `PLAY_TO_CLIENT`; sent from
  `SimpleMenuAction.perform` and `ScreenTracker.serverRestore`.
- `MinecraftMixin` wraps `MouseHandler.releaseMouse` in `Minecraft.setScreen`.
- Added `src/main/resources/META-INF/accesstransformer.cfg` with the SRG
  names `f_91507_` / `f_91508_`; `MouseHandler.xpos` / `ypos` are private in
  1.20.1.
- Deps: l2library 2.5.5 slim, l2serial 1.2.7, l2tabs 0.4.0; MC 1.20.1 /
  Forge 47.1.3.

### L2ItemSelector

- Deps: l2library 2.5.5, l2serial 1.2.7, l2modularblock 1.2.0,
  l2screentracker 0.1.5.

### L2DamageTracker

- Deps: l2library 2.5.5, l2serial 1.2.7, l2tabs 0.4.0.

### L2Library

- Deps and jarJar ranges updated to the rebuilt set: l2serial 1.2.7,
  l2tabs 0.4.0, l2screentracker 0.1.5, l2modularblock 1.2.0,
  l2itemselector 0.2.1.
- `l2damagetracker 0.4.4` stays `runtimeOnly` (not nested), matching the
  original build file.
- 2.5.6 fixes `RayTraceUtil.serverGetTarget` to check `instanceof
  LivingEntity` instead of an unchecked cast.
- The nested `l2tabs-0.4.0.jar` is byte-identical to the rebuilt
  `L2Tabs-1.20/build/libs/l2tabs-0.4.0.jar` (SHA-1 `d4d359d9…`).
- Class-surface check on the slim jar: `base/overlay`, `base/L2Registrate`,
  `base/effects/EffectUtil`, `base/menu/base`, `base/tile`,
  `capability/entity`, `capability/player`, `compat/patchouli`,
  `serial/advancements`, `serial/config`, `serial/recipe` (incl.
  `BaseRecipeCategory`), `util/raytrace`, `init/explosion` are present.
  `SyncedData`, `BaseSavedData`, `serial/loot`, `SchedulerHandler` and
  `ConfigInit` are not in the library; they belong to the mod-side compat
  layer (O6).

### L2Tabs

- Branch is `entity` — the legacy 1.20.1 code. `main` is the experimental tab
  rework that was adopted in 1.21.1; do not build the backport from it.
- `ll_version` bumped `0.3.3` → `0.4.0`; deps moved to l2library 2.5.5 slim
  and l2serial 1.2.7.
- Rebuilt from `entity`. `L2ScreenTracker` and `L2DamageTracker` were
  recompiled against that jar because their `l2tabs` imports differ from the
  rework API.
- A `clean` build first fails because ForgeGradle generates the AT variant
  lazily and two configurations resolve it concurrently. Both passes write
  the same `build/fg_cache/..._mapped_official_1.20.1.jar`, so one deletes it
  while the other is copying; the surviving jar is incomplete and FART fails
  with `imtd is null`. Gradle then reports the variant as missing from
  `bundeled_repo`.
- Workaround that works: after `rm -rf build`, resolve one configuration
  first (`./gradlew dependencies --configuration compileClasspath`) so the
  AT jar is produced once and its hash store is saved, then run
  `./gradlew build --offline` (cache hit, no second AT pass).

### FastProjectileAPI

- Branch `1.20.1` is 3.1.2 ported down; the 1.21.1 package layout is kept
  (`entity/`, `collision/`, `render/`, `spellcircle/`, `mixin/`).
- ForgeGradle 5.1 / MC 1.20.1 / Forge 47.1.3, Gradle 7.3 wrapper,
  `mods.toml` instead of `neoforge.mods.toml`, mixins level `JAVA_17` with
  refmap.
- AT uses SRG names and un-finals `Entity.setPosRaw`; this fixed the
  `setPosRaw` / `position` / `blockPosition` compile errors.
- Artifact `fast_projectile_api-3.1.2+1.20.1.jar` verified with refmap and
  AT.

### DanmakuAPI

- Branch `1.20` is 3.0.6 ported down from 1.21.1; package layout unchanged,
  no mixin config.
- Build: ForgeGradle 5.1 / MC 1.20.1 / Forge 47.1.3; Registrate
  `MC1.20-1.3.11`; `mods.toml`.
- Deps: l2library 2.5.6 slim, l2serial 1.2.7, l2damagetracker 0.4.4,
  fast_projectile_api 3.1.2+1.20.1.
- `./gradlew build --offline` passes; artifacts
  `danmaku_api-3.0.6+1.20.1.jar` and `-sources.jar`.
- API substitutions are listed in `discrepancies.md` §6.6–6.7. The two that
  change the mod's imports: `DanmakuHurtEvent.getResult()` →
  `getAnswer()`, and `ItemProviderEntry<Item, T>` → `ItemProviderEntry<T>`.
- The `libs/` directory in this repo still contains the old 1.21.1 jars next
  to the copied 1.20.1 ones; only the 1.20.1 jars are referenced by
  `build.gradle`.

## Toolchain

The 1.20.1 library repos use Gradle 7.3. Running them on JDK 21 makes any
changed `build.gradle` fail with `Unsupported class file major version 65`
during script recompilation. Use JDK 17:

```
export JAVA_HOME=/Users/arthur/Library/Java/JavaVirtualMachines/temurin-17.0.20.1/Contents/Home
```

ForgeGradle 5.1.77 also fails on the TLS certificate of
`maven.minecraftforge.net` on this machine; pass
`-Dnet.minecraftforge.gradle.check.certs=false` if a dependency download
fails (the dependency downloads themselves still work).

Any project that declares an access transformer is exposed to the concurrent
AT-variant race described under L2Tabs. If a clean build fails with FART
`imtd is null` / a missing `..._at_...` variant, wipe `build/` and resolve a
single configuration before building.

## Next

A0 (L2 libraries), B0 (FastProjectileAPI), B1 (DanmakuAPI) and A1 (mod
bootstrap) are complete. The source port has not started.

1. Parse unblock: rewrite the four record patterns listed in
   [`source-port.md`](source-port.md). This is the only thing standing
   between `compileJava` and the real attribution errors.
2. A2: land the O6 compat layer (`Val`, `SR`, `CdcVal`, `SimpleEntry`,
   `LegacyHolder`, `LootHelper`, `SyncedData`, `BaseSavedData`, `ConfigInit`,
   `DCStack`, `SchedulerHandler`, `DataGenHolder`, `DatapackReg`, the
   `L2Registrate` helpers, and the D4 component shim). The surface is frozen
   in [`source-port.md`](source-port.md); implement in the order listed there.
3. Loader swap: `net.neoforged.*` → `net.minecraftforge.*` (125 files), then
   the non-mechanical deltas (capabilities, fluid ingredients, tick events),
   then A3–A10 in the order in [`plan.md`](plan.md).
