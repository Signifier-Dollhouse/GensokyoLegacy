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

Each document opens with a DIALOG ANIMATIONS section listing every trigger a
dialog may name, resolved against that character's rig, and every `animations`
list in the body is annotated with the clip the trigger actually plays. That
resolution is read out of the Java sources and the GeckoLib asset rather than
hand-listed here, so the review text cannot claim a clip the game no longer has.
"""

import argparse
import json
import os
import re
import unicodedata
from collections import OrderedDict, Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src/generated/resources/data/gensokyolegacy/gensokyolegacy")
DATA_MAP = os.path.join(ROOT, "src/generated/resources/data/gensokyolegacy/data_maps/entity_type/default_dialog.json")
GIFT_DATA_MAP = os.path.join(ROOT,
                              "src/generated/resources/data/gensokyolegacy/data_maps/entity_type/gift_dialog.json")
EN_LANG = os.path.join(ROOT, "src/generated/resources/assets/gensokyolegacy/lang/en_us.json")
ZH_LANG = os.path.join(ROOT, "src/main/resources/assets/gensokyolegacy/lang/zh_cn.json")
OUT = os.path.join(ROOT, "out")

JAVA = os.path.join(ROOT, "src/main/java/dev/xkmc/gensokyolegacy")
YOUKAI_ANIM_JAVA = os.path.join(JAVA, "content/entity/youkai/impl/YoukaiAnim.java")
GEO_YOUKAI_JAVA = os.path.join(JAVA, "content/entity/youkai/impl/GeoYoukaiAnim.java")
GL_ENTITIES_JAVA = os.path.join(JAVA, "init/registrate/GLEntities.java")
ANIM_ASSETS = os.path.join(ROOT, "src/main/resources/assets/gensokyolegacy/animations")

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

    "section_anims": ("## DIALOG ANIMATIONS", "## 对话动画选项"),
    "anim_how1": ("A dialog may carry `animations`, a list of trigger names. The line plays one of",
                  "每条对话可带 `animations`，内容是触发名列表。台词出现时会从中随机"),
    "anim_how2": ("them once, rolled fresh every time the line opens. Leaving the list out is NOT",
                  "取出一个播放，每次出现都重新抽取。**不写该字段并不等于没有动画**："),
    "anim_how3": ('"no animation" - an empty list plays a random `交流_*` talk clip instead. A',
                  "留空（列表为空）会自动随机播放一段 `交流_*` 交谈动画。枚举槽名"),
    "anim_how4": ("trigger her rig does not map plays nothing at all, and the same trigger means",
                  "（如 `THINK`）同样有效。触发名按角色分别解析：若该角色的资源中"),
    "anim_how5": ("a different clip on a different character.",
                  "没有对应片段，则什么也不会播放；同一触发名在不同角色身上"),
    "anim_how6": ("Every name below resolves to the clip she actually plays, read out of",
                  "也可能对应不同的片段。下表中每个触发名对应的片段，均由"),
    "anim_how7": ("%s and %s.",
                  "`%s` 与 `%s` 直接解析得出。"),
    "anim_rig": ("rig: %s", "动画资源：%s"),
    "anim_no_rig": ("rig: could not be resolved for %s from the Java sources",
                    "动画资源：无法从 Java 源码中解析出 %s 的资源"),
    "col_trigger": ("trigger", "触发名"),
    "col_slot": ("slot", "枚举槽"),
    "col_len": ("length", "时长"),
    "col_bones": ("bones", "骨骼"),
    "col_used": ("used", "使用"),
    "col_clip": ("clip", "片段"),
    "anim_code": ("fired by code, not addressable from dialog data:",
                  "以下由代码触发，不能写进对话数据："),
    "anim_nothing": ("(no clip on this rig)", "（该资源上没有此片段）"),
    "anim_unused": ("unused triggers: %s", "未被使用的触发名：%s"),
    "anim_all_unused": ("unused triggers: none", "未被使用的触发名：无"),
    "anim_usage": ("of her %d dialogs, %d name an animation and the other %d fall back to a random talk clip.",
                   "她的 %d 条对话中，%d 条指定了动画，其余 %d 条使用随机交谈动画。"),
    "anim_usage_none": ("none of her %d dialogs names an animation; they all fall back to a random talk clip.",
                        "她的 %d 条对话都没有指定动画，全部使用随机交谈动画。"),
    "anim_triggered": ("the greeting line plays %s by itself (code-triggered).",
                       "问候语由代码直接播放 %s。"),
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

    "anim_multi": ("[anim] one of these, picked at random:", "[动作] 以下随机播放其一："),
    "anim_clip": ("[anim] %s -> %s  %s  %d bones", "[动作] %s -> %s  %s  %d 根骨骼"),
    "anim_len": ("%.2fs", "%.2f 秒"),
    "anim_nolen": ("(no length)", "（无时长）"),
    "anim_no_clip": ("[anim] %s -> NOT MAPPED on %s (nothing plays)", "[动作] %s -> %s 没有对应片段（什么也不会播放）"),
    "anim_no_trigger": ("[anim] %s -> not a YoukaiAnim trigger", "[动作] %s -> 不是有效的动画触发名"),
    "anim_missing_asset": ("clip '%s' is not in %s", "资源 %s 中没有片段“%s”"),
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
    "flag_anim_unknown": ("%s: animation '%s' is not a YoukaiAnim trigger, so it is ignored",
                          "%s：动画“%s”不是有效的触发名，将被忽略"),
    "flag_anim_unmapped": ("%s: animation '%s' maps to no clip on %s, so nothing plays",
                           "%s：动画“%s”在 %s 上没有对应片段，不会播放"),
    "flag_anim_no_clip": ("%s: animation '%s' wants clip '%s', which is not in %s",
                          "%s：动画“%s”指向片段“%s”，但 %s 中没有该片段"),
}

EFFECT_NAMES = {
    "minecraft:haste": ("Haste", "急迫"),
    "minecraft:bad_omen": ("Bad Omen", "不祥之兆"),
    "gensokyolegacy:looting": ("Looting (mod)", "抢夺（mod）"),
}

SPECIAL_WORDS = {"tnt": "TNT", "jukebox": "Jukebox"}

# `case GREET, TALK_01, ... -> Optional.of(X);` / `-> Optional.empty();`
CASE_RE = re.compile(r'case\s+(.+?)\s*->\s*Optional\.(?:of\(\s*(\w+)\s*\)|empty\(\))', re.S)
# `RawAnimation TALK_01 = RawAnimation.begin().thenPlay("交流_01");`
CLIP_RE = re.compile(r'RawAnimation\s+(\w+)\s*=\s*RawAnimation\.begin\(\)\.then(PlayAndHold|Play|Loop)\("([^"]+)"\)')
# `GensokyoLegacy.loc("animations/marisa.animation.json")`
ASSET_RE = re.compile(r'loc\("animations/([^"/]+)\.animation\.json"\)')


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def load_text(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def strip_comments(src):
    """Drop `//` and `/* */` comments.

    These rigs comment out cases for exactly the slots they do not ship, so
    parsing the raw text would read a disabled case as a live mapping.
    """
    out = []
    in_block = False
    for line in src.split("\n"):
        kept, i, quote = [], 0, None
        while i < len(line):
            if in_block:
                if line.startswith("*/", i):
                    in_block, i = False, i + 2
                else:
                    i += 1
                continue
            ch = line[i]
            if quote:
                if ch == "\\" and i + 1 < len(line):
                    kept.append(line[i:i + 2])
                    i += 2
                    continue
                if ch == quote:
                    quote = None
            elif line.startswith("//", i):
                break
            elif line.startswith("/*", i):
                in_block, i = True, i + 2
                continue
            elif ch in "\"'":
                quote = ch
            kept.append(ch)
            i += 1
        out.append("".join(kept))
    return "\n".join(out)


def display_width(s):
    """Column width in a terminal: CJK and full-width punctuation take two cells."""
    return sum(2 if unicodedata.east_asian_width(c) in "WF" else 1 for c in s)


def pad(s, width):
    return s + " " * max(0, width - display_width(s))


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


_JAVA_INDEX = None


def java_index():
    """{class simple name: path}, so an entity class found in GLEntities can be located."""
    global _JAVA_INDEX
    if _JAVA_INDEX is None:
        _JAVA_INDEX = {}
        for dirpath, _, files in os.walk(JAVA):
            for fn in files:
                if fn.endswith(".java"):
                    _JAVA_INDEX[fn[: -len(".java")]] = os.path.join(dirpath, fn)
    return _JAVA_INDEX


_SLOTS = None
_TRIGGER_SLOTS = None


def anim_slots():
    """`YoukaiAnim`: enum slot -> trigger name, in declaration order.

    Declaration order is the entity-event id order (`ANIM_EVENT_BASE + ordinal`),
    so the order here is also the order the enum must keep.
    """
    global _SLOTS, _TRIGGER_SLOTS
    if _SLOTS is None:
        src = strip_comments(load_text(YOUKAI_ANIM_JAVA))
        _SLOTS = OrderedDict(re.findall(r'\b([A-Z][A-Z_0-9]*)\("([^"]+)"\)', src))
        _TRIGGER_SLOTS = {t: s for s, t in _SLOTS.items()}
    return _SLOTS


def slot_of(trigger):
    """Resolve a dialog trigger name: by trigger first, then by enum slot name."""
    anim_slots()
    return _TRIGGER_SLOTS.get(trigger, trigger.upper() if trigger.upper() in _SLOTS else None)


_EXTRA = None


_SHARED_CLIPS = None


def shared_clips():
    """Clips declared on `GeoYoukaiAnim` rather than on the character.

    `EAT` and `HURT` are named the same in every rig that has them, so the chew and
    the flinch live on the interface and a character maps `case EAT ->` to an
    inherited constant instead of repeating it eight times. Resolving a slot's clip
    therefore has to look at the interface too, or those two rows would read as
    unmapped.
    """
    global _SHARED_CLIPS
    if _SHARED_CLIPS is None:
        src = strip_comments(load_text(GEO_YOUKAI_JAVA))
        _SHARED_CLIPS = {c: clip for c, _, clip in CLIP_RE.findall(src)}
    return _SHARED_CLIPS


class AnimRig:
    """One character's rig: what each dialog trigger actually plays, for her.

    Resolved from the Java sources and the GeckoLib asset rather than listed by
    hand, so the review text cannot advertise a clip the game no longer has:
    `GLEntities` gives the entity class for a character id, the class gives both
    the slot -> clip switch and the constants naming the clips, and the model
    gives the asset those clip names live in. Everything is optional - a rig that
    cannot be resolved degrades to "no clip", never to a crash.
    """

    def __init__(self, char_id):
        self.char_id = char_id
        self.slots = anim_slots()
        self.entity_class = None
        self.entity_file = None
        self.asset = None
        self.clips = {}
        self.slot_clip = {}          # slot -> clip name, or absent when Optional.empty()
        self._resolve()

    def _resolve(self):
        src = strip_comments(load_text(GL_ENTITIES_JAVA))
        self.entity_class = (dict(re.findall(r'\.entity\(\s*"([a-z0-9_]+)"\s*,\s*(\w+)::new', src))
                             .get(self.char_id))
        self.entity_file = java_index().get(self.entity_class)
        if not self.entity_file:
            return
        src = strip_comments(load_text(self.entity_file))
        # the character's own constants win over the interface's: a rig free to
        # override `EAT` with a differently named clip does so by shadowing.
        consts = {**shared_clips(), **{c: clip for c, _, clip in CLIP_RE.findall(src)}}
        at = src.find("getAnim(YoukaiAnim")
        if at < 0:
            return
        for names, const in CASE_RE.findall(src[at:src.find("};", at)]):
            for slot in (n.strip() for n in names.split(",")):
                self.slot_clip[slot] = consts.get(const) if const else None
        model = self._model_file()
        if model:
            found = ASSET_RE.search(strip_comments(load_text(model)))
            self.asset = found.group(1) if found else None
        if self.asset:
            path = os.path.join(ANIM_ASSETS, self.asset + ".animation.json")
            if os.path.exists(path):
                self.clips = {name: {"length": a.get("animation_length"),
                                     "loop": bool(a.get("loop")),
                                     "bones": list((a.get("bones") or {}).keys())}
                              for name, a in load_json(path).get("animations", {}).items()}

    def _model_file(self):
        """`<Name>Model.java` next to the entity, else any model in that package."""
        d = os.path.dirname(self.entity_file)
        base = self.entity_class[: -len("Entity")] if self.entity_class.endswith("Entity") \
            else self.entity_class
        want = os.path.join(d, base + "Model.java")
        if os.path.exists(want):
            return want
        for fn in sorted(os.listdir(d)):
            if fn.endswith("Model.java"):
                return os.path.join(d, fn)
        return None

    # -- lookup --------------------------------------------------------------

    def clip_of(self, trigger):
        """(slot, clip name) for a trigger; slot is None when the name is unknown."""
        slot = slot_of(trigger)
        return slot, (self.slot_clip.get(slot) if slot else None)

    def info(self, clip):
        """Length / loop / bones of a clip in this rig's asset, or None."""
        return self.clips.get(clip)


