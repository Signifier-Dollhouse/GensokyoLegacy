# Prompt

Plan marisa quests and trades. Design necessary dialog/text yourself. Write a draft dialog in a format that I can submit to other AI for polishing.

## One-time quests
- (1.1) Marisa expressed that this world is new to her, and ask player to help collect some red and brown mushroom for research.
- (1.2, unlocked by 1.1) Marisa expressed that it's her first time to see huge mushrooms, but she has no idea how to collect them. She want player to get some large mushroom blocks.
- (2.1, unlocked by 1.1 and player visited nether) Marisa discuss the nether with player, and wants player to collect some mushrooms from the nether.
- (2.2, unlocked by 2.1) Player talked about the fungi trees in nether and the shroomlights. Marisa wants some samples.
- (2.3, unlocked by 2.2 and player visited fortress) Player talked about blaze and nether wart. Marisa is interested in understanding the native minecraft potion system, and ask for some samples of blaze rod and nether wart.
- (2.4, unlocked by 2.3) Player talked about enchanted golen apple and how it's a lost technology to make them. Marisa wants an sample and promise to be able to reproduce it.

## Daily quests
- Marisa wants player to help her collect some materials. Mainly mushrooms from this mod.
- Marisa wants player to collect some rotten flesh, spider eye, and miasma mushrooms
- (unlocked by 2.2) Marisa wants player to help her collect some mushrooms and shroomlights from nether.
- (unlocked by 2.3) Marisa wants player to help collect some brewing ingredients.

## Trades - restocking
- Marisa buys nether mushrooms, shroomlights, and mod alchemy materials from player. nether stuff unlocked after respective one-time quest.
- Marisa buys mob loot from player.

## Trades - offering
- Marisa sells hexbrews to player with varying reputation requirements

## Trades - processing
- Marisa can help using golden apple + golden block to make enchanted golden apple after 2.4 completed
- Marisa can help to concentrate 4 mundane hexbrew into 1 hexbrew elixir

# Draft

This section now reflects the **polished** draft (see `doc/polished.md`, the
response from the external review AI). It is the authoritative spec for
implementation. The polished source is reproduced below in condensed form;
`doc/polished.md` contains the full verbatim lines.

Character entity: `gensokyolegacy:kirisame_marisa`.
Voice: energetic, confident, casually bossy, research-obsessed, proud of her
magic; uses "ya"/"ze"/"human" naturally. Player choice labels are distinct from
Marisa's spoken dialogue.

Keys mirror the Reimu system: `marisa/<quest>/{title,desc,requirement,option,dialog}`,
+ `marisa/chat` starter.

---

## 1. One-time quests

### (1.1) First mushrooms — `marisa/first_mushroom`
Unlock: none. Type: one-time.
- Initiation: "Ah, a new human! Marisa Kirisame, the ordinary magician, zo! ... Bring me some red and brown mushrooms from the surface, would ya?"
  - Reject "Ehh, sounds like a hassle." → "Aw, c'mon! Mushrooms are the foundation of every good potion recipe..."
  - Accept "Sure, I'll gather some." (`start_quest`) → "That's the spirit, ze! Eight red or brown mushrooms'll do."
- Requirement: red/brown mushroom ×8 (red ×4 + brown ×4). Desc: "Bring Marisa red and brown mushrooms from the surface."
- Follow-up "Got 'em yet? I can practically taste the potion potential!" → "Take your time — just don't skimp on me, now!"
- Completion "Ohoho, these are perfect! ..." → no-op "Heh. You're alright, human." / complete "Wahoo! Thanks a ton, human!"
- Rewards: exp 50, rep +40.

### (1.2) Giant mushrooms — `marisa/huge_mushroom`
Unlock: requires 1.1. Type: one-time.
- Initiation "Whoa, hold on. Have ya seen the mushrooms around here? They're practically trees! ..."
  - Reject "Can't you just break them yourself?" → "Tried it! They shatter into tiny caps..."
  - Accept "I'll bring you fresh blocks." (`start_quest`) → "That's my human! Bring 'em intact..."
