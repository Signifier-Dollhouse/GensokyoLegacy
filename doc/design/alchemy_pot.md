# Alchemy Pot — Design Doc

> **Status:** design only; no code yet — updated 2026-08-31 with owner answers
> **Block id:** `gensokyolegacy:alchemy_pot` (`GLBlocks.ALCHEMY_POT`, `DelegateBlock.newBaseBlock`) — model at `assets/.../custom/utensil/alchemy_pot.json`, blockstate generated in `GLBlocks.java:176`
> **Package suggestion:** `content/block/alchemy/` (`AlchemyPotBlock`, `AlchemyPotBlockEntity`, `AlchemyPotRenderer`) + `content/recipe/alchemy/`

---

## 1. Overview

The alchemy pot is a single-block in-world vessel (no menu):

- holds **fluid** with partial amounts allowed, capacity = 1 bucket (1000 mB), exposed via `IFluidHandler` capability;
- holds **up to 12 item stacks**, each slot count-capped to 1 — cap is `AlchemyPotBlockEntity.MAX_SLOTS = 12` (static final, single source of truth);
- **must contain a full bucket (1000 mB) before any item can be added** (both hand interaction and capability-insertion blocked until fluid is full). Item-only states exist only as a reaction *result*; empty-fluid + items is not a valid input state;
- contents react over time (`TimedRecipe`-like, no heat requirement, auto-start) to a **result** that can be *fluid-only*, *fluid+item*, or *item-only* — both fluid and items remain inside the pot after the reaction in the same slots/tank (no separate output slots);
- during the reaction (`inProgress>0`) *no fluid/item may be added or removed via interaction or capability*, but **block breaking and shift-right-click clearing are still allowed**; bubble particles + vanilla sound play;
- items are filtered on insertion to **only those that could still match at least one recipe** (YH `tryAddItem` logic) and a look-at overlay hints the next valid ingredients. Potion NBT (components patch) is significant.

It is the Gensokyo Legacy analogue of YoukaisFeasts' cooking pots, with fluid as first-class state and color-only intermediates (water texture).

---

## 2. Reference: YoukaisFeasts Pot / Soup Base — Understanding

Studied in sibling project `YoukaisFeasts-1.21` (`content/pot/cooking/*`):

### 2.1 Stack layers

```
TimedRecipeBlockEntity<T,C>          // src: youkaishomecoming/content/pot/base/TimedRecipeBlockEntity.java
  ├─ server: searches recipe via getRecipeFor(getRecipeType(), createContainer())
  ├─ fields: totalTime, recipeProgress, recipeId, doRecipeSearch (dirty flag), RecipeHolder<T> recipe (transient)
  ├─ tick(): server advances recipeProgress→finishRecipe; client mirrors progress for animation, rewinds if shouldStopProcessing()
  └─ abstract: getRecipeType(), isEmpty(), shouldStopProcessing(), createContainer(), finishRecipe()

CookingBlockEntity extends TimedRecipeBlockEntity<PotCookingRecipe<?>, CookingInv>
  ├─ items: CookingItemContainer(this, 12).setMax(1)   // @SerialField, 12 slots, stack limit 1
  ├─ soup: SoupHolder                                    // transient visual state, rechecked on load/client
  ├─ tryAddItem(stack, simulate): builds hypothetical list = existing non-empty + stack(1), constructs CookingInv(containerType, list, isComplete=false),
  │     queries RecipeManager.getRecipeFor(COOKING_RT). If any match, allow; else reject. On !simulate, items.addItem(stack.copyWithCount(1)).
  ├─ createContainer(isComplete=false/true): snapshot of non-empty item stacks + container Item (pot variant)
  ├─ shouldStopProcessing = !isHeated(level,pos) — requires TagRef.HEAT_SOURCES / HEAT_CONDUCTORS beneath pot (+ LIT check)
  ├─ finishRecipe: assemble(), if result is BlockItem → place block inheriting HORIZONTAL_FACING; else popResource above. Trigger criterion, clear.
  └─ overlay / tooltip: getHints() enumerates all matched recipes' remaining Ingredients when not cooking; lines() shows progress.

CookingItemContainer extends BaseContainer        // canAddItem / canPlaceItem delegate to be.tryAddItem
CookingInv = record(container:Item, list:List<ItemStack>, isComplete:boolean) implements RecipeInput
PotCookingRecipe<T> extends BaseRecipe + TimedRecipe  // RecType via YHBlocks.*, result ItemStack + time; subclass provides getInput() + getHints()
  └─ UnorderedCookingRecipe: input = List<Ingredient>; matches if every inv stack matches a distinct input entry and (isComplete? remain empty : else ok)

SoupHolder — "intermediate liquid" pattern to borrow
  ├─ Field: Level recipe cache List<RecipeHolder<SoupBaseRecipe<?>>>
  ├─ recheckSoup(be, level): queries SOUP_RT with be.createContainer()
  ├─ tickSoup(be, time): tracks nextTime (earliest soup time > current), picks current/next SoupData by best match:
  │     findSoup(time) = among recipes with time <= now, pick max getIngredientCount(); tiebreak smallest id lexicographically
  ├─ current/next: SoupData(ResourceLocation id, int color, int time) where color is ARGB; DEF = (water, -1, 0)
  ├─ floatingItems: for each item stack, if it would be consumed by next soup but not current, mark with life=next.time else -1; renderer uses life to animate sinking/fading.
  ├─ SoupBaseRecipe<T> extends BaseRecipe<CookingInv>: id, color, time, getIngredientCount(), removeConsumed(List<ItemStack>)
  │     └─ SimpleSoupBaseRecipe: input List<Ingredient>; matches subsets; removeConsumed clears matched slots
  └─ Rendering: CookingRenderUtil interpolates color/alpha between current and next SoupData by getProgress(pTick) with 0.5 crossfade.
       Soup id drives texture lookup (assets/youkaisfeasts/textures/block/bowl/soup/<id>.png); color tints fluid box.
```

