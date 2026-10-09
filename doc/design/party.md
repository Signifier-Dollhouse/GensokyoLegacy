# Party Event — Design Doc

> **Status:** implemented — compiles, `runData` and `./gradlew build` both green. Not yet played in a live world, so §6's flow and §10's limits are reasoned from the code rather than observed.
> **Package:** `content/attachment/party/` — `core/` (math, planner, state, handler lookup), `food/` (one handler class per kind of block food), `item/` (letter), plus `content/entity/module/PartyModule.java` and `content/entity/behavior/task/party/YoukaiPartyTask.java`
>
> Party state is attachment-shaped, so the whole feature lives under `content/attachment/` alongside the other per-feature packages. That puts `PartyArea`'s neighbour `ChunkPosRange` in `content/attachment/area/` next to it rather than across the tree.
>
> **Related:** `character_visit.md` (the guest-clone trick reused here), `reputation.md` (the reputation this reads)

---

## 1. Overview

A player lays block food out on tables, sets chairs, writes a letter, and sends it. Characters with enough reputation come, eat, and leave gifts on the table.

**Every character can come.** There is no whitelist and no character is locked out — a character with no authored `party_taste` entry gets a permissive default (§5.2). What decides who shows up is only how good the spread is and how well the player knows them.

```
lay out block food on party tables  +  put out seats
        ↓
right-click a table with the invitation letter  →  send
        ↓
letter consumed; spread appeal measured; candidates scored (§5)
        ↓
knapsack picks the guest set that fits the appeal budget and the seat count
        ↓
guests spawn as temporary clones, walk in, sit, eat, gift, leave
```

There is no monetary cost and no cooldown. The letter names no specific character — it goes out to everyone who qualifies, and who that is depends entirely on the spread and on the player's standing with each of them. The only thing the player spends is food, and the only thing they get back is gifts.

Four pieces:

| Piece | Lives in | Job |
|---|---|---|
| **Attractiveness / fulfillment math** | `content/attachment/party/core/PartyMath.java` | pure functions, no world writes |
| **Roster selection** | `content/attachment/party/core/PartyPlanner.java` | knapsack + seat cap, pure |
| **Party state** | `GLMeta.PARTY`, a `LevelChunk` capability | host, anchor, roster, phase, counters |
| **Guest behaviour** | `PartyModule` + `YoukaiPartyTask` | walk, sit, eat, gift, dismiss |

The first two are deliberately side-effect free so `/gensokyo party preview` can run the identical code path as a real send and be guaranteed to predict it.

---

## 2. Tables, seats and food

### 2.1 Tags

Two block tags, both **generated** by `GLTagGen.onBlockTagGen`:

```java
/** Tables a party may be laid out on. Our large dining tables only — see §2.3. */
public static final TagKey<Block> PARTY_TABLE = block("party_table");

/** Seats. A guest needs somewhere to sit; no seats, no party. */
public static final TagKey<Block> PARTY_SEAT = block("party_seat");
```

```java
private static void genPartyTag(RegistrateTagsProvider.IntrinsicImpl<Block> pvd) {
    pvd.addTag(PARTY_TABLE).addTag(LARGE_TABLE);
    pvd.addTag(PARTY_SEAT).addTag(CHAIR).addTag(STOOL);
    for (var wood : YSF_WOODS) {
        pvd.addTag(PARTY_TABLE).addOptional(ResourceLocation.parse("youkaisfeasts:" + wood + "_dining_table"));
        pvd.addTag(PARTY_SEAT).addOptional(ResourceLocation.parse("youkaisfeasts:" + wood + "_dining_chair"));
    }
}
```

`LARGE_TABLE` already contains the 11 `*_large_table` blocks; `CHAIR` the 11 `*_large_chair` plus `wooden_large_chair_scarlet_devil_mansion`; `STOOL` the 11 `*_chair` stools. Nesting those rather than listing them keeps the party tags in step as blocks are added.

Youkai's Feasts' dining tables and chairs go in as optional entries, which the tag loader drops when the mod is absent — that is what keeps it optional rather than a hard dependency, and no Java here names one of its blocks. It uses the same 11 wood names we do, so one loop covers both tags.

**There is no `party_food` tag.** Food is decided by code and by datapack, not by a tag — see §2.2's `isFood`. A handler recognising the block, or a `party_food` data map entry, is what makes something food; there is no third way, and no tag to keep in step.

#### One trap worth naming: `loc()` is not a parser

```java
public static ResourceLocation loc(String id) {
    return ResourceLocation.fromNamespaceAndPath(MODID, id);   // namespaces a bare path
}
```

`loc()` prepends **our** modid, so `loc("youkaisfeasts:oak_dining_table")` builds `gensokyolegacy:youkaisfeasts:oak_dining_table` — a namespace plus a path containing a colon. Foreign ids must go through `ResourceLocation.parse`.

This cost real time. The symptom was that **any** `addOptional` call in this tag provider made `runData` hang: the Registrate provider never returned, workers went idle, and the main thread sat forever in `CompletableFuture.join` with no error and no stack trace. It was bisectable only by commenting lines out, and two plausible-sounding culprits were blamed first — `addOptional` itself, then `GLDecoBlocks.WoodType.values()` class-initialising mid-provider — before the malformed id turned up. Neither was the cause: `addOptionalElement` only appends to a list, and the wood list is a literal. One bad id, and a silent hang that looks exactly like a deadlock in someone else's code.

### 2.2 Food value

Appeal is **derived from what a food actually is to eat**, and counted **per serving** rather than per block.

```java
// content/attachment/party/core/PartyFoodValue.java
private static final float NUTRITION_SCALE = 0.5f;
private static final float SATURATION_SCALE = 2.0f;
private static final int BENEFICIAL_BONUS = 4;

/** The floor for anything that IS food; invalid food scores 0 instead. */
public static final int MIN_APPEAL = 1;
public static final int MAX_APPEAL = 40;

/** Appeal of ONE serving, from what it is to eat. */
public static int of(FoodProperties props) {
    if (props.nutrition() <= 0 || Servings.refuses(props)) return 0;
    float bonus = 0;
    for (var e : props.effects()) {
        var holder = e.effect().getEffect();
        if (holder != null && holder.value().isBeneficial()) bonus += BENEFICIAL_BONUS;
    }
    return clamp(Math.round(props.nutrition() * NUTRITION_SCALE + props.saturation() * SATURATION_SCALE + bonus));
}

/** Whether this block is party food at all. */
public static boolean isFood(RegistryAccess access, BlockState state) {
    return PartyFoodHandler.claims(state.getBlock()) != null || PartyFood.of(access, state) != null;
}

/** Total appeal still on the table: serves x per-serve. */
public static int appeal(RegistryAccess access, BlockState state) {
    var override = PartyFood.of(access, state);
    if (override != null) return override.appeal() * result(state).serves();
    var result = result(state);
    return result.valid() ? of(result.food()) * result.serves() : 0;
}

/** Take one serving, leaving the world consistent. */
public static int consume(ServerLevel level, BlockPos pos, BlockState state) { ... }
```

#### What makes a block food

There is no `party_food` tag. A block is party food if **either**:

- a registered `BlockFoodHandler` recognises its class — `CakeFood` claims anything that is a `CakeBlock`, `FeastFood` anything that is a `FeastBlock`, and so on; or
- a datapack gave it a `party_food` data map entry.

The second is how a foreign block is opted in, and it is also how an override corrects a value the heuristic got wrong, so one mechanism does both jobs. The catch-all handler is deliberately **not** enough on its own: it exists to score a block something has already declared food, so it must not make every block on the table a cake.

