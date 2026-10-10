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
 * @param repGain       share of the party's total consumed appeal that attending it
 *                      is worth, as reputation with her. 0.05 means 5% of what the
 *                      table was worth — the host's payment for the banquet, and the
 *                      one number here that rewards the host rather than the guest
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
		// a sit of zero ticks would have a guest drawing, eating and drawing again
		// inside a single tick, which reads as teleporting rather than as a meal
		hungrySeat = Math.max(1, hungrySeat);
		fullSeat = Math.max(1, fullSeat);
		// clamped rather than trusted: a chance above 1 would leave her certain and
		// certain reads as a bug, and a negative one never fires at all
		leaveChance = Math.clamp(leaveChance, 0, 1);
		// clamped for the same reason: a share above 1 would pay out more reputation
		// than the party was worth, which reads as a bug rather than as generosity
		repGain = Math.clamp(repGain, 0, 1);
	}

	/**
	 * The reputation this guest's visit is worth to the host.
	 * <p>
	 * <b>Always at least one, for anyone who ate at all.</b> The share is of what
	 * <i>she</i> consumed, which is small by construction: five percent of a guest's own
	 * three slices of cake is 0.3, and that floors to nothing. A rule that pays nothing
	 * for an ordinary party is not a rule the host can learn from, and the alternative —
	 * scaling {@code repGain} up until small parties pay — would make the number mean
	 * something different for every character. The floor costs nothing in practice,
	 * because {@code gainReputation} refuses to move a reputation that is already at its
	 * cap anyway, so it only ever speaks for a guest who ate.
	 *
	 * @param appeal the appeal <i>this guest</i> consumed, the same figure her gift is
	 *               sized on — a host is paid for feeding her, not for the size of the
	 *               table she sat at
	 */
	public int reputationFor(int appeal) {
		if (appeal <= 0) return 0;
		return Math.max(1, (int) Math.floor(appeal * repGain));
	}

	/**
	 * This character's taste, or <b>null</b> when she has no authored entry — which
	 * means she is not party-eligible and must not be invited.
	 * <p>
	 * Null rather than {@link #DEF}, and that is the whole eligibility rule: the data
	 * map is the list of who comes to parties, so a character nobody has written down is
	 * absent on purpose rather than appearing on default terms. The {@code null} check in
	 * {@code PartySend.plan} depends on it, and a fallback here makes that check dead
	 * code — every character joins the candidate pool, and an unwritten one then shows up
	 * in the wand and preview readouts with a weight of 0 and everybody else's numbers.
	 * <p>
	 * {@link #DEF} stays as the template to copy, which is all it is for: nothing reads
	 * it at runtime. {@link #ofOrDefault} exists for the one caller that needs a value
	 * for a character who may legitimately have no row.
	 */
	@Nullable
	public static PartyTaste of(RegistryAccess access, EntityType<?> type) {
		return GLMeta.PARTY_TASTE.get(access, type.builtInRegistryHolder());
	}

	/**
	 * This character's taste, or the {@link #DEF} template if she has no row.
	 * <p>
	 * For reading a guest's <i>behaviour</i> mid-visit rather than for deciding whether
	 * to invite her. A datapack can pull a row while a guest is at the table, and a
	 * guest who starts throwing a null because her tuning data was edited under her is a
	 * worse outcome than one eating on default numbers for the rest of her visit.
	 */
	public static PartyTaste ofOrDefault(RegistryAccess access, EntityType<?> type) {
		var ans = of(access, type);
		return ans == null ? DEF : ans;
	}

}