### 2.2 Overlay

- `content/pot/overlay/HintOverlay` implements `LayeredDraw.Layer`, registered in `YHClient`.
- Per-frame: ray-trace `BlockHitResult`, resolve `IHintableBlock` via `BlockEntity instanceof` / `DelegateBlockImpl.one(IHintableBlock.class)` / `Block instanceof`.
- If the block hasn't changed for ≥ 15 ticks and crosshair stays on it, calls `be.getHints(level,pos)` (empty while cooking). Deduplicates `Ingredient`s by hash of `getItems()` stacks (item id + components patch: `hash = BuiltInRegistries.ITEM.getId(item) + componentsPatch.hashCode()*15; result = 31*result + hash`). For up to 12 distinct hints, shows a 4-wide grid tooltip (`TileClientTooltip`) cycling `arr[time/15 % arr.length]` per slot. Header `YHLangData.CUISINE_ALLOW`, tail "+N extra".
- `YH` lang splitting tooltip uses `OverlayUtil.renderTooltipInternal` (custom positioned box at 0.7w×0.5h).

### 2.3 Key lessons to reuse

1. **Constant for slots**: YH hardcodes `12` in field initializer but all loops respect it; alchemy must extract to `public static final int MAX_SLOTS = 12` and reference everywhere.
2. **Insertion gating**: `canAddItem`/`canPlaceItem` and hand interaction both call the same `tryAddItem(...,simulate=true)` predicate derived from the recipe manager — automation (hoppers/pipes) is automatically filtered.
3. **No-registration intermediate**: soup base never registers a `Fluid`; it resolves to `(id,color)` and client renders it via `FluidRenderer.renderFluidBox(tex,color)`. The items that produced the base stay in slots until the *final* cook recipe consumes them. `removeConsumed` is only for visual exclusion.
4. **Timed vs immediate**: `SoupBaseRecipe.time` drives staged visual changes *during* a cook; the cook recipe's `time` drives completion.
5. **NBT-sensitive dedup**: hint hashing includes `componentsPatch`, so potion variants are distinguished — required for alchemy too.

---

## 3. Alchemy Pot — Requirements (Resolved)

Owner answers applied (2026-08-31):

- **Fluid**: partial amounts allowed, tank capacity 1000 mB, fluid capability always exposed. No multi-fluid mixing; `fill`/`drain` amount is respected (not just bucket-sized).
- **Fluid-first rule**: `tryAddItem` and `canPlaceItem` reject if `fluid.getAmount() < 1000` (`FluidStack.EMPTY` or partial). Item-only state exists only as a *result* of a reaction (fluid consumed, item remains). The pot never accepts items until a full bucket is present again.
- **Intermediates — color only, water texture**: no custom texture id. All fluid rendering uses vanilla water still texture (`IClientFluidTypeExtensions.getStillTexture`) tinted by ARGB color. Stage records carry `color` only.
- **Keep both inside**: after a reaction, the resulting fluid and/or items remain inside the same tank/slots (no separate output; `Block.popResource` is *not* used). The pot's next state is simply the result.
- **Container return**: items with `hasCraftingRemainingItem()` return their container immediately on insertion (e.g., potion bottle → glass bottle returned to player hand/inventory). Assume such items always contribute to an intermediate, so the visual gap is not awkward.
 - **Clearing equivalence (voids fluid)**: `onRemove` (block broken) and shift-right-click "clear" return the *same* contents: only items **not** consumed by current intermediate are dropped; consumed items are dissolved. **Fluid is voided on both** clear and break (player must bucket-extract before break/clear if they want to keep it).
