# Who Comes — appeal, weight and the draw

Three quantities, all pure functions over plain values: how much the spread appeals, how much each character wants it, and how much it actually feeds anyone. A weighted draw under a shared hunger budget turns them into a roster.

## 1. Attractiveness of the spread

`PartyArea.appeal` from area.md §2: the sum of `PartyFoodValue.appealPerFood` over every block the walk found that is food and is laid out on or against a `party_table`/`party_seat`. Simple additive, no multipliers — the player controls it directly by choosing what to lay out, and the per-serving number comes from the food's real properties rather than a table of hand-picked values.

**Each distinct food is counted once, and the serve count does not multiply.** `appealPerFood` names its entries by food id, and the scan keeps a set of the ids it has already paid for, so a second cake by the door adds no appeal and neither do two racks of the same bun. What is being measured is how *good* the spread is, and seven slices of one cake is not seven times as good. The scan is the only place that dedupe can live, because it is the one walk over the whole spread; everything downstream receives a finished number. `PartyFoodValue.appealPerServe` is the guest's own number and is deliberately the weighted one — she eats every slice, so three slices move her three times.

This does not make a partly-eaten dish worthless. `PartyArea.fulfillment` is the other quantity and **is** counted in serves: the budget a spread offers is the total number of servings on the table, and a cake down to two slices still seats two guests. That is what makes one cake a meal for several guests instead of a single indivisible unit.

## 2. Guest weight

### 2.1 The two quantities from the food

The food on the tables is measured **twice**, and keeping them apart is what makes §4
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
14 and 14 — so the §5 worked example is unaffected. They diverge as soon as saturation or
effects are involved: `cooked porkchop` is appeal 12, hunger 13, while a slice with two
beneficial effects is appeal 10, hunger 2.

### 2.2 The weight

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

The return is an **integer** because that is what §4's draw consumes. `weight` is a
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
 * @param hungrySeat    ticks between dishes while she is still hungry; 60
 * @param fullSeat      ticks between dishes once she is only grazing; 160
 * @param leaveChance   chance per fullSeat that she calls it a day; 0.05
 * @param repGain       share of the appeal she consumed that the host is paid in
 *                      reputation with her; 0.05
 * @param weight        her weight at full interest; 100 is ordinary
 */
