# Character Doll Host — Alice's retinue

`DollHost` was already an abstraction: the player capability (`DollAttachment`) and the controller block both implement it, and the doll entity never names either. This doc covers the third kind of host — a **living entity that conjures its own dolls** — and the character that uses it.

## 1. Why a character can be a host

`DollHost` is the entire doll↔host contract: the pairing half (`findSummoned` / `update` / `detach` / `onDeath`) plus the **command surface** (`getFormationYaw`, `summonedAllies`, `isCommandedTarget`, `doneType`, `handAhead`, `handOff`). Every command-surface method has an inert default, so the controller block and a stray "just work" by doing nothing.

That matters because the doll entity code (`DollCommandGoal`, `FollowDollOwnerGoal`, `DollStatus`, `DollDanmakuAlly`, `DollFriendlyFire`, `DollActionHandler`, `DollDanmakuBehavior`) calls the host unconditionally. Before this refactor those seven sites each branched on `host instanceof DollAttachment` to reach the commander, which meant adding a host meant editing all of them. Now a new host implements `DollLedger` — `dolls()` plus `commands()` — and the interface forwards the command surface for it.

Two supporting generalizations came with it:

- **`DollCommander` works off `DollLedger`, not `DollAttachment`,** and every entry point takes a `LivingEntity` commander instead of a `ServerPlayer`. A glove call passes the player; Alice passes herself. One fan-out, two callers.
- **`BaseDollEntity` accepts any living owner.** `setOwner`/`isOwner` widened from `Player` to `LivingEntity`, and `getHost()` resolves the owner entity and asks *it* for a host: a `ServerPlayer` yields the capability, anything else that implements `DollHost` yields itself. That single branch is the whole extension point — no registry, no interface lookup, no new entity type.

## 2. What a character host looks like

`AliceDollHost` is a youkai module (`AbstractYoukaiModule`), so its whole ledger rides Alice's chunk save like any other module data. The materialization path is `DollSpawn.materialize`, the same one the player ledger uses, so the pairing invariants (fresh uuid per entity, entry registered before `addFreshEntity`, free-space placement) hold identically.

It departs from the player ledger in exactly the ways the character demands.
Its ledger is bound to her lifetime, so there is no way for a retinue to outlive
her (§6):

| | Player ledger | `AliceDollHost` |
|---|---|---|
| origin | doll items | conjured out of thin air |
| `STORED` | waits for a free slot, then itemizes | **unused** — she has no inventory (§2.1) |
| `detach` | cuts a doll loose (stray) | refused; she issues no suicide order, so it is never asked for |
| `onDeath` | recovery is the reconcile pass | no-op; the reconcile pass drops the entry (§2.1) |
| size | the player's collection | a **roster** sized by her post (§3) |
| arming | the player's choice | star wands, iff she is fighting (§4) |
| orders | the glove, player-driven | she issues them herself (§5) |

A doll is created **on demand**: the roster grows as her post asks for more, rather than conjuring eight up front and leaving seven standing around.

### 2.1 `STORED` — a destroyed doll just vanishes

The player ledger parks a zero-health doll as `STORED` and hands it back as a
broken item to be repaired — that whole dance exists because a doll is a *thing
the player owns*, so its count and its survival both have to be respected.

Alice owns no dolls. She conjures them, so a destroyed one is not a broken
possession to be nursed back to health: the reconcile pass discards the entity
and **drops the entry**, and the conjure pass simply makes another. The ledger
therefore never has to track how many dolls she is *allowed* to have, and
`MAX_DOLLS` bounds only the live roster.

The one state she does keep is `TEMP` — parked, not destroyed. A doll retired
because her roster shrank keeps its values and comes back when it grows again;
only a doll that was actually destroyed is forgotten.

## 3. The roster — `AliceDollHost.Post`

How many dolls she keeps out is a function of what she is doing, read off her brain's active activity:

| Post | Activities | Dolls |
|---|---|---|
| `INDOOR` | at home, asleep, in conversation, anything else | 1–2 |
| `OUTDOOR` | idle, playing | 3–4 |
| `COMBAT` | hunt, fight | 6–8 |

