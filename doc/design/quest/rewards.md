# Quest rewards in JEI

The quest reward page (`compat/jei/QuestRewardCategory`) draws the character, the quest name and
one line stating how the reward list is handed out, then the flat item list itself. That line is
one of four rules (`RewardRule`):

| rule | line | source |
|---|---|---|
| `ALL` | All of the following rewards | loot-table pools of one entry each, or a `GiveItemAction` bundle on one option |
| `RANDOM` | Any of the following rewards (loot table randomization) | a table with one pool of several entries |
| `CHOICE` | Any of the following rewards of your choice | sibling completion-dialog options that each grant one item |
| `MIXED` | Complex drop with mixed rule | anything else: several rules, mixed pools, conditional alternatives |

## Reading the rule

Loot tables are server data and are never synced, so `LootReader` reads every table on the server
and decides the table's rule from its pools:

- several pools, one entry each → `ALL`;
- one pool, several entries → `RANDOM`;
- anything else, such as a multi-entry pool next to another pool → `MIXED`.

The quest's tables combine through `RewardRule.combine`: one source keeps its rule, several sources
are `ALL` only if every one is, otherwise `MIXED`. The sampled drops and the resulting rule travel
together in `QuestLootToClient`, keyed by quest.

The completion dialog is fully synced with the quest, so JEI reads the chosen reward from the quest
itself, one slot per `GiveItemAction` and no extra marker in the action. Its options are the
alternatives:

- several options that each grant one item → `CHOICE`;
- one granting option, or several grants on one option → `ALL`;
- several grants on an option next to other granting options, or conditional/random alternatives →
  `MIXED`.

Loot groups and dialog groups then combine through the same `RewardRule.combine`, which is why the
rule belongs to the quest rather than to a drop.

## Layout

The grid is 3x2; a longer authored reward says `+n more` in the page tooltip. A loot-table count
the table only bounds is drawn beside the stack, while a `GiveItemAction` count is baked into the
stack. A choice slot's tooltip carries the option's own line, so the player sees which pick grants
it.
