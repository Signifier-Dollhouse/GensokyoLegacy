# Seven-Colored Doll Glove

`DollGloveItem` — the **Seven-Colored Doll Glove** — itemizes doll control: mode selection via the l2itemselector wheel, exactly mirroring the border-umbrella architecture (`content/item/umbrella/`).

> Status: implemented. Texture is a generated placeholder (`textures/item/tool/doll_glove.png`, seven-stripe mitten) — replace with `temp/七色人偶手套.png` art when available (16×16, same path).

## 1. Mode enum + handlers

`DollGloveMode` enum (ids for the wheel) + six small handler classes under `content/item/glove/mode/`, following `BorderUmbrellaMode`/`UmbrellaMode`:

| idx | Mode | Icon | Interaction |
|---|---|---|---|
| 0 | `SUMMON` | glove icon (itself) | `use()`: no summoned dolls → summon everything parked (STORED promoted to TEMP, then summoned) plus every doll item in the player inventory, in a ring around the owner — no ledger cap. Otherwise recall: `itemize` every summoned doll, overflow parked to data as STORED, and all TEMP consolidated to STORED. Block-hosted dolls never touched. |
| 1 | `HEAL_MARK` | `Items.GOLDEN_CARROT` | Hidden from the wheel, implementation kept: `use()` toggles the heal mark on the cached ray-trace target (control.md §5–6). Any living entity, same 48-block range. |
| 2 | `VOLLEY` | `GLItems.STAR` (star danmaku, the starlight-hexbrew star) | `use()` or left-click: `issueIteration(player, target, REGULAR_ATTACK)` on the cached target (left-click on an entity uses the punched entity). |
| 3 | `SUPER` | explosive hexbrew | `use()` or left-click: `issueOneTime(player, target, SUPER_ATTACK)` — one **random** available doll. Listed only while a summoned doll can perform it, unless already selected. |
| 4 | `SUICIDE` | `Items.TNT` | `use()` or left-click: `issueOneTime(player, target, SUICIDE)` — one **random** available doll. Listed only while a summoned doll can perform it, unless already selected. |
| 5 | `STOP` | `Items.SHIELD` | Hidden from the wheel, implementation kept: `use()` stops **all summoned** dolls — `doll.actions.stop()` + clear `iteration`. Strays and block-hosted dolls untouched. |

There is no editor mode: right-clicking an owned doll (or any doll in creative) opens its loadout in every mode, before the mode action runs. Left-click paths never open the editor, so fighting never pops a menu.

Summon and volley are always listed; heal-mark and stop are implemented but hidden. Attack/mark modes act on the cached ray-trace target (§2); empty cache → `no_target` message.

## 2. Targeting — client ray trace, cached UUID, server re-check

No entity clicks: the glove acts on a cached ray-trace target (max 48 blocks), so out-of-reach entities work.

