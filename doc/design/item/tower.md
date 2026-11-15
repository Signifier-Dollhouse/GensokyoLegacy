# Treasure Tower (`treasure_tower`) — Turning Stone into Ore

A held item that leaves the hand as an entity: it flies to a spot four blocks ahead and two blocks up,
beams the aimed stone, and flies back. The beam replaces that stone and everything connected to it
with the ore of a gem the player is carrying. One gem is spent per block, the item is not. The
recipe is not written down: it is derived from the datapack as **ore → gem** (smelting) and
**ore → stone** (ore features), and the two halves are joined on the ore block.

Cost: a spiritual power pool on the item (`SpiritualItem`, 1000 capacity) spends 100 per cast,
plus one gem per block converted and a 10-tick cooldown. The gems cap one cast at the size of a
single stack, which is what keeps one stack of diamonds from paving a mine.

---

## 1. Rules

**Gem.** The smelt result of the ore must be in `#c:gems` (`Tags.Items.GEMS`). Nothing else is
consulted — not the loot table, not a local tag. `c:gems` is the common tag every gem-producing mod
already populates, and the smelting recipe is the statement of what an ore is worth; also requiring
the loot table to agree would only reject modded ores that drop a raw variant.

**Stone.** The stone is whatever the ore's **configured feature** says it replaces: the rule test of
the `OreConfiguration` target that produces that ore. A `tag_match` rule carries a block tag, a
`block_match` rule carries a single block. Nether ores use `block_match` on netherrack, which is why
the rule is read rather than assumed to be a tag.

**No curated stone.** The stone set is exactly what ore features replace. A stone no feature claims
is not a target, and there is no fallback list: end stone converts to nothing until a mod adds an
end-stone ore feature — an `endstone diamond ore` — at which point the rule test names it and the
tower accepts it with no code change. Likewise a gem whose ore has no `OreConfiguration` target is not
a conversion target at all.

**The pair.** A stone converts to an ore when the stone is in the ore's ingredient and the player
holds that ore's gem. The stone decides the variant (deepslate vs stone), the gem decides the
material.

---

## 2. Vanilla outcome

Gems, as `#c:gems` stands in 1.21.1: `amethyst_shard`, `diamond`, `emerald`, `lapis_lazuli`,
`prismarine_crystals`, `quartz`. Four of them have an ore. Coal and redstone are **not** in
`c:gems` (they are `c:dusts` material), so they do not work out of the box; a pack that tags them in
gets them immediately, since coal ore and redstone ore both smelt to them.

| Ore | Smelts to | In `c:gems` | Pair |
|---|---|---|---|
| diamond / deepslate_diamond | diamond | yes | yes |
| emerald / deepslate_emerald | emerald | yes | yes |
| lapis / deepslate_lapis | lapis_lazuli | yes | yes |
| nether_quartz | quartz | yes | yes |
| coal / deepslate_coal | coal | no | only if a pack tags coal as a gem |
| redstone / deepslate_redstone | redstone | no | only if a pack tags redstone as a gem |
| iron / deepslate_iron | iron_ingot | no (ingot) | no |
| gold / deepslate_gold | gold_ingot | no (ingot) | no |
| copper / deepslate_copper | copper_ingot | no (ingot) | no |
| nether_gold | gold_ingot | no (ingot) | no |
| ancient_debris | netherite_scrap | no (scrap) | no |

The metal ores are excluded by the gem test alone: their smelt result is an ingot, not a gem. No
curated list of metals is needed.

Resulting recipe, 7 entries:

| Gem | Ore | Ingredient |
|---|---|---|
| diamond | `diamond_ore` | `#minecraft:stone_ore_replaceables` |
| diamond | `deepslate_diamond_ore` | `#minecraft:deepslate_ore_replaceables` |
| emerald | `emerald_ore` | `#minecraft:stone_ore_replaceables` |
| emerald | `deepslate_emerald_ore` | `#minecraft:deepslate_ore_replaceables` |
| lapis_lazuli | `lapis_ore` | `#minecraft:stone_ore_replaceables` |
| lapis_lazuli | `deepslate_lapis_ore` | `#minecraft:deepslate_ore_replaceables` |
| quartz | `nether_quartz_ore` | `minecraft:netherrack` |

