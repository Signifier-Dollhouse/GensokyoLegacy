# Interior Wand

A debug/authoring item for a structure's interior: room boxes (`InteriorRoom`) and the routing
nodes between them (`InteriorNode`). It is the authoring counterpart of `tools/checkconfig.py`
and of the hand-written `StructureConfigBuilder` interiors. The wand records an origin from a
vanilla structure block's bound, edits rooms and waypoints in the world, and exports the
template-local `interior` JSON.

Related: [interior_rework.md](interior_rework.md), [mirror_stick.md](mirror_stick.md).

## 1. Item data

Two data components:

- `gensokyolegacy:interior_wand_mode` — the current mode, an `InteriorWandMode` ordinal. Picked
  from the shared item selector (wheel + scroll), and every mode is always listed.
- `gensokyolegacy:interior_wand` — `InteriorWandData`:

```java
record InteriorWandData(
    BlockPos structureBlock,     // the structure block that supplied the origin
    BlockPos origin,             // world position of template-local (0,0,0)
    List<BoundingBox> rooms,     // world coords, auto-named room_<index> on export
    List<Node> nodes,            // world coords, room indices into `rooms`
    int selectedRoom,            // bound-edit selection, -1 for none
    @Nullable BlockPos pendingStart,   // first corner of a bound in progress
    int nodeRoomA, int nodeRoomB,      // waypoint room selections; NO_ROOM / EXIT
    @Nullable BlockPos pendingNodeA    // first position of a dual waypoint
)
```

`Node` mirrors `InteriorNode`: `posA`, `posB`, `roomA`, `roomB`, with `roomB = -1` an entry/exit.
Rooms and nodes are stored in world coordinates, because that is what hit-testing and rendering
need; conversion to template-local happens once, at export. Transient click state lives in the
same component so it survives a mode change and is reset by `InteriorWandItem.setMode`.

## 2. Origin

The first right-click on a block inside a vanilla structure block's bound records:

- the structure block position (identity, and the source of rotation/mirror at export);
- the placement corner `structureBlock + structurePos`, which is the world position of local
  `(0,0,0)`.

The smallest bound wins when several structure blocks overlap, the same rule as the mirror
stick. After that, every room and node belongs to this origin; shift-right-click air clears the
component and starts over.

## 3. Modes

Mode switching is through the shared `l2itemselector` wheel; the wand is tagged
`l2itemselector:selectable`. Switching modes drops any half-finished click.

### 3.1 Bound

- Right-click a block → record it as the first corner (`pendingStart`).
- Right-click a second block → `BoundingBox.fromCorners`, append a room, clear the pending
  corner.
- Left-click a block inside a recorded bound → delete that bound (and every node touching it,
  renumbering the remaining node room indices).
- Left-click never breaks a block: the wand overrides `canAttackBlock` to return false, and that
  same override is where the left-click logic runs. It is the only left-click hook that reaches
  the server. A held creative click resends the action every 5 ticks, so repeats within 6 ticks
  count as one click.

### 3.2 Bound edit

- With no selection, right-click a block inside a bound selects it.
- With a selection, right-click a block outside it deselects.
- Right-click a block inside the selected bound expands the bound by one layer on the clicked
  block face. The face is the axis the player is looking at; it is not derived from the bound
  centre, so a click on a room's floor extends downward.
- Left-click a block inside the selected bound shrinks it by one layer on the clicked face.
  A bound already one block thick on that axis cannot shrink further.

Only the clicked face is used. A block in the middle of a room is a valid click; the wand does
not require the clicked block to lie on the bound's own boundary. Left-clicks get no face from
`canAttackBlock`, so the face is the one from the block centre toward the player's eye.

### 3.3 Waypoint

A node needs two rooms and one waypoint cell.

1. Right-click a block inside a bound → room A. The first selection must be a bound; an exit is
   only meaningful as the far end.
2. Right-click a block in a different bound → room B, or outside every bound → exit
   (`roomB = -1`).
3. Right-click a block → the waypoint is the cell **off the clicked face**
   (`pos.relative(face)`). Both endpoints are that cell, giving a shared doorway node.

Left-click deletes the node whose `posA` or `posB` matches the clicked cell or the cell off the
clicked face. This is why creation and deletion both use the face offset: the waypoint itself
is air and cannot be attacked, so the author clicks the solid block it sits on.

### 3.4 Dual waypoint

Same two room selections, then two right-clicks for `posA` and `posB` (each off its clicked
face), producing a stair-style dual node. Dual mode refuses an exit as room B; use waypoint mode
for an entry node.

### 3.5 Export

Right-click a block or air writes the recorded `interior` object as pretty-printed JSON to
`out/interior/<namespace>/<structure name>.json` under the game directory. The file contains
template-local coordinates, so it can be pasted into the structure's data-map entry or moved to
`src/generated/resources/data/gensokyolegacy/data_maps/worldgen/structure/structure_config.json`.
A preview is still printed to chat and the log by right-clicking air in the other modes.

World-to-template conversion inverts vanilla's `StructureTemplate.transform` about the zero
pivot: undo the rotation, then undo the mirror, then subtract the recorded origin. A structure
block that is missing or unloaded falls back to no rotation and mirror, and says so in chat.

## 4. Rendering

While the wand is held, every recorded bound is drawn as a red line box, `0.05` blocks larger
than the true bound. The selected bound (bound edit) and the two waypoint selections are green.
Recorded node cells are small cyan cubes, a pending bound corner and a pending dual waypoint are
yellow. The renderer reads the held component every frame and holds no state of its own.

## 5. Why the wand does not write the data map

The structure config is a NeoForge data map generated at datagen time; changing it at runtime
would be a second source of truth that disappears on restart. The wand therefore produces the
exact JSON fragment, in template coordinates, and leaves writing the data map to datagen. This
is the same split as the mirror stick, which places jigsaws but never saves templates.

## 6. Files

| File | Role |
|---|---|
| `content/item/debug/InteriorWandItem.java` | clicks, mode dispatch, export command, tooltip |
| `content/item/debug/InteriorWandData.java` | recorded rooms/nodes and click state, codec |
| `content/item/debug/InteriorWandMode.java` | the five modes |
| `content/item/debug/InteriorWandHelper.java` | origin lookup, inverse transform, JSON export |
| `content/item/debug/InteriorWandSelectionListener.java` | item-selector registration |
| `content/item/debug/client/InteriorWandModeWheel.java` | selector wheel |
| `content/client/debug/InteriorWandOutlineRenderer.java` | bound and waypoint outlines |
