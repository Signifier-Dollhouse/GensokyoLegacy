# API differences: 1.21.1 → 1.20.1

Only the surface GensokyoLegacy calls is listed. The 1.20.1 side is what the
current branch actually compiles against; the shims live in
`dev.xkmc.gensokyolegacy.compat.l2lib`.

## 1. Imports and package roots

Mechanical rewrites, safe to apply before looking at any symbol:

| 1.21.1 import | 1.20.1 target |
|---|---|
| `l2serial.serialization.marker.SerialClass` | `l2serial.serialization.SerialClass` |
| `l2serial.serialization.marker.SerialField` | `l2serial.serialization.SerialClass.SerialField` |
| `l2serial.serialization.marker.OnInject` | `l2serial.serialization.SerialClass.OnInject` |
| `l2modularblock.core.*` | `l2modularblock.*` (package root) |
| `l2menustacker.*` | `l2screentracker.*` (see §10) |
| `l2itemselector.overlay.*` | `l2library.base.overlay.*` |
| `l2library.content.raytrace.RayTraceUtil` | `l2library.util.raytrace.RayTraceUtil` |
| `l2core.*` | `l2library.*` or `compat.l2lib.*`, map in §3.1 |
| `net.neoforged.fml.*` | `net.minecraftforge.fml.*` |
| `net.neoforged.api.distmarker.*` | `net.minecraftforge.api.distmarker.*` |
| `net.neoforged.bus.api.*` | `net.minecraftforge.eventbus.api.*` |
| `net.neoforged.neoforge.*` | `net.minecraftforge.*` (exceptions below) |

`net.neoforged.neoforge.*` is not fully mechanical:

| 1.21.1 | 1.20.1 |
|---|---|
| `EventBusSubscriber` | `Mod.EventBusSubscriber` |
| `NeoForge.EVENT_BUS` | `MinecraftForge.EVENT_BUS` |
| `NeoForgeMod` | `ForgeMod` (`LAVA_TYPE.get()`, not `.value()`) |
| `ModConfigSpec` | `ForgeConfigSpec` |
| `Capabilities.ItemHandler/FluidHandler` | `ForgeCapabilities.ITEM_HANDLER/FLUID_HANDLER` |
| `NeoForgeRegistries` | `ForgeRegistries` |
| `ItemAbility` / `ItemAbilities` | `ToolAction` / `ToolActions` |
| `IEntityWithComplexSpawn` | `IEntityAdditionalSpawnData` |
| `RegisterSpawnPlacementsEvent` | `SpawnPlacementRegisterEvent` |
| `BaseFlowingFluid` | `ForgeFlowingFluid` |
| `VanillaGuiLayers` | `VanillaGuiOverlay` |
| `ClientHooks` | `ForgeHooksClient` |
| `CommonHooks` / `EventHooks` | `ForgeHooks` |
| `NeoForgeDataMaps` | data maps shim, §7 |
| `RegisterClientExtensionsEvent` | per-item `Item#initializeClient`, per-fluid `FluidType#initializeClient` |
| `ModifyDefaultComponentsEvent` | set defaults in `Item.Properties` / on the item |
| `TriState` | `boolean` (`canSustainPlant`) |
| `DataComponentIngredient` | `NBTIngredient` |

## 2. Java 21 → 17

- Record patterns: `if (x instanceof Foo(int a, boolean b))` → `if (x instanceof Foo f)`
  then `f.a()`, `f.b()`. Four such sites existed in the port.
- `List.getFirst()` / `getLast()` → `get(0)` / `get(size() - 1)`.
- `Math.clamp(v, lo, hi)` → `Mth.clamp(v, lo, hi)`.
- Records, sealed types and switch expressions are fine on 17.

## 3. Registration and the L2 namespace

### 3.1 `l2core` → 1.20.1

