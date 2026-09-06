# YoukaiEntity and CharacterData — Implementation & Usage Guide

This document reflects the current implementation in `content/entity/youkai/`, `content/entity/foundation/`, `content/attachment/character/`, `content/entity/behavior/`, `content/entity/module/` and `init/registrate/`.

## 1. Components

| Class | File | Role |
|---|---|---|
| `YoukaiEntity` | `content/entity/youkai/YoukaiEntity.java:57` | Abstract root `@SerialClass` extends `DamageClampEntity`. Holds `YoukaiTargetContainer`, `SpellCardWrapper`, `YoukaiModuleHolder`, `YoukaiCombatManager`, `YoukaiNavigationControl`, `YoukaiCardHolder`, `RestrictData` (synced + NBT). |
| `SmartYoukaiEntity` | `content/entity/youkai/SmartYoukaiEntity.java:35` | Adds `TaskBoard` + `SmartBrain` integration; defines scheduled/prioritized activities + sensors; handles `MEM_TALK`. |
| `GeneralYoukaiEntity` | `content/entity/youkai/GeneralYoukaiEntity.java` | `MAX_HEALTH 60`, `SPELL_DATA` synced model id, `DefaultCombatManager`. Base for most characters. |
| `BossYoukaiEntity` | `content/entity/youkai/BossYoukaiEntity.java` | `MAX_HEALTH 200`, `YoukaiFeatureSet.BOSS`. |
| `FairyEntity` / `MaidenEntity` / `RumiaEntity` | `content/entity/characters/*/` | Concrete subclasses. `Fairy` overrides `FairyCombatManager` + reinforcement; `Maiden` marks human navigation; `Rumia` adds `RumiaStateMachine`, `POWERED` flag, attribute modifier `EXRUMIA`. |
| `DamageRefactorEntity` | `content/entity/foundation/DamageRefactorEntity.java` | Replaces `LivingEntity.health` with `CombatProgress` (`@SerialField maxProgress/progress/oldProgress`). Guards `die`/`isDeadOrDying` via progress. |
| `DamageClampEntity` | `content/entity/foundation/DamageClampEntity.java` | Adds boss bar, `hurtCD`, speed clamp via `maxSpeed`, non-danmaku damage division. |
| `CharacterData` | `content/attachment/character/CharacterData.java:11` | `@SerialClass` per-character (`EntityType` key). `@SerialField int reputation`, `@SerialField FeedModuleData foodData`. Thresholds `150/ -50/ -150` → `FRIEND/STRANGER/JERK/ENEMY`. |
| `CharacterAttachment` | `content/attachment/character/CharacterAttachment.java:15` | `PlayerCapabilityTemplate<CharacterAttachment>` (`l2core AttReg`). `@SerialField Map<EntityType<?>,CharacterData>` (LinkedHashMap). `tick` at `dayTime==0` calls `dailyUpdate`. |
| `CharDataHolder` | `content/attachment/character/CharDataHolder.java` | `record(CharacterData data, Player player, EntityType<?> type, @Nullable YoukaiEntity e)` — transient facade. `sync()` sends `CharDataToClient`. |
| `ReputationState` | `content/attachment/character/ReputationState.java` | `FRIEND/STRANGER/JERK/ENEMY`. `toInfo(int)` colored text, `asTargetKind()` → `WORTHY/NONE/ENEMY`. |
| `FeedModuleData` | `content/attachment/character/FeedModuleData.java` | `@SerialClass Map<Item,Integer> statistics` + `List<Item> history` (MEMORY 10, RATE 0.9). `feed()` returns diminishing multiplier `1/(1+sum(0.9^n))`. |
| `CharacterConfig` | `content/attachment/datamap/CharacterConfig.java` | Datamap `DataMap<EntityType, CharacterConfig>`. Fields `structure RL, discardTime, respawnTime, wanderRadius, noPlayerVanishTime`. `create()` spawns `YoukaiEntity` + `HomeModule.setHome` + `restrictTo`. |
| `TaskBoard` | `content/entity/behavior/brain/TaskBoard.java:28` | DSL for brain graph: `always / exclusive(priority) / random` behaviors, `scheduled / prioritized` activities, `sensors`, `schedule`. |
| `SmartBrain` | `content/entity/behavior/brain/SmartBrain.java` | `Brain<E>` subclass. `construct(board,dynamic)` via `Provider` codec `Brain.codec(memoryTypes,sensorTypes)`. Priority scan `updateActivities`. |
| `GLBrains` | `init/registrate/GLBrains.java:15` | `SR`-registered sensors `SN_HOME/SN_HUNT/SN_ITEM/SN_LE/SN_PLAYER`, memories `MEM_PATH/MEM_PREY/MEM_DOWN/MEM_TALK/MEM_ITEMS`, activities `AT_HOME/HUNT/DOWN/TALK`. |
| `YoukaiModuleHolder` | `content/entity/module/YoukaiModuleHolder.java` | `List<AbstractYoukaiModule>` + `Map<Class,Optional>` cache + `hasPickup` flag. Iterable. |
| `HomeModule` | `content/entity/module/HomeModule.java` | `@SerialField StructureKey home` (immovable — structures/BEs never move, so no re-binding needed). `tickServer → BedRefData.entityTick`, `onKilled → onEntityDie`. |
| `YoukaiTargetContainer` | `content/entity/youkai/YoukaiTargetContainer.java` | `@SerialField LinkedHashSet<UUID> list` (maxSize 10/8). Maintains hostile UUIDs; `tick()` promotes `lastHurtByMob`/target. |

