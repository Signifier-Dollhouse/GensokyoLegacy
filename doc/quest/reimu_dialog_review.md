# Reimu Quest Dialog — Review Package

This package is for an **external AI reviewer**. It contains everything needed to review (and where asked, polish) the English dialog for the Hakurei Reimu questline in Gensokyo Legacy (NeoForge 1.21.1 mod). It documents the dialog format and structure the mod's systems require, the character, the themes of each questline, the requested in-world facts, and the current draft you are asked to review.

Source of truth for implementation-level detail: `doc/quest/reimu.md`. This file focuses on dialog review.

---

## 1. What you are reviewing

Five one-time quests and four daily quests for the character **Reimu Hakurei** (`hakurei_reimu`), a shrine maiden from Gensokyo who has just arrived in the Minecraft-like world the player lives in.

Your job: review the drafted dialog lines below for **(a)** in-character voice, **(b)** correct arc and escalation across the three questlines, **(c)** internal consistency (callbacks between quests), **(d)** clarity and length, and **(e)** consistency with the requested objectives (which items/mobs are asked for). Then return, per quest, a verdict + targeted line edits. Do **not** change quest balance (item counts, exp/reputation, unlock order) — those are out of scope.

---

## 2. How dialog becomes in-game content

Every quest dialog you write is data, not code. The datagen (Java) class shapes it into **three dialog nodes per quest**, and the game **auto-lists** those nodes in Reimu's conversation menu based on the player's quest state:

| State (when the player talks to Reimu) | Node shown | You provide… |
|---|---|---|
| Quest available (unlock met, not yet started) | **Start** | start button + intro line + accept/reject branches |
| Quest started, requirements not met | **Follow-up** | follow button + a "how's it going?" line + one response line |
| Requirements met | **Complete** | complete button + Reimu's reaction line + handover/reject branches |

The character greeting (a separate `DialogStarter`) is fixed and shared; a **"Bye!"** exit option always ends the conversation. Do not add new top-level topics beyond the three nodes per quest.

### 2.1 Node anatomy (one-time quests)

Each node is: a **button** (short label shown in the conversation list) → a **Reimu line** (a `Dialog`) → one or more **options**. Every branch must end in a closing option that returns to the conversation (a "Bye!"-style exit). The accepted shapes (mirroring the existing Marisa quests):

```
START
  button:           "Talk about X."            ← short phrase, shown in menu
  Reimu intro:      "<full line introducing the request>"
    option reject:  "Maybe next time."         ← optional
      Reimu:        "<short understanding line>"
    option accept:  "Sure!"                    ← MUST trigger start_quest action
      Reimu:        "<acknowledge + repeat the goal>"

FOLLOW-UP
  button:           "Ask about X."
  Reimu:            "<how's it going?>"
    option on-it:   "I'm on it!"               ← no action
      Reimu:        "<encouragement line>"

COMPLETE
  button:           "Hand over X."
  Reimu:            "<excited confirmation line>"
    option reject:  "Not yet."                 ← optional
      Reimu:        "<no-rush line>"
    option handover:"Here you go!"             ← MUST trigger complete_quest action
      Reimu:        "<thanks + reaction line>"
```

Rules for one-time quests:

- **Buttons are player-facing action phrases** (≤ 6 words): "Talk about the mushrooms.", "I'll bring you a meal.", "Here you go.", "Maybe next time." Feel free to vary them per quest.
- **Reimu lines are spoken text** (1–2 sentences, roughly 8–20 words; the intro may be a bit longer). No channel/system text.
- The accept option of the *start* node and the handover option of the *complete* node are the only places quest actions fire; nothing else may trigger them.
- Every visual branch (reject / accept / on-it / handover) ends in a closing line, after which the player returns to the menu. No dead ends.

### 2.2 Node anatomy (daily quests)

Dailies reuse one **fixed shared button set** (same labels for all four dailies, contributed once under a shared prefix). The only unique text per daily is the spoken lines. Structure:

```
START
  button:  "I can gather that for you."
  Reimu intro (unique):  "<the day's request>"
    reject:  "Maybe later."     → Reimu reject line (unique)
    accept:  "I'll do it!"      → triggers start_quest; Reimu accept line (unique)

FOLLOW-UP
  button:  "How's it going?"
  Reimu follow line (unique)
    on-it: "I'm on it!"         → Reimu on-it line (unique)

COMPLETE
  button:  "I've got the goods!"
  Reimu:   "Oh, you got 'em? Let me see!"   ← shared catch line
    handover: "Here you go!"    → triggers complete_quest; Reimu complete line (unique)
```