- **Heat**: none. `shouldStopProcessing()` always returns `false`; reaction auto-starts when any `AlchemyRecipe` matches and ticks every tick.
- **JEI / UI**: no container menu; JEI category mirrors YH `PotCookingRecipeCategory`.
- **Particles/sound**: vanilla only (`BUBBLE`, `BUBBLE_POP`, `BUBBLE_COLUMN_UPWARDS_*` sounds).
- **Dynamic color**: prepare for a special `AlchemyStageRecipe` subclass whose color is computed from inputs (e.g., potion color blend), not a static field.
 - **Potions as inputs**: use `dev.xkmc.l2core.init.reg.ingredient.PotionIngredient` (L2Core) for NBT-sensitive tests; `IHintableBlock` dedup hashes `componentsPatch` (same as YH). `PotionIngredient` handles `DataComponents.POTION_CONTENTS` matching.
 - **Partial result fluid + client-only drain animation**: recipes may output partial amounts (e.g., 500 mB). Server fluid stays at full until `finishRecipe` then snaps to `resultFluid.amount`; client lerps visual height `lerp(inProgress(), FLUID_CAPACITY, resultAmount)` for gradual decrease.
 - **One-by-one insertion**: even a 64-stack inserts only 1 item per interaction/hopper tick (via `copyWithCount(1)` + `shrink(1)` gating; hopper `canPlaceItem` called per slot).
 - **Dynamic color**: special `AlchemyStageRecipe` serializer to be added later; base keeps `getColor(inv,registries)` hook.

---

## 4. Block & BlockEntity

### 4.1 Block

```java
// GLBlocks.java — replace placeholder
ALCHEMY_POT = REGISTRATE.block("alchemy_pot", p -> DelegateBlock.newBaseBlock(p,
        BlockTemplates.HORIZONTAL, new AlchemyPotBlock(), AlchemyPotBlock.BE))
    .initialProperties(() -> Blocks.COPPER_BLOCK)
    .properties(BlockBehaviour.Properties::noOcclusion)
    .blockstate((ctx,pvd)-> pvd.simpleBlock(ctx.get(), ...custom/utensil/alchemy_pot...renderType cutout))
    .simpleItem().register();
ALCHEMY_POT_BE = REGISTRATE.blockEntity("alchemy_pot", AlchemyPotBlockEntity::new)
    .validBlock(ALCHEMY_POT).renderer(()->AlchemyPotRenderer::new).register();
```

- `AlchemyPotBlock` implements `IHintableBlock` (delegates to BE) and forwards `useItemOn` / `useWithoutItem` to BE.
- Model `alchemy_pot.json` stays; fluid rendered in BER as a box inside cavity.

### 4.2 BlockEntity

```java
@SerialClass
public class AlchemyPotBlockEntity extends TimedRecipeBlockEntity<AlchemyRecipe<?>, AlchemyInv>
        implements BlockContainer, IHintableBlock, InfoTile, TickableBlockEntity {

    public static final int MAX_SLOTS = 12;
    public static final int FLUID_CAPACITY = 1000;

    @SerialField public final AlchemyItemContainer items = new AlchemyItemContainer(this, MAX_SLOTS).setMax(1);
    @SerialField public FluidStack fluid = FluidStack.EMPTY; // 0..1000, any fluid, not just bucket-aligned
    public final AlchemyStageHolder stage = new AlchemyStageHolder(); // transient

    private boolean recheckStage = true;
    // IFluidHandler + IItemHandler exposed via RegisterCapabilitiesEvent
}
```

