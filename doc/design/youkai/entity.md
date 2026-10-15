# Youkai Entity — `YoukaiEntity` and its `impl/` facets

`YoukaiEntity` is the single base every character descends from: `GeneralYoukaiEntity` adds a spell card,
`SmartYoukaiEntity` adds a brain, `FairyEntity` / `MaidenEntity` / `RumiaEntity` / … add what makes each one herself.
What sits directly on the base is everything *every* youkai does — chew, shoot, flinch, sleep, keep score with the
players around her — and that half lives in `content/entity/youkai/impl/`, the same shape the doll uses
(`doc/design/doll/entity.md` §1.1).

There is deliberately **no second class level**. The dolls needed one because of the pairing boundary — the ledger
logic has to stay on a base that every doll kind shares — and `BaseDollEntity` / `DollEntity` is that boundary.
`YoukaiEntity` is already that base, so the split here is interfaces only and nothing inherits differently.

## 1. The facet rules

Copied from the doll implementation, which is the reference for the pattern:

1. **One interface per feature, all method bodies `default`.** The entity class body keeps only what Java will not
   let an interface hold.
2. **`YoukaiBaseImpl.asYoukai()`** gives every facet a typed `self`, so no facet declares its own cast. Named
   `asYoukai()` rather than `self()` because `DamageClampEntity` already owns a `self()`.
3. **Facets hold no instance state.** A `default` method has nowhere to keep a field, and the entity's `@SerialField`
   surface is the NBT contract — see §4.
4. **A vanilla override stays on the class, as a one-line delegator.** See §2 — this is not optional.
5. **The base dispatches by iterating `createModules()`.** Unchanged from before the split.

### 1.1 What is in `impl/`

Seven types, in two kinds. The facets are the default-method interfaces the rules above are about; the other three
are plain supporting types that moved in because they are the same *per-character machinery* and the old split left
them stranded one package over.

| Facet | Owns |
|---|---|
| `YoukaiBaseImpl` | `asYoukai()`, `getEntityData()`, the flag bitfield, `resolvePlayer` / `getData` |
| `YoukaiEating` | the chew: two synced accessors, its clock, the crumbs |
| `YoukaiCombat` | targeting ticks, the target predicates, the danmaku hooks, the spell circle, `shoot` |
| `YoukaiHurt` | damage bookkeeping, the dislike threshold, death and kill-credit |
| `GeoYoukaiAnim` | the GeckoLib rig: the three controllers, the `YoukaiAnim` → clip mapping (moved from `facet/`) |

| Supporting type | Why it is here |
|---|---|
| `YoukaiAnim` | the shared one-shot vocabulary, trigger names and event ids (moved from `facet/`) |
| `YoukaiFlags` | the flag bitfield's enum, the index `YoukaiBaseImpl` reads and writes (moved from the parent package) |

`facet/` is gone. `GeoYoukaiAnim` and `YoukaiAnim` were its whole contents, and `YoukaiFlags` sat beside them
logging a split that no longer means anything: `facet/` held two of the four things a character's own machinery is,
and the other two were in the parent package. `impl/` is now all of it. The doll set the precedent — `DollGeo`, the
doll's rig interface, has always been in `impl/`.

`YoukaiPickup`, `YoukaiSleep` and `YoukaiRelation` are the remaining candidates; none of them has earned its file
yet — pickup is thirty lines of module scanning, sleep is twenty, and the relation half is a dozen one-line policy
hooks that `SmartYoukaiEntity` overrides wholesale.

## 2. What cannot move, and why

**A superclass method silently beats an interface default.** If a class inherits a concrete method from
`DamageClampEntity` and a `default` with the same signature from a facet, the superclass wins — no error, no
warning, the default is dead code. This is the same trap `IDimensionBoundEntity` documents at
`doc/design/doll/entity.md` §3.1a, and it is why that marker carries no methods.

So every override of a `Entity` / `LivingEntity` / `Mob` method stays on `YoukaiEntity` as a real override that
delegates into the facet:

| Stays on the class | Delegates to |
|---|---|
| `isInvulnerableTo` | `YoukaiCombat#invulnerableTo` |
| `actuallyHurt` | `YoukaiHurt#onDamaged` |
| `die` | `YoukaiHurt#onDeath` |
| `killedEntity` | `YoukaiHurt#onKillEntity` |
| `postHurt` | nothing - the flinch clock is state, so the whole override stayed |
| `canPickUpLoot`, `wantsToPickUp`, `pickUpItem` | module scan |

`shouldHurt`, `onDanmakuHit`, `onDanmakuImmune`, `shouldShowSpellCircle`, `getSpellCircle` and `getCircleSize` were
`final` on the class and are `default`s on the facet, because they are declared by `IYoukaiEntity` and
`SpellCircleHolder`. That drops the `final`, which is a real loss: the guard used to stop a character re-deriving
combat policy one predicate at a time. The replacement is `YoukaiCombatManager`, which is the intended extension
point for anything genuinely character-specific.