## 2. Hierarchy

```
PathfinderMob
 └─ DamageRefactorEntity (CombatProgress, @SerialClass)
     └─ DamageClampEntity (bossEvent, hurtCD, clampDamage)
         └─ YoukaiEntity (abstract, @SerialClass, IYoukaiEntity, SpellCircleHolder)
             └─ SmartYoukaiEntity (@SerialClass, Brain<SmartBrain>, TaskBoard)
                 ├─ GeneralYoukaiEntity (SPELL_DATA, 60HP)
                 │   ├─ FairyEntity (FairyCombatManager)
                 │   │   └─ CirnoEntity (CountPickupModule, Hunt sensors)
                 │   ├─ MaidenEntity → BossYoukaiEntity (200HP, markHuman)
                 │   ├─ MorichikaEntity, Mystia, etc.
                 │   └─ Boss entries (Yukari, Koishi) via GLEntities
                 └─ RumiaEntity (RumiaStateMachine, EX form, 1.7x0.4 fall dims)
```

Character side:

```
PlayerCapabilityTemplate<CharacterAttachment>  (GLMeta.CHAR, PlayerCapabilityNetworkHandler)
 └─ CharacterAttachment: Map<EntityType<?>, CharacterData>
     └─ CharacterData: reputation(int) + FeedModuleData
         └─ ReputationState → TargetKind
             └─ CharDataHolder (facade) → CharDataToClient packet → client replace()
```

## 3. Workflows

### 3.1 Creation

```java
// via BedRespawn (see structure_bed_entity.md)
CharacterConfig cfg = CharacterConfig.of(GLEntities.REIMU.get());
YoukaiEntity e = cfg.create(type, sl, bedPos, key);
// inside create():
//   type.create(sl) → youkai
//   IHomeHolder.of(sl,key).getWanderCenter() → center
//   youkai.setPos(center)
//   HomeModule.setHome(key)
//   youkai.initSpellCard()  // TouhouSpellCards
//   youkai.restrictTo(center, home.getWanderBaseRadius() + cfg.wanderRadius())
//   sl.addFreshEntity(youkai)

// or natural spawn:
SpawnGroupData finalizeSpawn(level, difficulty, reason, data) → initSpellCard()
```

Attributes via `GLEntities` (`L2Registrate`):
- `GeneralYoukaiEntity.createAttributes()` `0.3 move, 0.4 fly, 48→128 follow`.
- Boss overrides `200 HP`, `YoukaiFeatureSet.BOSS`.

### 3.2 Server Tick

```java
// Entity.tick() chain:
DamageClampEntity.tick() // hurtCD++, speed clamp, bossEvent progress, effectImmune clear
YoukaiEntity.tick()      // combatManager.tick()
customServerAiStep():
  targets.tick()         // prune invalid, promote lastHurtByMob/target
  tickTargeting()        // heal if noTarget+noTargetHealing, bossBar visibility, discard if noPlayerVanishTime
  tickSpell()            // cardHolder.tick if isAggressive && shouldShowSpellCircle else reset
  navCtrl.tickMove()     // flying combat, gravity factor 0.6/0.8, debugger
  modules.tickServer()   // HomeModule keeps BedRefData alive, Gift/Feed cooldown, Talk validation
SmartYoukaiEntity.customServerAiStep() extends:
  getBrain().tick(sl, this) // SmartBrain: forgetOutdated → tickSensors → updateActivities → startEachNonRunning → tickEachRunning
```

