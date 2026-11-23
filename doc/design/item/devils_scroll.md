# The Devil's Scroll

A one-handed scroll with three stances — **Passive**, **Superhuman** and **Devil** — switched from
the item wheel. It is a melee weapon in the main hand, a support charm in the off hand, a short
dash that bends onto a target under the crosshair, and a ring of orange lasers that answers every
hit its bearer lands.

> **Status:** implemented. Numbers below are first guesses and are meant to be tuned; the playtest
> checklist is §10.
> **Package:** `content/item/devil/` (item, mode, selector, model), `content/spell/item/` (the dash
> ticker), `event/` (damage, fall, knockback).
> **Related:** `minamitsu_anchor.md` (mode wheel and selection listener), `dagger_glove.md`
> (mode component and wheel), `doll/glove.md` (the shared crosshair target cache), `fancy_fan.md`
> (a shove applied by hand rather than by a projectile).

## 1. What it is

The scroll is held, not thrown. Its attributes depend only on **which hand holds it**; its mode
decides which of its active abilities run.

| | main hand | off hand |
|---|---|---|
| attack damage | **+7** (flat) | **+20%** (of base) |
| movement speed | **+100%** (of base) | **+50%** (of base) |

The attributes apply in every mode. The modes gate the rest:

| effect | requires |
|---|---|
| attack cooldown bypass (`BYPASS_COOLDOWN` on `player_attack`) | main hand + **Superhuman** |
| Jump Boost II, fall-damage immunity, knockback immunity | main hand + **Superhuman** |
| special melee knockback (ground / sneaking / airborne) | main hand + **Superhuman** |
| right-click dash | main hand + **Superhuman** |
| dash aim assist + target glow (crosshair cache) | main hand + **Superhuman** |
| four orange lasers on dealing damage | either hand + **Devil** |

So a scroll in the off hand in Superhuman mode is only a stat stick, and a scroll in the main hand
in Devil mode trades the superhuman melee package for the lasers. **Passive** is the plain item:
attributes only, no active ability at all, and the default a freshly obtained scroll starts in. The
wheel is how the player picks which trade they want.

## 2. Components

| Piece | File | Job |
|---|---|---|
| `DevilScrollItem` | `content/item/devil/DevilScrollItem.java` | item, attributes, `use` (dash), `inventoryTick` (jump boost), laser summon helper, the scroll's `GloveTargeting` |
| `GloveTargetCache` / `GloveTargetAttachment` | `content/item/common/` | the shared crosshair target cache the dash reads (§5.5); no scroll-specific copy |
| `DevilScrollMode` | `content/item/devil/DevilScrollMode.java` | `PASSIVE / SUPERHUMAN / DEVIL`, display name, description, wheel entry |
| `DevilScrollSelectionListener` | `content/item/devil/DevilScrollSelectionListener.java` | wheel/list registration and mode swap |
| `DevilScrollModeWheel` | `content/item/devil/client/DevilScrollModeWheel.java` | the client wheel |
| `DevilScrollModel` | `content/item/devil/DevilScrollModel.java` | one model per mode, selected by the mode predicate |
| `DevilScrollDash` | `content/spell/item/DevilScrollDash.java` | `ItemSpell` that sets the dash velocity and runs the sweep ticker |
| `DevilScrollAim` | `content/item/devil/client/DevilScrollAim.java` | client camera lock onto the cached target (§5.5) |
| `DevilScrollEventHandlers` | `event/DevilScrollEventHandlers.java` | lasers on damage, fall immunity, knockback rules |
| `GLAttackListener` | `event/GLAttackListener.java` | adds the `BYPASS_COOLDOWN` state on `player_attack` |
| `GLItems` | `init/registrate/GLItems.java` | item entry and `DEVILS_SCROLL_MODE` component |
| `GensokyoLegacy` | `init/GensokyoLegacy.java` | registers the selector in `commonSetup` |
| `GLClient` | `init/GLClient.java` | registers the `devils_scroll_mode` item predicate |

## 3. Attributes

The four modifiers are declared once, as a constant on `DevilScrollItem`, and attached in the
constructor. The ids must be **distinct per hand**: `LivingEntity` stores applied modifiers by id, so
two entries sharing an id collapse into one when a scroll is in each hand.

