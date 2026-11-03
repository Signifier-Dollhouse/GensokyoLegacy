# Minamitsu's Anchor

Minamitsu's anchor is a single heavy weapon with three modes: a mining hammer that comes back, a
grappling hook that drags things, and a storm caller that turns wet air into danmaku.

> **Status:** implemented, including the two-entity split in §5. Numbers are first guesses and are
> meant to be tuned; the playtest checklist is §10.
> **Package:** `content/item/anchor/` (item, mode, wheel), `content/entity/misc/anchor/` (hook,
> unbound bullet, shared storm helper), `content/entity/misc/render/` (the hook renderer),
> `event/` (left-click plumbing).
> **Related:** `nue_trident.md` (the other thrown weapon; the unbound volley shares
> `ItemBulletEntity`), `dagger_glove.md` (mode wheel and selector listener),
> `doll/ghost_power.md` (the spiritual power pool the storm mode spends).

## 1. What it is

A thrown anchor that flies in a straight line with no gravity, out to a fixed reach, then does
whatever its mode says. Two entity types share the weapon: a bounded **hook** (a plain
`ThrowableProjectile`) for every throw that can come back, and an unbound **danmaku bullet** for the
storm volley. Only the volley is a danmaku; the hook is an ordinary projectile, so it can stop on a
block face, hang, and drag its owner with vanilla collision and server-authoritative position.

| mode | left click | right click |
|---|---|---|
| **Destroy** | normal attack / mine (vanilla) | throw; on a block break 3×3×3, on an entity deal damage; then fly home |
| **Voyage** | *empty* left click only throws; the anchor pulls the hit entity toward the player. A block or entity click stays vanilla | throw; stick where it hits — or hang where the reach runs out — and pull the player in |
| **Storm** | throw (5 spiritual power); on impact deal area damage | throw 5 unbound anchors (10 spiritual power), reach 48, no return |

The anchor is also a pickaxe: diamond harvest level, and it swings for +9 attack damage at −3 attack
speed. Mining in Destroy mode is ordinary mining; the 3×3×3 break is only the thrown anchor.

**Wet danmaku.** For either Storm throw, the anchor checks each tick whether it is wet (water, rain,
bubble column). The bounded hook is a real entity, so it reads its own fluid state; the unbound
bullet is a danmaku, whose fluid state is a no-op, so it samples the block it is in and asks its
thrower (§5.3). While wet it sheds 4 straight danmaku per tick and is drawn with the water-anchor
model (§5.4); when it lands it bursts 100 gravitational ones.

## 2. Components

| Piece | File | Job |
|---|---|---|
| `AnchorItem` | `content/item/anchor/AnchorItem.java` | the item: mode dispatch, cost, cooldown, spawn |
| `SpiritualDanmakuItem` | `content/item/spiritual/SpiritualDanmakuItem.java` | `DanmakuItem` with the spiritual bar (§2.1) |
| `AnchorMode` | `content/item/anchor/AnchorMode.java` | `DESTROY / VOYAGE / STORM`, display name, wheel entry |
| `AnchorModel` | `content/item/anchor/AnchorModel.java` | the base model plus one GUI icon model per mode (§3) |
| `AnchorThrow` | `content/item/anchor/AnchorThrow.java` | what a single throw does: `DESTROY / GRAB / PULL / STORM / STORM_UNBOUND` |
| `AnchorHookEntity` | `content/entity/misc/anchor/AnchorHookEntity.java` | the bounded anchor: one synced throw type, one synced phase machine |
| `AnchorBulletEntity` | `content/entity/misc/anchor/AnchorBulletEntity.java` | the unbound storm anchor, a danmaku bullet |
| `AnchorStorm` | `content/entity/misc/anchor/AnchorStorm.java` | wetness, trail, burst and area damage, shared by both |
| `AnchorHookRenderer` | `content/entity/misc/render/AnchorHookRenderer.java` | draws the hook in its throw orientation and the chain back to the thrower |
| `NoFadeBulletRenderer` | `content/entity/misc/render/NoFadeBulletRenderer.java` | the unbound volley's renderer: no fading, on the flight line (existing) |
| `AnchorSelectionListener` | `content/item/anchor/AnchorSelectionListener.java` | wheel/list registration, like the fan's |
| `AnchorModeWheel` | `content/item/anchor/client/AnchorModeWheel.java` | the client wheel |
| `AnchorSwingPacket` | `content/item/anchor/network/AnchorSwingPacket.java` | left-click in the air reaches the server |
| `AnchorEventHandlers` | `event/AnchorEventHandlers.java` | left-click plumbing, per mode (§4) |

`AnchorItem` and the unbound bullet reuse the iron dagger's approach: the item is a `DanmakuItem`
and the bullet an `ItemBulletEntity`, which gives the volley the existing `ItemBulletRenderer`
drawing path (from the item's own model), the l2serial save/spawn code and the fast-projectile
collision. The bounded hook reuses none of that: it is a normal `ThrowableProjectile` with a real
renderer, because a throw that must stop on a block face, hang there and drag its owner needs the
server to own its position (§5). `NoFadeBulletRenderer` therefore stays only on the volley.

### 2.1 `SpiritualDanmakuItem`

The bar belongs to the item, but `SpiritualItem` extends `Item` and the anchor needs `DanmakuItem`,
so the bar gets a danmaku base of its own:

```java
public class SpiritualDanmakuItem extends DanmakuItem {

	public SpiritualDanmakuItem(Properties properties, DanmakuBullet type, DyeColor color, float size, int max) {
		super(properties.component(GLItems.SPIRITUAL_POWER_MAX.get(), max), type, color, size);
	}

	// isBarVisible / getBarWidth / getBarColor / appendHoverText -> SpiritualPower
}
```

* `SpiritualPower` already carries `appendHoverText`; add `barVisible(stack)`, `barWidth(stack)` and
  `BAR_COLOR = 0x55FFFF` beside it, so `SpiritualItem`'s overrides become one-line delegations (no
  behaviour change) and `SpiritualDanmakuItem` restates nothing but the delegation;