**A facet that supplies a default for an interface method must extend that interface.** `YoukaiCombat` declares
`extends YoukaiBaseImpl, IYoukaiEntity, SpellCircleHolder`, so its defaults *override* those declarations. Had it
been an unrelated sibling, every character would have failed to compile with "inherits abstract and default for
`getCircleSize(float)` from types `YoukaiCombat` and `SpellCircleHolder`" - javac will not let a default and an
abstract from two unrelated interfaces meet on the same class. `DollDanmakuAlly` solves the same problem the same
way. `YoukaiEntity` still lists both interfaces on its own `implements` clause for readability; that is harmless
precisely because the facet extends them.

`getModule` stays on the class too, with `createModules`: it is the module-registry seam, three lines long, and
`modules` is `protected` so a facet cannot reach the holder behind it anyway.

## 3. Synced accessors

The doll defines its `EntityDataAccessor`s inside a nested `final class XxxModule implements DollModule`, because
`defineSynchedData` runs inside the vanilla `Entity` constructor before any subclass state exists. The youkai has
the same constraint and one extra wrinkle: its accessors ride a chain of `SyncedData` objects (`YOUKAI_DATA` →
`SPELL_DATA` → `PLAIN_FAIRY_DATA`, …), and `YoukaiEntity#data()` returns the tip so `defineSynchedData` registers
the whole chain.

The eating accessors leave that chain. They were declared with a `null` name, which `SyncedData#write` and
`#read` skip, so they were never part of the NBT contract — moving them to a plain `builder.define` is
behaviourally identical, and `data()` keeps its meaning as the *persisted* half of the entity data.
`YoukaiEating#defineSynchedData` is the static hook that does it.

The chain itself keeps `DATA_FLAGS_ID`, the one accessor with a name (`"youkai_flags"`), because that one *is*
persisted and must round-trip. It went from `protected` to `public` so `YoukaiBaseImpl` can reach it from another
package, matching `DollEntity.DATA_COLOR`.

## 4. State

A facet is stateless, so every field that moved logic needs somewhere to live:

- `lastHurtAnim` — the flinch clock stays a private field on `YoukaiEntity`. It is genuinely per-entity mutable
  state with no home of its own, and moving it into `YoukaiCombatManager` would give the manager a second job.
  That is also why `postHurt` never became a facet: there was nothing left to move but the cooldown arithmetic.
- `noTargetTime` — same, but reachable from a facet, so it is exposed as `noTargetTime()` / `setNoTargetTime(int)`.
  A `protected` field is not readable from a facet: `YoukaiCombat` is not a subclass of `DamageClampEntity`.
- `dead` — also `protected`, and also only readable from inside the entity. `die` folds the before/after pair into
  a single boolean and hands that to `YoukaiHurt#onDeath`.
- `bossEvent` — lives on `DamageClampEntity`, exposed as `setBossBarVisible(boolean)` beside the rest of the
  boss-bar API there.
- `noPlayerTime` is written and never read anywhere in the codebase. It is left in place rather than deleted,
  because removing a `@SerialField` changes the auto-serial blob. Its two zeroing sites now both go through
  `refreshIdle()`.

None of the `@SerialField` names move, so no existing save breaks.

## 5. Module registry

`AbstractYoukaiModule` gains `default void defineSynchedData(SynchedEntityData.Builder)`, mirroring `DollModule`,
and `YoukaiEntity#defineSynchedData` iterates a freshly built module list the way `BaseDollEntity` does — a local
list, because the `modules` field does not exist yet during `Entity` construction.

This is deliberate: there is exactly one module registry. The doll could afford a second list because
`DollModule` and the doll facets are disjoint concerns; here the facets and `AbstractYoukaiModule` are the same
thing seen from two angles, and two registries over one `createModules()` list would be strictly worse.

All the module constructors are trivial (`super(ID, self)` plus field initialisers), so calling `createModules()`
twice per entity load costs a handful of allocations and is what the dolls already pay.

That triviality is now a **constraint rather than an observation**: the list is built inside `Entity`'s own
constructor, so a module whose constructor or field initialiser dereferences entity state will throw there, in a
stack trace that points nowhere near the real mistake. Same rule the doll states at
`doc/design/doll/entity.md` §1.1 — module creation is static-safe, and the synced snapshot rather than the entity
is what it is handed.

## 6. What is not a facet

The subclass-policy hooks stay on the class: `mayFly`, `mayLand`, `vanishOutOfSight`, `isTalkingTo`,
`shouldSearchTarget`, `mayInteract(Player, Activity)` and `createCombatManager`. Every one of them is overridden by
`SmartYoukaiEntity` or a named character, and an interface default there would add an indirection and nothing
else.

## 7. Package layout

```
content/entity/youkai/
        YoukaiEntity, GeneralYoukaiEntity, SmartYoukaiEntity, BossYoukaiEntity
        YoukaiFeatureSet, RestrictData          — config and NBT records, entity-adjacent
        impl/                                   — per-character machinery (§1.1)
        renderer/                               — render layers and the general renderer
```

`tools/export_dialogs.py` parses `YoukaiAnim.java` and `GeoYoukaiAnim.java` out of their source paths to build its
per-character animation table, so those two constants (`YOUKAI_ANIM_JAVA`, `GEO_YOUKAI_JAVA`) track the package. A
move of either file silently breaks the DIALOG ANIMATIONS section — the script does not fail, it just stops
resolving clips.