- Persist `fluid` with `FluidStack.CODEC` via `@SerialField` (verify l2serial supports it; else manual `saveAdditional/loadAdditional`). On any mutation: `setChanged(); sync(); doRecipeSearch = true; recheckStage = true;`.
- Capabilities (registered in `GensokyoLegacy.registerCapabilities`):
  - `ItemHandler` via `InvWrapper(items)` — `canAddItem`/`canPlaceItem` gates through `tryAddItem` and the fluid-full check.
  - `FluidHandler` (`IFluidHandler`) capacity 1000. Behavior:
    - `fill(resource, action)`: if `inProgress()>0` → 0. Else if `fluid.isEmpty()` or `fluid.getFluid()==resource.getFluid()`, accept up to `min(resource.amount, FLUID_CAPACITY - fluid.amount)`. Empty tank accepts any fluid; non-empty only same fluid (no mixing). Return filled amount. On `EXECUTE`, update `fluid`, notify.
    - `drain(resource, action)` / `drain(maxDrain, action)`: if `inProgress()>0` → `EMPTY` (forbid extraction during reaction). Else drain from `fluid`. Note: design now says *forbid fluid extraction during reaction* via capability; interaction path also forbid. Breaking/clearing still allowed as separate code path (not via capability drain).
- Tick:
  - server: `super.tick()` handles recipe search → `totalTime/recipeProgress` → `finishRecipe`. After progress advances or recipe search, call `stage.tickStage(this, recipeProgress)` if needed.
  - client: `super.tick()` mirrors `recipeProgress` (even though `shouldStopProcessing()==false`, client still increments to stay in sync); also `stage.tickStage`; spawn bubbles if `inProgress()>0`.
  - `shouldStopProcessing(Level)` → `false` always (no heat).

### 4.3 Collision / Removal

- Simple cube; no waterlogging.
- `onRemove` / `getDrops` / `onDestroyed`: call `dumpInventory()`-like helper that drops only `stage.floatingItems` items with `life<0` or `stack.isEmpty()==false && amount==1` — i.e., items **not** consumed by current stage. Fluid is not dropped as bucket item; it is voided (consistent with "clearing" equivalence). If fluid is full and no stage consumes it, should fluid be bucket-dropped? See §9 — default: void fluid on break (player must bucket-extract before break if they want to keep it).

---

## 5. Recipe System

### 5.1 Types to register in `GLRecipes`

| RecipeType | Serializer RecType | Purpose |
|---|---|---|
| `ALCHEMY_RT` | `RecType<AlchemyRecipe<?>, AlchemyRecipe<?>, AlchemyInv>` | main timed alchemy reaction (auto-start, timed) |
| `ALCHEMY_STAGE_RT` | `RecType<AlchemyStageRecipe<?>, AlchemyStageRecipe<?>, AlchemyInv>` | visual intermediate: color-only, water texture |

Both serializers registered via `SR.of(Registries.RECIPE_SERIALIZER)` + `SR.of(Registries.RECIPE_TYPE)` pattern (`GLRecipes.java:12`).

### 5.2 AlchemyInv

```java
public record AlchemyInv(FluidStack fluid, List<ItemStack> list, boolean isComplete) implements RecipeInput {
    public int size() { return list.size(); }
    public ItemStack getItem(int i) { return list.get(i); }
    public boolean isEmpty() { ... }
}
```

- Created as `new AlchemyInv(fluid.copy(), snapshotOfNonEmptyItems, isComplete)`. Snapshot copies each `ItemStack` with count 1.
- `isComplete` follows YH: `false` for insertion gating (`tryAddItem` builds hypothetical `isComplete=false` list — allows prefixes), `true` for final match (`createContainer()` for `getRecipeFor` search).

### 5.3 AlchemyRecipe (timed)

```java
@SerialClass
public abstract class AlchemyRecipe<T extends AlchemyRecipe<T>>
        extends BaseRecipe<T, AlchemyRecipe<?>, AlchemyInv> implements TimedRecipe {

    @SerialField public FluidIngredient inputFluid = FluidIngredient.empty(); // supports tag/fluid; test with FluidStack
    @SerialField public int time;
    @SerialField public ItemStack resultItem = ItemStack.EMPTY;   // may be EMPTY
    @SerialField public FluidStack resultFluid = FluidStack.EMPTY; // may be EMPTY; amount 0..FLUID_CAPACITY
    // plus input ingredients list in subclass
    public abstract List<Ingredient> getInputItems();
    public abstract List<Ingredient> getHints(Level level, AlchemyInv inv);
    public int getProcessTime() { return time; }
}
```

