# Party Debug Tools — the command and the wand

Two tuning tools, deliberately different: `preview` **prints** its answer, and the wand **holds** its answer on the item.

## 1. The command

```
/gensokyo party preview    spread appeal, seat count, the full candidate table with
                           weights, and the chosen roster — consumes nothing
/gensokyo party send       force a send at the table being looked at
/gensokyo party end        dismiss every guest; leftover food is left untouched
/gensokyo party info       current phase, roster, served vs budget, guest states
```

`preview` is the tuning tool and the reason the planner is pure: it calls the identical `PartyPlanner.select`, so what it prints is exactly what `send` will do. Without it, tuning `party_taste` numbers through a 30-second party with a letter to spend each attempt is miserable. `SUGGEST_PARTY_ELIGIBLE` is built off `party_taste`, mirroring how `SUGGEST_VISITORS` reads `StructureConfig.visitors()`.

`end` runs the same exit as a party that ends on its own (guest.md §2.3) — unseat, then walk away — rather than dropping every guest in one tick. A host calling it mid-banquet should watch a table wind down, not disappear. Leftover food is still untouched either way.

## 2. The debug wand

The second tuning tool, holding its answer on the item rather than printing it:

| Action | Runs on | Shows |
|---|---|---|
| hover a food block | client | serves, nutrition, saturation, attraction, serves-to-satisfy |
| hover a character | client, from synced tallies | how much she has eaten — hunger and appeal, against what she wants (§4) |
| click a food block | server | — takes one serving, as a guest would |
| click a party table | server | bound position, table/seat/food counts, hunger, appeal, **who would come — the chosen roster, heaviest weight first, not every candidate with her weight (that is `preview`'s job)** — and the area drawn in the world |
| click anything the wand cannot read | server | — clears the held reading, so a stale simulation is not left on the item (§4) |

The hover readout is computed **client-side** from the block state and the item registry, both of which the client already holds. That is not laziness: a live readout re-asks every frame, and a server round trip per frame — or a packet per block looked at — would be paid for numbers that cannot disagree with the client's own copy.

What it describes depends on what is actually on the block, and the two cases are read differently. A **single dish** is read as one serving — how many items that is, and what one of them is worth — and a cutting board's serving is the whole stack it holds, so that count is not always one (food.md §1). A **container holding several** has no single serving to describe, so it is read as the block's totals instead: serves and hunger for the block as a whole. Its appeal line is one entry per distinct food on it with the serve count deliberately not multiplying, exactly the rule the scan counts under (select.md §1). A container's food lives in its block entity rather than its state, so this is a world read — board contents and steamer racks are both synced, which is why it still works on the client, and an unloaded container simply reports nothing rather than being guessed at.

The party simulation is the opposite case. Scoring characters by reputation needs server state, so the click runs the identical `PartySend.plan` a real send calls before it commits — no party is opened, no guest spawned, no letter spent — and the answer is written to an item data component:

```java
public record PartyWandData(BlockPos bound, int tables, int seats, int foods,
        int hunger, int needed, List<Candidate> candidates) { ... }
```

A **component** rather than a packet or a chat message, because vanilla already keeps item components in sync to the client; the overlay reads it back whenever the wand is held, with no machinery of its own.

## 3. Drawing the area

The readout is text, and text cannot say *where* the food it counted is. A spread across two rooms, or a chair the walk never reached, is only visible in the world, so `PartyAreaRenderer` draws the scan: a green cube per dish, blue per seat, gold per table, inside one wireframe box over the extent of all of them.

**The positions live on the component, not on a packet.** That was the first design and it was wrong twice over. A separate `PartyAreaToClient` packet put the same facts on two transports — counts on the item, positions on the wire — so the picture and the numbers could disagree, and it needed its own lifecycle (`init`, `tickClient`, expiry, clear-on-logout, clear-on-level-unload) to stay in step with the item that already had none of that.

Storing the positions instead of the counts removes all of it:

```java
public record PartyWandData(BlockPos bound, List<BlockPos> tables, List<BlockPos> seats,
        List<BlockPos> foods, int hunger, int appeal, List<EntityType<?>> guests) { ... }
```

"How many chairs" is `seats.size()`, so the counts are not data to transmit — they are a projection of the positions. One transport, one copy, and the renderer holds **no state at all**: it reads the held wand's component each frame and returns immediately when no wand is held. Nothing to expire, nothing to clear, no tick handler, and no way for the drawing to disagree with the overlay because both read the same field.

`/gensokyo party preview` records onto the wand in the sender's hand (`PartyWand.recordIfHeld`), taking the already-planned `Outcome` so preview does not walk the room twice and report on a different area than it just printed. A **send** deliberately does not: the host is holding a letter, and silently rewriting a wand somewhere in their inventory because they sent a party is a surprising thing for a debug tool to do to an item. The wand records *scans*, and a send is not one.

Staleness is unchanged and is now the only staleness there is: the food is being eaten while the wand is held, which the overlay already flags by distance.

`StructureOutlineRenderer.renderPosSet` and the new `renderBound` are the shared primitives; `renderPosSet` takes a colour so the three sets can be told apart.

One detail of the text readout survives unchanged: the result is a snapshot of a party that was only *simulated* — the food is being eaten while the wand sits in a hand — so the overlay flags a reading taken more than 8 blocks from its table as possibly stale.

## 4. One reading at a time

The wand can describe a dish, a character, or a whole simulated party, and it has room on the overlay for exactly one of them at once. Three rules settle which, and they are not three independent decisions — they are one rule (**one reading, and a live thing beats a stored one**) applied three times:

| the player is looking at | the overlay shows |
|---|---|
| a block that is party food | **the food readout**, and no party detail |
| a character at a party | **her consumption**, and no party detail |
| anything else | **the party detail**, if the wand is holding one |

**Hovering food hides the party detail.** The two are answering different questions at the same moment — what is this dish worth, and what would this table do — and stacking them produces a twenty-line tooltip where the second half is about a table thirty blocks away. The stored reading loses to the live one every time; the wand is not a clipboard, it is a magnifier.

**A click on a block the wand cannot read clears the held reading.** Not only an invalid food block — *any* block that is neither food with something on it nor a party table, a wall included. A click that produces no reading at all means the honest result is no reading rather than the last one it happened to compute: without the clear, a simulation sits on the item indefinitely, and the player is still being told what a table downstairs would do with no sign that anything replaced it. Clicking a *table* replaces it with a fresh simulation and clicking valid food replaces it with a serving taken, so the clear only ever fires where the wand would otherwise have nothing to say.

**The invalid-food message is suppressed while a party detail is on screen.** It is the food branch's way of saying "there is nothing here", and if the party detail is showing, something *is* here — the player is simply not looking at it. The message therefore appears only when the wand holds nothing, which is exactly when it is the only thing it can honestly say. The three rules compose into one sentence: *what you are looking at wins, and what you are not looking at is dropped when it is wrong.*

### 4.1 Hovering a character — the one readout that cannot be client-side

The food readout is computed on the client because the client already holds everything it needs (§2). A guest's consumption is the exception: `PartyModule.eaten` and `appealEaten` are server state, and **nothing syncs a module's fields** — characters reach the client through `YoukaiFlags`, one bit each, and there is no module payload packet in `GLPackets`. So a client-side hover would read two zeroes off a module instance the client built itself.

The number is worth having: "eaten 8 of 12 hunger" beside a guest who is visibly still going, and the appeal total with it, is the fastest way to tell a thin spread from a generous one mid-party — and appeal is the quantity that is otherwise invisible anywhere in the whole feature (select.md §2).

**It rides the entity debug overlay rather than a packet of its own.** `DebugCharacterTooltip` already asks the server about a character the player is looking at — `CharacterRequestToServer` gathers her home, her bed, her reputation and her cooldowns and answers with formatted lines, re-requested every ten ticks and cached client-side. A guest's two numbers join that answer as two more lines:

```java
public record PartyTally(int eaten, int appeal, int wanted) { }

// CharacterRequestToServer, for anyone at a party
var party = e.getModule(PartyModule.class)
        .map(m -> m.partying()
                ? new CharacterInfoToClient.PartyTally(m.eaten(), m.appealEaten(), wanted(m))
                : null)
        .orElse(null);
```

That is strictly better than the packet this section originally called for, and worth saying why:

- **no new transport, no lifecycle.** A packet needs a client-side map keyed by entity, plus clearing it on logout and on level unload. This needs neither, because the store that already exists is the one being asked.
- **it only goes to players who can see her,** because that is who the existing requester serves; a second packet would have to re-derive that.
- **the party wand, the debug wand and the debug glasses all show it.** A debug tool should say what a guest has eaten whichever debug tool you happen to be holding, and asking three tools to each carry their own copy of the same number is how they drift apart.

**Both numbers travel, not the ratio.** `eaten` against her `party_taste` fulfillment, and `appealEaten` on its own. A ratio is what the entity can already compute and is exactly what a client cannot reconstruct from the halves without also being sent the taste — so sending the halves costs the same and is more useful.

The rejected alternative, for the record: writing it onto the wand component like the party simulation. The component is a snapshot and this is a number that moves while the player watches it, so it would be a readout that only updates when you click — which is a readout nobody looks at twice.
