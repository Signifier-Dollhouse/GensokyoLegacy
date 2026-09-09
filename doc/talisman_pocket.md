# Talisman Pocket — implementation plan (Phase 5)

Fill in the deferred `TalismanPocket` item + its 9-slot management menu. Uses the
requested stack: `IItemHandler` + `StackCopySlot`/`ItemHandlerCopySlot` +
`BaseContainerMenu` + the layout generator. GUI generator patterns are copied from
L2Backpack (`BaseBagMenu`, `BaseOpenableScreen`) and ModularGolems
(`EquipmentsMenu`, `ToggleGolemConfigMenu`).

## Existing state

- `DC_TALISMAN_POCKET` (`GLTalismans.java:34`) already holds `TalismanPocketData(TalismanSlot[9])`
  in `pocket/TalismanSlot.java` where each `TalismanSlot` = `(foldedStack, paperStack)`.
- `TalismanCurioItem.java` provides the single curio tick path (`curioTick` → `getActiveTalismans`).
- `FoldedPaperTalisman.fold(paper)` / `.paper(stack)` exist.
- No pocket item, menu, screen, or layout exist yet (GL has no `menu_layout` jsons yet).

## Design decisions

1. **Slot model.** Pocket slots are *not indexed by kind* (per `doc/talisman.md`): a slot's
   kind is derived from its contents via `TalismanSlot.kindItem()`. The menu shows exactly
   9 slots, one per kind currently held, in pocket-index order.
2. **`IItemHandler` facade, not `ComponentItemHandler`.** A `TalismanSlot` holds *two*
   stacks; `ComponentItemHandler` (NeoForge) only stores one per slot, so it can't back the
   existing record. Instead we write a custom `TalismanPocketItemHandler`
   `implements IItemHandlerModifiable` that reads/writes `DC_TALISMAN_POCKET`. It must be
   `IItemHandlerModifiable` because `ItemHandlerCopySlot.setStackCopy` casts to it
   (see ItemHandlerCopySlot.java:41). This keeps the already-settled record serialization
   and the two-stack magazine model intact.
3. **Handler slot semantics** (index `i` = pocket slot):
   - `getStackInSlot(i)` = `foldedStack` if present, else `paperStack` (display/menu icon).
   - `isItemValid(i, s)` = s is a folded talisman or a talisman paper, **and**
     (slot `i` is empty **or** `slot.kindItem() == s.getItem()`). Wrong-kind and 10th-kind
     are rejected implicitly (every occupied slot refuses).
   - `insertItem(i, s, sim)`:
     - folded of kind k: allowed only if slot i is empty, or slot i has kind k but an empty
       `foldedStack`. Sets `foldedStack = s`; returns EMPTY. Reject if the slot already has
       a folded talisman.
     - talisman paper of kind k: slot empty → fold one paper via `FoldedPaperTalisman.fold`
       into `foldedStack`, remainder → `paperStack`; slot already kind k with a filled folded →
       remainder → `paperStack` (cap 64, return leftover).
     - `simulate` mutates a throwaway copy of the data.
   - `extractItem(i, n, sim)` = take the active `foldedStack` (count 1) if present, else take
     `n` papers off `paperStack`. Extraction first is the right priority: the menu's visible
     item is the active talisman, and "remove" means "take my talisman out".
   - `setStackInSlot(i, s)` = clear slot then reinsert (`ItemHandlerCopySlot`/quickcraft path).
   - Handler holds the **live pocket `ItemStack`** captured in the menu constructor and writes
     `stack.set(DC_TALISMAN_POCKET, newData)` on every change — this is the write-back.

## Menu

`TalismanPocketMenu extends BaseContainerMenu<TalismanPocketMenu>`
(`dev.xkmc.l2core.base.menu.base.BaseContainerMenu`).

- Static layout:
  ```java
  public static final SpriteManager MANAGER = new SpriteManager(GensokyoLegacy.MODID, "talisman_pocket");
  ```
