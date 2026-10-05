# Candlestick — Lit/Unlit Design

> **Status:** implemented (`CandlestickBlock`, `GLFurniture.CANDLESTICK`)
> **Block id:** `gensokyolegacy:candlestick` (`GLFurniture.CANDLESTICK`, a `DelegateBlock`)
> **Package:** `content/block/deco/misc/CandlestickBlock.java`

---

## 1. What it does

The candlestick is a floor-standing decorative block (`canSurvive` demands a face-sturdy
block below, `updateShape` drops it to air if that support goes). It carries a
`BooleanProperty LIT` next to the horizontal `FACING` that `BlockTemplates.HORIZONTAL`
adds, and **emits light only while `LIT`** — before this it was a permanent 15-level
light source, which made it read as a lamp rather than a candlestick.

Interactions, all lifted from vanilla `AbstractCandleBlock` / `CandleBlock`:

| Action | Result |
|---|---|
| Right-click with **flint and steel** | lights it |
| Right-click with a **fire charge** | lights it (and does *not* place fire on the ground) |
| **Empty-hand** right-click | snuffs it, three smoke puffs, `candle_extinguish` |
| Right-click holding anything else | passes through to the item, no state change |

## 2. Why not just extend `AbstractCandleBlock`

Two reasons, both hard:

1. The block is a `DelegateBlock` built from l2modularblock `BlockMethod` interfaces, and
   `AbstractCandleBlock` is a `Block` subclass. Making it one would mean dropping
   `DelegateBlock` and losing the `FACING`/shape plumbing it gets for free.
2. `CandleBlock.canLight(state)` — the hook `IBlockExtension#getToolModifiedState` uses to
   light candles — requires the block to be in `#minecraft:candles` **and** to carry both
   `LIT` and `WATERLOGGED`. The candlestick is a floor deco block that needs no waterlogged
   variant (there is nothing to fill: the holder is a solid cup), so that path is
   unavailable.

So `CandlestickBlock` implements the three `l2modularblock` methods directly instead:
`ToolModifyBlockMethod` for lighting, `UseItemOnBlockMethod` for snuffing, and
`AnimateTickBlockMethod` for the particles. It reuses `BlockStateProperties.LIT` rather
than inventing a property, so the property name and the state id match vanilla's.

### Not implemented (and why)

- **Not in `#minecraft:candles`.** That tag is what `AbstractCandleBlock.isLit` checks, so
  adding the block to it would make a thrown water potion douse it. It is a deliberate
  omission: the tag also implies the `WATERLOGGED` contract above, and the block does not
  honour that contract.
- **No lit/unlit textures.** Vanilla's `candle` swaps to a `_lit` texture, but the visible
  difference is one warm row on the candle's top face — the read comes from the flame
  particles and the light level, not the texture. The candlestick has a single hand-drawn
  32×32 sheet used by both states, exactly as vanilla's `candle` and `candle_cake` reuse
  theirs for the unlit state.

## 3. Flame placement

`FLAME_OFFSETS` is the one piece of model knowledge in the block. The candlestick model has
three candles, each with its own wick element:

| Candle | Wick top (model units) | North-facing offset |
|---|---|---|
| centre | y 14 | `(0.5, 15/16, 0.5)` |
| left arm | y 12 | `(3/16, 13/16, 0.5)` |
| right arm | y 12 | `(13/16, 13/16, 0.5)` |

Each flame sits **one texel above its wick tip**, which is what vanilla does too (its single
candle's wick tops out at y 7 and the particle is at y 8).

**The offsets rotate with `FACING`.** Only the north-facing set is written down; the other
three are turned out at class-init by a local `allFaces` that reuses
`VoxelBuilder#rotateFromNorth`'s angle (`180 - toYRot`), so the flames land on the same
arm the rendered model does — the arms are modelled along x, and the blockstate turns the
model by `toYRot + 180`, so they swing onto the z axis for the east/west facings. Without
this, an east- or west-facing candlestick burned with its two outer flames floating off the
side of the block. `extinguish` reads the offsets through the same `flames(state)` helper,
so the smoke puffs follow too.

Two details in that rotation, both inherited from `VoxelBuilder` rather than invented:

- `Vec3#yRot` pivots about the **origin**, so the offsets are shifted to the block centre
  (0.5, 0.5) before rotating and shifted back after — the same `−8 / yRot / +8` sandwich
  `VoxelBuilder` uses. Skipping it sends the side flames to negative coordinates, outside
  the block.
- `Vec3#yRot` and the blockstate's model rotation are **inverse** rotations of each other,
  so the sign does not strictly agree. It is invisible here because the two side flames are
  exact mirrors about the block centre, so a 180° flip of the pair yields the same pair, and
  only the x↔z axis flip is load-bearing. A deliberately asymmetric candlestick would need
  the sign re-derived.

`animateTick` then runs vanilla's `addParticlesAndSound` per offset unchanged: a
`small_flame` particle every tick, a `smoke` particle 30% of the time, and a
`candle_ambient` sound on 17% of those (i.e. ~5% overall). Snuffing mirrors vanilla's
`extinguish`: state change with flag `11`, a `smoke` particle per offset with a slight
upward drift, `candle_extinguish`, and a `BLOCK_CHANGE` game event so a piston or a
sculk sensor sees the same thing vanilla's candles would.

## 4. What is generated

`runData` writes `blockstates/candlestick.json` with all eight `facing=…,lit=…` variants,
all pointing at the single `block/candlestick` model. The loot table, the `mineable/axe`
tag, and Alice's `structure_fix/alice_house/would_fix` tag are unaffected.
