# Sealing Pot (`sealing_pot`) — Anti-Spawn Barrier via Area Effect System

The **sealing pot** (封魔之壶) is an already-registered `DelegateBlock` in `block.functional.barriers`
(registry id `sealing_pot`, `GLBlocks.SEALING_POT`, `GLBlocks.java:108`). Originally only a voxel
shape (`SealingPotShape.java`), it is now wired into the **area effect system**
(`content/attachment/area/`) to suppress **hostile natural spawns** inside a **9×9 chunk** region
(`r = 4` chunks around the pot's chunk, configurable).

The area effect system is generic infra whose first real consumer is the sealing pot. Each pot owns
one `AreaEffectEntry`; hostile natural spawns are cancelled by a `NaturalSpawner` mixin querying
`AreaEffectManager.getAffecting(...)`.

---

## 1. Overview

```
+----------------------+      add/remove      +------------------------+
| SealingPotBlock      | -------------------> | AreaEffectManager      |
| (onPlace/onReplaced) |                      |  (content/attachment/  |
+----------------------+                      |   area/)               |
                                              |   byId                 |
+----------------------+                      |   fan-out -> chunk     |
| SealingEffectData    |  EffectData subclass |   attachments + pending|
| (isOwnerStillValid)  |                      +------------------------+
+----------------------+                                  |
                                                          | getAffecting(pos)
                                                          v
                                              +------------------------+   cancel   +--------------+
                                              | Spawn prevention hook  | ----------> | Monster spawn|
                                              | (check-zone at spawn)  |             |  cancelled   |
                                              +------------------------+             +--------------+
```

Key properties:
- **9×9 chunks = 81 chunks** (`ChunkPosRange.ofOwner(ownerPos, 4)` → `(2r+1)² = 81`). This is below
  the system's 1024-chunk cap (`AreaEffectManager.add`, `GLBlocks`-independent).
- **Owner validity** is the pot block itself — if the pot is removed/exploded, the effect is
  auto-cleaned by `LevelAreaAttachment.tickValidation` within ≤100 ticks (≈5 s bucket), plus an
  instant `onReplaced` removal for snappy feedback.
- **No block entity required.** The owner is a plain block; validity is `state.is(SEALING_POT)`.
  This keeps the block lightweight (`r=4` is small, no per-tile logic needed).

---

## 2. Components

In `src/main/java/dev/xkmc/gensokyolegacy/content/block/functional/barriers/`:

| Class | File | Role |
|---|---|---|
| `SealingPotShape` | `SealingPotShape.java` | Standalone reusable `ShapeBlockMethod` (pot silhouette). Passed as its own block method at registration; usable by future blocks. |
| `SealingEffectData` | `SealingEffectData.java` | `EffectData` subclass. `isOwnerStillValid` = block is still `SEALING_POT`. Marked `@SerialClass`. |
| `SealingPotBlock` | `SealingPotBlock.java` | Behavior-only `BlockMethod`: `OnPlaceBlockMethod`, `OnReplacedBlockMethod`, `PlacementBlockMethod` (no shape). Adds/removes the effect on place/break, prevents placing into an already-sealed chunk. |
| `ClientSealingPotRenderer` | `ClientSealingPotRenderer.java` | Client renderer (Dist.CLIENT). Draws forcefield-texture walls (texture copied from vanilla `textures/misc/forcefield.png` into `assets/gensokyolegacy/textures/barriers/sealing_pot.png`) around each sealed region. |
| Mixin | `mixin/NaturalSpawnerSealingMixin.java` | Hostile natural-spawn cancellation (see §5). |
| Config | `GLModConfig` | `sealingPotRadius` (int, default `4`, range `[1,8]`). See §7. |

The block is still a `DelegateBlock` — shape and behavior are separate `BlockMethod`s supplied
independently so either can be reused by other blocks:
`DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new SealingPotShape(), new SealingPotBlock())`.

---

## 3. `SealingEffectData` — effect payload

Modeled on the `MyAuraData` example in `doc/area_effect.md` §3. Currently the data needs no extra
fields, but keep the class because `EffectData` is the system's extension point and it's what the
spawn hook inspects.

```java
@SerialClass
public class SealingEffectData extends EffectData {

    public SealingEffectData() {}

    @Override
    public boolean isOwnerStillValid(ServerLevel level, BlockPos ownerPos, BlockState state) {
        return state.is(GLBlocks.SEALING_POT.get());
    }
}
```

- Because `AreaEffectEntry.data` is a `@SerialClass EffectData` field, `TagCodec` handles the
  inheritance and persists the concrete `SealingEffectData` type across save/load (per
  `area_effect.md` §1).
- Facing doesn't matter; validity only checks the block id.

---

## 4. `SealingPotBlock` — behavior (no shape)

Behavior-only: `OnPlaceBlockMethod` + `OnReplacedBlockMethod` + `PlacementBlockMethod`. The shape
lives in the separate `SealingPotShape` method (see §2), decoupled for reuse by other blocks.

**[DECIDED]** Limit overlap: a pot is **blocked (placement prevented)** when its chunk is already
sealed by another pot's effect.

```java
public class SealingPotBlock implements OnPlaceBlockMethod, OnReplacedBlockMethod, PlacementBlockMethod {

    // ----- prevents placement into an already-sealed chunk (item stays in hand) -----
    @Override
    public @Nullable BlockState getStateForPlacement(BlockState def, BlockPlaceContext context) {
        if (!(context.getLevel() instanceof ServerLevel sl)) return def;
        BlockPos target = context.replacingClickedOnBlock()
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace());
        return isChunkSealed(sl, new ChunkPos(target)) ? null : def;
    }

    // ----- effect activation on placement -----
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (level.isClientSide || state.is(old.getBlock())) return;
        ServerLevel sl = (ServerLevel) level;
        if (!sl.isLoaded(pos)) return;          // owner chunk must be loaded
        removeFor(sl, pos);
        if (isChunkSealed(sl, new ChunkPos(pos))) return; // non-placement paths: no effect
        ChunkPosRange range = ChunkPosRange.ofOwner(pos, GLModConfig.SERVER.sealingPotRadius.get()); // r=4 -> 81 chunks
        AreaEffectManager.add(sl, pos, range, new SealingEffectData());
    }

    // ----- removal -----
    @Override
    public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (level.isClientSide || state.is(newState.getBlock())) return;
        if (level instanceof ServerLevel sl) removeFor(sl, pos);
    }

    static boolean isChunkSealed(ServerLevel level, ChunkPos pos) { // overlap check
        for (AreaEffectEntry e : AreaEffectManager.getAffecting(level, pos))
            if (e.data instanceof SealingEffectData) return true;
        return false;
    }

    private static void removeFor(ServerLevel sl, BlockPos pos) {
        var att = GLMeta.LEVEL_EFFECT.type().getOrCreate(sl);
        att.getById().values().removeIf(e -> e.ownerPos.equals(pos)
                && e.data instanceof SealingEffectData
                && AreaEffectManager.remove(sl, e.id));
    }
}
```

Notes:
- **Overlap prevention** has two layers:
  - `getStateForPlacement` → `null`: `BlockItem` placement fails cleanly, item stays in hand
    (highest UX; via `DelegateBlockImpl`'s `reduce(PlacementBlockMethod.class, …)`).
  - `onPlace` guard → no effect if already sealed: covers non-item paths that bypass
    `getStateForPlacement` (`/setblock`, pistons, structure generation). The block may still exist
    there but is inert and will show no effect.
- **`onReplaced`** matches `onRemove` (block replaced/removed); the `removeIf` + nested
  `AreaEffectManager.remove` pattern is exactly the fallback in `area_effect.md` §3 "Owner block
  removal fallback". Guard by `e.data instanceof SealingEffectData` so we only touch our own effects
  at this position.
- **Shape split**: `SealingPotBlock` no longer implements `ShapeBlockMethod`/holds `SHAPE`;
  `SealingPotShape` is its own `BlockMethod` passed separately in `GLBlocks.java:108`:
  `DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new SealingPotShape(), new SealingPotBlock())`.
  Either method can later be reused by other blocks.
- No block entity, no inventory, no `BlockEntityEntry` — a plain placed block is sufficient.

---

## 5. Spawn prevention hook (implemented: mixin)

**[DECIDED]** Cancel **hostile natural spawns only**. The mod's youkai are spawned programmatically
(`finalizeSpawn`/`addFreshEntity`), not through the chunk natural-spawner, and programmatic hostile
spawns (eggs, spawners, commands) must remain allowed — so the hook is placed in the natural-spawn
pipeline itself.

Implementation: **`mixin/NaturalSpawnerSealingMixin`** (registered in `gensokyolegacy.mixins.json`,
server array). `@Inject(method = "spawnCategoryForChunk", at = @At("HEAD"), cancellable = true)`
cancels the whole hostile category for a chunk when it is sealed:

- `NaturalSpawner.SPAWNING_CATEGORIES` iterates every `MobCategory` except `MISC`, so inside
  `spawnCategoryForChunk` `!category.isFriendly()` is exactly `MONSTER` (hostile).
- `spawnCategoryForChunk` fires only from `NaturalSpawner.spawnForChunk` (the natural chunk
  spawner), so eggs/spawners/programmatic spawning are untouched — **youkai indifferent**.
- Sealing query: `AreaEffectManager.getAffecting(level, chunk.getPos())` and cancel on any
  `SealingEffectData`. No force-load (holder nullable), one query per hostile-category-per-chunk.

```java
@Mixin(NaturalSpawner.class)
public class NaturalSpawnerSealingMixin {
	@Inject(method = "spawnCategoryForChunk", at = @At("HEAD"), cancellable = true)
	private static void gensokyolegacy$sealHostileSpawns(MobCategory category, ServerLevel level,
			LevelChunk chunk, NaturalSpawner.SpawnPredicate filter,
			NaturalSpawner.AfterSpawnCallback callback, CallbackInfo ci) {
		if (category.isFriendly()) return;
		if (isSealed(level, chunk)) ci.cancel();
	}
	private static boolean isSealed(ServerLevel level, LevelChunk chunk) {
		for (AreaEffectEntry e : AreaEffectManager.getAffecting(level, chunk.getPos()))
			if (e.data instanceof SealingEffectData) return true;
		return false;
	}
}
```

Why a mixin instead of `MobSpawnEvent`:
- `MobSpawnEvent.FinalizeSpawn`/`PositionCheck` fire per candidate mob right before finalization;
  the mixin cancels the *entire category pass for the chunk* in one shot, which is both cheaper
  (1 query per sealed hostile chunk, not per mob) and clearer ("no hostile spawn in this chunk").
- The mod already has a mixins infrastructure; `defaultRequire: 1` catches signature drift early.

---

## 6. Wiring (done)

1. `SealingEffectData.java` — effect payload + owner-validity (pot still present). ✅
2. `SealingPotBlock.java` — `onPlace`/`onReplaced` add/remove via `AreaEffectManager`. ✅
3. `mixin/NaturalSpawnerSealingMixin.java` — hostile natural-spawn suppression. ✅
4. `GLBlocks.java:108` registration → `new SealingPotBlock()`. ✅
5. `GLModConfig` — `sealingPotRadius` (default 4). ✅
6. `ClientSealingPotRenderer.java` — barrier-texture walls around sealed region. ✅
7. No `runData` re-gen needed (no blockstate/model/datapack changes).

---

## 7. Config (implemented)

`sealingPotRadius` (int, default `4`, clamped `[1, 8]`) in `GLModConfig.SERVER` under the `blocks`
section; read in `SealingPotBlock.onPlace` via `GLModConfig.SERVER.sealingPotRadius.get()`.
`ChunkPosRange.ofOwner(pos, r)` produces `(2r+1)²` chunks (max `17²=289`, under the 1024 cap).
The overlap check uses it transitively — any pot whose effect covers the pot's chunk blocks a new pot.

Radius changes only take effect on pots replaced after the change (existing entries keep their old
range until re-placed); fine for v1.

---

## 8. Client rendering (implemented, placeholder)

**[DECIDED]** Visual feedback shows the forcefield texture ("for now", placeholder).

The renderer is generic: **each `EffectData` declares its own visuals**, so any area effect
(not just sealing) can render. `EffectData.getClientVisual()` returns a `List<AreaEffectVisual>`
(texture, RGBA, scroll speed, which faces) — an entry may contribute to **multiple render passes**
by returning several visuals; `SealingEffectData` returns one.

`ClientSealingPotRenderer` (`@EventBusSubscriber`, `Dist.CLIENT`) listens to
`RenderLevelStageEvent` at `AFTER_ENTITIES`:
- Groups visuals by texture into passes (one `BufferBuilder` + draw per distinct texture).
- **Frustum-culls each region before the buffer is built**; a pass with no visible regions is
  skipped entirely (no begin/build/draw). Also guards a null frustum.
- Walls/top/bottom of each region (`ChunkPosRange` block extents) as translucent
  `POSITION_TEX_COLOR` quads using the visual's texture
  (`gensokyolegacy:textures/barriers/sealing_pot.png`, copied from vanilla
  `textures/misc/forcefield.png`), tiled every 16 blocks via repeat wrap (UV beyond `[0,1]`).
- Texture scrolls **vertically** on walls: V offsets by `speed * time` (tiles/second, time from
  game time + partial tick); sealing uses `0.5` tiles/s.
- Walls on the **negative X/Z sides are UV-flipped** so adjacent sides read consistently from inside.
- Camera-relative coords (the level render model-view is already camera-translated at AFTER_ENTITIES).
- Full build-height columns (`minBuildHeight..maxBuildHeight`).

---

## 9. Design decisions & trade-offs

| Decision | Choice | Rationale |
|---|---|---|
| Effect data type | `SealingEffectData extends EffectData` | First concrete subclass; the system's intended extension point; persistence + sync for free. |
| Block entity | **None** | Owner validity only checks block id; no inventory/tick state. Keeps `r=4` effects cheap. |
| Effect lifecycle | `onPlace` add / `onReplaced` remove | Instant on break; `tickValidation` auto-removal (≤5 s) as fallback for explosions/`/setblock`. |
| Overlap limit | `PlacementBlockMethod` blocks placing into a sealed chunk; `onPlace` guard for non-item paths | No redundant pots; the item is not consumed on a blocked placement. |
| Block shape | separate `SealingPotShape` `BlockMethod` | Decided: shape decoupled from `SealingPotBlock` so both are reusable by other blocks. |
| Spawn hook | `NaturalSpawner` mixin (§5) | Hostile-natural-only; programmatic spawns untouched; cheaper than per-mob events. |
| Spawn scope | **Hostile (`MONSTER`) natural spawns only** | Decided: friendly mobs still spawn; youkai indifferent (programmatic → unaffected). |
| Visual | forcefield-texture walls | Decided: `textures/barriers/sealing_pot.png` (copied from vanilla `textures/misc/forcefield.png`) placeholder until a proper effect render/art exists. |
| Visual API | `EffectData.getClientVisual()` → `List<AreaEffectVisual>` | Decided: per-effect visuals (texture/color/speed/faces); one entry → multiple render passes; renderer batches by texture. Culling happens before any buffer is built. |
| Visual scroll | V offset `speed*time` on walls, `0.5` tiles/s | Decided: textures move vertically; speed is per-visual. |
| Config | `sealingPotRadius` default 4 | Decided: server-synced config, range `[1, 8]`. |
| Chunk quantize | `getAffecting(level, ChunkPos)` | No force-load; bounds check to the pot's sealed chunks. |
| Range | `r=4` (9×9=81 chunks) | Task spec; well under 1024 cap. |

---

## 10. Follow-ups

- Replace the forcefield-texture placeholder with proper per-pot visual art/volume once designed.
- Radius config changes don't live-update existing pots (re-place to apply).
- `AreaEffectManager.add`'s 1024-chunk cap is currently a no-op comment (`// enforce max`); kept
  under it by the `[1,8]` clamp, but the cap could be implemented if the system grows.
- Mod youkai intentionally remain spawnable in sealed zones (programmatic spawn); revisit only if a
  sealed zone should also bar custom youkai via other spawn paths (spawners, `mob_spawner`).