- Concrete `UnorderedAlchemyRecipe`: `List<Ingredient> input`; `matches` algorithm:
  1. `inputFluid.test(inv.fluid)` — must pass. Since fluid-first rule forbids items unless fluid is full, but recipe matching still checks amount/fluid type exactly. Use `FluidIngredient.test(FluidStack)` (amount-aware if needed). For potion-aware fluids, `FluidStack` components are not used.
  2. Multiset ingredient containment (unordered): copy `input` to `remain`; for each `inv` stack, find matching `Ingredient.test(stack)` (which respects `componentsPatch` for potions); remove on match; if any stack fails to match, return false.
  3. If `inv.isComplete()`, require `remain.isEmpty()`; else allow `size <= input.size()` and remainder non-empty is ok (prefix).
  4. Also enforce `inv.size() == input.size()` only when complete? YH checks `remain.isEmpty()` for complete, else size check not needed beyond `size <= input.size()`.
- JSON shape:

```json
{
  "type": "gensokyolegacy:unordered_alchemy",
  "input_fluid": { "fluid": "minecraft:water" },
  "input": [ { "item": "minecraft:nether_wart" }, { "item": "minecraft:glowstone_dust" } ],
  "time": 200,
  "result_fluid": { "fluid": "minecraft:water", "amount": 1000 },
  "result_item": { "id": "gensokyolegacy:condensed_essence", "count": 2 }
}
```

`input_fluid` via `FluidIngredient.CODEC` (already registered in `GensokyoLegacy.java:109`). At least one of `result_fluid`/`result_item` must be non-empty (validated in builder/codec).

### 5.4 Intermediate / Stage Recipe — color only, extensible

```java
@SerialClass
public abstract class AlchemyStageRecipe<T extends AlchemyStageRecipe<T>>
        extends BaseRecipe<T, AlchemyStageRecipe<?>, AlchemyInv> {

    @SerialField public int color = -1; // ARGB, -1 = derive from fluid tint (e.g., potion color fallback)
    @SerialField public int time = 0;   // when this stage becomes active during cooking (for handles that want staging)

    public abstract int getIngredientCount();
    public abstract void removeConsumed(List<ItemStack> list);

    /** Overridable for dynamic color. Default returns field `color`. */
    public int getColor(AlchemyInv inv, HolderLookup.Provider registries) { return color; }
}
```

- `SimpleAlchemyStageRecipe`: unordered `List<Ingredient>` + optional `FluidIngredient` filter. `matches` uses same subset test but ignores `time`. `removeConsumed` clears matched slots (set to `EMPTY`).
- **Dynamic-color subclass** (future): e.g., `PotionBlendStageRecipe extends AlchemyStageRecipe` overrides `getColor(inv, registries)` to inspect `inv.list()` potion `DataComponents.POTION_CONTENTS`, blend `int` colors via average/lerp, and return ARGB. Registration adds a new `RecType` (`ALCHEMY_STAGE_DYNAMIC`) with same `AlchemyInv` input. The holder calls `recipe.value().getColor(inv, level.registryAccess())` so static vs dynamic is transparent.
- JSON for static case:

```json
{
  "type": "gensokyolegacy:simple_alchemy_stage",
  "input_fluid": { "fluid": "minecraft:water" },
  "input": [ { "item": "minecraft:nether_wart" } ],
  "color": 16711680,
  "time": 0
}
```

For dynamic, omit `color` and set `"type": "gensokyolegacy:dynamic_alchemy_stage"` — loader will instantiate subclass.

Holder:

```java
public class AlchemyStageHolder {
    List<RecipeHolder<AlchemyStageRecipe<?>>> recipes = List.of();
    public StageData current = StageData.DEF, next = StageData.DEF;
    public List<ItemEntry> floating = List.of();

    public void recheck(AlchemyPotBlockEntity be, Level level) {
        var cont = be.createContainer(); // fluid + items snapshot
        recipes = level.getRecipeManager().getRecipesFor(ALCHEMY_STAGE_RT.get(), cont, level);
        // also query dynamic type and merge? Or single type with polymorphic serializers covers it.
        // Reset current/next/floating; nextTime tracking like SoupHolder
    }
    public void tickStage(AlchemyPotBlockEntity be, int time) {
        // find current/next by max ingredientCount ≤ time (or immediate if time irrelevant — pick max count)
        // then compute floating: copy current items, apply current.removeConsumed / next.removeConsumed to derive which stacks are still floating
        // resolve colors via recipe.getColor(inv, registries) when building StageData
    }
    public record StageData(int color, int time) { static DEF = new StageData(-1,0); }
    public record ItemEntry(ItemStack stack, int life) { float getAmount(float pTick){ return life<0?1:1-pTick; } }
    // + getProgress(pTick), getCurrentColor(pTick), getNextColor(pTick) with 0.5 crossfade on alpha
}
```

