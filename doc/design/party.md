# Party Event — Design Doc

> **Status:** design only, no code yet — owner answers applied 2026-10-06
> **Package:** `content/party/` — `core/` (math + planner + state), `item/` (letter), plus `content/entity/module/PartyModule.java` and `content/entity/behavior/task/party/YoukaiPartyTask.java`
> **Related:** `character_visit.md` (the guest-clone trick reused here), `reputation.md` (the reputation this reads)

---

## 1. Overview

A player lays block food out on tables, sets chairs, writes a letter, and sends it. Characters with enough reputation come, eat, and leave gifts on the table.

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
| **Attractiveness / fulfillment math** | `content/party/core/PartyMath.java` | pure functions, no world writes |
| **Roster selection** | `content/party/core/PartyPlanner.java` | knapsack + seat cap, pure |
| **Party state** | `GLMeta.PARTY`, a `LevelChunk` capability | host, anchor, roster, phase, counters |
| **Guest behaviour** | `PartyModule` + `YoukaiPartyTask` | walk, sit, eat, gift, dismiss |

The first two are deliberately side-effect free so `/gensokyo party preview` can run the identical code path as a real send and be guaranteed to predict it.

---

## 2. Tables, seats and food

### 2.1 Tags

Three new tags in `GLTagGen`, all block tags:

```java
/** Tables a party may be laid out on. Our large dining tables only — see §2.3. */
public static final TagKey<Block> PARTY_TABLE = block("party_table");

/** Seats. A guest needs somewhere to sit; no seats, no party. */
public static final TagKey<Block> PARTY_SEAT = block("party_seat");

/** Block foods a guest will eat. Items are never party food. */
public static final TagKey<Block> PARTY_FOOD = block("party_food");
```

Generated from tags that already exist:

```java
// GLTagGen.onBlockTagGen
pvd.addTag(PARTY_TABLE).addTag(GLTagGen.LARGE_TABLE);
pvd.addTag(PARTY_SEAT).addTag(GLTagGen.CHAIR).addTag(GLTagGen.STOOL);

// third-party entries, emitted with "required": false so absent mods drop them silently.
// Youkai's Feasts uses the same 11 wood names we do, so one loop covers both.
for (var wood : WoodType.values()) {
    String name = wood.name().toLowerCase(Locale.ROOT);
    pvd.addTag(PARTY_TABLE).addOptional(loc("youkaisfeasts:" + name + "_dining_table"));
    pvd.addTag(PARTY_SEAT).addOptional(loc("youkaisfeasts:" + name + "_dining_chair"));
}
```

`LARGE_TABLE` already contains the 11 `*_large_table` blocks; `CHAIR` the 11 `*_large_chair` plus `wooden_large_chair_scarlet_devil_mansion`; `STOOL` the 11 `*_chair` stools.

`addOptional` is the whole "not a hard mod dependency" story. Registrate writes the entry with `"required": false`, so the tag loader skips it when the mod is absent, and **no Java anywhere names another mod's block**. With neither Farmer's Delight nor Youkai's Feasts installed the system still runs — you simply have `minecraft:cake` as your only food, which is enough for a two-person party.

### 2.2 Food value

Value comes from a data map on the block, not a hardcoded list:

```java
// init/registrate/GLMeta.java
public static final DataMapReg<Block, PartyFood> PARTY_FOOD =
        GensokyoLegacy.REG.dataMap("party_food", Registries.BLOCK, PartyFood.class);

/**
 * @param appeal   how attractive one block of this food is at the table
 * @param servings how many guests one block can satisfy, for the tooltip
 */
public record PartyFood(int appeal, int servings) {

    /** Used for any block that is in {@code party_food} but has no data map entry. */
    public static final int DEFAULT_APPEAL = 4;

    public static PartyFood of(RegistryAccess access, BlockState state) {
        var data = GLMeta.PARTY_FOOD.get(access, state.getBlockHolder());
        return data == null ? new PartyFood(DEFAULT_APPEAL, 1) : data;
    }
}
```

The `DEFAULT_APPEAL` fallback is deliberate: a datapack can add `farmersdelight:roast_chicken_block` to `party_food` and it works immediately at a generic value, then gain a `party_food` data map entry later to tune it. That is the graceful-degradation path — the tag is the gate, the data map is the tuning knob, and neither requires the other mod at build time.

Registered like any other data map at the block entry:

```java
// example, on a Gensokyo food block
.item().dataMap(GLMeta.PARTY_FOOD.reg(), new PartyFood(10, 4)).build()
```

and shipped defaults for the cross-mod blocks, written with `required: false` semantics like the tags:

```jsonc
// data/gensokyolegacy/data_maps/block/party_food.json
{
  "values": {
    "minecraft:cake":                        { "appeal": 8,  "servings": 4 },
    "farmersdelight:roast_chicken_block":    { "appeal": 20, "servings": 6 },
    "farmersdelight:sweet_berry_cheesecake": { "appeal": 12, "servings": 4 }
  }
}
```

### 2.3 The support problem (verified, and why only `large_table`)

Third-party foods refuse to stay on a thin table top. Checked against the actual classes:

| Food | `canSurvive` test | Passes on `large_table`? |
|---|---|---|
| FD feasts (`farmersdelight:feasts`) | `Block.canSupportRigidBlock` → `isFaceSturdy(UP, RIGID)` | **no** |
| FD / vanilla cake | `below.isSolid()` | **no** |
| YSF saucer foods (`BaseSaucerBlock`) | `below.isSolid()` | **no** |

`large_table`'s collision shape is `Block.box(0,13,0,16,16,16)` — a 3/16-thick slab (`LargeTableBlock.java:31`). `getBlockSupportShape` defaults to the collision shape (`BlockBehaviour.java:324`), so its top face is that slab. `SupportType.RIGID` requires the top face to cover everything outside `box(2,0,2,14,16,14)`, and `isSolid()` requires a collision AABB of ≥ 0.729 volume or full height (`BlockBehaviour.calculateSolid`, `:535`). A slab fails both, so every one of those foods breaks the instant it is placed.

Fix, on `large_table` only:

```java
// LargeTableBlock
/** What may be stood on us. Full block, so third-party foods and furniture survive. */
@Override
public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
    return Shapes.block();
}
```

plus `forceSolidOn()` on the block properties, which short-circuits `isSolid()` to `true` (`BlockBehaviour.java:536`) and covers cake and the YSF saucer foods. Both are needed: `forceSolidOn` alone does not satisfy `SupportType.RIGID`.

`ShapeBlockMethod.getBlockSupportShape` is already dispatched by `DelegateBlockImpl.java:188`, and `LargeTableBlock` already implements `ShapePathFindBlockMethod extends ShapeBlockMethod`, so this is an override, not new plumbing.

**`tea_table` and `magic_table` are excluded from this feature.** They are not in `party_table` and are not modified. `tea_table`'s surface is at y=12, so food placed on it would visibly hover; `magic_table` is a two-block-wide partner block whose halves make guest pathing and seat placement ambiguous. Neither is worth the support change for a first pass, and excluding them keeps §2.1's tag trivially correct. Revisit if the party system needs a small indoor table.

**Side effects accepted:** `large_table` becomes valid support for anything — redstone dust, levers, torches, other mods' stacked furniture — and stops allowing mobs to spawn inside it. That is the ordinary behaviour of a solid table top and matches what a full block does in vanilla. It is still a behavioural change to a shipped block, so it lands in its own commit.

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
            if (!here.is(GLTagGen.PARTY_FOOD)) continue;
            // food only counts when it is actually on a table
            if (!level.getBlockState(pos.below()).is(GLTagGen.PARTY_TABLE)) continue;
            food.add(pos.immutable());
            appeal += PartyFood.of(level.registryAccess(), here).appeal();
        }
        return new PartyArea(anchor, food, seats, appeal);
    }
}
```

Two rules in six lines, and they are the whole of "strictly requires block food placed on table blocks": the food block must be in `party_food`, **and** the block directly below it must be in `party_table`. A cake on the floor contributes nothing and is never eaten.

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

`PartyArea.appeal` from §3.1: the sum of `PartyFood.appeal` over every block in `party_food` that is sitting on a `party_table` inside the scan box. Simple additive, no multipliers — the player controls it directly by choosing what to lay out.

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
 * @param minReputation below this she will not come, whatever is on the table
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
        fulfillment = Math.max(1, fulfillment);
        repBonus = Math.max(0, repBonus);
    }
}
```

The compact constructor clamps the inverted ranges so a datapack typo degrades into "no bonus" rather than a negative weight.

**An entity type with no `party_taste` entry is not party-eligible at all.** That is the whitelist. Bosses, fairies and anything not yet written stay out until someone authors them a taste — no separate flag needed.

Initial values, aimed at "two cakes seats two friends, a proper banquet seats the cast":

| Character | minRep | maxRep | repBonus | minAppeal | maxAppeal | fulfillment |
|---|---|---|---|---|---|---|
| `cirno` | 50 | 150 | 0.3 | 10 | 50 | 16 |
| `hakurei_reimu` | 100 | 200 | 0.3 | 20 | 80 | 20 |
| `kirisame_marisa` | 100 | 200 | 0.3 | 20 | 80 | 20 |
| `morichika_rinnosuke` | 100 | 200 | 0.3 | 25 | 80 | 20 |
| `alice_margatroid` | 150 | 250 | 0.15 | 30 | 100 | 24 |
| `izayoi_sakuya` | 150 | 250 | 0.15 | 40 | 120 | 24 |

