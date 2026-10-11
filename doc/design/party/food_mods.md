# Party Food — third-party mods

One handler class per kind of block food, in `content/attachment/party/food/`, each constructed only when its mod is loaded — so a line naming an absent mod's classes costs nothing and cannot throw, and nothing here needs editing when a mod is added or removed. The derivation they all feed is in `food.md`.

## 1. Two counting conventions, and getting them backwards

The properties do **not** agree on what they count, and assuming one convention silently breaks the other kind of food:

| Block | Property | Range | Fresh value | Counts | Serves left |
|---|---|---|---|---|---|
| vanilla `cake` | `bites` | `0..6` | 0 | **up** — bites taken | `value + 1` → 7 |
| FD `PieBlock` | `bites` | `0..3` | 0 | **up** | `value + 1` → 4 |
| FD `FeastBlock` | `servings` | `0..4` | 4 | **down** — servings left | `value` → 4 |
| YSF `PotFoodBlock` | `serve` | `1..2` / `1..4` | max | **down** | `value` |
| KC `FoodBiteBlock` | `bites` | `0..maxBites` | 0 | **up**, but see below | `maxBites - value` |
| KC `PlateBlock` | `servings` | `0..maxCount` | **max** | **down** — servings left | `value` |

`serves` and `consume` both need the direction, and `AbstractCountdownFood` reads it off the property rather than being configured with it:

```java
/** Whether the block's count property starts at its maximum (servings left). */
private static boolean countsDown(BlockState state, IntegerProperty property) {
    // a fresh state sitting at the maximum counts down; one at the minimum counts up
    return state.getBlock().defaultBlockState().getValue(property) >= maximum(property);
}

@Override
public int serves(BlockState state) {
    var left = countProperty(state);
    if (left == null) return 1;                       // no counter: one indivisible serve
    int value = state.getValue(left);
    return countsDown(state, left) ? value : value + 1;
}
```

This matters more than it looks. Had `consume` assumed "always decrement", an FD feast would step its servings **up** towards its maximum and, having started there, would never go anywhere: each guest eats, the block stays at max, and the party can never be satisfied. The reverse assumption breaks cake and pie the same way. Reading the direction off the block is the only version correct for both, and it names no foreign property.

### 1.1 And one counter whose range lies by one

Kaleidoscope Cookery also counts **up**, and also calls it `bites` — which is exactly why it is worth spelling out that this handler does not go through `AbstractCountdownFood`.

Vanilla cake's property runs one *past* the last slice (`BITES` 0..6 for seven slices) because `CakeBlock.eat` needs somewhere to land after the seventh and removes the block from there. Cookery instead builds `IntegerProperty.create("bites", 0, maxBites)` and stops incrementing at the maximum, leaving an emptied block standing:

```java
// FoodBiteBlock.eat — the whole counter, abridged
int bites = state.getValue(this.bites);
if (bites < this.getMaxBites()) level.setBlock(pos, state.setValue(this.bites, bites + 1), 3);
```

So the true count is `maxBites - bites`, not `bites + 1`. Sharing `AbstractCountdownFood` would report one serve too many on a fresh dish, and `consume` would compute `0 + 1 > maxBites` on the very first bite and **destroy a whole untouched dish**. `BiteFood` therefore asks the block for `getMaxBites()` — a public accessor — rather than deriving the count from the property's range. This is the general lesson: a counter's *range* is not its *count*, and the two agreeing on four kinds of food is no reason to assume it on a fifth.

## 2. The cake problem

In 1.21.1 `CakeBlock` never builds a `FoodProperties` at all. It feeds the player from code:

```java
// CakeBlock.eat — net/minecraft/world/level/block/CakeBlock.java:101
int i = state.getValue(BITES);
player.getFoodData().eat(2, 0.1F);
if (i < 6) level.setBlock(pos, state.setValue(BITES, i + 1), 3);
else level.removeBlock(pos, false);
```

`BITES` runs 0..6, so the cake is **7 slices of `eat(2, 0.1F)`**. Its `BlockItem` carries no food, so reading it yields `null` — a cake worth nothing, and with no other mods installed *no character would ever come to a party*.

So `CakeFood` tries the id permutation first, and only falls back to vanilla's own numbers:

```java
@Override
public @Nullable FoodProperties food(BlockState state) {
    var permuted = Permutations.foodOf(state.getBlock());
    return permuted == null ? fallback : permuted;
}
```

The order matters. A mod's cake is read from **its own** slice item, which is authoritative; vanilla's numbers are a last resort that only applies when no slice item exists. `0.4` is not arbitrary: `FoodConstants.saturationByModifier` is `foodLevel * saturationModifier * 2.0`, so `2 * 0.1 * 2.0 = 0.4`.

One slice is then `round(2 × 0.5 + 0.4 × 2.0) = 2`, and a whole 7-slice cake is worth **14**. Compare the block-valued approach this replaced, which scored the whole cake 13 and treated it as a single indivisible unit — one guest ate the lot, and there was no such thing as a partly-eaten cake.

## 3. Reference values, per serving

Every one is derived by the code above, not chosen by hand:

| Food | per serving | serves | whole block |
|---|---|---|---|
| `minecraft:cake` | 2 | 7 | **14** |
| carrot | 3 | — | 3 |
| bread | 5 | — | 5 |
| steak | 7 | — | 7 |
| golden apple (2 beneficial effects) | 16 | — | 16 |

## 4. Registration is by class — and by identity for the two that have no class

`BlockFoods.register()` is called once from the common setup, after the blocks exist. `PartyFoodHandler` maps a handler to a block **class** and resolves by walking the hierarchy most-derived-first:

```java
// content/attachment/party/core/PartyFoodHandler.java
private static final Map<Class<?>, BlockFoodHandler> BY_CLASS = new ConcurrentHashMap<>();
private static final Map<Block, BlockFoodHandler> RESOLVED = new ConcurrentHashMap<>();   // cached per block

public static BlockFoodHandler of(Block block) {
    var cached = RESOLVED.get(block);
    if (cached != null) return cached;
    var found = resolve(block);
    RESOLVED.put(block, found);
    return found;
}
```

There is **no per-block map and no class→handler map**. The handlers are one ordered list, and each decides for itself which blocks it covers:

```java
// content/attachment/party/core/PartyFoodHandler.java
private static final List<BlockFoodHandler> HANDLERS = new CopyOnWriteArrayList<>();

public static BlockFoodHandler of(Block block) {
    for (var handler : HANDLERS) {
        if (handler.handles(block)) return handler;   // first match wins
    }
    return MISSING;
}
```

Every handler answers `handles` with an `instanceof` against the one class it was written for — `CakeFood` tests `CakeBlock`, `FeastFood` tests `FeastBlock`, and so on — so subclasses are covered without anything enumerating them. The fallback is not a special path either: `BlockItemFood` accepts every block and simply sits **last** in the list, which is the only thing keeping it from swallowing the rest. There are no block ids in the party food setup at all.

**Two handlers cannot use `instanceof`, because the blocks they name have no class.** Youkai's Feasts registers its cutting board and both halves of its steamer as `DelegateBlock`s built from `BlockMethod`s — `CuisineBoardBlock` is a method, not a `Block`, so there is nothing to test a block against — and the only way to name one is identity:

```java
// content/attachment/party/food/SteamerRackFood.java
@Override
public boolean handles(Block block) {
    return block == YHBlocks.STEAMER_RACK.get() || block == YHBlocks.STEAMER_POT.get();
}
```

Both live in the **container** list, not this one, because they are containers. That is the whole reason there are two lists: a state-derived handler that happened to match a container's class first would route it through the cache, and a cached answer about a block entity is a wrong answer kept for ever.

The catch-all earns its place. Farmer's Delight's feasts are all the same `FeastBlock` class and each hands out a different serving item, which at one point looked like it needed a per-block table; it does not, because `FeastBlock.getServingItem(BlockState)` and `PieBlock.getPieSliceItem()` are both public and the handler simply asks the block.

