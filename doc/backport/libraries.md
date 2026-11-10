# L2 and danmaku libraries

The 1.20.1 branch is pinned to the library versions below. The 1.20.1 branches
are one-way ports: a later 1.21.1 library change has to be ported down by hand,
and several 1.21 refactors are deliberately **not** ported.

## Dependencies

`libs/` is a `flatDir` repository; every jar is `fg.deobf`'d. L2Library and the
jars it ships are external dependencies (`implementation`), not jar-in-jar'd.
Only libraries the full L2Library does not carry are `jarJar`'d.

| coordinate | scope | note |
|---|---|---|
| `dev.kxmc.l2library:l2library:2.5.7-slim` | `implementation` | slim build; full jar is for players |
| `com.tterrag.registrate:Registrate:MC1.20-1.3.11` | `implementation` | shipped by L2Library |
| `io.github.llamalad7:mixinextras-forge:0.2.0-beta.8` | `implementation` | shipped |
| `dev.xkmc.l2serial:l2serial:1.2.8` | `implementation` | shipped |
| `dev.xkmc.l2tabs:l2tabs:0.4.0` | `implementation` | shipped |
| `dev.xkmc.l2screentracker:l2screentracker:0.1.6` | `implementation` | shipped |
| `dev.xkmc.l2modularblock:l2modularblock:1.2.1` | `implementation` | shipped |
| `dev.xkmc.l2itemselector:l2itemselector:0.2.1` | `implementation` | shipped |
| `dev.xkmc.l2damagetracker:l2damagetracker:0.4.4` | `implementation` + `jarJar` | not shipped |
| `dev.xkmc.fastprojectileapi:fast_projectile_api:3.1.2+1.20.1` | `implementation` + `jarJar` | not shipped |
| `dev.xkmc.danmakuapi:danmaku_api:3.0.6+1.20.1` | `implementation` + `jarJar` | not shipped |
| `com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.10` | `implementation` + `jarJar` | not shipped |
| `software.bernie.geckolib:geckolib-forge-1.20.1:4.4.9` | `implementation` | external mod |
| `dev.xkmc.l2harvester:l2harvester:0.1.2` | `implementation` | external mod, `youkaisfeasts` requires it |

The `dev.kxmc` group on `l2library` is the real coordinate, typo included.
`mavenRange` in `build.gradle` strips the `+mc` / `+1.20.1` suffix before
building a jarJar range, because Maven ranges reject it.

### Compat set

Curios 5.10, JEI 15.20, Patchouli 1.20.1-84, farmersdelight / display-delight,
youkaisfeasts 0.5.2. `twilightdelight`, `lendercataclysm`, `lionfish-api` and
`twilightforest` were dropped because no 1.21.1 code referenced them.

Touhou Little Maid was removed from the backport (`remove TLM`): the compat
package and the hooks in `GeneralYoukaiEntity` / `GeneralYoukaiRenderer` are
gone, and `TouhouSpellCards` now lives in `content/spell/card`. A later 1.21.1
change that touches TLM must be ported as ordinary code or dropped again.

## Repos and final versions

| repo | branch | version | output |
|---|---|---|---|
| `L2Serial-1.20` | `1.20.1` | 1.2.8 | `l2serial-1.2.8.jar` |
| `L2Library-1.20` | `1.20` | 2.5.7 | slim + full + sources |
| `L2ModularBlock-1.20` | `1.20` | 1.2.1 | `l2modularblock-1.2.1.jar` |
| `L2Tabs-1.20` | `entity` | 0.4.0 | `l2tabs-0.4.0.jar` |
| `L2ScreenTracker-1.20` | `main` | 0.1.6 | `l2screentracker-0.1.6.jar` |
| `L2ItemSelector-1.20` | `main` | 0.2.1 | `l2itemselector-0.2.1.jar` |
| `L2DamageTracker-1.20` | `main` | 0.4.4 | `l2damagetracker-0.4.4.jar` |
| `FastProjectileAPI-1.20` | `1.20.1` | 3.1.2+1.20.1 | `fast_projectile_api-*.jar` |
| `DanmakuAPI-1.20` | `1.20` | 3.0.6+1.20.1 | `danmaku_api-*.jar` |
| `L2ModularBlock-1.20` | `1.20.4` (same repo) | 3.0.0+8 | 1.21.1 source restored; port source |

`L2Tabs` `main` is the experimental 1.21 tab rework; **do not build the 1.20.1
backport from it**. The legacy 1.20.1 code is the `entity` branch.

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

The six compile against `l2library-*-slim` to break the
`L2Library ↔ L2Tabs/L2ScreenTracker/…` cycle. Build the slim jar first, then
the six, then the full jar. DanmakuAPI needs FastProjectileAPI plus the L2 jars.

When a jar is rebuilt: copy the new jar **and** its sources jar into `libs/`,
bump `gradle.properties`, and rerun `stripjarjar.py` if the jar came from
`youkaisfeasts`.

## Per-library porting rules

### L2Serial

- Keep `BasePacketHandler`, `SerialPacketBase` and `SimplePacketBase` as
  abstract classes, and `SimpleChannel` / `NetworkEvent.Context` as transport.
