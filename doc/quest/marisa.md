# Marisa Kirisame — Quest & Trade Chart

Entity: `kirisame_marisa`. Main spine: 1.1 → 1.2, 1.1 → 2.1 → 2.2 → 2.3 → 2.4.
Two side quests hang off the spine: 3.1 off 2.3, 3.2 off reputation alone.

**Status: implemented** (`MarisaQDGen`). Tables below match the datagen
(`src/generated/resources/data/gensokyolegacy/gensokyolegacy/{quest,dialog,dialog_starter,trade}/marisa/...`).

Marisa carries the widest trade list and the only `AnyCondition` gate in the
mod. See [quest_design.md](quest_design.md) for the authoring rules.

## Chats

| Id | Gate | Weight | Topic |
|----|------|--------|-------|
| `chat` | none | — | greeting starter (weight 1) |
| `chat_reimu` | missing adv `main/enter_hakurei_shrine` + rep 50 | 100 | "this world is dangerous" → a shrine maiden who may help in a fight |
| `chat_morichika` | missing adv `main/enter_morichika_shop` + rep 50 | 100 | "I want tools to explore" → Kourindou, run by an old acquaintance |
| `chat_alice` | rep 50 | 100 | "who is the best doll-maker?" — names Alice outright; the follow-up doubles as a hint at quest 2.1's dye order |

`chat_alice` is the deliberate exception to the "intro chats never name the
other character" rule: Marisa and Alice know each other, so she names her. Alice
has no house structure and therefore no `ENTER_*` advancement to gate on, so
this chat is reputation-gated only.

## One-time Quests (soft cap 300, max cap 300)

| # | Id | Unlock | Requirements | Rewards |
|---|----|--------|--------------|---------|
| 1.1 | `first_mushroom` | none | red mushroom ×8, brown mushroom ×8 | exp 50, rep +10 (cap +10/max 300), loot: emerald ×4 |
| 1.2 | `huge_mushroom` | 1.1 | huge mushroom (tag cap+stem) ×8 | exp 100, rep +10 (cap +0/max 300), loot: emerald ×6 |
| 2.1 | `nether_mushroom_prep` | 1.1 + adv `nether/root` | crimson fungus ×4, warped fungus ×4 | exp 150, rep +20 (cap +10/max 300), loot: emerald ×6 + miasma hexbrew ×1 |
| 2.2 | `shroomlight` | 2.1 | shroomlight ×8 | exp 150, rep +10 (cap +0/max 300), loot: emerald ×6 + explosive hexbrew ×2 |
| 2.3 | `brewing` | 2.2 + adv `nether/find_fortress` | blaze rod ×4, nether wart ×12 | exp 200, rep +20 (cap +10/max 300), loot: emerald ×8 + hexbrew elixir ×1 |
| 2.4 | `golden_apple` | 2.3 | enchanted golden apple ×1 | exp 300, rep +10 (cap +0/max 300), loot: emerald ×10 |
| 3.1 | `koishi_hat` | 2.3 + `reimu/ominous_banner` | evidence from the Nether — completed by the **koishi hat dropping**, no description line | exp 300, rep +20 (cap +10/max 300), loot: emerald ×8 |
| 3.2 | `talisman_request` | rep 100 **and** `any(reimu/talisman_materials, alice/seven_colors)` | heal talisman ×4 | exp 200, rep +20 (cap +10/max 300), loot: broom ×1 + star wand ×1 + emerald ×16 |

Notes:

- 3.1 (`marisa/koishi_hat`) is the quest that pays for the koishi hat's
  existence as a *goal* rather than an accident: the requirement is
  `koishi_hat`, dispatched by `KoishiAttackCapability` when the hat drops, and
  its description list is empty on purpose. It also cross-gates on Reimu's 3.1,
  so Reimu's ominous-banner quest feeds Marisa's.
- 3.2 (`marisa/talisman_request`) is the only `AnyCondition` in the mod. Its
  gate is "rep 100 **and** proof you carry Reimu's ofuda stock or Alice's doll
  work", and `AnyCondition` is the only way to write that because every
  condition *list* is ANDed (`GatedEntry#match`). It pays out the broom **and**
  the star wand, and is the gate for the three trades that sell them, so it is
  the hinge of Marisa's late game.

## Daily Quests (cooldown 24000, exp 60, soft cap 150)

