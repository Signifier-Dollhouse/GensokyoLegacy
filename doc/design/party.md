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

**Only a character with a `party_taste` entry can come.** There is no separate whitelist: the data map is the eligibility list, so a character nobody has written down never attends (§5.2). Among those who are eligible, what decides who shows up is how good the spread is and how well the player knows them.

```
lay out block food on party tables  +  put out seats
        ↓
right-click a table with the invitation letter  →  send
        ↓
letter consumed; spread appeal measured; candidates scored (§5)
        ↓
a weighted draw picks guests who fit the hunger budget and the seat count
        ↓
guests spawn as temporary clones, walk in, sit, eat, gift, leave
```

There is no monetary cost and no cooldown. The letter names no specific character — it goes out to everyone who qualifies, and who that is depends entirely on the spread and on the player's standing with each of them. The only thing the player spends is food, and the only thing they get back is gifts.

Four pieces:

| Piece | Lives in | Job |
|---|---|---|
| **Attractiveness / fulfillment math** | `content/attachment/party/core/PartyMath.java` | pure functions, no world writes |
| **Roster selection** | `content/attachment/party/core/PartyPlanner.java` | weighted draw under the hunger budget and seat count |
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

**There is no `party_food` tag.** Food is decided entirely in code, not by a tag and not by a datapack — see §2.2's `isFood`. A `BlockFoodHandler` recognising the block, and finding something worth eating in it, is the whole test; the `party_food` data map only revalues a block that already passed it. There is no third way, and no tag to keep in step.

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

There is no `party_food` tag, and there is no datapack opt-in. A block is party food if
**all** of:

- a registered `BlockFoodHandler` recognises its class — `CakeFood` claims anything that is a `CakeBlock`, `FeastFood` anything that is a `FeastBlock`, and so on;
- that handler reports servings left on the placed block; and
- one of those servings is worth eating.

```java
// PartyFoodValue - the whole of "is this block party food", and it reads no datapack
public static boolean isFood(BlockState state) {
    return result(state).valid();
}
```

The `party_food` data map is consulted *after* that, and only to replace the number:

```java
public static int appealPerServe(RegistryAccess access, BlockState state) {
    var result = result(state);
    if (!result.valid()) return 0;                 // validated first, deliberately
    var override = PartyFood.of(access, state);
    return override != null ? override.appeal() : appealOf(result.food());
}
```

The ordering is the point. An earlier version consulted the override *first* and let it
short-circuit validity, which made a `party_food` row a way to declare a block edible. It
looked harmless because a non-food block has no servings, so the block's total appeal
came to 0 anyway — but "is this food" and "is this worth anything" had stopped being the
same question, and the block would still answer `true` to `isFood` and swallow a click in
`PartyWand.useOn`.

**Opting a foreign dish in is already handled in code**, by the `BlockItemFood` catch-all:
it accepts any block whose item carries food properties, so a modded cake is party food
with no datapack help at all. What the data map is for is the case code cannot know — a
mod has balanced its feast to be worth far more than its properties suggest, and the
heuristic cannot see that. Revalue, don't opt in.

#### Invalid food is not food

Three things make a handler-recognised block worth nothing, and all three score **0**
rather than the minimum:

- **No hunger value** — `nutrition() <= 0`. A block item can carry `FoodProperties` while being worth nothing to eat: a decorative item, a husk, another mod's placeholder. There is deliberately no "minimum appeal" fallback for a missing or empty serving, because a placeholder scoring 1 would still count towards the party's budget *and* still be walked to and eaten by a guest, costing a guest their whole visit for no fulfillment at all.
- **Poison or wither** — the same two `FeedModule` refuses outright.
- **Nothing to read** — no `FoodProperties` on the block, its serving item, or any permuted id.

A 0 is the signal that the block is **not party food at all**, and both consumers act on it — see `FoodResult` below for where the rule lives:

```java
// PartyArea.scan - dropped from the spread entirely, left standing in the world
if (worth <= 0 || !PartyFoodValue.result(here).valid()) continue;   // recognised, but not food
food.add(pos.immutable());
appeal += worth;

// YoukaiPartyTask.nearest - never walked to, so no guest's time is wasted
if (!PartyFoodValue.result(state).valid()) continue;
```

So a datapack author who adds a dud id to `party_food` gets a no-op rather than a bug:
nothing is counted, nothing is eaten, and the block simply stays on the table. The same
goes for a **valid-looking** id that names a block no handler can serve — it is inert,
because the row is never reached.

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

#### `FoodResult` — one resolution, four answers

Everything above is derived from the same block state and is always wanted together: the planner needs the serve count, the appeal needs the properties, the readout wants to name it, and the guest needs the action. So it is resolved once and kept.

