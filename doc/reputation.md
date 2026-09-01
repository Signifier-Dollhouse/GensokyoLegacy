# Reputation System Refactor Proposal

## Current System

`CharacterData` stores a single `int reputation` (range -300 to 300, hardcoded `MAX`/`MIN` constants). Each action that grants reputation carries a per-call `max` ceiling via `gainReputation(int val, int max)`:

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
    public static final int DAILY_DECAY_THRESHOLD = 150;

    // --- Combat: youkai killed by player ---
    public static final int KILLED_GAIN = 100;
    public static final int KILLED_SOFT_CAP = -50;

    // --- Combat: player killed by youkai ---
    public static final int DEATH_LOSS = 200;

    // --- Combat: player hurt youkai ---
    public static final int HURT_FIRST_SMALL_LOSS = 1;
    public static final int HURT_FIRST_SMALL_REP_THRESHOLD = 100;
    public static final int HURT_FIRST_BIG_LOSS = 5;
    public static final int HURT_FIRST_BIG_FLOOR = -100;
    public static final int HURT_REPEAT_LOW_LOSS = 10;
    public static final int HURT_REPEAT_LOW_FLOOR = -150;
    public static final int HURT_REPEAT_HIGH_LOSS = 20;

    // --- Feeding ---
    public static final int FEED_SOFT_CAP = 150;

    // --- Gifts ---
    public static final int GIFT_SOFT_CAP = 150;

    private ReputationConstants() {}
}
```

## Proposed System

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
//   softCap      -- >= 0: absolute value above which gains are halved (0 = no soft cap)
//                   < 0: forgiveness ceiling; reputation never rises above it (gain clamped)
//   capIncrease  -- how much to increase reputationCap (0 = no cap increase)
//   maxCap       -- absolute maximum reputationCap can reach (ignored if capIncrease <= 0)
public void gainReputation(int val, int softCap, int capIncrease, int maxCap) {
    if (capIncrease > 0) {
        int room = Math.max(0, maxCap - reputationCap);
        reputationCap += Math.min(capIncrease, room);
    }
    if (softCap < 0) {
        // Negative soft cap: forgiveness ceiling. Reputation climbs toward zero but never
        // rises above softCap, so it only recovers while the relationship is negative.
        if (reputation < softCap) {
            reputation = Math.min(reputation + val, softCap);
        }
        return;
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
    reputation = Math.max(reputation - val, ReputationConstants.MIN_REPUTATION);
}
```

**Negative soft cap (forgiveness):** When `softCap < 0`, gains act as forgiveness: reputation is raised toward zero but clamped at `softCap`. Example `gainReputation(100, -50, 0, 0)`: reputation -200 -> -100 (100 gained); reputation -20 -> unchanged (already above -50); reputation -40 -> -50 (clamped at ceiling).

**Soft cap explained:** A non-negative soft cap is an absolute reputation value. Gains are halved when reputation is already at or above the soft cap. When crossing the soft cap in a single gain, the portion before the threshold is full and the portion after is halved.

Example with `softCap = 150`:
- reputation 50, gain 30 -> 80 (fully below, full gain)
- reputation 140, gain 30 -> 155 (crosses: 10 full + 10 halved = 15)
- reputation 200, gain 30 -> 215 (already past, halved: 15)

### Daily decay

Positive reputation decays toward a configurable floor at 80% of the reputation cap (server config `reputation.reputationDecayFloor`, default 0.8). It never falls below that floor. Negative reputation still drifts back toward zero from the JERK threshold.

```java
protected void dailyUpdate() {
    int floor = (int) (reputationCap * GLModConfig.SERVER.reputationDecayFloor.get());
    if (reputation > floor) {
        loseReputation(ReputationConstants.DAILY_DECAY_AMOUNT, floor);
    } else if (reputation < ReputationConstants.THRESHOLD_JERK) {
        gainReputation(ReputationConstants.DAILY_DECAY_AMOUNT, 0, 0, 0);
    }
}
```

`loseReputation(int val)` keeps its constant negative floor (`MIN_REPUTATION`) unless an explicit floor is passed.

### Combat events

```java
protected void onKilledByCharacter() {
    gainReputation(
        ReputationConstants.KILLED_GAIN,
        ReputationConstants.KILLED_SOFT_CAP,
        0, 0
    );
}

protected void onKillCharacter() {
    loseReputation(ReputationConstants.DEATH_LOSS);
}

protected void onHurtCharacter(Player player, YoukaiEntity e, float damage, DamageSource source) {
    boolean danmaku = source.is(DanmakuDamageTypes.DANMAKU_TYPE);
    if (danmaku) return;
    boolean first = !e.targets.contains(player) && e.getLastHurtByMob() != player;
    if (first && damage <= 4) {
        if (reputation >= ReputationConstants.HURT_FIRST_SMALL_REP_THRESHOLD)
            loseReputation(ReputationConstants.HURT_FIRST_SMALL_LOSS);
        else if (reputation >= ReputationConstants.THRESHOLD_STRANGER)
            loseReputation(ReputationConstants.HURT_FIRST_BIG_LOSS);
        else loseReputation(ReputationConstants.HURT_FIRST_BIG_LOSS);
    } else {
        if (first && reputation >= ReputationConstants.HURT_FIRST_SMALL_REP_THRESHOLD)
            loseReputation(ReputationConstants.HURT_FIRST_BIG_LOSS);
        else if (reputation >= ReputationConstants.THRESHOLD_STRANGER)
            loseReputation(ReputationConstants.HURT_REPEAT_LOW_LOSS);
        else loseReputation(ReputationConstants.HURT_REPEAT_HIGH_LOSS);
    }
}
```

