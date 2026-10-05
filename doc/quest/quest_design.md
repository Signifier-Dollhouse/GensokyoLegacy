# Quest & Trade Design Workflow

How to plan, draft, and implement quests / trades / chats for a character in
Gensokyo Legacy. This is the process used for Marisa, Reimu, Alice and
Morichika and acts as a template for the next character.

Companion pages (the authoritative per-character tables, generated from the
datagen classes): [reimu.md](reimu.md), [marisa.md](marisa.md),
[alice.md](alice.md), [morichika.md](morichika.md).

## 1. Plan (`doc/`)

Write the design spec in `doc/quest/<char>.md` — the same file that later holds
the shipped tables. Plan these four things:

- **Chats** (`dialog_starter`): a greeting starter plus weighted side chats.
  Side chats are the character's lore drip and her pointers to other
  characters; each gets a gate and a weight (see §4).
- **One-time quest chain**: ordered quests with a clear unlock chain. Each
  quest lists requirements (items to submit / mobs to kill), rewards (exp +
  reputation + optional loot) and its three dialogs.
- **Daily quests**: `QuestRecurrence(cooldown)` — 24000 ticks = 1 game day.
- **Trades** (see §6): restocking (player sells → currency), offering (player
  buys stock), processing (craft-style, result is not currency).

**Unlock conditions.** Earlier quests unlock via `HasQuestCompletedCondition`.
Nether / fortress gating uses vanilla advancements (`nether/root`,
`nether/find_fortress`) via `HasAdvancementCondition`. Home visits use the
`GLAdvGen.ENTER_*` advancements. Reputation tiers use `SelfReputationCondition`
(§5). "Either A or B" needs `AnyCondition` (§8) — condition *lists* are always
ANDed.

Reviews are working files: fold the final dialog into the datagen class and
delete the review drafts. The authoritative dialog source of truth is the
datagen class + `en_us.json`; the `doc/quest/<char>.md` table is the summary.

## 2. Create the datagen class

New `init/data/rpg/<Char>QDGen.java` extending `QuestDialogData`:

```java
public class CharQDGen extends QuestDialogData {

    public static final ResourceLocation QUEST_PREV = GensokyoLegacy.loc("char/<quest_id>");

    // shared option keys for this character, registered once under <char>/shared
    private final String byeKey, dailyGroupKey, dailyStartKey, dailyAcceptKey;

    public CharQDGen() {
        prefix("char/shared");
        byeKey = text("option", "bye", "Bye!");
        dailyGroupKey = text("option", "daily_group", "Daily Tasks");
        // ...

        chats();
        quests();
        trades();
    }

    private void chats() {
        prefix("char/chat");
        defaultDialog(GLEntities.CHAR.get(), "greeting line", "trade line");
        starter("char/chat", new DialogStarter(GLEntities.CHAR.get(), List.of(),
                starterText("start", "player line"),
                dialog("hi", "npc line", option("hi/end", "player line"))));

        prefix("char/chat_other");
        chat("char/chat_other", GLEntities.CHAR.get(),
                List.of(missingAdv(GLAdvGen.ENTER_OTHER_HOUSE), new SelfReputationCondition(50)),
                starterText("start", "player line"),
                dialog("talk", "npc line", /* options */),
                CHAT_INFO);
    }

    private void quests() {
        prefix("char/<quest_id>");
        quest("char/<quest_id>", new Quest(GLEntities.CHAR.get(),
                List.of(new HasQuestCompletedCondition(QUEST_PREV)),
                questTitle("Title"), questDesc("..."),
                Optional.empty(),
                new TreeMap<>(Map.of("a-req", new SubmitItemRequirement(List.of(item(ITEM, 4))))),
                List.of(new ExpReward(50), new ReputationReward(10, 300, 10, 300),
                        loot("char/<quest_id>", LootTable.lootTable().withPool(lootItem(ITEM, 1)))),
                start(/* ... */), follow(/* ... */), complete(/* ... */)));
    }

    private void trades() {
        prefix("char");
        trade("sell_x", GLEntities.CHAR.get(), new ItemStack(Items.EMERALD),
                new TradeRecurrence(10, 24000), item(ITEM, 8));
        trade("offer_y", new TradeOffer(GLEntities.CHAR.get(),
                List.of(new SelfReputationCondition(50)),
                new ItemStack(RESULT),
                new TradeRecurrence(4, 24000), List.of(item(Items.EMERALD, 3))));
    }
}
```

