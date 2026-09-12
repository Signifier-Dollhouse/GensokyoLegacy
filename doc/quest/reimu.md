# Hakurei Reimu — Quest & Trade Chart (Draft)

Entity: `hakurei_reimu`. Quest unlock chain: 1.1 → 2.1 → 2.2 → 3.1 → 3.2 (linear one-time chain; dailies unlock from the corresponding one-time quest).

**Supersedes:** the prototype `reimu/kill_zombie` quest (recurrence 1000) is removed and replaced by the proper chain below. Keep `reimu/chat` starter/default dialog, the `rotten_flesh` sell trade, and the `gap_portal` buy trade (rep 100).

## Narrative arc

- **Questline 1.x — A stranger in a strange land**: Reimu is new to the Minecraft world. Her arc is exploration and survival — learning this world's land, its food, its basics — and leaning on the player for help while she finds her footing at the shrine.
- **Questline 2.x — Getting involved**: Reimu gets pulled deeper into this world. She discovers its hostile mobs and its mysterious materials, and learns to put them to her own use — research, and a talisman craft reinvented for this world.
- **Questline 3.x — The raider problem**: Reimu finds out raiders exist and learns what their ominous banners mean. Then she draws up a plan to bait them into a village and give them a hit they won't forget.

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

- **Start** — "Oh, a human visitor! Help a stranded shrine maiden out? I just got to this world and I'm still figuring out what's safe to eat here. I've been living off shrine dust. Bring me some of the local food — bread and mushroom stew — so I can see what this world has to offer."
  - Accept: "I'll bring you a meal." → "Great! Bread and mushroom stew — nothing fancy, just enough to fill a stomach and teach me the local cuisine."
  - Reject: "Can't you cook for yourself?" → "I taught myself the sword and the sutra, but 'foraging in an unfamiliar world' wasn't in the shrine maiden handbook. Please, anything helps!"
- **Follow-up** — "Trying this world's food yet?" → "Got anything for me? I keep watching the villagers chow down and feeling left out."
- **Complete** — "Welcome back! Please tell me you brought food." → handover "Here you go, a proper meal." → "Ahh, real food! I'll take notes for next time. Thank you — you're the first person I can count on in this world!"

### 2.1 `hostile_loot` — "Hostile Analysis" *(2.x: discovering this world's mobs)*

Requirements: kill `#minecraft:zombies` ×10, kill `#minecraft:skeletons` ×10; submit `rotten_flesh` ×8, `bone` ×8.

- **Start** — "Now that I've been wandering this world a bit, I keep getting ambushed by the walking dead and clattering skeletons. They're not exactly youkai, but they're bad news either way. I want to study them — kill enough of them to learn their habits, and bring back their flesh and bones as samples."
  - Accept: "I'll hunt zombies and skeletons." → "Good. Ten of each, and bring back their rotten flesh and bones for analysis. Watch out — they swarm at night."
  - Reject: "That sounds dangerous." → "It is, a little. But you've been surviving here longer than I have — you can handle it once you're ready."
- **Follow-up** — "How's the hunt going?" → "Got those samples yet? Their flesh and bones are the best lead I have on what they really are."
- **Complete** — "Here are the samples." → "Perfect — rotting flesh and clean bones, ten kills on each side. Now I can compare them against the youkai of home. Thanks for the fieldwork!"

### 2.2 `talisman_materials` — "Talisman Materials" *(2.x: discovering & utilizing new materials)*

Requirements: submit `paper` ×16, `redstone` ×8.
Reward loot table `reimu/talisman_materials`: `folded_paper_talisman` ×1, `heal_talisman` ×2.

- **Start** — "I've been looking into this world's materials. Paper here folds up nicely, and this redstone dust? It's red, it glows, and it feels full of power. I'm sure I can make this world's own ofuda with it — bring me some paper and redstone and I'll prove it."
  - Accept: "I'll gather paper and redstone." → "That's the spirit! Paper for folding, redstone as the vermilion ink — enough to stock the shrine for a while."
  - Reject: "Why redstone?" → "It's red, it glows, it's packed with power — the perfect stand-in for ink. Trust the miko on this one."
