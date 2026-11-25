# Structure Mirror Stick

A debug item that places vanilla **jigsaw blocks** between the pieces of a multi-piece structure that is laid out
in the world with vanilla **structure blocks**, so the pieces can be saved as templates that form a valid jigsaw
tree. The stick is an authoring aid only: it records the tree on the item, places and validates the connector
blocks, and never saves templates or generates structures.

## 1. Requirements

### 1.1 Right-click a block

**No recorded data (acquire the root).** The clicked block must lie inside the bounds of a vanilla structure
block. That structure block's piece becomes the root entry. Nothing else changes in the world. If no structure
block bound contains the click, fail with a reason.

**Recorded data (attach a child).**

1. The clicked block must lie inside a recorded entry's bound. If it lies in more than one, the smallest bound
   wins (most specific piece).
2. The clicked block must be on a face of that bound: exactly one of its six neighbours must be outside the
   bound.
   - No out-of-bound neighbour: the click was not on a face.
   - Two or more out-of-bound neighbours: an edge or corner.
   - Either way, fail with a reason.
3. The single out-of-bound neighbour is the **parent-side jigsaw cell**: the empty cell just off the clicked face.
   The cell one further outward is the **child-side jigsaw cell**, and it must lie inside the bounds of another
   vanilla structure block. That piece is the child. If not, fail.
4. That piece must not already be recorded. A piece has exactly one parent, so a connector between two recorded
   pieces is forbidden (it would break the tree).
5. Both jigsaw cells must be **air**: a jigsaw turns into air (`final_state`), so only air may be replaced and the
   clicked block itself is never touched. A non-air cell fails, with a specific reason for a jigsaw or a structure
   block.
6. Place the jigsaw pair:
   - parent jigsaw: the cell off the clicked face, front facing the child jigsaw;
   - child jigsaw: the next cell outward, front facing the parent jigsaw.
   - Both blocks turn into `minecraft:air` (`final_state`).
   - A horizontal connector uses `joint = rollable`; a vertical connector uses `joint = aligned` so the child
     cannot yaw-roll around the vertical axis. Both blocks of a vertical pair use `top = north`.
7. Record the child entry and show a success message with both piece names.

### 1.2 Jigsaw names and pool

Names are derived from the two structure block names. For parent `a:b/c` and child `a:b/d` (the short names are
the last path segments, `c` and `d`):

| block | `name` | `target` | `pool` |
|---|---|---|---|
| parent | `a:c2d` | `a:d2c` | `a:b/d` (the child piece) |
| child | `a:d2c` | `a:c2d` | `minecraft:empty` |

This satisfies vanilla's `JigsawBlock.canAttach`: the parent's target equals the child's name, the two fronts
are opposite, and (for an aligned joint) the tops match. The direction-specific names also guarantee that a
piece can never attach through one of its own exit connectors, because an exit connector is named after its
target piece, not after the piece that owns it.

### 1.3 Right-click air

Print the recorded tree to chat **and** to the log. One indented line per entry shows the piece name only; the
bound and connector positions are omitted so the tree shape stays readable. A name that cannot be read (structure
block missing or chunk unloaded) is shown as `<unloaded #index>`, and an entry outside every root's subtree is
prefixed with `orphan`.

### 1.4 Shift-right-click air

Scan every recorded pair and validate both jigsaw blocks: block present, front/top orientation, name, target,
pool, joint type and final state. Invalid pairs are re-placed and re-configured. Report the number of valid,
fixed and failed pairs, with a reason per pair, to chat and the log.

### 1.5 Right-click bedrock outside every bound

Right-clicking a bedrock block that lies outside every recorded bound triggers a vanilla structure-block **save**
on every recorded piece, writing each piece to its `data/<namespace>/structure/<name>.nbt`. Report how many pieces
were saved and name the ones that failed (structure block missing, unloaded, unnamed, or no longer in save mode).

### 1.6 Left-click a jigsaw

While holding the stick, left-clicking a jigsaw that belongs to a recorded pair:

- if the pair's child entry is a **leaf** (no children), remove both jigsaw blocks and drop the child entry from
  the item data;
- otherwise fail, report the reason, and **do not destroy the block**.

A jigsaw that is not part of a recorded pair also fails and is not destroyed.

### 1.7 Failure reporting

Every failure shows a human-readable reason in chat and writes it to the log.

### 1.8 Bound outline

While the stick is held the client draws every recorded bound as a red line box, `0.05` block larger than the
true bound on every side so it does not hide the structure block's own bound rendering. The box is read from the
item component every frame, so it never lags the recorded tree.

## 2. Item data

A data component holds one ordered list of entries:

```java
record Entry(
    BoundingBox bound,          // world bounds of the piece
    BlockPos block,             // structure block position (piece identity)
    int parent,                 // index of the parent entry; -1 for the root
    @Nullable BlockPos parentJigsaw, // jigsaw off the parent's bound face on the parent link
    @Nullable BlockPos childJigsaw   // jigsaw inside this piece on the parent link
)
```

The root has `parent = -1` and both jigsaws null. Ordering is parent-before-child, so every index is valid and the
list describes a tree. Both connector positions are stored: the pair sits in the air between the two bounds, so
neither position can be derived from the other plus a bound.

The piece name is not stored; it is read from the structure block at `block` when the chunk is loaded.
Authoring always happens with the structure blocks loaded.

## 3. Plan

1. `content/item/debug/StructureMirrorData.java`: entries, codec, tree helpers (bounds hit test, parent
   direction, parent jigsaw, leaf test, add/remove).
2. `content/item/debug/StructureMirrorHelper.java`: loaded-chunk structure-block scan, bound computation
   (rotation/mirror included), name lookup, connector naming, pair placement, validation and repair, tree
   description.
3. `content/item/debug/StructureMirrorStick.java`: `use` (tree / repair), `useOn` (root / attach / save),
   tooltip.
4. `StructureMirrorStick.canAttackBlock`: server-side delete on left-click, always refusing the break so the
   block is never destroyed.
5. `content/client/debug/StructureMirrorOutlineRenderer.java`: red bound outline while the stick is held, wired
   in `GLClientEventHandlers` at `AFTER_BLOCK_ENTITIES`.
6. Register the item and data component in `GLItems`; tag it as a debug item.
7. Add `GLLang.ItemDebug` messages and zh_cn translations; run `organize.ResourceOrganizer`.
8. Run datagen for the item model and English lang, then build.

All world access is server-side. The stick never force-loads chunks: a structure block in an unloaded chunk is
reported as unavailable, not loaded.