* `AnchorItem extends SpiritualDanmakuItem` and passes the pool size through the constructor, as
  the fan does through `SpiritualItem`.

`JizoBlock` already refills any held stack with a `spiritual_power_max` component, so no new refill
path is needed: the anchor is refilled by offering XP to a Jizo, exactly like the fan.

## 3. The item

`AnchorItem extends SpiritualDanmakuItem` — `stacksTo(1)`, no durability, pool size 100 — and its
registration adds:

```java
reg.item("minamitsu_anchor", p -> new AnchorItem(p.stacksTo(1)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 9.0, ADD_VALUE), MAINHAND)
                        .add(Attributes.ATTACK_SPEED,
                                new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.0, ADD_VALUE), MAINHAND)
                        .build())
                .component(DataComponents.TOOL, Tiers.DIAMOND.createToolProperties(BlockTags.MINEABLE_WITH_PICKAXE)),
        DanmakuItems.Bullet.DAGGER, DyeColor.WHITE, 1.0F, 100))
```

* `TOOL` is what makes a plain `Item` behave as a diamond pickaxe: `isCorrectToolForDrops` and the
  mining speed both read it, and the 3×3×3 break reuses the same rule.
* No `MAX_DAMAGE`: the anchor never wears out, and the durability bar is free for the power pool.
* +9 / −3 are the item's own modifiers, so the tooltip reads as a normal weapon; a swung hit is a
  normal `player.attack` hit (no custom melee code).
* `DanmakuItems.Bullet.DAGGER` is passed as the danmaku type so `buildRenderer` picks the item-model
  renderer. **Do not** add `DanmakuItems.Bullet.DAGGER.tag` — the anchor is not ammo.
* Tags: `ItemTags.PICKAXES`, `ItemTags.MINING_ENCHANTABLE`, `ItemTags.MINING_LOOT_ENCHANTABLE`,
  `ItemTags.WEAPON_ENCHANTABLE`, `ItemTags.SHARP_WEAPON_ENCHANTABLE`; creative tab;
  `L2ISTagGen.SELECTABLE` (the wheel needs it, like the fan and the gloves).

The art is already prepared in `temp/` and only needs moving and wiring:

| from `temp/` | to `src/main/resources/assets/gensokyolegacy/` |
|---|---|
| `anchor.json` | `models/custom/minamitsu_anchor.json`; texture `block/铁锚` and the particle → `gensokyolegacy:item/tool/iron_anchor` |
| `anchor.json` (same file) | `models/custom/minamitsu_anchor_wet.json`; texture `block/铁锚` and the particle → `gensokyolegacy:item/tool/water_anchor` |
| `iron_anchor.png` (64×64) | `textures/item/tool/iron_anchor.png` |
| `water_anchor.png` (64×64) | `textures/item/tool/water_anchor.png` |
| `iron_anchor_icon.png` (16×16) | `textures/item/tool/iron_anchor_icon.png` — GUI icon, Destroy |
| `anchor_icon_sink.png` (16×16) | `textures/item/tool/anchor_icon_sink.png` — GUI icon, Voyage |
| `anchor_icon_storm.png` (16×16) | `textures/item/tool/anchor_icon_storm.png` — GUI icon, Storm |

The Blockbench model has hand display transforms and no GUI transform, so registration uses the
same split as the iron dagger / doll lance: the custom model is the base, and
`ItemDisplayContext.GUI` gets a flat `item/generated` model over one of the three 16×16 icons
through `SeparateTransformsModelBuilder`. The hook renderer draws with `ItemDisplayContext.GROUND`
and lays the model down itself (its shaft is authored along +y, the flight direction along +z), so
the model needs no ground transform and the file stays hand-authored; datagen never rewrites it.

`AnchorModel.model` generates one such model per mode — `minamitsu_anchor` itself (Destroy icon)
plus `minamitsu_anchor_destroy/_voyage/_storm` — and hangs them off the base model as item
overrides on `gensokyolegacy:anchor_display`, whose value is `AnchorItem.displayPredicate`:
`AnchorMode.ordinal() + 1`. Vanilla takes the first override whose predicate reaches its value, so
the generated overrides are emitted in mode order. That is how the held icon follows the mode
without a second item per mode; the wheel entries are the anchor stack itself (§3.1).

The two models belong to two items. The visible `minamitsu_anchor` always uses the iron model; a
hidden twin `anchor_wet` exists only so a thrown storm anchor can be drawn with the water texture,
and is registered but kept out of the creative tab and tagged `WEBSITE_HIDDEN`. It is a plain
`DanmakuItem` — same bullet type, colour and size, no `AnchorItem` behaviour — so it can never be
thrown or given a mode. The twin is needed because the danmaku renderer resolves one baked model per
*item* from `new ItemStack(item)` with no entity or level, so an `ItemProperties` predicate cannot
see wetness (§5.4). The hook does not share that limit, but returns the same twin from `getItem()` so
both entities show the same art.

### 3.1 Mode storage and selection

`AnchorMode` lives in a data component, exactly like `YuyukoFanMode`:

```java
DC.enumVal("anchor_mode", EnumCodec.of(AnchorMode.class, AnchorMode.values()))
```

`AnchorSelectionListener` / `AnchorModeWheel` are near-copies of `YuyukoFanSelectionListener` /
`YuyukoFanModeWheel`, registered in `commonSetup` next to the other selectors. A wheel entry is
`AnchorMode.icon()`: the anchor stack itself with the mode component set and `ITEM_NAME` naming the
mode, so the item override of §3 already gives it the right flat icon and no icon items exist. The
held stack is what `swap` writes the mode to. `SelectorSelectPacket` already exists and needs no
change.