`tickTargeting` detail `YoukaiEntity.java:329`:
- `noTargetTime++` if `getTarget()==null`; `doHeal` when `noTargetHealing && noTargetTime>=20 && tick%20==0` or `progress<maxHealth`; suppressed if `lastHurtByMob==creative Player` within 100 ticks.
- BossBar visible only when `noTargetTime==0` and `hasBossBar`, hidden after 40 ticks.
- Discard after `noPlayerTime>noPlayerDiscardTime` (MAIDEN 30 ticks worth of 20-tick checks ≈30s) when `level.getNearestPlayer(this,32)==null`.

### 3.3 Brain Construction

```java
// lazy in SmartYoukaiEntity.checkBoard():
board = new TaskBoard();
constructTaskBoard(board); // subclass hooks addFightTasks + sensors
board.build(); // sorts exclusives, injects FIGHT prioritized(0)

// makeBrain(Dynamic dynamic) called by EntityType:
SmartBrain.construct(board, dynamic)
//   Provider: Brain.codec(board.memories(), board.getSensors().map(SensorType::new))
//   board.buildBrain(brain):
//     for each Activity in [CORE] + priorities(sorted) + scheduled(Integer.MAX_VALUE)
//       behaviors = fetch(activity) // GateBehavior ORDERED for exclusives, SHUFFLED for random
//       current = [previous prioritized memories as VALUE_ABSENT] + [entry.memory as VALUE_PRESENT]
//       brain.addActivityWithConditions(act, behaviors, current)
//     brain.setPriorityActivities(priorityList)
//     brain.setSchedule(schedule)

// per-tick SmartBrain.tick():
//   forgetOutdatedMemories
//   tickSensors (NearbyPlayerSensor radius 32/32 every 5t, NearbyLiving 32/16 10/20t, YoukaiUpdateHomeSensor 80t → HOME)
//   updateActivities: iterate priorityActivities first, if meets VALUE_* then setActiveActivity else schedule.getActivityAt(dayTime)
//   startEachNonRunningBehavior / tickEachRunningBehavior
```

Default `constructTaskBoard` graph (`SmartYoukaiEntity.java:72`):

| Behaviour | Activity | Kind |
|---|---|---|
| `YoukaiLookAtTarget`, `YoukaiMoveTask`, `YoukaiSwimTask`, `YoukaiSmartDoorTask` | CORE | always |
| `YoukaiUpdateTargetTask`, `YoukaiAttackTask(16)`, `StrafeTarget` | FIGHT | always |
| `YoukaiFetchTargetTask` | TALK/AT_HOME/REST | always |
| `YoukaiSearchTargetTask`, `YouKaiVanishTask` | IDLE/PLAY | always |
| `YoukaiSleepTask` | REST p0 | exclusive |
| `YoukaiTalkTask` | TALK p0 | exclusive |
| `YoukaiGoHomeTask` | IDLE/AT_HOME p100 | exclusive |
| `YoukaiRepairHouseTask` | AT_HOME p200 | exclusive |
| `SetEntityLookTarget(player 32)`, `SetEntityLookTarget(24)` | IDLE/PLAY p1100/1200 | exclusive |
| `RandomStroll(0.8)`, `YoukaiStayInRoom`, `YoukaiStayNearHouse`, `YoukaiSitTask`, `DoNothing` | IDLE/PLAY/AT_HOME | random (Gate SHUFFLED/RUN_ONE) |

Schedule: `AT_HOME@10 → IDLE@2000 → PLAY@4000 → IDLE@8000 → AT_HOME@10000 → REST@12000`.

### 3.4 Damage & Reputation

```java
hurt(DamageSource, amount):
  if shouldIgnore(le) → invulnerable (via isInvulnerableTo)
  check YoukaiFightEvent cancel
  if getCD(source)>0 && hurtCD<cd → return false
  super.hurt()
    actuallyHurt():
      spellCard.hurt(cardHolder, source, amount) // card damage handling
      CharDataHolder.get(player,this).onHurt(source, amount) // if non-danmaku → loseReputation
      actuallyHurtImpl():
        clampDamage: limit maxHealth/limiter, nonDanmakuProtection divides, damageFilter
        hurtFinal → combatProgress.set() → CombatToClient to tracking players

die(source):
  modules.onKilled() // HomeModule clears BedRefData.entityId
  if sourceEntity instanceof LivingEntity le → onKilledBy(le) → CharDataHolder.onKillCharacter() lose 200 to MIN(-300)

killedEntity(level, entity):
  CharDataHolder.get(entity,this).onKilledByCharacter() // gain 100 to -50
```

