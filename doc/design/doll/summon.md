# Summons — conjured non-doll escorts

A **summon** (`DollNature.SUMMON`) uses the whole doll machinery — pairing, the follow formation, the command goal, the deferred damage pipeline — but is not a doll: it has no item form, cannot be recalled, and has no loadout. Everything here is already true of a character's dolls (`AliceDollHost`, host.md); what was missing is that a *player* ledger could not express any of it.

The sibling nature is `ITEM_GADGET` — owned, itemizable and recallable, but loadout-free and inert: see [gadget.md](gadget.md).

No summon exists yet. This doc covers the API one is built from.

## 1. `DollNature` — the three-item axis

`DollState` says what shape an entry is in right now (`SUMMONED` / `STORED` / `TEMP`). `DollNature` says what it is **allowed to become**, and it is declared once, on the kind:

```java
public abstract class SomeSummon extends DollEntity {
    @Override
    public DollNature nature() {
        return DollNature.SUMMON;
    }
}
```

`DollSpawn.materialize` stamps `data.nature = doll.nature()` onto the ledger entry as it materializes, so a host conjuring a summon never has to say anything — `data.type` already names the entity, and the entity already declares its nature. The entry mirrors it; every gate reads the entry.

| predicate | `DOLL` | `SUMMON` | `ITEM_GADGET` | read by |
|---|---|---|---|---|
| `itemizable()` | yes | no | yes | the six places that would mint a doll item (§3) |
| `recallable()` | yes | no | yes | `DollAttachment.recallAll` |
| `loadoutCapable()` | yes | no | no | the two loadout-editor opens (§2) |

Three predicates rather than one, because they are asked at three unrelated places and a kind may well want exactly one of them flipped — a thing that cannot be recalled but *can* still be itemized by hand is a normal doll with a leash, not a contradiction.

Entries predating the field read `null` and are treated as `DOLL`, the same fallback `DollData.getColor()` uses.

## 2. No loadout

A summon's `DollData.inventory` still exists — `DollLoadout.loadout()` returns it, `ledgerStack` reads it — it is simply never written, so every slot is empty and every mirrored accessor stays empty. Nothing structural had to change; only the two ways in are gated, both on `nature().loadoutCapable()` read off the entity so neither needs a host lookup:

- `DollGloveItem.interactLivingEntity` — the right-click poke. It returns `PASS` rather than `SUCCESS`, so a loadout-free kind's poke falls through as an ordinary interaction instead of being swallowed by a menu that has nothing to show.
- `DollGloveHandler.tryOpenEditor` — the 16-block crosshair trace. Same reason: the crosshair falls through to the mode action.

`AliceDollHost.arm()` skips a non-`loadoutCapable` entry. It is the only automatic loadout writer outside the menu, so it is the one that would otherwise quietly put a lance in a summon's hand.

The consequence worth stating plainly: **a summon with an intrinsic melee move behaves exactly like a bare-handed doll** — `DollMeleeBehavior.strike` reads `HandMatch.bareHand()`, which is true of an intrinsic match (its stack is `EMPTY`), so it plays `toy_slapattack` and deals the bare `ATTACK_DAMAGE`. There is no weapon to swing and nothing has to pretend otherwise.

## 3. No item form, and how a summon ends

A doll's death is not an end: the reconcile pass parks it `STORED` and hands back a broken item to be repaired, because a doll is a thing the player owns. A summon owns nothing, so its death is the end — `DollLedger.drop(uuid)`: the entry is forgotten and nothing is produced. `drop` is entry-only and the caller disposes of the entity **before** calling it, or the doll spends one tick alive and unpaired and its own inverse check logs `doll.resync.orphan` at it.

There are six places that could mint a doll item, and all six ask `itemizable()`:

| site | summon behaviour |
|---|---|
| `DollAttachment.itemize` | refuses |
| `DollAttachment.park(STORED)` | refuses (`TEMP` still works — it only means "an entity again") |
| `DollAttachment.tryItemize` | **drops** the entry instead of retrying it forever |
| `DollAttachment.tickSummoned` death branch | **drops** the entry rather than parking `STORED` |
| `DollAttachment.itemizeStray` | refuses |
| `StrayHost.onDeath` | drops nothing, and the entry is consumed |

The last one is the only path outside a ledger, and it is the one way a summon could otherwise end up as an item: `becomeStray` synthesizes a detached entry when the host refuses `detach`, and `StrayHost` drops that entry as an item on death. A summon has no `SUICIDE_ATTACK` to be ordered into `becomeStray` with, so it is unreachable — but it is one guard away from reachable, and the guard is the difference.

`tryItemize` and `tickSummoned` drop rather than refuse because they are the two *deferred* paths, and a refusal there is a silently dead entry: it would be re-asked every tick forever and never clear. Dropping is the same answer `AliceDollHost.reconcile` already gives a destroyed doll (§2.1 of host.md).

### Recall

`recallAll` skips a non-recallable entry **whole** — not itemized, not parked, and its `TEMP` left as `TEMP` by the final fold so it resummons instead of waiting for an inventory slot it will never get. `summonAll` is deliberately untouched: a summon parked over an owner's logout is `TEMP`, and bringing it back is not a recall.

