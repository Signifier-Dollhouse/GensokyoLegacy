package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Who comes: a weighted draw under a shared hunger budget, with the seat count as a loop
 * bound. Prediction is bought back by seeding the draw from the centre of the spread's
 * food ({@code PartySend.plan}), so preview predicts a send exactly.
 *
 * @see doc/design/party/select.md
 */
public class PartyPlanner {

	/**
	 * @param weight      {@link PartyMath#weight}; her share of the draw and nothing else.
	 *                    0 means never drawn
	 * @param fulfillment what she costs against the spread's capacity, in the same unit
	 */
	public record Candidate(EntityType<?> type, int weight, int fulfillment, int reputation) {
	}

	/**
	 * Draws one guest at a time, spending her fulfillment from the shared budget and
	 * dropping whoever the remainder can no longer satisfy — so both constraints hold by
	 * construction: {@code sum(fulfillment) <= capacity} and {@code size() <= seats}.
	 *
	 * @param capacity the hunger the spread provides
	 * @param rng      seeded so one spread always draws one party
	 * @return chosen characters, in the order they were drawn
	 */
	public static List<EntityType<?>> select(List<Candidate> pool, int capacity, int seats, Random rng) {
		if (seats <= 0 || capacity <= 0) return List.of();
		// weight 0 declines outright; beyond capacity she could never be satisfied at all
		var viable = new ArrayList<>(pool.stream()
				.filter(e -> e.weight() > 0 && e.fulfillment() <= capacity)
				.toList());

		int remaining = capacity;
		var chosen = new ArrayList<EntityType<?>>(Math.min(seats, viable.size()));
		while (chosen.size() < seats && !viable.isEmpty()) {
			var guest = viable.remove(draw(viable, rng));
			chosen.add(guest.type());
			final int rest = remaining - guest.fulfillment();
			// re-run the budget filter every round; what was affordable may no longer be
			viable.removeIf(e -> e.fulfillment() > rest);
			remaining = rest;
		}
		return chosen;
	}

	/**
	 * One index of {@code viable}, drawn with probability proportional to weight. The total
	 * is accumulated in {@code long}: scaled datapack weights can leave the int range, and
	 * an overflow would quietly skew every later draw.
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
	 * The highest-weighted candidate that did not come, and why — so the host is told which
	 * character she was short of. Deliberately not a draw: it must answer the same twice.
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