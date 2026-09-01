package dev.xkmc.gensokyolegacy.content.attachment.character;

import dev.xkmc.danmakuapi.init.data.DanmakuDamageTypes;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

@SerialClass
public class CharacterData {

	@SerialField
	protected final FeedModuleData foodData = new FeedModuleData();

	@SerialField
	public int reputation;

	@SerialField
	public int reputationCap = ReputationConstants.INITIAL_CAP;

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
		reputation = Math.max(reputation - val, ReputationConstants.MIN_REPUTATION);
	}

	protected void dailyUpdate() {
		if (reputation > ReputationConstants.THRESHOLD_FRIEND) {
			loseReputation(ReputationConstants.DAILY_DECAY_AMOUNT);
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

	protected void onKilledByCharacter() {
		gainReputation(
				ReputationConstants.KILLED_GAIN,
				ReputationConstants.KILLED_SOFT_CAP,
				0, 0
		);
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

	protected void onKillCharacter() {
		loseReputation(ReputationConstants.DEATH_LOSS);
	}

}
