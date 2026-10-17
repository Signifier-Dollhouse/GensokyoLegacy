# Gifts

Handing a character a gift item raises her reputation by the item's `favor`, scaled by how much she is
in the mood for that item's *category*, and starts a per-item cooldown. Two data maps drive it, plus a
per-class hook, and the three resolve in a fixed order.

## Where a preference lives

Everything is data. `GiftPreference` is the value of the `gift_preference` datamap, keyed by **entity
type**, and it holds two maps:

- `preference`, keyed by `GiftType` — a factor for each category this character has an opinion about
- `override`, keyed by `Item` — a factor for a single item, which wins outright

A character with no datamap entry is neutral on every category.

**One entry per entity type, never two.** A datamap entry replaces rather than merges: naming an
entity type in two files, or under both a tag and its own key, gives last-one-wins for the whole
record, so a character with tastes of her own has to state everything in one entry. Cirno is the case
that bites — she is a fairy *and* has her own opinions, so her entry holds her three categories **and**
the three every fairy shares.

### Sharing a species' opinion

There is no hook on the entity class for this: what a species shares is a constant handed to each of
its registrations, declared in the `GIFT_*` block in `GLEntities` next to the `TASTE_*` ones.

```java
GiftPreference GIFT_FAIRY = GiftPreference.of(Map.of(
        GiftType.ALCOHOL, 0.0,
        GiftType.MAGIC, 0.0,
        GiftType.FROG, 2.0));
```

Each fairy entity type — `sunny_milk`, `luna_child`, `star_sapphire`, `plain_fairy` — passes
`GIFT_FAIRY`, and Cirno restates the same three values inside her own entry. The cost is that a new
fairy has to be given the constant; the benefit is that every preference in the game is readable as
data, tweakable by a datapack, with no behaviour hidden in an entity class.

## The factor

`GiftItemData.getFavor()` resolves in three steps, each overriding only what the one above leaves out:

1. **Per-item override** — `pref.override().get(item)`. Wins outright.
2. **Per-category** — `pref.preference().get(type)`.
3. **Neutral** — `type.neutral()`, for a category this character has no opinion about.

The result is multiplied by the item's base `favor`.

### Zero means refused

A factor of 0 (or any value that truncates the product to 0) makes `getFavor` return 0, and
`GiftModule.interact()` already treats a non-positive favor as `InteractionResult.PASS` — the item is
not consumed, no cooldown starts, and the interaction falls through to whatever else handles it. So
**a 0 factor is a refusal, not a zero-value gift**; there is no separate "reject list" to keep in sync.

## `neutral`: 1.0 versus 0.0

Every category is neutral at 1.0 — someone with no opinion takes it at face value. `FROG` is the
exception, neutral at **0.0**: a frozen frog is a fairy's delicacy and nobody else's, so a character
who has never heard of the category refuses it rather than accepting it by default. That is what makes
"only fairies take these" one entry rather than a 0.0 on all sixteen other character types, and it
stays correct for any character added later.

## Categories

| Type | Neutral | Gifts | Notes |
|------|---------|-------|-------|
| `TOY` | 1.0 | `sturdy_teddy_bear` (5) | Sakuya 0.5, Cirno 2.0, Suika 0 |
| `FOOD` | 1.0 | `fairy_cake` (3), `magic_candy` (3), `kappa_bento` (4) | Cirno 1.5, Suika 1.5 |
| `DRINK` | 1.0 | `lucky_tea` (4) | Drinks that are not spirits. Reimu and Suika both at 2.0 |
| `ALCOHOL` | 1.0 | `tengu_sake`, `ghost_sake`, `zun_beer` (5 each) | Split out of `DRINK` so a fairy's refusal of spirits does not also refuse her tea. Reimu 2.0, Suika 2.0, all fairies 0 |
| `BOOK` | 1.0 | `magic_book` (6) | Reimu 1.5, Cirno 0.5 |
| `UTENSIL` | 1.0 | `fancy_fan` (4) | |
| `TOOL` | 1.0 | *(none yet)* | |
| `MAGIC` | 1.0 | `star_flower` (1) | All fairies 0 |
| `FROG` | **0.0** | `frozen_frog_cold` / `_warm` / `_temperate` (3 each) | Fairies 2.0, everyone else refused |