```java
public record FoodResult(ResourceLocation id, int serves, @Nullable FoodProperties food,
        FoodConsume consume) {
    public static final FoodResult INVALID = new FoodResult(INVALID_ID, 0, null, (level, pos, state) -> 0);

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

**The `id` names the food by one rule: the serving item's where there is one, the block's otherwise.** A cake is named by the cake block's own item, because that item *is* the food; a Farmer's Delight feast is named by its `*_slice` item, because that is the thing a guest is actually handed; vanilla cake, which has no slice item at all, falls back to the block. It is identity for display and diagnostics, and it is deliberately **not** an override key — `party_food` stays keyed by block, so a row on a cutting board revalues the whole board rather than one apple on it.

#### `ContainerFoodHandler` — food that is not in the block state

Most block foods describe themselves entirely by their state, which is what makes the cache below possible. A **container** does not: the same `steamer_rack` block holds bread on one tick and nothing on the next, and the block state does not change when a guest eats from it. Anything derived from a container's contents therefore **cannot** be cached — a cached answer about them would be right once and wrong for ever after — so it is a second interface, in a second list, never routed through `FoodResultCache`:

```java
public interface ContainerFoodHandler {
    boolean handles(Block block);
    List<FoodResult> contents(Level level, BlockPos pos, BlockState state);
    boolean isPutOut(Level level, BlockPos pos, BlockState state);
    int consume(Level level, BlockPos pos, BlockState state);
}
```

Two things about it are not obvious.

**A container reports a list, not one food.** A board with apples, cheese and a carrot is three answers with three values, and averaging them into one would misprice both the spread and the draw. Each entry is one *kind* with `serves` set to how many of it there are — two apples are two servings of one entry, not two entries, because they are one food a guest comes back to.

**A container decides its own layout.** `isPutOut` is not the shared furniture test, because the rule genuinely differs by block (see below). One rule for all containers would have to be the loosest of them, which would put cutting boards on the floor into the spread.

Everything downstream of this is why `PartyFoodValue` now takes a `Level` and a `BlockPos` everywhere instead of just a `BlockState`. There is one set of methods rather than two parallel ones, and the cost is that the common case carries two arguments it does not use. The answers are a **snapshot, not a transaction**: reading the serves and then consuming is two looks at the world, so two guests converging on one dish can both read it present and the second finds nothing there. The consequence is bounded — a guest occasionally eats one dish less than she banked — and closing it properly would mean holding a reservation across ticks, which is a far larger change than the window is worth.

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

#### Registration is by class — and by identity for the two that have no class

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

There is deliberately **no** `party_food` data map file shipped. A handler — including the
`BlockItemFood` catch-all — recognises every food we know about, so nothing needs one.

**Correcting a value the heuristic got wrong.** This is the *only* thing the map does. A
block a handler already recognises but which scores badly gets a row:

```jsonc
// data/gensokyolegacy/data_maps/block/party_food.json
{ "values": { "some_mod:baroque_cake": { "appeal": 20 } } }
```

The row replaces the per-serving appeal, and the serve multiplier still applies on top, so
overriding a cake sets the value of a *slice* rather than of the whole thing.

A row on a block that is **not** already party food does nothing at all — `isFood` never
reads this map, and `appealPerServe` validates before consulting it. A foreign dish is not
opted in here; the catch-all already serves any block whose item carries food properties.
A row naming a block with no food properties is inert, not a broken dish.

This is the safer shape of the two. The alternative — treating a row as a declaration of
edibility — reads as a natural thing for a datapack to want, and it was briefly what the
code did. It is wrong because it splits one question in two: the block answers `true` to
"is this food" while being worth nothing, which then leaks into `PartyWand.useOn`, where
clicking such a block consumes nothing and still swallows the interaction.


#### The two Youkai's Feasts containers

Neither is a plate of food in the usual sense — both are vessels you fill — and both are party food anyway, because a host who has put out a cutting board of ingredients or a steaming pot has laid out a party.

**`CuisineBoardFood` — a cutting board.** Whatever is lying on it, one entry per kind of thing. Whether there is anything to eat is decided by resolving the board's `TableItem` model and asking whether it came back as a `FoodTableItem`: that is the form that carries contents, and an empty board never gets one. Reading the list is then done from the block entity rather than from the model, because the model's copy is the snapshot taken when the recipe matched while the block entity's is live — and because a guest taking one item has to take it from the board itself, or the next guest is offered food that is no longer there.

**`SteamerRackFood` — a steamer, both halves of it.** The pot carries up to two rack tiers of its own (the `pot_racks` property, which is how you steam with no rack at all) and each rack block stands in the pot with up to four tiers. They are separate block entities with separate storage, so a full steamer is genuinely two food sources and neither shadows the other. Unlike the board this needs no reflection — the contents are public fields on `RackData` and `RackItemData`.

**Their placement rules differ, and that asymmetry is the design.** A board is food only on the table, exactly like any other dish. A pot is food **wherever it stands** — on a stove, on the table, on the ground — because a pot of food on a kitchen floor is a thing a host has actually done, and refusing it would put the feature out of reach in the one room it was made for. A rack is food on the table, or in a pot, and on the ground it is not: a rack is a part of a steamer rather than a dish in its own right, and it needs a pot or a table to be part of anything. The pot has to be looked *through* to work that out — a rack inside one has pot interior below it, not the pot block, so the block that carries it is two or three down.

**The board's contents are read reflectively, and that is forced.** Both the board's list and the model's are private with no accessor. The alternatives were a mixin accessor — which would need a config plugin to survive Youkai's Feasts being absent, and it *is* absent from a dev launch where it is `compileOnly`, so an unguarded mixin would fail the game every time rather than only when the feature is used — or `getHints`, which describes what the board could become rather than what is on it: a board holding one apple out of a three-part recipe would report three ingredients, and the party would be budgeted against food the host never put out. The field is resolved once and never again, and a board whose field has been renamed by an update reads as "no food on it" rather than taking the party down.

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

Everything the party touches — food, seats, tables, guests, the anchor — is found by walking out from the block the player clicked. The walk is the unit of "a party": it covers the connected space the host laid the party out in, and stops at a wall.

### 3.1 A walk, not a box

```java
public record PartyArea(BlockPos anchor, List<BlockPos> food, List<BlockPos> seats,
        List<BlockPos> tables, int appeal) {

    /** The box each piece of furniture queues around itself: 5 x 3 x 5, centred. */
    private static final int POKE_X = 2, POKE_Y = 1, POKE_Z = 2;
    private static final int MAX_VISITED = 1 << 16;

    public static PartyArea scan(ServerLevel level, BlockPos anchor, int distance) {
        var food = new ArrayList<BlockPos>();
        var seats = new ArrayList<BlockPos>();
        var tables = new ArrayList<BlockPos>();
        int appeal = 0;
        long reach = (long) distance * distance;
        var seen = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(anchor.immutable());
        while (!queue.isEmpty() && seen.size() < MAX_VISITED) {
            var pos = queue.poll();
            if (!seen.add(pos)) continue;
            if (!within(anchor, pos, reach) || !level.isLoaded(pos)) continue;
            var state = level.getBlockState(pos);
            var furniture = false;
            if (state.is(GLTagGen.PARTY_SEAT)) { seats.add(pos); furniture = true; }
            if (state.is(GLTagGen.PARTY_TABLE)) { tables.add(pos); furniture = true; }
            // a chair or table is solid, so its box is what carries the walk past it
            if (furniture) poke(queue, pos);
            if (PartyFoodValue.isFood(level.registryAccess(), state) && isLaidOut(level, pos)) {
                int worth = PartyFoodValue.appeal(level.registryAccess(), state);
                if (worth <= 0 || !PartyFoodValue.result(state).valid()) continue;
                food.add(pos);
                appeal += worth;
            }
            if (!furniture && state.getCollisionShape(level, pos).isEmpty())
                for (var face : FACES) queue.add(pos.relative(face));
        }
        return new PartyArea(anchor.immutable(), food, seats, tables, appeal);
    }
}
```

Four rules, and together they are the whole of what counts as a party's spread:

1. **The walk passes only through space a guest could stand in** — empty collision shape. A wall ends it; a doorway does not. So a party is the *room* it was set in, not the building around it, and a table in a neighbouring hall is a different party.
2. **Furniture extends the walk by a 5×3×5 box**, centred on itself: two out sideways, one up and down. A chair cannot be walked through, only reached, so without this the walk would stop at the first chair and never find the rest of a long table. Because the box's contents are then read and walked *in the ordinary way*, the walk resumes from the air on the guest side of the chair and carries on — a whole row of seating extends the walk along its length without the rule being applied twice. It also reaches furniture the walk could never have stood near: a chair pushed into a nook, a dish tight against a wall.
3. **Food counts on a table or touching one** (`PartyArea.isLaidOut`, shared with `YoukaiPartyTask` so a guest can never be handed something the scan would not have counted). On the table is the block below; touching means the four horizontal faces plus up, but **not** down — a dish tucked under a table is not laid out for anyone.
4. **Nothing beyond `partyScanDistance` from the anchor counts**, however it was reached, and nothing in an unloaded chunk is read at all. The scan runs inside one click and must not drag a chunk in to answer a question about a party.

Two more things the walk gets right that a box sweep did not: a food block is solid, so the walk only ever meets one by reading it off a seat's or table's box, which is exactly where laid-out food is; and an unclaimed food block sitting on the floor is never reached at all.

`MAX_VISITED` is a safety valve, not the limit. The 32-block distance cap alone allows the better part of a million blocks of open air — a stall the player cannot see or stop — so the walk stops at 65536 blocks. It only ever bites in a large empty space, where there is by then nothing to find.

`PartyArea` collects tables alongside food and seats, so nothing needs a second scan of its own.

### 3.1.1 What a refusal still knows

`PartySend.Outcome` carries the scanned `area` even when `plan` is null. That is the interesting half of a refusal: the host wants to see what the walk did and did not take around a table that was refused, and re-scanning it would be a second walk over the same blocks. The wand renders the area whether or not the plan succeeded.

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

At `partyScanDistance = 32` the reachable area spans at most 5×5 chunks, so the set of chunks a party can possibly touch is small and bounded.

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
    @SerialField private int hungerBudget;      // snapshot of the spread at send
    @SerialField private int hungerServed;      // running total actually consumed

    /** Whether this chunk already hosts a party; the send-time overlap guard reads this. */
    public boolean active() { return phase != Phase.IDLE; }
}
```

