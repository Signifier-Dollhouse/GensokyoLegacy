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
Shared base for the two equippable talisman items (`FoldedPaperTalisman`, `TalismanPocket`). Not registered itself. **Implemented (Phase 3).**
- `extends Item implements ICurioItem` (pattern: CurseOfPandora `EffectRefreshItem`), `stacksTo(1)` — both subclasses are unstackable and directly tag `curios:charm`.
- **Ticking source is Curios itself**: overrides `curioTick(SlotContext, ItemStack)`; when `slotContext.entity()` is a `ServerPlayer`, iterates `getActiveTalismans(stack)` and calls each paper's `tickTalisman`. No `PlayerTickEvent`/`inventoryTick` — there is exactly one tick path.
- Abstract API:
  - `getActiveTalismans(ItemStack stack)` → list of the paper stacks to tick (`FoldedPaperTalisman` → `[self]`, `TalismanPocket` → folded stacks of occupied slots).
- Both charm items occupy the same slot, so wearing a whole pocket vs a single folded talisman are alternatives (default Curios setup has one charm slot).

### TalismanPaperItem (one per kind, stackable)
Each talisman kind is its own item. All kinds share the abstract base `TalismanPaperItem extends Item`. **Implemented (Phase 2).**
- `stacksTo(64)`, stackable, **no effects** in hand (effects only run through a folded/equipped curio form — see tick wiring below).
- Holds the talisman identity and logic: `getDurability()`, **`getColor()` + `getTexture()` (per-kind abstract)** and the behavior hooks `test()`, `trigger()`, `onAttacked()`, `onDamaged()`. **There is no kind enum — the paper item itself IS the kind identity.** Everything else derives the kind from the item: the folded talisman stores the paper `Holder<Item>`; the pocket identifies each slot by the paper contents it carries.
- `durability` (final int) = number of uses the talisman gets once folded. This is **data**, not vanilla durability.
- `getColor()` tints the item-model overlay (layer1) via `ItemColors` in `GLClient`; `getTexture()` selects one of the 3 preset textures (`attack`/`life`/`speed`).
- The 5 subclasses (`HealTalisman`, `SpeedTalisman`, `HydrophobicTalisman`, `LavaAffinityTalisman`, `ShelterTalisman`) are direct subclasses; the old `BasePaperTalisman` is deleted, its body reworked into `TalismanPaperItem`.