| 1.21.1 | 1.20.1 |
|---|---|
| `base.effects.EffectUtil` | `l2library.base.effects.EffectUtil` (takes `AddReason`) |
| `base.menu.base.*`, `base.tile.*` | same paths in `l2library` |
| `capability.attachment.GeneralCapabilityTemplate` | `l2library.capability.entity.GeneralCapabilityTemplate` |
| `capability.player.*` | `l2library.capability.player.*` |
| `compat.jei.BaseRecipeCategory` | `l2library.serial.recipe.BaseRecipeCategory` |
| `compat.patchouli.PatchouliHelper` | `l2library.compat.patchouli.PatchouliHelper` |
| `content.raytrace` | `l2library.util.raytrace` |
| `content.explosion` | `l2library.init.explosion` |
| `serial.advancements.*` | `l2library.serial.advancements.*` (legacy `FrameType`) |
| `serial.recipe.*` | `l2library.serial.recipe.*` (legacy `Container`) |
| `util.Proxy` | `l2library.util.Proxy`; `getWorld()` → `getLevel()` |
| `init.reg.registrate.L2Registrate` | `compat.l2lib.GLRegistrate` (extends `l2library.base.L2Registrate`) |
| `init.reg.registrate.{SimpleEntry, LegacyHolder}` | `compat.l2lib.*` |
| `init.reg.simple.{Val, SR, Reg, CdcVal, CdcReg, DCReg, DCVal, AttReg, AttVal, EnumCodec}` | `compat.l2lib.*` |
| `init.reg.datapack.{DatapackReg, DataMapReg}` | `compat.l2lib.*` |
| `init.reg.ench.DataGenHolder` | `compat.l2lib.DataGenHolder` |
| `base.entity.SyncedData` | `compat.l2lib.SyncedData` |
| `capability.level.BaseSavedData` | `compat.l2lib.BaseSavedData` |
| `serial.loot.{LootHelper, AddItemModifier}` | `compat.l2lib.*` |
| `events.SchedulerHandler` | `compat.l2lib.SchedulerHandler` |
| `util.{ConfigInit, DCStack}` | `compat.l2lib.*` |

The mod-side classes stay in `compat/l2lib`; do not add them to the L2Library
jar. `DataMapReg` is a working replacement rather than removal: keep 1.21
`get(Holder)` / `get(RegistryAccess, Holder)` / `getAll(RegistryAccess)` call
shapes.

### 3.2 Registrate

- `DeferredHolder<X, T>` → `com.tterrag.registrate.util.entry.RegistryEntry<T>`
  or `compat.l2lib.Val<T>`. `Val` has `get`, `id`, `key`.
- `DeferredRegister` still exists; `SR.of(REG, registry).reg(id, supplier)`
  returns a `Val`.
- `GLRegistrate` adds the helpers the 3.x `L2Registrate` had:
  `particle` (13 sites), `registerSynced`, `registerUnsynced`, `loc`,
  `registerClient`, plus `GensokyoLegacy.REG.dataReg` / `REG.dataMap`.
- `ProviderType.DATA_MAP` does not exist. Data maps use §7.
- `ItemProviderEntry<Item, T>` and `RegistryEntry<T>` take **one** type
  parameter in `MC1.20-1.3.11`.
- `RegisterParticleProvidersEvent` has the same shape in Forge; the
  `ParticleSupplier` shim keeps the 3.x registration lambda.

## 4. Attachments → capabilities

1.21 `AttachmentType` call sites keep their shape through `AttVal`:

```java
AttVal.CapVal<Level, LevelAreaAttachment> LEVEL_EFFECT = ATT.entity(
        "level_area", LevelAreaAttachment.class, LevelAreaAttachment::new, Level.class, e -> true);
AttVal.PlayerVal<CharacterAttachment> CHARACTER = ATT.player(...);

// reads, unchanged from 1.21
GLMeta.CHARACTER.type().getOrCreate(player);
GLMeta.CHARACTER.type().getExisting(player);   // Optional
```

Each attachment class is still a legacy template with a static capability token:

```java
public class PartyAttachment extends GeneralCapabilityTemplate<LevelChunk, PartyAttachment> {
    public static final Capability<PartyAttachment> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});
}
```

`AttReg.player(...)` / `AttReg.entity(...)` build the legacy
`PlayerCapabilityHolder` / `GeneralCapabilityHolder` from that token, and
`AttType` exposes `getOrCreate`, `getExisting`, `isProper`.

Legacy lifecycle, which `PlayerCapabilityTemplate` subclasses must follow:
`tick()`, `init()`, `onClone(boolean)`; the `player` / `world` fields are
inherited. 1.21 signatures take the player as a parameter.