```java
public static final ItemAttributeModifiers ATTRIBUTES = ItemAttributeModifiers.builder()
        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        GensokyoLegacy.loc("devils_scroll_main_damage"), 7,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND)
        .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                        GensokyoLegacy.loc("devils_scroll_main_speed"), 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.MAINHAND)
        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        GensokyoLegacy.loc("devils_scroll_off_damage"), 0.2,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.OFFHAND)
        .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                        GensokyoLegacy.loc("devils_scroll_off_speed"), 0.5,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.OFFHAND)
        .build();
```

`ADD_MULTIPLIED_BASE` is the "+X% of base" reading, matching the percentages in the spec. The
`ATTRIBUTE_MODIFIERS` component replaces the item's default modifiers entirely, which is fine here
because the scroll has no other modifiers. Vanilla already prints the "When in Main Hand / Off Hand"
tooltip sections from the component.

The main-hand set does **not** include an attack-speed modifier. `BYPASS_COOLDOWN` (below) removes
the target's i-frames, not the attacker's swing timer; see §9.

## 4. Modes and the wheel

The mode is a stack component, like the anchor's, so it travels with the item and is visible to both
sides.

```java
public static final DCVal<DevilScrollMode> DEVILS_SCROLL_MODE =
        DC.enumVal("devils_scroll_mode",
                EnumCodec.of(DevilScrollMode.class, DevilScrollMode.values()));
```

`DevilScrollItem.getMode(stack)` defaults to `PASSIVE`. The wheel mirrors
`AnchorSelectionListener` / `AnchorModeWheel`:

- `DevilScrollSelectionListener.getHeldScroll(player)` checks the main hand, then the off hand.
- `test` is `stack.getItem() instanceof DevilScrollItem`.
- `getList` returns one `mode.icon()` per mode, each named after the mode.
- `swap` writes the mode to the held scroll; the wheel also writes it optimistically on the client,
  then sends `SelectorSelectPacket`.
- `get` returns the wheel for `wheelIndex == 0`.

`DevilScrollMode.icon()` is a fresh scroll with the mode set and `DataComponents.ITEM_NAME` replaced
by the mode's display name. The model predicate then draws the matching texture, so the wheel, the
selector list and the held item all show the same per-mode art.

Register the selector in `GensokyoLegacy.commonSetup`, beside the anchor's:

```java
DevilScrollSelectionListener.register();
```

## 5. Superhuman mode

All of this is gated on `DevilScrollItem.isSuperhuman(player)`: the scroll is in the main hand and
its mode is `SUPERHUMAN`.

### 5.1 Attack cooldown bypass

`BYPASS_COOLDOWN` is a `DamageState` from l2damagetracker and maps to
`DamageTypeTags.BYPASSES_COOLDOWN`, the tag `LivingEntity#hurt` checks before rejecting a hit that
lands inside the target's i-frames. Enable it on the player's own melee source in
`GLAttackListener#onCreateSource`:

```java
if (event.getOriginal().equals(DamageTypes.PLAYER_ATTACK)
        && event.getAttacker() instanceof Player player
        && DevilScrollItem.isSuperhuman(player)) {
    event.enable(DefaultDamageState.BYPASS_COOLDOWN);
}
```

This branch must go **before** the existing doll branch, which starts with
`if (!(event.getDirect() instanceof BaseDollEntity)) return;` — appended after it, the player branch
would never run. This makes repeated clicks land on the same target instead of being swallowed by
the 10-tick invulnerability window.

### 5.2 Jump Boost, fall damage, knockback

- **Jump Boost II** is re-applied from the item's `inventoryTick` every 20 ticks, for 40 ticks, at
  amplifier 1, ambient and without a visible icon — the `CentiPickaxe` pattern. Server side only,
  and only while the stack really is the main-hand item.
- **Fall damage immunity** is a cancelled `LivingFallEvent` for a player whose main hand is the
  scroll.
- **Knockback immunity** is a cancelled `LivingKnockBackEvent` when the *target* is that player.

### 5.3 Melee knockback

`LivingKnockBackEvent` gives the target, its strength and the two horizontal ratios, but not the
attacker. By the time it fires, `LivingEntity#hurt` has already called `setLastHurtByMob`, so the
attacker is `target.getLastHurtByMob()`.