`onKilledByCharacter` is forgiveness: a youkai killing the player gains reputation to offset part of the death penalty, but the gain is capped at `KILLED_SOFT_CAP` (-50) so it only helps while the relationship is hostile. No cap increase -- a death doesn't deepen the relationship.

## Quest Reward: `ReputationReward`

### New fields

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
// Early quest: increases cap and gives some rep
new ReputationReward(50, 0, 100, 200) // +50 rep, +100 cap (up to 200)

// Mid-game quest: gives rep with soft cap, increases cap further
new ReputationReward(80, 150, 50, 250) // +80 rep (halved above 150), +50 cap (up to 250)

// Simple daily task: rep only, no cap increase
new ReputationReward(10, 200, 0, 0) // +10 rep, halved above 200, no cap change
```

### Codec update

```java
public static final MapCodec<ReputationReward> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
    Codec.INT.fieldOf("reputation").forGetter(ReputationReward::reputation),
    Codec.INT.fieldOf("soft_cap").forGetter(ReputationReward::softCap),
    Codec.INT.fieldOf("cap_increase").forGetter(ReputationReward::capIncrease),
    Codec.INT.fieldOf("max_cap").forGetter(ReputationReward::maxCap)
).apply(i, ReputationReward::new));
```

**Migration note:** Existing quest JSON with the old `{"reputation": N, "max": M}` format will fail to decode. All quest JSON must be updated via `./gradlew runData`.

## Callers to update

| Caller | Current call | New call |
|--------|-------------|----------|
| `CharDataHolder.feed()` | `gainReputation(v, 300)` | `gainReputation(v, FEED_SOFT_CAP, 0, 0)` |
| `GiftModule.interact()` | `gain(favor, 300)` | `gain(favor, GIFT_SOFT_CAP, 0, 0)` |
| `DonationBoxBlockEntity.take()` | `gain(value * count, 300)` | `gain(value * count, FEED_SOFT_CAP, 0, 0)` -- rep only, no cap increase |
| `ReputationReward.execute()` | `gain(reputation, max)` | `gain(reputation, softCap, capIncrease, maxCap)` |
| `CharacterData.onKilledByCharacter()` | `gainReputation(100, -50)` | `gainReputation(KILLED_GAIN, KILLED_SOFT_CAP, 0, 0)` |
| `CharacterData.dailyUpdate()` | `loseReputation(1, 150)` / `gainReputation(1, -150)` | `loseReputation(DAILY_DECAY)` / `gainReputation(DAILY_DECAY, 0, 0, 0)` |
| `DebugWand` reset | `new CharacterData()` | No change -- both fields initialized from constants |

## `CharDataHolder` changes

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

No structural change -- `CharacterData` is serialized via l2serial, so the new `reputationCap` field will automatically be included in the packet once added as `@SerialField`.

## UI changes

### `ReputationState.getState()`

Update to reference constants:

```java
public static ReputationState getState(int reputation) {
    if (reputation >= ReputationConstants.THRESHOLD_FRIEND)   return ReputationState.FRIEND;
    if (reputation >= ReputationConstants.THRESHOLD_STRANGER) return ReputationState.STRANGER;
    if (reputation >= ReputationConstants.THRESHOLD_JERK)     return ReputationState.JERK;
    return ReputationState.ENEMY;
}
```

### `ReputationState.toInfo()`

Update to display both reputation and cap:

```java
public static Component toInfo(int reputation, int cap) {
    // "Reputation: 150 / 300" (colored by state)
}
```

### `CharacterRequestToServer`

Pass `data.data().reputationCap` alongside `reputation` to `CharacterInfoToClient.ofEntity()`.

### `DonationBoxBlockEntity.getDebugPacket()`

Show `reputationCap` in the tooltip.

### Lang key

Update `gensokyolegacy.info.entity_reputation` to format `"Reputation: %d / %d"` (current / cap).

## Conditions (no change)

`SelfReputationCondition` and `OtherReputationCondition` already test `data().reputation >= threshold`. This remains correct -- conditions check current reputation, not cap.

## Files to modify

1. **New** `content/attachment/character/ReputationConstants.java` -- centralized constants
2. `content/attachment/character/CharacterData.java` -- add `reputationCap` field, rewrite `gainReputation`/`loseReputation`, reference constants
3. `content/attachment/character/CharDataHolder.java` -- update `feed()`, `gain()` signatures
4. `content/attachment/character/ReputationState.java` -- update `getState()` and `toInfo()` to use constants and show cap
5. `content/rpg/reward/ReputationReward.java` -- add 2 new fields, update codec and execute
6. `content/block/donation/DonationBoxBlockEntity.java` -- update `take()` and `getDebugPacket()`
7. `content/entity/module/GiftModule.java` -- update gain call
8. `content/client/debug/CharacterRequestToServer.java` -- pass cap to info packet
9. `content/client/debug/CharacterInfoToClient.java` -- accept and store cap
10. `content/client/debug/CharacterInfoClientManager.java` -- display cap in tooltip
11. `init/data/rpg/ReimuQDGen.java` -- update quest reward constructors
12. All generated quest/trade JSON -- update `ReputationReward` format
13. `init/data/GLLang.java` -- update lang format string
14. Generated lang files (`en_us.json`, `en_ud.json`, `zh_cn.json`)

## Migration

Breaking change. Existing quest JSON must be regenerated via `./gradlew runData`. Old worlds will deserialize `reputationCap` as 0 (l2serial default), effectively capping all rep at 0 until cap-increasing actions are performed again.