`RegisterCapabilitiesEvent` only has `register(Class)` in 1.20.1. The port does
not register block-entity/item capabilities there; use
`AttachCapabilitiesEvent` or an `ICapabilityProvider` on the block entity/item.

Block entity signatures drop the registry provider:
`load(CompoundTag)`, `saveAdditional(CompoundTag)`, `getUpdateTag()`.

## 5. Networking

- Packets stay **records** and implement `dev.xkmc.l2serial.network.RecordPacketBase`
  (added in l2serial 1.2.8). `handle(Player)` keeps the 1.21 body.
- `SerialPacketBase<T>` (a record interface) and `PacketHandler` /
  `StreamCodec` / `IPayloadContext` do not exist. Non-record packets extend
  `SerialPacketBase` and implement `handle(NetworkEvent.Context)`.
- `GLPackets.create(version)` builds a `PacketHandlerWithConfig`
  (`l2library.serial.config`), kept as `GensokyoLegacy.HANDLER`. One
  `.toClient(X.class)` / `.toServer(X.class)` line per packet.
- `NetDir` → `NetworkDirection`; `RegistryFriendlyByteBuf` → `FriendlyByteBuf`.
  `toServer`, `toClientPlayer`, `toTrackingPlayers`, `toAllClient`,
  `sendToNear` keep their names.
- `StreamCodec` overloads in `GLPackets` are dropped; codecs are built from
  `FriendlyByteBuf` functions instead.
- `CodecAdaptor` and the marker annotations folded into `SerialClass` exist in
  l2serial 1.2.8. `Handlers.registerReg` does not; datapack registries go
  through the `DatapackReg` shim (§7).

## 6. Item data components → NBT

There is no component registry. `DCVal` is a named NBT entry on the stack:

```java
DCVal<HakkeroMode> MODE = DC.enumVal("mode", EnumCodec.of(HakkeroMode.class, HakkeroMode.values()));
MODE.get(stack); MODE.set(stack, value); MODE.getOrDefault(stack, def); MODE.has(stack);
```

`DCReg` (`DC` in `GLItems`) has `reg(id, Codec|Class, cache)`, `enumVal`,
`intVal`, `uuid`, `loc`, `unit`, `stack`, `registry`. `DCStack` compares stacks
by NBT. Components map as follows:

| component | 1.20.1 |
|---|---|
| `POTION_CONTENTS` | `PotionUtils` plus `Potion`, `CustomPotionColor`, `CustomPotionEffects` (and the mod-side `PotionContents` helper) |
| `ITEM_NAME` / `CUSTOM_NAME` | `ItemStack.setHoverName` / `getHoverName` |
| `FOOD` | `stack.getItem().getFoodProperties(stack, player)` |
| `REPAIR_COST` | NBT `RepairCost` |
| `ENCHANTMENT_GLINT_OVERRIDE` | NBT flag + `Item#isFoil` |
| `FIREWORKS` | NBT `Fireworks` via `FireworkRocketItem` |
| `TOOL` | `DiggerItem` / `TieredItem` |
| `FluidIngredient` (NeoForge) | `compat.l2lib.FluidIngredient` record + `CODEC` + `test/getStacks` |

`ItemStack.set/get(DataComponentType, T)` and `MutableDataComponentHolder` do
not exist. `ItemStack.OPTIONAL_CODEC` → NBT.

## 7. Data maps

`DataMapReg` wraps a `ConfigTypeEntry` and keeps the 1.21 read API:

```java
static final DataMapReg<Block, BedData> BED_DATA =
        GensokyoLegacy.REG.dataMap("bed_data", Registries.BLOCK, DataMapConfigs.Beds.class);

BED_DATA.get(access, holder);  // or get(holder)
BED_DATA.getAll(access);
```

Each map needs a config class with a concrete `values` field, because l2serial
cannot resolve a `V` inherited from a generic superclass:

```java
@SerialClass
public static final class Beds extends DataMapConfig<BedData> {
    @SerialClass.SerialField
    @ConfigCollect(CollectType.MAP_OVERWRITE)
    public LinkedHashMap<ResourceLocation, BedData> values = new LinkedHashMap<>();

    @Override public LinkedHashMap<ResourceLocation, BedData> values() { return values; }
}
```

