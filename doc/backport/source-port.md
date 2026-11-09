# Source port

Status: **A1 (bootstrap) done.** The Forge 1.20.1 build configures, resolves
both classpaths, and `prepareRuns` accepts the access transformer and mixin
config. No `src/main/java` file has been changed yet.

This document is the working map for the source port. It is derived from the
actual 1.20.1 jars in `libs/`, not from the 1.21.1 sources.

## Build (A1)

- ForgeGradle 6, Gradle 8.10, Java 17, Forge 47.1.3, official mappings.
- `META-INF/mods.toml`, `pack.mcmeta` 15, mixin `JAVA_17` + `refmap`.
- `./gradlew dependencies --configuration compileClasspath` and
  `runtimeClasspath` pass; `./gradlew prepareRuns` passes.

### Dependency model (D24)

`libs/` is a `flatDir` repository and every jar is `fg.deobf`'d. L2Library is
an explicit external dependency; the mod does not jar-in-jar it or the jars it
ships.

| coordinate | scope |
|---|---|
| `dev.kxmc.l2library:l2library:2.5.6-slim` | `implementation` |
| `com.tterrag.registrate:Registrate:MC1.20-1.3.11` | `implementation` |
| `io.github.llamalad7:mixinextras-forge:0.2.0-beta.8` | `implementation` |
| `dev.xkmc.l2serial:l2serial:1.2.7` | `implementation` |
| `dev.xkmc.l2tabs:l2tabs:0.4.0` | `implementation` |
| `dev.xkmc.l2screentracker:l2screentracker:0.1.5` | `implementation` |
| `dev.xkmc.l2modularblock:l2modularblock:1.2.0` | `implementation` |
| `dev.xkmc.l2itemselector:l2itemselector:0.2.1` | `implementation` |
| `dev.xkmc.l2damagetracker:l2damagetracker:0.4.4` | `implementation` + `jarJar` |
| `dev.xkmc.fastprojectileapi:fast_projectile_api:3.1.2+1.20.1` | `implementation` + `jarJar` |
| `dev.xkmc.danmakuapi:danmaku_api:3.0.6+1.20.1` | `implementation` + `jarJar` |
| `software.bernie.geckolib:geckolib-forge-1.20.1:4.4.9` | `implementation` + `jarJar` |
| `com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.10` | `implementation` + `jarJar` |
| content mods | `implementation` / `compileOnly` |

The six `implementation` jars below L2Library are exactly what the full
L2Library jar carries. At runtime the player installs L2Library; in dev they
are separate classpath entries so the slim jar can be used without `jij`.

## Compile baseline

`./gradlew compileJava` stops after parsing: 11 errors in 4 files. No
attribution errors are reported until these are rewritten.

| file | line | construct |
|---|---|---|
| `compat/jei/QuestRewardCategory.java` | 120 | `instanceof LootTableReward(ResourceLocation table)` |
| `content/attachment/party/core/PartyGiftReader.java` | 77 | `instanceof AppealConsumedCondition(int, int, boolean)` |
| `content/rpg/reward/LootReader.java` | 44 | `instanceof LootTableReward(ResourceLocation table)` |
| `content/command/GLCommands.java` | 613 | `instanceof ReputationReward(int, int, int, int)` |

These are Java 21 record patterns. Rewrite each to `instanceof X x` plus
accessors. That is the first slice; it unblocks the real attribution errors.

## Mechanical rewrites

Verified against the jars in `libs/`. Counts are import lines; files are
unique source files.

| 3.x import | 1.20.1 target | imports | files |
|---|---|---|---|
| `l2serial.serialization.marker.SerialClass` | `l2serial.serialization.SerialClass` | 121 | 121 |
| `l2serial.serialization.marker.SerialField` | `l2serial.serialization.SerialClass.SerialField` | 84 | — |
| `l2serial.serialization.marker.OnInject` | `l2serial.serialization.SerialClass.OnInject` | 5 | — |
| `l2modularblock.core.*` | `l2modularblock.*` (root) | 53 | 31 |
| `l2menustacker.*` | `l2screentracker.*` | 22 | 14 |
| `l2core.*` | `l2library.*` (D21 map) | 127 | 95 |
| `l2library.content.raytrace.RayTraceUtil` | `l2library.util.raytrace.RayTraceUtil` | 4 | 4 |
| `l2itemselector.overlay.*` | `l2library.base.overlay.*` | 6 | 6 |
| `net.neoforged.*` | `net.minecraftforge.*` | — | 125 |

