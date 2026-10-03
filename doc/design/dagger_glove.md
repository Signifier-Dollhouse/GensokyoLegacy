# Dagger Glove

`DaggerGloveItem` — the **Dagger Glove** — turns the returnable `IronDaggerItem` into a
three-mode thrown weapon: one mode per spread, driven by the l2itemselector wheel exactly as
`DollGloveMode` is.

> Status: implemented. All three modes wear the same glove art
> (`textures/item/tool/dagger_glove{,_single,_fan,_homing}.png` are identical copies of one 16×16
> texture), so the mode is carried by the tooltip and the wheel rather than by the held model.
> While held the glove wears a mitten, the same model the doll glove wears under its own skin
> (§6, §8).

The glove is the *second* way to throw a dagger. The plain `IronDaggerItem` stays the single-dagger
weapon; the glove is what turns a stack of daggers into a pattern, at the cost of a cooldown per
shot instead of a cooldown per dagger.

It lives in `content/item/dagger/`, its own package under `item/`: `content/item/glove/` belongs to
the doll glove. The two things the gloves share sit in their own packages beside them —
`content/item/targeting/`, the ray-trace target cache whose contract `GloveTargeting` both implement
(§2d), and `content/item/glovehand/`, the held mitten model both wear (§6).

## 1. Modes

`DaggerGloveMode` (ordinals 0–2, stored in the `DAGGER_GLOVE_MODE` component):

| idx | Mode | Daggers | Angles (deg, from look) | Cooldown | Life |
|---|---|---|---|---|---|
| 0 | `SINGLE` | 1 | 0 | 5 | 40 |
| 1 | `FAN` | 7 | −15 … +15, 5 apart | 10 | 40 |
| 2 | `HOMING` | 10, 5 per side | ±20, ±25, ±30, ±35, ±40 | 20 | per-stage, §2 |

Single and fan fly at **speed 2**, and so does a homing dagger's aimed stage. A homing dagger's
outbound stage is the exception: **speed = 16 / its turn tick**. Every mode spends one iron dagger
per shot.

- `SINGLE` — the plain throw, but on the glove's own (shorter) cooldown and out of the glove's
  inventory pool rather than the held stack.
- `FAN` — a flat 7-wide spread, symmetric about the look vector, in the look vector's own
  horizontal plane (`DanmakuHelper.getOrientation(look).rotateDegrees(angle)`).
- `HOMING` — see §2.

**Life** is the bullet's `DanmakuBulletEntity.lifetime`, i.e. how long a dagger may exist before it
is erased (and returned, §4). Single and fan keep `IronDaggerItem`'s 40. Homing is per-stage
rather than per-shot: the first stage's life *is* its turn tick, and the second stage lives 40,
which is a little over the 32 ticks that 64 blocks at speed 2 takes, so the aimed stage can always
reach anything the glove was willing to aim at.

## 2. Homing — two stages, one turn, sampled server-side

A homing dagger is **two entities**, not one steering entity. Following `ReimuPart`
(`content/spell/part/ReimuPart.java`) and danmaku_api's own `HomingSpellForm.Stage`, the stage
boundary is what turns the dagger:

1. **Stage 1** flies the initial spread angle with a life of exactly the dagger's turn tick, at
   speed `16 / turnTick` so it turns **16 blocks out** regardless of which pair it is.
2. On expiry, `ItemBulletEntity#terminate` fires the stage's `DaggerHomingTrail`, which spawns
   **stage 2** from exactly where stage 1 stopped, aimed at the target's position *now*, at speed 2.

Both stages are `IronDaggerBulletEntity` and both are real daggers with the same item, damage and
rendering; the glove spends one dagger for the pair (§4).

### 2a-1. Why stage 1's speed is 16/delay

A turn tick says how long a dagger has to reach the turn point, not how far. Flying all five pairs
at one speed therefore gave the volley five different turn radii — 16 blocks for the 8-tick pair up
to **40** for the 20-tick pair — so the wider pairs spent most of their flight further from the
target than the narrow ones and the volley lost its shape. Dividing a fixed 16-block reach by the
delay puts every turn point at the same distance and lets the longer-delayed pairs simply arrive
more slowly:

| pair | delay | speed | turns at | (was, all at speed 2) |
|---|---|---|---|---|
| 0 | 8 | 2.000 | 16 | 16 |
| 1 | 11 | 1.455 | 16 | 22 |
| 2 | 14 | 1.143 | 16 | 28 |
| 3 | 17 | 0.941 | 16 | 34 |
| 4 | 20 | 0.800 | 16 | 40 |

Stage 2 is unaffected and stays at 2: its job is to close on a target, and its life is already
sized to the target range (§2, `HOMING_STAGE_LIFE`).

### 2a. Why a trail action and not a steering mover

The reason is client/server consistency, and it is the whole reason this is two entities.

A mover can only ask the level where an entity **is**, and the client's copy of that entity is not
the server's: entity positions arrive late, they are interpolated, and under load the gap is large
enough to matter. A mover steering on a live target position therefore evaluates a *different* path
on each side, on every tick — the daggers get drawn along a trajectory they are not actually
flying, and client-side collision disagrees with the server's about where they are. A first draft of
this design did exactly that and was rewritten for this reason.

Re-aiming exactly once, on the server, makes the whole trajectory a **fixed geometric path**: two
line segments meeting at one server-chosen point, with one server-chosen direction. Neither side
consults a live entity to evaluate it, and the sampled direction reaches the client as part of stage
2's spawn packet — there is no separate sync to get wrong.

**Neither stage uses a mover**, and that is load-bearing rather than a simplification. Constant
velocity does not need one, and a mover is actively wrong here: `TargetPosMover` derives each tick's
velocity as `pos(tick) - prevPos`, where `tick` is the entity's `tickCount`. On the server that is a
step counter, but on the client it is the entity's **age**, rebuilt on spawn from
`level().getGameTime() - spawnTime` (`SimplifiedProjectile#readSpawnData`) — a client that first
sees a dagger mid-flight cannot know how many moves it has already made, so it asks for a position far
ahead of the synced one and hands the whole difference back as a single enormous step. A
`RectMover` pinned to the original muzzle position makes it worse still, since `pos(age)` is measured
from there while `prevPos` is wherever the dagger has been synced to.

Playtesting reported the homing daggers as flying at a strange speed, and this is why: they visibly
leap when they come into view, at a multiple of their real speed. With no mover,
`BaseProjectile#updateVelocity` falls through to `ProjectileMovement.of(deltaMovement)` and each side
integrates the velocity it was handed — the same speed for any age.

`ItemBulletEntity#terminate` is reached only on the server, so `DaggerHomingTrail` runs there and
only there; stage 2 is an ordinary entity that arrives through the normal spawn path. There is no
client-side counterpart to fall out of step, and none is needed.

The trade is honest and worth stating: **the turn aims at where the target is at the turn tick and
does not track it afterwards.** A target that keeps running sidesteps stage 2 rather than being
followed to the end. That is the price of a path both sides agree on, and it is the same trade the
spellcards make.

### 2b. Staggered turn

The five daggers on each side are staggered in *both* angle and turn time, indexed `i = 0..4`:

```
angle(i) = ±(20 + 5·i)      →  ±20, ±25, ±30, ±35, ±40
turn(i)  =  8 + 3·i  ticks  →    8,  11,  14,  17,  20
```

The innermost dagger turns first and the outermost last, so the volley visibly converges rather
than snapping over as one block. Side is the sign of the angle; dagger `2·i` is the left/− side and
`2·i+1` the right/+ side.

The wide spread and the early turn are one change: daggers thrown at ±20–40° leave the hand
already splayed, so they turn sooner to correct back onto the target rather than flying further out
first. `HOMING_STAGE_LIFE` must stay above the latest turn (20), which it is at 40.

### 2c. Fallbacks

