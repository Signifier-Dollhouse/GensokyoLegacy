# Strays and Ghosts — wild mobs, support summons, and the bone flute

This doc plans the first concrete inhabitants of the `SUMMON` nature: a wild **stray doll**, a wild
**stray ghost**, and an owned **ghost** that a player can bind with a **bone flute**. It also settles
the inheritance question the brief asks: a wild mob is *not* a `BaseDollEntity`.

## 1. "Stray" already means something else

The doll framework calls a doll **stray** when it has been cut from its ledger and is hosted by a
`StrayHost` (`DollEntity.becomeStray()`, control.md §5.4). That stray is still a doll projection: it
has a detached `DollData`, keeps its gear, and drops its item form on death.

The wild entities here are different. They are ordinary mobs with **no ledger entry at all**. To keep
the two apart:

| term in this doc | meaning | ledger entry | `StrayHost` |
|---|---|---|---|
| **wild stray doll** | `stray_doll`, a neutral ambient mob | none | no |
| **stray ghost** | `stray_ghost`, an unowned wild mob | none | no |
| **owned ghost** | `ghost`, a player-bound support summon | one `SUMMONED` entry in the player's ledger | no |
| **framework stray** | a cut doll (control.md §5.4) | detached entry in a `StrayHost` | yes |

Whenever this doc says "stray" without qualification it means the wild mob, never the `StrayHost`
state. The entity type `stray_ghost` is deliberately the wild one; it is not `isStray()`.

## 2. The four things being added

| entity type | class | base class | category | item form | ledger | spawn egg |
|---|---|---|---|---|---|---|
| `stray_doll` | `StrayDollEntity` | `StrayFlyingMob` | `MONSTER` | none | no | yes |
| `stray_ghost` | `StrayGhostEntity` | `StrayFlyingMob` | `MONSTER` | none | no | yes (testing/creative) |
| `ghost` | `GhostEntity` | `BaseDollEntity` | `MISC` | none | player's `DollAttachment` | no |
| — | `BoneFluteItem` | `Item` | — | — | — | — |

`MISC` on the owned ghost is deliberate: it is never a natural spawn and must not consume the
monster cap. The two wild types take `MONSTER` as requested; the category is a spawn-budget and
presentation choice, not a base class (a `PathfinderMob` can carry it without becoming hostile).

## 3. Inheritance verdict — wild mobs are not dolls

**Do not** make `StrayDollEntity` or `StrayGhostEntity` extend `DollEntity`, `BaseDollEntity`, or the
owned `GhostEntity`. Both wild types extend `StrayFlyingMob`, a thin `PathfinderMob`; only the
owned `GhostEntity` extends `BaseDollEntity`. The split is forced by the base's hard behaviours, not
by taste:

| requirement | what `BaseDollEntity` actually does | consequence for a wild mob |
|---|---|---|
| live without an owner | `tick()` calls `getHost()` and `discard()`s when it is null (BaseDollEntity:437-441); `onJoinLevelCheck()` does the same | the wild mob vanishes on its first tick |
| persist in a chunk | `shouldBeSaved()` returns `strayHost != null && strayHost.data() != null` (BaseDollEntity:501-503) | a wild mob with no `StrayHost` is never written to disk |
| wander and retaliate | the base registers only `FollowDollOwnerGoal` and `LookAtDollOwnerGoal` | there is no wander, look-around, or `HurtByTargetGoal` |
| be ownerless | `OwnableEntity` plumbing, `isAlliedTo`, formation, `getHost()` host lookup | every query has to pretend an owner exists |
| be itemized | empty-hand `mobInteract` routes through the itemization gate | a wild mob would be poked or sucked into a ledger it does not belong to |
| take damage | paired entries defer into `DollData.combat`; unpaired entries fall back to plain damage | works, but the paired half is meaningless |

The tempting shortcut is a synthetic `StrayHost` per wild mob, which would satisfy `getHost()` and
`shouldBeSaved()`. Reject it:

- it turns a wild mob into a private one-entry **ledger projection**, contradicting the invariant that
  a doll with no ledger entry is never persisted (entity.md §5);
- `StrayHost.onDeath` and `detach` still encode doll semantics (gear, item drops) that a wild mob
  does not have;
- it does not solve the brief's requirement that `stray_ghost` and `ghost` be **two entity types** —
  a subclass of `GhostEntity` is still a `ghost` subclass, and adopting it would still need a type
  swap;
- the owned ghost has to discard itself when its owner leaves the level. A wild mob must not.

What the wild mobs actually share with the doll is **presentation and one goal set**, not pairing.
Share those by composition:

- a `GhostVariant` enum and a `GhostVariantHolder` interface, implemented by both ghost classes;
- a shared flying body (`StrayFlyingMob`: `FlyingPathNavigation`, `FlyingMoveControl`, no gravity and
  the plain fairy's one-block hover) and wander/look set (`HoverStrollGoal`, `LookAtPlayerGoal`,
  `RandomLookAroundGoal`); the doll adds `MeleeAttackGoal` + `HurtByTargetGoal`, the ghost adds
  nothing, so each registers its own `registerGoals()`;
- the same geo/animation assets, loaded by separate models. `DollRig` cannot be reused directly: its
  default cache casts `this` to `BaseDollEntity` (DollRig.java), so a plain mob implementing it would
  `ClassCastException`. Give the wild renderers their own `GeoEntity` cache and controllers.

The owned ghost is the opposite case: it *is* a ledger projection and wants exactly the base's
pairing, follow and never-save behaviour. It stops at `BaseDollEntity` because it takes no orders,
carries no loadout and has no attack — the same reason a gadget stops there (entity.md §9.1). The
`DollNature.SUMMON` choice is about item/recall/loadout, not about class, and not about ownership:
the ghost is player-owned despite being a summon.

## 4. Wild stray doll

`StrayDollEntity` is an ambient neutral mob. It is not convertible, not summonable, not an item, and
not associated with any player.

### 4.1 Behaviour

- **Wander** — `HoverStrollGoal(this, 1.0)` picks nearby air points. `StrayFlyingMob` supplies
  `FlyingPathNavigation` + `FlyingMoveControl`, `setNoGravity(true)` and a one-block floor clearance
  like the plain fairy (`PlainFairyEntity.hoverStep`). The vanilla `RandomStrollGoal` cannot be used
  here: its ground target is rejected by `FlyingPathNavigation.isStableDestination`, so the mob would
  stand still.
- **React, never initiate** — `targetSelector.addGoal(1, new HurtByTargetGoal(this))` plus
  `goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true))`. There is deliberately **no**
  `NearestAttackableTargetGoal`: the doll only fights what hit it first, which is what "neutral"
  means here.
- **Look alive** — `LookAtPlayerGoal(this, Player.class, 8.0F)` and `RandomLookAroundGoal(this)`.
- **Sound** — none (the dolls are silent); optional idle particles later.

Attributes: `Monster.createMonsterAttributes()` as a convenient builder, then `MAX_HEALTH 20`,
`ATTACK_DAMAGE 2`, `MOVEMENT_SPEED 0.25`, `FOLLOW_RANGE 16`, `FLYING_SPEED 0.3`. Low damage because
it is an ambient mob, not a threat.

### 4.2 Registration and persistence

- Entity type `stray_doll`, `MobCategory.MONSTER`, `.sized(0.4F, 0.9F).clientTrackingRange(10)`.
- `.spawnEgg(primary, secondary).build()` — the first spawn egg in the mod. Registrate's
  `EntityBuilder.spawnEgg` is deprecated upstream but is the builder route here; it creates a
  NeoForge `DeferredSpawnEggItem` and registers it. It returns an `ItemBuilder`, so `.build()` is
  required to get back to the entity builder and continue the chain. The egg lands in vanilla's
  spawn-egg tab automatically.
- `.loot(EntityLootGen::noLoot)` — no drop. Dropping a doll item would create an unintended
  acquisition path; if a drop is wanted later it should be its own decision.