```java
// init/registrate/BlockFoods.java
public static void register() {
    PartyFoodHandler.register(new CakeFood());       // instanceof CakeBlock
    if (loaded(FD)) {
        PartyFoodHandler.register(new FeastFood());   // instanceof FeastBlock
        PartyFoodHandler.register(new PieFood());     // instanceof PieBlock
    }
    if (loaded(YSF)) {
        PartyFoodHandler.register(new SaucerFood());  // instanceof FoodSaucerBlock
        PartyFoodHandler.register(new PotFood());     // instanceof BowlBlock
    }
    if (loaded(KC)) {
        PartyFoodHandler.register(new BiteFood());     // instanceof FoodBiteBlock
        PartyFoodHandler.register(new PlateFood());    // instanceof PlateBlock
    }
    PartyFoodHandler.register(new BlockItemFood());   // last: accepts everything
}
```

Handlers are only **constructed** when their mod is present. A handler that was never constructed is never asked, so its class is never loaded and its foreign types never resolve — which is what keeps the three mods optional without reflection or a hard dependency.

Youkai's Feasts has several genuinely different kinds, so it needs one entry per shape — but three of its block classes are covered by registrations made elsewhere, because it builds on the other two mods:

| YSF class | Shape | Covered by |
|---|---|---|
| `FoodSaucerBlock` | saucer dishes, 1 serve | `SaucerFood` |
| `BowlBlock` (incl. `IronBowlBlock`, `PotFoodBlock`) | bowls and pots, 2 or 4 | `PotFood` |
| `YHCakeBlock`, `YHCandleCakeBlock` | cakes, 7 slices | `CakeFood`'s `instanceof CakeBlock` |
| `YHPieBlock` | pies, 4 bites | `PieFood`'s `instanceof PieBlock` |
| `SurpriseFeastBlock` | feasts, 4 servings | `FeastFood`'s `instanceof FeastBlock` |

`SaucerFood` tests `FoodSaucerBlock` rather than its `BaseSaucerBlock` parent on purpose: the parent also covers `EmptySaucerBlock`, which serves nothing, and being precise here is what lets `BlockItemFood` stay a blunt catch-all.

Kaleidoscope Cookery is close to the opposite case: two block classes cover its whole food set, because the mod builds every dish from the registry entry of the same id. `CommonRegistry` constructs each block from the `FoodData` of the id it is about to register, so no per-block entries are needed:

| KC class | Shape | Serves |
|---|---|---|
| `FoodBiteBlock` | the bulk of the Chinese-cuisine set, 1 to 6 bites (`golden_salad` is the six) | `maxBites - bites` |
| `FoodBiteOneByTwoBlock` | the wider dishes, 3 to 5 bites per half | `maxBites - bites` |
| `PlateBlock` | the eleven plates and platters, 3 to 5 servings | `servings`, counted down |

Its `foodProperties` field is `protected` with no accessor, but that is no obstacle: `FoodBiteRegistry.FOOD_DATA_MAP` is public and holds an entry for every bite block, and `FoodData.blockFood()` is the very properties instance the block feeds per bite. The same entry's `itemFood()` must **not** be used — that is the whole dish, not a bite of it, and valuing each serve at the entire meal would score a single plate of Dark Cuisine above a golden apple. This is the case the `Permutations` rationale above was written for: reach for the mod's own public table before reflecting a private field.

### 4.1 A plate is not a container

`PlateBlock` *looks* like the hard case in this feature: its state holds a bare `servings` count and nothing about what is on it, so a plate of spring rolls and a plate of century eggs read as the same block. The first draft of this handler concluded from that it was unpriceable, and skipped it. That was wrong, and wrong for a boring reason — the mod does not have one generic plate.

`PlateRegistry` gives each of the eleven a `PlateData` of its own, listing the items that plate may carry, and `CommonRegistry` builds the `PlateBlock` from that list:

```java
// PlateRegistry.init, abridged
registerPlateData(mcLoc("shengjian_mantou_plate"),
        PlateData.create(5).setServingItems(ModItems.SHENGJIAN_MANTOU));
registerPlateData(mcLoc("berry_platter"),
        PlateData.create(4).addServingItems(() -> Items.SWEET_BERRIES, () -> Items.GLOW_BERRIES));
```

So the state says **how many** and the registry says **of what**, and the two together answer the question exactly. `PlateFood.food` reads `PLATE_DATA_MAP.get(blockId).getServingItems()` — the same shape of answer as `BiteFood`, and for the same reason: the serving item list is the mod's own public table.