| player state | result |
|---|---|
| sneaking | cancel the event; set the target's velocity to a strong **upward** launch (`y = 1.0`) |
| airborne, not sneaking | cancel the event; set the target's velocity to a strong **downward** slam (`y = -1.5`) |
| on the ground, not sneaking | keep the event; raise `strength` to at least `1.5` for a heavy horizontal shove |

Sneaking wins over airborne when both are true. Cancelling is what lets the vertical variants work:
vanilla caps the on-ground vertical at `0.4`, and it also halves the target's existing velocity, so
the custom variants set the whole velocity themselves. The horizontal part is kept from the event's
ratios, pointing away from the player. Both variants flag `hasImpulse` and, for a player target,
`hurtMarked` so the server sends the motion.

The handler only reacts when the attacker is a player holding the scroll in the main hand in
Superhuman mode. The target's own knockback immunity check (§5.2) runs first and returns.

### 5.4 Dash

Right-click in the main hand in Superhuman mode:

1. `use` checks the item cooldown (10 ticks), resolves the crosshair aim target (§5.5), and calls
   `SpellContainer.castSpell(sp, DevilScrollDash::new, target)`.
2. `DevilScrollDash.start` bends `dir` onto the target when it is within 20° of the view (§5.5),
   then sets the player's velocity to `dir * 3`, flags `hasImpulse` and `hurtMarked`, and zeroes
   `fallDistance`. The `hurtMarked` flag is what makes the server send the new motion, the same way
   the anchor's grapple does. It also records the pre-dash position for the first sweep segment.