The party lives in the chunk holding `center(food)` (§3.2), so:

- it is **chunk-scoped for free** — unload the chunk and the party freezes rather than corrupting;
- it **survives host logout**, because the data belongs to the chunk, not the player;
- it is dropped with the chunk like everything else in it, so there is no stale state to garbage-collect.

**The roster is frozen when the letter is sent and never recomputed.** Re-drawing on a timer would let a guest who wandered in mid-banquet change the guest list under everyone else, and would make the outcome depend on tick order. Frozen, the roster is a pure function of (reputation, food on the table, seats available) at the moment of sending — and because the draw is seeded from the centre of that food, it is a *reproducible* one, which is what makes `/gensokyo party preview` able to predict a real send perfectly.

**Ticking.** The party advances its phase from the host's chunk tick at 5 Hz via the capability `tick` hook, the same one `CharacterAttachment` uses for daily decay (`CharacterAttachment.java:22`). Nothing in a party needs 20 Hz resolution: guests are walking and eating on multi-second timescales.

Phase transitions:

| From | To | Condition |
|---|---|---|
| `IDLE` | `GATHERING` | letter sent, roster frozen, clones spawned |
| `GATHERING` | `FEASTING` | every guest has claimed a chair (`PartyTicker.allSeated`) |
| `FEASTING` | `CLOSING` | every guest dismissed, or `PARTY_TIMEOUT` elapsed |
| `CLOSING` | `IDLE` | every guest dismissed |
| any | `IDLE` | host left the area, or all guests died |

`FEASTING` has no "everyone is satisfied" exit: satisfaction is per-guest and no longer
ends anything. A guest leaves when she has eaten all the spread she can reach, so the
roster drains on its own and the party closes as the last guest goes — see §6.5.

The `GATHERING` gate is party-wide on purpose: the table feasts as a table. Its cost is
that one guest with nowhere to sit holds the whole table out of the feast until
`PARTY_TIMEOUT` (10 min) calls the party off. A per-guest gate would not stall, but the
last guest to arrive would eat alone at an ungathered table, which reads as a bug. Seating
is a host problem either way; the timeout is the backstop for a host who never puts out
another chair.

Aborting to `IDLE` always leaves the remaining food untouched — an interrupted party wastes the guests' time, not the player's food.

---

## 5. The algorithm

Three quantities, all pure functions over plain values.

### 5.1 Attractiveness of the spread

`PartyArea.appeal` from §3.1: the sum of `PartyFoodValue.appeal` over every block the walk found that is food and is laid out on or against a `party_table`/`party_seat`, where each block's contribution is **serves × per-serving appeal** (§2.2). Simple additive, no multipliers — the player controls it directly by choosing what to lay out, and the per-serving number comes from the food's real properties rather than a table of hand-picked values.

Because it is counted in serves, the budget a spread offers is the total number of servings on the table, and a partly-eaten dish is worth proportionally less. That is what makes one cake a meal for several guests instead of a single indivisible unit.

### 5.2 Guest weight

#### The two quantities from the food

The food on the tables is measured **twice**, and keeping them apart is what makes §5.4
mean something:

| | measured as | used for | what it answers |
|---|---|---|---|
| **appeal** | `nutrition×0.5 + saturation×2.0 + 4 per beneficial effect` | `PartyMath.weight`, and nothing else | how much this draws a guest in |
| **hunger** | `nutrition()` | the planner's budget, and a guest's `fulfillment` | how much this actually feeds anyone |

A rare-effect delicacy scores high on appeal and no higher than a plain slice on hunger. A
spread of such dishes draws a crowd and seats almost nobody — which is the behaviour the two
names exist to make expressible, rather than a bug to paper over. The `party_food` data map
reaches **appeal only**; letting it reach hunger would let a datapack silently change how
many guests a spread seats.

`PartyArea.scan` gathers both in the same pass, so the two can never end up describing
different food:

```java
food.add(pos);
appeal += PartyFoodValue.totalAppeal(access, state);   // -> PartyMath.weight
fulfillment += PartyFoodValue.totalHunger(state);       // -> the planner's budget
```

For a vanilla cake the two happen to agree — a slice is appeal 2 and hunger 2, a whole cake
14 and 14 — so the §5.5 worked example is unaffected. They diverge as soon as saturation or
effects are involved: `cooked porkchop` is appeal 12, hunger 13, while a slice with two
beneficial effects is appeal 10, hunger 2.

#### The weight

Each character is scored on the two axes the design calls for: how much the spread appeals to her, and how much the player has earned her company.

