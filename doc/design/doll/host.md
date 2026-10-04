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
| arming | the player's choice | a lance or a wand iff she is fighting, talismans iff spendable (§4) |
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

## 4. Arming — lances and wands iff combat, talismans iff spendable

A doll gets a weapon in its **main hand** the moment `Post.COMBAT` begins and
loses it the moment that ends. This is exact and bidirectional, not "armed until
she runs dry":

- outside combat, no doll holds anything, so a `REGULAR_ATTACK` order cannot be
  *accepted* at all (`canAccept` requires a matching hand) — an unarmed doll is not
  merely idle, it is incapable.
- in combat, every doll is armed within the same tick the post flips, because
  `arm()` walks the roster and mutates the ledger loadout directly.

### 4.1 The split — half lances, half wands

**Not** eight identical shooters. The roster divides: **even ledger slots take a
`DollLanceItem`, odd slots a `StarWandItem`**, so a full combat roster fields 4
lancers and 4 shooters, a 6-doll roster 3 and 3. One is a
`DollMeleeBehavior` charge (§5.1b), the other a `DollDanmakuBehavior` shot, and
neither has to be told — the weapon decides, through the same registry the
player's glove goes through. A doll with no danmaku item charges; a doll with a
lance never shoots, because the lance is registered *below* danmaku and nothing
else competes for its hand.

Parity is the whole rule, and it is deliberately **stateless**: no assigned-weapon
field to keep in step with the roster, nothing to serialize, nothing to migrate.
A doll that is parked and conjured again comes back with whatever its slot says,
which is the same thing it had. The only thing that shifts a doll's slot is a
**destroyed** entry dropping out of the ledger, and that is already the moment the
roster has changed underneath her — the one doll that changed flank is cheaper than
a second field.

It is also free visually. The follow formation is indexed in ledger order too
(`FollowDollOwnerGoal`, entity.md §3.2), and the arc runs slot 0 to slot n−1 from
one flank, up over the top and down to the other, so parity splits the arc itself:
**the lances hang on one side and the wands on the other**, the retinue reading as
a left flank and a right flank rather than a uniform cloud.

The two halves are not interchangeable at range, and that is the point of the
split. The charge has to *start* inside its 16-block engage range — a doll idling
in formation 2–6 blocks off Alice, while Alice herself holds a 16–24 band from
the target (§5.1), is often outside it, and the wand has no such limit. So a lancer
is a doll that engages when the mob comes to it, and a shooter is a doll that never
has to. `command()` accounts for this rather than feeding the charge work it cannot
do (§5).

The weapons are hers, not loot: they are conjured alongside the dolls and vanish
with them. Nothing is consumed and nothing is dropped — the lance has no durability
at all (item.md §9) and the star wand is never spent — so the fight costs her
nothing but the attention.

### 4.2 The talisman

A folded **heal talisman sits in the off hand only while it can be spent** — in
combat, or when anyone in reach is actually hurt — and leaves both hands again when
that stops being true. The off hand used to be unconditional, on the reasoning that
`HEAL` is not her decision but the heal pass's: it is scheduler-issued by
`DollCommander`, not by her `command()`, so the talisman only had to be *present*
for that pass to find a hand. True as far as it goes, and useless as a policy — it
means every doll of the retinue carries a healing charm at all times, so a house
full of dolls idling with full health bars looks like it is holding a spare
medkit it has no intention of using. A charm is worth a hand only while there is
someone to spend it on, exactly like a wand is worth a hand only while there is
someone to shoot. So `arm()` asks `commander.healNeeded(owner)`: the same set the
heal pass picks its targets from — her, any live doll of the roster, or a marked
entity — via the static `HealTalisman.needsHeal`, which is what `test` itself
delegates to. The interesting case is the one outside combat: off-duty dolls still
patch up a hurt doll and still heal her, and the charm appears for exactly as long
as that lasts.

Talismans are folded on demand like the wands, and are hers rather than loot too:
one put away goes back to the air it was folded from, and the next emergency gets a
fresh one. The two hands do not contend: `REGULAR_ATTACK` and `HEAL` are different
action types and a doll only ever holds one ticket.

`arm()` therefore has to own the **whole** loadout, not just the weapon. The heal
behaviour has a sticky swap (control.md §2): acting on an off-hand item moves it
into the main hand and leaves it there, because main-hand-ness *is* the doll's
standing choice of hand. A doll that has just healed is therefore holding the
talisman in the **main** hand and its weapon in the off hand. A pass that only
policed the main hand for the weapon would leave that talisman sitting there, and
then refuel the off hand on top of it — the doll ends up carrying two. So `arm()`:

1. hands any main-hand talisman **back to the off hand, keeping the stack**
   (moving it, not refolding a fresh one — otherwise every heal burns a talisman),
   but only while one is wanted at all, and empties the main hand either way, since
   that is the hand the weapon owns;
2. sets the main hand to this slot's weapon iff in combat, empty otherwise;
3. refolds the off hand when a talisman is wanted and what is there is not a usable
   one, or empties the off hand when none is wanted.

Step 2 is an **identity** test, not "does it hit at all". The lance is bound by an
exact `stack.is(GLItems.DOLL_LANCE.get())` match, so a doll that somehow came back
holding a *different* danmaku item would keep it, and one holding something with no
lance match has to be replaced rather than counted as armed.

Note the ordering: a talisman is judged **once per pass**, not per doll, so a doll
cannot be seen with a charm the pass has already decided nobody needs.

A doll **holding a ticket is skipped entirely**. The swap is deliberate for the
action in flight, and reloading the loadout out from under it would make the heal
no-op on the very tick it resolves; the layout is repaired on the first idle tick
afterwards.

A doll that just **spent** an item is skipped for `DollHandLock.HOLD` (10) ticks on
top of that, because idle is not the same as finished drawing. The one-shot
animations are triggered by the action that spends the item — `toy_bow`, `toy_skill`,
`toy_bomb` all fire from the same tick the stack is consumed or the talisman is
burned — and heal, throw and laser **complete their ticket on that tick**. So
without the hold, the very next `arm()` pass would find an idle doll with a spent
stack in its fist and take it away, or move it back to the other hand, while the
client is still playing the swing that fist performed: the wand blinks out of the
hand a frame after the last shot of a fight.

The record lives on the doll, not the host, and is written at the single point every
behaviour already passes through — `DollBehavior.ensureMainHand`, called immediately
before the item is used, so "acting with the main hand" and "just spent it" cannot
disagree. `DollHandLock` is transient and server-only, like `DollActionHandler`: the
client is told what to draw by the synced loadout mirror and keeps no timing of its
own, so there is no client-side half of this to keep in step. The hold is per doll
and covers both hands, which is right because the animation covers both.

`arm()` walks the **whole ledger**, not just the live dolls, so a doll retired
mid-fight is already parked by the time it runs and does not carry a wand back
out the next time she goes to the park.

## 5. Orders — one-time, one doll per mob, and nobody idle

`command()` runs every `COMMAND_INTERVAL` ticks, only in combat, in two passes:

```
claimed = every target some doll is already holding a ticket for

pass 1 (spread):   for each free doll -> first valid target not in claimed that it can reach
pass 2 (overflow): for each still-free doll -> first valid target it can reach, claimed or not
```

**Pass 1 is the spread**: a doll takes a mob no other doll is engaged with, so a
crowd is dealt with rather than focused.

**Pass 2 is the point of the whole thing.** Once the mobs run out, the dolls that
are still free pile onto the survivors. Without it a one-mob fight would have
exactly one doll shooting and seven standing idle — which reads as the dolls not
working at all. "One doll per mob" is a *preference for the first pass*, never a
cap on how many may engage a given mob.

**Both passes skip a doll that cannot reach the mob** — `canReach` on the behavior
its own weapon resolves to, asked per doll with a fresh instance so it costs nothing
(control.md §5.1b). This only became a real question when the roster split (§4.1).
The charge has to *start* inside its 16-block engage range, and her dolls start
their orders in formation 2–6 blocks off Alice while she holds a 16–24 band from
the target (§5.1) — so a lancer is routinely out of reach of the mob she is
pointed at. Two things go wrong if it is not filtered. The order itself is dead on
arrival: `DollCommandGoal.checkStartTimeouts` sees `!canReach` and completes the
ticket that same tick, so it drains rather than acting. And because a drained
lancer is free again by the next pass, the pass would re-offer it the same dud
forever, claiming a mob in the spread step that the shooter it displaced never gets
— one permanently misassigned slot per lancer, on every pass, every fight.

Leaving it waiting is the honest result rather than a shortfall. A lance is not a
ranged weapon; a lancer whose mob is out of charge range stands in formation and
engages when the mob closes, which is what the weapon is for. `nextFree` taking the
reach as part of its filter is also what lets a *nearby* mob win the overflow pass
over the primary one — pass 2 is "first target it can reach", so a lancer that
cannot touch the mob she is fixated on takes the closer one instead of standing
idle beside it.

