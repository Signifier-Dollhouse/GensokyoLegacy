# Missing Treasure Tower (`missing_treasure_tower`) — A Lost Tower as a Tracked World Object

The character loses the treasure tower; the player has to find it in the wild, within 128 blocks of
the giver, and bring it back.
The tower is **not an item**. A per-player record in `IndexStorage` is the authority on where it is;
a plain entity is only a view of that record. The entity may be unloaded, killed or deleted at any
time — the record survives and the tower comes back. That single invariant is what makes the quest
impossible to soft-lock. Once on the ground, the tower stays there for as long as its chunk is loaded,
whether or not its owner is online or anywhere near.

---

## 1. Record

`IndexStorage` (the overworld `SavedData`) gains one map:

```java
@SerialField
private final LinkedHashMap<UUID, MissingTowerState> missingTowers = new LinkedHashMap<>();
```

`MissingTowerState` (`@SerialClass`, one entry per player who has the quest started):

| field | type | meaning |
|---|---|---|
| `quest` | `ResourceLocation` | the quest that placed the tower; gates pickup and completion |
| `dim` | `ResourceLocation` | dimension holding the ground position |
| `pos` | `BlockPos` | ground position, or the last followed position while carried |
| `origin` | `BlockPos` | centre used to re-roll after a cross-pickup: the giver's block position at start |
| `carried` | `boolean` | `false` = on the ground, `true` = following its player |
| `entity` | `UUID` (nullable) | the only entity allowed to exist for this record |
| `updated` | `long` | game time of the last transition, for diagnostics |

Every type round-trips through l2serial (`UUID` and `ResourceLocation` as strings, `BlockPos` as a
compound, `boolean` and `long` natively). The map is held by the overworld instance; each record
carries its own dimension, so a tower can be lost in the Nether or in Gensokyo.

`IndexStorage` exposes `getTower(UUID)`, `createTower(...)`, `removeTower(UUID)` and a read-only
`towers()` view. `createTower` rolls the position and, if the player already has a record, returns it
unchanged — restarting or re-syncing a quest must never duplicate a tower.

---

## 2. Placement

`origin` is the quest giver's block position at the moment the quest starts. The position is a
uniform point in the **annulus 32..128 blocks** around it:

```
d     = sqrt(lerp(32², 128², random))
angle = random * 2π
x, z  = origin + (cos(angle), sin(angle)) * d
level.getChunk(x >> 4, z >> 4)                  // getHeight does not load a chunk
y     = level.getHeight(MOTION_BLOCKING_NO_LEAVES, x, z)
```

The candidate is accepted when `pos` and `pos.above()` are empty of blocks and fluid and
`pos.below()` is a solid, non-fluid block. The 32-block minimum keeps the tower from spawning at the
giver's feet; the 128-block maximum is the placement radius (`MIN_RADIUS`, `MAX_RADIUS` constants, no
config).

A retry can land in a different chunk, so the search is bounded to **4 generated chunks**, 8 columns
each; if none pass, the tower falls back to `origin.above(2)`. `Level.getHeight` returns
`minBuildHeight` for an unloaded chunk, which is why the chunk is loaded explicitly before the
heightmap is read. The four chunks are the price of placing the tower in terrain the player has not
visited; it happens once per quest start, not per tick.

---

## 3. Entity

`MissingTowerEntity extends Entity` — a separate type, not `TreasureTowerEntity`. The latter is a
projectile tied to an item stack and a cast cycle; the missing tower is a persistent world object
whose only state is its owner.

Properties: no physics, no gravity, no collision, fire/lava immune, invulnerable, persistence
required, attackable `false`, pickable `true`, `clientTrackingRange(12)` so it is visible at 128.
Only `owner` (UUID) is saved; every other fact comes from the record.

Server tick, in this order:

1. Look up the record by `owner`. No record, or `record.entity != getUUID()` → `discard()`. This is
   what kills stale copies after a reload, a race or a completion.
2. Carried only: owner player is offline, dead or in another dimension → set the record to ground at
   the entity's current position; `discard()`. The tower reappears when its chunk loads. A ground
   tower ignores the owner entirely.
3. Carried: more than 32 blocks from the owner → set ground at the entity's current position and
   `discard()`. Otherwise follow the player at the off-hand shoulder (right/back/up offset, lerped),
   and copy the entity's block position into `record.pos` every 10 ticks. That saved position is the
   recovery spot.
4. Ground: the record must still be `carried == false` and name this entity; otherwise `discard()`
   (the record itself does not change). There is no owner-presence or distance test — the tower hovers
   at `record.pos` for as long as its chunk is loaded (the renderer adds the bob/spin).

