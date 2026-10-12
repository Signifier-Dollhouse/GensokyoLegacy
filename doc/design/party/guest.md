# The Guest — the state machine and the three phases

A guest's visit is a **cascade of eleven brain tasks**, routed by which of seven pieces of her own state are set. There is no phase enum, no dispatcher, and no state to lose.

Those seven were brain memories once, and that was the wrong home: `Brain.MemoryValue.serialize` writes a memory only when its type carries a codec, and a guest reloaded from disk came back without any of them — no line of the tree to resume on, so she went back to her schedule as though the party had never happened. The machine position is therefore **fields on `PartyModule`**, serialized with the entity beside the chair claim and the tallies that already survived a reload. The only party memory left is `MEM_PARTY`, the activity gate, and it is re-asserted every tick rather than saved.

## 1. The machine

Driven from a `PARTY` activity at priority 200, and registered in one line:

```java
board.addPrioritizedActivity(GLBrains.PARTY.get(), GLBrains.MEM_PARTY.get(), 200);
board.addPartyTasks();
```

**Which task runs is decided by which of those seven are set, not by a variable.** Each task declares the ones that must be set (and those that must not) in its entry condition, and `checkExtraStartConditions` covers what they cannot know — whether she is actually a passenger, whether the party has reached FEASTING. `TaskBoard.fetch` collects every exclusive task under one activity into a single `GateBehavior(ORDERED, RUN_ONE)`, which tries them **in registration order and runs the first that matches**. So the order *is* the priority, and a task lower down can be given a signature as loose as "nothing" — everything more specific has already been claimed by then.

### The seven

| field | type | means |
|---|---|---|
| `MEM_PARTY` | `GlobalPos` | she is at a party at all — the activity gate, and the one that is still a memory |
| `party.serve` | `PartyServe` | **has food in hand**, and it is not yet banked |
| `party.serveDish` | `BlockPos` | **has a food target** — the dish she committed to |
| `party.hesitate` | `int` | eat wait: deciding at the dish, ticks left |
| `party.grab` | `int` | the pickup gesture, ticks left |
| `party.chew` | `int` | eat wait: chewing, ticks left |
| `party.rest` | `int` | **post-eat wait**: sitting between dishes, ticks left |
| `party.leaveArmed` / `party.leave` | `boolean` / `int` | the unseat wait, then the walk out |

**One convention for all of them: 0 is not running, and a null dish or serve is not held**, so a restart mid-wait resumes the wait rather than re-rolling it. `leaveArmed` is the exception that proves it: the departure is a separate flag precisely because zero has to mean "armed and already on her way" — the tick after the last of the unseat wait — and the timer alone could not say whether she was leaving or merely sitting down to eat. Clearing it mid-walkout would drop the cascade back to whatever fits an ordinary guest, and she would sit back down on the way out of the door.

`MEM_PARTY` is registered with `Optional.empty()` and is therefore *never* written out, which is why `PartyModule.tickServer` re-asserts it from the module every tick instead.

### The cascade

`✓` must be set, `✗` must be clear, `·` is don't care. Seated-ness and phase are physical checks, not machine state.

| # | task | S | D | H | G | C | R | L | seated | phase |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Leave | · | · | · | · | · | · | ✓ | · | · |
| 2 | Chew | ✓ | · | · | · | ✓ | · | · | · | · |
| 3 | Eat | ✓ | · | · | · | ✗ | · | · | seated | · |
| 4 | GoToSeat | ✓ | · | · | · | · | · | · | not seated | · |
| 5 | Delay | ✗ | ✓ | ✓ | · | · | · | · | · | · |
| 6 | Pickup | ✗ | ✓ | ✗ | ✓ | · | · | · | · | · |
| 7 | GoToFood | · | · | · | · | · | · | · | · | · |
| 8 | Rest | · | · | · | · | · | ✓ | · | · | · |
| 9 | FindFood | · | · | · | · | · | · | · | seated | FEASTING |
| 10 | WaitTable | · | · | · | · | · | · | · | seated | not FEASTING |
| 11 | FindSeat | · | · | · | · | · | · | · | not seated | · |

Read down as a decision tree:

```
leave armed?                    → Leave
has serve, chewing?             → Chew
has serve, seated?              → Eat
has serve                       → GoToSeat
has dish, hesitating?           → Delay
has dish, grabbing?             → Pickup
has dish                        → GoToFood
resting?                        → Rest
seated, party feasting?         → FindFood
seated, party not feasting?     → WaitTable
otherwise                       → FindSeat
```

The bottom four constrain **nothing at all**, which is the point: by the time the gate reaches them, every specific state has been ruled out, so they are the terminal fallbacks that make the cascade total. A brute-force check of all 512 combinations (7 pieces of state × seated × FEASTING) confirms every one resolves to exactly one task — 0 uncovered.

`GoToFood` shows as all-don't-care because its `stillMine` carries the dish conditions instead; either way the combined signature is "has dish, not hesitating, not grabbing".

