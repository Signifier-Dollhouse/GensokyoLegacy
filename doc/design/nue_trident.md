# Nue's Trident

Nue's trident is a reusable throwing weapon: every throw sends out a copy, the real trident never
leaves the hand, and whatever the copy hits has its held item swapped for a UFO.

> **Status:** implemented. Numbers below are first guesses, chosen to match the vanilla
> trident where one exists, and are expected to be tuned by play.
> **Package:** `content/item/nue/` (item and UFO), `content/entity/misc/` (the projectile).
> **Related:** `minamitsu_anchor.md` (the other thrown weapon; shares the throw plumbing),
> `dagger_glove.md` (the throw-and-return pattern this is modelled on).

## 1. What it is

A trident with two twists over vanilla's:

* **The throw is free, and charged.** Hold right-click past the vanilla ten ticks and release to
  throw a *copy*; the stack in the hand is untouched, so the weapon is never lost and never needs
  Loyalty. The copy cannot be picked up, cannot be retrieved, and simply vanishes when it lands or
  expires.
* **A hit is a small theft.** A living target's held item is replaced by a **UFO**, a plain wrapper
  holding that stack. Hold right-click on it for two seconds — at any point afterwards, with no
  deadline — and the stack comes back out.

The trident is otherwise a normal trident: it can be swung in melee for the vanilla trident's
damage and speed, and the thrown copy flies on the vanilla trident arc.

## 2. Components

| Piece | File | Job |
|---|---|---|
| `NueTridentItem` | `content/item/nue/NueTridentItem.java` | the item: melee stats, charge, throw, cooldown |
| `NueTridentProjectile` | `content/entity/misc/NueTridentProjectile.java` | the flying copy: gravity, damage, UFO conversion |
| `NoFadeBulletRenderer` | `content/entity/misc/render/NoFadeBulletRenderer.java` | the copy's renderer: no distance fading, centred on the flight line |
| `UfoItem` | `content/item/nue/UfoItem.java` | the wrapper: holds the stolen stack, hands it back after a two-second hold |

The projectile is an `ItemBulletEntity` subclass, like `IronDaggerBulletEntity`, which buys the
l2serial save/spawn code and the fast-projectile collision for free. Two quirks come with it and are
accepted:

* a danmaku would fade at distance and be hidden from its own thrower; `NoFadeBulletRenderer` returns
  a fading of `1` and `shouldRender` of `true`, so the trident is fully opaque and visible from the
  first tick, at any distance. It keeps the shared renderer's half-height render offset, which puts
  the model's centre on the projectile's hitbox centre instead of at its feet;
* it is a danmaku as far as targeting is concerned, so it follows `IDanmakuEntity.canHurt` — no
  friendly fire, no hitting players without PvP — which is the behaviour a weapon wants anyway.

If the danmaku coupling ever gets in the way (graze on the thrower, danmaku damage events), the
fallback is a plain `BaseProjectile` subclass plus a small `EntityRenderer` that draws the item
model the way `ThrownTridentRenderer` does. Nothing else in this document depends on that choice.

## 3. The trident item

`NueTridentItem extends DanmakuItem`, but it throws on release rather than on click, so it replaces
`DanmakuItem.use` with a charge. The overrides are:

| override | value | why |
|---|---|---|
| `consume()` | `false` | the hand keeps the trident; the throw spends nothing |
| `cooldown()` | 10 ticks | a throw is a real attack, not a spam button |
| `getUseAnimation(stack)` | `UseAnim.SPEAR` | the vanilla trident's pose while charging |
| `getUseDuration(stack, entity)` | `72000` | never completes; the throw waits for the release |
| `use(...)` | `startUsingItem(hand)`, `consume(stack)` | a click only raises the trident |
| `releaseUsing(...)` | at `>= 10` ticks held and off cooldown: post `DanmakuUseEvent`, play the sound, spawn on the server, award the stat, apply the cooldown | the vanilla charge threshold, then the throw `DanmakuItem.use` would have done |
| `newBullet(player, level)` | `new NueTridentProjectile(GLEntities.NUE_TRIDENT.get(), player, level)` | the copy |
| `spawnBullet(...)` | overridden: `setItem`, `setup(8, 120, false, false, look.scale(2.5))`, `moveTo(eye)`, `addFreshEntity`, `SpellContainer.track` | the arc and the damage are the trident's, not the dagger's |
| `buildRenderer()` | inherited `DAGGER` branch → `ItemModelProjectileType` | draws the item's own model |
| `playThrowSound(...)` | `SoundEvents.TRIDENT_THROW` | — |

`releaseUsing` refuses to throw when the stack is no longer in either hand: our own UFO swap calls
`stopUsingItem` on the victim, and a use the player no longer holds must not launch a copy.

Melee stats are the vanilla trident's, added as components in registration:

```java
p.attributes(ItemAttributeModifiers.builder()
        .add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 8.0, ADD_VALUE), MAINHAND)
        .add(Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.9, ADD_VALUE), MAINHAND)
        .build())
```

Pass `DanmakuItems.Bullet.DAGGER` as the `DanmakuItem` bullet type so `buildRenderer` selects the
item-model renderer, and **do not** add `DanmakuItems.Bullet.DAGGER.tag`: the trident must not
become ammo for the dagger glove, spell cards or anything else that reads that tag.

The art is already prepared in `temp/` and only needs moving and wiring:

| from `temp/` | to `src/main/resources/assets/gensokyolegacy/` |
|---|---|
| `trident.json` | `models/custom/nue_trident.json`, texture `block/texture` → `gensokyolegacy:item/tool/nue_trident` |
| `trident.png` (64×64) | `textures/item/tool/nue_trident.png` |
| `trident_icon.png` (16×16) | `textures/item/tool/nue_trident_icon.png` |

The Blockbench model has hand display transforms and no GUI transform, so registration uses the
same split as the iron dagger / doll lance: the custom model is the base, and
`ItemDisplayContext.GUI` gets a flat `item/generated` model over `nue_trident_icon` through
`SeparateTransformsModelBuilder`. That also gives the projectile renderer the 3D model for free.

Tags: `ItemTags.WEAPON_ENCHANTABLE`, `ItemTags.SHARP_WEAPON_ENCHANTABLE`, and the mod's normal
creative tab. No durability (`stacksTo(1)`, no `MAX_DAMAGE`), so it can never break and the throw
never has to hand anything back.

## 4. The projectile

`NueTridentProjectile extends ItemBulletEntity`, `@SerialClass`, one field: `dealtDamage` (so a
copy that already hurt something cannot hurt a second one on the way down).

| constant | value | notes |
|---|---|---|
| throw speed | 2.5 b/t | vanilla `TridentItem.SHOOT_POWER` |
| gravity | 0.05 b/t² | vanilla arrow/trident |
| drag | 0.99 | vanilla arrow/trident |
| lifetime | 120 ticks | vanilla arrow default; also covers a long lazy arc |
| hit damage | 8 | vanilla `TridentItem.BASE_DAMAGE` |
| pickup | never | the entity is not an `ItemEntity`; nothing to pick up |

`setup(8, 120, false, false, look.scale(2.5))` from the overridden `spawnBullet`, then
`moveTo(player.getEyePosition())`: the copy starts at the thrower's eyes, on the crosshair line, not
at the body's centre. In `updateVelocity`:

```java
return ProjectileMovement.of(vec.scale(0.99).subtract(0, 0.05, 0));
```

`NoFadeBulletRenderer` is registered for this entity in `GLEntities`; it overrides `fading` to `1`
and `shouldRender` to `true`, so a weapon is drawn at full opacity from the first tick at any range.
The half-height offset is inherited: the model is centred on the hitbox rather than on the
projectile's feet.

Hit handling:

* **block** — play `TRIDENT_HIT_GROUND`, discard.
* **entity** — if `dealtDamage` return; set it, hurt for 8 with
  `damageSources().thrown(this, owner)`, play `TRIDENT_HIT`, run the UFO conversion, discard.
* **expiry** — vanish quietly (no drop, no return).

`checkBlockHit()` stays true; the copy is not loyal and never passes through terrain.

## 5. The UFO conversion

### 5.1 Which item

The target's **main hand**, falling back to the **offhand** when the main hand is empty. Nothing
happens (beyond the damage) if both are empty, if the held stack is already a UFO, or if the target
is not a `LivingEntity`. Whatever was held — a sword, a stack of sixteen dirt, a bucket — is moved
whole into the UFO; the count is preserved because the UFO stores the `ItemStack` itself, not an
item type.

That hand is also made a guaranteed drop for a mob (`Mob#setGuaranteedDrop`), so the UFO — and
therefore the stolen item — always falls out when the victim dies instead of being rolled against
the usual 8.5% hand chance. A player drops their hands normally, so nothing is needed there.

### 5.2 Stored on the UFO

One data component, registered in `GLItems` with the existing `DCReg`:

| component | type | contents |
|---|---|---|
| `ufo_content` | `DCStack` (`DC.stack`) | the stolen stack, `ItemStack.EMPTY`-optional |

Nothing else is stored: the UFO has no clock. It is a wrapper, like a shulker box that holds one
stack and opens on demand.

### 5.3 Opening it

`UfoItem` is a held-use item, not a click item:

| override | value |
|---|---|
| `getUseDuration(stack, entity)` | `40` — two seconds of holding |
| `getUseAnimation(stack)` | `UseAnim.BRUSH` — the arm works at the item, with no extra prop |
| `use(level, player, hand)` | `InteractionResultHolder.consume(stack)` when content is present, `PASS` when empty |
| `finishUsingItem(stack, level, entity)` | return `content` — the game puts it back in the slot the UFO occupied |

