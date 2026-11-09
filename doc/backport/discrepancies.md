# Significant API discrepancies

Four different targets, depending on the library:

- **Adopt the legacy 1.20.1 API and class location** — L2Library/L2Core,
  L2Serial, L2ItemSelector. The mod is adapted to the 1.20.1 surface; the 3.x
  classes are only backported where the mod needs a class that does not exist.
- **Backport new contents at the legacy class location** — L2ModularBlock. The
  new method interfaces and helpers are added, but the six delegate classes stay
  at the package root instead of moving into `core/`.
- **Target the legacy 1.20 API** — L2DamageTracker and L2Tabs. Both were
  refactored in 1.21.1; the mod is adapted to the legacy API instead.
- **Reference-based port** — DanmakuAPI and FastProjectileAPI, using
  `Youkai-Homecoming-1.20` `main` as the 1.20.1 implementation.

L2ScreenTracker is still open (O5): the legacy package exists but lacks
`CacheMouseToClient`.

The comparisons below are against the class list and signatures GensokyoLegacy
actually imports, not the whole library.

## 1. L2Library / L2Core

### 1.1 Missing classes

43 distinct `dev.xkmc.l2core` imports. 17 resolve under `dev.xkmc.l2library`
today; 26 do not:

```
base/entity/SyncedData
capability/attachment/GeneralCapabilityTemplate
capability/level/BaseSavedData
compat/jei/BaseRecipeCategory
events/SchedulerHandler
init/reg/datapack/{DataMapReg, DatapackReg}
init/reg/registrate/{L2Registrate, LegacyHolder, SimpleEntry}
init/reg/simple/{AttReg, AttVal, CdcReg, CdcVal, DCReg, DCVal, EnumCodec, Reg, SR, Val}
serial/loot/{AddItemModifier, LootHelper}
util/{ConfigInit, DCStack}
```

`init/reg/ench/DataGenHolder` is excluded with the enchantment system, but it is
a generic id+value holder used by `QuestDialogData` and `StructBuilding`, so it
must still exist somewhere.

`L2Library` also needs `content/raytrace` (5 imports) and `content/explosion`
(1 import), neither of which exists on the 1.20.1 branch. Under D21 they map to
the legacy `util/raytrace` and `init/explosion`.

The 26 missing classes are not all new: most have a legacy home, so only the
import path changes. Full mapping in [`decisions.md`](decisions.md) D21. The
classes with **no** legacy home are `base/entity/SyncedData`,
`capability/level/BaseSavedData`, `serial/loot/*`, `events/SchedulerHandler`,
`util/ConfigInit`, `util/DCStack`, `init/reg/ench/DataGenHolder` and the
`init/reg/{simple,registrate,datapack}` framework.

### 1.2 Capability lifecycle

| 1.20.1 | 3.x |
|---|---|
| `GeneralCapabilityTemplate` has `getThis()`, `check()` | `getThis()`, `tick(E)` |
| `PlayerCapabilityTemplate` has `player`/`world` fields, `preInject()`, `init()`, `onClone(boolean)`, `check()`, `getThis()`, `tick()` | `init(Player)`, `onClone(Player, boolean)`; `tick(E)` inherited |
| `PlayerCapabilityNetworkHandler.toClientSyncAll` / `toClientSyncClone` | `toClient(ServerPlayer)` |

The mod's six `PlayerCapabilityTemplate` subclasses override `tick(Player)`,
`init(Player)` and `onClone(Player, boolean)` and read `player`/`world` from the
superclass. In the legacy model those become `tick()`, `init()` and
`onClone(boolean)` using the inherited fields.

### 1.3 Block entities

| 1.20.1 | 3.x |
|---|---|
| `load(CompoundTag)` | `loadAdditional(CompoundTag, HolderLookup.Provider)` |
| `saveAdditional(CompoundTag)` | `saveAdditional(CompoundTag, HolderLookup.Provider)` |
| `getUpdateTag()` | `getUpdateTag(HolderLookup.Provider)` |

