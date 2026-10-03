# Doll Loadout — four held item slots

Dolls carry a small **inventory of four slots** that determines their behavior (the "doll weapons and logic" pending item). The slots are hosted in `DollData` (authoritative, survives park/restore/logout), mirrored to clients via new synced entity data on `DollEntity`, and **deliberately not** written into vanilla `EquipmentSlot`s — that lets doll weapon AI bypass vanilla equip/AI code entirely.

> Status: planned. Nothing below exists in code yet except the base entity/item/pairing layers it builds on.

## 1. The four slots

`DollSlot` enum (`content/item/doll/DollSlot.java`):

| Slot | Semantics | Accepted items |
|---|---|---|
| `MAIN_HAND` | assault weapon — decides regular / super / suicide attack | every bound item: `DanmakuItem`, `DollLanceItem`, `LaserItem`, throwable `HexBrewBottleItem`, `Items.TNT`, folded heal talisman — plus the vanilla shield (§4) |
| `OFF_HAND` | secondary throw option — hexbrew super attack | the same set as the main hand |
| `CLOTH` | worn paper/cloth — enables passive moves (heal) | none — inert for now, though the heal talisman is the item this slot was designed around and it is bound to a hand |
| `CORE` | reserved for future mechanical cores | none yet — synced but functionally inert |

The accepted set is not a hand-written list: it is the behavior binding table read as a whole (`DollBehaviorRegistry.matchesAny`), so any item that drives some action is admitted to either hand automatically, and a new binding line unlocks its item in the editor with no second edit. The vanilla shield is the one addition — it is a reactive block rather than a behavior (control.md §5.6).

## 2. Storage: authoritative `DollData.inventory`, two variants

```java
@SerialField public MutableDollInventory inventory = new MutableDollInventory(); // 4 slots, never null
```

Two classes share one array-backed shape (one `ItemStack` per `DollSlot` ordinal, never an enum map):

- `DollInventory` (`content/attachment/doll/`) — the **immutable item variant**: reads return copies, writes produce new instances. Backs the doll item's own `DOLL_LOADOUT` component (`GLItems.DOLL_LOADOUT`, never inside `DollItemData`), hence cached `hashCode` like `TalismanPocketData`. Converts to/from the mutable variant only at the item boundary (`fromInventory` / `toInventory`).
- `MutableDollInventory` — the **mutable data variant**: `get` returns the live stack for in-place modification, `set`/`swapHands` mutate. Lives in `DollData` and is the only thing server logic touches.

l2serial round-trips `ItemStack` fields natively (built-in `CodecHandler`), so no registration was needed. Item ↔ data conversion copies every stack, so the component and the ledger never alias.

## 3. Client mirror: synced entity data on `DollEntity`

Four accessors, one per slot (doll-specific logic/render state — `DollEntity`, per entity.md §8.1), default `ItemStack.EMPTY`:

```java
private static final EntityDataAccessor<ItemStack> DATA_MAIN_HAND =
        SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.ITEM_STACK);
// DATA_OFF_HAND / DATA_CLOTH / DATA_CORE — same
```

Direction discipline: server logic reads and writes the ledger directly (`DollEntity.loadout()` / `ledgerStack()` / `consumeLoadoutItem()` / `swapHands()`); the accessors are **client mirror only and are never written back** — `writeValuesTo` carries scalars + color, and every ledger mutation ends with `syncLoadoutMirror()`. `loadout()` never returns null (a detached empty inventory when hostless), so callers need no null checks. `readValuesFrom` seeds the mirror at summon. Vanilla item-stack entity-data serialization is EMPTY-safe, so re-summons/resyncs need no extra packets.

## 4. Arming — the loadout editor menu

Click-arming is gone; the loadout is managed through `DollLoadoutMenu` (`content/entity/dolls/menu/`, opened by sneak-interacting the doll as owner/creative), built on l2core's `BaseContainerMenu` + JSON layout exactly like the equipment menu in ModularGolems and our own talisman pocket:

- Four slots arranged as a cross with no gaps on the right side: left main hand, right off hand, top core, middle cloth (the body-worn slot), plus the player inventory; shift-click moves both ways.
- **Every slot is locked**, on two axes, both enforced in `DollLoadoutItemHandler` — the only writer:
  - *What*: `isItemValid` admits a hand only what a doll can use (`DollBehaviorRegistry.usableInHand` — every bound item, whatever the action type, plus the vanilla `ShieldItem`). Either hand accepts the same set; only blocking reads the off hand, so a shield parked in the main hand is inert until a sticky swap (control.md §2) carries it across. The **core** takes a folded talisman, which the doll wears rather than uses (`DollCoreTalisman`, below). Cloth still takes nothing (its predicate opens up with the future cloth items).
  - *How many*: **one item per hand**, with a single exception — **ammunition**, i.e. an item whose binding *spends* it one per use (`DollBehaviorRegistry.spendsOnUse`, the `consumed` flag each registration declares). So a hand holds a whole stack of throwable hexbrew (up to its own 16), laser charges, or TNT (up to 64), where N in the slot is N throws/charges/dives; a danmaku item, the star wand, the lance and a folded talisman are one per hand however large their own stack is, since nothing is spent (a talisman wears down by durability instead). Core and cloth are single slots whatever the stack would allow.
  - The count rule is **stack-dependent**, which `IItemHandler.getSlotLimit` cannot express, so it is asked per item: the menu's `LoadoutSlot` (a two-line `ItemHandlerCopySlot` subclass) overrides `getMaxStackSize(ItemStack)` with `DollLoadoutItemHandler.stackLimit`. Every vanilla placement path sizes its transfer through that overload — click, number-key swap, shift-click, drag — so nothing is ever over-written and then truncated away. `insertItem` applies the same rule for non-menu callers, and `getSlotLimit` answers the 64 ceiling as the honest stack-agnostic answer.
- Backed live by the ledger `MutableDollInventory` through `DollLoadoutItemHandler`, refreshing the client mirror on every change — no staging, no take-back on close.
- Like the ModularGolems equipment menu, the doll itself renders on the left side of the screen (`InventoryScreen.renderEntityInInventoryFollowsAngle`, mouse-following), and the slots use a background atlas instead of baked-in frames: layout comps are blank `empty_slot`s while the screen draws `slot` frames plus per-slot ghost icons (`slotbg_main` sword, `slotbg_off` shield, `slotbg_core` gem, `slotbg_cloth` paper) from the texture side sprites whenever a slot is empty. The container source lives at `src/test/.../gui/-templates/container/gensokyolegacy/doll_loadout.json` — regenerate with `organize.GUIGenerator`, never hand-edit the texture or coordinates.
- Layout `data/.../l2core/menu_layout/doll_loadout.json` + texture `textures/gui/container/doll_loadout.png`; menu type `GLMisc.DOLL_LOADOUT`; `stillValid` closes when the doll is gone or the viewer isn't the owner.
- The same menu edits a doll **item**: right-clicking the stack in an inventory (`InvClickItem` via `GLClickHandler`, item.md §8) opens it through `DollItemLoadoutProvider`, backed live by the stack's `DOLL_LOADOUT` component instead of the ledger. The network flag (`true` = entity id, `false` = item) tells `fromNetwork` which backing to build client-side.
- Plain empty-hand itemize is unchanged; non-sneak interactions with items pass through.

## 5. Item rendering — TODO (deferred)

Main-hand / off-hand items should be rendered on the doll model (GeckoLib geo). **Deferred by design**: the synced data + slot API §2/§3 are the contract; the render pass is a later TODO. Drop placeholder hooks (an `ItemRenderer` call) into `DollRenderer`/`DollModel`, DDC'd off. Cloth-slot clothing and core rendering are likewise future work — all four actions in control.md run server-side, so client visuals are not required for the feature to function.

## 6. Relation to the rest of the system

- The authoritative inventory is `DollData.inventory`, so it survives itemize→summon parking and logout (pairing.md §2.2).
- Action selection reads these slots (`control.md`); the glove issues commands (`glove.md`).
- The core slot is not read by action selection at all: `DollBehaviorRegistry.findBest` scans hands only. A core talisman is a *passive holder*, reached instead through `TalismanCurioItem`'s discovery — the same discovery the Curios charm slot uses, generalised by the `TalismanHolder` interface that `DollCoreTalisman` implements. So a doll's core paper gets the passive tick (`DollEntity.tick` → `tickCoreTalisman`) and the on-attacked / on-damaged hooks (`GLAttackListener`) for free, with no Curios involved. It is **self-only by construction**: the context's target is the doll, and an effect only ever acts on its own target, which is the deliberate opposite of a heal talisman in a hand, which the doll walks over to a wounded ally to spend.
- Nothing ever drops: suicide consumes only the TNT, everything else persists on the item (control.md §7).