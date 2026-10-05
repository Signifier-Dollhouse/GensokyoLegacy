#!/usr/bin/env python3
"""
Export character greeting / chat / quest dialogs as plain text for human review.

Reads the datagen output under
src/generated/resources/data/gensokyolegacy/gensokyolegacy and the lang files,
then writes one review file per character *and per language*:

    out/en/<character>.txt        english (en_us), the source of truth
    out/zh_cn/<character>.txt     chinese (zh_cn), for localization review

    python3 tools/export_dialogs.py                  # both languages
    python3 tools/export_dialogs.py --lang en        # english only
    python3 tools/export_dialogs.py --character alice

Everything the game shows the player is read from the lang file, so the two
exports carry the same structure and only differ in the text (and in the
Chinese export missing translations are marked in place).
"""

import argparse
import json
import os
from collections import OrderedDict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src/generated/resources/data/gensokyolegacy/gensokyolegacy")
DATA_MAP = os.path.join(ROOT, "src/generated/resources/data/gensokyolegacy/data_maps/entity_type/default_dialog.json")
EN_LANG = os.path.join(ROOT, "src/generated/resources/assets/gensokyolegacy/lang/en_us.json")
ZH_LANG = os.path.join(ROOT, "src/main/resources/assets/gensokyolegacy/lang/zh_cn.json")
OUT = os.path.join(ROOT, "out")

TICKS_PER_SECOND = 20
TICKS_PER_DAY = 24000

