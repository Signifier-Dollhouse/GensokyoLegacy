// Data access. Every datapack file is fetched at its real path under
// src/generated/resources/ - nothing is bundled or duplicated.
//
// `rpg-manifest.json` is only an index of which registry files exist. Loot tables,
// item tags and lang files have derivable paths, so an index that predates a new
// file still resolves it.

import { setProgress } from "./dom.js";
import { state } from "./state.js";

export const store = {
  manifest: null,
  /** The index's content fingerprint, which versions every data URL. See `versioned`. */
  revision: null,
  lang: new Map(), // locale -> { key: text }, from the mod's own lang files
  vanillaLang: new Map(), // locale -> { key: text }, from assets/lang/vanilla
  quests: new Map(), // resource id -> json
  trades: new Map(),
  starters: new Map(),
  dialogs: new Map(), // resource id -> json
  loot: new Map(), // resource id -> json | null
  tags: new Map(), // tag id -> Set<string> | null
  characters: new Map(), // key -> { key, entity, dirs:Set, name }
  // The item section, filled in on first use: see `loadItems`.
  recipes: new Map(), // recipe id -> json
  guide: null, // { books: [...] }, see `loadGuide`
  questDrops: new Map(), // quest id -> [{ item, count, table }]
  items: new Map(), // item id -> { id, guide, recipes, trades, drops, shelf }
  structureHosts: new Map(), // structure id -> entity id, from `character_config`
};

const inflight = new Map();

/**
 * A data file's URL, carrying the index's content fingerprint.
 *
 * This is what makes the browser's cache worth having. The page asks for several hundred
 * files, and a plain request would have the browser ask the server about every one of
 * them on every visit - hundreds of round trips before anything can be drawn. Asking
 * instead for the path plus the fingerprint means the cached copy is exactly the right
 * one: it is kept as long as the data is unchanged, and the name changes the moment new
 * content lands on the branch, which is when the copy should be dropped. One update
 * costs one refresh; every visit after it costs none.
 *
 * An index predating this field simply fetches the plain path, which is what the page
 * did before.
 */
function versioned(file) {
  return store.revision ? `${file}?v=${store.revision}` : file;
}

/**
 * Fetches a JSON file from the published repository root, caching the promise.
 *
 * `revalidate` is for the two things that must never be served stale: the index, which
 * carries the fingerprint everything else is versioned by, and the committed vanilla
 * tables, which are small, live with the page rather than with the datapack, and so are
 * not covered by the fingerprint.
 */
export function fetchJson(file, { revalidate = false } = {}) {
  // Keyed by what was actually requested, so a file asked for at two revisions in one
  // sitting is fetched twice rather than answered twice from the same promise.
  const url = revalidate ? file : versioned(file);
  let pending = inflight.get(url);
  if (!pending) {
    pending = fetch(url, { cache: revalidate ? "no-cache" : "force-cache" })
      .then((response) => {
        if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
        return response.json();
      })
      .catch((error) => {
        inflight.delete(url);
        throw error;
      });
    inflight.set(url, pending);
  }
  return pending;
}

/** Runs `task` over `items` with a bounded number of parallel requests. */
export async function pool(items, limit, task) {
  const queue = [...items];
  const width = Math.max(1, Math.min(limit, queue.length));
  await Promise.all(
    Array.from({ length: width }, async () => {
      for (let item = queue.shift(); item !== undefined; item = queue.shift()) {
        await task(item);
      }
    }),
  );
}

/** Maps a registry name to the map holding its parsed entries. */
export function tableFor(name) {
  return { quest: store.quests, trade: store.trades, dialog_starter: store.starters }[name];
}

/** `gensokyolegacy:reimu/daily_food` -> `reimu`, the character folder. */
export function dirOf(id) {
  return id.split(":")[1].split("/")[0];
}

/** Builds a registry entry from a file path under the registry's root. */
function makeEntry(registry, file) {
  const idPath = file.slice(registry.root.length + 1, -".json".length);
  return { id: `${store.manifest.namespace}:${idPath}`, dir: idPath.split("/")[0], file };
}