public record PartyTaste(
        int weight,
        int minReputation, int maxReputation, double repBonus,
        int minAppeal, int maxAppeal, int fulfillment,
        int hungrySeat, int fullSeat, double leaveChance,
        double repGain
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
            PartyMath.weight(taste, area.appeal(), rep), taste.fulfillment(), rep,
            chars.invited(type)));
}
```

The last field is the personal-invitation flag, spent by the send that actually puts her on the roster — send.md §1.

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

**That null is load-bearing, and it was quietly given back once.** `PartyTaste.of` briefly
fell back to `DEF` — the same record, with `weight` 0 — and nothing broke visibly: she had
weight 0, so `PartyPlanner.select` dropped her before she could be drawn, and only the
readouts showed it. The wand and `/gensokyo party preview` list **every** candidate rather
than every guest, so Koishi appeared in both as a dark-grey line with weight 0 and the
whole of `DEF`'s numbers, reading as a candidate rather than as an absence. A character who
could never come was being shown to the host as one who might. `DEF` is a template that
nothing reads at runtime now, and `PartyModule` uses `ofOrDefault()` for the one case that
genuinely needs a value: reading a guest's manners mid-visit, where a row pulled out from
under her should cost her the rest of her manners and not the rest of her meal.

The `DEFAULT` template is sized against the derived appeal of food.md §1, where a whole vanilla cake
is worth 14: `maxAppeal` 24 is under two cakes, and `fulfillment` 8 is just over half of
one.

Shipped values, in `data/gensokyolegacy/data_maps/entity_type/party_taste.json`. **These
seven rows are the whole eligible cast** — every other character, including the remaining
eight youkai, has no entry and does not come:

| Character | weight | minRep | maxRep | repBonus | minAppeal | maxAppeal | fulfillment | repGain |
|---|---|---|---|---|---|---|---|---|
| `cirno` | 100 | **0** | 150 | 1.0 | 4 | 20 | 6 | 0.05 |
| `hakurei_reimu` | 100 | 0 | 200 | 1.0 | 10 | 32 | 20 | 0.05 |
| `kirisame_marisa` | 100 | 0 | 200 | 1.0 | 20 | 32 | 20 | 0.05 |
| `morichika_rinnosuke` | 100 | 0 | 200 | 1.0 | 20 | 32 | 20 | 0.05 |
| `alice_margatroid` | 100 | 0 | 250 | 1.0 | 24 | 40 | 12 | 0.05 |
| `ibuki_suika` | 100 | 0 | 250 | 1.0 | 26 | 40 | 10 | 0.05 |
| `izayoi_sakuya` | 100 | 0 | 250 | 1.0 | 30 | 48 | 20 | 0.05 |
| *(everyone else)* | — | — | — | — | — | — | **not eligible** | — |

`hungrySeat` 60 and `fullSeat` 160 are the same for all seven and are not shown above;
`leaveChance` is 0.05 for all of them except Suika, who leaves at 0.1. Those three are per
character so that a glutton and a nibbler can be told apart, not because much currently is.

Every one ships `weight` 100, so these are exactly the pre-`weight` numbers scaled by 100
— the axis is authored but not yet tuned, and moving one is a datapack edit. Likewise
`minRep` 0 and `repBonus` 1.0 everywhere: the reputation axis is wired but flat, so nobody
is locked out and nobody gets extra draw for knowing them well.

Cirno has the lowest fulfillment (6), which is what makes her the reliable first guest at
an early game. Suika is the other end of the same axis in a different direction — a
fulfillment of 10 and a `leaveChance` of 0.1, so she eats less than the adults and is the
quickest of the lot to call it a day. Sakuya asks the most (30) and wants the most
(48) before she is fully interested.

Sakuya and Suika have no `character_config` and no home — both are guest-only by design —
which makes them the natural test cases for the whole system, since a party guest needs no
home binding either way.

Fulfillments are 6/8/8/8/10/12 rather than the original 16/20/20/20/24/24. They were retuned once appeal stopped being hand-authored and became derived from real food properties: a whole vanilla cake is worth 14, not the 8 originally assumed, and a modded feast lands somewhere above that. A fulfillment of 16 against a 14-appeal cake would have left the no-mods-installed path unable to satisfy anyone at all.

They are also now counted in **serves**, not blocks. A guest is satisfied once the hunger she has eaten reaches her fulfillment, and eating is one serve at a time, so a fulfillment of 6 against a 2-hunger slice means three slices — she leaves the other four for whoever is next.

## 3. Fulfillment

A guest tracks the **hunger** she has eaten and is satisfied when it reaches her
`fulfillment`. Hunger, not appeal: her fulfillment is charged against the same quantity in
§4, and crediting her with appeal instead would leave a guest eating forever at a party
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

The draw in §4 guarantees the *planned* total fits the spread, but food can still run short — the player can break a dish mid-party, or two guests can converge on the last serve. Fulfillment is therefore checked at runtime, not assumed from the plan. A guest who is under-served still eats what is there and still leaves a gift, but a reduced one (send.md §4). This is what makes "guests consume all the food you prepare" mean something: a thin spread produces thin gifts.

## 4. Who comes — a weighted draw under a shared budget

Draw one guest at a time, each with probability proportional to her weight, spending her
fulfillment from a budget that starts at the spread's **hunger** (§2.1). Both constraints
hold by construction:

- `sum(fulfillment) <= capacity`, because nothing is drawn that does not fit what is left;
- `size() <= seats`, because the loop stops at the seat count.

Both sides of the first constraint are hunger, which is what makes them comparable: a
guest's `fulfillment` and the spread's capacity are the same unit, so the arithmetic means
something. The spread's **appeal** never enters here — it is computed upstream, once, and
reaches the planner only through the weights it produced.

```java
public record Candidate(EntityType<?> type, int weight, int fulfillment, int reputation,
                        boolean priority) { }

/**
 * @param rng seeded so one spread always draws one party
 * @return chosen characters, in the order they were drawn
 */
public static List<EntityType<?>> select(List<Candidate> pool, int capacity, int seats, Random rng) {
    if (seats <= 0 || capacity <= 0) return List.of();
    // invitations are the whole point of the pool: spend them before the draw
    var invited = new ArrayList<Candidate>();
    var ordinary = new ArrayList<Candidate>();
    for (var c : pool) {
        if (c.weight() <= 0 || c.fulfillment() > capacity) continue;
        (c.priority() ? invited : ordinary).add(c);
    }

    int remaining = capacity;
    var chosen = new ArrayList<EntityType<?>>(Math.min(seats, invited.size() + ordinary.size()));
    while (chosen.size() < seats && (!invited.isEmpty() || !ordinary.isEmpty())) {
        var from = invited.isEmpty() ? ordinary : invited;
        var guest = from.remove(draw(from, rng));
        chosen.add(guest.type());
        final int rest = remaining - guest.fulfillment();
        invited.removeIf(e -> e.fulfillment() > rest);
        ordinary.removeIf(e -> e.fulfillment() > rest);
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
- **An invitation buys priority, not certainty.** Invited candidates are drawn first, but
  still by weight and still under the hunger budget and seat count; if nothing they can eat
  is left, or the seats fill, an invitation does not override the arithmetic. The flag is
  carried by the candidate rather than read inside the draw so preview and send partition
  the pool identically.

### 4.1 Why not the knapsack this replaced

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

### 4.2 What was given up, and how it is bought back

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

## 5. Worked example

Vanilla only, and checkable against food.md §1: one `minecraft:cake` is 7 slices of 2 hunger = **14**, and its appeal is also 14 (§2.1), so the spread sizes below are both. Reputation 180 across the board. Recomputed against the shipped `party_taste.json`.

> The roster below predates `ibuki_suika`, who is eligible and so should have a row here. It
> is a seeded draw, so her weights have to be recomputed by running the selection rather than
> worked out by hand — until someone does, read this as the shape of the result and not as the
> full cast.

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
