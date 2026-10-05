# Izayoi Sakuya — Quest & Trade Chart

Entity: `izayoi_sakuya`.

**Status: implemented** (`SakuyaQDGen`). Tables below match the datagen
(`src/generated/resources/data/gensokyolegacy/gensokyolegacy/{dialog_starter,trade}/sakuya/...`;
there is no `quest/sakuya/`).

Sakuya is the mod's first **home-less** character: she owns no structure, no bed
and no `character_config` entry, and exists only as a visit-system guest. Kourindou
is the only house she ever stands in — declared in `GLStructureGen` as
`visit(0.30f, 2400, 6000)`, i.e. 30% of days for 2–5 minutes.

Consequences that shape everything below:

- Every entry is gated on `visitingAt(morichika_shop)`. `homeBound()` content
  would be dead — she can never be home-bound — and ungated content would leak
  into other hosts if she ever visits a second one.
- **No quests, by design.** The visit system hard-suppresses `QuestHandle.Kind.START`
  for a guest (`ServerCharacterDialogManager.getInitialConversation`), so a quest
  would be reachable only if something else handed it out.
- No `character_config` entry means `VisitScheduler` falls back to its
  `FALLBACK_WANDER` of 12 blocks for her wander radius.

See [quest_design.md](quest_design.md) for the authoring rules and
[../design/character_visit.md](../design/character_visit.md) for the visit system.

## Chats

| Id | Gate | Weight | Topic |
|----|------|--------|-------|
| `visit_morichika_shop_greet` | visiting `morichika_shop` | 1 (`CHAT_DEFAULT`) | is the shop open; she has been here before |

No greeting starter of her own: she has no home to greet you at, so the
`visit_greeting` line carries the whole first-contact impression.

## Trades

Stock = max times tradeable per refresh; Refresh = ticks until restock (20 ticks = 1 s).

| Id | Gate | Pay | Get | Stock | Refresh |
|----|------|-----|-----|-------|---------|
| `offer_iron_dagger` | visiting `morichika_shop` | iron ingot ×4 | iron dagger ×1 | 16 | 24000 |

- The gate is the one that matters for a visitor: with `VisitModule` expired
  she is simply gone, so an ungated offer would show on a stale trade list.
- 16 per day is the stock *and* the flavor — a maid keeps that many sharp in a
  day's work — so it is a deliberate rate rather than a balancing knob.
- 4 iron for a returning projectile dagger is the cheapest per-use weapon in the
  mod, which is the point: it is the entry-level replacement for the dagger
  glove, and `main/obtain_iron_dagger` (already parented off
  `main/enter_morichika_shop`) now has a real source.

## Deliberate gaps

- **No home, no structure, no bed.** Nothing to repair or restock, so unlike
  Morichika she has no shelf, no crafting task and no schedule of her own; the
  visit system's `VISITING` activity handles her whole day.
- **No reputation content.** She gives no exp, no rep and gates nothing on rep,
  so `CharacterAttachment` has an entry for her that only gift/feed would touch.
- **No pointer to another character.** There is nothing to send the player
  toward — she is a shop-within-a-shop, and the two canonical pointers
  (`chat_marisa` / `chat_reimu`) are Morichika's to give.
- **One house only.** If a second host is ever added, both this chart and the
  `visitingAt` gates have to grow with it.