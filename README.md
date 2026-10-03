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

| Tab | Registry | Highlights |
| --- | --- | --- |
| Quests | `gensokyolegacy:quest` without `recurrence` | title, description, unlock **conditions**, **requirements**, rewards, conversation entry points |
| Dailies | `gensokyolegacy:quest` with `recurrence` | the same card, for the quests that come back on a cooldown |
| Trades | `gensokyolegacy:trade` | ingredients → result, stock and restock, unlock conditions |
| Dialogs | `gensokyolegacy:dialog_starter`, `gensokyolegacy:dialog` | conversation starters with their conditions, and a viewer that walks each dialog branch |

Quests and dailies share one registry and one card; the tab only splits them, so a
condition linking to a quest lands on whichever of the two it actually belongs to.

Everything is grouped per character, filterable by search, and addressable by URL
(`#quest/reimu`, `#daily/reimu`, `#trade/all`, `#dialog/marisa`).

### Languages

The EN/中文 toggle covers two separate things, both re-resolved on every render so
a switch takes effect immediately:

| What | Where it comes from |
| --- | --- |
| Content: quest titles, dialog lines, item, block and entity names | the mod's own lang files (`assets/gensokyolegacy/lang/{en_us,zh_cn}.json`), resolved by `assets/lib/format.js` |
| Interface: headings, buttons, section labels, units, error messages, `aria-label`s | the tables in `assets/lib/i18n.js` |
| Character names | `character.<folder>` in `assets/lib/i18n.js` — a short name (灵梦) keyed by the folder the character's content sits in, with the mod's full entity name (博丽灵梦) as the tooltip and the fallback |

Interface strings use positional `{0}` placeholders, so a translation may reorder the
sentence. A key missing from `zh_cn` falls back to English rather than to the key.

### Files

| Path | Purpose |
| --- | --- |
| `index.html` | page shell |
| `assets/site.css`, `assets/app.js`, `assets/lib/*.js` | styles and viewer modules |
| `assets/lib/i18n.js` | interface string tables for both locales |
| `rpg-manifest.json` | generated index of which registry files exist |
| `scripts/build_manifest.py` | regenerates that index from the datagen output |
| `scripts/check_site.py` | parses the modules, verifies their imports and checks both locale tables |
| `.github/workflows/rpg-index.yml` | keeps the index in step with the datapack |
| `.nojekyll` | serve the tree verbatim, without Jekyll filtering |

### Setup

In the repository settings, set **Pages → Build and deployment → Source** to
*Deploy from a branch*, branch `gh-page`, folder `/ (root)`.

### Keeping it up to date

`rpg-manifest.json` is the only generated file; the datapack JSONs are read live and
never copied. Two things have to stay in step for new content to appear:

1. **`gh-page` must contain the datapack JSONs**, because that is the branch Pages
   serves. New datagen output lands on your working branch, not on `gh-page`.
2. **`rpg-manifest.json` must list the new files.** Regenerate it after
   `./gradlew runData` with:

   ```sh
   python3 scripts/build_manifest.py
   ```

`.github/workflows/rpg-index.yml` does both automatically: it mirrors the RPG datapack
and lang files from the source branch onto `gh-page`, then regenerates the index.
GitHub only runs workflows present on the default branch, so that one YAML file has to
be copied to `main` before the automation starts. Set the `RPG_SOURCE_BRANCH`
repository variable if the site should track a branch other than `main`.

Independently of the workflow, the page refreshes its index from the published branch
at runtime, so newly added files are noticed even before the index is regenerated.
Note that this only helps for files already present on `gh-page` - it cannot fetch a
datapack file that only exists on another branch.

Run `python3 scripts/check_site.py` to parse the ES modules, confirm every import
resolves, and confirm that every interface string exists in both locales with
matching `{0}` slots - including the ones built at runtime, such as the character
names; it needs `pip install tree_sitter tree_sitter_javascript` and skips itself if
those are absent.

### A note on what gets published

Publishing the branch root also publishes the rest of the repository, including the
`libs/` jars. That is harmless for a public repository - `libs/` is already part of the
open-source tree - but it does mean the site root serves the whole mod, not just the
page.
