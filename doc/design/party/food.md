# Party Food — what a dish is worth

Appeal is **derived from what a food actually is to eat**, and counted **per serving** rather than per block. There is no `party_food` tag: a block is party food exactly when a registered `BlockFoodHandler` recognises it, reports servings left, and one of those servings is worth eating — and the `party_food` data map only revalues a block that already passed. The per-mod specifics (counting conventions, cake, Farmer's Delight, Youkai's Feasts, Kaleidoscope Cookery) are in `food_mods.md`.

## 1. Food value

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

### 1.1 What makes a block food

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

### 1.2 Invalid food is not food

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

## 2. `BlockFoodHandler` — how a block food is read and eaten

The derivation above needs three facts about a *placed* block, and they differ per mod, so there is one implementation per kind of food in `content/attachment/party/food/`. A handler answers them **in one call**, and assembles the `FoodResult` itself:

```java
public interface BlockFoodHandler {
    /** Everything one serving is, or INVALID. Assembled from the four below. */
    default FoodResult result(BlockState state) {
        return result(serving(state), state.getBlock(), serves(state), food(state));
    }

    /** How many servings are left right now. One by default. */
    default int serves(BlockState state) { return 1; }

    /** The stack a guest is handed for one serving. The block's own item by default. */
    default ItemStack serving(BlockState state) { return new ItemStack(state.getBlock()); }

    /** What ONE serving is worth, read off the serving stack by default. */
    @Nullable default FoodProperties food(BlockState state) { return Servings.of(serving(state)); }

    /** Take one serving, advancing the block. Returns servings remaining. */
    int consume(Level level, BlockPos pos, BlockState state);
}
```

**One call, not four, and the reason is the serving stack.** It used to be `serves` plus `food` plus an `id(BlockState)` that the cache assembled from. That split is where two rules that matter ended up with nowhere to live: the *id* was a default method that only two handlers overrode and everyone else inherited, and the stack a guest picks up was reconstructed from that id by the caller. Both are now inside the result, so a handler cannot name a food one way and hand over another. `food` defaulting to `Servings.of(serving(state))` is the same consolidation seen from the other side — for every food with an item form, the properties *are* on the serving, and only the two handlers whose food lives elsewhere (vanilla cake's hard-coded numbers, Cookery's registry entry) override it at all.

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

## 3. `FoodResult` — one resolution, four answers

Everything above is derived from the same block state and is always wanted together: the planner needs the serve count, the appeal needs the properties, the readout wants to name it, and the guest needs the action. So it is resolved once and kept.

```java
public record FoodResult(ItemStack serving, ResourceLocation blockId, int serves,
        @Nullable FoodProperties food, FoodConsume consume) {
    public static final FoodResult INVALID =
            new FoodResult(ItemStack.EMPTY, INVALID_ID, 0, null, (level, pos, state) -> 0);

    /** The serving item's id where there is one; the block's own otherwise. */
    public ResourceLocation id() {
        if (serving.isEmpty()) return blockId;
        return BuiltInRegistries.ITEM.getKey(serving.getItem());
    }

    /** A stack of this serving, hers to keep — the result is one shared object. */
    public ItemStack servingCopy() { return serving.copy(); }
}
```

**`serving` is what a guest is handed, at the count one serving is worth.** It is nearly always one item — a feast's slice, a pie's piece, a pot's bowl, a plate's serving, a rack's bread. The exception is a **cutting board**, whose serving is the whole stack it holds: Youkai's Feasts writes its nigiri and gunkan recipes to yield two at a time and `addToPlayer` hands both over together, so a pair of sushi is one serving of a guest's `serves`, worth `Servings.of(stack, count)`, and *picked up as a pair*. That is the only food in the system whose serving is more than one item, and the reason is a property of the board rather than of the food.

**`id()` is derived, not stored.** A result carries both the serving and the block, and the serving is the more specific of the two, so the id is the serving item's wherever there is one. The fallback is the block's — and **vanilla cake is the only food that takes it**, because it is the only food in the game whose slices exist only as numbers in code: `CakeBlock.eat` feeds the player from source and the `BlockItem` carries no food, so there is no slice item to hand over and none to name. Every other dish names itself after the item a guest is given, which is what makes the readout say "youkai_sake" rather than "whatever block that was".

**`serving` is shared and mutable, so nobody hands it out.** `FoodResultCache` keeps one result per block state for the whole run; a guest holding that stack would be editing the party's answer for every other guest. `servingCopy()` is what callers use.

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

## 4. `ContainerFoodHandler` — food that is not in the block state

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

**A container reports a list, not one food.** A board with apples, cheese and a carrot is three answers with three values, and averaging them into one would misprice both the spread and the draw. Each entry is one *kind* with `serves` set to how many times a guest can come back for it — which is a property of the container, not of the item: a rack hands out one item at a time, a board hands over the whole stack.

**A container decides its own layout.** `isPutOut` is not the shared furniture test, because the rule genuinely differs by block (see below). One rule for all containers would have to be the loosest of them, which would put cutting boards on the floor into the spread.

Everything downstream of this is why `PartyFoodValue` now takes a `Level` and a `BlockPos` everywhere instead of just a `BlockState`. There is one set of methods rather than two parallel ones, and the cost is that the common case carries two arguments it does not use. The answers are a **snapshot, not a transaction**: reading the serves and then consuming is two looks at the world, so two guests converging on one dish can both read it present and the second finds nothing there. The consequence is bounded — a guest occasionally eats one dish less than she banked — and closing it properly would mean holding a reservation across ticks, which is a far larger change than the window is worth.

## 5. `FoodResultCache` — resolving it once

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

## 6. Finding the item form by permuting the id

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

## 7. The data map is an override, not a source

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
