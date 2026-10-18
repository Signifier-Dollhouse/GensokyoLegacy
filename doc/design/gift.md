# Gifts

Handing a character a gift item raises her reputation by the item's `favor`, scaled by how much she is
in the mood for that item's *category*, and starts a per-item cooldown. Two data maps drive it — the
item's own worth, and the character's opinion — and the lookup resolves them in a fixed order.

## Where a preference lives

Everything is data. `GiftPreference` is the value of the `gift_preference` datamap, keyed by **entity
type**, and it holds two maps:

- `preference`, keyed by `GiftType` — a factor for each category this character has an opinion about
- `override`, keyed by `Item` — a factor for a single item, which wins outright

A character with no datamap entry is neutral on every category.

**One entry per entity type, never two.** A datamap entry replaces rather than merges: naming an
entity type in two files, or under both a tag and its own key, gives last-one-wins for the whole
record. Cirno is the case that bites — she is a fairy *and* has opinions of her own, so her entry
cannot be the species' constant plus a second file; it is the constant rebuilt with `and`.

### Sharing a species' opinion

There is no hook on the entity class for this: what a species shares is a constant handed to each of
its registrations, declared in the `GIFT_*` block in `GLEntities` next to the `TASTE_*` ones.

```java
GiftPreference GIFT_FAIRY = GiftPreference.of(Map.of(
        GiftType.TOY, 2.0,
        GiftType.FOOD, 1.5,
        GiftType.BOOK, 0.0,
        GiftType.ALCOHOL, 0.0,
        GiftType.MAGIC, 0.0,
        GiftType.COLLECTION, 0.5,
        GiftType.TOOL, 0.0,
        GiftType.FROG, 1.5));

GiftPreference GIFT_CIRNO = GIFT_FAIRY.and(GiftType.FROG, 3.0);
```

Only `plain_fairy` carries the `GIFT_FAIRY` entry today; the other three fairy types do not pass it at
all. Cirno, who has opinions of her own, is built off the constant with `and` rather than restating it,
since restating is exactly the duplication the datamap cannot merge. The cost is that a new fairy has
to be given the constant; the benefit is that every preference in the game is readable as data,
tweakable by a datapack, with no behaviour hidden in an entity class.

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
"only fairies take these" one entry rather than a 0.0 on every other character type, and it stays
correct for any character added later.

## Categories

| Type | Neutral | Gifts | Preferences |
|------|---------|-------|-------------|
| `TOY` | 1.0 | `sturdy_teddy_bear` (5) | Fairies 2.0, Alice 1.5, Suika 0 |
| `FOOD` | 1.0 | `fairy_cake` (3), `magic_candy` (2), `kappa_bento` (4) | Fairies 1.5, Reimu 1.5, Suika 1.5 |
| `DRINK` | 1.0 | `lucky_tea` (4) | Reimu 2.0, Sakuya 2.0, Alice 1.5, Morichika 1.5 |
| `ALCOHOL` | 1.0 | `tengu_sake`, `ghost_sake`, `zun_beer` (5 each) | Split out of `DRINK` so a fairy's refusal of spirits does not also refuse her tea. Suika 3.0, Reimu 2.0, Marisa 2.0, Morichika 1.5, all fairies 0 |
| `BOOK` | 1.0 | `magic_book` (6) | Marisa 2.0, Alice 2.0, Reimu 1.5, Morichika 1.5, Suika 0, all fairies 0 |
| `COLLECTION` | 1.0 | `fancy_fan` (5) | Reimu 1.5, Marisa 1.5, Alice 1.5, Suika 0.5, fairies 0.5 |
| `TOOL` | 1.0 | *(none yet)* | Sakuya 1.5, Morichika 1.5, all fairies 0 |
| `MAGIC` | 1.0 | `star_flower` (1) | Marisa 2.0, Alice 1.5, Morichika 0.5, Suika 0, all fairies 0 |
| `FROG` | **0.0** | `frozen_frog_cold` / `_warm` / `_temperate` (3 each) | Cirno 3.0, other fairies 1.5, everyone else refused |

Every `GIFT_*` constant in `GLEntities` restates the whole species opinion rather than naming only
what it changes, so the datamap file is one flat read of who likes what; a category left out of a
constant is at `neutral` and takes the gift at face value.

The `DRINK` / `ALCOHOL` split is the one that carries its weight: without it a fairy's refusal of
spirits would also refuse the tea, and `lucky_tea` is the only thing a character who spurns alcohol
can be given to drink.

## Fairies

