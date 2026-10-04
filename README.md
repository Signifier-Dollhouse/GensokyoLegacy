# Gensokyo Legacy

A NeoForge 1.21.1 mod adding Touhou characters, structures and an RPG quest, dialog
and trade system to Minecraft. See `AGENTS.md` for the development guide.

## Content website

The `gh-page` branch also publishes a browsable index of the RPG content at
<https://signifier-dollhouse.github.io/GensokyoLegacy/>.

It is a static page with no build step: it fetches the datapack JSONs **directly from
`src/generated/resources/` in this repository**, so the site always shows the content
that actually ships in the mod.

**Why the page is served from the branch root.** GitHub Pages publishes exactly one
folder per deployment. When the source is the branch root, that folder is the whole
repository, so `src/generated/resources/...` is reachable over HTTP. Selecting `docs/`
instead would work too, but then `src/` sits outside the published folder and every
fetch 404s - Pages cannot serve files from outside the directory it publishes.

### What it shows

| Tab | Read from | Highlights |
| --- | --- | --- |
| Quests | `gensokyolegacy:quest` without `recurrence` | title, description, unlock **conditions**, **requirements**, rewards, conversation entry points |
| Dailies | `gensokyolegacy:quest` with `recurrence` | the same card, for the quests that come back on a cooldown |
| Trades | `gensokyolegacy:trade` | ingredients → result, stock and restock, unlock conditions, grouped into collapsible *sell to*, *buy from* and *processing* sections |
| Starters | `gensokyolegacy:dialog_starter` | the gated entry points into a conversation, with their conditions |
| Dialogs | `gensokyolegacy:dialog` | every dialog node, grouped by the conversation it belongs to, with a viewer that walks each branch |

The sidebar carries two lists side by side, and the tabs belong to the first: **characters**
(quests, dailies, trades, starters and dialogs) and **items** (the guide, and where each
item comes from). Items belong to no character, so they are a section of their own rather
than a sixth tab; picking one hides the tab bar, and picking a character brings it back.
Both are addressable: `#quest/reimu`, `#trade/all`, `#dialog/marisa`, `#items/decoration`,
`#items/undocumented`.

The item entries are the guide's own categories, in its order, plus one for the items it
never mentions. Selecting one lays its items out in the panel: what the guide names one
by one at the top, then a collapsible group for every page that spotlights a tag -
*Take a Seat* for the seventeen cushions, *Noren Curtains* for the sixty-four. Being named
by a tag is the whole difference, and the groups follow the book's own order. Groups start
open, and one the reader folds stays folded across a redraw or a language switch, since
the panel is rebuilt on every keystroke of the search box and a `<details>` would
otherwise forget it had been closed.

Quests and dailies share one registry and one card; the tab only splits them, so a
condition linking to a quest lands on whichever of the two it actually belongs to.
Starters and dialogs are separate registries, and keeping them apart also means the
Starters tab does not have to pull in the 200+ dialog nodes.

Each trade card is titled with the item the trade is about: the ingredient going in
for a sale, the result coming out otherwise. The trade list is split into collapsible
*sell to* / *buy from* / *processing* sections, and on the quest, trade and starter cards
the conditions, requirements and rewards fold away too - all of them expanded to begin
with, so a card reads top to bottom unless you close one. Conditions inside a dialog
option stay inline, since an option is already a small block.

The tab badges count the selected character; the item list beside them counts the items
in each section. Picking a character, a tab or an item section clears the search box.

### The item section

The item list is a guide to what the mod adds and where to find it. Nothing about it is
written down in the site: every list, every source and all of the prose is read from the
repository at the paths it already lives at, so merging new content onto `gh-page` is
all it takes for the page to show it.

