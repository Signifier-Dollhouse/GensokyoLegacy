# Gadgets — owned things that are only themselves

A **gadget** (`DollNature.ITEM_GADGET`) is the third nature, added between the two it shares half of each with: a `DOLL` is owned, itemizable, recallable **and** loaded out; a `SUMMON` is none of those ([summon.md](summon.md)). A gadget is owned like a doll and carries no loadout like a summon:

| predicate | `DOLL` | `ITEM_GADGET` | `SUMMON` |
|---|---|---|---|
| `itemizable()` | yes | yes | no |
| `recallable()` | yes | yes | no |
| `loadoutCapable()` | yes | no | no |

So a gadget converts to its item, the glove sweeps it back like any doll, and it survives logout parked like any doll — while the two loadout-editor opens refuse it, so the glove poke falls through as an ordinary interaction instead of popping an empty menu.

The predicates above are all about the **item**. What a gadget entity *is* is the class it extends: `BaseDollEntity` pairs, follows and itemizes; `DollEntity` adds everything with a choice in it (entity.md §9.1). A gadget is the first kind to stop at the base.

## 1. Where the two kinds meet

Every itemize / park / recall gate reads `nature().itemizable()` or `.recallable()` and nothing else, so a gadget needed no new gate: the only places that had to learn about it are the ones that *mint* an item, which ask the entry's type instead of assuming `DOLL`.

The item is the inverse half of that. `DollItem` used to be a single kind — `DollData.fromItemData` wrote `DollItem.TYPE` into every entry and `DollItem.makeItem` handed back `GLItems.DOLL` — so a second entity type had no way to be summoned from, or to come back as, anything but a plain doll. It now names its kind at construction:

```java
protected DollItem(Properties props, ResourceLocation type)   // the gadget passes its own kind
```

and that constructor files the item in a static `type → item` map. Construction is the only moment available: an entry's `.get()` is unbound until registration ends, and an overridden `dollType()` would read a field the subclass has not been able to assign yet — so the kind is a constructor argument, not an override. Two things follow from that one map:

- **item → entry**: `DollData.fromItemData` reads `((DollItem) stack.getItem()).dollType()` instead of the constant. A gadget item therefore materializes its own entity type, and `DollSpawn` resolves it through the ordinary `ENTITY_TYPE` lookup.
- **entry → item**: `DollItem.makeItem(data)` looks the item up by `data.type`, falling back to the plain doll for an entry that names no kind. Every path that mints an item — `itemize`, the stray sweep, `tryItemize`, `StrayHost.onDeath`, the controller block's eject — goes through it, so none of them had to change.

`makeItem` is a lookup followed by a **virtual** `makeStack(data)`. A gadget overrides `makeStack` to `new ItemStack(this)`, which is the whole of §2.

## 2. No data on the item

A gadget item carries no components at all, and that is not a simplification — it is the item's rule. It has:

- **no `DOLL_DATA`**, so no durability bar (`DollItem.isBarVisible` is false for a stack without it) and a re-summon starts at `DollItemData.fresh()`, since `fromItemData` falls back to fresh for a data-less stack;
- **no `DOLL_LOADOUT`**, so the shift-hover tooltip is empty and the item-level editor click (`InvClickItem.handleClick`) is a no-op — there is nothing to edit, so the click is not an empty menu either;
- **no `DOLL_COLOR`** and no `CUSTOM_NAME`, since a gadget is never dyed and never renamed by a player.

The consequence to keep in mind: a gadget that somehow dies is handed straight back as a whole item. The "broken doll in an anvil with wool" loop exists for a doll because a doll is a thing the player owns and wears out; a gadget has no state to carry the damage, so it comes back whole. If a kind ever needs a wear state, it needs a real `DOLL_DATA` and a nature that says so. The Sekibanki head skips the question altogether by never being hurt (§4).

The model takes one untinted texture for the same reason — a gadget has no dye, and `DollTint` lives in the doll layer, so there is no colour to sync and no layer-1 tint index to do.

## 3. The Sekibanki head

