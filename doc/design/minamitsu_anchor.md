# Minamitsu's Anchor

Minamitsu's anchor is a single heavy weapon with three modes: a mining hammer that comes back, a
grappling hook that drags things, and a storm caller that turns air into danmaku.

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
`ThrowableProjectile`) for every throw that can come back, and an unbound **danmaku bullet** for
every storm throw. Only the storm throws are danmaku; the hook is an ordinary projectile, so it can
stop on a block face, hang, and drag its owner with vanilla collision and server-authoritative
position.

| mode | left click | right click |
|---|---|---|
| **Destroy** | *empty* left click throws; on an entity deal damage, then fly home. It no longer breaks blocks. A block click mines as usual; an entity click is a radius-3 swing instead of a vanilla attack | hold 1–2 seconds, then release: throw an anchor that breaks blocks and damages entities in a sphere of `charge seconds + 2` blocks (3–4) around the impact; then fly home |
| **Voyage** | *empty* left click only throws; the anchor pulls the hit entity toward the player. A block click mines as usual; an entity click is a radius-3 swing instead of a vanilla attack | throw; if it hits a block it sticks there, pulls the player in and holds them at the anchor until retrieved; if it hits nothing it pulls from the reach limit and then returns |
| **Storm** | *empty* left click throws one unbound anchor (5 spiritual power), reach 48, no return; on impact deal area damage | hold right click for 1 second, then release: throw 5 unbound anchors (10 spiritual power), reach 48, no return |

The anchor is also a pickaxe: diamond harvest level, and it swings for +9 attack damage at −3 attack
speed. Mining is ordinary; an entity click swings a radius-3 sphere for the wielder's attack damage
and spends the attack cooldown, and the charged blast is only ever the thrown anchor. While a
bounded hook is out the hand is empty, so both left clicks are dead until it returns (§4).

**Storm danmaku.** Every storm throw is an unbound anchor, always drawn with the water-anchor model
(§5.4). It sheds one gravitational danmaku with zero initial speed every tick, and bursts 300
gravitational ones when it lands (§6.3). No wetness check is involved.

## 2. Components

| Piece | File | Job |
|---|---|---|
| `AnchorItem` | `content/item/anchor/AnchorItem.java` | the item: mode dispatch, cost, cooldown, spawn |
| `SpiritualDanmakuItem` | `content/item/spiritual/SpiritualDanmakuItem.java` | `DanmakuItem` with the spiritual bar (§2.1) |
| `AnchorMode` | `content/item/anchor/AnchorMode.java` | `VOYAGE / DESTROY / STORM`, display name, wheel entry |
| `AnchorModel` | `content/item/anchor/AnchorModel.java` | the base model plus one GUI icon model per mode (§3) |
| `AnchorThrow` | `content/item/anchor/AnchorThrow.java` | what a single throw does: `DESTROY / DESTROY_CHARGED / GRAB / PULL / STORM / STORM_VOLLEY` |
| `AnchorHookEntity` | `content/entity/misc/anchor/AnchorHookEntity.java` | the bounded anchor: one synced throw type, one synced phase machine |
| `AnchorBulletEntity` | `content/entity/misc/anchor/AnchorBulletEntity.java` | the unbound storm anchor, a danmaku bullet |
| `AnchorStorm` | `content/entity/misc/anchor/AnchorStorm.java` | trail, burst and area damage, shared by the storm throws |
| `AnchorHookRenderer` | `content/entity/misc/render/AnchorHookRenderer.java` | draws the hook in its throw orientation and the chain back to the thrower |
| `NoFadeBulletRenderer` | `content/entity/misc/render/NoFadeBulletRenderer.java` | the unbound volley's renderer: no fading, on the flight line (existing) |
| `AnchorSelectionListener` | `content/item/anchor/AnchorSelectionListener.java` | wheel/list registration, like the fan's |
| `AnchorModeWheel` | `content/item/anchor/client/AnchorModeWheel.java` | the client wheel |
| `AnchorSwingPacket` | `content/item/anchor/network/AnchorSwingPacket.java` | left-click in the air reaches the server |
| `AnchorEventHandlers` | `event/AnchorEventHandlers.java` | left-click plumbing, per mode (§4) |