# Every reviewer-facing label, in both languages: key -> (en, zh_cn).
LABELS = {
    "lang_line": ("lang: english (en_us)",
                  "语言：简体中文（zh_cn）"),
    "legend1": ('legend: `player: "..."` and `> "..."` are the player\'s button lines; every other',
                "图例：`玩家：“…”` 与 `> “…”` 为玩家的按钮文本，其余每段缩进内容均为角色台词。"),
    "legend2": ("        indented block is what the character says. `gate:` = when it is available,",
                "        `条件：` 表示何时可用，`效果：` 表示点击后的结果，`[动作]` 表示动画，"),
    "legend3": ("        `action:` = what a click does, `[anim]` = the animation,",
                "        `[按钮“X”]` 表示与 X 组内其他条目共用同一个按钮。"),
    "legend4": ('        `[button "X"]` = shared with the other entries of group X.',
                "        闲聊与任务均按注册表（字母）顺序排列。"),
    "legend5": ("        Chats and quests are in registry (alphabetical) order. Trades are not",
                "        交易不在本导出范围内，参见 doc/quest/<char>.md。"),
    "legend6": ("        exported; see doc/quest/<char>.md for those.",
                None),

    "section_greetings": ("## GREETINGS", "## 问候"),
    "greet_home": ("at her own home", "在自己家中"),
    "greet_visit": ("while visiting", "作为客人来访时"),
    "greet_trade": ("trade button", "交易按钮"),
    "section_chats": ("## CHATS (%d)", "## 闲聊（%d）"),
    "no_chats": ("(none)", "（无）"),
    "chat_head": ("chat %s", "闲聊 %s"),
    "weight": ("weight %s   (1 = default greeting, 30 misc, 100 info, 1000 special)",
               "权重 %s（1 = 默认问候，30 杂谈，100 情报，1000 特殊事件）"),
    "gate_none": ("gate: none (always available)", "条件：无（始终可用）"),
    "player": ("player: ", "玩家："),
    "section_quests": ("## QUESTS (%d)", "## 任务（%d）"),
    "quest_head": ("quest %s", "任务 %s"),
    "no_quests": ("(none - she only has chats and trades)", "（无 —— 她只有闲聊与交易）"),
    "daily": ("DAILY, cooldown %s", "每日任务，冷却 %s"),
    "one_time": ("one-time", "一次性任务"),
    "title": ("title:", "标题："),
    "description": ("description:", "描述："),
    "requirements": ("requirements:", "需求："),
    "req_none": ("(none)", "（无）"),
    "rewards": ("rewards:", "奖励："),
    "start": ("START (player asks, no progress known)", "开始（玩家询问，角色不知道进度）"),
    "follow_up": ("FOLLOW-UP (quest in progress)", "跟进（任务进行中）"),
    "complete": ("COMPLETE (handover)", "完成（交付）"),

    "req_submit": ("hand over to her:", "交给角色："),
    "req_carry": ("carry (not handed over):", "随身携带（无需上交）："),
    "req_kill_enemy": ("kill %d enemies:", "击杀 %d 个敌人："),
    "req_kill_mob": ("kill %d x %s:", "击杀 %d 个 %s："),
    "req_raid": ("win %d raid(s):", "赢得 %d 次袭击："),
    "req_roll": ("rolled once when the quest starts, from loot table %s",
                 "任务开始时从战利品表 %s 抽取一次"),
    "req_koishi": ("the koishi hat drops (event, nothing to do on purpose)",
                   "恋符“三色乌帽子”掉落（事件触发，无需主动操作）"),
    "item_any": ("(any)", "（任意）"),
    "item_override": ("[display-name override]", "[覆盖显示名]"),
    "item_custom": ("custom: ", "自定义："),

    "reward_exp": ("%d exp", "%d 经验"),
    "reward_rep": ("reputation +%d (gains halve past %d, %s)",
                   "好感 +%d（超过 %d 后收益减半，%s）"),
    "no_cap_growth": ("no cap growth", "不提升上限"),
    "cap_growth": ("cap +%d up to %d", "上限 +%d，最多 %d"),
    "reward_loot": ("loot: %s", "战利品：%s"),

    "act_start": ("action: starts the quest", "效果：开始任务"),
    "act_complete": ("action: completes the quest", "效果：完成任务"),
    "act_effect": ("action: grants %s for %s", "效果：获得 %s，持续 %s"),
    "act_timer": ("action: starts cooldown '%s' (%s)", "效果：开始冷却“%s”（%s）"),
    "act_raw": ("action: ", "效果："),

    "gate_home": ("gate: only at her own home", "条件：仅在自己的家中"),
    "gate_visiting": ("gate: only while visiting someone else", "条件：仅在作为客人来访时"),
    "gate_visit_structure": ("gate: visiting %s", "条件：正在访问 %s"),
    "gate_quest_done": ("gate: completed quest %s", "条件：已完成任务 %s"),
    "gate_quest_done_title": ('gate: completed quest %s = %s', "条件：已完成任务 %s＝%s"),
    "gate_has": ("gate: has advancement %s", "条件：已获得进度 %s"),
    "gate_missing_adv": ("gate: missing advancement %s", "条件：尚未获得进度 %s"),
    "gate_rep_ge": ("gate: reputation >= %d", "条件：好感 ≥ %d"),
    "gate_rep_lt": ("gate: reputation < %d", "条件：好感 < %d"),
    "gate_other_rep": ("gate: reputation with %s >= %d", "条件：与 %s 的好感 ≥ %d"),
    "gate_timer": ("gate: cooldown '%s' elapsed", "条件：冷却“%s”已结束"),
    "gate_has_item": ("gate: player carries", "条件：玩家持有"),
    "gate_any": ("gate: ANY of", "条件：以下任一"),
    "gate_raw": ("gate: ", "条件："),

    "anim": ("[anim: %s]", "[动作：%s]"),
    "ends": ("(dialog ends)", "（对话结束）"),
    "ends_no_options": ("(dialog ends here, no further options)", "（对话在此结束，没有后续选项）"),
    "weighted": ("weighted outcome (weight %d):", "加权结果（权重 %d）："),
    "button_group": ('[button "%s"] > ', "[按钮“%s”] > "),
    "loops": ("(loops back to %s)", "（回到 %s）"),
    "missing_dialog": ("!! missing dialog: %s", "!! 缺失对话：%s"),

    "flags_head": ("## REVIEW FLAGS (%d)", "## 审阅提示（%d）"),
    "flag_no_reject": ("quest %s: START dialog has one option, so there is no reject branch "
                       "(quest_design.md §10)", "任务 %s：开始对话只有一个选项，缺少拒绝分支"
                                            "（quest_design.md §10）"),
    "flag_raw_button": ("button text is a raw string, not a lang key (quest_design.md §10): %r",
                        "按钮文本是原始字符串而非语言键（quest_design.md §10）：%r"),
    "flag_unreachable": ("dialog never reachable from %s: %s", "对话无法从 %s 到达：%s"),
}

