# Ibuki Suika — Quest & Trade Chart

Entity: `ibuki_suika`.

**Status: entity implemented, content not.** `SuikaQDGen` currently registers
the greeting lines and nothing else, so there are no `dialog_starter/suika/`,
no `trade/suika/` and no `quest/suika/` entries in the datagen output. This
page records what exists and what the rules are for adding to it.

Suika is the mod's second **home-less** character, after Sakuya. She owns no
structure, no bed and no `character_config` entry, and exists only as a
visit-system guest. The mod has no bamboo forest to put an oni's house in, so
the Hakurei shrine is the only house she ever stands in — declared in
`GLStructureGen` as `visit(0.30f, 2400, 6000)`, i.e. 30% of days for 2–5
minutes. The shrine sits against the bamboo that is the edge of her own forest,
which is the only reason she has any business being there.

Consequences that shape everything below:

- Any entry must be gated on `visitingAt(hakurei_shrine)`. `homeBound()`
  content would be dead — she can never be home-bound — and ungated content
  would leak into other hosts if she ever visits a second one.
- **No quests, by design.** The visit system hard-suppresses
  `QuestHandle.Kind.START` for a guest (`ServerCharacterDialogManager.getInitialConversation`),
  so a quest would be reachable only if something else handed it out.
- No `character_config` entry means `VisitScheduler` falls back to its
  `FALLBACK_WANDER` of 12 blocks for her wander radius.
- No spell card. `TouhouSpellCards.set` leaves `spellCard.card` null, which
  `SpellCardWrapper` already handles, so she fights with reach alone.

See [quest_design.md](quest_design.md) for the authoring rules and
[../design/structure/character_visit.md](../design/structure/character_visit.md) for the visit system.

## Dialog lines

| Key | Text |
|---|---|
| `suika/chat/greeting` | shown above the topic list at home |
| `suika/chat/visit_greeting` | shown instead while she is a guest |
| `suika/chat/trade` | empty |

The `visit_greeting` line is the one piece of content she cannot do without: a
home-less character still needs it, because it is what a first-time player sees
(§9 of `character_visit.md`). It is also currently the *entire* text a player
sees from her, since she has no topic list to open.

The trade line is empty on purpose. With no offers in the trade registry the
trade topic is never offered in the first place
(`ServerCharacterDialogManager#getInitialConversation`), so there is nothing for
it to label; `TradeHandle#display` falls back to the generic label regardless.

## Chats

None yet. When they are written, the shape to follow is Sakuya's
([izayoi_sakuya.md](izayoi_sakuya.md)) — a single greeting-weight chat plus
whatever `CHAT_MISC`/`CHAT_INFO` topics earn their place.

## Trades

None yet, and so no party-gift ladder either (`GiftLootGen`). A guest falls
back to the generic three-rung `DEFAULT` table until she has one of her own.

## Deliberate gaps

- **No home, no structure, no bed.** Nothing to repair or restock, so unlike
  Morichika she has no shelf, no crafting task and no schedule of her own; the
  visit system's `VISITING` activity handles her whole day.
- **No content at all yet.** She can be met and will greet you, and that is the
  whole of her.
- **No spell card**, unlike every other boss-tier character here. She is melee
  only and reaches through `BossYoukaiEntity`'s 200 HP and 10 damage instead.
- **Two dialog animations, and they change with her pose.** Her rig ships
  `交流_01` and `交流_02` and nothing else — no greeting clip, no `思考中_*`,
  no `疑惑` — so `getAnim` answers empty for every other dialog slot and
  `SuikaEntity#hasBlinkAnim` answers no, for the same reason. That
  half is a rig limitation rather than a content choice and stands even once
  chats are written; the two she has are plenty, because a dialog with no
  `animations` list picks one at random out of the mapped talk slots.
- **She talks lying down.** `侧卧_交流` is `侧卧` with the talking motion written
  into it. A conversation does not get her out of bed: TALK(300) outranks NAP(500)
  so the dialog is what runs, and `YoukaiNapTask#canStillUse` holds the pose
  across it anyway. While the synced `YoukaiFlags.NAPPING` is set,
  `SuikaEntity#syncDialogAnims` re-points every dialog trigger at `侧卧_交流`, so
  all of her dialog animation — not just the talk slots — is the lying variant
  while she is down. `getAnim` stays a function of the rig alone and the swap
  happens client-side, so the server's "does this slot exist" table never has to
  agree with what is playing. She gets up for a fight or a party, and — if the
  conversation outlasted the nap's five-to-twenty-second memory — three seconds
  after the last line of dialogue.
- **No `OUTDOOR_IDLE` either.** She is the one character whose `侧卧` clip is not
  spent as an idle flourish: `YoukaiAnim.OUTDOOR_IDLE` resolves empty for her and
  she lies down through the `NAP` activity instead — rolled from `napChance`,
  five to twenty seconds long, and ended by anything above it on the priority
  scale except a conversation. See `youkai_character.md` §3.3.
- **One clip in the rig is wired to nothing.** `喝` is in `suika.animation.json`
  with no slot behind it, same as it has been for the other rigs that ship one.
  Neither is referenced from any Java source. `聚集` next to it *is* wired: it is
  `GeoYoukaiAnim.GATHER`, fired by her spell card's pull and opted in by
  `SuikaEntity#hasGatherAnim` — the one character whose rig ships it.
- **One house only.** If a second host is ever added, both this page and the
  `visitingAt` gates have to grow with it.