### Walking is not a task

Every party task carries `WALK_TARGET: VALUE_ABSENT`, and `PartyTask.canStillUse` yields the moment a walk order is outstanding: `YoukaiMoveTask` does the walking and clears the memory on arrival, at which point the gate re-picks the same task and it finds itself arrived.

**That is why reach needs no state of its own.** Being out of reach and being in reach are the same task; the walk order stands in for the difference. It also means the state that moves her and the state where she thinks are genuinely two states rather than one state with an embedded pause — she crosses to the dish first and hesitates *at* it, which is the only arrangement in which changing her mind at the last moment is possible at all.

### The un-banked serve

`party.serve` holds both "she is carrying something" and what it is worth, which is what makes the window durable. It used to be two task fields plus a `stop()` that banked them on the way out — a race with task shutdown. Serialized on the module it is simply state: a guest interrupted mid-bite by an unload or a restart still has the serve when she comes back and is credited for it then.

### The director

One **always-task** under `PARTY`, running alongside whichever of the eleven is going, for the three things that are not states: the party ending under her, the party closing while she is mid-course (which arms the departure, the wait drawn per guest so a table does not empty in one tick), and her chair being taken away by the layout rescan. It also owns the teleport fallback for a walk the pathfinder has already failed.

### What the module records

`PartyModule` is serialized with the entity under `YoukaiModules`, so the seat claim and the visit tallies (`eaten`, `appealEaten`, `gifted`) were already durable, and the seat-retry counters joined them there. The distinction that held before the machine moved was that memories said *which task* while the module recorded *what happened to her*; now both live here, and the seat episode's reason for it still stands — it is visit bookkeeping, and a reload must not reset the count, or a guest who cannot find a chair gives up in three ticks instead of nine seconds.

**`recoverPosition()` no longer exists**, which is the clearest sign the design is right: on reload the signature table plus physics fully determine what happens next — serve set and seated means Chew, dish set means GoToFood, neither and seated means FindFood.

**The party caches its own spread and its own chairs.** `PartyAttachment` holds both lists and refreshes them on two schedules from `PartyTicker`: the food every two seconds, the layout every ten. A guest needs the food list every time she draws a dish, which is every few seconds for the length of her visit, so having each guest sweep a box for herself made the party cost far more to run than the party itself. Two rates because the two change at very different speeds — every dish a guest eats is one fewer, while a chair that moved a second ago will still be there in four more — and because conflating them would either make a refilled dish take ten seconds to appear or rebuild the chair list four times a second for nothing.

The fast rate is also what makes **refilling work at all**. The scan runs once per send and freezes a roster against it; without a rescan a host putting out a second course would have it ignored until everyone had left, and a guest would walk to a dish another guest had just finished. The lists are re-checked per use rather than trusted — the cache is two seconds old, and in those two seconds a dish may have been eaten or moved.

One consequence worth stating: `hungerBudget` now tracks what the guests can actually reach, and moves as the host refills. It therefore **narrows** after a send, from "the whole scanned room" to "the guest box" — which is the honest answer, since nothing outside that box can be eaten, but it is a different number from the one the send printed.

**The dish is drawn, not found.** `drawDish` reads the party's cached food list, keeps everything still laid out and still worth eating, and picks one with probability proportional to `appealPerServe`. That is what makes a guest work across the spread instead of clearing the nearest plate first and working outwards, and what makes the tasty things go before the plain ones. It replaced a `nearestFood` search, which had the opposite property by construction.

**The gift is given when the departure is armed, not when it is taken.** There are three ways a visit ends — the spread running out, a roll on a satisfied sit, and the host calling it off — and only the first two used to pass through the one place that arms the exit. The third is `PartyTicker`'s: it closes the party when the spread is gone two rescans running, which arms the departure for every guest at once, so a guest who had eaten a whole spread and left to a host who ended it early left nothing on the table. Every arming point now gives, which is harmless to repeat because `PartyGift` is a no-op once she has given. **The door is a one-shot event, not a state**, which is what made this the wrong place to hang a side effect in the first place.

**Her walk-away destination is drawn once.** It used to be drawn afresh on every tick of the walkout, and a walk-away destination is a random roll — so she re-rolled it every tick, walked toward a target that kept moving, and never arrived. She also never looked stuck, because she was walking, constantly, and never triggered the stall repair either, since the target that repair watches was itself moving: guests paced about the party for the rest of it.

**She never leaves on being satisfied — but she does leave eventually.** Reaching her fulfillment only lengthens the sit between dishes, from her `hungrySeat` (1s) to her `fullSeat` (5s), both per character in `party_taste` rather than constants: a glutton plausibly dawdles between courses and a nibbler plausibly does not. After each *satisfied* sit she rolls `leaveChance` (5%) and goes if it comes up. That is the only thing that ends a party before the food does, and it has to exist — otherwise a guest who is already full has no reason ever to go home and the table stays occupied until the host's spread is gone. It is a chance per sit rather than a flat timer so that the two per-character fields mean something together: a long `fullSeat` also means a slow departure. In simulation, a guest facing a 200-dish spread leaves after about eleven dishes, every time.

