# Hakurei Reimu — Quest & Trade Chart

Entity: `hakurei_reimu`. Quest unlock chain: 1.1 → 2.1 → 2.2 → 3.1 → 3.2 (linear one-time chain; dailies unlock from the corresponding one-time quest).

**Status: implemented** (`ReimuQDGen`, commits `reimu quest` + follow-up fixes). Dialog text below is the final text baked into the datagen class (en_us via raw lang keys; zh_cn in the split lang files). The old prototype `reimu/kill_zombie` quest is removed — only the bare `dialog/reimu/kill_zombie/{start,follow_up,complete}` dirs linger (empty/untracked, harmless).

## Narrative arc

- **Questline 1.x — A stranger in a strange land**: Reimu is new to the Minecraft world. Her arc is exploration and survival — learning this world's food — and leaning on the player for help while she finds her footing at the shrine.
- **Questline 2.x — Getting involved**: Reimu gets pulled deeper into this world. She discovers its hostile mobs and its mysterious materials, and learns to put them to her own use — research, and a talisman craft reinvented for this world.
- **Questline 3.x — The raider problem**: Reimu finds out raiders exist and learns what their ominous banners mean. Then she uses her ofuda to recreate the Bad Omen mark and bait them into a village.

## One-time Quests (soft cap 300, max cap 300)

| # | Id           | Unlock | Requirements | Rewards |
|---|--------------|--------|--------------|---------|
| 1.1 | `local_food`   | none    | bread ×8, mushroom stew ×3 | exp 50, rep +10 (cap +10/max 300) |
| 2.1 | `hostile_loot` | 1.1 | kill `#minecraft:zombies` ×10, kill `#minecraft:skeletons` ×10, rotten flesh ×8, bone ×8 | exp 100, rep +20 (cap +10/max 300) |
| 2.2 | `talisman_materials` | 2.1 | paper ×16, redstone ×8 | exp 100, rep +10 (cap +0/max 300), loot: folded paper talisman ×1 + healing talisman paper ×2 |
| 3.1 | `ominous_banner` | 2.2 | ominous banner ×1 | exp 200, rep +20 (cap +10/max 300) |
| 3.2 | `raid`        | 3.1 | win a raid (bad omen mark given by Reimu) | exp 400, rep +30 (cap +20/max 300), loot: Reimu's spellcard "Innate Dream" ×1 |

### 1.1 `local_food` — "Shrine Provisions" *(1.x: exploring & surviving in a new world)*

Requirements: submit `bread` ×8, `mushroom_stew` ×3.

- **Start** — "I'm still figuring out what's safe to eat here. Bring me some bread and mushroom stew so I can learn the local food."
  - Accept: "I'll bring you a meal." → "Great. Bread and mushroom stew. Nothing fancy, just enough to keep this shrine maiden fed."
  - Reject: "Maybe next time." → "Foraging in an unfamiliar world wasn't in the shrine maiden handbook. But I suppose I'll manage."
- **Follow-up** — "Ask about the food." → "How's the food hunt going?" → "Still looking." → "I keep watching the villagers eat and feeling left out."
- **Complete** — "Welcome back. Please tell me you brought food, not another donation."
  - Reject: "Not yet." → "No rush. I'll just keep dreaming about mushroom stew."
  - Handover: "Here you go." → "Ahh, real food. Thanks. You're the first person I can count on here."

### 2.1 `hostile_loot` — "Hostile Analysis" *(2.x: discovering this world's mobs)*

Requirements: kill `#minecraft:zombies` ×10, kill `#minecraft:skeletons` ×10; submit `rotten_flesh` ×8, `bone` ×8.

- **Start** — "The walking dead and clattering skeletons keep ambushing me. They're not exactly youkai, but I want to study them. Bring me some flesh and bones."
  - Accept: "I'll hunt zombies and skeletons." → "Good. Ten of each, plus rotten flesh and bones. Bring me enough samples to study."
  - Reject: "Maybe next time." → "It is dangerous. But you've survived here longer than I have. You'll manage when you're ready."
- **Follow-up** — "Ask about the hunt." → "How's the hunt going?" → "Still hunting." → "Got those samples yet? Flesh and bones should tell me plenty."
- **Complete** — "You're back. Please tell me you didn't become one of them."
  - Reject: "Not yet." → "No rush. Just don't let them nibble on you."
  - Handover: "Here are the samples." → "Perfect. Now I can compare these against the youkai I know. Thanks for the fieldwork."

### 2.2 `talisman_materials` — "Talisman Materials" *(2.x: discovering & utilizing new materials)*

Requirements: submit `paper` ×16, `redstone` ×8.
Reward loot table `reimu/talisman_materials`: `folded_paper_talisman` ×1, `heal_talisman` ×2.

