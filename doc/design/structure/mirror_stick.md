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
3. The single out-of-bound neighbour is the **child-side** cell. It must lie inside the bounds of another
   vanilla structure block. If not, fail.
4. That piece must not already be recorded. A piece has exactly one parent, so a connector between two recorded
   pieces is forbidden (it would break the tree).
5. Neither connector cell may already be a jigsaw block or a structure block. If it is, fail.
6. Place the jigsaw pair:
   - parent jigsaw: the clicked block, front facing the child cell;
   - child jigsaw: the out-of-bound neighbour, front facing the parent cell.
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

Print the recorded tree to chat **and** to the log. Each line names the entry, its index, its bound, its
structure block position, and (for non-root entries) the parent index and the two connector positions. Names
are shown when the structure block is in a loaded chunk.

### 1.4 Shift-right-click air

Scan every recorded pair and validate both jigsaw blocks: block present, front/top orientation, name, target,
pool, joint type and final state. Invalid pairs are re-placed and re-configured. Report the number of valid,
fixed and failed pairs, with a reason per pair, to chat and the log.

### 1.5 Left-click a jigsaw

While holding the stick, left-clicking a jigsaw that belongs to a recorded pair:

- if the pair's child entry is a **leaf** (no children), remove both jigsaw blocks and drop the child entry from
  the item data;
- otherwise fail, report the reason, and **do not destroy the block**.

A jigsaw that is not part of a recorded pair also fails and is not destroyed.

### 1.6 Failure reporting

Every failure shows a human-readable reason in chat and writes it to the log.

## 2. Item data

A data component holds one ordered list of entries:

```java
record Entry(
    BoundingBox bound,          // world bounds of the piece
    BlockPos block,             // structure block position (piece identity)
    int parent,                 // index of the parent entry; -1 for the root
    @Nullable BlockPos childJigsaw // jigsaw block inside this piece on the parent link
)
```

The root has `parent = -1` and `childJigsaw = null`. Ordering is parent-before-child, so every index is valid
and the list describes a tree.

The parent-side jigsaw position and the link direction are **derived** from `childJigsaw` and the parent bound:
exactly one neighbour of `childJigsaw` lies inside the parent bound; that cell is the parent jigsaw, and the
direction from the child jigsaw to it is the direction from the child toward the parent. No duplicate joint
list is stored.

The piece name is not stored; it is read from the structure block at `block` when the chunk is loaded.
Authoring always happens with the structure blocks loaded.

## 3. Plan

1. `content/item/debug/StructureMirrorData.java`: entries, codec, tree helpers (bounds hit test, parent
   direction, parent jigsaw, leaf test, add/remove).
2. `content/item/debug/StructureMirrorHelper.java`: loaded-chunk structure-block scan, bound computation
   (rotation/mirror included), name lookup, connector naming, pair placement, validation and repair, tree
   description.
3. `content/item/debug/StructureMirrorStick.java`: `use` (tree / repair), `useOn` (root / attach), tooltip.
4. `StructureMirrorStick.canAttackBlock`: server-side delete on left-click, always refusing the break so the
   block is never destroyed.
5. Register the item and data component in `GLItems`; tag it as a debug item.
6. Add `GLLang.ItemDebug` messages and zh_cn translations; run `organize.ResourceOrganizer`.
7. Run datagen for the item model and English lang, then build.

All world access is server-side. The stick never force-loads chunks: a structure block in an unloaded chunk is
reported as unavailable, not loaded.