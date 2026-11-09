# Library jar work plan

Scope: every jar the 1.20.1 mod will compile against. Repos are the clones in
`~/git/l2lib/`. Decisions D19–D23 and O2/O3/O5/O6/O7 in
[`decisions.md`](decisions.md) set the target API for each one.

## Build order

```
l2serial
  └ l2library (slim)
      ├ l2tabs
      ├ l2screentracker
      ├ l2itemselector
      ├ l2damagetracker
      └ l2modularblock
l2library (full, jarJars the six)
fastprojectileapi
  └ danmakuapi
```

The libraries compile against `l2library-*-slim` to break the
`L2Library ↔ L2Tabs/L2ScreenTracker/…` cycle. Build the slim jar first, then the
six, then the full jar. FastProjectileAPI only needs l2serial; DanmakuAPI needs
FastProjectileAPI plus the L2 jars.

## Summary

| jar | repo / branch | source | published | action | target |
|---|---|---|---|---|---|
| `l2serial` | `L2Serial-1.20` / `1.20.1` | 1.2.6 | 1.2.6 | audit 1.21 fixes, no API change | 1.2.7 or 1.2.6 |
| `l2library` | `L2Library-1.20` / `1.20` | 2.5.5 | 2.4.x slim only | rebuild from source | 2.5.5 slim + full |
| `l2tabs` | `L2Tabs-1.20` / `entity` | 0.3.3 | 0.3.3 (same legacy line) | rebuild from `entity`, bump | 0.4.0 |
| `l2screentracker` | `L2ScreenTracker-1.20` / `main` | 0.1.4 | 0.1.4 | add `CacheMouseToClient`, `MouseCache` | 0.1.5 |
| `l2itemselector` | `L2ItemSelector-1.20` / `main` | 0.2.1 | 0.1.9 | rebuild from source | 0.2.1 |
| `l2damagetracker` | `L2DamageTracker-1.20` / `main` | 0.4.4 | 0.3.7 (stale) | rebuild from source | 0.4.4 |
| `l2modularblock` | `L2ModularBlock-1.20` / `1.20.4` → `1.20` | 1.1.1 | 1.1.0 | restore 1.21.1 content, port to 1.20 | 1.2.0 |
| `fastprojectileapi` | `FastProjectileAPI-1.20` / `main` (3.1.1, 1.21.1) | — | none for 1.20.1 | new `1.20.1` branch, port down | 3.1.2+1.20.1 |
| `danmakuapi` | `DanmakuAPI-1.20` / `main` (3.0.6+3, 1.21.1) | — | none for 1.20.1 | new `1.20.1` branch, port down | 3.0.6+1.20.1 |
| `l2harvester` | `Youkai-Homecoming-1.20` build | 0.1.2 | 0.1.2 | not ported; jar-in-jar'd for `youkaisfeasts` | 0.1.2 |
| `mobweaponapi` | — | — | — | not imported | — |

## 1. l2serial

Repo `L2Serial-1.20`, branch `1.20.1`, source and jar both 1.2.6.

- **No API change** (D20). Keep `BasePacketHandler`, `SimplePacketBase`,
  `SerialPacketBase` as abstract classes and the `SimpleChannel` transport.
- Audit `origin/1.21` for platform-neutral fixes and cherry-pick only those:
  `fix arr len`, `concurrency fix`, `datagen fix`, `context fix`. The 1.20.1
  branch already has `fix tree codec`, `fix null handling`, `byte arr fix` and
  `fix attr`.
- Do **not** port `PacketHandler`, `CodecAdaptor`, `IPayloadContext`,
  `StreamCodec` or the marker-package move.
- `Handlers.registerReg` is not added; `GiftDialogData` stores a
  `ResourceLocation` and resolves it through `CodecRegistry.DIALOG` instead
  (O6 compat layer).

Output: `l2serial-1.2.7` if a fix applies, otherwise keep 1.2.6.

## 2. l2library

Repo `L2Library-1.20`, branch `1.20`, source 2.5.5. The copies in the other
repos' `libs/` are 2.4.x slim builds; there is no 2.5.x jar locally.

- **Rebuild only.** The mod-side compat layer (O6) takes `SyncedData`,
  `BaseSavedData`, `serial/loot`, `SchedulerHandler` and `ConfigInit`, so the
  library jar needs no new classes.
- Verify the classes the mod will import are present at their legacy paths:
  `base/overlay/*`, `base/L2Registrate`, `base/effects/EffectUtil`,
  `base/menu/base/*`, `base/tile/*`, `capability/entity/*`,
  `capability/player/*`, `compat/patchouli/*`, `serial/advancements/*`,
  `serial/config/*`, `serial/recipe/*` (including `BaseRecipeCategory`),
  `util/raytrace/*`, `init/explosion/*`.
- Build `-slim` first, then the full jar after the six dependants are rebuilt.

Output: `l2library-2.5.5-slim` and `l2library-2.5.5`.

## 3. l2tabs

Repo `L2Tabs-1.20`, branch `entity` (the legacy 1.20.1 line), source 0.3.3.
`main` is the experimental tab rework adopted in 1.21.1 and is not used here.

- **Rebuild only.** Legacy `TabRegistry.registerTab(priority, factory,
  icon, title)` is the API the mod moves to (D19).
- Bump to `0.4.0` so it cannot be confused with the 0.3.3 jar.

Output: `l2tabs-0.4.0`.

## 4. l2screentracker

Repo `L2ScreenTracker-1.20`, branch `main`, source 0.1.4.

- **Keep the legacy names** (O5): `dev.xkmc.l2screentracker`, `L2ScreenTracker`,
  `L2ScreenTrackerConfig`, `ScreenTracker.register()`.
