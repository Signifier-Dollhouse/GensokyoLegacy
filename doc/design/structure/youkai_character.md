# YoukaiEntity and CharacterData — Implementation & Usage Guide

This document reflects the current implementation in `content/entity/youkai/` (base entities, plus `facet/` and `renderer/`), `content/entity/foundation/`, `content/attachment/character/`, `content/entity/behavior/` (brain/sensor/task/move/combat, plus `module/` and `visit/`) and `init/registrate/`.

Related: [home_index.md](home_index.md), [structure_bed_entity.md](structure_bed_entity.md), [reputation.md](reputation.md), [character_visit.md](character_visit.md), [party/](party/).

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
| `CharacterData` | `content/attachment/character/CharacterData.java:11` | `@SerialClass` per-character (`EntityType` key). `@SerialField int reputation` **plus `int reputationCap`** (quests raise the ceiling; simple actions never pass it), `Map<String,Long> timers` (per-action cooldowns via `isAvailable`/`setTimer`), `@SerialField FeedModuleData foodData`. `gainReputation(val, softCap, capIncrease, maxCap)` halves the gain past `softCap`; `loseReputation` floors at `-150`. Design: `reputation.md`. |
| `ReputationConstants` | `content/attachment/character/ReputationConstants.java` | Every tuning number in one place: bounds `-300..300`, `INITIAL_CAP 100`, thresholds `150/-50/-150`, `KILLED_RESTORE 0`, `COMBAT_SAFE_THRESHOLD 0`, `DEATH_LOSS 200`, hurt losses `1/5/10/20`, `FEED_SOFT_CAP`/`GIFT_SOFT_CAP 150`, `DAILY_DECAY_AMOUNT 1`. |
| `CharacterAttachment` | `content/attachment/character/CharacterAttachment.java:15` | `PlayerCapabilityTemplate<CharacterAttachment>` (`l2core AttReg`). `@SerialField Map<EntityType<?>,CharacterData>` (LinkedHashMap). `tick` at `dayTime==0` calls `dailyUpdate` on every entry. `allTypes`/`reset`/`clearAll` for the quest and admin surfaces. |
| `CharDataHolder` | `content/attachment/character/CharDataHolder.java` | `record(CharacterData data, Player player, EntityType<?> type, @Nullable YoukaiEntity e)` — transient facade. Every mutator (`onHurt`/`onKilledByCharacter`/`onKillCharacter`/`feed`/`gain`) syncs afterwards, so callers never sync by hand. |
| `ReputationState` | `content/attachment/character/ReputationState.java:15` | `FRIEND/STRANGER/JERK/ENEMY`. `toInfo(reputation, cap)` colored text, `asTargetKind()` → `TargetKind.WORTHY/NONE/ENEMY`. |
| `FeedModuleData` | `content/attachment/character/FeedModuleData.java` | `@SerialClass Map<Item,Integer> statistics` + `List<Item> history` (MEMORY 10, RATE 0.9). `feed()` returns diminishing multiplier `1/(1+sum(0.9^n))`. |
| `CharacterConfig` | `content/attachment/character/CharacterConfig.java` | Datamap `DataMap<EntityType, CharacterConfig>`. Fields `structure RL, discardTime, respawnTime, wanderRadius, noPlayerVanishTime`. `create()` spawns `YoukaiEntity` at the bed, sets `HomeModule.home` and restricts it to the home's wander radius. |
| `TaskBoard` | `content/entity/behavior/brain/TaskBoard.java:28` | DSL for brain graph: `always / exclusive(priority) / random` behaviors, `scheduled / prioritized` activities, `sensors`, `schedule`, plus `addPartyTasks()` for the shared guest behaviour. |
| `SmartBrain` | `content/entity/behavior/brain/SmartBrain.java` | `Brain<E>` subclass. `construct(board,dynamic)` via `Provider` codec `Brain.codec(memoryTypes,sensorTypes)`. Priority scan `updateActivities`. |
| `GLBrains` | `init/registrate/GLBrains.java:15` | `SR`-registered sensors `SN_HOME/SN_HUNT/SN_ITEM/SN_LE/SN_PLAYER/SN_HOME_BLOCKS`; memories `MEM_PATH/MEM_PREY/MEM_DOWN/MEM_TALK/MEM_ITEMS/MEM_FEED/MEM_VISIT/MEM_PARTY/MEM_FEAR/MEM_NAP`; activities `AT_HOME/HUNT(700)/DOWN(-100)/TALK(300)/VISITING(600)/PARTY(400)/FEAR(100)/FEAST(200)/NAP(500)`. The priority scale is documented as a block above the activity constants. |
| `YoukaiModuleHolder` | `content/entity/behavior/module/YoukaiModuleHolder.java` | `List<AbstractYoukaiModule>` + `Map<Class,Optional>` cache + `hasPickup` flag. Iterable. |
| `HomeModule` | `content/entity/behavior/module/HomeModule.java` | `@SerialField StructureKey home` (immovable — structures/BEs never move, so no re-binding needed). `tickServer → BedRefData.entityTick`, `onKilled → onEntityDie`. |
| `VisitModule` | `content/entity/behavior/module/VisitModule.java` | Drives `YoukaiFlags.VISITING` and the `MEM_VISIT` heartbeat; a visitor has no `HomeModule` binding, so she can neither sleep nor go home. See `character_visit.md`. |
| `PartyModule` | `content/entity/behavior/module/PartyModule.java` | Drives `YoukaiFlags.PARTYING` and re-asserts `MEM_PARTY` every tick (that memory has no codec, so it does not survive a save). See `../party`. |
| `YoukaiTargetContainer` | `content/entity/behavior/combat/YoukaiTargetContainer.java` | `@SerialField LinkedHashSet<UUID> list` (maxSize 10/8). Maintains hostile UUIDs; `tick()` promotes `lastHurtByMob`/target. |

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
                 │   │   └─ Marisa, Reimu, Sakuya
                 │   ├─ MorichikaEntity, Mystia, etc.
                 │   ├─ BossYoukaiEntity → SuikaEntity (guest-only, no spell card)
                 │   └─ Boss entries (Yukari, Koishi) via GLEntities
                 └─ RumiaEntity (RumiaStateMachine, EX form, 1.7x0.4 fall dims)
