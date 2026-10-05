# Character Visit

A character who does not live in a structure may turn up there for a couple of
minutes during the day. She is a **visitor**: no bed, no sleep, a different
greeting, no new quests — but she can still hand over finished quests and trade.

## 1. What a visitor is

A visitor is a **temporary, homeless clone** of a character, spawned inside
another character's home.

| | resident | visitor |
|---|---|---|
| `HomeModule.home` | its own `StructureKey` | **null** |
| `MemoryModuleType.HOME` | set by `YoukaiUpdateHomeSensor` | never set |
| `Activity.REST` / `AT_HOME` | reachable | unreachable (they require `HOME`) |
| `BedRefData` slot | claimed by its UUID | never touched |
| lifetime | until death | the visit window, then discard |
| reputation | `CharacterAttachment[EntityType]` | **the same entry** — she is the same person |

Spawning a clone rather than teleporting the resident matters: the resident back
home keeps living its day, keeps its bed binding, and cannot be yanked out from
under a player who came to the shrine.

Not setting `HomeModule.home` is what makes a visitor un-bed-bound. Nothing else
has to know about it — the whole sleep/bed subsystem is already gated on `HOME`.

## 2. The visit window

The visitor's lifetime is **not stored**. It is a pure function of
`(structure root pos, guest entity type, game day)`, evaluated the same way by
the spawner and by the visitor itself, so both always agree.

```java
WINDOW = 12000                       // day ticks 0..12000

seed  = mix(key.pos().asLong(), id(guest), day)
rand  = RandomSource.create(seed)
stay  = chanceGate(rand) ? minStay..maxStay : none     // scaled by the global multiplier
start = rand.nextInt(WINDOW - stay + 1)                // 0 .. 12000-stay

remaining(dayTime) = start <= dayTime%24000 < start+stay
                        ? start + stay - dayTime%24000
                        : 0
```

* A 4000-tick visit starts somewhere in `0..8000`.
* `WINDOW` closes exactly where the default schedule switches to
  `Activity.REST` (`SmartYoukaiEntity:124`), so a guest is never on site while
  her host is asleep.
* The implied frequency of a guest is `chance × avgStay / WINDOW`, so `chance`
  is the authored knob and `minStay`/`maxStay` the length knob.

Because nothing is stored there is no bookkeeping to get wrong across day
rollover, `/time set`, chunk unload/reload, or `/reload`. The cost is that a
window can shift if the game version ever changes `RandomSource.create`'s
algorithm — acceptable, and why the seed is pre-mixed here rather than left to
`BlockPos.asLong` alone.

The `gap` dimension is excluded implicitly: its `getDayTime` is pinned at 18000,
past `WINDOW`, so `remaining` is always 0. Visits are a surface-world daytime
event.

### 2.1 Expiry

```java
discard when remaining == 0
          &&  noDisappear == 0        // 400t, granted by a conversation
          &&  !seenByAnyPlayer()      // within 32 blocks *and* in line of sight
```

Neither hold extends the other. A visitor never vanishes in front of a player,
and a conversation buys 20 seconds of grace — enough to finish a trade, not
enough to keep her around indefinitely. There is no farewell dialog.

Sight matters more than proximity here: a player on the other side of the house
cannot see her, so she may as well go. Standing around for someone who cannot
observe her just leaves her in the room longer than intended.

### 2.2 Two modes

`VisitModule` runs in one of two modes, both stored in the same three fields:

| mode | set by | expiry |
|---|---|---|
| **natural** | `VisitScheduler` from the bed tick | the table window |
| **forced** | `/gensokyo visit <character>` | an explicit `forcedUntil` |

A forced visit is what makes the feature testable without waiting a whole day for
a 30% roll.

## 3. Data

### 3.1 The guest pool lives in `StructureConfig`

Each structure declares who may visit it. The pool is keyed by **structure type**
(the datamap is on `Registries.STRUCTURE`) but every visit is drawn from a seed
that includes the **structure's instance position**, so two shrines never share
a schedule.

```jsonc
// data_maps/worldgen/structure/structure_config.json
"gensokyolegacy:hakurei_shrine": {
  "entities": ["gensokyolegacy:hakurei_reimu"],     // residents, unchanged
  "visitors": {                                     // ← new
    "gensokyolegacy:kirisame_marisa": { "chance": 0.2, "min_stay": 2400, "max_stay": 3600 }
  }
}
```

