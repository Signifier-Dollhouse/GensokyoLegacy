# Party State — the chunk capability

A `LevelChunk` capability, alongside the existing `STRUCTURE` and `CHUNK_EFFECT` chunk capabilities, holding the host, the anchor, the frozen roster, the phase and the counters. Its phase names are the vocabulary the guest-side procedure in `guest.md` §2 is written against.

## 1. The capability

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

The party lives in the chunk holding `center(food)` (area.md §3), so:

- it is **chunk-scoped for free** — unload the chunk and the party freezes rather than corrupting;
- it **survives host logout**, because the data belongs to the chunk, not the player;
- it is dropped with the chunk like everything else in it, so there is no stale state to garbage-collect.

**The roster is frozen when the letter is sent and never recomputed.** Re-drawing on a timer would let a guest who wandered in mid-banquet change the guest list under everyone else, and would make the outcome depend on tick order. Frozen, the roster is a pure function of (reputation, food on the table, seats available) at the moment of sending — and because the draw is seeded from the centre of that food, it is a *reproducible* one, which is what makes `/gensokyo party preview` able to predict a real send perfectly.

**Ticking.** `PartyTicker` advances every phase in the level at 1 Hz (`LevelTickEvent.Post`, `GLMetaEventHandlers.onLevelTick`). Nothing in a party needs 20 Hz resolution: guests are walking and eating on multi-second timescales. Parties are rare, so only the chunks actually hosting one are swept.

One thing does not fit that cadence — the gathering rockets of §2.1, whose gap is drawn out of a range and so cannot be honoured at 1 Hz. The driver therefore runs **two clocks off one sweep**: `tick` snapshots the active chunks twice a second and hands each pass only the work it is for. Nothing else about it changes.

### 1.1 The part that does not persist

`PartyTicker.ACTIVE` — level → the chunks currently hosting a party — is a plain `ConcurrentHashMap` in memory. It is not serialised, because nothing about it *can* be: it is a cache of which chunks to look at, and the answer is already in the chunks. The problem is that nothing ever refills it. `PartySend.send` was the only writer, so after a restart `ACTIVE` was empty and every party that had just been faithfully restored from disk sat in whatever phase it was saved in, for ever: no arrivals, no spread rescans, no closing, no guest ever told to leave. The party looked like it was there and behaved like it was gone.

The fix is to treat chunk load as a second writer:

```java
// event/GLMetaEventHandlers.java, in onChunkLoad
var party = PartyAttachment.of(sl, chunk.getPos());
if (party != null && party.active()) PartyTicker.register(sl, chunk.getPos());
```

`ChunkEvent.Load` is the right moment and is guaranteed late enough: `ChunkStatusTasks` posts it after `ChunkSerializer` has already run `readAttachmentsFromNBT`, so the restored party is readable there. It is also the right *scope*: a party whose chunk has not loaded cannot advance anyway, so registering on load rather than eagerly costs nothing and needs no scan of the region files.

Going through `PartyAttachment.of` rather than `type().getOrCreate` is deliberate — `of` re-binds `owner`, which is not a `@SerialField` and is therefore `null` in a freshly deserialized attachment. Without it the restored party would mutate correctly but never mark its chunk unsaved, so the next change would be lost too.

**Three consequences of parties resuming, all of which had to go with it:**

| | what it did | why resuming made it worse |
|---|---|---|
| the roster sweep in `tickOne` | any guest whose entity `getEntity` could not find was dropped | `getEntity` only finds **loaded** entities, and a restored party starts ticking from its chunk load — *before* the guests' entities are necessarily back. The sweep emptied the roster, `finished()` read the party as over, `reset()` wiped the state the chunk had just restored. |
| `allSeated` | an unloaded guest `continue`d, counting as seated | the party flipped `GATHERING` → `FEASTING` on the first tick after a rejoin, before anyone had sat down, and the staggered arrival became a formality nobody had to pass |
| `PartyModule#onKilled` | only called `leave()`, which never touched the roster | it was the only other caller of `removeGuest`, so with the sweep gone a corpse would pin the party open until `PARTY_TIMEOUT` |