`AnchorItem` and the unbound bullet reuse the iron dagger's approach: the item is a `DanmakuItem`
and the bullet an `ItemBulletEntity`, which gives the storm throws the existing `ItemBulletRenderer`
drawing path (from the item's own model), the l2serial save/spawn code and the fast-projectile
collision. The bounded hook reuses none of that: it is a normal `ThrowableProjectile` with a real
renderer, because a throw that must stop on a block face, hang there and drag its owner needs the
server to own its position (§5). `NoFadeBulletRenderer` therefore stays only on the storm throws.

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
  mining speed both read it, and the charged blast reuses the same rule.
* No `MAX_DAMAGE`: the anchor never wears out, and the durability bar is free for the power pool.
* +9 / −3 are the item's own modifiers, so the tooltip reads as a normal weapon; a swung hit is a
  normal `player.attack` hit (no custom melee code).
* `DanmakuItems.Bullet.DAGGER` is passed as the danmaku type so `buildRenderer` picks the item-model
  renderer. **Do not** add `DanmakuItems.Bullet.DAGGER.tag` — the anchor is not ammo.
* Tags: `ItemTags.PICKAXES`, `ItemTags.MINING_ENCHANTABLE`, `ItemTags.MINING_LOOT_ENCHANTABLE`,
  `ItemTags.WEAPON_ENCHANTABLE`, `ItemTags.SHARP_WEAPON_ENCHANTABLE`; creative tab;
  `L2ISTagGen.SELECTABLE` (the wheel needs it, like the fan and the gloves).

The art is already prepared in `temp/` and only needs moving and wiring:

| from `temp/` | to `src/main/resources/assets/gensokyolegacy/` (generated where noted) |
|---|---|
| `anchor.json` | `models/custom/anchor.json` — the hand-authored geometry; `#all` and the particle point at the skin |
| (generated) | `custom/minamitsu_anchor.json` / `custom/minamitsu_anchor_wet.json` — `AnchorModel` wraps that geometry with `item/anchor/iron_anchor` / `item/anchor/water_anchor` |
| `iron_anchor.png` (64×64) | `textures/item/anchor/iron_anchor.png` |
| `water_anchor.png` (64×64) | `textures/item/anchor/water_anchor.png` |
| `anchor_voyage.png` (16×16) | `textures/item/anchor/anchor_voyage.png` — GUI icon, Voyage |
| `anchor_destroy.png` (16×16) | `textures/item/anchor/anchor_destroy.png` — GUI icon, Destroy |
| `anchor_storm.png` (16×16) | `textures/item/anchor/anchor_storm.png` — GUI icon, Storm |
| `anchor_icon_voyage.png` (16×16) | `textures/item/anchor/anchor_icon_voyage.png` — wheel icon, Voyage |
| `anchor_icon_destroy.png` (16×16) | `textures/item/anchor/anchor_icon_destroy.png` — wheel icon, Destroy |
| `anchor_icon_storm.png` (16×16) | `textures/item/anchor/anchor_icon_storm.png` — wheel icon, Storm |

The Blockbench model has hand display transforms and no GUI transform, so registration uses the
same split as the iron dagger / doll lance: the custom model is the base, and
`ItemDisplayContext.GUI` gets a flat `item/generated` model over one of the three 16×16 icons
through `SeparateTransformsModelBuilder`. The hook renderer draws with `ItemDisplayContext.GROUND`
and lays the model down itself (its shaft is authored along +y, the flight direction along +z), so
the model needs no ground transform. The hand-authored `custom/anchor.json` owns the elements; the
two per-texture wrappers `custom/minamitsu_anchor{,_wet}.json` (parent + `#all` skin) are generated
by `AnchorModel`, so only the Blockbench geometry stays a main resource.

`AnchorModel.model` generates one such model per mode — `minamitsu_anchor` itself (Voyage icon)
plus `minamitsu_anchor_voyage/_destroy/_storm` — and hangs them off the base model as item
overrides on `gensokyolegacy:anchor_display`, whose value is `AnchorItem.displayPredicate`:
`AnchorMode.ordinal() + 1`. Vanilla takes the first override whose predicate reaches its value, so
the generated overrides are emitted in mode order. That is how the held icon follows the mode
without a second item per mode.

The wheel icons are a second range on the same predicate: `AnchorItem.iconStack` sets the
`ICON` unit component, and `displayPredicate` then returns `modes.length + 1 + ordinal`,
which the flat `minamitsu_anchor_icon_<mode>` overrides answer with
`textures/item/anchor/anchor_icon_<mode>.png`. The range sits behind the held one and is emitted
after it, so a plain anchor still shows its GUI icon while a display stack shows the wheel icon.
This is the shared `ICON` gate and the dagger glove's two-range layout (§3.1).

The two models belong to two items. The visible `minamitsu_anchor` always uses the iron model; a
hidden twin `anchor_wet` exists only so the unbound storm anchor can be drawn with the water texture,
and is registered but kept out of the creative tab and tagged `WEBSITE_HIDDEN`. It is a plain
`DanmakuItem` — same bullet type, colour and size, no `AnchorItem` behaviour — so it can never be
thrown or given a mode. The twin is needed because the danmaku renderer resolves one baked model per
*item* from `new ItemStack(item)` with no entity or level (§5.4); the hook never uses it.

### 3.1 Mode storage and selection

`AnchorMode` lives in a data component, exactly like `YuyukoFanMode`:

```java
DC.enumVal("anchor_mode", EnumCodec.of(AnchorMode.class, AnchorMode.values()))
```

`AnchorSelectionListener` / `AnchorModeWheel` are near-copies of `YuyukoFanSelectionListener` /
`YuyukoFanModeWheel`, registered in `commonSetup` next to the other selectors. A wheel entry is
`AnchorMode.icon()`: the anchor stack itself with the mode component set, `ITEM_NAME` naming the
mode and the `ICON` marker, so the item override of §3 gives it the wheel icon; the selector
list uses the same stacks. The held stack is what `swap` writes the mode to. `SelectorSelectPacket`
already exists and needs no change.

### 3.2 Right click and left click

```java
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (isLinked(stack)) return retrieve(level, player, stack);
    if (GrazeHelper.forbidDanmaku(player)) return InteractionResultHolder.fail(stack);
    if (getMode(stack) != AnchorMode.VOYAGE) {     // Destroy and Storm charge
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
    if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
    return throwAnchors(sp, hand, AnchorThrow.PULL)
            ? InteractionResultHolder.sidedSuccess(stack, false)
            : InteractionResultHolder.fail(stack);
}

@Override
public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
    if (!(entity instanceof Player player)) return;
    if (level.isClientSide || !(player instanceof ServerPlayer sp)) return;
    int used = getUseDuration(stack, entity) - timeLeft;
    switch (getMode(stack)) {
        case STORM -> {
            if (used < 20) return;                            // 1 second
            throwAnchors(sp, player.getUsedItemHand(), AnchorThrow.STORM_VOLLEY);
        }
        case DESTROY -> {
            if (used < 20) return;                            // 1 second
            int held = Math.min(used, 40) / 20;               // 2 at the 2-second cap
            int radius = Mth.clamp(2 + held, 3, 4);
            float t = (float) (Mth.clamp(used, 20, 40) - 20) / 20;
            float scale = Mth.lerp(t, 1.5F, 2.5F);            // 1.5x at 1s, 2.5x at 2s
            throwAnchors(sp, player.getUsedItemHand(), AnchorThrow.DESTROY_CHARGED, radius, scale);
        }
        case VOYAGE -> { }
    }
}
```

Destroy and Storm are charge attacks on the right click: `use` only raises the anchor
(`UseAnim.SPEAR`, like the trident) and the release fires it. Destroy needs one second and scales
its blast from 3 blocks at 1 second to 4 at 2 seconds (`n = charge seconds + 2`, clamped to 3–4),
with the blast damage scaling linearly from 1.5× to 2.5× of the thrower's attack damage over the
same window. Storm wants one second before the five-anchor volley. A short click therefore throws
nothing and spends nothing. Voyage still throws on the right click, and the link branch handles a
hook that is out: retrieve within 3 blocks, pull again from farther, regardless of the selected
mode (§3.3, §6.2). Every hook copies the thrower's `ATTACK_DAMAGE` as its hit damage (`scale` is 1
for the other throws), matching the trident and the dagger.

