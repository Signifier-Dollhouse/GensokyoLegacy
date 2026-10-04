// Gensokyo Legacy content browser.
//
// The page is published from the repository root of the `gh-page` branch, so the
// RPG datapack files are fetched at their real paths under
// src/generated/resources/ - nothing is bundled, rewritten or duplicated here.

import { clear, fill, h, setProgress, setStatus } from "./lib/dom.js";
import { characterLabel, entityLabel, itemLabel, ingredientId, label, modName, prettify } from "./lib/format.js";
import { applyLanguage, tr, trPlural } from "./lib/i18n.js";
import { REPO, setLanguage, setTheme, state, TABS } from "./lib/state.js";
import {
  buildCharacters,
  buildItemIndex,
  characterKeyOf,
  fetchJson,
  loadAllDialogs,
  loadCurrencyTag,
  loadGuide,
  loadItemSources,
  loadLang,
  loadManifest,
  loadRegistry,
  loadVanillaLang,
  store,
  tableFor,
  VANILLA_LANG,
} from "./lib/store.js";
import {
  categoryName,
  collapsible,
  guideEntryName,
  guideGroupName,
  guideSortnum,
  itemRow,
  openDialogViewer,
  openItemViewer,
  questCard,
  starterCard,
  tradeCard,
  tradeKind,
  tradeTitle,
} from "./lib/views.js";

// ---------------------------------------------------------------------------
// location
// ---------------------------------------------------------------------------

/**
 * The view is addressable: `#trade`, `#dialog/reimu`, `#quest/all`, `#items/alchemy`.
 * Characters are slugged by their registry folder (`reimu`) and guide categories by
 * their own folder (`alchemy`), both rather than the full id, so the URL stays
 * readable; an unknown slug falls back to showing everything.
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

/**
 * The URL slug for an item section, and the section a slug names. A guide category is
 * named by its own folder, and a group of tag-documented items by the first tag on the
 * page that spotlights them - the two rarely collide, and if they did the untagged
 * section is found first.
 */
function categorySlug(category) {
  if (category === null) return "all";
  if (category === UNDOCUMENTED) return "undocumented";
  return String(category).split("|")[0].split(":").pop();
}

function categoryFromSlug(slug) {
  if (!slug || slug === "all") return null;
  if (slug === "undocumented") return UNDOCUMENTED;
  return itemSections().find((section) => categorySlug(section.category) === slug)?.category ?? null;
}

