package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Decides who comes to a party: a 0/1 knapsack over the spread's appeal.
 * <p>
 * The budget is the appeal the player laid out, each candidate costs her
 * fulfillment and is worth her weight, so maximising total weight subject to
 * total cost picks the best guest set the food can actually support. That is what
 * lets both requirements hold at once — how much food you provide sets the
 * budget, who you are on good terms with sets the weights.
 * <p>
 * Pure and deterministic: {@code /gensokyo party preview} calls this exact
 * method and is guaranteed to predict a real send.
 */
public class PartyPlanner {

	/** Float-tie tolerance, so a tie resolves the same way on every run. */
	private static final double EPS = 1e-9;

	/**
	 * @param weight      {@link PartyMath#weight} against this player's reputation
	 * @param fulfillment appeal she needs before she counts as satisfied
	 */
	public record Candidate(EntityType<?> type, double weight, int fulfillment, int reputation) {
	}

	/**
	 * Picks the guests. Maximises total weight subject to
	 * {@code sum(fulfillment) <= appeal}, then truncates to the seats available.
	 *
	 * @return chosen characters, highest weight first
	 */
	public static List<EntityType<?>> select(List<Candidate> pool, int appeal, int seats) {
		if (seats <= 0 || appeal <= 0) return List.of();
		// Two filters before the DP. weight 0 means she declines outright and must
		// not occupy budget or appear in the roster. fulfillment > appeal means she
		// could never be satisfied even by eating everything, so inviting her only
		// produces a guest who leaves hungry.
		var items = pool.stream()
				.filter(e -> e.weight() > 0 && e.fulfillment() <= appeal)
				.sorted(Comparator.comparingDouble(Candidate::weight).reversed())
				.toList();

		int n = items.size();
		double[] best = new double[appeal + 1];
		boolean[][] took = new boolean[n][appeal + 1];

		for (int i = 0; i < n; i++) {
			int cost = items.get(i).fulfillment();
			// descending a => 0/1, each character can be invited at most once
			for (int a = appeal; a >= cost; a--) {
				double cand = best[a - cost] + items.get(i).weight();
				if (cand > best[a] + EPS) {
					best[a] = cand;
					took[i][a] = true;
				}
			}
		}

		// Walk the decisions back out. This must run BACKWARDS: took[i][a] records
		// whether item i improved best[a] using items 0..i, so the chain is only
		// coherent from the last item down. A forward walk compiles and runs, and
		// quietly returns a roster one or two characters short of optimal.
		var chosen = new ArrayList<EntityType<?>>();
		for (int i = n - 1, a = appeal; i >= 0; i--) {
			if (took[i][a]) {
				chosen.add(items.get(i).type());
				a -= items.get(i).fulfillment();
			}
		}
		// items came in weight-descending, so undo the backwards walk's reversal
		Collections.reverse(chosen);
		// A heavier guest is always at least as good a use of a seat as a lighter
		// one, so truncating the weight-ordered list is the seat cap.
		return chosen.stream().limit(seats).toList();
	}

	/** Why an eligible character did not make the cut. */
	public enum Miss {
		/** She wants more food than the whole spread provides. */
		TOO_PLAIN,
		/** She is willing, but every seat is taken by someone she outranks. */
		NO_SEAT,
		/** She is willing and affordable, but the roster came out empty anyway. */
		OTHER
	}

	/**
	 * The highest-weighted candidate that did not come, and why.
	 * <p>
	 * A player whose spread is one appeal short should be told which character
	 * they were short of, and whether to lay out more food or befriend someone,
	 * rather than just that nobody came.
	 */
	@Nullable
	public static Map.Entry<Candidate, Miss> nearestMiss(List<Candidate> pool, int appeal) {
		var viable = pool.stream()
				.filter(e -> e.weight() > 0)
				.sorted(Comparator.comparingDouble(Candidate::weight).reversed())
				.toList();
		if (viable.isEmpty()) return null;
		var best = viable.getFirst();
		if (best.fulfillment() > appeal) return Map.entry(best, Miss.TOO_PLAIN);
		for (var e : viable) {
			if (e.fulfillment() > appeal) continue;
			// affordable and willing, so only the seat cap can have excluded her
			return Map.entry(e, Miss.NO_SEAT);
		}
		return Map.entry(best, Miss.OTHER);
	}

	private PartyPlanner() {
	}

}