While the anchor is raised the client narrows the FOV with the bow's curve through
`ComputeFovModifierEvent`: the squared charge over the 20-tick `CHARGE_TIME`, 15% at full draw, and
scaled by the player's FOV effect setting like the vanilla bow.

`throwAnchors(sp, hand, type)` is the single gate for both clicks:

1. refuse while the stack still carries a hook's UUID (§3.3);
2. refuse if the item is on cooldown (`player.getCooldowns().isOnCooldown(this)`);
3. for `STORM` / `STORM_VOLLEY`, refuse unless the stack has 5 / 10 power, with a chat message
   (`GLLang.ItemAnchor.NEED_POWER`), matching how `ScatterFanHandler` refuses an empty fan;
4. spawn the entity — one `AnchorHookEntity` for every non-storm throw, one `AnchorBulletEntity`
   for `STORM` and five for `STORM_VOLLEY` (§5) — spend the power, add the cooldown, swing the
   hand, play the sound; `SpellContainer.track` only the bullets, since a hook is not a
   `SimplifiedProjectile` and discards itself when its owner is gone;
5. return `sidedSuccess`.

The four-argument overload also carries the charge radius of a `DESTROY_CHARGED` throw; it is written
onto the hook before the first move and saved to NBT, so a chunk reload in mid-flight keeps the
blast size.

### 3.3 One bounded anchor at a time

