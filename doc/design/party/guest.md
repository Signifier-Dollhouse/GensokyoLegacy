# The Guest — the task cycle and the three phases

`YoukaiPartyTask` is a cycle driven from a `PARTY` activity at priority 200: seat, eat, go back to the seat, sit a while, repeat, until the spread is gone.

## 1. The task

`YoukaiPartyTask` is a **cycle**, not a walk-then-finish: seat, eat, go back to the seat, sit a while, repeat, until the spread is gone.

```
start()      → recover her place in the cycle from PartyModule (see below)
SEEK_SEAT    → walk to the ground beside a free chair, stand on it, sit, claimSeat(...), WAIT_TABLE
WAIT_TABLE   → seated, doing nothing until the party reaches FEASTING
SEEK_FOOD    → draw a dish, weighted by its appeal per serve
DELAY        → 10..40 ticks before she reaches for it (§2.2)
              within seated reach of her chair?  taken from where she sits
              otherwise                   dismount, walk to the ground beside it,
                                           stand on it, then carry it back
PICKUP       → USE_MAINHAND, then the serving in her hand (§2.2)
EATING       → seated: the eat animation if her rig has one, the item leaving
               her hand during it; hunger AND appeal credited when it ends
RETURNING    → walk back to her chair, sit again
RESTING      → wait hungrySeat (1s) if still hungry, fullSeat (5s) once satisfied
               roll leaveChance after a satisfied sit — and go, if it comes up
no chair      → tell the host, look again every 3s, leave after three failures (§2.1)
no dish left  → giveGift(), then the exit
LEAVING      → stand up after 0..60 ticks, walk away or go back (§2.3)
```

**The party caches its own spread and its own chairs.** `PartyAttachment` holds both lists and refreshes them on two schedules from `PartyTicker`: the food every two seconds, the layout every ten. A guest needs the food list every time she draws a dish, which is every few seconds for the length of her visit, so having each guest sweep a box for herself made the party cost far more to run than the party itself. Two rates because the two change at very different speeds — every dish a guest eats is one fewer, while a chair that moved a second ago will still be there in four more — and because conflating them would either make a refilled dish take ten seconds to appear or rebuild the chair list four times a second for nothing.

The fast rate is also what makes **refilling work at all**. The scan runs once per send and freezes a roster against it; without a rescan a host putting out a second course would have it ignored until everyone had left, and a guest would walk to a dish another guest had just finished. The lists are re-checked per use rather than trusted — the cache is two seconds old, and in those two seconds a dish may have been eaten or moved.

One consequence worth stating: `hungerBudget` now tracks what the guests can actually reach, and moves as the host refills. It therefore **narrows** after a send, from "the whole scanned room" to "the guest box" — which is the honest answer, since nothing outside that box can be eaten, but it is a different number from the one the send printed.

**The dish is drawn, not found.** `drawDish` reads the party's cached food list, keeps everything still laid out and still worth eating, and picks one with probability proportional to `appealPerServe`. That is what makes a guest work across the spread instead of clearing the nearest plate first and working outwards, and what makes the tasty things go before the plain ones. It replaced a `nearestFood` search, which had the opposite property by construction.

**She never leaves on being satisfied — but she does leave eventually.** Reaching her fulfillment only lengthens the sit between dishes, from her `hungrySeat` (1s) to her `fullSeat` (5s), both per character in `party_taste` rather than constants: a glutton plausibly dawdles between courses and a nibbler plausibly does not. After each *satisfied* sit she rolls `leaveChance` (5%) and goes if it comes up. That is the only thing that ends a party before the food does, and it has to exist — otherwise a guest who is already full has no reason ever to go home and the table stays occupied until the host's spread is gone. It is a chance per sit rather than a flat timer so that the two per-character fields mean something together: a long `fullSeat` also means a slow departure. In simulation, a guest facing a 200-dish spread leaves after about eleven dishes, every time.