- Requirement: huge mushroom blocks ×8 (stems/caps acceptable). Desc: "Bring Marisa huge mushroom blocks."
- Completion "THESE! These are exactly what I needed! ..." → complete "You're a lifesaver, human!"
- Rewards: exp 100, rep +50.

### (2.1) Nether mushrooms — `marisa/nether_mushroom_prep`
Unlock: requires 1.1 AND visited Nether. Type: one-time.
- Initiation "Say — you've been to that creepy red world under the rock, right? ..."
  - Reject "The Nether scares even me." → "Heh, fair enough. But that's where the really good research material is!"
  - Accept "I've been to the Nether. I can do this." (`start_quest`) → "Then I knew I could count on ya!"
- Requirement: nether mushroom/wart material ×8. Desc: "Bring Marisa nether mushroom samples."
- Completion "Oh man, look at this! ..." → complete "Science! You get it! Thanks a million!"
- Rewards: exp 150, rep +50. Unlocks: nether daily + nether trades.

### (2.2) Shroomlight & fungus trees — `marisa/shroomlight`
Unlock: requires 2.1. Type: one-time.
- Initiation "Okay, okay! You mentioned those giant *fungus trees* in the Nether — whole towers of mushroom! ..."
  - Reject "They're dangerous to climb." → "Everything good is a little dangerous!"
  - Accept "I'll bring back samples." (`start_quest`) → "Now we're talkin'!"
- Requirement: shroomlight ×4 + nether fungus stem ×4. Desc: "Bring Marisa shroomlights and nether fungus blocks."
- Completion "Ohhh, these little lights are *beautiful*! ..." → complete "Like 'em? I love 'em!"
- Rewards: exp 150, rep +50. Unlocks: shroomlight daily + nether restock trades.

### (2.3) Brewing the Minecraft way — `marisa/brewing`
Unlock: requires 2.2 AND visited Nether fortress. Type: one-time.
- Initiation "Hold up! You mentioned *blazes* down there, right? ..."
  - Reject "Blazes are tough to fight." → "So are goblins, and I've survived those bars for years!"
  - Accept "I'll get you the samples." (`start_quest`) → "Right on! Blaze rods and nether wart..."
- Requirement: blaze rod ×4 + nether wart ×12. Desc: "Bring Marisa blaze rods and nether wart."
- Completion "Jackpot! Blaze rods and nether wart — now I can finally study this 'brewstand' business..." → complete "Magic! That's the spirit!"
- Rewards: exp 200, rep +60. Unlocks: brewing daily + brewing restock trades.

### (2.4) Enchanted golden apple — `marisa/golden_apple`
Unlock: requires 2.3. Type: one-time.
- Initiation "One more thing, one more thing! Ever seen those fancy golden apples — the glowy ones...?"
  - Reject "Those are really rare." → "Rare stuff is exactly the fun stuff!"
  - Accept "I'll try to find one." (`start_quest`) → "That's the Marisa-approved spirit!"
- Requirement: enchanted golden apple ×1. Desc: "Bring Marisa an enchanted golden apple."
- Completion "THIS! This is a treasure, human! ..." → complete "Heh! With me around, ain't nothin' impossible!"
- Rewards: exp 300, rep +80. Unlocks: golden-apple processing trade.

---

## 2. Daily quests (recurrence cooldown ≈ 24000 ticks = 1 day)

| id | unlock | requirement | reward (exp / rep) |
|----|--------|-------------|--------------------|
| `marisa/daily_mycelium` | always | ghost_fire ×4 + dream ×4 + demonic miasma ×4 | 60 / +15 |
| `marisa/daily_witchcraft` | always | rotten flesh ×8 + spider eye ×4 + demonic miasma ×4 | 80 / +15 |
| `marisa/daily_shroomlight` | 2.2 | shroomlight ×4 + crimson fungus ×4 | 120 / +20 |
| `marisa/daily_brewing` | 2.3 | blaze rod ×2 + nether wart ×8 | 150 / +25 |

