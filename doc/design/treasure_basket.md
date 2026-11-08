# Treasure Basket (`treasure_basket`) — Item Time-out Catch via Area Effect

The **treasure basket** is a `DelegateBlock` in `block.functional.treasure`. It owns one area effect
of `r = 7` chunks (configurable as `treasureBasketRadius`); while an item entity inside that area
times out, the basket stores the stack instead of letting it despawn.

The block has **no model or texture of its own yet**: its blockstate and item model reuse
`custom/utensil/sealing_pot`, `block/utensil/sealing_pot` and `item/utensil/sealing_pot`.

---

## 1. Storage lives in the effect, not the block

`TreasureBasketEffectData` extends `EffectData` and holds a 54-slot `ItemStack[]` (the size of a
large chest). The array is `@SerialField(toClient = false)`:

- **Persistence:** the effect entry is part of `LevelAreaAttachment`, so contents save with the
  level attachment even when the basket's own chunk is not loaded.
- **Access while unloaded:** `LevelAreaAttachment` keeps `byOwner: BlockPos -> UUID`, so the data
  can be resolved without loading or ticking the basket block.
- **No block entity:** breaking the block drops the contents straight from the effect data.
- **No client sync:** the `toClient = false` field is skipped by the area-effect sync packet's
  `SerialField::toClient` codec predicate (`AreaEffectSyncPacket.CODEC`).

L2Serial still reads the array on disk with the default "all fields" predicate, so the contents
round-trip.

---

## 2. Placement rule

`TreasureBasketBlock` uses `PlacementBlockMethod` + `OnPlaceBlockMethod` + `OnReplacedBlockMethod`.
A basket cannot be placed in any chunk already covered by a treasure basket effect, on both sides:
the server checks `AreaEffectManager.getAffecting`, the client checks
`ClientAreaEffectTracker.getAffecting`. Like the sealing pot, the guard also covers
`/setblock` and piston paths inside `onPlace`.

Two baskets may still have overlapping areas when one is placed just outside the other's range;
the nearest-basket rule below handles that.

---

## 3. Expiry interception

`TreasureBasketEventHandlers.onItemExpire` is registered at `EventPriority.LOWEST` so it runs after
other mods had a chance to extend an item's lifetime.

Key checks:

1. `item.age >= clamp(item.lifespan + event.getExtraLife(), 0, Short.MAX_VALUE - 1)` — another mod's
   extension (or a directly changed `lifespan`) wins; the item is not caught early.
2. Candidate baskets are the `TreasureBasketEffectData` effects affecting the item's chunk, sorted
   by squared distance from the item to the basket block.
3. `TreasureBasketEffectData.insert` is all-or-nothing: it merges into matching stacks first, then
   fills empty slots, but only when the basket can take the whole stack. Otherwise the nearest
   basket is refused and the next nearest is tried.
4. A fully stored item is discarded. When no basket has room, the item expires as usual; a stack is
   never split between baskets.

---

## 4. Breaking the basket

`onReplaced` calls `AreaEffectManager.removeOwner`, which now returns the removed
`AreaEffectEntry`. If the entry carries `TreasureBasketEffectData`, every stored stack is popped as
an item at the block position and the buffer is cleared.

---

## 5. Config

`GLModConfig.Server.treasureBasketRadius` — default `7`, range `1..15` (the area effect system's
`(2r+1)^2 <= 1024` chunk budget).

---

## 6. Files

| Class | Role |
|---|---|
| `content/block/functional/treasure/TreasureBasketBlock.java` | Placement/removal behavior and effect ownership. |
| `content/block/functional/treasure/TreasureBasketEffectData.java` | 54-slot buffer, insertion, drop-all. |
| `event/TreasureBasketEventHandlers.java` | `ItemExpireEvent` interception. |
| `init/registrate/block/GLBlocks.java` | `TREASURE_BASKET` registration, sealing-pot placeholder model. |
| `init/data/GLModConfig.java` | `treasureBasketRadius`. |
| `content/attachment/area/AreaEffectSyncPacket.java` | Network codec that honours `toClient = false`. |