Sakuya has no `character_config` and no home — she is guest-only by design (`GLEntities.java:55`) — which makes her the natural first test case for the whole system, since a party guest needs no home binding either way.

### 5.3 Fulfillment

A guest tracks the appeal she has eaten and is satisfied when it reaches her `fulfillment`:

```java
// PartyModule
@SerialField private int eaten = 0;

public void eat(PartyFood food) { eaten += food.appeal(); }

public boolean satisfied(PartyTaste taste) { return eaten >= taste.fulfillment(); }

/** 0..1 — how well she was actually served, relative to what she wanted. */
public double fulfillmentRatio(PartyTaste taste) {
    return Math.min(1.0, eaten / (double) Math.max(1, taste.fulfillment()));
}
```

The knapsack in §5.4 guarantees the *planned* total fits the spread, but food can still run short — the player can break a cake mid-party, or two guests can converge on the same dish. Fulfillment is therefore checked at runtime, not assumed from the plan. A guest who is under-served still eats what is there and still leaves a gift, but a reduced one (§6.4). This is what makes "guests consume all the food you prepare" mean something: a thin spread produces thin gifts.

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

Two FD feasts (`PartyFood(20, 6)`) and a cake (`PartyFood(8, 4)`) on three large tables, four chairs out. Spread appeal = 48, reputation 180 across the board.

| Character | appeal factor | rep factor | weight | fulfillment |
|---|---|---|---|---|
| Cirno | 48 ≥ 50? no → (48−10)/40 = 0.95 | 180 ≥ 150 → 1.3 | **1.235** | 16 |
| Reimu | (48−20)/60 = 0.467 | 180 ≥ 200? no → (180−100)/100 = 0.8 ⇒ 1.24 | **0.579** | 20 |
| Marisa | 0.467 | 1.24 | **0.579** | 20 |
| Morichika | (48−25)/55 = 0.418 | 1.24 | **0.519** | 20 |
| Alice | reputation 180 < minRep 150? no, ok. (48−30)/70 = 0.257 | (180−150)/100 = 0.3 ⇒ 1.045 | **0.269** | 24 |
| Sakuya | (48−40)/80 = 0.1 | 1.045 | **0.105** | 24 |

Budget 48, four seats out. The best subset is Cirno (16) + Reimu (20) = 36 appeal, total weight 1.814. Any third character needs at least 56, which overruns the budget. **Two guests, 12 appeal of food left over.**

| Spread | Appeal | Roster | Seats used |
|---|---|---|---|
| 2 feasts + 1 cake | 48 | Cirno, Reimu | 2 |
| 2 feasts + 3 cakes | 64 | Cirno, Reimu, Marisa | 3 |
| 3 feasts + 2 cakes | 84 | Cirno, Reimu, Marisa, Morichika | 4 |

Four guests is where the chairs run out. Laying a fifth seat and pushing the spread to ~108 appeal brings Alice in (total 100 spent) — she is a genuine "lay out a proper banquet" target rather than something a little more food trivially unlocks.

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
                                  remove the block, eaten += appeal, pick the next one
               satisfied()? → giveGift(), dismiss()
               no food left? → dismiss()
