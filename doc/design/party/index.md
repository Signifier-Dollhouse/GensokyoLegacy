# Party Event — Design Doc

> **Status:** implemented — compiles, `runData` and `./gradlew build` both green. Not yet played in a live world, so the flow (`send.md`, `guest.md`) and `limits.md` §1 are reasoned from the code rather than observed.
> **Package:** `content/attachment/party/` — `core/` (math, planner, state, handler lookup), `food/` (one handler class per kind of block food), `item/` (letter, wand), plus `content/entity/behavior/module/PartyModule.java` and `content/entity/behavior/task/party/PartyTasks.java`
>
> Party state is attachment-shaped, so the whole feature lives under `content/attachment/` alongside the other per-feature packages. That puts `PartyArea`'s neighbour `ChunkPosRange` in `content/attachment/area/` next to it rather than across the tree.
>
> **Related:** `character_visit.md` (the guest-clone trick reused here), `reputation.md` (the reputation this reads)

## What a party is

A player lays block food out on tables, sets chairs, writes a letter, and sends it. Characters with enough reputation come, eat, and leave gifts on the table.

**Only a character with a `party_taste` entry can come.** There is no separate whitelist: the data map is the eligibility list, so a character nobody has written down never attends (`select.md` §2). Among those who are eligible, what decides who shows up is how good the spread is and how well the player knows them.

```
lay out block food on party tables  +  put out seats
        ↓
right-click a table with the invitation letter  →  send
        ↓
letter consumed; spread appeal measured; candidates scored (`select.md`)
        ↓
a weighted draw picks guests who fit the hunger budget and the seat count
        ↓
guests spawn as temporary clones, walk in, sit, eat, gift, leave
```

There is no monetary cost and no cooldown. The letter names no specific character — it goes out to everyone who qualifies, and who that is depends entirely on the spread and on the player's standing with each of them. The only thing the player spends is food, and the only thing they get back is gifts.

## The four pieces

| Piece | Lives in | Job |
|---|---|---|
| **Attractiveness / fulfillment math** | `content/attachment/party/core/PartyMath.java` | pure functions, no world writes |
| **Roster selection** | `content/attachment/party/core/PartyPlanner.java` | weighted draw under the hunger budget and seat count |
| **Party state** | `GLMeta.PARTY`, a `LevelChunk` capability | host, anchor, roster, phase, counters |
| **Guest behaviour** | `PartyModule` + `PartyTasks` | walk, sit, eat, gift, dismiss |

The first two are deliberately side-effect free so `/gensokyo party preview` can run the identical code path as a real send and be guaranteed to predict it.

## Document tree

| Doc | Covers | Status |
|---|---|---|
| [area.md](area.md) | the tags a party is laid out on, the walk that finds the spread, the centre that decides which chunk owns it, and the one-party-per-area guard | implemented |
| [food.md](food.md) | what a dish is worth: the appeal derivation, `BlockFoodHandler` / `FoodResult` / `ContainerFoodHandler`, the state cache, id permutation, and the override-only `party_food` data map | implemented |
| [food_mods.md](food_mods.md) | the per-mod handlers — counting conventions, vanilla cake, Farmer's Delight, Youkai's Feasts, Kaleidoscope Cookery — and the `canSurvive` support measurements | implemented |
| [state.md](state.md) | `PartyAttachment`, the chunk capability, the frozen roster, the ticker, and the phase machine | implemented |
| [select.md](select.md) | spread appeal, `party_taste`, `PartyMath.weight`, fulfillment, the seeded weighted draw, and the worked example | implemented |
| [send.md](send.md) | the invitation letter, `PartySend.send` and its rejections, guest clones, gifts and the reputation payout | implemented |
| [guest.md](guest.md) | `PartyTasks` and the three phases — arriving, the bite, leaving — plus the brain wiring | implemented |
| [debug.md](debug.md) | `/gensokyo party`, the debug wand, the area renderer, and the one-reading-at-a-time rule | implemented |
| [checklist.md](checklist.md) | every file created and modified, and the generated resources | — |
| [limits.md](limits.md) | known limits, and the owner answers that settled them | — |

Suggested reading order: **area → food → food_mods** (what a party is made of), then **state → select → send** (how it is run), then **guest** (what one guest does), then **debug** and **limits** (how to tune it and what it cannot do).