**No chair: tell the host, wait three seconds, look again.** The rule is in §2.1 — the host hears about it on the first failure, the guest re-checks every `SEAT_RETRY`, and the third consecutive failure is what ends it. What matters here is *why* it exists at all: the party-wide feast gate (state.md §2) waits for *every* guest to be seated, so one guest who cannot find a chair would otherwise hold the whole table out of the feast until the ten-minute `PARTY_TIMEOUT`, and the host would be told nothing until the party was called off. Saying so is the point — the host put out chairs and they were not enough, or one too many guests were invited, and without a word the only visible sign is a table with fewer people at it than the letter promised. No gift either way, since she never ate; and the same rule covers a chair taken away under her, because the layout rescan (state.md §2) puts her back in `SEEK_SEAT` on the same terms.

**Her chair is re-checked, so a removed one does not strand her.** The layout rescan (§1) bumps a version counter when the chair list changes, and the guest compares it against the version she last saw. A chair that has been broken is no longer hers to sit on, so she drops the claim and starts looking again — on the same terms as a guest who never found one, giving up if there is nothing else (§2). Without this, a guest removed from her `ChairEntity` would stand beside the table in GATHERING for ever, holding the party with her.

**Most rounds she does not move.** A dish within seated reach (3 blocks) of where she sits is eaten from the chair. Only a dish further out sends her across the room — which costs a dismount, because **sitting means riding** a `ChairEntity` (`ISeatableBlock.sitDown` calls `startRiding(seat, true)`) and a passenger does not walk. Every departure therefore owes a return, or the second trip out would be measured from wherever the first one left her.

**Neither a chair nor a dish is ever walked to.** Both walks go to a **standable block next to them**, and she has to be standing *on* it before she acts. Two reasons, and both of them are about the furniture:

- A chair is not something you walk into. It is furniture you sit down from *beside* — the old target was `seat.above()`, the block the rider ends up in, which is inside the chair's own collision box. A pathfinder asked to reach it either fails outright or parks the guest standing on the seat, and a character standing on a chair is the most visible way this feature can look broken.
- A dish on a table has **air** beside it, not floor. The block next to a plate on a laid table is at table height with nothing under it, so the answer is the ground beside the *table*. Which is why `PartySend.standingNear` tries two rings: the block's own neighbours first, then its neighbours one support down. A dish on the floor is answered by the first ring, a dish on a table only by the second.

Because the walk target and the thing being acted on are **different blocks**, the task keeps them in different fields — `standing` is where she is walking to, `target` is the chair or the dish — and tests them differently:

| | reach | why |
|---|---|---|
| arriving at the block she walked to | **0** | she either reached that floor tile or she did not. There is nothing to be "near" about a block she is meant to be standing on, and a tolerance here would have her act on a dish from the far end of the table |
| a dish, from her chair | **3** | she is holding a plate out over the table, and this is what makes most rounds of a laid banquet happen without her standing up at all |
| a dish, from her feet | **2** | standing up and crossing to a dish three blocks away should not be possible when she could have eaten it from where she was sitting |
| **her chair** | **3** | arriving and sitting are two steps, and the second one is `setSitting` finding a `ChairEntity` to put her on. Three blocks is the slack for a pathfinder that stopped a block short — the case that otherwise leaves a guest standing beside her own chair with the whole party held in GATHERING on her |
| a dish with nothing to stand beside | — | she walks at it anyway and the leg timeout judges. Never being able to reach food is worse than reaching it badly |

`SEAT_REACH` is checked *before* the claim rather than around it: `setSitting` is a silent no-op on a block that is not one of ours, so a guest out of range does not claim a chair she is not on and tries again next tick, by which time she has walked the rest of the way.

All three are measured to the **block's box rather than its centre** — standing on the block beside a chair is *zero* blocks from it, so the numbers mean what they look like. Measured to the centre, a guest standing next to her own chair is 1.1 blocks from its middle and could never sit in it.

**Departing is the one place the exact rule does not apply.** A guest on her way out is not picking anything up, so her arrival at the destination uses a loose 2 blocks rather than zero — the last two steps of a departure are not worth the leave timeout that would follow them.

The walk-failure fallback (§2.1) already lands where this wants it to — it teleports to free space *beside* the target — so a guest who could not path there arrives able to act, not merely closer.

`HomeSearchUtil.setSitting` (`HomeSearchUtil.java:75`) already handles claiming a free seat, checking `ChairEntity` occupancy and yielding to players, and is a silent no-op when the block is not `ISeatableBlock` — so a third-party chair from `party_seat` simply has the guest stand beside it, with no special case. The claim is still recorded, because otherwise one unseatable chair would hold the party in GATHERING for ever.

