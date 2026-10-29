# Ghost power and Yuyuko's fan

Ghosts carry a power pool that fades while they serve. Yuyuko's fan stores that power in one
universal spiritual pool, so a player can carry ghosts instead of keeping them summoned.

## 1. Scale

| value | meaning |
|---|---|
| `GhostPower.MAX = 1000` | full power of one ghost; the fan refills to this |
| `GhostPower.WILD_MIN/MAX = 500/1000` | starting power of a wild ghost |
| `GhostPower.SUMMON_COST = 1000` | spiritual power the fan spends on one summon |
| `GhostPower.LEAVE_THRESHOLD = 200` | below this an owned ghost leaves |
| `SpiritualPower` | universal item pool, stored as the `spiritual_power` component |
| `spiritual_power_max` | per-stack capacity; the fan's intrinsic value is 10000 |

The ghost's power is synced entity data on every ghost type and persisted in `DollData.power`.
Because the ledger entry is server-only (`toClient = false`), the entity's synced accessor is what
the renderer reads; `GhostPowerModule` is the bridge across park/resummon, like
`GhostVariantModule` for the variant.

Wild power is rolled in `finalizeSpawn` only for `SPAWN_EGG`, `COMMAND` and `NATURAL`. A released
ghost keeps the power it had, because `DollAttachment.release` copies it onto the stray.

## 2. Fading

`GhostPowerHolder.powerAlpha` maps power to opacity: 1 at full, `MIN_ALPHA = 0.05` at zero, linear in
between. Both ghost renderers override `getRenderColor` to scale the vertex alpha and
`getRenderType` to return `RenderType.entityTranslucent`. The default `entityCutoutNoCull` would
discard alpha instead of blending it, so the render-type override is what makes fading visible at
all. Invisible and glowing entities keep the vanilla renderer paths; the haunting ghost is the
exception, returning no render type while it hides for a volley.

## 3. Drain while following

The drain ticks only while the owner is alive, in the same level and within the aura range (16
blocks), which is the same condition the aura itself uses. A ghost far away or parked costs nothing.

| variant | drain | skill |
|---|---|---|
| curios | 1 power / 40 ticks | cleanse costs 50, only when power is at least 50 |
| foodie | 1 power / 20 ticks | — |
| lively | 1 power / 20 ticks | — |
| dual | — | death ward, no aura |

At zero power the aura stops (existing effects run out their 60-tick tail) and the curios skill is
refused, though the leave threshold below normally ends the binding first. Dual has no drain and no
aura, so its power only matters when it is absorbed or spent as a ward.

Below `GhostPower.LEAVE_THRESHOLD = 200` the binding breaks instead. `GhostEntity.tick` calls
`DollAttachment.release`, which spawns the wild counterpart at the ghost's current spot with its
remaining power, so the ghost leaves as a wild ghost rather than vanishing. The bone flute checks
the same threshold before `adopt`: a stray below 200 power refuses the flute, so a ghost that left
cannot be re-bound until its power is restored (the fan still absorbs it). The dual handler opts out
of the leave rule because it has no upkeep.

### 3.1 Variant handlers

`GhostVariant` is a handler enum, like `HexBrew`: each constant owns a `GhostVariantHandler`
(`CuriosGhostHandler`, `FoodieGhostHandler`, `LivelyGhostHandler`, `DualGhostHandler`).
`GhostEntity.tick` never switches on the variant; it asks the handler for `tick`, `aura`,
`leavesWhenWeak` and `wardDeath`. `wild()` keeps dual out of the wild roll and out of the character
ghost host: `GhostVariant.wildValues()` is the three support variants, while
`GhostVariant.values()` includes dual for the fan's summon loop.

## 4. Yuyuko's fan

`YuyukoFanItem` holds one spiritual pool, stored as `spiritual_power` on the stack, capped by the
`spiritual_power_max` component. The fan's capacity is intrinsic to the item: registration patches
its default components to 10000 through `ModifyDefaultComponentsEvent`. The `SpiritualPower` helper
reads both components, clamps writes and renders the bar, so future spiritual items only need their
own intrinsic max.

The fan has four modes, stored in the `yuyuko_fan_mode` component and selected with the shared item
wheel/list (`YuyukoFanSelectionListener`). Each mode is a `YuyukoFanModeHandler`, mirroring the
ghost handlers, so `YuyukoFanItem.use` only delegates.

- **Right-click a ghost** absorbs `min(power, room)` into the pool. This works in every mode. A wild
  ghost is discarded; an owned ghost is removed from the ledger through `DollAttachment.absorb`,
  which does not spawn a stray. The write targets the live held stack because vanilla hands creative
  players a copy of the stack to `interactLivingEntity`; writing the argument would spend the ghost
  without storing its power.
- **Full pool** on an owned ghost: `DollAttachment.release` runs instead, so the ghost is not
  destroyed with power still in it. A wild ghost at a full pool is simply refused. The haunting
  ghost is the exception: release refuses it, so it stays in the ledger following its owner.