### 3.2 Right click and left click

```java
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (isLinked(stack)) return InteractionResultHolder.fail(stack);
    if (GrazeHelper.forbidDanmaku(player)) return InteractionResultHolder.fail(stack);
    if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
    if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
    AnchorThrow type = switch (getMode(stack)) {
        case DESTROY -> AnchorThrow.DESTROY;
        case VOYAGE -> AnchorThrow.PULL;
        case STORM -> AnchorThrow.STORM_UNBOUND;
    };
    return throwAnchors(sp, hand, type)
            ? InteractionResultHolder.sidedSuccess(stack, false)
            : InteractionResultHolder.fail(stack);
}
```

`throwAnchors(sp, hand, type)` is the single gate for both clicks:

1. refuse while the stack still carries a hook's UUID (§3.3);
2. refuse if the item is on cooldown (`player.getCooldowns().isOnCooldown(this)`);
3. for `STORM` / `STORM_UNBOUND`, refuse unless the stack has 5 / 10 power, with a chat message
   (`GLLang.ItemAnchor.NEED_POWER`), matching how `ScatterFanHandler` refuses an empty fan;
4. spawn the entity — one `AnchorHookEntity` for every throw except `STORM_UNBOUND`, five
   `AnchorBulletEntity` for it (§5) — spend the power, add the cooldown, swing the hand, play the
   sound; `SpellContainer.track` only the bullets, since a hook is not a `SimplifiedProjectile` and
   discards itself when its owner is gone;
5. return `sidedSuccess`.

### 3.3 One bounded anchor at a time

`GLItems.ANCHOR_LINK` (`DC.uuid("anchor_link")`) holds the UUID of the bounded hook the stack threw,
written only after `addFreshEntity` succeeds and only for hooks (`STORM_UNBOUND` bullets leave it
empty). Its presence alone is the client's answer: no entity lookup, no sync of its own.

* **Client** — `ItemInHandRendererMixin` cancels the held-item draw while the stack has the
  component. That covers first person, and third person via `ItemInHandLayer`, which calls the same
  `renderItem`. The arm is still drawn, so the player looks empty-handed rather than armless.
* **Server** — `AnchorItem.inventoryTick` runs against the `ServerLevel` lookup every tick for every
  inventory compartment: if `sl.getEntity(id)` is not a live `AnchorHookEntity`, the component is
  removed. `ServerPlayer.tick` broadcasts inventory changes every tick, so the hand comes back on the
  same tick the hook discards.
* Clearing on the hook's `remove` was rejected: by then the stack may sit in another slot, a shulker
  or a container, none of which the hook can reach, and `discard()` is `final` anyway.

The link does not survive a hook that is unloaded with its chunk: the lookup misses and the server
frees the hand. The hook is 200 ticks old at most and its reach is 48 blocks, so this only happens
after a teleport or a restart, where dropping the link is the desired outcome.

## 4. Left-click plumbing

Left clicks never reach `use`, so the modes that need one are plumbed through events. Each mode is
handled separately — Destroy is entirely vanilla, Voyage only throws on an empty hand, and Storm
throws on any left click:

| event | Destroy | Voyage | Storm |
|---|---|---|---|
| `PlayerInteractEvent.LeftClickEmpty` | nothing | throw `GRAB` | throw `STORM` |
| `PlayerInteractEvent.LeftClickBlock` | vanilla mine | vanilla mine | cancel, throw `STORM` |
| `AttackEntityEvent` | vanilla attack | vanilla attack | cancel, throw `STORM` |

Empty clicks never reach the server on their own, so `LeftClickEmpty` (client only) sends
`AnchorSwingPacket`, whose handler repeats the mode/cooldown/power checks before throwing — a client
is only a hint, as with `DollGloveSwingPacket`. Voyage deliberately leaves the block and entity
clicks vanilla: it is a grappling hook, and losing mining or punching while it is held would be a
nuisance. Cancelling `LeftClickBlock` on the client as well stops the mining animation from
starting in Storm mode.

`AnchorSwingPacket` is registered in `GLPackets` (one `.toServer(...)` line) and the handler version
goes `3` → `4`.

## 5. The two entities

| throw | entity | base | returns |
|---|---|---|---|
| `DESTROY`, `GRAB`, `PULL`, `STORM` | `AnchorHookEntity` | `ThrowableProjectile` | yes |
| `STORM_UNBOUND` | `AnchorBulletEntity` | `ItemBulletEntity` | no |

`AnchorThrow` stays the item-level enum (`reach()`, `cost()`, `isStorm()`); `AnchorItem.throwAnchors`
reads it and spawns one hook or five bullets. `returns()` goes away — the hook only ever sees the
four throws that return, and the bullet is always unbound.

**Why the split.** A danmaku is a `SimplifiedEntity`: self-predicted movement, no fluid state, and a
batched renderer, which is exactly what the volley needs (five anchors, each shedding a trail, must
not cost five draw calls). A bounded anchor needs the opposite: server-authoritative position, so it
stops flush on a block face, hangs where it stopped, and drags its owner with a real velocity instead
of a guess. Sharing one base means the hook inherits a no-op fluid state and unusable prediction, or
the bullet pays for a phase machine it never enters.

The three references agree on the moving parts; the hook takes the best of each:

| from | taken |
|---|---|
| vanilla `FishingHook` / `ThrowableProjectile` | `ProjectileUtil.getHitResultOnMoveVector` swept hit, hit callbacks that need not discard, owner id in the spawn packet |
| Cataclysm `Tidal_Hook_Entity` | `getDefaultGravity() = 0`, empty pickup, pull = velocity toward the anchor with `hurtMarked` |
| Twilight Forest `CubeOfAnnihilation` | zero gravity, velocity re-set by the entity every tick |