| What | Where it is read from |
| --- | --- |
| The item list | the mod's own lang files, which enumerate every `item.`/`block.` it registers, plus the few ids from other namespaces the mod hands out (`patchouli:guide_book`, converted planks) |
| The guide | `src/main/resources/assets/gensokyolegacy/patchouli_books/<book>/<locale>/` - the in-game book's own categories and entries, with its `$(bold)`, `$(br)` and `$(br2)` macros rendered, and its spotlights followed into the item tags they name |
| Ways to obtain | `recipe/**`, including the mod's alchemy and brewing types; `trade/**`, for the offers that hand out something other than currency, which is how Rinnosuke's shop appears; and the `loot_table` each quest reward names |
| The sidebar entries | the guide's own categories, in the book's order, plus one entry for everything it does not document |
| The panel groups | within an entry, the items the guide names one by one, then one collapsible group per page that spotlights a tag, named by that page's own translated title |

An item page is its guide entry - the category it sits in, the advancement that grants
it, and the entry's prose under a heading that folds away - followed by every way to get
one, one collapsible section per kind of source, with jumps back to the quest or the
offer it came from.

A guide page is about one item *or a whole tag of them*: Patchouli writes a tag
reference as `tag:namespace:path`, and a spotlight may name several at once under one
title. Every member of a tag is documented by the same entry - seventeen cushions all
point at *Seats & Seat Cloths* - and an item's page says which group it was documented
under, with a link to the tag's members. Pages that belong to a *different* item stay
apart: within one entry, a cushion sees the prose about cushions and a large chair sees
the prose about large chairs, never each other's. Only a text page that opens an entry,
naming no item, is shared by all of it.

The guide book is fetched with everything else, since it is small and it is what the
sidebar's sections and counts are built from - along with the item tags it spotlights,
which are resolved into their members. The recipes and the quest reward tables are a
few hundred files, so they are fetched the first time the item section is opened - the
same trade-off the dialog tab makes. That is also when the handful of items from other
namespaces turn up, so the counts can move by two or three at that point.

A shaped recipe is drawn as the crafting grid with a key beneath it, since the layout is
part of the recipe; the alchemy and brewing recipes show their fluid and their extra
ingredients, since a hexbrew is brewed rather than crafted.

An alchemy or brewing recipe produces a *fluid*, and the game fills a `<fluid>_bottle`
item from it, so those recipes are recorded against the bottle - the thing the player
actually carries - rather than against the fluid, which is not an item.

### Languages

The EN/中文 toggle covers three separate things, all re-resolved on every render so
a switch takes effect immediately:

| What | Where it comes from |
| --- | --- |
| Mod content: quest titles, dialog lines, mod items, blocks and entities | the mod's own lang files (`assets/gensokyolegacy/lang/{en_us,zh_cn}.json`), resolved by `assets/lib/format.js` |
| The mod's own name, `东方幻想绮谈` | `gensokyolegacy.title` in the mod's lang files, so the heading and the document title follow the toggle like any other content |
| Vanilla content: `minecraft:` items, blocks, mobs, advancements and effects | `assets/lang/vanilla/{en_us,zh_cn}.json`, generated from the game data by `scripts/build_vanilla_lang.py` |
| Interface: headings, buttons, section labels, units, error messages, `aria-label`s | the tables in `assets/lib/i18n.js` |
| Character names | `character.<folder>` in `assets/lib/i18n.js` — a short name (灵梦) keyed by the folder the character's content sits in, with the mod's full entity name (博丽灵梦) as the tooltip and the fallback |

Interface strings use positional `{0}` placeholders, so a translation may reorder the
sentence. A key missing from `zh_cn` falls back to English rather than to the key.
Anything no lang file names - a third party item, a tag, a deleted id - falls back to a
prettified id, so nothing is ever blank.

### The vanilla table

The mod's lang files carry no vanilla text, so `minecraft:iron_ingot` and
`minecraft:nether/find_fortress` would otherwise render as prettified English on the
Chinese page. `scripts/build_vanilla_lang.py` pulls those strings out of the game data
- `minecraft_1.21.1_client.jar` from NeoForm's cache for `en_us`, the downloaded asset
objects for `zh_cn` - and keeps only the keys the content actually refers to, which is
currently 81 keys rather than the game's 3000. Recipes count as content: their
ingredients are vanilla more often than not, which is most of what the item section
added. The output is committed, because the published site has no game installation to
pull from.