```java
/**
 * How much this character wants to come, as the share of the next draw she wins.
 *
 * @param spreadAppeal 0 at or below minAppeal, 1 at or above maxAppeal, linear
 *                     between: a spread too plain for her is worth nothing to her.
 * @param reputation   1.0 at exactly minReputation, rising to 1 + repBonus at
 *                     maxReputation: being genuinely well liked turns a maybe into a yes.
 * @return 0 when she declines outright, below minReputation
 */
public static int weight(PartyTaste taste, int spreadAppeal, int reputation) {
    if (reputation < taste.minReputation()) return 0;      // she declines outright
    double appealFactor = ramp(spreadAppeal, taste.minAppeal(), taste.maxAppeal());
    double repFactor = 1.0 + taste.repBonus()
            * ramp(reputation, taste.minReputation(), taste.maxReputation());
    return (int) Math.round(taste.weight() * appealFactor * repFactor);
}

/** 0 at or below lo, 1 at or above hi, linear in between. */
public static double ramp(int value, int lo, int hi) {
    if (value >= hi) return 1.0;
    if (value <= lo || hi <= lo) return 0.0;
    return (value - lo) / (double) (hi - lo);
}
```

`weight` is her value at **full** interest, and `100` is ordinary: an uninterested
character scores 0 whatever her `weight`, and a fully-interested average-reputation one
scores her `weight` times `1 + repBonus`. So `weight` is a third, independent axis — not
another name for the same thing as `repBonus`. `repBonus` scales with *how well liked*
you are; `weight` scales with *who she is*, and does it whether you are her best friend
or merely an acquaintance. A popular character (high `weight`, low `minReputation`)
fills a room; a character you must work hard for (low `weight`, high `minReputation`)
earns one place in it.

The return is an **integer** because that is what §5.4's draw consumes. `weight` is a
share of a probability distribution, so a fractional one would buy nothing — and rounding at
the end rather than carrying a `double` through means every candidate's weight is exactly
the number the data map asks for, with no float drift between authoring and drawing.

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
 * @param fulfillment   hunger she must be served before she counts as satisfied
 * @param weight        her weight at full interest; 100 is ordinary
 */
public record PartyTaste(
        int weight,
        int minReputation, int maxReputation, double repBonus,
        int minAppeal, int maxAppeal, int fulfillment
) {
    public PartyTaste {
        weight = Math.max(0, weight);
        maxReputation = Math.max(maxReputation, minReputation);
        maxAppeal = Math.max(maxAppeal, minAppeal);
        repBonus = Math.max(0, repBonus);
        fulfillment = Math.max(1, fulfillment);
    }
}
```

The compact constructor clamps the inverted ranges so a datapack typo degrades into "no bonus" rather than a negative weight, and floors `weight` at 0 — where 0 means she is never picked, which is a legitimate thing for a datapack to say.

**A character with no authored `party_taste` does not come to parties.** There is no
separate whitelist — the data map *is* the eligibility list:

```java
/** This character's taste, or null when she has no authored entry. */
@Nullable
public static PartyTaste of(RegistryAccess access, EntityType<?> type) {
    return GLMeta.PARTY_TASTE.get(access, type.builtInRegistryHolder());
}
```

`PartySend.plan` skips a null taste when it builds the candidate pool, so a character is
invited only if `isCharacter(type) && taste != null`. Two gates, not one:
`isCharacter` keeps the chair, the broom, the doll and the projectiles out of a pool they
have no business in, and the data map decides who among the actual characters is
invitable.

```java
for (var type : BuiltInRegistries.ENTITY_TYPE) {
    if (!isCharacter(type)) continue;
    var taste = PartyTaste.of(level.registryAccess(), type);
    if (taste == null) continue;                          // not party-eligible
    int rep = reputation.applyAsInt(type);
    pool.add(new PartyPlanner.Candidate(type,
            PartyMath.weight(taste, area.appeal(), rep), taste.fulfillment(), rep));
}
```

`DEFAULT` survives as a **template to copy**, not a fallback — nothing reads it at
runtime, and it exists so a datapack author has seven concrete numbers to start from
rather than a blank row.

This is a deliberate reversal of an earlier decision, which fell back to `DEFAULT` and so
made every character eligible with no whitelist at all. The problem was that "no data"
silently meant "a generic guest on default terms" — a newly registered character showed up
to parties automatically, and no row in the file recorded that it was allowed to. Putting
eligibility in the data map makes the file the single place that says who comes, so an
absent character is absent on purpose rather than by accident.

The cost is that a character nobody has written down never appears, which is the
trade being made deliberately: an unwritten character is a decision not yet taken, not a
decision taken for you.

The `DEFAULT` template is sized against §2.2's derived appeal, where a whole vanilla cake
is worth 14: `maxAppeal` 24 is under two cakes, and `fulfillment` 8 is just over half of
one.

Shipped values, in `data/gensokyolegacy/data_maps/entity_type/party_taste.json`. **These
six rows are the whole eligible cast** — every other character, including the remaining
eight youkai, has no entry and does not come:

| Character | weight | minRep | maxRep | repBonus | minAppeal | maxAppeal | fulfillment |
|---|---|---|---|---|---|---|---|
| `cirno` | 100 | **0** | 150 | 0.3 | 4 | 20 | 6 |
| `hakurei_reimu` | 100 | 50 | 200 | 0.3 | 10 | 32 | 20 |
| `kirisame_marisa` | 100 | 50 | 200 | 0.3 | 20 | 32 | 20 |
| `morichika_rinnosuke` | 100 | 50 | 200 | 0.3 | 20 | 32 | 20 |
| `alice_margatroid` | 100 | 50 | 250 | 0.15 | 24 | 40 | 12 |
| `izayoi_sakuya` | 100 | 50 | 250 | 0.15 | 30 | 48 | 20 |
| *(everyone else)* | — | — | — | — | — | — | **not eligible** |

Every one ships `weight` 100, so these are exactly the pre-`weight` numbers scaled by 100
— the axis is authored but not yet tuned, and moving one is a datapack edit.

Cirno is the one entry with no reputation floor and the lowest fulfillment, which is what
makes her the reliable first guest at an early game. The other five all need a standing
reputation of at least 50, so an unwritten save invites nobody until someone has met them.

Sakuya has no `character_config` and no home — she is guest-only by design (`GLEntities.java:55`) — which makes her the natural first test case for the whole system, since a party guest needs no home binding either way.

Fulfillments are 6/8/8/8/10/12 rather than the original 16/20/20/20/24/24. They were retuned once appeal stopped being hand-authored and became derived from real food properties: a whole vanilla cake is worth 14, not the 8 originally assumed, and a modded feast lands somewhere above that. A fulfillment of 16 against a 14-appeal cake would have left the no-mods-installed path unable to satisfy anyone at all.

They are also now counted in **serves**, not blocks. A guest is satisfied once the hunger she has eaten reaches her fulfillment, and eating is one serve at a time, so a fulfillment of 6 against a 2-hunger slice means three slices — she leaves the other four for whoever is next.

### 5.3 Fulfillment

A guest tracks the **hunger** she has eaten and is satisfied when it reaches her
`fulfillment`. Hunger, not appeal: her fulfillment is charged against the same quantity in
§5.4, and crediting her with appeal instead would leave a guest eating forever at a party
whose dishes looked magnificent and fed her nothing.

```java
// PartyModule
@SerialField private int eaten = 0;

