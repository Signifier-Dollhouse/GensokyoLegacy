# Dagger Glove

`DaggerGloveItem` — the **Dagger Glove** — turns the returnable `IronDaggerItem` into a
three-mode thrown weapon: one mode per spread, driven by the l2itemselector wheel exactly as
`DollGloveMode` is.

> Status: implemented. Textures are generated placeholders
> (`textures/item/tool/dagger_glove*.png`) — replace with real art when available (16×16, same paths).

The glove is the *second* way to throw a dagger. The plain `IronDaggerItem` stays the single-dagger
weapon; the glove is what turns a stack of daggers into a pattern, at the cost of a cooldown per
shot instead of a cooldown per dagger.

It lives in `content/item/dagger/`, its own package under `item/`: `content/item/glove/` belongs to
the doll glove and the two share nothing but the word.

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

A fresh **server-side** ray trace on every use, 64 blocks
(`RayTraceUtil.rayTraceEntity`), not a client cache:

- no client → server target packet, no cache TTL, no staleness. The doll glove needs a cache
  because its targets act on *left*-click, long after the hover; the glove acts on *this* use, so
  re-tracing is both cheaper and exact.
- The predicate is the same one the danmaku would hit anyway: `LivingEntity`, not a spectator,
  not the holder, and `IDanmakuEntity.canHurt(holder, target)` so allied entities are never
  targeted.
- Walls do **not** block the trace. The daggers home, so refusing a target behind a wall would
  only make the mode less useful; the trace answers "what is under the crosshair", not
  "what can I shoot at".

**No target → no shot.** With nothing under the crosshair inside 64 blocks, homing fires nothing,
spends no dagger and starts no cooldown, and tells the holder so (`no_target`). Same rule as the
doll glove's `no_target`: a shot that cannot happen should not cost anything. Single and fan have
no target requirement at all — they are aimed, not homed.

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
has actually been spawned*. Whichever way the shot ends, exactly one dagger comes back:

| how the shot ends | what returns the dagger |
|---|---|
| normal turn, stage 2 spawned | stage 2 (stage 1 suppressed by the hand-off) |
| target died / unloaded during the outbound flight | stage 1, on expiry |
| stage 1 hit a wall or entity first | stage 1, on the hit |
| stage 2 hit or expired | stage 2 |
| trail lost its level (dagger reloaded mid-flight) | stage 1 |

| | returnable | carries rune |
|---|---|---|
| single / fan (one entity) | yes | yes |
| homing stage 1 (outbound) | yes | no |
| homing stage 2 (aimed) | yes, and claims stage 1's | yes |

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

Wheel entries render a **fresh glove stack carrying the mode**, so the wheel shows the held
glove's texture for each mode — the same trick as `DollGloveItem.displayStack`, driven here by the
`dagger_glove_display` model predicate rather than a separate icon component, because the dagger
glove's modes are not hidden from the wheel and so need no distinct icon variants.

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
  model = `item/generated` with three ascending `dagger_glove_display` overrides onto
  `item/dagger_glove_<mode>` sub-models, `.lang("Dagger Glove")`, tab `TAB`, plus the
  `DAGGER_GLOVE_MODE` (`DC.enumVal`, persistent) / `DAGGER_GLOVE_RUNE` (`DC.loc`) components.
  Tagged `L2ISTagGen.SELECTABLE` so the wheel offers it, like the doll glove.
- Mod constructor — `DaggerGloveSelectionListener.register()` beside
  `DollGloveSelectionListener.register()`; `GensokyoLegacy.HANDLER` registers
  `DaggerGloveSelectPacket`.
- `GLLang.ItemDaggerGlove` — mode names and descriptions, the rune line, `no_dagger`, `no_target`.
  The tooltip reuses `GLLang.ItemGlove.WHEEL` for the "hold the wheel key" hint rather than
  duplicating the string.
- `GLClient` — `ItemProperties.register(GLItems.DAGGER_GLOVE.get(), gensokyolegacy:dagger_glove_display, ...)`,
  returning the mode ordinal + 1 so each held mode picks its own texture override.
- Textures — `textures/item/tool/dagger_glove.png` plus `_single` / `_fan` / `_homing`.

## 9. Files

The glove lives in its own package under `item/`, not under `item/glove/` — that package is the
doll glove's, and the two gloves share nothing but the word.

- `content/item/dagger/DaggerGloveItem.java`
- `content/item/dagger/DaggerGloveMode.java`
- `content/item/dagger/DaggerHomingTrail.java` — the stage boundary: fires stage 2 and claims the
  first stage's return (extends danmaku_api's `TrailAction`)
- `content/item/dagger/DaggerGloveRune.java`, `DaggerGloveRunes.java`
- `content/item/dagger/DaggerGloveSelectionListener.java`
- `content/item/dagger/network/DaggerGloveSelectPacket.java`
- `content/item/dagger/client/DaggerGloveModeWheel.java`, `DaggerGloveModeEntry.java`
- `content/entity/misc/IronDaggerBulletEntity.java` (edited: `handOffTo` return transfer, rune id
  field, rune on hit)
- `init/registrate/GLItems.java`, `init/data/GLLang.java`, `init/GLClient.java`,
  `init/GensokyoLegacy.java` (edited)
- `textures/item/tool/dagger_glove{,_single,_fan,_homing}.png` (placeholders)

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

Not yet verified in game: the strange-speed fix (§2a — the mover removal), the new homing spread,
turn timing and `16/delay` reach, the feel of the turn, whether a single re-aim reads as "homing" or
as a bend, whether the 64-block trace is the range players expect, and the wheel's behaviour. The
dagger loss is fixed and modelled but **not** yet confirmed in game — worth watching the inventory
count through a homing volley at a target you can kill mid-flight.