function readLocation() {
  const [tab, slug] = decodeURIComponent(location.hash.replace(/^#/, "")).split("/");
  if (tab === "items") {
    state.section = "item";
  } else if (TABS.includes(tab)) {
    state.section = "character";
    state.tab = tab;
  }
  if (store.characters.size) state.character = keyFromSlug(slug);
  // The item sections are only known once the guide book has loaded.
  if (state.section === "item" && store.guide) state.category = categoryFromSlug(slug);
}

function writeLocation() {
  const slug =
    state.section === "item"
      ? categorySlug(state.category)
      : encodeURIComponent(slugOf(state.character));
  const next = `#${state.section === "item" ? "items" : state.tab}/${slug}`;
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
  state.section = "character";
  setQuery(id);
  render();
}

/** Switches to the item section, showing one guide category or every item. */
function selectCategory(category) {
  state.category = category;
  state.section = "item";
  setQuery("");
  render();
}

// ---------------------------------------------------------------------------
// panels
// ---------------------------------------------------------------------------

function renderPanel() {
  const panel = clear(document.querySelector("#panel"));
  if (!panel) return;

  // The tab bar only means something for a character, so the item section hides it.
  const tabs = document.querySelector(".tabs");
  if (tabs) tabs.hidden = state.section === "item";
  if (state.section === "item") return renderItemPanel(panel);
  if (state.tab === "dialog") return renderDialogPanel(panel);
  if (state.tab === "starters") return renderStartersPanel(panel);
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

/** Follows a quest link from anywhere, landing on the tab the quest lives in. */
function onQuestLink(id) {
  state.tab = isDaily(store.quests.get(id)) ? "daily" : "quest";
  select("all", id);
}

/** Follows an offer link from the item page. */
function onTradeLink(id) {
  state.tab = "trade";
  select("all", id);
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

  const grid = h("div", { class: "grid" });
  for (const entryData of entries) {
    const quest = table.get(entryData.id);
    if (quest) grid.append(questCard(entryData, quest, onQuestLink));
  }
  panel.append(grid);
}

/** The order trades are listed in, which is also the order of the sections. */
const TRADE_KINDS = ["sell", "buy", "craft"];

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

  // Grouped by direction, the way the trade screen reads them: what the player
  // hands over, what they pay for, and what the character makes.
  const groups = new Map(TRADE_KINDS.map((kind) => [kind, []]));
  for (const entryData of entries) {
    const trade = table.get(entryData.id);
    if (trade) groups.get(tradeKind(trade)).push(tradeCard(entryData, trade));
  }
  panel.append(...TRADE_KINDS.map((kind) => collapsible(tradeTitle(kind), groups.get(kind).length, groups.get(kind))));
}

function emptyState(what) {
  return h("p", { class: "empty", text: tr("empty.match", what) });
}

/**
 * The item section. The guide book is already loaded - it is what the sidebar is built
 * from - but the recipes and the quest reward tables are a few hundred files, so they
 * are fetched the first time the section is opened.
 */
async function renderItemPanel(panel) {
  if (!state.sourcesLoaded) {
    const counter = h("span", { text: "0 / 0" });
    const bar = h("div", { class: "progress-track" }, h("div", { class: "progress-fill" }), counter);
    panel.append(h("p", { class: "status", text: tr("status.loadingItems") }), bar);

    await loadItemSources((done, total) => {
      counter.textContent = `${done} / ${total}`;
      bar.firstChild.style.width = `${Math.round((done / Math.max(1, total)) * 100)}%`;
    });
    state.sourcesLoaded = true;
    // Several hundred files take a moment; if the reader moved on meanwhile, this
    // panel is no longer the one on screen and must not be repainted over it.
    if (state.section !== "item") return;
    clear(panel);
    setStatus("");
    renderSidebar(); // the counts can move once the sources are known
  }

  const section = itemSections().find((entry) => entry.category === state.category);
  const items = (section ? section.items : allItems())
    .filter((item) => matchesQuery(item.id, itemLabel(item.id), item.guide && guideEntryName(item.guide)))
    .sort((a, b) => itemLabel(a.id).localeCompare(itemLabel(b.id)));

  if (!items.length) return panel.append(emptyState(tr("noun.items")));

  panel.append(
    h("p", { class: "card-id", text: tr("item.note") }),
    h("ul", { class: "list" }, ...items.map((item) => itemRow(item, () => openItem(item.id)))),
  );
}

/** Every item the index knows, in no particular order; the panel sorts them. */
function allItems() {
  return [...store.items.values()];
}

/**
 * Items the guide does not document are filed under this rather than under a category
 * of their own. A symbol, because it is not a category id and must never be mistaken
 * for one - nor for the "all items" selection, which is `null`.
 */
const UNDOCUMENTED = Symbol("undocumented");

/**
 * The sidebar's item sections.
 *
 * An item documented under a tag is filed under that tag rather than under its guide
 * category, so a group the book spotlights as one thing - seventeen cushions - becomes a
 * section of its own with every member in it, rather than rows scattered through the
 * decoration category. Sections the book does not tie to a tag come first, in its own
 * order, and the tag sections follow, named by the spotlight they came from. Items the
 * guide does not document are filed last among the untagged ones.
 */
function itemSections() {
  const untagged = new Map();
  const tagged = new Map();

  for (const item of store.items.values()) {
    if (item.guide?.group) {
      addSection(tagged, item.guide.group, item, guideGroupName(item.guide));
    } else {
      const category = item.guide?.category ?? UNDOCUMENTED;
      addSection(
        untagged,
        category,
        item,
        category === UNDOCUMENTED ? tr("item.undocumented") : categoryName(item.guide.book, category),
      );
    }
  }

  return [...orderedSections(untagged), ...orderedSections(tagged)];
}

function addSection(sections, key, item, name) {
  const section = sections.get(key) ?? { category: key, name, sortnum: Infinity, items: [] };
  section.name = name;
  if (typeof item.guide?.category === "string") {
    section.sortnum = guideSortnum(item.guide.book, item.guide.category);
  }
  section.items.push(item);
  sections.set(key, section);
}

/** The book's own order, by the sort number of the category each section came from. */
function orderedSections(sections) {
  return [...sections.values()].sort((a, b) => a.sortnum - b.sortnum || a.name.localeCompare(b.name));
}

/** Opens one item's page, wiring the jumps back to the quest and trade panels. */
function openItem(id) {
  openItemViewer(id, { quest: onQuestLink, trade: onTradeLink });
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
    // Several hundred files take a moment; if the reader moved on meanwhile, this
    // panel is no longer the one on screen and must not be repainted over it.
    if (state.tab !== "dialog") return;
    clear(panel);
    setStatus("");
  }

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

/** Starters are the gated entry points into a conversation, so they get their own
 *  tab: they are few, they are what a player meets first, and loading them does
 *  not pull in the 200+ dialog nodes the next tab lists. */
function renderStartersPanel(panel) {
  const starters = entriesOf(store.manifest.registries.dialog_starter)
    .filter(inCharacter)
    .filter((entryData) => {
      const starter = store.starters.get(entryData.id);
      return matchesQuery(entryData.id, starter?.text && label(starter.text));
    });

  panel.append(
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

/**
 * The sidebar: one list of characters and, beside it, one list of item sections -
 * the guide's categories and everything it does not document. Picking a character
 * shows their content behind the tab bar; picking an item section shows the item
 * list without it, since neither belongs to a character.
 */
function renderSidebar() {
  fill(clear(document.querySelector("#characters")), ...characterEntries());
  fill(clear(document.querySelector("#items")), ...itemEntries());

  fill(
    clear(document.querySelector("#source")),
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

/** One sidebar button: a name, a count, and what selecting it means. */
function navEntry(current, name, count, title, onClick) {
  return h(
    "li",
    {},
    h(
      "button",
      {
        type: "button",
        title,
        "aria-current": String(current),
        // Picking a section is a new view, so any search text is dropped.
        onclick: onClick,
      },
      h("span", { text: name }),
      h("span", { class: "count", text: String(count) }),
    ),
  );
}

/**
 * How much content each character has: the registries that name a character, since
 * dialogs are attributed to one by the folder they sit in.
 */
function characterCounts() {
  const counts = new Map();
  for (const name of ["quest", "trade", "dialog_starter"]) {
    for (const [id, data] of tableFor(name)) {
      if (!data) continue;
      const key = characterKeyOf(id);
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
  }
  return counts;
}

function characterEntries() {
  const counts = characterCounts();
  const here = (key) => state.section === "character" && state.character === key;

  return [
    navEntry(
      here("all"),
      tr("nav.allCharacters"),
      [...counts.values()].reduce((sum, value) => sum + value, 0),
      null,
      () => select("all"),
    ),
    ...orderedCharacters().map((character) =>
      navEntry(
        here(character.key),
        characterName(character),
        counts.get(character.key) ?? 0,
        entityLabel(character.entity),
        () => select(character.key),
      ),
    ),
  ];
}

function itemEntries() {
  return [
    navEntry(state.section === "item" && state.category === null, tr("item.all"), allItems().length, null, () =>
      selectCategory(null),
    ),
    ...itemSections().map(itemGroup),
  ];
}

/**
 * One collapsible group of the item sidebar: a heading that folds the group away, and
 * every item it holds listed underneath as its own entry. Groups start open, since the
 * sidebar is the way in and a closed group is a group you cannot see. Clicking the
 * heading also selects the section, which is what the panel below then lists - and
 * since that redraws the sidebar, whether a group is folded is remembered in `state`
 * rather than left to the element.
 */
function itemGroup(section) {
  const key = section.category;
  const title = key === UNDOCUMENTED ? tr("item.undocumented") : String(key);
  const current = state.section === "item" && state.category === key;
  return h(
    "li",
    {},
    h(
      "details",
      {
        class: "nav-group",
        open: !state.collapsed.has(key),
        ontoggle: (event) => {
          if (event.target.open) state.collapsed.delete(key);
          else state.collapsed.add(key);
        },
      },
      h(
        "summary",
        { title, "aria-current": String(current), onclick: () => selectCategory(key) },
        h("span", { text: section.name }),
        h("span", { class: "count", text: String(section.items.length) }),
      ),
      h(
        "ul",
        { class: "nav-list nav-items" },
        ...section.items.map((item) =>
          h(
            "li",
            {},
            h(
              "button",
              { type: "button", title: item.id, onclick: () => openItem(item.id) },
              h("span", { text: itemLabel(item.id) }),
            ),
          ),
        ),
      ),
    ),
  );
}

/**
 * Tab badges count the selected character's content, not the whole registry, so
 * the numbers match what the panel below is showing. Items are in the sidebar, not
 * here, and are counted there.
 */
function renderTabs() {
  const counts = { quest: 0, daily: 0, trade: 0, starters: 0, dialog: 0 };

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
  for (const entryData of entriesOf(store.manifest.registries.dialog_starter).filter(inCharacter)) {
    if (store.starters.get(entryData.id)) counts.starters += 1;
  }
  counts.dialog = entriesOf(store.manifest.registries.dialog).filter(inCharacter).length;

  for (const node of document.querySelectorAll("[data-count]")) {
    node.textContent = String(counts[node.dataset.count]);
  }
  for (const node of document.querySelectorAll("[data-tab]")) {
    node.setAttribute("aria-selected", String(state.section === "character" && node.dataset.tab === state.tab));
  }
}

/**
 * The chrome that names the mod. `data-i18n-lang` holds a key from the *mod's* lang
 * files rather than the interface tables, so the heading and the document title follow
 * the language toggle the way every other piece of content does. `<title>` is built
 * here rather than by `applyLanguage`, since the mod name comes first.
 */
function renderChrome() {
  for (const node of document.querySelectorAll("[data-i18n-lang]")) {
    node.textContent = label(node.dataset.i18nLang);
  }
  document.title = `${modName()} · ${tr("page.tagline")}`;
}

/**
 * Redraws the page one part at a time. The parts are independent - the sidebar, the tab
 * badges, the chrome, the URL, the panel - and each is run on its own so that one of
 * them throwing costs that part alone rather than the whole render. A browser can end
 * up running a newer module against a cached `index.html`, and a blank panel is a much
 * worse outcome than a missing sidebar list.
 */
function render() {
  for (const step of [renderSidebar, renderTabs, renderChrome, writeLocation, renderPanel]) {
    try {
      Promise.resolve(step()).catch(reportFailure);
    } catch (error) {
      reportFailure(error);
    }
  }
}

/**
 * A blank page is the worst possible failure mode for a data browser, so whatever went
 * wrong is said out loud. On boot this also catches the usual cause: a Pages deployment
 * that serves the site with the wrong base path, breaking every relative fetch.
 */
function reportFailure(error) {
  console.error(error);
  setStatus(tr("error.content", error.message), true);
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
      state.section = "character";
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
  const guideFiles = (store.manifest.guides ?? []).flatMap((book) =>
    Object.values(book.locales).reduce((sum, locale) => sum + locale.categories.length + locale.entries.length, 0),
  );
  const total =
    registries.reduce((sum, name) => sum + store.manifest.registries[name].files.length, 0) + guideFiles;

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
    // The guide book is what the item sidebar is built from, so it is small enough
    // to fetch with everything else rather than behind a click.
    loadGuide(progressFor("guide")),
    ...registries.map((name) => loadRegistry(name, progressFor(name))),
  ]);

  buildCharacters();
  await buildItemIndex();
  readLocation(); // now the character slug and the item category can be resolved
  setStatus("");
  setProgress(0, 0);
  render();

  refreshFromGitHub();
}

/**
 * Opportunistic freshness: if the published branch holds more files than the committed
 * index knows about, adopt the newer listing so new content appears without anyone
 * re-running the generator. Failure is expected and silent - the committed index, or
 * the derived fallback paths, keep working either way.
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

    /** The published files under `prefix`, when there are more than the index lists. */
    const adopt = (files, prefix) => {
      const found = published.filter((file) => file.startsWith(prefix) && file.endsWith(".json"));
      if (found.length <= files.length) return 0;
      files.length = 0;
      files.push(...found);
      return found.length;
    };

    let added = 0;
    for (const registry of Object.values(store.manifest.registries)) {
      added += adopt(registry.files, `${registry.root}/`);
    }
    if (store.manifest.recipes) added += adopt(store.manifest.recipes.files, `${store.manifest.recipes.root}/`);
    for (const book of store.manifest.guides ?? []) {
      for (const [locale, files] of Object.entries(book.locales)) {
        for (const kind of ["categories", "entries"]) {
          added += adopt(files[kind], `${book.pages}/${locale}/${kind}/`);
        }
      }
    }
    if (!added) return;

    // Dialogs are lazy, so only the eagerly loaded registries need refilling.
    for (const name of ["quest", "trade", "dialog_starter"]) {
      await loadRegistry(name);
    }
    // The item index is built from the listings just adopted, so it is rebuilt too -
    // and if the reader is already looking at items, the sources are refetched.
    await buildItemIndex();
    if (state.section === "item") {
      state.sourcesLoaded = false;
      await loadItemSources();
    }
    buildCharacters();
    render();
    setStatus(trPlural("status.refreshed", added, added));
  } catch {
    /* offline, rate limited, or the branch is not published: keep the committed index */
  }
}

boot().catch(reportFailure);