| KC block | Serves | Of |
|---|---|---|
| `shengjian_mantou_plate`, `baozi_plate` | 5 | the matching mod item |
| `sticky_rice_cake_plate`, `tomato_platter`, `chorus_fruit_platter` | 5 | the matching mod item |
| `qingtuan_plate`, `sticky_candy_plate`, `zongzi_plate`, `apple_platter` | 4 | the matching mod item |
| `berry_platter` | 4 | sweet berries **and** glow berries |
| `watermelon_platter` | 3 | melon slices |

Two details make `PlateFood` an ordinary `AbstractCountdownFood` rather than a special case like `BiteFood`:

- **The default state is `maxCount`, not zero.** A freshly placed plate is *full* and the count falls as food is taken off — the "servings left" convention, so `countsDown` reads it off the block correctly and `serves` is the property value verbatim. Assuming a plate starts at zero would have inverted every count in the mod.
- **`consume` is inherited unchanged.** Taking the last serving steps the count to zero and leaves the empty plate standing, which is what the mod does: `PlateBlock` only removes itself when a player right-clicks an *empty* plate to pick it up, and that is a player gesture the party system should not imitate. An emptied plate is inert — `serves` is 0, so `FoodResult` is `INVALID` and `PartyArea.scan` drops it from the count without deleting anything.

`berry_platter` hands out two items per serving, and one serve of two things has no single taste to price it by. It is scored by the first serving item that carries food, which undervalues it by roughly half — a deliberate approximation on one decorative platter rather than a rule invented to accommodate it. (It is take-only in the mod regardless: `canRefill` refuses any plate not holding exactly one item, so a two-item platter can never be refilled by hand.)

Each mod's handlers are constructed only when `ModList.isLoaded` says so, so a line naming an absent mod's classes costs nothing and cannot throw. Nothing here needs editing when a mod is added or removed.

## 5. The two Youkai's Feasts containers

Neither is a plate of food in the usual sense — both are vessels you fill — and both are party food anyway, because a host who has put out a cutting board of ingredients or a steaming pot has laid out a party.

**`CuisineBoardFood` — a cutting board.** Whatever is lying on it, one entry per kind of thing. Reading the list is done from the block entity rather than from the model, because the model's copy is the snapshot taken when the recipe matched while the block entity's is live — and because a guest taking a plate has to take it from the board itself, or the next guest is offered food that is no longer there.

**A stack on a board is one plate, not a number of servings.** YSF writes its nigiri and gunkan recipes to `count: 2`, and a board handed a pair keeps the pair as one stack that `addToPlayer` hands over whole — there is no interaction that lifts one off and leaves its partner. So `contents` reports `serves = 1` with the stack's `FoodProperties` scaled to the plate, and `take` empties the stack rather than shrinking it by one.

Both halves matter, and only doing the first is a quiet failure. Leaving `serves` at the stack count values the board at exactly the same total — the planner multiplies — but lets two guests each eat one nigiri off a board that only ever gave them out in pairs. The scaling is `Servings.of(stack, stack.getCount())`: nutrition, saturation and eating time multiply, and an effect's probability combines as `1 − (1 − p)ⁿ` rather than doubling, because it is rolled once per item eaten and that is the only reading of "twice the odds" that is true.

**The board's dish is told apart from a recipe in progress by what is on it, not by the model's class alone.** A `FoodTableItem` is the mod's own "this is the whole dish" form and covers every roll. A nigiri or a gunkan does **not** produce one: it is put on the board as the finished item rather than assembled there, and the model the board resolves to is the one YSF uses to *assemble* it, so it carries both the recipe and the dish and there is no second form to ask which is which. The remaining test is the contents themselves — a dish is made of nothing but food, and a half-built one always has rice, kelp or a sauce on it, none of which is anything a guest can eat. That is also what keeps an empty board and a bare rice bowl out of the spread.

**`SteamerRackFood` — a steamer, both halves of it.** The pot carries up to two rack tiers of its own (the `pot_racks` property, which is how you steam with no rack at all) and each rack block stands in the pot with up to four tiers. They are separate block entities with separate storage, so a full steamer is genuinely two food sources and neither shadows the other. Unlike the board this needs no reflection — the contents are public fields on `RackData` and `RackItemData`.

