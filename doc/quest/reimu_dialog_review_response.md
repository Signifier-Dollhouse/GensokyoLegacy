# Reimu Quest Dialog — Review Response

## Overall assessment

The draft is structurally sound and follows the intended progression from newcomer chores → practical research → tactical counterstrike. The strongest material is the 2.x–3.x escalation and the recurring callbacks to ofuda, the banner, and the mark.

The main issues are:
- Reimu is occasionally too earnest, explanatory, or modern-sounding for the requested casual, dry voice.
- A few lines reveal information too early or introduce details not supported by the objective sheet.
- Some objective wording is inaccurate: the 3.1 follow-up says captains “wear” banners, while the canonical detail is that the captain drops the banner.
- The 3.2 explanation should make the plan feel more deliberate and should avoid treating the mark as literally being the raiders' “summons.”
- Several lines are longer than the preferred spoken length.
- The daily hunt line “before they organize” is not necessary and slightly overstates the objective.

No balance, counts, rewards, or unlock order are changed below.

## Greeting

**Verdict:** minor edits

**Tone check:** The greeting is friendly and serviceable, but “What brings you to the shrine?” is slightly formal compared with Reimu's plain, casual voice. The fixed “Hi!” is fine.

**Consistency check:** No callback issue.

**Facts check:** No factual issue.

**Format check:** The greeting remains a separate starter and does not add a quest topic. Keep the trade header unchanged if it is fixed elsewhere.

**Suggested lines:**
- Menu header greeting: “Hi! What brings you to the shrine?”
- Trade header: “I'd like to trade with you!”

No change is strictly required here; the greeting can remain as-is.

## 7.1 `local_food` — Shrine Provisions

**Verdict:** rewrite

**Tone check:** The premise works well, but “Oh, a human visitor!” is unnecessarily theatrical, “shrine dust” is an odd gag that does not transfer especially well, and “teach me the local cuisine” sounds more academic than Reimu needs.

**Consistency check:** The newcomer premise is good. The donation-box callback is useful and fits her poverty. Keep the curiosity about unfamiliar food.

**Facts check:** Correctly asks for bread ×8 and mushroom stew ×3 and establishes that she does not yet know the local cuisine.

**Format check:** The start intro is too long, and the reject branch is too long. The rest is mostly within the intended shape.

**Suggested lines:**
- Start button: “Talk about food.”
- Reimu intro: “I'm still figuring out what's safe to eat here. Bring me some bread and mushroom stew so I can learn the local food.”
- Reject: “Foraging in an unfamiliar world wasn't in the shrine maiden handbook. But I suppose I'll manage.”
- Accept: “Great. Bread and mushroom stew. Nothing fancy, just enough to keep this shrine maiden fed.”
- Follow-up button: “Ask about the food.”
- Follow-up: “How's the food hunt going?”
- On-it: “I keep watching the villagers eat and feeling left out.”
- Complete button: “Hand over the food.”
- Complete: “Welcome back. Please tell me you brought food, not another donation.”
- Reject: “No rush. I'll just keep dreaming about mushroom stew.”
- Handover: “Ahh, real food. Thanks. You're the first person I can count on here.”

## 7.2 `hostile_loot` — Hostile Analysis

**Verdict:** minor edits

**Tone check:** This is broadly in character: calm about danger, direct, and curious about the local monsters. “They swarm at night” is unnecessary, and “what they really are” makes the research sound more academic than intended.

**Consistency check:** The walking-dead/skeleton comparison correctly supports the arc's research theme. The requested flesh and bones remain the only samples.

**Facts check:** Correctly asks for 10 zombies and 10 skeletons plus 8 rotten flesh and 8 bones. No balance changes needed.

**Format check:** Mostly good. The completion line unnecessarily repeats the kill counts, which makes the spoken line feel like a system confirmation.

**Suggested lines:**
- Start button: “Offer to hunt for samples.”
- Reimu intro: “The walking dead and clattering skeletons keep ambushing me. They're not exactly youkai, but I want to study them. Bring me some flesh and bones.”
- Reject: “It is dangerous. But you've survived here longer than I have. You'll manage when you're ready.”
- Accept: “Good. Ten of each, plus rotten flesh and bones. Bring me enough samples to study.”
- Follow-up button: “Ask about the hunt.”
- Follow-up: “How's the hunt going?”
- On-it: “Got those samples yet? Flesh and bones should tell me plenty.”
- Complete button: “Hand over the samples.”
- Complete: “You're back. Please tell me you didn't become one of them.”
- Reject: “No rush. Just don't let them nibble on you.”
- Handover: “Perfect. Now I can compare these against the youkai I know. Thanks for the fieldwork.”