Six block entities are affected. The provider arguments do not exist in 1.20.1,
so the backport keeps the 1.20 signatures and the mod's `BaseBlockEntity`
subclasses are unchanged.

### 1.4 Menus

| 1.20.1 | 3.x |
|---|---|
| `BaseContainerMenu.sprite` (`SpriteManager`) | `player`, `access` fields, `getLayout()` |
| `SpriteManager(String modid, String path)` | `SpriteManager(ResourceLocation id)` |
| `MenuLayoutConfig extends BaseConfig`; `getTexture()`, `getRenderer(gui)` | record with `parent()`, `getTexture(id)`, `getRenderer(id, gui)` |

`MenuLayoutConfig` is the worked example of the data-map → `BaseConfig` change:
in 1.20.1 it is a serialized config loaded through `ConfigTypeEntry`, in 3.x it
is a data-map value. The mod has three `BaseContainerMenu` subclasses and three
`SpriteManager` uses.

### 1.5 Recipes

| 1.20.1 | 3.x |
|---|---|
| `BaseRecipe<..., Inv extends Container>` | `BaseRecipe<..., Inv extends RecipeInput>` |
| `assemble(Inv, RegistryAccess)` | `assemble(Inv, HolderLookup.Provider)` |
| `RegistryEntry<RecipeType<SRec>> type` | `Supplier<RecipeType<SRec>>` |
| `getId()` on the recipe | removed |
| `BaseRecipeBuilder.save(Consumer<FinishedRecipe>, id)` | `save(RecipeOutput, id)` |
| `CriterionTriggerInstance` | `Criterion<?>` |
| `Result` record | removed |

The JEI categories extend `BaseRecipeCategory`, which is part of the same
backport. The 1.20.1 side keeps `Container` and `RegistryAccess`.

### 1.6 Datagen and advancements

| 1.20.1 | 3.x |
|---|---|
| `FrameType` | `AdvancementType` |
| `CriterionBuilder.modify(String, Builder)` | `onBuild(String, Builder, List<ICondition>)` |
| `item(..., CompoundTag)` | `item(..., DataComponentPredicate)` |
| `AdvancementGenerator.patchouli(..., ResourceLocation book, ...)` | `patchouli(..., PatchouliHelper, ...)` |
| `PatchouliHelper.buildShapedRecipe(Consumer<ShapedPatchouliBuilder>, ...)` | `Consumer<ShapedRecipeBuilder>` |

### 1.7 Misc

- `EffectUtil`: 3.x drops `AddReason` and takes an optional `Entity source`.
  The mod calls `refreshEffect(entity, ins, source)`; legacy needs the extra
  `AddReason` argument.
- `Proxy`: `getWorld()` / `getClientWorld()` → `getLevel()`.
- `BaseRecipeCategory` (JEI) is not new: the legacy class is
  `l2library.serial.recipe.BaseRecipeCategory`, not `compat.jei`.

## 2. L2Serial

### 2.1 Packet model

| 1.20.1 | 3.x |
|---|---|
| `SimplePacketBase` abstract class with `write(FriendlyByteBuf)` + `handle(Supplier<Context>)` | interface with `handle(IPayloadContext)` |
| `SerialPacketBase` abstract class with `handle(NetworkEvent.Context)` | interface `SerialPacketBase<T extends Record & SerialPacketBase<T>>` with `handle(Player)` |
| packets extend the base class | packets are records implementing the interface |

44 mod packets are `record ... implements SerialPacketBase<T>` with
`handle(Player)`. That form is 3.x-only. Under D20 the mod is adapted to the
legacy model: each packet becomes a class extending `SerialPacketBase` and
implementing `handle(NetworkEvent.Context)` (records cannot extend a class).
The record components become fields, and `PacketCodec` supplies
`write`/`decode`.

### 2.2 Transport

3.x `PacketHandler` is built on NeoForge's payload system:

```
CustomPacketPayload / PayloadRegistrar / IPayloadContext / StreamCodec
```