EFFECT_NAMES = {
    "minecraft:haste": ("Haste", "急迫"),
    "minecraft:bad_omen": ("Bad Omen", "不祥之兆"),
    "gensokyolegacy:looting": ("Looting (mod)", "抢夺（mod）"),
}

SPECIAL_WORDS = {"tnt": "TNT", "jukebox": "Jukebox"}


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def load_registry(name):
    """Return {registry id without namespace: json} for a datapack registry dir."""
    out = OrderedDict()
    root = os.path.join(DATA, name)
    for dirpath, _, files in os.walk(root):
        for fn in sorted(files):
            if not fn.endswith(".json"):
                continue
            path = os.path.join(dirpath, fn)
            rel = os.path.relpath(path, root)[: -len(".json")]
            out[rel.replace(os.sep, "/")] = load_json(path)
    return out


def loc(path):
    """`alice/chat/hi` -> `gensokyolegacy:alice/chat/hi`."""
    return path if ":" in path else "gensokyolegacy:" + path


def plain_loc(path):
    return loc(path).split(":", 1)[1]


def prettify(item_id):
    """Vanilla ids are not in the lang files, so title-case them instead."""
    words = []
    for w in plain_loc(item_id).replace("/", " ").split("_"):
        w = SPECIAL_WORDS.get(w, w)
        if w:
            words.append(w[:1].upper() + w[1:])
    return " ".join(words)


class Ctx:
    """Registries + one lang file, plus the review notes collected on the way."""

    def __init__(self, lang, path):
        self.id = lang                      # "en" or "zh_cn"
        self.i = 0 if lang == "en" else 1    # index into the LABELS tuples
        self.lang = load_json(path)
        self.dialogs = load_registry("dialog")
        self.starters = load_registry("dialog_starter")
        self.quests = load_registry("quest")
        self.default_dialogs = load_json(DATA_MAP)["values"]
        self.notes = []

    def t(self, key, *args):
        return LABELS[key][self.i] % args if args else LABELS[key][self.i]

    # -- translatable text --------------------------------------------------

    def quoted(self, body):
        return '"%s"' % body if self.id == "en" else "\u201c%s\u201d" % body

    def text(self, key, indent="", prefix=""):
        """One lang entry, or the raw key flagged when it is not registered."""
        body = self.lang.get(key)
        if body is None:
            self.notes.append("missing %s key: %s" % (self.id, key))
            return [indent + prefix + key + '"   <-- NOT REGISTERED, shown as raw key']
        return [indent + prefix + self.quoted(body)]

    def lang_key(self, kind, id_):
        """`item`, `minecraft:string` -> `item.minecraft.string`."""
        return "%s.%s" % (kind, loc(id_).replace(":", "."))

    def item_name(self, item_id):
        for kind in ("item", "block"):
            key = self.lang_key(kind, item_id)
            if key in self.lang:
                return "%s   [%s]" % (self.lang[key], item_id)
        return "%s   [%s]" % (prettify(item_id), item_id)

    def entity_name(self, entity_id):
        return self.lang.get(self.lang_key("entity", entity_id), entity_id)

    def quest_title(self, path):
        q = self.quests.get(plain_loc(path))
        return self.lang.get(q["title"]) if q else None

    def ticks(self, n):
        if n % TICKS_PER_DAY == 0 and n >= TICKS_PER_DAY:
            d = n / TICKS_PER_DAY
            return ("%d ticks (%g day%s)" % (n, d, "" if d == 1 else "s")) if self.id == "en" \
                else ("%d tick（%g 天）" % (n, d))
        if n % (TICKS_PER_SECOND * 60) == 0 and n >= TICKS_PER_SECOND * 60:
            return ("%d ticks (%g min)" % (n, n / (TICKS_PER_SECOND * 60))) if self.id == "en" \
                else ("%d tick（%g 分钟）" % (n, n / (TICKS_PER_SECOND * 60)))
        if n % TICKS_PER_SECOND == 0:
            return ("%d ticks (%g s)" % (n, n / TICKS_PER_SECOND)) if self.id == "en" \
                else ("%d tick（%g 秒）" % (n, n / TICKS_PER_SECOND))
        return ("%d ticks" % n) if self.id == "en" else ("%d tick" % n)

    def effect(self, name):
        return EFFECT_NAMES.get(name, (name, name))[self.i]