- Constructor (L2Backpack `BaseBagMenu` super pattern — real storage is the handler, the
  `SimpleContainer(0)` is the never-used placeholder):
  ```java
  super(GLMisc.TALISMAN_POCKET.get(), wid, plInv, MANAGER, menu -> new SimpleContainer(0), false);
  handler = new TalismanPocketItemHandler(player.getItemInHand(hand));   // hand from buffer
  getLayout().getSlot("grid", (x, y) -> new ItemHandlerCopySlot(handler, added++, x, y), this::addSlot);
  ```
  (identical to `BaseBagMenu.addSlot`, but with NeoForge's `ItemHandlerCopySlot` because the
  storage matches `StackCopySlot`'s immutable-storage contract.)
- `quickMoveStack`: copy `BaseBagMenu.quickMoveStack` (`L2Backpack`), using
  `handler.getSlots()` for the target range `[36, 36+n)` instead of
  `container.getContainerSize()` (which is 0 here). Shift-click routing then falls out of
  `isItemValid` per slot: same-kind slot first, then first empty.
- `fromNetwork(MenuType, int, Inventory, RegistryFriendlyByteBuf)` reads the hand ordinal
  (single byte) and resolves `player.getItemInHand(hand)` on *both* sides so client and
  server build the handler over the same pocket stack.
- `stillValid` = pocket stack is still in the hand.
- Register in `GLMisc`:
  ```java
  public static final MenuEntry<TalismanPocketMenu> TALISMAN_POCKET =
      GensokyoLegacy.REGISTRATE.menu("talisman_pocket",
          TalismanPocketMenu::fromNetwork, () -> TalismanPocketScreen::new).register();
  ```
  (L2Backpack `LBMenu` / ModularGolems pattern.)

## Screen

`TalismanPocketScreen extends BaseContainerScreen<TalismanPocketMenu>` — copy
`BaseOpenableScreen` verbatim:
```java
@Override protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
    getRenderer().start(g);
}
```
`BaseContainerScreen` already sets `imageHeight`/`inventoryLabelY` from
`menu.getLayout()` and `getRenderer()` resolves the texture from `SpriteManager.id()`
→ `textures/gui/container/talisman_pocket.png`.

## Layout + texture (datapack `l2core:menu_layout`)

`src/main/resources/data/gensokyolegacy/l2core/menu_layout/talisman_pocket.json`
(same files-as-plain-resources convention as ModularGolems' `equipments.json` /
L2Backpack's `backpack_1.json`):
```json
{
	"height": 166,
	"comp": { "grid": { "x": 62, "y": 17, "w": 18, "h": 18, "rx": 3, "ry": 3 } },
	"side": {}
}
```
`MenuLayoutConfig.getSlot` expands `rx`×`ry` starting at (x, y); our
`(rx:3, ry:3)` yields the 9 positions matching pocket indexes 0..8.

Texture `assets/gensokyolegacy/textures/gui/container/talisman_pocket.png` — 176×166
(right), standard inventory frame + 3×3 slot grid starting at (62,17), same slot
styling as `trade_1..5.png`. (Height 166 ⇒ player inventory at y=84, fits 3 rows
ending at y=71.)

## Pocket item

`content/item/talisman/pocket/TalismanPocket.java extends TalismanCurioItem`:

- `getActiveTalismans(stack)` — read `DC_TALISMAN_POCKET`, return the `foldedStack` of
  every occupied slot (preserves existing `curioTick` wiring).
- `curioTick` — `super.curioTick(...)`, then if server, `refill(stack)`:
  for each slot with `foldedStack` empty and `paperStack` non-empty →
  `foldedStack = FoldedPaperTalisman.fold(takeOne(paperStack))`; a folded talisman spent to
  durability 0 vanishes and is re-folded on the next tick (self-reloading magazine).
  Empty paperStack + empty foldedStack clears the slot (`findSlot`/`findEmptySlot` already
  support this).
- `use()` — on server `sp.openMenu(new TalismanPocketProvider(sp, hand))`; provider writes
  the hand ordinal and `createMenu` returns `new TalismanPocketMenu(...)`. (ModularGolems
  `ToggleGolemConfigMenu`/GL `TradeProvider` pattern.)
- `appendHoverText` — short usage line via new `GLLang.Talisman.POCKET_DESC`.

Register in `GLTalismans` (Phase 5 slot, mirroring `FOLDED_PAPER_TALISMAN`):
`stacksTo(1)`, tag `curios:charm`, layered item model + `GLClient` tint registration
(reuse `talisman_pocket` texture preset or a pocket-specific one), `.lang("Talisman Pocket")`.

Lang: add `Talisman.POCKET("Talisman Pocket")` + `Talisman.POCKET_DESC(...)` to `GLLang`;
zh_cn to `src/test/resources/.../zh_cn/items.json`, rerun `organize.ResourceOrganizer`.

## Sync / correctness notes

- `broadcastChanges` pushes slot snapshot diffs (StackCopySlot `getItem()` is a fresh copy
  per read); since both sides build the handler over the same mirrored pocket stack, server
  writes propagate correctly. Container-based recursion is avoided entirely
  (`isVirtual=false`, placeholder `SimpleContainer(0)`), so no `securedServerSlotChange`
  override is needed.
- First-item cap: reserve `paperStack` ≤ 64; `setStackInSlot` clamps rather than dropping,
  and `quickMoveStack` uses `insertItem`-aware transfer (like `DrawerQuickInsert`) so big
  stacks route correctly (fold 1, rest into reserve, overflow returned).
- Cooldowns stay keyed on the paper `Item` (doc/talisman.md) — unaffected.

## New / touched files

- New: `TalismanPocketItemHandler` (pocket pkg), `TalismanPocket`, `TalismanPocketMenu`,
  `TalismanPocketScreen`, `TalismanPocketProvider` (content/ui/talisman pkg).
- New resources: `data/.../l2core/menu_layout/talisman_pocket.json`,
  `textures/gui/container/talisman_pocket.png`, pocket item texture(s).
- Touched: `GLTalismans` (item reg), `GLMisc` (menu reg), `GLLang`, zh_cn `items.json`.
- Verify: `./gradlew runData build` (runData regenerates item model + en_us/en_ud lang +
  `curios:charm` item tag); committed with generated output.

## Deferred / open questions

- Opening the menu while the pocket is merely *worn* (not in hand) needs a Curios/click
  hook — out of scope this pass. Right-click-in-hand is the primary path.
- Right-click-on-slot to take just reserve papers (copying `BagSlot.startSession/endSession`
  + `DrawerQuickInsert.tryItemClickBehaviourOverride`) — optional polish, not required.
- Locking the pocket's own hand slot via `shouldLock` while the menu is open (L2Backpack
  does this for bags) — add for robustness.