Forge 1.20.1 has none of these. The legacy stack is `BasePacketHandler` +
`SimpleChannel` + `NetworkEvent.Context` + `FriendlyByteBuf`. The mod's
`PacketHandler` import becomes `l2library.serial.config.PacketHandler` (which
extends `BasePacketHandler` and registers itself for `PacketHandler.setup`),
`NetDir` becomes `NetworkDirection`, and the `create(Class, NetDir)` factory
becomes `create(Class, Function<FriendlyByteBuf, T>, NetworkDirection)`.
`toServer`, `toTrackingPlayers`, `toClientPlayer`, `toAllClient` and
`sendToNear` keep their names.

`GLPackets` also declares `StreamCodec<RegistryFriendlyByteBuf, T>` overloads.
`RegistryFriendlyByteBuf` and `StreamCodec` are 1.20.5+, so those two overloads
are dropped (the mod has zero 3-argument call sites).

Because `ConfigTypeEntry` needs a `PacketHandlerWithConfig`, the mod's main
handler should be one, so packets and `BaseConfig` share the channel.

### 2.3 CodecAdaptor and codec helpers

`CodecAdaptor<T>` is new in 3.x and implements DFU `Codec<T>`. The mod uses it
once (`GLMeta`, for a data map that D12 removes). Under D20 it is not part of the
legacy API; if it is still wanted for `DatapackReg`, backport it as a utility
over `UnifiedCodec`/`TagCodec` with `FriendlyByteBuf` functions.

The mod also uses 1.20.5+ buffer helpers that have no 1.20.1 form:
`ByteBufCodecs.fromCodecWithRegistries` (5 sites in `GLMeta`) and
`FluidIngredient.STREAM_CODEC`. These become explicit `FriendlyByteBuf`
encode/decode functions.

### 2.4 Marker annotations

`SerialClass`, `SerialField` and `OnInject` moved from
`serialization.*` to `serialization.marker.*`. The mod already imports the
`marker` package, so only the library side moves.

### 2.5 Handlers, CodecHandler, TagCodec

| 1.20.1 | 3.x |
|---|---|
| `Handlers.MAP` | `Handlers.CODEC_MAP`, `NULL_DEFER` |
| `enableVanilla(Class, Supplier<IForgeRegistry<T>>)` | `enableVanilla(Class, Registry<T>)`, `registerReg`, `getReg` |
| `CodecHandler<T> extends ClassHandler<Tag, T>` | plain `CodecHandler<T>` |
| `CodecHandler(Class, Codec, Function<FriendlyByteBuf,T>, BiConsumer<FriendlyByteBuf,T>)` | `CodecHandler(Class, Codec, StreamCodec)` |
| `TagCodec.toTag(...)`, predicate arguments on every call | `pred(Predicate<SerialField>)`, `fromTag` without the predicate |

`GLMeta.register()` is the only call site: `enableVanilla(Fluid.class,
BuiltInRegistries.FLUID)` needs `() -> ForgeRegistries.FLUIDS`, and the five
`CodecHandler` constructions need explicit `FriendlyByteBuf` functions. The
`Handlers.registerReg(Dialog.class, …)` call is 3.x-only (D11).

## 3. L2ModularBlock

D23: backport the 3.x contents, but keep the legacy class location. The six
delegate classes stay at the package root; the mod's `l2modularblock.core.*`
imports move to `l2modularblock.*` (53 import lines).

### 3.1 New classes at the root

`BlockTemplates`, `VoxelBuilder` and `DelegateEntityBlockImpl` are new. The
1.20.1 branch already has `DelegateBlock`, `DelegateBlockImpl` and
`BlockImplementor` at the root. `BlockTemplates` (16 mod imports) and
`VoxelBuilder` (14) are the bulk of the block code.

### 3.2 Waterlogging

