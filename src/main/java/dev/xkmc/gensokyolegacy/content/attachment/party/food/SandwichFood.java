package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import someassemblyrequired.block.SandwichBlock;

import java.util.List;

/**
 * A Some Assembly Required sandwich: the layers live in a block entity, so this is a container rather than
 * a state-derived handler. One placed sandwich is one indivisible serving.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class SandwichFood implements ContainerFoodHandler {

	@Override
	public boolean handles(Block block) {
		// the sandwiching station is a separate block class and is deliberately not covered
		return block instanceof SandwichBlock;
	}

	@Override
	public List<FoodResult> contents(BlockGetter level, BlockPos pos, BlockState state) {
		// 1.20.1 has no SANDWICH_CONTENTS component; the mod snapshots the block entity into the item's BlockEntityTag.
		var serving = SandwichBlock.createSandwich(level.getBlockEntity(pos));
		var food = Servings.of(serving);
		if (food == null) return List.of();   // a part-built stack is ingredients, not a sandwich
		return List.of(FoodResult.of(serving, state.getBlock(), 1, food, this::consume));
	}

	@Override
	public boolean isPutOut(Level level, BlockPos pos, BlockState state) {
		return PartyArea.touchingFurniture(level, pos);
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		// contents() priced the whole sandwich as one serve, so eating it clears the block rather than
		// working down the layers the way a player eats it
		level.destroyBlock(pos, false);
		return 0;
	}

}