**No chair: tell the host, wait three seconds, look again.** The rule is in §2.1 — the host hears about it on the first failure, the guest re-checks every `SEAT_RETRY`, and the third consecutive failure is what ends it. What matters here is *why* it exists at all: the party-wide feast gate (state.md §2) waits for *every* guest to be seated, so one guest who cannot find a chair would otherwise hold the whole table out of the feast until the ten-minute `PARTY_TIMEOUT`, and the host would be told nothing until the party was called off. Saying so is the point — the host put out chairs and they were not enough, or one too many guests were invited, and without a word the only visible sign is a table with fewer people at it than the letter promised. No gift either way, since she never ate; and the same rule covers a chair taken away under her, because the layout rescan (state.md §2) puts her back in `SEEK_SEAT` on the same terms.

**Her chair is re-checked, so a removed one does not strand her.** The layout rescan (§1) bumps a version counter when the chair list changes, and the guest compares it against the version she last saw. A chair that has been broken is no longer hers to sit on, so she drops the claim and starts looking again — on the same terms as a guest who never found one, giving up if there is nothing else (§2). Without this, a guest removed from her `ChairEntity` would stand beside the table in GATHERING for ever, holding the party with her.

**Most rounds she does not move.** A dish within seated reach (3 blocks) of where she sits is eaten from the chair. Only a dish further out sends her across the room — which costs a dismount, because **sitting means riding** a `ChairEntity` (`ISeatableBlock.sitDown` calls `startRiding(seat, true)`) and a passenger does not walk. Every departure therefore owes a return, or the second trip out would be measured from wherever the first one left her.

**The spread is not a floor — but only for guests.** For a party character the table, the block above the table, the dishes, the block above those, and the chairs are all `PathType.BLOCKED`. Without it she routes across the table to reach a chair on the far side, which is the one way a banquet reads as a queue of characters walking through the food.

The rule is **party-only**, and that is why `YoukaiPathTypeHandler` takes the mob. A rule about who is walking is not a rule about a block: a character crossing an ordinary room has no reason to treat a laid table as a wall, and a rule that held for everybody would be wrong for everyone not at a party. The mob arrives from `NodeEvaluator.mob`, which both the walk and the fly evaluators already inherit, so both carry it and nothing else changed shape.

It costs nothing where there is no party, because the party test is the first thing it does, and past that it is one `FoodResultCache` lookup on an interned block state for all but the handful of container blocks. A path asks the rule once per node and a guest's route is a few dozen nodes.

**"Is this food" is `PartyFoodValue.isFood`, deliberately.** Not `PartyFoodHandler.claims`, which was the first attempt and was wrong: a claim says which handler was *written for* a block, so it excludes the catch-all covering any block whose own item is food — a dish with no bespoke handler stayed walkable while still being served. A pathfinding rule that disagrees with the rule that picks dinner is worse than no rule at all. There is no location pre-check either: `isLaidOut` answers "does the party's scan count this", which is a budgeting question, and pathfinding only asks "is this food".

`standingNear` is unaffected by all of it: it asks about collision shapes, so "the floor beside the dish" resolves the same way as before.

**Neither a chair nor a dish is ever walked to.** Both walks go to a **standable block next to them**, and she has to be standing *on* it before she acts. Two reasons, and both of them are about the furniture:

- A chair is not something you walk into. It is furniture you sit down from *beside* — the old target was `seat.above()`, the block the rider ends up in, which is inside the chair's own collision box. A pathfinder asked to reach it either fails outright or parks the guest standing on the seat, and a character standing on a chair is the most visible way this feature can look broken.
- A dish on a table has **air** beside it, not floor. The block next to a plate on a laid table is at table height with nothing under it, so the answer is the ground beside the *table*. Which is why `PartySend.standingNear` tries two rings: the block's own neighbours first, then its neighbours one support down. A dish on the floor is answered by the first ring, a dish on a table only by the second.

Because the walk target and the thing being acted on are **different blocks**, `PartyTarget` carries both, plus a distance for each:

| | reach | why |
|---|---|---|
| arriving at the block she walked to | **1** | **this used to be 0, and 0 was the bug.** It looked stricter and was worse: a pathfinder that stops one block short of the tile it was sent to — ordinary, and reliably produced by a chair beside a table — would never satisfy it, so the guest stood beside her own seat until the walk timeout rescued her. The interaction reach is what actually decides whether she can act; the arrival test only needs to mean "she is roughly there" |
| a dish, from her chair | **3** | she is holding a plate out over the table, and this is what makes most rounds of a laid banquet happen without her standing up at all |
| a dish, from her feet | **2** | standing up and crossing to a dish three blocks away should not be possible when she could have eaten it from where she was sitting |
| **her chair** | **2** | she stands on the block beside it, which is *zero* blocks from its box, so two is ample and keeps her from sitting down from across the table |
| a dish with nothing to stand beside | — | she walks at it anyway and the leg timeout judges. Never being able to reach food is worse than reaching it badly |

