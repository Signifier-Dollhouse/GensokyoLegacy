// Rendering of every datapack node: conditions, requirements, rewards, dialog
// options, and the cards that group them.

import { append, clear, h } from "./dom.js";
import {
  advancementLabel,
  characterLabel,
  effectLabel,
  entityLabel,
  fluidLabel,
  formatTicks,
  ingredientId,
  isTagIngredient,
  itemLabel,
  kindOf,
  label,
  prettify,
} from "./format.js";
import { tr, trPlural } from "./i18n.js";
import { guideLocale, guideSubjects, loadDialog, loadEntityTag, loadItemTag, loadLootTable, store } from "./store.js";

// ---------------------------------------------------------------------------
// fragments
// ---------------------------------------------------------------------------

export function pill(kind, text, title) {
  return h("span", { class: `pill ${kind}`, title, text });
}

/** A character badge: short name, with their full in-game name in the tooltip. */
export function characterPill(entity) {
  return pill("accent", characterLabel(entity), entityLabel(entity));
}

/**
 * An ingredient's display name: an explicit `text` override, the game's name for a
 * plain item, or a marked id for a tag, which the game never translates.
 */
function ingredientName(ingredient) {
  if (ingredient?.text) return ingredient.text;
  const id = ingredientId(ingredient);
  return isTagIngredient(ingredient) ? `#${prettify(id)}` : itemLabel(id);
}

/** An item or item tag, with its required count. */
function itemPill(ingredient) {
  const node = pill("item", "", ingredientId(ingredient));
  if (isTagIngredient(ingredient)) node.classList.add("tag");
  append(node, [
    ingredientName(ingredient),
    ingredient.count !== undefined ? " " : "",
    ingredient.count !== undefined ? h("span", { class: "n", text: `x${ingredient.count}` }) : null,
  ]);
  return node;
}

export function ingredientList(ingredients) {
  return h("div", { class: "pill-stack" }, ...(ingredients ?? []).map(itemPill));
}

/** A stat label paired with its value - the shape of every list row. */
function entry(kind, ...body) {
  return h(
    "li",
    {},
    h("div", { class: "entry" }, h("span", { class: "entry-kind", text: kind }), h("div", { class: "entry-body" }, ...body)),
  );
}

/** A section with a heading, containing `entry` rows. */
export function section(name, ...rows) {
  if (!rows.length) return null;
  return h("section", {}, h("p", { class: "section-label", text: name }), h("ul", { class: "list" }, ...rows));
}

/**
 * The summary of a collapsible section: the marker plus the label. The marker is a
 * real character rather than a list marker, so the row height stays the same open or
 * closed and the rotation can be animated.
 */
function collapsibleHead(name) {
  return h("summary", { class: "section-label" }, h("span", { class: "marker", text: "▸" }), name);
}

/**
 * The same rows under a heading that folds away. Expanded by default, since a card
 * is mostly read by scrolling past it; the marker comes from the stylesheet so the
 * summary keeps the quiet look of an inline section label.
 */
export function collapsibleSection(name, ...rows) {
  if (!rows.length) return null;
  return h("details", { class: "section", open: true }, collapsibleHead(name), h("ul", { class: "list" }, ...rows));
}

/** As `collapsibleSection`, but for prose rather than rows - a guide entry's pages. */
export function collapsibleBlock(name, ...blocks) {
  const content = blocks.filter(Boolean);
  if (!content.length) return null;
  return h("details", { class: "section", open: true }, collapsibleHead(name), ...content);
}

/**
 * A collapsible group of cards or rows, labelled with how many it holds. Native
 * `<details>` so it opens, closes and reports its state without any wiring, and it
 * is dropped entirely when empty rather than showing an empty heading.
 */
export function collapsible(name, count, content, className = "grid") {
  if (!count) return null;
  return h(
    "details",
    { class: "collapse", open: true },
    h("summary", {}, h("span", { text: name }), h("span", { class: "count", text: String(count) })),
    h("div", { class: className }, content),
  );
}

function code(value) {
  return h("code", { class: "mono", text: value });
}

/** A button that filters the current tab down to one entry. */
function focusLink(text, onClick, title) {
  return h("button", { class: "linkish", type: "button", title, onclick: onClick, text });
}