`interact(Player, InteractionHand)` is the only way to pick it up:

- server side only; the record must be on the ground and must name this entity;
- the player must have `record.quest` started in their `QuestAttachment`; anyone else is refused;
- the owner picks it up for free;
- another quest holder **adopts** it (see §5);
- a player already carrying a tower is refused with an action-bar line.

There is no drop action. The entity never holds an item and never enters an inventory, so it cannot
be thrown, stored, burned or despawned.

The renderer draws `GLItems.TREASURE_TOWER` as a spinning, bobbing item, like `TreasureTowerRenderer`
but slightly larger (1.4) so it reads at distance. No new model or texture.

---

## 4. Spawn and discard driver

`MissingTowerEventHandlers.onLevelTick` (overworld only, every 20 game ticks) walks the records, not
the players:

```
for each (owner, record) in storage.towers():
  level = server.getLevel(record.dim); if none, continue
  if record.carried:
      P = playerList.getPlayer(owner); if none, continue   // logged out: keep carried
      if P.level != level or P.distanceToSqr(record.pos) > 32²:
          setGround(record); discard(record)
      else ensureEntity(record, level)          // restore after a crash
  else:
      ensureEntity(record, level)               // ground: chunk-bound, no distance test
```

A ground tower does not care where its owner is. `ensureEntity` spawns it as soon as its chunk is
loaded, so a player returning to a tower they left behind finds it there, and another quest holder who
wanders past it can adopt it even while the owner is offline or in another dimension.

`ensureEntity` is idempotent:

- a live entity with `record.entity` whose owner is this record → nothing;
- a UUID that resolves to anything else (wrong type or wrong owner) → discard it, clear the field;
- `!level.isLoaded(record.pos)` → wait; nothing is added to an unloaded chunk. `ChunkEvent.Load`
  calls `ensureEntity` for every **ground** record in that level whose `pos` falls in the loaded
  chunk, so the tower appears the moment the chunk loads rather than at the next second pass;
- otherwise spawn at `record.pos`, `addFreshEntity`, store the new UUID. A failed add retries next pass.

The pass is O(active records) per second and does one map lookup plus one entity lookup per record.
Nothing is scanned by entity type. The chunk hook is the only event besides the tick; it makes the
tower's lifetime exactly the chunk's lifetime, with the record as the durable backup.

---

## 5. State transitions

| event | record | entity |
|---|---|---|
| quest start | new record on the ground at the rolled position | spawns when its chunk loads |
| right-click by the owner | `carried`; `pos` follows | follows the shoulder |
| right-click by another quest holder | picker `carried`; original owner re-rolled around `origin` | rebound to the picker |
| owner dies | ground at the drop point | discarded; respawns when the chunk loads |
| owner logs out | ground at the last followed position (carried if its entity never ticked) | discarded |
| owner changes dimension or gets >32 blocks away | ground at the last followed position (stays behind) | discarded |
| owner walks away from a ground tower | unchanged | unchanged while the chunk is loaded |
| chunk unloads | unchanged | saved with the chunk |
| chunk loads | unchanged | kept, or respawned if it was missing |
| entity deleted (`/kill`, bug, datapack) | unchanged | respawned by the next pass or chunk load |
| quest completion | record removed | discarded |

**Drop point.** Always the entity's last followed position, snapped to a safe block if needed — never
the player's destination after a teleport. Death drops the tower where the player died; a long
teleport leaves it behind, so the tower cannot be shortcut back to the quest giver.

**Cross-pickup.** A player with the same quest started may take another player's tower whenever the
tower's chunk is loaded, even if its owner is away, in another dimension or offline. The picker's
record becomes `carried` on this entity; the entity's `owner` becomes the picker. The previous owner's
record is re-rolled in its existing dimension around its stored `origin`, its `entity` cleared, and its
state reset to ground — their tower is missing again somewhere else. The previous owner loses no
progress irrecoverably. If
the picker is already carrying a tower, the pickup is refused: one tower per player. A player whose
record does not exist gets one created around themselves before adoption, so the state can never be
half-migrated.

**Completion.** `MissingTowerRequirement.doComplete` removes the record and discards the entity. A
stale entity left anywhere discards itself on its next tick because its record is gone.

---

## 6. Quest wiring

`MissingTowerRequirement(ResourceLocation quest, String text)` is registered as
`requirement/missing_tower` in `CodecRegistry`.

- `start`: `IndexStorage.createTower(sp.getUUID(), quest, character.level().dimension(), character.blockPosition())`.
- `canComplete`: server side; the record exists, is `carried`, names this `quest`, and belongs to the
  completing player. The dialog manager hides the completion topic until this returns true, so the
  player has to actually be carrying the tower when they talk to the giver. The client cannot read the
  record and returns `true`, leaving the display to the progress gate below.