- `.tag(GLTagGen.CARRYON_BLACKLIST)` — carriers must not pick it up.
- **No natural spawn.** No `spawnPlacement`, no biome modifier, no spawn-rule hook. The egg is the
  only source for now, exactly as the brief asks.
- Persistence: leave the vanilla mob default (an egg-spawned mob can despawn when the player leaves).
  It is decorative; a player who wants to keep one can name it. If it should never despawn, call
  `setPersistenceRequired()` in `finalizeSpawn` instead.

### 4.3 Rendering

Reuse `geo/doll.geo.json` and `animations/doll.animation.json` with a wild-specific texture
(`textures/geo/stray_doll.png`) and a `StrayDollRenderer`/`StrayDollModel`. The model plays
`toy_idle`/`toy_fly` from its own controller. It does not implement `DollRig` (§3).

## 5. Stray ghost

`StrayGhostEntity` is the wild, ownerless ghost. It is the only entity the bone flute can bind.

### 5.1 Behaviour

- **Wander/hover** — same `StrayFlyingMob` body and `HoverStrollGoal` as the wild doll, plus
  `LookAtPlayerGoal` and `RandomLookAroundGoal`.
- **Passive** — no target goals at all: no `HurtByTargetGoal`, no `MeleeAttackGoal`, no
  player-targeting goal. The wild ghost never fights back, which is what makes the flute a calm
  choice rather than a chase. The wild doll stays neutral and retaliates.
- **Persistent** — call `setPersistenceRequired()` whenever a stray is created (spawn egg, flute
  release, command). Strays are rare and must not despawn while the player fetches a flute.
- **No loot**, no ledger, no item form. Killing one loses its variant.
- Attributes: `MAX_HEALTH 20`, `MOVEMENT_SPEED 0.25`, `FLYING_SPEED 0.3`, `FOLLOW_RANGE 16`;
  `MobCategory.MONSTER`, `.sized(0.4F, 0.9F).clientTrackingRange(10)`.

### 5.2 Variant data and rendering

The variant is a synced `int` on the entity (for the client renderer) and an NBT field (for chunk
persistence):

```java
public enum GhostVariant {
    CURIOS(new CuriosGhostHandler()),
    FOODIE(new FoodieGhostHandler()),
    LIVELY(new LivelyGhostHandler()),
    DUAL(new DualGhostHandler());

    private static final GhostVariant[] VALUES = values();
    private static final GhostVariant[] WILD = Arrays.stream(VALUES)
            .filter(e -> e.handler.wild()).toArray(GhostVariant[]::new);

    public static GhostVariant byId(int id) {
        return VALUES[Mth.clamp(id, 0, VALUES.length - 1)];
    }

    public static GhostVariant[] wildValues() {
        return WILD.clone();
    }
}
```

Both ghost classes define their own `DATA_VARIANT` accessor and implement a tiny
`GhostVariantHolder` (`getVariant()` / `setVariant()`) so the renderers and the flute do not care
which class they hold. `StrayGhostEntity` writes/reads the id in `addAdditionalSaveData` /
`readAdditionalSaveData`; the owned ghost gets it from the ledger module (§6.2). `finalizeSpawn`
picks a random variant for `MobSpawnType.SPAWN_EGG`/`NATURAL`; a command/NBT load keeps what was
written. The owned ghost never rolls randomly.

The stray and the owned ghost share the geo/animation files (`geo/ghost.geo.json`,
`animations/ghost.animation.json`) but use separate models/renderers. Per-variant textures
(`curios_ghost.png`, `foodie_ghost.png`, `lively_ghost.png`, `dual_ghost.png`, under
`textures/geo/ghost/`) let the player see what they are catching; name tags show "Curios Ghost",
"Foodie Ghost", "Lively Ghost" or "Dual Ghost" via an overridden `getName()` that falls back to the
variant name only when no custom name is set. Dual never rolls wild: `GhostVariant.random` uses only
`wildValues()`, while the fan can summon it by name.

## 6. Owned ghost

### 6.1 The kind

