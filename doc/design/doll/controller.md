# Doll Controller Block

The player capability ledger (pairing.md) is one way to host dolls. A **doll controller block** is the other: a placeable block that hosts its own resident dolls and deploys them as hovering companions, with no player owner. It is the second implementation of `DollHost`.

## 1. `DollHost` — the shared host interface

`content/attachment/doll/DollHost.java`: anything that hosts doll data and pairs it with the deployed `BaseDollEntity`.

```java
public interface DollHost {
    @Nullable DollData findSummoned(UUID uuid);
    void update(BaseDollEntity doll);
}
```

- `DollAttachment` (player ledger) and `DollControllerBlockEntity` (block) both implement it.
- The `DollData` key is the entity's own game uuid (never serialized on entity or item), so entity-side lookups always hit the real living entity and the inverse check (world → host) is identical for every host type — `BaseDollEntity.getHost()` resolves a player-owned doll to the capability and a block-hosted doll to this block (entity.md §4).

## 2. `DollControllerBlock`

`content/block/functional/doll/DollControllerBlock.java` implements `ShapeBlockMethod`, `UseWithoutItemBlockMethod`, `UseItemOnBlockMethod`, `OnReplacedBlockMethod` (l2modularblocks).

- Shape: `Block.box(2, 0, 2, 14, 13, 14)` — a small pedestal.
- **Use with a doll item** (`useItemOn`): if the block entity exists and the stack is a doll item, `be.install(stack)`; on success consumes the item unless creative. Non-doll items pass to default block interaction.
- **Use with empty hand** (`useWithoutItem`):
  - **Sneak-use** → `be.eject(player)` — reclaim the resident doll as a doll item (into the player's inventory).
  - **Normal use** → `be.deploy()` (if a stored doll exists) else `be.recall()` (if a deployed one exists) else pass.
- **Block broken** (`onReplaced`, not a same-block move): `be.onDestroy()` — never lose a doll; each resident is dropped as a doll item (see §4).
- Client side short-circuits with a success/consume guess so there is no ghost interaction.

> **Registration status**: the block-entity method is currently commented out (`DollControllerBlock.java:39`) and the block/BE registration is pending. Until the BE registration and `BlockEntityBlockMethodImpl` are wired, the block is defined but not yet functional in-world.

## 3. `DollControllerBlockEntity`

`content/block/functional/doll/DollControllerBlockEntity.java` extends `BaseBlockEntity`, implements `TickableBlockEntity` and `DollHost`, and is `@SerialClass`.

- `MAX_DOLLS = 4` — one block can host up to four resident dolls.
- `@SerialField public final List<DollData> dolls` — an **ordered list**, unlike the player's `LinkedHashMap<UUID, DollData>` keyed by a live entity uuid. Installed dolls are `STORED` and have **no uuid yet**; deployed ones are keyed by the live entity's uuid and are `SUMMONED`.

### 3.1 Resident management

| Method | Transition | Notes |
|---|---|---|
| `install(ItemStack)` | item → `STORED` | Requires space; `DollData.fromItemData(stack, above().getCenter(), dim, 0)`; rejects health ≤ 0. Item consumed by the block. |
| `deploy()` | `STORED` → `SUMMONED` | Picks the first stored doll with a valid type and health > 0; sets `yRot` to the block's facing; `spawnFromData(found, getDeployPos())`. |
| `recall()` | `SUMMONED` → `STORED` | Picks the first deployed doll; writes values back, discards the entity, back to `STORED`. |
| `eject(ServerPlayer)` | resident → item | Prefers a deployed doll (recall first), otherwise the first stored one; `makeItem` + `dolls.remove` + `placeItemBackInInventory`. |
| `onDestroy()` | all → item (dropped) | Deployed dolls: write values, discard, drop item at the entity's last position (or `data.position`). Stored dolls: drop at the block. List cleared. Never loses a doll. |

`findSummoned(uuid)` returns the first entry that is `SUMMONED` with a matching uuid. `update(doll)` ignores dolls not homed to this block and otherwise calls `doll.writeValuesTo(data)` + `setChanged()`.

### 3.2 Spawning from stored data: `spawnFromData`

```java
data.uuid = be.getUUID();          // fresh game uuid, BEFORE addFreshEntity
data.state = DollState.SUMMONED;
data.dimension = sl.dimension().location();
data.position = pos;
be.setHome(worldPosition);          // block-hosted, no owner
be.readValuesFrom(data);
sl.addFreshEntity(be);
```

The entity is created from `data.type` via the entity-type registry, must be a `BaseDollEntity`, is **homed** at the block (so `getHost()` resolves to this block entity), and the entry is **re-keyed to the new uuid before the entity joins the level** — exactly the player-summon ordering (pairing.md §3.1). The block's own `sync()` is left to the caller.

Deployment geometry: `getDeployPos()` = block center offset `1.5` blocks in the block's `HORIZONTAL_FACING` direction, `+0.5` y — i.e. the doll hovers in front of the block. `getFacing()` reads `BlockTemplates.HORIZONTAL_FACING`.

### 3.3 Tick and lifecycle

- **`tick()`** (server, every 20 game ticks): for each `SUMMONED` entry, if the live entity exists and is homed here → `writeValuesTo(data)`; otherwise discard any stray entity and respawn from `data.position` (or the deploy pos) when type and health are valid, else fall back to `STORED`. Any change → `setChanged()` + `sync()`.
- **`onLoad()`**: every `SUMMONED` entry reverts to `STORED`, because dolls are never chunk-serialized (entity.md §5) and a reloaded block has no live entity. The player must redeploy.

### 3.4 Future work seam

`getDeployedDoll()` returns the first currently materialized doll homed to this block. Future worker tasks (mining etc.) attach their goals to that entity. A deployed block doll has **no owner**, so the player follow/look goals stay dormant; only future task goals drive it.

## 4. Block-hosted vs player-hosted

| | Player-hosted (`DollAttachment`) | Block-hosted (`DollControllerBlockEntity`) |
|---|---|---|
| Anchor | `ownerUUID` | `homePos` (`isBlockHosted()`) |
| Storage | `LinkedHashMap<UUID, DollData>`, persisted with the player | `List<DollData>` (max 4), persisted with the block entity |
| Parked handling | STORED/TEMP deferred transitions in the player tick | `STORED` = resident (no entity); `SUMMONED` = deployed; no TEMP (the block owns the doll, the entity is respawned from the entry) |
| Identity | fresh uuid per materialization; item⇔data⇔entity exclusivity via itemize | same—fresh uuid per deploy, entry re-keyed before spawn |
| Ownership | owner-gated interact, itemize back to item | no owner; block methods (install/deploy/recall/eject) manage it |
| On removal | ledger survives (capability) | `onDestroy()` drops every doll as an item |
| Goals | follow + look at owner | dormant (no owner); future task goals |

`BaseDollEntity.getHost()` decides at runtime: if `ownerUUID != null` it is a player doll; otherwise it uses `homePos` and resolves this block entity (must be loaded, must still be a `DollHost`) — block-hosted dolls are never itemized by the empty-hand `mobInteract` path (entity.md §9).

## 5. Invariants (unchanged from the player ledger)

- Identity is always the entity's game uuid; fresh per materialization, never on the item or the entity (pairing.md §2).
- An entity can never be in the world without a `SUMMONED` entry somewhere (pairing.md §3.1) — for block-hosted dolls that "somewhere" is the block, resolved through `getHost()`.
- No duplication is possible: an item is either consumed into a player ledger entry, into a block's resident data, or held as the sole copy.

## 6. Block-hosted edge cases

- **Owner offline / block chunk unloaded**: `getHost()` returns `null` and the doll self-discards (nobody can reach it). Fail-safe: when the host's chunk loads back, the block's `tick()`/`onLoad()` reconciliation runs and the entry is respawned or reverted to `STORED`.
- **Two blocks, one doll**: impossible — the install consumes the item, and the entity is born from the block's own resident data.
- **Breaking the block with the doll deployed far away**: never lost — `onDestroy()` finds the live child entity, writes values back, discards it, and drops the item **at the doll's location**, regardless of how far it strayed from the block.
- **Client**: `dolls` is a `@SerialField`, so it syncs to the client for screens/renderers; interaction results mirror the player-doll conventions (`PASS` on empty/full to avoid accidental `DollItem.useOn` fall-through).