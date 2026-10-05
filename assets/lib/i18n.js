// Interface strings: the page chrome, the labels around the datapack nodes, and
// the messages that are not part of the mod itself.
//
// This is deliberately separate from the mod's lang files (assets/lib/format.js
// resolves those). Those carry quest, dialog, item and entity text; everything the
// site says *about* that content lives here, so neither has to change when the
// other does. Placeholders are positional (`{0}`), so a translation is free to
// reorder the sentence - which Chinese usually needs.

import { state } from "./state.js";

const en_us = {
  // -- page chrome ----------------------------------------------------------
  // The mod's own name is prepended to the tagline at runtime, from the game's
  // translation of `gensokyolegacy.title` rather than from this table.
  "page.tagline": "Quests, Dialogs, Trades & Items",
  "meta.description":
    "Browser for the Gensokyo Legacy RPG datapack: every quest, dialog and trade offer with its conditions and requirements, plus every item with the guide entry that documents it and every way to get one, read straight from the generated JSON.",
  "a11y.skip": "Skip to content",
  "a11y.search": "Search",
  "a11y.language": "Language",
  "a11y.theme": "Toggle colour scheme",
  "a11y.browse": "Browse",
  "a11y.contentType": "Content type",
  "a11y.detail": "Detail",

  "brand.sub": "Quests, dialogs, trades & items — read from the datapack",
  "search.placeholder": "Search quests, dialogs, items…",

  // -- navigation -----------------------------------------------------------
  "nav.characters": "Characters",
  "nav.allCharacters": "All characters",
  "nav.items": "Items",
  "nav.patchouli": "Patchouli",
  "tab.quest": "Quests",
  "tab.daily": "Dailies",
  "tab.trade": "Trades",
  "tab.starters": "Starters",
  "tab.dialog": "Dialogs",

  // Short character names, keyed by the registry folder their content sits in.
  // The full in-game name is the tooltip, and the fallback when one is missing.
  "character.reimu": "Reimu",
  "character.marisa": "Marisa",
  "character.alice": "Alice",
  "character.morichika": "Rinnosuke",

  "source.summary": "{0} datapack files - {1} loot tables",
  "source.fetchedFrom": "Fetched live from ",
  "source.onBranch": " on branch ",
  "source.end": ".",

  // -- panels ---------------------------------------------------------------
  "noun.quests": "quests",
  "noun.dailies": "dailies",
  "noun.trades": "trade offers",
  "noun.starters": "starters",
  "noun.dialogs": "dialogs",
  "noun.items": "items",
  "noun.entries": "guide entries",
  "empty.match": "No {0} match the current filters.",
  "status.loadingDialogs": "Loading dialog files...",
  "nav.loadingItems": "Loading items...",
  "nav.loadingGuide": "Loading the guide book...",
  "status.loadingItems": "Loading recipes, the guide book and quest rewards...",
  "status.refreshed.one": "Index refreshed from GitHub: {0} new file found on this branch.",
  "status.refreshed.many": "Index refreshed from GitHub: {0} new files found on this branch.",

  "starters.note": "The gated entry points a player can trigger by talking to a character.",
  "dialogs.note": "Every dialog node, grouped by the conversation it belongs to.",
  "dialog.lines.one": "{0} line - first: {1}",
  "dialog.lines.many": "{0} lines - first: {1}",
  "dialog.end": "- end of conversation -",
  "dialog.startConversation": "start conversation ->",
  "dialog.failed": "Dialog {0} could not be loaded.",

  // -- viewer ---------------------------------------------------------------
  "viewer.back": "back",
  "viewer.close": "close",

  // -- sections -------------------------------------------------------------
  "section.conditions": "Conditions to unlock",
  "section.requirements": "Requirements",
  "section.rewards": "Rewards",
  "section.conversation": "Conversation",

  // -- conditions -----------------------------------------------------------
  "cond.quest": "Quest",
  "cond.advancement": "Advancement",
  "cond.notYet": "Not yet",
  "cond.items": "Items",
  "cond.cooldown": "Cooldown",
  "cond.ready": "Ready",
  "cond.elapsed": " must have elapsed",
  "cond.reputation": "Reputation",
  "cond.reputation.below": "below {0}",
  "cond.reputation.atLeast": "at least {0}",
  "cond.reputation.withCharacter": " with this character",
  "cond.reputation.with": " with {0}",
  "cond.anyOf": "Any of",
  "cond.home": "Home",
  "cond.home.own": "in this character's own home",
  "cond.home.visiting": "visiting another character's home",
  "cond.visiting": "Visiting",
  "cond.visiting.custom": "any player-built home",
  "cond.generic": "Condition",

  // -- requirements ---------------------------------------------------------
  "req.submit": "Submit",
  "req.carry": "Carry",
  "req.kill": "Kill",
  "req.defeat": "Defeat",
  "req.defeatEnemies": "Defeat enemies",
  "req.raid": "Raid",
  "req.winRaid": "Win a raid",
  "req.randomLoot": "Random loot",
  "req.hidden": "Hidden",
  "req.koishiHat": "Obtain the {0}",
  "req.hiddenNote": "Hidden objective - no progress is displayed in game.",
  "req.generic": "Requirement",

  // -- rewards --------------------------------------------------------------
  "reward.exp": "Experience",
  "reward.expValue": "{0} exp",
  "reward.reputation": "Reputation",
  "reward.reputationDetail": " (soft cap {0}, cap +{1}, max {2})",
  "reward.loot": "Loot",
  "reward.generic": "Reward",

  // -- loot tables ----------------------------------------------------------
  "loot.loading": "Loading loot table...",
  "loot.failed": "Loot table {0} could not be loaded.",
  "loot.pool": "Pool {0}",
  "loot.rolls": "Rolls {0}x",
  "loot.perDrop": "{0} per drop",

  // -- tags -----------------------------------------------------------------
  "tag.unknownTarget": "unknown target",
  "tag.showMembers": "Show members",
  "tag.vanilla": "{0} (vanilla tag, not in this repository)",
  "tag.baseMod": "This tag belongs to a base mod, so its members are not in this repository.",
  "tag.others": " or {0} other",

  // -- dialog options -------------------------------------------------------
  "action.startQuest": "starts the quest",
  "action.completeQuest": "completes the quest",
  "option.conditions": "{0} cond.",
  "option.actions": "{0} act.",
  "option.continue": "continue ->",
  "option.weight": "weight {0}",
  "option.endsHere": "ends here",

  // -- quest cards ----------------------------------------------------------
  "quest.offer": "Offer",
  "quest.followUp": "Follow-up",
  "quest.handIn": "Hand in",
  "quest.cooldown": "cooldown {0}",
  "quest.oneTime": "one-time",
  "quest.openDialog": "open dialog ->",

  // -- trade cards ----------------------------------------------------------
  // One heading per direction of trade, and a title per card that names the item.
  "trade.group.sell": "Sell to character",
  "trade.group.buy": "Buy from character",
  "trade.group.craft": "Processing",
  "trade.title.sell": "Sell {0}",
  "trade.title.buy": "Buy {0}",
  "trade.title.craft": "Craft {0}",
  "trade.stock": "stock {0}",
  "trade.restock": "restock {0}",

  // -- item section ---------------------------------------------------------
  "item.note":
    "Every item the mod adds, with the guide entry that documents it and every way to get one. The guide text is the in-game book's own, not a copy of it.",
  "item.all": "All items",
  "item.undocumented": "Not in the guide",
  "item.guide": "Guide entry",
  "item.guideTag": "Documented for every item in",
  "item.category": "Guide category",
  "item.noGuide": "No guide entry documents this item yet.",
  "item.sources": "Where to get it",
  "item.ways.one": "{0} way to get it",
  "item.ways.many": "{0} ways to get it",
  "item.source.recipe": "Recipes",
  "item.source.trade": "Character offers",
  "item.source.quest": "Quest rewards",
  "item.source.shelf": "Rinnosuke's shop shelves",
  "item.shelfPrice": "price ¥{0}",
  "item.shelfStock": "stock {0}",
  "item.shelfNote": "Rinnosuke rolls these when he restocks the shelves.",
  "item.source.none":
    "Nothing in the datapack hands this item out: expect creative mode, worldgen, or a source that is not written yet.",
  "item.advancement": "Advancement",
  "item.pay": "Pay",
  "item.openQuest": "open quest ->",
  "item.stock": "stock {0}",
  "item.restock": "restock {0}",
  "item.perBrew": "{0} per brew",
  "item.usedIn": "Used in",
  "item.openItem": "open item ->",

  // -- guide book ----------------------------------------------------------
  "guide.note":
    "The guide book's own entries, page by page, exactly as the book writes them - the item list beside it is the same prose read from each item instead.",
  "guide.pages.one": "{0} page",
  "guide.pages.many": "{0} pages",

  // -- recipes --------------------------------------------------------------
  "recipe.type.crafting_shaped": "Crafting",
  "recipe.type.crafting_shapeless": "Crafting, shapeless",
  "recipe.type.stonecutting": "Stonecutter",
  "recipe.type.unordered_alchemy": "Alchemy pot",
  "recipe.type.witch_enhance": "Brewing, enhance",
  "recipe.type.witch_merge": "Brewing, merge",
  "recipe.type.potion_alchemy_stage": "Brewing, potion",

  // -- raw json -------------------------------------------------------------
  "raw.summary": "Source JSON",
  "raw.loadedFrom": "Loaded from ",

  // -- units ----------------------------------------------------------------
  "unit.second": "s",
  "unit.minute": "min",
  "unit.hour": "h",
  "ticks.tooltip": "{0} ticks",

  // -- failures -------------------------------------------------------------
  "error.manifest":
    "Could not load rpg-manifest.json ({0}). Publish this branch with GitHub Pages set to the " +
    "branch root (not /docs), and make sure rpg-manifest.json is committed.",
  "error.content": "Failed to load content: {0}",
};