Fixed shared button labels (do not change): `I can gather that for you.` / `Maybe later.` / `I'll do it!` / `How's it going?` / `I'm on it!` / `I've got the goods!` / `Here you go!`; shared catch line `Oh, you got 'em? Let me see!`.

### 2.3 Global format rules

- **Emphasis:** use `*asterisks*` around a word for emphasis (renders as italic).
- **Person:** buttons are first-person player ("I'll…", "Sure!"); Reimu addresses the player as "you".
- **Length:** buttons ≤ 6 words; branch lines ≤ ~12 words; intro/complete lines ≤ ~25 words.
- **No meta.** Do not mention quest ids, achievements, items-by-id, triggers, cooldowns, reputation, or any system the character couldn't know. Reimu *may* name in-world things (bread, mushroom stew, paper, redstone dust, ominous banner, villages, raiders).
- **No reveals ahead of their quests.** e.g. redstone's *power* is discovered in 2.2; the raider's "mark" meaning is only explained in 3.1→3.2.
- **Translations:** Chinese localization is authored later from these English lines. Avoid puns, wordplay, or culture-specific jokes that can't transfer.

---

## 3. Character: voice & tone guide

Reimu Hakurei — the Hakurei shrine maiden. Recently arrived in this world, homeless, living at a small shrine with an empty donation box.

- **Confident and self-assured** — she has seen/nixed much weirder "youkai" than zombies, so she stays calm and casual about danger.
- **Casual, direct, plain-spoken.** Short sentences. Dry, slightly lazy laid-back humor. She is friendly but not saccharine; glad for help but never groveling.
- **Practical, a little hungry.** She's poor, so food/restock requests come up naturally (the running "donation box" gag).
- She calls this world's hostile mobs by their local names (zombies, skeletons) but thinks of monsters generally as "youkai-like".
- **Do NOT:** use "ze" (that's Marisa); use heavy magic-research jargon (Marisa's domain); sound academic; use Japanese honorifics; reference other Gensokyo characters.

---

## 4. The three questlines — themes the dialogs must serve

| Line | Title | Theme |
|---|---|---|
| **1.x** | A stranger in a strange land | Reimu is new to this world: exploring it and surviving. She leans on the player for basics. Tone: newcomer curiosity, light gratitude. |
| **2.x** | Getting involved | Reimu gets pulled into this world: discovers its hostile mobs (2.1) and its materials (2.2) and puts them to her own use — research, and an ofuda craft reinvented here. Tone: growing competence, "I've got this world figured out." |
| **3.x** | The raider problem | Reimu finds out raiders exist, deciphers their ominous banners (3.1), then devises a plan to bait and crush them (3.2). Tone: investigative → tactical, decisive. This is the payoff arc — the plan should feel like a plan. |

Escalation across 1→2→3 should be felt: chores → research → counterstrike.

---

## 5. In-world facts / objective sheet (keep these straight)

| Quest | Asks for / involves | Canonical details to respect |
|---|---|---|
| 1.1 | bread ×8, mushroom stew ×3 | She is new here; doesn't yet know the local cuisine. |
| 2.1 | kill zombies ×10 + skeletons ×10; submit rotten flesh ×8 + bone ×8 | "Walking dead" and "clattering skeletons" — she wants to *study* them, comparing to Gensokyo youkai. Only flesh and bones are asked for. |
| 2.2 | paper ×16, redstone ×8 | Paper = paper; redstone dust = red, glowing, full of power; used as the "vermilion ink" to write ofuda. Reward includes folded/paper talismans she made. |
| 3.1 | ominous banner ×1 (from a raid captain) | Killing a pillager **captain** drops the ominous banner *and* leaves the killer with an ominous "mark" (Bad Omen). Reimu warns about the mark; she wants to read what the banner "means". |
| 3.2 | win a raid | Reimu recreates the mark (Bad Omen) via her talismans and places it on the player. Entering a **village** with the mark summons raiders; the player must repel them. Reward: Reimu's spellcard "Innate Dream". |
| daily_food | any random mix of wool / leather / iron / gold / bread / apple | generic restock chore. |
| daily_talisman | paper ×16, redstone ×8 | reprising 2.2. |
| daily_hunt | kill hostiles, bring loot | generic thining-out chore. |
| daily_raid | win a raid (mark again) | reprising 3.2. |

The "mark" (Bad Omen) is an established thread: 3.1 warns it appears, 3.2 explains and weaponizes it. Keep that continuity.

---

## 6. Quest overview (context only — do not alter)

One-time (soft cap 300/300 rep): 1.1 → 2.1 → 2.2 → 3.1 → 3.2 linear.
Rows: unlock / requirements / rewards at `doc/quest/reimu.md`. Dailies unlock from the matching one-time quest.

---

## 7. Draft dialogs for review