- Behavior: identical to `SoupHolder.tickSoup` — best match among recipes whose `time <= progress`, max ingredient count, id tiebreak for determinism. The renderer asks `holder` for fluid tint and `floating` for per-item sink amount. Since all textures are water, renderer just tints the water box.
- Extensibility note: if staging by `time` is never desired (all stages are immediate), set all stage recipes `time=0` and the holder picks the best ingredient-count match regardless of progress.

### 5.5 Finish logic — keep both inside

```java
protected void finishRecipe(Level level, AlchemyRecipe<?> recipe) {
    // 1. Determine outputs
    var outFluid = recipe.resultFluid.copy(); // may be EMPTY
    var outItem  = recipe.resultItem.copy();  // may be EMPTY
    // 2. Handle craftingRemainingItem for any input that had a container? No — containers already returned on insertion.
    // 3. Replace pot contents in-place
    items.clear();
    if (!outItem.isEmpty()) {
        // distribute result items into the 12 slots (respecting MAX_SLOTS; for now assume single-stack result)
        // If result count >1 and stackable, split across slots? For count==1, just add one stack.
        items.addItem(outItem.copyWithCount(1)); // or handle count>1 via loop
        // If outItem count > MAX_SLOTS logic: spill extra via popResource? Prefer keep-both but overflow drops.
    }
    if (!outFluid.isEmpty()) {
        fluid = outFluid; // replace fluid (partial amounts preserved if recipe specifies)
    } else {
        // fluid-only vs item-only distinction: if result is item-only, empty the tank
        // Need to know if recipe intended to consume fluid: if outFluid is EMPTY and outItem is non-empty → item-only
        if (!outItem.isEmpty()) fluid = FluidStack.EMPTY;
        // if both empty cannot happen (validated)
    }
    // 4. Notify + re-evaluate stages
    notifyTile(); // sync + doRecipeSearch=true + recheckStage=true
}
```

- No `Block.popResource` for success — both outputs stay inside. Overflow (e.g., result item count > MAX_SLOTS) drops excess via `Block.popResource` as fallback.
- The pot is now in its result state; if the result again matches a recipe, the next auto-start will begin on the next tick.

---

## 6. Interaction & Gating

| Action | When `inProgress==0` | When reacting (`inProgress>0`) |
|---|---|---|
| **Right-click with item stack** (hand) | If `fluid.getAmount()<1000` → reject (send message "requires full bucket"). Else `tryAddItem(stack, true)` — checks hypothetical `AlchemyInv(fluid, items+stack,false)` against `ALCHEMY_RT` *and* `ALCHEMY_STAGE_RT`? Requirement says "only items that match recipes" — interpret as any recipe that could still be completed from current state, i.e., there exists a recipe where `inv.size()+1 <= input.size()` and all current+new items are subset. If accepted and not `simulate`, handle container return: `var remainder = stack.getCraftingRemainingItem()` (or `stack.getCraftingRemainingItem()` / `SlipBottleItem` pattern); `stack.shrink(1)` (unless creative); if remainder non-empty give to player (`player.getInventory().placeItemBackInInventory(remainder)`). Then `items.addItem(stack.copyWithCount(1))`, `notifyTile`. | Reject (no insert; swing, no consume) |
| **Right-click with fluid container (bucket etc.)** via hand or `FluidUtil` | Delegate to fluid capability: `fill` if tank not full and fluid matches; `drain` if holding empty container and tank non-empty. Use `FluidUtil.tryEmptyContainer` / `tryFillContainer` helpers. Allow partial (e.g., bucket always 1000, but pipe may insert 100) | Reject — capability `fill`/`drain` return 0/`EMPTY` when `inProgress>0` |
| **Right-click empty hand / sneak+click** | If not sneaking: pop last item (LIFO) into player hand (if any floating). If sneaking: **clear** — drop `stage.floating` non-consumed items, **void fluid** (`fluid=EMPTY`), clear `items`, cancel progress if any (`totalTime=0; recipeProgress=0`), `notifyTile`. | **Allowed** per owner (exception): same clear behavior even while reacting — cancels progress, voids fluid, drops non-consumed items, syncs. |
| **Hopper / pipe `IItemHandler` insert/extract** | `canPlaceItem` → `fluidFull && tryAddItem`. `canTakeItemThroughFace` → only if `inProgress==0` and `floating` item (i.e., not consumed) — or simply allow extraction of any slot only when idle. | `canPlaceItem` false; `canTakeItemThroughFace` false (except clearing path not via capability) |
| **Hopper / pipe `IFluidHandler`** | `fill`/`drain` as above (partial allowed) | `fill`→0, `drain`→`EMPTY` |
| **Break block** | Drop `floating` items only; fluid voided (or kept in block NBT — choose void). Progress lost. | **Allowed** (exception). Same drops as clearing; fluid voided. Cancel progress. |