```java
public class GhostEntity extends BaseDollEntity implements GhostVariantHolder {
    public static final ResourceLocation TYPE = GensokyoLegacy.loc("ghost");

    @Override
    public DollNature nature() {
        return DollNature.SUMMON;
    }

    @Override
    public ResourceLocation getDollTypeId() {
        return TYPE;
    }
}
```

Because it extends `BaseDollEntity` it gets pairing, the follow formation, the deferred damage
pipeline, the never-save guard and the `DollRig` cache for free. Because it is a `SUMMON`:

- there is no `DollItem` for it, so it cannot be summoned from an item;
- `itemizable()` is false, so empty-hand interaction does not convert it, `park(STORED)` refuses, and
  `tryItemize`/`tickSummoned` drop instead of minting;
- `recallable()` is false, so the glove's recall sweep skips it;
- `loadoutCapable()` is false, so neither loadout-editor open runs.

It also stays out of the command surface: `DollCommander.roster` stamps a formation slot over every
summoned entry but returns only `DollEntity` rows, and `DollGloveHandler`'s crosshair trace filters
`DollEntity`, so a ghost has no sidebar frame, cannot accept a ticket and cannot be ordered. It takes
no `DollCommandGoal` and no `DollActionHandler`. It still follows the owner, looks at them, and takes
damage through the base.

The ghost is `MISC` and has no spawn egg: a spawn egg would create an ownerless `ghost`, which the
base would discard on its first tick because `getHost()` is null. The only way into `ghost` is the
flute (§8).

### 6.2 Variant persistence

`DollData` gains one kind-specific scalar:

```java
/** Kind-specific variant id; ghosts use it (stray.md §6.2), other kinds ignore it. */
@SerialField(toClient = false)
public int variant;
```

A `GhostVariantModule implements DollModule` carries it across park/summon. `GhostEntity.createDollModules` calls `super` and appends it, next to the base's `DollRigModule`: 

```java
final class GhostVariantModule implements DollModule {
    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GhostEntity.DATA_VARIANT, GhostVariant.CURIOS.ordinal());
    }

    @Override
    public void writeValuesTo(SynchedEntityData sync, DollData data) {
        data.variant = sync.get(GhostEntity.DATA_VARIANT);
    }

    @Override
    public void readValuesFrom(SynchedEntityData sync, DollData data) {
        sync.set(GhostEntity.DATA_VARIANT, GhostVariant.byId(data.variant).ordinal());
    }
}
```

`DollData` is server-only (`toClient = false`), so the client reads the same value from the entity's
synced accessor. On logout/death `parkAll` calls `writeValuesTo` before discarding; on login
`DollSpawn.materialize` calls `readValuesFrom`, so the variant survives the round trip. Entries
predating the field read `0`, which is `CURIOS` — an acceptable default, like `color` reading red.

### 6.3 Animation

`GhostEntity` implements `DollRig` through the base and supplies its own clip set from
`animations/ghost.animation.json`: `idle` and `fly` loop, and `skill` is the one-shot (a pulse/emote;
nothing can order an attack). `registerControllers` uses two controllers, `ANIM_CONTROLLER` for the
loop and `ONE_SHOT_CONTROLLER` for the triggered `skill`; there is no wink controller because the
ghost has no expressive wink clip. In the geo the body bone is `Body`;
`DefaultedEntityGeoModel(GhostEntity.TYPE, "Body")` and the model overrides the
model/animation/texture paths as `DollModel` does.

## 7. Variants and their auras

Each variant's drain and aura live in its own `GhostVariantHandler` implementation, carried by the
`GhostVariant` constant; `GhostEntity` only calls `handler.tick` and `handler.aura`. The three
support variants apply **owner auras** in `tick()` only when:

- the ghost is server-side and alive;
- its `getOwner()` is a living entity in the same level;
- the owner is within `ABILITY_RANGE = 16` blocks (squared distance) and is not a spectator.

