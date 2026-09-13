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