Datagen extends `ConfigDataProvider`; the JSON path is
`data/<ns>/gensokyolegacy_config/<map_name>/main.json` (not
`data_maps/<registry>/<id>.json`). All files of a type merge by key overwrite.

### Datapack registries

`GensokyoLegacy.REG.dataReg(id, Codec<T>)` returns a `DatapackReg<T>` backed by
`DataPackRegistryEvent.NewRegistry`; reads are `get(access, id)` /
`getAll(access)`. Runtime JSON stays at
`data/<ns>/<modid>/<registry>/...` (for example
`data/gensokyolegacy/gensokyolegacy/dialog/`). Datagen bootstraps are collected
by `compat/datagen/DataProviderInitializer` and emitted through
`GLDatapackProvider`.

## 8. Serialization and config

| 1.21.1 | 1.20.1 |
|---|---|
| `CodecHandler(Class, Codec, StreamCodec)` | `CodecHandler(Class, Codec, Function<FriendlyByteBuf, T>, BiConsumer<FriendlyByteBuf, T>)` |
| `enableVanilla(Class, Registry<T>)` | `enableVanilla(Class, Supplier<IForgeRegistry<T>>)` |
| `Handlers.CODEC_MAP`, `NULL_DEFER` | `Handlers.NBT_MAP`, `Handlers.MAP` (1.2.8 also splits `JSON_MAP` / `PACKET_MAP`) |
| `TagCodec.toTag(..., pred)` | predicate arguments stay on every 1.20.1 call |
| `CodecAdaptor` (1.21) | present in l2serial 1.2.8; no change |
| `DataMapType` / `CodecAdaptor` registration | `BaseConfig` / `ConfigTypeEntry`, §7 |

Config: `ConfigInit` wraps `ForgeConfigSpec`; `GLRegistrate.registerSynced`
registers `ModConfig.Type.SERVER`, `registerUnsynced` `COMMON`. There is no
NeoForge config screen.

## 9. Blocks (L2ModularBlock)

- The six delegate classes and `BlockTemplates`, `VoxelBuilder` live at the
  package root. `l2modularblock.core.*` does not exist.
- 1.20.1 has one `use(...)`; 1.21 splits it into `useItemOn` /
  `useWithoutItem`. The backport keeps `OnClickBlockMethod` and dispatches
  `UseItemOnBlockMethod` / `UseWithoutItemBlockMethod` from `use(...)`.
  `PathFindBlockMethod` and `PlayerDestoryBlockMethod` are present.
- Waterlogging: `DelegateBlock.newBaseBlock` returns
  `DelegateBlockSimpleWaterImpl` / `DelegateEntityBlockSimpleWaterImpl` when
  the method list carries `WaterloggedMethodMarker`.
- `impl.DoubleBlockImpl` is present (it was missing upstream before the port).
- `ItemInteractionResult` → `InteractionResult`; Forge `ToolAction` replaces
  `ItemAbility`; `isPathfindable` takes 4 arguments; `playerWillDestroy`
  returns `void`.

## 10. Legacy-targeted libraries

### Damage tracker

| 1.21.1 | 1.20.1 |
|---|---|
| `DamageData.Attack` / `.Defence` / `.DefenceMax` | `AttackCache` + `ItemStack` |
| `onAttack(DamageData.Attack)` | `onAttack(AttackCache, ItemStack)` |
| `data.getTarget()` / `getAttacker()` | `cache.getAttackTarget()` / `getAttacker()` |
| `data.getSource()` | `cache.getLivingAttackEvent().getSource()` |
| `data.getWeapon()` / `getStrength()` | `cache.getWeapon()` / `getStrength()` |
| `data.getDamageOriginal()` | `cache.getPreDamageOriginal()` |
| `data.addDealtModifier(...)` | `cache.addDealtModifier(...)` |
| `data.getDamageFinal()` | `cache.getDamageDealt()` |

`onAttack` returns `void`; cancel through the carried `LivingAttackEvent`.
Do not port the 3.x `DamageData` hierarchy.

### Tabs

