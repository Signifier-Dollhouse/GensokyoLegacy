# Doll Item

`content/item/doll/DollItem.java` — the item form of a doll. While a doll is an item, the item is the **sole authoritative copy**: itemization (pairing.md §3.2) deletes the ledger `DollData` before producing it, and nobody else stores a doll in item form.

## 1. Registration

```java
public static final ItemEntry<DollItem> DOLL = reg.item("doll", p -> new DollItem(p.stacksTo(1)))
        .model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/doll/" + ctx.getName())))
        .lang("Doll")
        .tab(TAB.key())
        .register();
```

- `stacksTo(1)` — one item per doll.
- `GLItems.DOLL_DATA` — `DC.reg("doll_item_data", DollItemData.class, false)`, the `DataComponentType<DollItemData>` holding the doll's combat values (see §2). Registered alongside the item.
- The entity kind is fixed for all doll items: `public static final ResourceLocation TYPE = GensokyoLegacy.loc("doll")` (used by `DollData.fromItemData`). It is **not** stored on the item — every doll item materializes this same `doll` entity type.

## 2. Building items from data: `makeItem` / `blank`

```java
public static ItemStack makeItem(@Nullable DollData data) {
    if (data == null) return blank();
    ItemStack stack = new ItemStack(GLItems.DOLL.get());
    stack.set(GLItems.DOLL_DATA.get(), new DollItemData(data.combat));        // combat -> DOLL_DATA
    if (data.customName != null) stack.set(DataComponents.CUSTOM_NAME,
                                           data.customName);                   // name -> vanilla component
    stack.set(DataComponents.DYED_COLOR,
              new DyedItemColor(data.getColor().getTextColor(), false));       // tint -> vanilla component
    return stack;
}

public static ItemStack blank() {
    return new ItemStack(GLItems.DOLL.get());    // pristine, no components -> no durability bar
}
```

- This is the **inverse** of `DollData.fromItemData` (which reads the same components back off a stack into a fresh `DollData`, with position/dimension/facing supplied at the summon/deploy site and state left to the caller — SUMMONED for a player summon, STORED for a controller install).
- A `null`/`blank()` doll has no components: no `DOLL_DATA` (so no durability bar), no custom name, and the tint defaults to red in all readers (`colorOf`, §4).
- Combat is stored as the `@SerialClass DollItemData`, a final class holding **only** a `CombatData combat` field (amount = health; no max health on the item). Its `hashCode` is prebuilt in the constructor and rebuilt by `onInject()` after l2serial deserialization. `DollItemData.fresh()` yields the full-health default.
- There is **no "lost doll" concept**: a missing `DOLL_DATA` component simply means a fresh doll; `summon`/`install` fall back to `DollItemData.fresh()`.

## 3. Health as a durability bar

The doll shows its remaining health over `BaseDollEntity.DEFAULT_MAX_HEALTH` (20) using the vanilla item durability bar:

- `isBarVisible`: `DOLL_DATA` present **and** `combat().amount() < 20`. A fresh/component-less doll is full health → no bar.
- `getBarWidth`: `Math.round(13.0F * amount / DEFAULT_MAX_HEALTH)`.
- `getBarColor`: green→red hue ramp, `Mth.hsvToRgb(frac / 3.0F, 1.0F, 1.0F)`.

## 4. Tint

