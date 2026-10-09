package dev.xkmc.gensokyolegacy.content.attachment.party.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * What one simulated party looked like, as stored on the wand.
 * <p>
 * Kept on the item as a data component rather than pushed to the client per tick, so
 * vanilla's own item synchronisation carries it: the server plans once on a table click,
 * writes the answer here, and the overlay reads it back whenever the wand is held. The
 * renderer reads the same component, so the counts in the text and the boxes in the world
 * are the same numbers read twice rather than two copies that can drift apart.
 * <p>
 * The positions are stored rather than counted. A count alone cannot say <em>where</em>,
 * and "how many chairs" is {@code chairs().size()}.
 * <p>
 * A plain record with a codec rather than a live handle on a party — this describes a
 * party that was <em>simulated</em>, and by the time it is read the food may have been
 * eaten and the seats taken. {@link PartyWand} shows the distance from the player so a
 * stale reading is visibly stale.
 *
 * @param bound       the table the simulation was taken at
 * @param tables      party tables the scan walked to
 * @param seats       seats it found
 * @param foods       food blocks it counted towards the spread
 * @param appeal      the spread's total attraction, which is also the guests' budget
 * @param fulfillment total fulfillment the chosen roster needs, i.e. budget actually spent
 * @param candidates  every eligible character and the weight the planner gave her
 */
public record PartyWandData(
		BlockPos bound,
		List<BlockPos> tables,
		List<BlockPos> seats,
		List<BlockPos> foods,
		int appeal,
		int fulfillment,
		List<Candidate> candidates
) {

	/**
	 * One character's line in the readout.
	 *
	 * @param invited whether the knapsack actually chose her
	 */
	public record Candidate(EntityType<?> type, int weight, int fulfillment, int reputation, boolean invited) {

		public static final Codec<Candidate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(Candidate::type),
				// was a double until the weight became an integer; a wand saved with the
				// old field no longer decodes and simply loses its readout until re-scanned
				Codec.INT.optionalFieldOf("weight", 0).forGetter(Candidate::weight),
				Codec.INT.optionalFieldOf("fulfillment", 0).forGetter(Candidate::fulfillment),
				Codec.INT.optionalFieldOf("reputation", 0).forGetter(Candidate::reputation),
				Codec.BOOL.optionalFieldOf("invited", false).forGetter(Candidate::invited)
		).apply(instance, Candidate::new));

		public Candidate(EntityType<?> type, int weight, int fulfillment, int reputation) {
			this(type, weight, fulfillment, reputation, false);
		}

		/** Characters that declined, or were left out for want of budget or seats. */
		public boolean declined() {
			return weight <= 0;
		}
	}

	public static final Codec<PartyWandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("bound").forGetter(PartyWandData::bound),
			BlockPos.CODEC.listOf().optionalFieldOf("tables", List.of()).forGetter(PartyWandData::tables),
			BlockPos.CODEC.listOf().optionalFieldOf("seats", List.of()).forGetter(PartyWandData::seats),
			BlockPos.CODEC.listOf().optionalFieldOf("foods", List.of()).forGetter(PartyWandData::foods),
			Codec.INT.optionalFieldOf("appeal", 0).forGetter(PartyWandData::appeal),
			Codec.INT.optionalFieldOf("fulfillment", 0).forGetter(PartyWandData::fulfillment),
			Candidate.CODEC.listOf().optionalFieldOf("candidates", List.of()).forGetter(PartyWandData::candidates)
	).apply(instance, PartyWandData::new));

	/**
	 * The whole readout, for the case where a send would be refused outright.
	 * <p>
	 * The area still rides along on a refusal: a table the wand refused is exactly the
	 * case where the host needs to see what the scan did and did not take.
	 */
	public static PartyWandData refused(BlockPos bound, List<BlockPos> tables, List<BlockPos> seats,
			List<BlockPos> foods) {
		return new PartyWandData(bound, tables, seats, foods, 0, 0, List.of());
	}

	/** Nobody would come, so there is no candidate list worth showing. */
	public boolean empty() {
		return candidates.isEmpty();
	}

	/** How many characters the planner actually invited. */
	public long invited() {
		return candidates.stream().filter(Candidate::invited).count();
	}

	/** Every block drawn, in one list, so the extent box needs no second copy kept in step. */
	public List<BlockPos> drawn() {
		if (tables.isEmpty() && seats.isEmpty() && foods.isEmpty()) return List.of();
		var all = new ArrayList<BlockPos>(tables.size() + seats.size() + foods.size());
		all.addAll(tables);
		all.addAll(seats);
		all.addAll(foods);
		return all;
	}

}