`PartyTarget`'s interaction reach is checked *before* the claim rather than around it: `setSitting` is a silent no-op on a block that is not one of ours, so a guest out of range does not claim a chair she is not on and tries again next tick, by which time she has walked the rest of the way.

All of them are measured to the **block's box rather than its centre** — standing on the block beside a chair is *zero* blocks from it, so the numbers mean what they look like. Measured to the centre, a guest standing next to her own chair is 1.1 blocks from its middle and could never sit in it.

**Departing is the one place the exact rule does not apply.** A guest on her way out is not picking anything up, so her arrival at the destination uses a loose 2 blocks rather than zero — the last two steps of a departure are not worth the leave timeout that would follow them.

The walk-failure fallback (§2.1) already lands where this wants it to — it teleports to free space *beside* the target — so a guest who could not path there arrives able to act, not merely closer.

`HomeSearchUtil.setSitting` (`HomeSearchUtil.java:75`) already handles claiming a free seat, checking `ChairEntity` occupancy and yielding to players, and is a silent no-op when the block is not `ISeatableBlock` — so a third-party chair from `party_seat` simply has the guest stand beside it, with no special case. The claim is still recorded, because otherwise one unseatable chair would hold the party in GATHERING for ever.

**`PartyModule` holds the chair.** The claim is serialized with the entity, so it survives a chunk unload and a restart, as does the machine position beside it. `FindSeatTask` is the only task that runs while she is not seated, and it recovers her place from nothing more than that.

### How the split went wrong, three times

The machine was a single `Behavior` with a `switch`, then nine behaviours behind a phase enum, then eleven tasks behind seven brain memories, and finally eleven tasks over seven fields on the module. The first three shipped broken, and the failure mode is the same each time: the entity looks perfectly healthy while doing nothing.

| | symptom | why it is invisible |
|---|---|---|
| the draw lived in a task step | guests crossed the room forever, target replaced every tick | she is constantly walking, so nothing looks stuck |
| the in-reach path cleared the target | **no guest ever ate anything**, in any round | the cycle runs perfectly; she just picks up air forever |
| `seatCheckAt` set once, never advanced | gave up on finding a chair in under a tenth of a second | she does leave, and the message to the host is correct |
| the per-tick deadline refresh dropped | guests written off mid-party, on their own chair | looks like a slow-loading or an empty spread |
| a departure that renamed instead of arming | the party kept holding a chair nobody was in | the guest still leaves, so nothing looks wrong |
| `stillMine` stayed true after the task acted | it re-acts every tick and starves the next task of the gate | the next task simply never runs |

**The cascade's version of the last one is the departure flag**: a departure is arming state, never just entering a state, and it must survive the walkout rather than expiring when the unseat wait is up.

**One rule the cascade makes impossible to break by accident:** a task sets the field that hands over, and the gate picks somebody else on the next tick. There is no "phase changed" moment to get wrong, because there is no phase.

**The rule it is easy to break by accident:** `stillMine` must go false once a task has done its job. A running task keeps the gate — the brain only re-picks when *nothing* is running — so a signature that stays true after the task acts means the task simply keeps acting. Two of those shipped: `FindFoodTask` redrew the dish every tick, and `GoToSeatTask` re-armed its own chew every tick and starved `ChewTask` of the gate. Every task that sets a field therefore tests for that field in `stillMine`, and that test *is* the handoff.


**Food contention needs no locking.** On arrival a guest re-validates that the block is still food and still laid out; if another guest took the last serve first it is gone and she draws again. With per-serving consumption this is *more* forgiving than it was: two guests walking to the same cake no longer collide destructively, since each finds a serve still on it. Worst case is a wasted walk.

**A serve is taken when she picks it up and credited when she finishes it.** `PartyFoodValue.consume` is called in `PartyContext.takeServing`, and the readings are taken *before* the consume, because a serve's readings are properties of the state it was taken from — which is why they are read into the serve rather than re-read later. The serve then rides that state across the `EAT_TICKS` window, so her tallies and the party's served total move together. A visit that ends mid-bite still credits it through `finishServe`, because the plate left a dish that is already gone.

**Eating while seated needs `isLaidOut` to accept a chair underneath.** A plate set on a guest's own seat is within reach of her and, on the old test, not part of the spread at all — the scan would not have counted it, so she could not eat it. Rather than special-casing the eating rule, the chair was added as a surface: a dish on a chair is now part of the spread and budgeted for like any other, instead of being edible but unaccounted.