- **Target vanished** (died, unloaded, left the level): stage 2 is not re-aimed and instead flies on
  along stage 1's own heading — what the spellcard does when its target disappears.
- **Trail has lost its level** (the dagger was saved mid-flight and reloaded): no stage 2, the shot
  simply ends. The level and owner are held as `transient` fields, deliberately unsaved, mirroring
  `TrailAction`'s own unsaved `cached` holder; a trail only ever runs a second or two after setup.

### 2d. Targeting

The **shared glove target cache** (glove.md §2), not a trace of its own: the client traces the
crosshair every 5 ticks while the glove is held and syncs the hit to the server, which re-validates
it on use. Homing is the second user of that cache after the doll glove, and it plugs into it by
implementing `GloveTargeting`:

| member | `DaggerGloveItem` | why |
|---|---|---|
| `targetRange()` | 64 | the mode's own reach; unchanged |
| `blockedByBlocks()` | `false` | see below |
| `targetTtl()` | 60 ticks (half a second) | see below |
| `acceptsTarget(stack, holder, candidate)` | homing mode only; not the holder, non-spectator, `IDanmakuEntity.canHurt` | allies are never homed on, and an aimed mode marks nothing |
| `targetGlow(stack)` | red, or null outside homing | the marker tells the holder what the volley will turn onto |

- **Walls do not block the trace.** The daggers turn, so refusing a target behind a wall would only
  make the mode less useful; the trace answers "what is under the crosshair", not "what can I shoot
  at". This is why `blockedByBlocks()` is a member rather than baked into the shared trace: the
  doll glove's targets must not be behind a wall, and the two rules differ.
- **The TTL is 60 ticks, not the doll glove's 100.** A homing shot fires on the *same click* that
  asks for the target, so a hint that outlived the look would send ten daggers at someone the holder
  had already turned away from; the doll glove's targets act on left-click, long after the hover,
  and want the generous window. Both sides read the same number — the marker is drawn for exactly as
  long as a use would still accept the target — so half a second of lingering is all there is, which
  is two of the shared cache's five-tick trace intervals and so never rejects a target the holder is
  still looking at.
- **The predicate is the same one the danmaku would hit anyway**: `LivingEntity`, not a spectator,
  not the holder, and `IDanmakuEntity.canHurt(holder, target)` so allied entities are never
  targeted. It is answered once, by the item, and used twice — as the client trace pre-filter and
  again on the server against the resolved hint, so the two sides cannot drift apart.
- **What the cache costs.** The target is up to one trace interval (250 ms) plus the TTL linger old
  rather than traced at the instant of the use, and the server no longer re-traces, so within that
  window a shot can be aimed at something the holder has just looked away from. With the TTL at 60
  ticks the window is half a second; the server re-checks range, level, liveness and the predicate
  anyway. In exchange the holder gets a **red outline on the target before firing**, which a
  server-only trace could never draw.
- **No target → no shot.** With nothing cached inside 64 blocks, homing fires nothing, spends no
  dagger and starts no cooldown, and tells the holder so (`no_target`). Same rule as the doll
  glove's `no_target`: a shot that cannot happen should not cost anything. Single and fan have no
  target requirement at all — they are aimed, not homed.

This replaces a fresh server-side `RayTraceUtil.rayTraceEntity` on every use. The old reasoning —
"the glove acts on *this* use, so re-tracing is both cheaper and exact" — was right about the
fresh trace but wrong about what it bought: the mode's own reach, its through-walls rule and its
accept predicate were all hard-coded into one private method, while the doll glove was already
paying for the same trace, the same TTL and the same server re-check. The one thing the shared
cache actually adds is the marker, and it costs nothing.

## 3. Ammo

The glove spends real iron daggers out of the **holder's whole inventory** (main hand, off hand,
armour, and every inventory slot), not off the held stack: the glove is the weapon, the daggers are
the ammunition.