**One-time, not iterative.** The iterative mode exists to chain a *single player
order* across dolls so a volley spreads over time without a central cursor. That
is exactly the wrong tool here: Alice is not issuing one order, she is
continuously re-tasking a squad. One-time orders mean no shared `done` set, no
handoff, no stall guard, and no chain to strand — a doll finishes, is free, and is
re-tasked on the next pass against whatever is left. A dead target frees its doll
immediately instead of queueing behind nine.

What paces the shooting is therefore not Alice at all but each doll's own danmaku
cooldown (`DollDanmakuBehavior`, 20 ticks). Throughput is one shot per doll per
cooldown, and with the wand half of a full combat roster that is a shot every half
second from every shooter at once — the "non-stop" is emergent, not scripted. The
lance half is paced the same way (`COOLDOWN_TICKS` 20, the identical constant) but
bounds itself: a charge that misses returns home on its own rather than
re-engaging (§5.1b).

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
| **unload** (chunk leaves) | The ledger is written with her — dolls and all their values. The doll entities are *not*: `shouldBeSaved` is false for any doll without stray data (entity.md §5), so vanilla never writes one. On the next tick `reconcile` parks every `SUMMONED` entry `TEMP` and `conjure` brings the roster back at her side. Health, colour and gear all survive. |
| **kill** | `onKilled` discards every doll immediately — a corpse does not keep ordering dolls about. Her data is never written back, so the roster dies with her. |
| **discard** (vanish task, reputation, debug reset) | She leaves the level, so `getHost()` returns null for each doll and each self-discards on its next tick. Nothing of hers is left in the world. |

A respawn from her bed is a fresh Alice with a fresh retinue — the same bargain
every character gets.

Other edge cases:

- **A doll restored from disk.** `DollNeverSave` used to be written but never read, and `ownerUUID` is never persisted at all — so a doll that got written to a chunk came back as a zombie: vanilla keeps its game UUID, but it has no owner, so it can neither follow nor be commanded, *while its ledger entry still matches it by UUID*. That last part is the nasty one: a live count that includes a doll which cannot function suppresses the conjure pass that would replace it, so one zombie doll can wedge a whole roster — and if its chunk never ticks it, it never even self-discards. Such a doll is also **immune to every kind of damage**, because `takeDamage` had nowhere to write the hit and dropped it rather than falling through to the plain path. Three things fix it, at three different depths:
  1. `shouldBeSaved` is false for a paired doll, so it is never written to a chunk in the first place (entity.md §5). A load-time `setRemoved` on the marker is *not* enough and was actively worse: the level callback is attached after `readAdditionalSaveData` and `addEntityWithoutEvent` ignores the removal reason, so the doll landed in the world anyway — in the level, in the uuid lookup and in the live count, but frozen and hostless. That is precisely the reported symptom.
  2. `DollCommander`'s resolution refuses to return a doll whose host it cannot resolve, so a useless doll is never counted live and can never suppress its own conjure. This is what makes any ledger heal out of a wedged roster rather than needing the one above to hold.
  3. `reconcile` parks a hostless doll like any other unusable one, and `reconcile` reports whether it repaired anything so the conjure happens in the same tick rather than up to a second later.
- **A doll drifts off.** Past `DollHost.PULLBACK_DISTANCE` (or into another dimension) the reconcile pass discards it and parks the entry; the next conjure brings it back at her side.
- **A retired doll stays armed.** `arm` walks the whole ledger, not just the live dolls, so a doll parked mid-fight does not carry a weapon back out the next time she goes to the park — a lance or a wand, whichever its slot says.
- **A destroyed doll shifts the split.** Slots are parity over ledger order and the ledger drops a destroyed entry outright (§2.1), so every doll after it moves one slot and swaps weapon. Only the frame of the fight, only by one, and the alternative is a second field to serialize and keep in step with a roster that is already rebuilt every tick.
- **Dolls never itemize.** `mobInteract`'s empty-hand recall is gated on `isPlayerOwned()`, so a creative player cannot pull one of Alice's dolls into their inventory. Her dolls have no item form at all.
- **No strays.** `detach` is refused. A stray needs somewhere to hand its entry back to and Alice has no way to take one in, so she never creates one — she issues no `SUICIDE_ATTACK`, the only thing that cuts a doll loose.
