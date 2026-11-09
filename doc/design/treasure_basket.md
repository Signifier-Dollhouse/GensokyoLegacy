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
  can be resolved without loading or ticking the basket block. `AreaEffectManager.getOwner` is the
  lookup.
- **No client sync:** the `toClient = false` field is skipped by the default packet codec, which
  l2serial applies `SerialField::toClient` to on every packet (`CodecAdaptor.toNetwork`).
- **No block entity storage:** breaking the block drops the contents straight from the effect data.

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

### Client visual

`TreasureBasketEffectData.getClientVisual` returns the sealing pot's forcefield visual
(`textures/barriers/sealing_pot.png`, green tint, walls only) as a placeholder, gated the same way:
`StrangeGlassesItem.isWearing` or holding a treasure basket in either hand. Replace the constant
when the basket gets its own texture.

---

## 3. Expiry interception

`TreasureBasketEventHandlers.onEntityTick` (on `EntityTickEvent.Post`) makes items age faster while a
basket covers their chunk: each basket adds `treasureBasketExpirySpeed` ticks of age per game tick,
minus the one tick the item's own `tick` already applied. Items with a negative age (unlimited or
offset lifetime, e.g. `setUnlimitedLifetime`) are left alone, and the age is capped at
`Short.MAX_VALUE - 1` like the vanilla expiry check.

`TreasureBasketEventHandlers.onItemExpire` is registered at `EventPriority.LOWEST` so it runs after
other mods had a chance to extend an item's lifetime.

Key checks:

1. `item.age >= clamp(item.lifespan + event.getExtraLife(), 0, Short.MAX_VALUE - 1)` — another mod's
   extension (or a directly changed `lifespan`) wins; the item is not caught early.
2. Candidate baskets are the `TreasureBasketEffectData` effects affecting the item's chunk, sorted
   by squared distance from the item to the basket block.
3. `TreasureBasketEffectData.insert` merges into matching stacks first, then fills empty slots and
   returns the remainder. A stack too large for the nearest basket is split: the nearest takes what
   fits, and the remainder is offered to the next nearest.
4. A fully stored item is discarded; otherwise only the remainder stays on the item entity, which
   then expires as usual.

---

## 4. Block entity, item handler and GUI

`TreasureBasketBlockEntity` exists only as a capability/menu handle; it never stores items itself.

- `TreasureBasketItemHandler` implements `IItemHandlerModifiable` and re-resolves the effect data on
  every call through `AreaEffectManager.getOwner`, so it stays valid while the owner chunk is
  unloaded. It is exposed as `Capabilities.ItemHandler.BLOCK` for the block entity.
- `TreasureBasketMenu` is a 6-row / 54-slot `AbstractContainerMenu` laid out like a large chest. The
  server slots are `ItemHandlerCopySlot`s over the effect-backed handler; the client ones wrap a
  dummy `ItemStackHandler`, which the menu's normal slot sync fills.
- `TreasureBasketScreen` draws `textures/gui/container/generic_54.png` in the two blits vanilla's
  `ContainerScreen` uses; `imageHeight = 114 + 6 * 18 = 222`.
- Right-clicking opens the menu. The block position travels in the menu open buffer, so
  `TreasureBasketMenu.fromNetwork` can rebuild the client menu.

---

## 5. Breaking the basket

`onReplaced` calls `AreaEffectManager.removeOwner`, which now returns the removed
`AreaEffectEntry`. If the entry carries `TreasureBasketEffectData`, every stored stack is popped as
an item at the block position and the buffer is cleared.

---

## 6. Config

`GLModConfig.Server.treasureBasketRadius` — default `7`, range `1..15` (the area effect system's
`(2r+1)^2 <= 1024` chunk budget).

`GLModConfig.Server.treasureBasketExpirySpeed` — default `10`, range `1..100`; ticks an item ages per
game tick for each basket covering its chunk (`1` disables the acceleration).

---

## 7. Files

| Class | Role |
|---|---|
| `content/block/functional/treasure/TreasureBasketBlock.java` | Placement/removal behavior, effect ownership, menu opening. |
| `content/block/functional/treasure/TreasureBasketEffectData.java` | 54-slot buffer, insertion, drop-all, placeholder client visual. |
| `content/block/functional/treasure/TreasureBasketBlockEntity.java` | Block entity that exposes the capability and the menu. |
| `content/block/functional/treasure/TreasureBasketItemHandler.java` | `IItemHandlerModifiable` facade over the effect data. |
| `content/block/functional/treasure/TreasureBasketMenu.java` | 54-slot chest-style menu. |
| `content/block/functional/treasure/TreasureBasketScreen.java` | Large-chest background rendering. |
| `event/TreasureBasketEventHandlers.java` | `EntityTickEvent.Post` ageing speed and `ItemExpireEvent` interception. |
| `init/registrate/block/GLBlocks.java` | `TREASURE_BASKET` / `TREASURE_BASKET_BE` registration, sealing-pot placeholder model. |
| `init/registrate/GLMisc.java` | `treasure_basket` menu registration. |
| `init/data/GLModConfig.java` | `treasureBasketRadius`. |