/** One serve eaten: her tally, and the party's shared total. */
public void eat(int serveHunger, PartyAttachment party) {
    eaten += serveHunger;
    party.serve(serveHunger);
}

public boolean satisfied(PartyTaste taste) { return eaten >= taste.fulfillment(); }

/** 0..1 — how well she was actually served, relative to what she wanted. */
public double fulfillmentRatio(PartyTaste taste) {
    return Math.min(1.0, eaten / (double) Math.max(1, taste.fulfillment()));
}
```

The draw in §5.4 guarantees the *planned* total fits the spread, but food can still run short — the player can break a dish mid-party, or two guests can converge on the last serve. Fulfillment is therefore checked at runtime, not assumed from the plan. A guest who is under-served still eats what is there and still leaves a gift, but a reduced one (§6.4). This is what makes "guests consume all the food you prepare" mean something: a thin spread produces thin gifts.

### 5.4 Who comes — a weighted draw under a shared budget

Draw one guest at a time, each with probability proportional to her weight, spending her
fulfillment from a budget that starts at the spread's **hunger** (§5.2). Both constraints
hold by construction:

- `sum(fulfillment) <= capacity`, because nothing is drawn that does not fit what is left;
- `size() <= seats`, because the loop stops at the seat count.

Both sides of the first constraint are hunger, which is what makes them comparable: a
guest's `fulfillment` and the spread's capacity are the same unit, so the arithmetic means
something. The spread's **appeal** never enters here — it is computed upstream, once, and
reaches the planner only through the weights it produced.

Both sides of the first constraint are hunger, which is what makes them comparable: a
guest's `fulfillment` and the spread's capacity are the same unit, so the arithmetic means
something. The spread's **appeal** never enters here — it is computed upstream, once, and
reaches the planner only through the weights it produced.

```java
public record Candidate(EntityType<?> type, int weight, int fulfillment, int reputation) { }

/**
 * @param rng seeded so one spread always draws one party
 * @return chosen characters, in the order they were drawn
 */
public static List<EntityType<?>> select(List<Candidate> pool, int capacity, int seats, Random rng) {
    if (seats <= 0 || capacity <= 0) return List.of();
    var viable = new ArrayList<>(pool.stream()
            .filter(e -> e.weight() > 0 && e.fulfillment() <= capacity)
            .toList());

    int remaining = capacity;
    var chosen = new ArrayList<EntityType<?>>(Math.min(seats, viable.size()));
    while (chosen.size() < seats && !viable.isEmpty()) {
        var guest = viable.remove(draw(viable, rng));
        chosen.add(guest.type());
        final int rest = remaining - guest.fulfillment();
        viable.removeIf(e -> e.fulfillment() > rest);   // the budget filter, every round
        remaining = rest;
    }
    return chosen;
}

/** One index, drawn with probability proportional to weight. */
private static int draw(List<Candidate> viable, Random rng) {
    long total = 0;
    for (var c : viable) total += c.weight();
    if (total <= 0) return 0;
    long roll = rng.nextLong(total);
    for (int i = 0; i < viable.size(); i++) {
        roll -= viable.get(i).weight();
        if (roll < 0) return i;
    }
    return viable.size() - 1;
}
```

Implementation notes:

- **The budget filter is re-run every round**, which is the part that makes the appeal
  constraint real. A guest who was affordable when the round began may not be after it, so
  dropping the unaffordable each time is what stops a late draw overrunning the budget.
- **`weight() <= 0` is filtered once, up front.** A character who declined outright must
  never be drawn at all, which is a different rule from "cannot afford her right now" —
  one is permanent, the other is a consequence of this round's spending.
- **The weight total is accumulated in `long`.** Weights come from datapack and
  `PartyMath.weight` scales a `party_taste` weight by the appeal and reputation ramps, so a
  dozen of them can leave the `int` range. An overflow wraps to the wrong sign or magnitude
  and quietly skews every later draw, which surfaces only as "the roster looks odd".
- **Roster order is draw order**, not weight order. That is the visible face of the
  randomness: the first guest drawn is the one the draw favoured most on that occasion.

#### Why not the knapsack this replaced

The previous implementation was a 0/1 knapsack maximising total weight, with the seat cap
applied afterwards by truncating the weight-ordered result. It was **correct** — brute
force over 200k random pools found zero cases where the DP missed the true budget-only
optimum — but it had two faults:

1. **The seat cap was not a constraint.** Truncating a maximum-weight solution to its
   `seats` heaviest is not the same as maximising weight under a seat limit. It loses
   weight in roughly 1% of all cases, and up to 22 weight in the worst found: at appeal 20
   with 2 seats the DP returned 4 guests worth 66, the truncation left 2 worth 43, while a
   seat-legal pair scored 65. Nobody was ever *invited and then dropped* — that was handled
   — but the choice of who survived the truncation was made under the wrong objective.
2. **It was perfectly predictable.** Same spread, same guest list, every time, so a host
   learned the party by heart after one attempt and there was no reason to lay out the
   food differently.

This version fixes both. The seat count is a loop bound, so the draw is made under both
constraints at once, and the roster varies.

#### What was given up, and how it is bought back

A weighted draw cannot be predicted — that is the whole point — which would have broken
`/gensokyo party preview`, whose entire justification is that it runs the identical code
path as a real send.

The fix is the seed. `PartySend.plan` seeds from the **centre of the spread's food**
(`PartyArea.center`), not from the block the player clicked:

```java
long seed = (long) center.getX() * 0x9E3779B97F4A7C15L
        + (long) center.getY() * 0xC2B2AE3D27D4EB4FL
        + (long) center.getZ() * 0x165667B19E3779F9L;