`CharacterData.onHurtCharacter` `CharacterData.java:56` (non-danmaku only):
- `first = !targets.contains(player) && getLastHurtByMob()!=player`
- `first && damage<=4`: `-1` if rep>=100 else `-5` if rep>=0 else `-10` (cap -100 or 0)
- else: `-5` if first&&rep>=100 else `-10` if rep>=0 else `-20` (cap -150 or 0)

`CombatToClient` sync via `GensokyoLegacy.HANDLER.toTrackingPlayers` on every `CombatProgress.set`.

### 3.5 Interaction (Modules)

`mobInteract` iterates `HomeModule → GiftModule → FeedModule → TalkModule` (order in `createModules()`), first non-PASS wins `YoukaiEntity.java:216`.

- **TalkModule**: requires `mayInteract` (`!isHostileTo && !isTalking && activity!=REST/FIGHT`), `reputation!=ENEMY`, empty hand. Server: `setTalkTo(sp,-1)` → `BrainUtils.setMemory(MEM_TALK, player)` + `FirstDialogProvider.open`. Client validates `distance<=5` + still `ITalkMenu`.
- **FeedModule**: `@SerialField feedCoolDown`. `getFavor(FoodProperties) = nutrition - poison penalties + beneficial`, capped 10. Only if `feedCoolDown==0 && favor>=0 && food != empty`. Server: `shrink(1)`, `coolDown += nutrition*100`, `CharDataHolder.feed(stack, favor)` → `foodData.feed()` multiplier + `gainReputation(round(val*favor), MAX)`, `setTalkTo(sp,-1)` auto-talk.
- **GiftModule**: via `GLMeta.GIFT_DATA` datamap `GiftData.getFavor(stack, self)` + `cooldown()`. On success `gain(favor, MAX)` + shrink + `GIFTED` flag + `HEART` event + levelup sound. Takes priority over Feed (Gift before Feed in list).
- **HomeModule**: not interactable; storage only.

Flags `YoukaiFlags` bitmask in `DATA_FLAGS_ID`: `CHARGING, FAINTED, POWERED, FED, GIFTED, FLYING`.

### 3.6 Persistence & Networking

```java
addAdditionalSaveData(CompoundTag tag):
  super (DamageRefactorEntity: CombatProgress via TagCodec)
  tag.putInt("Age", tickCount)
  tag.put("auto-serial", TagCodec.toTag(this)) // @SerialField targets, spellCard, noTargetTime, noPlayerTime
  if(hasRestriction()) tag.put("Restrict", TagCodec.valueToTag(RestrictData))
  data().write(registry, tag, entityData) // YOUKAI_DATA + General SPELL_MODEL
  tag.put("YoukaiModules", { moduleId→TagCodec.toTag(module) }) // HomeModule.home etc.

readAdditionalSaveData: reverse; modules fromTag; if getTarget==null → navCtrl.setWalking()

Brain not serialized directly: SmartBrain.Provider stores Codec<Brain<E>> via Dynamic; survives chunk save via vanilla brain NBT.

CharacterAttachment:
  Storage: PlayerCapabilityTemplate via AttReg (l2core), LinkedHashMap<EntityType,CharacterData> @SerialClass
  NBT: player NBT, copyOnDeath handled by cap
  Sync: CharDataHolder.sync() → if ServerPlayer → HANDLER.toClientPlayer(new CharDataToClient(type, uuid, data))
  Client: CharDataToClient.handle → GLMeta.CHAR.type().getOrCreate(player).replace(target,data)
  Tick: CharacterAttachment.tick(Player) server only at dayTime==0 → dailyUpdate: lose 1 to 150, gain 1 to -150
```

### 3.7 Navigation

`YoukaiNavigationControl(self)` owns `Ground walkNav (YoukaiWalkNodeEvaluator)` + `Flying flyNav (YoukaiFlyNodeEvaluator)` + `CombatFlyingControl` + `Ground walkCtrl / FlyingCtrl`. `tickMove()` handles falling multiplier + combat flying strafe when `isAggressive && isFlying`. `moveTo(CompoundPath, speed)` picks nav by `isFlying || tempFly`. `GroundPathNavigation.moveTo()` falls back to flying if unreachable/stuck and `mayFly()`.

