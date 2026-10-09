package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * What sort of party one character is willing to attend.
 * <p>
 * Read from the {@code party_taste} entity-type data map. <b>A character with no
 * entry does not come to parties at all</b> — {@link #of} returns null and she is
 * never a candidate. Being a character is not enough; she has to be written down.
 * That makes the data map the eligibility list as well as the tuning knob, so a
 * datapack author decides who shows up by writing a row, and a newly registered
 * character is absent until someone writes one rather than appearing on the
 * default terms.
 *
 * @param weight        her weight at full interest — the value the knapsack
 *                      actually adds up. 100 is ordinary; 250 makes her twice as
 *                      worth a seat as an ordinary guest, 0 means she is never
 *                      picked however much she likes the spread
 * @param minReputation below this she declines, whatever is on the table
 * @param maxReputation reputation at which the reputation bonus is fully earned
 * @param repBonus      extra share at {@code maxReputation}; 0.5 means up to 1.5x
 * @param minAppeal     spread appeal below which she is not interested at all
 * @param maxAppeal     spread appeal at which she is fully interested
 * @param fulfillment   appeal she must be served before she counts as satisfied
 */
public record PartyTaste(
		int weight,
		int minReputation, int maxReputation, double repBonus,
		int minAppeal, int maxAppeal, int fulfillment
) {

	/**
	 * A template to copy, <b>not</b> a fallback. Nothing reads this at runtime any
	 * more; it is here so a datapack author has a concrete set of numbers to start
	 * from rather than seven fields to guess at.
	 * <p>
	 * Deliberately undemanding — no reputation floor, so a fresh acquaintance can
	 * still be fed — and sized to the appeal {@link PartyFoodValue} derives from real
	 * food properties, where a vanilla cake is worth about 8.
	 */
	public static final PartyTaste DEFAULT = new PartyTaste(100, 0, 200, 0.25, 0, 24, 8);

	public PartyTaste {
		weight = Math.max(0, weight);
		maxReputation = Math.max(maxReputation, minReputation);
		maxAppeal = Math.max(maxAppeal, minAppeal);
		repBonus = Math.max(0, repBonus);
		fulfillment = Math.max(1, fulfillment);
	}

	/**
	 * This character's taste, or <b>null</b> when she has no authored entry — which
	 * means she is not party-eligible and must not be invited.
	 */
	@Nullable
	public static PartyTaste of(RegistryAccess access, EntityType<?> type) {
		return GLMeta.PARTY_TASTE.get(access, type.builtInRegistryHolder());
	}

}