| Id | Unlock | Requirements | Rewards |
|----|--------|--------------|---------|
| `daily_mycelium` | always | roll 1 each of ghost cap ×3-6, dream cap ×3-6, miasma cap ×2-4 (three pools, so all three) | exp 60, rep +10 (no cap growth), loot: emerald ×1 |
| `daily_witchcraft` | always | roll 1 of miasma cap ×2-3, roll 1 rotten flesh ×6-12, roll 1 of (spider eye / bone / gunpowder) ×2-3 | exp 60, rep +10 (no cap growth), loot: emerald ×1 + miasma hexbrew ×1 |
| `daily_shroomlight` | 2.2 | roll 2 of (shroomlight / crimson fungus / warped fungus) ×3-6 each | exp 60, rep +10 (no cap growth), loot: emerald ×2 |
| `daily_brewing` | 2.3 | blaze rod ×2, nether wart ×8 | exp 60, rep +20 (cap +5/max 130), loot: emerald ×8 |
| `daily_talisman` | 3.2 | roll 1 of heal talisman ×2-3 | exp 60, rep +10 (no cap growth), loot: star wand ×1 + emerald ×16 |

`daily_mycelium` uses one pool per mushroom (not a single rolling pool) so the
daily always asks for all three; `daily_shroomlight` uses one pool with two
independent rolls, so the same fungus can come up twice. `daily_brewing` is the
only Marisa daily that keeps growing the reputation ceiling.

`daily_talisman` is the loop behind `talisman_request`: buy healing papers from
Reimu (emerald ×8 each), hand them to Marisa, and she hands back a star wand and
emeralds — enough to run the errand again.

## Trades

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

### Restocking (player sells → emeralds)

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

The four ungated restocks are available from the first minute so Marisa is never
a dead end; `sell_blaze_rod` is the only one that pays **2** emeralds, since a
blaze rod is the hardest thing she asks for.

### Offering (player buys, rep/quest gated)

Hexbrew results. Witch hexbrews carry a real potion effect: effects that have a
**strong** tier are sold as a single strong bottle; the odd one out (`fire`)
falls back to the **long** form at 4 bottles — both for emerald ×4. Cures are
stripped from every witch bottle so the effect can't be milked with milk.

| Id | Gate | Pay | Get (hexbrew / item) | Stock | Refresh |
|----|----------|-----|---------------|-------|---------|
| `offer_miasma` | none | emerald ×3 | miasma ×4 | 4 | 24000 |
| `offer_witch_speed` | 2.3 | emerald ×4 | witch (strong swiftness) ×1 | 16 | 12000 |
| `offer_witch_strength` | 2.3 | emerald ×4 | witch (strong strength) ×1 | 16 | 12000 |
| `offer_witch_regen` | 2.3 | emerald ×4 | witch (strong regeneration) ×1 | 16 | 12000 |
| `offer_witch_leaping` | 2.3 | emerald ×4 | witch (strong leaping) ×1 | 16 | 12000 |
| `offer_witch_fire` | 2.3 | emerald ×4 | witch (long fire resistance) ×4 | 4 | 12000 |
| `offer_shield` | rep 50 | emerald ×2 | shield hexbrew ×1 | 8 | 24000 |
| `offer_hyphae` | 1.2 | emerald ×2 | hyphae hexbrew ×1 | 8 | 24000 |
| `offer_explosive` | rep 50 | emerald ×3 | explosive hexbrew ×4 | 4 | 24000 |
| `offer_starlight` | rep 120 | emerald ×4 | starlight hexbrew ×1 | 8 | 24000 |
| `offer_sealing_pot` | rep 120 | emerald ×24 | sealing pot ×1 | 1 | 48000 |
| `offer_star_wand` | rep 100 + 3.2 | emerald ×8 | star wand ×1 | 4 | 24000 |
| `offer_broom` | 3.2 | mystical straw ×8 | broom ×1 | 1 | 24000 |

`offer_shield` is the **shield hexbrew**, not a vanilla shield. The wand and the
broom share a gate (3.2) and a one-a-day ceiling each, so neither reward walks
out on the other; the broom is paid in straw rather than emeralds, which makes
it a processing trade in the UI.

### Processing (craft-style, rep-gated)

| Id | Gate | Inputs | Output | Stock | Refresh |
|----|------|--------|--------|-------|---------|
| `process_golden_apple` | rep 30 | golden apple ×1 + gold block ×1 | enchanted golden apple ×1 | 1 | 24000 |
| `process_elixir` | rep 80 | mundane hexbrew ×4 | hexbrew elixir ×1 | 16 | 24000 |

## Cross-character pointers

- `chat_reimu` (missing `main/enter_hakurei_shrine` + rep 50) — points at Reimu
  as "an old friend of mine" without naming her first; the where-ans names the
  Hakurei Shrine and warns about the donation.
- `chat_morichika` (missing `main/enter_morichika_shop` + rep 50) — points at
  Kourindou and does name Rinnosuke's shop, but calls him "an old acquaintance".
- `chat_alice` (rep 50) — the inverse of Alice's `chat_star_wand`, and the
  in-fiction reason Marisa (not Alice) is the star-wand vendor: Marisa makes
  magic tools, Alice makes dolls.
- `koishi_hat` and `talisman_request` are the two places where Marisa's chain
  reads Reimu's and Alice's quest state — see [reimu.md](reimu.md) and
  [alice.md](alice.md).