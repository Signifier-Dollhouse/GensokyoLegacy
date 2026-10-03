// Translation, naming and unit formatting.
//
// `label`/`itemLabel` resolve the mod's own lang files (quest, dialog, item and
// entity text); the words the site puts around that content come from `tr`.

import { tr, trOrNull } from "./i18n.js";
import { characterSlugOf, store } from "./store.js";
import { state } from "./state.js";

/**
 * Looks a translation key up in the active locale, falling back to English. The
 * mod's lang files and the vanilla tables are consulted together: they never
 * overlap, but only the pair can name `minecraft:iron_ingot` or a vanilla
 * advancement, neither of which the mod translates.
 */
export function t(key) {
  if (typeof key !== "string") return null;
  for (const locale of [state.lang, "en_us"]) {
    for (const table of [store.lang.get(locale), store.vanillaLang.get(locale)]) {
      if (table && key in table) return table[key];
    }
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

/**
 * A namespaced id as the dotted path a lang key uses: `minecraft:iron_ingot` is
 * `item.minecraft.iron_ingot`. Minecraft splits ids on `:` but lang keys on `.`,
 * so every lookup has to convert first.
 */
function langPath(id) {
  return String(id).replace(":", ".");
}

/** Item display name, checking the item, block and entity namespaces in turn. */
export function itemLabel(id) {
  if (!id) return "";
  const bare = langPath(id.replace(/^#/, ""));
  return t(`item.${bare}`) ?? t(`block.${bare}`) ?? t(`entity.${bare}`) ?? prettify(id);
}

export function entityLabel(id) {
  return id ? (t(`entity.${langPath(id)}`) ?? prettify(id)) : "";
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

/**
 * Advancement title. The lang key is a dotted path rather than the id: the
 * namespace is dropped for vanilla advancements (`nether/root` is
 * `advancements.nether.root.title`) but kept for modded ones, so both spellings
 * are tried.
 */
export function advancementLabel(id) {
  const [namespace, path = ""] = String(id).split(":");
  const dotted = path.replaceAll("/", ".");
  return (
    t(`advancements.${dotted}.title`) ?? t(`advancements.${namespace}.${dotted}.title`) ?? prettify(dotted)
  );
}

/** Mob effect name, used by the `give_mob_effect` dialog actions. */
export function effectLabel(id) {
  return t(`effect.${langPath(id)}`) ?? prettify(id);
}

/**
 * Fluid name, for the alchemy pot and brewing recipes. A fluid is its own lang key
 * family, but vanilla names water and lava as blocks, so those are tried too.
 */
export function fluidLabel(id) {
  if (!id) return "";
  return t(`fluid.${langPath(id)}`) ?? itemLabel(id);
}

/**
 * The mod's own name, translated by the game rather than by the interface tables:
 * `gensokyolegacy.title`. Falls back to the English name before the mod's lang files
 * have loaded, which is what the static markup says anyway.
 */
export function modName() {
  return t(`${store.manifest?.namespace ?? "gensokyolegacy"}.title`) ?? "Gensokyo Legacy";
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