The support handlers refresh every 20 ticks with a 60-tick duration. The 3-second duration is a
grace tail: if the ghost dies, is parked, or leaves range, the buff fades within three
seconds instead of snapping off mid-tick. `ambient = true, visible = true, showIcon = true` keeps the
icon readable without a particle storm. Two ghosts of the same variant do not stack; they just
refresh the same instance. Dual has no aura and no upkeep, so it is absent from the subsections
below; it is discussed in [ghost_power.md](ghost_power.md) §5.

### 7.1 Curios ghost

Every 60 ticks (3 s), remove **one** harmful effect from the owner — the one with the most time
remaining:

```java
MobEffectInstance target = null;
for (MobEffectInstance effect : owner.getActiveEffects()) {
    if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) continue;
    if (target == null || remaining(effect) > remaining(target)) target = effect;
}
if (target != null) owner.removeEffect(target.getEffect());
```

`remaining()` treats an infinite duration (`-1`) as `Integer.MAX_VALUE`, so a permanent debuff is
cleansed first rather than last. Removing after the loop avoids
`ConcurrentModificationException`. One effect per pulse means a long debuff is worked down before a
short one that would have expired on its own; there is no need to snapshot the collection.

On a successful cleanse, the ghost broadcasts `EVENT_SKILL`, which plays its `skill` clip on nearby
clients. No extra particle burst is spawned.

The name is a **variant name**, not the Curios mod. The ability is an aura, not a Curios slot; no
`compat/curios` code is involved. If a Curios-integrated trinket was intended, that is a different
feature and should be designed separately.

### 7.2 Foodie ghost

Refresh `GLEffects.FOODIE` on the owner: `new MobEffectInstance(FOODIE, 60, 0, true, true, true)`
every second. The effect itself does the work (§7.4):

- once per second, above 16 hunger, converts 1 hunger into 1 saturation;
- eating is twice as fast.

### 7.3 Lively ghost

Refresh three effects on the owner every second, all with `AURA_DURATION`:

| effect | amplifier | display |
|---|---|---|
| `MobEffects.DIG_SPEED` | 1 | Haste II |
| `MobEffects.MOVEMENT_SPEED` | 1 | Speed II |
| `GLEffects.LIVELY` | 0 | Lively I (20% faster item cooldowns) |

`LIVELY` is already implemented through `LivelySource`/`CooldownClock` (cooldown.md), so this is a
real benefit, not a decorative icon. A stronger external potion effect is not downgraded: vanilla's
`addEffect` keeps the stronger amplifier/duration.

### 7.4 The `foodie` effect

Register in `GLEffects` as a real effect subclass, not an `EmptyEffect`:

```java
public static final LegacyHolder<MobEffect> FOODIE = genEffect("foodie",
        () -> new FoodieEffect(MobEffectCategory.BENEFICIAL, 0xFFD27F),
        "Above 16 hunger, converts 1 hunger into 1 saturation per second. Eat twice as fast.");
```

`FoodieEffect.applyEffectTick` runs every tick but only acts once per second
(`player.tickCount % 20 == 0`) and only server-side:

```java
FoodData food = player.getFoodData();
int level = food.getFoodLevel();
float saturation = food.getSaturationLevel();
if (level <= 16 || saturation >= level) return true;
int nextLevel = level - 1;
float nextSaturation = Math.min(saturation + 1, nextLevel);
if (nextSaturation <= saturation) return true;
food.setFoodLevel(nextLevel);
food.setSaturation(nextSaturation);
```

Rules:

- **once per second** — the effect tick is the clock; `shouldApplyEffectTickThisTick` returns true so
  `applyEffectTick` is called, and the modulo keeps the conversion at 1 Hz;
- **server only** — the client's `FoodData` is not touched; the server syncs the result;
- **above 16** — the 16th hunger point is never consumed, so the player keeps a normal food buffer;
- **no wasted point** — when saturation cannot rise (it is at or one below the new hunger cap), the
  conversion is skipped rather than spending hunger for nothing;
- **cap** — saturation never exceeds hunger, matching vanilla's `FoodData` cap;
- the effect does not replace vanilla eating: normal food still restores hunger and saturation, the
  aura just slowly moves hunger into the saturation buffer between meals.