**The bite is four beats, not one, and two of them are art work in progress.** `§2.2` has the sequence: a delay, a pickup animation with the serving item actually in her hand, a walk back to the chair if she left it, and an eat animation on arrival during which the item leaves her hand. What exists today is uneven, and the two gaps are worth naming because they are not the same gap:

- **The chew is a rig capability, and every rig now answers it.** All seven shipped character animations carry a `吃` clip, and only one of them was wired to play it: `PlainFairyEntity` fired it as entity event **80**, deliberately outside `YoukaiAnim`, which is the dialog vocabulary and whose 71-79 are its wire ids (`GeoYoukaiAnim.java:30`). The rest of a guest's meal played out over an animation nobody saw. It now lives on `GeoYoukaiAnim` instead: `broadcastEat` fires the event for any geo-animated character, and `handleAnimEvent` — which all seven already called — plays it. `FairyCakeTask` is the precedent for the whole shape: a chew, then a `USE_MAINHAND` gesture timed against the clip's own length. The clip is registered per rig with `addEatAnim` rather than folded into `addDialogAnims`, because registering a clip a rig does not have resolves it to `null` and then silently freezes that rig in its last pose.
- **Holding an item was a per-renderer question, and it is now answered.** The data side is one `setItemInHand(MAIN_HAND, …)` and `YoukaiHeldItemLayer` renders a character's `MAINHAND` at her `RightHandLocator` bone — but a geo renderer draws no items by itself, where vanilla `MobRenderer` did, so a character that became geo-rendered lost whatever she was holding. For this feature that is most of what she does: the serving is in her hand for the whole walk back to her chair.

**Only Cirno and `PlainFairy` carried a plate.** The layer was registered on two of eight renderers. It is now on all seven of the geo ones (Alice, Marisa, Reimu, Morichika, Sakuya added to Cirno and `PlainFairy`); `Rumia` is a plain `MobRenderer` and never lost it. The layer itself is unchanged: one layer serves every rig, because Blockbench exports the wrist bones under the same two names for all of them.

What is left of the old treatment is the sound: `entity.swing(MAIN_HAND)` + `YoukaiAnim.USE_MAINHAND` + `SoundEvents.GENERIC_EAT`, which is exactly what `MarisaForageTask` does for foraging (`MarisaForageTask.java:60`).

## 2. The three phases, one at a time

§1 is one guest's loop. This is the party's: what happens between the letter being sent and the last character walking away from the table. It is the part of the feature most likely to be read as obvious and least likely to be right — an arrival that puts four characters on one tile, a guest who stands beside a chair for ever, a party that ends and takes its guests with it — so every rule below is written out with the thing it is protecting against.

The phases themselves are in state.md §2; this is the procedure inside them. Everything below is built; the table at the end says which parts of it needed anything that was not obvious from the rule.

### 2.1 Arriving — GATHERING

**One guest a second, not the whole roster in one tick.** The party admits one roster entry per pass, and `PartyTicker` already runs at 1 Hz (state.md §1), so one admission per pass is one arrival per second with no timer of its own: `PartyAttachment` keeps a cursor into `roster`, and `PartySend.send` opens the party and returns.

| | roster spawned in one tick | one a second |
|---|---|---|
| what the host sees | a table that fills in a single frame | people arriving |
| pathfinding | every guest paths on the same tick | spread across the first few ticks |
| who is invited | — | unchanged: the draw is still frozen at send (state.md §1) |

The roster is still a frozen list. This is about *when* a guest enters the world, not about whether she is still coming — a draw re-run per second would let the guest list change under everyone, which is exactly what state.md §1's freeze exists to prevent.

**She only starts walking once she can stand.** No walk order is issued while she is off the ground, which is what stops a guest *flying* to a party: these entities are all `mayFly()`, and both the walk control and the fly control are live, so an order given mid-air is answered by the flying one. She falls, lands, and is told where to go on a later tick — with the leg clock held off meanwhile, or a guest dropped from above the table would be written off before she landed.

The test is `navCtrl.isCloseToGround()` rather than `onGround()`, and the difference is the whole point: a walk node wants solid ground under the feet, so `onGround` is exactly the condition `YoukaiEntity.mayLand`'s own note says leaves a hovering character unable to path anywhere at all. "Close to ground" is what this codebase already uses to decide when a flyer should land and try walking.

**She appears out in the room, on the ground, never in the furniture.** A spawn is a random position within `partyScanDistance` (32) of the anchor, preferring somewhere she can stand:

```java
// send.md §3's spawn, with an arrival position instead of the middle of the table
Vec3 arrival = RandomPos.generateRandomPos(youkai, () -> {
    var spot = randomXZAround(anchor, GLModConfig.SERVER.partyScanDistance.get());
    if (!youkai.getNavigation().isStableDestination(spot)) return null;
    return LandRandomPos.movePosUpOutOfSolid(youkai, spot);
});
// null, or standing in a chair or a table, and the guest is not spawned here at all
if (arrival == null || furnitureAt(level, arrival)) return null;
```

