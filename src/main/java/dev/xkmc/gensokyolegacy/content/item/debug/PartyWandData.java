package dev.xkmc.gensokyolegacy.content.item.debug;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What one simulated party looked like, as stored on the wand.
 * <p>
 * Kept on the item as a data component rather than pushed to the client per tick, so
 * vanilla's own item synchronisation carries it: the server plans once on a table click,
 * writes the answer here, and the overlay reads it back whenever the wand is held.
 * <p>
 * A plain record with a codec rather than a live handle on a party — this describes a
 * party that was <em>simulated</em>, and by the time it is read the food may have been
 * eaten and the seats taken. {@link PartyWand} shows the distance from the player so a
 * stale reading is visibly stale.
 *
 * @param bound       the table the simulation was taken at
 * @param tables      party tables found in the scan box
 * @param seats       seats found
 * @param foods       food blocks found
 * @param appeal      the spread's total attraction, which is also the guests' budget
 * @param fulfillment total fulfillment the chosen roster needs, i.e. budget actually spent
 * @param candidates  every eligible character and the weight the planner gave her
 */
public record PartyWandData(
		BlockPos bound,
		int tables,
		int seats,
		int foods,
		int appeal,
		int fulfillment,
		List<Candidate> candidates
) {

	/**
	 * One character's line in the readout.
	 *
	 * @param invited whether the knapsack actually chose her
	 */
	public record Candidate(EntityType<?> type, double weight, int fulfillment, int reputation, boolean invited) {

		public static final Codec<Candidate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(Candidate::type),
				Codec.DOUBLE.optionalFieldOf("weight", 0.0d).forGetter(Candidate::weight),
				Codec.INT.optionalFieldOf("fulfillment", 0).forGetter(Candidate::fulfillment),
				Codec.INT.optionalFieldOf("reputation", 0).forGetter(Candidate::reputation),
				Codec.BOOL.optionalFieldOf("invited", false).forGetter(Candidate::invited)
		).apply(instance, Candidate::new));

		public Candidate(EntityType<?> type, double weight, int fulfillment, int reputation) {
			this(type, weight, fulfillment, reputation, false);
		}

		/** Characters that declined, or were left out for want of budget or seats. */
		public boolean declined() {
			return weight <= 0;
		}
	}

	public static final Codec<PartyWandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("bound").forGetter(PartyWandData::bound),
			Codec.INT.optionalFieldOf("tables", 0).forGetter(PartyWandData::tables),
			Codec.INT.optionalFieldOf("seats", 0).forGetter(PartyWandData::seats),
			Codec.INT.optionalFieldOf("foods", 0).forGetter(PartyWandData::foods),
			Codec.INT.optionalFieldOf("appeal", 0).forGetter(PartyWandData::appeal),
			Codec.INT.optionalFieldOf("fulfillment", 0).forGetter(PartyWandData::fulfillment),
			Candidate.CODEC.listOf().optionalFieldOf("candidates", List.of()).forGetter(PartyWandData::candidates)
	).apply(instance, PartyWandData::new));

	/** The whole readout, for the case where a send would be refused outright. */
	public static PartyWandData refused(BlockPos bound, int tables, int seats, int foods) {
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

	@Nullable
	public BlockPos bound() {
		return bound;
	}
}