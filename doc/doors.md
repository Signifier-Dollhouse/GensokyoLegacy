# Doors: SmartYoukaiEntity opens + closes doors

Status: **PLANNING** (living document, updated as the design conversation proceeds). 4.1 (SlidingDoor public API),
4.2 (pathfinding framework), 4.3 (task), 4.4 (registration), 4.5 (datagen) done; 4.6 (manual) pending.
In-game bugs found and fixed (2026-08):
1. sliding doors never closed after the youkai left — close loop now keeps open panels + never closes into an entity (D7a/D9);
2. youkai pathed through and stuck at an unopenable sliding door — path handler now `BLOCKED`s unopenable/parked panels (D2);
3. **regression from the 2026-08 fixes** — neither regular doors nor sliding doors closed, and opening was delayed
   — the task's `PATH` entry condition made it stop when the path ended (close loop never ran again), the node
   cooldown was inverted (new doors were skipped while the same door re-fired every cycle), and the D2a `rePath`
   throttled reopening. Fixed by dropping `PATH` from the entry map, flipping + shortening the cooldown (20→5),
   removing the distance-abandon rule, and removing `rePath` (D2a) (see D2a/D7/D7a/D9, §4.3).
Scope: `SmartYoukaiEntity` (covers GeneralYoukai: fairy/merchant/boss + Rumia) should
open vanilla doors **and** the mod's `SlidingDoor`, then remember to close them after passing through.

---

## 1. Current state (research findings)

### Vanilla doors already work today
- `SmartYoukaiEntity.constructTaskBoard` registers `InteractWithDoor.create()` as a CORE always-behavior
  (`SmartYoukaiEntity.java:76`), and the walk nav has `canPassDoors(true)` + `canOpenDoors(true)`
  (`YoukaiNavigationControl.markHuman`, lines 93-99; `Ground.createPathFinder` lines 161-166).
- Vanilla flow: `WalkNodeEvaluator` maps a closed wooden `DoorBlock` cell to `DOOR_WOOD_CLOSED`; the
  inherited `getPathTypeWithinMobBB` converts it to `WALKABLE_DOOR` when `canOpenDoors() && canPassDoors()`
  (walk nav only). `InteractWithDoor` then opens the door when the door cell is the path's prev/next node,
  records `GlobalPos` in `MemoryModuleType.DOORS_TO_CLOSE`, and closes it once the mob is ~3 blocks past
  (skipping if same-type mobs are coming through, within 2 blocks).
- Memory wiring already present: `PATH` is populated by `YoukaiMoveTask`; `NEAREST_LIVING_ENTITIES` by
  `NearbyLivingEntitySensor`; `DOORS_TO_CLOSE` is a vanilla memory (registered by default).

### SlidingDoor is NOT a DoorBlock — two gaps
`SlidingDoor` (`content/block/door/SlidingDoor.java`) is a l2modularblock `DelegateBlock` (double block,
`HALF`/`HINGE`/`STACK` 1-4). It has no `OPEN` property; the "open" state is **positional**:
- closed  = panel block present at the 2x1 doorway position (seat),
- open    = panel absent from the seat, parked in the pocket cell one block to the `hingeDir` side
  (STACK merging when shared).

Gaps:
1. **Pathfinding**: `WalkNodeEvaluator.getPathTypeFromState` returns `BLOCKED` for the solid closed panel,
   so walk paths never route through a closed sliding door.
2. **Opening**: vanilla `InteractWithDoor` requires `instanceof DoorBlock` (plus the
   `MOB_INTERACTABLE_DOORS` tag), so it ignores `SlidingDoor`.

### Historical reference
`content/entity/behavior/task/home/YoukaiSmartDoorTask.java` is fully commented out — an old
SmartBrainLib-based subclass of `InteractWithDoor` (live at commit `804da11`). It:
skipped opening while `navCtrl.isFlying()`, opened `DoorBlock`s, recorded into `DOORS_TO_CLOSE`,
and closed passed doors within 3 blocks (holding for same-type mobs). No sliding-door support, and SBL
is removed (`ff3b0fd` "remove sbl") — must be rewritten against vanilla + l2serial APIs.