`RandomPos` + `LandRandomPos` is the pair `HomeSearchUtil.getRandomPos` already uses to put a character somewhere in a house she can stand in (`HomeSearchUtil.java:244`), and `generateRandomPos` tries ten positions before it gives up — the fallback is a real case, not a formality.

It is deliberately **not** `partyGuestRadius` (24). That is how far she may wander once she is *at* the party (limits.md §1); 32 is how far away she may appear. A guest who materialised inside the guest radius would already be at the table, which is the thing the rule exists to prevent — and the furniture test is the same one on both the spawn and the teleport below, because the teleport is the path most likely to land in the middle of the table.

**She walks in, and if she cannot walk in she is moved rather than stranded.** The walk out to her chair is watched for *lost ground*, not for elapsed time: the closest approach she has made to her target is kept, and reset whenever she beats it. Only three seconds of not getting any nearer teleports her to a free, standable block *beside* her seat, and she carries on from there. The test is this one because a fixed deadline from issuing the order was the wrong rule — a guest crossing a long room is slow and fine, and the deadline declared her stuck while she was halfway across it. The seat is only worth sitting in if she can get to it, and a character standing in a doorway for the rest of a party is worse than one that blinks into place. This only ever fires when the pathfinder has already failed, so the teleport is a repair and not a shortcut.

**The party seats them, so they do not fight over chairs.** Left to themselves every guest walks to the *nearest* free chair, and at a laid table the nearest chair is the same chair: `HomeSearchUtil.setSitting` ejects whoever is sitting there, so the second arrival wins and the first is stood up mid-meal. `PartyAttachment` therefore keeps the seating plan — a chair → guest map — and a guest asks it for a chair instead of picking one:

| | what the party does |
|---|---|
| she has a chair and it is still there | **the same one.** First-write-wins, and a table rearranged under the party does not walk anyone off a chair they are on |
| she does not have one | the free chair **nearest where she is standing**, so guests take the chair on their own side rather than all crossing to one end of the table |
| every chair is claimed | nothing. That is the whole of "no chair" — she is never offered another guest's chair a second time |
| her chair stops being a chair | the claim is released, and she looks again |

Only the **last** row reassigns anyone, which is the rule that matters: re-arranging furniture, a guest standing up for a dish and a chair being briefly unreachable are all reasons to keep the seat you had. The claim is released exactly once otherwise — when she leaves, which is what stops an early guest's chair staying claimed for ever and the table never filling.

The plan is answered from the party's own record rather than from what the guest believes, because a guest does not record a chair until she is sitting on it: a guest whose walk was interrupted mid-approach would otherwise be handed a *second* chair on the next attempt and leave the first claimed for ever.

**No chair: say so at once, wait three seconds, try again.**

| when | what |
|---|---|
| the first time she cannot find a chair | the host is told — `<name> is looking for a chair` |
| every `SEAT_RETRY` — 3 s, where it is 40 ticks today | look again |
| the third consecutive failure | leave the party, and tell the host she gave up |

The window is about nine seconds, which is what the old single ten-second timeout bought — but the host hears about it at the first failure instead of at the end, and a chair put out in the middle of the window is picked up without the clock having run out. It is `PartyMessage`'s existing `GLLang.Party.NO_CHAIR` line split into a *still looking* and a *gave up* form, because only the second one is terminal and only the second one should read like an ending. No gift either way: she never ate, and a guest carried in on the roster who found no chair is not one the host owes anything to.

**The roster names characters, not clones.** Before spawning one, the party looks for her already being on site: a visitor inside a nearby home (`VisitModule.visiting()`) or a resident standing near her own structure (`HomeModule.home()`), within reach of the party's own guest box (§1). Found, she joins **as herself** — no second copy of a Reimu at one table — and what she was doing before is recorded on her `PartyModule` so it can be given back:

| she was | she is given back to |
|---|---|
| visiting a `StructureKey` | that visit, and the ten seconds of grace below |
| homed near a `StructureKey` | her own home and her ordinary schedule |
| neither — the ordinary clone | nothing; she walks away and is gone |

This is the one place the party writes onto an entity that has a life of her own, and it is why the exit is a walk rather than a `discard()`. It also means a reused character's tallies start at whatever she had already eaten *at this party* — the counters belong to the party, not to her, so joining is `arrive()` and not a reset.

**A visitor who outlives her window gets ten seconds.** A guest who was visiting when her visit ended mid-party does not blink out: she is given `VisitModule.hold(200)` when she leaves, so she has ten seconds to walk off and be seen leaving. `hold` rather than a window extension because the visit window is a pure function of `(structure pos, type, day)` (`character_visit.md` §2) and has no field to extend — `hold` is the mechanism that already exists for exactly this ("a conversation buys 20 seconds", `TALK_HOLD`), and it expires on its own.

### 2.2 Feasting — the bite