3. A `Ticker` runs for up to 10 ticks. Each tick it sweeps an `AABB` from the previous position to
   the current one, inflated by the player's half-width plus `0.5`, and for each new living entity
   that is not the player, not an ally, not a passive animal/villager and still alive:
   - hurts it with `player.damageSources().playerAttack(player)` for the player's
     `ATTACK_DAMAGE` (which includes the scroll's `+7`);
   - lets vanilla's `hurt` apply its knockback, which the §5.3 handler then turns into the
     superhuman variant.
   A `List<Integer>` of already-hit entity ids stops one entity being hit twice in a single dash.
   Targets are filtered with `GrazeHelper.shouldPlayerHurt`, the same test danmaku uses.
4. The ticker ends after 10 ticks or when the player's speed drops below `0.2` blocks/tick (a wall).

`ItemSpell` is used only as the serialized per-player ticker container; the dash spawns no danmaku.
Both the spell and its ticker carry `@SerialClass`, and every field that must survive a save is
`@SerialField`, as in `YukariItemSpellLaser` and `SuikaSpell.ChargeTicker`.

### 5.5 Aim assist

The dash reads the same crosshair target cache as the glove items, by implementing
`GloveTargeting` (`doll/glove.md` §2). There is no scroll-specific cache: `GloveTargetCache` traces
the crosshair on the client, `GloveTargetPacket` feeds the hint to the server, and
`GloveTargetAttachment` stores it per item.

| member | value | why |
|---|---|---|
| `acceptsTarget` | mode `SUPERHUMAN`, main hand, a living entity the holder's danmaku could hurt | only the superhuman package can dash, and the off-hand scroll is a stat stick |
| `targetRange` | 32 blocks | covers the dash's own reach (3 × 10 ticks) |
| `targetTtl` | 40 ticks (2 s) | a glance at a target still aims a dash a moment later |
| `blockedByBlocks` | false | the dash plows into the wall rather than refusing to aim at what stands behind it |
| `targetGlow` | orange `0xFF8800` in `SUPERHUMAN` | the holder sees the cached target, as with the gloves |

`use` resolves the hint on both sides. The server re-runs `acceptsTarget` and passes the target to
`SpellContainer.castSpell`; `DevilScrollDash.start` then compares the view direction with the vector
from the eye to the target's centre and replaces `dir` when the angle is under `AIM_COS` (20°) or the
player is sneaking, so the dash bends onto the target. The client turns the camera
(`setYRot` / `setXRot` / `setYHeadRot`) onto the same centre in two places: once on the dash click in
`use`, and every client tick from `DevilScrollAim.lock`, driven by `GLClient`.

The tick lock is what makes sneaking a held lock-on: while the player sneaks with a superhuman scroll
in the main hand, the camera keeps tracking the cached target until the player stops sneaking or the
target leaves the cache. The dash click also locks for the non-sneaking 20° case.

The lock runs a second time from `RenderFrameEvent.Pre`, because `Minecraft.runTick` applies
`mouseHandler.handleAccumulatedMovement()` after the client ticks and before the frame is drawn. A
tick-only lock would therefore be overwritten by the mouse delta on every frame. That call passes
the frame's `DeltaTracker` partial tick, so the camera aims at the target's **interpolated** position
(`getPosition(partialTick)`, the same lerp the renderer uses) and at the player's interpolated eye:
locking onto the raw tick position would make the view step at 20 Hz while the mob is drawn at frame
rate. The tick call uses `1.0`, the current position, which is also what the server's dash reads.
`lockView` also sets `xRotO` / `yRotO` to the locked angles, so the partial-tick camera lerp has
nothing stale to blend in between the lock and the drawn frame.

`AIM_COS` lives on `DevilScrollItem` so both sides read one number. The server's bend stays
authoritative: the use packet can reach the server before the rotated movement packet, so the camera
lock is presentation while the dash direction is the contract.

**Sneaking skips the gate** on both sides and always takes the cached target: a sneak-dash commits
to a target even off the crosshair, and the lock keeps the camera on it while the key is held.
The rest of the dash is unchanged: it still needs a live cache entry, so this is "stop aiming, start
committing", not "dash at anything".

## 6. Devil mode

While a scroll in **either** hand has mode `DEVIL`, every damage event the player causes spawns the
four lasers. The trigger is `LivingDamageEvent.Post`, which is server-side and runs after the health
loss, so it fires once per real hit:

```java
if (event.getNewDamage() <= 0) return;
DamageSource source = event.getSource();
if (!(source.getEntity() instanceof ServerPlayer player)) return;
if (source.getDirectEntity() instanceof DanmakuLaserEntity) return; // the scroll's own lasers
if (!DevilScrollItem.isDevil(player)) return;
if (player.getCooldowns().isOnCooldown(GLItems.DEVILS_SCROLL.get())) return;
player.getCooldowns().addCooldown(GLItems.DEVILS_SCROLL.get(), 5);
DevilScrollItem.summonLasers(player, event.getEntity());
```

The `DanmakuLaserEntity` exclusion is load-bearing: the scroll's own lasers are owned by the player,
so without it a laser hit would spawn another set and the chain would never end. Bullets and arrows
are not excluded — only laser danmaku is, per the spec.

### 6.1 The four lasers

Origins are four points around the player's eyes: the eye position offset `±1.5` vertically and
`±1.5` along the player's horizontal right vector (the look vector crossed with up, normalised; a
straight-up look falls back to world +X).

Each laser aims at the **target that was just damaged**: the direction is
`target.getBoundingBox().getCenter() - origin`, normalised, with the player's look as the fallback
for a degenerate direction. All four beams therefore converge on the victim rather than firing
parallel.

| property | value |
|---|---|
| item / colour | `DanmakuItems.Laser.LASER`, `DyeColor.ORANGE` |
| prepare | 5 ticks |
| setup | 5 ticks |
| duration | 20 ticks |
| close | 5 ticks |
| damage | 6 |
| length | 80 blocks |
| cooldown | 5 ticks |
| walls | bypassed (`setup(..., true, dir)`), matching the existing player lasers |

`DanmakuLaserEntity#setupTime(prepare, start, life, end)` takes the four phase lengths directly, so
the spawn is:

```java
var laser = new ItemLaserEntity(DanmakuEntities.ITEM_LASER.get(), player, level);
laser.setItem(new ItemStack(DanmakuItems.Laser.LASER.get(DyeColor.ORANGE).get()));
laser.setup(6, 20, 80, true, dir);   // damage, duration, length, bypassWall, direction
laser.setupTime(5, 5, 20, 5);       // prepare, setup, duration, close
laser.setPos(origin);
EntityStorageHelper.fastAdd(level, laser);
```