Bucket helper uses `FluidUtil.getFluidContained(stack)` / `FluidStack` + NeoForge's `FluidUtil.tryEmptyContainerAndStow`. All logic server-only.

---

## 7. Overlay Hint

- Reuse `youkaishomecoming/content/pot/overlay/HintOverlay.java` pattern; copy or generalize to `content/block/alchemy/AlchemyHintOverlay.java`.
- BE implements `IHintableBlock.getHints(Level, BlockPos)`:

```java
public List<Ingredient> getHints(Level level, BlockPos pos) {
    if (inProgress() > 0) return List.of();
    if (fluid.getAmount() < FLUID_CAPACITY) return List.of(); // fluid-first: no hints until full
    var cont = createContainer(false);
    var recs = level.getRecipeManager().getRecipesFor(getRecipeType(), cont, level);
    List<Ingredient> ans = new ArrayList<>();
    for (var r : recs) ans.addAll(r.value().getHints(level, cont));
    return ans;
}
```

- `UnorderedAlchemyRecipe.getHints` returns remaining `Ingredient`s after subtracting matched items (copy of YH logic, plus check that `inputFluid` matches). Accounts for `componentsPatch` sensitivity for potions.
- Registration: add `HintOverlay` as a `LayeredDraw.Layer` in client init, positioned at `0.7w × 0.5h`; cycle ingredients every 15 ticks. Up to `MAX_SLOTS` hints, 4-wide grid.

---

## 8. Visual & Audio

 - **Fluid**: `AlchemyPotRenderer` (BER):
  - Resolve color: if `stage.current != DEF` use `stage.current.color` (already computed via `getColor(inv,registries)`); else derive from `fluid` tint (`IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid)`). All use water still texture.
  - Call `FluidRenderer.renderFluidBox(waterTex, xMin=3..13, yMin=4, yMax=10, ..., color)` inside cavity. For crossfade, render two boxes with `getCurrentColor` / `getNextColor` alphas (same 0.5 crossfade as `SoupHolder.getProgress`).
  - Fluid height scales with `fluid.getAmount()/FLUID_CAPACITY` — for reactions with partial result, **client-only lerp**: `visualAmount = Mth.lerp(inProgress(), FLUID_CAPACITY, recipe.resultFluid.getAmount())` (server fluid stays full until `finishRecipe` snaps). On break/clear, client height snaps to 0.
- **Items**: small floating `ItemStack` renders bobbing (`level.getGameTime()*0.05`) — reuse `CookingRenderUtil` / `LargeCookingPotRenderer` logic; consumed items (`life>=0`) sink via `1 - getProgress` alpha/offset.
- **Progress**: `InfoTile.lines()` shows `"Alchemy Progress: N%"` when `inProgress>0` (or via `TileInfoDisplay` for Jade).
- **Particles** (client tick, vanilla only):
  ```java
  if (inProgress()>0 && level.random.nextFloat() < 0.25f) {
      double x = pos.getX()+0.5+rand(-0.18,0.18), y=pos.getY()+0.78, z=pos.getZ()+0.5+rand(-0.18,0.18);
      level.addParticle(ParticleTypes.BUBBLE, x,y,z, 0,0.04,0);
      if (level.random.nextFloat()<0.08f) level.addParticle(ParticleTypes.BUBBLE_POP, x,y,z, 0,0.02,0);
  }
  if (level.random.nextFloat()<0.02f) level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.0f);
  ```
- **Block entity tick**: client mirrors `recipeProgress` increment even without heat; bubble guard ensures lock.

---

## 9. Persistence, Sync, Misc

- Serial fields: `items` (BaseContainer), `fluid` (FluidStack), `totalTime/recipeProgress/recipeId` from `TimedRecipeBlockEntity`. `stage` recomputed — `recheckStage` flag triggers `recheckStage(be,level)` on `loadAdditional` and on client tick when dirty.
- No JEI category yet — add `compat/jei/AlchemyRecipeCategory.java` + `AlchemeyStageCategory.java` (optional; stage category may be hidden/merged).
 - **Loot / breaking & clearing**: `Block.getDrops` / `clear()` both return only `stage.floating` non-consumed items; fluid voided on both (see §3). If fluid is full and unconsumed, breaking still voids fluid — player must bucket-extract first. Alternative NBT-stored fluid rejected per spec.