- `colorOf(ItemStack)`: reads the vanilla `DYED_COLOR` component, mapping the exact text color back to a `DyeColor`; missing/no-match → `RED` (matches `DollData.getColor()`'s fallback).
- `getColor(stack, tintIndex)`: item-model tint index 1 → dye color; any other index → -1 (untinted). The doll item model applies tint only on layer 1.
- Entity side: the renderer picks a pre-tinted per-color texture (`textures/geo/doll/<dye>.png`, 16 colors) via `BaseDollEntity.getColor()` — **red** on the base, overridden by the `DollEntity` synced `INT` accessor. The item's `doll1` grayscale overlay is tinted through tint index 1.

## 5. Summon on use: `useOn`

1. Server-side only; `ServerPlayer` required.
2. Compute the spawn position: the clicked block's collision shape decides whether the doll goes inside the block or on the clicked face; the actual point is the block *center* at `+0.05` y (so the doll hovers slightly above the surface it was placed on).
3. `DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);` then `att.summon(sp, ctx.getItemInHand(), pos)` (pairing.md §3.1 — no entry can pre-exist, item and `DollData` never coexist).
4. On success: `shrink(1)` unless creative, return `CONSUME`; otherwise `FAIL`.

## 6. Trading and granting

Dolls are ordinary, tradeable items: item form carries no owner-id and no uuid, so a doll can be dropped/given/traded freely. Whoever summons one mints a **fresh** uuid and a fresh `DollData` entry in **their** ledger (pairing.md §7).

**Granting a fresh doll** (crafting/creative/loot): give a plain `DOLL` stack with **no components** — identity and full health are minted at summon time. Do **not** register anything in any capability; the `DollData` entry is created only at summon, in the summoner's ledger.

## 7. Tradeoffs / edge cases

- **Destroyed while itemized** (e.g. thrown into lava): that is a player-caused loss of a normal item; the ledger entry is already gone by design (pairing.md §7).
- **Cloned spares** just summon fresh, independent dolls (fresh uuid each) — cloning can only produce more *items*, never two entities sharing identity.

## 8. Loadout tooltip + inventory editing

- `getTooltipImage` reuses the talisman pocket's `InvTooltip`/`ClientInvTooltip` pair (already registered in `GLClient`): a 4×1 grid of the `DOLL_LOADOUT` stacks in menu order (main, off, core, cloth), hidden while shift is held and when the loadout is empty.
- `DollItem implements InvClickItem`, so right-clicking the stack in an inventory routes through the existing `GLClickHandler` and opens the same `DollLoadoutMenu` via `DollItemLoadoutProvider` — backed live by the stack's `DOLL_LOADOUT` component (`DollLoadoutItemHandler` item mode), titled with the doll's hover name. In-world `use`/`useOn` still summon.

## 9. The doll lance: `DollLanceItem`

`content/item/doll/DollLanceItem.java` — the mod's only melee weapon, and the **only** item that arms a doll's charge (§5.1b of control.md). A sword no longer does: the binding is `stack -> stack.is(GLItems.DOLL_LANCE.get())`, not a `#minecraft:swords` tag, so it matches this item exactly and nothing else.

```java
DOLL_LANCE = reg.item("doll_lance", DollLanceItem::new)
        .lang("Doll Lance").tab(TAB.key())
        .register();
```

A plain `Item`, deliberately **not** a `SwordItem`: a lance is a polearm, and inheriting `SwordItem` would bring along a `TOOL` component that cuts cobwebs, sword item abilities, and a `hurtEnemy` that returns true for every block.

### Damage

`ItemAttributeModifiers` in the vanilla melee-weapon shape, supplied through `Properties.attributes(...)` rather than a `getDefaultAttributeModifiers` override:

| Attribute | Amount | Result on a player (base 1 / 4) |
|---|---|---|
| `ATTACK_DAMAGE` | **+4** | 5 per swing |
| `ATTACK_SPEED` | **−3** | 4 − 3 = 1, i.e. one swing a second |

Both entries carry `Item.BASE_ATTACK_DAMAGE_ID` / `Item.BASE_ATTACK_SPEED_ID`. That is load-bearing twice over: `ItemStack.addModifierTooltip` folds the holder's own attribute base into a modifier with those ids, so the tooltip reads "4 Attack Damage, 1 Attack Speed" instead of a bare "+4 / −3" in raw attribute language — and `DollMeleeBehavior.meleeDamage`, which adds the held stack's main-hand `ATTACK_DAMAGE` modifiers to the doll's own `ATTACK_DAMAGE` attribute (base 1, the same base a player swings with), gets 4 for free and lands **5** on a charge. Declaring damage any other way (a getter, a custom component) would silently drop the doll back to bare 1. The sum is needed precisely because the loadout is not vanilla equipment: the lance is not an item in the doll's hand, so nothing feeds its modifiers into the entity's attribute map the way an equipped weapon's are (control.md §5.1b).

### No durability

`Properties.durability` is never called, so stacks carry no `max_damage` and no swing can wear one — which also means no `stacksTo(1)`: in a player's inventory a lance stacks like any other non-tool item, and there is nothing to repair or replace. (A *doll hand* is the exception and holds exactly one, whatever the stack: nothing spends a lance, so it is not ammunition — loadout.md §4.) This retires the old open question in control.md §10 ("sword durability: the charge costs the swing nothing") — the answer is that the item never had any.

### Model

Split with `SeparateTransformsModelBuilder` and `gui_light: front` (the same shape as `STRANGE_GLASSES`), because the two halves want opposite things:

| Context | Model | Why |
|---|---|---|
| all but `gui` (base) | `models/custom/doll_lance.json` — a Blockbench item model with real `elements` | a 28-unit polearm along **+Z**, which neither `item/generated` nor `item/handheld` can express, so it has to be hand-authored |
| `gui` | generated, `item/generated` + `textures/item/tool/doll_lance_icon.png` | a flat 16×16 sprite is the only thing that reads at slot size; no transform of the 3D lance lands inside 16px |

```java
var base = pvd.nested()
        .parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/doll_lance")));
var guiModel = pvd.nested()
        .parent(new ModelFile.UncheckedModelFile("item/generated"))
        .texture("layer0", pvd.modLoc("item/tool/" + ctx.getName() + "_icon"));
pvd.getBuilder(ctx.getName())
        .customLoader(SeparateTransformsModelBuilder::begin)
        .base(base)
        .perspective(ItemDisplayContext.GUI, guiModel)
        .end().guiLight(BlockModel.GuiLight.FRONT);
```

- `custom/doll_lance.json` is the **Blockbench export kept verbatim** — `format_version`, `credit`, `texture_size`, `groups`, `particle` and the `#0` texture key all stay, exactly as `custom/strange_glasses_head.json` does. Only the placeholder texture is filled in (`gensokyolegacy:item/tool/doll_lance`); the game ignores the rest, and round-tripping the file through Blockbench must not produce a diff.
- Its four hand `display` transforms are the authored ones. `fixed` (item frame) deliberately has none: the lance is 28 units pointing at the camera there.
- `SeparateTransformsModel` takes `getQuads`/`getRenderTypes`/`getParticleIcon` from the **base**, so dropped-item particles come from the 3D model's `particle` key. Only `applyTransform` redirects, which is what swaps the quads per context.