### Path type conversion chain (1.21.1, verified from decompiled sources)
- `WalkNodeEvaluator.getPathType(context,x,y,z)` → `getPathTypeStatic` (uses server `PathTypeCache` per pos).
- `getPathTypeWithinMobBB` converts `DOOR_WOOD_CLOSED`→`WALKABLE_DOOR` iff `canOpenDoors() && canPassDoors()`,
  and `DOOR_OPEN`→`BLOCKED` iff `!canPassDoors()`.
- The mod's `YoukaiWalkNodeEvaluator`/`YoukaiFlyNodeEvaluator` both override `getPathType` and delegate to
  `YoukaiNodeEvaluatorUtils.getPathType(ans, context, x, y, z)`, which routes through the
  `YoukaiNodeEvaluatorRegistry` of per-block/per-tag `YoukaiPathTypeHandler`s (no hardcoded block logic).
- `FlyNodeEvaluator extends WalkNodeEvaluator` → same conversion path; fly nav has `canPassDoors(false)`,
  so door-like cells stay `DOOR_WOOD_CLOSED` (malus -1) → unpathable. Good: flying youkai never route through doors.
- `PathTypeCache` is invalidated in `ServerLevel.sendBlockUpdated` on block change → opening/closing a door
  auto-invalidates cached path types; no manual invalidation needed.
- `Behavior` with (min,max) duration (0,0) behaves like vanilla `OneShot` (start+tick each brain tick).

### Block identity
The registered block is `DelegateBlockImpl` (a `DelegateBlock`); `SlidingDoor` is one of its impl components.
`instanceof SlidingDoor` on the block **fails**. Detection options: block tag (recommended), or reaching into
`DelegateBlockImpl.impl` (package-private, needs AT).

---

## 2. Goals

1. Walk pathfinder routes through **closed** sliding doors (as it already does for vanilla doors).
2. A door behavior opens vanilla `DoorBlock`s and `SlidingDoor`s as the youkai approaches.
3. Opened doors are recorded and **closed after the youkai passes** (holding open for other youkai).
4. No regression to existing vanilla-door behavior; works for all `SmartYoukaiEntity` subclasses.

---

## 3. Design decisions — status

### D1. One custom task replaces vanilla `InteractWithDoor` — **DECIDED**
Write `YoukaiSmartDoorTask extends Behavior<SmartYoukaiEntity>` (plain class, codebase style) handling both
vanilla doors and sliding doors, registered as CORE always in `constructTaskBoard` replacing line 76.
Replicates vanilla open/track/close semantics + adds sliding doors + flying guard. Package: `task/core/`
(matches CORE movement tasks) — old commented file lives in `task/home/`, will be replaced.

### D2. Pathfinder: reuse `DOOR_WOOD_CLOSED` for closed sliding doors — **DECIDED** — **DONE**
Sliding doors register a `YoukaiPathTypeHandler` for the `GLTagGen.SLIDING_DOOR` tag: if the cell holds a
SlidingDoor panel that is **openable/seated** (`SlidingDoorUtils.isSeatedAndOpenable`, the `canOpen` equivalent),
return `PathType.DOOR_WOOD_CLOSED`. The inherited `getPathTypeWithinMobBB` then yields `WALKABLE_DOOR` for walk nav
and `DOOR_WOOD_CLOSED` (unpathable) for fly nav — free fly-vs-walk discrimination, no malus changes. Parked panels
(open, sitting in a pocket) classify as `BLOCKED` (can't walk through the panel).
- No hardcoded block checks: `YoukaiNodeEvaluatorRegistry` dispatches by `Block` or `TagKey<Block>`. Handlers are
  registered in `YoukaiNodeEvaluatorRegistry.init()` for `BlockTags.MOB_INTERACTABLE_DOORS` (doors),
  `BlockTags.TRAPDOORS` (open-trapdoor → `BLOCKED` fix), and `GLTagGen.SLIDING_DOOR`.
- Classifies a cell by normalizing the panel to its bottom half; both halves of a seated door are door-like
  (mirrors vanilla DoorBlock, where both halves map to `DOOR_WOOD_CLOSED`).
- A closed-but-unopenable door (blocked pocket) is `BLOCKED` — correct: the player can't open it either, and a
  path should not commit to it. **Bug-fix note (2026-08):** the handler *used to* `return ans` here, and because a
  sliding panel's collision shape is thin (depth `stack+1` px), vanilla `BlockBehaviour.isPathfindable(LAND)`
  returns `true` → `WalkNodeEvaluator` classified a closed panel as `PathType.OPEN`, so the youkai pathed straight
  through a closed unopenable door and stood stuck against the panel. Now the handler explicitly returns
  `PathType.BLOCKED` for any sliding panel that isn't seated-and-openable (closed unopenable doors AND parked
  panels with no free space beyond).

