// Gensokyo Legacy content browser.
//
// The page is published from the repository root of the `gh-page` branch, so the
// RPG datapack files are fetched at their real paths under
// src/generated/resources/ - nothing is bundled, rewritten or duplicated here.

import { append, clear, fill, h, setProgress, setStatus } from "./lib/dom.js";
import { characterLabel, entityLabel, itemLabel, ingredientId, label, modName, prettify } from "./lib/format.js";
import { applyLanguage, tr, trPlural } from "./lib/i18n.js";
import { REPO, setLanguage, setTheme, state, TABS } from "./lib/state.js";
import {
  buildCharacters,
  buildItemIndex,
  characterKeyOf,
  fetchJson,
  guideCategoryIds,
  guideEntryIds,
  guideEntryOf,
  guideFileCount,
  loadAllDialogs,
  loadCurrencyTag,
  loadGuide,
  loadItemSources,
  loadLang,
  loadManifest,
  loadRegistry,
  loadShopOffers,
  loadStructureHosts,
  loadVanillaLang,
  sourceFileCount,
  store,
  tableFor,
  VANILLA_LANG,
} from "./lib/store.js";
import {
  categoryName,
  collapsible,
  guideEntryName,
  guideEntryRow,
  guideGroupName,
  guideSortnum,
  itemRow,
  openDialogViewer,
  openGuideEntryViewer,
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
 * The view is addressable: `#trade`, `#dialog/reimu`, `#quest/all`, `#items/alchemy`,
 * `#patchouli/dolls`. Characters are slugged by their registry folder (`reimu`) and
 * guide categories by their own folder (`alchemy`), rather than by the full id, so the
 * URL stays readable; an unknown slug falls back to showing everything.
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

/**
 * The URL slug for a guide category: its own folder, the same spelling the item list's
 * categories use, since they are the same folders. Null - the whole book, which the panel
 * answers with every entry - is "all"; the bucket for entries that name no category has no
 * folder to be named by, so it spells itself out rather than borrowing "all" and
 * colliding with it.
 */
function guideCategorySlug(category) {
  if (category === null) return "all";
  if (category === UNCATEGORISED) return "uncategorised";
  return String(category).split(":").pop();
}

/** The category a slug names, or null - which lands on every entry in the book. */
function guideCategoryFromSlug(slug) {
  if (!slug || slug === "all") return null;
  return guideSections().find((section) => guideCategorySlug(section.category) === slug)?.category ?? null;
}

function readLocation() {
  const [tab, slug] = hashParts();
  if (tab === "items") {
    state.section = "item";
  } else if (tab === "patchouli") {
    state.section = "guide";
  } else if (TABS.includes(tab)) {
    state.section = "character";
    state.tab = tab;
  }
  if (store.characters.size) state.character = keyFromSlug(slug);
  readLazySelection();
}

/**
 * The item category or the guide category the hash names, once the book is in. Separate
 * from `readLocation` because the guide book is fetched behind the first render, so a
 * link straight to `#items/decoration` or `#patchouli/dolls` can only be answered once
 * it arrives - and answering it must not disturb the character the reader was on.
 */
function readLazySelection() {
  if (!store.guide) return;
  const slug = hashParts()[1];
  if (state.section === "item") state.category = categoryFromSlug(slug);
  if (state.section === "guide") state.guideCategory = guideCategoryFromSlug(slug);
}

/** Only the first `/` separates the section from its slug; an id may hold more. */
function hashParts() {
  const [section, ...rest] = decodeURIComponent(location.hash.replace(/^#/, "")).split("/");
  return [section, rest.join("/")];
}

function writeLocation() {
  // Neither the item category nor the guide category can be resolved until the book is
  // in, and the hash is then the only record of what was linked to, so it is left as it
  // is rather than overwritten with "all".
  if (state.section !== "character" && !state.itemsLoaded) return;

  const slug =
    state.section === "item"
      ? categorySlug(state.category)
      : state.section === "guide"
        ? guideCategorySlug(state.guideCategory)
        : encodeURIComponent(slugOf(state.character));
  // Each section is named after the sidebar list it belongs to; a character is named
  // after its tab rather than after the character.
  const section = { character: state.tab, item: "items", guide: "patchouli" }[state.section];
  const next = `#${section}/${slug}`;
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

/** Switches to the guide book, showing one of its categories or every entry. */
function selectGuideCategory(category) {
  state.guideCategory = category;
  state.section = "guide";
  setQuery("");
  render();
}

// ---------------------------------------------------------------------------
// panels
// ---------------------------------------------------------------------------

function renderPanel() {
  const panel = clear(document.querySelector("#panel"));
  if (!panel) return;

  // The tab bar only means something for a character, so the sections that belong to no
  // character hide it: the items and the book that documents them.
  const tabs = document.querySelector(".tabs");
  if (tabs) tabs.hidden = state.section !== "character";
  if (state.section === "guide") return renderGuidePanel(panel);
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
  // hands over, what they pay for, and what the character makes. `append` rather than
  // the DOM's own, which would turn the empty groups into the text "null".
  const groups = new Map(TRADE_KINDS.map((kind) => [kind, []]));
  for (const entryData of entries) {
    const trade = table.get(entryData.id);
    if (trade) groups.get(tradeKind(trade)).push(tradeCard(entryData, trade));
  }
  append(panel, TRADE_KINDS.map((kind) => collapsible(tradeTitle(kind), groups.get(kind).length, groups.get(kind))));
}

function emptyState(what) {
  return h("p", { class: "empty", text: tr("empty.match", what) });
}

/**
 * The item section and the guide book section both wait on the same late load - the
 * book, the tags it names, the recipes and the reward tables. A reader who opens one of
 * them before it has landed joins that load rather than starting a second.
 *
 * False means the reader moved on while it was going, so this panel is no longer the one
 * on screen and must not be painted over it.
 */
async function itemsReady(panel, section) {
  if (state.itemsLoaded) return true;

  const counter = h("span", { text: "0 / 0" });
  const bar = h("div", { class: "progress-track" }, h("div", { class: "progress-fill" }), counter);
  panel.append(h("p", { class: "status", text: tr("status.loadingItems") }), bar);

  await loadItems((done, total) => {
    counter.textContent = `${done} / ${total}`;
    bar.firstChild.style.width = `${Math.round((done / Math.max(1, total)) * 100)}%`;
  });
  if (state.section !== section) return false;

  clear(panel);
  setStatus("");
  return true;
}

/**
 * The item section. Everything it is made of - the guide book, the tags it names, the
 * recipes and the reward tables - is fetched behind the first render, so this waits for
 * that load if the reader gets here before it has landed.
 */
async function renderItemPanel(panel) {
  if (!(await itemsReady(panel, "item"))) return;

  const section = itemSections().find((entry) => entry.category === state.category);
  const row = (item) => itemRow(item, () => openItem(item.id));

  // "All items" and the undocumented bucket stay a flat list: there is no category to
  // group them under, and a heading per group would say nothing about them.
  const groups =
    section && section.category !== UNDOCUMENTED
      ? itemGroups(section)
      : [{ key: null, items: section ? section.items : allItems() }];

  const shown = groups
    .map((group) => ({ ...group, items: group.items.filter(matchesItem) }))
    .filter((group) => group.items.length)
    .sort(byGroupSize);
  if (!shown.length) return panel.append(emptyState(tr("noun.items")));

  panel.append(
    h("p", { class: "card-id", text: tr("item.note") }),
    ...shown.map((group) =>
      group.name
        ? itemGroup(group, row)
        : h("ul", { class: "list" }, ...group.items.sort(byName).map(row)),
    ),
  );
}

/**
 * One category of the guide book: its entries listed, each opening its own pages. The
 * sidebar navigates by category, as the item list beside it does, so the panel is reached
 * by a category and the entry is the thing picked inside it - the same two steps as picking
 * an item section and then an item, only here the entry is the book written whole rather
 * than the prose about one item.
 */
async function renderGuidePanel(panel) {
  if (!(await itemsReady(panel, "guide"))) return;

  const sections = guideSections();
  // No category selected - `#patchouli` on its own, or a slug the book does not have -
  // so every entry is listed rather than the panel showing nothing at all.
  const section =
    state.guideCategory === null ? null : sections.find((candidate) => candidate.category === state.guideCategory);
  const entries = section ? guideSectionEntries(section) : guideEntries();

  panel.append(h("p", { class: "card-id", text: tr("guide.note") }));
  const shown = entries.filter(matchesGuideEntry);
  if (!shown.length) return panel.append(emptyState(tr("noun.entries")));

  panel.append(
    h(
      "ul",
      { class: "list" },
      ...shown.map((entry) => guideEntryRow(entry, () => openGuideEntry(entry.id))),
    ),
  );
}

/** A group of the item panel: folded until the reader opens it, then remembered. */
function itemGroup(group, row) {
  return h(
    "details",
    {
      class: "collapse",
      open: state.expanded.has(group.key),
      ontoggle: (event) => {
        if (event.target.open) state.expanded.add(group.key);
        else state.expanded.delete(group.key);
      },
    },
    h(
      "summary",
      {},
      h("span", { text: group.name }),
      h("span", { class: "count", text: String(group.items.length) }),
    ),
    h("ul", { class: "list" }, ...group.items.map(row)),
  );
}

function byName(a, b) {
  return itemLabel(a.id).localeCompare(itemLabel(b.id));
}

/**
 * The panel's groups, fewest items first, so the short ones are met before the long ones
 * and the count beside each name reads upwards. Ordered by what is on screen rather than
 * by what the book holds, so a group emptied by the search box is gone from the order too
 * instead of holding the place its full size earned it. The unheaded group of items the
 * guide names one by one leads, since it is a list rather than a group, and a tie between
 * two groups falls back to the book's own order.
 */
function byGroupSize(a, b) {
  if (a.key === null || b.key === null) return a.key === b.key ? 0 : a.key === null ? -1 : 1;
  return a.items.length - b.items.length || (a.order ?? 0) - (b.order ?? 0);
}

/** Free text match over an item's id and the names it is shown and documented under. */
function matchesItem(item) {
  return matchesQuery(item.id, itemLabel(item.id), item.guide && guideEntryName(item.guide));
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
 * The sidebar's "loading items" and "loading the guide" lines, so the loader can write
 * the count into them without redrawing the whole sidebar on every file. `itemEntries`
 * and `guideNavEntries` hand over the current ones on each redraw, so these never name
 * a node that has been replaced.
 */
let itemLoading = null;
let guideLoading = null;

/** The item list's one load, shared by every caller. Not view state, so not on it. */
let itemsPromise = null;

/**
 * The sidebar's item entries: one per patchouli category, in the book's own order, plus
 * everything the guide does not document. The tag groups an entry is made of are a
 * detail of the panel below it, not another level of navigation.
 */
function itemSections() {
  const sections = new Map();
  for (const item of allItems()) {
    const category = item.guide?.category ?? UNDOCUMENTED;
    const section = sections.get(category) ?? { category, items: [] };
    section.items.push(item);
    sections.set(category, section);
  }
  for (const section of sections.values()) {
    section.name = sectionName(section);
  }
  return [...sections.values()].sort((a, b) => sectionSortnum(a) - sectionSortnum(b) || a.name.localeCompare(b.name));
}

function sectionName(section) {
  if (section.category === UNDOCUMENTED) return tr("item.undocumented");
  return categoryName(section.items[0].guide.book, section.category);
}

function sectionSortnum(section) {
  const book = section.items[0].guide?.book;
  return section.category === UNDOCUMENTED || !book ? Infinity : guideSortnum(book, section.category);
}

/**
 * Every entry of every guide book, tagged with the category it sits in and the book it
 * belongs to. Read from the book's own entry files, so an entry that documents no item
 * is here all the same - the book is listed as it is written, which is a different thing
 * from the item list beside it, since that one only holds the items some entry happens to
 * document.
 *
 * The pages are per locale, so each entry is resolved through `guideEntryOf` as it is
 * read: the prose follows the language toggle without the list being built twice.
 */
function guideEntries() {
  return (store.guide?.books ?? []).flatMap((book) =>
    [...guideEntryIds(book)]
      .map((id) => ({ book, id, page: guideEntryOf(book, id) }))
      .filter((entry) => entry.page)
      .map((entry) => ({ ...entry, category: entry.page.data?.category ?? null })),
  );
}

/**
 * An entry that names no category, or one no category file declares, is filed under this
 * rather than dropped. A symbol, because it is not a category id and must never be
 * mistaken for one - nor for the "all entries" selection, which is `null`.
 */
const UNCATEGORISED = Symbol("uncategorised");

/**
 * One sidebar entry per guide category: the category and the entries filed under it, in
 * the order the book presents them in.
 *
 * The categories come from the book's own category files rather than from the entries, so
 * a category the book declares but writes no entry for is still listed - it is a page of
 * the book, and an empty one is better than a missing link. An entry's `category` names a
 * category in the book's own namespace rather than the book's id, so a section is keyed by
 * book and id together, in case a second book ever declares the same category id.
 */
function guideSections() {
  const sections = new Map();
  // The uncategorised bucket is keyed by its name rather than interpolated: a symbol has
  // no string form, and the key is all that has to be unique here.
  const sectionFor = (book, category) => {
    const key = `${book.id}|${category === UNCATEGORISED ? "uncategorised" : category}`;
    return sections.get(key) ?? { key, book, category, name: null, entries: [] };
  };

  for (const entry of guideEntries()) {
    const declared = entry.category && guideCategoryIds(entry.book).has(entry.category);
    const section = sectionFor(entry.book, declared ? entry.category : UNCATEGORISED);
    section.entries.push(entry);
    sections.set(section.key, section);
  }
  // A category the book declares but writes no entry for is a page of the book all the
  // same, so it is listed rather than left out of the navigation entirely.
  for (const book of store.guide?.books ?? []) {
    for (const id of guideCategoryIds(book)) {
      const section = sectionFor(book, id);
      sections.set(section.key, section);
    }
  }

  for (const section of sections.values()) section.name = guideSectionName(section);
  return [...sections.values()].sort(
    (a, b) =>
      guideSectionSortnum(a) - guideSectionSortnum(b) ||
      String(a.category).localeCompare(String(b.category)) ||
      a.name.localeCompare(b.name),
  );
}

/** The name a category is listed under, which is the book's own. */
function guideSectionName(section) {
  if (section.category === UNCATEGORISED) return tr("guide.uncategorised");
  return categoryName(section.book, section.category);
}

/** A category's place in the book; an entry filed under no category comes last. */
function guideSectionSortnum(section) {
  return section.category === UNCATEGORISED ? Infinity : guideSortnum(section.book, section.category);
}

/**
 * The entries of one category in the book's order: the entry's own `sortnum`, then its
 * name. An entry that declares no `sortnum` falls to the end rather than the front - the
 * book would leave it wherever the file system puts it, and leading the list is a claim
 * about the book that its own files do not make.
 */
function guideSectionEntries(section) {
  return [...section.entries].sort(
    (a, b) =>
      (a.page.data?.sortnum ?? Infinity) - (b.page.data?.sortnum ?? Infinity) ||
      guideEntryName(a).localeCompare(guideEntryName(b)),
  );
}

/**
 * How one sidebar entry is laid out in the panel: what the guide names one by one at the
 * top, then a group for every page that spotlights a tag. Being named by a tag is the
 * whole difference - a page naming an item directly documents that item alone, while a
 * page naming a tag documents every member of it, and those read better gathered under
 * the page's own title.
 */
function itemGroups(section) {
  const direct = [];
  const tagged = new Map();

  for (const item of section.items) {
    if (!item.guide?.group) {
      direct.push(item);
      continue;
    }
    const group =
      tagged.get(item.guide.group) ??
      { key: item.guide.group, name: guideGroupName(item.guide), order: item.guide.order, items: [] };
    group.items.push(item);
    tagged.set(group.key, group);
  }

  // The items the guide names one by one have no heading of their own: they are listed
  // straight into the panel, ahead of the groups a spotlit tag makes.
  const groups = direct.length ? [{ key: null, items: direct }] : [];
  // The book's own order, which is what the panel falls back on to break a tie; for a
  // spotlit tag it is the order the book presents them in.
  groups.push(...[...tagged.values()].sort((a, b) => (a.order ?? 0) - (b.order ?? 0)));
  for (const group of groups) group.items.sort((a, b) => itemLabel(a.id).localeCompare(itemLabel(b.id)));
  return groups;
}

/** Opens one item's page, wiring the jumps back to the quest and trade panels. */
function openItem(id) {
  openItemViewer(id, { quest: onQuestLink, trade: onTradeLink, item: openItem });
}

/** Opens one guide entry's pages, wiring the jumps from its spotlights to their items. */
function openGuideEntry(id) {
  const entry = guideEntries().find((candidate) => candidate.id === id);
  if (!entry) return;
  openGuideEntryViewer(entry, { item: openItem });
}

/** Free text match over a guide entry's id and the name the book gives it. */
function matchesGuideEntry(entry) {
  return matchesQuery(entry.id, guideEntryName(entry));
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
 * The sidebar: one list of characters, beside it one list of item sections - the guide's
 * categories and everything it does not document - and beside that the guide book's own
 * entries. Picking a character shows their content behind the tab bar; picking anything
 * else hides it, since neither items nor a book entry belongs to a character.
 */
function renderSidebar() {
  fill(clear(document.querySelector("#characters")), ...characterEntries());
  fill(clear(document.querySelector("#items")), ...itemEntries());
  fill(clear(document.querySelector("#guide")), ...guideNavEntries());

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
  // The list is still being fetched, and its categories are the guide's own, so there
  // is nothing honest to show yet but the fact that it is on its way.
  if (!state.itemsLoaded) {
    itemLoading = h("span", { class: "nav-note", text: tr("nav.loadingItems") });
    return [h("li", {}, itemLoading)];
  }

  const here = (category) => state.section === "item" && state.category === category;
  return [
    navEntry(here(null), tr("item.all"), allItems().length, null, () => selectCategory(null)),
    ...itemSections().map((section) =>
      navEntry(
        here(section.category),
        section.name,
        section.items.length,
        section.category === UNDOCUMENTED ? tr("item.undocumented") : String(section.category),
        () => selectCategory(section.category),
      ),
    ),
  ];
}

/**
 * The guide book's categories, counted by how many entries each holds - an entry being
 * the unit the sidebar navigates by, since the entries themselves are what the panel
 * lists. The count is of entries rather than of pages or items, so the number beside a
 * category is the number of things selecting it shows.
 */
function guideNavEntries() {
  // The book arrives with the item list, so there is nothing to list until then.
  if (!state.itemsLoaded) {
    guideLoading = h("span", { class: "nav-note", text: tr("nav.loadingGuide") });
    return [h("li", {}, guideLoading)];
  }

  const sections = guideSections();
  const here = (category) => state.section === "guide" && state.guideCategory === category;
  return [
    navEntry(here(null), tr("guide.all"), guideEntries().length, null, () => selectGuideCategory(null)),
    ...sections.map((section) =>
      navEntry(
        here(section.category),
        section.name,
        section.entries.length,
        section.category === UNCATEGORISED ? tr("guide.uncategorised") : String(section.category),
        () => selectGuideCategory(section.category),
      ),
    ),
  ];
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

  // The sidebar's groups fold away so one can have the room to itself. Their state is
  // remembered here, since the elements themselves are never rebuilt.
  const folds = [
    ["#nav-characters", "characters"],
    ["#nav-items", "items"],
    ["#nav-patchouli", "patchouli"],
  ];
  for (const [selector, key] of folds) {
    const group = document.querySelector(selector);
    group.open = !state.navFolded.has(key);
    group.addEventListener("toggle", () => {
      if (group.open) state.navFolded.delete(key);
      else state.navFolded.add(key);
    });
  }
}

// ---------------------------------------------------------------------------
// boot
// ---------------------------------------------------------------------------

async function boot() {
  applyTheme();
  wireChrome();
  readLocation(); // the section only; the character slug needs the loaded registries
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
    // Whose home each structure is, so a visit condition can name its host. One small
    // file, and a condition that says "visiting Reimu" is worth it on the first paint.
    loadStructureHosts(),
    ...registries.map((name) => loadRegistry(name, progressFor(name))),
  ]);

  buildCharacters();
  readLocation(); // now the character slug can be resolved
  setStatus("");
  setProgress(0, 0);
  render();

  refreshFromGitHub();

  // Everything the item list is made of - the guide book, the tags it names, the
  // recipes and the reward tables - is a few hundred files that no character content
  // needs. It is fetched behind the first render, so the characters are readable while
  // it arrives and the sidebar says it is loading; the panel waits for it if the
  // reader gets there first. Nobody awaits it here, so its failures are reported here.
  loadItems(reportItemProgress).catch(reportFailure);
}

/** The item list, fetched once and kept. Shared, so a second caller joins the first. */
function loadItems(onProgress) {
  itemsPromise ??= (async () => {
    const files = guideFileCount() + sourceFileCount();
    // Each part reports its own count, so track them separately and show the sum.
    const seen = new Map();
    const report = () => onProgress?.([...seen.values()].reduce((sum, value) => sum + value, 0), files);

    await Promise.all([
      loadGuide((done) => {
        seen.set("guide", done);
        report();
      }),
      // What Rinnosuke may put on his shelves, which is an item's fourth source.
      loadShopOffers(),
      loadItemSources((done) => {
        seen.set("sources", done);
        report();
      }),
    ]);
    // The index is built from whatever has landed, so it makes no difference which of
    // the three arrived first - and this is the build that has all of them.
    await buildItemIndex();
    state.itemsLoaded = true;
    // A link straight to `#items/decoration` or `#patchouli/alchemy` could only be
    // answered now.
    readLazySelection();
    renderSidebar();
  })().catch((error) => {
    // A failed load is not kept: the reader who opens the item section afterwards
    // should get another try rather than the same rejection.
    itemsPromise = null;
    throw error;
  });

  return itemsPromise;
}

/** The sidebar's own progress lines, which are all it can say until the list is in. */
function reportItemProgress(done, total) {
  if (itemLoading) itemLoading.textContent = `${tr("nav.loadingItems")} ${done} / ${total}`;
  if (guideLoading) guideLoading.textContent = `${tr("nav.loadingGuide")} ${done} / ${total}`;
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
    // New content may bring recipes, loot tables or guide pages with it, so the item
    // load is redone: dropping the shared promise is what makes the next caller
    // refetch rather than reuse what is already in the store.
    itemsPromise = null;
    await loadItems();
    buildCharacters();
    render();
    setStatus(trPlural("status.refreshed", added, added));
  } catch {
    /* offline, rate limited, or the branch is not published: keep the committed index */
  }
}

boot().catch(reportFailure);
