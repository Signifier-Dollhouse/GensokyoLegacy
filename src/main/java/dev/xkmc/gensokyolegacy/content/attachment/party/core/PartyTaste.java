package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * What sort of party one character will attend and how she behaves at one, from the
 * {@code party_taste} entity-type data map. A character with no row does not come at all.
 * @param weight        her share of the draw at full interest; 100 is ordinary, 0 means
 *                      she is never drawn
 * @param minReputation below this she declines, whatever is on the table
 * @param maxReputation reputation at which the reputation bonus is fully earned
 * @param repBonus      extra share at {@code maxReputation}; 0.5 means up to 1.5x
 * @param minAppeal     spread appeal below which she is not interested at all
 * @param maxAppeal     spread appeal at which she is fully interested
 * @param fulfillment   hunger she must be served before she counts as satisfied
 * @param hungrySeat    ticks she sits between dishes while still hungry
 * @param fullSeat      ticks she sits between dishes once satisfied
 * @param leaveChance   chance, per {@code fullSeat} spent, that she calls it a day
 * @param repGain       share of the appeal a guest consumed, paid to the host in
 *                      reputation with her; 0.05 means 5%
 * @see "doc/design/party/select.md"
 */
public record PartyTaste(
		int weight,
		int minReputation, int maxReputation, double repBonus,
		int minAppeal, int maxAppeal, int fulfillment,
		int hungrySeat, int fullSeat, double leaveChance,
		double repGain
) {

	public static final PartyTaste DEF = new PartyTaste(
			0, 0, 100, 0,
			0, 40, 20,
			60, 160, 0.05,
			0.05
	);

	public PartyTaste {
		weight = Math.max(0, weight);
		maxReputation = Math.max(maxReputation, minReputation);
		maxAppeal = Math.max(maxAppeal, minAppeal);
		repBonus = Math.max(0, repBonus);
		fulfillment = Math.max(1, fulfillment);
		// a zero-tick sit would have her draw, eat and draw again inside one tick
		hungrySeat = Math.max(1, hungrySeat);
		fullSeat = Math.max(1, fullSeat);
		// clamped rather than trusted: over 1 leaves her certain, under 0 never fires
		leaveChance = Math.clamp(leaveChance, 0, 1);
		repGain = Math.clamp(repGain, 0, 1);
	}

	/** The reputation this visit is worth the host, from the appeal <i>she</i> consumed. */
	public int reputationFor(int appeal) {
		if (appeal <= 0) return 0;
		return Math.max(1, (int) Math.floor(appeal * repGain));
	}

	/** This character's taste, or <b>null</b> when she has no authored row — she is then not
	 * party-eligible. Never fall back to {@link #DEF} here: that null is what makes the data map the
	 * eligibility list.
	 */
	@Nullable
	public static PartyTaste of(RegistryAccess access, EntityType<?> type) {
		return GLMeta.PARTY_TASTE.get(access, type.builtInRegistryHolder());
	}

	/** This character's taste, or {@link #DEF}, for reading behaviour mid-visit. */
	public static PartyTaste ofOrDefault(RegistryAccess access, EntityType<?> type) {
		var ans = of(access, type);
		return ans == null ? DEF : ans;
	}

}