**Every bite waits first.** After a dish is drawn the guest holds for a random **10–40 ticks** before she touches it. Nothing else changes — it is a delay in front of picking the serving up — but it is the difference between a table of guests moving as one organism and a table of people. The floor matters as much as the ceiling: an instant draw reads as the character reacting to the player rather than deciding on anything, and half a second is the shortest delay that still looks like a choice. Forty ticks is two seconds, and `hungrySeat` (60 ticks, three seconds) still sets the pace of the meal — a guest who is genuinely hungry comes back for the next course sooner than she hesitates over the one she just finished.

**Then four beats.**

```
draw ─ 0..40t ─ pick it up ─ back to the chair ─ eat it
                   (use anim)    (if she left it)   (item vanishes)
```

| beat | what happens |
|---|---|
| **pick it up** | `swing(MAIN_HAND)` + `YoukaiAnim.USE_MAINHAND`, then **the serving item in her main hand** — the item a guest is actually handed, not the block it came off. A dish with no item form of its own (`SaucerFood`, `BlockItemFood` — eaten where it lies) plays the beat with an empty hand. |
| **back to the chair** | if she left it to reach the dish she dismounts, walks and sits again. A dish already within `REACH` skips this — she is already where she is going to eat. |
| **eat** | seated, she plays the eat animation **if her rig has one**, and the item leaves her hand during it. |
| **credit** | hunger and appeal are banked when the animation ends, unchanged from §1. |

The two rules that made the serve safe still hold, and the pickup beat is where they now sit: the block is consumed **once**, at pickup, with both readings taken before it is consumed, and a serve taken and never credited is still banked by `stop()`. The item in her hand is presentation over that same serve — it is emptied during the animation and is never a second one.

### 2.3 Closing — leaving

**Two ways a party ends, and food is the weaker of them.**

| trigger | rule |
|---|---|
| **the spread ran out** | the food list was empty at one rescan **and** still empty at the next — two consecutive empty scans, `FOOD_RESCAN` (40 ticks) apart |
| **everybody left** | the last guest goes, for any of the reasons above — `leaveChance`, the leg timeout, no chair to be found |

The two-scan debounce costs nothing and is not the same as "empty". The food list is refreshed every two seconds for the express reason that a host putting out a second course should see it noticed while the table is still full (§1); deciding on a single empty scan would end the party in the two seconds between one guest taking the last slice and the host putting down the cake. Either way the party's food is never touched on the way out (state.md §2).

The second trigger is a tuning statement as much as a rule. §1 already gives a satisfied guest a 5% chance per sit to leave, and with a large enough spread every guest eventually rolls it — so **a party is bounded by its guests' patience, not by its food**. The spread decides how *well* a party goes; the guests decide how *long* it lasts. A host who laid out forty dishes has bought a better party, not a longer one, and the counter to that is `fullSeat` and `leaveChance` on the character, not more food on the table.

**Leaving is staggered, and it is not a dismissal.** The end of a party and a guest walking out mid-party run the same exit:

| step | rule |
|---|---|
| **unseat** | after a random **0–60 tick** delay, so a table does not empty in one frame |
| **go** | summoned by the party → walk away from the table and be gone once she is outside it; at home, or visiting → walk back to where she came from and pick up what she was doing |

**A summoned guest walks off *far*, and that is not decoration.** She is put away when she
arrives, so how far she gets is how far the host sees her go — and a destination rolled
within the guest radius meant standing up, taking three steps and vanishing in front of the
table. Worse, a character still holding her party memory on the tick she starts walking
could be pulled back toward it, so the party appeared to be reeling her in. The destination
is therefore rolled outside the guest radius and checked **twice**: once on the spot, and
again on the ground the search found for it, because a search that walks outward from a
too-close roll can easily return a neighbouring block and undo the point. Several rolls,
because most spots in a room have no standable ground near them; if none works she leaves
from where she stands, which is dull but not wrong.

The delay is per guest and random, not a party-wide countdown, because the thing it avoids is a table going from full to empty inside a single tick — which reads as a cutscene, and which is what a loop that discarded every guest on the chunk was.

**Where she came from is recorded when she joins, not worked out at the end.** The `arrive` field above is the whole of it, and the ordering is load-bearing: walk *first*, clear the party state *after*. Clearing first resumes her schedule while she is still walking to the table, and a character with a `HOME` memory and a walk target is a character who goes home and out of the room at the same time.

Leftover food is untouched, as ever (state.md §2). A party that ends with half the spread on it costs the host nothing but the guests' evening.

| rule | state |
|---|---|
| one guest a second; random arrival on the ground, never in the furniture | built |
| teleport beside the chair when the walk fails | built |
| chair retry — tell the host, 3 s, leave after three failures | built |
| an on-site character joins as herself | built |
| ten seconds of grace for a visitor whose window closed | built |
| 10–40 tick delay before each bite | built |
| hold the serving item; it leaves her hand during the eat animation | built; the eat animation itself is one rig's art (§1) |
| the party seats its guests, so they do not fight over chairs | built |
| one gift per guest, ever | built |
| the host is paid `repGain` of each guest's own consumed appeal | built |
| staggered unseat; walk away, or go back to where she came from | built |
| two empty scans end the party; the last guest out also ends it | built |

