package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An explicit appeal value for a block food, read from the {@code party_food}
 * block data map.
 * <p>
 * This <b>revalues and cannot opt in</b>. A block with no entry is scored from its own
 * {@link net.minecraft.world.food.FoodProperties} by
 * {@link PartyFoodValue#appealPerServe}; a block with an entry has that number replaced.
 * Either way the block must already be party food — {@link PartyFoodValue#isFood} never
 * consults this map, so a row on a block no handler can serve does nothing at all.
 * <p>
 * The two jobs are not the same. Opting a foreign dish in is already handled in code, by
 * the {@code BlockItemFood} catch-all, which accepts any block whose item carries food
 * properties. What a datapack is for is the case code cannot know: a mod has balanced its
 * cake to be far better than its properties suggest, and the heuristic cannot see that.
 *
 * @param appeal how attractive one serving of this food is at the table
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