`l2menustacker` has no 1.20.1 counterpart package; the classes are renamed:

| 3.x | 1.20.1 |
|---|---|
| `screen.source.PlayerSlot` | `l2screentracker.screen.source.PlayerSlot` |
| `screen.packets.CacheMouseToClient` | `l2screentracker.screen.packets.CacheMouseToClient` |
| `click.writable.WritableStackClickHandler` | `l2screentracker.click.writable.WritableStackClickHandler` |
| `click.writable.ClickedPlayerSlotResult` | `l2screentracker.click.writable.ClickedPlayerSlotResult` |
| `init.L2MenuStacker` | `init.L2ScreenTracker` (`PACKET_HANDLER`) |

Not import swaps:

| site | change |
|---|---|
| `init/GLPackets.java` | 3.x `PacketHandler` builder (`NetDir`, `PacketConfiguration`, codec overloads) → `BasePacketHandler.create(Class, NetworkDirection)` / `create(Class, Function, NetworkDirection)` |
| `init/registrate/GLMeta.java` | `DataMapType` + `CodecAdaptor` → `BaseConfig` / `ConfigTypeEntry` (D12) |
| `init/registrate/GLItems.java` | `l2core.init.reg.simple.EnumCodec` (uses `StreamCodec`) → mod-side `DCVal` shim codec |
| `init/registrate/GLMisc.java`, `content/ui/quest/QuestTab.java`, `QuestInfoScreen.java` | 3.x `TabBase` / `InvTabData` / `TabManager<>(screen, data)` → legacy `BaseTab`, `TabRegistry.registerTab(priority, factory, icon, title)`, `TabManager(screen)` |
| 4 talisman/listener files | `DamageData.Attack/Defence/DefenceMax` → `AttackCache`; 1.20.1 `onAttack` returns `void`, so cancellation moves to the carried `LivingAttackEvent` |
| 14 files | `ItemInteractionResult` → `InteractionResult` |
| 27 files | `net.minecraft.core.component` / `DataComponents` → mod-side `compat.components` shim (D4) |
| 6 files | `MinecraftServer.reloadableRegistries()` → `registryAccess()` + `getLootTables()` |

## Mod-side compat surface (O6)

Classes the mod imports that do not exist in any 1.20.1 jar. These go into
`compat/` and must not be added to L2Library.

| package | classes | imports |
|---|---|---|
| `init.reg.simple` | `Val`, `SR`, `Reg`, `CdcVal`, `CdcReg`, `DCReg`, `DCVal`, `AttReg`, `AttVal`, `EnumCodec` | 26 |
| `init.reg.registrate` | `SimpleEntry`, `LegacyHolder`, `L2Registrate.ParticleSupplier` | 9 |
| `serial.loot` | `LootHelper`, `AddItemModifier` | 11 |
| `base.entity` | `SyncedData` | 6 |
| `capability.level` | `BaseSavedData` | 3 |
| `util` | `ConfigInit`, `DCStack` | 3 |
| `events` | `SchedulerHandler` | 1 |
| `init.reg.datapack` | `DatapackReg`, `DataMapReg` | 3 |
| `init.reg.ench` | `DataGenHolder` | 2 |

`L2Registrate` exists at `l2library.base.L2Registrate` but lacks the 3.x
helpers the mod calls: `particle` (13 call sites), `registerSynced` (1),
`registerUnsynced` (1). The compat layer supplies them; it does not patch the
library jar.

`DataComponents` is the largest shim. The surface is 26 files; freeze it
before writing code (A2 exit).

### A2 surface, frozen

Signatures below are the ones the mod actually calls. Only the body is adapted;
call sites keep their shape.

