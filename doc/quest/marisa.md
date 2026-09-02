# Marisa Kirisame — Quest & Trade Chart

Entity: `kirisame_marisa`. Quest unlocks chain: 1.1 → 1.2 → 2.1 → 2.2 → 2.3 → 2.4.

## One-time Quests (soft cap 300, max cap 300)

| # | Id | Unlock | Requirements | Rewards |
|---|----|--------|--------------|---------|
| 1.1 | `first_mushroom` | none | red mushroom ×4, brown mushroom ×4 | exp 50, rep +10 (cap +10/max 300) |
| 1.2 | `huge_mushroom` | 1.1 | huge mushroom (tag cap+stem) ×8 | exp 100, rep +10 (cap +0/max 300) |
| 2.1 | `nether_mushroom_prep` | 1.1 + adv `nether/root` | crimson fungus ×4, warped fungus ×4 | exp 150, rep +20 (cap +10/max 300) |
| 2.2 | `shroomlight` | 2.1 | shroomlight ×8 | exp 150, rep +10 (cap +0/max 300) |
| 2.3 | `brewing` | 2.2 + adv `nether/find_fortress` | blaze rod ×4, nether wart ×12 | exp 200, rep +20 (cap +10/max 300) |
| 2.4 | `golden_apple` | 2.3 | enchanted golden apple ×1 | exp 300, rep +10 (cap +0/max 300) |

## Daily Quests (cooldown 24000, exp 60, soft cap 150)

| Id | Unlock | Requirements | Rewards |
|----|--------|--------------|---------|
| `daily_mycelium` | always | roll 3 of (ghost cap/dream cap/miasma cap) ×3-6 each | exp 60, rep +10 (no cap growth) |
| `daily_witchcraft` | always | miasma cap ×2-3 + rotten flesh ×6-12 + 1 of (spider eye/bone/gunpowder) ×2-3 | exp 60, rep +10 (no cap growth) |
| `daily_shroomlight` | 2.2 | roll 2 of (shroomlight/crimson fungus/warped fungus) ×3-6 each | exp 60, rep +10 (no cap growth) |
| `daily_brewing` | 2.3 | blaze rod ×2, nether wart ×8 | exp 60, rep +20 (cap +5/max 130) |

## Trades

### Restocking (player sells → emeralds)

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

| Id | Gate | Pay (item) | Get | Stock | Refresh |
|----|------|-----------|-----|-------|---------|
| `sell_mod_shroom` | none | ghost cap ×8 | emerald | 10 | 24000 |
| `sell_dream_shroom` | none | dream cap ×8 | emerald | 10 | 24000 |
| `sell_miasma_shroom` | none | miasma cap ×8 | emerald | 8 | 24000 |
| `sell_spider_eye` | none | spider eye ×8 | emerald | 4 | 24000 |
| `sell_shroomlight` | 2.2 | shroomlight ×4 | emerald | 4 | 24000 |
| `sell_nether_fungus` | 2.2 | crimson fungus ×8 | emerald | 4 | 24000 |
| `sell_blaze_rod` | 2.3 | blaze rod ×3 | emerald ×2 | 4 | 24000 |
| `sell_nether_wart` | 2.3 | nether wart ×8 | emerald | 4 | 24000 |

### Offering (player buys, rep/quest gated)

| Id | Gate | Pay | Get (hexbrew) | Stock | Refresh |
|----|----------|-----|---------------|-------|---------|
| `offer_miasma` | none | emerald ×3 | miasma ×4 | 4 | 24000 |
| `offer_witch_speed` | 2.3 | emerald ×3 | witch (swiftness) ×4 | 4 | 12000 |
| `offer_witch_strength` | 2.3 | emerald ×3 | witch (strength) ×4 | 4 | 12000 |
| `offer_witch_regen` | 2.3 | emerald ×3 | witch (regeneration) ×4 | 4 | 12000 |
| `offer_witch_leaping` | 2.3 | emerald ×2 | witch (leaping) ×4 | 4 | 12000 |
| `offer_witch_fire` | 2.3 | emerald ×2 | witch (fire resistance) ×4 | 4 | 12000 |
| `offer_shield` | rep 50 | emerald ×2 | shield ×1 | 8 | 24000 |
| `offer_explosive` | rep 50 | emerald ×3 | explosive ×4 | 4 | 24000 |
| `offer_starlight` | rep 120 | emerald ×4 | starlight ×1 | 8 | 24000 |

### Processing (craft-style, rep-gated)

| Id | Gate | Inputs | Output | Stock | Refresh |
|----|------|--------|--------|-------|---------|
| `process_golden_apple` | rep 30 | golden apple ×1 + gold block ×1 | enchanted golden apple ×1 | 1 | 24000 |
| `process_elixir` | rep 80 | mundane hexbrew ×4 | hexbrew elixir ×1 | 16 | 24000 |