Vanilla's entries do not overlap, so all **19 conversions** are live: 6 for each of diamond, emerald
and lapis (four `stone_ore_replaceables` blocks plus deepslate and tuff) and 1 for quartz. End stone is
not among the stones. JEI has **7 pages**, one per entry above.

---

## 3. Building the mapping

The build is a straight four-step pass over registries the server already has. No loot table is
read, and nothing is cached between reloads beyond the result.

### 3.1 ores

```java
for (Holder<Block> ore : BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.ORES))
```

19 blocks in vanilla; a pack adds its own.

### 3.2 ore → gem

One pass over `RecipeType.SMELTING` (~70 recipes): for every item the recipe's `Ingredient`
accepts, map the item to `getResultItem(registries)`, first recipe wins. Keep an ore only if that
result is in `Tags.Items.GEMS`.

### 3.3 ore → rule test

Walk `Registries.CONFIGURED_FEATURE` (196 entries, 30 `OreConfiguration`, 47 targets). A target
belongs to an ore when its result state's block is an ore from §3.1 that survived §3.2. Store the
target's `RuleTest`, deduplicated per ore by signature — the tag id for a `tag_match`, the block id
for a `block_match`, the rule's own string form otherwise. Several features (small/medium/large)
that share a rule collapse into one entry.

At this point the recipe is `(gem, ore, rule test)`; 7 entries in vanilla.

### 3.4 rule test → ingredient (server, tags stay tags)

The rule test becomes the ingredient the recipe actually uses, and **a tag is never expanded**:

- `tag_match` → the tag id itself, kept as a tag. The ingredient is `#minecraft:stone_ore_replaceables`,
  not the four blocks it currently holds;
- `block_match` → that block;
- `block_state_match` → the state's block;
- anything else (random tests, custom modded tests) → brute force: every block in
  `BuiltInRegistries.BLOCK` is tested with `rule.test(state, RandomSource.create(0))` and the
  matches kept as blocks. ~1100 cheap tests, only for rules that are not one of the three shapes
  above.

Keeping the tag means the ingredient does not freeze at build time: a pack that adds granite to
`stone_ore_replaceables` changes what the tower accepts without touching the recipe, and the packet
carries one id instead of a long block list.

Every entry is `(gem, ore, one rule)`; an ore that no feature places simply never gets one. Mapping
building is complete:

```java
record OreRecipe(Item gem, Block ore, ArrayList<ResourceLocation> tags, ArrayList<Block> stones)
```

`tags` are block tag ids (a `ResourceLocation` rides the wire as-is; a `TagKey` has no l2serial
codec), `stones` are the loose blocks from `block_match` and the brute-force arm. Both lists are
declared `ArrayList` because l2serial decodes a field by instantiating its declared type, so a `List`
field would ask the interface for a constructor. The record is the in-memory recipe, the packet
payload and the JEI page, all in one.

---

## 4. Sync and reload order

Loot and worldgen registries do not exist client-side, so the server does all of §3 and sends the
finished recipe list. `TreasureTowerToClient` carries `ArrayList<OreRecipe>`: the gem and ore as
registry objects, the tag ids, and the loose blocks. No rule test crosses the wire, no codec
handler is needed, and no list is expanded at either end.

- `TagsUpdatedEvent` (`SERVER_DATA_LOAD`) → `TreasureTowerManager.clear()`.
- `OnDatapackSyncEvent` → build with `event.getPlayerList().getServer()`, then send to
  `event.getRelevantPlayers()`. This fires after recipes and tags are reloaded and before vanilla
  sends its own tag/recipe sync, so a client that opens JEI has the entries already.
- `TreasureTowerClient` keeps the list and builds its per-gem index from it.

`/reload` reloads recipes and tags but **not** worldgen configured features. The recipe rebuilds
from whatever worldgen the running world has, which is correct: a datapack that adds an ore feature
needs a world restart anyway.

---

## 5. The item

`TreasureTower extends SpiritualItem`, `MAX_POWER = 1000`, `COST = 100`, `COOLDOWN = 10`,
`REACH = 24`. `SpiritualItem` already draws the pool as the durability bar, appends the
`Spiritual Power: n / m` line, and `SpiritualEffect` refills it at 10/s per level. A jizo shrine
also fills it, trading experience levels — the same loop the anchor and the fan use.

