# Grouping Duplicate Dialog Options

## Problem

`ServerCharacterDialogManager.getInitialConversation` (src/main/java/dev/xkmc/gensokyolegacy/content/rpg/core/ServerCharacterDialogManager.java:63) collects one `IDialogHandle` per available starter / quest-state / trade, and `FirstDialogMenu` renders every handle as a horizontal option button.

Multiple handles can resolve to the **same displayed button text** (for example several quests whose `initialDialog`/`followUpDialog` options share the same lang key, or several `DialogStarter` entries sharing one starter key). The player then sees N identical buttons; guessing which one does what is luck.

## Desired Behavior

Only when **2 or more handles share the same text key** at the same time, collapse them into a single group:

```
Old:  starter A/B/C (same message)            -> button=A  dialog A
                                                -> button=B  dialog B
                                                -> button=C  dialog C

New:  one grouped button (shared message)
        -> character re-asks "which?" (body)
              option A (quest title)          -> dialog A
              option B (quest title)          -> dialog B
              option C (quest title)          -> dialog C
```

A handle that is the only one with its key is untouched (no data changes, no extra click for the common case).

## Design

The grouping is a **runtime transformation**, computed server-side at the end of `getInitialConversation`. No datapack schema / lang / generated JSON changes.

### 1. Grouping key: `IDialogHandle.groupKey()`

Grouping keys on the **raw translation key**, not the rendered string. Add to the interface:

```java
// content/rpg/handle/IDialogHandle.java
Component display();                 // existing: rendered button text

default String text() { return ""; } // raw translation key backing display()

default String groupKey() { return text(); }   // grouping key, override to group by something else

default Component groupLabel() { return display(); } // label shown INSIDE a group
```

`text()` is the translation key of the button. Override per handle:

* `DialogHandle` → `starter.value().text()` (the `DialogStarter` key).
* `QuestHandle` → `dialog.text()`, which requires exposing `String text()` on the `DialogOption` interface — `SimpleDialogOption` already stores it as a record component (it is the only implementation; the interface just declares it).
* `TradeHandle` → dialog-config trade key when set, else the lang key: `cfg != null && !cfg.trade().isEmpty() ? cfg.trade() : GLLang.Trade.OPTION.key()` (mirrors `TradeHandle.display`, GLLang.java:210).
* `ClientHandle` → the defaults. It is the client-side network mirror of an **already-grouped** list and never participates in grouping, so `text()`/`groupKey()` stay unused (`""`).

### 2. Distinguish inside a group: `IDialogHandle.groupLabel()`

Default = `display()`. `QuestHandle` overrides to the quest title:

```java
// content/rpg/handle/QuestHandle.java
@Override
public Component groupLabel() {
    return Component.translatable(quest.value().title());
}
```

`title()` is the already-keyed quest title registered via `QuestDialogData.questTitle()` — no new lang work.

**Identical labels inside a group are allowed.** If two members produce the same `groupLabel()` (e.g. a quest title collides, or two non-quest members fall back to `display()`), they are simply shown twice — no dedup, no suffixing, no special treatment. The quest preview on hover (`FirstDialogScreen.renderQuestInfo`) is what lets the player tell them apart.

### 3. New `GroupHandle`

```java
// content/rpg/handle/GroupHandle.java
public record GroupHandle(Component display, List<IDialogHandle> members) implements IDialogHandle {

    @Override
    public Component display() { return display; }

    @Override
    public void openMenu(ServerPlayer sp, YoukaiEntity character) {
        FirstDialogProvider.openGroup(sp, character, this);
    }

    @Override
    public Optional<Holder<Quest>> getQuest() { return Optional.empty(); }

    @Override
    public Component groupLabel() { return display; }
}
```

* `display` = the shared key's `Component` (first member's display, preserving insertion order).
* `openMenu` reuses the first-dialog menu (see §4/§5) — **no dedicated group menu/screen**.
* `getQuest()` stays empty — a group is not a quest.

### 4. Reuse the first-dialog menu/screen (answer: yes, reuse)

The group sub-menu shows avatar + frame, a body line, clickable option buttons, and quest preview on hover — exactly what `FirstDialogMenu`/`FirstDialogScreen` already do. Its only differences are the handle list (the group members) and the body text (the shared starter message instead of the `DialogConfig` greeting).

