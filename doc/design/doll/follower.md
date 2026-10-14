# Followers — temporary non-doll escorts

A **follower** uses the whole doll machinery — pairing, the follow formation, the command goal, the deferred damage pipeline — but is not a doll: it has no item form, cannot be recalled, has no loadout, and usually runs out of life. Everything here is already true of a character's dolls (`AliceDollHost`, host.md); what was missing is that a *player* ledger could not express any of it, and that nothing had a lifespan.

No follower exists yet. This doc covers the API one is built from.

## 1. `DollNature` — the item axis

`DollState` says what shape an entry is in right now (`SUMMONED` / `STORED` / `TEMP`). `DollNature` says what it is **allowed to become**, and it is declared once, on the kind:

```java
public abstract class SomeFollower extends DollEntity {
    @Override
    public DollNature nature() {
        return DollNature.FOLLOWER;
    }
}
```

`DollSpawn.materialize` stamps `data.nature = doll.nature()` onto the ledger entry as it materializes, so a host conjuring a follower never has to say anything — `data.type` already names the entity, and the entity already declares its nature. The entry mirrors it; every gate reads the entry.

| predicate | `DOLL` | `FOLLOWER` | read by |
|---|---|---|---|
| `itemizable()` | yes | no | the six places that would mint a doll item (§3) |
| `recallable()` | yes | no | `DollAttachment.recallAll` |
| `loadoutCapable()` | yes | no | the two loadout-editor opens (§2) |

Three predicates rather than one, because they are asked at three unrelated places and a future kind may well want exactly one of them flipped — a thing that cannot be recalled but *can* still be itemized by hand is a normal doll with a leash, not a contradiction.

Entries predating the field read `null` and are treated as `DOLL`, the same fallback `DollData.getColor()` uses (follower.md §5).

## 2. No loadout

A follower's `DollData.inventory` still exists — `DollLoadout.loadout()` returns it, `ledgerStack` reads it — it is simply never written, so every slot is empty and every mirrored accessor stays empty. Nothing structural had to change; only the two ways in are gated, both on `nature().loadoutCapable()` read off the entity so neither needs a host lookup:

- `DollGloveItem.interactLivingEntity` — the right-click poke. It returns `PASS` rather than `SUCCESS`, so a follower's poke falls through as an ordinary interaction instead of being swallowed by a menu that has nothing to show.
- `DollGloveHandler.tryOpenEditor` — the 16-block crosshair trace. Same reason: the crosshair falls through to the mode action.

`AliceDollHost.arm()` skips a non-`loadoutCapable` entry. It is the only automatic loadout writer outside the menu, so it is the one that would otherwise quietly put a lance in a follower's hand.

The consequence worth stating plainly: **a follower with an intrinsic melee move behaves exactly like a bare-handed doll** — `DollMeleeBehavior.strike` reads `HandMatch.bareHand()`, which is true of an intrinsic match (its stack is `EMPTY`), so it plays `toy_slapattack` and deals the bare `ATTACK_DAMAGE`. There is no weapon to swing and nothing has to pretend otherwise.

## 3. No item form, and how a follower ends

A doll's death is not an end: the reconcile pass parks it `STORED` and hands back a broken item to be repaired, because a doll is a thing the player owns. A follower owns nothing, so its death — and the end of its lifespan — are the same event, `DollLedger.drop(uuid)`: the entry is forgotten and nothing is produced. `drop` is entry-only and the caller disposes of the entity **before** calling it, or the doll spends one tick alive and unpaired and its own inverse check logs `doll.resync.orphan` at it.

There are six places that could mint a doll item, and all six ask `itemizable()`:

| site | follower behaviour |
|---|---|
| `DollAttachment.itemize` | refuses |
| `DollAttachment.park(STORED)` | refuses (`TEMP` still works — it only means "an entity again") |
| `DollAttachment.tryItemize` | **drops** the entry instead of retrying it forever |
| `DollAttachment.tickSummoned` death branch | **drops** the entry rather than parking `STORED` |
| `DollAttachment.itemizeStray` | refuses |
| `StrayHost.onDeath` | drops nothing, and the entry is consumed |

The last one is the only path outside a ledger, and it is the one way a follower could otherwise end up as an item: `becomeStray` synthesizes a detached entry when the host refuses `detach`, and `StrayHost` drops that entry as an item on death. A follower has no `SUICIDE_ATTACK` to be ordered into `becomeStray` with, so it is unreachable — but it is one guard away from reachable, and the guard is the difference.

`tryItemize` and `tickSummoned` drop rather than refuse because they are the two *deferred* paths, and a refusal there is a silently dead entry: it would be re-asked every tick forever and never clear. Dropping is the same answer `AliceDollHost.reconcile` already gives a destroyed doll (§2.1 of host.md).

### Recall

