# Alice Margatroid — Quest & Trade Chart

Entity: `alice_margatroid`. Alice has no preset house structure (her bed binds
to any player-built custom home), so her dialog assumes she lives somewhere
quiet in the Magical Forest and nothing gates on an `ENTER_*` advancement. Quest
unlock chain: 1.1 → 2.1 → `daily_doll`.

**Status: implemented** (`AliceQDGen`). Tables below match the datagen
(`src/generated/resources/data/gensokyolegacy/gensokyolegacy/{quest,dialog,dialog_starter,trade}/alice/...`).

Alice is the smallest of the four implementations but the one with the clearest
item loop: the dyes she asks for in 2.1 are the same dyes the glove trade wants,
and 2.1 hands out the doll, the glove and the lance that the trades then sell.
See [quest_design.md](quest_design.md) for the authoring rules.

The seven colors of the Doll Glove (`SPECTRUM_DYES` in `AliceQDGen`) are red,
orange, yellow, light green (lime), cyan, light blue, purple. They are shared by
the 2.1 request, the daily roll and the glove trade, so the three cannot drift.

## One-time Quests (soft cap 300, max cap 300)

| # | Id | Unlock | Requirements | Rewards |
|---|----|--------|--------------|---------|
| 1.1 | `first_doll` | none | string ×8, wool (tag) ×8, mystical straw ×4 | exp 50, rep +10 (cap +10/max 300), loot: emerald ×4 |
| 2.1 | `seven_colors` | 1.1 | red / orange / yellow / lime / cyan / light blue / purple dye ×4 each | exp 200, rep +20 (cap +10/max 300), loot: doll glove ×1 + doll ×1 + **doll lance** ×1 |

2.1 pays out the whole doll kit at once — glove, a doll to put in it, and a
lance so the doll has something to fight with — and it is the gate for all three
of Alice's trades. (The chat used to promise a star wand; it now promises the
lance, and the star wand is a Marisa purchase. See *Cross-character pointers*.)

2.1 is also one of the two ways into Marisa's `talisman_request` — see
`MarisaQDGen#talismanQuests`, which gates on `self_reputation 100 AND
any(reimu/talisman_materials, alice/seven_colors)`. The `any` wrapper is
`AnyCondition` (`condition/any`); condition lists are ANDed everywhere else
(`GatedEntry#match`), so a composite is the only way to write "either A or B".

## Daily Quests (cooldown 24000, exp 60, soft cap 150)

| Id | Unlock | Requirements | Rewards |
|----|--------|--------------|---------|
| `daily_doll` | 2.1 | string ×8, wool (tag) ×8, broom grass ×8, roll 1 iron ingot ×6-8, roll 3 of the 7 dyes ×1 each | exp 60, rep +10 (no cap growth), loot: doll ×1 + doll lance ×1 |

Three sub-goals, deliberately mixed:

- The fixed part (string / wool / broom grass) is the doll body — broom grass
  instead of mystical straw because she is a day's restock, not a commission,
  and the follow-up text says to break it by hand rather than shear it.
- The iron is a `requestTable` roll (`roll 1 of 6-8`), so the spear-head count
  is fixed once the quest starts and shown in the request like a static item.
- The dye sub-goal is one loot pool with `rolls: 3` over the seven dye entries, so
  the same colour can come up twice (rolls are independent, i.e. with
  replacement) and the daily is never a fixed shopping list.

The daily pays a doll **and** a lance, matching the "a doll with an empty hand
is a doll in danger" line — the two are never separated.

## Chats

| Id | Gate | Weight | Topic |
|----|------|--------|-------|
| `chat` | none | — | greeting starter (weight 1) |
| `chat_star_wand` | 2.1 | 100 (`CHAT_INFO`) | how a doll fights: badly, on its own — the lance, or a magician's star wand |

`chat_star_wand` is unlocked by 2.1, the quest that hands out the first doll
lance, so it is also the chat that explains the lance. It points the player at
`marisa/offer_star_wand` for the ranged alternative.

## Trades

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

| Id | Gate | Pay | Get | Stock | Refresh |
|----|------|-----|-----|-------|---------|
| `offer_doll` | 2.1 | emerald ×32 | doll ×1 | 1 | 24000 |
| `offer_doll_lance` | 2.1 | iron ingot ×8 | doll lance ×1 | 1 | 24000 |
| `offer_doll_glove` | 2.1 | 7 dyes ×1 each + white wool ×3 | doll glove ×1 | 1 | 168000 |

`offer_doll_glove` moved here from Morichika (`morichika/offer_doll_glove`,
32 emeralds, gated on `marisa/talisman_request`): the glove is Alice's work, and
the weekly stock plus the dye price keeps it rarer than the plain dolls. The
lance is the cheap one — iron and a day's work — but a day is still a day, so
one a day; it renders as a processing trade rather than a purchase, because the
payment is not currency.

Note that all three trades are gated on 2.1 alone, so Alice has no reputation
tier at all: her progression is quest-shaped, and her reputation only feeds
Marisa's `talisman_request`.

## Cross-character pointers

- `marisa/chat_alice` (rep 50, `CHAT_INFO`) — Marisa names Alice outright,
  unlike her other intro chats, because she knows her; the follow-up option
  ("what does she want?") doubles as a hint at the 2.1 dye order.
- `alice/chat_star_wand` — the mirror image, and the in-fiction explanation for
  why Marisa (not Alice) is the star-wand vendor.
- `marisa/talisman_request` reads `alice/seven_colors` as one of the two proofs
  that the player is worth trusting with an errand.