Rejected: the cube's "fly toward a point 16 blocks ahead of the owner" (the anchor keeps its throw
direction, so aiming means aiming) and the tidal claw's `owner.setNoGravity(true)` during the pull —
a lost anchor must never leave a player floating.

### 5.1 `AnchorHookEntity`

Fields, with the two the client needs in `SynchedEntityData`:

| field | type | side | meaning |
|---|---|---|---|
| `type` | `AnchorThrow` | synced byte | the renderer shows the wet model only for a storm throw |
| `phase` | `Phase` | synced byte | `FLY`, `RETURN`, `GRAPPLE`, `DRAG`; the client steers by it too |
| `damage` | `float` | server, NBT | the thrower's attack damage at throw time |
| `origin` | `Vec3` | server, NBT | eye position at throw; the reach is measured from here |
| `flyDir` | `Vec3` | NBT, spawn data | normalized throw vector; drives FLY and the client's prediction |
| `grapple` | `int` | server, NBT | ticks left on the pull or the drag |
| `returnTicks` | `int` | server, NBT | return timer |
| `trail` | `int` | server, NBT | trail budget already spent |
| `grabbed` | `UUID` | server, NBT | the entity a `GRAB` hook is dragging |

| constant | value |
|---|---|
| fly speed | 1.5 b/t, **no gravity** |
| reach | 16 blocks, measured from `origin` (not from the moving owner) |
| return speed | 1.5 b/t |
| return timeout | 60 ticks |
| grapple timeout | 40 ticks |
| drag speed | 1 b/t |
| drag timeout | 40 ticks |
| lifetime cap | 200 ticks |

`launch(type, damage, origin, dir)` stores the fields (including the normalized throw vector), writes
the type and phase to the synched data, and calls `shoot` so the entity starts out rotated the way it
flies. The vector is stored rather than recovered from that rotation: `Projectile` records yaw as
`atan2(dx, dz)` and pitch as positive-up, the *mirror* of the look convention that
`Vec3.directionFromRotation` inverts, so a round trip through the rotation feeds back a reflected
heading and the hook settles on a fixed one.

The velocity is re-set every tick *before* the base class moves, and gravity is zero, so the base's
0.99 drag never shows:

```java
@Override
public void tick() {
    steer();                 // set this phase's delta
    super.tick();            // swept hit test and move, both use that delta
    if (level().isClientSide || isRemoved()) return;
    tickPhase();             // trail, reach, return timer, pull
}

private void steer() {
    switch (phase()) {
        case FLY -> setDeltaMovement(flyDir.scale(FLY_SPEED));
        case RETURN -> {
            Entity owner = getOwner();
            Vec3 diff = owner == null ? Vec3.ZERO : owner.getEyePosition().subtract(position());
            setDeltaMovement(diff.lengthSqr() < 1e-6 ? Vec3.ZERO : diff.normalize().scale(RETURN_SPEED));
        }
        case GRAPPLE, DRAG -> setDeltaMovement(Vec3.ZERO);
    }
}

// the orientation is set by shoot() at launch and never updated again
protected void updateRotation() {
}
```

`flyDir` is written to NBT and to the spawn data (`IEntityWithComplexSpawn`): the client is never
sent the spawn velocity, so it needs the vector to predict the flight; the NBT copy keeps it after a
chunk reload. `updateRotation` is empty for a reason: the vanilla implementation lerps the rotation
toward the current delta, so the anchor would turn around on the return leg and while it drags. A
fixed heading is also what lets the renderer draw the model without per-tick interpolation.

**Collision.** `ThrowableProjectile` already runs the vanilla swept test
(`ProjectileUtil.getHitResultOnMoveVector`) used by `FishingHook`, so nothing custom is needed.
`canHitEntity` returns `phase() == FLY && throwType() != PULL && super.canHitEntity(target)`:

* only the outbound leg can hit — a returning or hanging anchor passes through everything;
* a `PULL` anchor is for terrain, so it ignores entities entirely and flies on (no impact sound, no
  deflection);
* the owner is already excluded by `super`.

The callbacks never discard; they move the phase machine. `onHitBlock` snaps to the face first,
because zeroing the delta stops the entity short of the hit point:

```java
@Override
protected void onHitBlock(BlockHitResult hit) {
    if (level().isClientSide || phase() != Phase.FLY) return;
    setPos(hit.getLocation());
    setDeltaMovement(Vec3.ZERO);
    playImpact(SoundEvents.TRIDENT_HIT_GROUND);
    switch (throwType()) { ... }
}
```

Both callbacks return early on the client. The client runs the same swept test, but the server owns
the outcome; position updates every tick (§7) are what the client renders.

| phase | per tick | leaves when |
|---|---|---|
| `FLY` | storm trail while wet; reach check | hit, or `position().distanceTo(origin) > reach()` |
| `RETURN` | homing to the owner's eye | within 1.5 blocks, 60 ticks, owner gone → discard |
| `GRAPPLE` | pull the owner (§6.2) | arrival (<2 blocks), 40 ticks, sneak, owner gone → `RETURN` |
| `DRAG` | pull the grabbed entity toward the owner (§6.2) | arrival (<2 blocks), 40 ticks, target gone → `RETURN` |

`tickCount > 200` is the backstop: a hook that neither hits, expires nor returns is discarded rather
than left to a chunk unload.

| hit | `DESTROY` | `GRAB` | `PULL` | `STORM` |
|---|---|---|---|---|
| block | break, `RETURN` | `RETURN` | `GRAPPLE` | burst, `RETURN` |
| entity | hurt, `RETURN` | `DRAG`, then `RETURN` | flies through | burst, `RETURN` |

