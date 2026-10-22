# Implementation Checklist

Consolidated file/registration checklist for the doll system. Items marked **(done)** exist in code today; the rest are planned. See the individual docs for the reasoning.

## 1. Files to create

Core (all done):
- `content/attachment/doll/DollState.java`, `DollData.java`, `DollAttachment.java`, `DollHost.java`
- `content/entity/dolls/BaseDollEntity.java`, `DollEntity.java`, `DollMoveControl.java`, plus `render/DollModel.java`, `render/DollRenderer.java`
- `content/entity/dolls/goals/FollowDollOwnerGoal.java`, `LookAtDollOwnerGoal.java`, `DollCommandGoal.java`
- `content/item/doll/DollItem.java`, `DollItemData.java`
- `content/block/functional/doll/DollControllerBlock.java`, `DollControllerBlockEntity.java`
- `event/DollEventHandlers.java` — login/logout/death/respawn/join-level events (pairing.md §4)

Loadout (done — slots, storage, mirror, editor menu; render TODO):
- `content/item/doll/DollSlot.java` — 4-slot enum (loadout.md §1)
- `content/attachment/doll/DollInventory.java` — immutable item variant; `MutableDollInventory.java` — mutable data variant (loadout.md §2)

Summons (API only — no summon exists yet, see summon.md):
- `content/attachment/doll/DollNature.java` — `DOLL` / `SUMMON` / `ITEM_GADGET`: the item, recall and loadout predicates (summon.md §1, gadget.md §1)
- `content/entity/dolls/behavior/DollBehaviorRegistry.java` — `registerIntrinsic`: a binding keyed on the doll's class, for the one move a loadout-free summon owns (§4)

Gadgets (done — the Sekibanki head, gadget.md):
- `content/item/doll/DollGadgetItem.java` — a doll item that names its own entity kind and carries no state back
- `content/entity/dolls/SekibankiHeadEntity.java` + `render/SekibankiHeadModel.java`, `render/SekibankiHeadRenderer.java` — the `ITEM_GADGET` kind and its head-only rig
- assets — `geo/sekibanki_head.geo.json`, `animations/sekibanki_head.animation.json`, `textures/geo/sekibanki_head.png`, `textures/item/doll/sekibanki_head.png`

Control (done — framework; glove calls pending):
- `content/attachment/doll/DollCommander.java` — via `DollAttachment.commands`: volley/one-time/stop, handoff, stall guard, heal scheduling + 1-second mark prune, transient heal marks (control.md §6/§8)
- `content/entity/dolls/action/DollAction.java`, `DollActionType.java`, `DollActionMode.java`, `DollActionHandler.java`, `DollCardHolder.java` + `goals/DollCommandGoal.java` (control.md §1/§3–5, goals under `dolls/goals/`)
- `content/entity/dolls/behavior/DollBehavior.java`, `DollBehaviorRegistry.java`, `DollBehaviors.java`, `DollFriendlyFire.java` + `DollDanmakuBehavior.java`, `DollLaserBehavior.java`, `DollThrowBehavior.java`, `DollSuicideBehavior.java`, `DollHealBehavior.java`, `DollMeleeBehavior.java` (control.md §1/§5, lance charge §5.1b)
- `content/item/doll/DollLanceItem.java` — the only melee weapon a doll arms with (item.md §9)
- `content/entity/dolls/impl/DollHandLock.java` — 10-tick post-spend hold on a doll's hands, stamped from `DollBehavior.ensureMainHand`; a host's arming pass skips a doll inside it (host.md §4)

- `content/entity/dolls/menu/DollLoadoutMenu.java`, `DollLoadoutScreen.java`, `DollLoadoutProvider.java`, `DollLoadoutItemHandler.java` + `GLMisc.DOLL_LOADOUT`, layout JSON, container texture (loadout.md §4)

Implemented — glove (item, 6 modes with heal/stop hidden, wheel, target cache + glow mixin, packets):
- `content/item/glove/DollGloveItem.java`
- `content/item/glove/mode/DollGloveMode.java`
- `content/item/glove/mode/DollGloveHandler.java` + 6 mode classes
- `content/item/glove/DollGloveSelectionListener.java` (the left-click paths live in `DollGloveItem` + `event/DollEventHandlers.java`)
- `content/item/glove/client/DollGloveModeWheel.java`, `DollGloveModeEntry.java`
- `content/item/glove/network/DollGloveSwingPacket.java`
- `content/item/common/network/SelectorSelectPacket.java` — the shared selector-wheel mode pick (no glove-specific select packet any more)
- `content/item/common/` — the shared target cache (glove.md §2): `GloveTargeting.java`, `client/GloveTargetCache.java`, `network/GloveTargetPacket.java`, plus `content/attachment/glove/GloveTargetAttachment.java` as the per-player server store — + `mixin/ClientGlowMixin.java` (via `content/client/ClientGlowManager.java`) + mixins-json entry (glove.md §2)

## 2. Files to modify