Spirits, books, tools and strong magic are refused, and a toy, a sweet and a frozen frog are what a
fairy is glad to be handed — `ALCOHOL 0, BOOK 0, TOOL 0, MAGIC 0` against `TOY 2.0, FOOD 1.5, FROG 1.5`
in `GIFT_FAIRY`, with a fan at half (`COLLECTION 0.5`). Cirno is the one exception, taking the frog at
3.0.

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

`fancy_fan` is a plain item: it does nothing, which is the point of it.

## Selling gifts on a shelf

Some gifts are also among the stock Morichika restocks on the shelves around his home, so a player who
wants one does not have to be liked enough to be given it. Membership is the `MORICHIKA_OFFERS` item
tag, which `YoukaiRestockShelfTask` draws an offer from uniformly at random — the tag alone decides
what can appear, so nothing in it is unreachable, and an item no gift preference touches can still be
sold. Price and stock are the separate `MORICHIKA_OFFER` datamap (`MorichikaOfferData`), rolled per
restock, and an item with no entry falls back to stock 1 at price 1.

| item | price | stock |
|------|-------|-------|
| `magic_candy` | 2-4 | 3-6 |
| `tengu_sake`, `ghost_sake`, `zun_beer`, `sturdy_teddy_bear` | 5-10 | 3-6, except the bear at 2-3 |
| `fancy_fan` | 10-20 | 1 |
| `magic_book` | 12-24 | 1 |

A gift is priced at its own favor up to double — an emerald buys back about as much favor as handing
the item over does — and stocked a few at a time, because a shelf of drinks and sweets is a shelf of
things he can keep in stock. The fan and the book are doubled again at a single unit each, which for
the book also keeps it honest as a furnace fuel: at 40000 ticks it burns 200 copies, so a price near
the rest of the gifts would make it worth more as fuel than as an offer.

The rest of the tag is tools and curios rather than gifts, and is priced by what they are worth
instead: `mini_hakkero_prototype` 16-24, `dowser` 12-18, `cat_bell` 8-12, `strange_glasses` 6-10,
`centipickaxe` and `mermaid_pearl` 4-6, `straw_hat` 3-6.

## Adding a gift

1. Give the item a `GIFT_DATA` entry: `new GiftItemData(favor, cooldown, type)`. Datagen writes
   `src/generated/resources/data/gensokyolegacy/data_maps/item/gift_data.json`.
2. Give any character that cares a `GIFT_PREFERENCE` entry — per-item `override` for a single item, or a
   per-category factor. One entry per entity type; if the character also belongs to a group with a
   shared opinion, state both in the same entry.
3. If the category is one only some characters want, set its `neutral` to `0.0` so the rest refuse it
   without restating anything.
4. If it should also be buyable, tag it `MORICHIKA_OFFERS` and give it a `MorichikaOfferData` — see
   *Selling gifts on a shelf*.

The `gift_data`, `gift_preference` and `morichika_offer` datamaps and the `en_us` / `en_ud` lang
files are generated — run `./gradlew runData` and commit `src/generated/resources/` alongside the
code. `zh_cn` is hand-authored under `src/test/resources/gensokyolegacy/lang/zh_cn/`.

## Files

- `content/item/gift/GiftType.java` -- the categories, their display names, and `neutral()`
- `content/item/gift/GiftItemData.java` -- base favor / cooldown / type, and the three-tier lookup
- `content/item/gift/GiftPreference.java` -- the `gift_preference` datamap value
- `content/entity/behavior/module/GiftModule.java` -- the interaction, the cooldown, the favor gain
- `content/entity/characters/fairy/FairyEntity.java` -- the fairy base class (her preferences are data, not code)
- `init/registrate/GLItems.java`, `init/registrate/block/{GLDecoBlocks,GLNaturalBlocks}.java` -- the `GIFT_DATA` entries
- `init/registrate/GLEntities.java` -- the `GIFT_*` preference constants and the registrations that pass them
- `init/registrate/GLMeta.java` -- the `GIFT_DATA`, `GIFT_PREFERENCE` and `MORICHIKA_OFFER` registrations
- `init/data/GLTagGen.java` -- the `MORICHIKA_OFFERS` item tag
- `event/GLClientEventHandlers.java` -- the favor / type tooltip
- `content/block/deco/shelf/MorichikaOfferData.java` -- the shelf price / stock band
- `content/entity/behavior/task/home/YoukaiRestockShelfTask.java` -- picks the offer and rolls its price
- `content/attachment/character/ReputationConstants.java` -- `GIFT_SOFT_CAP`, what the favor is spent on ([reputation.md](structure/reputation.md))