### 7.0 Greeting (fixed, existing — light review only)

- Conversation-starter line → `"Hi!"` → `"Hi!"` (option: `Bye!`)
- Menu header greeting: `"Hi! What brings you to the shrine?"`
- Trade header: `"I'd like to trade with you!"`

### 7.1 Quest 1.1 `local_food` — "Shrine Provisions" *(arc 1)*

**START**
- button: `Talk about food.`
- Reimu: "Oh, a human visitor! Help a stranded shrine maiden out? I just got to this world and I'm still figuring out what's safe to eat here. I've been living off shrine dust. Bring me some of the local food — bread and mushroom stew — so I can see what this world has to offer."
  - reject `Maybe next time.` → Reimu: "I taught myself the sword and the sutra, but 'foraging in an unfamiliar world' wasn't in the shrine maiden handbook. Please, anything helps!"
  - accept `I'll bring you a meal.` → Reimu: "Great! Bread and mushroom stew — nothing fancy, just enough to fill a stomach and teach me the local cuisine."

**FOLLOW-UP**
- button: `Ask about the food.`
- Reimu: "Trying this world's food yet?"
  - on-it `Still looking.` → Reimu: "Got anything for me? I keep watching the villagers chow down and feeling left out."

**COMPLETE**
- button: `Hand over the food.`
- Reimu: "Welcome back! Please tell me it's food and not another donation box."
  - reject `Not yet.` → Reimu: "Sure, no rush. I'll just sit here and dream about mushroom stew."
  - handover `Here you go.` → Reimu: "Ahh, real food! I'll take notes for next time. Thank you — you're the first person I can count on in this world!"

### 7.2 Quest 2.1 `hostile_loot` — "Hostile Analysis" *(arc 2)*

**START**
- button: `Offer to hunt for samples.`
- Reimu: "Now that I've been wandering this world a bit, I keep getting ambushed by the walking dead and clattering skeletons. They're not exactly youkai, but they're bad news either way. I want to study them — kill enough to learn their habits, and bring back their flesh and bones as samples."
  - reject `Maybe next time.` → Reimu: "It is a little dangerous. But you've been surviving here longer than I have — you can handle it once you're ready."
  - accept `I'll hunt zombies and skeletons.` → Reimu: "Good. Ten of each, and bring back their rotten flesh and bones. Watch out — they swarm at night."

**FOLLOW-UP**
- button: `Ask about the hunt.`
- Reimu: "How's the hunt going?"
  - on-it `Still hunting.` → Reimu: "Got those samples yet? Flesh and bones are the best lead I have on what they really are."

**COMPLETE**
- button: `Hand over the samples.`
- Reimu: "You're back! Please tell me you didn't become one of them."
  - reject `Not yet.` → Reimu: "No rush — just don't let them nibble on you."
  - handover `Here are the samples.` → Reimu: "Perfect — rotting flesh and clean bones, ten kills on each side. Now I can compare them against the youkai of home. Thanks for the fieldwork!"

### 7.3 Quest 2.2 `talisman_materials` — "Talisman Materials" *(arc 2)*

**START**
- button: `Talk about the talismans.`
- Reimu: "I've been looking into this world's materials. Paper here folds up nicely, and this redstone dust? It's red, it glows, and it feels full of power. I'm sure I can make this world's own ofuda with it — bring me some paper and redstone and I'll prove it."
  - reject `Why redstone?` → Reimu: "It's red, it glows, it's packed with power — the perfect stand-in for ink. Trust the miko on this one."
  - accept `I'll gather paper and redstone.` → Reimu: "That's the spirit! Paper for folding, redstone as the vermilion ink — enough to stock the shrine for a while."

**FOLLOW-UP**
- button: `Ask about the ofuda.`
- Reimu: "Got the materials yet?"
  - on-it `Still gathering.` → Reimu: "Are the paper and redstone on the way? I want to test the new ofuda."

**COMPLETE**
- button: `Hand over the materials.`
- Reimu: "You brought them! Let me see, let me see."
  - reject `Not yet.` → Reimu: "Okay, okay — the ofuda shelves can wait a little longer."
  - handover `Here's the paper and redstone.` → Reimu: "Excellent! I can feel the power already. With these I'll make this world's first ofuda — and a few for you, as thanks."

### 7.4 Quest 3.1 `ominous_banner` — "Raider's Banner" *(arc 3)*

**START**
- button: `Talk about the raiders.`
- Reimu: "I've been asking around about this world's 'youkai' — and people keep whispering about *raiders* instead. Bands of armed humans marching out of wooden forts, flying black-and-white banners. I want to know what those banners mean. Bring me one from their captain — I'll read it like a sutra."
  - reject `Maybe next time.` → Reimu: "They're just humans swinging sharp things. You've handled real monsters — you can handle this when you're ready."
  - accept `I'll find a raid captain.` → Reimu: "Good. Their captains fly the ominous banners — take one down and bring it to me. Careful, though: cutting down a captain leaves a nasty mark on you."

