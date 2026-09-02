# Quest & Trade Design Workflow

How to plan, draft, polish, and implement quests/trades for a new character in Gensokyo Legacy. This is the process used for Marisa and acts as a template.

## 1. Plan (`doc/`)

Write the design spec in `doc/<char>_quests.md`:

- **One-time quest chain**: ordered quests with a clear unlock chain. Each quest lists requirements (items to submit / mobs to kill), rewards (exp + reputation + optional loot), and dialog (intro / follow-up / complete).
- **Unclock conditions**: earlier quests unlock via `HasQuestCompletedCondition`; Nether/fortress gating uses vanilla advancements (`nether/root`, `nether/find_fortress`) via `HasAdvancementCondition`.
- **Daily quests**: `QuestRecurrence` (cooldown ~24000 ticks = 1 game day), shared short dialog ids but unique per quest `prefix`.
- **Trades**:
  - *Restocking* (player sells items to character for emeralds) — short restock time.
  - *Offering* (player buys hexbrews etc. for emeralds) — gated by `SelfReputationCondition` tiers; higher rep = better/rarer stock.
  - *Processing / craft-style* (item + item → item) — unlocked by a completed quest.
- **Reputation gates & prices** for offering trades; **exp/rep rewards** per quest.

Extract a review-style summary into `doc/draft.md`, get it polished → `doc/polished.md` (final dialog + notes). The final Chinese localization lives in `doc/<char>.docx`.

## 2. Draft — create the datagen class

New `init/data/rpg/<Char>QDGen.java` extending `QuestDialogData`:

```java
public class CharQDGen extends QuestDialogData {
    public CharQDGen() {
        super();
        prefix("<char>/chat");
        defaultDialog(entity, greeting, tradeLine);
        starter("<char>/chat", new DialogStarter(entity, List.of(),
                starterText("start", "..."),
                dialog("hi", "...", option("chat/bye", "Bye!"))));

        prefix("<char>/<quest_id>");
        quest("<char>/<quest_id>", new Quest(entity,
                List.of(new HasQuestCompletedCondition(PREV)),
                questTitle("Title"), questDesc("..."),
                Optional.empty(),
                new TreeMap<>(Map.of("a-x", new SubmitItemRequirement(List.of(item(ITEM, 4))))),
                List.of(new ExpReward(50), new ReputationReward(40, 100, 10, 200)),
                start(...), follow(...), complete(...)));

        prefix("<char>");
        trade("restock_x", entity, new ItemStack(Items.EMERALD),
                new TradeRecurrence(10, 1200), item(ITEM, 8));
        trade("offer_y", new TradeOffer(entity,
                List.of(new SelfReputationCondition(50)),
                new ItemStack(HexBrew.MIASMA_HEXBREW.bottle.get()),
                new TradeRecurrence(3, 2400), List.of(item(Items.EMERALD, 3))));
        trade("process_z", new TradeOffer(entity,
                List.of(new HasQuestCompletedCondition(QUEST)),
                result, new TradeRecurrence(1, 24000),
                List.of(item(INPUT_A, 1), item(INPUT_B, 1))));
    }
}
```

Conventions:

- Organize content by `prefix(...)` scope (chat / each quest / trades).
- Item refs: mushroom caps/blocks via `GLNaturalBlocks.MUSHROOM_SET` (ghost / dream / demonic_miasma) `.cap`/`.block`; entity via `GLEntities.<CHAR>.get()`; hexbrews via `HexBrew.*.bottle`.
- Reuse base helpers (`start/follow/complete`, `daily`) on `QuestDialogData`; note the private `prefix` state means sections must be self-contained.
- `processing`-style trades are plain `TradeOffer` — the currency heuristic already suppresses the price tag and shows the result for non-currency item trades, so no extra field is needed.

## 3. Register & wire

In the `GensokyoLegacy` constructor:

```java
var reimu = new ReimuQDGen();
var marisa = new MarisaQDGen();
var char = new CharQDGen();
QuestDialogData.build(REGISTRATE, reimu, marisa, char);
```

`QuestDialogData.build(L2Registrate, QuestDialogData...)` is a single static method that registers each datapack registry (dialog / starter / quest / trade) *exactly once*, iterating all instances' content — this must be a single `.add` per registry or `RegistrySetBuilder` throws `Multiple entries with same key`. Subclasses keep **per-instance (non-static) content maps**; `build` owns the one-time registration.

Register any new condition / action / requirement / reward subclasses in `CodecRegistry` (e.g. `HAS_QUEST` → `HasQuestCompletedCondition`).

## 4. Implement base machinery (only if missing)

Only add shared code if an existing feature is missing (e.g. a new condition class, a `processing` trade mode). Verify the UI already renders the case before adding fields.

## 5. Generate & commit

- `./gradlew compileJava` — must pass.
- `./gradlew runData` — writes `src/generated/resources/data/gensokyolegacy/gensokyolegacy/{dialog,dialog_starter,quest,trade}/...` plus `data_maps/entity_type/default_dialog.json`. Commit generated JSON alongside code.
- Commit style: short lowercase one-liner (e.g. `"marisa quest"`).

## 6. Chinese localization

- Add keys to `src/test/resources/gensokyolegacy/lang/zh_cn/<char>.json`:
  - `"-slash": true`, nested objects mirroring the en key structure (e.g. `gensokyolegacy/marisa/first_mushroom/quest/title`).
  - Use `\u201c` / `\u201d` for Chinese straight double quotes inside JSON string values.
- Merge by running the `organize.ResourceOrganizer` main:
  ```
  java -cp "build/classes/java/test:<gson jar>:<datafixerupper jar>" organize.ResourceOrganizer
  ```
  This rewrites `src/main/resources/assets/gensokyolegacy/lang/zh_cn.json`. Cross-check that every `gensokyolegacy/<char>/...` en_us key has a zh_cn counterpart (the organizer rewrites the whole file, so re-run after any en change).