- **All upfront.** The full count for the mode (`1` / `7` / `10`) must be present *before*
  anything fires. A partial spread is never fired — a homing volley of 4 scattered daggers at a
  64-block target is not a mode, it is a bug. If the count falls short nothing is spent, no
  cooldown starts, and the holder is told (`no_ammo`).
- One `ItemStack` of 1 count is taken per dagger, so a stack of 12 supplies two homing volleys.
- Each dagger is handed to the bullet it spawns (§4), which is what makes the count come back.

## 4. Returnable daggers

The glove reuses `IronDaggerBulletEntity` as the bullet, so the return path is already written: a
returnable dagger hands itself back on block hit, entity hit, or expiry.

**The return is transferred between stages, not decided up front.** Both homing stages are flagged
returnable, and stage 2 claims stage 1's return (`IronDaggerBulletEntity#handOffTo`) *only after it
has actually been spawned*, and takes its own returnability from the outcome of that claim. Stage 1
disarms its trail when it pays the return (`IronDaggerBulletEntity#giveBack`). Whichever way the shot
ends, exactly one dagger comes back:

| how the shot ends | what returns the dagger |
|---|---|
| normal turn, stage 2 spawned | stage 2 (stage 1 suppressed by the hand-off) |
| target died / unloaded during the outbound flight | stage 1, on expiry |
| stage 1 hit a wall or entity before its life ran out | stage 1, on the hit |
| **stage 1 hit a wall or entity on the tick its life ran out** | **stage 1, on the hit — and no stage 2 at all** |
| stage 2 hit or expired | stage 2 |
| trail lost its level (dagger reloaded mid-flight) | stage 1 |
| trail lost its first stage | stage 1 (stage 2 is spawned non-returnable) |

| | returnable | carries rune |
|---|---|---|
| single / fan (one entity) | yes | yes |
| homing stage 1 (outbound) | yes, while it still owns the return | no |
| homing stage 2 (aimed) | yes, and only if it actually claimed stage 1's | yes |

Deciding this at throw time instead — flagging stage 1 non-returnable and trusting the trail to
always produce a successor — is what the first playtest caught: **homing shots were losing daggers.**
Stage 1's non-returnable flag meant any ending where no successor appeared destroyed the dagger with
nothing to replace it. The two worst paths were

- **stage 1 hitting a wall or entity.** `BaseProjectile#tick` runs `onHit` *before* its lifetime
  check and `onHitBlock` discards immediately, so `terminate()` — and therefore the trail — never
  ran. At the original ±10–30° spread the outbound legs hit nearby geometry often enough to lose a
  noticeable number of daggers every volley. (This is also why widening the spread made the bug
  worse rather than better.)
- **the target dying during the outbound flight**, which is the entire point of a homing shot, and
  which left `reAim` with nothing to aim at.

Both are now covered: the return is claimed last, after a successor exists, so "no successor" is a
normal ending rather than a lost dagger.

### 4a. The same ordering, in the dupe direction

The fix above went too far the other way, and the second playtest found the mirror image of the
first bug: **homing shots were duplicating daggers.**

The two fixes share one cause — `BaseProjectile#tick` resolves the move vector's hit *before* it
checks the lifetime:

```java
HitResult hitresult = getHitResultOnMoveVector(this, checkBlockHit());
if (hitresult != null) onHit(hitresult);      // giveBack() + discard()
if (tickCount >= lifetime()) {
    if (level() instanceof ServerLevel) {
        projectileMove();
        terminate();                          // still runs — the entity is already discarded
        markErased(false);
        return;
    }
```

A stage 1 that hits a block *before* its final tick is discarded and never reaches `terminate`, so
it correctly ends the shot and returns its own dagger. But a stage 1 that hits on the **final** tick
goes out through *both*: `onHitBlock` hands the dagger back and discards the entity, and the tick
carries straight on into `terminate()` anyway — nothing returns after the hit. The trail then did
what it always does: spawned a stage 2 flagged returnable. Stage 1's own `handedOff` flag was never
set, and its `givenBack` flag did nothing about a *different* entity's return, so the shot handed
back two daggers for one spent.

