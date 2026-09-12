# Hakurei Reimu — Quest & Trade Chart

Entity: `hakurei_reimu`. Quest unlocks chain: 1.1 → 2.1, then branches — 2.2 → 2.3, and 3.1 → 3.2.

**Status: implemented** (`ReimuQDGen`). Tables below match the datagen (`src/generated/resources/data/gensokyolegacy/.../{quest,dialog,dialog_starter,trade}/reimu/...`).

## One-time Quests (soft cap 300, max cap 300)

| # | Id | Unlock | Requirements | Rewards |
|---|----|--------|--------------|---------|
| 1.1 | `local_food` | none | bread ×8, mushroom stew ×3 | exp 50, rep +10 (cap +10/max 300) |
| 2.1 | `hostile_loot` | 1.1 | kill `#minecraft:zombies` ×10, kill `#minecraft:skeletons` ×10, rotten flesh ×8, bone ×8 | exp 100, rep +20 (cap +10/max 300) |
| 2.2 | `talisman_materials` | 2.1 | paper ×16, redstone ×8 | exp 100, rep +10 (cap +0/max 300), loot: talisman pocket ×1 + heal talisman ×2 |
| 2.3 | `ender_materials` | 2.2 | ender pearl ×1, ender eye ×1 | exp 150, rep +10 (cap +0/max 300), unlocks `gap_portal` trade |
| 3.1 | `ominous_banner` | 2.1 | ominous banner ×1 | exp 200, rep +20 (cap +10/max 300), loot: shelter talisman ×2 |
| 3.2 | `raid` | 3.1 | win a raid (bad omen mark given by Reimu) | exp 400, rep +30 (cap +20/max 300), loot: border umbrella ×1 |

## Daily Quests (cooldown 24000, soft cap 150-200)

| Id | Unlock | Requirements | Rewards |
|----|--------|--------------|---------|
| `daily_food` | 1.1 | roll 2 of (white wool ×6-8 / leather ×4-6 / iron ×6-8 / gold ×3-4 / bread ×6-8 / apple ×4-6) | exp 60, rep +10 (no cap growth), loot: emerald ×1 |
| `daily_hunt` | 2.1 | kill `#minecraft:monster` ×12 + roll 1 of (rotten flesh ×4-8 / bone ×4-8 / gunpowder ×2-4 / spider eye ×2-4) | exp 80, rep +10 (cap +5/max 120), loot: emerald ×3 |
| `daily_talisman` | 2.2 | paper ×16, redstone ×8 | exp 60, rep +10 (no cap growth), loot: heal talisman ×2 |
| `daily_raid` | 3.2 | win a raid (bad omen mark) | exp 200, rep +20 (cap +10/max 150), loot: shelter talisman ×2 |

## Trades

### Restocking (player sells → emeralds)

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

| Id | Gate | Pay (item) | Get | Stock | Refresh |
|----|------|-----------|-----|-------|---------|
| `sell_bread` | 1.1 | bread ×16 | emerald | 1 | 24000 |
| `sell_chicken` | 1.1 | cooked chicken ×4 | emerald | 1 | 24000 |
| `sell_string` | 1.1 | string ×6 | emerald | 4 | 96000 |
| `sell_wool` | 1.1 | `#minecraft:wool` ×6 | emerald | 4 | 96000 |
| `sell_paper` | 2.2 | paper ×12 | emerald | 16 | 24000 |
| `sell_redstone` | 2.2 | redstone ×4 | emerald | 16 | 24000 |
| `sell_gunpowder` | 2.2 | gunpowder ×4 | emerald | 4 | 24000 |

### Offering (player buys, quest gated)

The `gap_portal` trade is the odd one out — it is paid in materials (purple wool, ender eye, ender pearl, crying obsidian) rather than emeralds.

| Id | Gate | Pay | Get | Stock | Refresh |
|----|----------|-----|---------------|-------|---------|
| `offer_heal_talisman` | 2.2 | emerald ×8 | heal talisman ×1 | 16 | 24000 |
| `offer_shelter_talisman` | 2.2 | emerald ×8 | shelter talisman ×1 | 16 | 24000 |
| `offer_speed_talisman` | 2.2 | emerald ×4 | speed talisman ×1 | 16 | 24000 |
| `offer_hydrophobic_talisman` | 2.2 | emerald ×4 | hydrophobic talisman ×1 | 16 | 24000 |
| `offer_lava_talisman` | 2.2 | emerald ×4 | lava talisman ×1 | 16 | 24000 |
| `offer_talisman_pocket` | 2.2 | emerald ×16 | talisman pocket ×1 | 4 | 24000 |
| `offer_border_umbrella` | 3.2 | emerald ×48 | border umbrella ×1 | 1 | 168000 |
| `gap_portal` | 2.3 | purple wool ×2 + ender eye ×2 + ender pearl ×2 + crying obsidian ×2 | gap portal ×1 | 1 | 6000 |