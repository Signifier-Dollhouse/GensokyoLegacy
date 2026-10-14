# Hakurei Reimu — Quest & Trade Chart

Entity: `hakurei_reimu`. Quest unlock chain: 1.1 → 2.1, then two branches —
2.1 → 2.2 → 2.3, and 2.1 → 3.1 → 3.2.

**Status: implemented** (`ReimuQDGen`). Tables below match the datagen
(`src/generated/resources/data/gensokyolegacy/gensokyolegacy/{quest,dialog,dialog_starter,trade}/reimu/...`).

Reimu is the reference implementation for the quest system: it is the only
character with the full chat ladder (rep-gated lore), a `RandomDialogOption`
(§`chat_fortune`), and quest dialogs that carry extra actions
(`GiveMobEffectAction` on the raid quests). See
[quest_design.md](quest_design.md) for the rules the tables below obey.

## Chats

`pickChat` draws **one** unlocked starter per talk. Weights: default 1, misc 30,
info 100, special 1000.

| Id | Gate | Weight | Topic |
|----|------|--------|-------|
| `chat` | none | — | greeting starter (always competes at weight 1) |
| `chat_shrine` | rep **< 50** (`SelfReputationCondition(50, true)`) | 1 | "Where is this place?" — the pre-reputation intro; inverts so it disappears once you know her |
| `chat_shrine_close` | rep ≥ 50 | 30 | what the shrine was like in Gensokyo, and why it can't move |
| `chat_duty` | rep ≥ 100 | 30 | her job: keeping Gensokyo in order, unpaid and year-round |
| `chat_guests` | rep ≥ 100 | 30 | who used to visit: youkai moochers, no human worshippers |
| `chat_power` | rep ≥ 100 | 30 | her ability: flying, plus scary intuition |
| `chat_money` | rep ≥ 100 | 30 | why she wants money: nobody donates to an empty shrine |
| `chat_frog` | straw hat ×1 (`hasItem`) **and** `ominous_banner` done | 1000 | a straw hat on a frog → Suwako's frogs raiding for her |
| `chat_marisa` | missing adv `main/enter_marisa_house` + rep ≥ 50 | 100 | ofuda vs potions → points at Marisa without naming her until asked |
| `chat_fortune` | `local_food` done + `fortune` timer free | 100 | fortune-stick draw |

### `chat_fortune`

Weighted draw **2 : 2 : 1**, rolled server-side by `RandomDialogOption`. Every
branch applies its effect for 24000 ticks *and* sets the `fortune` timer to
+24000, so any outcome costs one game day:

| Weight | Effect |
|--------|--------|
| 2 | `gensokyolegacy:looting` 24000 |
| 2 | `dig_speed` 24000 |
| 1 | bad omen 24000 |

## One-time Quests (soft cap 300, max cap 300)

| # | Id | Unlock | Requirements | Rewards |
|---|----|--------|--------------|---------|
| 1.1 | `local_food` | none | bread ×8, mushroom stew ×3 | exp 50, rep +10 (cap +10/max 300) |
| 2.1 | `hostile_loot` | 1.1 | kill `#minecraft:zombies` ×10, kill `#minecraft:skeletons` ×10, rotten flesh ×8, bone ×8 | exp 100, rep +20 (cap +10/max 300) |
| 2.2 | `talisman_materials` | 2.1 | paper ×16, redstone ×8 | exp 100, rep +10 (cap +0/max 300), loot: talisman pocket ×1 + heal talisman ×2 |
| 2.3 | `ender_materials` | 2.2 | ender pearl ×1, ender eye ×1 | exp 150, rep +10 (cap +0/max 300), unlocks `gap_portal` trade |
| 3.1 | `ominous_banner` | 2.1 | ominous banner ×1 (raid-leader banner, name-matched) | exp 200, rep +20 (cap +10/max 300), loot: shelter talisman ×2 |
| 3.2 | `raid` | 3.1 | win a raid (bad omen mark given by Reimu) | exp 400, rep +30 (cap +20/max 300), loot: border umbrella ×1 |

2.3 → 3.1 are independent branches off 2.1; both 2.2 and 3.1 exist so the two
"research Reimu's powers" lines (gaps, calamity) can diverge.

`reimu/raid` is the quest that needs multi-action options: `startRaidEx` applies
Bad Omen (12000) *and* starts the quest on accept, and `followEx` re-applies the
mark when it has faded. `ominous_banner` is the only requirement matched by item
name rather than id — it is a `DataComponentIngredient` over
`Raid.getLeaderBannerInstance(...)`.

