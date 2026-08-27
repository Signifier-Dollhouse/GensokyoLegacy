# Suggestions — Cross-Cutting Improvements

This document synthesises issues and improvement directions across the three systems audited: `YoukaiEntity/CharacterData`, `Structure-Bed-Entity`, and `Home/Index (preset vs custom)`. Items are ordered by impact and grouped by risk.

## 1. Correctness Bugs

### 1.1 `StructureKey.support` inverted for preset structures — HIGH

**Where:** `content/attachment/index/StructureKey.java:40`

```java
public boolean support(CharacterConfig config) {
    return structure().equals(CUSTOM) || !config.structure().equals(structure);
}
```

`!equals` means a `CIRNO` bed (`config.structure = cirno_nest`) is considered *supported* by every structure *except* `cirno_nest`, and *unsupported* by its own home. The tick path in `YoukaiBedBlockEntity.java:39` and `LocatedBlockEntity.java:34` both gate on `key.support(config)` (or `home.supportEntity`), so preset respawn is currently broken or only works for `CUSTOM`. The debug path `YoukaiBedBlockEntity.getDebugPacket:75` correctly uses `config.structure().equals(key.getStructure().location())`, highlighting the inconsistency.

**Fix:** Change to `config.structure().equals(structure)`. Add a unit test for `support` covering `CUSTOM` vs preset match/mismatch. Backfill `CharacterConfig` for the 3 hard-coded beds to point at their intended structure ids (or `CUSTOM` if stand-alone) before flipping.

### 1.2 `StructureHomeHolder` synthetic fallback masks missing registrations — MEDIUM

**Where:** `content/attachment/home/structure/StructureHomeHolder.java:of` (`//TODO 结构自定义` fallback injecting `CIRNO`)

When `StructureConfig` DataMap has no entry for a discovered `Structure`, a dummy config containing `CIRNO` is synthesised. Combined with 1.1, this silently makes any unknown structure appear to "support" every bed except its dummy id. It hides `GLStructureGen` emptiness.

**Fix:** Replace fallback with `null` return + warning log, or with an explicit `DataMap` validation at server start that lists all `structureManager` structures lacking `StructureConfig`.

### 1.3 `RoomVerifier.BlockData.updateShape` culling is wrong — MEDIUM

**Where:** `content/attachment/home/custom/RoomVerifier.java:57` `//TODO wrong implementation`

`updateShape` infers face culling from `shape.bounds` span tests (`min==0 && max==1` per axis) rather than querying `state.skipRendering` / voxel collision per face. Thin walls, stairs, slabs misclassify as culling or not. Impact is custom home `isInside` accuracy.

**Fix:** Replace bounds heuristic with per-face solidity check, e.g. `Block.isFaceFull(shape, dir)` or `state.getBlockSupportShape`. Cache result in `BlockData.cull` as now, but compute via shape API.

## 2. Design / Cohesion

### 2.1 Bed-Bound logic split across three owners

`LocatedBlockEntity` owns `key` locating; `YoukaiBedBlockEntity` owns `IndexStorage.blockTick + home.tick`; `BedRefData` owns respawn/dedup; `HomeModule` owns heartbeat. Locating runs once (`located` flag).

**Invariant correction:** Structures and block entities are immovable, and destroying either half of a `YoukaiBedBlock` atomically removes the other half (`YoukaiBedMethods.updateShape` → `AIR` + `YoukaiBedBlock.playerWillDestroy` for creative). Therefore the one-shot bind does *not* go stale due to movement, and no `relocate()` API for translation is needed or correct to introduce. Staleness can only arise from explicit deletion (wand `DELETE`, structure regeneration, or `sl.removeBlock`) — which already cleans the relevant maps via `IndexStorage.remove` / `attachment.custom.remove` / `BedRefData` dupe path.

**Proposal (reduced):**
- Keep the one-shot `located` semantics; do not add move-tracking. If a `relocate()` is ever added, scope it strictly to explicit deletion flows (e.g. `StructureEditToServer.DELETE` already removes `IndexStorage` entry).
- Extract a single `BedHomeLinker` service with `locate(BE)`, `heartbeat(BedRefData)`, `tickHome(IHomeHolder)` so the HEAD-tick sequence is testable outside level tick.

### 2.2 Global `IndexStorage` + chunk `StructureAttachment` dual-write

`IndexStorage` (global) and `StructureAttachment` (chunk) both persist the same `StructureKey`. `BedRefData.bedPos` is only a HEAD pos; the FOOT is implicitly co-located and paired-destroyed, so no separate FOOT state exists. Chunk unload vs global entry can still diverge when the chunk is deleted but `IndexStorage` entry remains.

**Proposal:**
- Document ownership: `IndexStorage` is source-of-truth for entityId/bedPos (HEAD); `StructureAttachment` for geometry; never duplicate paired bed state.
- A periodic consistency sweep is *optional* (not required for movement) — if added, scope it to loaded chunks only (e.g. once per 6000t ~= 5 min, drop entries where `sl.isLoaded(bedPos)` and no `LocatedBlockEntity` HEAD exists and `IHomeHolder.of(key)==null`), to avoid forcing chunk loads. Explicit deletion paths already handle the common case.