```sh
python3 scripts/build_vanilla_lang.py           # regenerate, after runData or a Minecraft bump
python3 scripts/build_vanilla_lang.py --check   # verify only, no game data needed
```

`--check` is what CI runs: it fails when the content names an id that no lang file can
name, when a locale is missing a key the other has, or when the file holds a key the
content no longer references. It reports, without failing, ids that nothing upstream
translates (currently `patchouli:guide_book`) and tags, which Minecraft does not
translate at all.

### Files

| Path | Purpose |
| --- | --- |
| `index.html` | page shell |
| `assets/site.css`, `assets/app.js`, `assets/lib/*.js` | styles and viewer modules |
| `assets/lib/i18n.js` | interface string tables for both locales |
| `assets/lang/vanilla/*.json` | the vanilla strings the content refers to |
| `assets/img/hakkero.png` | the logo and favicon: a copy of the mod's Mini Hakkero texture, checked against it by `scripts/check_site.py` |
| `rpg-manifest.json` | generated index of which registry, recipe and guide files exist |
| `scripts/build_manifest.py` | regenerates that index from the datagen output |
| `scripts/build_vanilla_lang.py` | extracts the referenced vanilla strings from the game |
| `scripts/check_site.py` | parses the modules, verifies their imports, checks both locale tables and the logo |
| `.github/workflows/rpg-index.yml` | keeps the index in step with the datapack |
| `.nojekyll` | serve the tree verbatim, without Jekyll filtering |

### Setup

In the repository settings, set **Pages → Build and deployment → Source** to
*Deploy from a branch*, branch `gh-page`, folder `/ (root)`.

### Keeping it up to date

`rpg-manifest.json` is the only file CI regenerates; the datapack JSONs, the guide book
and the lang files are read live and never copied, and the vanilla table is committed
because only a Minecraft installation can produce it. Four things have to stay in step
for new content to appear:

1. **`gh-page` must contain the datapack JSONs and the guide book**, because that is the
   branch Pages serves. New datagen output - and new `patchouli_books` entries, which are
   hand-authored under `src/main/resources` rather than generated - land on your working
   branch, not on `gh-page`.
2. **`rpg-manifest.json` must list the new files.** Regenerate it after
   `./gradlew runData` with:

   ```sh
   python3 scripts/build_manifest.py
   ```

3. **`assets/lang/vanilla/*.json` must cover any new vanilla id.** If a recipe, quest or
   trade starts naming a `minecraft:` item, block, mob, advancement, effect or fluid that
   the table does not yet have, regenerate it (see above); CI fails until you do. The
   mod's own ids need nothing, since they live in the mod's lang files.

`.github/workflows/rpg-index.yml` does the first two automatically: it mirrors the RPG
datapack, the guide book and the lang files from the source branch onto `gh-page`, then
regenerates the index. GitHub only runs workflows present on the default branch, so that
one YAML file has to be copied to `main` before the automation starts. Set the
`RPG_SOURCE_BRANCH` repository variable if the site should track a branch other than
`main`.

Independently of the workflow, the page refreshes its index from the published branch
at runtime - quests, trades, starters, dialogs, recipes and guide entries alike - so
newly added files are noticed even before the index is regenerated. Note that this only
helps for files already present on `gh-page`: it cannot fetch a datapack file that only
exists on another branch.

Run `python3 scripts/check_site.py` to parse the ES modules, confirm every import
resolves, confirm that every interface string exists in both locales with matching `{0}`
slots - including the ones built at runtime, such as the character names - and confirm
that `assets/img/hakkero.png` still matches the mod's texture; it needs `pip install
tree_sitter tree_sitter_javascript` and skips the module checks if those are absent.

### A note on what gets published

Publishing the branch root also publishes the rest of the repository, including the
`libs/` jars. That is harmless for a public repository - `libs/` is already part of the
open-source tree - but it does mean the site root serves the whole mod, not just the
page.