## 4. Usage Guide

### Register a new Youkai

```java
// in GLEntities.java
public static final EntityEntry<GeneralYoukaiEntity> MY_YOUKAI = REGISTRATE
    .entity("my_youkai", GeneralYoukaiEntity::new, MobCategory.CREATURE)
    .properties(e -> e.sized(0.6f, 1.8f).clientTrackingRange(10))
    .attributes(GeneralYoukaiEntity::createAttributes)
    .renderer(() -> GeneralYoukaiRenderer::new)
    .spawnEgg(0xFF0000, 0x00FF00)
    .defaultLang().loot().tag(GLEntities.YOUKAI).register();

// model/spell binding via GLMeta or TouhouSpellCards: set SPELL + SYNC model string
```

### Add a character-specific behaviour

```java
public class MyEntity extends GeneralYoukaiEntity {
    public MyEntity(EntityType<? extends YoukaiEntity> type, Level level) { super(type, level); }

    @Override protected YoukaiCombatManager createCombatManager() {
        return new MyCombatManager(this, MySpellCards.MY_CIRCLE);
    }
    @Override protected void constructTaskBoard(TaskBoard board) {
        super.constructTaskBoard(board);
        board.addSensor(new TemptingSensor<>(...)); // vanilla sensor wrapper
        board.addExclusive(50, new MyCustomTask<>(), GLBrains.AT_HOME.get());
    }
    @Override protected List<AbstractYoukaiModule> createModules() {
        return List.of(new HomeModule(this), new MyGiftModule(this), new FeedModule(this), new TalkModule(this));
    }
}
```

### Reputation usage

```java
// query
ReputationState state = youkai.getReputation(player); // STRANGER if unknown type
if (state == ReputationState.FRIEND) { /* quest */ }

// mutate via CharDataHolder (auto-sync)
CharDataHolder.get(player, youkai).gain(10); // gainReputation(10, MAX)
CharDataHolder.getUnbounded(player, GLEntities.REIMU.get()).onHurt(...);

// direct
CharacterAttachment att = GLMeta.CHAR.type().getOrCreate(player);
CharacterData data = att.get(player, youkai).data();
data.reputation // -300..300
```

### Add a new Module

```java
@SerialClass
public class MyModule extends AbstractYoukaiModule {
    public static final ResourceLocation ID = GensokyoLegacy.loc("my_module");
    @SerialField private int cooldown;
    public MyModule(YoukaiEntity self) { super(ID, self); }
    @Override public InteractionResult interact(Player p, InteractionHand h) { return InteractionResult.PASS; }
    @Override public void tickServer() { if(cooldown>0) cooldown--; }
}
```

### Extend TaskBoard

```java
board.addAlways(new YoukaiLookAtTarget(40,300), Activity.CORE);
board.addExclusive(100, new YoukaiGoHomeTask<>(), Activity.IDLE, GLBrains.AT_HOME.get());
board.addRandom(new YoukaiStayInRoomTask<>().speedModifier(0.8f), GLBrains.AT_HOME.get());
board.addSensor(new YoukaiUpdateHomeSensor<>());
board.addScheduledActivity(Activity.REST, MemoryModuleType.HOME);
board.addPrioritizedActivity(GLBrains.TALK.get(), GLBrains.MEM_TALK.get(), 100);
board.setSchedule(new ScheduleBuilder(new Schedule()).changeActivityAt(12000, Activity.REST).build());
board.build(); // must be last
```

## 5. Performance Notes

- `CharacterAttachment.tick` iterates `LinkedHashMap` once per player at dawn only (not per-tick hot).
- `tickTargeting` nearestPlayer scan `level.getNearestPlayer(this,32)` runs at most once per 20 ticks and only when `noTargetTime>100`.
- Sensors throttled: `NearbyPlayerSensor` every 5t, `NearbyLiving` 10/20t adaptive, `YoukaiUpdateHomeSensor` 80t.
- `SmartBrain.tick` is per-youkai server tick; keep `TaskBoard` behaviours cheap; avoid `level.getEntities` inside behaviours — use sensor memories `NEAREST_PLAYERS/MEM_PREY/MEM_ITEMS`.

