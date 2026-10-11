package dev.xkmc.gensokyolegacy.content.attachment.party.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

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
 * <p>
 * <b>Who would come is stored as names, not as a scored pool.</b> The readout says who
 * and stops there, so the weights and appetites that produced the draw are not kept on
 * the item to be thrown away — what used to be a forty-entry candidate list is a
 * half-dozen ids, and a wand in a chest is a smaller wand in a chest.
 *
 * @param bound       the table the simulation was taken at
 * @param tables      party tables the scan walked to
 * @param seats       seats it found
 * @param foods       food blocks it counted towards the spread
 * @param hunger      how much the spread can feed, and the budget the planner spent
 * @param appeal      how attractive the spread is, each food counted once — the figure
 *                    the roster was drawn against
 * @param guests      who the draw chose, heaviest weight first
 */
public record PartyWandData(
		BlockPos bound,
		List<BlockPos> tables,
		List<BlockPos> seats,
		List<BlockPos> foods,
		int hunger,
		int appeal,
		List<EntityType<?>> guests
) {

	public static final Codec<PartyWandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("bound").forGetter(PartyWandData::bound),
			BlockPos.CODEC.listOf().optionalFieldOf("tables", List.of()).forGetter(PartyWandData::tables),
			BlockPos.CODEC.listOf().optionalFieldOf("seats", List.of()).forGetter(PartyWandData::seats),
			BlockPos.CODEC.listOf().optionalFieldOf("foods", List.of()).forGetter(PartyWandData::foods),
			Codec.INT.optionalFieldOf("hunger", 0).forGetter(PartyWandData::hunger),
			// no default worth having: an old wand has no appeal field, and reporting 0
			// for a spread that plainly has some would be worse than reporting none
			Codec.INT.optionalFieldOf("appeal", 0).forGetter(PartyWandData::appeal),
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().listOf()
					.optionalFieldOf("guests", List.of()).forGetter(PartyWandData::guests)
	).apply(instance, PartyWandData::new));

	/**
	 * The readout for a send that would be refused.
	 *
	 * <p>
	 * The area's own numbers ride along on a refusal rather than being zeroed: a table
	 * the wand refused is exactly the case where the host needs to see what the scan
	 * counted, and a spread that fed nobody is far more informative at 40 than at 0.
	 *
	 * @param area what the scan walked to, or null when there was nothing to scan
	 */
	public static PartyWandData refused(BlockPos bound, @Nullable PartyArea area) {
		if (area == null) return new PartyWandData(bound, List.of(), List.of(), List.of(), 0, 0, List.of());
		return new PartyWandData(bound, area.tables(), area.seats(), area.food(),
				area.fulfillment(), area.appeal(), List.of());
	}

	/** Nobody would come, so there is no roster worth showing. */
	public boolean empty() {
		return guests.isEmpty();
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