def ingredients(ctx, entries, indent):
    lines = []
    for e in entries:
        count = e.get("count", 1)
        label = e.get("text")
        if label:
            name = "%s   [%s]" % (label, ctx.t("item_override"))
        elif "item" in e:
            name = ctx.item_name(e["item"])
        elif "tag" in e:
            name = "#" + plain_loc(e["tag"]) + " " + ctx.t("item_any")
        elif "items" in e:
            name = ctx.t("item_custom") + json.dumps(e["items"], ensure_ascii=False)
        else:
            name = json.dumps({k: v for k, v in e.items() if k != "count"}, ensure_ascii=False)
        lines.append(indent + "- " + name + " x" + str(count))
        if "components" in e:
            lines.append(indent + "    components: " + json.dumps(e["components"], ensure_ascii=False))
    return lines


def actions(ctx, items, indent):
    lines = []
    for a in items or []:
        t = a.get("type", "?")
        if t == "gensokyolegacy:start_quest":
            lines.append(indent + ctx.t("act_start"))
        elif t == "gensokyolegacy:complete_quest":
            lines.append(indent + ctx.t("act_complete"))
        elif t == "gensokyolegacy:give_mob_effect":
            amp = a.get("amplifier", 0)
            extra = (" (level %d)" % (amp + 1)) if amp else ""
            lines.append(indent + ctx.t("act_effect", ctx.effect(a["effect"]) + extra,
                                        ctx.ticks(a.get("duration"))))
        elif t == "gensokyolegacy:set_timer":
            lines.append(indent + ctx.t("act_timer", a["key"], ctx.ticks(a.get("delay"))))
        else:
            lines.append(indent + ctx.t("act_raw") + json.dumps(a, ensure_ascii=False))
    return lines


def condition(ctx, c, indent):
    t = c.get("type", "?")
    inv = c.get("invert", False)
    if t == "gensokyolegacy:home_bound":
        return [indent + ctx.t("gate_visiting" if inv else "gate_home")]
    if t == "gensokyolegacy:visit_structure":
        return [indent + ctx.t("gate_visit_structure", loc(c["structure"]))]
    if t == "gensokyolegacy:has_quest_completed":
        title = ctx.quest_title(c["quest"])
        if title:
            return [indent + ctx.t("gate_quest_done_title", loc(c["quest"]),
                                   ctx.quoted(title))]
        return [indent + ctx.t("gate_quest_done", loc(c["quest"]))]
    if t == "gensokyolegacy:has_advancement":
        return [indent + ctx.t("gate_missing_adv" if inv else "gate_has", loc(c["advancement"]))]
    if t == "gensokyolegacy:self_reputation":
        return [indent + ctx.t("gate_rep_lt" if inv else "gate_rep_ge", c["reputation"])]
    if t == "gensokyolegacy:other_reputation":
        return [indent + ctx.t("gate_other_rep", loc(c["character"]), c["reputation"])]
    if t == "gensokyolegacy:timer":
        return [indent + ctx.t("gate_timer", c["key"])]
    if t == "gensokyolegacy:has_item":
        return [indent + ctx.t("gate_has_item")] + ingredients(ctx, c["ingredients"], indent + "  ")
    if t == "gensokyolegacy:any":
        out = [indent + ctx.t("gate_any")]
        for sub in c["conditions"]:
            out += condition(ctx, sub, indent + "  - ")
        return out
    return [indent + ctx.t("gate_raw") + json.dumps(c, ensure_ascii=False)]