The sub-menu is therefore rendered by the **same** `GLMisc.DIALOG_FIRST` menu type and the **same** `FirstDialogScreen`; no new menu/screen/register entry is created. The only generalization needed is letting the provider/menu carry an optional body override:

```java
// content/ui/dialog/FirstDialogProvider.java
public static void open(ServerPlayer sp, YoukaiEntity ch) { ... }     // unchanged, body = null -> greeting

public static void openGroup(ServerPlayer sp, YoukaiEntity ch, GroupHandle group) {
    // handles = group.members()
    // options = members sorted as ClientHandles (display = groupLabel(), quest = getQuest())
    // body    = group.display()
    openWith(sp, ch, handles, options, body);
}

// write() gains the optional body (client cannot recompute the group, so it must be explicit):
buf.writeVarInt(ch.getId());
buf.writeBoolean(body != null);
if (body != null) ComponentSerialization.STREAM_CODEC.encode(buf, body);
buf.writeVarInt(options.size());
// ... existing ClientHandle loop
```

```java
// content/ui/dialog/FirstDialogMenu.java
// fromNetwork(): read the boolean + optional body before the size, store on the menu.
// new @Nullable Component body field; getBodyText(): body if set, else the existing greeting fallback.
```

No `FirstDialogScreen` change — it already renders `menu.getBodyText()` and handles clicks through `clickMenuButton` (FirstDialogMenu.java:69-78), which calls each member handle's `openMenu` → member's own `SimpleDialogProvider` / trade menu.

## Implementation Checklist

1. `content/rpg/dialog/DialogOption.java` — add `String text();`.
2. `content/rpg/handle/IDialogHandle.java` — add `text()`, `groupKey()`, `groupLabel()` defaults.
3. `content/rpg/handle/QuestHandle.java` — override `groupLabel()` → quest title; `text()` → `dialog.text()`.
4. `content/rpg/handle/DialogHandle.java` — override `text()` → `starter.value().text()`.
5. `content/rpg/handle/TradeHandle.java` — override `text()` → config trade key or `GLLang.Trade.OPTION.key()`.
6. `content/rpg/handle/GroupHandle.java` — new record.
7. `content/rpg/core/ServerCharacterDialogManager.java` — add group pass keyed by `groupKey()`:

```java
return groupHandles(ans);

private static List<IDialogHandle> groupHandles(List<IDialogHandle> raw) {
    LinkedHashMap<String, List<IDialogHandle>> map = new LinkedHashMap<>();
    for (var h : raw)
        map.computeIfAbsent(h.groupKey(), k -> new ArrayList<>()).add(h);
    List<IDialogHandle> ans = new ArrayList<>();
    for (var e : map.values()) {
        if (e.size() == 1) ans.add(e.get(0));
        else ans.add(new GroupHandle(e.get(0).display(), e));
    }
    return ans;
}
```

8. `content/ui/dialog/FirstDialogProvider.java` — optional body + `openGroup(...)`.
9. `content/ui/dialog/FirstDialogMenu.java` — decode/store optional body.
10. No `runData` / lang / mixin / registrate changes. Compile via `./gradlew compileJava`.

## Edge Cases & Decisions

* **Grouping key is the translation key** (`groupKey()`/`text()`), giving locale-stable grouping; all our dialog texts are plain translatable keys.
* **Identical `groupLabel()`s are shown as-is** — no dedup/suffix. Quest preview on hover disambiguates.
* **Deterministic**: grouping derives entirely from current player state + data; `FirstDialogMenu.create` recomputes on the server and sees the same list, so server-side click handling stays consistent.
* **Trade in a group**: `getQuest()` empty → no preview; still opens the trade menu via its own handle.
* **State-specific grouping**: start/follow-up/completable of the same quest chain may appear on different days with the same option text — they are grouped/ungrouped per current state, which is exactly the "only when 2+ at the same time" rule.
* **Single handle unchanged** — no behavior or data regression for the common one-quest case.

## Out of Scope (future)

Same textual-collision problem exists inside one `Dialog` file's `options` (two `SimpleDialogOption`s with a shared key). Grouping there would need them to become a nested sub-dialog rather than a sub-menu of arbitrary handles; defer unless data authoring hits it. This plan only covers the initial-conversation handle list.