#### Invalid food is not food

Three things make a tagged block worth nothing, and all three score **0** rather than the minimum:

- **No hunger value** — `nutrition() <= 0`. A block item can carry `FoodProperties` while being worth nothing to eat: a decorative item, a husk, another mod's placeholder. There is deliberately no "minimum appeal" fallback for a missing or empty serving, because a placeholder scoring 1 would still count towards the party's budget *and* still be walked to and eaten by a guest, costing a guest their whole visit for no fulfillment at all.
- **Poison or wither** — the same two `FeedModule` refuses outright.
- **Nothing to read** — no `FoodProperties` on the block, its serving item, or any permuted id.

A 0 is the signal that the block is **not party food at all**, and both consumers act on it — see `FoodResult` below for where the rule lives:

```java
// PartyArea.scan - dropped from the spread entirely, left standing in the world
if (worth <= 0 || !PartyFoodValue.result(here).valid()) continue;   // tagged, but not food
food.add(pos.immutable());
appeal += worth;

// YoukaiPartyTask.nearest - never walked to, so no guest's time is wasted
if (!PartyFoodValue.result(state).valid()) continue;
```

So a datapack author who adds a dud id to `party_food` gets a no-op rather than a bug: nothing is counted, nothing is eaten, and the block simply stays on the table.

#### `BlockFoodHandler` — how a block food is read and eaten

The derivation above needs three facts about a *placed* block, and they differ per mod, so there is one implementation per kind of food in `content/attachment/party/food/`:

```java
public interface BlockFoodHandler {
    /** How many servings are left right now. */
    int serves(BlockState state);

    /** What ONE serving is, as FoodProperties: from the block, or a serving item. */
    @Nullable FoodProperties food(BlockState state);

    /** Take one serving, advancing the block. Returns servings remaining. */
    int consume(Level level, BlockPos pos, BlockState state);
}
```

| Handler | Kind | Serves | Where the food is |
|---|---|---|---|
| `CakeFood` | vanilla cake, YSF cakes | 7, counted up | a permuted `*_slice` item, else vanilla's own numbers |
| `FeastFood` | FD feasts, YSF feasts | `servings` 0..4, counted **down** | the block's own `getServingItem` |
| `PieFood` | FD pies, YSF pies | 4, counted up | the block's own `getPieSliceItem` |
| `PotFood` | YSF bowls and pots | 2 or 4, counted **down** | a permuted item, else the block's `food` field |
| `SaucerFood` | YSF saucer dishes | 1, eaten whole | the block's own item |
| `BiteFood` | KC dishes and one-by-two dishes | `maxBites - bites`, 1 to 6 | `FoodBiteRegistry`'s entry for the block id |
| `PlateFood` | KC plates and platters | `servings` 3..5, counted **down** | `PlateRegistry`'s serving items for the block id |
| `BlockItemFood` | anything else, and the fallback | 1 | the block's own item |

#### `FoodResult` — one resolution, three answers

Everything above is derived from the same block state and is always wanted together: the planner needs the serve count, the appeal needs the properties, the guest needs the action. So it is resolved once and kept.

```java
public record FoodResult(int serves, @Nullable FoodProperties food, FoodConsume consume) {
    public static final FoodResult INVALID = new FoodResult(0, null, (level, pos, state) -> 0);

    public boolean valid() {
        if (serves <= 0 || food == null) return false;
        return food.nutrition() > 0 && !Servings.refuses(food);
    }
}
```

All four kinds of invalid food collapse to that one `INVALID`:

| Invalid because | Caught by |
|---|---|
| a container with 0 servings left | `serves <= 0` |
| no food properties anywhere | `food == null` |
| no hunger value | `food.nutrition() > 0` |
| poison or wither | `!Servings.refuses(food)` |

Mapping them to one value rather than four different near-zero scores is the point: callers ask `valid()` instead of each re-deciding what "worthless" means, and a handler that forgets to filter cannot accidentally make a dud look like food.

`consume` takes the state as a parameter rather than closing over it. The cached result belongs to the state it was built from, but by the time a guest gets round to eating, the world may have moved on — another guest took a slice, the player broke the block — and consuming against a stale state would put it back or resurrect it.

#### `FoodResultCache` — resolving it once

```java
private static final Map<BlockState, FoodResult> RESULTS = new ConcurrentHashMap<>();

public static FoodResult of(BlockState state) {
    var cached = RESULTS.get(state);
    if (cached != null) return cached;
    var resolved = resolve(state);
    RESULTS.put(state, resolved);
    return resolved;
}
```

Resolving is not cheap: a serving item costs a registry lookup and a string build, a serve count asks the block for its default state and walks the property's range, and a feast allocates an `ItemStack` to do it. `PartyArea.scan` walks up to a thirteen-cube box on every send and every guest re-reads the dish it is eating, so the same few hundred states were being resolved over and over.

The cache needs **no eviction policy**. States are interned per block, so it can never hold more entries than the blocks asked about have states between them — seven for a cake, five for a feast. It does not grow with world size, uptime, or how many parties have happened.

It is also **not datapack data**. What goes into a result is the serve count, the food properties and the consume action, and all three come from registration: the block's state definition, the item registry, and the handler. The `party_food` override is deliberately resolved by the caller in `PartyFoodValue` and never enters the cache, so a reload cannot leave a stale appeal behind here. `invalidate()` is still wired to the server's datapack sync as belt and braces — it costs nothing, and it removes a whole class of bug if the derivation ever grows a datapack input.

#### Finding the item form by permuting the id

Block foods commonly ship a matching item that holds the real food values — the slice of a cake, the contents of a bowl — and those items are named after the block rather than being reachable from it. Youkai's Feasts forms them by concatenating `"_slice"` onto the block name, so `youkaisfeasts:tarte_lune` has a `youkaisfeasts:tarte_lune_slice`.

`Permutations` exploits that: take the block's registry id, append each candidate suffix, and see whether the item registry has it.

```java
private static final String[] SUFFIXES = {"_slice", "_dish", "_piece"};

public static FoodProperties foodOf(Block block) {
    var id = BuiltInRegistries.BLOCK.getKey(block);
    for (var suffix : SUFFIXES) {
        var candidate = ResourceLocation.tryParse(id.getNamespace() + ":" + id.getPath() + suffix);
        if (candidate == null) continue;
        var props = propsAt(candidate);
        if (props != null) return props;      // a suffix that does not exist just misses
    }
    return null;
}
```

This is preferred over reflecting a field because it survives a mod update. YSF's `BowlBlock` keeps its food in a `protected final ItemLike food` with no accessor, so that read is still there as a **fallback** behind the permutation — it works today, but a rename would break it silently, and reflection against another mod is exactly the kind of thing that fails without a stack trace.

The four countdown kinds share `AbstractCountdownFood`, which owns the serve count and the step. Each of them **does** name its foreign block class in `handles` — that is the whole mechanism, and it is safe because the class is never constructed when its mod is absent (see below). Cookery contributes one of each sort: `PlateFood` is a fifth countdown kind that joins them, and `BiteFood` is a sixth that deliberately does *not*, for the reason in the next table.

#### Two counting conventions, and getting them backwards

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

##### And one counter whose range lies by one

Kaleidoscope Cookery also counts **up**, and also calls it `bites` — which is exactly why it is worth spelling out that this handler does not go through `AbstractCountdownFood`.

Vanilla cake's property runs one *past* the last slice (`BITES` 0..6 for seven slices) because `CakeBlock.eat` needs somewhere to land after the seventh and removes the block from there. Cookery instead builds `IntegerProperty.create("bites", 0, maxBites)` and stops incrementing at the maximum, leaving an emptied block standing:

```java
// FoodBiteBlock.eat — the whole counter, abridged
int bites = state.getValue(this.bites);
if (bites < this.getMaxBites()) level.setBlock(pos, state.setValue(this.bites, bites + 1), 3);
```

So the true count is `maxBites - bites`, not `bites + 1`. Sharing `AbstractCountdownFood` would report one serve too many on a fresh dish, and `consume` would compute `0 + 1 > maxBites` on the very first bite and **destroy a whole untouched dish**. `BiteFood` therefore asks the block for `getMaxBites()` — a public accessor — rather than deriving the count from the property's range. This is the general lesson: a counter's *range* is not its *count*, and the two agreeing on four kinds of food is no reason to assume it on a fifth.

#### The cake problem

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

#### Reference values, per serving

Every one is derived by the code above, not chosen by hand:

| Food | per serving | serves | whole block |
|---|---|---|---|
| `minecraft:cake` | 2 | 7 | **14** |
| carrot | 3 | — | 3 |
| bread | 5 | — | 5 |
| steak | 7 | — | 7 |
| golden apple (2 beneficial effects) | 16 | — | 16 |

#### Registration is per class only — no block ids anywhere

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

##### A plate is not a container

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

#### The data map is an override, not a source

```java
// content/attachment/party/core/PartyFood.java
public record PartyFood(int appeal) {
    public PartyFood { appeal = Math.max(0, appeal); }

    @Nullable
    public static PartyFood of(RegistryAccess access, BlockState state) {
        return GLMeta.PARTY_FOOD.get(access, state.getBlockHolder());
    }
}
```

There is deliberately **no** `party_food` data map file shipped. A handler recognises every food we know about, so nothing needs one; the data map is for the two cases code cannot cover.

**Opting a foreign block in.** With no tag, a block from another mod becomes party food by giving it an entry:

```jsonc
// data/gensokyolegacy/data_maps/block/party_food.json
{ "values": { "some_mod:baroque_cake": { "appeal": 20 } } }
```

That entry is also the declaration of "this is food", and the appeal is read from the block's own item by the fallback handler unless overridden — so one line is enough to put a modded dish on the table.

**Correcting a value the heuristic got wrong.** The same file, for a block a handler already recognises but which scores badly: it replaces `perServe`, and the serve multiplier still applies, so overriding a cake sets the value of a *slice* rather than of the whole thing.


### 2.3 The support problem — measured, and the answer was "no change needed"

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

---

## 3. Finding the party area

Everything the party touches — food, seats, guests, the anchor — lives inside one scan box around the block the player clicked. The scan is the unit of "a party".

### 3.1 One scan, three collections

```java
/** Everything a party occupies, found in a single pass over the scan box. */
public record PartyArea(BlockPos anchor, List<BlockPos> food, List<BlockPos> seats, int appeal) {

    public static PartyArea scan(ServerLevel level, BlockPos anchor, int radius) {
        var food = new ArrayList<BlockPos>();
        var seats = new ArrayList<BlockPos>();
        int appeal = 0;
        for (var pos : BlockPos.betweenClosed(anchor, radius)) {
            var here = level.getBlockState(pos);
            if (here.is(GLTagGen.PARTY_SEAT)) seats.add(pos.immutable());
            if (!PartyFoodValue.isFood(level.registryAccess(), here)) continue;
            // food only counts when it is actually on a table
            if (!level.getBlockState(pos.below()).is(GLTagGen.PARTY_TABLE)) continue;
            int worth = PartyFoodValue.appeal(level.registryAccess(), here);
            if (worth <= 0 || !PartyFoodValue.result(here).valid()) continue;   // claimed, but not food
            food.add(pos.immutable());
            // every serve still on it, not one number for the block
            appeal += worth;
        }
        return new PartyArea(anchor, food, seats, appeal);
    }
}
```

Three rules in nine lines, and they are the whole of "block food placed on a table, that is actually food": something must recognise the block as food or a datapack must have opted it in, the block directly below must be in `party_table`, and the block must still have something worth eating on it. A cake on the floor contributes nothing and is never eaten; neither does a decorative block someone tagged by mistake in a previous version of this design.

`BlockPos.betweenClosed(anchor, radius)` at radius 6 is a 13×13×13 box, ~2200 block reads, run **once per send** and once per `/gensokyo party preview`. Never per tick.

Seats are collected in the same pass because they are in the same box; no second scan.

### 3.2 Centre, and the chunk the party belongs to

The party is owned by the chunk containing the **centre of the food**, not the chunk of the block that was clicked. A spread laid across a chunk border puts its centre in one specific chunk, and that chunk owns the party.

```java
/** The mean of the food positions; the chunk containing it owns the party. */
public static BlockPos center(List<BlockPos> food) {
    long x = 0, y = 0, z = 0;
    for (var p : food) { x += p.getX(); y += p.getY(); z += p.getZ(); }
    int n = food.size();
    return new BlockPos((int) Math.floorDiv(x, n), (int) Math.floorDiv(y, n), (int) Math.floorDiv(z, n));
}
```

`floorDiv` rather than `/` so a negative-coordinate spread does not bias the centre toward zero. The mean, not the midpoint of the bounding box, because a spread of one cake and one far-away pie should centre nearer the cake.

With `radius <= 7` the whole scan box spans at most 3×3 chunks, so the set of chunks a party can possibly touch is tiny and bounded.

### 3.3 One party per area, enforced by chunk scan

A second player must not be able to send a letter at a table that is already hosting, and a spread straddling a border must not produce two independent parties over one set of tables. Both are caught by scanning every chunk the spread touches and rejecting if any of them already holds a live party.

```java
/** Every chunk the food touches. A party may not overlap another party's. */
public static ChunkPosRange touchedChunks(List<BlockPos> food) {
    if (food.isEmpty()) return null;
    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    for (var p : food) {
        minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
        minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
        minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
    }
    return ChunkPosRange.ofBlocks(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
}

/** @return the live party overlapping this spread's chunks, or null */
@Nullable
public static PartyAttachment existingParty(ServerLevel level, ChunkPosRange range) {
    PartyAttachment found = null;
    for (var cpos : range.stream().toList()) {
        // false => never force-load: an unloaded chunk cannot be holding a live party
        LevelChunk chunk = level.getChunkSource().getChunk(cpos.x, cpos.z, false);
        if (chunk == null) continue;
        var party = GLMeta.PARTY.type().getOrCreate(chunk);
        if (party.active()) found = party;
    }
    return found;
}
```

`ChunkPosRange.ofBlocks` and `.stream()` already exist (`content/attachment/area/ChunkPosRange.java`) and are exactly the shape needed. `getChunk(..., false)` returns `null` for an unloaded chunk instead of loading it, which is both correct and desirable: a party only exists while its chunk is loaded, so an unloaded chunk is by definition not hosting one.

This rule subsumes the single-anchor check. Two parties can never overlap, because any second spread sharing a table necessarily shares a chunk with the first party's centre chunk.

---

## 4. Party state

A `LevelChunk` capability, alongside the existing `STRUCTURE` and `CHUNK_EFFECT` chunk capabilities:

```java
// init/registrate/GLMeta.java
public static final AttVal.CapVal<LevelChunk, PartyAttachment> PARTY =
        ATT.entity("party_data", PartyAttachment.class, PartyAttachment::new, LevelChunk.class, e -> true);
```

