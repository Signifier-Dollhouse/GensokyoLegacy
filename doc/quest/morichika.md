# Morichika Rinnosuke — Quest & Trade Chart

Entity: `morichika_rinnosuke`.

**Status: implemented** (`MorichikaQDGen`). Tables below match the datagen
(`src/generated/resources/data/gensokyolegacy/gensokyolegacy/{dialog_starter,trade}/morichika/...`;
there is no `quest/morichika/` — Kourindou has no quests yet).

Kourindou is the mod's "intro hub": it is the one character who points at *both*
Reimu and Marisa, so he is the only place a new player can be sent to either
before they have seen the other two. He trades tools rather than story, and all
of his gates are advancements rather than reputation — he has no reputation
system of his own, so nothing here can soft-lock a fresh save.

See [quest_design.md](quest_design.md) for the authoring rules.

## Chats

`pickChat` draws **one** unlocked starter per talk; the greeting competes at
weight 1.

| Id | Gate | Weight | Topic |
|----|------|--------|-------|
| `chat` | none | — | greeting starter (weight 1) |
| `chat_marisa` | missing adv `main/enter_marisa_house` | 100 (`CHAT_INFO`) | "brewing potions is such a hassle" → a magician deep in the Magical Forest |
| `chat_reimu` | missing adv `main/enter_hakurei_shrine` | 100 (`CHAT_INFO`) | "it's dangerous out there" → a shrine maiden in the cherry grove |

Both intros are `missingAdv`-gated, so they fire **until** the player has visited
the other's home and then stop — the only `missingAdv` use with no reputation
condition attached. Both name the other character only after the player asks
where; the starters are worded so the NPC can plausibly be complaining about
something else.

`chat_marisa` and `chat_reimu` are the third copy of the two canonical pointers:
the Reimu pointer is also in `marisa/chat_reimu` and `reimu/chat_marisa`, and the
Marisa pointer also in `reimu/chat_marisa`. They differ only in voice — Morichika
sells both directions and does not need reputation to mention a neighbour.

## Trades

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

| Id | Gate | Pay | Get | Stock | Refresh |
|----|------|-----|-----|-------|---------|
| `offer_strange_glasses` | none | emerald ×8 | strange glasses ×1 | 1 | 24000 |
| `offer_guide_book` | adv `main/welcome` | emerald ×1 | tools guide (Patchouli book) ×1 | 1 | 24000 |
| `offer_koishi_hat` | adv `gensokyolegacy:koishi_hat` | emerald ×32 | koishi hat ×1 | 1 | 168000 |
| `offer_dagger_glove` | adv `main/obtain_iron_dagger` | emerald ×24 | dagger glove ×1 | 1 | 168000 |

- `offer_guide_book` is gated on the vanilla intro advancement rather than
  anything of Rinnosuke's, so a player who never speaks to him can still pick
  the book up for one emerald. It is a Patchouli book, registered via
  `PatchouliHelper.getBook`, not an item stack.
- `offer_koishi_hat` is gated on the `koishi_hat` advancement, which is granted
  by the same event that completes `marisa/koishi_hat` — so the hat is
  *earned* in the Nether, then sold back to you by the shop you have to find
  first. 32 emeralds and a weekly restock keep it a reward rather than a
  purchase.
- `offer_dagger_glove` is the one weapon he sells, and it is gated on Sakuya's
  `main/obtain_iron_dagger` rather than on its own `main/obtain_dagger_glove`:
  the glove spends the whole inventory's daggers to turn a stack into a
  pattern, so it is worthless to a player who cannot yet throw one — and by the
  time the offer appears, the player has already seen the counterparty who sells
  the ammunition ([izayoi_sakuya.md](izayoi_sakuya.md)). 24 emeralds and the same
  weekly restock as the hat, less than the hat's 32, because unlike the hat this
  one is a tool the player is expected to keep using.
- `offer_doll_glove` used to live here; it moved to Alice, who actually makes
  the glove (see [alice.md](alice.md)). `offer_dagger_glove` is its replacement —
  one he resells rather than makes, which is the same relationship he has to
  Sakuya.

## Deliberate gaps

- No quests, so no exp or reputation rewards, and nothing of his own gates
  anything else.
- No `chat_alice` counterpart: Alice has no house, so there is no advancement to
  point at and no shop to send the player to from here. Marisa's `chat_alice`
  covers the dolls instead.
- No rep tiers, unlike Reimu and Marisa: the shop's prices are flat and its
  stock is deliberately thin.