export async function loadManifest() {
  // The index is the one file that is always revalidated: a stale copy would pin the
  // page to the fingerprint it was built with, and no new content would ever be asked
  // for again. It is one small request, so the cost is a single conditional GET.
  store.manifest = await fetchJson("rpg-manifest.json", { revalidate: true });
  store.revision = store.manifest.revision ?? null;
}

/** Loads every entry of one registry into its table, reporting progress. */
export async function loadRegistry(name, progress) {
  const registry = store.manifest.registries[name];
  const table = tableFor(name);
  const entries = registry.files.map((file) => makeEntry(registry, file));
  let done = 0;
  await pool(entries, 10, async (entry) => {
    try {
      table.set(entry.id, await fetchJson(entry.file));
    } catch {
      table.set(entry.id, null);
    }
    progress?.(++done, entries.length);
  });
  return entries;
}

export async function loadLang(file) {
  const locale = file.split("/").pop().replace(".json", "");
  store.lang.set(locale, await fetchJson(file));
}

/**
 * Vanilla strings, kept apart from the mod's tables because the mod says nothing
 * about `minecraft:` items, blocks, mobs or advancements. The files are committed
 * rather than generated at build time, so they sit at a fixed path; the manifest
 * lists them for anyone regenerating it.
 */
export const VANILLA_LANG = ["en_us", "zh_cn"].map((locale) => `assets/lang/vanilla/${locale}.json`);

export async function loadVanillaLang(file) {
  const locale = file.split("/").pop().replace(".json", "");
  try {
    // Re-read every visit rather than versioned: the tables are committed with the page
    // and are small, so a hand edit to one should not need an index rebuild to be seen.
    store.vanillaLang.set(locale, await fetchJson(file, { revalidate: true }));
  } catch {
    // The page is still usable: every id falls back to a prettified name.
    store.vanillaLang.set(locale, null);
  }
}

/** Resolves a loot table id to its file, preferring the index. */
function lootFile(id) {
  const known = store.manifest.lootTables[id];
  if (known) return known;
  const [namespace, ...rest] = id.split(":");
  return `src/generated/resources/data/${namespace}/loot_table/${rest.join("/")}.json`;
}

export async function loadLootTable(id) {
  if (store.loot.has(id)) return store.loot.get(id);
  try {
    const table = await fetchJson(lootFile(id));
    store.loot.set(id, table);
  } catch {
    store.loot.set(id, null);
  }
  return store.loot.get(id);
}