`DelegateBlock.newBaseBlock` in 3.x inspects the method list for
`WaterloggedMethodMarker` and returns `DelegateBlockSimpleWaterImpl` or
`DelegateEntityBlockSimpleWaterImpl`. 1.20.1 always returns the non-waterlogged
implementation. Backport both variants and the marker.

### 3.3 Block entity interaction

| 1.20.1 | 3.x |
|---|---|
| `BlockEntityBlockMethodImpl.onClick(state, level, pos, player, hand, hit)` | `useWithoutItem(state, level, pos, player, hit)` |
| implements `OnClickBlockMethod` | implements `UseWithoutItemBlockMethod`, `SetPlacedByBlockMethod`, `AnalogOutputBlockMethod` |

Eight mod files use `BlockEntityBlockMethodImpl`. The 3.x signature follows the
vanilla 1.21 `useItemOn` / `useWithoutItem` split, which 1.20.1 does not have.
Keep the legacy `OnClickBlockMethod` and add `useWithoutItem` as a separate
interface dispatched from the single 1.20.1 `use(...)`.

### 3.4 New method interfaces

`StepOnBlockMethod`, `ToolModifyBlockMethod`, `ChangeStateExtraBlockMethod`,
`UseItemOnBlockMethod`, `UseWithoutItemBlockMethod`, `PathFindBlockMethod` and
`PlayerDestoryBlockMethod` are 3.x-only. The mod implements the last four
heavily (10 and 7 `UseItemOn`/`UseWithoutItem` imports). Backport them with the
3.x method signatures and dispatch from the 1.20.1 `use`/pathfind hooks.

### 3.5 Source/jar gap

The 1.20.1 source has `mult/SurviveBlockMethod`, which the 1.1.0 jar lacks.
`impl/DoubleBlockImpl` is imported by the mod but exists on no upstream branch
(O2).

## 4. L2ItemSelector

D22: adopt the legacy API. The 3.x per-stack signatures and `Holder` are not
added; the six mod listeners are rewritten to the legacy forms.

### 4.1 Selection API

| 1.20.1 | 3.x |
|---|---|
| `getSelection(Player)` returns `IItemSelector` | returns `IItemSelector.Holder(ItemStack, IItemSelector)` |
| `getIndex(Player)` | `getIndex(Player, ItemStack)` |
| `getList()` | `getList(ItemStack)` |
| `move(int, Player)` | `move(int, Player, ItemStack)` |
| `swap(Player, int)` | `swap(Player, int, ItemStack)` |
| `getDisplayList()` | `getDisplayList(ItemStack)` |
| — | `getSelHash(ItemStack)` |

`SelectorSelectPacket` already calls the legacy `swap(Player, int)`, so only the
six `WheelAdaptor.Provider` listeners change.

### 4.2 Overlay package

The 3.x `l2itemselector.overlay/{InfoSideBar, ItemSelSideBar,
L2TooltipRenderUtil, OverlayUtil, SelectionSideBar, SideBar, TextBox}` already
exist in l2library 1.20.1 under `base/overlay`. Under D22 the mod's five
`l2itemselector.overlay.*` imports move to `l2library.base.overlay.*`; no new
classes are needed. `events/GeneralEventHandler` stays the legacy handler.

## 5. L2ScreenTracker

O5 resolved: **use the legacy names.** The 3.x comparison below is only the
source for the two classes that are backported.

### 5.1 Rename

The 3.x package is `dev.xkmc.l2menustacker`; 1.20.1 is
`dev.xkmc.l2screentracker`. The 22 mod imports move back to the legacy name.

### 5.2 Init and registration

| 1.20.1 | 3.x |
|---|---|
| `L2ScreenTracker` | `L2MenuStacker` |
| `PacketHandler(new ResourceLocation(MODID, "main"), 1, ...)` | `PacketHandler(MODID, 1, ...)` |
| `ScreenTracker.register()`, `ScreenTrackerRegistry.register()` | `L2MSReg.register()` |
| `L2CuriosCompat.onStartup()` / `commonSetup()` | removed |
| `L2ScreenTrackerConfig` | `L2MSConfig` |
| — | `MouseCache`, `L2MSLangData` |
| — | `Reg REG` |