| 1.21.1 | 1.20.1 |
|---|---|
| `L2Tabs.loc(id)` | `new ResourceLocation(L2Tabs.MODID, id)` |
| `L2Tabs.GROUP.registerTab(factory, title)` | `TabRegistry.registerTab(int priority, TabFactory, Supplier<Item> icon, Component title)` |
| `L2Tabs.ICON` / `L2Tabs.ORDER` data maps | priority and icon are arguments to `registerTab` |
| `TabBase<G, T> extends Button` | `BaseTab<T> extends FloatingButton` |
| `TabManager.init(Consumer, ITabType)` | `TabManager.init(Consumer, TabToken<?>)` |
| `TabManager.init(screen, data)` | `TabManager(screen)` |

### Item selector

1.20.1 methods are per-player, not per-stack: `getIndex(Player)`,
`getList()`, `getDisplayList()`, `move(int, Player)`, `swap(Player, int)`.
`getSelection(Player)` returns the selector, not a `Holder`. The six
`WheelAdaptor.Provider` listeners drop their `ItemStack` parameters.
`l2itemselector.overlay.*` classes live in `l2library.base.overlay`.

### Screen tracker / menu stacker

`l2menustacker` → `l2screentracker`: `L2MenuStacker` → `L2ScreenTracker`,
`L2MSConfig` → `L2ScreenTrackerConfig`, `PlayerSlot`,
`WritableStackClickHandler`, `ClickedPlayerSlotResult` keep their names under
the legacy package. `CacheMouseToClient` and `MouseCache` were backported:

```java
L2ScreenTracker.PACKET_HANDLER.toClientPlayer(new CacheMouseToClient(), sp);
```

`PlayerSlot` reads/writes `FriendlyByteBuf`, not `RegistryFriendlyByteBuf`.

## 11. Loader events and helpers

| 1.21.1 | 1.20.1 |
|---|---|
| `PlayerTickEvent` / `EntityTickEvent` / `LevelTickEvent` | `TickEvent.PlayerTickEvent` / `.EntityTickEvent` / `.LevelTickEvent` |
| `ServerTickEvent.Post` | `TickEvent.ServerTickEvent` (no `.Post`) |
| `ClientTickEvent.Post` | `TickEvent.ClientTickEvent.Phase.END` |
| `RegisterGuiLayersEvent` | `RegisterGuiOverlaysEvent` |
| `LivingIncomingDamageEvent` | `LivingAttackEvent` / `LivingHurtEvent` |
| `LivingShieldBlockEvent` | `ShieldBlockEvent` |
| `EntityInvulnerabilityCheckEvent` | `LivingAttackEvent` / `LivingHurtEvent` |
| `ICancellableEvent` | `@Cancelable` + `setCancelable(true)` |
| `IEventBus.post(event)` returns the event | returns `boolean`: post, then read the event |
| `DamageContainer` / `DamageRefactorEntity` | rewrite onto `LivingHurtEvent` |

`SchedulerHandler` shims the 1.21 scheduler: `schedule`, `schedulePersistent`,
`serverTick(TickEvent.ServerTickEvent)`.

## 12. Client and rendering

- GeckoLib 4.4.9 (`geckolib-forge-1.20.1`); `GeoItem#initializeClient` is the
  client-extension hook.
- `GuiGraphics` differs; validate every `render*` override against 1.20.1.
- `DeltaTracker` → plain `float partialTick`.
- `VertexConsumer.addVertex/setUv/setColor` → `vertex/uv/color/endVertex`.
- `TransformOrigin` does not exist; `rootTransforms()` has no origin.
- `renderBackground(GuiGraphics)` only; `EditorScreen` sits on
  `OptionsSubScreen` / `OptionsList(Minecraft, width, height, y0, y1, itemHeight)`.
- `EventBusSubscriber.Bus.GAME` → `Bus.FORGE`.
- `ProjectileWeaponItem.shootProjectile` does not exist; drop the override.
- `ParticleType` takes `(boolean, Deserializer)`; `codec()` returns `Codec<T>`.
- `ClientObjectCache.renderAll` already takes the partial tick.

## 13. Worldgen and data access

- `NeoForgeRegistries.Keys.BIOME_MODIFIERS` → `ForgeRegistries.Keys.BIOME_MODIFIERS`;
  generated path `data/<ns>/neoforge/biome_modifier` → `data/<ns>/forge/biome_modifier`.
- TerraBlender 1.20.1-3.0.1.10. `FlatCheckStructure` follows the reference
  `FlatStructure`.