So a roster of nothing but summons makes the glove's summon/recall toggle a no-op on the recall half, and report `RECALLED(0, 0)`. That is correct rather than a dead button: there is nothing there to put away.

## 4. The one move — intrinsic bindings

`DollBehaviorRegistry` resolves a behavior from what a doll **holds**. A summon holds nothing, so it resolves nothing, and `canAccept` refuses every order — it is not idle, it is *incapable*, which is exactly what Alice's unarmed dolls are outside combat (host.md §4).

`registerIntrinsic` is the third binding form, keyed on the doll's class rather than a stack:

```java
// a shooter: one danmaku it fires out of nothing
DollBehaviorRegistry.registerIntrinsic("summon_shot", SomeSummon.class,
        DollActionType.REGULAR_ATTACK, GLItems.STAR_WAND.get(), DollDanmakuBehavior::new);
// a charger: the bare-hand charge, no card needed
DollBehaviorRegistry.registerIntrinsic("summon_charge", OtherSummon.class,
        DollActionType.REGULAR_ATTACK, null, DollMeleeBehavior::new);
```

Resolution order in `findBest` is **items → intrinsic → bare hand**, and both halves of that order are load-bearing:

- **After the items**, so a doll that somehow holds a wand still uses the wand. Registering an intrinsic for `DollEntity` itself would not disturb any existing doll.
- **Before the bare hand**, because `registerBareHand(REGULAR_ATTACK, DollMeleeBehavior)` is always registered and would otherwise win a summon's danmaku against its own declaration — the summon would charge at a mob it was built to shoot at.

The intrinsic names `DollSlot.MAIN_HAND` and carries `ItemStack.EMPTY`, so `ensureMainHand` finds nothing to bring forward and `bareHand()` reads true. It is matched with `isAssignableFrom`, so registering against a summon's base class covers its subclasses.

### Why the card rides on the match

`HandMatch` gained a `danmaku` field, and `DollDanmakuBehavior` fires *that* rather than re-reading `ledgerStack(MAIN_HAND)` as it used to. The old read was correct only because the stack it read was the one `ensureMainHand` had just swapped forward — a doll whose best danmaku was in the off hand got its main hand rewritten first. An intrinsic match has nothing to swap, so the two paths no longer share a location; putting the card on the match is what makes "the matched card" mean the same thing for both. It is also where a summon's card has to live, since it is in no slot at all.

## 5. Timeouts belong to the kind

The ledger carries **no lifespan field and no countdown**. `DollData` describes what an entry is, not how long it lasts, so a summon that expires does its own bookkeeping: it counts its own ticks (its `tick()`, a `DollModule`, or whatever else it owns) and ends itself through the same two steps every other end goes through — discard the entity, then `DollLedger.drop(uuid)`, in that order (§3).

That is deliberate rather than an omission. "Runs out of life" is one possible end among several — a summon can just as well expire on its owner leaving the level, on a post change, on its target dying, or not at all — and each of those is a fact about the kind, not about pairing. Keeping it on the kind also means:

- the countdown runs only while the kind is actually ticking, so a summon parked over an owner's logout spends no life on time it was not there;
- nothing in the framework has to guess what a parked entry's remaining life means;
- a summon that has no lifetime at all needs no flag to say so.

## 6. Building one

A summon kind is:

1. an entity extending `DollEntity` — not the bare base, because it still takes orders — with `nature()` returning `SUMMON` and its own `getDollTypeId()` via the registry entry (entity.md §9.1);
2. one `registerIntrinsic` naming its one move (§4);
3. a host that materializes a `DollData` with that `type` and a `position` — `DollSpawn.materialize` handles the rest, including the nature stamp;
4. if it expires, its own lifetime bookkeeping and the discard-then-`drop` end from §5.

Everything else it needs it already has: it pairs, follows the owner in formation, takes commands through the glove or its host, takes damage through the deferred pipeline, cannot change dimension, and is never chunk-serialized.

## 7. Edge cases

- **The roster shows it.** `DollCommander.roster` includes every summoned entry, and `DollStatus.syncActionStatus` builds its valid mask from `findHand`, so an intrinsic sets the `REGULAR_ATTACK` bit and the sidebar frame is honest. The "currently using" icon comes from `findUsing` over the synced mirror, which is client-safe over plain stacks by design — a summon shows the frame with no item in it. `DollGloveModes` then hides `SUPER` and `SUICIDE` off a summon-only roster, since nothing validates for them.
- **A summon's host must refuse `detach`.** `detach` returning null is what keeps `becomeStray` from synthesizing an entry nobody can hand back. `AliceDollHost` already documents this as her standing rule.
- **`drop` is not a discard of the world.** It only forgets the entry. A host that wants the entity gone — owner death, unload, expiry — still discards it itself, which is why every end discards before it drops.
- **`trySummon` re-materializes a `TEMP` summon** through the same `DollSpawn.materialize`, so it comes back with the same nature and a fresh uuid, and re-stamps the entry's identity fields. Whatever lifetime the kind tracks starts over with the new entity.