`StructureConfig.CharacterVisit(float chance, int minStay, int maxStay)`. Absent
key ⇒ the guest never visits. Authored in `StructStructure` so
`GLStructureGen` declares it the same way it declares beds and rooms.

`chance` is a **daily gate**, not a probability at spawn time: some days the
guest has no window at all. That keeps the whole thing a pure function of the
seed while still letting content state "30% of days" exactly.

`GLModConfig.SERVER.visitStayMultiplier` (default `1.0`) scales `minStay`/
`maxStay`, so a pack or server can make everyone visit more or less often
without touching the structure data.

Custom (wand-built) homes get no visitors: the pool is keyed by
`Registries.STRUCTURE` and `custom_structure` has no entry.

### 3.2 `VisitModule`

`@SerialField StructureKey host`, `int noDisappear`, `boolean forced`,
`long forcedUntil`. Nothing else — no history, no counters, no trace of a past
visit.

`YoukaiFlags.VISITING` is appended to the flag bitfield so the **client** can
read the state from `DATA_FLAGS_ID` with no new packet. That is only needed for
the visit greeting (§4).

### 3.3 Spawn side

`VisitScheduler.tick` is called from `StructureRefData.blockTick`, which already
de-duplicates per game time via `structureTick` — so the scheduler is
chunk-load-aware for free and needs no tick system of its own.

Every second, per structure, for each guest with `remaining > 0`:
look for an existing visitor of that type whose `host` is this key, and spawn one
if there is none. Independence means no guest blocks another.

Spawn position is `IHomeHolder.getRandomPosInBound` — random inside the total
structure bound, on the ground (`RandomPos.generateRandomPos` +
`LandRandomPos.movePosUpOutOfSolid`). `restrictTo` is the host's wander centre
and radius, so the visitor stays on the premises.

## 4. Content gating

`DialogStarter`, `Quest` and `TradeOffer` are all `GatedEntry` and all keyed by
`EntityType`, and the entity type does not change when a character becomes a
visitor. So visit-specific content needs **no new registry and no new handle
type** — only one new condition.

```java
// content/rpg/condition/HomeBoundCondition.java
public record HomeBoundCondition(boolean invert) implements QuestCondition<HomeBoundCondition> { ... }
```

| JSON | meaning |
|---|---|
| `{"type":"gensokyolegacy:home_bound"}` | home-bound characters only |
| `{"type":"gensokyolegacy:home_bound","invert":true}` | visiting characters only |

`invert` follows the existing `HasAdvancementCondition(invert)` /
`TimerCondition(invert)` precedent, so one condition covers both directions.
Datagen helpers: `homeBound()` and `visiting()`.

The **greeting line** (`DialogConfig.greeting`, rendered by
`FirstDialogScreen.getBodyText`) needs its own hook, because it is a body string
rather than a gated entry: `DialogConfig` gains a `visitGreeting` component and
the screen picks it when the `VISITING` flag is set.

### 4.1 Hard rules

A visitor can still talk, trade, feed, gift, hand over a finished quest and pick
up a follow-up. It can **not** start a new quest — a hard system rule in two
places, not per-quest opt-in:

* `ServerCharacterDialogManager.getInitialConversation` never offers a
  `QuestHandle` of `Kind.START` for a visiting character;
* `StartQuestAction.execute` no-ops for a visiting character, in case a click
  races the visit ending.

Reputation is shared with the resident: `CharacterAttachment` is keyed by
`EntityType`, and they are the same person. Visitors fight back and can be hurt
and killed like anyone else.

## 5. Brain

```java
board.addPrioritizedActivity(GLBrains.VISITING.get(), GLBrains.MEM_VISIT.get(), 150);
```

Priority 150 is load-bearing. `TaskBoard.buildBrain` walks
`[CORE] + priorities + scheduled` and injects each prioritized activity's memory
as `VALUE_ABSENT` into every **later** entry. With `FIGHT(0) < TALK(100) <
VISITING(150)`:

* `TALK` is walked *before* `VISITING`, so its requirements never mention
  `MEM_VISIT` — **a visitor can still talk and trade**;
* `VISITING` requires `MEM_TALK` absent, so it never fights the talk task;
* every scheduled activity requires `MEM_VISIT` absent — **a visitor can never
  sleep or go home**.