- `HolderLookup.Provider` → `RegistryAccess` for loot, recipes, configs and
  saved data.
- `MinecraftServer.reloadableRegistries()` → `registryAccess()` plus
  `getLootTables()`.
- `EquipmentSlotGroup` is absent; `AttributeModifier` uses a UUID, not a
  `ResourceLocation`.
- Structure APIs: `MultiSpreadPlacement`, `SetDataProcessor`, `PieceTree`,
  `GLSinglePiece`, `ChunkGeneratorStructureState` and the
  `NoiseBasedChunkGenerator` constructor were audited during the port; the
  `getNearestGeneratedStructure` signature is identical in 1.20.1.

## 14. Recipes, advancements, datagen

| 1.21.1 | 1.20.1 |
|---|---|
| `BaseRecipe<..., Inv extends RecipeInput>` | `BaseRecipe<..., Inv extends Container>` |
| `assemble(Inv, HolderLookup.Provider)` | `assemble(Inv, RegistryAccess)` |
| `Supplier<RecipeType<SRec>> type` | `RegistryEntry<RecipeType<SRec>> type` |
| `BaseRecipeBuilder.save(RecipeOutput, id)` | `save(Consumer<FinishedRecipe>, id)` |
| `Criterion<?>` | `CriterionTriggerInstance` |
| `AdvancementType` | `FrameType` |
| `CriterionBuilder.onBuild(String, Builder, List<ICondition>)` | `modify(String, Builder)` |
| `item(..., DataComponentPredicate)` | `item(..., CompoundTag)` |
| `PatchouliHelper.buildShapedRecipe(Consumer<ShapedRecipeBuilder>, ...)` | `Consumer<ShapedPatchouliBuilder>` |

Datagen: registrate `DataProviderInitializer` is shimmed in `compat/datagen`;
1.20.1 rejects two providers called `Registries`, so dynamic-registry data goes
through `GLDatapackProvider` (a named `DatapackBuiltinEntriesProvider`).

## 15. Mixins and access transformer

- The AT applies to the SRG-named jar; **entries are SRG names**
  (`f_114362_`, `m_140416_()Ljava/lang/Iterable;`, …). Do not copy official
  names from 1.21.1.
- Mixin targets that changed in the port:
  `AbstractContainerScreen#renderSlot(GuiGraphics, Slot)` (no
  `renderSlotContents`), `RandomizableContainerBlockEntity` (no
  `RandomizableContainer` interface),
  `net.minecraft.world.entity.animal.frog.ShootTongue` (moved package),
  `LocalPlayer#canSwimInFluidType(net.minecraftforge.fluids.FluidType)`.
  `ILivingEntityExtension` does not exist; the corresponding mixin was dropped.
- Mixin descriptors copied from 1.21.1 may name
  `net/neoforged/neoforge/fluids/FluidType`; swap it for
  `net/minecraftforge/fluids/FluidType`.
- Unlisted or unmatched mixins fail hard (`defaultRequire: 1`). A vanilla
  rename therefore appears as a startup crash.
- The AT variant race and `systemProp.fg.repo.recompile=false` are in the
  README build notes.

## 16. Danmaku and FastProjectile

Both libraries were ported down with the 1.21.1 package layout intact.

- `DanmakuHurtEvent.getResult()/setResult()` → `getAnswer()/setAnswer()`
  (`Event` already declares `getResult()` in 1.20.1).
- `DanmakuUseEvent` is `@Cancelable`: `isCanceled()` / `setCanceled()` replace
  the boolean result. `consume()` / `setConsume()` are unchanged.
- `IEntityWithComplexSpawn` → `IEntityAdditionalSpawnData`.
- `Entity.setPosRaw` is final in 1.20.1; the library's AT un-finals it. Do not
  assume that in mod code.
- The custom-spell component is gone: the form is the NBT compound `SpellData`
  (`TagCodec.valueToTag` / `valueFromTag`).
- `IrisCompat` reads `ShadowRenderer.ACTIVE` reflectively.
- `DanmakuManager` packet factories no longer take `registryAccess()`.
- DanmakuAPI has no mixin config and does not import `content/render` or
  `content/virtual` from FastProjectileAPI; those live in FastProjectile.