- Client `GloveTargetCache`: on item inventory tick while the glove is held (either hand, local player only, every 5 ticks), ray-trace the crosshair (48 blocks, living entities only, blocked by blocks, **holder excluded** — own-doll exclusion isn't reliable client-side, so allies are excluded by the server re-check instead) and cache the UUID + timestamp. Each cache refresh re-sends the UUID to the server; actual glove `use()` also refreshes the cache time. Cache expires after 5 seconds. On a miss the stale entry lingers until TTL (no flicker).
- Client pre-filters the ray trace by held mode, mirroring the server checks: every mode accepts dolls (right-click opens the loadout of an owned doll in any mode; ownership is server-side only — the owner UUID is not synced — so the client filters by type and the server enforces owner/creative); summon accepts nothing else (it summons/recalls globally); the rest accept valid attack targets (not self, not player-owned, not any doll, attackable, non-spectator).
- Glowing marker: client-side only, only while the player holds the glove — a mixin on the glow check (`LivingEntity`/`Entity#isCurrentlyGlowing` or the render path) returns true for the cached UUID, and a `getTeamColor` mixin tints the outline in the held mode's color (`DollGloveMode.glowColor`: aqua summon, green heal-mark, red attacks, gray stop), or gold when the target is a doll. Declared in `gensokyolegacy.mixins.json`. No server sync involved.
- Server `use()` consumes the last-synced UUID: re-validates alive, within 48 blocks. Right-click first tries the editor open (owned doll → loadout); otherwise attack modes require a valid attack target — no allies (`OwnableEntity` owned by the holder, which covers their dolls and pets), no dolls at all, attackable and non-spectator. Left-click paths (`onLeftClickEntity`, block click, empty-swing packet) skip the editor and run the attack directly. Heal-mark takes anything. Stale/missing cache → `no_target` message.
- Heal-mark mode uses the same target pipeline (any living entity, 48 blocks).

## 2b. Hover overlay (ModularGolems mirror)

- `DollGloveOverlay` (client GUI layer above the crosshair, registered in `GLClient`): while the glove is held in any mode with no screen open, hovering a doll (vanilla crosshair `EntityHitResult`) shows its name plus the loadout in the menu's cross arrangement (`DollLoadoutTooltip`/`DollClientLoadoutTooltip`, slot frames + ghost icons from the menu texture atlas). Display-only; opening still goes through right-click `use()`.

## 2c. Attack-mode status sidebar

- `DollAttackStatusOverlay` (client GUI layer above the crosshair, registered in `GLClient`): while the glove is held in volley/super/suicide mode with no screen open, the right side lists every summoned doll in ledger order as its doll item icon (tint + health bar, built from the doll entity's synced data) with a status frame — hidden for invalid (no valid weapon for this attack in either hand) or untracked dolls, white idle, yellow preparing (ticket held, behavior not started), red attacking, green done (already fired in the live volley). In every mode the doll under the crosshair is framed orange instead. Display-only.
- Validity is server-computed into the doll entity's `DATA_VALID_MASK` (one bit per `DollActionType`, from the authoritative ledger loadout via `DollBehaviorRegistry.findHand`); status reads the synced action mirror (`DollStatus`: `DATA_ACTION_STATE` + `DATA_ACTION_TYPE`, recomputed server-side every tick in `DollEntity.tick` from the ticket + `DollCommandGoal.isExecuting` + `DollCommander.doneType`). Done only reports while the volley is still in-flight, and only for the held attack's type — no latch to clear. Implemented as a l2itemselector `SelectionSideBar` with a right-side anchor (opposite side from the quest `InfoSideBar`, so both can show at once). The roster itself (entity ids in ledger order) is pushed in `DollRosterToClient` only when it changes — the `DollAttachment` capability syncs nothing to clients (all `DollData` fields are `toClient = false`).

## 3. Selector + wheel (umbrella mirror)

- `DollGloveSelectionListener` — `extends IItemSelector implements WheelAdaptor.Provider`, id `gensokyolegacy:doll_glove`, `static register()` → `IItemSelector.register(INSTANCE)` (§4); `test(stack)` = `instanceof DollGloveItem`; `getIndex`/`getList`/`swap`/`move` read and write the `DOLL_GLOVE_MODE` component ordinal. The wheel and scroll cycle the visible modes only (`DollGloveModes.available`: summon + volley always, super/suicide while a summoned doll matches, the current mode always so the index never goes missing); the select packet carries ordinals.
- `DollGloveModeWheel` — `PersistentWheel<DollGloveModeEntry>` over the visible modes; `DollGloveModeEntry` renders the mode icon; `select(index)` sends `DollGloveSelectPacket(0, ordinal)`.
- Default `WheelKeyHandler` (no manage screen / fake wheel needed).
- New data component: `DOLL_GLOVE_MODE` (`DC.int`, default 0) on the glove stack (§5).

## 4. Network

- `DollGloveSelectPacket(int wheel, int index)` — server-side mode switch on the held glove (mirror `BorderUmbrellaSelectPacket`); index is a mode ordinal.
- `DollGloveTargetPacket(UUID target)` — client→server target sync on every cache refresh (§2); the server keeps one cached UUID + timestamp per player and re-validates on `use()` (alive, ≤48 blocks, not ally).
- `DollGloveSwingPacket()` — client→server left-click-empty while the glove is held in an attack mode; runs the attack at the cached target without the editor check. No other packets required: all commands run server-side, the ledger is server-side, and slot sync rides the existing entity data (loadout.md §3).

## 5. Registration

- `GLItems` — `DOLL_GLOVE` = `reg.item("doll_glove", p -> new DollGloveItem(p.stacksTo(1)))` + `.model(generated item/doll_glove)` + `.lang("Seven-Colored Doll Glove")` + tab + `DOLL_GLOVE_MODE` DCVal.
- Mod constructor — `DollGloveSelectionListener.register()` beside `BorderUmbrellaSelectionListener.register()`; `GensokyoLegacy.HANDLER` registers `DollGloveSelectPacket` + `DollGloveTargetPacket`; the `CodecHandler<ItemStack>` (loadout.md §2) alongside `FluidIngredient`.
- `GLLang` — `ItemGlove` enum: mode names/descriptions plus a message for every non-trivial action (summoned count, recalled count + parked count, volley/super/suicide issued, stopped count, mark toggled/untoggled, no_target, no_doll, not_doll).
- Textures — copy `temp/七色人偶手套.png` to `assets/gensokyolegacy/textures/item/doll_glove.png`.
- Mixins — target-glow mixin declared in `gensokyolegacy.mixins.json`.

## 6. Files to create

- `content/item/glove/DollGloveItem.java`
- `content/item/glove/mode/DollGloveMode.java`
- `content/item/glove/mode/DollGloveHandler.java` + 6 mode classes
- `content/item/glove/DollGloveSelectionListener.java`
- `content/item/glove/DollGloveLeftClickHandler.java`
- `content/item/glove/client/DollGloveModeWheel.java`, `DollGloveModeEntry.java`
- `content/item/glove/network/DollGloveSelectPacket.java`, `DollGloveTargetPacket.java`, `DollGloveSwingPacket.java`
- `content/item/glove/client/GloveTargetCache.java` (+ per-player server cache, e.g. on the commander or a player attachment)
- `mixin/GloveTargetGlowMixin.java` (client glow for the cached UUID while the glove is held)

See `checklist.md` for the full cross-document file/registration list.