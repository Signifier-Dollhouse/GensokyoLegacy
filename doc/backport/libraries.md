# L2 library state

Every library is checked out under `~/git/l2lib/`. Only the libraries
GensokyoLegacy imports are in scope; `MobWeaponAPI`, `L2Complements` and
`ModularGolems` are content mods and are not needed.

Two questions are answered per library: what the 1.20.1 source branch is missing
relative to the 1.21 line, and whether the 1.20.1 source branch contains
anything that is not in the released jars under `L2Library-1.20/libs/` and
`ModularGolems-1.20/libs/`.

## Overview

| library | 1.20 ref | ver | 1.21 ref | ver | GL classes present / missing | jar vs source |
|---|---|---|---|---|---|---|
| L2Library (as L2Core) | `1.20` | 2.5.6 | `1.21` + L2Core | 3.0.9 | 17 / 25 | **target legacy** |
| L2Serial | `1.20.1` | 1.2.7 | `1.21` | 3.x | 6 / 5 | **fixes only** |
| L2ModularBlock | `1.20` | 1.2.1 | `1.20.4` (MC 1.21.1) | 3.0.0+8 | 17 / 11 | **new contents** |
| L2ItemSelector | `main` | 0.2.1 | `1.21` | 3.x | 10 / 5 | **target legacy** |
| L2DamageTracker | `main` | 0.4.4 | `1.21` | 3.x | 8 / 1 | **target legacy** |
| L2ScreenTracker | `main` | 0.1.5 | `1.21` | 3.x | 3 / 2 | **target legacy** |
| L2Tabs | `entity` | 0.4.0 | `1.21.1` (rework) | 3.0.6+7 | 6 / 0 | **build from `entity`** |
| L2Harvester | `main` | 0.1.2 | `1.21` | 3.x | not imported | dropped |
| FastProjectileAPI | `main` (1.21.1) | 3.1.2+1.20.1 | `main` (1.21.1) | 3.1.2 | 12 / 0 | **ported down, 1.21 layout kept** |
| DanmakuAPI | `main` (1.21.1) | 3.0.6+1.20.1 | `main` (1.21.1) | 3.0.6 | 42 / 0 | **ported down, 1.21 layout kept** |

`L2ModularBlock`'s 3.x branch is named `1.20.4` but its `gradle.properties`
says Minecraft 1.21.1 / version 3.0.0+7. The branch name is stale.

## L2Library / L2Core

The 1.20.1 branch is the 2.x monolith: 190 files across `base`, `capability`,
`serial`, `util`, `init` and `compat`. In 1.21.1 the project was split: 166 of
those files moved to `L2Core`, and `L2Library` kept only `content/` and
`events/`. So `L2Library-1.20` is the 1.20.1 stand-in for L2Core.

The mod imports 43 distinct `dev.xkmc.l2core` classes. 17 exist under the old
`dev.xkmc.l2library` package. 26 have no 1.20.1 equivalent:

```
base/entity/SyncedData
capability/attachment/GeneralCapabilityTemplate
capability/level/BaseSavedData
compat/jei/BaseRecipeCategory
events/SchedulerHandler
init/reg/datapack/{DataMapReg, DatapackReg}
init/reg/registrate/{L2Registrate, LegacyHolder, SimpleEntry}      (+ L2Registrate.ParticleSupplier)
init/reg/simple/{AttReg, AttVal, CdcReg, CdcVal, DCReg, DCVal, EnumCodec, Reg, SR, Val}
serial/loot/{AddItemModifier, LootHelper}
util/{ConfigInit, DCStack}
```

That list is the non-enchantment part of L2Core's `init/reg` framework plus the
new capability, config-value and loot helpers. `L2Core.init.reg.ench` is out of
scope, with one exception: `init/reg/ench/DataGenHolder` is not enchantment
logic — it is a plain `(ResourceKey, value)` holder used by `QuestDialogData`
and `StructBuilding` datagen. It is reimplemented in the mod compat layer.

Only a few of the 26 are genuine library classes. Under O6 the rest go into a
mod-side compat package, so the L2Library jar stays a plain rebuild:

- library: none required by default
- mod compat: `SyncedData`, `BaseSavedData`, `serial/loot`, `SchedulerHandler`,
  `ConfigInit`, `DCStack`, `DataGenHolder`, `DatapackReg`, and the registration
  wrappers (`Reg`, `SR`, `Val`, `AttReg`, `AttVal`, `CdcReg`, `CdcVal`, `DCReg`,
  `DCVal`, `EnumCodec`, `SimpleEntry`, `LegacyHolder`)