**Their placement rules differ, and that asymmetry is the design.** A board is food only on the table, exactly like any other dish. A pot is food **wherever it stands** — on a stove, on the table, on the ground — because a pot of food on a kitchen floor is a thing a host has actually done, and refusing it would put the feature out of reach in the one room it was made for. A rack is food on the table, or in a pot, and on the ground it is not: a rack is a part of a steamer rather than a dish in its own right, and it needs a pot or a table to be part of anything. The pot has to be looked *through* to work that out — a rack inside one has pot interior below it, not the pot block, so the block that carries it is two or three down.

**The board's contents are read reflectively, and that is forced.** Both the board's list and the model's are private with no accessor. The alternatives were a mixin accessor — which would need a config plugin to survive Youkai's Feasts being absent, and it *is* absent from a dev launch where it is `compileOnly`, so an unguarded mixin would fail the game every time rather than only when the feature is used — or `getHints`, which describes what the board could become rather than what is on it: a board holding one apple out of a three-part recipe would report three ingredients, and the party would be budgeted against food the host never put out. The field is resolved once and never again, and a board whose field has been renamed by an update reads as "no food on it" rather than taking the party down.

## 6. The support problem — measured, and the answer was "no change needed"

Third-party block foods run a `canSurvive` check against the block below. The two that matter:

| Food | `canSurvive` test |
|---|---|
| FD feasts, YSF `RotatedFeastBlock` | `Block.canSupportRigidBlock` → `isFaceSturdy(UP, RIGID)` |
| vanilla cake, YSF `BaseSaucerBlock` | `level.getBlockState(pos.below()).isSolid()` |
| KC `FoodBiteBlock`, `PlateBlock` | none — no `canSurvive` override, so they sit anywhere |

Both were initially assumed to fail on `large_table`, whose tabletop is only a 3/16-thick slab (`Block.box(0,13,0,16,16,16)`, `LargeTableBlock.java:31`). That drove two changes: `getBlockSupportShape` → `Shapes.block()`, and `forceSolidOn()` on the block properties.

**Both were wrong, and both have been reverted.** `large_table` already passes both checks unmodified.

Measured against the real shapes, replicating `SupportType.RIGID.isSupporting` and `BlockBehaviour.calculateSolid`:

```
shape                          RIGID    isSolid
TOP    (attached, bare)        true     true
BARE   (unattached, bare)      true     true
CLOTH  (attached, dressed)     true     true
CLOTHED(unattached, dressed)   true     true
```

- **`isSolid()`** — `calculateSolid` accepts a collision AABB whose `getSize()` (the *mean* of the three extents) is at least `0.7291666666666666`, or whose Y extent is a full block. `TOP` alone measures `(1.0 + 0.1875 + 1.0) / 3 = 0.7291666666666666` — exactly the threshold, inclusive. `BARE` additionally reaches y=0 through `STAND`, so its Y extent is 1.0 and it clears on the second clause. Both pass on their own.
- **`SupportType.RIGID`** — this was misread. Its check is `!joinIsNotEmpty(faceShape, RIGID_FRAME, ONLY_SECOND)`, where `RIGID_FRAME` is the block **minus** the inset `box(2,0,2,14,16,14)` — a 2-thick frame around the *perimeter*, not a required region the face must cover. `ONLY_SECOND` asks whether any part of that frame is *absent* from the face. Since `TOP` spans the full 16×16 footprint, its top face covers the frame completely, so there is no gap and the check passes. A narrow pillar would fail; a full-footprint slab does not.

So the table top was always a valid surface for block foods, and the change bought nothing while altering a shipped block's behaviour globally — anything could now be stood on it, and mobs stopped spawning inside it. Worth the note: `getSize()` is a mean, so a 3/16 slab passing is a coincidence of this particular slab's proportions, not a general property of thin tops. A thinner or narrower top would fail, and if a future table needs a different profile it must be re-measured rather than assumed.

**Side effects now avoided:** `large_table` keeps its original collision behaviour, so mob spawning inside it and what can be stacked on it are both unchanged from before this feature.