### FoldedPaperTalisman (single item, unstackable curio) **Implemented (Phase 4).**
Exactly one registered item represents any kind. Kind + durability are data components.
- `extends TalismanCurioItem`, `stacksTo(1)`, tagged `curios:charm` — **only effective while worn in a curio slot; it does NOT work in hand** (no inventory/hand ticking).
- Data components:
  - `DC_TALISMAN_PAPER`: `Holder<Item>` referencing the talisman paper item it represents.
  - `DC_TALISMAN_DURABILITY`: `Integer` remaining uses (counts down from paper's `durability` to 0).
- Behavior:
  - `static ItemStack fold(ItemStack paper)` — create a folded stack from a paper item (sets `DC_TALISMAN_PAPER` via `BuiltInRegistries.ITEM.wrapAsHolder`, `DC_TALISMAN_DURABILITY = paper().getDurability()`).
  - `static TalismanPaperItem paper(ItemStack folded)` — resolve the represented kind, or null.
  - `getActiveTalismans(stack)` — singleton `[stack]`, ticked by `curioTick` via `paper(stack).tickTalisman(stack, sp)`.
  - There is no separate `hurtTalisman`: the active form is the folded stack itself, so the paper base `hurtItem` runs on it and decrements `DC_TALISMAN_DURABILITY`, shrinking the whole curio stack at 0.
  - `appendHoverText()` — kind name (GOLD, reused paper name), effect description (paper's `appendTalismanDesc`), `uses/total` (`Talisman.DURABILITY`). Blank stack (no kind) shows `Talisman.BLANK`.
  - Item tint: the layered model's `layer1` overlay is tinted dynamically by `paper(stack).getColor()` (registered in `GLClient`); falls back to white for a blank folded stack.
- Unstackable + data-component durability is exactly why the folded form exists: Minecraft forbids durability on stackable items. `hurtItem` in the paper base must be reworked to decrement the component instead of vanilla damage, since the folded item has no vanilla durability.

### TalismanPocket (single item, unstackable curio)
The "charm" curio that carries up to 9 talismans.
- `extends TalismanCurioItem`, `stacksTo(1)`, tagged `curios:charm`.
- Data component `DC_TALISMAN_POCKET`: `TalismanPocketData` = `TalismanSlot[9]`. Slots are **not indexed by kind** — a slot is bound to whatever paper it holds (at most one slot per kind, enforced on insert).
- `TalismanSlot` record: `foldedStack` (active talisman), `paperStack` (reserve ammo). The slot's kind is derived from its own contents: `paperStack.getItem()`, or the `DC_TALISMAN_PAPER` of `foldedStack`. Derive, don't store — avoids serializing an `Item` reference in the record.
- Behavior:
  - Right-click opens a 9-slot management menu (follow existing `content/ui` menu patterns) that accepts the correct paper item into each slot. Inserting a paper of a kind already present routes to that slot; a kind not yet present takes the first empty slot; non-matching paper is rejected.
  - **Folding**: when a slot has paper in `paperStack` but an empty `foldedStack`, fold one paper → `FoldedPaperTalisman` into the folded slot. When the folded talisman is used up, it disappears and the next paper in the reserve is folded on the following tick — the paper stack is a self-reloading magazine.
- `getActiveTalismans(stack)` — the folded stacks of non-empty slots, ticked by `curioTick`; after ticking, run folding.

## Cooldown model
Cooldowns are keyed on the **paper `Item`** (`player.getCooldowns().addCooldown(this, ...)` uses the paper instance) because `tickTalisman`/`trigger` run on the paper object. This is correct: the folded talisman delegates to the paper, so all folded copies of a kind share one cooldown. Keep `tickTalisman` using `this` (the paper item), not the folded item.

## Global tick / damage wiring
Single polymorphic path through `TalismanCurioItem` — tick and damage code only know the base type.
- Tick — **wired from Curios `ICurioItem.curioTick(SlotContext, ItemStack)`**, overridden once in `TalismanCurioItem` (Phase 3). It fires server-side per equipped curio tick; no `PlayerTickEvent.Post`, no `inventoryTick`, no hand-slot collection. **Folded talisman and pocket only work while equipped in a curio slot.**
- Damage hooks (implemented in Phase 6):
  - `GLAttackListener.onAttack(cache)`: for each equipped `TalismanCurioItem` (charm slot scan), call `paper().onAttacked(foldedStack, sp, event)` over `getActiveTalismans(stack)`; cancel if any returns true.
  - `GLAttackListener.onDamage(data)`: same iteration calling `onDamaged`.

## Data components

| DCVal | Type | Registration | Purpose |
|---|---|---|---|
| `DC_TALISMAN_PAPER` | `Holder<Item>` | `DC.registry("talisman_paper", BuiltInRegistries.ITEM)` | Kind of a folded talisman |
| `DC_TALISMAN_DURABILITY` | `Integer` | `DC.intVal("talisman_durability")` | Remaining uses of a folded talisman |
| `DC_TALISMAN_POCKET` | `TalismanPocketData` | `DC.reg("talisman_pocket", TalismanPocketData.class, false)` | Pocket contents, up to 9 `TalismanSlot` |

`TalismanPocketData` / `TalismanSlot` are records serialized by l2serial `CodecAdaptor` (pattern: `BorderUmbrellaSlots`, `MiniFurnace1.Data`). Default-construct arrays filled with `TalismanSlot.EMPTY`.

## Curios integration
- `TalismanCurioItem implements ICurioItem` directly (pattern: CurseOfPandora `EffectRefreshItem`), so equipped talismans are ticked by Curios' own per-tick call without any event or capability code. Curios is a hard dependency of these items.
- Both `FOLDED_PAPER_TALISMAN` and `TALISMAN_POCKET` tagged `curios:charm` via `ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm"))` (pattern: `curios:back` for wings in `GLItems`).
- Phase 6 damage hooks scan the charm slot via `CuriosManager` (guarded by `ModList.get().isLoaded("curios")`, `CuriosApi.getCuriosInventory(...)`), pattern: `hasWings`.

## Paper textures and colors (implemented)
- 3 preset textures exist under `textures/item/talisman/`: `attack`/`life`/`speed` papers + `*_overlay.png`. All 3 bases are pixel-identical brown paper and all overlays are grayscale ink, so the base/overlay pair only fixes shape.
- Each kind picks one preset (`Heal→life`, `Speed→speed`, `Hydrophobic→attack`, `LavaAffinity→attack`, `Shelter→life`) and a per-kind color tinting the model's layer1 (overlay) via `ItemColors` (registered in `GLClient` from `GLTalismans.TALISMAN_PAPERS`). The generated layered model uses `tintIndex == layer index`, so tinting only colors the ink.
- Colors: heal `0xFF5050`, speed `0x55FF7F`, hydrophobic `0x5555FF`, lava `0xFFB37F`, shelter `0xFFFFD5`. English names come from `.lang(...)` at registration; zh_cn entries added to `items.json` (rerun `organize.ResourceOrganizer` to merge).

## Registration plan (`GLTalismans.java`)
All talisman registrations live in a dedicated class `dev.xkmc.gensokyolegacy.content.item.talisman.GLTalismans` (new in Phase 1/2), not inline in `GLItems`. `GLTalismans.register()` is a no-op that forces static init; called from `GLItems.register()` right before `GLFluids.register()` (and so just before `HexBrew.register()` inside it), matching the user requirement. Components/items are registered via `GensokyoLegacy.REGISTRATE` (shared with GLItems, so they land in the same datagen/tab) and components via a private `DCReg.of(GensokyoLegacy.REG)`.
1. Data components: `DC_TALISMAN_DURABILITY`, `DC_TALISMAN_PAPER`, `DC_TALISMAN_POCKET`.
2. Paper items: `HEAL_TALISMAN`, `SPEED_TALISMAN`, `HYDROPHOBIC_TALISMAN`, `LAVA_TALISMAN`, `SHELTER_TALISMAN` (registry ids `*_talisman`), stackable (default 64), layered model = parse spec above, `.tab(TAB.key())`.
3. `FOLDED_PAPER_TALISMAN` — `stacksTo(1)`, tag `curios:charm`, layered model, dynamic tint (Phase 4 ✅).
4. `TALISMAN_POCKET` — `stacksTo(1)`, tag `curios:charm`, `.tab(TAB.key())` (Phase 5 — pending).
5. Codec registration for any l2serial codec used by the pocket record, if the vanilla `CodecAdaptor` path needs a custom handler registered in the `GensokyoLegacy()` constructor.

Kinds present in code: heal(16), speed(180), hydrophobic(180), lava_affinity(180), shelter(16). Missing: attack (texture `attack_talisman_paper.png` exists, no class yet).

---

# Implementation plan

Follow the modular build order; each phase compiles + datagen independently.

## Phase 1: Data records and components ✅
1. `content/item/talisman/data/TalismanSlot.java` — record `(ItemStack foldedStack, ItemStack paperStack)`, `EMPTY` constant, nullable-normalizing constructor. `kindItem()` derives the kind from `paperStack.getItem()` or the folded stack's `DC_TALISMAN_PAPER`.
2. `content/item/talisman/data/TalismanPocketData.java` — record `(TalismanSlot[] slots)` normalized to length 9, `get/with/findSlot(paperItem)/findEmptySlot/defaultSlots()`.
3. `GLTalismans`: `DC_TALISMAN_DURABILITY` (`intVal`), `DC_TALISMAN_POCKET` (`reg`), `DC_TALISMAN_PAPER` (`DC.registry("talisman_paper", BuiltInRegistries.ITEM)` → `DCVal<Holder<Item>>`, holderBy name codec).

## Phase 2: Talisman paper items ✅
1. `BasePaperTalisman` deleted and reworked into abstract `TalismanPaperItem extends Item`: `durability` field + `tickTalisman`/`test`/`trigger`/`onAttacked`/`onDamaged`/`applyEffect`; `hurtItem` decrements `DC_TALISMAN_DURABILITY` and `shrink`s at 0, falling back to vanilla `setDamageValue` if a stack has real damage. Constructor normalized to `(Properties, int durability)`.
2. All 5 subclass constructors `super(p, durability)` (heal 16, speed 180, hydrophobic 180, lava 180, shelter 16), each overriding `getColor()` + `getTexture()`.
3. Registered the 5 paper items in `GLTalismans` with layered preset+overlay models, `.tab(TAB.key())`, `.lang("...")`, `stacksTo(1)` (paper is the stackable raw form, but these registrations are fire-once prototypes for now). Tint wired via `ItemColors` in `GLClient`.

## Phase 3: Common curio base ✅
1. `TalismanCurioItem extends Item implements ICurioItem` in `content/item/talisman/`.
2. `stacksTo(1)`; abstract `getActiveTalismans(ItemStack)` -> `List<ItemStack>`; concrete `curioTick(SlotContext, ItemStack)` ticks each active paper on a `ServerPlayer` (wired from Curios, not from an event).
3. Shared `curios:charm` tag applied per subclass at registration (Phase 4/5).

## Phase 4: Folded paper talisman ✅
1. `FoldedPaperTalisman extends TalismanCurioItem`.
2. Statics: `fold(ItemStack paper)` sets `DC_TALISMAN_PAPER` + `DC_TALISMAN_DURABILITY = paper().getDurability()`; `paper(stack)` helper resolving to `TalismanPaperItem`.
3. `getActiveTalismans` = `[stack]` (self), ticked via `curioTick` → `paper(stack).tickTalisman(stack, sp)`.
4. Damage/uses: no separate method — the paper base `hurtItem` runs on the folded stack itself (decrement `DC_TALISMAN_DURABILITY`, shrink at 0).
5. `appendHoverText`: kind name, effect line (paper's `appendTalismanDesc`), `uses/total`.
6. Register `FOLDED_PAPER_TALISMAN` in `GLTalismans`, `stacksTo(1)`, tag `curios:charm`; layered model + dynamic tint in `GLClient`.

## Phase 5: Talisman pocket (deferred — skipped this pass)
1. `TalismanPocket extends TalismanCurioItem`.
2. `getActiveTalismans(pocket)` reads `DC_TALISMAN_POCKET` and returns the folded stacks of occupied slots; `curioTick` ticks them, then runs folding.
3. Folding: for each slot with `paperStack` nonempty and `foldedStack` empty → `foldedStack = FoldedPaperTalisman.fold(takeOne(paperStack))`; a consumed folded talisman is replaced on the next tick. Empty paper stacks clear the slot.
4. Menu (right-click opens container-style menu over the charm slot — follow `content/ui` menu + `GLClickHandler` patterns): 9 slot views; inserting paper routes to its kind's slot (or first empty); wrong-kind / 10th-kind rejected. Menu changes write back into `DC_TALISMAN_POCKET`.
5. Register `TALISMAN_POCKET` in `GLTalismans`, `stacksTo(1)`, tag `curios:charm`, model + overlay.

## Phase 6: Damage wiring ✅
1. `CuriosManager.getEquippedTalismans(LivingEntity)`: guarded charm-slot scan (pattern: `hasWings`) returning the flat list of active talisman stacks over every `TalismanCurioItem` in the `charm` handler (`getStacksHandler("charm")` → `getStacks()`).
2. `GLAttackListener.onAttack` / `.onDamage`: if target is a `ServerPlayer`, iterate `CuriosManager.getEquippedTalismans` and call the paper's `onAttacked` (cancel when any returns true; per-paper cooldown gate) / `onDamaged` on each stack.
3. Tick needs no event wiring — Curios already drives it via `curioTick`.

## Phase 7: Lang, models, textures ✅
1. ✅ Paper item names via Registrate `.lang()` in `GLTalismans` (English); zh_cn entries added to `src/test/resources/gensokyolegacy/lang/zh_cn/items.json` (rerun `organize.ResourceOrganizer` to merge into `zh_cn.json`).
2. ✅ Paper item model JSONs: layered `item/generated` `layer0`=preset base, `layer1`=`*_overlay.png`; item tint colors layer1 per kind. Folded talisman model + dynamic tint generated in `GLClient`. Talisman pocket model pending (Phase 5).
3. ✅ Description strings: `talisman.*` entries in `GLLang.Talisman` (en-us/en_ud generated), paper kinds supply `appendTalismanDesc`, folded tooltip shows kind + desc + uses. All hooked into `appendHoverText`.
4. ✅ `./gradlew runData`, commit `src/generated` (item models + lang + `curios:charm` item tag generated).

## Phase 8: Content — distribution
1. Add talisman paper as sellable items in the character trade system (`data/gensokyolegacy/gensokyolegacy/trade/...`, e.g. a "talisman" merchant trade group). Not craftable, no loot table.
2. Write full descriptions for each kind in `appendHoverText`.
3. Optional follow-ups (not required): attack talisman class (texture exists), pocket GUI polish, auto-fold animation/sound.

## Open questions to resolve during implementation
- `DC_TALISMAN_PAPER`: registered via `DC.registry(..., BuiltInRegistries.ITEM)` returns `DCVal<Holder<Item>>` — uses `holderByNameCodec` + `ByteBufCodecs.holderRegistry`, so the holder persists through save/load. Verified compiling.
- Folded talisman and pocket share the one `curios:charm` slot — acceptable since wearing either is an either/or; no separate slot wanted.
- ~Tick source~ → resolved: Curios `curioTick` sets the single tick path; folded talisman does not function in hand. Damage hooks (Phase 6) remain event-based via `GLAttackListener`, scanning only the equipped charm-slot stack.
- Naming resolved: registry ids are `heal_talisman`/`speed_talisman`/`hydrophobic_talisman`/`lava_talisman`/`shelter_talisman`; `Heal` kind uses the `life` texture preset; English display names set at registration.