- **Start** — "Paper here folds nicely, and this redstone dust glows with power. I can use it as vermilion ink for new-world ofuda. Bring me some."
  - Accept: "I'll gather paper and redstone." → "Good. Paper for folding, redstone for ink. That should keep the shrine stocked."
  - Reject: "Why redstone?" → "It's red, it glows, and it's full of power. That's good enough for vermilion ink."
- **Follow-up** — "Ask about the ofuda." → "Got the materials yet?" → "Still gathering." → "I want to see how well the new ofuda work."
- **Complete** — "You brought them. Let me see."
  - Reject: "Not yet." → "Okay, okay. The ofuda can wait a little longer."
  - Handover: "Here's the paper and redstone." → "Excellent. I can make the new ofuda now. I'll make a few for you, too."

### 3.1 `ominous_banner` — "Raider's Banner" *(3.x: discovering the raiders)*

Requirements: submit an Ominous Banner ×1. Matched by **item name component** (`DataComponentIngredient` on `ITEMS.WHITE_BANNER` carrying the `ItemName` "Ominous Banner"), so any banner the player names "Ominous Banner" qualifies — including real captain drops.

- **Start** — "People keep whispering about *raiders*—armed groups with black-and-white banners. Bring me one from their captain. I want to know what it means."
  - Accept: "I'll find a raid captain." → "Good. Their captains carry those ominous banners. Take one down and bring it to me. Just watch for the mark they leave behind."
  - Reject: "Maybe next time." → "They're still just humans with weapons. Come back when you're ready to deal with them."
- **Follow-up** — "Ask about the banner." → "Found their captain yet?" → "Still searching." → "Check outposts and patrols. That's where you're most likely to find one."
- **Complete** — "That's the one. Let me see what this strange banner is saying."
  - Reject: "Not yet." → "Take your time. I'll be here."
  - Handover: "Here's the banner." → "There it is. I'll study what binds these raiders together. Then we'll see what I can do about them."

### 3.2 `raid` — "Repel the Raid" *(3.x: the plan to hit them)*

Requirements: `raid_victory` ×1 — completed when the player *wins a raid* while the quest is active. On accept (start), Reimu applies `bad_omen` to the player. The follow-up dialog re-applies the mark.

- **Start** — "I figured out the mark. My ofuda can recreate it. I'll put it on you; enter a village, draw the raiders in, and drive them out."
  - Accept: "I'll repel the raid." → applies bad omen (level 0, 12000 ticks): "There. The mark is on you. Walk into a village, draw them out, and drive every raider away."
  - Reject: "Can't you do it yourself?" → "I've got shrine duties. You're the one who knows this world, so I'll leave the fighting to you."
- **Follow-up** — "Ask about the raid." → "The village still stands, but the mark is fading. Care to go again? I can set a fresh one." → "Mark me again." → re-applies bad omen: "There. Walk into a village, draw them out, and drive every raider away."
- **Complete** — "Tell Reimu the raid is over." → "The waves stopped? Good. So the plan worked."
  - Reject: "Not yet." → "Not yet? Then stay sharp. They won't give up easily."
  - Handover: "The raiders are gone." → "You routed them. I knew baiting them into the village would work. Here—take my *Innate Dream* spellcard." Reward loot table `reimu/raid`: `spell_reimu` ×1.

## Daily Quests (cooldown 24000, shared dialog ids, per-quest prefix)

| Id             | Unlock | Requirements | Rewards |
|----------------|--------|--------------|---------|
| `daily_food`   | 1.1 | roll 2 of (white wool ×6-8 / leather ×4-6 / iron ×6-8 / gold ×3-4 / bread ×6-8 / apple ×4-6) | exp 60, rep +10 (soft 150, no cap growth) |
| `daily_hunt`   | 2.1 | kill `#minecraft:monster` ×12 + roll 1 of (rotten flesh ×4-8 / bone ×4-8 / gunpowder ×2-4 / spider eye ×2-4) | exp 80, rep +10 (soft 150, cap +5/max 130) |
| `daily_talisman` | 2.2 | paper ×16, redstone ×8 | exp 60, rep +10 (soft 150, no cap growth) |
| `daily_raid`   | 3.2 | win a raid (bad omen mark) | exp 200, rep +20 (soft 200, cap +5/max 250) |

Dialog uses the shared daily pattern (`daily_start` / `daily_accept` / `daily_reject` / `daily_follow` / `daily_follow_end` / `daily_complete` / `daily_handover` / `daily_gotem` keys — one `shared/option` + `shared/dialog/daily_gotem` block per character), each daily keeps its own `prefix` with unique intro/complete lines — except `daily_raid`, whose accept/follow options need the extra bad-omen action, so it is a full `Quest` with `dailyRaidStart`/`dailyFollow(.., action)`:

| Id | Title / Intro (start) | Complete line |
|----|-----------|---------------|
| `daily_food` | "Shrine Provisions" — "The donation box is empty again, and I'm hungry. Bring me a few things for the shrine." | "Just what the shrine needed. Thanks." |
| `daily_hunt` | "Monster Thinning" — "Something's been stirring up trouble near the shrine. Thin out the monsters and bring me some loot." | "Another night's sleep saved. Thanks." |
| `daily_talisman` | "Talisman Restock" — "Talisman stock is running low again. Paper and redstone, just like before." | "Good. The shelves are stocked again." |
| `daily_raid` | "Raid Defense" — "A new mark is ready, and another village could use protecting. Ready for another raid?" | "The village is still standing. You're getting good at this." |

`daily_raid`'s accept and follow options run both `start_quest` and the bad-omen `give_mob_effect` action (the standard `dailyStart`/`dailyFollow` helpers don't apply an effect, hence the `dailyRaidStart`/overloaded-`dailyFollow`/`startRaid`/`follow(.., action)` helpers).

The shared daily follow dialog never asks "how's it going" — the player line is the player asking to re-hear the task ("Could you go over the task again?"), and Reimu replies with a reminder/encouragement.

## Raid machinery (implemented for 3.2 / `daily_raid`)

The one-time `raid` quest and its `daily_raid` twin are driven by three new pieces, all registered in `CodecRegistry`:

1. `GiveMobEffectAction(Holder<MobEffect> effect, int duration, int amplifier)` — `DialogAction` applying the effect (level `amplifier`, duration ticks, e.g. `BAD_OMEN` @ 12000) to `context.sp()`. Registered: `ACTION.reg("give_mob_effect", ...)`.
2. `RaidTrigger(ServerPlayer player)` — `QuestTrigger` record.
3. `RaidVictoryRequirement(String text, int count)` — `QuestRequirement` matching `RaidTrigger`. Registered: `REQUIREMENT.reg("raid_victory", ...)`.
4. `RaidMixin` (declared in `gensokyolegacy.mixins.json`) — `@WrapOperation` on the `PlayerTrigger.trigger(ServerPlayer)` invocation inside `Raid.tick()`, exactly where `hero_of_the_village` fires; it calls the original, then dispatches `RaidTrigger` to the player via `GLMeta.QUEST`.
5. `ReimuQDGen` helper overloads that build an option with **two** actions (`StartQuestAction` + `GiveMobEffectAction(BAD_OMEN, ...)`): `startRaid(...)` for the 3.2 quest start, `dailyRaidStart(...)` for the daily, and `follow(.., DialogAction)` / `dailyFollow(.., DialogAction)` for the re-mark follow-ups.

Bad-omen gameplay: entering a village with active Bad Omen starts the raid; winning grants Village Hero and fires the `RaidTrigger` — the exact moment `RaidVictoryRequirement` completes.

## Trades

### Existing (kept as-is)

| Id | Direction | Gate | Details |
|----|-----------|------|---------|
| `rotten_flesh` | player sells | none | rotten flesh ×8 → emerald, stock 10 / 1200 |
| `gap_portal` | player buys | rep 100 | emerald ×10 + ender pearl ×4 + crying obsidian ×4 → gap portal ×1, stock 1 / 6000 |

### New (unlocked by quests)

| Id | Direction | Gate | Details |
|----|-----------|------|---------|
| `sell_bone` | player sells | 2.1 | bone ×8 → emerald, stock 4 / 24000 |
| `process_talisman` | processing | 2.2 | paper ×4 + redstone ×2 → `folded_paper_talisman` ×1, stock 4 / 24000 |

## Engineering notes (how the datagen is structured)

- `ReimuQDGen extends QuestDialogData`, organized as `chat` (starter/default dialog) → `quests()` → `dailies()` → `trades()`, each wrapped in its own `prefix(...)`.
- As in `MarisaQDGen`, the daily scaffolding lives in a private `daily(id, title, desc, reco, conditions, exp, rep, softCap, capIncrease, maxCap, intro, acceptLine, rejectLine, followLine, optLine, completeLine, reqs)` helper; shared keys (`bye`, `daily_start` …) are registered once under the `reimu/shared` prefix.
- Daily rolls use `requestTable("daily_x", ...)` loot tables (uniform count pools) consumed via `rollItem(table)`; one-time quest rewards use `loot("reimu/x", ...)`.
- Numbers/payoff: see tables above — they match the datagen (`src/generated/resources/data/gensokyolegacy/.../{quest,dialog,dialog_starter,trade}/reimu/...`).