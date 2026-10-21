# Cooldown Speed

`CooldownClock` — the **cooldown clock** — runs a player's item cooldowns faster than vanilla
does. It is the frame every source of cooldown speed plugs into, and the *lively* effect is the
first one to use it.

> Status: implemented. The clock is a mixin on vanilla's `ItemCooldowns`, the rate comes from
> `CooldownRates`, and the *lively* effect is registered as `GLEffects.LIVELY`.

## 1. The clock, not the duration

Vanilla stores every cooldown as an absolute end time read against a counter that advances once
per player tick (`ItemCooldowns`):

```java
private final Map<Item, CooldownInstance> cooldowns = Maps.newHashMap();
private int tickCount;

public void addCooldown(Item item, int ticks) {
    this.cooldowns.put(item, new CooldownInstance(this.tickCount, this.tickCount + ticks));
```

A cooldown is therefore a *clock reading*, not a duration that is counted down. Two ways to make
one end sooner:

- **Scale the duration at the start** — `addCooldown(item, ticks / 2)`. Simple, but it is a
  one-off: the cooldown that is already running when the bonus arrives does not change, and a
  bonus that arrives later does nothing to the cooldowns already paid for.
- **Run the clock faster** — advance `tickCount` by more than one per tick. Every cooldown runs
  short, including the ones already running, and the overlay stays correct because the whole
  time base moves together: the wipe fraction is
  `(endTime - tickCount) / (endTime - startTime)`, and both ends of that are in the same
  accelerated clock.

The clock is the one the mod uses.

### 1a. Where the clock is advanced

`Player#tick()` fires `PlayerTickEvent.Pre` at its first line and calls `this.cooldowns.tick()`
at its last, so the pre-tick is the one place where the clock can be pushed forward *before*
vanilla does its own increment and its expiry check. A cooldown that runs out during a tick is
therefore removed in that tick, not the next one.

`Player#tick()` is reached on both sides — `LocalPlayer#tick()` calls `super.tick()` on the
client and `ServerPlayer#doTick()` calls it on the server — and the server's
`ClientboundCooldownPacket` carries the **unscaled** duration. That is what an accelerated clock
needs: the duration is in clock ticks, and the clock itself is faster on both sides, so no packet
changes and the client's overlay and its own cooldown checks stay in step with the server.

### 1b. The phase

A rate is a fraction, so the clock cannot be advanced by a fraction of a tick. The gain is
instead the difference between two whole numbers:

```java
(int) (bonus * tick) - (int) (bonus * (tick - 1))   // bonus = rate - 1
```

which telescopes: over any run of ticks the clock gains exactly what the bonus says, and the
phase comes from the tick number rather than from a carried remainder. What is gained is the
bonus over vanilla's own tick, so the rate enters as `rate - 1`: a rate of 1.2 is a fifth more
ticks, not a fifth more on top of the tick vanilla already gives.

The tick number is the player's own `tickCount`, which both sides increment once per player tick
before the player is ticked, so the client and the server run the same pattern.

There is nothing to carry, and so nothing to reset: a respawn starts both the player's tick
count and its cooldown clock at zero, and the two stay in step. A rate that comes and goes
leaves the clock off by at most a tick per change, which is the same order as the difference
between the two sides' tick counts.

## 2. The rate

`CooldownRates.of(player)` is the player's clock speed, and 1.0 is vanilla speed. It is every
registered `CooldownSource` multiplied together, capped by `cooldownMaxRate`.

- The sources **multiply**, so a source is a factor of the whole rather than a share of it.
- The rate never drops under 1.0: the clock only ever runs forward, and a source that wants to
  slow the player down is ignored rather than stalling the clock.
- The cap is what keeps a pile of sources from running the clock away.

`CooldownSource` is the extension point: one method, one class, one `CooldownRates.register`
call. A source is asked once per player tick, so it should read state that is already there.

### 2a. The *lively* effect

`GLEffects.LIVELY` gives **20% more cooldown speed per level, added up rather than compounded**:
level one is 1.2 and level two is 1.4, so the second level is worth as much as the first.

The effect itself is an `EmptyEffect` — a marker, like `BAKA` and `LOOTING` — and the rate is
read by `LivelySource`. The effect is a `MobEffect`, so it is synced to the client on its own
and the client computes the same rate the server does.