## 7.3 `talisman_materials` — Talisman Materials

**Verdict:** minor edits

**Tone check:** The discovery of redstone is a good fit for Reimu's growing confidence. “Trust the miko on this one” is a little too performative; the voice is stronger when she is simply matter-of-fact.

**Consistency check:** Good callback to paper and redstone as the new-world materials for ofuda. The reward callback to making some for the player is appropriate.

**Facts check:** Correctly asks for paper ×16 and redstone ×8. The redstone-as-vermilion-ink idea is consistent with the requested lore. Avoid saying “world's first ofuda” unless that fact is actually established; the source only requires that she reinvent the craft here.

**Format check:** Good overall. The start intro and completion lines can be tightened.

**Suggested lines:**
- Start button: “Talk about the talismans.”
- Reimu intro: “Paper here folds nicely, and this redstone dust glows with power. I can use it as vermilion ink for new-world ofuda. Bring me some.”
- Reject: “It's red, it glows, and it's full of power. That's good enough for vermilion ink.”
- Accept: “Good. Paper for folding, redstone for ink. That should keep the shrine stocked.”
- Follow-up button: “Ask about the ofuda.”
- Follow-up: “Got the materials yet?”
- On-it: “I want to see how well the new ofuda work.”
- Complete button: “Hand over the materials.”
- Complete: “You brought them. Let me see.”
- Reject: “Okay, okay. The ofuda can wait a little longer.”
- Handover: “Excellent. I can make the new ofuda now. I'll make a few for you, too.”

## 7.4 `ominous_banner` — Raider's Banner

**Verdict:** rewrite

**Tone check:** The investigative setup is good, but the opening spends too many words on exposition. “Read it like a sutra” is flavorful, but the surrounding language should be more direct.

**Consistency check:** This is the first explicit mark warning, so the warning should be clear without explaining the later weaponization. The final line should establish Reimu's intention without prematurely explaining 3.2.

**Facts check:** The banner comes from a raid captain. Killing the captain leaves the killer with the ominous mark. The draft's “captains wear them right on their heads” is incorrect and should be removed.

**Format check:** The follow-up contains a factual problem and the opening is too long. Otherwise the node structure is correct.

**Suggested lines:**
- Start button: “Talk about the raiders.”
- Reimu intro: “People keep whispering about *raiders*—armed groups with black-and-white banners. Bring me one from their captain. I want to know what it means.”
- Reject: “They're still just humans with weapons. Come back when you're ready to deal with them.”
- Accept: “Good. Their captains carry those ominous banners. Take one down and bring it to me. Just watch for the mark they leave behind.”
- Follow-up button: “Ask about the banner.”
- Follow-up: “Found their captain yet?”
- On-it: “Check outposts and patrols. That's where you're most likely to find one.”
- Complete button: “Hand over the banner.”
- Complete: “That's the one. Let me see what this strange banner is saying.”
- Reject: “Take your time. I'll be here.”
- Handover: “There it is. I'll study what binds these raiders together. Then we'll see what I can do about them.”

## 7.5 `raid` — Repel the Raid

**Verdict:** rewrite

**Tone check:** This should be the payoff of the entire arc. The current draft is energetic, but the explanation is too long and makes the mark sound like a literal summons. Reimu should sound like she has worked out a practical trick and is confident it will work.

**Consistency check:** The mark callback is correct in concept: 3.1 warns about it, while 3.2 explains that Reimu can recreate it with her talismans and use it to bait raiders. The spellcard reward callback is also correct.

**Facts check:** The player receives the recreated mark, enters a village, and repels the raid. Keep the village and mark relationship explicit. Do not imply Reimu herself is conducting the raid.

**Format check:** The start intro is well over the preferred spoken length. The completion line is good in spirit but can be more decisive. The accept line correctly represents the mark application and should retain the “village” objective.