```

Character side:

```
PlayerCapabilityTemplate<CharacterAttachment>  (GLMeta.CHAR, PlayerCapabilityNetworkHandler)
 └─ CharacterAttachment: Map<EntityType<?>, CharacterData>
     └─ CharacterData: reputation(int) + reputationCap(int) + timers + FeedModuleData
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
//   IHomeHolder.of(sl,key).getWanderCenter() → center (null → give up, no spawn)
//   youkai.setPos(bedPos)          // wakes up in her own bed
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
  modules.tickServer()   // HomeModule keeps BedRefData alive, Visit/Party heartbeat, Talk validation
SmartYoukaiEntity.customServerAiStep() extends:
  getBrain().tick(sl, this) // SmartBrain: forgetOutdated → tickSensors → updateActivities → startEachNonRunning → tickEachRunning
  stale-sleep watchdog: isSleeping() && getActivity()!=REST → stopSleeping()
```

`tickTargeting` detail `YoukaiEntity.java:420`:
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

Default `constructTaskBoard` graph (`SmartYoukaiEntity.java:95`):

| Behaviour | Activity | Kind |
|---|---|---|
| `YoukaiLookAtTarget(40,300)`, `YoukaiMoveTask`, `YoukaiSwimTask(0.8)`, `YoukaiSmartDoorTask` | CORE | always |
| `YoukaiUpdateTargetTask`, `YoukaiAttackTask(16)`, `StrafeTarget` | FIGHT | always |
| `YoukaiAcquireTargetTask` | TALK/AT_HOME/REST/PARTY/IDLE/PLAY/VISITING | always |
| `YoukaiVanishTask` | IDLE/PLAY | always |
| `YoukaiSleepTask` | REST p0 | exclusive |
| `YoukaiTalkTask` | TALK p0 | exclusive |
| `YoukaiGoHomeTask` | IDLE/AT_HOME p100 | exclusive |
| `YoukaiRepairHouseTask` | AT_HOME p200 | exclusive |
| `SetEntityLookTarget(player 32)` | IDLE/PLAY/AT_HOME/VISITING p1100 | exclusive |
| `SetEntityLookTarget(24)` | IDLE/PLAY p1200 | exclusive |
| `RandomStroll(0.8)` | IDLE/PLAY, and VISITING | random (Gate SHUFFLED/RUN_ONE) |
| `YoukaiStayInRoomTask`, `YoukaiSitTask(100,200)` | AT_HOME | random |
| `YoukaiStayNearHouseTask`, `YoukaiDoNothing(30,60)` | IDLE | random |
| `YoukaiDoNothing(30,60)` | IDLE/PLAY/AT_HOME, and VISITING | random |
| `board.addPartyTasks()` | PARTY | shared guest set |
| `YoukaiNapTask` | NAP p0 | exclusive; Suika only, added by her `constructTaskBoard` |

Sensors: `NearbyPlayerSensor(32,32)` every 5t, `NearbyLivingEntitySensor(32,16)` 10/20t adaptive, `YoukaiUpdateHomeSensor` 80t → HOME, `YoukaiHomeBlocksSensor(CHAIR)`.

Schedule: `AT_HOME@10 → IDLE@2000 → PLAY@4000 → IDLE@8000 → AT_HOME@10000 → REST@12000`.

Priority scale (lower wins), 100 apart, documented in full as a block in `GLBrains`:

`DOWN -100 < FIGHT 0 < FEAR 100 < FEAST 200 < TALK 300 < PARTY 400 < NAP 500 < VISITING 600 < HUNT 700 < scheduled`

The rungs are deliberate — `TaskBoard` injects a prioritized activity's memory as `VALUE_ABSENT` into every less prioritized activity, so a visitor keeps the ability to talk and trade, a party guest additionally never sleeps or goes home and never wanders off to visit, a visitor never hunts, and a napping character is still talked to. The gaps are 100 apart only so there is always an open hundred to slot a new activity into.

`NAP(500)` is Suika's alone, and is the shape to copy for a new self-terminating activity: `MEM_NAP` is written with an expiry by `YoukaiDoNothing` (rolled from `SmartYoukaiEntity#napChance`, which every other character returns 0 for), the brain moves her into NAP on the next tick, and `YoukaiNapTask` holds the `侧卧` pose until the memory runs out or something above 500 takes over.