### 5.3 Packets

`CacheMouseToClient` is 3.x-only and is used in four mod files
(`FirstDialogSession`, `TalismanPocket`, `DollItem`, `Hakkero`). Under O5 it is
backported into the legacy library together with `MouseCache`, so the call
becomes `L2ScreenTracker.PACKET_HANDLER.toClientPlayer(new
CacheMouseToClient(), sp)`. `SlotClickToServer` is not needed.

### 5.4 Buffer type

`PlayerSlot.read` / `write` take `RegistryFriendlyByteBuf` in 3.x and
`FriendlyByteBuf` in 1.20.1. Twelve mod files use `PlayerSlot`; only the
serializer signature changes.

### 5.5 Curios

3.x drops `L2CuriosCompat`, `CurioInvTrace`, `CurioSource`, `CurioTabTrace` and
`NetworkHooksMixin`. The 1.20.1 branch keeps them, which is fine — the mod does
not depend on that compat.

## 6. DanmakuAPI and FastProjectileAPI

### 6.1 FastProjectileAPI already exists for 1.20.1

`Youkai-Homecoming-1.20` `main` vendors a complete 1.20.1 FastProjectileAPI
under `src/main/java/dev/xkmc/fastprojectileapi` (46 files), including the
collision map, `SimplifiedProjectile`, `BaseLaser`, `ProjectileMovement`, the
render core, the virtual danmaku renderer and the spell circle. It is not
declared in the reference build; it compiles as part of that mod.

### 6.2 Package layout differs

| 1.21 FastProjectileAPI | 1.20.1 reference |
|---|---|
| `render/ProjectileRenderHelper` | `render/core/ProjectileRenderHelper` |
| `render/ProjectileRenderer` | `render/core/ProjectileRenderer` |
| `render/ProjTypeHolder` | `render/core/ProjTypeHolder` |
| `render/RenderableProjectileType` | `render/type/RenderableProjectileType` |
| (in DanmakuAPI `content/render`) | `render/type/*ProjectileType`, `render/core/DisplayType` |
| (in DanmakuAPI `content/virtual`) | `render/virtual/{ClientDanmakuCache, DanmakuClientHandler, DanmakuManager, DanmakuToClientPacket, EraseDanmakuToClient}` |
| — | `collision/{FastMap, FastMapInit, LaserHitHelper, ProjectileHitHelper, SectionCache}` |

The backport can either keep the 1.21 package names and move the reference
files, or adopt the reference layout and rewrite the mod's imports. Moving the
files is smaller.

### 6.3 DanmakuAPI is not vendored

The reference has no `dev.xkmc.danmakuapi` package. The equivalent code is
in-mod:

```
content/entity/danmaku/   YHBaseDanmakuEntity, ItemDanmakuEntity, ItemLaserEntity,
                          YHBaseLaserEntity, IYHDanmaku, DanmakuHelper, renderers
content/spell/mover/      the same movers as DanmakuAPI
content/spell/spellcard/  the same spell-card classes
content/spell/item/       ItemSpell, PlayerHolder, SpellContainer
content/item/danmaku/     DanmakuItem, LaserItem, SpellItem, CustomSpellItem
content/particle/         DanmakuPoofParticle, options, helper
init/registrate/YHDanmaku the bullet enum and item registration
```

The 3.x `api/` package has no direct counterpart; `IYHDanmaku` and
`YHBaseDanmakuEntity` cover `IDanmakuEntity` / `IYoukaiEntity` / `DanmakuBullet`.

### 6.4 Class mapping for the mod's 42 imports