### 2.3 `CharacterData` thresholds are magic numbers scattered across files

`MAX/MIN 300/-300`, `150/-50/-150` state splits, `onHurt` losses `1/5/10/20`, `dailyUpdate ±1 to 150/-150`. No config, no datamap override.

**Proposal:** Promote to `GLModConfig.SERVER` with sensible defaults; or to `CharacterConfig` per-entity `reputationParams`. Keep current constants as defaults for backwards compat.

### 2.4 `TaskBoard` priority vs schedule interaction is subtle

`addPrioritizedActivity` injects `VALUE_ABSENT` requirements for all *higher* priority memories into *lower* activities, enforcing exclusiveness via brain predicate. The `TALK` prioritized activity (`MEM_TALK,100`) plus `FIGHT` auto-injected at `build()` (`ATTACK_TARGET,0`) produce `FIGHT > TALK > …` but schedule still proposes `REST/AT_HOME` at night regardless of `MEM_TALK`. Readers expect `TALK` to interrupt `REST`.

**Proposal:**
- Document numerically in `youkai_character.md` diagram (already done) and add an inline comment in `TaskBoard.buildBrain` enumerating resulting `Activity` order for current boards.
- Add a debug overlay line for `getActivity()` vs `getBrain().getActiveNonCoreActivity()` to catch drift.

## 3. Performance & Robustness

### 3.1 `IHomeHolder.find` can force-load chunks (immovability note)

**Where:** `IHomeHolder.java:42` `sl.getChunkAt(pos.offset(ix*16,…))`

`getChunkAt` without `false` forces generation. Custom 3×3 scan on every bed locate and on debug hover inflates I/O for wilderness beds.

**Fix:** Use `sl.getChunk(x,z, ChunkStatus.FULL, false)` nullable and skip if `null`. For debug, fall back to cached `StructureInfoClientManager` rather than re-scanning unloaded chunks.

### 3.2 `IntegrityVerifier` tag checks vs `isOutside` diverge

`IntegrityVerifier.process` classifies abnormal by `current.getBlock()!=ref.getBlock()` + `blocksMotion()` heuristics, while `isOutside/isPrimary/wouldFix` use `StructureConfig` tags. A block can be outside per tag but still count as abnormal.

**Proposal:** Unify classification through `StructureConfig` tags only (use `config.isOutside/isPrimary/wouldFix`) and add a single integration test that asserts `AbnormalCache` after a controlled `setBlock` sequence.

### 3.3 `CharacterAttachment.tick` scans full map at dawn

Per player per 24000t iterates `LinkedHashMap values`. Cheap today (≤14 entries), but grows with new characters. No issue, but document as `O(C)` dawn cost.

### 3.4 `RoomVerifier` allocs unbounded `BlockData` cache

`Map<BlockPos, BlockData> data` retains every visited column's height scan `BlockData` objects for the whole `run`. For a max `48×48` room with height scan up to 15, this is ~34k entries per verify. Fine for wand use, but running multiple concurrent verifies (e.g. mass SCAN) could pressure.

**Proposal:** Clear `data` after `acceptColumn` or scope it to column, or add an early `MAX_SIZE` check before BFS expansion (already present but after `visitColumn`).

## 4. Suggested Roadmap (ordered)

1. **Hotfix** `StructureKey.support` (1.1) + add regression test; remove synthetic fallback or gate behind `GLModConfig.debug` (1.2).
2. **Fix `RoomVerifier` culling** (1.3) + add unit tests `RoomVerifierTest` covering thin wall, door, slab/slope cases. Include a test that destroying either half of the bed (`HEAD` or `FOOT`) leaves no orphan block (assert both positions `AIR` via `updateShape` / `playerWillDestroy`).
3. **Unify lifecycle** — introduce `BedHomeLinker` for testability; do **not** add movement/`relocate()` (structures/BEs immovable); optional loaded-chunk-only stale-entry prune; avoid `getChunkAt` force-load (2.1, 2.2, 3.1).
4. **Config surface** — move reputation params + `discard/respawn/wander` defaults to `GLModConfig` / `CharacterConfig` with datamap override (2.3).
5. **Observability** — debug packet consistency (`getDebugPacket` vs `support` predicate), brain debug overlay, `AbnormalCache` tag unification tests (2.4, 3.2).

## 5. What Went Well (keep)

- Dual persistence model is sound: chunk cap for geometry + global index for entity binding gives clear ownership and survives chunk unload.
- `TaskBoard` DSL correctly captures the `always/exclusive/random × prioritized/scheduled` matrix without vanilla `Brain` boilerplate; incremental throttling via `PerformanceConstants` keeps TPS stable even with statistical integrity sampling.
- `CharacterData` facade `CharDataHolder` + explicit `CharDataToClient` sync keeps packet surface minimal (per-entity-type, not full map).

