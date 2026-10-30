# Reputation System

## Before the refactor

`CharacterData` stored a single `int reputation` (range -300 to 300, hardcoded `MAX`/`MIN` constants). Each action that granted reputation carried a per-call `max` ceiling via `gainReputation(int val, int max)`:

- Feeding: `gainReputation(v, 300)`
- Gifts: `gain(favor, 300)`
- Quests: `ReputationReward(reputation, max)` -- e.g. `ReputationReward(50, 200)` caps this quest at 200
- Kill-youkai: `gainReputation(100, -50)` -- only helps up to -50
- Donation box: `gain(value * count, 300)`

**Problems:**

1. **Magic numbers everywhere.** Every caller passes its own ceiling. There is no single source of truth for "how much reputation can this player have with this character."
2. **Early actions become useless.** A quest with `max=200` does nothing once reputation is already 200, even though the hard cap is 300. Simple actions (feed, gift) are the only way to close that gap, but they feel disconnected from quest progression.
3. **No concept of a "reputation journey."** The cap is implicit and per-source, not a property of the character relationship itself. There is no way to say "this character's trust grows over time."

## `ReputationConstants`

All magic numbers live here. Callers reference these instead of inline literals.

```java
public final class ReputationConstants {

    // --- Absolute bounds ---
    public static final int MIN_REPUTATION = -300;
    public static final int MAX_REPUTATION = 300;

    // --- Starting values ---
    public static final int INITIAL_REPUTATION = 0;
    public static final int INITIAL_CAP = 100;

    // --- ReputationState thresholds ---
    public static final int THRESHOLD_FRIEND = 150;
    public static final int THRESHOLD_STRANGER = -50;
    public static final int THRESHOLD_JERK = -150;

    // --- Daily decay ---
    public static final int DAILY_DECAY_AMOUNT = 1;

    // --- Negative recovery ---
    public static final int NEGATIVE_HALF_THRESHOLD = -20;
    public static final int NEGATIVE_RECOVER_AMOUNT = 10;

    // --- Combat: youkai killed the player (forgiveness) ---
    public static final int KILLED_RESTORE = 0;

    // --- Combat targeting ---
    public static final int COMBAT_SAFE_THRESHOLD = 0;
    public static final int DISCARD_REP_THRESHOLD = 0;

    // --- Combat: player killed a youkai ---
    public static final int DEATH_LOSS = 200;

    // --- Combat: player hurt a youkai ---
    public static final int HURT_LOSS_FLOOR = -150;
    public static final int HURT_FIRST_SMALL_LOSS = 1;
    public static final int HURT_FIRST_SMALL_REP_THRESHOLD = 100;
    public static final int HURT_FIRST_BIG_LOSS = 5;
    public static final int HURT_REPEAT_LOW_LOSS = 10;
    public static final int HURT_REPEAT_HIGH_LOSS = 20;

    // --- Feeding ---
    public static final int FEED_SOFT_CAP = 150;

    // --- Gifts ---
    public static final int GIFT_SOFT_CAP = 150;

    private ReputationConstants() {}
}
```

The two targeting thresholds both sit at 0 on purpose. `COMBAT_SAFE_THRESHOLD` is compared *above* (`rep > 0` leaves the player alone), so an untouched relationship is still a valid target; `DISCARD_REP_THRESHOLD` is compared *below*, so only an actively hostile relationship makes the character discard itself.

## How it works

### Data model

`CharacterData` stores two fields:

```java
@SerialField
public int reputation;     // current value, starts at INITIAL_REPUTATION (0)

@SerialField
public int reputationCap;  // maximum reachable value, starts at INITIAL_CAP (100)
```

Reputation can never exceed `reputationCap`. The default cap of 100 means the player can immediately start earning reputation with a character, but quests are required to push past 100 toward the hard maximum of 300.

### Gain/lose methods