## Daily Quests (cooldown 24000)

| Id | Unlock | Requirements | Rewards |
|----|--------|--------------|---------|
| `daily_food` | 1.1 | roll 2 of (white wool ×6-8 / leather ×4-6 / iron ×6-8 / gold ×3-4 / bread ×6-8 / apple ×4-6) | exp 60, rep +10 (no cap growth), loot: emerald ×1 |
| `daily_hunt` | 2.1 | kill any hostile (`kill_enemy`) ×12 + roll 1 of (rotten flesh ×4-8 / bone ×4-8 / gunpowder ×2-4 / spider eye ×2-4) | exp 80, rep +10 (cap +5/max 120), loot: emerald ×3 |
| `daily_talisman` | 2.2 | paper ×16, redstone ×8 | exp 60, rep +10 (no cap growth), loot: heal talisman ×2 |
| `daily_raid` | 3.2 | win a raid (bad omen mark) | exp 200, rep +20 (cap +10/max 150), loot: shelter talisman ×2 |

All four share the `<char>/shared/option/daily_*` group, so they collapse into
one "Daily Tasks" button — and the group is dropped automatically when only one
daily is unlocked. `daily_hunt` and `daily_raid` are the two that keep pushing a
capped account (`capIncrease` 5 and 10); the other two only ever gain up to the
soft cap.

## Trades

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

### Restocking (player sells → emeralds)

| Id | Gate | Pay (item) | Get | Stock | Refresh |
|----|------|-----------|-----|-------|---------|
| `sell_bread` | 1.1 | bread ×16 | emerald | 1 | 24000 |
| `sell_chicken` | 1.1 | cooked chicken ×4 | emerald | 1 | 24000 |
| `sell_string` | 1.1 | string ×6 | emerald | 4 | 96000 |
| `sell_wool` | 1.1 | `#minecraft:wool` ×6 | emerald | 4 | 96000 |
| `sell_paper` | 2.2 | paper ×12 | emerald | 16 | 24000 |
| `sell_redstone` | 2.2 | redstone ×4 | emerald | 16 | 24000 |
| `sell_gunpowder` | 2.2 | gunpowder ×4 | emerald | 4 | 24000 |

The paper / redstone / gunpowder restock is what makes 2.2 self-funding: the
quest's own inputs are also the shop's inputs, so completing it opens a supply
loop rather than a one-off errand.

### Offering (player buys, quest gated)

The `gap_portal` trade is the odd one out — it is paid in materials (purple
wool, ender eye, ender pearl, crying obsidian) rather than emeralds, which the
UI renders as a craft-style processing trade rather than a purchase.

| Id | Gate | Pay | Get | Stock | Refresh |
|----|------|-----|-----|-------|---------|
| `offer_heal_talisman` | 2.2 | emerald ×8 | heal talisman ×1 | 16 | 24000 |
| `offer_shelter_talisman` | 2.2 | emerald ×8 | shelter talisman ×1 | 16 | 24000 |
| `offer_speed_talisman` | 2.2 | emerald ×4 | speed talisman ×1 | 16 | 24000 |
| `offer_hydrophobic_talisman` | 2.2 | emerald ×4 | hydrophobic talisman ×1 | 16 | 24000 |
| `offer_lava_talisman` | 2.2 | emerald ×4 | lava talisman ×1 | 16 | 24000 |
| `offer_talisman_pocket` | 2.2 | emerald ×16 | talisman pocket ×1 | 4 | 24000 |
| `offer_border_umbrella` | 3.2 | emerald ×48 | border umbrella ×1 | 1 | 168000 |
| `gap_portal` | 2.3 | purple wool ×2 + ender eye ×2 + ender pearl ×2 + crying obsidian ×2 | gap portal ×1 | 1 | 6000 |

The umbrella is the one expensive purchase: 48 emeralds for a weekly restock,
gated behind the hardest quest.

## Cross-character pointers

- `chat_marisa` (missing `main/enter_marisa_house` + rep 50) — names Marisa only
  after the player asks where; the starter is the question "ofuda or potions?".
- `marisa/chat_reimu` — the mirror image from Marisa's side.
- `morichika/chat_reimu` — the same pointer from the shopkeeper.
- `marisa/koishi_hat` unlocks on `reimu/ominous_banner` (plus `marisa/brewing`),
  so 3.1 is load-bearing for Marisa's chain as well as Reimu's.
- `marisa/talisman_request` accepts `reimu/talisman_materials` as one of its two
  "you are known" proofs, and pays emeralds for the heal talismans bought above.