**Owner hygiene.** Every phase discards when the owner is gone, removed or dying, so the hook can
never outlive its thrower. It cannot be `SpellContainer.track`ed (that takes a
`SimplifiedProjectile`), and this check replaces the tracking the danmaku path had. No item is
dropped or picked up, and on a caught return nothing is given back: the item never left the hand.

### 5.2 `AnchorBulletEntity`

The unbound volley keeps the danmaku path unchanged: `@SerialClass`, `damage` + `origin` + the trail
counter, `setup(0, 200, false, false, dir.scale(1.5))` from the item, the fast-projectile collision,
and `markErased(false)` on impact.

* `configure(damage, origin)` — no throw type; every instance is an unbound storm anchor.
* reach 48 from `origin`; past it the bullet is erased.
* hit → `AnchorStorm.burst(this, owner)`, then erased. There is no return.
* trail → 4 straight bullets per tick while wet, capped at 160.
* `getItem()` answers with `ANCHOR_WET` while wet (§5.4); a bullet's fluid state is a no-op, so this
  is the one place that still needs the twin and the owner sampling.
* rendered by the existing `NoFadeBulletRenderer`: full opacity at every range, model on the flight
  line.

### 5.3 `AnchorStorm`

One static helper holds everything the hook and the bullet share, so wetness, trail, burst and area
damage exist once and the volley and the single throw emit identical danmaku:

| method | job |
|---|---|
| `wetAt(Level, Vec3)` | water, bubble column, or rain at that cell |
| `isWet(Entity)` | `self.isInWaterRainOrBubble() \|\| wetAt(...)` |
| `shedTrail(Entity self, Entity owner, boolean wet, int spent)` | 4 rotating cross bullets; returns the new budget |
| `impact(Entity self, Entity owner, boolean wet)` | area damage, then the 100-bullet spray if wet |
| `areaDamage(Entity self, Entity owner)` | 6 damage in 4 blocks, gated by `IDanmakuEntity.canHurt` |
| `burst(Entity self, Entity owner)` | the 100-bullet gravitational spray |

The owner is a parameter rather than `self.getOwner()`: `Entity` does not declare it, and the helper
also serves the danmaku bullet, whose owner only exists through `TraceableEntity` on the projectile
base classes.

The hook's wetness is just `AnchorStorm.isWet(this)` — it is a real entity, so `isInWater` and
`isInRain` update normally. The bullet's is `AnchorStorm.isWet(this) || owner.isInWaterRainOrBubble()`,
because `SimplifiedEntity.updateInWaterStateAndDoFluidPushing()` is a no-op and its owner is the only
thing that notices the rain.

### 5.4 Drawing the hook and the wet anchor

The held anchor is always the iron model; only a thrown storm anchor turns into the water one, and
only while the wet condition that emits danmaku holds. Both entities override `getItem()` to answer
with the twin while wet:

```java
// hook: the real stack never changes, it is synthesized
public ItemStack getItem() {
    return throwType().isStorm() && isWet() ? GLItems.ANCHOR_WET.get().getDefaultInstance()
            : GLItems.MINAMITSU_ANCHOR.get().getDefaultInstance();
}

// bullet: the twin replaces the stack it was spawned with
public ItemStack getItem() {
    return isWet() ? GLItems.ANCHOR_WET.get().getDefaultInstance() : super.getItem();
}
```

| what | how |
|---|---|
| hook | `AnchorHookRenderer`: the fixed yaw/pitch from launch, the model laid flat, then `ItemRenderer.renderStatic(..., ItemDisplayContext.GROUND)` |
| hook while wet | its `getItem()` returns `ANCHOR_WET` for a storm throw |
| unbound bullet | unchanged `NoFadeBulletRenderer` / `ItemModelProjectileType` |
| bullet while wet | its `getItem()` returns `ANCHOR_WET` |

`ThrownItemRenderer` was rejected for the hook: it billboards with `cameraOrientation` and throws
away the flight rotation, which reads as a sprite on a 3D anchor. The heading is the one from `shoot`
and never changes (§5.1), so the anchor points where it was thrown, at the wall it stuck into, and
keeps that pose while it returns or drags.

The item model is authored upright, so the renderer reproduces the danmaku pipeline's
`translate(-0.5) · rotateX(90°)` (which centers the model in its own space and maps the shaft onto
the flight axis) and cancels the `translate(-0.5)` that `ItemRenderer.renderStatic` adds on its own —
hence the two translations in `layFlat`. Using `ItemDisplayContext.GROUND` without that step leaves
the shaft standing perpendicular to the throw direction.

The outer rotations are the projectile convention, not the danmaku one: a `Projectile` stores yaw as
`atan2(dx, dz)` and pitch as positive-up, whereas a `SimplifiedProjectile` stores the inverse look
angles. So the hook passes `+getYRot()` and `-getXRot()` where the danmaku renderer passes
`-getYRot()` and `+getXRot()`; the same mistake in the flight steering is what made the hook ignore
the player's aim (§5.2).

The twin is still an item, not a renderer flag, because the danmaku renderer resolves one baked model
per *item* from `new ItemStack(item)` with no entity or level, so a per-stack predicate cannot see
wetness. The hook does not share that limit, but reads the same twin so both entities show one art
path. `getItem()` runs on the client too: the owner id arrives in the spawn data, so the client
answers it without a packet, and a cosmetic disagreement with the server (rain at the edge of a
chunk) is harmless.

### 5.5 The chain

The hook draws a chain from the thrower to the anchor in every phase, so a stuck anchor visibly ties
the player to the wall and a dragged mob visibly comes in on a line. It uses the vanilla iron chain
texture directly rather than the block atlas:

* `RenderType.entityCutoutNoCull(ResourceLocation.withDefaultNamespace("textures/block/chain.png"))`
  — standalone texture, so no atlas sprite UVs, and no culling so the ribbon's back face is drawn;