The size is re-rolled **only when the post changes**, so two Alices — and the same Alice on two visits — do not field identical rosters, while the count never jitters within a post. `MAX_DOLLS` is 8, the top of the combat band; the roster never grows past it.

A retired doll is parked `TEMP` and conjured again when the roster grows. Its entry is never dropped, so a doll that has served once can always be brought back.

Note the at-home post depends on `MemoryModuleType.HOME` being present — an Alice with no bed bound to a home is never "indoors" and keeps the outdoor escort. The bed is what earns her the smaller roster.

## 4. Arming — star wands iff combat

A doll gets a `StarWandItem` in its main hand the moment `Post.COMBAT` begins and loses it the moment that ends. This is exact and bidirectional, not "armed until she runs dry":

- outside combat, no doll holds a wand, so a `REGULAR_ATTACK` order cannot be *accepted* at all (`canAccept` requires a matching hand) — an unarmed doll is not merely idle, it is incapable.
- in combat, every doll is armed within the same tick the post flips, because `arm()` walks the live roster and mutates the ledger loadout directly.

The wands are hers, not loot: they are conjured alongside the dolls and vanish with them. Nothing is consumed and nothing is dropped, so the fight costs her nothing but the attention.

## 5. Orders — one-time, one doll per mob

`command()` runs every `COMMAND_INTERVAL` ticks, only in combat:

```
claimed = every target some doll is already holding a ticket for
for each idle doll:
    target = first valid target not in claimed
    issue DollAction.oneTime(REGULAR_ATTACK, target)   // ONE_TIME, not ITERATIVE
    if issued: claimed += target
```

**One-time, not iterative.** The iterative mode exists to chain a *single player order* across dolls so a volley spreads over time without a central cursor. That is exactly the wrong tool here: Alice is not issuing one order, she is continuously re-tasking a squad. One-time orders mean no shared `done` set, no handoff, no stall guard, and no chain to strand — a doll finishes, is free, and is re-tasked on the next pass against whatever is left. A dead target frees its doll immediately instead of queueing behind nine.

**Distinct targets.** `claimed` is seeded from the tickets the dolls already hold, then filled as orders go out, so no two dolls are ever issued the same mob. The retinue spreads over the prey instead of piling onto one target, and when the prey list shrinks the surplus dolls simply idle.

Targets come from `YoukaiTargetContainer` (her own hostile list, non-players) plus her current melee target by hand, since the container deliberately tracks only mobs.

## 6. Lifecycle — her retinue cannot outlive her

The ledger lives on Alice's entity, so it is bound by exactly her lifetime, and
dolls are never chunk-serialized (§5.8) so there is no second copy anywhere:

| Event | What happens |
|---|---|
| **unload** (chunk leaves) | The ledger is written with her — dolls and all their values. The doll entities are simply not saved, so on the next tick `reconcile` finds every `SUMMONED` entry without a live entity, parks it `TEMP`, and `conjure` brings the roster back at her side. Health, colour and gear all survive. |
| **kill** | `onKilled` discards every doll immediately — a corpse does not keep ordering dolls about. Her data is never written back, so the roster dies with her. |
| **discard** (vanish task, reputation, debug reset) | She leaves the level, so `getHost()` returns null for each doll and each self-discards on its next tick. Nothing of hers is left in the world. |

A respawn from her bed is a fresh Alice with a fresh retinue — the same bargain
every character gets.

Other edge cases:

- **A doll drifts off.** Past `DollHost.PULLBACK_DISTANCE` (or into another dimension) the reconcile pass discards it and parks the entry; the next conjure brings it back at her side.
- **More dolls than targets.** `nextFree` returns null and the loop breaks; the surplus stand down. They do not pile onto the last mob.
- **A retired doll stays armed.** `arm` walks the whole ledger, not just the live dolls, so a doll parked mid-fight does not carry a wand back out the next time she goes to the park.
- **Dolls never itemize.** `mobInteract`'s empty-hand recall is gated on `isPlayerOwned()`, so a creative player cannot pull one of Alice's dolls into their inventory. Her dolls have no item form at all.
- **No strays.** `detach` is refused. A stray needs somewhere to hand its entry back to and Alice has no way to take one in, so she never creates one — she issues no `SUICIDE_ATTACK`, the only thing that cuts a doll loose.
