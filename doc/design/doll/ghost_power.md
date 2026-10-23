# Ghost power and Yuyuko's fan

Ghosts carry a power pool that fades while they serve, and Yuyuko's fan stores that power by type
so a player can carry ghosts instead of keeping them summoned.

## 1. Scale

| value | meaning |
|---|---|
| `GhostPower.MAX = 1000` | full power; the fan also refills to this |
| `GhostPower.WILD_MIN/MAX = 500/1000` | starting power of a wild ghost |
| `GhostPower.SUMMON_COST = 1000` | power the fan spends on one summon |
| `GhostPower.FAN_CAPACITY = 10000` | storage per ghost type, ten full ghosts |

Power is synced entity data on both ghost types and persisted in `DollData.power`. Because the
ledger entry is server-only (`toClient = false`), the entity's synced accessor is what the renderer
reads; `GhostPowerModule` is the bridge across park/resummon, like `GhostVariantModule` for the
variant.

Wild power is rolled in `finalizeSpawn` only for `SPAWN_EGG`, `COMMAND` and `NATURAL`. A released
ghost keeps the power it had, because `DollAttachment.release` copies it onto the stray.

## 2. Fading

`GhostPowerHolder.powerAlpha` maps power to opacity: 1 at full, `MIN_ALPHA = 0.05` at zero, linear in
between. Both ghost renderers override `getRenderColor` to scale the vertex alpha and
`getRenderType` to return `RenderType.entityTranslucent`. The default `entityCutoutNoCull` would
discard alpha instead of blending it, so the render-type override is what makes fading visible at
all. Invisible and glowing entities keep the vanilla renderer paths.

## 3. Drain while following

The drain ticks only while the owner is alive, in the same level and within the aura range (16
blocks), which is the same condition the aura itself uses. A ghost far away or parked costs nothing.

| variant | drain | skill |
|---|---|---|
| curios | 1 power / 40 ticks | cleanse costs 50, only when power is at least 50 |
| foodie | 1 power / 20 ticks | — |
| lively | 1 power / 20 ticks | — |

At zero power the aura stops (existing effects run out their 60-tick tail) and the curios skill is
refused, though the leave threshold below normally ends the binding first.

Below `GhostPower.LEAVE_THRESHOLD = 200` the binding breaks instead. `GhostEntity.tick` calls
`DollAttachment.release`, which spawns the wild counterpart at the ghost's current spot with its
remaining power, so the ghost leaves as a wild ghost rather than vanishing. The bone flute checks
the same threshold before `adopt`: a stray below 200 power refuses the flute, so a ghost that left
cannot be re-bound until its power is restored (the fan still absorbs it).

## 4. Yuyuko's fan

`YuyukoFanItem` stores three independent pools as data components (`yuyuko_fan_curios`, `_foodie`,
`_lively`), plus the selected type (`yuyuko_fan_type`). There is no per-ghost data in the fan, exactly
as the brief asks: absorbing a ghost converts it to power of its type.

- **Right-click a ghost** absorbs `min(power, room)`. A wild ghost is discarded; an owned ghost is
  removed from the ledger through `DollAttachment.absorb`, which does not spawn a stray.
- **Fan full for that type** on an owned ghost: `DollAttachment.release` runs instead, so the ghost
  is not destroyed with power still in it. A wild ghost at a full fan is simply refused.
- **Right-click air** first looks for a summoned ghost of the selected type, full or not, and tops
  it up with as much stored power as the fan has. The fan never summons a second ghost of a type
  that is already following: a full one refuses the use. Only when no matching ghost is summoned,
  and at least `SUMMON_COST` is stored, does it summon a new one at full power through
  `DollAttachment.summonGhost`. The ledger cap (`MAX_SUMMONED`) still applies.
- **Type selection** is the shared item-selector wheel/list, registered as
  `YuyukoFanSelectionListener`; the fan carries `L2ISTagGen.SELECTABLE`.

The selector icons are stray-ghost spawn eggs renamed per type, because all three types share one
fan model and the egg is the only ghost-shaped item already in the mod.