Conventions:

- Organize content by `prefix(...)` scope (`<char>/shared`, `<char>/chat*`,
  each `<char>/<quest_id>`, then `<char>` for trades). The prefix state is a
  private field, so sections must be self-contained.
- `start` / `follow` / `complete` / `daily` are **per-gen private helpers**, not
  base methods — each QDGen carries its own copy (§9). Copy the pattern from
  `MarisaQDGen`.
- Item refs: mushroom caps/blocks via the `GLNaturalBlocks.*_MUSHROOM_SET` fields
  (`GHOST_FIRE_`, `DREAM_`, `DEMONIC_MIASMA_`) `.cap` / `.block`, plain plants
  via `GLNaturalBlocks.<PLANT>.asItem()`; entity via `GLEntities.<CHAR>.get()`;
  hexbrews via `HexBrew.*.bottle`; tags via `GLTagGen.*` / `ItemTags.*`.
- Item-name-matched requirements (e.g. Reimu's "Ominous Banner") use
  `new IngredientEntry(DataComponentIngredient.of(false, Raid.getLeaderBannerInstance(...)), 1, Optional.of("Ominous Banner"))`
  — the third argument is the display name override.
- Reward loot via `loot("<char>/<x>", LootTable.lootTable().withPool(lootItem(...)))`
  → `LootTableReward`.
- Randomized daily sub-goals via `requestTable("<daily>", ...)` (uniform-count
  pools) consumed by `rollItem(table)` → `RollItemRequirement`. `requestTable`
  writes under `loot_table/quest_req/<prefix>/`, so it never collides with a
  reward table.
- Multi-action options: `option(id, text, List.of(new StartQuestAction(), new GiveMobEffectAction(...)), next)`.
  Reimu's `startRaidEx` / `followEx` wrap exactly this shape.

## 3. Register & wire

In the `GensokyoLegacy` datagen entry point:

```java
var reimu = new ReimuQDGen();
var marisa = new MarisaQDGen();
var morichika = new MorichikaQDGen();
var alice = new AliceQDGen();
QuestDialogData.build(REGISTRATE, reimu, marisa, morichika, alice);
```

`QuestDialogData.build(L2Registrate, QuestDialogData...)` is a single static
method that registers each datapack registry (dialog / dialog_starter / quest /
trade_offer) plus the `entity_type/default_dialog` data map *exactly once*,
iterating all instances' content — a second `.add` per registry makes
`RegistrySetBuilder` throw `Multiple entries with same key`. Subclasses keep
**per-instance (non-static) content maps**; `build` owns the one-time
registration. The instances must be created here even though nothing calls them:
constructors populate the maps as a side effect.

Register any new condition / action / requirement / reward subclass in
`CodecRegistry` (e.g. `HAS_QUEST` → `HasQuestCompletedCondition`).

## 4. Chats

A `DialogStarter` is `(character, conditions, starterText, dialog, weight)`.
When the player talks to a character, `ServerCharacterDialogManager.pickChat`
picks **one** unlocked starter by weight, using the `CHAT_*` constants:

| Constant | Weight | Use |
|----------|--------|-----|
| `CHAT_DEFAULT` | 1 | the greeting; must lose to everything else |
| `CHAT_MISC` | 30 | lore / flavour unlocked by reputation |
| `CHAT_INFO` | 100 | informative: pointers to other characters, unlocks |
| `CHAT_SPECIAL` | 1000 | item-driven one-offs (Reimu's frog hat) |

Weights are relative, so a `CHAT_SPECIAL` chat effectively suppresses the
greeting while its condition holds. Gating is via `missingAdv` /
`hasAdv` / `hasQuest` / `hasItem` / `timer` / reputation conditions (§8).

`defaultDialog(entity, greeting, trade)` writes the two strings the entity's
idle greeting and the trade-screen button use into
`data_maps/entity_type/default_dialog.json`.

## 5. Reputation model

`ReputationReward(reputation, softCap, capIncrease, maxCap)` →
`CharacterData.gainReputation`:

1. `capIncrease > 0` → `reputationCap` grows by up to `capIncrease`, but never
   past `maxCap`.
2. Reputation is hard-capped at `reputationCap`; once at the cap, nothing is
   gained.
3. Crossing or sitting past `softCap` halves the gain (`val / 2`).

So the four numbers read: *+rep, the point where gains start halving, how much
the ceiling moves, and the absolute ceiling.* Convention:

- One-time quests: `softCap 300`, `maxCap 300`, `capIncrease` 0 or 10.
- Dailies: `softCap 150`, `capIncrease 0` ("no cap growth") — or `capIncrease 5`
  for the few dailies meant to keep pushing a capped character
  (`reimu/daily_hunt`, `marisa/daily_brewing`).

Gates read the same number: `SelfReputationCondition(v)` = rep ≥ v;
`SelfReputationCondition(v, true)` inverts it (Reimu's `chat_shrine` only fires
*below* rep 50, so the "what is this place" intro is gone once you know her).
`OtherReputationCondition(entity, v)` reads a different character.

## 6. Trades

`TradeOffer(character, conditions, result, recurrence, ingredients)` with
`TradeRecurrence(maxStock, restockTime)` — stock = max trades per refresh,
restock = ticks (20 = 1 s). There is no separate trade *type* field: the UI
derives the direction from the items (`TradeOffer#isSellOffer`):

- result **is** currency (`GLTagGen.CURRENCY` = emerald / gold ingot), one
  ingredient, and the player gives more than they get → **sell** (restock)
  trade: the price tag is suppressed.
- otherwise → the character is selling to the player. If the ingredients are
  currency this is a normal purchase; if they are not, it renders as
  craft-style **processing** (`marisa/process_*`, `marisa/offer_broom`) and shows
  the result instead of a price.

Consequences for authoring:

- Restock trades gate on `HasQuestCompletedCondition` so the character does not
  buy from a stranger.
- Purchase trades gate on reputation (§5) or a completed quest; the gate *is*
  the scarcity mechanism, so price and stock can stay generous.
- Processing trades are plain `TradeOffer`s — no extra field is needed.

## 7. Quests

`Quest(character, conditions, title, description, recurrence, requirements,
rewards, initialDialog, followUpDialog, completionDialog)`.

- `recurrence` is `Optional`: present = daily (cooldown ticks), empty = one-time.
- `requirements` is a `Map<String, QuestRequirement>` keyed `"a-", "b-", "c-"`
  (`new TreeMap<>(Map.of(...))` keeps the display order). Multiple entries =
  several sub-goals shown at once; one `SubmitItemRequirement` with several
  `IngredientEntry`s = one shopping list.
- `rewards` is a list: `ExpReward`, `ReputationReward`, `LootTableReward`.
- The three dialogs are mandatory and are picked by state
  (`ServerCharacterDialogManager#getInitialConversation`): completable →
  `completionDialog`, started → `followUpDialog`, else → `initialDialog`.

## 8. Machinery catalogue

Everything below is registered in `CodecRegistry`. Add a subclass there when
you add one.

**Options** (`CodecRegistry.OPTION`) — `simple`, `group`, `random`.

- `SimpleDialogOption` — text + actions + next; may carry its own `conditions`.
- `GroupDialogOption` — same, but tagged with a `group` key. Options sharing a
  group key across *different* entries of the conversation collapse into one
  button. `groupHandles` **drops the group when only one member is available**,
  so a one-daily character shows a plain button and no empty "Daily Tasks" row.
- `RandomDialogOption` + `WeightedEntry` — one button rolling a weighted list of
  (actions, next) pairs. Click handling is polymorphic: `DialogOption.resolve`
  returns an `OptionResult` (`SimpleDialogOption` / `GroupDialogOption` return
  themselves, `RandomDialogOption` returns the rolled entry, or `null` when
  handed no `RandomSource`).

**Actions** (`ACTION`) — `start_quest`, `complete_quest`, `give_mob_effect`,
`set_timer`. `GiveMobEffectAction(effect, duration, amplifier)` hands out e.g.
Bad Omen; `SetTimerAction(key, delay)` stamps `CharacterData.timers`.

**Conditions** (`CONDITION`) — `has_advancement`, `has_quest_completed`,
`has_item`, `timer`, `self_reputation`, `other_reputation`, `any`.
Lists are ANDed (`GatedEntry#match`); `AnyCondition` is the only way to write
"either A or B" and nests `CodecRegistry.CONDITION.codec()` directly (safe for
the same reason `Quest.CODEC` is — `CONDITION` is assigned before any condition
registers its codec).

**Requirements** (`REQUIREMENT`) — `submit_item`, `has_item`, `kill_mob`,
`kill_enemy`, `roll_item`, `raid_victory`, `koishi_hat`.
`roll_item` resolves a `quest_req/` loot table when the quest starts, so the
rolled numbers are fixed for that run and shown in the request like static items.
`koishi_hat` has an empty description on purpose: the quest is completed by the
*event* (the hat dropping), not by anything the player does on purpose.

**Rewards** (`REWARD`) — `exp`, `reputation`, `loot_table`.

**Triggers** (not codec-registered; bound by the requirement's `getTrigger`) —
`EmptyTrigger`, `KillTrigger`, `RaidTrigger`, `KoishiHatTrigger`. Two of them
are dispatched from outside the quest system and need the glue below.

Machinery that only exists because a quest needed it:

- `RaidTrigger` + `RaidVictoryRequirement` — completed by winning a raid.
  `RaidMixin` (`@WrapOperation` on `PlayerTrigger.trigger` in `Raid.tick()`, the
  `hero_of_the_village` grant) dispatches it. Declared in
  `gensokyolegacy.mixins.json`.
- `KoishiHatTrigger` + `KoishiHatRequirement` — completed when the koishi hat
  drops. `KoishiAttackCapability` dispatches it next to the `koishi_hat`
  advancement trigger.
- `TimerCondition` / `SetTimerAction` — generic per-character timestamp gates
  (`CharacterData.timers`, key → next-available game time), used for Reimu's
  fortune draw and reusable for any cooldown.
- `AnyCondition` — "either A or B" (Marisa's talisman errand).

## 9. Per-character helper duplication

`start`, `follow`, `complete`, `daily`, `dailyStart`, `dailyFollow`,
`dailyComplete` are private copies in `ReimuQDGen`, `MarisaQDGen` and
`AliceQDGen`, and they have **diverged on purpose**:

- Marisa's and Alice's `follow` take a `@Nullable` end override (`null` → reuse
  the shared `byeKey`); Reimu's takes a required end string.
- Marisa's `complete` has both a 3-arg and a 6-arg overload (with / without a
  reject branch); Reimu has `complete` + `completeSingle`.
- Reimu's dailies carry extra helpers (`dailyEx`, `followEx`, `completeSingle`)
  for the raid quests, which need `GiveMobEffectAction` on accept and on
  follow-up.

When adding a character, copy the version that matches and extend locally. Do not
"fix" the differences — the shared key set differs per character too (Reimu has
`take_reward` + `leave`, Marisa/Alice have `daily_thanks`).

## 10. Dialog design rules

- **3 states per quest**: `start` (player knows nothing — the NPC explains from
  scratch), `follow_up` (player re-checks the task — the NPC *restates the task
  or gives a hint*), `complete` (hand-over). There is no separate "in-progress
  status" screen; a progress answer would leak state the NPC shouldn't track.
  It is fine for the NPC to *guess* at progress in flavour ("Hm — nothing seems
  to be happening?"), as long as no branch reveals whether requirements are met.
- **Voice split**: option/button lines are the **player's** voice; dialog lines
  are the **NPC's**. Buttons are task reminders or hand-overs, never "how's it
  going?".
- **Rejection is in-fiction.** Every `start` has a reject branch that gives the
  character a reason (too dangerous, not enough time, wrong tool). A quest whose
  rejection would be "no" is a quest that should not exist.
- **Character-introduction chats**: cross-character pointers gated by
  `missingAdv(...)` never reveal the other character's name — refer to them by
  role ("a magician", "a shrine maiden", "an old acquaintance"). Each intro is
  need-based: the player's starter voices a problem and the host points at
  someone who may help. The one exception is a pair who already know each other
  (`marisa/chat_alice` names Alice outright).
  - *Marisa* (hosted by Reimu as `reimu/chat_marisa`, Morichika as
    `morichika/chat_marisa`): starter complains potion brewing is complicated →
    a forest magician who may make brewing easier.
  - *Reimu* (hosted by Marisa as `marisa/chat_reimu`, Morichika as
    `morichika/chat_reimu`): starter remarks the world is dangerous → a shrine
    maiden who may provide something to aid combat.
  - *Morichika* (hosted by Marisa as `marisa/chat_morichika`): starter wishes for
    tools to aid exploration → a curiosity-shop keeper who may sell useful tools.
- **Shared daily keys**: all dailies of a character share one group
  (`<char>/shared/option/daily_group`) and the same accept / reject / follow /
  follow-end / complete / thanks keys, so the collapsed "Daily Tasks" button
  behaves identically across quests. Per-quest `option` blocks and per-quest
  `complete` dialogs exist only for one-time quests.
- **Inline-option pitfall**: `optionKey(id, key, ...)` stores its 2nd argument
  *verbatim* as the option text with no lang registration, and
  `SimpleDialogOption.display()` is `Component.translatable(text())` — passing a
  raw button string where the key belongs bakes the English sentence into the
  dialog data and it can never be keyed or translated. The legacy
  `start`/`follow`/`complete` helpers used to do this for their
  `start/accept`, `start/reject`, `follow_up/end`, `complete/reject`,
  `complete/handover` sub-buttons; a §12 audit surfaced 50 such raw buttons
  across Reimu/Marisa, now fixed to route through `option(id, text, ...)`
  (which registers a key via `optionText`). Rule: pass only already-keyed
  strings to `optionKey` (shared/daily keys, `byeKey`); any user-visible button
  string goes through `option(...)`.

## 11. Generate & commit

- `./gradlew compileJava` — must pass.
- `./gradlew runData` — writes
  `src/generated/resources/data/gensokyolegacy/gensokyolegacy/{dialog,dialog_starter,quest,trade}/...`,
  `data/gensokyolegacy/loot_table/{... ,quest_req/...}`,
  `data_maps/entity_type/default_dialog.json`, and the `en_us` / `en_ud` lang.
  Commit generated JSON alongside code.
- Commit style: short lowercase one-liner (e.g. `"reimu quest"`, `"alice quest"`).

## 12. Chinese localization

- Add keys to `src/test/resources/gensokyolegacy/lang/zh_cn/<char>.json`
  (`morichika.json` is a file too, even though Morichika has no quests):
  - `"-slash": true`, nested objects mirroring the en key structure (e.g.
    `gensokyolegacy/marisa/first_mushroom/quest/title`). Because `-slash` is on,
    the dialog leaf keys are *doubled*: a dialog registered as
    `gensokyolegacy/marisa/chat/marisa/chat/hi` nests as
    `marisa → chat → dialog → marisa → chat → hi`.
  - Use `\u201c` / `\u201d` for Chinese quotes inside JSON string values.
- Merge by running the `organize.ResourceOrganizer` main (an IDE `main()`, not a
  JUnit test):
  ```
  java -cp "build/classes/java/test:<gson jar>:<datafixerupper jar>" organize.ResourceOrganizer
  ```
  This rewrites `src/main/resources/assets/gensokyolegacy/lang/zh_cn.json`.
- **Audit against en_us before regenerating**: `en` = flat keys of
  `src/generated/resources/assets/gensokyolegacy/lang/en_us.json`; expand each
  split file's nested path and `-cartesian` blocks, then
  - *dead keys* = zh-merged keys not in `en` → delete from the split file (the
    organizer rewrites the whole file, so dead keys otherwise survive forever).
  - *missing keys* = `en` keys with no zh counterpart → they fall back to
    English; translate the dialog/quest-system ones, leave furniture/hexbrew/
    umbrella names as a follow-up if out of scope.
  - Use the standalone organizer (run outside Gradle) for zh-only fixes — a full
    `./gradlew runData` regenerates `en_us` and re-touches unrelated generated
    files.
- Cross-check that every `gensokyolegacy/<char>/...` en_us key has a zh_cn
  counterpart; re-run the organizer after any en change. Current state: the
  quest/dialog/starter/greeting/trade keys are fully translated; the only dead
  zh keys are 16 block names from the dropped darkstone / packed-ice / snow set.