- Backport two classes from `origin/1.21`: `screen/packets/CacheMouseToClient`
  and its client-side `MouseCache`. Register the packet in
  `L2ScreenTracker.PACKET_HANDLER`. The cursor restore also needs
  `mixin/MinecraftMixin` (wraps `MouseHandler.releaseMouse`) and an AT for the
  private `MouseHandler.xpos` / `ypos` (`f_91507_` / `f_91508_`).
- Keep `PlayerSlot` on `FriendlyByteBuf`; do not adopt
  `RegistryFriendlyByteBuf`.
- Do not port `L2MSReg`, `L2MSConfig` or the `l2menustacker` rename.

Output: `l2screentracker-0.1.5`.

## 5. l2itemselector

Repo `L2ItemSelector-1.20`, branch `main`, source 0.2.1; the jar in
`L2Library-1.20/libs` is 0.1.9.

- **Rebuild only.** Keep the legacy `IItemSelector` signatures (D22).
- The five overlay classes stay in `l2library.base.overlay`; do not add
  `l2itemselector.overlay`.
- Bump `l2library_version` to 2.5.5 before building.

Output: `l2itemselector-0.2.1`.

## 6. l2damagetracker

Repo `L2DamageTracker-1.20`, branch `main`, source 0.4.4; the jars in both
`libs/` folders are the stale 0.3.7.

- **Rebuild only.** Keep `AttackCache`, `AttackListener`, `CreateSourceEvent`,
  `DamageModifier` and `DefaultDamageState` (D19).
- Do not port the 3.x `DamageData` hierarchy.

Output: `l2damagetracker-0.4.4`.

## 7. l2modularblock

Repo `L2ModularBlock-1.20`. Two branches, in this order.

**Step 1 — restore the 1.21.1 content** on `origin/1.20.4` (MC 1.21.1). The
branch is missing exactly the three classes the mod imports:

```
impl/DoubleBlockImpl
mult/PathFindBlockMethod
mult/PlayerDestoryBlockMethod
```

Restore them from the working copy the mod is built against, commit and bump
the 1.21.1 version. This is O2.

**Step 2 — port the 1.21.1 set to `1.20`** at the legacy root (D23, no `core/`
move):

- `BlockTemplates`, `VoxelBuilder`, `DelegateEntityBlockImpl`,
  `DelegateBlockSimpleWaterImpl`, `DelegateEntityBlockSimpleWaterImpl`,
  `impl/SimpleWaterloggedImpl`, `type/WaterloggedMethodMarker`
- `mult/{StepOn, ToolModify, ChangeStateExtra, UseItemOn, UseWithoutItem,
  PathFind, PlayerDestory}BlockMethod`
- `impl/DoubleBlockImpl`
- Keep the legacy `OnClickBlockMethod`; dispatch `useItemOn`/`useWithoutItem`
  from the single 1.20.1 `use(...)`.

Output: `l2modularblock-1.2.0`.

## 8. fastprojectileapi

Repo `FastProjectileAPI-1.20`, `main` is 3.1.1 (1.21.1). There is no 1.20.1
branch.

- Create `1.20.1` and port the 3.1.1 source down. Use the vendored 1.20.1 copy
  in `Youkai-Homecoming-1.20` `main` (46 files) for the platform-specific
  pieces: the collision map, the mixins and the renderer.
- Keep the 3.x package names the mod imports: `render/*` (not `render/core`),
  `spellcircle/*` including `SpellComponent` and `SpellRenderState`,
  `collision/*`, `entity/*`.
- Change `l2serial_ver` to 1.2.6.

Output: `fastprojectileapi-3.1.2+1.20.1`.

## 9. danmakuapi

Repo `DanmakuAPI-1.20`, `main` is 3.0.6+3 (1.21.1). There is no 1.20.1 branch.

- Create `1.20.1` and port the 121 files down. The 1.20.1 implementation source
  is the reference mod's in-mod code: `content/entity/danmaku`,
  `content/spell/{mover,spellcard,item}`, `content/particle`,
  `content/item/danmaku` and `init/registrate/YHDanmaku`.
- Rebuild `api/*` (`IDanmakuEntity`, `IYoukaiEntity`, `DanmakuBullet`, the
  events and `GrazeHelper`) on `IYHDanmaku` / `YHBaseDanmakuEntity`.
- Change the L2 dependency versions to the 1.20.1 set: l2serial 1.2.6,
  l2library 2.5.5, l2damagetracker 0.4.4, l2tabs 0.4.0, l2modularblock 1.2.0,
  l2itemselector 0.2.1.

Output: `danmakuapi-3.0.6+1.20.1`.

## 10. Publish

For each jar:

1. `./gradlew build` (or `jarJar` for L2Library).
2. Copy the artifact into `GensokyoLegacy-backport/libs/`.
3. Update the coordinate/version in `build.gradle` and `gradle.properties`.
4. Keep the `-slim` jar for compile-only dependencies.

## Sequencing

- Start now: L2Serial audit, L2Library slim rebuild, L2Tabs / L2DamageTracker /
  L2ItemSelector rebuilds (no code), FastProjectileAPI `1.20.1` branch.
- Next: L2ScreenTracker `CacheMouseToClient`; L2Library full jar.
- L2ModularBlock: 1.21.1 restore, then the 1.20 port.
- Last: DanmakuAPI, which needs FastProjectileAPI and the L2 jars.
- GensokyoLegacy phase A1 (bootstrap) can run in parallel; A2 (L2 wiring)
  waits for L2Serial, L2Library and the six.