`YoukaiVanishTask` is registered only under `IDLE`/`PLAY`, so a visitor never
discards itself early and no change is needed there.

## 6. Files

**New**

| File | Role |
|---|---|
| `content/entity/visit/VisitTable.java` | `WINDOW`, seed, `remaining(sl, key, guest)` |
| `content/entity/visit/VisitScheduler.java` | bed-tick side: dedup scan + spawn |
| `content/entity/module/VisitModule.java` | per-entity visit lifetime and holds |
| `content/rpg/condition/HomeBoundCondition.java` | the `home_bound` condition |
| `init/data/rpg/AliceQDGen.java` | minimal content so Alice can be a guest |

**Modified**

| File | Change |
|---|---|
| `content/attachment/datamap/StructureConfig.java` | `visitors` + `CharacterVisit` + `visitOf` + builder |
| `content/attachment/home/structure/StructureHomeHolder.java` | synthetic fallback constructor |
| `content/attachment/index/StructureRefData.java` | expose `structureTick()`, call the scheduler |
| `content/attachment/datamap/DialogConfig.java` | `visitGreeting` component |
| `content/entity/youkai/YoukaiFlags.java` | append `VISITING` |
| `content/entity/youkai/YoukaiEntity.java` | `createModules()` += `VisitModule`, `isVisiting()` |
| `content/entity/youkai/SmartYoukaiEntity.java` | board: `VISITING(150)` + behaviours |
| `content/entity/module/TalkModule.java` | `hold(400)` in `beginTalking` |
| `init/registrate/GLBrains.java` | `MEM_VISIT`, `VISITING` |
| `init/data/GLModConfig.java` | `visitStayMultiplier` |
| `content/rpg/core/CodecRegistry.java` | `HOME_BOUND` |
| `content/rpg/core/ServerCharacterDialogManager.java` | suppress quest `START` while visiting |
| `content/rpg/action/StartQuestAction.java` | guard |
| `content/ui/dialog/FirstDialogScreen.java` | pick `visitGreeting` |
| `init/data/rpg/QuestDialogData.java` | `homeBound()`/`visiting()`, `defaultDialog` arity |
| `init/data/structure/helper/StructStructure.java` | `visits` component |
| `init/data/structure/GLStructureGen.java` | author `visits` |
| `init/data/rpg/*QDGen.java` | gate home content, author visit chats + greetings |
| `init/GensokyoLegacy.java` | register `AliceQDGen` |
| `content/command/GLCommands.java` | `/gensokyo visit` |

## 7. Command

```
/gensokyo visit <character> [ticks]   force a visit at the home the player stands in (default 6000)
/gensokyo visit end                  dismiss the visitor the player is looking at
/gensokyo visit info                 print the home's guest table and today's window per guest
```

`info` is the playtesting workhorse: the window is deterministic, so it can be
inspected directly instead of waiting for a 30% roll.

## 8. Current visits

| structure | guest | chance | stay |
|---|---|---|---|
| `morichika_shop` | `hakurei_reimu` | 30% | 2400–6000 (2–5 min) |
| `morichika_shop` | `kirisame_marisa` | 30% | 2400–6000 (2–5 min) |
| `morichika_shop` | `izayoi_sakuya` | 30% | 2400–6000 (2–5 min) |
| `hakurei_shrine` | `kirisame_marisa` | 20% | 2400–3600 (2–3 min) |
| `marisa_house` | `alice` | 20% | 2400–3600 (2–3 min) |

## 9. Known limits

* **Duplicate spawn.** If a visitor is alive but in an unloaded chunk while the
  host bed's chunk is loaded, the dedup scan misses it and a second one spawns.
  Both expire on their own schedule. The visitor is `restrictTo`'d inside the
  structure bound, so this needs the bound to straddle an unloaded chunk —
  possible for the 80-block spread of `hakurei_shrine`.
* **`visit_greeting` is a required JSON field.** A datapack overriding
  `default_dialog` must add it. Every entry is generated; a character with no
  home still needs one, because `visitGreeting` is what a first-time player sees.
* **Quest dialog is not gated.** `initialDialog` is unreachable while visiting
  (hard rule), and `followUp`/`completion` lines are task talk rather than
  "you're in my house" talk, so they were left alone.