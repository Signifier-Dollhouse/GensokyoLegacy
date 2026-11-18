# Nazrin — Quest & Trade Chart

Entity: `nazrin`. Nazrin lives at Myouren Temple and is the first character to
hand out the missing-tower quest — Shou lost the tower, Nazrin dowses for it and
pays the player for fetching it. Her content is built around the `dowser` and
the `treasure_tower` rather than a shop: one daily, one follow-up and one
processing trade.

**Status: implemented** (`NazrinQDGen`). Tables below match the datagen
(`src/generated/resources/data/gensokyolegacy/gensokyolegacy/{quest,dialog,dialog_starter,trade}/nazrin/...`).

Every gate and reward below is a plain condition; the only new machinery she
needed is `ConditionalDialogOption` (world-state follow-up) and
`MissingTowerNearCondition`. See [quest_design.md](quest_design.md) §8 and
[missing_tower.md](../design/quest/missing_tower.md) §6.

## Chats

| Id | Gate | Weight | Topic |
|----|------|--------|-------|
| `chat` | none | — | greeting starter (weight 1), plus visit / party lines and the trade label |

Nazrin has no side chats: she is not a pointer to another character, and her
lore drip is the daily itself. The gift reactions and the `gift/return` table
are the shared `GiftQDGen` entries.

## Daily Quest (cooldown 24000, exp 60, rep +50, soft cap 150)

| Id | Unlock | Requirements | Rewards |
|----|--------|--------------|---------|
| `lost_tower` | none | find and pick up Shou's treasure tower (`missing_tower`) | exp 60, rep +50 (cap +10/max 300) |

The requirement is event-driven: `MissingTowerRequirement` places the tower
32–128 blocks from Nazrin when the quest starts and turns the requirement line
green once it is in hand. The tower is always placed on the surface, never
underground. The completion topic stays hidden until the tower is carried, so
the player has to be holding it when they talk to her.

### Follow-up hint

The follow-up topic is a single `ConditionalDialogOption` with two branches:

| Branch | Condition | Nazrin says |
|--------|-----------|-------------|
| near | `MissingTowerNearCondition(nazrin/lost_tower, 128)` | "it's close — keep looking around here, on the surface" |
| far | fallback (no conditions) | "not anywhere near here — follow the path Shou took" |

Both branches read the same `MissingTowerManager.MAX_RADIUS` the placement
uses, so the hint cannot drift from where the tower actually is. The condition
reads `IndexStorage`, so a carried or absent record reads as "far". She names
no distance: a dowsed twitch is a direction, not a number.

### Reward choice

The handover runs `CompleteQuestAction` first — quest completed, exp and
reputation paid — and opens a second menu. That menu is not a quest reward
list; each option carries a `GiveItemAction`:

| Option | Grant |
|--------|-------|
| `complete/reward/tower` | `treasure_tower` ×1 — an ordinary wooden tower Nazrin infused with a spark of Shou's power |
| `complete/reward/dowser` | `dowser` ×1 |
| `complete/reward/coins` | `coin_stack` ×4 |

The player picks one. This is why the quest rewards carry only exp and
reputation: the choice lives in the dialog, where it can differ per click,
which a `Quest.rewards` list cannot express. JEI reads the three options off the
completion dialog and shows them as "Any of the following rewards of your
choice" (see [rewards.md](../design/quest/rewards.md)).

## Trades

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

| Id | Gate | Pay | Get | Stock | Refresh |
|----|------|-----|-----|-------|---------|
| `process_dowser` | rep ≥ 51 | iron ingot ×6 | `dowser` ×1 | 1 | 24000 |

The gate is strictly above the daily's +50, so the first completed tower is
what unlocks the trade. It reads as processing rather than a purchase because
the payment is iron, not coin — the same shape as Alice's lance trade.