```java
@SerialClass
public class PartyAttachment {

    public enum Phase { IDLE, GATHERING, FEASTING, CLOSING }

    @SerialField private UUID host;
    @SerialField private BlockPos anchor;                                  // the table clicked
    @SerialField private Phase phase = Phase.IDLE;
    @SerialField private long startedAt;
    @SerialField private final ArrayList<EntityType<?>> roster = new ArrayList<>();  // frozen at send
    @SerialField private final ArrayList<UUID> guests = new ArrayList<>();            // live clones
    @SerialField private int appealBudget;      // snapshot of the spread at send
    @SerialField private int appealServed;      // running total actually consumed

    /** Whether this chunk already hosts a party; the send-time overlap guard reads this. */
    public boolean active() { return phase != Phase.IDLE; }
}
```

The party lives in the chunk holding `center(food)` (§3.2), so:

- it is **chunk-scoped for free** — unload the chunk and the party freezes rather than corrupting;
- it **survives host logout**, because the data belongs to the chunk, not the player;
- it is dropped with the chunk like everything else in it, so there is no stale state to garbage-collect.

**The roster is frozen when the letter is sent and never recomputed.** Re-running the knapsack on a timer would let a guest who wandered in mid-banquet change the guest list under everyone else, and would make the outcome depend on tick order. Frozen, the roster is a pure function of (reputation, food on the table, seats available) at the moment of sending — which is exactly what makes `/gensokyo party preview` able to predict a real send perfectly.

**Ticking.** The party advances its phase from the host's chunk tick at 5 Hz via the capability `tick` hook, the same one `CharacterAttachment` uses for daily decay (`CharacterAttachment.java:22`). Nothing in a party needs 20 Hz resolution: guests are walking and eating on multi-second timescales.

Phase transitions:

| From | To | Condition |
|---|---|---|
| `IDLE` | `GATHERING` | letter sent, roster frozen, clones spawned |
| `GATHERING` | `FEASTING` | every guest has arrived, or `GATHER_TIMEOUT` elapsed |
| `FEASTING` | `CLOSING` | every guest satisfied, or no food left, or `FEAST_TIMEOUT` |
| `CLOSING` | `IDLE` | every guest dismissed |
| any | `IDLE` | host left the area, or all guests died |

Aborting to `IDLE` always leaves the remaining food untouched — an interrupted party wastes the guests' time, not the player's food.

---

## 5. The algorithm

Three quantities, all pure functions over plain values.

### 5.1 Attractiveness of the spread

`PartyArea.appeal` from §3.1: the sum of `PartyFoodValue.appeal` over every block that is food and is sitting on a `party_table` inside the scan box, where each block's contribution is **serves × per-serving appeal** (§2.2). Simple additive, no multipliers — the player controls it directly by choosing what to lay out, and the per-serving number comes from the food's real properties rather than a table of hand-picked values.

Because it is counted in serves, the budget a spread offers is the total number of servings on the table, and a partly-eaten dish is worth proportionally less. That is what makes one cake a meal for several guests instead of a single indivisible unit.

### 5.2 Guest weight

Each character is scored on the two axes the design calls for: how much the spread appeals to her, and how much the player has earned her company.

```java
/**
 * How much this character wants to come, 0 .. ~(1 + repBonus).
 *
 * @param appealFactor 0 below minAppeal, 1 at or above maxAppeal, linear between:
 *                     a spread too plain for her is worth nothing to her.
 * @param repFactor    1.0 at exactly minReputation, rising to 1.0 + repBonus at
 *                     maxReputation: being genuinely well liked turns a maybe into a yes.
 */
public static double weight(PartyTaste taste, int spreadAppeal, int reputation) {
    if (reputation < taste.minReputation()) return 0;      // she declines outright
    double appealFactor = ramp(spreadAppeal, taste.minAppeal(), taste.maxAppeal());
    double repFactor = 1.0 + taste.repBonus()
            * ramp(reputation, taste.minReputation(), taste.maxReputation());
    return appealFactor * repFactor;
}

/** 0 at or below lo, 1 at or above hi, linear in between. */
public static double ramp(int value, int lo, int hi) {
    if (value >= hi) return 1.0;
    if (value <= lo || hi <= lo) return 0.0;
    return (value - lo) / (double) (hi - lo);
}
```

`PartyTaste` is a new entity-type data map, mirroring the existing `GIFT_PREFERENCE`:

```java
public static final DataMapReg<EntityType<?>, PartyTaste> PARTY_TASTE =
        GensokyoLegacy.REG.dataMap("party_taste", Registries.ENTITY_TYPE, PartyTaste.class);

/**
 * @param minReputation below this she declines, whatever is on the table
 * @param maxReputation reputation at which the reputation bonus is fully earned
 * @param repBonus      extra weight at maxReputation; 0.5 means up to 1.5x
 * @param minAppeal     spread appeal below which she is not interested
 * @param maxAppeal     spread appeal at which she is fully interested
 * @param fulfillment   appeal she must be served before she counts as satisfied
 */
public record PartyTaste(
        int minReputation, int maxReputation, double repBonus,
        int minAppeal, int maxAppeal, int fulfillment
) {
    public PartyTaste {
        maxReputation = Math.max(maxReputation, minReputation);
        maxAppeal = Math.max(maxAppeal, minAppeal);
        repBonus = Math.max(0, repBonus);
        fulfillment = Math.max(1, fulfillment);
    }
}
```

The compact constructor clamps the inverted ranges so a datapack typo degrades into "no bonus" rather than a negative weight.

**Every character is party-eligible.** There is no whitelist. A character with no authored `party_taste` entry gets `PartyTaste.DEFAULT`:

```java
public static final PartyTaste DEFAULT = new PartyTaste(0, 200, 0.25, 0, 24, 8);

public static PartyTaste of(RegistryAccess access, EntityType<?> type) {
    var taste = GLMeta.PARTY_TASTE.get(access, type.builtInRegistryHolder());
    return taste == null ? DEFAULT : taste;
}
```

`DEFAULT` has **no reputation floor**, so even someone the player has just met can be fed a party. The data map only makes a character pickier or hungrier than that — it is not a gate. This is a deliberate reversal of the earlier design, which treated a missing entry as "not party-eligible" and so left bosses, fairies and unwritten characters permanently locked out until someone authored each one by hand.

The `DEFAULT` numbers are sized against §2.2's derived appeal, where a whole vanilla cake is worth 14: `maxAppeal` 24 is under two cakes, and `fulfillment` 8 is just over half of one.

Shipped values, in `data/gensokyolegacy/data_maps/entity_type/party_taste.json`:

| Character | minRep | maxRep | repBonus | minAppeal | maxAppeal | fulfillment |
|---|---|---|---|---|---|---|
| `cirno` | 50 | 150 | 0.3 | 4 | 20 | 6 |
| `hakurei_reimu` | 100 | 200 | 0.3 | 8 | 32 | 8 |
| `kirisame_marisa` | 100 | 200 | 0.3 | 8 | 32 | 8 |
| `morichika_rinnosuke` | 100 | 200 | 0.3 | 10 | 32 | 8 |
| `alice_margatroid` | 150 | 250 | 0.15 | 12 | 40 | 10 |
| `izayoi_sakuya` | 150 | 250 | 0.15 | 16 | 48 | 12 |
| *(everyone else)* | **0** | 200 | 0.25 | **0** | 24 | 8 |

Sakuya has no `character_config` and no home — she is guest-only by design (`GLEntities.java:55`) — which makes her the natural first test case for the whole system, since a party guest needs no home binding either way.

Fulfillments are 6/8/8/8/10/12 rather than the original 16/20/20/20/24/24. They were retuned once appeal stopped being hand-authored and became derived from real food properties: a whole vanilla cake is worth 14, not the 8 originally assumed, and a modded feast lands somewhere above that. A fulfillment of 16 against a 14-appeal cake would have left the no-mods-installed path unable to satisfy anyone at all.