`SekibankiHeadEntity` is the first gadget, and it is the first kind to extend `BaseDollEntity` rather than `DollEntity` (entity.md §9.1). That one choice is what makes "no loadout, no commands, no attack" structural rather than a set of flags:

1. `nature()` returns `ITEM_GADGET`;
2. its own entity type `sekibanki_head`, which is what makes `getDollTypeId()` name it, and its own geo / animation / texture;
3. `SekibankiHeadItem` — a `DollGadgetItem` naming that type, and a `runData`-generated `item/generated` model on the head's own 16x16 icon. The subclass exists only for the tooltip: what possession is, is not a doll fact, and a second gadget would not share it (§5.1).

It gets the rig from `DollRig`, which the base implements, so it animates without any of the doll layer: the doll rig's head-only skeleton (same `Head` root bone, same bone names) and its own four clips (`sekibanki_idle` / `_fly` / `_blink` / `_shout`). `registerControllers` builds the same three controllers `DollGeo` builds, pointed at those clips. Two differences are deliberate:

- the one-shot is a plain `AnimationController`, not `DollOneShotController` — that subclass flashes a lance particle at an impact point, and there is no lance and no impact here;
- `handleEntityEvent` maps **both** `EVENT_ATTACK` and `EVENT_SLAP` onto the same shout. Nothing can order the head to swing, so this is the guard rather than a live path: an id arriving anyway reaches a `sekibanki_*` clip instead of the `toy_*` one this rig does not have.

## 4. No attack, and no damage

**Not fighting** costs nothing to state: every path that could make this head act starts from a `DollEntity` — `DollBehaviorRegistry.findHand`, `DollActionHandler.canAccept`, `DollCommandGoal`, and the `instanceof DollEntity` filters in `DollCommander`, the glove's crosshair trace and the sidebar overlay. It is not asked, so there is no mask to keep at zero and no predicate to override. The sidebar roster is the one place where "every doll" and "every doll that acts" had to be separated — `DollCommander.roster` now stamps the follow formation over **every** summoned entry and returns only the `DollEntity` ones, so a gadget still holds its place in the arc without drawing a frame.

**Not taking damage** is one override of the vanilla gate:

```java
@Override
public boolean isInvulnerableTo(DamageSource source) {
    return true;
}
```

That answers before `DamageRefactorEntity.actuallyHurt` reaches `hurtFinal`, so nothing is written to `DollData.combat` at all — the entry never loses health, never reaches zero, and so never parks as a broken item. It is deliberately not "takes damage and heals back": the ledger would churn, the combat tracker would record every hit, and the entry would flap between states for no reason.

A gadget that *did* take damage would come back whole on death anyway, since there is no component to carry the loss (§2); this kind simply never reaches that state. A kind that needs a wear state would have to keep a real `DOLL_DATA` and say so in its nature.

A gadget is still a doll in every other way: it pairs, follows the owner in formation, itemizes on an empty-hand click, survives logout parked, cannot change dimension, and is never chunk-serialized.

## 5. Possession — looking out through the head

Shift and right click hands the player's **camera** to the head: the view moves to where the head is, and the mouse and the movement keys fly it there instead of the body. The body stays where it was left, up to `REACH` (48) blocks away, and keeps its own hitbox, health and vulnerability the whole time.

### The camera is vanilla's

There is no camera packet of our own. `possess` sends `ClientboundSetCameraPacket`, which is the same packet spectator-hooping uses, and the client takes the head as `Minecraft#cameraEntity`. Everything downstream of that one field is free and already correct:

- `GameRenderer.pick` traces `hitResult` from `getCameraEntity()`, so the **crosshair** points out of the head with no work here;
- the camera's position drives the frustum and entity culling, so the world is drawn around the head rather than the body.