- `doComplete`: remove the record, discard the entity.
- `getDesc`: the translated `text` plus `n/1` from the synced `progress`; `getTrigger` is
  `EmptyTrigger`, `getMaxProgress` 1.
- `MissingTowerManager.syncProgress` writes that progress: 1 on pickup, 0 when the record goes back to
  ground or is re-rolled away from its previous owner. It reuses the existing `QuestStatusToClient`
  `UPDATE` packet, so the quest tab and the requirement line turn green while the tower is in hand
  and the completion topic stays hidden from the client. The same call on login reconciles a record
  that went to ground while its owner was offline.

`QuestRequirement.start` has no character parameter today, and the placement is supposed to be centred
on the giver rather than the player. The change is small and explicit:

- `QuestRequirement.start(QuestData, ServerPlayer, @Nullable YoukaiEntity, String)`;
- `QuestData.start(ServerPlayer, Quest, @Nullable YoukaiEntity)`;
- `QuestAttachment.start(ServerPlayer, Holder<Quest>, @Nullable YoukaiEntity)`;
- `StartQuestAction` passes `context.character()`;
- `RollItemRequirement` takes the new parameter and ignores it;
- `GLCommands.forceStart` passes the nearest matching character within 32 blocks, or null.

A null character means the quest was force-started with no giver at hand: the requirement then rolls
around the player's level and position.

Content (recommended wiring, change freely): a one-time Marisa quest `marisa/missing_tower`, unlocked
after `marisa/hakkero` (by then she owns a proper tower), title "The Missing Tower". Reward: exp 150,
reputation +15, loot `treasure_tower` ×1 and coins ×8. The quest is the intended way to obtain the
tower, which currently has no crafting recipe. `StartQuestAction` already refuses guests, so the
tower is always rolled around Marisa while she is at home.

---

## 7. The two questions

**May other players pick it up?** Yes, if they have the same quest started and are not already
carrying a tower. The tower is a world object, not a private instance: anyone who reaches its chunk
can take it, whether or not its owner is present. The pickup adopts the tower: the picker carries it,
and the original owner's tower is re-rolled around its origin, so nobody is left without a tower and
no tower is duplicated. A player without the quest can see the entity but cannot interact with it.

**What if the player picks it up and throws it away?** They cannot. There is no item form, no
inventory slot and no drop key; the entity carries no stack. The tower leaves the player only through
death, logout, a dimension change or a >32-block separation, and each of those sets the record to
ground at its last position. If the entity itself is deleted, the record is untouched and the manager
respawns it when the chunk loads; until then it waits for anyone to come and find it. The only
unrecoverable case is deleting the dimension the record names from the server, which is outside the
quest's control.

---

## 8. Recovery and edge cases

- **Server restart while carried.** The record still says `carried` and `pos` is the last followed
  position. If it is still carried on login, within 32 → the entity is spawned attached to the player;
  farther away (relog after a teleport) → the record goes to ground at `pos` and the player has to go
  back. If the entity ticked before the chunk unloaded, the record is already on the ground and the
  tower is simply waiting at `pos` to be picked up.
- **Duplicate entities.** Only the entity whose UUID equals `record.entity` survives; every other
  copy discards on its first tick. A replacement spawned after a deletion wins because the record now
  names it; the old copy, if it loads later, loses the check.
- **Chunk unload.** The entity saves with its chunk. While the chunk is unloaded `ensureEntity` does
  nothing and `ChunkEvent.Load` is what re-creates a missing one, so no duplicate is ever made.
  A tower on the ground is therefore as durable as the chunk it lies in, with the record as backup.
- **Owner offline.** A ground tower is unaffected: it stays in its chunk for anyone to find. A carried
  record waits for the owner to log back in, unless its entity ticked while offline and already put it
  on the ground.
- **Tower left behind.** Walking away from a ground tower changes nothing; the entity remains while the
  chunk is loaded, ready for the owner to return or another quest holder to adopt.
- **Terrain changed under a ground tower.** The entity is non-colliding and floats; it stays visible
  and interactable. The drop snap only runs when the state changes, not every tick.
- **One missing-tower quest per player.** The map is keyed by UUID. Supporting two would key it by
  `(UUID, quest)`; not needed while one quest exists.
- **Quest UI.** The requirement line shows `n/1` from the synced quest progress, driven by the existing
  status packet; no new packet is introduced. If playtesting shows 128 blocks is too hard to search, a
  future HUD hint (direction and distance) would be a small `toClient` packet and nothing else.

---

## 9. Files

New:

| file | role |
|---|---|
| `content/attachment/index/MissingTowerState.java` | the serialized record |
| `content/attachment/index/MissingTowerManager.java` | placement, drop snap, per-second pass, cross-pickup |
| `content/entity/misc/MissingTowerEntity.java` | the view: follow, ground hover, interaction |
| `content/entity/misc/render/MissingTowerRenderer.java` | spinning, bobbing tower item |
| `content/rpg/requirement/MissingTowerRequirement.java` | start / canComplete / doComplete |
| `event/MissingTowerEventHandlers.java` | the level pass, the chunk-load hook and the login re-sync |

Modified:

- `content/attachment/index/IndexStorage.java` — the map and its accessors;
- `init/registrate/GLEntities.java` — `missing_treasure_tower` entity, renderer, tracking range;
- `content/rpg/core/CodecRegistry.java` — `requirement/missing_tower`;
- `content/rpg/requirement/QuestRequirement.java`, `quest/QuestData.java`, `quest/QuestAttachment.java`,
  `action/StartQuestAction.java`, `requirement/RollItemRequirement.java` — the character parameter;
- `init/data/GLLang.java` — pickup/refusal messages;
- `content/command/GLCommands.java` — diagnostics and the force-start character lookup;
- `src/test/resources/gensokyolegacy/lang/zh_cn/*.json` — translations, then `organize.ResourceOrganizer`;
- `doc/design/README.md` — the new `quest/` row.

Content, not part of the core change: `init/data/rpg/MarisaQDGen.java` (the quest and its dialogs) and
`doc/quest/marisa.md` (the shipped quest row). Once those exist, `runData` also generates the quest
JSON, the loot table and the entity lang entries.

---

## 10. Diagnostics and tests

Commands under `/gensokyo missing_tower`:

- `list` — owner, quest, dimension, position, state and entity UUID for every record;
- `locate <player>` — the recorded position and the distance from the player;
- `reset <player>` — remove the record and discard the entity;
- `spawn <player>` — force the entity at the record, bypassing the loaded-chunk check.

Manual scenarios (there is no JUnit harness; `src/test/java/organize` is a utility):

1. Start the quest: a record appears, nothing enters the inventory.
2. Walk toward the tower: it appears when its chunk loads and can be picked up; it follows the player.
3. Talk to the giver while carrying: the completion topic appears, the quest completes, the record and
   entity are gone, the reward is paid.
4. Die while carrying: the entity drops at the death spot and can be retrieved.
5. Log out while carrying, log back in at the same spot: the tower is attached again, or already on
   the ground there and picked up with a right-click.
6. Teleport 1,000 blocks while carrying: the tower stays behind on the ground; no duplicate appears
   at the destination.
7. `/kill @e[type=gensokyolegacy:missing_treasure_tower]` while on the ground: it comes back on the
   next pass or chunk load, with no second entity.
8. Two players both start: A finds B's tower, takes it; B's record moves elsewhere; B can find a new
   tower and A carries the adopted one.
9. A player already carrying tries to take another: refused with the action-bar line.
10. A player without the quest right-clicks: no effect.
11. Restart the server while carrying: the tower is restored or grounded at its last position.
12. Chunk unload and reload: exactly one entity, no duplicate.
13. Ground tower whose owner logs out or walks far away: the entity stays in its loaded chunk and can
    be picked up by another quest holder.

---

## 11. Decisions

- **Record, not item.** An item can be dropped, stored, duplicated or destroyed; the quest needs an
  object that cannot be lost, so the record is the truth and the entity is disposable.
- **Separate entity.** `TreasureTowerEntity` is a projectile with a cast cycle and a carried stack;
  sharing it would let the missing tower be thrown or cast.
- **Chunk-bound ground tower.** A ground tower lives exactly as long as its chunk is loaded, with no
  owner-presence or distance test. It is a world object anyone with the quest can find and adopt, and
  the record is the backup if the entity is ever lost.
- **Adopt and re-roll.** Cross-pickup must neither duplicate nor destroy a tower; re-rolling the
  previous owner around the stored origin is the only rule that satisfies both.
- **Drop where it was, not where the player went.** A teleport must not become a free trip back to
  the quest giver.
- **Character-centred origin.** The `start` signature grows a `YoukaiEntity` rather than approximating
  the giver with the player's position; the call sites are few and the semantics stay honest.
- **Synced progress, not a new packet.** The client only needs to know whether the tower is in hand,
  and that is one bit the existing quest status packet already carries; the record stays server-side.
- **No config.** 32 / 128 / 32-block constants are tuning values; if playtests disagree they change in
  one file. The search radius is the one number most likely to move, and it stays a constant until
  there is a reason to make it per-server.