| compat class | surface the mod calls | 1.20.1 backing |
|---|---|---|
| `GLRegistrate` | `loc`, `particle`, `registerClient`, `registerUnsynced`, `registerSynced`, `dataReg` | `l2library.base.L2Registrate` subclass; `ConfigInit`; `Val` |
| `Val<T>` | `get`, `id`, `key`, `val` | `RegistryEntry<T>` / `RegistryObject<T>` (no `DeferredHolder`) |
| `SimpleEntry<T>` | `get`, `key`, `id`, `holder`, `equals`/`hashCode` | `RegistryEntry<T>`; `holder()` = `getHolder().orElseThrow()` |
| `LegacyHolder<T>` | `Holder<T>` + `get`, `key`, `holder` | `Holder<T>` defaults; `RegistryObject.getHolder()` |
| `ParticleSupplier<T>` | `provider`, `sprite`, `spriteSet`, `register` | `RegisterParticleProvidersEvent` (Forge class, same shape) |
| `DCVal<T>` | `get(stack)`, `getOrDefault(stack, def)`, `set(stack, val)` (152 calls) | item NBT; no data-component registry |
| `DCReg` | `reg(id, Class, cache)`, `reg(id, Codec, cache)`, `enumVal`, `intVal`, `uuid`, `loc`, `unit`, `stack`, `registry` | `TagCodec` / `NbtOps` over a namespaced NBT key |
| `DCStack` | `stack`, `equals`, `hashCode` | `ItemStack` NBT comparison |
| `EnumCodec<T>` | `of(Class, T[])` | string codec + ordinal (drop `StreamCodec`) |
| `CdcVal<T>` / `CdcReg<T>` | `get`, `id`, `reg(id, MapCodec)` | `Registry<MapCodec<? extends T>>` + `RegistryObject` |
| `SR<T>` | `of(REG, registry)`, `reg(id, supplier)` | `DeferredRegister<T>` + `RegistryObject` |
| `Reg` | `new Reg(modid)`, `dataReg`, `dataMap` (data maps drop) | `DeferredRegister` factory |
| `AttReg` / `AttVal` | `player(...)`, `entity(...)`, `type().getOrCreate(...)`, `cap(...)` | Forge capabilities (`AttachCapabilitiesEvent` / `ICapabilityProvider`) |
| `SyncedData` | `define`, `write`, `read`, `register` | `SynchedEntityData.defineId` at class load; `register` becomes a no-op |
| `BaseSavedData<T>` | `save`, `isDirty` | `SavedData` + `TagCodec`; `RegistryAccess` instead of `HolderLookup.Provider` |
| `LootHelper` | `resolve`, loot-table builders | `RegistryAccess` instead of `HolderLookup.Provider` |
| `AddItemModifier` | `LootModifier` + `MapCodec` | drop `DoubleConfigValue`; keep a `DoubleSupplier` |
| `SchedulerHandler` | `schedule`, `schedulePersistent`, event hooks | `TickEvent.ServerTickEvent` (no `.Post`) |
| `ConfigInit` | `Builder`, `define`, `push`, `text`, `comment`, `register` | `ForgeConfigSpec`; drop the NeoForge config screen |
| `DataGenHolder<T>` | `Holder<T>` record | copy; `Holder` API is the same |
| `DatapackReg<T>` | `onRegister`, `get(access, id)`, `getAll(access)`, `key` | `DataPackRegistryEvent.NewRegistry` (Forge has it) |
| `DataMapReg` | — | gone with D12 |

`AttReg`/`AttVal` is the largest non-mechanical piece. The 3.x types are
NeoForge attachment wrappers; the 1.20.1 shim sits on the legacy L2 capability
classes (`PlayerCapabilityTemplate`, `GeneralCapabilityTemplate`,
`GeneralCapabilityHolder`) and the vanilla `AttachCapabilitiesEvent`. The
`type()` object keeps the `getOrCreate(owner)` shape so the 20-odd call sites
do not change.

### Data components (D4)

The mod uses eight vanilla components and three component APIs. The shim is
`compat/components`; `ItemStack.set/get(DataComponentType, T)` is a 1.20.5
method, so those call sites become shim helpers.