**Eating speed — `LivingEntityUseItemEvent.Tick`** (in `event/MobEffectEventHandlers.java`):

```java
if (!(event.getEntity() instanceof Player player)) return;
if (!player.hasEffect(GLEffects.FOODIE)) return;
if (!event.getItem().has(DataComponents.FOOD)) return;
int left = event.getDuration();
if (left > 1) event.setDuration(left - 1);
```

`LivingEntity.updateUsingItem` assigns the event's duration back to `useItemRemaining` and then
decrements it once (LivingEntity:3151-3161), so subtracting one extra tick per tick halves the use
duration — the requested 2×. The `left > 1` guard prevents an instant finish or a negative duration.
The event fires on both sides, so the eat animation and the server completion stay in sync.

No `PlayerMixin` hook is needed: the conversion is driven by the effect's own tick, not by intercepting
`FoodData.eat`. Milk, potions and other non-food uses never get the effect's conversion anyway.

## 8. The bone flute

`BoneFluteItem` is the only bridge between the wild and owned ghost. It is not a doll item: it has no
`DOLL_DATA`, no loadout, and it summons nothing on its own.

### 8.1 Adopting a stray

`interactLivingEntity` on a `StrayGhostEntity`, server-side:

1. `DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp)`.
2. If `att.summonCapped()` → message `Doll.TOO_MANY`, return `FAIL` (the stray stays).
3. Build `DollData`:
   ```java
   DollData data = new DollData();
   data.type = GhostEntity.TYPE;
   data.state = DollState.SUMMONED;
   data.dimension = stray.level().dimension().location();
   data.position = stray.position();
   data.yRot = stray.getYRot();
   data.combat = new CombatData(stray.getHealth(), 0);
   data.variant = stray.getVariant().ordinal();
   data.customName = stray.getCustomName();
   ```
4. `att.adopt(sp, stray)` — a new `DollAttachment` method that builds the entry above, calls the
   existing private `doSummon` (materialize → `dolls.put` → `addFreshEntity`) and on success discards
   the stray. The ledger entry must exist **before** `addFreshEntity`, or the join-level inverse
   check sees an orphan (pairing.md §3.1).
5. Play the flute, set a 40-tick cooldown, return `InteractionResult.SUCCESS` on the server. The
   client returns `SUCCESS` as soon as it sees a ghost target, before any ledger access. Do not
   consume the flute.

Health and variant carry over. Position is the stray's position; `DollSpawn.materialize` places the
ghost there. The stray is discarded only after the owned ghost is added, so a failed materialization
leaves the target in the world.

### 8.2 Releasing an owned ghost

`interactLivingEntity` on a `GhostEntity`:

1. Require `ghost.isOwner(player)`; otherwise message `Ghost.NOT_YOURS` and return `FAIL`.
2. Look up the `SUMMONED` entry by `ghost.getUUID()`; it must exist and `data.type` must be
   `GhostEntity.TYPE`.
3. Create a `StrayGhostEntity` at the ghost's position/facing, copy `getVariant()`,
   `data.getHealth()` (the ledger value is authoritative), `getCustomName()`, and
   `setPersistenceRequired()`.
4. Add the stray first (`level.addFreshEntity(stray)`); if that returns false, discard it and keep the
   owned ghost. The stray is not a `BaseDollEntity`, so the ledger's join-level inverse check does not
   apply to it, and no tick runs between these statements.
5. Discard the owned ghost, then `dolls.remove(uuid)` — the same "entity first, entry second" order
   as every other end (summon.md §3).
6. Play the flute, cooldown, return `InteractionResult.SUCCESS`.

Creating the stray before touching the ledger means a failed spawn cannot lose the ghost. Once the
stray is in the world, the ghost is discarded and its entry dropped in the same server call, so no
tick ever sees the two coexist.

The point of this path is that **releasing is not a framework stray**: it does not call
`becomeStray`, does not create a `StrayHost`, does not leave a detached entry, and does not drop an
item on death. That is what makes `stray_ghost` a genuinely wild mob rather than a framework stray.

