Create paper talisman system. Currently I created some item classes.

# System design
Talisman is made of paper, redstone, and some other ingredients. 
It's not craftable by player. The only way to obtain them is to buy them from characters.

There are 3 kinds of items here:
- talisman paper: stackable item that has no effects.
- Folded paper talisman: unstackable item that has effects. There should only be 1 folded paper talisman item, which holds data component of total durability, and the actual talisman paper item it represents.
- Talisman pocket: a kind of curio item that can be placed into charm curios slot. It can store 9 kinds of talisman. For each kind, it can hold 1 folded paper talisman and 1 stack of talisman paper of the same kind.

Each kind of talisman have certain effects. Talisman paper item class holds logic, description, and data supplier code.
When talisman paper is put into pocket, one item is folded into folded talisman paper and enter the folded slot.

The reason to have talisman paper and folded paper talisman is that minecraft does not allow stackable items to have durability.

---

# Architecture

## Item hierarchy

### TalismanCurioItem (abstract common curio base)
Shared base for the two equippable talisman items (`FoldedPaperTalisman`, `TalismanPocket`). Not registered itself.
- `extends Item`, `stacksTo(1)` — both subclasses are unstackable.
- Defines the curios slot both go into (`curios:charm`) as a shared tag/drop rule, and a helper `equippedCurios(LivingEntity)` (guarded by `ModList.get().isLoaded("curios")`, via `CuriosManager`) returning every equipped `TalismanCurioItem` stack in that slot.
- Abstract API the tick/damage wiring calls polymorphically:
  - `tickAll(ItemStack stack, ServerPlayer sp)` — iterate this item's active folded talismans and run their paper logic.
  - `getActiveTalismans(ItemStack stack)` → list of folded talisman stacks to pass to `onAttacked`/`onDamaged`.
- Both charm items occupy the same slot, so wearing a whole pocket vs a single folded talisman are alternatives (default Curios setup has one charm slot).

### TalismanPaperItem (one per kind, stackable)
Each talisman kind is its own item. All kinds share one base class `TalismanPaperItem extends Item`.
- `stacksTo(64)`, stackable, **no effects when held**. Purely the paper form.
- Holds the talisman identity and logic: `getDurability()` and the behavior hooks `test()`, `trigger()`, `onAttacked()`, `onDamaged()`. **There is no kind enum — the paper item itself IS the kind identity.** Everything else derives the kind from the item: the folded talisman stores the paper `Holder<Item>`; the pocket identifies each slot by the paper contents it carries.
- `durability` (final int) = number of uses the talisman gets once folded. This is **data**, not vanilla durability.
- `appendHoverText()` gives the description.
- The existing 5 subclasses (`HealTalisman`, `SpeedTalisman`, `HydrophobicTalisman`, `LavaAffinityTalisman`, `ShelterTalisman`) become direct subclasses; the current `BasePaperTalisman` is renamed/reworked into `TalismanPaperItem`.

Notes on the current code in `content/item/talisman/`:
- `BasePaperTalisman extends Item` already has the `durability` field and tick/trigger/damage hooks — this is the seed for `TalismanPaperItem`.
- **Constructor inconsistency must be fixed**: the working-tree change folds all subclasses into `super(p, durability)` — verify no stray `p.stacksTo(16)` (HealTalisman) or `p.durability(180)` (HydrophobicTalisman) props remain, so durability lives only in the base field.
- The 5 subclasses are currently unregistered and orphaned; they need `ItemEntry` registration in `GLItems`.