They are also now counted in **serves**, not blocks. A guest is satisfied once the appeal she has eaten reaches her fulfillment, and eating is one serve at a time, so a fulfillment of 6 against a 2-per-slice cake means three slices — she leaves the other four for whoever is next.

### 5.3 Fulfillment

A guest tracks the appeal she has eaten and is satisfied when it reaches her `fulfillment`:

```java
// PartyModule
@SerialField private int eaten = 0;

/** One serve eaten: her tally, and the party's shared total. */
public void eat(int serveAppeal, PartyAttachment party) {
    eaten += serveAppeal;
    party.serve(serveAppeal);
}

public boolean satisfied(PartyTaste taste) { return eaten >= taste.fulfillment(); }

/** 0..1 — how well she was actually served, relative to what she wanted. */
public double fulfillmentRatio(PartyTaste taste) {
    return Math.min(1.0, eaten / (double) Math.max(1, taste.fulfillment()));
}
```

The knapsack in §5.4 guarantees the *planned* total fits the spread, but food can still run short — the player can break a dish mid-party, or two guests can converge on the last serve. Fulfillment is therefore checked at runtime, not assumed from the plan. A guest who is under-served still eats what is there and still leaves a gift, but a reduced one (§6.4). This is what makes "guests consume all the food you prepare" mean something: a thin spread produces thin gifts.

### 5.4 Who comes — the knapsack

Choose the subset of eligible characters maximising total weight subject to total fulfillment ≤ available appeal. This is exactly **0/1 knapsack**, and it is what lets both requirements hold at once: *how much food you provide* sets the budget, *who you have good reputation with* sets the weights.

```java
public record Candidate(EntityType<?> type, double weight, int fulfillment, int reputation) { }

/** Float-tie tolerance; keeps the roster stable across runs. */
private static final double EPS = 1e-9;

/**
 * Pick the guests. Maximises total weight subject to sum(fulfillment) <= appeal,
 * then truncates to the number of available seats.
 *
 * Deterministic: a 1e-9 epsilon makes float ties resolve identically every run, so
 * /gensokyo party preview predicts a real send exactly.
 *
 * @return chosen characters, highest weight first
 */
public static List<EntityType<?>> select(List<Candidate> pool, int appeal, int seats) {
    if (seats <= 0 || appeal <= 0) return List.of();
    // weight 0 means she declines outright: drop her so she cannot occupy budget
    // or appear in the roster. fulfillment > appeal likewise - she could never be
    // satisfied, so inviting her only to leave hungry is worse than not inviting her.
    var items = pool.stream()
            .filter(e -> e.weight() > 0 && e.fulfillment() <= appeal)
            .sorted(Comparator.comparingDouble(Candidate::weight).reversed())   // stable input order
            .toList();

    int n = items.size();
    double[] best = new double[appeal + 1];
    boolean[] took = new boolean[n][appeal + 1];

    for (int i = 0; i < n; i++) {
        int cost = items.get(i).fulfillment();
        // descending a => 0/1, each character can be invited at most once
        for (int a = appeal; a >= cost; a--) {
            double cand = best[a - cost] + items.get(i).weight();
            if (cand > best[a] + EPS) {
                best[a] = cand;
                took[i][a] = true;
            }
        }
    }

    // Walk the decisions back out. This must run BACKWARDS: took[i][a] records
    // whether item i improved best[a] using items 0..i, so the chain is only
    // coherent from the last item down. A forward walk silently under-fills the
    // roster, because the last item to touch best[a] is not the first one read.
    var chosen = new ArrayList<EntityType<?>>();
    for (int i = n - 1, a = appeal; i >= 0; i--) {
        if (took[i][a]) {
            chosen.add(items.get(i).type());
            a -= items.get(i).fulfillment();
        }
    }
    Collections.reverse(chosen);   // items are weight-descending, restore that order
    return chosen.stream().limit(seats).toList();
}
```

Implementation notes:

- **Descending `a`** is what makes this 0/1 rather than unbounded — the same character cannot be invited twice.
- **Candidates are sorted by weight, descending, before the DP**, and the reconstruction reverses its output. Sorting does not bias the result — the DP explores every subset regardless of input order — it only makes the final roster come out in weight order, so guests arrive best-first and the seat truncation drops the least valuable arrivals.
- **The reconstruction walks backwards.** `took[i][a]` records whether item `i` improved `best[a]` *using items 0..i*, so the decision chain is only coherent from the last item down. A forward walk compiles and runs, and quietly returns a roster one-or-two characters short of optimal — worth a comment in the real code, because the wrong version looks correct in every other respect.
- **Two filters run before the DP.** `weight() <= 0` means she declines outright and must not occupy budget or appear in the roster. `fulfillment > appeal` means she could never be satisfied even by eating everything, so inviting her would only produce a guest who leaves hungry — worse than not inviting her.
- `n <= 14` entity types and `appeal` in the low hundreds, so this is a few thousand iterations, once per send. A greedy sort by weight-per-appeal would be simpler but wrong: it cannot see that one greedy pick crowds out two better ones.
- **Seats are applied after the DP** by truncating the weight-ordered list. Folding the seat count into the DP would need a second dimension for no real gain, because a heavier guest is always at least as good a use of a seat as a lighter one.

### 5.5 Worked example

Vanilla only, so every appeal here is checkable against §2.2's derivation: one `minecraft:cake` is 7 slices of 2 = **14**. Reputation 180 across the board, comfortably above everyone's floor.

Per-character weights at each spread, reputation held at 180 (bold = on the roster the knapsack chose):

| Character | fulfillment | 1 cake (14) | 2 cakes (28) | 3 cakes (42) | 4 cakes (56) |
|---|---|---|---|---|---|
| `cirno` | 6 | **0.812** | **1.300** | **1.300** | **1.300** |
| `hakurei_reimu` | 8 | **0.310** | **1.033** | **1.240** | **1.240** |
| `kirisame_marisa` | 8 | 0.310 | **1.033** | **1.240** | **1.240** |
| `morichika_rinnosuke` | 8 | 0.225 | 1.015 | **1.240** | **1.240** |
| `alice_margatroid` | 10 | 0.075 | 0.597 | **1.045** | **1.045** |
| `izayoi_sakuya` | 12 | 0.000 | 0.392 | 0.849 | **1.045** |

Sakuya's weight is 0 against a single cake: 14 appeal is below her `minAppeal` of 16, so she is not interested no matter how well the player knows her. That is the `minAppeal` cutoff doing its job.

What the knapsack picks, at each spread:

| Spread | Appeal | Roster | Appeal spent | Seats needed |
|---|---|---|---|---|
| 1 cake | 14 | Cirno, Reimu | 14 | 2 |
| 2 cakes | 28 | Cirno, Reimu, Marisa | 22 | 3 |
| 3 cakes | 42 | Cirno, Reimu, Marisa, Morichika, Alice | 40 | 5 |
| 4 cakes | 56 | all six | 52 | 6 |

**No other mods installed is a working party.** The progression is 2 → 3 → 5 → 6 guests for 1 → 2 → 3 → 4 cakes, and it is the whole system running off one vanilla block.

Three things worth noting. At 14 appeal the budget is spent to the last unit — Cirno's 6 and Reimu's 8 come to exactly 14 — so a single cake is a complete meal for two and leaves nothing over. At 42 appeal Morichika's weight (1.240) equals Reimu's and Marisa', and his fulfillment is the same 8, so his place on the roster is a tie broken by the DP's stable weight-descending input order rather than a judgement that he is a better guest. And at 56 appeal the budget covers all six fulfillments — 6+8+8+8+10+12 = 52 — with 4 left over, so **four cakes seats the entire cast** and one Farmer's Delight feast is then enough to make the seat count the only limit.