* the content leaves only through a completed hold; releasing early (`releaseUsing`) does nothing,
  so a slip of the mouse cannot open it by accident;
* `finishUsingItem` returning the stack means no `setItemInHand` juggling and no dropped copy: the
  UFO is replaced in place by exactly what it held, count included;
* with empty content (a creative-given UFO) `use` is `PASS`, so it is a plain souvenir with no
  behaviour.

The tooltip names the stored stack (`Contains <item name>`) and says to hold to open it. There is no
expiry check anywhere, so a UFO left in a chest for a week still opens.

The UFO is `stacksTo(1)` (the components differ per stack anyway) and stacks with nothing.

### 5.4 What the victim sees

A mob simply holds the UFO until something makes it drop it. A player gets the UFO in the hand they
were using and can hold right-click for two seconds to get their item back — immediately, or after
the fight; a player holding a stack of blocks in the offhand still keeps those unless the main hand
was empty. Swapping the stack also cancels whatever use was in progress (`player.stopUsingItem()`),
which is the point of the weapon.

## 6. Registration checklist

* `GLItems`: `NUE_TRIDENT` (`NueTridentItem`, `stacksTo(1)`, `rarity(RARE)`), `UFO` (`UfoItem`,
  `stacksTo(1)`, generated model), `UFO_CONTENT`.
* `GLEntities`: `NUE_TRIDENT` projectile, `sized(0.5F, 0.5F)`, `clientTrackingRange(4)`,
  `updateInterval(1 << 16)`, `renderer(NoFadeBulletRenderer::new)` — no fading or near-self hiding.
* `GLLang`: `ItemLores.NUE_TRIDENT_LORE`, `ItemLores.UFO_LORE`, and an `ItemUfo` group for the
  `Contains <item>` line and the hold hint.
* `patchouli_books/tools_guide`: `entries/special_tools/nue_trident.json`, en_us and zh_cn.
* tags: `WEAPON_ENCHANTABLE`, `SHARP_WEAPON_ENCHANTABLE`; creative tab.
* assets: the three prepared files from §3, plus `textures/item/tool/ufo.png` (new art); the staged
  originals stay in `temp/` like the other prepared art there, nothing in the mod references them.
* zh_cn: `items.json` names, `tooltip.json` lores — then rerun `organize.ResourceOrganizer`.
* `./gradlew runData`, commit `src/generated/resources/`.
* `content/item/nue/package-info.java` and `content/entity/misc/render/package-info.java`
  (every new package gets one), no FQNs in the new sources.

Out of scope until asked: recipe/loot/offer acquisition, dispenser behaviour, TLM compat.

## 7. Edge cases and open questions

**Decided** (change these only with a reason):

* the throw is a vanilla-style charge: hold right-click past 10 ticks, then release; a click by
  itself does nothing, and the charge never completes on its own (`getUseDuration` is 72000, the
  throw lives in `releaseUsing`);
* the copy leaves from the thrower's eyes on the crosshair line, not from the body's centre;
* a mob with empty hands is damaged but gets no UFO;
* the converted hand is a guaranteed drop, so the UFO always comes off a dead mob;
* melee hits do not convert — only the thrown copy does;
* the UFO never expires and never loses its content: it is a wrapper, and opening it is a two-second
  hold rather than a click;
* the copy is a danmaku, so it obeys PvP/friendly-fire rules and grazes like other danmaku.

**Edge cases:**

* Hitting the same target twice with two tridents in flight: the second hit finds the UFO and does
  nothing extra (already a UFO), so the first one stays.
* Owner dies, logs out or changes dimension while the copy flies: the copy expires on its own; no
  return path exists, so nothing is lost.
* The victim dies and drops the UFO: the UFO drops as an item with its content intact, and whoever
  picks it up can open it.
* Opening a UFO while it is the whole main-hand stack: the content replaces it in the same slot, so
  nothing is dropped and nothing is duplicated.
* Creative: the throw costs nothing anyway, but the UFO conversion still applies — it is the
  trident's whole identity, not a survival-only rule.

## 8. Test plan

1. Hold right-click past ten ticks and release: the copy flies, the stack stays, the cooldown is
   10 ticks. Release before ten ticks: nothing is thrown.
2. Hit a zombie holding a sword: damage lands, the sword becomes a UFO; holding right-click on the
   UFO for two seconds returns the sword, count and all.
3. Release the hold after one second: nothing happens, the UFO stays.
4. Store the UFO in a chest, wait a few in-game days, then open it: the item still comes back.
5. Hit a zombie with empty hands: damage only.
6. Kill the converted zombie: it always drops the UFO, and the UFO still holds the stolen item.
7. Hit a block: the copy vanishes with a ground hit sound, nothing drops.
8. Throw into water/void and wait 120 ticks: the copy vanishes without a return.
9. Stand far away and watch the copy: it stays fully opaque instead of fading out.