### FoldedPaperTalisman (single item, unstackable curio)
Exactly one registered item represents any kind. Kind + durability are data components.
- `extends TalismanCurioItem`, `stacksTo(1)`, tagged `curios:charm` — can be worn directly in the charm slot (or held in hand) for passive effects.
- Data components:
  - `DC_TALISMAN_PAPER`: `Holder<Item>` referencing the talisman paper item it represents.
  - `DC_TALISMAN_DURABILITY`: `Integer` remaining uses (counts down from paper's `durability` to 0).
- Behavior:
  - `static ItemStack fold(ItemStack paper)` — create a folded stack from a paper item.
  - `static TalismanPaperItem paper(ItemStack folded)` — resolve the represented kind, or null.
  - `tickAll(stack, sp)` — tick itself: `paper(stack).tickTalisman(stack, sp)`.
  - `getActiveTalismans(stack)` — singleton `[stack]`.
  - `hurtTalisman(stack)` — decrement `DC_TALISMAN_DURABILITY`; at 0 the stack is removed.
  - `appendHoverText()` — kind name, effect description, uses/total.
- Unstackable + data-component durability is exactly why the folded form exists: Minecraft forbids durability on stackable items. `hurtItem` in the paper base must be reworked to decrement the component instead of vanilla damage, since the folded item has no vanilla durability.

### TalismanPocket (single item, unstackable curio)
The "charm" curio that carries up to 9 talismans.
- `extends TalismanCurioItem`, `stacksTo(1)`, tagged `curios:charm`.
- Data component `DC_TALISMAN_POCKET`: `TalismanPocketData` = `TalismanSlot[9]`. Slots are **not indexed by kind** — a slot is bound to whatever paper it holds (at most one slot per kind, enforced on insert).
- `TalismanSlot` record: `foldedStack` (active talisman), `paperStack` (reserve ammo). The slot's kind is derived from its own contents: `paperStack.getItem()`, or the `DC_TALISMAN_PAPER` of `foldedStack`. Derive, don't store — avoids serializing an `Item` reference in the record.
- Behavior:
  - Right-click opens a 9-slot management menu (follow existing `content/ui` menu patterns) that accepts the correct paper item into each slot. Inserting a paper of a kind already present routes to that slot; a kind not yet present takes the first empty slot; non-matching paper is rejected.
  - **Folding**: when a slot has paper in `paperStack` but an empty `foldedStack`, fold one paper → `FoldedPaperTalisman` into the folded slot. When the folded talisman is used up, it disappears and the next paper in the reserve is folded on the following tick — the paper stack is a self-reloading magazine.
- `tickAll(stack, sp)` — iterate the 9 folded slots calling `TalismanPaperItem.tickTalisman`, then run folding. `getActiveTalismans(stack)` — the folded stacks of non-empty slots.

## Cooldown model
Cooldowns are keyed on the **paper `Item`** (`player.getCooldowns().addCooldown(this, ...)` uses the paper instance) because `tickTalisman`/`trigger` run on the paper object. This is correct: the folded talisman delegates to the paper, so all folded copies of a kind share one cooldown. Keep `tickTalisman` using `this` (the paper item), not the folded item.

## Global tick / damage wiring
Single polymorphic path through `TalismanCurioItem` — tick and damage code only know the base type.
- Tick — dedicated `TalismanEventHandlers.onPlayerTick(PlayerTickEvent.Post)`: collect equipped `TalismanCurioItem` stacks via `TalismanCurioItem.equippedCurios(player)` (charm slot) **plus** mainhand/offhand stacks, then call `curio.tickAll(stack, sp)`. A single event source avoids relying on (and double-ticking from) Curios `inventoryTick`.
- Damage hooks:
  - `GLAttackListener.onAttack(cache)`: for each equipped/hand `TalismanCurioItem`, call `paper().onAttacked(foldedStack, sp, event)` over `getActiveTalismans(stack)`; cancel if any returns true.
  - `GLAttackListener.onDamage(data)`: same iteration calling `onDamaged`.
  - Hand-held folded talisman is covered by the same hand-slot collection.

## Data components

| DCVal | Type | Registration | Purpose |
|---|---|---|---|
| `DC_TALISMAN_PAPER` | `Holder<Item>` | `DC.registry("talisman_paper", BuiltInRegistries.ITEM)` | Kind of a folded talisman |
| `DC_TALISMAN_DURABILITY` | `Integer` | `DC.intVal("talisman_durability")` | Remaining uses of a folded talisman |
| `DC_TALISMAN_POCKET` | `TalismanPocketData` | `DC.reg("talisman_pocket", TalismanPocketData.class, false)` | Pocket contents, up to 9 `TalismanSlot` |

`TalismanPocketData` / `TalismanSlot` are records serialized by l2serial `CodecAdaptor` (pattern: `BorderUmbrellaSlots`, `MiniFurnace1.Data`). Default-construct arrays filled with `TalismanSlot.EMPTY`.

## Curios integration
- Both `FOLDED_PAPER_TALISMAN` and `TALISMAN_POCKET` tagged `curios:charm` via `ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm"))` (pattern: `curios:back` for wings in `GLItems`).
- `CuriosManager` gains a generic scan for `TalismanCurioItem` in the charm slot (guarded by `ModList.get().isLoaded("curios")`, using `CuriosApi.getCuriosInventory(...)`); the concrete lookup lives on `TalismanCurioItem.equippedCurios`.

## Registration plan (`GLItems.java`)
1. Data components: `DC_TALISMAN_PAPER`, `DC_TALISMAN_DURABILITY`, `DC_TALISMAN_POCKET`.
2. Paper items: `HEAL_TALISMAN_PAPER`, `SPEED_TALISMAN_PAPER`, `HYDROPHOBIC_TALISMAN_PAPER`, `LAVA_AFFINITY_TALISMAN_PAPER`, `SHELTER_TALISMAN_PAPER` (names "heal"/"life" — pick one; texture is `life_talisman_paper.png`), each `stacksTo(64)`, model from `textures/item/talisman/`, `.tab(TAB.key())`.
3. `FOLDED_PAPER_TALISMAN` — `stacksTo(1)`, tag `curios:charm`.
4. `TALISMAN_POCKET` — `stacksTo(1)`, tag `curios:charm`, `.tab(TAB.key())`.
5. Codec registration for any l2serial codec used by the pocket record, if the vanilla `CodecAdaptor` path needs a custom handler registered in the `GensokyoLegacy()` constructor.

Kinds present in code: heal(16), speed(180), hydrophobic(180), lava_affinity(180), shelter(16). Missing: attack (texture `attack_talisman_paper.png` exists, no class yet). Textures exist for heal/life, speed, attack papers + `folded_paper_talisman` + `talisman_pocket`; hydrophobic/lava/shelter have no dedicated texture yet.

---

# Implementation plan

Follow the modular build order; each phase compiles + datagen independently.

## Phase 1: Data records and components
1. `content/item/talisman/data/TalismanSlot.java` — record `(ItemStack foldedStack, ItemStack paperStack)`, `EMPTY` constant, nullable-normalizing constructor.
2. `content/item/talisman/data/TalismanPocketData.java` — record `(TalismanSlot[] slots)` normalized to length 9, `get/with/findSlot(paperItem)/findEmptySlot/defaultSlots()`. `findSlot` matches on `paperStack.getItem()` or the folded stack's `DC_TALISMAN_PAPER`.
3. `GLItems.java`: register `DC_TALISMAN_DURABILITY`, `DC_TALISMAN_POCKET`, `DC_TALISMAN_PAPER` (the `DC.registry` variant).

## Phase 2: Talisman paper items
1. Rework `BasePaperTalisman` → `TalismanPaperItem extends Item`: keep `durability` field + `tickTalisman`/`test`/`trigger`/`onAttacked`/`onDamaged`/`applyEffect`; rework `hurtItem` to decrement `DC_TALISMAN_DURABILITY` and `shrink` at 0 (folded form), falling back to vanilla `setDamageValue` for any stack that has real damage (defensive).
2. Normalize the 5 subclass constructors to `super(p, durability)`; remove any leftover `stacksTo(16)` / `durability(180)` property calls so durability lives only in the base field (fix the noted inconsistency).
3. Register the 5 paper items in `GLItems` with models pointing at existing textures and `.tab(TAB.key())`.

## Phase 3: Common curio base
1. `TalismanCurioItem extends Item` in `content/item/talisman/`.
2. `stacksTo(1)`; shared `curios:charm` tag; `equippedCurios(LivingEntity)` static returning equipped subclass stacks via `CuriosManager` (charm slot, guarded).
3. Abstract `tickAll(ItemStack, ServerPlayer sp)` and `getActiveTalismans(ItemStack)` -> `List<ItemStack>` (for damage hooks).

## Phase 4: Folded paper talisman
1. `FoldedPaperTalisman extends TalismanCurioItem`.
2. Statics: `fold(ItemStack paper)` sets `DC_TALISMAN_PAPER` + `DC_TALISMAN_DURABILITY = paper().getDurability()`; `paper(stack)` helper resolving to `TalismanPaperItem`.
3. `tickAll` = tick itself via `paper(stack).tickTalisman(stack, sp)`; `getActiveTalismans` = `[stack]`.
4. `hurtTalisman(stack)`: decrement component, remove stack at 0.
5. `appendHoverText`: kind name, effect line (reuse paper's description), `uses/total`.
6. Register `FOLDED_PAPER_TALISMAN` in `GLItems`, tag `curios:charm`.

## Phase 5: Talisman pocket
1. `TalismanPocket extends TalismanCurioItem`.
2. `tickAll(ItemStack pocket, ServerPlayer sp)`: read `DC_TALISMAN_POCKET`, tick each folded slot via its paper's `tickTalisman`, then run folding (below).
3. Folding: for each slot with `paperStack` nonempty and `foldedStack` empty → `foldedStack = FoldedPaperTalisman.fold(takeOne(paperStack))`; a consumed folded talisman is replaced on the next tick. Empty paper stacks clear the slot.
4. Menu (right-click opens container-style menu over the charm slot — follow `content/ui` menu + `GLClickHandler` patterns): 9 slot views; inserting paper routes to its kind's slot (or first empty); wrong-kind / 10th-kind rejected. Menu changes write back into `DC_TALISMAN_POCKET`.
5. Register `TALISMAN_POCKET` in `GLItems`, tag `curios:charm`, model + overlay.

## Phase 6: Event wiring
1. `CuriosManager`: generic charm-slot scan for `TalismanCurioItem` (guarded by curios load check, pattern: `hasWings`).
2. New `TalismanEventHandlers` (`@EventBusSubscriber`): `PlayerTickEvent.Post` → `TalismanCurioItem.equippedCurios(player)` (charm slot) + mainhand/offhand → `curio.tickAll(stack, sp)`.
3. `GLAttackListener.onAttack` / `.onDamage`: if target is player, iterate equipped/hand `TalismanCurioItem` → for each folded stack in `getActiveTalismans` call the paper's `onAttacked`/`onDamaged`.
4. If Curios ends up ticking curios items itself, guard the tick so talismans only tick once (dedupe by stack identity/location or track last ticked stack).

## Phase 7: Lang, models, textures
1. `GLLang` entries: item names via Registrate `.lang()`, plus description keys/`appendHoverText` strings (add a `Talisman` group to `GLLang` or reuse `ItemCommon`).
2. Item model JSONs for each paper (existing textures; `*_overlay.png` for durability/kind overlay), folded talisman, pocket.
3. Add zh_cn entries under `src/test/resources/gensokyolegacy/lang/zh_cn/talisman.json` and rerun `ResourceOrganizer`.
4. `./gradlew runData`, commit `src/generated`.

## Phase 8: Content — distribution
1. Add talisman paper as sellable items in the character trade system (`data/gensokyolegacy/gensokyolegacy/trade/...`, e.g. a "talisman" merchant trade group). Not craftable, no loot table.
2. Write full descriptions for each kind in `appendHoverText`.
3. Optional follow-ups (not required): attack talisman class (texture exists), pocket GUI polish, auto-fold animation/sound.

## Open questions to resolve during implementation
- Naming: "life" vs "heal" (`HealTalisman` vs `life_talisman_paper.png`).
- `DC_TALISMAN_PAPER`: registered via `DC.registry(..., BuiltInRegistries.ITEM)` returns `DCVal<Holder<Item>>` — confirm the item holder survives save/load (it uses `holderByNameCodec`, should).
- Folded talisman and pocket share the one `curios:charm` slot — acceptable since wearing either is an either/or; confirm no separate slot is wanted.
- Whether Curios calls `inventoryTick` on charm items — decide the single-tick source in Phase 6 accordingly.