seed ^= seed >>> 33; seed *= 0xFF51AFD7ED558CCDL; seed ^= seed >>> 33;   // splitmix64 finaliser
```

Two properties follow, and both are wanted:

- **Preview still predicts a send exactly.** Preview, a real send and a wand simulation all
  seed from the same centre, so they cannot disagree.
- **It does not depend on which table you clicked.** Seeding from the anchor would make the
  guest list depend on an arbitrary click; seeding from the food centre means clicking
  either end of a long table draws the same party.

The coordinates are scrambled rather than packed. A plain `(x << 32) ^ (z << 16) ^ y` makes
neighbouring centres differ only in the low bits, and neighbouring parties then come out
alike — the avalanche step is what stops a one-block move of the food from producing a
near-identical roster.

`nearestMiss` is deliberately **not** a draw. It is a diagnostic — "she wanted in but there
was no food/seat left" — and an answer that changed from one press to the next would be
worse than useless.

### 5.5 Worked example

Vanilla only, and checkable against §2.2: one `minecraft:cake` is 7 slices of 2 hunger = **14**, and its appeal is also 14 (§5.2), so the spread sizes below are both. Reputation 180 across the board, above everyone's floor. Recomputed against the shipped `party_taste.json`.

Per-character weight at each spread, reputation held at 180:

| Character | fulfillment | 1 cake (14) | 2 cakes (28) | 3 cakes (42) | 4 cakes (56) |
|---|---|---|---|---|---|
| `cirno` | 6 | **81** | **130** | **130** | **130** |
| `hakurei_reimu` | 20 | **23** | **103** | **126** | **126** |
| `izayoi_sakuya` | 20 | 0 | 0 | 73 | 110 |
| `kirisame_marisa` | 20 | 0 | 84 | 126 | 126 |
| `morichika_rinnosuke` | 20 | 0 | 84 | **126** | **126** |
| `alice_margatroid` | 12 | 0 | 27 | **110** | 110 |

Bold is on the roster drawn for the spread in the row. Unlike the knapsack this replaced,
that is a property of the *seed*, not of the appeal — the same spread at a different
centre gives a different roster. The bold is here to make the worked rows checkable, not to
claim those characters are the right answer.

What gets drawn, at a stated centre of (100, 64, -220) and reputation 180:

| Spread | Appeal | Roster (draw order) | Appeal spent | Seats |
|---|---|---|---|---|
| 1 cake | 14 | Cirno | 6 | 1 |
| 2 cakes | 28 | Cirno, Reimu | 26 | 2 |
| 3 cakes | 42 | Morichika, Alice, Cirno | 38 | 3 |
| 4 cakes | 56 | Marisa, Sakuya, Alice | 52 | 3 |

**The guest *count* is 1 → 2 → 3 → 3, and it flattens at three.** That is a property of the
fulfillments, not of the draw: with five of six characters needing 20, a fourth guest costs
at least 20 more appeal, and four guests needs 6+12+20+20 = 58. Four cakes buys 56. A fifth
guest would need 78 — six cakes. The count is stable because the constraint arithmetic is
stable; only the composition moves.

Two things follow. **The seat cap does nothing above two guests**, because the hunger budget
binds first — and `weight` cannot fix that, since a character who does not fit the budget is
removed before her weight is ever consulted. Raising appeal or lowering fulfillments is the
lever, and a Farmer's Delight feast (well above a cake) is the intended way past three.

Sakuya's weight is 0 at both 14 and 28: 28 is below her `minAppeal` of 30, so she is not
interested however well the player knows her. That is the `minAppeal` cutoff doing its job.
At 56 she is in the draw — the row above shows her arriving — because a draw weighs
*probability*, not rank: 110 against three others at 126 is a 22% share, not a loss.

Because eating is per-serving, those fulfillments are slice counts at this scale: Cirno's 6
is three slices of cake, Reimu's 20 is all seven slices plus three more, so she wants a
second cake's worth of spread. The last guest waits while the earlier ones take their
slices, and those slices simply stop existing as they are eaten.

The guest count tracks the food, and who shows up tracks reputation — both fall out of the
formulation rather than being special-cased anywhere.

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

1. `PartyArea.scan(level, clicked, GLModConfig.SERVER.partyScanDistance.get())` (§3.1).
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
public static void give(ServerLevel level, YoukaiEntity guest, PartyModule party) {
    ItemStack gift = pick(level, guest);                       // weighted pick from gift_data items
    int extra = Math.min(MAX_EXTRA, party.appealEaten() / APPEAL_PER_EXTRA);
    Block.popResource(level, table.above(), gift.copyWithCount(gift.getCount() + extra));
}
```

The size comes from **the appeal she personally consumed** — one extra item per `APPEAL_PER_EXTRA`, capped at `MAX_EXTRA` — and deliberately *not* from whether she was satisfied. Those are the two food measurements (§5.2) and they answer different questions: rare-effect delicacies fill her up no better than plain bread, and she is still more moved by them. An earlier version halved the gift for an under-served guest, which made a beautiful thin spread read as stingy rather than as modest; the host's generosity and the guest's appetite are not one axis.

`Block.popResource` at the table is the in-tree idiom for putting an item down in the world (`MarisaTaskUtil.java:94`, `ShelfBlockEntity`). That matches "thrown on table for now" exactly, and because nothing is granted into an inventory there is nothing to unwind when the real gift item is designed.

**There is no reputation change and no cooldown.** The food is the entire cost and the gifts are the entire return. Reputation is read once, at send, to decide the roster, and is never modified by the party itself. The letter names no character, so there is nothing to prevent being re-sent — repeating a successful spread simply works, and repeating an unsuccessful one fails the same way.

### 6.5 The task

`YoukaiPartyTask` is a **cycle**, not a walk-then-finish: seat, eat, go back to the seat, sit a while, repeat, until the spread is gone.

```
start()      → recover her place in the cycle from PartyModule (see below)
SEEK_SEAT    → walk to the nearest chair, sit, claimSeat(...), WAIT_TABLE
WAIT_TABLE   → seated, doing nothing until the party reaches FEASTING
SEEK_FOOD    → draw a dish, weighted by its appeal per serve
               within REACH of her chair?  eat it from where she sits
               otherwise                   dismount, walk to it
EATING       → swing + USE_MAINHAND + eating sound, wait EAT_TICKS,
               take ONE serve, credit hunger AND appeal
               then RETURNING if she left her chair, else RESTING
RETURNING    → walk back to her chair, sit again
RESTING      → wait hungrySeat (1s) if still hungry, fullSeat (5s) once satisfied
               roll leaveChance after a satisfied sit — and go, if it comes up
no chair in 10s → leave the party, so the table can go on without her
no dish left → giveGift(), dismiss()
```

**The party caches its own spread and its own chairs.** `PartyAttachment` holds both lists and refreshes them on two schedules from `PartyTicker`: the food every two seconds, the layout every ten. A guest needs the food list every time she draws a dish, which is every few seconds for the length of her visit, so having each guest sweep a box for herself made the party cost far more to run than the party itself. Two rates because the two change at very different speeds — every dish a guest eats is one fewer, while a chair that moved a second ago will still be there in four more — and because conflating them would either make a refilled dish take ten seconds to appear or rebuild the chair list four times a second for nothing.