- **Follow-up** — "Got the materials yet?" → "Are the paper and redstone on the way? I want to test the new ofuda."
- **Complete** — "Here's the paper and redstone." → "Excellent! I can feel the power already. With these I'll make this world's first ofuda — and a few for you, as thanks."

### 3.1 `ominous_banner` — "Raider's Banner" *(3.x: discovering the raiders)*

Requirements: submit `ominous_banner` ×1.

- **Start** — "I've been asking around about this world's 'youkai' — and people keep whispering about *raiders* instead. Bands of armed humans marching out of wooden forts, flying black-and-white banners. I want to know what those banners mean. Bring me one from their captain — I'll read it like a sutra."
  - Accept: "I'll find a raid captain." → "Good. Their captains fly the ominous banners — take one down and bring it to me. Careful, though: cutting down a captain leaves a nasty mark on you."
  - Reject: "Raiders are dangerous." → "They're just humans swinging sharp things. You've handled real monsters — you can handle this when you're ready."
- **Follow-up** — "Have you found the banner?" → "Any luck? Check the outposts and patrols — captains wear them right on their heads."
- **Complete** — "I've got the banner." → "There it is — that black sign. Bold, but ugly. I'll study what binds these 'raiders' together, and then we'll see about giving them a taste of their own."

### 3.2 `raid` — "Repel the Raid" *(3.x: the plan to hit them)* (requires new machinery, see below)

Requirements: `raid_victory` ×1 — completed when the player *wins a raid* while the quest is active. On accept (start), Reimu applies `bad_omen` to the player.

- **Start** — "I read that banner. It's a *claim* — the raiders come to take what the villages have. And the mark that landed on you when you cut down their captain? That's their summons. So I'm going to beat them at their own game. My talismans can recreate that mark, and I'll place it on you. Walk into a village wearing it and the raiders will come swarming — then we give them a hit they won't forget."
  - Accept: "I'll repel the raid." → applies bad omen: "Here's the mark. Don't wash it off until you've walked into a village and driven out every last raider. Come back when it's done."
  - Reject: "Can't you do it yourself?" → "I've got shrine duties, and you're the one who knows this world. The mark will be waiting whenever you're ready."
- **Follow-up** — "How's the raid going?" → "Is the village still standing? They'll come in waves — hold the line!"
- **Complete** — "The raiders are gone." → "You routed them! I knew baiting them into a decisive hit would pay off. The villages owe you — and so does this shrine. Here, take my spellcard as thanks." Reward loot table `reimu/raid`: `spell_reimu` ×1.

## Daily Quests (cooldown 24000, soft cap 150, shared dialog ids, per-quest prefix)

| Id             | Unlock | Requirements | Rewards |
|----------------|--------|--------------|---------|
| `daily_food`   | 1.1 | roll 2 of (white wool ×6-8 / leather ×4-6 / iron ×6-8 / gold ×3-4 / bread ×6-8 / apple ×4-6) | exp 60, rep +10 (no cap growth) |
| `daily_hunt`   | 2.1 | kill `#minecraft:monsters` ×12 + roll 1 of (rotten flesh ×4-8 / bone ×4-8 / gunpowder ×2-4 / spider eye ×2-4) | exp 80, rep +10 (cap +5/max 130) |
| `daily_talisman` | 2.2 | paper ×16, redstone ×8 | exp 60, rep +10 (no cap growth) |
| `daily_raid`   | 3.2 | win a raid (bad omen mark) | exp 200, rep +20 (soft 200, cap +5/max 250) |

Dialog uses Marisa's shared daily pattern (`daily_start` / `daily_accept` / `daily_reject` / `daily_follow` / `daily_complete` / `daily_handover` / `daily_gotem` keys), each daily keeps its own `prefix` with unique intro/complete lines:

| Id | Intro (start) | Complete line |
|----|---------------|---------------|
| `daily_food` | "Donation box is empty again and I'm hungry. Bring me a couple of things I can use around the shrine — food and materials alike." | "Just what the shrine needed! Thanks!" |
| `daily_hunt` | "Something's been stirring up trouble for the villages near the shrine. Thin out the monsters and bring me a sample of the loot." | "Another night's sleep saved. Thanks!" |
| `daily_talisman` | "Talisman stock is running low again. Paper and redstone, just like before." | "Good, the shelves are stocked again!" |
| `daily_raid` | "A new mark is ready, and there's a village that could use protecting. Up for one more raid?" | "The village is still standing! You're a natural." |

`daily_raid`'s accept option runs both `start_quest` and the bad-omen action (the standard `dailyStart` helper needs an overload accepting extra actions).

## New machinery (Step 4 — only for 3.2 / `daily_raid`)

The current system has no "apply an effect" action and no raid-won trigger:

1. `GiveBadOmenAction(int duration)` — `DialogAction` that applies `MobEffects.BAD_OMEN` (level 0, duration e.g. 12000) to `context.sp()`. Register: `ACTION.reg("give_bad_omen", ...)`.
2. `RaidTrigger(ServerPlayer player)` — `QuestTrigger` record.
3. `RaidVictoryRequirement(String text, int count)` — `QuestRequirement` matching `RaidTrigger`. Register: `REQUIREMENT.reg("raid_victory", ...)`.
4. Mixin `Raids.Raid.tick()` — inject at the `RAID_WIN` criterion grant (`PlayerTrigger.trigger`, i.e. `hero_of_the_village`), the exact point where victory is decided once; iterate `heroesOfTheVillage`, resolve each to `ServerPlayer`, and `GLMeta.QUEST.type().getOrCreate(sp).dispatch(sp, new RaidTrigger(sp))`. Declare the mixin in `src/main/resources/gensokyolegacy.mixins.json` (unlisted mixins fail hard).
5. `ReimuQDGen` needs a helper to build an option with two actions (`StartQuestAction` + `GiveBadOmenAction`) for the 3.2/daily accept; build a `SimpleDialogOption` with a `List.of(...)` directly.

Bad-omen gameplay to explain to the player in dialog: entering a village with active Bad Omen starts the raid; winning grants Village Hero and fires the trigger — this is exactly the moment `RaidVictoryRequirement` completes.

## Trades

### Existing (keep as-is)

| Id | Direction | Gate | Details |
|----|-----------|------|---------|
| `rotten_flesh` | player sells | none | rotten flesh ×8 → emerald, stock 10 / 1200 |
| `gap_portal` | player buys | rep 100 | emerald ×10 + ender pearl ×4 + crying obsidian ×4 → gap portal ×1, stock 1 / 6000 |

### New (unlocked by quests)

| Id | Direction | Gate | Details |
|----|-----------|------|---------|
| `sell_bone` | player sells | 2.1 | bone ×8 → emerald, stock 4 / 24000 |
| `process_talisman` | processing | 2.2 | paper ×4 + redstone ×2 → `folded_paper_talisman` ×1, stock 4 / 24000 |

## Engineering checklist

1. Rewrite `ReimuQDGen`: remove `kill_zombie`; add `quests()` + `dailyQuests()` + `trades()` (mirror Marisa's private `daily(...)`/`dailyStart`/`dailyFollow`/`dailyComplete` helpers, plus the two-action overload).
2. Implement the new action / trigger / requirement and register in `CodecRegistry`; add the `Raids.Raid.tick()` mixin and declare it.
3. `./gradlew compileJava` — must pass.
4. `./gradlew runData` — regenerates `quest/`, `dialog/`, `quest_req/<prefix>/...` loot tables, and removes `kill_zombie` JSON. Commit generated JSON alongside code (commit short lowercase one-liner, e.g. `"reimu quest"`).
5. zh_cn: add keys to `src/test/resources/gensokyolegacy/lang/zh_cn/<char>.json` (use `"<char>": true` block expansion), then rerun `organize.ResourceOrganizer`.