| | dagger in | daggers out |
|---|---|---|
| stage 1 hit on the final tick | 1 | **2** |
| stage 1 expired cleanly | 1 | 1 |
| stage 1 hit before the final tick | 1 | 1 |

Which is also why it read as a terrain bug rather than an ammo bug: the window is the stage 1
final leg — 2 blocks for the innermost pair, 0.8 for the outermost — and it needs something solid in
it. Flat ground never triggers it; a corridor, a room, or any wall inside the 16-block turn radius
does, and `blockedByBlocks() == false` (§2d) means the glove will happily target someone through one.

The fix is to make the return genuinely exclusive instead of best-effort, at the one place that
knows whether the dagger is still owed:

- **`giveBack` disarms the trail.** Paying the return sets `afterExpiry = null`, and
  `ItemBulletEntity#terminate` short-circuits on that — so a first stage that hit geometry on its
  final tick has nothing left to fire. Clearing a field the dagger already owns beats overriding a
  library hook to refuse: the trail ends with the dagger rather than outliving it. And it leans on
  nothing new, because every plain single/fan dagger already has a null `afterExpiry` and already
  relies on `terminate` short-circuiting on it.
- `handOffTo` now answers its own documented contract (`false` if the claim was already taken or the
  dagger is already given back) rather than returning `true` for any spawn that happened, off a
  single private `ownsReturn()` — `returnable && !givenBack && !handedOff`.
- `DaggerHomingTrail` sets stage 2's returnability from the claim's result instead of assuming it,
  which also covers a trail that lost its `firstStage` reference through a save/load: the successor
  is then spawned non-returnable and the reloaded stage 1 hands the dagger back itself.

Rejected alternatives, for the record:

- **Overriding `terminate`.** Works, and was the first cut, but it is a second place to keep in sync
  with the tick ordering it is compensating for, and it needs an `isRemoved()` check as well to cover
  an entity discarded without a return.
- **Guarding inside `DaggerHomingTrail#execute`.** Also works, but the shot then ends with a
  non-returnable stage 2 spawned from a discarded first stage — a phantom dagger flying off from
  inside a wall, which is worse than no second stage at all.
- **Deferring the hit-path return to `markErased`** (`if (tickCount >= lifetime())` in `onHitBlock`)
  and letting the successor take it. Returns 1 on every path, but silently *loses* the dagger if the
  library ever stops calling `markErased` right after `terminate`, and it churns the single/fan hit
  path for no benefit. A silent loss is a worse failure mode than a silent dupe.

The rune rides stage 2 only, for a different reason: stage 1 spends its whole life on the outbound
run and normally hits nothing, so a rune there would almost never fire, and firing on whatever the
outbound leg clipped is not what "on hit" should mean for a mode meant to land.

One addition to `IronDaggerBulletEntity`: the rune field (§5) and the `handedOff` flag, both
carried so the transfer survives a save/load.

Creative players are **not** exempted. The dagger is the cost of the glove and a creative player
who has none cannot use it; this matches `DanmakuItem`, where the creative player still pays
unless a listener clears `DanmakuUseEvent#consume()`.

The cooldown is checked server-side rather than left to the client. A well-behaved client will not
send the use while it displays the glove as cooling down, but nothing stops a modified one from
sending it anyway, and re-firing for free would hand out daggers that were never paid for.

## 5. Rune system (port, no runes yet)

A rune is an extra on-hit effect that lengthens the shot's cooldown. Nothing implements one yet;
the plumbing exists so that adding one is a new class and a one-line registration.

- `DaggerGloveRune` — the interface. Two members: `int cooldownCost()` (added to the mode's own
  cooldown) and `onHit(...)` (called from the bullet's entity hit, with owner, target, bullet and
  level). Server-side only; the bullet is the only thing that calls it. `onHit` runs *before* the
  damage the danmaku base class applies and before the dagger is handed back.
- `DaggerGloveRunes` — the id → rune table, plus the `NONE` sentinel (id `none`, zero cooldown, no
  effect). Runes are stateless singletons; a rune that needs per-stack data is the open question
  in §7.
