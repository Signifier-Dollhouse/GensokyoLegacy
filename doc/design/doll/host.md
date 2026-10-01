# Character Doll Host — Alice's retinue

`DollHost` was already an abstraction: the player capability (`DollAttachment`) and the controller block both implement it, and the doll entity never names either. This doc covers the third kind of host — a **living entity that conjures its own dolls** — and the character that uses it.

## 1. Why a character can be a host

`DollHost` is the entire doll↔host contract: the pairing half (`findSummoned` / `update` / `detach` / `onDeath`) plus the **command surface** (`getFormationYaw`, `summonedAllies`, `isCommandedTarget`, `doneType`, `handAhead`, `handOff`). Every command-surface method has an inert default, so the controller block and a stray "just work" by doing nothing.

That matters because the doll entity code (`DollCommandGoal`, `FollowDollOwnerGoal`, `DollStatus`, `DollDanmakuAlly`, `DollFriendlyFire`, `DollActionHandler`, `DollDanmakuBehavior`) calls the host unconditionally. Before this refactor those seven sites each branched on `host instanceof DollAttachment` to reach the commander, which meant adding a host meant editing all of them. Now a new host implements `DollLedger` — `dolls()` plus `commands()` — and the interface forwards the command surface for it.

Two supporting generalizations came with it:

- **`DollCommander` works off `DollLedger`, not `DollAttachment`,** and every entry point takes a `LivingEntity` commander instead of a `ServerPlayer`. A glove call passes the player; Alice passes herself. One fan-out, two callers.
- **`BaseDollEntity` accepts any living owner.** `setOwner`/`isOwner` widened from `Player` to `LivingEntity`, and `getHost()` resolves the owner entity and asks *it* for a host: a `ServerPlayer` yields the capability, anything else that implements `DollHost` yields itself. That single branch is the whole extension point — no registry, no interface lookup, no new entity type.

> **The host must be the entity, not the module.** Alice keeps her ledger in an
> `AliceDollHost` module, because that is where module data belongs — it rides her
> chunk save for free. But `getHost()` can only ask an *entity*, so `AliceEntity`
> has to implement `DollHost` itself and delegate every method to the module.
> Without that, `getHost()` returns null for all of her dolls and each one
> self-discards on its next tick, and the symptoms are extremely misleading: the
> dolls appear, they never move, they never fight, and the roster seems to ignore
> its own cap — because what the player is watching is a conjure/discard churn,
> not a working retinue.

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

## 4. Arming — star wands iff combat, talismans always

A doll gets a `StarWandItem` in its **main hand** the moment `Post.COMBAT` begins
and loses it the moment that ends. This is exact and bidirectional, not "armed
until she runs dry":

- outside combat, no doll holds a wand, so a `REGULAR_ATTACK` order cannot be
  *accepted* at all (`canAccept` requires a matching hand) — an unarmed doll is not
  merely idle, it is incapable.
- in combat, every doll is armed within the same tick the post flips, because
  `arm()` walks the roster and mutates the ledger loadout directly.

The wands are hers, not loot: they are conjured alongside the dolls and vanish
with them. Nothing is consumed and nothing is dropped, so the fight costs her
nothing but the attention.

A folded **heal talisman sits in the off hand at all times**, refolded whenever it
wears out. The off hand is deliberately unconditional: `HEAL` is not her decision
— it is scheduler-issued by `DollCommander`'s heal pass, which every ledger
already runs — so the talisman only has to be *present* for that pass to find a
hand, and a doll keeps one to spend on Alice and on her other dolls whenever they
are hurt. The two hands do not contend: `REGULAR_ATTACK` and `HEAL` are different
action types and a doll only ever holds one ticket.

`arm()` therefore has to own the **whole** loadout, not just the wand. The heal
behaviour has a sticky swap (control.md §2): acting on an off-hand item moves it
into the main hand and leaves it there, because main-hand-ness *is* the doll's
standing choice of hand. A doll that has just healed is therefore holding the
talisman in the **main** hand and the wand in the off hand. A pass that only
policed the main hand for the wand would leave that talisman sitting there, and
then refuel the off hand on top of it — the doll ends up carrying two. So `arm()`:

1. hands any main-hand talisman **back to the off hand, keeping the stack**
   (moving it, not refolding a fresh one — otherwise every heal burns a talisman);
2. sets the main hand to the wand iff in combat, empty otherwise;
3. refolds the off hand if what is there is not a usable talisman.

A doll **holding a ticket is skipped entirely**. The swap is deliberate for the
action in flight, and reloading the loadout out from under it would make the heal
no-op on the very tick it resolves; the layout is repaired on the first idle tick
afterwards.

`arm()` walks the **whole ledger**, not just the live dolls, so a doll retired
mid-fight is already parked by the time it runs and does not carry a wand back
out the next time she goes to the park.

## 5. Orders — one-time, one doll per mob, and nobody idle

`command()` runs every `COMMAND_INTERVAL` ticks, only in combat, in two passes:

```
claimed = every target some doll is already holding a ticket for

pass 1 (spread):   for each free doll -> first valid target not in claimed
pass 2 (overflow): for each still-free doll -> the first valid target, claimed or not
```

**Pass 1 is the spread**: a doll takes a mob no other doll is engaged with, so a
crowd is dealt with rather than focused.

**Pass 2 is the point of the whole thing.** Once the mobs run out, the dolls that
are still free pile onto the survivors. Without it a one-mob fight would have
exactly one doll shooting and seven standing idle — which reads as the dolls not
working at all. "One doll per mob" is a *preference for the first pass*, never a
cap on how many may engage a given mob.

