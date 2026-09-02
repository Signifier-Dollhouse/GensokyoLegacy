# Marisa Kirisame — Quest & Trade Chart

Entity: `kirisame_marisa`. Quest unlocks chain: 1.1 → 1.2 → 2.1 → 2.2 → 2.3 → 2.4.

## One-time Quests

| # | Id | Unlock | Requirements | Rewards |
|---|----|--------|--------------|---------|
| 1.1 | `first_mushroom` | none | red mushroom ×4, brown mushroom ×4 | exp 50, rep +20 (cap +10/max 300) |
| 1.2 | `huge_mushroom` | 1.1 | huge mushroom (tag cap+stem) ×8 | exp 100, rep +40 (cap +20/max 300) |
| 2.1 | `nether_mushroom_prep` | 1.1 + adv `nether/root` | crimson fungus ×4, warped fungus ×4 | exp 150, rep +30 (cap +10/max 300) |
| 2.2 | `shroomlight` | 2.1 | shroomlight ×8 | exp 150, rep +30 (cap +20/max 300) |
| 2.3 | `brewing` | 2.2 + adv `nether/find_fortress` | blaze rod ×4, nether wart ×12 | exp 200, rep +40 (cap +30/max 300) |
| 2.4 | `golden_apple` | 2.3 | enchanted golden apple ×1 | exp 300, rep +40 (cap +20/max 300) |

## Daily Quests (cooldown 24000, exp 60)

| Id | Unlock | Requirements | Rewards |
|----|--------|--------------|---------|
| `daily_mycelium` | always | roll 3 of (ghost cap/dream cap/miasma cap) ×3-6 each | exp 60, rep +15 (no cap growth) |
| `daily_witchcraft` | always | miasma cap ×2-3 + rotten flesh ×6-12 + 1 of (spider eye/bone/gunpowder) ×2-3 | exp 60, rep +15 (cap +10/max 200) |
| `daily_shroomlight` | 2.2 | roll 2 of (shroomlight/crimson fungus/warped fungus) ×3-6 each | exp 60, rep +20 (cap +10/max 200) |
| `daily_brewing` | 2.3 | blaze rod ×2, nether wart ×8 | exp 60, rep +25 (cap +20/max 200) |

## Trades

### Restocking (player sells → emeralds)

| Id | Gate | Pay (item) | Get |
|----|------|-----------|-----|
| `sell_mod_shroom` | none | ghost cap ×8 | emerald |
| `sell_dream_shroom` | none | dream cap ×8 | emerald |
| `sell_miasma_shroom` | none | miasma cap ×8 | emerald |
| `sell_mob_loot` | none | rotten flesh ×8 + spider eye ×4 | emerald |
| `sell_shroomlight` | 2.2 | shroomlight ×4 | emerald ×2 |
| `sell_nether_fungus` | 2.2 | crimson fungus ×8 | emerald |
| `sell_blaze_rod` | 2.3 | blaze rod ×3 | emerald ×2 |
| `sell_nether_wart` | 2.3 | nether wart ×8 | emerald |

### Offering (player buys, rep-gated)

| Id | Rep gate | Pay | Get (hexbrew) |
|----|----------|-----|---------------|
| `offer_mundane` | 0 | emerald ×3 | mundane |
| `offer_miasma` | 50 | emerald ×3 | miasma |
| `offer_shield` | 100 | emerald ×5 | shield |
| `offer_starlight` | 150 | emerald ×6 | starlight |
| `offer_explosive` | 200 | emerald ×8 | explosive |
| `offer_witch` | 250 | emerald ×12 | witch |

### Processing (craft-style, quest-unlocked)

| Id | Gate | Inputs | Output |
|----|------|--------|--------|
| `process_golden_apple` | 2.4 | golden apple ×1 + gold block ×1 | enchanted golden apple ×1 |
| `process_elixir` | none | mundane hexbrew ×4 | hexbrew elixir ×1 |
