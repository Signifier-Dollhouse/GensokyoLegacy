package dev.xkmc.gensokyolegacy.content.attachment.character;

import dev.xkmc.danmakuapi.init.data.DanmakuDamageTypes;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One player's relationship with one character: the reputation value, the ceiling it may
 * currently reach, per-action cooldowns and the feed memory.
 *
 * @see doc/design/reputation.md
 */
@SerialClass
public class CharacterData {

	@SerialField
	protected final FeedModuleData foodData = new FeedModuleData();

	@SerialField
	public int reputation;

	/** Highest reputation this relationship can currently reach; quests are what raise it. */
	@SerialField
	public int reputationCap = ReputationConstants.INITIAL_CAP;

	@SerialField
	public final Map<String, Long> timers = new LinkedHashMap<>();

	public boolean isAvailable(String key, long gameTime) {
		return gameTime >= timers.getOrDefault(key, 0L);
	}

	public void setTimer(String key, long nextAvailable) {
		timers.put(key, nextAvailable);
	}

	/**
	 * Adds {@code val}, raising the cap by {@code capIncrease} (never past {@code maxCap}) first.
	 * A positive {@code softCap} halves the gain; 0 turns off either part. Never passes {@code reputationCap}.
	 */
	public void gainReputation(int val, int softCap, int capIncrease, int maxCap) {
		if (capIncrease > 0) {
			int room = Math.max(0, maxCap - reputationCap);
			reputationCap += Math.min(capIncrease, room);
		}
		if (reputation >= reputationCap) return;
		if (softCap > 0 && reputation >= softCap) {
			reputation = Math.min(reputation + val / 2, reputationCap);
		} else if (softCap > 0 && reputation + val > softCap) {
			reputation = Math.min((val + softCap + reputation) / 2, reputationCap);
		} else {
			reputation = Math.min(reputation + val, reputationCap);
		}
	}

	public void loseReputation(int val) {
		loseReputation(val, ReputationConstants.MIN_REPUTATION);
	}

	public void loseReputation(int val, int floor) {
		reputation = Math.max(reputation - val, floor);
	}

	/** Decay stops at the configured fraction of the cap, so a cap raised by quests raises the floor too. */
	protected void dailyUpdate() {
		int floor = (int) (reputationCap * GLModConfig.SERVER.reputationDecayFloor.get());
		if (reputation > floor) {
			loseReputation(ReputationConstants.DAILY_DECAY_AMOUNT, floor);
		} else if (reputation < ReputationConstants.THRESHOLD_JERK) {
			gainReputation(ReputationConstants.DAILY_DECAY_AMOUNT, 0, 0, 0);
		}
	}

	public static ReputationState getState(int reputation) {
		if (reputation >= ReputationConstants.THRESHOLD_FRIEND)
			return ReputationState.FRIEND;
		if (reputation >= ReputationConstants.THRESHOLD_STRANGER)
			return ReputationState.STRANGER;
		if (reputation >= ReputationConstants.THRESHOLD_JERK)
			return ReputationState.JERK;
		return ReputationState.ENEMY;
	}

	public ReputationState getState() {
		return getState(reputation);
	}

	/** Forgiveness: a hostile relationship drops straight to {@link ReputationConstants#KILLED_RESTORE}, it does not climb. */
	protected void onKilledByCharacter() {
		if (reputation < ReputationConstants.KILLED_RESTORE) {
			reputation = ReputationConstants.KILLED_RESTORE;
		}
	}

	protected void onHurtCharacter(Player player, YoukaiEntity e, float damage, DamageSource source) {
		boolean danmaku = source.is(DanmakuDamageTypes.DANMAKU_TYPE);
		// Danmaku is exempt: a spell card is a fight, not an insult.
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

	protected void onKillCharacter() {
		loseReputation(ReputationConstants.DEATH_LOSS);
	}

}