Because eating is per-serving, those fulfillments are also slice counts at this scale: Cirno's 6 is three slices of cake, Reimu's 8 is four, Sakuya's 12 is six of the seven a single cake holds. The last guest waits while the earlier ones take their slices, and those slices simply stop existing as they are eaten.

The guest count tracks the food, and who shows up tracks reputation — both fall out of the formulation rather than being special-cased anywhere.

---

## 6. The flow

### 6.1 The invitation letter

A paper item. "Use paper texture for now" — the model points straight at vanilla's, so swapping in a real texture later is a one-line change.

```java
// init/registrate/GLItems.java
INVITATION_LETTER = reg.item("invitation_letter", InvitationLetterItem::new)
        .model((ctx, pvd) -> pvd.generated(ctx, pvd.mcLoc("item/paper")))
        .properties(p -> p.stacksTo(16))
        .lang("Invitation Letter").register();
```

Right-click a `party_table`:

```java
@Override
public InteractionResultHolder<ItemStack> useOn(UseOnContext ctx) {
    var level = ctx.getLevel();
    var pos = ctx.getBlockPos();
    if (!level.getBlockState(pos).is(GLTagGen.PARTY_TABLE)) {
        return InteractionResultHolder.pass(ctx.getItemInHand());
    }
    if (!(ctx.getPlayer() instanceof ServerPlayer sp)) {
        return InteractionResultHolder.success(ctx.getItemInHand());
    }
    return PartySend.send(sp, pos, ctx.getItemInHand());
}
```

`useOn` rather than `use`, so the letter must be aimed at a table and cannot be fired into the air. Mirrors how `TableDecoBlock` and `CoverableImpl` gate their interactions.

### 6.2 `PartySend.send` — the single entry point

In order:

1. `PartyArea.scan(level, clicked, GLModConfig.SERVER.partyScanRadius)` (§3.1).
2. **Reject** if `food.isEmpty()` — "there is no food on the table".
3. **Reject** if `seats.isEmpty()` — chairs are a hard requirement, so no chairs means no party at all, and the player is told to put some out.
4. **Reject** if `existingParty(level, touchedChunks(food))` is non-`IDLE` (§3.3) — "there is already a party here".
5. Build the candidate pool: every entity type in `Registries.ENTITY_TYPE` that has a `party_taste` entry, scored by `PartyMath.weight` against the player's reputation read via `CharDataHolder.getUnbounded(sp, type).data().reputation`.
6. `PartyPlanner.select(pool, area.appeal(), area.seats().size())`.
7. **Reject** if the roster is empty — the reason is the highest-scoring near-miss, so the player gets "Reimu would come, but the spread is too plain" rather than a bare failure.
8. Freeze the roster and counters into `GLMeta.PARTY` on the chunk holding `center(food)`, set phase `GATHERING`.
9. Consume one letter (unless creative) and play the send sound.

Every rejection happens **before** the letter is consumed, so a refused send costs nothing.

### 6.3 Guests

Spawned as temporary clones, exactly as `VisitScheduler.spawn` does (`VisitScheduler.java:71`), and for the same reason: **do not touch `HomeModule`**. With no home binding the clone never receives a `HOME` memory, so the entire sleep and bed subsystem stays unreachable, and the real resident keeps living her ordinary day at home.

```java
public static @Nullable YoukaiEntity spawn(ServerLevel sl, ServerPlayer host, BlockPos table, EntityType<?> type) {
    if (!(type.create(sl) instanceof YoukaiEntity youkai)) return null;
    youkai.setPos(table.getCenter().offset(0, 1, 0));
    youkai.initSpellCard();
    youkai.restrictTo(table.getCenter(), GLModConfig.SERVER.partyGuestRadius.get());
    youkai.getModule(PartyModule.class).ifPresent(e -> e.arrive(host.getUUID(), table));
    sl.addFreshEntity(youkai);
    return youkai;
}
```

`PartyModule` must be in **every** character's module list. The base `YoukaiEntity.createModules()` (`YoukaiEntity.java:224`) is overridden by `CirnoEntity` (`:139`) and `AliceEntity` (`:72`), so the module goes into all three lists, not just the base.

### 6.4 Gifts

Reuse the existing gift items. `GLMeta.GIFT_DATA` already scores favour per item and `GLMeta.GIFT_PREFERENCE` weights categories per character, so a guest picks a gift the same way `GiftModule` scores one handed to her.

```java
private void giveGift(ServerLevel sl, PartyTaste taste) {
    ItemStack gift = giftRoll(sl, self.getType());       // weighted pick from gift_data items
    // a guest who was not fully served gives a smaller gift
    int count = fulfilledFully(taste) ? gift.getCount() : Math.max(1, gift.getCount() / 2);
    Block.popResource(sl, table.above(), gift.copyWithCount(count));
}
```

`Block.popResource` at the table is the in-tree idiom for putting an item down in the world (`MarisaTaskUtil.java:94`, `ShelfBlockEntity`). That matches "thrown on table for now" exactly, and because nothing is granted into an inventory there is nothing to unwind when the real gift item is designed.

**There is no reputation change and no cooldown.** The food is the entire cost and the gifts are the entire return. Reputation is read once, at send, to decide the roster, and is never modified by the party itself. The letter names no character, so there is nothing to prevent being re-sent — repeating a successful spread simply works, and repeating an unsuccessful one fails the same way.

### 6.5 The task

`YoukaiPartyTask` follows `YoukaiRestockShelfTask`'s two-phase shape — walk, then act on a deadline.

```
start()      → claim the nearest free seat; WALK_TARGET + LOOK_TARGET at the seat
canStillUse  → arrived?  HomeSearchUtil.setSitting(...)      // no-op on a non-ISeatableBlock
               then: nearest unclaimed food → WALK_TARGET
               arrived at food?  swing + USE_MAINHAND + eating sound, wait EAT_TICKS,
                                  take ONE serve (PartyFoodValue.consume),
                                  eaten += that serve's appeal, pick the next one
               satisfied()? → giveGift(), dismiss()
               no food left? → dismiss()
```

`HomeSearchUtil.setSitting` (`HomeSearchUtil.java:75`) already handles claiming a free seat, checking `ChairEntity` occupancy and yielding to players, and is a silent no-op when the block is not `ISeatableBlock` — so a third-party chair from `party_seat` simply has the guest stand beside it, with no special case.

**The task must never return `false` from `canStillUse` to change phase.** One `Behavior` instance drives seat → food → eat, so the phase lives in a field on the task; `false` calls `doStop` → `stop()`, which wipes it, and the brain re-runs the exclusive behavior next tick with `start()` resetting to `SEEK_SEAT`. The guest would sit down and immediately begin hunting for another seat, forever, never reaching the food. Every phase change therefore keeps returning `true`, and `false` is reserved for actually finishing — party over, out of time, or nothing left to serve.

This is why `YoukaiPartyTask` cannot copy `MarisaForageTask`'s "arrive, act, return false" shape. The forage task is single-action and holds no state between runs, so stopping is free. A multi-phase task must keep the instance alive across its own transitions, and the failure mode is silent — the entity looks perfectly healthy, sits down, stands up, sits down again, and nothing is ever eaten.