**FOLLOW-UP**
- button: `Ask about the banner.`
- Reimu: "Found their captain yet?"
  - on-it `Still searching.` → Reimu: "Check the outposts and patrols — captains wear them right on their heads."

**COMPLETE**
- button: `Hand over the banner.`
- Reimu: "That's the one! Let me look at it properly…"
  - reject `Not yet.` → Reimu: "Take your time hunting it down."
  - handover `Here's the banner.` → Reimu: "There it is — that black sign. Bold, but ugly. I'll study what binds these 'raiders' together, and then we'll see about giving them a taste of their own."

### 7.5 Quest 3.2 `raid` — "Repel the Raid" *(arc 3; accept applies the Bad Omen mark)*

**START**
- button: `Talk about the plan.`
- Reimu: "I read that banner. It's a *claim* — the raiders come to take what the villages have. And that mark that landed on you when you cut down their captain? That's their summons. So I'm going to beat them at their own game. My talismans can recreate that mark, and I'll place it on you. Walk into a village wearing it and the raiders will come swarming — then we give them a hit they won't forget."
  - reject `Can't you do it yourself?` → Reimu: "I've got shrine duties, and you're the one who knows this world. The mark will be waiting whenever you're ready."
  - accept `I'll repel the raid.` (applies mark) → Reimu: "Here's the mark. Don't wash it off until you've walked into a village and driven out every last raider. Come back when it's done."

**FOLLOW-UP**
- button: `Ask about the raid.`
- Reimu: "Is the village still standing?"
  - on-it `Still fighting.` → Reimu: "They'll come in waves — hold the line!"

**COMPLETE**
- button: `Tell Reimu the raid is over.`
- Reimu: "The waves… they've stopped?"
  - reject `Not yet.` → Reimu: "Then keep your eyes open — they regroup quickly."
  - handover `The raiders are gone.` → Reimu: "You routed them! I knew baiting them into a decisive hit would pay off. The villages owe you — and so does this shrine. Here, take my spellcard as thanks."

### 7.6 Dailies

Buttons are the fixed shared set (§2.2). Per daily, the unique lines:

**daily_food** — periodic restock (unlock: 1.1)
- intro: "Donation box is empty again and I'm hungry. Bring me a couple of things I can use around the shrine — food and materials alike."
- accept: "That's my helper! Bring back a good haul."
- reject: "Aw. A starving miko is a distracted miko, you know."
- follow: "How's the haul coming along?"
- on-it: "Take your time — just come back with something worth cooking!"
- complete: "Just what the shrine needed! Thanks."

**daily_talisman** — paper + redstone (unlock: 2.2)
- intro: "Talisman stock is running low again. Paper and redstone, just like before."
- accept: "Great! Restock me and I'll keep the shrine warded."
- reject: "No stock, no warding. It's your call."
- follow: "Any paper and redstone yet?"
- on-it: "The ofuda shelves are starting to echo."
- complete: "Good, the shelves are stocked again!"

**daily_hunt** — kill hostiles + loot (unlock: 2.1)
- intro: "Something's been stirring up trouble for the villages near the shrine. Thin out the monsters and bring me a sample of the loot."
- accept: "That's the spirit! Rush them before they organize."
- reject: "They'll only grow bolder. But it's your call."
- follow: "How's the thrashing going?"
- on-it: "Stay careful out there — I need you whole to finish this."
- complete: "Another night's sleep saved. Thanks!"

**daily_raid** — win a raid (unlock: 3.2; accept applies the mark)
- intro: "A new mark is ready, and there's a village that could use protecting. Up for one more raid?"
- accept: "Here's the mark, same as before. Walk in, hold the line, drive them out."
- reject: "The mark keeps. Real raiders will be waiting whenever you're ready."
- follow: "Is the village holding up?"
- on-it: "Waves keep coming — don't drop your guard!"
- complete: "The village is still standing! You're a natural."

---

## 8. Reviewer output format

For each item (greeting / 7.1–7.6), return:

1. **Verdict:** keep / minor edits / rewrite.
2. **Tone check** (matches §3 & §4).
3. **Consistency check** (callbacks: mark, ofuda, banner, spellcard).
4. **Facts check** (matches §5 — requested items, mobs, unlock logic).
5. **Format check** (§2 — button roles, branch completeness, length, no meta leaks).
6. **Suggested lines** (exact replacement text, one line per quoted string, using `*asterisks*` where emphasis is wanted).