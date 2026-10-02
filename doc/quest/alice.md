# Alice Margatroid — Quest & Trade Chart

Entity: `alice_margatroid`. Alice has no preset house structure yet (her bed binds
to any player-built custom home), so her dialog assumes she lives somewhere quiet
in the Magical Forest. Quest unlocks chain: 1.1 → 2.1 → `daily_doll`.

**Status: implemented** (`AliceQDGen`). Tables below match the datagen (`src/generated/resources/data/gensokyolegacy/.../{quest,dialog,dialog_starter,trade}/alice/...`).

The seven colors of the Doll Glove (`SPECTRUM_DYES` in `AliceQDGen`) are red,
orange, yellow, light green (lime), cyan, light blue, purple. They are shared by
the 2.1 request, the daily roll and the glove trade, so the three cannot drift.

## One-time Quests (soft cap 300, max cap 300)

| # | Id | Unlock | Requirements | Rewards |
|---|----|--------|--------------|---------|
| 1.1 | `first_doll` | none | string ×8, wool (tag) ×8, mystical straw ×4 | exp 50, rep +10 (cap +10/max 300), loot: emerald ×4 |
| 2.1 | `seven_colors` | 1.1 | red/orange/yellow/lime/cyan/light blue/purple dye ×4 each | exp 200, rep +20 (cap +10/max 300), loot: doll glove ×1 + doll ×1 + star wand ×1 |

2.1 is also one of the two ways into Marisa's `talisman_request` — see
`MarisaQDGen#talismanQuests`, which gates on `self_reputation 100 AND any(reimu/talisman_materials, alice/seven_colors)`.
The `any` wrapper is the new `AnyCondition` (`condition/any`); condition lists
are ANDed everywhere else (`GatedEntry#match`), so a composite is the only way
to write "either A or B".

## Daily Quests (cooldown 24000, exp 60, soft cap 150)

| Id | Unlock | Requirements | Rewards |
|----|--------|--------------|---------|
| `daily_doll` | 2.1 | string ×8, wool (tag) ×8, broom grass ×8, roll 3 of the 7 dyes ×1 each | exp 60, rep +10 (no cap growth), loot: doll ×1 |

The dye sub-goal is one loot pool with `rolls: 3` over the seven dye entries, so
the same colour can come up twice (rolls are independent, i.e. with
replacement) and the daily is never a fixed shopping list.

## Chats

| Id | Gate | Weight | Topic |
|----|------|--------|-------|
| `chat` | none | 1 (default) | greeting; the forest and her dolls keep her company |
| `chat_star_wand` | 2.1 | 100 (`CHAT_INFO`) | a doll needs a star wand to fight, and only Marisa makes them — points the player at `marisa/offer_star_wand` |

## Trades

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

| Id | Gate | Pay | Get | Stock | Refresh |
|----|------|-----|-----|-------|---------|
| `offer_doll` | 2.1 | emerald ×32 | doll ×1 | 1 | 24000 |
| `offer_doll_glove` | 2.1 | 7 dyes ×1 each + white wool ×3 | doll glove ×1 | 1 | 168000 |

`offer_doll_glove` moved here from Morichika (`morichika/offer_doll_glove`,
32 emeralds, gated on `marisa/talisman_request`): the glove is Alice's work, and
the weekly stock plus the dye price keeps it rarer than the plain dolls.

## Cross-character pointers

- `marisa/chat_alice` (rep 50, `CHAT_INFO`) — Marisa names Alice outright, unlike
  her other intro chats, because she knows her; the follow-up option ("what does
  she want?") doubles as a hint at the 2.1 dye order.
- `alice/chat_star_wand` — the mirror image, and the in-fiction explanation for
  why Marisa (not Alice) is the star-wand vendor.
