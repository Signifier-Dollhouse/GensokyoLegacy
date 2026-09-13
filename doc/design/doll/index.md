# Doll System Design

Design docs for the GensokyoLegacy doll system: item ↔ entity pairing through the player-capability ledger, doll behavior (loadout, actions, control), and the Seven-Colored Doll Glove.

## What a doll is

A doll is a small flying companion that, at any instant, exists as **exactly one** of three forms — the anti-dupe invariant:

```
entity + DollData   — a materialized projection of a ledger entry (SUMMONED)
item                — the sole authoritative copy while in item form; no ledger entry exists
parked DollData     — a ledger entry with a restore intent (STORED → waits to
                      become an item; TEMP → waits to become an entity again)
```

Entity ↔ `DollData` identity is the entity's **own game uuid**, minted fresh on every materialization and never stored on the item. The player capability `DollAttachment` is the authoritative ledger for player-hosted dolls; the doll controller block is an alternative host. No item is ever lost or duplicated across any transition.

## Goals

- A **doll item** that places to summon a `DollEntity`; clicking the doll with an empty hand converts it back to the item.
- A **player capability** (`DollAttachment`) is the authoritative ledger for summoned dolls. The doll entity is a **projection** of the `DollData` held there.
- **No item loss, no duplication** — item and `DollData` never coexist (itemization removes the data first, atomic).
- **Fresh uuid per summon** — clones of an item always summon independent dolls; a stale entity whose uuid lost its `SUMMONED` entry discards itself.
- **No expensive per-tick state capture** — only *significant values* (uuid, type, dimension, position, facing, name) + `CombatData` are tracked, updated lazily; never a full `CompoundTag` snapshot.
- The doll **follows the player**, survives logout/death (parked with a restore intent), and keeps health/name/position; dimension changes are handled uniformly by store + resummon.
- **Guard against unauthorized external changes**.
- Dolls are **not targetable by hostile mobs** but **still take damage**.
- Player-hosted dolls have **behaviors driven by the items they hold** (§ loadout / control / glove) — combat actions under a single-ticket system, plus a glove item to mark heal targets and issue attack commands.

## Document tree

| Doc | Covers | Status |
|---|---|---|
| [pairing.md](pairing.md) | identity & data model (`DollState`/`DollData`/`DollAttachment`), state machine (summon / itemize / park / restore / deferred), lifecycle events, integrity + reconciliation backups, mob targeting | implemented |
| [entity.md](entity.md) | `BaseDollEntity` (pairing + foundational properties) vs `DollEntity` (doll-specific logic + rendering-facing hooks); movement/navigation, value sync, never-save guard, rendering | implemented |
| [item.md](item.md) | the doll item, significant values, item ↔ data conversion, trading | implemented |
| [controller.md](controller.md) | block-hosted dolls — the doll controller block and its block entity as a `DollHost` | implemented (block registration pending) |
| [loadout.md](loadout.md) | the four held item slots (main hand / off hand / cloth / core), synced entity data, loadout editor menu, item rendering TODO | implemented |
| [control.md](control.md) | doll actions (ticket, single execution), the four action types, heal-mark targets, iterative command sequences | framework implemented (glove pending) |
| [glove.md](glove.md) | the Seven-Colored Doll Glove: modes, itemselector wheel, network, registration | planned |
| [checklist.md](checklist.md) | registration & datagen checklist, files to create / modify, edge cases | — |

Suggested reading order: **pairing → entity → item → controller** (the core pairing model), then **loadout → control → glove** (doll behavior and control).