Each has accept (`start_quest`) / reject options; dialog lines per `polished.md`.

---

## 3. Trades — Restocking (player sells TO Marisa; emeralds)

| id | condition | player gives | currency | maxStock / restock |
|----|-----------|--------------|----------|--------------------|
| `marisa/sell_mod_shroom` | always | ghost_fire mushroom ×8 | 1 emerald | 10 / 1200 |
| `marisa/sell_dream_shroom` | always | dream mushroom ×8 | 1 emerald | 10 / 1200 |
| `marisa/sell_miasma_shroom` | always | demonic miasma mushroom ×8 | 1 emerald | 8 / 1200 |
| `marisa/sell_mob_loot` | always | rotten flesh ×8 / spider eye ×4 / bones | 1 emerald | 14 / 1200 |
| `marisa/sell_shroomlight` | 2.2 | shroomlight ×4 | 2 emerald | 6 / 2400 |
| `marisa/sell_nether_fungus` | 2.2 | crimson/warped fungus ×8 | 1 emerald | 8 / 2400 |
| `marisa/sell_blaze_rod` | 2.3 | blaze rod ×3 | 2 emerald | 6 / 2400 |
| `marisa/sell_nether_wart` | 2.3 | nether wart ×8 | 1 emerald | 10 / 2400 |

Trade line: "Got some ingredients to unload, human? Let's see what ya found!"

---

## 4. Trades — Offering (player buys FROM Marisa; emeralds)

Hexbrews gated by reputation (do not change thresholds):

| id | reputation | pays | receives | maxStock / restock |
|----|-----------|------|----------|--------------------|
| `marisa/offer_mundane_hexbrew` | 0 | 3 em | mundane hexbrew ×1 | 4 / 2400 |
| `marisa/offer_miasma_hexbrew` | 50 | 3 em | miasma hexbrew ×1 | 3 / 2400 |
| `marisa/offer_shield_hexbrew` | 100 | 5 em | shield hexbrew ×1 | 3 / 3600 |
| `marisa/offer_starlight_hexbrew` | 150 | 6 em | starlight hexbrew ×1 | 2 / 3600 |
| `marisa/offer_explosive_hexbrew` | 200 | 8 em | explosive hexbrew ×1 | 2 / 4800 |
| `marisa/offer_witch_hexbrew` | 250 | 12 em | witch hexbrew ×1 | 1 / 6000 |

Trade line: "Take a look, human! I've got some good stuff today, ze."

---

## 5. Trades — Processing

Multiple-ingredient processing is **not** directly expressible with the current
`TradeOffer` (N ingredients → 1 result, single consumer). Per `polished.md`,
implementation must add a dedicated processing mode capable of multiple required
ingredients, OR use a recipe-style system. Do **not** silently drop ingredients.

- **P1 (unlocked by 2.4):** golden apple ×1 + gold block ×1 → enchanted golden apple ×1.
- **P2:** mundane hexbrew ×4 → hexbrew elixir ×1.

---

## Implementation notes

1. **`has_quest_completed` condition** is required for quest chains and must be
   registered in `CodecRegistry` (reads `QuestAttachment` completed state).
2. **"Visited Nether / fortress" gates** — use advancement-based conditions.
3. **Processing trade mode** required (see §5).
4. **Reputation tuning** — friend ~150 around 2.2–2.3; high hexbrews at 200–250.
5. **Keys/lang** — add `marisa` to `GLLang` (en_us/en_ud) + `zh_cn/marisa.json`, re-merge via `ResourceOrganizer`.

---

## File/registration sketch (`MarisaQDGen extends QuestDialogData`)
Mirror `ReimuQDGen`: `prefix("marisa/chat")` + `starter(...)` + `defaultDialog(...)`; then one `quest("marisa/<id>", new Quest(...))` per quest above; `prefix("marisa")` + `trade(...)` per trade; register the `MarisaQDGen` initializer alongside `ReimuQDGen`. Run `./gradlew build` (or `./gradlew runData`) and commit the generated JSON + lang.