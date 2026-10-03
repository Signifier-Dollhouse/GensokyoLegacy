// Gensokyo Legacy content browser.
//
// The page is published from the repository root of the `gh-page` branch, so the
// RPG datapack files are fetched at their real paths under
// src/generated/resources/ - nothing is bundled, rewritten or duplicated here.

import { clear, h, setProgress, setStatus } from "./lib/dom.js";
import { characterLabel, entityLabel, itemLabel, ingredientId, label, prettify } from "./lib/format.js";
import { applyLanguage, tr, trPlural } from "./lib/i18n.js";
import { REPO, setLanguage, setTheme, state, TABS } from "./lib/state.js";
import {
  buildCharacters,
  characterKeyOf,
  fetchJson,
  loadAllDialogs,
  loadCurrencyTag,
  loadLang,
  loadManifest,
  loadRegistry,
  loadVanillaLang,
  store,
  tableFor,
  VANILLA_LANG,
} from "./lib/store.js";
import { openDialogViewer, questCard, starterCard, tradeCard } from "./lib/views.js";

// ---------------------------------------------------------------------------
// location
// ---------------------------------------------------------------------------

/**
 * The view is addressable: `#trade`, `#dialog/reimu`, `#quest/all`. Characters are
 * slugged by their registry folder (`reimu`) rather than the entity id, so the URL
 * stays readable; an unknown slug falls back to showing everything.
 */
function slugOf(key) {
  if (key === "all") return "all";
  const dirs = store.characters.get(key)?.dirs;
  return dirs?.size ? [...dirs].sort()[0] : key;
}

function keyFromSlug(slug) {
  if (!slug || slug === "all") return "all";
  const match = [...store.characters.values()].find((character) => character.dirs.has(slug));
  return match?.key ?? (store.characters.has(slug) ? slug : "all");
}

