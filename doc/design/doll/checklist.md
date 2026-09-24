# Implementation Checklist

Consolidated file/registration checklist for the doll system. Items marked **(done)** exist in code today; the rest are planned. See the individual docs for the reasoning.

## 1. Files to create

Core (all done):
- `content/attachment/doll/DollState.java`, `DollData.java`, `DollAttachment.java`, `DollHost.java`
- `content/entity/dolls/BaseDollEntity.java`, `DollEntity.java`, `DollMoveControl.java`, `DollModel.java`, `DollRenderer.java`
- `content/entity/dolls/goals/FollowDollOwnerGoal.java`, `LookAtDollOwnerGoal.java`, `DollCommandGoal.java`
- `content/item/doll/DollItem.java`, `DollItemData.java`
- `content/block/functional/doll/DollControllerBlock.java`, `DollControllerBlockEntity.java`
- `event/DollEventHandlers.java` — login/logout/death/respawn/join-level events (pairing.md §4)

Loadout (done — slots, storage, mirror, editor menu; render TODO):
- `content/item/doll/DollSlot.java` — 4-slot enum (loadout.md §1)
- `content/attachment/doll/DollInventory.java` — immutable item variant; `MutableDollInventory.java` — mutable data variant (loadout.md §2)

Control (done — framework; glove calls pending):
- `content/attachment/doll/DollCommander.java` — via `DollAttachment.commands`: volley/one-time/stop, handoff, stall guard, heal scheduling + 1-second mark prune, transient heal marks (control.md §6/§8)
- `content/entity/dolls/action/DollAction.java`, `DollActionType.java`, `DollActionMode.java`, `DollActionHandler.java`, `DollCardHolder.java` + `goals/DollCommandGoal.java` (control.md §1/§3–5, goals under `dolls/goals/`)
- `content/entity/dolls/behavior/DollBehavior.java`, `DollBehaviorRegistry.java`, `DollBehaviors.java`, `DollFriendlyFire.java` + `DollDanmakuBehavior.java`, `DollLaserBehavior.java`, `DollThrowBehavior.java`, `DollSuicideBehavior.java`, `DollHealBehavior.java` (control.md §1/§5)

- `content/entity/dolls/menu/DollLoadoutMenu.java`, `DollLoadoutScreen.java`, `DollLoadoutProvider.java`, `DollLoadoutItemHandler.java` + `GLMisc.DOLL_LOADOUT`, layout JSON, container texture (loadout.md §4)

Implemented — glove (item, 6 modes with heal/stop hidden, wheel, target cache + glow mixin, packets):
- `content/item/glove/DollGloveItem.java`
- `content/item/glove/mode/DollGloveMode.java`
- `content/item/glove/mode/DollGloveHandler.java` + 6 mode classes
- `content/item/glove/DollGloveSelectionListener.java` + `DollGloveLeftClickHandler.java`
- `content/item/glove/client/DollGloveModeWheel.java`, `DollGloveModeEntry.java`
- `content/item/glove/network/DollGloveSelectPacket.java`, `DollGloveTargetPacket.java`, `DollGloveSwingPacket.java`
- `content/item/glove/client/GloveTargetCache.java` (+ per-player server cache) + `mixin/ClientGlowMixin.java` (via `content/client/ClientGlowManager.java`) + mixins-json entry (glove.md §2)

## 2. Files to modify

- `content/attachment/doll/DollData.java` — `inventory` field (loadout.md §2) — done (mutable variant)
- `content/attachment/doll/DollAttachment.java` — destroyed-resummon revival keeping gear (§7); ledger transitions only
- `content/attachment/doll/DollHost.java` — `detach(UUID)` for stray cuts, `onDeath` hook; `StrayHost.java` holds the detached entry, answers pairing, persists via chunk save/load (control.md §5.4)
- `content/entity/dolls/BaseDollEntity.java` — pairing pipeline only (stray `getHost` branch, `die()` → `onDeath`); empty-hand itemize (arming removed, loadout.md §4)
- `content/entity/dolls/DollEntity.java` — `actions` field, one `DollCommandGoal`, vanilla shield hooks (§5.6), never-null ledger-direct loadout API, `becomeStray()`; 4 synced slot accessors, `writeValuesTo`/`readValuesFrom` (loadout.md §3 / entity.md §8.1) — done
- `content/entity/dolls/DollRenderer.java` / `DollModel.java` — exist; add **TODO** placeholders for the held-item render pass (loadout.md §5)
- `init/registrate/GLItems.java` — `DOLL_GLOVE` + `DOLL_GLOVE_MODE`
- `init/GensokyoLegacy.java` — glove listener, both packets (no `CodecHandler<ItemStack>`: l2serial already ships one)
- `init/data/GLLang.java` — `ItemGlove` enum: modes + a message per non-trivial action (glove.md §5)

Core (done):
- `init/registrate/GLMeta.java` — `DOLL` player attachment (pairing.md §2.3)
- `init/registrate/GLItems.java` — `DOLL` item + `doll_item_data` component (item.md §1)
- `init/data/GLLang.java` — `Doll.NO_SPACE`, `Doll.RESYNC_MISSING`, `Doll.RESYNC_ORPHAN`, `Doll.RESYNC_TAMPERED`
- resources — generated `en_us`/`en_ud` lang; zh_cn split file + organizer run

Run `./gradlew runData` (commit generated output), then `organize.ResourceOrganizer` for zh_cn.

## 3. Registration summary

| Registry | What | Where | Status |
|---|---|---|---|
| `GLItems` | `DOLL` item, `DOLL_DATA` + `DOLL_LOADOUT` components | Registrate | done |
| `GLItems` | `DOLL_GLOVE` item, `DOLL_GLOVE_MODE` DCVal | Registrate | done |
| `GLMeta` (`ATT.player`) | `DOLL` player capability → `DollAttachment` | attachment | done |
| `HANDLER` (l2serial) | `DollGloveSelectPacket`, `DollGloveTargetPacket` | mod constructor | done |
| item selector | `DollGloveSelectionListener.register()` | mod constructor (next to `BorderUmbrellaSelectionListener`) | done |
| `GLBlocks` | doll controller block + BE method | Registrate | pending (BE method commented out) |

## 4. Edge cases cross-reference

- Pairing / identity / anti-dupe: pairing.md §5, §7.
- Never chunk-serialized: entity.md §5.
- Block-hosted vs player-hosted: controller.md §4.
- Preemption/volley/revival/dup protection: control.md §10.
- Glove wire-up, targeting, and texture: glove.md §2/§5.

## 5. Open questions

- Render held items on the model (loadout.md §5).
- Cloth/core behaviors, mechanical `core` items, attack/tracking lasers, `ITERATIVE` for other types, a glove-inventory screen, block-hosted doll commands, modded-shield matching, heal-mark pruning (control.md §10).
- Config toggle for park intent (TEMP vs STORED) on logout/death (pairing.md §7).