A rate is a clock speed, so it only reaches a cooldown in whole game ticks, and the shorter the
cooldown the less of the rate it sees. How many game ticks a cooldown of N clock ticks takes:

| cooldown | ×1.0 | ×1.2 | ×1.4 | ×1.6 | ×2.0 |
|---|---|---|---|---|---|
| 5 (dagger glove, single) | 5 | 5 | 4 | 4 | 3 |
| 10 (dagger glove, fan) | 10 | 9 | 8 | 7 | 5 |
| 20 (border umbrella) | 20 | 17 | 15 | 13 | 10 |
| 40 (fairy dust) | 40 | 34 | 29 | 25 | 20 |
| 100 (mermaid pearl) | 100 | 84 | 72 | 63 | 50 |
| 200 (koishi hat) | 200 | 167 | 143 | 125 | 100 |

So a level of lively barely touches the dagger glove's 5 tick single shot and halves the mermaid
pearl, which is the shape of the effect: it is worth most to the slow items.

## 3. What this touches

Everything that goes through vanilla's `ItemCooldowns` accelerates, the mod's own items included,
with no change at any call site: the dagger glove (`mode.cooldown() + rune.cooldownCost()` is in
the same clock, so the rune's price drains faster too), the border umbrella, the cat bell, the
mermaid pearl, the fairy dust, the doll glove, the hakkero, the koishi hat and the fancy fan.
Vanilla items accelerate as well, the shield's disable cooldown among them.

Two cooldowns in the mod are *imposed on* the player rather than spent by them, and they
accelerate with everything else:

| Where | What it is | What a fast clock does to it |
|---|---|---|
| `KoishiAttackCapability#onBlock` | a 100 tick cooldown on the shield, rate limiting how often a koishi attack can be blocked | a fast player blocks the attack more often, so the hat comes sooner |
| `TalismanContext#addCooldown` | a 100 tick window on the *target* of a talisman | a fast target is talismaned more often |

A cooldown that must not accelerate cannot be exempted by item, because the clock is global: it
has to live outside `ItemCooldowns`, in a counter of its own. The koishi capability already has
one (`attackCooldown`) to hang it on.

## 4. The client and the server

The rate has to be computable on the client, or the client's clock falls behind the server's.
The cap comes from the server-synced config and a `MobEffect` is synced by vanilla, so the rate
the client computes is the one the server computes. A source that reads server-only state (a
reputation, say) would not be: the client's overlay would go stale, though the server's
`ClientboundCooldownPacket(item, 0)` still clears a finished cooldown, so the client never
blocks a use for longer than the server allows.

## 5. Files

- `content/cooldown/CooldownSource.java` — the extension point.
- `content/cooldown/CooldownRates.java` — the registry and the rate.
- `content/cooldown/CooldownClock.java` — the per-tick advance.
- `content/cooldown/LivelySource.java` — the *lively* effect's rate.
- `event/CooldownEventHandlers.java` — the `PlayerTickEvent.Pre` hook.
- `mixin/ItemCooldownsAccessor.java` — the clock's counter.
- `init/data/GLModConfig.java` — `cooldownMaxRate`.
- `init/registrate/GLEffects.java` — `LIVELY`.
- `textures/mob_effect/lively.png` — the effect icon.

## 6. Verified

- `./gradlew build` clean; `./gradlew runData` clean, with the effect's lang entries written.
- **In game, on a dedicated server**: a 20 tick cooldown was still running after 17 ticks with
  the vanilla clock, and already over after 17 ticks with the clock at rate 1.2 — which is the
  17 ticks the table above gives. That is the mechanism working, not a model of it.
- The gain checked outside the game, over 2000 game ticks: a rate of 1.2 hands out 400 extra
  ticks, so the clock advances at 1.2 ticks per game tick and never by more than 2 in a tick.
- The *lively* levels checked against the formula: level one 1.2, level two 1.4, level three
  1.6.
- The accessor's field checked against the compiled class: `ItemCooldowns` declares
  `private int tickCount`.

Not yet verified in game: that the client's clock and the server's stay in step over a long
session, that the overlay wipes at the accelerated rate rather than jumping, and how the
acceleration reads next to the dagger glove's 5 tick cooldown.
