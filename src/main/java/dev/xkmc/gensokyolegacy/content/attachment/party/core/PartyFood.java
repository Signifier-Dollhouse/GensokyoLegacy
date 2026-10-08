package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An explicit appeal value for a block food, read from the {@code party_food}
 * block data map.
 * <p>
 * This is an <b>override only</b>. A block with no entry is scored from its own
 * {@link net.minecraft.world.food.FoodProperties} by
 * {@link PartyFoodValue#appeal}, so a datapack can make a foreign block party
 * food with one tag entry and still get a sensible number; the data map exists
 * for correcting a value the heuristic gets wrong, not for supplying it.
 *
 * @param appeal how attractive one block of this food is at the table
 */
public record PartyFood(int appeal) {

	public PartyFood {
		appeal = Math.max(0, appeal);
	}

	/** The override for this state, or null when there is none. */
	@Nullable
	public static PartyFood of(RegistryAccess access, BlockState state) {
		return GLMeta.PARTY_FOOD.get(access, state.getBlockHolder());
	}

}
