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

/** One simulated party, held on the wand as a data component. Positions are stored rather than
 * counts, so the counts the overlay prints are a projection of them and the renderer reads the
 * same field.
 *
 * @param bound  the table the simulation was taken at
 * @param tables party tables the scan walked to
 * @param seats  seats it found
 * @param foods  food blocks it counted towards the spread
 * @param hunger how much the spread can feed, and the budget the planner spent
 * @param appeal how attractive it is, each distinct food counted once
 * @param guests who the draw chose, heaviest weight first
 * @see "doc/design/party/debug.md" */
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
			Codec.INT.optionalFieldOf("appeal", 0).forGetter(PartyWandData::appeal),
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().listOf()
					.optionalFieldOf("guests", List.of()).forGetter(PartyWandData::guests)
	).apply(instance, PartyWandData::new));

	/** The readout for a send that would be refused; the area's own numbers still ride along, because
	 * a refused table is exactly where the host needs to see what the scan counted.
	 *
	 * @param area what the scan walked to, or null when there was nothing to scan */
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