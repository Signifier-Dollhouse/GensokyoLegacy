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

### TalismanPaperItem (one per kind, stackable)
Each talisman kind is its own item. All kinds share one base class `TalismanPaperItem extends Item`.
- `stacksTo(64)`, stackable, **no effects when held**. Purely the paper form.
- Holds the talisman identity and logic: `getKind()`, `getDurability()`, and the behavior hooks `test()`, `trigger()`, `onAttacked()`, `onDamaged()`.
- `durability` (final int) = number of uses the talisman gets once folded. This is **data**, not vanilla durability.
- `appendHoverText()` gives the description.
- The existing 5 subclasses (`HealTalisman`, `SpeedTalisman`, `HydrophobicTalisman`, `LavaAffinityTalisman`, `ShelterTalisman`) become direct subclasses; the current `BasePaperTalisman` is renamed/reworked into `TalismanPaperItem`.

Notes on the current code in `content/item/talisman/`:
- `BasePaperTalisman extends Item` already has the `durability` field and tick/trigger/damage hooks — this is the seed for `TalismanPaperItem`.
- Constructors use `stacksTo`/`durability` inconsistently (`HealTalisman` uses `p.stacksTo(16)`, others use `p.durability(...)`); the fold-into-base constructor `super(p, durability)` is the right target.
- The 5 subclasses are currently unregistered and orphaned; they need `ItemEntry` registration in `GLItems`.