_RIGS = {}


def rig_for(char_id):
    if char_id not in _RIGS:
        _RIGS[char_id] = AnimRig(char_id)
    return _RIGS[char_id]


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
        # the gift reactions are opened by the module rather than by a starter, so they are reached
        # from here rather than from the topic list; without this they audit as unreachable
        self.gift_dialogs = load_json(GIFT_DATA_MAP)["values"]
        self.rig = None                      # set per character by render_character
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

    # -- animations ----------------------------------------------------------

    def anim_line(self, trigger, indent):
        """One `animations` entry, resolved to the clip this character plays."""
        slot, clip = self.rig.clip_of(trigger) if self.rig else (None, None)
        if slot is None:
            return [indent + self.t("anim_no_trigger", trigger)]
        if not clip:
            return [indent + self.t("anim_no_clip", trigger, self.entity_name(self.rig.char_id))]
        info = self.rig.info(clip)
        length = self.t("anim_nolen") if not info or info["length"] is None \
            else self.t("anim_len", info["length"])
        bones = len(info["bones"]) if info else 0
        return [indent + self.t("anim_clip", trigger, clip, length, bones)]

    def anim_lines(self, animations, indent):
        """The `animations` list of a dialog, annotated under the line it belongs to."""
        if not animations:
            return []
        if len(animations) == 1:
            return self.anim_line(animations[0], indent)
        lines = [indent + self.t("anim_multi")]
        for trig in animations:
            lines += self.anim_line(trig, indent + "  ")
        return lines


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
    lines += ctx.anim_lines(d.get("animations"), indent + "  ")
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