What that field does *not* give is a to-client state the client can read back cheaply. The enter is a `mobInteract` on the head (server-side, ownership-checked); the exit is the client's `mc.setCameraEntity(player)` in `HeadPossession`, plus a `HeadReleaseToServer` so the server drops its half. Both ends therefore hold their own copy of "am I possessed", which is why the possessor is a synced `POSSESSOR` UUID rather than a plain boolean: a bare flag would tell *every* client watching the head that it was locally controlled (§5.3), and the name is what tells one apart from the rest.

**The player is not teleported.** Spectator mode copies the camera onto the player body each tick (`ServerPlayer#tick`); here that is exactly wrong, since the whole point is a body left behind. So the server sends the set-camera packet *without* going through `setCamera`, and never touches `ServerPlayer#camera`. The body is left where it was, and it keeps telling the server where it is (§5.6).

### 5.1 What the body may not do

While the camera is away, the body neither attacks nor uses items. `InputEvent.InteractionKeyMappingTriggered` is cancelled for attack, pick and use alike, which is one place rather than three: the vanilla flows it guards are the only ones that can reach them.

| key | meaning |
|---|---|
| left click | give the camera back |
| right click | the head's own empty-hand block interaction |
| middle click | nothing |

Left click is the way out precisely *because* it is cancelled anyway: a body that cannot attack has nothing to lose by giving the key that would have attacked. It needs no edge detection, since stopping possession detaches the camera and the handler returns on the next press.

The four lines the item carries - summon, collect, possess, exit - are this kind's own, and are why `SekibankiHeadItem` exists (§3): the gadget base is deliberately silent, because a gadget is the first kind and possession only the second, and neither is a fact about dolls.

### 5.2 The head's hands

Right click is *not* a vanilla interact. `HeadUseToServer` carries the block the client traced, and the server re-traces from the head's eye before acting, so a client cannot reach through a wall or past the crosshair. What it then runs is `BlockState#useWithoutItem`, the empty-hand half of a block's interaction: a door opens and a chest fills, and whatever the body was holding is neither used nor consumed. The vanilla path cannot express this, because it is defined in terms of the player's held stack.

### 5.3 The camera looks with the mouse

Everything above is about *position*. Rotation needs its own answer, and getting it wrong is what makes a possessed camera feel like the head is steering it: `Entity#getViewYRot` interpolates between the entity's last two ticks, and the head only records a heading once a tick, so the camera would advance in twenty steps a second behind a mouse that turns every frame.

So while this head is the camera on this client, its view rotation *is* the player's — `getViewYRot` and `getViewXRot` answer with `Minecraft`'s own player rather than with anything of their own. `LocalPlayer` overrides those two methods for exactly this reason, and overriding them here is enough because every consumer funnels through them: `Camera#setup` for the view, and `getViewVector` — `final`, and built on these two — for the crosshair that `GameRenderer.pick` traces. Nothing else reads the head's rotation to aim anything.

The head's own `yRot` still tracks the player once a tick, which is all the model and the hitbox need.

### 5.4 Flight is client-authoritative

The head is client-authoritative the way a boat is, and for the same reason. `BroomEntity` says it outright: the riding client runs the flight model and the server just applies it. Here the owning client runs `travel` — creative flight is an *absolute* frame, not a nudge of whatever the last one was, so there is no ground friction to fight and releasing the keys stops the head dead — and reports the finished position in `HeadMoveToServer`. `heading` reproduces `moveRelative`'s turn, diagonal normalization and all, but returns the frame instead of folding it into a running total.

What makes this *work* rather than merely rubber-band is `isControlledByLocalInstance`. For a mob with no controlling passenger, `Entity`'s version falls back to `isEffectiveAi()`, which is false on the client — and that single boolean is read in two places that together cause both symptoms of doing this without it:

- `ClientPacketListener#handleMoveEntity` applies a server rotation only `if (!entity.isControlledByLocalInstance())`, so the server's echo of an *older* mouse frame lands on top of the client's own `setYRot` and the camera never quite points where the mouse does;
- `LivingEntity#aiStep` clears `lerpSteps` and re-bases the position codec only when it *is* true, so the head's position is dragged toward the server's stale copy every tick.