def conditions(ctx, items, indent):
    lines = []
    for c in items or []:
        lines += condition(ctx, c, indent)
    return lines


def option(ctx, o, indent, depth, path):
    """One button: the player's line, its gates and effects, then where it leads."""
    t = o.get("type", "?")
    lines = []
    if t == "gensokyolegacy:group":
        group = ctx.lang.get(o["group"], o["group"])
        lines += ctx.text(o["text"], indent=indent, prefix=ctx.t("button_group", group))
    else:
        lines += ctx.text(o["text"], indent=indent, prefix="> ")
    if t == "gensokyolegacy:random":
        lines += conditions(ctx, o.get("conditions"), indent + "  ")
        for e in o.get("entries", []):
            lines.append(indent + "  " + ctx.t("weighted", e.get("weight", 1)))
            lines += actions(ctx, e.get("actions"), indent + "    ")
            if e.get("next"):
                lines.append(indent + "    ->")
                lines += dialog(ctx, e["next"], indent + "      ", depth + 1, path)
            else:
                lines.append(indent + "    " + ctx.t("ends"))
        return lines
    lines += conditions(ctx, o.get("conditions"), indent + "  ")
    lines += actions(ctx, o.get("actions"), indent + "  ")
    if o.get("next"):
        lines.append(indent + "  ->")
        lines += dialog(ctx, o["next"], indent + "    ", depth + 1, path)
    else:
        lines.append(indent + "  " + ctx.t("ends"))
    return lines


def dialog(ctx, dialog_id, indent, depth, path):
    d = ctx.dialogs.get(plain_loc(dialog_id))
    if d is None:
        ctx.notes.append("dialog referenced but not registered: " + loc(dialog_id))
        return [indent + ctx.t("missing_dialog", loc(dialog_id))]
    if dialog_id in path or depth > 12:
        return [indent + ctx.t("loops", dialog_id)]
    path = path | {dialog_id}
    lines = ctx.text(d["text"], indent=indent)
    if d.get("animations"):
        lines.append(indent + "  " + ctx.t("anim", ", ".join(d["animations"])))
    opts = d.get("options", [])
    if not opts:
        lines.append(indent + "  " + ctx.t("ends_no_options"))
    for o in opts:
        lines += option(ctx, o, indent + "  ", depth + 1, path)
    return lines


def requirement(ctx, key, r):
    t = r.get("type", "?")
    head = "    [%s] " % key
    if t == "gensokyolegacy:submit_item":
        return [head + ctx.t("req_submit")] + ingredients(ctx, r["ingredients"], "        ")
    if t == "gensokyolegacy:has_item":
        return [head + ctx.t("req_carry")] + ingredients(ctx, r["ingredients"], "        ")
    if t == "gensokyolegacy:kill_enemy":
        return [head + ctx.t("req_kill_enemy", r["count"])] + ctx.text(r["text"], indent="      ")
    if t == "gensokyolegacy:kill_mob":
        what = r["target"].get("type") or r["target"].get("tags")
        return [head + ctx.t("req_kill_mob", r["count"], what)] + ctx.text(r["text"], indent="      ")
    if t == "gensokyolegacy:raid_victory":
        return [head + ctx.t("req_raid", r["count"])] + ctx.text(r["text"], indent="      ")
    if t == "gensokyolegacy:roll_item":
        return [head + ctx.t("req_roll", loc(r["table"]))]
    if t == "gensokyolegacy:koishi_hat":
        return [head + ctx.t("req_koishi")]
    return [head + json.dumps(r, ensure_ascii=False)]


