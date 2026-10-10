package dev.xkmc.gensokyolegacy.content.attachment.party.core;

/**
 * The attractiveness / fulfillment math behind a party. Pure functions over
 * plain values, so {@code /gensokyo party preview} can run them and be
 * guaranteed to predict what a real send will do.
 */
public class PartyMath {

	public static int weight(PartyTaste taste, int spreadAppeal, int reputation) {
		if (reputation < taste.minReputation()) return 0;
		double appealFactor = ramp(spreadAppeal, taste.minAppeal(), taste.maxAppeal());
		double repFactor = 1.0 + taste.repBonus()
				* ramp(reputation, taste.minReputation(), taste.maxReputation());
		return Math.max(1, (int) Math.round(taste.weight() * appealFactor * repFactor));
	}

	/**
	 * 0 at or below {@code lo}, 1 at or above {@code hi}, linear in between.
	 */
	public static double ramp(int value, int lo, int hi) {
		if (value >= hi) return 1.0;
		if (value <= lo || hi <= lo) return 0.0;
		return (value - lo) / (double) (hi - lo);
	}

	private PartyMath() {
	}

}