The `setup` call sets the rotation from `dir`; `setupTime` then overrides the default `20/20/20/20`
phases. The constructor sets the owner, so the danmaku damage source, graze and PvP rules are the
existing ones. The total lifetime is `5 + 5 + 20 + 5 = 35` ticks, with the hit window open for the
20-tick duration.

## 7. Files

New:

- `content/item/devil/DevilScrollItem.java`
- `content/item/devil/DevilScrollMode.java`
- `content/item/devil/DevilScrollSelectionListener.java`
- `content/item/devil/DevilScrollModel.java`
- `content/item/devil/client/DevilScrollModeWheel.java`
- `content/item/devil/package-info.java`, `content/item/devil/client/package-info.java`
- `content/spell/item/DevilScrollDash.java`
- `event/DevilScrollEventHandlers.java`
- `src/main/resources/assets/gensokyolegacy/textures/item/tool/devils_scroll.png` (passive)
- `src/main/resources/assets/gensokyolegacy/textures/item/tool/devils_scroll_superhuman.png`
- `src/main/resources/assets/gensokyolegacy/textures/item/tool/devils_scroll_devil.png`
- this document

Modified:

- `event/GLAttackListener.java` — the `BYPASS_COOLDOWN` branch in `onCreateSource`.
- `init/registrate/GLItems.java` — `DEVILS_SCROLL` entry and `DEVILS_SCROLL_MODE` component.
- `init/GensokyoLegacy.java` — `DevilScrollSelectionListener.register()`.
- `init/GLClient.java` — the `devils_scroll_mode` item predicate.
- `init/data/GLLang.java` — mode names, descriptions, lore, and the enum in `genLang`.
- `src/test/resources/gensokyolegacy/lang/zh_cn/items.json` and `tooltip.json` — the zh_cn strings;
  rerun `organize.ResourceOrganizer` afterwards.

Generated by `runData` (commit it): `models/item/devils_scroll*.json`, `lang/en_us.json`,
`lang/en_ud.json`, and the item tags.

## 8. Registration and assets

```java
DEVILS_SCROLL = reg.item("devils_scroll", DevilScrollItem::new)
        .model(DevilScrollModel::model)
        .tag(ItemTags.WEAPON_ENCHANTABLE, ItemTags.SHARP_WEAPON_ENCHANTABLE, L2ISTagGen.SELECTABLE)
        .lang("Devil's Scroll").tab(TAB.key())
        .register();
```

The item is added in the tools block, after `TAB` is built, so it lands in the creative tab. The
constructor applies `ATTRIBUTES` and `stacksTo(1)`. `L2ISTagGen.SELECTABLE` is what lets the item
wheel open on the scroll.

The three icons are **copies**, not references to the items they come from:

| mode | texture | copied from |
|---|---|---|
| Passive | `item/tool/devils_scroll.png` | vanilla `item/book.png` |
| Superhuman | `item/tool/devils_scroll_superhuman.png` | `item/gift/magic_book.png` (the obscure magic book) |
| Devil | `item/tool/devils_scroll_devil.png` | vanilla `item/enchanted_book.png` |

Copying rather than pointing the model at `item/book` or `item/gift/magic_book` keeps the scroll's
textures independent: the final art is a one-file replacement, and the source items can change
without touching this item.

`DevilScrollModel` generates the base model from the passive icon and emits overrides at predicate
values 1 and 2 for the other two. `DevilScrollItem.displayPredicate` returns the mode ordinal, so
the model, the wheel and the selector list all agree:

```java
public static float displayPredicate(ItemStack stack, Level level, LivingEntity entity, int seed) {
    return getMode(stack).ordinal();
}
```

`GLClient` registers the predicate under `gensokyolegacy:devils_scroll_mode`.

`GLLang` additions:

- `ItemDevilScroll.MODE_PASSIVE` / `MODE_SUPERHUMAN` / `MODE_DEVIL` — wheel and tooltip names.
- `ItemDevilScroll.DESC_*` — one line each, describing what the stance does.
- `ItemDevilScroll.MODE` — the "Mode: %s" tooltip line.
- `ItemLores.DEVILS_SCROLL_LORE` — the flavour line.

