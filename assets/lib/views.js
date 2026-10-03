// Rendering of every datapack node: conditions, requirements, rewards, dialog
// options, and the cards that group them.

import { append, clear, h } from "./dom.js";
import {
  advancementLabel,
  entityLabel,
  formatTicks,
  ingredientId,
  isTagIngredient,
  itemLabel,
  kindOf,
  label,
  prettify,
} from "./format.js";
import { loadDialog, loadEntityTag, loadItemTag, loadLootTable, store } from "./store.js";

// ---------------------------------------------------------------------------
// fragments
// ---------------------------------------------------------------------------

export function pill(kind, text, title) {
  return h("span", { class: `pill ${kind}`, title, text });
}

/** An item or item tag, with its required count. */
function itemPill(ingredient) {
  const id = ingredientId(ingredient);
  const isTag = isTagIngredient(ingredient);
  const name = ingredient.text ?? itemLabel(id);
  const node = pill("item", "", id);
  if (isTag) node.classList.add("tag");
  append(node, [
    isTag ? `#${prettify(id)}` : name,
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
    h("summary", { text: "Source JSON" }),
    h("pre", { text: JSON.stringify(data, null, 2) }),
    h(
      "p",
      { class: "entry-note" },
      "Loaded from ",
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

/** Reduces a loot table to the part a player cares about: rolls and possible drops. */
function lootNode(id) {
  const list = h("ul", { class: "list" }, h("li", { class: "entry-note", text: "Loading loot table..." }));
  const node = h("div", { class: "weights" }, code(id), list);

  loadLootTable(id).then((table) => {
    clear(list);
    if (!table) {
      list.append(h("li", { class: "entry-note", text: `Loot table ${id} could not be loaded.` }));
      return;
    }
    for (const [index, pool] of (table.pools ?? []).entries()) {
      const rolls = Math.round(pool.rolls ?? 0) + Math.round(pool.bonus_rolls ?? 0);
      const drops = (pool.entries ?? []).map((drop) => {
        const setCount = (drop.functions ?? []).find((fn) => fn.function === "minecraft:set_count");
        const count = setCount?.count;
        const range =
          count?.type === "minecraft:uniform"
            ? `${Math.ceil(count.min ?? 1)}-${Math.floor(count.max ?? 1)}`
            : count?.value !== undefined
              ? String(Math.floor(count.value))
              : null;
        const title = range ? `${count} per drop` : (drop.name ?? "");
        return pill("item", itemLabel(drop.name), title);
      });
      list.append(
        entry(`Pool ${index + 1}`, h("div", { text: `Rolls ${rolls}x` }), h("div", { class: "pill-stack" }, ...drops)),
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
  if (typeof id !== "string") return pill("item", "unknown target");
  if (!id.startsWith("#")) return pill("item", entityLabel(id), id);
  const known = Boolean(store.manifest.entityTags[id]);
  return focusLink(
    `#${prettify(id)}`,
    () => showEntityTag(id),
    known ? "Show members" : `${id} (vanilla tag, not in this repository)`,
  );
}

/** Opens a dialog listing the members of an entity type tag. */
async function showEntityTag(id) {
  const members = await loadEntityTag(id);
  openViewer(
    `#${prettify(id)}`,
    members
      ? h("ul", { class: "list" }, ...[...members].map((value) => entry("", pill("item", entityLabel(value), value))))
      : h("p", { class: "entry-note", text: "This tag belongs to a base mod, so its members are not in this repository." }),
  );
}

export function conditionNode(condition, onQuestLink) {
  switch (kindOf(condition)) {
    case "has_quest_completed": {
      const quest = store.quests.get(condition.quest);
      const text = quest?.title ? label(quest.title) : condition.quest;
      return entry(
        "Quest",
        // A link is only useful where a quest list is on screen to link to.
        onQuestLink && quest?.title
          ? focusLink(text, () => onQuestLink(condition.quest), condition.quest)
          : h("span", { title: condition.quest, text }),
      );
    }
    case "has_advancement":
      return entry(
        condition.invert ? "Not yet" : "Advancement",
        h("span", { text: advancementLabel(condition.advancement) }),
        h("div", { class: "entry-note" }, code(condition.advancement)),
      );
    case "has_item":
      return entry("Items", ingredientList(condition.ingredients));
    case "timer":
      return entry(
        condition.invert ? "Ready" : "Cooldown",
        code(condition.key),
        condition.invert ? null : h("span", { class: "entry-note", text: " must have elapsed" }),
      );
    case "self_reputation":
      return entry(
        "Reputation",
        h("span", { text: `${condition.invert ? "below" : "at least"} ${condition.reputation}` }),
        h("span", { class: "entry-note", text: " with this character" }),
      );
    case "other_reputation":
      return entry(
        "Reputation",
        h("span", { text: `at least ${condition.reputation}` }),
        h("span", { class: "entry-note", text: ` with ${entityLabel(condition.character)}` }),
      );
    case "any":
      return h(
        "li",
        {},
        h("div", { class: "entry" }, h("span", { class: "entry-kind", text: "Any of" })),
        h("ul", { class: "nested" }, ...(condition.conditions ?? []).map((node) => conditionNode(node, onQuestLink))),
      );
    default:
      return entry("Condition", code(condition.type));
  }
}

export function conditionsNode(conditions, onQuestLink) {
  if (!conditions?.length) return null;
  return section("Conditions to unlock", ...conditions.map((condition) => conditionNode(condition, onQuestLink)));
}

// ---------------------------------------------------------------------------
// requirements
// ---------------------------------------------------------------------------

export function requirementNode(key, requirement) {
  switch (kindOf(requirement)) {
    case "submit_item":
      return entry(key || "Submit", ingredientList(requirement.ingredients));
    case "has_item":
      return entry(key || "Carry", ingredientList(requirement.ingredients));
    case "kill_mob":
      // The text is the objective as the player sees it ("Exterminate skeletons"),
      // so the entity predicate is shown alongside it as the target list.
      return entry(
        key || "Kill",
        h("span", { text: requirement.text ? label(requirement.text) : "Defeat" }),
        h("span", { class: "n", text: ` x${requirement.count}` }),
        " ",
        entityTarget(requirement.target),
      );
    case "kill_enemy":
      return entry(
        key || "Kill",
        h("span", { text: requirement.text ? label(requirement.text) : "Defeat enemies" }),
        h("span", { class: "n", text: ` x${requirement.count}` }),
      );
    case "raid_victory":
      return entry(
        key || "Raid",
        h("span", { text: requirement.text ? label(requirement.text) : "Win a raid" }),
        h("span", { class: "n", text: ` x${requirement.count}` }),
      );
    case "roll_item":
      return h(
        "li",
        {},
        h("div", { class: "entry" }, h("span", { class: "entry-kind", text: key || "Random loot" })),
        lootNode(requirement.table),
      );
    case "koishi_hat":
      // KoishiHatRequirement#getDesc returns nothing, so this objective is never
      // shown in game: it completes as a side effect of picking up the hat.
      return entry(
        key || "Hidden",
        h("span", { text: "Obtain the Koishi Hat" }),
        h("div", { class: "entry-note", text: "Hidden objective - no progress is displayed in game." }),
      );
    default:
      return entry(key || "Requirement", code(requirement.type));
  }
}

export function requirementsNode(requirements) {
  const entries = Object.entries(requirements ?? {});
  if (!entries.length) return null;
  return section("Requirements", ...entries.map(([key, value]) => requirementNode(key, value)));
}

// ---------------------------------------------------------------------------
// rewards
// ---------------------------------------------------------------------------

export function rewardNode(reward) {
  switch (kindOf(reward)) {
    case "exp":
      return entry("Experience", h("span", { text: `${reward.point} exp` }));
    case "reputation":
      return entry(
        "Reputation",
        h("span", { text: `+${reward.reputation}` }),
        h("span", {
          class: "entry-note",
          text: ` (soft cap ${reward.soft_cap}, cap +${reward.cap_increase}, max ${reward.max_cap})`,
        }),
      );
    case "loot_table":
      return h(
        "li",
        {},
        h("div", { class: "entry" }, h("span", { class: "entry-kind", text: "Loot" })),
        lootNode(reward.table),
      );
    default:
      return entry("Reward", code(reward.type));
  }
}

export function rewardsNode(rewards) {
  if (!rewards?.length) return null;
  return section("Rewards", ...rewards.map(rewardNode));
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
          return pill("reward", "starts the quest");
        case "complete_quest":
          return pill("reward", "completes the quest");
        case "give_mob_effect": {
          const amplifier = action.amplifier ? ` ${"I".repeat(action.amplifier + 1)}` : "";
          return pill("condition", `${prettify(action.effect)} ${formatTicks(action.duration)}${amplifier}`, action.effect);
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
  const continueButton = () => h("button", { class: "linkish", type: "button", text: "continue ->", onclick: () => onNext(next) });

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
        option.conditions?.length ? pill("condition", `${option.conditions.length} cond.`) : null,
        option.actions?.length ? pill("reward", `${option.actions.length} act.`) : null,
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
                pill("requirement", `weight ${entry.weight}`),
                actionsNode(entry.actions),
                typeof entry.next === "string"
                  ? h("button", { class: "linkish", type: "button", text: "continue ->", onclick: () => onNext(entry.next) })
                  : h("span", { class: "entry-note", text: "ends here" }),
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
  element = h("dialog", { id: "viewer", class: "viewer", "aria-label": "Detail" });
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
        h("button", {
          class: "linkish",
          type: "button",
          text: "back",
          onclick: () => (trail.length > 1 ? renderTrail() : closeViewer()),
        }),
        h("button", { class: "linkish", type: "button", text: "close", onclick: closeViewer }),
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
      viewerBody.append(h("p", { class: "empty", text: `Dialog ${id} could not be loaded.` }));
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
        : h("p", { class: "entry-note", text: "- end of conversation -" }),
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
              members.size > 1 ? h("span", { class: "entry-note", text: ` or ${members.size - 1} other` }) : null,
            ),
          ),
        )
      : h("p", { class: "entry-note", text: "This tag belongs to a base mod, so its members are not in this repository." }),
  );
}

// ---------------------------------------------------------------------------
// cards
// ---------------------------------------------------------------------------

export function questCard(entryData, quest, onQuestLink) {
  const points = [
    ["Offer", quest.initialDialog],
    ["Follow-up", quest.followUpDialog],
    ["Hand in", quest.completionDialog],
  ].filter(([, option]) => option);

  return h(
    "article",
    { class: "card" },
    cardHead(quest.title ? label(quest.title) : entryData.id, entryData.id, [
      quest.recurrence
        ? pill("requirement", `cooldown ${formatTicks(quest.recurrence.cooldown)}`)
        : pill("condition", "one-time"),
      pill("accent", entityLabel(quest.character)),
    ]),
    quest.description ? h("p", { class: "card-desc", text: label(quest.description) }) : null,
    conditionsNode(quest.conditions, onQuestLink),
    requirementsNode(quest.requirements),
    rewardsNode(quest.rewards),
    points.length
      ? section(
          "Conversation",
          ...points.map(([name, option]) =>
            entry(
              name,
              h("span", { text: label(option.text) }),
              typeof option.next === "string"
                ? h("button", {
                    class: "linkish",
                    type: "button",
                    text: "open dialog ->",
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
 *   - not a sell offer                      -> the player hands over the ingredients: "Sell"
 *   - sell offer, an ingredient is currency  -> the player pays currency: "Buy"
 *   - sell offer, otherwise                  -> the character makes it: "Request a craft"
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

const TRADE_TITLES = {
  sell: "Sell to character",
  buy: "Buy from character",
  craft: "Request a craft",
};

export function tradeCard(entryData, trade) {
  const kind = tradeKind(trade);
  const stock = trade.recurrence?.maxStock;
  const restock = trade.recurrence?.restockTime;

  return h(
    "article",
    { class: "card" },
    cardHead(TRADE_TITLES[kind], entryData.id, [
      pill("accent", entityLabel(trade.character)),
      stock ? pill("requirement", `stock ${stock}`) : null,
      restock ? pill("condition", `restock ${formatTicks(restock)}`) : null,
    ]),
    h(
      "div",
      { class: "trade-flow" },
      ingredientList(trade.ingredients),
      h("span", { class: "arrow", text: "->" }),
      pill("reward", itemLabel(trade.result?.id), trade.result?.id),
      h("span", { class: "n", text: `x${trade.result?.count ?? 1}` }),
    ),
    conditionsNode(trade.conditions, null),
    rawJson(entryData.file, trade),
  );
}

export function starterCard(entryData, starter) {
  return h(
    "article",
    { class: "card" },
    cardHead(label(starter.text), entryData.id, [
      pill("accent", entityLabel(starter.character)),
      starter.weight !== undefined ? pill("requirement", `weight ${starter.weight}`) : null,
    ]),
    h(
      "div",
      { class: "starter-row" },
      typeof starter.dialog === "string"
        ? h("button", {
            class: "linkish",
            type: "button",
            text: "start conversation ->",
            onclick: () => openDialogViewer(starter.dialog),
          })
        : null,
    ),
    conditionsNode(starter.conditions, null),
    rawJson(entryData.file, starter),
  );
}