The `DRINK` / `ALCOHOL` split is the one that carries its weight: without it a fairy's refusal of
spirits would also refuse the tea, and `lucky_tea` is the only thing a character who spurns alcohol
can be given to drink.

## Fairies

Spirits and strong magic are refused, and a frozen frog is the one thing a fairy is glad to be handed —
`ALCOHOL 0, MAGIC 0, FROG 2` in `GIFT_FAIRY`. `CirnoEntity` and `PlainFairyEntity` are fairies too and
get the same, carried in their own datamap entries rather than by inheriting from a Java field.

A fairy's *gift* module is separate from her *feeding* one (`FairyFeedModule`), which is what a cake or
a cookie is worth. A frozen frog is not food she recognises, so it only ever reaches
`GiftModule.interact()`. See [want_bubble.md](want_bubble.md).

## A gift the player can also eat

A gift is not necessarily inert. `magic_candy`, `kappa_bento`, `fairy_cake`, `lucky_tea` and `zun_beer`
carry `FoodProperties`, so the player can consume them for the same effect that drinking or eating one
is worth. No character is fed one: the only feed modules in use are the fairy ones
(`FairyFeedModule`), which recognise a cake or a cookie and nothing else, so every other gift reaches
`GiftModule.interact()` and nowhere else. (`FeedModule`, the generic one that would take any food, is
currently attached to no character.)

That is why the food gifts carry real nutrition and the two drinks do not: the sakes, the tea and the
beer go through `sake()`, which builds `alwaysEdible()` food with no nutrition, so the party food system
never mistakes a bottle for a meal and a player on a full stomach can still drink one. `zun_beer` and
`lucky_tea` are the same effect one potency apart — Minecraft has a single haste/looting effect
(`MobEffects.DIG_SPEED`) that is haste on a mob and looting on an item.

`fancy_fan` is a plain item: it is only ever a gift, which is the point of it.

## Adding a gift

1. Give the item a `GIFT_DATA` entry: `new GiftItemData(favor, cooldown, type)`. Datagen writes
   `src/generated/resources/data/gensokyolegacy/data_maps/item/gift_data.json`.
2. Give any character that cares a `GIFT_PREFERENCE` entry — per-item `override` for a single item, or a
   per-category factor. One entry per entity type; if the character also belongs to a group with a
   shared opinion, state both in the same entry.
3. If the category is one only some characters want, set its `neutral` to `0.0` so the rest refuse it
   without restating anything.

All three datamaps and the `en_us` / `en_ud` lang files are generated — run `./gradlew runData` and
commit `src/generated/resources/` alongside the code. `zh_cn` is hand-authored under
`src/test/resources/gensokyolegacy/lang/zh_cn/`.

## Files

- `content/item/gift/GiftType.java` -- the categories, their display names, and `neutral()`
- `content/item/gift/GiftItemData.java` -- base favor / cooldown / type, and the three-tier lookup
- `content/item/gift/GiftPreference.java` -- the `gift_preference` datamap value
- `content/entity/behavior/module/GiftModule.java` -- the interaction, the cooldown, the favor gain
- `content/entity/characters/fairy/FairyEntity.java` -- the fairy base class (her preferences are data, not code)
- `init/registrate/GLItems.java`, `init/registrate/block/{GLDecoBlocks,GLNaturalBlocks}.java` -- the `GIFT_DATA` entries
- `init/registrate/GLEntities.java` -- the `GIFT_*` preference constants and the registrations that pass them
- `init/registrate/GLMeta.java` -- `GIFT_DATA` and `GIFT_PREFERENCE` datamap registrations
- `event/GLClientEventHandlers.java` -- the favor / type tooltip
- `content/attachment/character/ReputationConstants.java` -- `GIFT_SOFT_CAP`, what the favor is spent on ([reputation.md](structure/reputation.md))