```java
// Positive reputation gain with absolute soft cap and optional cap increase.
//   val          -- base reputation to add
//   softCap      -- absolute reputation value above which gains are halved (0 = no soft cap)
//   capIncrease  -- how much to increase reputationCap (0 = no cap increase)
//   maxCap       -- absolute maximum reputationCap can reach (ignored if capIncrease <= 0)
public void gainReputation(int val, int softCap, int capIncrease, int maxCap) {
    if (capIncrease > 0) {
        int room = Math.max(0, maxCap - reputationCap);
        reputationCap += Math.min(capIncrease, room);
    }
    if (reputation >= reputationCap) return;
    if (softCap > 0 && reputation >= softCap) {
        // already past soft cap: all gains halved
        reputation = Math.min(reputation + val / 2, reputationCap);
    } else if (softCap > 0 && reputation + val > softCap) {
        // crossing the soft cap: full gain up to softCap, halved for the rest
        reputation = Math.min((val + softCap + reputation) / 2, reputationCap);
    } else {
        // fully below soft cap: full gain
        reputation = Math.min(reputation + val, reputationCap);
    }
}

// Negative reputation (no cap change, no soft cap -- losses are always full).
public void loseReputation(int val) {
    loseReputation(val, ReputationConstants.MIN_REPUTATION);
}

// Same, but stopping at an explicit floor; daily decay uses it so its floor can ride the cap.
public void loseReputation(int val, int floor) {
    reputation = Math.max(reputation - val, floor);
}
```

**One gain path, no sign overloading:** `gainReputation(val, softCap, capIncrease, maxCap)` handles all progress (halving soft cap, optional quest cap increase, always bounded by `reputationCap`). Forgiveness is not a gain at all -- see the combat events below.

**Soft cap explained:** A non-negative soft cap is an absolute reputation value. Gains are halved when reputation is already at or above the soft cap. When crossing the soft cap in a single gain, the portion before the threshold is full and the portion after is halved.

Example with `softCap = 150`:
- reputation 50, gain 30 -> 80 (fully below, full gain)
- reputation 140, gain 30 -> 155 (crosses: 10 full + 10 halved = 15)
- reputation 200, gain 30 -> 215 (already past, halved: 15)

### Day changes

`CharacterAttachment` persists `lastDailyDay` and compares it with the overworld's day index:

```java
private static final long MIDNIGHT_SHIFT = 6000L;

private static long dayIndex(ServerPlayer player) {
    return (player.server.overworld().getDayTime() + MIDNIGHT_SHIFT) / Level.TICKS_PER_DAY;
}
```

The shift moves the rollover from dawn to midnight (tick 18000). Comparing indices instead of testing `getDayTime() == 0` cannot miss a day: a normal midnight advances the index, and sleeping through the night jumps the day time to the next morning, which advances it too. Reading the overworld keeps the index stable across dimensions. A backwards `/time set` only resets the index and processes nothing.

### Daily update

`CharacterAttachment.tick` calls `updateDay` on the server every tick. When the index advances, every character gets `dailyUpdate(daysPassed)`, where `daysPassed` is the number of day boundaries crossed, capped at 1000:

```java
protected void dailyUpdate(int daysPassed) {
    int floor = (int) (reputationCap * GLModConfig.SERVER.reputationDecayFloor.get());
    if (reputation > floor) {
        loseReputation(ReputationConstants.DAILY_DECAY_AMOUNT, floor);
    }
    for (int i = 0; i < daysPassed && reputation < 0; i++) {
        if (reputation <= ReputationConstants.NEGATIVE_HALF_THRESHOLD) {
            reputation /= 2;
        } else {
            reputation = Math.min(reputation + ReputationConstants.NEGATIVE_RECOVER_AMOUNT, 0);
        }
    }
}
```

Positive reputation decays toward a configurable floor at 80% of the reputation cap (server config `reputation.reputationDecayFloor`, default 0.8) and never falls below it. That decay applies once per settlement, so logging in after several days costs one point, not one per day. Negative recovery runs once per elapsed day: at or below `NEGATIVE_HALF_THRESHOLD` (-20) the value halves toward zero, above it gives back `NEGATIVE_RECOVER_AMOUNT` (10), capped at 0. From -300 the sequence is -150, -75, -38, -19, -9, 0. Because the index is server world time, an hour of server uptime (three game days) settles three recovery steps on the next tick after login.