The fast rate is also what makes **refilling work at all**. The scan runs once per send and freezes a roster against it; without a rescan a host putting out a second course would have it ignored until everyone had left, and a guest would walk to a dish another guest had just finished. The lists are re-checked per use rather than trusted — the cache is two seconds old, and in those two seconds a dish may have been eaten or moved.

One consequence worth stating: `hungerBudget` now tracks what the guests can actually reach, and moves as the host refills. It therefore **narrows** after a send, from "the whole scanned room" to "the guest box" — which is the honest answer, since nothing outside that box can be eaten, but it is a different number from the one the send printed.

**The dish is drawn, not found.** `drawDish` reads the party's cached food list, keeps everything still laid out and still worth eating, and picks one with probability proportional to `appealPerServe`. That is what makes a guest work across the spread instead of clearing the nearest plate first and working outwards, and what makes the tasty things go before the plain ones. It replaced a `nearestFood` search, which had the opposite property by construction.

**She never leaves on being satisfied — but she does leave eventually.** Reaching her fulfillment only lengthens the sit between dishes, from her `hungrySeat` (1s) to her `fullSeat` (5s), both per character in `party_taste` rather than constants: a glutton plausibly dawdles between courses and a nibbler plausibly does not. After each *satisfied* sit she rolls `leaveChance` (5%) and goes if it comes up. That is the only thing that ends a party before the food does, and it has to exist — otherwise a guest who is already full has no reason ever to go home and the table stays occupied until the host's spread is gone. It is a chance per sit rather than a flat timer so that the two per-character fields mean something together: a long `fullSeat` also means a slow departure. In simulation, a guest facing a 200-dish spread leaves after about eleven dishes, every time.

**Ten seconds without a chair and she is expelled.** The party-wide feast gate (§4) waits for *every* guest to be seated, so one guest who cannot find a chair would otherwise hold the whole table out of the feast until the ten-minute `PARTY_TIMEOUT`. After ten seconds she leaves the party instead — no gift, since she never ate, and a word to the host, since "there are not enough chairs for the number of guests I invited" is something the host can act on and a silently emptier table is not. This is the same ten seconds whether she never found a chair or lost the one she had.

**Her chair is re-checked, so a removed one does not strand her.** The layout rescan (§4) bumps a version counter when the chair list changes, and the guest compares it against the version she last saw. A chair that has been broken is no longer hers to sit on, so she drops the claim and starts looking again — on the same ten seconds, and expelling herself if there is nothing else. Without this, a guest removed from her `ChairEntity` would stand beside the table in GATHERING for ever, holding the party with her.

**Most rounds she does not move.** A dish within `REACH` (3 blocks) of where she sits is eaten from the chair. Only a dish further out sends her across the room — which costs a dismount, because **sitting means riding** a `ChairEntity` (`ISeatableBlock.sitDown` calls `startRiding(seat, true)`) and a passenger does not walk. Every departure therefore owes a return, or the second trip out would be measured from wherever the first one left her.

`HomeSearchUtil.setSitting` (`HomeSearchUtil.java:75`) already handles claiming a free seat, checking `ChairEntity` occupancy and yielding to players, and is a silent no-op when the block is not `ISeatableBlock` — so a third-party chair from `party_seat` simply has the guest stand beside it, with no special case. The claim is still recorded, because otherwise one unseatable chair would hold the party in GATHERING for ever.

**`PartyModule` holds the chair, not the task.** A visit now spans many dish cycles and can be interrupted at any point by the chunk unloading, which stops the task and starts a fresh one. `start()` therefore recovers her place in the cycle from what she is physically doing — no seat yet, so `SEEK_SEAT`; riding one, so `WAIT_TABLE`; neither, so mid-`RETURNING` — rather than assuming `SEEK_SEAT` as the old code did. Getting this wrong would send every guest back out for another walk on each unload.

**The task must never return `false` from `canStillUse` to change phase.** One `Behavior` instance drives the whole visit, so the phase lives in a field on the task; `false` calls `doStop` → `stop()`, which wipes it, and the brain re-runs the exclusive behavior next tick with `start()` re-deriving the phase. Every phase change therefore keeps returning `true`, and `false` is reserved for actually finishing — party over, out of time, or nothing left to serve.

This is why `YoukaiPartyTask` cannot copy `MarisaForageTask`'s "arrive, act, return false" shape. The forage task is single-action and holds no state between runs, so stopping is free. A multi-phase task must keep the instance alive across its own transitions, and the failure mode is silent — the entity looks perfectly healthy, sits down, stands up, sits down again, and nothing is ever eaten.

**Food contention needs no locking.** On arrival a guest re-validates that the block is still food and still laid out; if another guest took the last serve first it is gone and she draws again. With per-serving consumption this is *more* forgiving than it was: two guests walking to the same cake no longer collide destructively, since each finds a serve still on it. Worst case is a wasted walk.

**A serve is taken when eating starts and credited when it finishes.** `PartyFoodValue.consume` is called in `beginEating`, and `pendingHunger` / `pendingAppeal` — both read *before* the consume, because a serve's readings are properties of the state it was taken from — carry them across the `EAT_TICKS` window so her tallies and the party's served total move together. `stop()` credits a serve that was taken but never banked, so a party ending or a chunk unloading mid-bite cannot silently swallow food the guests already ate.

**Eating while seated needs `isLaidOut` to accept a chair underneath.** A plate set on a guest's own seat is within reach of her and, on the old test, not part of the spread at all — the scan would not have counted it, so she could not eat it. Rather than special-casing the eating rule, the chair was added as a surface: a dish on a chair is now part of the spread and budgeted for like any other, instead of being edible but unaccounted.

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
| `content/attachment/party/core/PartyPlanner.java` | candidate pool + seeded weighted draw under both constraints |
| `content/attachment/party/core/PartyTaste.java` | entity-type data map record; an absent entry means **not eligible**, `DEFAULT` is a copyable template |
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
| `init/data/GLModConfig.java` | `partyScanDistance`, `partyGuestRadius` |
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
| click a party table | server | bound position, table/seat/food counts, hunger, how much the guests need, every candidate with her weight, **and the area drawn in the world** |

The hover readout is computed **client-side** from the block state and the item registry, both of which the client already holds. That is not laziness: a live readout re-asks every frame, and a server round trip per frame — or a packet per block looked at — would be paid for numbers that cannot disagree with the client's own copy.