`content/raytrace` and `content/explosion` are not missing: the 1.20.1 branch
has them at `util/raytrace` and `init/explosion`, so those imports move.

Package naming is D21: any class that does end up in the library folds into
`dev.xkmc.l2library`, reusing the legacy package where one exists.

## L2Serial

1.20.1 (`1.20.1`, 1.2.6) matches its jar exactly. The 1.21 line adds:

```
network/PacketHandler
serialization/codec/{CodecAdaptor, MapCodecAdaptor}
serialization/generic_types/{CodecReg, HolderCodec, HolderReg, HolderSetCodec}
serialization/marker/{OnInject, SerialClass, SerialField}
serialization/unified_processor/RealCodecContext
util/ModContainerHack
```

and removes `network/BasePacketHandler` and `custom_handler/StackHelper`.

GensokyoLegacy needs `PacketHandler`, `CodecAdaptor`, and the three marker
classes. The markers moved from `serialization.SerialClass` to
`serialization.marker.SerialClass`, so existing imports change.

## L2ModularBlock

Most of the gap is a package relocation, not new behaviour. The 3.x line moves
`DelegateBlock`, `DelegateBlockImpl`, `DelegateEntityBlockImpl`,
`BlockImplementor`, `BlockTemplates` and `VoxelBuilder` under `core/`, adds
`impl/SimpleWaterloggedImpl` and `mult/{StepOn, ToolModify, ChangeStateExtra,
UseItemOn}BlockMethod`, and drops `tile_api/NameSetable`. The 1.20.1 branch has
`DelegateBlock` and `DelegateBlockImpl` at the package root.

Missing for GensokyoLegacy, added at the **package root** (D23, no `core/`
move):

```
BlockTemplates, VoxelBuilder, DelegateEntityBlockImpl
mult/{PathFindBlockMethod, PlayerDestoryBlockMethod, UseItemOnBlockMethod, UseWithoutItemBlockMethod}
impl/DoubleBlockImpl
```

`DoubleBlockImpl` does not exist on `1.20`, `1.20.4` or `26.1` — decision O2.

## L2ItemSelector

The 1.20.1 `main` (0.2.1) matches the ModularGolems jar; the copy in
`L2Library-1.20/libs` is the older 0.1.9. The 3.x line adds an entire `overlay/`
package:

```
overlay/{InfoSideBar, ItemSelSideBar, L2TooltipRenderUtil, OverlayUtil, SelectionSideBar, SideBar, TextBox}
events/L2ISEventHandler
select/item/CircularSelector
```

The five overlay classes the mod uses already exist in l2library 1.20.1 under
`base/overlay`; under D22 the mod imports move there and no `overlay/` package
is added to l2itemselector.

## L2DamageTracker

**Target the legacy 1.20.1 API; do not backport the 1.21.1 refactor.** The 3.x
line replaces `AttackCache`/`Stage`/`WrappedAttribute` with the `DamageData`
interface hierarchy, the `contents/immunity` and `contents/logging` packages and
new mixins. That refactor is out of scope.

The 1.20.1 `main` is at 0.4.4, but both `libs/` folders ship 0.3.7, which has a
different class set: the jar has `contents/attack/{Additive, Multiplicative,
Nonlinear}` and the source has `compat/*` instead. The jar is stale and should be
rebuilt from `main`.

GensokyoLegacy uses `DamageData.Attack` / `.Defence` / `.DefenceMax` in
`GLAttackListener` and the talisman callbacks. Those call sites move to the
legacy `AttackListener` signatures (`onAttack(AttackCache, ItemStack)` and
friends). See [`discrepancies.md`](discrepancies.md) §7.1 for the mapping.

## L2ScreenTracker → L2MenuStacker

The 1.20.1 `main` (0.1.4) uses `dev.xkmc.l2screentracker` and matches its jar.
The 3.x line renamed the package to `dev.xkmc.l2menustacker` and restructured it:
it adds `init/{L2MenuStacker, L2MSClient, L2MSConfig, L2MSLangData, MouseCache}`,
`screen/packets/CacheMouseToClient`, `screen/base/{L2MSReg, MenuCache,
MenuTriggerType}`, `screen/track/InventoryTrace`, `click/SlotClickToServer`, and
mixes into `Minecraft` and `ServerPlayer`. It drops the Curios compat
(`L2CuriosCompat`, `Curio*Trace`) and `NetworkHooksMixin`.