`use` resolves the shot server-side and sends the tower out; the item itself does no conversion:

1. Gate on the pool: below `COST` nothing happens but the action-bar message.
2. Ray-trace the aimed block with `player.pick(REACH)`. A miss, or a stone no gem accepts, does
   nothing. The result is locked in now, so looking away during the flight does not move the target.
3. Pick the gem: the offhand stack first, then the main hand, then the inventory in slot order.
   Only stacks whose item maps to an ore **for this stone** qualify, so carrying a diamond and an
   emerald still does the right thing on deepslate. That one stack is also the budget: its count caps
   the conversion, and stacks of the same gem are never summed.
4. Copy the stack into a `TreasureTowerEntity`, spawn it at the eye, and clear the hand; the entity
   is the tower until it returns. Start the cooldown.

The entity has three phases:

- **OUT** — fly from the eye to `player.position() + horizontalLook * 4 + (0, 2, 0)` at 0.45 blocks
  per tick. The look vector is flattened so looking up or down does not make the tower climb or dive.
- **CAST** — on arrival, BFS from the locked target through its six neighbours, keeping every block
  the chosen gem converts, and stop at the budget. The nearest N are converted, each to the ore its
  own recipe names, so one run of stone and deepslate becomes both ore variants. Spend `COST` power
  on the carried stack and N gems (gems skipped when `hasInfiniteMaterials`), chime, beam, `swing`
  and `awardStat`. The beam then keeps firing for 10 ticks from the tower's position to the target.
- **RETURN** — fly back to the hand at 0.6 blocks per tick, then put the stack into the slot it left;
  if that slot is taken, add it to the inventory, and drop it only if the inventory is full.

The carried stack is synched as the entity's item, so the client renders the real tower (power and
all) while it is in flight. If the owner dies, leaves the dimension or gets more than 32 blocks away,
the entity drops the tower where it is and vanishes. A saved entity keeps its phase, target, hand
and slot, so a chunk unload cannot lose the tower.

The beam is `HAPPY_VILLAGER`, four particles per block of distance with ±0.1 spread per axis. The
renderer draws the item spinning slowly with a small bob; there is no GeckoLib rig. The item model is
the ordinary `generated` one on `textures/item/tool/treasure_tower.png`.

---

## 6. JEI

`TreasureTowerCategory` draws one page per `OreRecipe`, in a single row:

- the gem on the left, the stones in the middle, the ore on the right with an arrow before it;
- the middle slot is **one ingredient holding every stone**, expanded from the tag on the client
  for display only, so JEI cycles them in place instead of tiling a grid;
- a tag that no client block answers to still names itself in the tooltip, and a loose block with
  no item form keeps its place in the mapping while being skipped in the slot.

Pages are keyed by `(gem, ore)` and every slot is a real item id, so JEI's item focus narrows the
list for free: looking up granite shows the three overworld pages, looking up diamond shows its two.
`getRegistryName` returns `gem/ore`, so a page keeps its identity across reloads.

`registerRecipes` returns early while the cache is empty, and JEI re-runs it after `/reload`, so the
page list never goes stale. The category title is `gensokyolegacy.jei.treasure_tower`.

---

## 7. Files

Add:

- `content/item/treasure/TreasureTower.java` — the item, target lock and launch, tooltip.
- `content/entity/misc/TreasureTowerEntity.java` — the three phases, the cast and the return.
- `content/entity/misc/render/TreasureTowerRenderer.java` — the spinning, bobbing item.
- `content/item/treasure/OreRecipe.java` — the recipe (gem, ore, tags, blocks), plus `matches(BlockState)`.
- `content/item/treasure/TreasureTowerData.java` — the recipe list and per-gem index, `@see "doc/design/item/tower.md"`.
- `content/item/treasure/TreasureTowerManager.java` — §3 end to end, plus `TagsUpdatedEvent` clearing.
- `content/item/treasure/TreasureTowerClient.java` — client cache.
- `content/item/treasure/network/TreasureTowerToClient.java` — the sync packet.
- `compat/jei/TreasureTowerCategory.java` — the category.
- `assets/gensokyolegacy/textures/item/tool/treasure_tower.png` — art.

