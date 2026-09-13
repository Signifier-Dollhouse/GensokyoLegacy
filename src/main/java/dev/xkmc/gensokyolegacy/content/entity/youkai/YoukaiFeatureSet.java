package dev.xkmc.gensokyolegacy.content.entity.youkai;

/**
 * @param effectImmune         Immune to mob effects, fire, heal reduction, etc
 * @param damageFilter         Enable damage filter, set health immunity, etc
 * @param noTargetHealing      Enable no-target healing
 * @param limiter              Damage limit factor (1/n). Also scales the dynamic reduction floor (1/n of max health)
 * @param dynamicReductionRate Rate at which the dynamic reduction floor drops per tick. 0 disables dynamic reduction
 */
public record YoukaiFeatureSet(
		boolean effectImmune, boolean damageFilter,
		boolean noTargetHealing, boolean trueDamageOnImmune, boolean hasBossBar,
		int limiter , int noPlayerDiscardTime,
		double maxSpeed, float dynamicReductionRate) {

	public static final YoukaiFeatureSet NONE = YoukaiFeatureSet.builder()
			.limit(5)
			.build();

	public static final YoukaiFeatureSet BOSS = YoukaiFeatureSet.builder()
			.markBoss().limit(20)
			.build();

	public static final YoukaiFeatureSet FULL = YoukaiFeatureSet.builder()
			.markBoss().limit(20).damageFilter().dynamicReduction()
			.build();

	public static final YoukaiFeatureSet SAGE = YoukaiFeatureSet.builder()
			.markBoss().limit(20).damageFilter().dynamicReduction()
			.trueDamageOnImmune()
			.build();

	public static final YoukaiFeatureSet MAIDEN = YoukaiFeatureSet.builder()
			.markBoss().limit(20).damageFilter().dynamicReduction()
			.trueDamageOnImmune().noPlayerTime(30)
			.build();

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private boolean effectImmune = false;
		private boolean damageFilter = false;
		private boolean noTargetHealing = false;
		private boolean hasBossBar = false;
		private boolean trueDamageOnImmune = false;
		private int limiter = 1;
		private int noPlayerDiscardTime = -1;
		private final double maxSpeed = 0.5;
		private float dynamicReductionRate = 0;

		public Builder markBoss() {
			effectImmune = true;
			noTargetHealing = true;
			hasBossBar = true;
			return this;
		}

		public Builder damageFilter() {
			damageFilter = true;
			return this;
		}

		public Builder dynamicReduction() {
			return dynamicReduction(20);
		}

		public Builder dynamicReduction(float rate) {
			dynamicReductionRate = rate;
			return this;
		}

		public Builder trueDamageOnImmune() {
			trueDamageOnImmune = true;
			return this;
		}

		public Builder limit(int limit) {
			limiter = limit;
			return this;
		}

		public Builder noPlayerTime(int time) {
			noPlayerDiscardTime = time;
			return this;
		}

		public YoukaiFeatureSet build() {
			return new YoukaiFeatureSet(
					effectImmune, damageFilter,
					noTargetHealing, trueDamageOnImmune, hasBossBar,
					limiter,
					noPlayerDiscardTime, maxSpeed,
					dynamicReductionRate
			);
		}

	}

}