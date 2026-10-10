package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Decides who comes to a party: a weighted draw, one guest at a time, against a shared
 * hunger budget.
 * <p>
 * The budget is how much the spread actually <b>feeds</b> — hunger, not appeal — and each
 * guest costs her fulfillment in the same unit. The weight decides <em>who</em>, by making
 * a guest's chance of being drawn proportional to how much she wants to be there; it is
 * computed from the spread's appeal and has no other role here.
 * <p>
 * This replaced a 0/1 knapsack that maximised total weight. The knapsack was correct —
 * verified optimal against brute force on 200k random pools — but it was also perfectly
 * predictable, so a host learned the party by heart after one attempt. Two things are
 * better this way:
 * <ul>
 * <li>the seat count is a loop bound rather than a truncation bolted on at the end, so a
 * guest is never invited and then dropped;</li>
 * <li>the roster varies with the spread, so two parties are not the same party.</li>
 * </ul>
 * What is <b>not</b> better is prediction, and that is bought back explicitly: the draw is
 * seeded from the centre of the spread's food (see {@code PartySend.plan}), so clicking a
 * different table of one party draws the same party and {@code /gensokyo party preview}
 * still predicts a real send exactly.
 */
public class PartyPlanner {

	/**
	 * @param weight      {@link PartyMath#weight} against this player's reputation; the
	 *                    draw share, and nothing else
	 * @param fulfillment hunger she must be served before she counts as satisfied, and
	 *                    what she costs against the spread's capacity
	 */
	public record Candidate(EntityType<?> type, int weight, int fulfillment, int reputation) {
	}

	/**
	 * Picks the guests by repeated weighted draw, under both constraints at once.
	 * <p>
	 * Each round draws one guest with probability proportional to weight, spends her
	 * fulfillment from the shared budget, and drops everyone the remaining budget can no
	 * longer satisfy. That filter is what makes the capacity constraint real rather than
	 * incidental: a guest who was affordable when the round began may not be after it, so
	 * without the filter a late draw could leave the roster over budget.
	 * <p>
	 * Both constraints therefore hold by construction —
	 * {@code sum(fulfillment) <= capacity}, because nothing is drawn that does not fit the
	 * remainder, and {@code size() <= seats}, because the loop stops at the seat count.
	 *
	 * @param capacity how much hunger the spread provides; the guests' share of it is
	 *                 measured in the same unit as their own fulfillment
	 * @param rng      seeded so one spread always draws one party
	 * @return chosen characters, in the order they were drawn
	 */
	public static List<EntityType<?>> select(List<Candidate> pool, int capacity, int seats, Random rng) {
		if (seats <= 0 || capacity <= 0) return List.of();
		// weight 0 means she declines outright and must never be drawn. fulfillment >
		// capacity means she could never be satisfied even by eating everything, so
		// inviting her only produces a guest who leaves hungry.
		var viable = new ArrayList<>(pool.stream()
				.filter(e -> e.weight() > 0 && e.fulfillment() <= capacity)
				.toList());

		int remaining = capacity;
		var chosen = new ArrayList<EntityType<?>>(Math.min(seats, viable.size()));
		while (chosen.size() < seats && !viable.isEmpty()) {
			var guest = viable.remove(draw(viable, rng));
			chosen.add(guest.type());
			final int rest = remaining - guest.fulfillment();
			// anyone the rest of the budget can no longer satisfy is out of the running
			viable.removeIf(e -> e.fulfillment() > rest);
			remaining = rest;
		}
		return chosen;
	}

	/**
	 * One index of {@code viable}, drawn with probability proportional to weight.
	 * <p>
	 * The total is accumulated in {@code long}, not {@code int}: weights come from
	 * datapack and {@link PartyMath#weight} scales a {@code party_taste} weight by the
	 * appeal and reputation ramps, so a dozen of them can leave the int range. An overflow
	 * would wrap to a value of the wrong sign or magnitude and quietly skew every draw
	 * after it, which is the kind of bug that only shows up as "the roster looks odd".
	 */
	private static int draw(List<Candidate> viable, Random rng) {
		long total = 0;
		for (var c : viable) total += c.weight();
		if (total <= 0) return 0;   // unreachable: the pool is filtered to weight > 0
		long roll = rng.nextLong(total);
		for (int i = 0; i < viable.size(); i++) {
			roll -= viable.get(i).weight();
			if (roll < 0) return i;
		}
		return viable.size() - 1;   // unreachable: the roll was below the total
	}

	/** Why an eligible character did not make the cut. */
	public enum Miss {
		/** She wants more food than the whole spread provides. */
		TOO_PLAIN,
		/** She was willing and affordable, but the draw filled the seats first. */
		NO_SEAT,
		/** She was willing and affordable, but the roster came out empty anyway. */
		OTHER
	}

	/**
	 * The highest-weighted candidate that did not come, and why.
	 * <p>
	 * A player whose spread is one appeal short should be told which character they were
	 * short of, and whether to lay out more food or befriend someone, rather than just
	 * that nobody came.
	 * <p>
	 * Deliberately <b>not</b> a draw. This is a diagnostic, and an answer that changed
	 * from one press to the next would be worse than useless: it reports the best
	 * character still off the roster, which is the one whose absence explains the party the
	 * player actually got.
	 */
	@Nullable
	public static Map.Entry<Candidate, Miss> nearestMiss(List<Candidate> pool, int capacity,
			List<EntityType<?>> roster) {
		var viable = pool.stream()
				.filter(e -> e.weight() > 0 && !roster.contains(e.type()))
				.sorted(Comparator.comparingInt(Candidate::weight).reversed())
				.toList();
		if (viable.isEmpty()) return null;
		var best = viable.getFirst();
		return best.fulfillment() > capacity ? Map.entry(best, Miss.TOO_PLAIN) : Map.entry(best, Miss.NO_SEAT);
	}

	private PartyPlanner() {
	}

}