`GLItems.ANCHOR_LINK` (`DC.uuid("anchor_link")`) holds the UUID of the bounded hook the stack threw,
written only after `addFreshEntity` succeeds and only for hooks (`STORM` / `STORM_VOLLEY` bullets leave it
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

Left clicks never reach `use`, so the modes that need one are plumbed through events. The rule is
the same in every mode — an empty click throws and an entity click swings — so there is no
per-mode special case. A stack that still carries a hook's UUID (§3.3) is out of the hand, so both
clicks are dead until the hook returns:

| event | all modes | anchor out |
|---|---|---|
| `PlayerInteractEvent.LeftClickEmpty` | throw the mode's throw | nothing |
| `PlayerInteractEvent.LeftClickBlock` | vanilla mine | vanilla mine |
| `AttackEntityEvent` | cancel, radius-3 swing | cancel, nothing |

Empty clicks never reach the server on their own, so `LeftClickEmpty` (client only) sends
`AnchorSwingPacket`, whose handler repeats the mode/cooldown/power checks before throwing — a client
is only a hint, as with `DollGloveSwingPacket`. The entity swing cancels the vanilla attack on both
sides but only damages on the server: everything alive within 3 blocks of the clicked target takes
the wielder's `ATTACK_DAMAGE` scaled by the attack cooldown, the cooldown is spent, and the hit is
gated by `IDanmakuEntity.canHurt` like the storm blast. The cancel stays while the anchor is out, so
the hidden weapon cannot be used as a plain melee hit either. Block clicks stay vanilla mining in
every mode: losing mining while the anchor is held would be a nuisance.

`AnchorSwingPacket` is registered in `GLPackets` (one `.toServer(...)` line) and the handler version
goes `3` → `4`.

## 5. The two entities

| throw | entity | base | returns |
|---|---|---|---|
| `DESTROY`, `DESTROY_CHARGED`, `GRAB`, `PULL` | `AnchorHookEntity` | `ThrowableProjectile` | yes |
| `STORM`, `STORM_VOLLEY` | `AnchorBulletEntity` | `ItemBulletEntity` | no |

`AnchorThrow` stays the item-level enum (`reach()`, `cost()`, `isStorm()`); `AnchorItem.throwAnchors`
reads it and spawns one hook, one bullet, or five bullets. `returns()` goes away — the hook only
ever sees the four throws that return, and the bullet is always unbound.

**Why the split.** A danmaku is a `SimplifiedEntity`: self-predicted movement, no fluid state, and a
batched renderer, which is exactly what the storm throws need (five anchors, each shedding a trail,
must not cost five draw calls). A bounded anchor needs the opposite: server-authoritative position,
so it stops flush on a block face, hangs where it stopped, and drags its owner with a real velocity
instead of a guess. Sharing one base means the hook inherits a no-op fluid state and unusable
prediction, or the bullet pays for a phase machine it never enters.

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
| `type` | `AnchorThrow` | synced byte | which bounded throw this hook is |
| `phase` | `Phase` | synced byte | `FLY`, `RETURN`, `GRAPPLE`, `DRAG`, `HANG`; the client steers by it too |
| `damage` | `float` | server, NBT | the thrower's attack damage at throw time |
| `radius` | `int` | server, NBT | blast radius of a `DESTROY_CHARGED` throw, from the charge |
| `origin` | `Vec3` | server, NBT | eye position at throw; the reach is measured from here |
| `flyDir` | `Vec3` | NBT, spawn data | normalized throw vector; drives FLY and the client's prediction |
| `grapple` | `int` | server, NBT | ticks left on the pull or the drag |
| `returnTicks` | `int` | server, NBT | return timer |
| `anchored` | `boolean` | server, NBT | the pull hit a block, so it ends in `HANG` rather than `RETURN` |
| `grabbed` | `UUID` | server, NBT | the entity a `GRAB` hook is dragging |

| constant | value |
|---|---|
| fly speed | 1.5 b/t, **no gravity** |
| reach | 16 blocks, measured from `origin` (not from the moving owner) |
| return speed | 1.5 b/t |
| return timeout | 60 ticks |
| grapple timeout | 40 ticks |
| hang hold speed | 0.12 b/t per block of offset (a spring) |
| hang hold range | 4 blocks |
| re-grapple distance | 3 blocks: a right click farther than this pulls instead of retrieving |
| owner leash | 24 blocks |
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
    smash(hit.getBlockPos());
    switch (throwType()) { ... }
}
```

Both hit callbacks give impact feedback with the mace smash: `SoundEvents.MACE_SMASH_GROUND` and
`LevelEvent.PARTICLES_SMASH_ATTACK` at power 750. The particle event samples the block at the
position, so a block hit passes `hit.getBlockPos()` and an entity hit passes the first non-air block
under the target; when there is none (an airborne target) only the sound plays. The cancelled melee
uses the same feedback at the clicked target, so a swing into a crowd lands with the same smash
(`AnchorStorm.smashAt`).

Both callbacks return early on the client. The client runs the same swept test, but the server owns
the outcome; position updates every tick (§7) are what the client renders.

| phase | per tick | leaves when |
|---|---|---|
| `FLY` | reach check | hit, or `position().distanceTo(origin) > reach()` |
| `RETURN` | homing to the owner's eye | within 1.5 blocks, 60 ticks, owner gone → discard |
| `GRAPPLE` | pull the owner (§6.2) | arrival (<2 blocks), 40 ticks, sneak, owner gone → `HANG` if it hit a block, else `RETURN` |
| `DRAG` | pull the grabbed entity toward the owner (§6.2) | arrival (<2 blocks), 40 ticks, target gone → `RETURN` |
| `HANG` | holds the owner at the anchor with a spring toward it | right click within 3 blocks → `RETURN`; farther → `GRAPPLE` again; sneak or 4 blocks of distance releases the hold |

`tickCount > 200` is the backstop: a hook that neither hits, expires nor returns is discarded rather
than left to a chunk unload. `HANG` is exempt — a stuck anchor waits for the player, not for a timer
— and still discards on the owner's death. The hold never touches the player's gravity flag: a hook
lost to a chunk unload can only drop them.

| hit | `DESTROY` | `DESTROY_CHARGED` | `GRAB` | `PULL` |
|---|---|---|---|---|
| block | `RETURN` (no block break) | break + hurt in `radius`, `RETURN` | `RETURN` | `GRAPPLE`, then `HANG` |
| entity | hurt, `RETURN` | break + hurt in `radius`, `RETURN` | `DRAG`, then `RETURN` | flies through |

A `PULL` throw that hits nothing is unchanged: it grapples from the reach limit and then returns.

**Owner hygiene.** Every phase discards when the owner is gone, removed or dying, so the hook can
never outlive its thrower. `ownerLost` adds the leash rules: more than 24 blocks away, a different
dimension, or a held `AnchorItem` whose link is missing or points elsewhere. The last one means a
second anchor in the hand frees the first hook, while a non-anchor slot does not; a linkless anchor
stack left in the inventory is fine. `canUsePortal` is false and `changeDimension` returns null, so
the hook itself never follows a portal — the owner's departure is what discards it. It cannot be
`SpellContainer.track`ed (that takes a `SimplifiedProjectile`), and this check replaces the tracking
the danmaku path had. No item is dropped or picked up, and on a caught return nothing is given back:
the item never left the hand.

### 5.2 `AnchorBulletEntity`

The unbound storm anchor keeps the danmaku path unchanged: `@SerialClass`, `origin` + the trail
counter, `setup(0, 200, false, false, dir.scale(1.5))` from the item, the fast-projectile collision,
and `markErased(false)` on impact.

* `configure(origin)` — no throw type; every instance is an unbound storm anchor.
* reach 48 from `origin`; past it the bullet is erased.
* hit → `AnchorStorm.impact(this, owner)`, then erased. There is no return.
* trail → one gravitational danmaku per tick with zero initial speed, capped at 160.
* `getItem()` always answers with `ANCHOR_WET` (§5.4).
* rendered by the existing `NoFadeBulletRenderer`: full opacity at every range, model on the flight
  line.

### 5.3 `AnchorStorm`

One static helper holds everything the storm throws share, so the trail, the burst and area damage
exist once and the single throw and the volley emit identical danmaku:

| method | job |
|---|---|
| `shedTrail(Entity self, Entity owner, int spent)` | one gravitational danmaku per tick with zero initial speed; returns the new budget |
| `impact(Entity self, Entity owner)` | area damage, then the 300-danmaku spray |
| `areaDamage(Entity self, Entity owner, Vec3 at, double radius, float damage)` | hurts every living entity in the sphere, gated by `IDanmakuEntity.canHurt`; storm impacts pass 6 blocks / 12 damage, the charged destroy blast passes its charge radius and the throw's damage |
| `burst(Entity self, Entity owner)` | the 300-danmaku gravitational spray |

The owner is a parameter rather than `self.getOwner()`: `Entity` does not declare it, and the helper
also serves the danmaku bullet, whose owner only exists through `TraceableEntity` on the projectile
base classes. There is no wetness check: a storm anchor always sheds its trail and always bursts,
and is always drawn with the water model.

### 5.4 Drawing the hook and the storm anchor

The held anchor is always the iron model; every thrown storm anchor is the water one. Only the
bullet needs the twin item, because the danmaku renderer bakes one model per item:

```java
// bullet: always the water twin
public ItemStack getItem() {
    return GLItems.ANCHOR_WET.get().getDefaultInstance();
}
```

| what | how |
|---|---|
| hook | `AnchorHookRenderer`: the fixed yaw/pitch from launch, the model laid flat, then `ItemRenderer.renderStatic(..., ItemDisplayContext.GROUND)` |
| hook art | its `getItem()` returns the plain `MINAMITSU_ANCHOR`; a hook is never a storm throw |
| unbound bullet | unchanged `NoFadeBulletRenderer` / `ItemModelProjectileType` |
| bullet art | its `getItem()` always returns `ANCHOR_WET` |

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
per *item* from `new ItemStack(item)` with no entity or level. The hook does not share that limit,
but returns the plain anchor so the two entities keep one art path: the twin means "storm".

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
from the spawn packet, so no extra synced field is needed. The hook plays `SoundEvents.CHAIN_PLACE`
every 5 ticks while it flies out, returns or pulls its owner (`FLY`, `RETURN`, `GRAPPLE`), so the
chain is audible without turning the flight into a buzz; a hanging or dragging chain is silent.

## 6. The three modes

### 6.1 Destroy

* **Left click, `DESTROY` (empty click)** — a block hit does nothing but return; it no longer breaks
  blocks. Entity hit: hurt for `damage` with `damageSources().thrown(this, owner)` and a little
  knockback, then return.
* **Left click on an entity, any mode** — the vanilla attack is cancelled and `AnchorStorm.melee`
  hurts everything within 3 blocks of the clicked target instead; see §4. Not while the anchor is
  out: the link hides it and the click is swallowed.
* **Right click, `DESTROY_CHARGED`** — hold 1–2 seconds and release: the same hook, but its impact
  excavates every harvestable block in a sphere of `radius` blocks and hurts every living entity in
  the same sphere for `damage × scale` (`AnchorStorm.areaDamage`, §5.3), then returns. `radius` is
  `2 + charge seconds`, clamped to 3–4, so a 1-second charge is 3 blocks and a 2-second charge is 4.
  `scale` is linear over the same window, 1.5× at 1 second and 2.5× at 2 seconds. Both a block and
  an entity hit trigger the blast at the impact point; the directly hit entity is not hit twice.
  The base `damage` is already the thrower's attack damage, as for every other throw.

The break rule is literally "what a diamond pickaxe would harvest", widened by the one parameter:

```java
private static final ItemStack DIAMOND_PICKAXE = new ItemStack(Items.DIAMOND_PICKAXE);