**Suggested lines:**
- Start button: “Talk about the plan.”
- Reimu intro: “I figured out the mark. My ofuda can recreate it. I'll put it on you; enter a village, draw the raiders in, and drive them out.”
- Reject: “I've got shrine duties. You're the one who knows this world, so I'll leave the fighting to you.”
- Accept: “There. The mark is on you. Walk into a village, draw them out, and drive every raider away.”
- Follow-up button: “Ask about the raid.”
- Follow-up: “Is the village still standing?”
- On-it: “They'll keep coming in waves. Hold the line.”
- Complete button: “Tell Reimu the raid is over.”
- Complete: “The waves stopped? Good. So the plan worked.”
- Reject: “Not yet? Then stay sharp. They won't give up easily.”
- Handover: “You routed them. I knew baiting them into the village would work. Here—take my *Innate Dream* spellcard.”

## 7.6 Dailies

### `daily_food`

**Verdict:** minor edits

**Tone check:** The empty-donation-box gag is appropriate, but “food and materials alike” sounds formal.

**Consistency check:** Good as a recurring restock chore and appropriately lighter than the one-time 1.1 quest.

**Facts check:** The objective allows a random mix of wool, leather, iron, gold, bread, and apple. The generic wording is therefore appropriate.

**Format check:** The fixed buttons are correctly preserved. The unique lines are short enough.

**Suggested lines:**
- Intro: “The donation box is empty again, and I'm hungry. Bring me a few things for the shrine.”
- Accept: “Good. Bring back a decent haul.”
- Reject: “A hungry miko is a distracted miko. Your call.”
- Follow: “How's the haul coming along?”
- On-it: “Take your time. Just bring back something useful.”
- Complete: “Just what the shrine needed. Thanks.”

### `daily_talisman`

**Verdict:** keep with minor polish

**Tone check:** Strong match for Reimu's practical, recurring-restock voice.

**Consistency check:** Excellent callback to 2.2 and the established paper/redstone → ofuda process.

**Facts check:** Correctly reprises paper ×16 and redstone ×8.

**Format check:** Fixed shared buttons are respected; lines are concise.

**Suggested lines:**
- Intro: “Talisman stock is running low again. Paper and redstone, just like before.”
- Accept: “Great. Restock me and I'll keep the shrine warded.”
- Reject: “No stock, no warding. Your call.”
- Follow: “Any paper and redstone yet?”
- On-it: “The ofuda shelves are starting to look empty.”
- Complete: “Good. The shelves are stocked again.”

### `daily_hunt`

**Verdict:** minor edits

**Tone check:** The casual “thrashing” and practical concern for the player work well. “Rush them before they organize” is more tactical than this generic daily needs to be.

**Consistency check:** It correctly echoes the 2.1 hunting arc without duplicating its research premise.

**Facts check:** The objective is generic hostile thinning plus loot, so the line should not imply a specific monster type or additional condition.

**Format check:** Fixed buttons are correct; all unique lines are concise.

**Suggested lines:**
- Intro: “Something's been stirring up trouble near the shrine. Thin out the monsters and bring me some loot.”
- Accept: “That's the spirit. Go clear them out.”
- Reject: “They'll only grow bolder. But it's your call.”
- Follow: “How's the thrashing going?”
- On-it: “Stay careful out there. I need you in one piece.”
- Complete: “Another night's sleep saved. Thanks.”

### `daily_raid`

**Verdict:** minor edits

**Tone check:** The confident repeatable-task voice fits. “Up for one more raid?” is fine, though Reimu can sound slightly more direct.

**Consistency check:** Strong callback to 3.2 and the recreated mark.

**Facts check:** Correctly involves the mark and a village raid. No extra objective is introduced.

**Format check:** Fixed buttons and shared catch line must remain unchanged; the unique lines fit the required lengths.

**Suggested lines:**
- Intro: “A new mark is ready, and another village could use protecting. Ready for another raid?”
- Accept: “Here's the mark, same as before. Enter the village, hold the line, and drive them out.”
- Reject: “The mark will keep. The raiders will still be there when you're ready.”
- Follow: “Is the village holding up?”
- On-it: “They keep coming in waves. Don't drop your guard.”
- Complete: “The village is still standing. You're getting good at this.”

## Final recommendation

Use the revised lines above, with the biggest priority on 3.1 and 3.2. Those quests carry the narrative payoff, so factual precision around the ominous mark and the raid plan matters most. The 1.1 rewrite is the next priority because its current prose is substantially longer and more sentimental than Reimu's target voice.

The existing quest structure, objective counts, unlock order, and reward progression can remain unchanged.