const zh_cn = {
  // -- page chrome ----------------------------------------------------------
  // 模组名称在运行时取自游戏自己的 `gensokyolegacy.title` 翻译，而不是这张表。
  "page.tagline": "任务、对话、交易与物品",
  "meta.description":
    "浏览东方幻想绮谈 RPG 数据包：全部任务、对话与交易内容及其条件与需求，以及每件物品对应的指南条目与全部获取方式，均直接读取自动生成的 JSON。",
  "a11y.skip": "跳到正文",
  "a11y.search": "搜索",
  "a11y.language": "语言",
  "a11y.theme": "切换配色",
  "a11y.browse": "浏览",
  "a11y.contentType": "内容类型",
  "a11y.detail": "详情",

  "brand.sub": "任务、对话、交易与物品——直接读取数据包",
  "search.placeholder": "搜索任务、对话、物品…",

  // -- navigation -----------------------------------------------------------
  "nav.characters": "角色",
  "nav.allCharacters": "全部角色",
  "nav.items": "物品",
  "nav.patchouli": "Patchouli 指南书",
  "tab.quest": "任务",
  "tab.daily": "日常",
  "tab.trade": "交易",
  "tab.starters": "对话入口",
  "tab.dialog": "对话",

  // Short names; the tooltip and fallback carry the full in-game name.
  "character.reimu": "灵梦",
  "character.marisa": "魔理沙",
  "character.alice": "爱丽丝",
  "character.morichika": "霖之助",

  "source.summary": "{0} 个数据包文件 · {1} 个战利品表",
  "source.fetchedFrom": "实时抓取自 ",
  "source.onBranch": "，来自分支 ",
  "source.end": "。",

  // -- panels ---------------------------------------------------------------
  "noun.quests": "任务",
  "noun.dailies": "日常任务",
  "noun.trades": "交易",
  "noun.starters": "对话入口",
  "noun.dialogs": "对话",
  "noun.items": "物品",
  "noun.entries": "指南条目",
  "empty.match": "没有符合当前筛选条件的{0}。",
  "status.loadingDialogs": "正在加载对话文件……",
  "status.loadingItems": "正在加载配方、指南书与任务奖励……",
  "nav.loadingItems": "正在加载物品……",
  "nav.loadingGuide": "正在加载指南书……",
  "status.refreshed.one": "已从 GitHub 刷新索引：该分支上有 {0} 个新文件。",
  "status.refreshed.many": "已从 GitHub 刷新索引：该分支上有 {0} 个新文件。",

  "starters.note": "与角色交谈时可能触发、且带有条件限制的对话入口。",
  "dialogs.note": "全部对话节点，按其所属的对话分组。",
  "dialog.lines.one": "{0} 行——首句：{1}",
  "dialog.lines.many": "{0} 行——首句：{1}",
  "dialog.end": "— 对话结束 —",
  "dialog.startConversation": "开始对话 ->",
  "dialog.failed": "对话 {0} 加载失败。",

  // -- viewer ---------------------------------------------------------------
  "viewer.back": "返回",
  "viewer.close": "关闭",

  // -- sections -------------------------------------------------------------
  "section.conditions": "解锁条件",
  "section.requirements": "需求",
  "section.rewards": "奖励",
  "section.conversation": "对话",

  // -- conditions -----------------------------------------------------------
  "cond.quest": "任务",
  "cond.advancement": "进度",
  "cond.notYet": "尚未获得",
  "cond.items": "物品",
  "cond.cooldown": "冷却",
  "cond.ready": "已就绪",
  "cond.elapsed": "（需已过）",
  "cond.reputation": "声望",
  "cond.reputation.below": "低于 {0}",
  "cond.reputation.atLeast": "至少 {0}",
  "cond.reputation.withCharacter": "（与该角色）",
  "cond.reputation.with": "（与{0}）",
  "cond.anyOf": "满足其一",
  "cond.home": "住所",
  "cond.home.own": "在该角色自己家中",
  "cond.home.visiting": "在别处做客",
  "cond.visiting": "拜访",
  "cond.visiting.custom": "任意玩家自建的房屋",
  "cond.generic": "条件",

  // -- requirements ---------------------------------------------------------
  "req.submit": "提交",
  "req.carry": "携带",
  "req.kill": "击杀",
  "req.defeat": "击败",
  "req.defeatEnemies": "击败敌人",
  "req.raid": "袭击",
  "req.winRaid": "赢得一场袭击",
  "req.randomLoot": "随机战利品",
  "req.hidden": "隐藏",
  "req.koishiHat": "获得{0}",
  "req.hiddenNote": "隐藏目标——游戏内不会显示进度。",
  "req.generic": "需求",

  // -- rewards --------------------------------------------------------------
  "reward.exp": "经验",
  "reward.expValue": "{0} 点经验",
  "reward.reputation": "声望",
  "reward.reputationDetail": "（软上限 {0}，单次上限 +{1}，最大 {2}）",
  "reward.loot": "战利品",
  "reward.generic": "奖励",

  // -- loot tables ----------------------------------------------------------
  "loot.loading": "正在加载战利品表……",
  "loot.failed": "战利品表 {0} 加载失败。",
  "loot.pool": "奖池 {0}",
  "loot.rolls": "抽取 {0} 次",
  "loot.perDrop": "每次 {0} 个",

  // -- tags -----------------------------------------------------------------
  "tag.unknownTarget": "未知目标",
  "tag.showMembers": "查看成员",
  "tag.vanilla": "{0}（原版标签，本仓库中没有）",
  "tag.baseMod": "该标签属于某个前置模组，本仓库中没有它的成员。",
  "tag.others": "（或另外 {0} 项）",

  // -- dialog options -------------------------------------------------------
  "action.startQuest": "开启任务",
  "action.completeQuest": "完成任务",
  "option.conditions": "{0} 个条件",
  "option.actions": "{0} 项效果",
  "option.continue": "继续 ->",
  "option.weight": "权重 {0}",
  "option.endsHere": "到此结束",

  // -- quest cards ----------------------------------------------------------
  "quest.offer": "接取",
  "quest.followUp": "进行中",
  "quest.handIn": "交付",
  "quest.cooldown": "冷却 {0}",
  "quest.oneTime": "一次性",
  "quest.openDialog": "查看对话 ->",

  // -- 交易卡片 --------------------------------------------------------------
  // 每种交易方向一个标题，每张卡片再带上物品名。
  "trade.group.sell": "卖给角色",
  "trade.group.buy": "向角色购买",
  "trade.group.craft": "加工",
  "trade.title.sell": "出售{0}",
  "trade.title.buy": "购买{0}",
  "trade.title.craft": "制作{0}",
  "trade.stock": "库存 {0}",
  "trade.restock": "补货 {0}",

  // -- 物品 ------------------------------------------------------------------
  "item.note": "模组添加的全部物品，以及各自的指南条目与全部获取方式。指南文字直接来自游戏内的指南书原文。",
  "item.all": "全部物品",
  "item.undocumented": "指南未收录",
  "item.guide": "指南条目",
  "item.guideTag": "本条目同时介绍该组内的所有物品：",
  "item.category": "所属分类",
  "item.noGuide": "暂时还没有指南条目介绍这件物品。",
  "item.sources": "获取方式",
  "item.ways.one": "{0} 种获取方式",
  "item.ways.many": "{0} 种获取方式",
  "item.source.recipe": "配方",
  "item.source.trade": "角色交易",
  "item.source.quest": "任务奖励",
  "item.source.shelf": "霖之助的商店货架",
  "item.shelfPrice": "售价 ¥{0}",
  "item.shelfStock": "库存 {0}",
  "item.shelfNote": "霖之助补货时会从这些区间里随机取值。",
  "item.source.none": "数据包中没有任何途径提供这件物品：可能只能通过创造模式或世界生成获得，也可能尚未实装。",
  "item.advancement": "进度",
  "item.pay": "支付",
  "item.openQuest": "查看该任务 ->",
  "item.stock": "库存 {0}",
  "item.restock": "补货 {0}",
  "item.perBrew": "每次酿造 {0}",
  "item.usedIn": "可用于制作",
  "item.openItem": "查看该物品 ->",

  // -- 指南书 --------------------------------------------------------------
  "guide.note": "指南书自身的条目，逐页照原样呈现——旁边的物品列表是同一段文字从每件物品的角度出发。",
  "guide.pages.one": "{0} 页",
  "guide.pages.many": "{0} 页",

  // -- 配方 ------------------------------------------------------------------
  "recipe.type.crafting_shaped": "合成",
  "recipe.type.crafting_shapeless": "合成（无序）",
  "recipe.type.stonecutting": "切石机",
  "recipe.type.unordered_alchemy": "炼金锅",
  "recipe.type.witch_enhance": "酿造——强化",
  "recipe.type.witch_merge": "酿造——合成",
  "recipe.type.potion_alchemy_stage": "酿造——药水",

  // -- raw json -------------------------------------------------------------
  "raw.summary": "源 JSON",
  "raw.loadedFrom": "加载自 ",

  // -- units ----------------------------------------------------------------
  "unit.second": "秒",
  "unit.minute": "分钟",
  "unit.hour": "小时",
  "ticks.tooltip": "{0} 刻",

  // -- failures -------------------------------------------------------------
  "error.manifest":
    "无法加载 rpg-manifest.json（{0}）。请在仓库设置中将 GitHub Pages 的来源设为“部署分支”，" +
    "并选择分支根目录（而非 /docs），同时确认 rpg-manifest.json 已提交。",
  "error.content": "内容加载失败：{0}",
};