Player player = owner instanceof Player p ? p : null;
boolean drop = player == null || !player.isCreative();
for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
    if (r > 1 && center.distSqr(pos) > r * r) continue;             // a sphere, not a cube
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

Creative does not drop blocks, as with normal mining. `DIAMOND_PICKAXE` is a static so the checks do
not build a stack each time. The explicit `BreakEvent` post is the Twilight Forest cube's approach:
`mayInteract` covers spawn protection and the vanilla adventure check, but not every claim mod, and
the cube is the reference for "a thrown weapon that excavates a volume". Only the charged blast
excavates: it uses a sphere so the hole does not grow cubic with the charge, and the left-click
`DESTROY` throw leaves terrain alone.

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
  reach from the throw origin runs out. Either way it drags the owner for up to 40 ticks or until
  they are within 2 blocks:

```java
Vec3 dir = anchor.position().subtract(player.position()).normalize();
player.setDeltaMovement(dir.scale(1.0));
player.hurtMarked = true;
player.fallDistance = 0;
```

  Sneaking cancels the pull, so a player is never dragged into something they cannot escape. Only a
  **block hit** is retained: when that pull ends the anchor stays where it stopped (`HANG`) with the
  link still on the stack, and `tickHang` keeps the player at it with a spring,
  `player.setDeltaMovement(diff.scale(0.12))` toward the anchor, which counters the drop without
  touching the player's gravity flag. The hold stops while they sneak, stand on the ground, fly, or
  move more than 4 blocks away. A right click within 3 blocks retrieves the anchor (`RETURN`); from
  farther away it pulls the player back in (`GRAPPLE` again) instead of detaching, and the mode on
  the stack does not matter (§3.2). The hook has no lifetime cap while hanging, so it cannot vanish
  from under the player; walking 24 blocks away, changing dimension, or holding another anchor
  discards it (`ownerLost`, §5.1).

  A throw that hits **nothing** keeps its old behaviour: it grapples from the reach limit and
  returns when the pull is over; only a block hit leaves the anchor behind.