### FoldedPaperTalisman (single item, unstackable)
Exactly one registered item represents any kind. Kind + durability are data components.
- `stacksTo(1)`.
- Data components:
  - `DC_TALISMAN_PAPER`: `Holder<Item>` referencing the talisman paper item it represents.
  - `DC_TALISMAN_DURABILITY`: `Integer` remaining uses (counts down from paper's `durability` to 0).
- Behavior:
  - `static ItemStack fold(ItemStack paper)` — create a folded stack from a paper item.
  - `static TalismanPaperItem paper(ItemStack folded)` — resolve the represented kind, or null.
  - `inventoryTick()` — when held in hand (standalone use), delegates to `paper(stack).tickTalisman(stack, player)`.
  - `hurtTalisman(stack)` — decrement `DC_TALISMAN_DURABILITY`; at 0 the stack is removed.
  - `appendHoverText()` — kind name, effect description, remaining uses / total.
- Unstackable + data-component durability is exactly why the folded form exists: Minecraft forbids durability on stackable items. `hurtItem` in the base must be reworked to decrement the component instead of vanilla damage, since the folded item has no vanilla durability.

### TalismanPocket (single item, unstackable, curio)
The "charm" curio that carries up to 9 kinds.
- `stacksTo(1)`, tagged `curios:charm`.
- Data component `DC_TALISMAN_POCKET`: `TalismanPocketData` = `TalismanSlot[9]` (one per `TalismanKind`, index == enum ordinal).
- `TalismanSlot` record: `foldedStack` (active talisman), `paperStack` (reserve ammo of the same kind).
- Behavior:
  - Right-click opens a 9-slot management menu (follow existing `content/ui` menu patterns) that accepts the correct paper item into each slot. Non-matching paper is rejected.
  - **Folding**: when a slot has paper in `paperStack` but an empty `foldedStack`, fold one paper → `FoldedPaperTalisman` (see Phase 3) into the folded slot. When the folded talisman is used up, it disappears and the next paper in the reserve is folded on the following tick — the paper stack is a self-reloading magazine.
- The folded slot of each kind is what actually ticks.

## Cooldown model
Cooldowns are keyed on the **paper `Item`** (`player.getCooldowns().addCooldown(this, ...)` uses the paper instance) because `tickTalisman`/`trigger` run on the paper object. This is correct: the folded talisman delegates to the paper, so all folded copies of a kind share one cooldown. Keep `tickTalisman` using `this` (the paper item), not the folded item.

## Global tick / damage wiring
- No ticking in `inventoryTick` of the pocket (Curios may or may not call `inventoryTick` on charm-slot items; relying on it is fragile and risks double-tick). Instead a single event source ticks pocket contents:
  - New `TalismanEventHandlers.onPlayerTick(PlayerTickEvent.Post)`: find the pocket via `CuriosManager.getTalismanPocket(player)` (charm slot), fall back to mainhand/offhand; then `pocket.tickAll(stack, player)` iterates the 9 folded slots calling `TalismanPaperItem.tickTalisman`.
  - Standalone folded talisman in hand ticks via its own `inventoryTick`. These two paths are mutually exclusive (pocket vs hand), so no double-tick.
- Damage hooks:
  - `GLAttackListener.onAttack(cache)`: target is a player → find pocket (charm slot / hand), call each folded slot's `paper().onAttacked(foldedStack, sp, event)`; cancel if any returns true.
  - `GLAttackListener.onDamage(data)`: same iteration calling `onDamaged`.
  - Hand-held folded talisman: also check mainhand/offhand stacks.

## TalismanKind enum
Central registry of kinds. Holds durability + a lazily-resolved reference to its registered paper item; a static `Item → TalismanKind` map is populated at registration time.

```java
public enum TalismanKind {
    HEAL, SPEED, HYDROPHOBIC, LAVA_AFFINITY, SHELTER;
    // durable / meta stored here or in the paper item; map Item→kind filled by TalismanKind.register(kind, paper)
}
```

Kinds present in code: heal(16), speed(180), hydrophobic(180), lava_affinity(180), shelter(16). Missing: attack (texture `attack_talisman_paper.png` exists, no class yet). Textures exist for heal/life, speed, attack papers + `folded_paper_talisman` + `talisman_pocket`; hydrophobic/lava/shelter have no dedicated texture yet.

## Data components

| DCVal | Type | Registration | Purpose |
|---|---|---|---|
| `DC_TALISMAN_PAPER` | `Holder<Item>` | `DC.registry("talisman_paper", BuiltInRegistries.ITEM)` | Kind of a folded talisman |
| `DC_TALISMAN_DURABILITY` | `Integer` | `DC.intVal("talisman_durability")` | Remaining uses of a folded talisman |
| `DC_TALISMAN_POCKET` | `TalismanPocketData` | `DC.reg("talisman_pocket", TalismanPocketData.class, false)` | Pocket contents, 9 `TalismanSlot` |

`TalismanPocketData` / `TalismanSlot` are records serialized by l2serial `CodecAdaptor` (pattern: `BorderUmbrellaSlots`, `MiniFurnace1.Data`). Default-construct arrays filled with `TalismanSlot.EMPTY`.

## Curios integration
- Pocket tagged `curios:charm` via `ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm"))` (pattern: `curios:back` for wings in `GLItems`).
- `CuriosManager.getTalismanPocket(LivingEntity)` — guarded by `ModList.get().isLoaded("curios")`, uses `CuriosApi.getCuriosInventory(...).findFirstCurio(pocketItem)`, falls back to null when curios absent.

## Registration plan (`GLItems.java`)
1. Data components: `DC_TALISMAN_PAPER`, `DC_TALISMAN_DURABILITY`, `DC_TALISMAN_POCKET`.
2. Paper items: `HEAL_TALISMAN_PAPER`, `SPEED_TALISMAN_PAPER`, `HYDROPHOBIC_TALISMAN_PAPER`, `LAVA_AFFINITY_TALISMAN_PAPER`, `SHELTER_TALISMAN_PAPER` (names "heal"/"life" — pick one; texture is `life_talisman_paper.png`), each `stacksTo(64)`, model from `textures/item/talisman/`, `.tab(TAB.key())`.
3. `FOLDED_PAPER_TALISMAN` — `stacksTo(1)`.
4. `TALISMAN_POCKET` — `stacksTo(1)`, tag `curios:charm`, `.tab(TAB.key())`.
5. After each paper item registers, call `TalismanKind.register(kind, paperItem)` (or bind in one loop) to populate the Item→kind map.
6. Codec registration for any l2serial codec used by the pocket record, if the vanilla `CodecAdaptor` path needs a custom handler registered in the `GensokyoLegacy()` constructor.

---

# Implementation plan

Follow the modular build order; each phase compiles + datagen independently.

## Phase 1: Data components and kind registry
1. `TalismanKind` enum in `content/item/talisman/` — ordinals are pocket slot indices; static `Item → TalismanKind` map; `register(kind, paper)` + `byItem(Item)`.
2. `content/item/talisman/data/TalismanSlot.java` — record `(ItemStack foldedStack, ItemStack paperStack)`, `EMPTY` constant, nullable-normalizing constructor.
3. `content/item/talisman/data/TalismanPocketData.java` — record `(TalismanSlot[] slots)` normalized to length 9, `get/with/findEmptySlot`, `defaultSlots()`.
4. `GLItems.java`: register `DC_TALISMAN_DURABILITY`, `DC_TALISMAN_POCKET`, `DC_TALISMAN_PAPER` (the `DC.registry` variant).

## Phase 2: Talisman paper items
1. Rework `BasePaperTalisman` → `TalismanPaperItem extends Item`: keep `durability` field + `tickTalisman`/`test`/`trigger`/`onAttacked`/`onDamaged`/`applyEffect`; add `getKind()`; rework `hurtItem` to decrement `DC_TALISMAN_DURABILITY` and `shrink` at 0 (folded form), falling back to vanilla `setDamageValue` for any stack that has real damage (defensive).
2. Update the 5 subclasses: constructors `super(p, kind, durability)` (or `super(p, durability)` + kind resolved from class), drop hardcoded `stacksTo(16)`/`durability(180)` props. Fix `HealTalisman`'s `p.stacksTo(16)` → `p`, and remove `durability(...)` from the others.
3. Register the 5 paper items in `GLItems` with models pointing at existing textures, `.tab(TAB.key())`, link `TalismanKind`.

## Phase 3: Folded paper talisman
1. `FoldedPaperTalisman extends Item` in `content/item/talisman/`.
2. Statics: `fold(ItemStack paper)` sets `DC_TALISMAN_PAPER` + `DC_TALISMAN_DURABILITY = paper().getDurability()`; `paper(stack)` helper resolving to `TalismanPaperItem`.
3. `inventoryTick` (ServerPlayer only): `paper(stack).tickTalisman(stack, sp)` — standalone use.
4. `hurtTalisman(stack)`: decrement component, remove stack at 0.
5. `appendHoverText`: kind name, effect line (reuse paper's description), `uses/total`.
6. Register `FOLDED_PAPER_TALISMAN` in `GLItems`.

## Phase 4: Talisman pocket
1. `TalismanPocket extends Item` in `content/item/talisman/`.
2. `tickAll(ItemStack pocket, ServerPlayer sp)`: read `DC_TALISMAN_POCKET`, for each slot tick the folded talisman via its paper's `tickTalisman`, then run folding (below).
3. Folding: for each slot with `paperStack` nonempty and `foldedStack` empty → `foldedStack = FoldedPaperTalisman.fold(takeOne(paperStack))`; if a folded talisman was consumed this tick, it is replaced next tick. Empty paper stacks clear the slot.
4. Menu (right-click opens container-style menu over the charm slot — follow `content/ui` menu + `GLClickHandler` patterns): 9 slot views; inserting paper of kind *N* targets slot *N*; wrong kind rejected. Menu changes write back into `DC_TALISMAN_POCKET`.
5. Register `TALISMAN_POCKET` in `GLItems`, tag `curios:charm`, model + overlay.

## Phase 5: Event wiring
1. `CuriosManager.getTalismanPocket(LivingEntity)` guarded by curios load check (pattern: `hasWings`).
2. New `TalismanEventHandlers` (`@EventBusSubscriber`): `PlayerTickEvent.Post` → locate pocket (charm slot, else mainhand/offhand) → `tickAll`.
3. `GLAttackListener.onAttack` / `.onDamage`: if target is player, iterate pocket slots (and hand-held folded talisman) → call `onAttacked`/`onDamaged` on the paper item with the folded stack.
4. If Curios ends up ticking curios items itself, guard the tick so pocket contents only tick once (dedupe by identity/location or by tracking last ticked stack).

## Phase 6: Lang, models, textures
1. `GLLang` entries: item names via Registrate `.lang()`, plus description keys/`appendHoverText` strings (add a `Talisman` group to `GLLang` or reuse `ItemCommon`).
2. Item model JSONs for each paper (existing textures; `*_overlay.png` for durability/kind overlay), folded talisman, pocket.
3. Add zh_cn entries under `src/test/resources/gensokyolegacy/lang/zh_cn/talisman.json` and rerun `ResourceOrganizer`.
4. `./gradlew runData`, commit `src/generated`.

## Phase 7: Content — distribution
1. Add talisman paper as sellable items in the character trade system (`data/gensokyolegacy/gensokyolegacy/trade/...`, e.g. a "talisman" merchant trade group). Not craftable, no loot table.
2. Write full descriptions for each kind in `appendHoverText`.
3. Optional follow-ups (not required): attack talisman class (texture exists), pocket GUI polish, auto-fold animation/sound, TalismanKind-driven creative tab population.

## Open questions to resolve during implementation
- Naming: "life" vs "heal" (`HealTalisman` vs `life_talisman_paper.png`).
- `DC_TALISMAN_PAPER`: registered via `DC.registry(..., BuiltInRegistries.ITEM)` returns `DCVal<Holder<Item>>` — confirm the item holder survives save/load (it uses `holderByNameCodec`, should).
- Whether Curios calls `inventoryTick` on charm items — decide the single-tick source in Phase 5 accordingly.