The party simulation is the opposite case. Scoring characters by reputation needs server state, so the click runs the identical `PartySend.plan` a real send calls before it commits — no party is opened, no guest spawned, no letter spent — and the answer is written to an item data component:

```java
public record PartyWandData(BlockPos bound, int tables, int seats, int foods,
        int hunger, int needed, List<Candidate> candidates) { ... }
```

A **component** rather than a packet or a chat message, because vanilla already keeps item components in sync to the client; the overlay reads it back whenever the wand is held, with no machinery of its own.

### 9.2 Drawing the area

The readout is text, and text cannot say *where* the food it counted is. A spread across two rooms, or a chair the walk never reached, is only visible in the world, so `PartyAreaRenderer` draws the scan: a green cube per dish, blue per seat, gold per table, inside one wireframe box over the extent of all of them.

**The positions live on the component, not on a packet.** That was the first design and it was wrong twice over. A separate `PartyAreaToClient` packet put the same facts on two transports — counts on the item, positions on the wire — so the picture and the numbers could disagree, and it needed its own lifecycle (`init`, `tickClient`, expiry, clear-on-logout, clear-on-level-unload) to stay in step with the item that already had none of that.

Storing the positions instead of the counts removes all of it:

```java
public record PartyWandData(BlockPos bound, List<BlockPos> tables, List<BlockPos> seats,
        List<BlockPos> foods, int hunger, int needed, List<Candidate> candidates) { ... }
```

"How many chairs" is `seats.size()`, so the counts are not data to transmit — they are a projection of the positions. One transport, one copy, and the renderer holds **no state at all**: it reads the held wand's component each frame and returns immediately when no wand is held. Nothing to expire, nothing to clear, no tick handler, and no way for the drawing to disagree with the overlay because both read the same field.

`/gensokyo party preview` records onto the wand in the sender's hand (`PartyWand.recordIfHeld`), taking the already-planned `Outcome` so preview does not walk the room twice and report on a different area than it just printed. A **send** deliberately does not: the host is holding a letter, and silently rewriting a wand somewhere in their inventory because they sent a party is a surprising thing for a debug tool to do to an item. The wand records *scans*, and a send is not one.

Staleness is unchanged and is now the only staleness there is: the food is being eaten while the wand is held, which the overlay already flags by distance.

`StructureOutlineRenderer.renderPosSet` and the new `renderBound` are the shared primitives; `renderPosSet` takes a colour so the three sets can be told apart.

One detail of the text readout survives unchanged: the result is a snapshot of a party that was only *simulated* — the food is being eaten while the wand sits in a hand — so the overlay flags a reading taken more than 8 blocks from its table as possibly stale.

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
- **A guest belongs to the chunk that owns the party, not the chunk she stands in.** Both the anchor table and the food centre lie inside the walk's reach, so a party routinely straddles a chunk border. Guests therefore carry the owning `ChunkPos` explicitly and look the party up by that — never by `new ChunkPos(table)`, which silently misses and makes her dismiss herself on her first tick. Same reasoning behind `/gensokyo party info|end` searching the neighbourhood through `PartyAttachment.near` instead of one chunk.
- **A guest claims one chair per party and keeps it for the visit.** `HomeSearchUtil.setSitting` is a one-shot call with no un-sit counterpart, so once seated she stays on *that* chair — she dismounts to walk to a dish beyond her reach and sits back down on the same one, and `claimSeat` is first-write-wins so a rearranged table cannot walk her off it. Seat reassignment after someone leaves is not implemented.
- **No cross-dimension concern.** A guest clone spawns in the host's level and `party_taste` is dimension-independent, so any character may be invited anywhere. Noted only so it is not rediscovered as a bug later.
- **A guest's seat and dish search is a fixed 6-block box, not `PartyArea.scan`.** `PartyArea.scan` walks a whole room; this is a triple loop over `GUEST_RADIUS`, swept by the **party** on a timer (§6.5) rather than by each guest for herself, because a guest asks for the list every time she draws a dish and that is every few seconds for the length of her visit. A banquet laid out over tens of blocks is not something a guest walks to and back anyway. This is the one place the scan distance deliberately does not apply, and the reason the scan radius and the guest radius are two different numbers.
- **A guest leaves when her `leaveChance` comes up, so a big table empties on its own.** A satisfied guest rolls 5% after each 5-second sit, which in simulation means a guest facing a 200-dish spread leaves after about eleven dishes — always by the roll, never by running the spread out. That is the intended shape and it is also a real tuning decision: 5% per sit is tuned so a guest lingers about a hundred seconds of sitting before wandering off, and a datapack author who wants a longer party raises `fullSeat` *or* lowers `leaveChance`, which are two different flavours of the same dial. `partyGuestRadius` keeps everyone inside the room but does not affect this.
- **The area draw is wand-only, and scan-only.** It never renders for a player not holding a party wand, so it is debug output with no gameplay footprint. It is also not refreshed by a real send (§9.2) — only by a wand click and by `preview`.
- **`en_ud` lang variants** regenerate with the rest; zh_cn entries go under `src/test/resources/gensokyolegacy/lang/zh_cn/` and the `ResourceOrganizer` main is rerun afterwards.

---

## 11. Resolved questions

Owner answers, 2026-10-06.

1. **`tea_table` / `magic_table`** — excluded from the feature. `party_table` is built from `LARGE_TABLE` only, and neither block is modified. Note that they needed no support workaround regardless: `large_table` already passes both survival checks unmodified (§2.3).
2. **Overlapping parties** — rejected by chunk scan. The party's owning chunk is the one holding the centre of the detected food; every chunk the food touches is scanned for an existing live party before a send is allowed.
3. **Chairs are a hard requirement** — zero seats rejects the send outright. A new `party_seat` tag carries our chairs and stools plus optional third-party seats; seating itself reuses `HomeSearchUtil.setSitting`, which no-ops on blocks that are not `ISeatableBlock`.
4. **Cost** — food only. No cooldown, no reputation gain or loss. Gifts are the whole return, sized on how much appeal that guest personally consumed (§6.4).
5. **No letter cooldown** — the letter names no character, so there is nothing to rate-limit; a repeated send simply re-runs the same seeded draw.
6. **Cross-dimension guests** — no issue.
7. **Eligibility lives in `party_taste`** (later decision, reversing the earlier fallback). A character with no entry does not come to parties at all; `PartyTaste.of` returns null and `PartySend.plan` skips her. There is no separate whitelist, so the data map is the single place that says who is invitable, and an unwritten character is absent by decision rather than accidentally. `PartyTaste.DEFAULT` remains only as a template to copy — nothing reads it at runtime. See §5.2.