| 3.x DanmakuAPI | 1.20.1 reference |
|---|---|
| `content.entity.DanmakuHelper` | `content.entity.danmaku.DanmakuHelper` |
| `content.entity.ItemBulletEntity` | `content.entity.danmaku.ItemDanmakuEntity` |
| `content.entity.ItemBulletRenderer` | `content.entity.danmaku.ItemDanmakuRenderer` |
| `content.entity.ItemLaserEntity` | `content.entity.danmaku.ItemLaserEntity` |
| `content.item.{DanmakuItem, LaserItem, SpellItem}` | `content.item.danmaku.*` |
| `content.spell.{mover, spellcard, item}.*` | `content.spell.*` |
| `content.render.*` | split between `fastprojectileapi.render.type` and `content.entity.danmaku` |
| `api.*` | `IYHDanmaku`, `YHBaseDanmakuEntity`, `DanmakuCommander` |
| `init.registrate.Danmaku{Entities, Items}` | `YHEntities`, `YHDanmaku` |

### 6.5 Forge vs NeoForge

The reference is Forge 1.20.1, so the render and registration code is already in
the target API. The work is repackaging and replacing the reference's mod-local
types (`YHDanmaku.Bullet`, `YHEntities`) with the library's own registration.

### 6.6 FastProjectileAPI port result

Branch `1.20.1` is the 1.21.1 `main` (3.1.2) ported down, not the reference
layout. The 1.21.1 package structure is kept (`entity/`, `collision/`,
`render/`, `spellcircle/`, `mixin/`); the reference implementation is used only
where the platform differs. Artifact: `fast_projectile_api-3.1.2+1.20.1.jar`.

Platform substitutions:

- `VertexConsumer.addVertex/setUv/setColor` → `vertex/uv/color/endVertex`.
- `Entity.setPosRaw` is final; the AT makes it `public-f` using the SRG name
  `m_20343_(DDD)V`. AT entries use SRG names throughout (`f_19825_`,
  `f_19826_`).
- `IEntityWithComplexSpawn` → `IEntityAdditionalSpawnData`.
- NeoForge event API → Forge: `@Cancelable` instead of `ICancellableEvent`, and
  `IEventBus.post` returns `boolean` (post the event, then read it).
- `attachCapability` / `registerCapability` remain the Forge capability API.

### 6.7 DanmakuAPI port result

Branch `1.20` is 3.0.6 ported down with the 1.21.1 package layout intact, on
top of the rebuilt FastProjectileAPI and L2 jars. It has no mixin config.
Artifact: `danmaku_api-3.0.6+1.20.1.jar`.