- The stack carries its rune as a plain id in the `DAGGER_GLOVE_RUNE` component
  (`DCVal<ResourceLocation>`), and `IronDaggerBulletEntity` carries the same id as a
  `@SerialField`. An id with no registered rune reads as `NONE`, so a rune removed from the game
  never breaks an old glove.

**The cooldown port** is one line, in `DaggerGloveMode#cooldown(ItemStack)`:

```java
return cooldown() + DaggerGloveRunes.get(rune(stack)).cooldownCost();
```

so a rune is priced purely as extra ticks and needs no changes anywhere else.

Nothing applies runes yet — there is no rune item, no crafting, no JEI. §7 lists what applying one
would need. The tooltip already has a line for a rune, which shows only when a stack actually
carries one, so a rune becomes visible the moment anything can set the component.

The glove posts no `DanmakuUseEvent`. That event prices a danmaku item's own use and lets
listeners veto or adjust it; the glove has already priced its shot two ways over (daggers spent,
cooldown started) and has no held stack to pay from.

## 6. Selector + wheel

Mirror of `DollGloveSelectionListener` (`glove.md` §3), minus the display-component trick:

- `DaggerGloveSelectionListener` — `extends IItemSelector implements WheelAdaptor.Provider`, id
  `gensokyolegacy:dagger_glove`, `test(stack)` = `instanceof DaggerGloveItem`. All three modes are
  always available, so `getList` is just the enum and `getSelHash` is constant.
- `DaggerGloveModeWheel` (`PersistentWheel<DaggerGloveModeEntry>`) + `DaggerGloveModeEntry` render
  one entry per mode; `select(index)` sends `DaggerGloveSelectPacket(0, ordinal)`.
- `DaggerGloveSelectPacket(int wheel, int index)` — server-side mode switch on the held glove.
- Data components: `DAGGER_GLOVE_MODE` (`DC.enumVal` over the enum, persistent) and
  `DAGGER_GLOVE_RUNE` (`DC.loc`).

Wheel entries render a **fresh glove stack carrying the mode**, the same trick as
`DollGloveItem.displayStack`, driven here by the `dagger_glove_display` model predicate rather than a
separate icon component, because the dagger glove's modes are not hidden from the wheel and so need
no distinct icon variants. Every mode currently wears the same glove art, so the wheel tells the
modes apart by the name it draws under the hovered entry; the per-mode overrides stay so that
distinct per-mode art can be dropped into the four texture paths without touching the wheel.

### 6a. Held model

The glove is a modelled mitten while held and a flat sprite everywhere else, so it is a
`neoforge:separate_transforms` model: the base is `item/tool/dagger_glove.png` (gui, wheel,
sidebar) and each of the four hand displays swaps in the shared mitten, skinned with this glove's own
`item/tool/dagger_glove_hand.png`.

The mitten is the doll glove's — one geometry, one set of Blockbench display transforms, in
`models/custom/glove_hand.json`, since the two gloves are the same mitten in different colours. It is
shared through `content/item/glovehand/GloveHandModel.java` rather than by either glove's package,
which the §9 split forbids; `GloveHandModel.perspectives` is the whole of it, and it takes the
glove's mitten skin as an argument and writes it over the shared model's `#0`. The doll glove is the
one whose skin is baked into `glove_hand.json`, so it renders correctly even unoverridden.

Per-mode held overrides are separate-transforms models too, not flat ones: vanilla replaces the whole
item model when a predicate matches, so a flat override would silently drop the mitten while in hand.
The dagger glove has no icon variants, so it has four models in all (base plus three modes).

## 7. Open questions

- **Cooldown is per item, not per mode.** `ItemCooldowns` is keyed by `Item`, so a homing shot's
  20 ticks is still running after switching to single. Deliberate: clearing the cooldown on
  switch would let a player bypass homing's cooldown by toggling. Per-mode cooldowns need a
  player-side map and a decision on persistence across logout.