`recallAll` skips a non-recallable entry **whole** — not itemized, not parked, and its `TEMP` left as `TEMP` by the final fold so it resummons instead of waiting for an inventory slot it will never get. `summonAll` is deliberately untouched: a follower parked over an owner's logout is `TEMP`, and bringing it back is not a recall.

So a roster of nothing but followers makes the glove's summon/recall toggle a no-op on the recall half, and report `RECALLED(0, 0)`. That is correct rather than a dead button: there is nothing there to put away.

## 4. The one move — intrinsic bindings

`DollBehaviorRegistry` resolves a behavior from what a doll **holds**. A follower holds nothing, so it resolves nothing, and `canAccept` refuses every order — it is not idle, it is *incapable*, which is exactly what Alice's unarmed dolls are outside combat (host.md §4).

`registerIntrinsic` is the third binding form, keyed on the doll's class rather than a stack:

```java
// a shooter: one danmaku it fires out of nothing
DollBehaviorRegistry.registerIntrinsic("follower_shot", SomeFollower.class,
        DollActionType.REGULAR_ATTACK, GLItems.STAR_WAND.get(), DollDanmakuBehavior::new);
// a charger: the bare-hand charge, no card needed
DollBehaviorRegistry.registerIntrinsic("follower_charge", OtherFollower.class,
        DollActionType.REGULAR_ATTACK, null, DollMeleeBehavior::new);
```

Resolution order in `findBest` is **items → intrinsic → bare hand**, and both halves of that order are load-bearing:

- **After the items**, so a doll that somehow holds a wand still uses the wand. Registering an intrinsic for `DollEntity` itself would not disturb any existing doll.
- **Before the bare hand**, because `registerBareHand(REGULAR_ATTACK, DollMeleeBehavior)` is always registered and would otherwise win a follower's danmaku against its own declaration — the follower would charge at a mob it was built to shoot at.

The intrinsic names `DollSlot.MAIN_HAND` and carries `ItemStack.EMPTY`, so `ensureMainHand` finds nothing to bring forward and `bareHand()` reads true. It is matched with `isAssignableFrom`, so registering against a follower's base class covers its subclasses.

### Why the card rides on the match

`HandMatch` gained a `danmaku` field, and `DollDanmakuBehavior` fires *that* rather than re-reading `ledgerStack(MAIN_HAND)` as it used to. The old read was correct only because the stack it read was the one `ensureMainHand` had just swapped forward — a doll whose best danmaku was in the off hand got its main hand rewritten first. An intrinsic match has nothing to swap, so the two paths no longer share a location; putting the card on the match is what makes "the matched card" mean the same thing for both. It is also where a follower's card has to live, since it is in no slot at all.

## 5. Lifespan

`DollData.lifespan` is a plain tick count, `0` meaning indefinite. Entries predating the field deserialize to `0`, so no existing doll loses its life — the lifespan is opt-in per entry, and "usually" rather than "always" is one assignment away from a follower kind that has none.

The countdown runs in `DollCommander.lapse`, inside the `tick` every ledger already performs. That placement is the whole trick: expiry costs no per-host code, and an entry is only ever dropped from there and from a host's own reconcile pass.

It counts only while the host ticks, so a follower parked over an owner's logout spends no life on time it was not there — the same bargain a doll parked `TEMP` gets from its ledger surviving the session.

## 6. Building one

A follower kind is:

1. an entity extending `DollEntity`, with `nature()` returning `FOLLOWER` and its own `getDollTypeId()` via the registry entry;
2. one `registerIntrinsic` naming its one move (§4);
3. a host that materializes a `DollData` with that `type`, a `position`, and a `lifespan` — `DollSpawn.materialize` handles the rest, including the nature stamp.

Everything else it needs it already has: it pairs, follows the owner in formation, takes commands through the glove or its host, takes damage through the deferred pipeline, cannot change dimension, and is never chunk-serialized.

## 7. Edge cases

- **The roster shows it.** `DollCommander.roster` includes every summoned entry, and `DollStatus.syncActionStatus` builds its valid mask from `findHand`, so an intrinsic sets the `REGULAR_ATTACK` bit and the sidebar frame is honest. The "currently using" icon comes from `findUsing` over the synced mirror, which is client-safe over plain stacks by design — a follower shows the frame with no item in it. `DollGloveModes` then hides `SUPER` and `SUICIDE` off a follower-only roster, since nothing validates for them.
- **A follower's host must refuse `detach`.** `detach` returning null is what keeps `becomeStray` from synthesizing an entry nobody can hand back. `AliceDollHost` already documents this as her standing rule.
- **`drop` is not a discard of the world.** It only forgets the entry. A host that wants the entity gone — owner death, unload — still discards it itself, which is why `lapse` discards before it drops.
- **`trySummon` re-materializes a `TEMP` follower** through the same `DollSpawn.materialize`, so it comes back with the same nature and a fresh uuid, and re-stamps the lifespan-independent fields. A follower parked at logout resumes with the life it had.