/** Locale -> key -> text. `en_us` is the fallback, so a gap shows English, not a key. */
const TABLES = { en_us, zh_cn };

/** BCP 47 tags for the `<html lang>` attribute. */
const HTML_LANG = { en_us: "en", zh_cn: "zh-CN" };

/**
 * Looks an interface string up in the active locale, substituting `{0}`, `{1}`
 * ... with the extra arguments. Falls back to English and then to the key itself,
 * so a missing translation is visible but never blanks the page.
 */
export function tr(key, ...args) {
  const text = trOrNull(key);
  if (text === null) return key;
  if (!args.length) return text;
  return text.replace(/\{(\d+)\}/g, (match, index) => (args[index] === undefined ? match : String(args[index])));
}

/**
 * The string for a key, or null when no locale has one. For keys built at runtime
 * (`character.${slug}`), where a caller needs to tell "absent" from "empty".
 */
export function trOrNull(key) {
  return TABLES[state.lang]?.[key] ?? TABLES.en_us[key] ?? null;
}

/** Picks the `.one` or `.many` variant of a key by count; Chinese uses both. */
export function trPlural(base, count, ...args) {
  return tr(count === 1 ? `${base}.one` : `${base}.many`, count, ...args);
}

/**
 * Translates the static markup declared in index.html. Text comes from
 * `data-i18n`; attributes come from `data-i18n-attr`, as a comma separated list of
 * `attribute:key` pairs. Called on boot and whenever the language changes.
 */
export function applyLanguage(root = document) {
  document.documentElement.lang = HTML_LANG[state.lang] ?? "en";
  for (const node of root.querySelectorAll("[data-i18n]")) {
    node.textContent = tr(node.dataset.i18n);
  }
  for (const node of root.querySelectorAll("[data-i18n-attr]")) {
    for (const pair of node.dataset.i18nAttr.split(",")) {
      const [attribute, key] = pair.split(":").map((part) => part.trim());
      if (attribute && key) node.setAttribute(attribute, tr(key));
    }
  }
}