| component | uses | 1.20.1 backing |
|---|---|---|
| `POTION_CONTENTS` | 18 | mod-side `PotionContents` record over `PotionUtils` (`Potion`, `CustomPotionColor`, `CustomPotionEffects`) |
| `ITEM_NAME` | 5 | `ItemStack.setHoverName(Component)` |
| `CUSTOM_NAME` | 2 | `ItemStack.setHoverName` / `getHoverName` |
| `FOOD` | 4 | `stack.getItem().getFoodProperties(stack, player)` |
| `REPAIR_COST` | 2 | NBT `RepairCost` |
| `ENCHANTMENT_GLINT_OVERRIDE` | 2 | NBT flag + `Item#isFoil` (or a mod-side mixin) |
| `TOOL` | 1 | `DiggerItem`/`TieredItem` for the anchor; no component |
| `FIREWORKS` | 1 | NBT `Fireworks` compound via `FireworkRocketItem` |
| `DataComponentType<T>` | 3 | shim marker type used by `DCVal` |
| `MutableDataComponentHolder` | 3 | `ItemStack` shim interface |

`PotionContents` is imported by 11 files and constructed in 8 shapes
(`new PotionContents(Optional<Holder<Potion>>, Optional<Integer>,
List<MobEffectInstance>)` and the one-arg form). Its used surface is
`getAllEffects`, `getColor`, `addPotionTooltip`, plus the static `getColor` and
`addPotionTooltip`. 1.20.1 `PotionUtils` provides all of them.

### Loader swap deltas

Mechanical package roots: `net.neoforged.bus.api` → `net.minecraftforge.eventbus.api`,
`net.neoforged.fml` → `net.minecraftforge.fml`,
`net.neoforged.api.distmarker` → `net.minecraftforge.api.distmarker`,
`net.neoforged.neoforge.*` → `net.minecraftforge.*` (with the exceptions below).

| 3.x | 1.20.1 |
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
| `LivingIncomingDamageEvent` | `LivingAttackEvent` / `LivingHurtEvent` |
| `LivingShieldBlockEvent` | `ShieldBlockEvent` |
| `EntityInvulnerabilityCheckEvent` | `LivingAttackEvent` / `LivingHurtEvent` |
| `PlayerTickEvent` / `EntityTickEvent` / `LevelTickEvent` | `TickEvent.PlayerTickEvent` / `.EntityTickEvent` / `.LevelTickEvent` |
| `ServerTickEvent.Post` | `TickEvent.ServerTickEvent` |
| `BaseFlowingFluid` | `ForgeFlowingFluid` |
| `VanillaGuiLayers` | `VanillaGuiOverlay` |
| `ClientHooks` | `ForgeHooksClient` |
| `CommonHooks` / `EventHooks` | `ForgeHooks` |
| `ICancellableEvent` | `Event#setCancelable(true)` in the constructor |
| `RegisterClientExtensionsEvent` | per-item `Item#initializeClient`, per-fluid `FluidType#initializeClient` |
| `TransformOrigin` | 1.20.1 `rootTransforms()` has no origin; rewrite the datagen transform |
| `TriState` | 1.20.1 `canSustainPlant` returns `boolean` |
| `DataComponentIngredient` | `NBTIngredient` (banner NBT) |
| `FluidIngredient` | mod-side record + codec + `test(FluidStack)` |
| `ItemHandlerCopySlot` | mod-side `Slot` subclass |
| `DamageContainer` | rewrite `DamageRefactorEntity` onto `LivingHurtEvent` |
| `ModifyDefaultComponentsEvent` | no 1.20.1 equivalent; set defaults on the item/`Item.Properties` |
| `NeoForgeDataMaps` (`Compostable`, `FurnaceFuel`) | `ComposterBlock.COMPOSTABLES` / `AbstractFurnaceBlockEntity.getFuel()` or `BaseConfig` |
| `DataMapType` | `BaseConfig` / `ConfigTypeEntry` (D12) |

`RegisterCapabilitiesEvent` is the one that changes shape: 1.20.1 only has
`register(Class)`. The mod's `registerBlockEntity`/`registerItem` calls
(`GensokyoLegacy.registerCapabilities`) move to `AttachCapabilitiesEvent` or an
`ICapabilityProvider` on `AlchemyPotBlockEntity` / `HexBrewWrapper`. The
reference mod uses `AttachCapabilitiesEvent<ItemStack>`; the same pattern
applies.

### Mixin and AT audit

The access transformer matches 1.20.1: every field and method in
`accesstransformer.cfg` exists (`EntityRenderDispatcher.renderers`,
`ChunkMap.getChunks`, `StructureManager.level`,
`NoiseBasedChunkGenerator.globalFluidPicker`, furnace counters, `Sensor`,
`Behavior`, `GateBehavior`, the loot-table internals).