If the entry is missing or the type mismatches, refuse rather than spawning a duplicate — the ghost
is already in an inconsistent state and the reconciliation pass will handle it.

### 8.3 The item

- `stacksTo(1)`, no durability, not consumed.
- 40-tick cooldown per player (`player.getCooldowns().addCooldown(this, 40)`) so the two-way
  conversion cannot be spammed.
- Sound: vanilla `SoundEvents.NOTE_BLOCK_FLUTE` (pitch ~0.8 on release, ~1.2 on adopt) or a custom
  `GLSounds` entry later.
- Tooltip (two lines): "Right-click a stray ghost to bind it to you." / "Right-click your own ghost to
  set it free."
- Acquisition: creative tab only for now. No recipe is generated yet; the recipe is a deferred
  follow-up, and the item must not depend on it to exist.
- Texture: `textures/item/tool/bone_flute.png`.
- No `interactLivingEntity` on any other entity: return `PASS`, so a normal doll, the wild doll, or a
  mob behaves exactly as it would with an empty hand.

## 9. Ledger and lifecycle

- **Adopt** writes a normal `SUMMONED` entry. From that moment the ghost is a full ledger projection:
  it follows in formation, is restored after logout/death as `TEMP`, keeps its variant via
  `GhostVariantModule`, and counts against `MAX_SUMMONED` like any doll.
- **Logout/death** — `parkAll` writes values, discards the entity and sets `TEMP`. `restore`/
  `trySummon` re-materializes it. `SUMMON` means `park(STORED)` and `tryItemize` refuse; the ghost
  never becomes an item.
- **Death** — `tickSummoned` sees health ≤ 0; `itemizable()` is false, so the entry is dropped and
  the corpse discarded. Nothing is returned. There is no repair loop.
- **Release** — §8.2: spawn the wild type first, then discard the ghost and drop the entry. Not a
  framework stray.
- **Dimension change** — the base is level-bound and self-discards when its owner leaves the level;
  the entry goes `TEMP` and is re-summoned next to the owner in the new dimension. The wild types
  are ordinary mobs and use vanilla portal behaviour (they stay behind unless pushed through).
- **Roster** — a ghost takes a formation slot (so other dolls leave room for it) but has no sidebar
  row and cannot be commanded. A roster of only ghosts makes the glove's recall half a no-op, which
  is correct.
- **Inventory** — adoption needs no inventory slot; only `MAX_SUMMONED` matters.

## 10. Registration, assets, datagen

### 10.1 `GLEntities`

| constant | name | class | category | extras |
|---|---|---|---|---|
| `STRAY_DOLL` | `stray_doll` | `StrayDollEntity` | `MONSTER` | attributes, spawn egg, no loot, carry-on tag, renderer |
| `STRAY_GHOST` | `stray_ghost` | `StrayGhostEntity` | `MONSTER` | attributes, spawn egg, no loot, carry-on tag, renderer |
| `GHOST` | `ghost` | `GhostEntity` | `MISC` | `BaseDollEntity` attributes, no spawn egg, no loot table (MISC entities cannot have one), carry-on tag, renderer |

`GhostEntity.TYPE`/`StrayGhostEntity.TYPE` are `GensokyoLegacy.loc(...)` constants used by the flute
and `DollData`. `GhostEntity.TYPE` is the one `DollData.type` stores.

The stray-ghost egg is not in the brief (which asks for one on the wild doll), but it is added for
two reasons: the flute has no other target in creative, and no natural spawn exists yet (§12).

### 10.2 `GLItems`, `GLEffects`, `GLLang`

