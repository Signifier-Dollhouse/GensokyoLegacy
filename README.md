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
`#items/undocumented`. Each of the two is a heading that folds its list away, so a reader
working through one does not have to scroll past the other; both start open.

The item entries are the guide's own categories, in its order, plus one for the items it
never mentions. Selecting one lays its items out in the panel: the items the guide names
on their own, listed straight in, then a group for every spotlight that names more than
one thing - *Take a Seat* for the seventeen cushions, *Noren Curtains* for the
sixty-four, *Somebody's Bed* for the three beds, *Cartons, Crates and Books* for the six.
The unit is the spotlight page, not the tag: three beds named together under one title
are as much a group as four noren tags, and the groups run fewest items first, so the three
beds come before the sixty-four noren - counted as the search box leaves them, so a group it
empties leaves the order with it, and a tie falls back to the book's own order. The groups start
folded, since the panel can hold seventy of them and the point of one is to open it; an
opened group stays open across a redraw or a language switch, since the panel is rebuilt
on every keystroke of the search box and a `<details>` would otherwise forget.

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
| The item list | the generated `en_us`/`en_ud` lang file, which enumerates every `item.`/`block.` the mod registers, plus the few ids from other namespaces the mod hands out (`patchouli:guide_book`). Only the generated tables are read: the hand-authored `zh_cn.json` lags behind on purpose, and a key left for a block that has since been removed would otherwise put it back into the list with no name to show for it. A vanilla item the mod hands out is left out too - it is not something the mod adds, and it is still named wherever it is traded for |
| The guide | `src/main/resources/assets/gensokyolegacy/patchouli_books/<book>/<locale>/` - the in-game book's own categories and entries, with its `$(bold)`, `$(br)` and `$(br2)` macros rendered, and its spotlights followed into the item tags they name |
| Ways to obtain | `recipe/**`, including the mod's alchemy and brewing types; `trade/**`, for the offers that hand out something other than currency; the `loot_table` each quest reward names; and the `morichika_offers` item tag, which is what Rinnosuke stocks his shelves from |
| The sidebar entries | the guide's own categories, in the book's order, plus one entry for everything it does not document |
| The panel groups | within an entry, the items the guide names on their own are listed straight in, then one collapsible group per spotlight that names several things - a tag or a list of ids - named by that page's own translated title, ordered by how many items each holds, fewest first |

Four files are listed for the viewer rather than found by following the content, because
nothing in the RPG registries refers to them: the `currency` tag, which decides whether a
trade reads as *sell to the character* or *request a craft*; the `morichika_offers` tag,
which only `MorichikaEntity` reads; the `morichika_offer` data map beside it, which
holds the price and stock range each item is shelved at; and the `character_config` data
map, whose `structure` field is what tells a `visit_structure` condition whose house it
is, so the row reads *Visiting · Reimu* rather than a structure id. An item on the shop's
tag but not in the data map is shelved at stock 1 and price 1, the default the code
documents.

An item page is its guide entry - the category it sits in, the advancement that grants
it, and the entry's prose under a heading that folds away - followed by every way to get
one, one collapsible section per kind of source, with jumps back to the quest or the
offer it came from.

A guide page is about one item *or a whole tag of them*: Patchouli writes a tag
reference as `tag:namespace:path`, and a spotlight may name several at once under one
title - tags or plain ids or a mixture. Every member of a tag is documented by the same entry - seventeen cushions all
point at *Seats & Seat Cloths* - and an item's page says which group it was documented
under, with a link to the tag's members. Pages that belong to a *different* item stay
apart: within one entry, a cushion sees the prose about cushions and a large chair sees
the prose about large chairs, never each other's. Only a text page that opens an entry,
naming no item, is shared by all of it.

The item list is the slow half of the page - the guide book, the tags it spotlights, the
recipes and the quest reward tables, some 380 files - and no character content needs any
of it. So it is fetched *behind* the first render rather than in front of it: the
characters are readable while it arrives, and the sidebar's items heading says
`Loading items... 140 / 385` until it lands. Opening the item section before then waits
for the same load rather than starting a second one, and the count is written into the
line in place so the sidebar does not have to be rebuilt on every file.

### Caching

The page fetches several hundred small JSON files, so what a returning visitor pays for
depends entirely on how they are asked for. Asking plainly, the browser revalidates every
one of them on every visit - hundreds of round trips before anything can be drawn. So
every data file is asked for at its own path *plus a fingerprint*, and the browser is
told to use what it has rather than ask again:

```
src/generated/resources/data/gensokyolegacy/recipe/oak_shelf_from_dark_oak_log_stonecutting.json?v=6dbdffd2…
```

The fingerprint is an md5 of every datapack file the index points at, written into the
index as `revision` by `scripts/build_manifest.py`. It changes when - and only when - the
content does: new content merged onto this branch moves it, so the next visit fetches
afresh instead of trusting a stale copy, and one update costs one refresh rather than
every visit costing one. Only bytes are hashed, never timestamps, so a re-run that changes
nothing leaves the number alone.

Two files are deliberately left unversioned and always revalidated:

| File | Why |
| --- | --- |
| `rpg-manifest.json` | it carries the fingerprint everything else is versioned by; a stale copy would pin the page to the revision it was built with and no new content would ever be asked for again |
| `assets/lang/vanilla/*.json` | committed with the page rather than generated from the datapack, so a hand edit to one should show up without an index rebuild |

An index predating the field is not a problem: the page fetches plain paths and works
exactly as it did before.

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
| `rpg-manifest.json` | generated index of which registry, recipe and guide files exist, plus their content fingerprint |
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