- **Applying runes.** Nothing applies one. The cheapest route is a second data component written
  by an anvil/enchanting interaction; a rune with per-stack data additionally needs a codec on the
  component rather than a bare id.
- **Rotation while flying.** A homing dagger's turn is a hard direction change at the stage
  boundary, which is what lets the flight stay a straight line at a constant speed (§2a-1). A short
  accelerate-into-the-turn (a `RectMover`, like the spellcard) would read as a missile boost, but it
  reintroduces the mover that §2a rules out, so it is not done.
- **The turn is one sample, not a track.** Stage 2 is aimed at where the target is at the turn tick
  and does not follow it afterwards, so a target that keeps running sidesteps it. This is the
  direct price of the client/server consistency in §2a: following the target to the end means
  steering on a live position every tick, which is exactly the thing both sides disagree about. If
  this reads as too weak in play, the fix is more *stages* (re-aim at 10, 14, 18, 22 ticks), not a
  steering mover — each re-aim is another server-sampled kink in the same fixed path.
- **A dagger that has flown past its target** keeps going along the line it was last aimed down
  instead of turning back. The collision check is what actually lands the shot, so this only
  becomes visible once something lets a dagger fly through entities.
- **`IronDaggerItem` reach.** The plain dagger throws one dagger; the glove's single mode does the
  same job on a 5-tick cooldown. If the glove makes the plain dagger pointless, the dagger's
  cooldown is the thing to raise, not the glove's.

## 7a. Lang

zh_cn is hand-authored in the split per-category files, then merged by the
`organize.ResourceOrganizer` main:

- `src/test/resources/gensokyolegacy/lang/zh_cn/items.json` — `item.gensokyolegacy.dagger_glove` =
  匕首手套, under the `item.gensokyolegacy` object next to `iron_dagger`.
- `src/test/resources/gensokyolegacy/lang/zh_cn/main.json` — a `dagger_glove` object beside `glove`,
  mirroring the `GLLang.ItemDaggerGlove` keys.

## 8. Registration

- `GLItems` — `DAGGER_GLOVE` = `reg.item("dagger_glove", p -> new DaggerGloveItem(p.stacksTo(1)))`,
  model = `DaggerGloveModel::model` (§6a): a `neoforge:separate_transforms` model whose base is
  `item/generated` and whose four hand displays carry the shared mitten, with three ascending
  `dagger_glove_display` overrides onto `item/dagger_glove_<mode>` sub-models (also
  separate-transforms, so the mitten survives the override), `.lang("Dagger Glove")`, tab `TAB`,
  plus the `DAGGER_GLOVE_MODE` (`DC.enumVal`, persistent) / `DAGGER_GLOVE_RUNE` (`DC.loc`)
  components. Tagged `L2ISTagGen.SELECTABLE` so the wheel offers it, like the doll glove.
- Mod constructor — `DaggerGloveSelectionListener.register()` beside
  `DollGloveSelectionListener.register()`; `GensokyoLegacy.HANDLER` registers
  `DaggerGloveSelectPacket`. The target cache needs nothing of its own: `GloveTargetPacket`,
  `GLMeta.GLOVE_TARGET` and the client tick all come with the doll glove (§2d).
- `GLLang.ItemDaggerGlove` — mode names and descriptions, the rune line, `no_dagger`, `no_target`.
  The tooltip reuses `GLLang.ItemGlove.WHEEL` for the "hold the wheel key" hint rather than
  duplicating the string.
- `GLClient` — `ItemProperties.register(GLItems.DAGGER_GLOVE.get(), gensokyolegacy:dagger_glove_display, ...)`,
  returning the mode ordinal + 1 so each held mode picks its own texture override. The red target
  marker comes from the shared glow rule instead of a new one (§2d).