**`PartyModule` holds the chair, not the task.** A visit now spans many dish cycles and can be interrupted at any point by the chunk unloading, which stops the task and starts a fresh one. `start()` therefore recovers her place in the cycle from what she is physically doing — no seat yet, so `SEEK_SEAT`; riding one, so `WAIT_TABLE`; neither, so mid-`RETURNING` — rather than assuming `SEEK_SEAT` as the old code did. Getting this wrong would send every guest back out for another walk on each unload.

**The task must never return `false` from `canStillUse` to change phase.** One `Behavior` instance drives the whole visit, so the phase lives in a field on the task; `false` calls `doStop` → `stop()`, which wipes it, and the brain re-runs the exclusive behavior next tick with `start()` re-deriving the phase. Every phase change therefore keeps returning `true`, and `false` is reserved for actually finishing — party over, out of time, or nothing left to serve.

This is why `YoukaiPartyTask` cannot copy `MarisaForageTask`'s "arrive, act, return false" shape. The forage task is single-action and holds no state between runs, so stopping is free. A multi-phase task must keep the instance alive across its own transitions, and the failure mode is silent — the entity looks perfectly healthy, sits down, stands up, sits down again, and nothing is ever eaten.

**Food contention needs no locking.** On arrival a guest re-validates that the block is still food and still laid out; if another guest took the last serve first it is gone and she draws again. With per-serving consumption this is *more* forgiving than it was: two guests walking to the same cake no longer collide destructively, since each finds a serve still on it. Worst case is a wasted walk.

**A serve is taken when eating starts and credited when it finishes.** `PartyFoodValue.consume` is called in `beginEating`, and `pendingHunger` / `pendingAppeal` — both read *before* the consume, because a serve's readings are properties of the state it was taken from — carry them across the `EAT_TICKS` window so her tallies and the party's served total move together. `stop()` credits a serve that was taken but never banked, so a party ending or a chunk unloading mid-bite cannot silently swallow food the guests already ate.

**Eating while seated needs `isLaidOut` to accept a chair underneath.** A plate set on a guest's own seat is within reach of her and, on the old test, not part of the spread at all — the scan would not have counted it, so she could not eat it. Rather than special-casing the eating rule, the chair was added as a surface: a dish on a chair is now part of the spread and budgeted for like any other, instead of being edible but unaccounted.

**The bite is four beats, not one, and two of them are art work in progress.** `§2.2` has the sequence: a delay, a pickup animation with the serving item actually in her hand, a walk back to the chair if she left it, and an eat animation on arrival during which the item leaves her hand. What exists today is uneven, and the two gaps are worth naming because they are not the same gap:

- **The chew exists for one rig.** `PlainFairyEntity` maps `吃` as `thenPlayAndHold` and fires it as entity event **80** (`PlainFairyEntity.java:123`, `:134`) — deliberately outside `YoukaiAnim`, which is the dialog vocabulary and whose 71–79 are its wire ids (`GeoYoukaiAnim.java:30`). `FairyCakeTask` is the precedent for the whole shape: a chew, then a `USE_MAINHAND` gesture timed against the clip's own length. No party-eligible character has it, so for the shipped roster the eat beat is skipped, not faked.
- **Holding an item is a per-renderer question.** The data side is one `setItemInHand(MAIN_HAND, …)` and `YoukaiHeldItemLayer` already renders a character's `MAINHAND` at her `RightHandLocator` bone — but only `CirnoRenderer` and `PlainFairyRenderer` register it, so of the six eligible characters only Cirno would visibly carry a slice. Adding one `addRenderLayer` per renderer is the whole fix, and the layer is written to serve every rig in the mod.

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

**She walks in, and if she cannot walk in she is moved rather than stranded.** The walk out to her chair gets the ordinary leg timeout; a guest who never arrives is teleported to a free, standable block *beside* her seat and carries on from there. The seat is only worth sitting in if she can get to it, and a character standing in a doorway for the rest of a party is worse than one that blinks into place. This only ever fires when the pathfinder has already failed, so the teleport is a repair and not a shortcut.

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