### Combat events

```java
protected void onKilledByCharacter() {
    if (reputation < ReputationConstants.KILLED_RESTORE) {
        reputation = ReputationConstants.KILLED_RESTORE;
    }
}

protected void onKillCharacter() {
    loseReputation(ReputationConstants.DEATH_LOSS);
}

protected void onHurtCharacter(Player player, YoukaiEntity e, float damage, DamageSource source) {
    boolean danmaku = source.is(DanmakuDamageTypes.DANMAKU_TYPE);
    if (danmaku) return;
    boolean first = !e.targets.contains(player) && e.getLastHurtByMob() != player;
    int floor = ReputationConstants.HURT_LOSS_FLOOR;
    if (first && damage <= 4 && reputation >= ReputationConstants.HURT_FIRST_SMALL_REP_THRESHOLD) {
        loseReputation(ReputationConstants.HURT_FIRST_SMALL_LOSS, floor);
    } else if (first && reputation >= ReputationConstants.HURT_FIRST_SMALL_REP_THRESHOLD) {
        loseReputation(ReputationConstants.HURT_FIRST_BIG_LOSS, floor);
    } else if (reputation >= ReputationConstants.THRESHOLD_STRANGER) {
        loseReputation(first ? ReputationConstants.HURT_FIRST_BIG_LOSS : ReputationConstants.HURT_REPEAT_LOW_LOSS, floor);
    } else {
        loseReputation(first ? ReputationConstants.HURT_FIRST_BIG_LOSS : ReputationConstants.HURT_REPEAT_HIGH_LOSS, floor);
    }
}
```

`onKilledByCharacter` is forgiveness, and it is the one place that does not go through the gain path: a hostile relationship is dragged straight up to `KILLED_RESTORE` (0) instead of climbing, a friendly one is untouched, and the cap never moves -- a death doesn't deepen a relationship. Danmaku never counts as an insult, so it returns before any loss is applied.

Hurt losses are floored at `HURT_LOSS_FLOOR` (-150, the JERK threshold): only killing a youkai can push below it.

## Quest Reward: `ReputationReward`

### Fields

```java
public record ReputationReward(
    int reputation,     // base rep to gain
    int softCap,        // absolute soft cap (0 = no soft cap)
    int capIncrease,    // how much to increase reputationCap (0 = no cap increase)
    int maxCap          // absolute maximum reputationCap (ignored if capIncrease <= 0)
) implements QuestReward<ReputationReward> { ... }
```

### Example usage in datagen

```java
// Story quest: rep plus a chunk of cap
new ReputationReward(20, 300, 10, 300) // +20 rep, +10 cap (up to 300)

// Repeatable daily: small cap bump, gains halve once past 150
new ReputationReward(10, 150, 5, 120) // +10 rep, +5 cap (up to 120)

// Late quest: rep only, the cap is already open
new ReputationReward(10, 300, 0, 300) // +10 rep, no cap change
```

### Codec

```java
public static final MapCodec<ReputationReward> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
    Codec.INT.fieldOf("reputation").forGetter(ReputationReward::reputation),
    Codec.INT.fieldOf("soft_cap").forGetter(ReputationReward::softCap),
    Codec.INT.fieldOf("cap_increase").forGetter(ReputationReward::capIncrease),
    Codec.INT.fieldOf("max_cap").forGetter(ReputationReward::maxCap)
).apply(i, ReputationReward::new));
```

**Migration note:** The old `{"reputation": N, "max": M}` format no longer decodes, so all quest JSON was regenerated with `./gradlew runData`.

## Callers