The lesson is the same in all three: **`getEntity` returning null means "not loaded", not "not there"**, and a sweep built on it cannot tell a death from a chunk boundary. Guests now come off the roster from the two ends that can only mean it — death, and leaving.

## 2. Phase transitions

| From | To | Condition |
|---|---|---|
| `IDLE` | `GATHERING` | letter sent, roster frozen; guests are admitted one a second from here (guest.md §2) |
| `GATHERING` | `FEASTING` | every guest has claimed a chair (`PartyTicker.allSeated`) |
| `FEASTING` | `CLOSING` | the spread was empty on two consecutive food scans, **or** the last guest left with food still on the table (guest.md §2) |
| `CLOSING` | `IDLE` | every guest has finished leaving (guest.md §2) |
| any | `IDLE` | host left the area, or all guests died |

`FEASTING` has no "everyone is satisfied" exit: satisfaction is per-guest and no longer
ends anything. A guest leaves when she has eaten all the spread she can reach, so the
roster drains on its own and the party closes as the last guest goes — see guest.md §1.

**There are exactly two ways out of a feast, and food is the weaker of them.** The spread
running out ends the party; the last guest walking out ends it just as surely, with the
host's food untouched. guest.md §2 says why that is deliberate.

The `GATHERING` gate is party-wide on purpose: the table feasts as a table. Its cost is
that one guest with nowhere to sit holds the whole table out of the feast until
`PARTY_TIMEOUT` (10 min) calls the party off. A per-guest gate would not stall, but the
last guest to arrive would eat alone at an ungathered table, which reads as a bug. Seating
is a host problem either way; the timeout is the backstop for a host who never puts out
another chair.

Aborting to `IDLE` always leaves the remaining food untouched — an interrupted party wastes the guests' time, not the player's food.

### 2.1 The gathering rockets

While the table is filling, one rocket goes up every **10–20 ticks**, and the window is exactly the gap between the two things the phase machine already knows: the first guest sitting down and the last one doing it.

```
first guest seated ──▶  rocket, rocket, rocket  ──▶  last guest seated
                              10–20 ticks apart
```

`PartyTicker.rocket` is the whole of it, and it reads `anySeated` / `admitted() && allSeated` — the same pair as the `GATHERING → FEASTING` gate, deliberately, so the rockets stop when the gate would open and not ten seconds before. **`admitted()` is load-bearing**: `allSeated` alone is true the moment the guests admitted *so far* are all down, while the rest of the roster is still walking in, which would put the rockets out well before the table was full.

The gap is drawn from the range on each rocket rather than fixed, which is what makes it read as hand-fired instead of metered. `sparkAt` lives on the capability, not the ticker, so a chunk unload mid-gathering resumes the gap instead of restarting it; it starts at 0, so the first rocket is the first pass **after** the first guest sits rather than a count from the send.

**Where a rocket goes up is the whole of the difficulty.** A firework burst hurts every living thing within five blocks of it, and a guest knocked off her chair drops her seat claim — which holds the table in `GATHERING` until `PARTY_TIMEOUT` (10 min) calls it off. So `PartyFirework` fires from a **ring 12–18 blocks out** from the table: every chair is within `GUEST_RADIUS` (6) of it, so no seated guest is within reach of a burst over that ring. The ring cannot do the whole job, because a guest still walking in is out as far as the scan distance and so is the host, so a round also **skips any spot with a living entity in the column a burst could land in**, widened by the damage radius. A round with nowhere to go fires nothing and is simply tried again.

`RISE = 8` is a deliberate under-estimate of the dozen-odd blocks an unobstructed rocket climbs, and it errs in the safe direction: indoors a rocket stops at the ceiling, which is *lower*, and a burst higher than assumed is further from anything standing on the floor.