# Slots that exist in YoukaiAnim but are never named by dialog data: the hand use
# and the chew and the flinch are fired by behaviour tasks, the eating and damage
# paths, the outdoor idle by the wander task, and the skills by spell cards. They
# resolve like any other trigger, so the table lists them separately rather than
# pretending they are dialog options. This mirrors YoukaiAnim#isDialog.
CODE_ONLY_SLOTS = ("USE_MAINHAND", "OUTDOOR_IDLE", "SKILL_01", "SKILL_02", "SKILL_03", "EAT", "HURT")


def collect_anims(ctx, starters, quests):
    """{trigger: dialogs naming it} plus how many dialogs this character reaches."""
    counts = Counter()
    seen, total = set(), 0

    def walk(did):
        nonlocal total
        key = plain_loc(did)
        if key in seen:
            return
        seen.add(key)
        d = ctx.dialogs.get(key)
        if d is None:
            return
        total += 1
        for trig in d.get("animations", []):
            counts[trig] += 1
        for o in d.get("options", []):
            for target in [o.get("next")] + [e.get("next") for e in o.get("entries", [])]:
                if target:
                    walk(target)

    for s in starters.values():
        walk(s["dialog"])
    for q in quests.values():
        for field in ("initialDialog", "followUpDialog", "completionDialog"):
            if q[field].get("next"):
                walk(q[field]["next"])
    return counts, total


