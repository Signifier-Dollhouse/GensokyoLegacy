# Ibuki Suika — Quest & Trade Chart

Entity: `ibuki_suika`.

**Status: entity implemented, greeting lines only.** `SuikaQDGen` registers her
home/visit greeting and her party greeting, so there are no `quest/suika/` or
`trade/suika/` entries; the only `dialog_starter/suika/` entry is the gift-return
override from `GiftQDGen`. This page records what exists and what the rules are
for adding to it.

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
| `suika/chat/greeting` | "Oh? You came too. I'm enjoying a nice little buzz right now." |
| `suika/chat/visit_greeting` | empty; falls back to `greeting` |
| `suika/chat/party_greeting` | "Yo, I'm here for the feast — you'd better show me a good time." |
| `suika/chat/trade` | empty |

A home-less character is a guest wherever she stands, so the home line is
written into `greeting` and `visit_greeting` is left empty: at the shrine a
visitor is not `isVisiting()`, so the empty visit line falls through to it. The
party line is a greeting variant, not a chat: `FirstDialogScreen` picks it while
`isPartying()`, and it outranks the visit line (`DialogConfig#body`).

The trade line is empty on purpose. With no offers in the trade registry the
trade topic is never offered in the first place
(`ServerCharacterDialogManager#getInitialConversation`), so there is nothing for
it to label; `TradeHandle#display` falls back to the generic label on the empty
string.

## Chats

None of her own. The party line is greeting text, so there is no chat starter and
nothing to roll; the only `dialog_starter` is the gift-return override. Anything
added later is gated on `visitingAt(hakurei_shrine)` for the shrine itself, never
on a party, because a party guest is not visiting a home.

## Trades

None yet, and so no party-gift ladder either (`GiftLootGen`). A guest falls
back to the generic three-rung `DEFAULT` table until she has one of her own.

## Deliberate gaps

- **No home, no structure, no bed.** Nothing to repair or restock, so unlike
  Morichika she has no shelf, no crafting task and no schedule of her own; the
  visit system's `VISITING` activity handles her whole day.
- **Almost no content.** She can be met and will greet you, at home or at a
  party; there are no chats, no quests and no trades.
- **No spell card**, unlike every other boss-tier character here. She is melee
  only and reaches through `BossYoukaiEntity`'s 200 HP and 10 damage instead.
- **Two dialog animations, and they change with her pose.** Her rig ships
  `talk_01` and `talk_02` and nothing else — no greeting clip, no `think_*`,
  no `puzzle`, no `love`/`hate`/`agree` — so `getAnim` folds every slot she
  needs onto one of the two, and answers empty for the rest.
  `SuikaEntity#hasBlinkAnim` answers no for the same reason. That half is a rig
  limitation rather than a content choice and stands even once more chats are
  written; the two she has are plenty, because a dialog with no `animations`
  list picks one at random out of the mapped talk slots. `AGREE` is folded onto
  `talk_02` because her gift neutral reaction names `dialog_agree` and the rig
  has no agree clip; `LIKE` and `DISLIKE` map to her real `love` and `hate`
  clips.
- **She talks lying down.** `lie_01_talk` is `lie_01` with the talking motion written
  into it. A conversation does not get her out of bed: TALK(300) outranks NAP(500)
  so the dialog is what runs, and `YoukaiNapTask#canStillUse` holds the pose
  across it anyway. While the synced `YoukaiFlags.NAPPING` is set,
  `SuikaEntity#triggerAnim` re-points every dialog trigger at that clip, so all
  of her dialog animation — not just the talk slots — is the lying variant while
  she is down. `getAnim` stays a function of the rig alone and the swap happens
  client-side, so the server's "does this slot exist" table never has to agree
  with what is playing. She gets up for a fight or a party, and — if the
  conversation outlasted the nap's five-to-twenty-second memory — three seconds
  after the last line of dialogue.
- **The nap has a variant, and it is 0.** `SuikaEntity#getNapVariant` is a synced
  `int` on her own `SUIKA_DATA` layer, NBT-backed like the rest of her entity
  data, and it indexes `NAP_ANIMS` for the loop she holds and
  `YoukaiAnim.SPECIAL_TALKS` for the talk she answers from it. She ships one
  `lie_01_*` clip, so `NAP_VARIANTS` is 1 and nothing rolls the number: the slot
  index and the clip list are the shape a second nap has to fit into, not
  machinery in use yet. `TALK_SPECIAL_01` and `TALK_SPECIAL_02` therefore both
  resolve to `lie_01_talk` — slot *n* is the talk written for nap *n*, and only
  one of those exists. When a second `lie_01_*` clip lands it goes into `NAP_ANIMS`
  and takes over `TALK_SPECIAL_02`, with no other change. `SKILL_02` and
  `SKILL_03` exist in the enum for the same reason and are wired to nothing at
  all: her rig has no second or third skill clip, so they resolve empty and no
  triggerable is registered for them.
- **No `OUTDOOR_IDLE` either.** She is the one character whose `lie_01` clip is not
  spent as an idle flourish: `YoukaiAnim.OUTDOOR_IDLE` resolves empty for her and
  she lies down through the `NAP` activity instead — rolled from `napChance`,
  five to twenty seconds long, and ended by anything above it on the priority
  scale except a conversation. See `youkai_character.md` §3.3.
- **One clip in the rig is wired to nothing.** `drink` is in `suika.animation.json`
  with no slot behind it, same as it has been for the other rigs that ship one.
  Neither is referenced from any Java source. `skill` next to it *is* wired: it is
  her `YoukaiAnim.SKILL_01`, played by her spell card's pull and mapped by her
  `getAnim` — the one character whose rig ships it.
- **One house only.** If a second host is ever added, both this page and the
  `visitingAt` gates have to grow with it.