Entity hits on a `PULL` anchor are ignored (it keeps flying) — the mode is for terrain. Clicking an
entity directly with the anchor held is the radius-3 swing of §4, not a `GRAB`: the throw is for an
empty click.

### 6.3 Storm

* **Left click, `STORM` (5 power)** — throw one unbound anchor (`AnchorBulletEntity`) with reach 48
  and no return. On block or entity hit, deal area damage around the impact and vanish.
* **Right click, `STORM_VOLLEY` (10 power)** — hold for one second (20 ticks) and release to throw
  five unbound anchors (`AnchorBulletEntity`) in a narrow fan (±25° yaw around the aim), each with
  reach 48 and no return. The release repeats `throwAnchors`, so power and cooldown are checked
  there; a click shorter than the charge raises the anchor and does nothing else. They deal the same
  area damage and vanish on impact.

Both clicks throw the same entity; only the count, the fan and the charge differ. The anchor always
uses the water model and always emits its danmaku — there is no wetness check anywhere.

Area damage: every `LivingEntity` within 6 blocks of the impact that `IDanmakuEntity.canHurt(owner,
target)` allows takes 12 damage from `damageSources().thrown(this, owner)`. The entity that was hit
directly is not hit twice (it is inside the box once).

Storm danmaku:

| | bullets | direction | speed | lifetime | mover |
|---|---|---|---|---|---|
| while flying, per tick | 1 | none | 0 | 40–50 | `RectMover(pos, 0, (0, -0.05, 0))` |
| on impact | 300 | random sphere, biased forward | 0.7 | 40–50 | `RectMover(pos, v, (0, -0.05, 0))` |

The trail danmaku starts at rest and falls, so a long throw leaves a curtain of droplets under its
path instead of a helix. Both the trail and the burst use `GLItems.DROPLET`, a hidden `DanmakuItem`
with `DanmakuItems.Bullet.BALL` and `DyeColor.BLUE` that draws its own 16×16 water texture instead
of the shared ball palette; damage 1, `bypassWall = false`, `bypassEntity = false`. `AnchorStorm`
(§5.3) emits all of them as ordinary `ItemBulletEntity` projectiles, so they graze, fade and clean up
like every other danmaku.