**Food contention needs no locking.** On arrival a guest re-validates that the block is still food and sitting on a `party_table`; if another guest took the last serve first it is gone and she picks again. With per-serving consumption this is *more* forgiving than it was: two guests walking to the same cake no longer collide destructively, since each finds a serve still on it. Worst case is a wasted walk.

**A serve is taken when eating starts and credited when it finishes.** `PartyFoodValue.consume` is called at `reach()`, and `pendingAppeal` — read *before* the consume, because the appeal of a serve is a property of the state it was taken from — carries it across the `EAT_TICKS` window so the tally and the party's served total move together. `stop()` credits a serve that was taken but never banked, so a party ending or a chunk unloading mid-bite cannot silently swallow food the guests already ate.

**There is no eating animation.** `YoukaiAnim` has no EAT or DRINK slot, and nothing in the codebase calls `startUsingItem` on a character — `FeedModule` only plays a sound and heart particles. The task uses `entity.swing(MAIN_HAND)` + `YoukaiAnim.USE_MAINHAND` + the vanilla eating sound, which is exactly what `MarisaForageTask` does for foraging (`MarisaForageTask.java:60`). Adding a real `eat` clip to every character model is follow-up art work, not a system blocker.

---

## 7. Brain wiring

```java
// init/registrate/GLBrains.java
public static final Val<Activity> PARTY = ACTIVITIES.reg("party", () -> new Activity("party"));
public static final Val<MemoryModuleType<GlobalPos>> MEM_PARTY =
        MEMORIES.reg("party", () -> new MemoryModuleType<>(Optional.empty()));
```

Added once in `SmartYoukaiEntity.constructTaskBoard` (`SmartYoukaiEntity.java:81`), which every character subclass reaches through `super.constructTaskBoard(board)` — so one edit covers all fourteen characters.

**Priority 200**, above `VISITING` (150) and `TALK` (100), below the scheduled activities. `TaskBoard.buildBrain` (`TaskBoard.java:150`) walks `[CORE] + prioritized + scheduled` and injects each prioritized activity's memory as `VALUE_ABSENT` into every *later* entry, so with `PARTY` at 200:

- `TALK` is walked **before** `PARTY`, so its conditions never mention `MEM_PARTY` — **a party guest can still talk and trade**;
- `PARTY` requires `MEM_TALK` absent, so eating never interrupts a conversation already in progress;
- every scheduled activity requires `MEM_PARTY` absent — **a guest never sleeps and never goes home**, the same guarantee `VISITING` gives a house guest, one rung up the board.

---

## 8. Files

**New**

| File | Role |
|---|---|
| `content/attachment/party/core/PartyMath.java` | `ramp`, `weight` — pure |
| `content/attachment/party/core/PartyArea.java` | one scan: food, seats, appeal, centre, touched chunks |
| `content/attachment/party/core/PartyPlanner.java` | candidate pool + knapsack + seat cap — pure |
| `content/attachment/party/core/PartyTaste.java` | entity-type data map record + `DEFAULT` for every unwritten character |
| `content/attachment/party/core/PartyFood.java` | override-only block data map record |
| `content/attachment/party/core/PartyFoodValue.java` | per-serving appeal derivation, totals, `consume` (§2.2) |
| `content/attachment/party/core/PartyFoodHandler.java` | the handler list and the `claims` test that replaces the food tag (§2.2) |
| `content/attachment/party/food/BlockFoodHandler.java` | the serves / food / consume interface (§2.2) |
| `content/attachment/party/food/AbstractCountdownFood.java` | shared serve counting and stepping, both directions |
| `content/attachment/party/food/CakeFood.java` | slices: a permuted `*_slice` item, else vanilla's numbers |
| `content/attachment/party/food/FeastFood.java` | FD feast: 4 servings down, serving item from the block |
| `content/attachment/party/food/PieFood.java` | FD pie: 4 bites up, slice from the block |
| `content/attachment/party/food/PotFood.java` | YSF pot: 2 or 4 serves down, permuted item or the block's field |
| `content/attachment/party/food/SaucerFood.java` | YSF saucer dish: one serve, food on the block's own item |
| `content/attachment/party/food/BiteFood.java` | KC dish: `maxBites - bites`, food from `FoodBiteRegistry` |
| `content/attachment/party/food/PlateFood.java` | KC plate/platter: `servings` down, food from `PlateRegistry`'s serving items |
| `content/attachment/party/food/FoodResult.java` | serves + properties + consume, and the valid/not-valid rule |
| `content/attachment/party/food/FoodResultCache.java` | resolves each state once; invalidated on datapack sync |
| `content/attachment/party/food/Permutations.java` | finds the item form of a block food by id suffix |
| `content/attachment/party/food/BlockItemFood.java` | one serve from the block's own item; the catch-all |
| `content/attachment/party/food/Servings.java` | shared read of one serving's `FoodProperties`, and the invalid-food rule |
| `init/registrate/BlockFoods.java` | which block classes are known, gated per mod (§2.2) |
| `content/attachment/party/core/PartyAttachment.java` | chunk capability, phase machine, roster |
| `content/attachment/party/core/PartySend.java` | the single send entry point + rejections |
| `content/attachment/party/core/PartyGift.java` | what a guest leaves, scaled by §5.3 |
| `content/attachment/party/core/PartyTicker.java` | level-tick driver, active-chunk registry |
| `content/attachment/party/item/InvitationLetterItem.java` | the letter |
| `content/attachment/party/item/PartyMessage.java` | one place that turns an outcome into player-facing text |
| `content/entity/module/PartyModule.java` | per-guest state, `arrive`/`tickServer`/`leave` |
| `content/entity/behavior/task/party/YoukaiPartyTask.java` | walk → sit → eat → gift |
| `content/rpg/condition/PartyCondition.java` | `{"type":"gensokyolegacy:party"}`, `invert` for home-bound |

**Modified**

| File | Change |
|---|---|
| `init/registrate/GLMeta.java` | `PARTY`, `PARTY_FOOD`, `PARTY_TASTE` |
| `init/registrate/GLBrains.java` | `PARTY`, `MEM_PARTY` |
| `init/registrate/GLItems.java` | `INVITATION_LETTER`, calls `BlockFoods.register()` |

| `content/entity/youkai/YoukaiEntity.java` | `PartyModule` in `createModules()`, `isPartying()` |
| `content/entity/youkai/YoukaiFlags.java` | append `PARTYING` (ordinal is the synced bit index) |
| `content/entity/youkai/SmartYoukaiEntity.java` | `PARTY(200)` + `YoukaiPartyTask` |
| `content/entity/characters/fairy/CirnoEntity.java` | `PartyModule` in its `createModules()` override |
| `content/entity/characters/magician/AliceEntity.java` | same |
| `init/registrate/GLEntities.java` | `TASTE_*` constants, applied via `.dataMap(GLMeta.PARTY_TASTE.reg(), …)` |
| `content/rpg/core/CodecRegistry.java` | `PARTY` condition |
| `content/rpg/core/ServerCharacterDialogManager.java` | suppress quest `START` while partying, as for visiting |
| `content/rpg/action/StartQuestAction.java` | same guard, for dialog-driven starts |
| `content/command/GLCommands.java` | `/gensokyo party` |
| `content/item/debug/PartyWand.java` | the debug wand: hover, eat, simulate (§9.1) |
| `content/item/debug/PartyWandData.java` | the simulated result, as the wand's data component payload |
| `content/item/debug/PartyWandComponent.java` | the `DataComponentType`, registered on the mod bus |
| `init/data/GLModConfig.java` | `partyScanRadius`, `partyGuestRadius` |
| `init/data/GLLang.java` | `Party` enum, wired into `genLang`'s group array |
| `init/data/GLTagGen.java` | `PARTY_TABLE`, `PARTY_SEAT` and `genPartyTag` (§2.1) |
| `event/GLMetaEventHandlers.java` | `PartyTicker.tick` on level tick, `clear` on level unload |
| `build.gradle` | Farmer's Delight + Youkai's Feasts as `compileOnly` from the `libs/` flatDir |

