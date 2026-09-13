# Doll Entity — `BaseDollEntity` and `DollEntity`

The doll is split across two classes to mirror the pairing/behavior responsibility boundary:

- **`BaseDollEntity`** (`content/entity/dolls/BaseDollEntity.java`) — *foundation and pairing*: identity, movement/navigation, goals, the deferred damage pipeline, the itemize interaction, the reconciliation hooks, and the never-save guard. This is the part that every doll kind shares and that keeps the ledger honest.
- **`DollEntity`** (`content/entity/dolls/DollEntity.java`) — *doll-specific entity logic and rendering-facing state*: the GeckoLib animation rig (the actual `GeoEntity`/`Animatable` implementation lives here), the per-doll tint (`DATA_COLOR`), and the planned synced held-item accessors (§8).

> Note on wording: an earlier draft pushed more logic (damage vulkan, interact, goals) down into `DollEntity`. The current layout instead keeps **all** of that in `BaseDollEntity`, because it is pairing/integrity logic that the ledger hook (`resolveDollData`, `getHost`) depends on. `DollEntity` therefore stays thin: it is the single place where doll-specific appearance/render logic and per-doll synced data live. This document describes that actual layout.

## 1. Class relationship

```
DamageRefactorEntity (l2damagetracker)
        │
        ▼
BaseDollEntity (abstract) implements OwnableEntity
        │ ownerUUID, homePos, getHost(), write/readValuesTo/From,
        │ DollMoveControl, FlyingPathNavigation, deferred damage,
        │ mobInteract itemize, tick() sync+inverse check, never-save
        ▼
DollEntity extends BaseDollEntity implements GeoEntity
        │ IDLE/MOVE anims, isLeftie, DATA_COLOR, registerControllers
```

Subclasses are the concrete doll kinds (player doll now, character dolls later); `BaseDollEntity` is abstract. All `dolls.entity.doll` registration goes through the base type; `DollData.type` (from `getDollTypeId()`) records which concrete type a given ledger entry refers to so reconciliation can validate it (pairing.md §5).

## 2. Entity type and registration

- Entity registered in `GLDolls` as `doll` (final class name `doll`, not re-registered twice). Uses `EntityType.Builder` / L2R "fixed" egg-free registration.
- Worldgen/block-gated spawns are disabled — a doll is only ever created through the ledger (summon) or a controller block (controller.md), never by random despawn-replacement voters.

## 3. Attributes, movement, navigation

```java
public static AttributeSupplier.Builder createAttributes() {
    return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH)      // 20 (DEFAULT_MAX_HEALTH constant)
            .add(Attributes.FOLLOW_RANGE, 48)
            .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0);          // doesn't take fall damage
}
```

- `DEFAULT_MAX_HEALTH = 20` is the full-health reference for the item's durability bar and for fresh (data-less) summons.
- Navigation: `FlyingPathNavigation` with doors + float allowed (flying companion).
- Move control: `DollMoveControl`, and `setNoGravity(true)` in the constructor.
- Constructor wires `moveControl = new DollMoveControl(this)` and `setNoGravity(true)` directly.

### 3.1 Movement cap

```java
public static final double MAX_SPEED = 0.5;   // blocks per tick
```

Every tick, the ellipsoidal velocity vector is hard-clamped: if its length exceeds 0.5, it is scaled back to 0.5 exactly. Applied **after** `super.tick()` so even *external* pushes (knockback, explosions, attacks) cannot exceed the cap. The follow goal's preferred speed is well below the cap, so the follow behavior is unaffected; the clamp only reins in the extreme cases. The cap is documented in the constant comment so its coupling to the reference-goal speed stays visible.

The follow goal's >12-block / cross-dimension **teleport** remains the catch-up mechanism (it does not violate the cap — teleporting is not velocity). `canChangeDimensions()` may be overridden to `true` as optional hardening.

### 3.2 Goals

```java
goalSelector.addGoal(1, new FollowDollOwnerGoal(this));      // follows the owner
goalSelector.addGoal(99999, new LookAtDollOwnerGoal(this)); // always faces the owner when idle
```