def reward(ctx, r):
    t = r.get("type", "?")
    if t == "gensokyolegacy:exp":
        return "    - " + ctx.t("reward_exp", r["point"])
    if t == "gensokyolegacy:reputation":
        cap = ctx.t("no_cap_growth") if r["cap_increase"] == 0 \
            else ctx.t("cap_growth", r["cap_increase"], r["max_cap"])
        return "    - " + ctx.t("reward_rep", r["reputation"], r["soft_cap"], cap)
    if t == "gensokyolegacy:loot_table":
        return "    - " + ctx.t("reward_loot", loc(r["table"]))
    return "    - " + json.dumps(r, ensure_ascii=False)


def rule(char="=", n=78):
    return char * n


def audit(ctx, char_id, starters, quests):
    """The cheap checks from doc/quest/quest_design.md §10, as review notes."""
    notes = []
    used_dialogs = set()
    used_keys = set()

    def walk(did):
        used_dialogs.add(plain_loc(did))
        d = ctx.dialogs.get(plain_loc(did))
        if d is None:
            return
        used_keys.add(d["text"])
        for o in d.get("options", []):
            used_keys.add(o.get("text", ""))
            if o.get("group"):
                used_keys.add(o["group"])
            for target in [o.get("next")] + [e.get("next") for e in o.get("entries", [])]:
                if target:
                    walk(target)

    for s in starters.values():
        used_keys.add(s["text"])
        walk(s["dialog"])
    for q in quests.values():
        used_keys.add(q["title"])
        used_keys.add(q["description"])
        for r in q.get("requirements", {}).values():
            if "text" in r:
                used_keys.add(r["text"])
        for field in ("initialDialog", "followUpDialog", "completionDialog"):
            used_keys.add(q[field].get("text", ""))
            if q[field].get("next"):
                walk(q[field]["next"])

    # §10: "Every start has a reject branch that gives the character a reason."
    for k, q in quests.items():
        nxt = q["initialDialog"].get("next")
        d = ctx.dialogs.get(plain_loc(nxt)) if nxt else None
        if d is not None and len(d.get("options", [])) < 2:
            notes.append(ctx.t("flag_no_reject", loc(k)))

    # §10 inline-option pitfall: raw button text that can never be translated
    for key in sorted(used_keys):
        if key and key not in ctx.lang and not key.startswith("gensokyolegacy/"):
            notes.append(ctx.t("flag_raw_button", key))

    # §12: the Chinese export must not silently fall back to English
    if ctx.id == "zh_cn":
        for key in sorted(used_keys):
            if key in ctx.lang and key not in load_json(EN_LANG):
                notes.append("missing en_us key (nothing to translate): " + key)

    # unused dialogs owned by this character
    for did in sorted(ctx.dialogs):
        if did.startswith(char_id + "/") and did not in used_dialogs:
            notes.append(ctx.t("flag_unreachable", char_id, loc(did)))

    return notes