API-affecting substitutions (the mod's danmaku imports must follow these):

- `DanmakuHurtEvent.getResult()/setResult()` → `getAnswer()/setAnswer()`.
  1.20.1 `Event` already declares `getResult()` returning `Event.Result`, so the
  old name cannot be overridden.
- `DanmakuUseEvent` uses `@Cancelable`; `isCanceled()`/`setCanceled()` replace
  the 1.21 boolean result. `consume()`/`setConsume()` keep their semantics.
- `IEventBus.post` returns `boolean`; every call site stores the event, posts,
  then reads the event.
- `IEntityWithComplexSpawn` → `IEntityAdditionalSpawnData`.
- Registrate `ItemProviderEntry<Item, T>` → `ItemProviderEntry<T>` (one type
  parameter in MC1.20-1.3.11). `RegistryEntry` is likewise one parameter.
- `DeltaTracker` → a plain `float partialTick`; `ClientObjectCache.renderAll`
  already takes the float.
- `renderBackground(GuiGraphics)` only; `EditorScreen` is rewritten against the
  1.20 `OptionsSubScreen` / `OptionsList(Minecraft, width, height, y0, y1,
  itemHeight)` and positions its footer buttons manually.
- `EventBusSubscriber.Bus.GAME` → `Bus.FORGE`.
- `ProjectileWeaponItem.shootProjectile` does not exist on 1.20.1; the override
  is removed.
- Config is `ForgeConfigSpec` + `ModLoadingContext.registerConfig`, loaded as
  `CLIENT` and `COMMON` (the same shape as `YHModConfig`). There is no
  `registerSynced`.
- The custom-spell data component is gone: the form is written as the NBT
  compound `SpellData` on the stack (`TagCodec.valueToTag` /
  `valueFromTag`).
- `ParticleType` takes `(boolean, Deserializer)` and `codec()` returns
  `Codec<T>`.
- `IrisCompat` reads `ShadowRenderer.ACTIVE` reflectively so Iris is never
  linked when absent.
- `DanmakuManager` packet factories no longer take `registryAccess()`.


## 7. Legacy-targeted libraries

### 7.1 L2DamageTracker

The 3.x `DamageData` interface hierarchy does not exist in 1.20.1:

```
DamageData.Attack / Offence / OffenceMax / Defence / DefenceMax
```

The legacy listener takes the cache and the weapon directly:

| 3.x | 1.20.1 |
|---|---|
| `onAttack(DamageData.Attack)` | `onAttack(AttackCache, ItemStack)` |
| `onDamage(DamageData.Defence)` | `onDamage(AttackCache, ItemStack)` |
| `onDamageFinalized(DamageData.DefenceMax)` | `onDamageFinalized(AttackCache, ItemStack)` |
| `data.getTarget()` | `cache.getAttackTarget()` |
| `data.getAttacker()` | `cache.getAttacker()` |
| `data.getSource()` | `cache.getLivingAttackEvent().getSource()` |
| `data.getWeapon()` | `cache.getWeapon()` |
| `data.getStrength()` | `cache.getStrength()` |
| `data.getDamageOriginal()` | `cache.getPreDamageOriginal()` |
| `data.addDealtModifier(...)` | `cache.addDealtModifier(...)` |
| `data.getDamageFinal()` | `cache.getDamageDealt()` |

`GLAttackListener` and the talisman callbacks are the only affected sites. Do
not backport the 3.x refactor.

### 7.2 L2Tabs

The 3.x registration surface does not exist in 1.20.1:

| 3.x | 1.20.1 |
|---|---|
| `L2Tabs.loc(id)` | `new ResourceLocation(L2Tabs.MODID, id)` |
| `L2Tabs.TABS` (`RegistryInstance<TabToken>`) | no registry; tokens are plain objects |
| `L2Tabs.GROUP.registerTab(Supplier<TabFactory>, Component)` | `TabRegistry.registerTab(int priority, TabFactory, Supplier<Item> icon, Component title)` |
| `L2Tabs.ICON` / `L2Tabs.ORDER` data maps | priority and icon are arguments to `registerTab` |
| `TabBase<G, T> extends Button` (rework) | `BaseTab<T> extends FloatingButton` |
| `TabManager.init(Consumer<AbstractWidget>, ITabType)` | `TabManager.init(Consumer<AbstractWidget>, TabToken<?>)` |

`GLMisc` (tab registration), `GLDataMapGen` (icon/order) and `QuestTab`
(`BaseTab` constructor) are the affected sites.

## 8. Data maps → BaseConfig

The 1.20.1 replacement for `DataMapReg<K, V>` is the `BaseConfig` system in
`l2library.serial.config`:

| 3.x | 1.20.1 |
|---|---|
| `DataMapReg<K, V>` | `ConfigTypeEntry<T extends BaseConfig>` |
| `DataMapType` registration event | `PacketHandlerWithConfig` + `ConfigTypeEntry` constructor |
| `reg.get(access, holder)` | `entry.getEntry(id)` / `entry.getMerged()` |
| `reg.getAll(access)` | `entry.getAll()` |
| data-map JSON | `@ConfigCollect` fields merged by `ConfigMerger` |
| `data/<ns>/data_maps/<registry>/<id>.json` | `data/<ns>/<config_path>/<name>/<id>.json` |
| data-map datagen | `ConfigDataProvider` |

`MenuLayoutConfig` is the model: it is a `BaseConfig` with `@SerialField`
members and is registered through `ConfigTypeEntry`. The mod's ten data-map
users follow the same pattern; `l2tabs` icon/order maps disappear entirely
because the legacy tab API takes both as arguments.
