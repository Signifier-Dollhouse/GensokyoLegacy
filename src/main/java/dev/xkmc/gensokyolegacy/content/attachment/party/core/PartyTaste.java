package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * What sort of party one character is willing to attend, and how she behaves at one.
 * <p>
 * Read from the {@code party_taste} entity-type data map. <b>A character with no
 * entry does not come to parties at all</b> — {@link #of} returns null and she is
 * never a candidate. Being a character is not enough; she has to be written down.
 * That makes the data map the eligibility list as well as the tuning knob, so a
 * datapack author decides who shows up by writing a row, and a newly registered
 * character is absent until someone writes one rather than appearing on the
 * default terms.
 * <p>
 * The last three fields are her <i>manners</i> rather than her appetite: how long she
 * sits between dishes while she is still hungry and once she is not, and how likely
 * she is to call it a day afterwards. They live here rather than as constants because a
 * character who is a great glutton plausibly dawdles, and one who nibbles plausibly
 * does not — and because the leave chance has to exist at all, or a guest who is already
 * full has no reason ever to go home.
 *
 * @param weight        her share of the guest draw at full interest. 100 is
 *                      ordinary; 250 makes her twice as likely to be drawn as an
 *                      ordinary guest, 0 means she is never drawn however much she
 *                      likes the spread
 * @param minReputation below this she declines, whatever is on the table
 * @param maxReputation reputation at which the reputation bonus is fully earned
 * @param repBonus      extra share at {@code maxReputation}; 0.5 means up to 1.5x
 * @param minAppeal     spread appeal below which she is not interested at all
 * @param maxAppeal     spread appeal at which she is fully interested
 * @param fulfillment   hunger she must be served before she counts as satisfied
 * @param hungrySeat    ticks she sits at her chair between dishes while still
 *                      hungry — she is back for the next course
 * @param fullSeat      ticks she sits between dishes once satisfied — she is only
 *                      grazing, and takes her time over it
 * @param leaveChance   chance, per {@code fullSeat} spent, that she leaves after
 *                      coming to her senses. The reason a party ends at all: a guest
 *                      who is already full would otherwise eat until the food is gone
 */
public record PartyTaste(
		int weight,
		int minReputation, int maxReputation, double repBonus,
		int minAppeal, int maxAppeal, int fulfillment,
		int hungrySeat, int fullSeat, double leaveChance
) {

	public PartyTaste {
		weight = Math.max(0, weight);
		maxReputation = Math.max(maxReputation, minReputation);
		maxAppeal = Math.max(maxAppeal, minAppeal);
		repBonus = Math.max(0, repBonus);
		fulfillment = Math.max(1, fulfillment);
		// a sit of zero ticks would have a guest drawing, eating and drawing again
		// inside a single tick, which reads as teleporting rather than as a meal
		hungrySeat = Math.max(1, hungrySeat);
		fullSeat = Math.max(1, fullSeat);
		// clamped rather than trusted: a chance above 1 would leave her certain and
		// certain reads as a bug, and a negative one never fires at all
		leaveChance = Math.clamp(leaveChance, 0, 1);
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