Five mixins need a target change; the rest match.

| mixin | 1.20.1 change |
|---|---|
| `AbstractContainerScreenMixin` | `renderSlotContents` does not exist; inject `renderSlot(GuiGraphics, Slot)` at TAIL and use `slot.getItem()` |
| `ILivingEntityExtensionMixin` | `ILivingEntityExtension` → `IForgeLivingEntity`; `NeoForgeMod.LAVA_TYPE.value()` → `ForgeMod.LAVA_TYPE.get()` |
| `LocalPlayerMixin` | descriptor `net/neoforged/neoforge/fluids/FluidType` → `net/minecraftforge/fluids/FluidType` |
| `RandomizableContainerBlockEntityMixin` | `RandomizableContainer` is not a 1.20.1 class; target `RandomizableContainerBlockEntity` |
| `ShootTongueMixin` | `net.minecraft.world.entity.ai.behavior.ShootTongue` → `net.minecraft.world.entity.animal.frog.ShootTongue` |

`BrainAccessor` is fine (`memories`, `sensors`, `codec`,
`lastScheduleUpdate`, `forgetOutdatedMemories`, `tickSensors` all exist).
`ChunkGeneratorMultiSpreadMixin`'s private
`getNearestGeneratedStructure(Set, LevelReader, StructureManager, int, int,
int, boolean, long, RandomSpreadStructurePlacement)` signature is identical in
1.20.1. `GuiGraphicsMixin`'s
`renderItemDecorations(Font, ItemStack, int, int, String)` descriptor also
exists.

### Packet model (A5)

44 packet classes are records implementing the 3.x interface
`SerialPacketBase<T>`; `GLPackets` is the 3.x `PacketHandler` builder. Neither
exists in 1.20.1.

- Each record becomes a final class with `@SerialClass` fields, a no-arg
  constructor, and `handle(NetworkEvent.Context)`.
- `GLPackets` becomes a `BasePacketHandler` with
  `create(Class, NetworkDirection)`; the `StreamCodec` overloads disappear.
- `l2serial.serialization.marker.SerialClass` / `.SerialField` / `.OnInject`
  become nested types of `l2serial.serialization.SerialClass`.
- The five `SimplePacketBase` classes keep their shape but use
  `write(FriendlyByteBuf)` + `handle(Supplier<Context>)`.

The packet payloads themselves are plain `@SerialClass` fields, so the
serialization logic does not change; only the base class and the registration
chain do.

## Port order

1. **Parse unblock** — the four record-pattern files above.
2. **A2 shim** — `compat.components` surface, `compat/l2reg` wrappers,
   `SyncedData` / `BaseSavedData` / loot / config. The surface is frozen above;
   implement in this order: `Val` / `SimpleEntry` / `LegacyHolder` →
   `GLRegistrate` → `SR` / `CdcReg` / `DCVal` → `AttReg` / `AttVal` →
   components → the remaining helpers.
3. **Loader swap** — `net.neoforged.*` → `net.minecraftforge.*` (125 files),
   `@Mod` / `EventBusSubscriber`, capabilities, fluid/item handlers. Do the
   package rename first, then the non-mechanical table above; the capability
   rewrite is the only slice that needs a design pass.
4. **A3 registration** — `GLItems`, `GLBlocks`, `GLEntities`, fluids, menus,
   particles, effects; `DeferredHolder` → `RegistryObject`.
5. **A4 data layer** — attachments → capabilities, data maps → `BaseConfig`,
   datapack registries → `DataPackRegistryEvent`.
6. **A5 networking** — 48 packets → `SerialPacketBase`, `GLPackets` →
   `BasePacketHandler`.
7. **A6 worldgen** — dimension, biomes, structures, TerraBlender, datagen.
8. **A7 entities/AI** — brains, sensors, GeckoLib 4.4.
9. **A8 client/UI** — screens, tabs, item selector, menu stacker.
10. **A9 compat** — Curios, JEI, Patchouli, TLM, food mods.
11. **A10 datagen** — regenerate and commit.

## Verification

- After each slice: `./gradlew compileJava` and group errors by file.
- `./gradlew prepareRuns` after any AT or mixin change.
- Add `-Dnet.minecraftforge.gradle.check.certs=false` on this machine.
- Prefer `--offline` after the first resolve.