### D2a. Unopenable-door recovery: ~~re-path~~ → retry (bug-fix, 2026-08)
Bug report: "youkai paths through and then gets stuck at an unopenable sliding door." With D2 fixed, an
*unopenable* door is never committed to at path time. A residual race remains when a door is **openable at path
time but unopenable at arrival** (pocket got occupied meanwhile, e.g. a shared-pocket double door where one panel
blocks the other's pocket). The task *used to* detect this via `rePath` (`navigation.moveTo(walkTarget)`), but
**rePath is REMOVED (regression fix below)**: it overwrote the navigation's path and could trigger the fly-nav
fallback (`SwapControl`), making the youkai fly off and abandon open doors, and it re-armed the node cooldown,
reintroducing the opening delay. Instead the task just retries the door on its next 5-tick window; since an
unopenable panel is now classified `BLOCKED`, the move task re-paths around it on its own once the door stops
matching the path.

### D3. Track doors-to-close by **seat** position in vanilla `DOORS_TO_CLOSE` — **DECIDED**
Store the seat (doorway) `BlockPos` where the closed door lives. Open = `doOpen` at seat (panel moves to pocket).
Close = find the panel (scan seat + 4 horizontal neighbors for a panel whose own seat == recorded seat), then
`doClose`. Skip-while-passing + "hold for others" checks compare against the seat (mirrors vanilla node logic).
Entries dropped when >3 blocks away / door already closed / not a panel.
Store the seat (doorway) `BlockPos` where the closed door lives. Open = `doOpen` at seat (panel moves to pocket).
Close = find the panel (scan seat + 4 horizontal neighbors for a panel whose own seat == recorded seat), then
`doClose`. Skip-while-passing + "hold for others" checks compare against the seat (mirrors vanilla node logic).
Entries dropped when >3 blocks away / door already closed / not a panel.

### D4. "Hold open for others" scope — **DECIDED: any `SmartYoukaiEntity`**
Hold the door open (delay closing) when another smart youkai within 2 blocks has a `PATH` whose prev/next node is
the door seat (mirrors vanilla's per-mob check, broadened from same-type to any `SmartYoukaiEntity`).

### D5. Flying — **DECIDED**
Skip opening while `navCtrl.isFlying()` (fly nav can't path through doors anyway; guard is cheap insurance
against stale walk paths). Closing is NOT guarded by flying (a youkai that opened a door on foot then took off
should still close it).

### D6. Sliding door detection — **DECIDED: block tag**
New block tag `gensokyolegacy:sliding_doors` (`GLTagGen`), applied in `GLDecoBlocks` sliding-door registration;
detect via `state.is(tag)`. Keeps detection data-driven and avoids an AT into l2modularblock internals.

### D7. Close even after the path ends — **DECIDED** — regression fix applied
Entry conditions are `REGISTERED` (not `VALUE_PRESENT`) for `DOORS_TO_CLOSE`, `NEAREST_LIVING_ENTITIES` (**and,
until the regression fix below, `PATH`**), so the task ticks every tick (`canStillUse` = true, so once started it
never stops). Opening logic runs only while a live path exists; the close loop runs whenever `DOORS_TO_CLOSE` is
non-empty, so a youkai that stops mid-route still closes doors within the 3-block range.
**Regression fix (2026-08):** the `PATH` entry was removed from the entry map. Because `YoukaiMoveTask` erases
`PATH` when the walk target is reached, a `PATH: REGISTERED` entry made `tryStart` fail exactly when the youkai
stopped right past a door → the task stopped → `closeDoors()` never ran again → doors were left open forever.
The close loop does not need `PATH` at all (it only *skips* entries equal to the live prev/next node, which is an
optional refinement); with the entry dropped, the close loop survives path end and doors always close.

### D7a. Close robustness (bug-fix, 2026-08)
Bug report: "sliding doors never close after the youkai leaves." Root cause candidates eliminated by
walkthrough + the fix: the task already ran every tick, so the failure was the close loop's *abandon* policy.
Two fixes:
- **Sliding entries are not dropped by the 3-block distance rule while still open.** The seat of an open sliding
  door is air; the far-away branch now removes the entry only when the seat is *not* air (door closed/vanished).
  The panel closes on the next tick the youkai is back within range instead of being forgotten forever.
- **Never close a sliding door into an entity.** Closing materializes a solid panel at the seat; `blockedByEntity`
  holds the door open while this youkai or any nearby living entity's bounding box overlaps the 1×2×1 doorway
  column, so a youkai that stops at the door isn't embedded, and the door closes the tick it steps clear.
  `holdingForOthers` (other smart youkai pathing through) still applies on top of that.

### D8. Vanilla door parity — **DECIDED: wooden only**
Only `DoorBlock`s in `BlockTags.MOB_INTERACTABLE_DOORS` are opened/closed (exactly vanilla's restriction).
Iron doors remain unpathable/unopenable by mobs.

### D9. Close-retry vs vanilla-abandon — **DECIDED: keep entry on hold/failed close**
Vanilla *removes* the door from tracking when it holds it open for followers or (implicitly) after one close
attempt, which effectively abandons it. We instead keep the entry when (a) holding for another youkai,
(b) `SlidingDoor.tryClose` fails (seat occupied/blocked), or (c) an entity is standing in the doorway; and retry
next tick. Sliding entries are also kept beyond the 3-block range while still open (see D7a), so an open panel is
never forgotten; the entry expires only when the door is actually closed or the panel is gone (player interference).
**Regression fix (2026-08):** the separate far-away rule was **removed entirely** — a door entry is now only ever
expired by (dimension mismatch), (door already closed / panel gone), or (close succeeded). Distance no longer
drops entries at all (vanilla doors close from any distance the tick after the youkai stops), which guarantees an
open door is always closed even if the youkai leaves the chunk before closing.

---

## 4. Implementation plan (draft — will finalize after discussion)

### 4.1 `SlidingDoor` — expose a small public API (no behavior change) — **DONE**
Refactor `canOpen`/`canClose`/`isConnected`/`isAir`/`bottom` to take `BlockGetter` (they only read blocks).
Keep `doOpen`/`doClose` on `Level`. New public statics:
- `SlidingDoorUtils.isSeatedAndOpenable(BlockGetter, BlockPos)` — normalize to bottom half (guard: bottom must
  also be a sliding door, else false) and run the `canOpen` logic. This is the pathfinder classifier.
- `@Nullable BlockPos SlidingDoorUtils.tryOpen(Level, BlockPos)` — normalize to bottom, `canOpen` → `doOpen`;
  returns the bottom seat pos if opened, else null.
- `boolean SlidingDoorUtils.tryClose(Level, BlockPos)` — normalize to bottom, `canClose` → `doClose`.
- `BlockPos SlidingDoorUtils.seatOf(BlockState panelState, BlockPos panelPos)` —
  `panelPos.relative(hingeDir(state).getOpposite())`, used by the close-loop panel scan.
- `SlidingDoor.bottom(BlockGetter, BlockPos)` — made existing private helper public.

Implemented. Datagen methods (`buildBlockState`/`genItemModel`/`genLoot` + model helpers and `BASE`/`ITEM_BASE`
caches) were split out of `SlidingDoor` into `content/block/door/SlidingDoorJsons.java`; the 4.1 API statics went
into `content/block/door/SlidingDoorUtils.java` (needs `canOpen`/`canClose`/`doOpen`/`doClose`/`hingeDir` at
package-private visibility). `GLDecoBlocks` registration now calls `SlidingDoorJsons`. Verified with
`./gradlew compileJava`.

### 4.2 Pathfinding — framework + sliding door registration — **DONE**
No hardcoded block logic in the evaluators. New files in `content/entity/behavior/move/`:
- `YoukaiPathTypeHandler` — `PathType getPathType(PathType ans, PathfindingContext context, BlockPos pos, BlockState state)`.
- `YoukaiNodeEvaluatorRegistry` — `register(Block, handler)` / `register(TagKey<Block>, handler)`, applied in
  `apply(...)` after vanilla's classification. `init()` (called from the mod constructor) registers:
  - `BlockTags.MOB_INTERACTABLE_DOORS` → explicit `DOOR_OPEN`/`DOOR_WOOD_CLOSED` for any state with an `OPEN`
    property (covers vanilla wooden doors and modded openable doors carrying the tag).
  - `BlockTags.TRAPDOORS` → open trapdoor cell (`TRAPDOOR`/`DANGER_TRAPDOOR`) → `BLOCKED` (kept existing fix).
  - `GLTagGen.SLIDING_DOOR` → `SlidingDoorUtils.isSeatedAndOpenable(...)` → `DOOR_WOOD_CLOSED`.
- `YoukaiNodeEvaluatorUtils.getPathType` now just delegates to `YoukaiNodeEvaluatorRegistry.apply`.
Walk nav converts to `WALKABLE_DOOR` (canOpen+canPass); fly nav stays `DOOR_WOOD_CLOSED` (unpathable). No malus
changes; both `YoukaiWalkNodeEvaluator` and `YoukaiFlyNodeEvaluator` already delegate here. The
`gensokyolegacy:sliding_door` block tag was generated (all 11 wood variants) via runData.

### 4.3 New `task/core/YoukaiSmartDoorTask<E extends SmartYoukaiEntity>` — **DONE** (regression fixes 2026-08)
`extends Behavior<E>`, entry conditions `{DOORS_TO_CLOSE: REGISTERED, NEAREST_LIVING_ENTITIES: REGISTERED}`,
duration `(0,0)` (OneShot-like: start+tick every brain tick; instance fields persist, matching vanilla's
cooldown state). Registers `DOORS_TO_CLOSE`/`NEAREST_LIVING_ENTITIES` into the brain via `TaskBoard`. **`PATH`
was deliberately dropped from the entry conditions** so the task keeps running after the path is erased (see D7).

tick() = tryOpenDoors() + closeDoors().

**tryOpenDoors** (adapted from vanilla `InteractWithDoor`):
- no-op unless a live `PATH` exists (`!notStarted && !isDone`) and `!navCtrl.isFlying()`.
- node cooldown: unchanged `getNextNode()` → reset to 5; changed node → open immediately. **Regression fix: the
  cooldown used to be 20 and its polarity was inverted** (same node reset to 20 every cycle → doors re-fired
  constantly, while a changed node sat in the cooldown and was skipped for ~1s) — that was the "significant delay
  in opening". Now a new door opens the instant the youkai's path reaches it, and a door that fails to open is
  retried at most once per 5 brain cycles (10 ticks).
- for `getPreviousNode()` and `getNextNode()` cells:
  - `DoorBlock` in `MOB_INTERACTABLE_DOORS`, closed → `setOpen(entity, level, state, pos, true)`; record node pos.
  - sliding door (tag) → `SlidingDoorUtils.tryOpen`; on success record the returned **seat** pos.

**closeDoors** (adapted from vanilla `closeDoorsThatIHaveOpenedOrPassedThrough`):
- iterate `DOORS_TO_CLOSE`; drop only if wrong dimension.
- skip (keep entry) if recorded pos equals the path's prev or next node (still at the doorway).
- per entry, by block at recorded pos:
  - `DoorBlock` (open) → close via `setOpen(false)` unless holding for others; remove.
  - sliding door panel present → already closed (someone closed it) → remove.
  - seat empty → scan seat + 4 horizontal neighbors for a panel with `seatOf(panel) == recorded pos`; if none,
    remove (panel removed); else if holding for others → keep; else if an entity overlaps the 1×2×1 doorway
    column → keep (D7a); else `tryClose` (keep entry if it fails, retry).
- **holding for others**: any `SmartYoukaiEntity` (≠ self) within 2 blocks whose own live `PATH` has the seat
  as prev/next node.
- **Regression fix: no distance rule.** The old "drop if >3 blocks away" branch is gone; entries close from any
  distance once the youkai stops, and sliding entries are never abandoned while open.

### 4.4 Registration — **DONE**
- ~~`GLTagGen`: `public static final TagKey<Block> SLIDING_DOOR = block("sliding_door");`~~ **DONE**.
- ~~`GLDecoBlocks`: `.tag(GLTagGen.SLIDING_DOOR)` on the sliding-door block~~ **DONE** (tag JSON generated).
- ~~`SmartYoukaiEntity.constructTaskBoard` line 76: replace `InteractWithDoor.create()` with `new YoukaiSmartDoorTask<>()`~~ **DONE**.
- ~~Delete commented `task/home/YoukaiSmartDoorTask.java`~~ **DONE** (file removed).

### 4.5 Datagen & build — **DONE**
- ~~`./gradlew runData` → commits new `data/gensokyolegacy/tags/block/sliding_door.json` (+ any lang drift)~~ **DONE**
  (runData ran clean, 0 files written — tag JSON already committed from 4.2).
- `./gradlew build` to compile (passes after 4.3/4.4).

### 4.6 Manual verification (in-game)
- Single, double (connected), and STACK>1 sliding doors: youkai opens, passes, closes (sound plays, panel returns).
- Vanilla wooden door still opens/closes; iron door untouched.
- Pathing debug renderer (`NavigationDebugger`) shows paths routing through closed doors.
- Flying youkai ignores doors; a youkai that opens then takes off still closes it.
- Two youkai through one door: door held open while both pass.
- Player interference: player opens/closes/moves a panel while a youkai is mid-pass — no crashes, entry dropped.

### 4.7 Bug-fix verification (2026-08)
Regressions to re-check after D2/D7a and the regression fix:
- **Unopenable door**: door placed flush against a wall (no pocket) → youkai no longer paths through it; it routes
  around or gives up instead of standing stuck against the panel. Open failure is retried, not re-pathed.
- **Shared-pocket double door**: youkai path crossing a door whose pocket gets filled by a sibling panel → open
  fails, retried next window; the move task re-paths around the now-blocked door on its own.
- **Close after stop**: youkai walks through a door and stops right past it → door still closes (PATH cleared;
  task keeps running since `PATH` is no longer an entry condition; close loop processes the entry with null
  prev/next).
- **Standing in doorway**: youkai/player standing in the doorway → door stays open (no panel spawned into the
  entity), closes the tick the doorway clears.
- **Far-away retry**: if a close attempt is missed while leaving, the panel closes whenever the youkai's next
  brain tick reaches the entry — no distance rule, so nothing is ever abandoned (was "within 3 blocks").
- **Opening delay (regression)**: approaching either a vanilla door or a sliding door opens it immediately when
  the youkai's path first reaches the door cell (previously delayed ~1s by the inverted cooldown).
- **Both door types close (regression)**: after passing, both regular and sliding doors close once the youkai
  stops/passes (previously never closed because the task died with the path).

---

## 5. Edge cases / risks (notes for testing)

- **Seated vs parked ambiguity**: classifier uses `canOpen`; a parked panel whose hinge side has free space beyond
  (exposed double-panel layouts) still classifies as `DOOR_WOOD_CLOSED`. In the common 1-deep recess install the
  parked panel is `BLOCKED`. Documented; low risk in normal wall installs.
- **Merged/STACK>1 panels**: opening merges into pocket (sum STACK); closing moves sum-1 back to seat, leaves 1
  unit in pocket (block's own close semantics). Two entries may reference one shared panel — verify double doors.
- **Connected doors** (`isConnected`): two doors opening into a shared recess; verify open/close both halves.
- **Entity standing in doorway when closing**: `blockedByEntity` (D7a) holds the door open while any living entity's
  BB overlaps the 1×2×1 doorway column, so a panel never materializes inside an entity. `holdingForOthers` still
  keeps it open for another smart youkai that is pathing through.
- **Open door pocket cell vs path**: after opening, pocket becomes solid → `sendBlockUpdated` triggers path
  recompute for mobs whose path used that cell; re-path avoids it (block now BLOCKED).
- **No `PATH` entry condition**: the close loop intentionally runs with no live path (see D7). It reads `PATH`
  only to skip the door the youkai is currently at; with `PATH` null the skip is a no-op and closing proceeds.
- **Node cooldown is per-task-singleton**: `node`/`cooldown` live on the shared task instance, so the open-throttle
  carries across brain restarts. With (0,0) duration the task re-runs every 2 ticks; a 5-tick cooldown therefore
  allows a fresh open attempt every 10 game ticks.
- **Player/mob interference**: only doors this youkai opened are tracked (vanilla semantics). If a player moved
  the panel, close step no-ops and drops the entry.