**New resources**

| File | Change |
|---|---|
| `src/generated/resources/data/gensokyolegacy/tags/block/party_{table,seat}.json` | generated by `runData`, §2.1 |
| `src/generated/resources/data/gensokyolegacy/data_maps/entity_type/party_taste.json` | generated |
| `src/generated/resources/assets/gensokyolegacy/models/item/invitation_letter.json` | generated |
| `src/test/resources/gensokyolegacy/lang/zh_cn/party.json` | zh_cn strings, merged by `LangFileOrganizer` |

There is **no** `data_maps/block/party_food.json` — the data map is an override and nothing in the mod needs one (§2.2).

The `large_table` support change is a behavioural change to a shipped block and lands in its own commit, separate from the party feature.

---

## 9. Command

```
/gensokyo party preview    spread appeal, seat count, the full candidate table with
                           weights, and the chosen roster — consumes nothing
/gensokyo party send       force a send at the table being looked at
/gensokyo party end        dismiss every guest; leftover food is left untouched
/gensokyo party info       current phase, roster, served vs budget, guest states
```

`preview` is the tuning tool and the reason the planner is pure: it calls the identical `PartyPlanner.select`, so what it prints is exactly what `send` will do. Without it, tuning `party_taste` numbers through a 30-second party with a letter to spend each attempt is miserable. `SUGGEST_PARTY_ELIGIBLE` is built off `party_taste`, mirroring how `SUGGEST_VISITORS` reads `StructureConfig.visitors()`.

### 9.1 The debug wand

`party_wand` is the other tuning tool, and it works differently from `preview` on purpose: `preview` prints, the wand **holds** its answer on the item.

| Action | Runs on | Shows |
|---|---|---|
| hover a food block | client | serves, hunger, saturation, attraction, serves-to-satisfy |
| click a food block | server | — takes one serving, as a guest would |
| click a party table | server | bound position, table/seat/food counts, attraction, fulfillment needed, every candidate with her weight |

The hover readout is computed **client-side** from the block state and the item registry, both of which the client already holds. That is not laziness: a live readout re-asks every frame, and a server round trip per frame — or a packet per block looked at — would be paid for numbers that cannot disagree with the client's own copy.

The party simulation is the opposite case. Scoring characters by reputation needs server state, so the click runs the identical `PartySend.plan` a real send calls before it commits — no party is opened, no guest spawned, no letter spent — and the answer is written to an item data component:

```java
public record PartyWandData(BlockPos bound, int tables, int seats, int foods,
        int appeal, int fulfillment, List<Candidate> candidates) { ... }
```

A **component** rather than a packet or a chat message, because vanilla already keeps item components in sync to the client; the overlay reads it back whenever the wand is held, with no machinery of its own.

Two details worth knowing. The result is a snapshot of a party that was only simulated — the food is being eaten while the wand sits in a hand — so the overlay flags a reading taken more than 8 blocks from its table as possibly stale. And `PartyArea` collects only food and seats, so the wand counts tables with its own scan; folding that into the area would mean changing `PartyArea`'s record and every caller of it, for a debug-only number.

---

## 10. Known limits

- **`tea_table` and `magic_table` cannot host parties.** Deliberate (§2.3). A small indoor party needs a second pass.
- **Farmer's Delight and Youkai's Feasts are `compileOnly` in the dev build.** Their jars sit in the `libs/` flatDir and resolve for compilation, but neither is loaded at runtime. Adding either as an `implementation` dependency puts a second copy of the L2 libraries on the module path and the dev launcher dies during class-layer construction with a `java.lang.module.ResolutionException` about split packages — before any mod code runs, so no amount of code changes avoids it. Consequence for testing: a dev run has **no modded party food**, only `minecraft:cake`, and the `required: false` tag entries resolve to nothing. The party is fully playable and testable that way; testing the FD/YSF foods needs a real instance with the mods installed.
- **A malformed id in a tag provider hangs `runData`, silently** (§2.1). `GensokyoLegacy.loc()` namespaces a bare path with our modid, so passing it a foreign id produces a path containing a colon; the provider then never returns, with no error and no stack trace, and it looks exactly like a deadlock in Registrate. Foreign ids must go through `ResourceLocation.parse`.
- **`YSF_WOODS` is a hand-kept list in `GLTagGen`.** It exists because a tag provider must not touch `GLDecoBlocks`, and it must be kept in step with `WoodType` by hand.
- **A party can only be sent on a `party_table`,** so the letter is aimed at the table rather than at the food. Clicking a food block directly does nothing.
- **Reputation is sampled once, at send.** Reputation earned during the party does not upgrade the roster and reputation lost during it evicts nobody. A guest who turns hostile mid-party keeps eating. A consequence of freezing the roster, and the reason `/gensokyo party preview` can be exact.
- **Guest eating is not interruptible.** If the player breaks the food out from under a guest she re-validates and picks again, but she cannot be redirected to a different dish.
- **Chunk unload freezes the party mid-feast.** Guests stop ticking with the chunk and resume on reload. Acceptable — the same behaviour as every other entity in the chunk — but a party in a chunk the player walks away from simply pauses.
- **A guest belongs to the chunk that owns the party, not the chunk she stands in.** Both the anchor table and the food centre lie inside the scan box, so a party routinely straddles a chunk border. Guests therefore carry the owning `ChunkPos` explicitly and look the party up by that — never by `new ChunkPos(table)`, which silently misses and makes her dismiss herself on her first tick. Same reasoning behind `/gensokyo party info|end` searching the neighbourhood through `PartyAttachment.near` instead of one chunk.
- **A guest claims one seat per party and never gives it back.** `HomeSearchUtil.setSitting` is a one-shot call with no un-sit counterpart, so she stays parked on the chair for the rest of the visit. Fine — she is a party guest and has nowhere else to be — but seat reassignment after someone leaves is not implemented.
- **No cross-dimension concern.** A guest clone spawns in the host's level and `party_taste` is dimension-independent, so any character may be invited anywhere. Noted only so it is not rediscovered as a bug later.
- **`en_ud` lang variants** regenerate with the rest; zh_cn entries go under `src/test/resources/gensokyolegacy/lang/zh_cn/` and the `ResourceOrganizer` main is rerun afterwards.

---

## 11. Resolved questions

Owner answers, 2026-10-06:

1. **`tea_table` / `magic_table`** — excluded from the feature. `party_table` is built from `LARGE_TABLE` only, and neither block is modified. Note that they needed no support workaround regardless: `large_table` already passes both survival checks unmodified (§2.3).
2. **Overlapping parties** — rejected by chunk scan. The party's owning chunk is the one holding the centre of the detected food; every chunk the food touches is scanned for an existing live party before a send is allowed.
3. **Chairs are a hard requirement** — zero seats rejects the send outright. A new `party_seat` tag carries our chairs and stools plus optional third-party seats; seating itself reuses `HomeSearchUtil.setSitting`, which no-ops on blocks that are not `ISeatableBlock`.
4. **Cost** — food only. No cooldown, no reputation gain or loss. Gifts are the whole return, scaled down for under-served guests.
5. **No letter cooldown** — the letter names no character, so there is nothing to rate-limit; a repeated send simply re-runs the same deterministic check.
6. **Cross-dimension guests** — no issue.