- `GLItems.BONE_FLUTE` — `BoneFluteItem`, generated item model, lang "Bone Flute", creative tab.
- `GLEffects.FOODIE` — `FoodieEffect`, beneficial, warm colour, description (§7.4).
- `GLLang`:
  - `ItemBoneFlute` — `BIND` / `RELEASE` tooltips.
  - `Ghost` — `CURIOS` / `FOODIE` / `LIVELY` / `DUAL` display names plus `NOT_YOURS`,
    `BIND_FAIL`, `RELEASE_FAIL`, the fan messages and `DUAL_WARD`.
  - `Spiritual` — the shared `BAR` tooltip for spiritual items.
  - Reuse `Doll.TOO_MANY` for the cap message.
  - Entity names are generated by Registrate from the registry names.
- zh_cn: add the item/entity/effect/tooltip/message keys to the split files, then rerun
  `organize.ResourceOrganizer`.

### 10.3 Assets

- `geo/ghost.geo.json`, `animations/ghost.animation.json` and
  `textures/geo/ghost/{curios,foodie,lively,dual}_ghost.png` (copied from `temp/`; the staged
  originals are left untouched).
- The wild doll reuses `geo/doll.geo.json` and `animations/doll.animation.json` with a fixed
  `textures/geo/doll/red.png`; it does not get its own art.
- `textures/item/tool/bone_flute.png`.
- Spawn egg models are generated (`template_spawn_egg`).

### 10.4 Datagen and verification

- `./gradlew runData` for item models, loot tables (`noLoot`), lang and tags; commit the generated
  output.
- `./gradlew compileJava`.
- Manual checks: egg spawns both wild types; both wander and hover about one block over the floor;
  wild doll retaliates only after being hit; wild ghost never retaliates; flute adopts/releases;
  variant survives logout/death; a released ghost is
  persistent; curios removes one harmful effect per 3-second pulse, longest first; foodie doubles eat
  speed and converts hunger to saturation above 16; lively gives Haste II/Speed II/Lively I; the cap
  refuses a ninth summon; a ghost is absent from the roster and the loadout editor.

## 11. Edge cases

- **Two entity types, one variant enum** — the flute copies the variant across the type swap; do not
  try to keep the same entity and change its type.
- **A stray with a full ledger** — refuse before discarding the stray; show `TOO_MANY`.
- **A ghost owned by another player** — refuse; `isOwner` is the check, creative mode does not bypass
  it (releasing someone else's ghost is destructive).
- **A ghost that lost its entry** — refuse, do not spawn a stray; the reconciliation pass owns that
  repair.
- **A released ghost at low health** — copy the health; releasing is not a heal. A ghost at 0 is
  already dead and cannot be interacted with.
- **Flute on the wild doll or a normal doll** — `PASS`; no conversion.
- **`isStray()` collision** — `stray_ghost` is never a framework stray. Do not call
  `becomeStray()`/`detach` in the release path.
- **No item form** — every itemization gate must stay on `nature().itemizable()`; the ghost must not
  acquire a `DollItem` by accident. `DollItem.makeItem` would fall back to the plain doll if an entry
  ever named no kind, so no ghost entry may reach it.
- **Effect refresh while parked** — no entity, no aura; the 3-second tail is the only grace.
- **Multiple ghosts** — same variant refreshes, different variants stack by kind; the cap still
  limits the total.
- **Natural spawn** — explicitly out of scope for both wild types. The consequence is that stray
  ghosts currently come only from the spawn egg or from releasing an owned ghost; the flute has no
  survival source until a natural-spawn pass adds one.

## 12. Open questions

1. **First ghost acquisition** — with no natural spawn, the bone flute is circular in survival.
   Decide whether stray ghosts get a natural-spawn pass (and where) later; the spawn egg and the
   flute's release path are the current sources.
2. **Flute recipe** — no recipe is generated yet. Decide between bone + bamboo, bone + stick, or a
   ritual/crafting-gated source.
3. **Textures/animations** — final art, and whether strays should get a desaturated overlay instead of
   sharing the variant textures. The variant names are confirmed by the staged
   `temp/{curios,foodie,lively,dual}_ghost.png` files; no Curios mod compat is planned.

Answered during implementation: the wild ghost is fully passive; curios removes one harmful effect
per pulse, longest first; foodie converts hunger to saturation once per second above 16 and needs no
mixin; the flute is creative-only for now.
