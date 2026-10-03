// Translation, naming and unit formatting.
//
// `label`/`itemLabel` resolve the mod's own lang files (quest, dialog, item and
// entity text); the words the site puts around that content come from `tr`.

import { tr, trOrNull } from "./i18n.js";
import { characterSlugOf, store } from "./store.js";
import { state } from "./state.js";

/** Looks a translation key up in the active locale, falling back to English. */
export function t(key) {
  if (typeof key !== "string") return null;
  for (const locale of [state.lang, "en_us"]) {
    const table = store.lang.get(locale);
    if (table && key in table) return table[key];
  }
  return null;
}

/** Resolves a key, falling back to the key itself so nothing is ever hidden. */
export function label(key) {
  if (typeof key !== "string") return "";
  return t(key) ?? key;
}

/** `minecraft:white_wool` -> `White Wool`, for ids with no translation. */
export function prettify(id) {
  const tail = String(id ?? "")
    .split(":")
    .pop()
    .split("/")
    .pop();
  return tail
    .replace(/[_.]/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase())
    .trim();
}

/** Item display name, checking the item, block and entity namespaces in turn. */
export function itemLabel(id) {
  if (!id) return "";
  const bare = id.replace(/^#/, "");
  return t(`item.${bare}`) ?? t(`block.${bare}`) ?? t(`entity.${bare}`) ?? prettify(bare);
}

export function entityLabel(id) {
  return id ? (t(`entity.${id}`) ?? prettify(id)) : "";
}

/**
 * Short display name for a character, keyed by the folder their content sits in
 * (`reimu`). The mod's own entity name is the fallback, so a character added to the
 * game shows up in game wording rather than as an id; callers that have room put
 * the full name in the tooltip.
 */
export function characterLabel(entity) {
  return trOrNull(`character.${characterSlugOf(entity)}`) ?? entityLabel(entity);
}

export function advancementLabel(id) {
  const path = String(id).split(":")[1];
  return t(`advancements.${path}.title`) ?? prettify(path);
}

/** `gensokyolegacy:kill_enemy` -> `kill_enemy`, for readable dispatch. */
export function kindOf(node) {
  return typeof node?.type === "string" ? node.type.split(":").pop() : "?";
}

/**
 * Datapack durations are in ticks. Renders them the way a player reads them,
 * keeping the exact tick count available in the tooltip.
 */
export function formatTicks(ticks) {
  const seconds = Math.round((ticks ?? 0) / 20);
  if (seconds < 90) return `${seconds}${tr("unit.second")}`;
  if (seconds < 5400) return `${Math.round(seconds / 60)} ${tr("unit.minute")}`;
  const hours = seconds / 3600;
  return `${hours.toFixed(hours % 1 === 0 ? 0 : 1)} ${tr("unit.hour")}`;
}

/** Same as `formatTicks` but as a node carrying the raw value. */
export function ticks(ticks_) {
  const node = document.createElement("span");
  node.title = tr("ticks.tooltip", ticks_);
  node.textContent = formatTicks(ticks_);
  return node;
}

/**
 * The id an ingredient refers to. Ingredients are inlined in the JSON, so a
 * plain item is `{item}`, a tag is `{tag}` and a NeoForge component set is
 * `{type: "neoforge:components", items}`.
 */
export function ingredientId(ingredient) {
  if (!ingredient) return null;
  if (typeof ingredient.item === "string") return ingredient.item;
  if (typeof ingredient.items === "string") return ingredient.items;
  if (typeof ingredient.tag === "string") return ingredient.tag;
  return null;
}

export function isTagIngredient(ingredient) {
  return Boolean(ingredient?.tag) || ingredientId(ingredient)?.startsWith("#") === true;
}
