// Data access. Every datapack file is fetched at its real path under
// src/generated/resources/ - nothing is bundled or duplicated.
//
// `rpg-manifest.json` is only an index of which registry files exist. Loot tables,
// item tags and lang files have derivable paths, so an index that predates a new
// file still resolves it.

import { setProgress } from "./dom.js";

export const store = {
  manifest: null,
  lang: new Map(), // locale -> { key: text }
  quests: new Map(), // resource id -> json
  trades: new Map(),
  starters: new Map(),
  dialogs: new Map(), // resource id -> json
  loot: new Map(), // resource id -> json | null
  tags: new Map(), // tag id -> Set<string> | null
  characters: new Map(), // key -> { key, entity, dirs:Set, name }
};

const inflight = new Map();

/** Fetches a JSON file from the published repository root, caching the promise. */
export function fetchJson(file) {
  let pending = inflight.get(file);
  if (!pending) {
    pending = fetch(file, { cache: "no-cache" })
      .then((response) => {
        if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
        return response.json();
      })
      .catch((error) => {
        inflight.delete(file);
        throw error;
      });
    inflight.set(file, pending);
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
  store.manifest = await fetchJson("rpg-manifest.json");
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

/** The currency tag, used to tell "sell to character" from "request a craft". */
export async function loadCurrencyTag() {
  const id = "gensokyolegacy:currency";
  if (store.tags.has(id)) return store.tags.get(id);
  const file = store.manifest.itemTags[id] ?? `src/generated/resources/data/${id.split(":")[0]}/tags/item/currency.json`;
  try {
    store.tags.set(id, new Set((await fetchJson(file)).values));
  } catch {
    store.tags.set(id, new Set(["minecraft:emerald", "minecraft:gold_ingot"]));
  }
  return store.tags.get(id);
}

export { setProgress };