/** Expands an item tag into its members, preferring the index. */
export async function loadItemTag(id) {
  const key = id.replace(/^#/, "");
  if (store.tags.has(key)) return store.tags.get(key);
  const [namespace, ...rest] = key.split(":");
  const file = store.manifest.itemTags[key] ?? `src/generated/resources/data/${namespace}/tags/item/${rest.join("/")}.json`;
  try {
    const tag = await fetchJson(file);
    store.tags.set(key, new Set(tag.values ?? []));
  } catch {
    store.tags.set(key, null);
  }
  return store.tags.get(key);
}

export async function loadEntityTag(id) {
  const key = id.replace(/^#/, "");
  const [namespace, ...rest] = key.split(":");
  const file = store.manifest.entityTags[key] ?? `src/generated/resources/data/${namespace}/tags/entity_type/${rest.join("/")}.json`;
  try {
    const tag = await fetchJson(file);
    store.tags.set(`entity:${key}`, new Set(tag.values ?? []));
    return store.tags.get(`entity:${key}`);
  } catch {
    return null;
  }
}

/**
 * Fetches every dialog file. Dialogs are the bulk of the datapack (200+ files),
 * so they are loaded on demand rather than on first paint.
 */
export async function loadAllDialogs(progress) {
  const registry = store.manifest.registries.dialog;
  const entries = registry.files.map((file) => makeEntry(registry, file));
  let done = 0;
  await pool(entries, 12, async (entry) => {
    try {
      store.dialogs.set(entry.id, await fetchJson(entry.file));
    } catch {
      store.dialogs.set(entry.id, null);
    }
    progress?.(++done, entries.length);
  });
  return entries;
}

/** Fetches a single dialog, used by the conversation viewer. */
export async function loadDialog(id) {
  if (store.dialogs.has(id)) return store.dialogs.get(id);
  const [namespace, ...rest] = id.split(":");
  const file = `src/generated/resources/data/${namespace}/gensokyolegacy/dialog/${rest.join("/")}.json`;
  try {
    const dialog = await fetchJson(file);
    store.dialogs.set(id, dialog);
    return dialog;
  } catch {
    store.dialogs.set(id, null);
    return null;
  }
}

/**
 * Groups every registry entry by character.
 *
 * Quests, trades and starters carry an explicit `character` entity id; dialogs do
 * not, so they are attributed to the character whose registry folder they sit in.
 */
export function buildCharacters() {
  store.characters.clear();
  const ensure = (key, entity) => {
    let character = store.characters.get(key);
    if (!character) {
      character = { key, entity: entity ?? null, dirs: new Set() };
      store.characters.set(key, character);
    }
    if (entity) character.entity = entity;
    return character;
  };

  for (const name of ["quest", "trade", "dialog_starter"]) {
    for (const [id, data] of tableFor(name)) {
      if (!data) continue;
      ensure(data.character ?? dirOf(id), data.character ?? null).dirs.add(dirOf(id));
    }
  }

  for (const file of store.manifest.registries.dialog.files) {
    const dir = makeEntry(store.manifest.registries.dialog, file).dir;
    const owner = [...store.characters.values()].find((character) => character.dirs.has(dir));
    (owner ?? ensure(dir, null)).dirs.add(dir);
  }

  return store.characters;
}

/** Which character owns the entry with this id. */
export function characterKeyOf(id) {
  const dir = dirOf(id);
  return [...store.characters.values()].find((character) => character.dirs.has(dir))?.key ?? dir;
}

/**
 * The registry folder a character's content sits in, from their entity id. This is
 * the stable, human readable handle (`reimu`) that names and URLs are keyed on,
 * whereas the entity id spells out the full name.
 */
export function characterSlugOf(entity) {
  if (!entity) return null;
  const character = [...store.characters.values()].find((entry) => entry.entity === entity);
  if (!character) return null;
  return [...character.dirs].sort()[0] ?? null;
}

const CURRENCY = "gensokyolegacy:currency";

/** The currency tag, used to tell "sell to character" from "request a craft". */
export async function loadCurrencyTag() {
  const tag = await loadItemTag(CURRENCY);
  if (tag) return tag;
  // The two currencies the game accepts, should the tag be missing. Cached rather than
  // returned, since the trade list and the item index both read it from the store.
  const fallback = new Set(["minecraft:emerald", "minecraft:gold_ingot"]);
  store.tags.set(CURRENCY, fallback);
  return fallback;
}

const SHOP_OFFERS = "gensokyolegacy:morichika_offers";

/**
 * What Rinnosuke may put on his shop shelves, and at what price.
 *
 * The tag is named by `GLTagGen.MORICHIKA_OFFERS` and read by `MorichikaEntity`; the
 * price and stock ranges come from the `morichika_offer` data map. Neither is referred
 * to by the RPG registries, so both are listed in the manifest's `EXTRA_ITEM_TAGS` and
 * `EXTRA_DATA_MAPS` rather than discovered by following the content.
 */
export async function loadShopOffers() {
  const file =
    store.manifest.dataMaps?.morichika_offer ??
    "src/generated/resources/data/gensokyolegacy/data_maps/item/morichika_offer.json";
  let ranges = {};
  try {
    ranges = (await fetchJson(file)).values ?? {};
  } catch {
    // The tag alone still says what the shop can have, just not what for.
  }

  const offers = new Map();
  for (const id of (await loadItemTag(SHOP_OFFERS)) ?? []) {
    offers.set(id, ranges[id] ?? { minPrice: 1, maxPrice: 1, minStock: 1, maxStock: 1 });
  }
  store.shopOffers = offers;
  return offers;
}

/**
 * Whose home a structure is: structure id -> the entity id of the character living
 * there.
 *
 * A `visit_structure` condition names a structure, but what it means to a reader is
 * *whose house this is*, and the character is what the rest of the page is written
 * around. `CharacterConfig.structure` is the game's own record of which home belongs
 * to whom, so the mapping is read rather than guessed from the id.
 *
 * A player-built home has no resident and is not in this map at all, which is the
 * answer rather than a gap: `StructureKey.CUSTOM` names every one of them, and the
 * viewer's caller says so.
 */
export async function loadStructureHosts() {
  const file =
    store.manifest.dataMaps?.character_config ??
    "src/generated/resources/data/gensokyolegacy/data_maps/entity_type/character_config.json";
  try {
    for (const [entity, config] of Object.entries((await fetchJson(file)).values ?? {})) {
      if (typeof config?.structure === "string") store.structureHosts.set(config.structure, entity);
    }
  } catch {
    // No homes named: a visit condition falls back to its structure id.
  }
  return store.structureHosts;
}

// ---------------------------------------------------------------------------
// the item section
// ---------------------------------------------------------------------------

/**
 * What the item section needs that the registries do not carry: the guide book, which
 * decides the categories and holds the prose, and the recipes plus quest reward tables,
 * which decide where an item comes from.
 *
 * The guide is small and is fetched with everything else, so the sidebar can list the
 * categories and their counts from the first paint. The sources are a few hundred files
 * and are fetched the first time an item is actually opened - the same trade-off the
 * dialog tab makes. Nothing here is derived ahead of time: every entry is read from the
 * JSON at its real path, so merging new content onto the branch is all it takes for the
 * site to show it.
 */
export async function loadItemSources(progress) {
  const recipes = store.manifest.recipes;
  const questLoot = questLootTables();
  const total = recipes.files.length + questLoot.size;
  let done = 0;
  const tick = () => progress?.(++done, total);

  // Cleared first, so loading again after new content appears on the branch rebuilds
  // the index rather than adding the new sources to the old one.
  store.recipes.clear();
  store.questDrops.clear();

  await Promise.all([
    pool(recipes.files, 10, async (file) => {
      const id = `${store.manifest.namespace}:${file.slice(recipes.root.length + 1, -".json".length)}`;
      try {
        store.recipes.set(id, { id, file, recipe: await fetchJson(file) });
      } catch {
        store.recipes.set(id, null);
      }
      tick();
    }),
    pool([...questLoot.keys()], 8, async (table) => {
      const questId = questLoot.get(table);
      const drops = collectDrops(table, await loadLootTable(table));
      if (drops.length) store.questDrops.set(questId, [...(store.questDrops.get(questId) ?? []), ...drops]);
      tick();
    }),
  ]);

  await buildItemIndex();
}

/** The loot tables quest rewards hand out, as table id -> quest id. */
function questLootTables() {
  const tables = new Map();
  for (const [id, quest] of store.quests) {
    for (const reward of quest?.rewards ?? []) {
      if (reward.type === "gensokyolegacy:loot_table" && typeof reward.table === "string") {
        tables.set(reward.table, id);
      }
    }
  }
  return tables;
}

/**
 * The item drops a loot table can roll, with the pool they come from. The entry is
 * kept as it is written, since how many it yields is read out of its functions at
 * display time rather than flattened here.
 */
function collectDrops(tableId, table) {
  const drops = [];
  for (const pool of table?.pools ?? []) {
    const rolls = (pool.rolls ?? 0) + (pool.bonus_rolls ?? 0);
    for (const entry of pool.entries ?? []) {
      if (entry.type === "minecraft:item" && typeof entry.name === "string") {
        drops.push({ item: entry.name, table: tableId, rolls, weight: entry.weight ?? 0, entry });
      }
    }
  }
  return drops;
}

/** Fetches every guide book: the definition, plus each locale's pages. */
export async function loadGuide(progress) {
  const books = [];
  const total = guideFileCount();
  let done = 0;
  for (const book of store.manifest.guides ?? []) {
    // A category is named `<namespace>:<folder>` in the book's own namespace, which is
    // what an entry's `category` field refers to - not the book's id.
    const namespace = book.id.split(":")[0];
    const locales = {};
    for (const [locale, files] of Object.entries(book.locales)) {
      const prefix = `${book.pages}/${locale}`;
      const categories = new Map();
      const entries = new Map();
      const load = async (file, into, id) => {
        try {
          into.set(id, { file, data: await fetchJson(file) });
        } catch {
          /* a page that will not load simply does not appear */
        }
        progress?.(++done, total);
      };
      await Promise.all([
        ...files.categories.map((file) => load(file, categories, `${namespace}:${categoryId(file, prefix)}`)),
        ...files.entries.map((file) => load(file, entries, `${book.id}/${entryPath(file, prefix)}`)),
      ]);
      locales[locale] = { categories, entries };
    }
    books.push({ id: book.id, definition: await fetchJson(book.book), locales });
  }
  store.guide = { books };
}

/** How many files the guide book is made of, so a progress bar has something to fill. */
export function guideFileCount() {
  return (store.manifest.guides ?? []).reduce(
    (sum, book) =>
      sum +
      Object.values(book.locales).reduce((count, locale) => count + locale.categories.length + locale.entries.length, 0),
    0,
  );
}

/** How many files the recipes and the quest reward tables come to. */
export function sourceFileCount() {
  return store.manifest.recipes.files.length + questLootTables().size;
}

/** `.../categories/alchemy.json` -> `alchemy`, the id an entry's `category` uses. */
function categoryId(file, prefix) {
  return file.slice(prefix.length + "/categories/".length, -".json".length);
}

/** `.../entries/alchemy/hexbrew.json` -> `alchemy/hexbrew`. */
function entryPath(file, prefix) {
  return file.slice(prefix.length + "/entries/".length, -".json".length);
}

/** A book's pages in the active language, falling back to English. */
export function guideLocale(book) {
  return book.locales[state.lang] ?? book.locales.en_us ?? Object.values(book.locales)[0];
}

/**
 * The item a recipe hands out. The crafting types produce one directly; the alchemy
 * and brewing types produce a fluid, which the game fills a `<fluid>_bottle` item
 * from - `HexBrew.java` registers the bottle under exactly that name - so the recipe
 * is recorded against the bottle rather than against the fluid, which is not an item
 * and has no place in the list.
 */
export function recipeOutputs(recipe) {
  if (typeof recipe?.result?.id === "string") return [recipe.result.id];
  return typeof recipe?.resultFluid?.id === "string" ? [`${recipe.resultFluid.id}_bottle`] : [];
}

/** `minecraft:crafting_shaped` -> `crafting_shaped`, spelled out here rather than imported. */
function recipeKind(recipe) {
  return String(recipe?.type ?? "?").split(":").pop();
}

/**
 * Every item a recipe names as an ingredient, and so consumes.
 *
 * The twin of `recipeInputs` in views.js, which draws these same fields: a shaped recipe
 * spells its ingredients out in `key`, every other type in a field of its own. Only what
 * the recipe names directly is collected. A `tag` is a group rather than an item, so a
 * recipe taking `#gensokyolegacy:cushions` is not an ingredient of each cushion in it,
 * and a fluid is not an item at all - neither can be looked up in the item index.
 */
export function recipeIngredients(recipe) {
  const ids = new Set();
  const add = (value) => {
    if (typeof value === "string") {
      ids.add(value);
      return;
    }
    if (typeof value?.item === "string") ids.add(value.item);
    // A NeoForge component set spells its one item `items`; `tag` is left out above.
    else if (typeof value?.items === "string") ids.add(value.items);
  };
  const addAll = (value) => (Array.isArray(value) ? value : [value]).forEach(add);

  switch (recipeKind(recipe)) {
    case "crafting_shaped":
      for (const ingredient of Object.values(recipe?.key ?? {})) addAll(ingredient);
      break;
    case "stonecutting":
      addAll(recipe?.ingredient);
      break;
    case "unordered_alchemy":
      addAll(recipe?.input);
      break;
    case "witch_enhance":
    case "witch_merge":
      addAll(recipe?.extra);
      addAll(recipe?.potionIngredient);
      break;
    default:
      addAll(recipe?.ingredients);
  }
  return ids;
}

/**
 * Builds the item index: for every item the mod adds or hands out, the guide entry
 * that documents it, every way to get one and every way it is used.
 *
 * The item list is derived, never written down: the mod's own lang files enumerate
 * everything it registers, and the sources below add the handful of items from other
 * namespaces that it gives the player (the guide book, converted planks). Guide links
 * are read from every locale, since the ids a page names are the same in each - only
 * the prose is translated.
 */
export async function buildItemIndex() {
  const items = new Map();
  const entryFor = (id) => {
    let item = items.get(id);
    if (!item) {
      items.set(id, (item = { id, guide: null, recipes: [], usedIn: [], trades: [], drops: [], shelf: null }));
    }
    return item;
  };

  for (const id of registeredItems()) entryFor(id);

  const books = store.guide?.books ?? [];
  // A spotlight may name a whole tag, and every member of it is documented by that
  // page, so the tags the book uses are fetched before the pages are handed out.
  await pool([...guideTags(books)], 8, (tag) => loadItemTag(tag));

  for (const book of books) {
    for (const locale of Object.values(book.locales)) {
      for (const [id, page] of locale.entries) {
        const category = page.data?.category ?? null;
        const subjects = guideSubjects(page.data);

        const document = (itemId, subject) => {
          const item = entryFor(itemId);
          // The first entry that names an item documents it; a later one saying
          // something else about the same item would only dilute the page.
          if (item.guide) return;
          // Which subject it came under, rather than the pages themselves: those live
          // in per-locale files and are looked up when the page is opened.
          item.guide = {
            book,
            id,
            category,
            subject: subject.key,
            tag: subject.tag ? subject.key : null,
            group: subject.grouped ? subject.group : null,
            order: subject.order ?? 0,
          };
        };

        for (const [key, subject] of subjects.entries) {
          for (const itemId of guideSubjectItems(key, subject.tag)) document(itemId, subject);
        }
        // The icon is the entry's own illustration and names nothing. It is drawn on
        // the entry in the book, where it sits beside the title rather than under a
        // spotlight, so treating it as a subject would put an item on a page that never
        // spotlights it - `alchemy/ingredients` uses the Ghost Fire Mushroom that way,
        // and the page about that mushroom is `nature/magical_forest`. An entry with no
        // spotlight at all documents no item, which leaves those items to the
        // undocumented bucket rather than pointing them at prose that only mentions them.
      }
    }
  }

  for (const source of store.recipes.values()) {
    if (!source) continue;
    for (const itemId of recipeOutputs(source.recipe)) entryFor(itemId).recipes.push(source);
  }

  // Only an offer that hands out something other than currency gives the player an
  // item; the rest are the player selling to a character.
  const currency = store.tags.get(CURRENCY);
  for (const [id, trade] of store.trades) {
    const result = trade?.result?.id;
    if (!result || currency?.has(result)) continue;
    entryFor(result).trades.push({ id, trade });
  }

  for (const [questId, drops] of store.questDrops) {
    for (const drop of drops) entryFor(drop.item).drops.push({ questId, ...drop });
  }

  for (const [id, offer] of store.shopOffers ?? []) entryFor(id).shelf = offer;

  // The list is what the mod adds, so a vanilla item it happens to hand out - a
  // processed golden apple, say - is dropped rather than given a page of its own. It is
  // still named wherever it is traded for, since the lang table covers that.
  for (const id of items.keys()) if (id.startsWith("minecraft:")) items.delete(id);

  // The recipes read backwards, which is what answers "what do I make this into". Both
  // ends have to be on the list: a recipe naming an item from another namespace produces
  // something with no page to land on, and the ingredient is only ever an item here
  // because the list is indexed by item.
  for (const source of store.recipes.values()) {
    if (!source) continue;
    const outputs = recipeOutputs(source.recipe).filter((id) => items.has(id));
    if (!outputs.length) continue;
    for (const input of recipeIngredients(source.recipe)) {
      for (const output of outputs) items.get(input)?.usedIn.push({ ...source, output });
    }
  }

  store.items = items;
}

/**
 * What a spotlight page is about: one item, a whole tag of them, or several of either.
 * Patchouli spells a tag reference `tag:namespace:path`, where a datapack would write
 * `#namespace:path`, and lets a spotlight name a list - four noren tags under one title.
 *
 * One spotlight may name several subjects, and then they are documented together, so
 * what groups them is the page rather than any one tag: three beds under one title are as
 * much a group as four noren tags. Being named alongside something else is therefore the
 * whole difference, and a spotlight that names one subject on its own groups nothing.
 * The subjects are ids, the same in every locale, so their names make a key for the group
 * that does not shift with the language toggle.
 */
function guideSubjectsOf(value, order) {
  const subjects = (Array.isArray(value) ? value : [value])
    .filter((entry) => typeof entry === "string")
    .map((entry) => (entry.startsWith("tag:") ? { key: entry.slice(4), tag: true } : { key: entry, tag: false }));
  const group = subjects.map((subject) => subject.key).sort().join("|");
  // A tag always gathers its members, however many subjects share the spotlight; a
  // named item only joins a group when the spotlight documents it with others.
  const grouped = subjects.length > 1;
  for (const subject of subjects) {
    subject.group = group;
    subject.grouped = subject.tag || grouped;
    subject.order = order;
  }
  return subjects;
}

/**
 * Splits an entry's pages by what each one is about. Exported so an item's page can be
 * resolved in the language being read: the pages are per-locale files, and only their
 * order and the ids they name are the same in each.
 *
 * A spotlight names its item or tag, and the text page below it belongs to that
 * spotlight, while a text page that opens the entry belongs to the entry as a whole. So
 * an entry that spotlights several things keeps their pages apart - a miasma mushroom
 * does not inherit the prose about the miasma bottle - and every item still sees the
 * shared text.
 */
export function guideSubjects(entry) {
  const shared = [];
  const entries = new Map();
  let current = []; // the subjects the pages below a spotlight belong to

  for (const [index, page] of (entry?.pages ?? []).entries()) {
    const spotlights = page.type === "patchouli:spotlight" ? guideSubjectsOf(page.item, index) : [];
    if (spotlights.length) current = spotlights;

    for (const subject of current) {
      // The key and the group ride along on the subject, so a caller holding one knows
      // what it names and which page named it.
      const found = entries.get(subject.key) ?? { key: subject.key, ...subject, pages: [] };
      found.pages.push(page);
      entries.set(subject.key, found);
    }
    // Nothing above it: the page opens the entry, so it belongs to all of it.
    if (!current.length) shared.push(page);
  }
  return { shared, entries };
}

/** The item ids a subject covers: one item, or every member of a tag it names. */
function guideSubjectItems(key, isTag) {
  if (!isTag) return [key];
  // A tag the repository does not have documents nothing rather than a phantom item.
  return [...(store.tags.get(key) ?? [])];
}

/** Every item tag any guide page spotlights, across every book and locale. */
function guideTags(books) {
  const tags = new Set();
  for (const book of books) {
    for (const locale of Object.values(book.locales)) {
      for (const page of locale.entries.values()) {
        for (const [key, subject] of guideSubjects(page.data).entries) {
          if (subject.tag) tags.add(key);
        }
      }
    }
  }
  return tags;
}

/**
 * The lang files the mod generates from its registrations. They are the list of what
 * exists, since they are written from the registrations themselves.
 */
const GENERATED_LOCALES = new Set(["en_us", "en_ud"]);

/**
 * Every item the mod registers, read out of its own lang files. `item.` and `block.`
 * are the two spellings one item can have, and a block's item id is the block's own.
 *
 * Only the generated tables are read. The hand-authored translations lag behind on
 * purpose - a key for something that has since been removed costs the game nothing,
 * since it names an id nothing answers to - but a stale key would otherwise put a block
 * back into the item list, with no name to show for it in every other language.
 */
function* registeredItems() {
  const namespace = `${store.manifest.namespace}:`;
  const generated = [...store.lang].filter(([locale]) => GENERATED_LOCALES.has(locale));
  const seen = new Set();
  for (const [, table] of generated.length ? generated : store.lang) {
    for (const key of Object.keys(table ?? {})) {
      // A lang key is a dotted path, so the id has to be spelled back before it can
      // be compared with a namespaced one.
      const match = /^(?:item|block)\.(.+)$/.exec(key);
      if (!match) continue;
      const id = match[1].replace(".", ":");
      if (!id.startsWith(namespace) || seen.has(id)) continue;
      seen.add(id);
      yield id;
    }
  }
}

export { setProgress };