- Comparator output: `items.size() * 15 / MAX_SLOTS` plus fluid signal: `fluid.getAmount()*15/FLUID_CAPACITY` weighted? Simple: `max(itemsSignal, fluidSignal)`.
- No heat tag — `shouldStopProcessing` returns `false`.

---

## 10. Configuration

- `public static final int MAX_SLOTS = 12;` in `AlchemyPotBlockEntity`.
- `public static final int FLUID_CAPACITY = 1000;`
- If balancing needed later, expose `GLModConfig.SERVER.alchemyMaxSlots` — not now.

---

## 11. File / Registration Checklist (when implementation starts)

- `content/block/alchemy/AlchemyPotBlock.java`
- `content/block/alchemy/AlchemyPotBlockEntity.java` + `AlchemyItemContainer.java`
- `content/block/alchemy/AlchemyPotRenderer.java`
- `content/block/alchemy/AlchemyStageHolder.java` (color-only, dynamic-ready)
- `content/recipe/alchemy/AlchemyInv.java`
- `content/recipe/alchemy/AlchemyRecipe.java` + `UnorderedAlchemyRecipe.java` + builders
- `content/recipe/alchemy/AlchemyStageRecipe.java` + `SimpleAlchemyStageRecipe.java` (+ `DynamicAlchemyStageRecipe.java` placeholder for color blend) + builders
- `content/block/alchemy/AlchemyHintOverlay.java` + client hook in `GLClient` / `YoukaisHomecoming` `YHClient`-style
- `compat/jei/AlchemyRecipeCategory.java`
- Datagen: `init/data/GLAlchemyRecipeGen.java`, lang in `GLLang`, recipe JSONs via `BaseRecipeBuilder`
- Registrations in `GLBlocks` (`ALCHEMY_POT`, `ALCHEMY_POT_BE`), `GLRecipes` (`ALCHEMY_RT`, `ALCHEMY_STAGE_RT`, `ALCHEMY_STAGE_DYNAMIC_RT` if needed), `GensokyoLegacy.registerCapabilities` (`IFluidHandler`, `IItemHandler`).

---

## 12. Resolved Questions & Remaining Considerations

Owner answers applied (see §3). Remaining design considerations:

- **Partial fluid + fluid-first interaction**: `fill` may be called with <1000 mB (e.g., 250 via pipe). The pot will accept partial fills until 1000; only at 1000 are items allowed. `tryAddItem` must reject if `fluid.getAmount() < FLUID_CAPACITY`, even if a stage recipe would otherwise match a partial fluid (stage `inputFluid` may require exactly 1000 — validate accordingly).

- **Container return edge**: `ItemStack.hasCraftingRemainingItem()` returns `Item` for containers; on `tryAddItem` success, return `stack.getCraftingRemainingItem()` to player. For potions, this is a glass bottle — ensure creative mode doesn't dupe.

 - **Stage consumption vs break/clear**: `removeConsumed` defines which slots are considered dissolved. `dumpInventory`/`clear`/`onRemove` must copy `items.getAsList()`, apply `stage.current.removeConsumed(copy)` *or* inspect `stage.floating` where `life<0` means still floating (returnable). Void fluid on both.

- **Item-only result**: `finishRecipe` sets `fluid = EMPTY` when `resultFluid.isEmpty() && !resultItem.isEmpty()`. The tank is now empty, so next `tryAddItem` will be blocked until fluid is refilled — as intended.

 - **Dynamic color preparation**: keep `AlchemyStageRecipe.getColor(AlchemyInv, RegistryAccess)` hook. Simple stage returns field; future special serializer (separate `RecType`) will inspect `inv.list()` potion contents/blend. Add `@SerialField` `color` in base for static case; subclass may ignore it.

 - **Potion NBT sensitivity**: use `PotionIngredient` from `l2core` (`dev.xkmc.l2core.init.reg.ingredient.PotionIngredient`) for `DataComponents.POTION_CONTENTS`-aware matching; `ItemStack` hashing in hint dedup includes `componentsPatch` (as YH does). Example JSON: `{ "type":"l2core:potion", "potion":"minecraft:strong_healing" }` (check L2Core codec exact field).

All other prior open questions are resolved; proceed to implementation.