* two ribbons crossed at 90° around the line, `3/16` block wide each, mapped to the texture's two
  3-pixel columns (`u` `0..3/16` and `3/16..6/16`) exactly like the vanilla chain model;
* split into one-block segments so `v` stays `0..1` per segment and a link is always a quarter block
  long, without depending on texture wrapping;
* the start is the owner's chest (`y + bbHeight * 0.6`), the end is the anchor's interpolated
  position, both rebased into the renderer's entity-space `PoseStack`.

The chain is drawn before the model, from the owner's interpolated position, so it stretches and
shortens with the return instead of snapping. It is client-only: `getOwner()` is already restored
from the spawn packet, so no extra synced field is needed.

## 6. The three modes

### 6.1 Destroy

* **Block hit** — break 3×3×3 around the hit block, then return.
* **Entity hit** — hurt for `damage` with `damageSources().thrown(this, owner)` and a little
  knockback, then return.

The break rule is literally "what a diamond pickaxe would harvest":

```java
private static final ItemStack DIAMOND_PICKAXE = new ItemStack(Items.DIAMOND_PICKAXE);

Player player = owner instanceof Player p ? p : null;
boolean drop = player == null || !player.isCreative();
for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
    BlockState state = level.getBlockState(pos);
    if (state.isAir()) continue;
    if (state.getDestroySpeed(level, pos) < 0) continue;            // bedrock, barrier
    if (!DIAMOND_PICKAXE.isCorrectToolForDrops(state)) continue;    // diamond level, right tool
    if (player != null) {
        if (!level.mayInteract(player, pos)) continue;              // spawn protection
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled())
            continue;                                               // claims and protection mods
    }
    level.destroyBlock(pos, drop, owner);
}
```

Creative does not drop blocks, as with normal mining. `DIAMOND_PICKAXE` is a static so the 27
checks do not build a stack each time. The explicit `BreakEvent` post is the Twilight Forest cube's
approach: `mayInteract` covers spawn protection and the vanilla adventure check, but not every claim
mod, and the cube is the reference for "a thrown weapon that excavates a volume".

### 6.2 Voyage

* **Left click, `GRAB`** — on entity hit, the hook latches onto the target and enters `DRAG`: every
  tick it sets the target's velocity along `(owner - target).normalize()` at 1 b/t with
  `hurtMarked = true` and a cleared fall distance, so the target is dragged the whole way instead of
  coasting on one impulse. The hook rides on the target (chest height) so the chain stays attached.
  The drag ends when the target is within 2 blocks of the owner, after 40 ticks, or when the target
  dies; then the hook returns. No damage.

```java
private void tickDrag() {
    LivingEntity target = grabbedTarget();
    if (target == null || !target.isAlive() || getOwner() == null) { toReturn(); return; }
    setPos(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
    Vec3 diff = getOwner().position().subtract(target.position());
    if (diff.lengthSqr() < DRAG_DIST * DRAG_DIST || --grapple <= 0) { toReturn(); return; }
    target.setDeltaMovement(diff.normalize().scale(DRAG_SPEED));
    target.hurtMarked = true;
    target.fallDistance = 0;
}
```

* **Right click, `PULL`** — the anchor enters `GRAPPLE` the moment its flight ends: on the first
  block it hits, snapped flush to that face (§5.1), or, if it hits nothing at all, where the 16-block
  reach from the throw origin runs out — it hangs there in the air rather than expiring. Either way
  it drags the owner for up to 40 ticks or until they are within 2 blocks:

```java
Vec3 dir = anchor.position().subtract(player.position()).normalize();
player.setDeltaMovement(dir.scale(1.0));
player.hurtMarked = true;
player.fallDistance = 0;
```

  Sneaking cancels the pull, so a player is never dragged into something they cannot escape. When
  the pull ends, for whatever reason, the anchor `RETURN`s and is absorbed by the owner.

Entity hits on a `PULL` anchor are ignored (it keeps flying) — the mode is for terrain.

### 6.3 Storm

* **Left click, `STORM` (5 power)** — throw one hook (`AnchorHookEntity`). On block or entity hit,
  deal area damage around the impact and then return.
* **Right click, `STORM_UNBOUND` (10 power)** — throw five bullets (`AnchorBulletEntity`) in a narrow
  fan (±25° yaw around the aim), each with reach 48 and no return. They deal the same area damage and
  vanish on impact.

Area damage: every `LivingEntity` within 4 blocks of the impact that `IDanmakuEntity.canHurt(owner,
target)` allows takes 6 damage from `damageSources().thrown(this, owner)`. The entity that was hit
directly is not hit twice (it is inside the box once).

Wet danmaku (a storm hook, or any unbound bullet):

| | bullets | direction | speed | lifetime | mover |
|---|---|---|---|---|---|
| while flying, per tick | 4 | a rotating cross perpendicular to the flight | 0.7 | 40–50 | none (straight) |
| on impact | 100 | random sphere, biased forward | 0.7 | 40–50 | `RectMover(pos, v, (0, -0.05, 0))` |

The cross is `DanmakuHelper.getOrientation(getDeltaMovement())` rotated by
`90° * i + tickCount * 15°`, so a long throw draws a helix of danmaku behind it. The burst is
`DanmakuItems.Bullet.CIRCLE` in a storm colour (proposed `DyeColor.BLUE`), damage 1, `bypassWall =
false`, `bypassEntity = false`. `AnchorStorm` (§5.3) emits all of them as ordinary
`ItemBulletEntity` projectiles, so they graze, fade and clean up like every other danmaku.

Both entities read wetness per tick and at impact, so an anchor that leaves the rain stops shedding
and one that lands in water still bursts. While the anchor is wet it is drawn with the water model
(§5.4), which doubles as the player's cue that the storm is live.