Claiming local control on the possessor's client only — every other client keeps the vanilla answer and watches the head fly as an ordinary tracked entity — is what makes the mouse authoritative and the flight stop stuttering. It is the same hook `BroomEntity` uses through `getControllingPassenger`, reached here without making the player a passenger.

The input and the report straddle the level tick deliberately:

| hook | does |
|---|---|
| `ClientTickEvent.Pre` | reads the keys into `applyMove`, before the level tick |
| level tick | `travel` consumes them and moves the head |
| `ClientTickEvent.Post` | reports the position `travel` just arrived at |

That ordering is why the packet carries a position and not a set of keys: post-tick, the frame has already happened. The server's only correction is `leash`, which both sides run — the client before reporting, the server on the report — so the two agree on the leash without trusting each other's arithmetic, and the server's pass also refuses any frame longer than `MAX_FRAME`, which no flight of this head could have produced. `MAX_FRAME` covers the case the leash arithmetic alone gets wrong: a player who respawns 500 blocks away would otherwise drag the head across the world in one tick, so past `REACH` it is walked home a block at a time instead.

### 5.5 Coming home

Left click ends possession, and the head may be anywhere inside `REACH` when it does. It comes back on its own rather than being teleported: `flyingHome` claims the head from the follow goal while it is beyond `RETURN_DISTANCE` blocks *and* has line of sight to its player, and `flyHome` walks it straight back at `RETURN_SPEED`, still leashed. Inside `RETURN_DISTANCE` the goal takes over and the head rejoins the formation arc it was always in.

The line of sight test is the load-bearing part. Without it a straight-line return would drive the head into any wall between it and its player and jam there for good, since suppressing the goal is what gives it the wheel. With it the closed case is left to the goal — which is exactly the problem pathfinding exists to solve — and the two never disagree, because both read the same predicate.

Server-side only, unlike the flight it replaces: the head is an ordinary tracked entity again the moment possession ends, so running this on both sides would have the two copies pull it apart.

### 5.6 The body is still the client's

A player's position belongs to the client in vanilla. The server restores the player entity to where it was at the start of every tick (`ServerGamePacketListenerImpl#tick`), and the only thing that says where it went is the client's own movement packet. Those packets stop when the camera is not the player — `LocalPlayer#sendPosition` gates them on `isControlledCamera` — so a body left behind freezes on the server while the client's own copy keeps falling and being knocked about. The two copies are then in two places at once, and the first sign of it is the server pulling the body back to wherever its frozen copy had stayed.

So the camera leaving the player does not take the body's authority with it. The gate is a `@WrapOperation` on that one `isControlledCamera` call, which leaves the packets — and the bookkeeping that goes with them — exactly the vanilla ones: the body is placed the way a player who never possessed anything is placed, and the server's copy of it stays a copy rather than becoming the original.

## 6. Edge cases

- **The glove's summon-all picks it up.** `summonAll` scans the inventory for any `DollItem`, so a gadget item is summoned in the ring and consumed like a doll's. The generic `instanceof DollItem` test is what makes that work with no gadget-specific branch.
- **The glove cannot reach it.** `rayTraceDoll` filters `instanceof DollEntity`, so right-clicking the head with the glove falls through to the mode action rather than opening an editor. That is the same outcome the `loadoutCapable()` gate would have produced, one layer earlier.
- **The controller block ignores it.** `DollControllerBlockEntity` ejects `DollItem.TYPE` only; a gadget is a player-owned kind and the block has no roster slot for one.
- **Recall counts it.** A gadget is recallable, so `recallAll` reports it in its `RECALLED` count and hands back a plain item — the same sweep a doll gets, with no separate path.
- **It holds a formation slot.** `roster()` stamps `formationIndex` / `formationTotal` over every summoned entry, so a gadget takes its place in the follow arc instead of stacking on the owner's shoulder.
- **Possession cannot outlast the head.** A doll is a projection of a ledger entry and is discarded the moment that entry has no host, so `remove` releases the camera rather than leaving a client looking out of an entity that is gone. `tick` also releases on a possessor who has left the level.
