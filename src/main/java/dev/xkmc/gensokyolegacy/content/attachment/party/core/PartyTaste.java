package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;

/**
 * What sort of party one character is willing to attend.
 * <p>
 * Read from the {@code party_taste} entity-type data map, falling back to
 * {@link #DEFAULT} when a character has no entry. <b>Every</b> character is
 * party-eligible; the data map only makes one pickier or more generous than the
 * default.
 *
 * @param minReputation below this she declines, whatever is on the table
 * @param maxReputation reputation at which the reputation bonus is fully earned
 * @param repBonus      extra weight at {@code maxReputation}; 0.5 means up to 1.5x
 * @param minAppeal     spread appeal below which she is not interested at all
 * @param maxAppeal     spread appeal at which she is fully interested
 * @param fulfillment   appeal she must be served before she counts as satisfied
 */
public record PartyTaste(
		int minReputation, int maxReputation, double repBonus,
		int minAppeal, int maxAppeal, int fulfillment
) {

	/**
	 * Every character can be invited to a party; this is what one who has no
	 * authored {@code party_taste} entry gets. Deliberately undemanding — no
	 * reputation floor — so a fresh acquaintance can still be fed. The data map
	 * exists to make a character pickier or hungrier, not to keep anyone out.
	 * <p>
	 * Sized to the appeal {@link PartyFoodValue} derives from real food properties,
	 * where a vanilla cake is worth about 8.
	 */
	public static final PartyTaste DEFAULT = new PartyTaste(0, 200, 0.25, 0, 24, 8);

	public PartyTaste {
		maxReputation = Math.max(maxReputation, minReputation);
		maxAppeal = Math.max(maxAppeal, minAppeal);
		repBonus = Math.max(0, repBonus);
		fulfillment = Math.max(1, fulfillment);
	}

	/**
	 * This character's taste, or {@link #DEFAULT} when she has no authored entry.
	 * Every youkai is party-eligible.
	 */
	public static PartyTaste of(RegistryAccess access, EntityType<?> type) {
		var taste = GLMeta.PARTY_TASTE.get(access, type.builtInRegistryHolder());
		return taste == null ? DEFAULT : taste;
	}

}
