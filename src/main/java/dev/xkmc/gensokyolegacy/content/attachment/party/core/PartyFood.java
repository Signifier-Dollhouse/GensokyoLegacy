package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An explicit appeal value for a block food, from the {@code party_food} block data map.
 * <p>
 * Revalues and cannot opt in: {@link PartyFoodValue#isFood} never consults this map.
 *
 * @param appeal what one serving of this food is worth at the table
 * @see "doc/design/party/food.md"
 */
public record PartyFood(int appeal) {

	public PartyFood {
		appeal = Math.max(0, appeal);
	}

	/**
	 * The override for this state, or null when there is none.
	 */
	@Nullable
	public static PartyFood of(RegistryAccess access, BlockState state) {
		return GLMeta.PARTY_FOOD.get(access, state.getBlockHolder());
	}

}
