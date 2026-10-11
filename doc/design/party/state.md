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

**Ticking.** The party advances its phase from the host's chunk tick at 5 Hz via the capability `tick` hook, the same one `CharacterAttachment` uses for daily decay (`CharacterAttachment.java:22`). Nothing in a party needs 20 Hz resolution: guests are walking and eating on multi-second timescales.

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