def render_character(ctx, char_id, display):
    out = []
    a = out.append
    starters = OrderedDict((k, v) for k, v in ctx.starters.items() if plain_loc(v["character"]) == char_id)
    quests = OrderedDict((k, v) for k, v in ctx.quests.items() if plain_loc(v["character"]) == char_id)

    a(rule("="))
    a("%s   (gensokyolegacy:%s)" % (display, char_id))
    a(ctx.t("lang_line"))
    for k in ("legend1", "legend2", "legend3", "legend4", "legend5", "legend6"):
        if LABELS[k][ctx.i] is not None:
            a(ctx.t(k))
    a(rule("="))
    a("")

    a(ctx.t("section_greetings"))
    cfg = ctx.default_dialogs.get("gensokyolegacy:" + char_id, {})
    for field, key in (("greeting", "greet_home"), ("visitGreeting", "greet_visit"),
                       ("trade", "greet_trade")):
        if field in cfg:
            a("  %s:" % ctx.t(key))
            out.extend(ctx.text(cfg[field], indent="    "))
    a("")

    a(ctx.t("section_chats", len(starters)))
    a("")
    if not starters:
        a("  " + ctx.t("no_chats"))
        a("")
    for k, s in starters.items():
        a(rule("-"))
        a(ctx.t("chat_head", loc(k)))
        a("  " + ctx.t("weight", s.get("weight", 1)))
        gates = conditions(ctx, s.get("conditions"), "  ")
        if gates:
            out.extend(gates)
        else:
            a("  " + ctx.t("gate_none"))
        out.extend(ctx.text(s["text"], indent="  ", prefix=ctx.t("player")))
        a("  %s:" % display)
        out.extend(dialog(ctx, s["dialog"], "    ", 0, set()))
        a("")

    a("")
    a(ctx.t("section_quests", len(quests)))
    a("")
    if not quests:
        a("  " + ctx.t("no_quests"))
        a("")
    for k, q in quests.items():
        a(rule("-"))
        a(ctx.t("quest_head", loc(k)))
        rec = q.get("recurrence")
        a("  " + (ctx.t("daily", ctx.ticks(rec["cooldown"])) if rec else ctx.t("one_time")))
        gates = conditions(ctx, q.get("conditions"), "  ")
        if gates:
            out.extend(gates)
        else:
            a("  " + ctx.t("gate_none"))
        a("  " + ctx.t("title"))
        out.extend(ctx.text(q["title"], indent="    "))
        a("  " + ctx.t("description"))
        out.extend(ctx.text(q["description"], indent="    "))
        a("  " + ctx.t("requirements"))
        for rk, r in q.get("requirements", {}).items():
            out.extend(requirement(ctx, rk, r))
        if not q.get("requirements"):
            a("    " + ctx.t("req_none"))
        a("  " + ctx.t("rewards"))
        for r in q.get("rewards", []):
            a(reward(ctx, r))
        for key, field in (("start", "initialDialog"), ("follow_up", "followUpDialog"),
                           ("complete", "completionDialog")):
            a("  " + ctx.t(key) + ":")
            out.extend(option(ctx, q[field], "    ", 0, set()))
        a("")

    flags = audit(ctx, char_id, starters, quests)
    if flags:
        a(ctx.t("flags_head", len(flags)))
        a("")
        for f in flags:
            a("  ! " + f)
        a("")

    return "\n".join(out) + "\n"


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--lang", choices=["en", "zh_cn", "both"], default="both",
                    help="which export to write (default: both)")
    ap.add_argument("--character", action="append",
                    help="only this character (repeatable), e.g. --character alice")
    args = ap.parse_args()

    langs = [("en", EN_LANG), ("zh_cn", ZH_LANG)] if args.lang == "both" \
        else [(args.lang, EN_LANG if args.lang == "en" else ZH_LANG)]
    ctxs = [Ctx(lang, path) for lang, path in langs]

    entities = {v["character"] for c in ctxs for v in list(c.starters.values()) + list(c.quests.values())}
    order = sorted(entities, key=plain_loc)
    if args.character:
        wanted = {c.lower() for c in args.character}
        order = [e for e in order if plain_loc(e) in wanted or plain_loc(e).split("_")[0] in wanted]

    for ctx in ctxs:
        out_dir = os.path.join(OUT, ctx.id)
        os.makedirs(out_dir, exist_ok=True)
        for e in order:
            name = plain_loc(e)
            text = render_character(ctx, name, ctx.entity_name(e))
            path = os.path.join(out_dir, name + ".txt")
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)
            print("wrote %s (%d lines)" % (os.path.relpath(path, ROOT), text.count("\n")))

    notes = sorted({n for ctx in ctxs for n in ctx.notes})
    if notes:
        print("\nreview notes (%d):" % len(notes))
        for n in notes:
            print("  " + n)


if __name__ == "__main__":
    main()