- **Summon mode, right-click air** refills every summoned ghost, then summons one of each missing
  summonable kind from `GhostVariant.values()` (including dual, excluding the haunting ghost) while
  the pool has at least `SUMMON_COST` and the ledger cap (`MAX_SUMMONED`) allows it. A full ghost is
  skipped; if nothing was refilled or summoned, the fan reports the cap or that everything is
  already full.
- **Absorb mode, right-click air** collects every loaded summoned ghost, including the haunting
  ghost. At a full pool a ghost is released instead, the same escape hatch the single-target path
  uses; release refuses the haunting ghost, so it is left following rather than destroyed with power
  still in it. Absorbing fewer ghosts than expected is not an error.
- **Scatter mode, right-click air** spends 10 spiritual power on a forward fan of butterfly danmaku:
  21 rays three degrees apart, five danmaku per ray. Odd (1-based) rays use the pink item at
  0.6–1.0 distance, even rays the blue item at 0.55–0.95. Within a ray the five are spaced evenly
  across that range, and each moves at the distance it spawned at, so the outermost is the fastest.
  The mode is refused, with a message, when the pool holds less than 10.
- **Haunt mode, right-click air** spends 1000 spiritual power to send a haunting ghost at the entity
  under the crosshair. Targeting reuses the shared glove trace (`YuyukoFanItem` implements
  `GloveTargeting`): 64 blocks, no block veto, the same accepted targets as the dagger glove's
  homing mode, and a violet outline. When a haunting ghost is already in the ledger, no second one
  is summoned: the power refills it (only the power actually added is spent) and it is sent at the
  new target.

The selector icons are the stray-ghost spawn egg for summoning, the fan itself for absorbing, the
pink butterfly for scattering and the bone flute for haunting, all named after the mode.

### 4.1 Haunting ghost

`GhostVariant.HAUNTING` is an ordinary ledger ghost, not a separate entity: it uses the same
`GhostEntity`, `DollAttachment` entry, park/resummon path and summon cap as every other variant. It
has no upkeep and never leaves on its own. With no target it follows its owner in the normal
formation and behaves like any other ghost. `HauntingGhostHandler.hunt` gives it a target; the
handler then suppresses the follow goals, chases the target, and attacks. When the target dies or
leaves the level, it clears the target and resumes following.

Each volley spends 10 power; at zero it vanishes, removing its ledger entry, so one full summon is
worth 100 volleys. `SummonFanHandler` never summons or refills it, `AbsorbFanHandler` recalls it
like any other ghost, and the bone flute cannot release it (`releasable` is false), so the fan stays
the only exit. A full fan leaves it following rather than releasing it.

A volley is 72 butterflies: each of 36 centres gets one clockwise and one counterclockwise one. The
n-th centre sits four blocks from the ghost at `10n` degrees, and the butterfly itself starts on the
ghost, so the 36 centres form a ring around it while every orbit passes through its body. The ring
lies in a plane tilted through the target (`DanmakuHelper.getOrientation` on the ghost-to-target
direction gives the plane, and `rotateDegrees` places the centres), so the flower spreads around the
target instead of staying horizontal. Each butterfly orbits its own centre at the same four-block
radius for 40 ticks, which is exactly one full revolution (`PolarMover` at `2π/40`); the two
directions per centre take opposite colours, alternating pink and blue. The ghost holds its position
and stays invisible for all 40 ticks; `HauntingGhostModule` keeps the lock and cooldown as transient
combat state, so a ghost parked mid-volley comes back following. When the volley ends it has a
further 20 ticks of cooldown, which it spends tracing the target; if the target is still in range it
volleys again.

The variant wears `textures/geo/ghost/dual_ghost.png`; `GhostVariant.texture` returns that stem for
`HAUNTING` instead of the usual `<variant>_ghost` derivation.

## 5. Dual ghost and the death ward

Dual is the other fan-only variant. `GhostVariant.random` cannot roll it and the character ghost host
conjures only `wildValues()`, so it never appears without a player summoning one. It has no upkeep,
no aura, and does not leave at low power.

When its owner would die, `GhostEventHandlers.onPlayerDeath` runs at `EventPriority.HIGH`. It walks
the summoned ghosts and calls `handler.wardDeath`; `DualGhostHandler` absorbs itself and returns
true, so the event is canceled and the owner is set to 1 health. Running before
`DollEventHandlers.onPlayerDeath` (NORMAL) keeps the remaining ghosts summoned: a saved death does
not trigger the usual park-all pass.

## 6. Jizo offering

Right-clicking a Jizo with an item that has a `spiritual_power_max` consumes one experience level
and the current progress, then adds `consumed_points * GLModConfig.SERVER.jizoXpRatio` (default
0.1) spiritual power to the stack. `ExperienceHelper` owns the level arithmetic: a level is worth
the points needed to leave it plus the progress into it. The offer is refused when the item is
already full or the player has no level to give, and the deduction runs through
`giveExperiencePoints` so the XP change event still sees it. When the stack fills before the whole
offer is stored, the XP for the overflow is given back, so only the stored power is paid for.

## 7. Debug readout

The debug wand's ghost click and the debug glasses' look-at overlay share `GhostDebugInfo`, which
prints `Wild/Tamed <variant>: <power> / 1000 power`. Both read the synced accessors directly, so the
readout works for wild and owned ghosts without a server round trip.