The droplet keeps the camera-facing billboard but leans along its flight. `DropletProjectileType`
takes the camera-to-droplet vector from the pose translation, removes the component of the motion
along it, and rotates the sprite in the screen plane so its tip points along what is left, i.e. the
motion as the player actually sees it. A droplet flying straight at or away from the camera has no
such component and stays upright; the window texture has its tip at the top, so "up" is "forward".

**Budget.** A 48-block unbound anchor sheds up to ~130 trail bullets and a 300-bullet burst; five
of them at once is ~2,150 entities. Add a per-anchor trail cap (`MAX_TRAIL = 160`) and a server
config toggle (`anchorStormDanmaku`, default on) so a pack of players cannot turn one click into a
server stall. If the config is off, the storm still does its damage.

## 7. Registration checklist

* `GLItems`: `MINAMITSU_ANCHOR` (`AnchorItem`, `stacksTo(1)`), the hidden `ANCHOR_WET` twin (§3),
  the hidden `DROPLET` storm danmaku (§6.3), `ANCHOR_MODE` (§3.1), `ICON` (§3), `ANCHOR_LINK` (§3.3),
  the attributes and `TOOL` components, `AnchorModel::model` for the base and the per-mode models
  (GUI and wheel), `L2ISTagGen.SELECTABLE`, creative tab.
* `GLEntities`, two entries, because the split is the whole point of §5:
  * `ANCHOR_HOOK`, id `minamitsu_anchor` (the current id), `sized(0.5F, 0.5F)`,
    `clientTrackingRange(6)`, **`updateInterval(1)`**, `renderer(AnchorHookRenderer::new)`. The
    every-tick interval is what makes the client see the stop, the hang and the pull; a hook may not
    keep the storm throw's `1 << 16`.
  * `ANCHOR_BULLET`, id `minamitsu_anchor_unbound`, `sized(0.5F, 0.5F)`, `clientTrackingRange(4)`,
    `updateInterval(1 << 16)`, `renderer(NoFadeBulletRenderer::new)` — a fully predicted danmaku
    needs no position packets.
* new classes: `AnchorHookEntity` + `AnchorBulletEntity` + `AnchorStorm` in
  `content/entity/misc/anchor/`, `AnchorHookRenderer` in `content/entity/misc/render/`; delete
  `AnchorProjectile`.
* `GLLang`: `ItemLores.MINAMITSU_ANCHOR_LORE`, an `ItemAnchor` group (`MODE_DESTROY/VOYAGE/STORM`,
  `NEED_POWER`, the cost/charge/retrieve lines), plus the wheel hint line the fan has. No "selected
  mode" tooltip line: the wheel names each mode, and the Patchouli entry explains them.
* `patchouli_books/tools_guide`: `entries/special_tools/minamitsu_anchor.json`, en_us and zh_cn.
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

* the bounded anchor is a normal `ThrowableProjectile`; every storm throw is the unbound danmaku
  bullet (§5);
* bounded throws return; unbound bullets vanish; a `GRAB` hook drags its target home and then
  returns;
* `GRAB` does not damage — it is a repositioning tool;
* every mode keeps vanilla mining on a block click, and an entity click is a radius-3 swing for the
  wielder's attack damage instead of a vanilla attack; only an empty left click throws, in every
  mode; while a hook is out both left clicks do nothing;
* a `PULL` hook sticks even when it hits nothing, hanging at the reach limit until the pull ends;
* only a `PULL` hook that hit a block stays in `HANG` after the pull; one that ran out of reach in
  open air returns as before;
* a retained hook holds the player at it with a spring, never by disabling the player's gravity;
* a retained hook has no timeout and is ended only by the linked stack's right click (or the
  owner's death);
* right-clicking a retained hook within 3 blocks retrieves it; from farther away, including after
  sneaking out of the hold, it pulls the player back in; the selected mode is ignored;
* a bounded hook never changes dimension; it is discarded instead when its owner does, when the
  owner is more than 24 blocks away, or when a *held* anchor's link is missing or points at another
  hook;
* the storm volley needs a one-second charge, so a right click alone never spends 10 power; the
  left-click storm throw is instant and costs 5;
* Destroy throws on an empty left click (no block break), and its right click is a 1–2 second charge
  for a 3–4 block blast (`radius = charge seconds + 2`, damage `1.5×` to `2.5×` of the thrower's
  attack damage); block clicks stay vanilla mining and entity clicks are the radius-3 swing;
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
  the charged blast consults `mayInteract` and posts `BlockEvent.BreakEvent`, so neither griefs what
  the player could not touch.
* **Storm anchors over a base** — the falling trail does not bypass walls, so it stops on terrain;
  the burst is what reaches through a doorway.