**Budget.** A 48-block unbound anchor sheds up to ~130 trail bullets and a 100-bullet burst; five
of them at once is ~1,150 entities. The single storm hook adds ~160. Add a per-anchor trail cap
(`MAX_TRAIL = 160`) and a server config toggle (`anchorStormDanmaku`, default on) so a pack of
players cannot turn one click into a server stall. If the config is off, the storm still does its
damage.

## 7. Registration checklist

* `GLItems`: `MINAMITSU_ANCHOR` (`AnchorItem`, `stacksTo(1)`), the hidden `ANCHOR_WET` twin (§3),
  `ANCHOR_MODE` and `ANCHOR_LINK` (§3.3), the attributes and `TOOL` components, `AnchorModel::model`
  for the base and the per-mode models, `L2ISTagGen.SELECTABLE`, creative tab.
* `GLEntities`, two entries, because the split is the whole point of §5:
  * `ANCHOR_HOOK`, id `minamitsu_anchor` (the current id), `sized(0.5F, 0.5F)`,
    `clientTrackingRange(6)`, **`updateInterval(1)`**, `renderer(AnchorHookRenderer::new)`. The
    every-tick interval is what makes the client see the stop, the hang and the pull; a hook may not
    keep the volley's `1 << 16`.
  * `ANCHOR_BULLET`, id `minamitsu_anchor_unbound`, `sized(0.5F, 0.5F)`, `clientTrackingRange(4)`,
    `updateInterval(1 << 16)`, `renderer(NoFadeBulletRenderer::new)` — a fully predicted danmaku
    needs no position packets.
* new classes: `AnchorHookEntity` + `AnchorBulletEntity` + `AnchorStorm` in
  `content/entity/misc/anchor/`, `AnchorHookRenderer` in `content/entity/misc/render/`; delete
  `AnchorProjectile`.
* `GLLang`: `ItemLores.MINAMITSU_ANCHOR_LORE`, an `ItemAnchor` group (`MODE_DESTROY/VOYAGE/STORM`,
  `NEED_POWER`, `SELECTED`, cost descriptions), plus the wheel hint line the fan has.
* `GLPackets`: `AnchorSwingPacket`, handler version bump.
* `GensokyoLegacy.commonSetup`: `AnchorSelectionListener.register()`.
* `GLClient`: `ItemProperties.register(MINAMITSU_ANCHOR, "anchor_display", AnchorItem::displayPredicate)`
  — without it the mode icons never switch.
* `event/AnchorEventHandlers`: the events of §4.
* `SpiritualDanmakuItem` + the `SpiritualPower` bar helpers from §2.1.
* assets: the prepared models, textures and three icons from §3 (no new art needed, no model edit
  either — the hook renderer centers the model); the staged originals stay in `temp/` like the other
  prepared art there, nothing in the mod references them.
* zh_cn: `items.json`, `tooltip.json`, then rerun `organize.ResourceOrganizer`.
* `./gradlew runData`, commit `src/generated/resources/`.
* `package-info.java` in `content/item/anchor/`, `content/item/anchor/client/`,
  `content/item/anchor/network/`, `content/entity/misc/anchor/` and `content/entity/misc/render/`;
  no FQNs in the new sources.

Out of scope until asked: recipes/loot/offers, dispenser behaviour, TLM compat, enchantment
effects beyond what the tags grant.

## 8. Edge cases and open questions

**Decided** (change these only with a reason):

* the bounded anchor is a normal `ThrowableProjectile`; only the unbound volley is danmaku (§5);
* Destroy and Storm hooks return; unbound bullets vanish; a `GRAB` hook drags its target home and
  then returns;
* `GRAB` does not damage — it is a repositioning tool;
* Voyage keeps vanilla mining and attacking; only an empty left click throws;
* a `PULL` hook sticks even when it hits nothing, hanging at the reach limit until the pull ends;
* the 16-block reach is measured from the throw origin, not from the moving player, so walking
  forward cannot stretch a throw;
* entity hits on a `PULL` hook are passed through (`canHitEntity`), since the mode is for terrain;
* the pull belongs to the hook, not to the hand: switching items mid-pull does not end it, unlike
  the tidal claw, because the throw is the commitment;
* the pull sets velocity and clears fall distance but never `setNoGravity` on the player, so a hook
  lost to a chunk unload cannot leave anyone floating;
* the storm volley is five bullets in a ±25° yaw fan — "up to 48 blocks" is read as reach, not as a
  literal vertical launch (the other reading is a single upward spray, which is a one-line change);
* the held stack carries the out hook's UUID, hiding the item and refusing throws until the server
  sees the hook gone (§3.3); the client never looks the entity up;
* the anchor charges spiritual power in creative too, matching the fan.

**Edge cases:**

* **Owner dies / logs out mid-throw** — the hook discards in every phase (owner check); the unbound
  volley is cleared by `SpellContainer` as before.
* **Server restart mid-grapple** — `type` and `phase` are NBT and synced, so the anchor is still
  stuck and the pull resumes; the timeout prevents a permanent pull.
* **Owner changes dimension** — `getOwner()` returns null; the hook discards, the bullets are
  ordinary danmaku and expire on their own.
* **Hook removed by a chunk unload or a command mid-pull** — the pull was velocity per tick, so the
  player simply stops being dragged; nothing was stored on them.
* **Client disagreement about the stop point** — at most one tick; the server owns the hit and the
  position packet corrects it.
* **Power** — checked before spawning, so a refused storm costs nothing and adds no cooldown. The
  pool max of 100 is 20 left-click storms or 10 right-click ones per Jizo refill.
* **Creative** — the anchor still charges power (the fan does too); a debug command or a config
  exemption can be added later if testing demands it.