function readLocation() {
  const [tab, slug] = decodeURIComponent(location.hash.replace(/^#/, "")).split("/");
  if (TABS.includes(tab)) state.tab = tab;
  // Characters are only known once the registries are loaded.
  if (store.characters.size) state.character = keyFromSlug(slug);
}

function writeLocation() {
  const next = `#${state.tab}/${encodeURIComponent(slugOf(state.character))}`;
  if (location.hash !== next) history.replaceState(null, "", next);
}

// ---------------------------------------------------------------------------
// selection helpers
// ---------------------------------------------------------------------------

/** All entries of a registry that belong to the selected character. */
function entriesOf(registry) {
  return registry.files.map((file) => {
    const idPath = file.slice(registry.root.length + 1, -".json".length);
    return { id: `${store.manifest.namespace}:${idPath}`, dir: idPath.split("/")[0], file };
  });
}

function inCharacter(entryData) {
  if (state.character === "all") return true;
  return store.characters.get(state.character)?.dirs.has(entryData.dir) === true;
}

/** Free text match over ids, titles and item names. */
function matchesQuery(...values) {
  if (!state.query) return true;
  const haystack = values.filter(Boolean).join(" ").toLowerCase();
  return haystack.includes(state.query.toLowerCase());
}

/** The search box and the state that backs it; a fresh view clears it. */
function setQuery(value) {
  state.query = value;
  document.querySelector("#search").value = value;
}

/** Switches character. `id` focuses one entry, so a navigation passes nothing. */
function select(name, id = "") {
  state.character = name;
  setQuery(id);
  render();
}
// ---------------------------------------------------------------------------
// panels
// ---------------------------------------------------------------------------

function renderPanel() {
  const panel = clear(document.querySelector("#panel"));

  if (state.tab === "dialog") return renderDialogPanel(panel);
  if (state.tab === "trade") return renderTradePanel(panel);
  return renderQuestPanel(panel, state.tab);
}

/**
 * Quests and dailies share one registry and one card; only the split differs. A
 * quest is a daily when it declares a `recurrence`, which is also what puts a
 * cooldown on its card.
 */
function isDaily(quest) {
  return Boolean(quest?.recurrence);
}

function renderQuestPanel(panel, tab) {
  const table = store.quests;
  const entries = entriesOf(store.manifest.registries.quest)
    .filter(inCharacter)
    .filter((entryData) => isDaily(table.get(entryData.id)) === (tab === "daily"))
    .filter((entryData) => {
      const quest = table.get(entryData.id);
      return matchesQuery(entryData.id, quest?.title && label(quest.title), quest?.description && label(quest.description));
    });

  if (!entries.length) return panel.append(emptyState(tr(tab === "daily" ? "noun.dailies" : "noun.quests")));

  const onQuestLink = (id) => {
    // Land on the tab the linked quest actually lives in.
    state.tab = isDaily(store.quests.get(id)) ? "daily" : "quest";
    select("all", id);
  };

  const grid = h("div", { class: "grid" });
  for (const entryData of entries) {
    const quest = table.get(entryData.id);
    if (quest) grid.append(questCard(entryData, quest, onQuestLink));
  }
  panel.append(grid);
}

function renderTradePanel(panel) {
  const table = store.trades;
  const entries = entriesOf(store.manifest.registries.trade)
    .filter(inCharacter)
    .filter((entryData) => {
      const trade = table.get(entryData.id);
      return matchesQuery(
        entryData.id,
        entityLabel(trade?.character),
        itemLabel(trade?.result?.id),
        ...(trade?.ingredients ?? []).map((ingredient) => itemLabel(ingredientId(ingredient))),
      );
    });

  if (!entries.length) return panel.append(emptyState(tr("noun.trades")));

  const grid = h("div", { class: "grid" });
  for (const entryData of entries) {
    const trade = table.get(entryData.id);
    if (trade) grid.append(tradeCard(entryData, trade));
  }
  panel.append(grid);
}

function emptyState(what) {
  return h("p", { class: "empty", text: tr("empty.match", what) });
}

async function renderDialogPanel(panel) {
  if (!state.dialogsLoaded) {
    const counter = h("span", { text: "0 / 0" });
    const bar = h("div", { class: "progress-track" }, h("div", { class: "progress-fill" }), counter);
    panel.append(h("p", { class: "status", text: tr("status.loadingDialogs") }), bar);

    await loadAllDialogs((done, total) => {
      counter.textContent = `${done} / ${total}`;
      bar.firstChild.style.width = `${Math.round((done / total) * 100)}%`;
    });
    state.dialogsLoaded = true;
    clear(panel);
    setStatus("");
  }

  // Starters are the gated entry points into a conversation, so they lead.
  const starters = entriesOf(store.manifest.registries.dialog_starter).filter(inCharacter).filter((entryData) => {
    const starter = store.starters.get(entryData.id);
    return matchesQuery(entryData.id, starter?.text && label(starter.text));
  });

  panel.append(
    h("h3", { text: tr("starters.title") }),
    h("p", { class: "card-id", text: tr("starters.note") }),
    starters.length
      ? h(
          "div",
          { class: "grid" },
          ...starters.map((entryData) => {
            const starter = store.starters.get(entryData.id);
            return starter ? starterCard(entryData, starter) : null;
          }),
        )
      : emptyState(tr("noun.starters")),
  );

  // 200+ dialog nodes grouped by the conversation they belong to, so the tree
  // stays browsable: `reimu/daily_food/{start,follow_up,complete}/...` is one row.
  const conversations = new Map();
  for (const entryData of entriesOf(store.manifest.registries.dialog)) {
    if (!inCharacter(entryData)) continue;
    const dialog = store.dialogs.get(entryData.id);
    if (!matchesQuery(entryData.id, dialog?.text && label(dialog.text), ...(dialog?.options ?? []).map((o) => label(o.text)))) {
      continue;
    }
    const root = entryData.id.split(":")[1].split("/").slice(0, -1).join("/");
    if (!conversations.has(root)) conversations.set(root, []);
    conversations.get(root).push({ entryData, dialog });
  }

  panel.append(
    h("h3", { text: tr("dialogs.title") }),
    h("p", { class: "card-id", text: tr("dialogs.note") }),
    conversations.size
      ? h(
          "ul",
          { class: "list" },
          ...[...conversations].map(([root, nodes]) =>
            h(
              "li",
              { class: "starter-row" },
              h("button", {
                class: "linkish",
                type: "button",
                text: `${prettifyRoot(root)} ->`,
                onclick: () => openFirst(nodes),
              }),
              h("span", { class: "card-id mono", text: `${store.manifest.namespace}:${root}` }),
              h("span", {
                class: "entry-note",
                text: trPlural("dialog.lines", nodes.length, nodes.length, label(nodes[0].dialog?.text ?? "?")),
              }),
            ),
          ),
        )
      : emptyState(tr("noun.dialogs")),
  );
}

function prettifyRoot(root) {
  return prettify(root.split("/").pop());
}

/** Opens a conversation at its shallowest node. */
function openFirst(nodes) {
  const shallowest = nodes
    .map((node) => ({ ...node, depth: node.entryData.id.split("/").length }))
    .sort((a, b) => a.depth - b.depth)[0];
  openDialogViewer(shallowest.entryData.id);
}

// ---------------------------------------------------------------------------
// chrome
// ---------------------------------------------------------------------------

function characterName(character) {
  // Names are resolved per render rather than cached: that keeps them correct when
  // the language toggle changes. A character with no entity falls back to its folder.
  return characterLabel(character.entity) || prettify(character.key);
}

function orderedCharacters() {
  return [...store.characters.values()].sort((a, b) => characterName(a).localeCompare(characterName(b)));
}

function renderSidebar() {
  const list = clear(document.querySelector("#characters"));
  const counts = new Map();

  for (const name of ["quest", "trade", "dialog_starter"]) {
    for (const [id, data] of tableFor(name)) {
      if (!data) continue;
      const key = characterKeyOf(id);
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
  }

  const button = (key, name, count, title) =>
    h(
      "li",
      {},
      h(
        "button",
        {
          type: "button",
          title,
          "aria-current": String(state.character === key),
          // Picking a character is a new view, so any search text is dropped.
          onclick: () => select(key),
        },
        h("span", { text: name }),
        h("span", { class: "count", text: String(count) }),
      ),
    );

  list.append(
    button("all", tr("nav.allCharacters"), [...counts.values()].reduce((sum, value) => sum + value, 0)),
  );
  for (const character of orderedCharacters()) {
    list.append(
      button(character.key, characterName(character), counts.get(character.key) ?? 0, entityLabel(character.entity)),
    );
  }

  clear(document.querySelector("#source")).append(
    h("p", {
      text: tr("source.summary", store.manifest.stats.files, Object.keys(store.manifest.lootTables).length),
    }),
    h(
      "p",
      {},
      tr("source.fetchedFrom"),
      h("code", { text: store.manifest.resources }),
      tr("source.onBranch"),
      h("code", { text: REPO.ref }),
      tr("source.end"),
    ),
  );
}

/**
 * Tab badges count the selected character's content, not the whole registry, so
 * the numbers match what the panel below is showing.
 */
function renderTabs() {
  const counts = { quest: 0, daily: 0, trade: 0, dialog: 0 };

  for (const entryData of entriesOf(store.manifest.registries.quest).filter(inCharacter)) {
    const quest = store.quests.get(entryData.id);
    if (!quest) continue;
    if (isDaily(quest)) counts.daily += 1;
    else counts.quest += 1;
  }
  // A file that failed to load is in the index but not on screen, so it is not counted.
  for (const entryData of entriesOf(store.manifest.registries.trade).filter(inCharacter)) {
    if (store.trades.get(entryData.id)) counts.trade += 1;
  }
  counts.dialog = entriesOf(store.manifest.registries.dialog).filter(inCharacter).length;

  for (const node of document.querySelectorAll("[data-count]")) {
    node.textContent = String(counts[node.dataset.count]);
  }
  for (const node of document.querySelectorAll("[data-tab]")) {
    node.setAttribute("aria-selected", String(node.dataset.tab === state.tab));
  }
}

function render() {
  renderSidebar();
  renderTabs();
  writeLocation();
  renderPanel();
}

function applyTheme() {
  const theme = state.theme ?? (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  document.documentElement.dataset.theme = theme;
}

function wireChrome() {
  // The static markup in index.html is English until this runs.
  applyLanguage();

  const search = document.querySelector("#search");
  search.value = state.query;
  search.addEventListener("input", (event) => {
    setQuery(event.target.value.trim());
    render();
  });

  for (const button of document.querySelectorAll("[data-lang]")) {
    button.addEventListener("click", () => {
      setLanguage(button.dataset.lang);
      for (const sibling of document.querySelectorAll("[data-lang]")) {
        sibling.setAttribute("aria-pressed", String(sibling === button));
      }
      // The chrome, the character names and every datapack label are resolved per
      // render, so a language switch only has to retranslate the static markup.
      applyLanguage();
      buildCharacters();
      render();
    });
    button.setAttribute("aria-pressed", String(button.dataset.lang === state.lang));
  }

  for (const button of document.querySelectorAll("[data-tab]")) {
    button.addEventListener("click", () => {
      // Another slice of the same character: the old search rarely applies to it.
      state.tab = button.dataset.tab;
      setQuery("");
      render();
    });
  }

  document.querySelector("#theme").addEventListener("click", () => {
    const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    setTheme(next);
    applyTheme();
  });
}

// ---------------------------------------------------------------------------
// boot
// ---------------------------------------------------------------------------

async function boot() {
  applyTheme();
  wireChrome();
  readLocation(); // tab only; the character slug needs the loaded registries
  addEventListener("hashchange", () => {
    // Following a URL is a navigation too, so a stale search must not survive it.
    readLocation();
    setQuery("");
    render();
  });

  try {
    await loadManifest();
  } catch (error) {
    setStatus(tr("error.manifest", error.message), true);
    return;
  }

  const langFiles = store.manifest.lang;
  const registries = ["quest", "trade", "dialog_starter"];
  const total = registries.reduce((sum, name) => sum + store.manifest.registries[name].files.length, 0);

  // Each registry reports its own running count, so track them separately and
  // show the sum rather than adding every intermediate value.
  const seen = new Map();
  const progressFor = (name) => (count) => {
    seen.set(name, count);
    setProgress([...seen.values()].reduce((sum, value) => sum + value, 0), total);
  };

  await Promise.all([
    ...langFiles.map(loadLang),
    // The mod's lang files carry no vanilla text, so items, mobs and advancements
    // only resolve once these are in. An index predating them falls back to the
    // fixed paths rather than breaking the page.
    ...(store.manifest.vanillaLang ?? VANILLA_LANG).map(loadVanillaLang),
    // The currency tag decides whether a trade reads as "sell" or "craft".
    loadCurrencyTag(),
    ...registries.map((name) => loadRegistry(name, progressFor(name))),
  ]);

  buildCharacters();
  readLocation(); // now the character slug can be resolved
  setStatus("");
  setProgress(0, 0);
  render();

  refreshFromGitHub();
}

/**
 * Opportunistic freshness: if the published branch holds more registry files than
 * the committed index knows about, adopt the newer listing so new content appears
 * without anyone re-running the generator. Failure is expected and silent - the
 * committed index, or the derived fallback paths, keep working either way.
 */
async function refreshFromGitHub() {
  if (location.protocol === "file:") return;

  try {
    const response = await fetch(
      `https://data.jsdelivr.com/v1/packages/gh/${REPO.owner}/${REPO.name}@${REPO.ref}?structure=flat`,
      { cache: "no-cache" },
    );
    if (!response.ok) return;

    const listing = await response.json();
    const published = (listing.files ?? []).map((file) => file.name);
    if (!published.length) return;

    let added = 0;
    for (const registry of Object.values(store.manifest.registries)) {
      const prefix = `${registry.root}/`;
      const files = published.filter((file) => file.startsWith(prefix) && file.endsWith(".json"));
      if (files.length <= registry.files.length) continue;
      added += files.length - registry.files.length;
      registry.files = files;
    }
    if (!added) return;

    // Dialogs are lazy, so only the eagerly loaded registries need refilling.
    for (const name of ["quest", "trade", "dialog_starter"]) {
      await loadRegistry(name);
    }
    buildCharacters();
    render();
    setStatus(trPlural("status.refreshed", added, added));
  } catch {
    /* offline, rate limited, or the branch is not published: keep the committed index */
  }
}

boot().catch((error) => {
  // A blank page is the worst possible failure mode for a data browser, so say
  // what went wrong. This also catches the usual cause: a Pages deployment that
  // serves the site with the wrong base path, breaking every relative fetch.
  console.error(error);
  setStatus(tr("error.content", error.message), true);
});