Goals live in `content/entity/dolls/goals/` (moved out of `content/entity/behavior/goals/` so all doll goals sit together). `FollowDollOwnerGoal` reuses the `DollMoveControl`; `LookAtDollOwnerGoal` runs at the max priority so the doll always presents its face to the owner (matches the always-face-camera merchant pattern).

## 4. Owner, home, and host resolution (`getHost`)

The doll's pairing anchor is either a **player owner** (`ownerUUID`) or a **controller block** (`homePos`):

```java
public void setOwner(Player owner) { this.ownerUUID = owner.getUUID(); }
public boolean isOwner(Player player) { ... }
public @Nullable UUID getOwnerUUID() { return ownerUUID; }   // OwnableEntity

public void setHome(BlockPos pos) { this.homePos = pos.immutable(); }
public boolean isBlockHosted() { return homePos != null; }   // player-owned ⇔ block-hosted
public boolean isHomedTo(BlockPos pos) { ... }
```

`getHost()` resolves the pairing ledger one way or the other:

- Player-owned → `level().getPlayerByUUID(ownerUUID)`; if that player is a `ServerPlayer`, return their `DollAttachment` (`GLMeta.DOLL.type().getOrCreate(sp)`). **No `setOwner` → hosted = null → self-discard** (must have an owner or a home).
- Block-hosted → the `DollControllerBlockEntity` at `homePos`, only if the chunk is loaded; null when the chunk is unloaded or the block is gone.
- **`null` means the host is unreachable** (owner offline, home chunk unloaded, block removed) → the doll self-discards; its data stays in the ledger and a fresh entity is respawned from the cached values when the host is reachable again (pairing.md §5).

## 5. Never persisted into chunks

```java
@Override
public void addAdditionalSaveData(CompoundTag compound) {
    compound.putByte("DollNeverSave", (byte) 1);   // marker only
}
@Override
public void readAdditionalSaveData(CompoundTag compound) {
    this.ownerUUID = compound.hasUUID("OwnerUUID") ? compound.getUUID("OwnerUUID") : null;
    this.setNoGravity(true);
}
```

- Only a **marker** is written to the chunk for paired dolls. No paired-doll state (damage, color, data — only the owner uuid for context) is ever chunk-serialized; **all** paired state lives in the player capability (pairing.md §2). Any non-stray doll found inside a chunk on load is a leftover: it reads back only a marker, so it cannot be a genuine entity, and reconciliation discards it (the on-load check in §6).
- Exception: **stray** dolls (control.md §5.4) have no ledger, so the chunk is their only persistence — they serialize the detached entry through `StrayHost` (l2serial `TagCodec`, presence of the compound marks stray; owner rides the base `OwnerUUID` field) and resume from it on load.
- Because paired dolls never serialize into chunks, "two entities for one uuid" is impossible — an entity only reappears by fresh-tick spawning, which always mints a fresh uuid (pairing.md §3.1).

## 6. Ledger sync inside `tick()`

```java
private static final int SYNC_INTERVAL = 10;      // periodic value sync
private static final int INVERSE_INTERVAL = 20;   // world → host inverse check
```

`tick()` does three pairing jobs on the server:

1. **Movement cap clamp** (§3.1), on both sides.
2. **Lazy value sync** every `SYNC_INTERVAL` ticks: `host.update(this)` — the *significant values* (type, dimension, position, facing, name, `lastUpdate`) are written into the owner's `DollData`. Cheap scalars, never a full NBT snapshot (pairing.md §2.2).
3. **Inverse check** every `INVERSE_INTERVAL` ticks: `host.findSummoned(getUUID())` must return a **SUMMONED** `DollData` whose recorded type matches `getDollTypeId()`; otherwise the doll logs **`doll.resync.orphan`** and `discard()`s itself. This is the world→ledger honesty net (pairing.md §5.2).

`onJoinLevelCheck()` runs the same inverse check once when the entity first joins a level (covers freshly-spawned and on-disk strays), via the join-level event. Entity-join *and* periodic checks both short-circuit if there is no host (unreachable → discard; the entry reconciles to TEMP and re-summons later).

## 7. Value sync: `writeValuesTo` / `readValuesFrom`

`BaseDollEntity` provides the scalar-both-ways pair; `DollEntity` extends it to carry the doll tint:

```java
// BaseDollEntity
public void writeValuesTo(DollData d) {
    d.type = getDollTypeId();
    d.dimension = level().dimension().location();
    d.position = position();
    d.yRot = getYRot();
    d.customName = getCustomName();
    d.lastUpdate = level().getGameTime();
}
public void readValuesFrom(DollData d) {
    applyData(d.combat);            // seed entity CombatData from the authoritative DollData.combat
    setCustomName(d.customName);
    if (d.position != null) setPos(d.position);
    setYRot(d.yRot);
}

// DollEntity
@Override
public void writeValuesTo(DollData d) { super.writeValuesTo(d); d.color = getColor(); }
@Override
public void readValuesFrom(DollData d) { super.readValuesFrom(d); setDyeColor(d.color); }
```

- On read, the entity was **just created** (doSummon/spawnFromData), so its base `MAX_HEALTH` is already the configured default — nothing to reset. Position/yRot were set by the caller *before* spawning, so `readValuesFrom` only applies them.
- Combat is the single source of health and is **never null**: `applyData(d.combat)` (l2damagetracker `DamageHost`/`CombatData`) seeds the entity's `CombatData`. `DollData.getColor()`'s default is red, matching `BaseDollEntity.getColor()` default.

## 8. Doll-specific logic and rendering in `DollEntity`

`DollEntity` implements `GeoEntity` — the entire GeckoLib animation rig and rendering-facing data are *here*, not in the base:

- Four `RawAnimation` loops selected by movement + handedness: `hover_idle_l/hover_idle_r/hover_move_l/hover_move_r`. `isLeftie` is rolled once per instance (fresh entity per summon → consistent per doll), interpolated by `DollModel`/renderer only for the correct hand.
- `DATA_COLOR` is a synced `EntityDataAccessor<Integer>` (`EntityDataSerializers.INT`, default `DyeColor.RED.getId()`), exposed as `getColor()` / `setDyeColor(DyeColor)` and used by `DollModel` for per-`DyeColor` texture lookup. It flows through `write/readValuesTo/From` (overriding `BaseDollEntity.getColor()`'s red default each way).
- `registerControllers(AnimatableManager.ControllerRegistrar)` adds the "all" controller with the pure `dollAnimController(event)` state machine; `getAnimatableInstanceCache()` returns `GeckoLibUtil.createInstanceCache(this)`.

### 8.1 Planned additions (landing with `loadout.md`)

When the four-slot loadout ships, the per-slot synced item accessors land **here** (this is the doll-specific logic side of the split):

- Four `EntityDataAccessor<ItemStack>` (`EntityDataSerializers.ITEM_STACK`): `DATA_MAIN_HAND`, `DATA_OFF_HAND`, `DATA_CLOTH`, `DATA_CORE` (+ the guard in syncher for `setTag(null)`), default empty.
- Item rendering deferred as a TODO: `DollRenderer` (or the doll renderer in `content/entity/characters/`) will render the main-hand / off-hand items with a `TridentItemRenderer`-style transform anchored to the doll model's hand joint, and the cloth item on the doll's back. Until then the accessors are written but not drawn.
- Behavior side (action selection from these slots) is specified in `control.md` and stays base-side (it is pairing-independent logic); only the synced storage + rendering are `DollEntity` concerns.

## 9. Interaction and damage (pairing logic, lives in the base)

- **`mobInteract`**: sneak-interact (owner or creative, both sides) opens the loadout editor (`DollLoadoutMenu`, loadout.md §4); empty main hand, owner or creative on the server: `DollAttachment.itemize(...)` — the entity → item conversion (pairing.md §3.2). Block-hosted dolls are *not* itemized here (they belong to the controller block; controller.md). Non-sneak interactions with items pass through.
- **Deferred damage pipeline**: `takeDamage` (intercept) and `onHeal` (hook) both call `resolveDollData()` first. If a paired SUMMONED entry exists, the change lands in `DollData.combat` first (via `deferCombatData`), then is mirrored into the entity's `CombatData` (+ `CombatToClient.send` when the amount actually changed). No paired entry → plain damage path unaffected. Death-would-be and expressions flow through `DamageRefactorEntity`'s `preventDeath`/`actuallyHurt` untouched.
- **Targetability**: `canBeSeenAsEnemy()` returns `false` (pairing.md §6) — never targeted by hostile mobs, still takes damage.