## 9. Implementation order

Steps 1–6 were done in one pass and the build and `runData` are green; step 7 is the playtest. Kept as
a record of why the split landed in that order:

1. Extract `AnchorStorm` from `AnchorProjectile` and point the current entity at it — no behaviour
   change, build green.
2. `AnchorHookEntity` with `DESTROY` only — `ThrowableProjectile`, synced type/phase, zero gravity,
   per-phase velocity, swept collision, `setPos(hit)` on the face; `AnchorHookRenderer`; register
   `ANCHOR_HOOK` with `updateInterval(1)`.
3. `GRAB` and `PULL` (including the hanging grapple) on the hook.
4. `AnchorItem.throwAnchors` spawns the hook for the four bounded throws.
5. Strip `AnchorProjectile` into `AnchorBulletEntity` (no phase, no type, reach 48, always
   `markErased` on impact), register `ANCHOR_BULLET`, repoint the storm throws.
6. Delete `AnchorThrow.returns()` and `AnchorProjectile`, `./gradlew build`, `./gradlew runData`.
7. Playtest: reach 16 against a wall and into open air, return into the owner's hand, the charged
   blast in stone and in obsidian, grapple across a ravine, storm in air and in water.
8. The held-stack link (§3.3): merged `ItemInHandRendererMixin`, `AnchorItem.inventoryTick` clears it;
   the hook keeps no link code, so a stack moved mid-throw still frees itself.

## 10. Test plan

1. Destroy: an *empty* left click throws; a block hit does not break anything and the hook simply
   returns; an entity hit deals damage; the hook stops flush against the face, keeps the throw
   orientation, returns to the hand on a chain and cannot be picked up as an item. Mining still works
   on a block click; a mob click deals the radius-3 swing, not a vanilla hit.
2. Destroy vs. a mob: damage lands once, the hook returns, no second hit on the way home. A charged
   right click released before one second throws nothing. Clicking a mob hurts every mob in 3 blocks
   once each, and the damage follows the attack cooldown rather than spam-clicking; clicking a mob
   while the hook is still out does nothing at all.
3. Destroy charged: a 1-second hold → a 3-block sphere of blocks breaks and mobs inside take damage
   once each for 1.5× the thrown hit; a 2-second hold → 4 blocks and 2.5×; a 3-second hold → still
   4 blocks and 2.5×. A 1.5-second hold lands between the two. The blast triggers on an entity hit
   too, without hitting the target twice, and the hook returns.
4. Voyage: an *empty* left click at a cow 10 blocks away drags it all the way to the player, one tick
   at a time (it does not coast), and the chain follows it; left-clicking a block with the anchor held
   mines as usual, and left-clicking a mob is the radius-3 swing and throws nothing.
5. Voyage right click at a wall → the hook sticks in the face, the player is pulled to the wall and
   held there instead of falling; after the pull the anchor stays stuck (no timeout), and a right
   click within 3 blocks sends it home and frees the hand. Sneak out of the hold, walk more than 3
   blocks away, then right click → the player is pulled in again (in any mode); within 3 blocks the
   same click retrieves. Right click at open sky → the hook hangs 16 blocks from the *launch point*,
   pulls the player, and then returns as it always did. The pull ends on arrival, on sneak, or after
   40 ticks.
6. Leash: leave a retained anchor and walk more than 24 blocks away → it discards and the hand comes
   back. Switch to a second anchor stack in the hotbar → the first hook discards; switch to a sword
   → it does not. Step into a portal and leave → the hook does not follow, it discards, and no hook
   is ever found in another dimension.
7. Voyage through a wall: the return path ignores terrain, and the grappled player stops at the
   wall instead of inside it; switching hotbar slots mid-pull does not cancel the pull. The chain
   runs from the player's chest to the anchor in every phase, and the anchor does not turn around
   while returning.
8. Storm left: throws exactly one unbound anchor (not five), costs 5 from a 100 pool, the anchor is
   always the water model, sheds one falling danmaku per tick, area damage hits every mob in 6 blocks
   once for 12, and the anchor vanishes on impact.
9. Storm right: a short click throws nothing and costs nothing; holding for one second and releasing
   throws five unbound anchors, each reaching 48 blocks, 10 power for the volley; every throw emits
   the falling trail and the 300-danmaku burst and shows the water model, wet or dry.
10. Bound vs. unbound: the hook keeps full opacity at 40 blocks and cannot be grazed; the bullets
   still graze, fade and vanish like danmaku. The bullets are erased on impact; the hook returns
   first and is discarded at the hand.
11. Wheel: switching modes persists on the stack, and the held model/wheel icon follow.
12. Power: a 4-power anchor refuses a storm with the message and no cooldown; a Jizo refills it.
13. One anchor at a time: after a bounded throw the item is gone from the hand (first and third
    person) and a second click does nothing; the item reappears the tick the hook returns or is
    discarded, also when it was moved to another hotbar slot or into a chest before the hook landed.
    A storm throw does not hide the item — no hook was thrown.

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