function rawJson(file, data) {
  return h(
    "details",
    { class: "raw" },
    h("summary", { text: tr("raw.summary") }),
    h("pre", { text: JSON.stringify(data, null, 2) }),
    h(
      "p",
      { class: "entry-note" },
      tr("raw.loadedFrom"),
      h("a", { href: file, target: "_blank", rel: "noopener", text: file.replace(/^src\/generated\/resources\//, "") }),
    ),
  );
}

function cardHead(title, id, badges) {
  return h(
    "div",
    { class: "card-head" },
    h("div", {}, h("h3", { text: title }), h("p", { class: "card-id mono", text: id })),
    badges?.length ? h("div", { class: "badges" }, ...badges) : null,
  );
}

// ---------------------------------------------------------------------------
// loot table
// ---------------------------------------------------------------------------

/**
 * How many of a drop a single roll yields. Datagen writes an exact count as a bare
 * number and a range as a min/max provider, so both shapes have to be read; `null`
 * when the entry says nothing, which means a single item.
 */
function dropCount(drop) {
  const setCount = (drop.functions ?? []).find((fn) => fn.function === "minecraft:set_count");
  const count = setCount?.count;
  if (typeof count === "number") return String(Math.floor(count));
  if (count?.min !== undefined && count?.max !== undefined) {
    return `${Math.ceil(count.min)}-${Math.floor(count.max)}`;
  }
  return count?.value !== undefined ? String(Math.floor(count.value)) : null;
}

/** Reduces a loot table to the part a player cares about: rolls and possible drops. */
function lootNode(id) {
  const list = h("ul", { class: "list" }, h("li", { class: "entry-note", text: tr("loot.loading") }));
  const node = h("div", { class: "weights" }, code(id), list);

  loadLootTable(id).then((table) => {
    clear(list);
    if (!table) {
      list.append(h("li", { class: "entry-note", text: tr("loot.failed", id) }));
      return;
    }
    for (const [index, pool] of (table.pools ?? []).entries()) {
      const rolls = Math.round(pool.rolls ?? 0) + Math.round(pool.bonus_rolls ?? 0);
      const drops = (pool.entries ?? []).map((drop) => {
        const range = dropCount(drop);
        const node = pill("item", itemLabel(drop.name), range ? tr("loot.perDrop", range) : (drop.name ?? ""));
        // The count is the whole point of a drop, so it is shown rather than hidden
        // in the tooltip; a pool without one always yields a single item.
        if (range) append(node, [h("span", { class: "n", text: ` x${range}` })]);
        return node;
      });
      list.append(
        entry(
          tr("loot.pool", index + 1),
          h("div", { text: tr("loot.rolls", rolls) }),
          h("div", { class: "pill-stack" }, ...drops),
        ),
      );
    }
  });

  return node;
}

// ---------------------------------------------------------------------------
// conditions
// ---------------------------------------------------------------------------

/** Renders an `EntityPredicate`: either one entity type or a `#tag` of them. */
function entityTarget(target) {
  const id = target?.type;
  if (typeof id !== "string") return pill("item", tr("tag.unknownTarget"));
  if (!id.startsWith("#")) return pill("item", entityLabel(id), id);
  const known = Boolean(store.manifest.entityTags[id]);
  return focusLink(
    `#${prettify(id)}`,
    () => showEntityTag(id),
    known ? tr("tag.showMembers") : tr("tag.vanilla", id),
  );
}

/** Opens a dialog listing the members of an entity type tag. */
async function showEntityTag(id) {
  const members = await loadEntityTag(id);
  openViewer(
    `#${prettify(id)}`,
    members
      ? h("ul", { class: "list" }, ...[...members].map((value) => entry("", pill("item", entityLabel(value), value))))
      : h("p", { class: "entry-note", text: tr("tag.baseMod") }),
  );
}

export function conditionNode(condition, onQuestLink) {
  switch (kindOf(condition)) {
    case "has_quest_completed": {
      const quest = store.quests.get(condition.quest);
      const text = quest?.title ? label(quest.title) : condition.quest;
      return entry(
        tr("cond.quest"),
        // A link is only useful where a quest list is on screen to link to.
        onQuestLink && quest?.title
          ? focusLink(text, () => onQuestLink(condition.quest), condition.quest)
          : h("span", { title: condition.quest, text }),
      );
    }
    case "has_advancement":
      return entry(
        condition.invert ? tr("cond.notYet") : tr("cond.advancement"),
        h("span", { text: advancementLabel(condition.advancement) }),
        h("div", { class: "entry-note" }, code(condition.advancement)),
      );
    case "has_item":
      return entry(tr("cond.items"), ingredientList(condition.ingredients));
    case "timer":
      return entry(
        condition.invert ? tr("cond.ready") : tr("cond.cooldown"),
        code(condition.key),
        condition.invert ? null : h("span", { class: "entry-note", text: tr("cond.elapsed") }),
      );
    case "self_reputation":
      return entry(
        tr("cond.reputation"),
        h("span", {
          text: condition.invert
            ? tr("cond.reputation.below", condition.reputation)
            : tr("cond.reputation.atLeast", condition.reputation),
        }),
        h("span", { class: "entry-note", text: tr("cond.reputation.withCharacter") }),
      );
    case "other_reputation":
      return entry(
        tr("cond.reputation"),
        h("span", { text: tr("cond.reputation.atLeast", condition.reputation) }),
        h("span", { class: "entry-note", text: tr("cond.reputation.with", characterLabel(condition.character)) }),
      );
    case "any":
      return h(
        "li",
        {},
        h("div", { class: "entry" }, h("span", { class: "entry-kind", text: tr("cond.anyOf") })),
        h("ul", { class: "nested" }, ...(condition.conditions ?? []).map((node) => conditionNode(node, onQuestLink))),
      );
    default:
      return entry(tr("cond.generic"), code(condition.type));
  }
}

function conditionRows(conditions, onQuestLink) {
  return (conditions ?? []).map((condition) => conditionNode(condition, onQuestLink));
}

/** Conditions as a collapsible block, for the cards. */
export function conditionsBlock(conditions, onQuestLink) {
  return collapsibleSection(tr("section.conditions"), ...conditionRows(conditions, onQuestLink));
}

/** Conditions laid out inline, for a dialog option, which is already a small block. */
export function conditionsNode(conditions, onQuestLink) {
  return section(tr("section.conditions"), ...conditionRows(conditions, onQuestLink));
}

// ---------------------------------------------------------------------------
// requirements
// ---------------------------------------------------------------------------

export function requirementNode(key, requirement) {
  switch (kindOf(requirement)) {
    case "submit_item":
      return entry(key || tr("req.submit"), ingredientList(requirement.ingredients));
    case "has_item":
      return entry(key || tr("req.carry"), ingredientList(requirement.ingredients));
    case "kill_mob":
      // The text is the objective as the player sees it ("Exterminate skeletons"),
      // so the entity predicate is shown alongside it as the target list.
      return entry(
        key || tr("req.kill"),
        h("span", { text: requirement.text ? label(requirement.text) : tr("req.defeat") }),
        h("span", { class: "n", text: ` x${requirement.count}` }),
        " ",
        entityTarget(requirement.target),
      );
    case "kill_enemy":
      return entry(
        key || tr("req.kill"),
        h("span", { text: requirement.text ? label(requirement.text) : tr("req.defeatEnemies") }),
        h("span", { class: "n", text: ` x${requirement.count}` }),
      );
    case "raid_victory":
      return entry(
        key || tr("req.raid"),
        h("span", { text: requirement.text ? label(requirement.text) : tr("req.winRaid") }),
        h("span", { class: "n", text: ` x${requirement.count}` }),
      );
    case "roll_item":
      return h(
        "li",
        {},
        h("div", { class: "entry" }, h("span", { class: "entry-kind", text: key || tr("req.randomLoot") })),
        lootNode(requirement.table),
      );
    case "koishi_hat":
      // KoishiHatRequirement#getDesc returns nothing, so this objective is never
      // shown in game: it completes as a side effect of picking up the hat.
      return entry(
        key || tr("req.hidden"),
        h("span", { text: tr("req.koishiHat", itemLabel("gensokyolegacy:koishi_hat")) }),
        h("div", { class: "entry-note", text: tr("req.hiddenNote") }),
      );
    default:
      return entry(key || tr("req.generic"), code(requirement.type));
  }
}

export function requirementsNode(requirements) {
  const entries = Object.entries(requirements ?? {});
  return collapsibleSection(
    tr("section.requirements"),
    ...entries.map(([key, value]) => requirementNode(key, value)),
  );
}

// ---------------------------------------------------------------------------
// rewards
// ---------------------------------------------------------------------------

export function rewardNode(reward) {
  switch (kindOf(reward)) {
    case "exp":
      return entry(tr("reward.exp"), h("span", { text: tr("reward.expValue", reward.point) }));
    case "reputation":
      return entry(
        tr("reward.reputation"),
        h("span", { text: `+${reward.reputation}` }),
        h("span", {
          class: "entry-note",
          text: tr("reward.reputationDetail", reward.soft_cap, reward.cap_increase, reward.max_cap),
        }),
      );
    case "loot_table":
      return h(
        "li",
        {},
        h("div", { class: "entry" }, h("span", { class: "entry-kind", text: tr("reward.loot") })),
        lootNode(reward.table),
      );
    default:
      return entry(tr("reward.generic"), code(reward.type));
  }
}

export function rewardsNode(rewards) {
  return collapsibleSection(tr("section.rewards"), ...(rewards ?? []).map(rewardNode));
}

// ---------------------------------------------------------------------------
// dialog options
// ---------------------------------------------------------------------------

function actionsNode(actions) {
  if (!actions?.length) return null;
  return h(
    "div",
    { class: "pill-stack" },
    ...actions.map((action) => {
      switch (kindOf(action)) {
        case "start_quest":
          return pill("reward", tr("action.startQuest"));
        case "complete_quest":
          return pill("reward", tr("action.completeQuest"));
        case "give_mob_effect": {
          const amplifier = action.amplifier ? ` ${"I".repeat(action.amplifier + 1)}` : "";
          return pill(
            "condition",
            `${effectLabel(action.effect)} ${formatTicks(action.duration)}${amplifier}`,
            action.effect,
          );
        }
        case "set_timer":
          return pill("condition", `${action.key} ${formatTicks(action.delay)}`);
        default:
          return pill("condition", action.type);
      }
    }),
  );
}

/** One dialog option. `onNext` follows the option's `next` link. */
function optionNode(option, onNext) {
  const group = kindOf(option) === "group" ? label(option.group) : null;
  const next = typeof option.next === "string" ? option.next : null;
  const continueButton = () =>
    h("button", { class: "linkish", type: "button", text: tr("option.continue"), onclick: () => onNext(next) });

  return h(
    "div",
    { class: "option" },
    h(
      "div",
      { style: "flex:1;min-width:0" },
      group ? h("div", { class: "option-group", text: group }) : null,
      h("div", { class: "option-text", text: label(option.text) }),
      h(
        "div",
        { class: "option-meta" },
        option.conditions?.length ? pill("condition", tr("option.conditions", option.conditions.length)) : null,
        option.actions?.length ? pill("reward", tr("option.actions", option.actions.length)) : null,
      ),
      conditionsNode(option.conditions, null),
      actionsNode(option.actions),
      kindOf(option) === "random"
        ? h(
            "ul",
            { class: "list weights" },
            ...(option.entries ?? []).map((entry) =>
              h(
                "li",
                { class: "starter-row" },
                pill("requirement", tr("option.weight", entry.weight)),
                actionsNode(entry.actions),
                typeof entry.next === "string"
                  ? h("button", { class: "linkish", type: "button", text: tr("option.continue"), onclick: () => onNext(entry.next) })
                  : h("span", { class: "entry-note", text: tr("option.endsHere") }),
              ),
            ),
          )
        : null,
    ),
    next ? continueButton() : null,
  );
}

// ---------------------------------------------------------------------------
// modal viewer
// ---------------------------------------------------------------------------

/**
 * The dialog element, created if the page did not provide one. Resolving it
 * lazily keeps this module usable on its own: an import-time failure here would
 * take every view down with it.
 */
function viewerElement() {
  let element = document.querySelector("#viewer");
  if (element) return element;
  element = h("dialog", { id: "viewer", class: "viewer", "aria-label": tr("a11y.detail") });
  document.body.append(element);
  return element;
}

const viewerTitle = h("h2", {});
const viewerBody = h("div", { class: "viewer-body" });
let trail = [];

function openViewer(title, ...content) {
  const element = viewerElement();
  viewerTitle.textContent = title;
  append(clear(viewerBody), content);
  trail = [];
  if (!element.open) element.showModal();
}

export function closeViewer() {
  const element = viewerElement();
  if (element.open) element.close();
  trail = [];
}

/** Builds the dialog chrome once, on first use. */
function mountViewer() {
  const element = viewerElement();
  if (element.dataset.mounted) return element;
  element.dataset.mounted = "true";
  element.append(
    h(
      "div",
      { class: "viewer-head" },
      viewerTitle,
      h(
        "div",
        { class: "option-meta" },
        // `data-i18n` keeps the chrome translated if the language changes while
        // the viewer is open, since the head is only built once.
        h("button", {
          class: "linkish",
          type: "button",
          "data-i18n": "viewer.back",
          text: tr("viewer.back"),
          onclick: () => (trail.length > 1 ? renderTrail() : closeViewer()),
        }),
        h("button", {
          class: "linkish",
          type: "button",
          "data-i18n": "viewer.close",
          text: tr("viewer.close"),
          onclick: closeViewer,
        }),
      ),
    ),
    viewerBody,
  );
  return element;
}

/** Walks the dialog graph from `startId`, keeping a trail for the back button. */
export function openDialogViewer(startId) {
  const element = mountViewer();
  trail = [startId];
  if (!element.open) element.showModal();
  renderTrail();
}

function renderTrail() {
  const id = trail.at(-1);
  viewerTitle.textContent = prettify(id.split("/").pop());
  clear(viewerBody);

  loadDialog(id).then((dialog) => {
    if (trail.at(-1) !== id) return; // navigated away while loading
    if (!dialog) {
      viewerBody.append(h("p", { class: "empty", text: tr("dialog.failed", id) }));
      return;
    }

    const go = (next) => {
      trail.push(next);
      renderTrail();
    };

    append(viewerBody, [
      h("p", { class: "dialog-line", text: label(dialog.text) }),
      h("p", { class: "card-id mono", text: id }),
      dialog.animations?.length
        ? h("div", { class: "pill-stack" }, ...dialog.animations.map((animation) => pill("condition", animation)))
        : null,
      dialog.options?.length
        ? h("div", { class: "option-list" }, ...dialog.options.map((option) => optionNode(option, go)))
        : h("p", { class: "entry-note", text: tr("dialog.end") }),
    ]);
  });
}

/** Opens the members of an item tag. */
export async function showItemTag(id) {
  const members = await loadItemTag(id);
  openViewer(
    `#${prettify(id)}`,
    members
      ? h(
          "ul",
          { class: "list" },
          ...[...members].map((value) =>
            entry(
              "",
              pill("item", itemLabel(value), value),
              members.size > 1 ? h("span", { class: "entry-note", text: tr("tag.others", members.size - 1) }) : null,
            ),
          ),
        )
      : h("p", { class: "entry-note", text: tr("tag.baseMod") }),
  );
}

// ---------------------------------------------------------------------------
// cards
// ---------------------------------------------------------------------------

export function questCard(entryData, quest, onQuestLink) {
  const points = [
    [tr("quest.offer"), quest.initialDialog],
    [tr("quest.followUp"), quest.followUpDialog],
    [tr("quest.handIn"), quest.completionDialog],
  ].filter(([, option]) => option);

  return h(
    "article",
    { class: "card" },
    cardHead(quest.title ? label(quest.title) : entryData.id, entryData.id, [
      quest.recurrence
        ? pill("requirement", tr("quest.cooldown", formatTicks(quest.recurrence.cooldown)))
        : pill("condition", tr("quest.oneTime")),
      characterPill(quest.character),
    ]),
    quest.description ? h("p", { class: "card-desc", text: label(quest.description) }) : null,
    conditionsBlock(quest.conditions, onQuestLink),
    requirementsNode(quest.requirements),
    rewardsNode(quest.rewards),
    points.length
      ? section(
          tr("section.conversation"),
          ...points.map(([name, option]) =>
            entry(
              name,
              h("span", { text: label(option.text) }),
              typeof option.next === "string"
                ? h("button", {
                    class: "linkish",
                    type: "button",
                    text: tr("quest.openDialog"),
                    onclick: () => openDialogViewer(option.next),
                  })
                : null,
            ),
          ),
        )
      : null,
    rawJson(entryData.file, quest),
  );
}

/**
 * Classifies an offer the way the trade screen does.
 *
 * `TradeOffer#isSellOffer` answers "does this hand out a non-currency item?", and
 * `TradeScreen#actionText` then splits that into three cases:
 *   - not a sell offer                      -> the player hands over the ingredients
 *   - sell offer, an ingredient is currency  -> the player pays currency
 *   - sell offer, otherwise                  -> the character makes it
 */
export function tradeKind(trade) {
  const currency = store.tags.get("gensokyolegacy:currency");
  const isCurrency = (id) =>
    Boolean(currency?.has(id) ?? (id === "minecraft:emerald" || id === "minecraft:gold_ingot"));

  const resultIsCurrency = isCurrency(trade.result?.id);
  const single = (trade.ingredients?.length ?? 0) === 1 ? trade.ingredients[0] : null;
  const ingredientIdOfSingle = ingredientId(single);

  // TradeOffer#isSellOffer(): "does this hand out something that is not currency?"
  const isSellOffer =
    !resultIsCurrency || !single
      ? true
      : isCurrency(ingredientIdOfSingle) && single.count > trade.result.count;

  if (!isSellOffer) return "sell";
  return (trade.ingredients ?? []).some((ingredient) => isCurrency(ingredientId(ingredient))) ? "buy" : "craft";
}

// The card title for each kind of offer, taking the item the trade is about.
// Resolved on every render so that a language switch is picked up without
// rebuilding the card.
const TRADE_TITLES = {
  sell: (item) => tr("trade.title.sell", item),
  buy: (item) => tr("trade.title.buy", item),
  craft: (item) => tr("trade.title.craft", item),
};

/** Section heading for a kind of offer; used to group the cards. */
export function tradeTitle(kind) {
  return tr(`trade.group.${kind}`);
}

/**
 * The item a card is about. Selling hands the ingredients over, so the interesting
 * item is the one going in; buying and crafting are about what comes out. Note that
 * a result is spelled `{id}`, unlike an ingredient's `{item}`.
 */
function tradeItemOfInterest(trade, kind) {
  if (kind === "sell") return ingredientName(trade.ingredients?.[0]);
  return trade.result?.text ?? itemLabel(trade.result?.id);
}

export function tradeCard(entryData, trade) {
  const kind = tradeKind(trade);
  const stock = trade.recurrence?.maxStock;
  const restock = trade.recurrence?.restockTime;

  return h(
    "article",
    { class: "card" },
    cardHead(TRADE_TITLES[kind](tradeItemOfInterest(trade, kind)), entryData.id, [
      characterPill(trade.character),
      stock ? pill("requirement", tr("trade.stock", stock)) : null,
      restock ? pill("condition", tr("trade.restock", formatTicks(restock))) : null,
    ]),
    h(
      "div",
      { class: "trade-flow" },
      ingredientList(trade.ingredients),
      h("span", { class: "arrow", text: "->" }),
      pill("reward", itemLabel(trade.result?.id), trade.result?.id),
      h("span", { class: "n", text: `x${trade.result?.count ?? 1}` }),
    ),
    conditionsBlock(trade.conditions, null),
    rawJson(entryData.file, trade),
  );
}

export function starterCard(entryData, starter) {
  return h(
    "article",
    { class: "card" },
    cardHead(label(starter.text), entryData.id, [
      characterPill(starter.character),
      starter.weight !== undefined ? pill("requirement", tr("option.weight", starter.weight)) : null,
    ]),
    h(
      "div",
      { class: "starter-row" },
      typeof starter.dialog === "string"
        ? h("button", {
            class: "linkish",
            type: "button",
            text: tr("dialog.startConversation"),
            onclick: () => openDialogViewer(starter.dialog),
          })
        : null,
    ),
    conditionsBlock(starter.conditions, null),
    rawJson(entryData.file, starter),
  );
}

// ---------------------------------------------------------------------------
// the item section
// ---------------------------------------------------------------------------

/** A fluid as it appears in the alchemy pot, which is not yet an item. */
function fluidPill(id) {
  return pill("fluid", fluidLabel(id), id);
}

/** How many ways there are to get an item, as one number. */
function sourceCount(item) {
  return item.recipes.length + item.trades.length + item.drops.length;
}

/**
 * One row of the item list. The detail is a page away rather than inline, since there
 * are hundreds of items and only one of them is being read.
 */
export function itemRow(item, onOpen) {
  const ways = sourceCount(item);
  return h(
    "li",
    { class: "starter-row" },
    h("button", { class: "linkish", type: "button", text: itemLabel(item.id), title: item.id, onclick: onOpen }),
    h("span", { class: "card-id mono", text: item.id }),
    ways ? h("span", { class: "entry-note", text: trPlural("item.ways", ways, ways) }) : null,
  );
}

/** The name of the guide category an entry sits in, which the book spells out. */
export function guideCategoryName(guide) {
  return categoryName(guide.book, guide.category);
}

/** A category's own name, read from the book's category file. */
export function categoryName(book, id) {
  return guideLocale(book).categories.get(id)?.data?.name ?? prettify(id);
}

/** A category's position in the book, which is the order the item groups follow. */
export function guideSortnum(book, id) {
  return guideLocale(book).categories.get(id)?.data?.sortnum ?? Infinity;
}

/**
 * An entry's page in the active language. The files are per locale, so the text is
 * looked up rather than kept, and an entry a translation has not caught up with falls
 * back to the English one instead of disappearing.
 */
function guideEntry(guide) {
  const locales = Object.values(guide.book.locales);
  return (
    guideLocale(guide.book).entries.get(guide.id) ??
    locales.find((locale) => locale.entries.has(guide.id))?.entries.get(guide.id)
  );
}

/** The entry's own title, which the book spells out per locale. */
export function guideEntryName(guide) {
  return guideEntry(guide)?.data?.name ?? prettify(guide.id);
}

/**
 * The item page: the guide entry that documents it, then every way to get one. The
 * links come from the caller, since jumping to a quest or an offer is a navigation
 * the panels own rather than something this module can do on its own.
 */
export function openItemViewer(id, links = {}) {
  const item = store.items.get(id);
  if (!item) return;
  mountViewer();
  openViewer(
    itemLabel(id),
    h("p", { class: "card-id mono", text: id }),
    item.guide ? guideSection(item) : h("p", { class: "entry-note", text: tr("item.noGuide") }),
    sourcesSection(item, links),
  );
}

function guideSection(item) {
  const { book, id, subject, tag } = item.guide;
  const page = guideEntry(item.guide);
  const data = page?.data ?? {};
  // Resolved here rather than stored, so the prose follows the language toggle.
  const { shared, entries } = guideSubjects(data);
  const pages = [...shared, ...(entries.get(subject)?.pages ?? [])];
  return h(
    "section",
    { class: "guide" },
    h("p", { class: "section-label", text: tr("item.category") }),
    h("p", { class: "entry-note", text: guideCategoryName(item.guide) }),
    // A tag spotlight documents every member at once, so the item's page says which
    // group it was documented under rather than implying the page is only about it.
    tag
      ? h(
          "p",
          { class: "entry-note" },
          tr("item.guideTag"),
          pill("item tag", `#${prettify(tag)}`, tag),
          focusLink(tr("tag.showMembers"), () => showItemTag(tag), tag),
        )
      : null,
    data.advancement
      ? h("p", { class: "entry-note" }, `${tr("item.advancement")}: ${advancementLabel(data.advancement)}`)
      : null,
    // The prose folds away: it is the longest thing on the page, and the sources
    // below it are what a reader usually came for.
    collapsibleBlock(tr("item.guide"), ...pages.map((block) => guideBlock(block, item.id))),
    rawJson(page?.file, data),
    h("p", { class: "entry-note" }, `${label(book.definition?.name)} · ${id}`),
  );
}

/** A spotlight introduces an item with a title; a text page is just prose. */
function guideBlock(page, itemId) {
  if (page.type !== "patchouli:spotlight") return guideText(page.text, itemId);
  return h(
    "div",
    { class: "guide-spotlight" },
    guideText(page.text, page.item ?? itemId),
    page.title ? h("p", { class: "guide-title", text: page.title }) : null,
  );
}

/** Splits on a capturing group, so odd indices are the macro names. */
const MACRO = /\$\(([a-z0-9_]*)\)/i;

/**
 * A guide page's text. Patchouli's macros are inline commands rather than markup:
 * `$(bold)` turns bold on and the `$()` after it turns bold off again, `$(br)` and
 * `$(br2)` break the line. Anything unrecognised is left exactly as written, so a
 * macro this renderer does not know shows up rather than silently disappearing.
 */
function guideText(text, itemId) {
  const node = h("p", { class: "guide-text" });
  const styles = []; // the open <b> elements, innermost last; text lands in the last
  const write = (value) => (styles.at(-1) ?? node).append(document.createTextNode(value));
  const here = () => styles.at(-1) ?? node;

  for (const [index, part] of String(text ?? "").split(MACRO).entries()) {
    if (index % 2 === 0) {
      if (part) write(part);
      continue;
    }
    switch (part.toLowerCase()) {
      case "bold":
        styles.push(h("b", {}));
        node.append(styles.at(-1));
        break;
      case "":
        styles.pop();
        break;
      case "br":
        here().append(h("br"));
        break;
      case "br2":
        here().append(h("br"), h("br"));
        break;
      case "item":
      case "thing":
        write(itemLabel(itemId));
        break;
      default:
        write(`$(${part})`);
    }
  }
  return node;
}

/** Every way to get the item, one collapsible section per kind of source. */
function sourcesSection(item, links) {
  const sections = [
    [tr("item.source.recipe"), item.recipes.map(recipeRow)],
    [tr("item.source.trade"), item.trades.map((source) => tradeSourceRow(source, links))],
    [tr("item.source.quest"), item.drops.map((drop) => dropSourceRow(drop, links))],
  ].filter(([, rows]) => rows.length);

  return section(
    tr("item.sources"),
    ...(sections.length
      ? sections.map(([name, rows]) => collapsibleSection(name, ...rows))
      : [entry("", h("span", { class: "entry-note", text: tr("item.source.none") }))]),
  );
}

/** One recipe: what it takes, what it makes, and how long a brew takes. */
function recipeRow(source) {
  const recipe = source.recipe;
  return entry(
    tr(`recipe.type.${kindOf(recipe)}`),
    h(
      "div",
      { class: "trade-flow" },
      recipeInputs(recipe),
      h("span", { class: "arrow", text: "->" }),
      recipeOutput(recipe),
    ),
    recipe.time ? h("span", { class: "entry-note", text: tr("item.perBrew", formatTicks(recipe.time)) }) : null,
    rawJson(source.file, recipe),
  );
}

/**
 * What a recipe consumes. A shaped recipe is drawn as its grid, since the layout is
 * part of the recipe; everything else is listed as it is written, which is the order
 * the recipe book shows. Fluids are pills of their own - the alchemy pot and the
 * brewing stand work in fluid, not in items.
 */
function recipeInputs(recipe) {
  if (kindOf(recipe) === "crafting_shaped") return shapedGrid(recipe);

  const inputs = h("div", { class: "pill-stack" });
  const add = (value) => {
    if (!value || typeof value !== "object") return;
    if (typeof value.fluid === "string") inputs.append(fluidPill(value.fluid));
    else inputs.append(itemPill(value));
  };
  const addAll = (value) => (Array.isArray(value) ? value : [value]).filter(Boolean).forEach(add);
  // `inputFluid` is a list in one recipe type and a single fluid in the others.
  const addFluid = (value) => addAll(Array.isArray(value) ? value : value?.fluid ? [value] : []);

  switch (kindOf(recipe)) {
    case "crafting_shapeless":
      addAll(recipe.ingredients);
      break;
    case "stonecutting":
      add(recipe.ingredient);
      break;
    case "unordered_alchemy":
      addFluid(recipe.inputFluid);
      addAll(recipe.input);
      break;
    case "witch_enhance":
      addFluid(recipe.inputFluid);
      addAll(recipe.extra);
      break;
    case "witch_merge":
      addFluid(recipe.inputFluid);
      addAll(recipe.extra);
      if (recipe.potionIngredient) add({ ...recipe.potionIngredient, count: recipe.potionCount });
      break;
    default:
      addFluid(recipe.inputFluid);
      addAll(recipe.ingredients);
  }
  return inputs;
}

/**
 * A shaped recipe as the crafting grid shows it: the pattern as symbols, with a key
 * beneath naming what each one stands for. A grid of full ingredient names would be
 * unreadable at three columns, and the layout is the part of the recipe that matters.
 */
function shapedGrid(recipe) {
  const rows = recipe.pattern ?? [];
  const width = Math.max(1, ...rows.map((row) => row.length));
  const grid = h("div", { class: "recipe-grid", style: `grid-template-columns:repeat(${width},22px)` });
  for (const row of rows) {
    for (let column = 0; column < width; column += 1) {
      grid.append(h("span", { class: "cell", text: row[column] ?? " " }));
    }
  }

  const key = h("div", { class: "pill-stack" });
  for (const [symbol, ingredient] of Object.entries(recipe.key ?? {})) {
    key.append(h("span", { class: "recipe-key" }, h("code", { class: "mono", text: symbol }), itemPill(ingredient)));
  }
  return h("div", { class: "recipe-shaped" }, grid, key);
}

/** What a recipe makes: an item for the crafting types, a fluid for the brewing ones. */
function recipeOutput(recipe) {
  if (recipe.result?.id) {
    const node = pill("reward", itemLabel(recipe.result.id), recipe.result.id);
    if (recipe.result.count > 1) node.append(h("span", { class: "n", text: ` x${recipe.result.count}` }));
    return node;
  }
  if (recipe.resultFluid?.id) return pill("reward fluid", fluidLabel(recipe.resultFluid.id), recipe.resultFluid.id);
  return pill("condition", prettify(recipe.type));
}

/** An offer that hands the item over, with what the player has to pay. */
function tradeSourceRow(source, links) {
  const trade = source.trade;
  const kind = tradeKind(trade);
  return entry(
    characterLabel(trade.character),
    h(
      "div",
      { class: "trade-flow" },
      h("span", { class: "entry-note", text: tr("item.pay") }),
      ingredientList(trade.ingredients),
      h("span", { class: "arrow", text: "->" }),
      pill("reward", itemLabel(trade.result?.id), trade.result?.id),
    ),
    trade.recurrence?.maxStock ? pill("requirement", tr("item.stock", trade.recurrence.maxStock)) : null,
    trade.recurrence?.restockTime ? pill("condition", tr("item.restock", formatTicks(trade.recurrence.restockTime))) : null,
    links.trade
      ? focusLink(TRADE_TITLES[kind](tradeItemOfInterest(trade, kind)), () => links.trade(source.id), source.id)
      : null,
  );
}

/** A quest whose reward table can drop the item. */
function dropSourceRow(drop, links) {
  const quest = store.quests.get(drop.questId);
  const range = dropCount(drop.entry);
  const node = pill("item", itemLabel(drop.item), drop.item);
  if (range) node.append(h("span", { class: "n", text: ` x${range}` }));
  return entry(
    quest?.title ? label(quest.title) : drop.questId,
    node,
    h("span", { class: "entry-note", text: tr("loot.rolls", Math.round(drop.rolls)) }),
    links.quest ? focusLink(tr("item.openQuest"), () => links.quest(drop.questId), drop.questId) : null,
  );
}