Modify:

- `init/registrate/GLItems.java` — `TREASURE_TOWER = reg.item(...)` in the tab, tagged
  `SPIRITUAL_TOOLS`, `.lang("Treasure Tower")`.
- `init/registrate/GLEntities.java` — `TREASURE_TOWER` entity, its renderer and tracking interval.
- `init/GLPackets.java` and `GensokyoLegacy.HANDLER` — new packet, protocol version 4 → 5.
- `event/GLMetaEventHandlers.java` — send on `OnDatapackSyncEvent` beside the gift/loot packets.
- `init/data/GLLang.java` — item tooltip, JEI title and notes.
- `content/command/GLCommands.java` — the `treasure_tower` subcommand.
- `compat/jei/GLJEIPlugin.java` — recipe type, category, catalyst, recipes.
- `src/test/resources/gensokyolegacy/lang/zh_cn/*.json` — hand-written translations, then
  `organize.ResourceOrganizer`.

Generated, then committed: `src/generated/resources/assets/gensokyolegacy/models/item/treasure_tower.json`.

---

## 8. Load cost

| Step | Work |
|---|---|
| ores | one tag lookup, 19 blocks |
| ore → gem | 70 smelting recipes, ingredient expansion |
| ore → rule | 196 features, 47 targets, 7 entries |
| rule → ingredient | 2 tag ids kept as-is, 1 block, brute force only for exotic rules |
| maps | 7 recipes, one index per gem; a cast scans only that gem's entries |

Measured over the pre-parsed vanilla data in a Python prototype: **well under 0.1 ms** per build,
with no loot table touched at all. The Java version adds class loading on the first call (10–30 ms,
once). This is cheaper than the quest-reward loot read that already runs on the same datapack sync.

Because tags are not expanded, the cost no longer depends on how large a pack's stone tag is; only
the brute-force arm for exotic rules walks the block registry, and only once per such rule.

---

## 9. Diagnostics

`/gensokyo treasure_tower stats` — entry count, tag and loose-block counts, last build duration.

`/gensokyo treasure_tower dump [json]` — every entry with its tags and blocks, as chat lines or
as `out/treasure_tower.json`, with the tag's current members expanded in the dump only. Run it after
touching a tag or adding a mod: a missing gem means the smelt result is not in `c:gems`, a missing
ore means no smelting recipe names it, and a missing stone means no ore feature's rule test reaches
it.

---

## 10. Decisions

- **Reusable, spiritual power, 10-tick cooldown.** No durability: the pool already is the wear, and
  it refills from a renewable source. A full pool is ten casts; a cast costs 100 whether it converts
  one block or a whole stack.
- **The tower is the entity.** Leaving the hand is what makes the beam come from the tower rather
  than the player; the item is cleared on launch and handed back on return, so there is never a
  second copy. A lost entity drops the stack instead of deleting it.
- **One stack pays.** The gem budget is the count of the selected stack, never the sum of the pack:
  a cast is a bounded, predictable purchase, and a second stack is a second cast.
- **Smelt result only.** No loot table test; the smelting recipe is the authority on what an ore is
  worth, and the gem tag filters out the metals for free.
- **`#c:gems` only.** No local gem tag: coal and redstone stay out until a pack says otherwise, and
  a modded gem works the moment it is tagged.
- **Tags stay tags.** The ingredient keeps the tag id from the rule test all the way to the cast,
  so a tag edit is live without a rebuild and the packet stays small; only `block_match` and exotic
  rules contribute loose blocks.
- **Feature or nothing.** A stone is accepted only because an `OreConfiguration` says the ore
  replaces it; there is no curated stone list, so end stone waits for a mod that adds an end-stone
  ore feature, and an ore no feature places is never a target.
- **Expansion is a display concern.** JEI resolves tags to blocks on the client, for the page only;
  the item never expands one.
- **No crafting recipe yet.** The item ships registered and obtainable from creative; the recipe is
  a balance decision to make once the conversion feels right.
- **No version bump.** `mod_version` is left to the release that ships the feature.