```

`HomeSearchUtil.setSitting` (`HomeSearchUtil.java:75`) already handles claiming a free seat, checking `ChairEntity` occupancy and yielding to players, and is a silent no-op when the block is not `ISeatableBlock` — so a third-party chair from `party_seat` simply has the guest stand beside it, with no special case.

**Food contention needs no locking.** On arrival a guest re-validates that the block is still `party_food`; if another guest took it first it is gone and she picks again. Worst case is a wasted walk, and with a multi-second eat animation and a 2-block approach, contention is rare. Explicit claiming would need shared mutable state across entities for no real gain.

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
| `content/party/core/PartyMath.java` | `ramp`, `weight` — pure |
| `content/party/core/PartyArea.java` | one scan: food, seats, appeal, centre, touched chunks |
| `content/party/core/PartyPlanner.java` | candidate pool + knapsack + seat cap — pure |
| `content/party/core/PartyTaste.java` | entity-type data map record |
| `content/party/core/PartyFood.java` | block data map record + `DEFAULT_APPEAL` fallback |
| `content/party/core/PartyAttachment.java` | chunk capability, phase machine, roster |
| `content/party/core/PartySend.java` | the single send entry point + rejections |
| `content/party/item/InvitationLetterItem.java` | the letter |
| `content/entity/module/PartyModule.java` | per-guest state, `arrive`/`tickServer`/`leave` |
| `content/entity/behavior/task/party/YoukaiPartyTask.java` | walk → sit → eat → gift |
| `content/rpg/condition/PartyCondition.java` | `{"type":"gensokyolegacy:party"}`, `invert` for home-bound |

**Modified**

| File | Change |
|---|---|
| `init/data/GLTagGen.java` | `PARTY_TABLE`, `PARTY_SEAT`, `PARTY_FOOD` + optional third-party entries |
| `init/registrate/GLMeta.java` | `PARTY`, `PARTY_FOOD`, `PARTY_TASTE` |
| `init/registrate/GLBrains.java` | `PARTY`, `MEM_PARTY` |
| `init/registrate/GLItems.java` | `INVITATION_LETTER` |
| `init/registrate/block/GLDecoBlocks.java` | `forceSolidOn()` on `*_large_table` |
| `content/block/deco/variants/LargeTableBlock.java` | `getBlockSupportShape` → `Shapes.block()` |
| `content/entity/youkai/YoukaiEntity.java` | `PartyModule` in `createModules()`, `isPartying()` |
| `content/entity/youkai/YoukaiFlags.java` | append `PARTYING` (ordinal is the synced bit index) |
| `content/entity/youkai/SmartYoukaiEntity.java` | `PARTY(200)` + `YoukaiPartyTask` |
| `content/entity/characters/fairy/CirnoEntity.java` | `PartyModule` in its `createModules()` override |
| `content/entity/characters/magician/AliceEntity.java` | same |
| `init/registrate/GLEntities.java` | author `party_taste` for the six initial characters |
| `content/rpg/core/CodecRegistry.java` | `PARTY` condition |
| `content/rpg/core/ServerCharacterDialogManager.java` | suppress quest `START` while partying, as for visiting |
| `content/command/GLCommands.java` | `/gensokyo party` |
| `init/data/GLModConfig.java` | `partyScanRadius`, `partyGuestRadius` |
| `init/data/GLLang.java` | strings |
| `src/main/resources/data/gensokyolegacy/data_maps/block/party_food.json` | new, hand-authored defaults incl. `required: false` cross-mod entries |
| `src/generated/resources/data/gensokyolegacy/tags/block/*.json` | regenerated by `runData` |
| `src/generated/resources/data/gensokyolegacy/data_maps/entity_type/party_taste.json` | generated |

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

---

## 10. Known limits

- **The `large_table` support change is global.** `forceSolidOn` plus a full `getBlockSupportShape` lets anything be stood on the tables and stops mobs spawning inside them. Intended, and normal for a solid table top, but it does change existing behaviour.
- **`tea_table` and `magic_table` cannot host parties.** Deliberate (§2.3). A small indoor party needs a second pass.
- **A party can only be sent on a `party_table`,** so the letter is aimed at the table rather than at the food. Clicking a food block directly does nothing.
- **Reputation is sampled once, at send.** Reputation earned during the party does not upgrade the roster and reputation lost during it evicts nobody. A guest who turns hostile mid-party keeps eating. A consequence of freezing the roster, and the reason `/gensokyo party preview` can be exact.
- **Guest eating is not interruptible.** If the player breaks the food out from under a guest she re-validates and picks again, but she cannot be redirected to a different dish.
- **Chunk unload freezes the party mid-feast.** Guests stop ticking with the chunk and resume on reload. Acceptable — the same behaviour as every other entity in the chunk — but a party in a chunk the player walks away from simply pauses.
- **No cross-dimension concern.** A guest clone spawns in the host's level and `party_taste` is dimension-independent, so any character may be invited anywhere. Noted only so it is not rediscovered as a bug later.
- **`en_ud` lang variants** regenerate with the rest; zh_cn entries go under `src/test/resources/gensokyolegacy/lang/zh_cn/` and the `ResourceOrganizer` main is rerun afterwards.

---

## 11. Resolved questions

Owner answers, 2026-10-06:

1. **`tea_table` / `magic_table`** — excluded from the feature. `party_table` is built from `LARGE_TABLE` only, and neither block is modified.
2. **Overlapping parties** — rejected by chunk scan. The party's owning chunk is the one holding the centre of the detected food; every chunk the food touches is scanned for an existing live party before a send is allowed.
3. **Chairs are a hard requirement** — zero seats rejects the send outright. A new `party_seat` tag carries our chairs and stools plus optional third-party seats; seating itself reuses `HomeSearchUtil.setSitting`, which no-ops on blocks that are not `ISeatableBlock`.
4. **Cost** — food only. No cooldown, no reputation gain or loss. Gifts are the whole return, scaled down for under-served guests.
5. **No letter cooldown** — the letter names no character, so there is nothing to rate-limit; a repeated send simply re-runs the same deterministic check.
6. **Cross-dimension guests** — no issue.
