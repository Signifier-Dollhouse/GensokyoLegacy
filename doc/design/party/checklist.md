# Files

Every file the feature creates and modifies, and the resources it generates.

## 1. New

| File | Role |
|---|---|
| `content/attachment/party/core/PartyMath.java` | `ramp`, `weight` — pure |
| `content/attachment/party/core/PartyArea.java` | one scan: food, seats, appeal, centre, touched chunks |
| `content/attachment/party/core/PartyPlanner.java` | candidate pool + seeded weighted draw under both constraints |
| `content/attachment/party/core/PartyTaste.java` | entity-type data map record; an absent entry means **not eligible**, `DEFAULT` is a copyable template |
| `content/attachment/party/core/PartyFood.java` | override-only block data map record |
| `content/attachment/party/core/PartyFoodValue.java` | per-serving appeal derivation, totals, `consume` (food.md §1) |
| `content/attachment/party/core/PartyFoodHandler.java` | the handler list and the `claims` test that replaces the food tag (food.md §1) |
| `content/attachment/party/food/BlockFoodHandler.java` | the serves / food / consume interface (food.md §1) |
| `content/attachment/party/food/AbstractCountdownFood.java` | shared serve counting and stepping, both directions |
| `content/attachment/party/food/CakeFood.java` | slices: a permuted `*_slice` item, else vanilla's numbers |
| `content/attachment/party/food/FeastFood.java` | FD feast: 4 servings down, serving item from the block |
| `content/attachment/party/food/PieFood.java` | FD pie: 4 bites up, slice from the block |
| `content/attachment/party/food/PotFood.java` | YSF pot: 2 or 4 serves down, permuted item or the block's field |
| `content/attachment/party/food/SaucerFood.java` | YSF saucer dish: one serve, food on the block's own item |
| `content/attachment/party/food/BiteFood.java` | KC dish: `maxBites - bites`, food from `FoodBiteRegistry` |
| `content/attachment/party/food/PlateFood.java` | KC plate/platter: `servings` down, food from `PlateRegistry`'s serving items |
| `content/attachment/party/food/FoodResult.java` | serves + properties + consume, and the valid/not-valid rule |
| `content/attachment/party/food/FoodResultCache.java` | resolves each state once; invalidated on datapack sync |
| `content/attachment/party/food/Permutations.java` | finds the item form of a block food by id suffix |
| `content/attachment/party/food/BlockItemFood.java` | one serve from the block's own item; the catch-all |
| `content/attachment/party/food/Servings.java` | shared read of one serving's `FoodProperties`, and the invalid-food rule |
| `init/registrate/BlockFoods.java` | which block classes are known, gated per mod (food.md §1) |
| `content/attachment/party/core/PartyAttachment.java` | chunk capability, phase machine, roster |
| `content/attachment/party/core/PartySend.java` | the single send entry point + rejections |
| `content/attachment/party/core/PartyGift.java` | what a guest leaves, rolled from her table |
| `content/attachment/party/core/AppealConsumedCondition.java` | `{"condition":"gensokyolegacy:appeal_consumed"}`, `min`/`max`/`invert` — the gift tiers' gate |
| `init/data/loot/GiftLootGen.java` | one `party_gift/<character>` table per guest, a pool per generosity step, each banded on appeal |
| `content/attachment/party/core/PartyTicker.java` | level-tick driver, active-chunk registry |
| `content/attachment/party/item/InvitationLetterItem.java` | the letter |
| `content/attachment/party/item/PartyMessage.java` | one place that turns an outcome into player-facing text |
| `content/entity/module/PartyModule.java` | per-guest state, `arrive`/`join`/`beginLeave`/`completeLeave`, and `Origin` |
| `content/entity/behavior/task/party/PartyTasks.java` | the 11-task cascade → delay → pickup → chew → rest, and the exit |
| `content/rpg/condition/PartyCondition.java` | `{"type":"gensokyolegacy:party"}`, `invert` for home-bound |

## 2. Modified

| File | Change |
|---|---|
| `init/registrate/GLMeta.java` | `PARTY`, `PARTY_FOOD`, `PARTY_TASTE` |
| `init/registrate/GLBrains.java` | `PARTY`, `MEM_PARTY` |
| `init/registrate/GLItems.java` | `INVITATION_LETTER`, calls `BlockFoods.register()` |

| `content/entity/youkai/YoukaiEntity.java` | `PartyModule` in `createModules()`, `isPartying()`, `broadcastEat()` |
| `content/entity/youkai/YoukaiFlags.java` | append `PARTYING` (ordinal is the synced bit index) |
| `content/entity/youkai/SmartYoukaiEntity.java` | `PARTY(200)` + `board.addPartyTasks()` |
| `content/entity/characters/fairy/CirnoEntity.java` | `PartyModule` in its `createModules()` override |
| `content/entity/characters/magician/AliceEntity.java` | same |
| `content/entity/characters/fairy/PlainFairyEntity.java` | `broadcastEat()` as the override of the base no-op |
| `content/entity/module/VisitModule.java` | stand down while she is partying (guest.md §2) |
| `content/client/debug/CharacterRequestToServer.java` | carry a guest's tally in the existing character-info answer (debug.md §4) |
| `content/client/debug/CharacterInfoToClient.java` | the `PartyTally` record and its two lines |
| `init/registrate/GLEntities.java` | `TASTE_*` constants, applied via `.dataMap(GLMeta.PARTY_TASTE.reg(), …)` |
| `content/rpg/core/CodecRegistry.java` | `PARTY` condition |
| `content/rpg/core/ServerCharacterDialogManager.java` | suppress quest `START` while partying, as for visiting |
| `content/rpg/action/StartQuestAction.java` | same guard, for dialog-driven starts |
| `content/command/GLCommands.java` | `/gensokyo party` |
| `content/attachment/party/item/PartyWand.java` | the debug wand: hover, eat, simulate (debug.md §2) |
| `content/attachment/party/item/PartyWandData.java` | the simulated result, as the wand's data component payload |
| `content/attachment/party/item/PartyWandComponent.java` | the `DataComponentType`, registered on the mod bus |
| `init/data/GLModConfig.java` | `partyScanDistance`, `partyGuestRadius` |
| `init/data/GLLang.java` | `Party` enum, wired into `genLang`'s group array |
| `init/data/GLTagGen.java` | `PARTY_TABLE`, `PARTY_SEAT` and `genPartyTag` (area.md §1) |
| `event/GLMetaEventHandlers.java` | `PartyTicker.tick` on level tick, `clear` on level unload |
| `build.gradle` | Farmer's Delight + Youkai's Feasts as `compileOnly` from the `libs/` flatDir |

## 3. New resources

| File | Change |
|---|---|
| `src/generated/resources/data/gensokyolegacy/tags/block/party_{table,seat}.json` | generated by `runData`, area.md §1 |
| `src/generated/resources/data/gensokyolegacy/data_maps/entity_type/party_taste.json` | generated |
| `src/generated/resources/assets/gensokyolegacy/models/item/invitation_letter.json` | generated |
| `src/test/resources/gensokyolegacy/lang/zh_cn/party.json` | zh_cn strings, merged by `LangFileOrganizer` |

There is **no** `data_maps/block/party_food.json` — the data map is an override and nothing in the mod needs one (food.md §1).

The `large_table` support change is a behavioural change to a shipped block and lands in its own commit, separate from the party feature.