| Caller | Call |
|--------|------|
| `CharDataHolder.feed()` | `gainReputation(v, FEED_SOFT_CAP, 0, 0)` |
| `GiftModule.interact()` | `gain(favor, GIFT_SOFT_CAP, 0, 0)` |
| `DonationBoxBlockEntity.take()` | `gain(value * count, FEED_SOFT_CAP, 0, 0)` -- rep only, no cap increase |
| `ReputationReward.execute()` | `gain(reputation, softCap, capIncrease, maxCap)` |
| `CharacterData.onKilledByCharacter()` | `reputation = max(reputation, KILLED_RESTORE)` |
| `CharacterData.dailyUpdate(daysPassed)` | one `loseReputation(DAILY_DECAY_AMOUNT, floor)` plus `daysPassed` negative-recovery steps |
| `CharacterAttachment.tick()` | `dailyUpdate(passed)` for every day-index advance |
| `CharacterAttachment.reset()` | `new CharacterData()` -- both fields initialized from constants |
| `GLCommands` | admin-only set/add/cap commands, clamped to `MIN_REPUTATION`/`MAX_REPUTATION` |

## `CharDataHolder`

Both reputation entry points sync to the client themselves, so callers never sync by hand. `feed()` returns the reputation it granted -- the feed memory rate may already have cut that below the nominal `favor`, while the soft cap is applied on top and is not reflected in the return value.

```java
public int feed(ItemStack food, int favor) {
    double rate = data.foodData.feed(food);
    int v = (int) Math.round(rate * favor);
    data.gainReputation(v, ReputationConstants.FEED_SOFT_CAP, 0, 0);
    sync();
    return v;
}

public void gain(int v, int softCap, int capIncrease, int maxCap) {
    data.gainReputation(v, softCap, capIncrease, maxCap);
    sync();
}
```

## Network sync: `CharDataToClient`

No structural change -- `CharacterData` is serialized via l2serial, so `reputationCap` is part of the packet as a `@SerialField`.

## UI

### `CharacterData.getState()`

Derives the state from the thresholds, highest first:

```java
public static ReputationState getState(int reputation) {
    if (reputation >= ReputationConstants.THRESHOLD_FRIEND)   return ReputationState.FRIEND;
    if (reputation >= ReputationConstants.THRESHOLD_STRANGER) return ReputationState.STRANGER;
    if (reputation >= ReputationConstants.THRESHOLD_JERK)     return ReputationState.JERK;
    return ReputationState.ENEMY;
}
```

### `ReputationState.toInfo()`

Shows reputation and cap together, coloured by state:

```java
public static Component toInfo(int reputation, int cap) {
    // GLLang.Info.ENTITY_REPUTATION -> "Your reputation: 150 / 300"
}
```

### `CharacterRequestToServer`

Passes `data.data().reputationCap` alongside `reputation` to `CharacterInfoToClient.ofEntity()`, which shows it via `ReputationState.toInfo`. `DonationBoxBlockEntity.getDebugPacket()` does the same for a bound bed.

### Lang key

`gensokyolegacy.info.entity_reputation` formats `"Your reputation: %s / %s"` (current / cap).

## Conditions (no change)

`SelfReputationCondition` and `OtherReputationCondition` already test `data().reputation >= threshold`. This remains correct -- conditions check current reputation, not cap.

## Files

- `content/attachment/character/ReputationConstants.java` -- every tuning number
- `content/attachment/character/CharacterData.java` -- `reputation` / `reputationCap`, the gain and lose methods, daily recovery and the combat reactions
- `content/attachment/character/CharacterAttachment.java` -- `lastDailyDay` and day-change detection
- `content/attachment/character/CharDataHolder.java` -- `feed()` and `gain()`, each syncing afterwards
- `content/attachment/character/ReputationState.java` -- state to colour and target kind
- `content/rpg/reward/ReputationReward.java` -- the four fields and their codec
- `content/block/deco/donation/DonationBoxBlockEntity.java` -- gold exchange and debug tooltip
- `content/entity/behavior/module/GiftModule.java` -- the gift gain
- `content/client/debug/CharacterRequestToServer.java`, `CharacterInfoToClient.java`, `CharacterInfoClientManager.java` -- cap on the debug tooltip
- `init/data/rpg/{Reimu,Alice,Marisa}QDGen.java` and the generated quest JSON -- reward values
- `init/data/GLLang.java` and the generated lang files -- the format string

## Migration

Breaking change. Existing quest JSON must be regenerated via `./gradlew runData`. Old worlds will deserialize `reputationCap` as 0 (l2serial default), effectively capping all rep at 0 until cap-increasing actions are performed again.