The mode enum's `.values()` must be added to the `LangEntry[][]` array in `GLLang.genLang` or the
strings are never emitted.

## 9. Open questions and tuning

- **Dash cooldown** is not in the spec; 10 ticks is used. The dash damage uses the player's full
  attack damage, which can be high with the `+7`; halve it if it one-shots.
- **Dash duration** is 10 ticks or until speed drops below `0.2`. A one-tick 3-block impulse is
  already most of the distance; the ticker mainly exists to sweep the path.
- **`BYPASS_COOLDOWN`** removes the target's i-frames, not the attacker's swing timer. If "bypass
  cooldown" is meant to include full damage on every click, the scroll needs an `ATTACK_SPEED`
  modifier (visible in the tooltip) or a player-ticker mixin. The spec names the damage state, so
  the implementation does that and leaves the swing timer alone.
- **Laser walls**: `bypassWall = true` matches the existing player lasers. Set it to `false` if the
  beams should stop at terrain.
- **Laser length** is 80, the same as the spell-card lasers; the beams converge on the target, so a
  shorter length would still reach a normal melee range.
- **Default mode** is `PASSIVE`, so a new scroll is just the attribute item until the player picks a
  stance.
- **Shared cooldown**: `ItemCooldowns` keys by item, so the dash's 10-tick cooldown and the lasers'
  5-tick cooldown share one timer across both hands. If a main-hand Superhuman dash should never
  delay an off-hand Devil's lasers, the laser gate needs a player attachment instead.
- **Both hands at once**: the distinct modifier ids make the two attribute sets stack. If that is not
  wanted, gate one set on the other hand being empty, or accept it as a reward for the off-hand
  investment.
- **Devil lasers and the dash**: the dash is a Superhuman action, so it only procs lasers when the
  *other* hand holds a Devil-mode scroll. That is consistent with the hand/mode table, but worth
  confirming in play.
- **Aim assist window**: 20° is the spec's number; `TARGET_TTL` is 2 s and `TARGET_RANGE` is 32.
  The range covers the dash's full travel, but the assist only changes the direction, not the
  distance — a target 25 blocks away still eats a normal 10-tick dash. Widen the duration if the
  assist is meant to close long gaps.
- **Aim assist vs. wall**: `blockedByBlocks` is false, so a target behind a wall can glow and bend
  the dash — which then stops at the wall. Set it to true if the glow should mean "reachable".

## 10. Playtest checklist

- Textures: the passive, superhuman and devil modes each draw their own book icon in the inventory,
  the hand and the wheel; switching modes changes the icon.
- Attributes: the main-hand and off-hand tooltips show the four values, and the values change on
  switching hands. A scroll in each hand stacks as expected.
- Passive mode: none of the active abilities fire.
- `BYPASS_COOLDOWN`: two clicks inside the target's i-frames both deal damage.
- Jump Boost II is permanent while held in Superhuman; falling from a height deals no damage; a
  zombie's hit does not move the player.
- Knockback: a ground hit shoves hard, a sneaking hit launches up, an airborne hit slams down.
- Dash: right-click moves about three blocks on the first tick, hits every entity in the path once,
  does not hit allies, and respects its cooldown.
- Aim cache: in Superhuman mode with the scroll in the main hand, the crosshair target glows orange;
  the glow and the assist survive a 2 s pause and are gone after it; an off-hand Superhuman scroll
  never glows.
- Dash aim: right-clicking with a target under the crosshair bends the dash onto it and turns the
  camera onto the same target; a target 30° off the view does neither while not sneaking.
- Sneak lock: with a superhuman scroll in the main hand, sneaking keeps the camera on the cached
  target every tick, at any angle; letting go of sneak or losing the target releases the lock. Moving
  the mouse does not shake the locked view.
- Lasers: four orange beams appear at the four head offsets, converge on the damaged target, take
  5/5 ticks to open, deal 6, last 20 ticks and fade over 5. A second hit 5 ticks later starts a new
  set. The lasers themselves never start another set. Arrows and bullets do start a set.
- Mode wheel: all three entries are listed with their own icons, switching updates the held stack on
  both sides, and works from either hand.
- Dedicated server: the selector packet, the dash motion and the laser spawn all behave the same as
  in single-player.