- **Do not port** the 3.x `PacketHandler` / `IPayloadContext` / `StreamCodec`
  model.
- Additions carried by 1.2.8: `RecordPacketBase` (record packets),
  `CodecAdaptor`, the marker annotations folded into `SerialClass`
  (`serialization.marker.SerialClass` → `serialization.SerialClass`), and the
  thread-safety fixes (`ArrayCodec` reallocation, concurrent `Handlers` maps,
  synchronized `GenericCodec` registration).

### L2Library

- `L2Library-1.20` is the 2.x monolith and stands in for 1.21's `L2Core`.
  The mod imports the legacy paths (`base.*`, `capability.*`, `serial.*`,
  `util.*`, `init.*`), never `dev.xkmc.l2core`.
- The 26 missing L2Core classes are **not** in the jar. They live in
  `compat/l2lib`, so the library stays a plain rebuild.
- `serial.config` (`BaseConfig`, `ConfigTypeEntry`, `ConfigCollect`,
  `ConfigDataProvider`, `PacketHandlerWithConfig`) is present and is the data
  map backing, §7 in [`api.md`](api.md).
- The L2Core enchantment system is out of scope except the generic
  `DataGenHolder`.

### L2ModularBlock

- 3.x contents were ported to the **legacy package root**: `BlockTemplates`,
  `VoxelBuilder`, `DelegateEntityBlockImpl`,
  `DelegateBlockSimpleWaterImpl`, `DelegateEntityBlockSimpleWaterImpl`,
  `impl/SimpleWaterloggedImpl`, `type/WaterloggedMethodMarker`,
  `impl/DoubleBlockImpl`, and the new `mult/*` interfaces (`StepOn`,
  `ToolModify`, `ChangeStateExtra`, `UseItemOn`, `UseWithoutItem`, `PathFind`,
  `PlayerDestory`).
- Do not move the delegate classes into `core/`. Keep `OnClickBlockMethod`
  and the 1.20.1 single-`use(...)` dispatch.
- 1.20.1 adaptations: `ItemInteractionResult` → `InteractionResult`,
  `ItemAbility` → `ToolAction`, 4-arg `isPathfindable`, `void
  playerWillDestroy`.

### L2Tabs

- Legacy API only: `TabRegistry.registerTab(priority, factory, icon, title)`,
  `BaseTab extends FloatingButton`, `TabManager(screen)`.
- Do not port `L2Tabs.GROUP` / `TABS` / `ICON` / `ORDER`, `TabBase` or data-map
  driven icons/order.

### L2DamageTracker

- Legacy API only: `AttackCache`, `AttackListener`, `CreateSourceEvent`,
  `DamageModifier`, `DefaultDamageState`.
- Do not port the 3.x `DamageData` hierarchy (`Attack`, `Offence`,
  `Defence`, `DefenceMax`) or the `contents/immunity` / `contents/logging`
  packages.

### L2ItemSelector

- Legacy per-player signatures; see [`api.md`](api.md) §10.
- Do not add `l2itemselector.overlay`; those classes live in
  `l2library.base.overlay`.

### L2ScreenTracker

- Legacy names: `dev.xkmc.l2screentracker`, `L2ScreenTracker`,
  `L2ScreenTrackerConfig`, `ScreenTracker.register()`.
- Backported from 3.x: `init/MouseCache`,
  `screen/packets/CacheMouseToClient` and `mixin/MinecraftMixin` (wraps
  `MouseHandler.releaseMouse` in `Minecraft.setScreen`). The mixin needs an AT
  for `MouseHandler.xpos` / `ypos` (`f_91507_` / `f_91508_`).
- Do not port the `l2menustacker` rename, `L2MSReg`, `L2MSConfig`,
  `SlotClickToServer` or `RegistryFriendlyByteBuf`.

### FastProjectileAPI and DanmakuAPI

- Both are full ports of the 1.21.1 source, with the 1.21 package layout kept
  (`entity/`, `collision/`, `render/`, `spellcircle/`, `mixin/`;
  DanmakuAPI `api/` rebuilt on `IYHDanmaku` / `YHBaseDanmakuEntity`).
- API differences the mod must follow are in [`api.md`](api.md) §16.
- DanmakuAPI has no mixin config.

### L2Harvester

Never imported by GensokyoLegacy. It stays an external dependency because
`youkaisfeasts` declares it mandatory and the slim feast build does not ship
it. Do not jar-in-jar it.

## Library build environment

- The 1.20.1 library repos use Gradle 7.3. On JDK 21 a changed `build.gradle`
  fails with `Unsupported class file major version 65`; run them on JDK 17.
- ForgeGradle 5.1.77 fails on the TLS certificate of
  `maven.minecraftforge.net` on this machine. Pass
  `-Dnet.minecraftforge.gradle.check.certs=false` if a dependency download
  fails; the downloads themselves work.
- Clean builds that declare an AT can race two configurations resolving the
  generated AT variant (FART `imtd is null`, missing `..._at_...` variant).
  Workaround: `rm -rf build`, resolve one configuration
  (`./gradlew dependencies --configuration compileClasspath`), then
  `./gradlew build --offline`.