A rung is about the activity, not about the body. TALK(300) above NAP(500) means a conversation is what *runs* while she is lying there — the dialog, the topics, the trade — while `YoukaiNapTask#canStillUse` deliberately accepts TALK as well as NAP so the pose survives it. She answers in `侧卧_交流`, and if the conversation outlives the nap's memory she stays down three more seconds (`WAKE_UP_TICKS`) rather than standing up the instant the dialogue closes. That write has to live in `canStillUse` and not in `tick`: `Behavior#tickOrStop` checks `canStillUse` first, so a re-arm in `tick` would land one tick after she was already told to stand. Compare `FairyFleeTask`, which is the other half of the trade — there the activity is what has to be noticed.

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
      if vanishOnDislike() && reputation < DISCARD_REP_THRESHOLD → discard + tell the player
      actuallyHurtImpl():
        clampDamage: limit maxHealth/limiter, nonDanmakuProtection divides, damageFilter
        hurtFinal → combatProgress.set() → CombatToClient to tracking players

die(source):
  modules.onKilled() // HomeModule clears BedRefData.entityId
  if sourceEntity instanceof LivingEntity le → onKilledBy(le) → CharDataHolder.onKillCharacter() // -200, floored at MIN -300

killedEntity(level, entity):
  CharDataHolder.get(entity,this).onKilledByCharacter() // forgiveness: jumps straight up to KILLED_RESTORE (0)
