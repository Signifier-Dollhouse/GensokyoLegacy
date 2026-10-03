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
| Quests | `gensokyolegacy:quest` | title, description, unlock **conditions**, **requirements**, rewards, conversation entry points |
| Trades | `gensokyolegacy:trade` | ingredients → result, stock and restock, unlock conditions |
| Dialogs | `gensokyolegacy:dialog_starter`, `gensokyolegacy:dialog` | conversation starters with their conditions, and a viewer that walks each dialog branch |

Everything is grouped per character, filterable by search, and addressable by URL
(`#quest/reimu`, `#trade/all`, `#dialog/marisa`). English and Chinese both render from
the mod's own lang files.

### Files

| Path | Purpose |
| --- | --- |
| `index.html` | page shell |
| `assets/site.css`, `assets/app.js`, `assets/lib/*.js` | styles and viewer modules |
| `rpg-manifest.json` | generated index of which registry files exist |
| `scripts/build_manifest.py` | regenerates that index from the datagen output |
| `scripts/check_site.py` | parses the modules and verifies their imports |
| `.github/workflows/rpg-index.yml` | keeps the index in step with the datapack |
| `.nojekyll` | serve the tree verbatim, without Jekyll filtering |

### Setup

In the repository settings, set **Pages → Build and deployment → Source** to
*Deploy from a branch*, branch `gh-page`, folder `/ (root)`.

### Keeping it up to date

`rpg-manifest.json` is the only generated file; the datapack JSONs are read live and
never copied. After `./gradlew runData`, regenerate the index with:

```sh
python3 scripts/build_manifest.py
```

`.github/workflows/rpg-index.yml` does this automatically whenever quest, dialog,
trade, loot table, tag or lang content changes on `main`, committing the result to
`gh-page`. GitHub only runs workflows present on the default branch, so that file has
to reach `main` before the automation starts.

Independently of the workflow, the page refreshes its index from the published branch
at runtime, so newly added content shows up even before the index is regenerated.

Run `python3 scripts/check_site.py` to parse the ES modules and confirm every import
resolves; it needs `pip install tree_sitter tree_sitter_javascript` and skips itself if
those are absent.

### A note on what gets published

Publishing the branch root also publishes the rest of the repository, including the
`libs/` jars. That is harmless for a public repository - `libs/` is already part of the
open-source tree - but it does mean the site root serves the whole mod, not just the
page.