- `content/attachment/doll/DollData.java` — `inventory` field (loadout.md §2) — done (mutable variant); `nature` field (summon.md §1) — done
- `content/attachment/doll/DollLedger.java` — `drop(UUID)`, the forget-an-entry path a death and an expiry both end on (summon.md §3)
- `content/attachment/doll/DollAttachment.java` — destroyed-resummon revival keeping gear (§7); stray rejoin as `TEMP` and the recall sweep for strays within 48 blocks (control.md §5.4a); ledger transitions only; the itemize / park / tryItemize / death / stray gates and the recall skip for a summon (summon.md §3)
- `content/attachment/doll/DollCommander.java` — no timeout sweep: expiry is the kind's own bookkeeping, and the commander only drops entries it is told to (summon.md §5)
- `content/attachment/doll/DollSpawn.java` — stamps the entity's `nature()` onto the materialized entry (summon.md §1)
- `content/attachment/doll/DollHost.java` — `detach(UUID)` for stray cuts, `onDeath` hook; `StrayHost.java` holds the detached entry, answers pairing, hands it over one-shot via `take()`, persists via chunk save/load (control.md §5.4), and mints no item for a summon (summon.md §3); `DelegateDollHost.java` — the whole surface forwarded to a host kept elsewhere, so a character answers as the host with one `dolls()` method (host.md §1)
- `content/entity/dolls/BaseDollEntity.java` — pairing pipeline only (stray `getHost` branch, `die()` → `onDeath`); empty-hand itemize (arming removed, loadout.md §4); `setSpeedCap` escape hatch on the movement cap for the melee charge (entity.md §3.1, control.md §5.1b); `nature()`, the one place a kind declares what it may become (summon.md §1)
- `content/entity/dolls/DollEntity.java` — `actions` field, one `DollCommandGoal`, vanilla shield hooks (§5.6), never-null ledger-direct loadout API, `becomeStray()` / `rejoinOwner()` / `maybeRejoin()` (1-second idle rejoin from `tick()`); 4 synced slot accessors, `writeValuesTo`/`readValuesFrom` (loadout.md §3 / entity.md §8) — done; `DollGeo`, the plain doll's clip set on top of the base's `DollRig` (entity.md §8)
- `content/item/doll/DollItem.java` — the `type → item` map every item files itself into from its constructor, and the virtual `makeStack` a gadget answers bare (item.md §2, gadget.md §1) — done
- `content/item/doll/MiniShieldItem.java` / `DollShieldItem.java` — the doll-only shield and the marker that tells the doll what it can block with (item.md §10) — done
- `content/attachment/doll/DollData.java` — `fromItemData` takes the entry's type from the item rather than the constant (gadget.md §1) — done
- `init/registrate/GLEntities.java` / `GLItems.java` — `SEKIBANKI_HEAD` entity + item (gadget.md §3) — done
- `content/entity/dolls/behavior/DollDanmakuBehavior.java` — fires the card off its `HandMatch` rather than the main hand, so an intrinsic card works (summon.md §4)
- `content/item/glove/DollGloveItem.java` / `content/item/glove/mode/DollGloveHandler.java` — both loadout-editor opens refuse a summon, so the poke falls through (summon.md §2)
- `content/entity/characters/magician/AliceDollHost.java` — `drop(UUID)`; `arm()` skips a loadout-free entry (summon.md §2/§3)
- `content/entity/dolls/render/DollRenderer.java` / `DollModel.java` — exist; add **TODO** placeholders for the held-item render pass (loadout.md §5)
- `init/registrate/GLItems.java` — `DOLL_GLOVE` + `DOLL_GLOVE_MODE`
- `init/GensokyoLegacy.java` — glove listener, both packets (no `CodecHandler<ItemStack>`: l2serial already ships one); both packet registrations now live in `init/GLPackets.java`
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
| `GLItems` / `GLEntities` | `SEKIBANKI_HEAD` gadget item + its entity type (gadget.md §3) | Registrate | done |
| `GLItems` | `DOLL_GLOVE` item, `DOLL_GLOVE_MODE` DCVal | Registrate | done |
| `GLItems` | `MINI_SHIELD` item, a doll-only shield (item.md §10) | Registrate | done |
| `GLMeta` (`ATT.player`) | `DOLL` player capability → `DollAttachment` | attachment | done |
| `HANDLER` (l2serial) | `SelectorSelectPacket` (shared wheel pick), `GloveTargetPacket` (shared cache, glove.md §2) | mod constructor | done |
| item selector | `DollGloveSelectionListener.register()` | mod constructor (next to `BorderUmbrellaSelectionListener`) | done |
| `GLBlocks` | doll controller block + BE method | Registrate | pending (BE method commented out) |

## 4. Edge cases cross-reference

- Pairing / identity / anti-dupe: pairing.md §5, §7.
- Never chunk-serialized: entity.md §5.
- Block-hosted vs player-hosted: controller.md §4.
- Preemption/volley/revival/dup protection: control.md §10.
- Glove wire-up, targeting, and texture: glove.md §2/§5.
- The no-item-form rule and its six gates, per-kind timeouts, intrinsic move: summon.md §3/§5/§4.
- The item's own entity kind, the component-less gadget item, and the base-vs-doll split that makes "cannot act" structural: gadget.md §1/§2/§4, entity.md §9.1.

## 5. Open questions

- Render held items on the model (loadout.md §5).
- Cloth/core behaviors, mechanical `core` items, attack/tracking lasers, `ITERATIVE` for other types, a glove-inventory screen, block-hosted doll commands, modded-shield matching, heal-mark pruning (control.md §10).
- Config toggle for park intent (TEMP vs STORED) on logout/death (pairing.md §7).
- The actual summons: which kinds exist, what summons them, and whether expiry is instant or a staggered stagger-out like the party's (summon.md §6).