**One-time, not iterative.** The iterative mode exists to chain a *single player
order* across dolls so a volley spreads over time without a central cursor. That
is exactly the wrong tool here: Alice is not issuing one order, she is
continuously re-tasking a squad. One-time orders mean no shared `done` set, no
handoff, no stall guard, and no chain to strand — a doll finishes, is free, and is
re-tasked on the next pass against whatever is left. A dead target frees its doll
immediately instead of queueing behind nine.

What paces the shooting is therefore not Alice at all but each doll's own danmaku
cooldown (`DollDanmakuBehavior`, 20 ticks). Throughput is one shot per doll per
cooldown, and with a full combat roster that is a shot every half second from
every doll at once — the "non-stop" is emergent, not scripted.

Targets come from `YoukaiTargetContainer` (her own hostile list, non-players)
plus her current melee target by hand, since the container deliberately tracks
only mobs.

## 5.1 Fighting stance — a 16-24 band, in 3D

`AliceRangeTask` replaces the stock `AttackTask` + `StrafeTarget` pair, which both
want to be standing on top of the target: the strafe stops inside its radius and
the attack task walks in until it is within half of its melee reach. Her dolls
shoot; she conducts from a lane.

A single strafe *radius* cannot express "keep your distance" — the stock strafe
circles inside the radius and charges outside it, so anything sitting at the
threshold flips between the two forever. The task therefore holds a **dead band**:

| 3D distance to target | behaviour |
|---|---|
| `> 24` (`MAX`) | step to 20 blocks out, along the full 3D vector |
| `16..24` | circle at 20, in the direction she is already orbiting |
| `< 16` (`MIN`) | step to 20 blocks out, along the full 3D vector |

**3D, not a horizontal plane.** The orbit is a circle whose *horizontal* radius is
solved from what is left of the 20-block budget after the height error: the closer
she is to the target's height, the wider the circle, and the further above or below
it, the tighter. A horizontal-only version reads identically on flat ground and is
simply wrong against a player on a tower or a dragon in the air — it would leave
her circling at the wrong altitude forever. A floor on the horizontal radius stops
the circle collapsing to a point when the height gap is most of the budget.

Two implementation notes that are easy to get wrong:

- Movement goes through `getNavigation()`, **not** `MoveControl.strafe`.
  `FlyingMoveControl.tick` only handles `MOVE_TO` and clears `noGravity` in its
  else branch, so a strafe operation makes a flying youkai drop out of the sky.
- The entry condition requires `WALK_TARGET = ABSENT`, which is what keeps the
  always-on `YoukaiMoveTask` from claiming navigation on the same tick. This is the
  same gate `StrafeTarget` uses.

She also gets a short grace period (20 ticks) before charging a target she has
lost line of sight on, so a blink behind a pillar does not send her across the
arena.

## 6. Lifecycle — her retinue cannot outlive her

The ledger lives on Alice's entity, so it is bound by exactly her lifetime, and
dolls are never chunk-serialized (§5.8) so there is no second copy anywhere:

| Event | What happens |
|---|---|
| **unload** (chunk leaves) | The ledger is written with her — dolls and all their values. The doll entities are *not*: `addAdditionalSaveData` stamps `DollNeverSave`, and `readAdditionalSaveData` reads it back and removes the entity during load, so a doll that did get written to disk is gone before it is ever added. On the next tick `reconcile` parks every `SUMMONED` entry `TEMP` and `conjure` brings the roster back at her side. Health, colour and gear all survive. |
| **kill** | `onKilled` discards every doll immediately — a corpse does not keep ordering dolls about. Her data is never written back, so the roster dies with her. |
| **discard** (vanish task, reputation, debug reset) | She leaves the level, so `getHost()` returns null for each doll and each self-discards on its next tick. Nothing of hers is left in the world. |

A respawn from her bed is a fresh Alice with a fresh retinue — the same bargain
every character gets.

Other edge cases:

- **A doll restored from disk.** `DollNeverSave` used to be written but never read, and `ownerUUID` is never persisted at all — so a doll that got written to a chunk came back as a zombie: vanilla keeps its game UUID, but it has no owner, so it can neither follow nor be commanded, *while its ledger entry still matches it by UUID*. That last part is the nasty one: a live count that includes a doll which cannot function suppresses the conjure pass that would replace it, so one zombie doll can wedge a whole roster — and if its chunk never ticks it, it never even self-discards. `readAdditionalSaveData` now honours the marker and removes the entity at load, and `reconcile` reports whether it repaired anything so the conjure happens in the same tick rather than up to a second later.
- **A doll drifts off.** Past `DollHost.PULLBACK_DISTANCE` (or into another dimension) the reconcile pass discards it and parks the entry; the next conjure brings it back at her side.
- **A retired doll stays armed.** `arm` walks the whole ledger, not just the live dolls, so a doll parked mid-fight does not carry a wand back out the next time she goes to the park.
- **Dolls never itemize.** `mobInteract`'s empty-hand recall is gated on `isPlayerOwned()`, so a creative player cannot pull one of Alice's dolls into their inventory. Her dolls have no item form at all.
- **No strays.** `detach` is refused. A stray needs somewhere to hand its entry back to and Alice has no way to take one in, so she never creates one — she issues no `SUICIDE_ATTACK`, the only thing that cuts a doll loose.