- Textures — `textures/item/tool/dagger_glove.png` plus `_single` / `_fan` / `_homing` (the four flat
  sprites), and `textures/item/tool/dagger_glove_hand.png` (this glove's mitten skin, §6a).

## 9. Files

The glove lives in its own package under `item/`, not under `item/glove/` — that package is the
doll glove's. The only code the two share sits in two shared packages: the target cache in
`content/item/targeting/` (§2d) and the held mitten model in `content/item/glovehand/` (§6a).

- `content/item/dagger/DaggerGloveItem.java`
- `content/item/dagger/DaggerGloveMode.java`
- `content/item/dagger/DaggerGloveModel.java` — item model generation, the three modes on top of the
  shared mitten (§6a, §8)
- `content/item/dagger/DaggerHomingTrail.java` — the stage boundary: fires stage 2 and claims the
  first stage's return (extends danmaku_api's `TrailAction`)
- `content/item/dagger/DaggerGloveRune.java`, `DaggerGloveRunes.java`
- `content/item/dagger/DaggerGloveSelectionListener.java`
- `content/item/dagger/network/DaggerGloveSelectPacket.java`
- `content/item/dagger/client/DaggerGloveModeWheel.java`, `DaggerGloveModeEntry.java`
- `content/item/targeting/GloveTargeting.java` (shared with the doll glove, §2d) — the client trace
  and the server store it drives are not this glove's and are not listed here
- `content/item/glovehand/GloveHandModel.java` (shared with the doll glove, §6a) — the mitten
  geometry and the separate-transforms wiring, neither of which is this glove's
- `content/entity/misc/IronDaggerBulletEntity.java` (edited: `handOffTo` return transfer, `giveBack`
  disarms the trail, rune id field, rune on hit)
- `init/registrate/GLItems.java`, `init/data/GLLang.java`, `init/GLClient.java`,
  `init/GensokyoLegacy.java` (edited)
- `textures/item/tool/dagger_glove{,_single,_fan,_homing}.png` (one glove, copied to all four flat
  paths) and `textures/item/tool/dagger_glove_hand.png` (this glove's mitten skin)
- `models/custom/glove_hand.json` (edited: was `doll_glove_hand.json`, now shared — the display
  transforms retuned and a `particle` key added, with the doll glove's skin as the baked-in default)

## 10. Verified

- `./gradlew build` clean; `./gradlew runData` clean, with the generated `models/item/dagger_glove*.json`,
  the `l2itemselector` selectable tag and the `en_us`/`en_ud` lang entries written; the merged
  `lang/zh_cn.json` carries both the item name and the mode strings.
- Two-stage homing geometry checked numerically outside the game: stage 1's speed is `16/delay`
  and every one of the five pairs turns at exactly 16.00 blocks (verified for all of 8/11/14/17/20),
  and stage 2's heading is computed from the turn point to the target's *current* position.
- Return accounting modelled the same way, over every path a shot can end. The transfer returns
  exactly 1 dagger on all of them (target died mid-flight, trail lost its level, degenerate heading,
  normal turn); the same model with stage 1 flagged non-returnable returns **0** when no successor
  appears, which is the playtest bug reproduced in isolation.
- The §4a dupe path modelled the same way, from the ordering in `BaseProjectile#tick` rather than
  from the design's intent: a stage 1 hitting a block or entity on its final tick returns **2**
  daggers out of one spent, while the same hit before the final tick and a clean expiry both return
  1. Disarming the trail in `giveBack` makes all three return 1, and every "no successor" path still
  returns 1.

Not yet verified in game: the strange-speed fix (§2a — the mover removal), the new homing spread,
turn timing and `16/delay` reach, the feel of the turn, whether a single re-aim reads as "homing" or
as a bend, whether 64 blocks is the range players expect, the wheel's behaviour, and the cached
targeting (§2d) — in particular whether the red marker appears soon enough to feel like an aim, and
whether a shot at a target the holder has just looked away from reads as wrong. Neither the dagger
loss nor the §4a duplication is confirmed fixed in game — worth watching the inventory count through
a homing volley at a target you can kill mid-flight, and then through the same volley fired down a
corridor, which is what the duplication needed.