Under O5 the mod keeps the legacy `L2ScreenTracker` name. Only
`screen/packets/CacheMouseToClient` and `MouseCache` are backported; the other
20 imports resolve against the old package. Restoring the cursor position also
needs `mixin/MinecraftMixin` (wraps `MouseHandler.releaseMouse` in
`Minecraft.setScreen`) and an AT for `MouseHandler.xpos` / `ypos`, which are
private in 1.20.1 (`f_91507_` / `f_91508_` in the SRG-named AT).

## L2Tabs

**Target the legacy 1.20.1 API; do not backport the 1.21.1 refactor.** The 3.x
line replaces the legacy `TabRegistry.registerTab` with `L2Tabs.GROUP` +
`L2Tabs.TABS`, moves icons and ordering into data maps, and rebases `TabBase`
on `FloatingButton`.

The 1.20.1 repo has two API generations: `main` is the experimental rework
adopted in 1.21.1, `entity` is the legacy code used for 1.20.1.

`main` (rework) has, `entity` does not:

```
tabs/contents/BaseAttributeScreen
tabs/core/{ITabScreen, TabBase, TabGroup, TabGroupData}
tabs/inventory/{InvTabData, ScreenWrapper, TabRegistry}
```

`entity` (legacy) has, `main` does not:

```
compat/CurioSlot
tabs/core/{BaseTab, FloatingButton, TabRegistry}
```

GensokyoLegacy currently imports `tabs.core.TabBase` and
`tabs.inventory.InvTabData` (the rework API); those call sites move to the
legacy `TabRegistry.registerTab(priority, factory, icon, title)` form
(D19). Build L2Tabs from `entity`. See [`discrepancies.md`](discrepancies.md)
§7.2.

## L2Harvester

Dropped. GensokyoLegacy never imports it; the `build.gradle` entry was a
runtime-only leftover.

## DanmakuAPI and FastProjectileAPI

Neither has a 1.20.1 branch, tag or jar. Both `main` branches are 1.21.1
NeoForge. GensokyoLegacy uses 42 danmaku classes and 12 fast-projectile classes,
including entities, renderers, movers, spell cards, damage types and datagen.
This is a port, not a delta backport.

The 1.20.1 reference is `Youkai-Homecoming-1.20` `main`:

- FastProjectileAPI is **vendored** there under
  `src/main/java/dev/xkmc/fastprojectileapi` (46 files, including the collision
  map, `SimplifiedProjectile`, the render core and the spell circle). It can be
  lifted into the `FastProjectileAPI-1.20` repo, moving the six render classes
  from `render/core` to `render` to match the 3.x package names.
- DanmakuAPI is not vendored; the equivalent is in-mod under
  `content/entity/danmaku`, `content/spell`, `content/particle`,
  `content/item/danmaku` and `init/registrate/YHDanmaku`. That code is the port
  source for the 121-file library.

The reference `main` is a food mod that also carries the danmaku implementation;
it is not expected to be binary-compatible with the backported Gensokyo Legacy.
The `1.20/lite` branch is the pure-food variant.

## Work implied

Rebuild from source (jar behind source):

- `L2Tabs` (legacy API, from `entity`)
- `L2ModularBlock`
- `L2DamageTracker` (legacy API, from `main`)
- `L2Library`
- `L2Serial` (if the 1.2.6 jar is behind `main`)

Backport new contents only:

- `L2Library` ← L2Core: the missing classes, mapped into the legacy packages
  (`SyncedData`, `BaseSavedData`, `serial/loot`, `SchedulerHandler`,
  `ConfigInit`, `DCStack`, `init/reg/*`, `DataGenHolder`)
- `L2Serial`: `CodecAdaptor`, `Handlers.registerReg`, `TagCodec` predicate
  helpers — fixes only, no interface packet model
- `L2ModularBlock`: `BlockTemplates`, `VoxelBuilder`, waterlogged variants, the
  new `mult` interfaces, `DoubleBlockImpl` — all at the package root
- `L2ScreenTracker`: `CacheMouseToClient` / `MouseCache` only (O5)

Reference-based port (from `Youkai-Homecoming-1.20` `main`):

- `FastProjectileAPI` (vendored, 46 files)
- `DanmakuAPI` (in-mod danmaku/spell code)

Mod-side adaptation (legacy targets):

- 127 `l2core.*` imports → `l2library.*` (D21 mapping)
- 44 packets: records → classes extending legacy `SerialPacketBase`
- 6 `IItemSelector` listeners: drop the `ItemStack` parameters
- `L2DamageTracker`: `DamageData` call sites → `AttackCache` + `ItemStack`
- `L2Tabs`: `L2Tabs.GROUP/TABS/ICON/ORDER` → `TabRegistry.registerTab(...)`
- 53 `l2modularblock.core.*` imports → root package

Dropped:

- `L2Harvester`