**Four things the code needed that this section did not ask for**, all consequences rather than choices:

- **The gathering gate had to wait for admission.** `PartyTicker.allSeated` is vacuously true with no guests, which was harmless when a send spawned the whole roster in one tick and is fatal now that an empty guest list is the normal state of a party in its first few seconds — every party would have feasted before anybody arrived. The gate is `admitted() && allSeated`.
- **"Everyone has gone" needed the same guard.** An empty roster no longer means the party is over; it means it has not finished introducing itself. A party is finished when the roster is empty *and* has been worked through, or when a closing party has drained.
- **A closing party needs its own backstop.** Closing ends when the last guest finishes leaving, and a guest who cannot path to her destination would otherwise hold the chunk's party open for ever. `CLOSING_TIMEOUT` (30 s) is the answer; past it the party resets and anyone still attached is dismissed by the ordinary "my party is gone" path.
- **`restrictTo` has no getter**, so a reused character's wander area has to be re-applied from her home on the way out rather than read back. Left out, a resident who wandered to a party could never go home again.

**A fourth that only showed up once the walks were exact**: the walk-failure teleport has to say where it put her. It moved her to free space beside the seat and left `standing` pointing at the floor tile she had *failed* to reach, so the arrival test never passed and she was teleported again every timeout for as long as the party lasted — a guest standing near her chair for the whole banquet. A teleport is an arrival.

**Three more that are load-bearing**, in the order they bite:

1. **`PartyModule.party()` no longer returns null for a CLOSING party.** It used to, and that was the signal to self-dismiss. A closing party is still hers — she needs it to run her exit — and it only goes null once the party has reset, by which time she has already left.
2. **`dismiss()` is no longer `discard()`.** A summoned clone is discarded; a character who came as herself is put back, because discarding a resident deletes a real character out of the world over a party. Both go through `completeLeave`, which reads `origin` before clearing it — walk first, clear after, or she resumes her schedule while still walking to the table.
3. **`VisitModule` stands down while she is partying.** It renews its visit memory every tick and dismisses when the window closes; left alone it fights the party for the entity, dismissing a guest the host is still feeding. One early return on `isPartying()`, and it resumes by itself when the party lets go.

**One number that came from the rig rather than from taste:** `PICKUP_TICKS` is 6, because that is how long `使用主手物品` runs — the same figure `FairyCakeTask` times a gesture against. The two gestures are the same clip, so the two timings should be the same number rather than two independently plausible ones.

## 3. Brain wiring

```java
// init/registrate/GLBrains.java
public static final Val<Activity> PARTY = ACTIVITIES.reg("party", () -> new Activity("party"));
public static final Val<MemoryModuleType<GlobalPos>> MEM_PARTY =
        MEMORIES.reg("party", () -> new MemoryModuleType<>(Optional.empty()));
```

Added once in `SmartYoukaiEntity.constructTaskBoard` (`SmartYoukaiEntity.java:81`), which every character subclass reaches through `super.constructTaskBoard(board)` — so one edit covers all fourteen characters.

**Priority 200**, above `VISITING` (150) and `TALK` (100), below the scheduled activities. `TaskBoard.buildBrain` (`TaskBoard.java:150`) walks `[CORE] + prioritized + scheduled` and injects each prioritized activity's memory as `VALUE_ABSENT` into every *later* entry, so with `PARTY` at 200:

- `TALK` is walked **before** `PARTY`, so its conditions never mention `MEM_PARTY` — **a party guest can still talk and trade**;
- `PARTY` requires `MEM_TALK` absent, so eating never interrupts a conversation already in progress;
- every scheduled activity requires `MEM_PARTY` absent — **a guest never sleeps and never goes home**, the same guarantee `VISITING` gives a house guest, one rung up the board.

**A guest walks. She does not hover.** These entities can fly, both controls are live at once, and the flying one answers an order given while airborne — so a guest told to walk to her chair would fly there instead, and the walk fallback would then be chasing a guest who is not stuck, just airborne. `SmartYoukaiEntity.mayFly` returns false while the party flag is set, which is enough for the pathfinding; the rest needs two more gates, because both of them would otherwise reinstall flight afterwards:

- hovering characters re-assert the flying navigation on **every** awake tick, since loading an entity puts its control back on the ground, so `PlainFairyEntity.travel` tests `mayFly` and not just `isSleeping`;
- `FairyFleeTask` reaches past `mayFly` for the flying navigation directly, so it repeats the gate and lets a guest who is attacked stand and take it rather than fly off over the host's table.

Arrival is the third gate: `PartyModule.enter` puts her on the walking navigation straight away, and leaving clears the flag, after which the hovering character re-asserts flight for herself.