* **PvP / friendly mobs** — the storm and the thrown damage consult `IDanmakuEntity.canHurt`, and
  the 3×3×3 break consults `mayInteract` and posts `BlockEvent.BreakEvent`, so neither griefs what
  the player could not touch.
* **Unbound anchors in rain over a base** — the trail is straight and does not bypass walls, so it
  stops on terrain; the burst is what reaches through a doorway.

## 9. Implementation order

Steps 1–6 were done in one pass and the build and `runData` are green; step 7 is the playtest. Kept as
a record of why the split landed in that order:

1. Extract `AnchorStorm` from `AnchorProjectile` and point the current entity at it — no behaviour
   change, build green.
2. `AnchorHookEntity` with `DESTROY` only — `ThrowableProjectile`, synced type/phase, zero gravity,
   per-phase velocity, swept collision, `setPos(hit)` on the face; `AnchorHookRenderer`; register
   `ANCHOR_HOOK` with `updateInterval(1)`.
3. `GRAB`, `PULL` (including the hanging grapple) and `STORM` on the hook; the trail and burst via
   `AnchorStorm`.
4. `AnchorItem.throwAnchors` spawns the hook for the four bounded throws and keeps `AnchorProjectile`
   for `STORM_UNBOUND`.
5. Strip `AnchorProjectile` into `AnchorBulletEntity` (no phase, no type, reach 48, always
   `markErased` on impact), register `ANCHOR_BULLET`, repoint the volley.
6. Delete `AnchorThrow.returns()` and `AnchorProjectile`, `./gradlew build`, `./gradlew runData`.
7. Playtest: reach 16 against a wall and into open air, return into the owner's hand, 3×3×3 in stone
   and in obsidian, grapple across a ravine, storm in rain and in water.
8. The held-stack link (§3.3): merged `ItemInHandRendererMixin`, `AnchorItem.inventoryTick` clears it;
   the hook keeps no link code, so a stack moved mid-throw still frees itself.

## 10. Test plan

1. Destroy: throw at stone → 27 blocks break and drop; at obsidian → breaks (diamond level); at
   bedrock → nothing; the hook stops flush against the face, keeps the throw orientation, returns to
   the hand on a chain and cannot be picked up as an item.
2. Destroy vs. a mob: damage lands once, the hook returns, no second hit on the way home.
3. Voyage: an *empty* left click at a cow 10 blocks away drags it all the way to the player, one tick
   at a time (it does not coast), and the chain follows it; left-clicking a block or a mob with the
   anchor held mines / punches as usual and throws nothing.
4. Voyage right click at a wall → the hook sticks in the face and the player is pulled to the wall;
   right click at open sky → the hook hangs 16 blocks from the *launch point* and pulls the player
   anyway. The pull ends on arrival, on sneak, or after 40 ticks, and the hook then returns.
5. Voyage through a wall: the return path ignores terrain, and the grappled player stops at the
   wall instead of inside it; switching hotbar slots mid-pull does not cancel the pull. The chain
   runs from the player's chest to the anchor in every phase, and the anchor does not turn around
   while returning.
6. Storm left: throws exactly one hook (not five), costs 5 from a 100 pool, area damage hits every
   mob in 4 blocks once, and the hook returns.
7. Storm right: five bullets, each reaching 48 blocks, 10 power for the volley; a dry throw emits
   no danmaku and stays iron; the same throw in rain emits a trail, a 100-bullet burst, and shows
   the water model while wet.
8. Bound vs. unbound: the hook keeps full opacity at 40 blocks and cannot be grazed; the bullets
   still graze, fade and vanish like danmaku. The bullets are erased on impact; the hook returns
   first and is discarded at the hand.
9. Wheel: switching modes persists on the stack, and the held model/wheel icon follow.
10. Power: a 4-power anchor refuses a storm with the message and no cooldown; a Jizo refills it.
11. One anchor at a time: after a throw the item is gone from the hand (first and third person) and a
    second click does nothing; the item reappears the tick the hook returns or is discarded, also
    when it was moved to another hotbar slot or into a chest before the hook landed. A storm volley
    does not hide the item — no hook was thrown.

## 11. Reference notes

What the three implementations actually do, kept here so a later revision does not have to re-read
them:

| reference | what it does | taken or avoided |
|---|---|---|
| vanilla `FishingHook` | runs its own `ProjectileUtil.getHitResultOnMoveVector` every tick; `Projectile.getAddEntityPacket` carries the owner id and `recreateFromPacket` restores it | the swept test and the free owner sync |
| Cataclysm `Tidal_Hook_Entity` | an `AbstractArrow` with `noGravity`, base damage 0 and `getPickupItem() = ItemStack.EMPTY`; discards when the owner dies, leaves range, or switches away from the claws; pull = `maxSpeed / 6` velocity with a brake zone, zeroed horizontally when close; sets `noGravity` on the player and restores it in `kill()` | zero gravity, empty pickup, brake-zone pull; no player no-gravity |
| Twilight Forest `CubeOfAnnihilation` | `getDefaultGravity() = 0`; its own `tick()` aims at `owner.look * 16` (or the owner when returning), clamps speed to 0.5, annihilates an AABB; returns when the use stops; posts `BlockEvent.BreakEvent` for each block and honours adventure mode | per-tick self-set velocity, `BreakEvent` per block; no auto-aim |

Two vanilla behaviours shaped the plan:

* `ThrowableProjectile.tick()` runs the hit test **before** moving and reads the delta again after
  the callback, so zeroing it in `onHitBlock` stops the entity short of the face; the hook snaps to
  `hit.getLocation()` first, and the base's own `setPos` then agrees with it.
* `ClientboundAddEntityPacket` carries position and rotation but not motion, so the client's delta is
  zero at spawn. The hook sends its launch vector in `IEntityWithComplexSpawn` data rather than
  rebuilding it from the rotation — see §5.2 for why the rotation cannot be inverted naively.
