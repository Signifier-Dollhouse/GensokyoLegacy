# Doll Control — item behaviors, commands, iterations

"Player-hosted dolls have behaviors which depend on the items they hold." Commands come only from the player (glove) — a doll **never acts or switches items on its own**. Two things escape the command path without breaking that rule: a shield held in the off hand blocks projectiles reactively (blocking never moves items), and `HEAL` is scheduler-issued (ordered by the ledger tick, never by the doll itself).

> Status: framework implemented (registry, behaviors, ticket queue, shield, scheduled heal, iteration, revival). Glove (`glove.md`) planned — it will call `commands.issueVolley` / `commands.issueOneTime` / `commands.stopAll`.

## 1. Framework — two levels, mirrored from MobWeaponAPI, doll-local

Same split as MobWeaponAPI's `WeaponRegistry` → `WeaponGoalRegistry` (see pairing.md §1 for the shared history), but implemented in `content/entity/dolls/action/` with no dependency on the API:

1. **Item → behavior binding** (`DollBehaviorRegistry`): entries map *held items* to execution. One entry = `(id, ItemStack predicate, DollActionType, priority, behavior factory)`, registered once at startup. The matched entry drives both capability checks and execution — behaviors are created per execution from the available items, so the doll stores none and per-run state can never go stale. A new item feeding an existing action is one registration line, no behavior code.
2. **Action → execution** (goal-like `DollBehavior`s on `DollEntity`, §5, under one delegating `Goal`): one behavior per action type, gated on the current command (§4). Behaviors read the **live ledger loadout** in `canUse`, so loadout changes need no propagation at all — simpler than ModularGolems' `setItemSlot → reassessWeaponGoal` deferral, which exists only because golem goals cache their weapon.

Deliberate divergences from MobWeaponAPI / ModularGolems:

- **No dependency**: GensokyoLegacy does not jarJar MobWeaponAPI; the pattern is mirrored doll-side. Reasons: hard-dep weight for every player; the API assumes vanilla equipment slots and ground movers; and its switch policy (`AbstractWeaponManager.checkSwitch`) is exactly what dolls must not do.
- **No auto-switch, ever**: `checkSwitch`-style reassignment is banned. The only item movement in the system is the command-driven sticky swap (§2) plus the future arming system (loadout.md §4).
- **Closed command vocabulary, open item set**: `DollActionType` stays an enum because the glove speaks it (fixed UI), while *which items satisfy each type* is registry-open. A future core action adds an enum value + goal + glove mode.

## 2. Hands, active hand, and the sticky swap

- The doll **acts with its main hand**. `findHand(doll, type)` scans both hands for the highest-priority registry match — e.g. a laser in the off hand beats hexbrew in the main for `SUPER_ATTACK`.
- If the match is in the off hand, the behavior swaps the ledger first (`DollEntity.swapHands()` + mirror refresh), then acts. The swap **sticks** until a further off-hand command swaps back: there is no active-hand flag; main-hand-ness *is* the state.
- Shield is off-hand-native and never swaps. An off hand holding a shield simply fails `findHand` for attack actions — losing the slot *is* losing dual-wield; no special rule needed.
- CLOTH/CORE hold no items (loadout.md) and have no behaviors.

## 3. Action set and modes

| Type | Capability (registry predicate) | Consumes |
|---|---|---|
| `SUICIDE` | either hand `Items.TNT` | the TNT stack (§5.4) |
| `SUPER_ATTACK` | `LaserItem` anywhere, else throwable `HexBrewBottleItem` (cf. `HexBrewBottleItem.use`) anywhere | the laser/hexbrew stack |
| `REGULAR_ATTACK` | `DanmakuItem` in either hand | nothing |
| `HEAL` | folded heal talisman in either hand, with remaining durability | talisman durability (§5.5; scheduler-issued auto only) |
| shield block | off hand shield | shield durability (§5.6, reactive — never queued) |

There is no priority between types: each doll holds a single ticket (§4). `HEAL` is never player-queued; the scheduler issues it as an auto order.

`DollActionMode` — issuance mode, carried on the command:

- `ONE_TIME` — one glove command, one doll, one execution (super, suicide).
- `ITERATIVE` — one glove command fans across dolls, each doll at most once (regular volley only, §8).
- `AUTO` — like `ONE_TIME` (one doll, one execution, no chaining) but a player command may interrupt it; scheduler-issued heals use this mode (light blue sidebar frame).

## 4. The ticket

`DollAction` record:

```java
record DollAction(DollActionType type, DollActionMode mode, @Nullable UUID target, Set<UUID> done) {
    // done is the shared iterative progress set (§8); empty (and unused) for ONE_TIME / AUTO.
    // Shared by reference across the dolls of one volley — mutated, never copied.
}
```

`DollActionHandler` (`content/entity/dolls/action/`, transient field on **`DollEntity`** — loadout and actions are doll-specific logic, so per entity.md §8 this lives beside the synced slots, not on the pairing base):

```java
// server-only, never persisted
public final DollActionHandler actions = new DollActionHandler();
```

- `boolean isActive()`, `@Nullable DollAction getCurrent()`
- `boolean tryStart(DollAction, gameTime)` — takes the ticket: idle dolls accept; a running `REGULAR_ATTACK` or a running `AUTO` ticket is aborted for the new order; anything else running refuses. Re-issuing the current order is a no-op success. The dropped behavior is stopped by the delegating goal on its next pass; the orphaned action (if iterative) is picked back up by the stall guard.
- `void stop()` — releases the ticket (glove *stop* mode); the running behavior sees the current vanish and aborts. Also stamps a short suppress (so a scheduled heal doesn't re-fire on the very next tick against a still-valid target).
- `boolean canAccept(DollAction)` — `findHand` hits either hand (an off-hand hit implies the sticky swap at start); a ticket held by anything but an abortable regular/auto refuses; that `(type, target)` is not already current; and (for `ITERATIVE`) the doll is not in `done`. Used to find "available" dolls (§8).
- Cooldowns: transient `Map<DollActionType, Long>` of gameTime stamps on the handler (dolls are never chunk-serialized and commands are ephemeral, so nothing persists). Heal needs none of its own — its 100-tick cooldown lives target-side (`TalismanContext`, §5.5).
- KAMIKAZE flag: transient marker for the suicide dive (§5.4). Set by the suicide behavior on start alongside `becomeStray()`, cleared on complete/abort/`stop()`. Mostly a state marker — leash and pullback exemptions are structural once the entry is detached.
- No `tick()` driver: behaviors self-drive through the delegating goal (§5). Completion releases the ticket (`complete()`); for `ITERATIVE` it first hands the action off (§8).

## 5. Execution as vanilla goals

Each action is a goal-like `DollBehavior` sharing an abstract base (current-command access, target resolution, face/move helpers via the existing `DollMoveControl`/`FlyingPathNavigation`). Exactly one `DollCommandGoal extends Goal` is registered in the selector at priority **0** — above follow (1) and look (99999) — with `MOVE`/`LOOK` flags, so an acting doll automatically yields follow/look with no `isActive` guard. On start the goal constructs the behavior for the command from the held items (`createFor`) and delegates every lifecycle call to it, dropping the instance on stop.

Targeting: actions carry a `target UUID` resolved server-side each tick (`level.getEntity(uuid)`); a vanished target aborts and the handler advances. The glove supplies the UUID from its cached ray-trace target (glove.md §2, ≤48 blocks, attack-valid: no allies, no dolls, attackable only, with a server-side re-check); the cache is a hint, never authority. Exactly one execution per doll per command, no re-targeting.

Movement bounds: an acting doll never leaves a 10-block radius around its owner and never chases unreachable targets — if the target is beyond weapon range and closing would breach the leash, the goal aborts immediately rather than pursuing. Per-action ranges (doll→target): danmaku 48, laser 40, hexbrew 16, heal approach-to-2; suicide is contact range and exempt via KAMIKAZE. The 48-block ledger pullback (§10) stays as the outer net and never trips mid-action inside these bounds.

Ally lanes: all ranged attacks check the firing lane before firing. Allies are the owner plus fellow summoned dolls (never the target itself); a lane is blocked when an ally body comes within hit margin of the doll → target segment. A blocked doll strafes — sidesteps away from the blocker at hover height, clamped into the leash — instead of firing into its own team (`DollFriendlyFire`).

Start timeouts: an accepted-but-unstarted ticket is timed in the delegating goal. An iterative regular hands ahead after 1s stalled and aborts (done + release) after 3s; anything else aborts after 2s. The same hand-ahead fires while strafing post-start (first second of sidestep only, shared flag) — so a lane-blocked volley keeps moving and the waiter either shoots late or skips at ~2s blocked. Aborts run the normal `complete()` path, so chains keep moving and no ticket wedges.

Danmaku/laser spawning uses `DollCardHolder` (`implements CardHolder`, mirror `YoukaiCardHolder` at `content/entity/behavior/combat/YoukaiCardHolder.java`): `self() = doll`, `center()` = doll body center, `forward()/target()` from the resolved action target, default danmaku damage source. Shot damage is the bullet's base `DanmakuBullet.damage()` (dolls carry no attack-damage attribute).

### 5.1 `REGULAR_ATTACK` — Danmaku

One danmaku, **item not consumed**.

- Capability: either hand holds a `DanmakuItem` (danmaku_api; `type`/`color` are public fields).
- Behavior: fire immediately on start → 10-tick wind-down → `complete()` (handoff for volleys, §8). No wind-up. Strafed long (1–2s of sidestepping): shoot, then complete immediately — the wind-down wait is skipped so the handoff isn't delayed.
- Shot: `holder.prepareDanmaku(life, dir, type, color)` with `dir` from `DollShootUtils.predictShotDir` (laser-style: flight ticks from distance ÷ shot speed, `predictCenter`, one refinement pass; fired at that same **speed 1**), `life = 60`; `holder.shoot(e)`.
- Blocked lane: strafe instead of firing; after ~2s continuously blocked, skip the shot (complete without firing) so the volley keeps moving.
- Cooldown: short handler cooldown so a volley doesn't instantly re-fire.

### 5.2 `SUPER_ATTACK` — Laser

One laser, **consuming the laser stack** at emission.

- Capability: `LaserItem` anywhere (beats hexbrew by registry priority even from the off hand).
- Goal phases: **setup 5 ticks** (stop, face, no laser) → **duration 40 ticks** → **close 10 ticks** (hold while the visual fades).
- At setup end: `holder.prepareLaser(40, pos, dir, len, type, color)` (life 40 == duration), `dir` aimed at the 0.5s motion prediction (`DollShootUtils.predictCenter`), length ~40; consume then.
- Blocked lane: strafe during setup (counter paused); past ~2s cumulatively blocked, skip the check and fire anyway. Once emitted, the beam runs to completion regardless.
- Ticket cost: only stop/suicide (both player-issued) can interrupt; ammo committed at emission is then wasted. Accepted — the ticket is a leash, not a transaction.
- Future upgrade (noted, not planned): track the target during the duration and re-aim via a homing/`AttachedFreeRotMover` mover like `MarisaItemSpell`.

### 5.3 `SUPER_ATTACK` — Throwable Hexbrew

Throw the hexbrew, **consuming one bottle**.

- Capability: no laser anywhere, but a throwable `HexBrewBottleItem` in either hand.
- Behavior: face target → throw → done. Blocked lane: strafe instead of throwing; past ~1s cumulatively blocked, skip the check and throw anyway.
- Throw: `HexBrewBottleEntity` from the bottle stack at hand height, aimed by `DollShootUtils.shootAimHelper` (velocity ~1.5F, straight with lead — no arc compensation, same drop as before), `setItem(stack)` + `addFreshEntity`. Explosion/effect handlers run exactly as player-thrown ones.

### 5.4 `SUICIDE_ATTACK` — TNT

Fly into the target and explode. **Always destroys the doll; the stray death drop returns the item form.**

- Capability: `Items.TNT` in either hand.
- On start the behavior raises the handler KAMIKAZE flag **and cuts the doll from pairing** (`DollEntity.becomeStray()` → `DollHost.detach` into a `StrayHost`): the ledger entry is removed with no item produced. From here the doll is stray — pairing queries resolve against the host, so no sync, no pullback, no commands.
- Goal: dive at the target at full `MAX_SPEED` with no leash; within blast range (≤ 2 blocks or contact) → the explosive-hexbrew blast itself (`HexBrew.EXPLOSIVE_HEXBREW.handler.onHit` with the doll as thrower: power 4, terrain kept, thrower and allies excluded) — the doll is excluded as thrower, so its death is guaranteed by the fallback below, not the blast.
- Afterwards (server): consume the TNT from the loadout, then guarantee death (`hurt(genericKill, MAX)` if the blast didn't finish it) → `DollHost.onDeath` (only `StrayHost` acts: drops the item form) and the entity is gone. Abort (target vanished/unloaded) leaves a stray ronin: it keeps following its owner on stray-routed combat, commands can't reach it, and only death ends it — persisting in chunks until then.

### 5.5 `HEAL` — folded heal talisman, scheduler-issued auto

Apply the talisman's own effect on the scheduled target (§6). There is no glove heal command.

- Capability: either hand holds a folded heal talisman (`FoldedPaperTalisman` whose paper is a `HealTalisman`) with remaining durability.
- Goal: navigate to within ~2 blocks of the target (self needs no move) → stop → trigger on the **live** held stack: `new TalismanContext(target, heldStack, 0, heldStack, paper)` (same worn-directly shape as `FoldedPaperTalisman`, so `hurtItem()` wears the ledger stack in place) → `paper.trigger(ctx)` (`HealTalisman`: heal 30% max, 100-tick target-side cooldown, 1 durability use) → `complete()`.
- If the target vanished, is full health, or the talisman broke mid-approach, the action is a no-op pop.
- An auto heal holds its ticket to completion like anything else, but any player command aborts it the same way a running regular is aborted — only `stop()` or a preempting order interrupts it, and it is short anyway.

### 5.6 Shield block — reactive, off hand, regular vanilla pipeline

Blocking runs through the regular `LivingEntity.hurt` shield path, not a custom hook: `DollEntity` overrides `isBlocking()` (off-hand loadout shield present and block cooldown ready), `isDamageSourceBlocked()` (projectile-only on top of vanilla bypass/pierce/front gating), and `hurtCurrentlyUsedShield()` (wears the live ledger shield in place + mirror refresh + cooldown stamp). Blocked hits never reach `actuallyHurt`, so the pairing pipeline and combat tracker stay silent; melee and explosions are never blocked, and the axe-disable branch can't fire (it requires a melee blocker). No modded-shield support for now (open question: tag-based match).

## 6. Heal scheduling and heal marks

`HEAL` is issued by the scheduler tick as a one-time order, never by the glove or the doll. In `DollCommander.tick` (server, per player, via the attachment tick), a cheap pass over idle `SUMMONED` dolls holding a usable heal talisman (and not suppressed by a recent stop):

```
for doll in summoned dolls, idle and not suppressed:
    target = owner if hurt
        else doll itself if hurt
        else first marked target, alive and damaged, within ~16, target-side cooldown expired
    if target != null: doll.actions.tryStart(HEAL/AUTO -> target)
```

Target priority is **owner > self > marked**. `HealTalisman.test` (damaged + alive) is the validity check everywhere; the 100-tick repeat guard is target-side (`TalismanContext.isOnCooldown`), so the ledger keeps no heal timestamps.

Marks are session-local targeting state on the commander — forgotten on logout / death, never serialized:

```java
// DollCommander, plain field — NOT @SerialField
public final Set<UUID> healTargets = new LinkedHashSet<>();
```

- Glove heal-mark mode (glove.md §1): `use()` toggles the cached target's UUID in `healTargets` (+ chat feedback). Any living entity, same 48-block pipeline as attacks.
- A mark is the target's entity UUID. Dead or vanished marks are trimmed once per second across all loaded dimensions (`pruneHealMarks`); scheduling skips the rest every tick. Marks die with the session (logout / death clears them); the exact same UUID can't reappear legitimately, so stale marks are harmless.
- `DollCommander.isMarked(LivingEntity)` helper for the scheduling pass.

## 7. Itemization and persistence — gear never drops loose

All loadout items live in data (`DollData.inventory` ↔ `DOLL_LOADOUT`). The only world drop in the whole system is the stray death drop (§5.4):

- **Normal itemize** (`interact`, recall, park→`STORED`→item): gear rides the item untouched. `park`/`TEMP`/`SUMMONED` transitions keep `DollData.inventory` as before.
- **Suicide itemize**: gone — the doll goes stray at dive start (§5.4) and the death drop returns the 0-hp item with remaining gear. Nothing is ever handed to the player directly.
- **Revival**: `DollAttachment.summon` treats `combat.amount() <= 0` as fresh (`DollItemData.fresh()` combat, gear intact) — persist everything, no kamikaze loadout price. All other transitions already preserve health and gear.
- **Clone duplication**: cloning a doll item (e.g. creative) clones its gear too — accepted, same precedent as shulker boxes. The pairing invariant (pairing.md §5) covers doll *identity*, which gear never participates in. Ammo can never duplicate through commands: TNT/laser/hexbrew are consumed from the live ledger stack, talisman/shield wear lands on the live stack.

## 8. Iterative command sequences — doll-to-doll handoff

`ITERATIVE` (regular volley only, for now) fans one command across dolls with no central cursor: **a doll performs the action, puts itself into the action's `done` set, then finds the next available doll; if none is available, the iteration stops.** Every command executes at most once per doll — continuous fire with automatic targeting is future work (§10).

- Glove *volley* (glove.md §1): find the first summoned doll with `actions.canAccept(volley)` and start it with an empty shared `done` set. No doll available → `doll_glove.no_doll` message.
- On `complete()` of an `ITERATIVE` action, the performing doll adds its own uuid to `done`, then scans ledger-order `SUMMONED` dolls for the first one whose `actions.canAccept(sameAction)` holds (alive, capable hand, ticket free or abortable regular, not in `done`) and starts the *same action instance* (shared set) on it. Spacing between dolls falls out of execution time (~1s per danmaku) — no timer. Early goal stops (target lost mid-execution) and unstartable waits (dead target, lost capability) release through the same `complete()` path, so chains never stall on yellow.
- Stall guard: if a doll holding a current `ITERATIVE` action leaves `SUMMONED` (park/itemize) before completing, the commander hands the action (with its `done` set) to the next available doll on the following tick, so a parked mid-volley doll can't strand the chain.
- Hand-ahead: an accepted-but-unstarted volley action that still hasn't started after 1s tells the next available doll to go ahead (without releasing its own wait or touching `done`); after 3s unstarted it aborts via `complete()` (done + release, chain moves on). Other unstarted tickets abort the same way after 2s.
- `ONE_TIME` (super/suicide) and `AUTO` (scheduled heal) tell exactly one available doll and never chain — glove super/suicide pick the doll **randomly**. Glove *stop* calls `commands.stopAll` (per-doll `actions.stop()` + suppress stamp, in-flight set cleared; suicide dives are skipped, summoned only, strays and block-hosted untouched).

## 9. Files created / modified

Created (`content/entity/dolls/action/`, behaviors in `.../behavior/`, all goals in `.../goals/`) — follow/look goals carry vanilla-standard flags (`MOVE`+`LOOK` / `LOOK`) so the single command goal outranks them in the selector:

- `DollActionType.java` — closed command enum (SUICIDE, SUPER_ATTACK, REGULAR_ATTACK, HEAL; no priority — single ticket)
- `DollActionMode.java` — `ONE_TIME`, `ITERATIVE`, `AUTO` (§3)
- `DollAction.java` — `record DollAction(DollActionType type, DollActionMode mode, @Nullable UUID target, Set<UUID> done)` (shared-mutable `done` for `ITERATIVE`, §8)
- `DollActionHandler.java` — single-ticket state + cooldowns + suppress stamp + KAMIKAZE flag (§4)
- `DollBehaviorRegistry.java` (`behavior/`) — `(id, predicate, type, priority, factory)` entries; `findHand` for capability, `createFor` for per-execution construction (§1–2)
- `DollBehaviors.java` — built-in registration (danmaku / laser / hexbrew / TNT / heal-talis­man / shield-offhand)
- `DollCardHolder.java` — `implements CardHolder` for the doll (§5)
- `DollBehavior.java` + `DollDanmakuBehavior`, `DollLaserBehavior`, `DollThrowBehavior`, `DollSuicideBehavior`, `DollHealBehavior` (`behavior/`, §5; laser before throw for super)
- `DollFriendlyFire.java` (`behavior/`) — ally lanes, blockage test, leash-clamped strafe (§5)
- `goals/DollCommandGoal.java` — the single delegating `Goal`: behavior cache, start timeouts, stop completes the still-held ticket (§5)

- `DollCommander.java` (`content/attachment/doll/`, via `DollAttachment.commands`) — volley/one-time/stop, handoff, stall guard, heal scheduling + 1-second mark prune, transient heal marks (§6/§8)
- `DollShootUtils.java` (`util/`) — trimmed copy of MobWeaponAPI's `ShootUtils` aim helpers (target lead, gravity arcs; arrow/infinity parts dropped), used for danmaku aim and hexbrew throws

Modify (all done):

- `DollEntity` — `actions` field, one `DollCommandGoal` at selector priority 0, vanilla shield hooks (§5.6), never-null ledger-direct loadout API, `becomeStray()`
- `BaseDollEntity` — pairing pipeline only (plus the stray `getHost` branch and `die()` → `onDeath` fan-out); empty-hand itemize (arming removed, loadout.md §4)
- `DollHost` — `detach(UUID)` for stray cuts, `onDeath` hook (only `StrayHost` acts); impls on the attachment and the controller; `StrayHost` holds the detached entry, answers pairing queries, and persists it via chunk save/load
- `DollAttachment` — destroyed-resummon revival (§7); ledger transitions only, command logic lives in the commander
- `DollItem`/`DollData` — revival keeps gear (§7)
- `GLLang` — glove + feedback messages (glove.md §1)

## 10. Edge cases & open questions

- **Ticket cost**: aborting a running regular wastes its progress; committed ammo (laser at emission, thrown bottle, TNT) is wasted if `stop()` lands after the commit — both player-issued either way. An auto heal holds its ticket to completion; only `stop()` or a preempting player order interrupts it. Accepted.
- **Blocked-lane skip**: a danmaku skipped after ~2s blocked completes without firing (chain moves, no friendly fire); laser fires anyway past ~2s strafing and hexbrew past ~1s (explicitly ordered strikes). Volley spacing absorbs the difference.
- **Mid-volley re-summons**: the `done`-set chain doesn't snapshot, so a re-summoned doll simply becomes eligible again (its new uuid isn't in `done`) — the next handoff can pick it up. No stale-snapshot problem by construction.
- **A doll with no relevant item is skipped** by `canAccept` — quiet skip; the chain ends when a full scan finds no acceptor. A volley with zero armed dolls posts a `doll_glove.no_doll` message at issue time.
- **Suicide while block-hosted**: `itemize` needs the player ledger; block-hosted dolls can't be recalled to item form. Suicide commands are only issued over the player ledger, so this never arises; block-hosted dolls keep `mobInteract`'s existing no-itemize rule.
- **Destroyed-doll revival vs. health-at-zero**: bounded exception — revival spawns *fresh* health with gear intact instead of a 0-HP corpse-cycle (§7). All other transitions stay health-preserving.
- **Gear vs. dup protection**: gear never leaves data (§7); cloning an item clones gear (shulker precedent, accepted); ammo is consumed from the live ledger stack, so commands can't duplicate anything.
- **Leash vs. pullback**: goals enforce the 10-block owner leash (§5), strictly inside the 48-block ledger pullback — the yank only ever fires on knockback spikes or bugs mid-action. Stray dolls (detached entries) never see the ledger at all; park/resummon are otherwise unchanged.
- **Stray ronin**: a dive aborted before detonation (or a stray hurt by anything else) keeps following its owner on stray-routed combat, shield still works, but no ledger path can reach it — no commands, no scheduling, no itemize. Only death ends it, via the death drop, persisting in chunks until then. There is no rejoin path by design.
- **Future work (out of scope)**: continuous attacks with automatic targeting, mechanical `core` items, attack-tracking lasers, `ITERATIVE` for other types, render held items (loadout.md §5), a glove-inventory screen, block-hosted doll commands, modded-shield matching, heal-mark pruning.