def render_anim_section(ctx, rig, counts, total):
    """The DIALOG ANIMATIONS section: every option, resolved against this rig."""
    out = []
    a = out.append
    a("")
    a(ctx.t("section_anims"))
    a("")
    for key in ("anim_how1", "anim_how2", "anim_how3", "anim_how4", "anim_how5", "anim_how6"):
        a("  " + ctx.t(key))
    if rig.asset and rig.entity_class:
        a("  " + ctx.t("anim_how7", os.path.basename(rig.entity_file),
                        "assets/gensokyolegacy/animations/" + rig.asset + ".animation.json"))
    a("")
    if not rig.asset:
        a("  " + ctx.t("anim_no_rig", ctx.entity_name(rig.char_id)))
        a("")
        return out
    a("  " + ctx.t("anim_rig", "%s -> assets/gensokyolegacy/animations/%s.animation.json"
                   % (rig.entity_class, rig.asset)))
    a("")

    cols = [ctx.t("col_trigger"), ctx.t("col_slot"), ctx.t("col_len"),
            ctx.t("col_bones"), ctx.t("col_used"), ctx.t("col_clip")]

    def used(slot):
        """How many of her dialogs name this slot, by trigger or by enum name."""
        return counts.get(rig.slots[slot], 0) + counts.get(slot, 0)

    def row(slot):
        clip = rig.slot_clip.get(slot)
        info = rig.info(clip) if clip else None
        return [rig.slots[slot], slot,
                ctx.t("anim_nolen") if not info or info["length"] is None
                else ctx.t("anim_len", info["length"]),
                str(len(info["bones"])) if info else "-",
                str(used(slot)),
                clip if info is not None else
                (ctx.t("anim_nothing") if not clip else
                 clip + "   <-- " + ctx.t("anim_missing_asset", clip, rig.asset))]

    def emit(r):
        return "  " + "  ".join(pad(r[i], widths[i]) for i in range(5)) + "  " + r[5]

    dialog_rows = [row(s) for s in rig.slots if s not in CODE_ONLY_SLOTS]
    code_rows = [row(s) for s in CODE_ONLY_SLOTS if s in rig.slots]
    widths = [max(display_width(cols[i]),
                  max([display_width(r[i]) for r in dialog_rows + code_rows], default=0))
              for i in range(5)]
    a("  " + "  ".join(pad(cols[i], widths[i]) for i in range(5)) + "  " + cols[5])
    for r in dialog_rows:
        a(emit(r))
    a("")
    a("  " + ctx.t("anim_code"))
    for r in code_rows:
        a(emit(r))
    greet = rig.slot_clip.get("GREET")
    if greet:
        a("  " + ctx.t("anim_triggered", greet))
    a("")
    unused = [rig.slots[s] for s in rig.slots if s not in CODE_ONLY_SLOTS and not used(s)]
    a("  " + ctx.t("anim_unused", ", ".join(unused)) if unused
      else "  " + ctx.t("anim_all_unused"))
    named = sum(counts.values())
    a("  " + ctx.t("anim_usage", total, named, total - named) if named
      else "  " + ctx.t("anim_usage_none", total))
    a("")
    return out


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
    for entry in ctx.gift_dialogs.values():
        for which in ("like", "dislike"):
            did = plain_loc(entry.get(which, ""))
            if did.startswith(char_id + "/"):
                walk(did)
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

    # animations that resolve to nothing: a bad name is dropped silently, and an
    # unmapped slot plays nothing, so neither shows up in game as a problem
    if ctx.rig is not None:
        for did in sorted(used_dialogs):
            for trig in ctx.dialogs.get(did, {}).get("animations", []):
                slot, clip = ctx.rig.clip_of(trig)
                if slot is None:
                    notes.append(ctx.t("flag_anim_unknown", loc(did), trig))
                elif not clip:
                    notes.append(ctx.t("flag_anim_unmapped", loc(did), trig,
                                       ctx.entity_name(char_id)))
                elif ctx.rig.info(clip) is None:
                    notes.append(ctx.t("flag_anim_no_clip", loc(did), trig, clip,
                                       ctx.rig.asset))

    return notes


def render_character(ctx, char_id, display):
    out = []
    a = out.append
    starters = OrderedDict((k, v) for k, v in ctx.starters.items() if plain_loc(v["character"]) == char_id)
    quests = OrderedDict((k, v) for k, v in ctx.quests.items() if plain_loc(v["character"]) == char_id)
    ctx.rig = rig_for(char_id)

    a(rule("="))
    a("%s   (gensokyolegacy:%s)" % (display, char_id))
    a(ctx.t("lang_line"))
    for k in ("legend1", "legend2", "legend3", "legend4", "legend5", "legend6"):
        if LABELS[k][ctx.i] is not None:
            a(ctx.t(k))
    a(rule("="))
    a("")

    counts, total = collect_anims(ctx, starters, quests)
    out.extend(render_anim_section(ctx, ctx.rig, counts, total))

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