```

`CharacterData.onHurtCharacter` (`CharacterData.java:102`, danmaku exempt, every loss floored at `HURT_LOSS_FLOOR -150`):
- `first = !targets.contains(player) && getLastHurtByMob()!=player`
- `first && damage<=4 && reputation>=100` → `-1` (one light tap on someone already friendly)
- `first` otherwise → `-5`, at any reputation
- repeated while still a STRANGER or better (`reputation>=-50`) → `-10`
- repeated below `-50` → `-20`

Daily decay: `dailyUpdate()` moves `DAILY_DECAY_AMOUNT 1` toward `reputationCap * GLModConfig.SERVER.reputationDecayFloor` when positive, and back up toward 0 when below `THRESHOLD_JERK`. Raising the cap via quests raises the floor with it.

`CombatToClient` sync via `GensokyoLegacy.HANDLER.toTrackingPlayers` on every `CombatProgress.set`.

### 3.5 Interaction (Modules)

`mobInteract` iterates `HomeModule → VisitModule → PartyModule → TalkModule` (order in `createModules()`, `YoukaiEntity.java:273`), first non-PASS wins.

- **TalkModule**: requires `mayInteract` (`!isHostileTo && !isTalking && activity!=REST/FIGHT`), `reputation!=ENEMY`, empty hand. `beginTalking(sp)` is the single entry point for every conversation start (talking, feeding, gifting): it records `talkTarget` then calls `setTalkTo(sp,-1)` → `BrainUtils.setMemory(MEM_TALK, player)` + `FirstDialogSession.open`. Server validates `distance<=5` + that the player still has a talk UI: either a live `DialogSession` or a container menu implementing `ITalkMenu` (the trade screen, which takes over from a dialog).
- **HomeModule / VisitModule / PartyModule**: not interactable; they are state, not verbs.
- **FeedModule / GiftModule**: still implemented and still carry their own `interact`, but both are currently commented out of `createModules()` while the party/visit work settles, so neither is reachable in game. Feed was `gainReputation(round(rate*favor), FEED_SOFT_CAP, 0, 0)` + auto-talk; Gift used the `GLMeta.GIFT_DATA` datamap, set `GIFTED`, and auto-talked. Re-enabling them means putting them back in the `createModules()` list.

Flags `YoukaiFlags` bitmask in `DATA_FLAGS_ID`, append-only (ordinal is the bit index): `CHARGING, FAINTED, POWERED, NONE, FED, GIFTED, FLYING, VISITING, FLEEING, PARTYING`.

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
  Tick: CharacterAttachment.tick(Player) server only at dayTime==0 → dailyUpdate: drift 1 toward
        reputationCap*GLModConfig.SERVER.reputationDecayFloor, or back toward 0 when below -150
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
        // keep the base order; feed/gift are commented out upstream, add them back there
        return List.of(new HomeModule(this), new VisitModule(this), new PartyModule(this), new TalkModule(this));
    }
}
```

### Reputation usage

```java
// query
ReputationState state = CharacterData.getState(data.reputation);
if (state == ReputationState.FRIEND) { /* quest */ }

// mutate via CharDataHolder (auto-sync)
CharDataHolder.get(player, youkai).gain(10, 150, 0, 0);        // +10, halved past 150, cap untouched
CharDataHolder.get(player, youkai).gain(10, 150, 25, 300);    // quest reward: also raises the cap by 25
CharDataHolder.getUnbounded(player, GLEntities.REIMU.get()).onHurt(...);

// direct
CharacterAttachment att = GLMeta.CHAR.type().getOrCreate(player);
CharacterData data = att.get(player, youkai).data();
data.reputation     // -300..300
data.reputationCap  // ceiling quests have lifted this relationship to
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

