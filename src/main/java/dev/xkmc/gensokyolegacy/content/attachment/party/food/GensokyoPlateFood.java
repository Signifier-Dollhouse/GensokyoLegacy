package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.gensokyolegacy.content.block.functional.plate.PlateBlockEntity;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * This mod's own plate: one food item sitting in a block entity, so it is a container rather than a
 * state-described dish. A plate holds exactly one item, so one plate is one indivisible serve and
 * eating it clears the plate rather than working the plate away. The plate itself stays standing.
 * <p>Not {@link PlateFood}, which is Kaleidoscope Cookery's plate block and counts servings on the
 * block state instead.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class GensokyoPlateFood implements ContainerFoodHandler {

	@Override
	public boolean handles(Block block) {
		// a DelegateBlock has no class to test for, so identity is the only way to name it
		return block == GLBlocks.PLATE.get();
	}

	@Override
	public List<FoodResult> contents(CollisionGetter level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof PlateBlockEntity be)) return List.of();
		// an empty plate, and anything a guest would refuse, is not part of the spread. The plate only
		// ever accepts food with no container, so this is nearly always the empty case.
		var props = Servings.of(be.stack);
		if (props == null) return List.of();
		return List.of(FoodResult.of(be.stack.copyWithCount(1), state.getBlock(), 1, props, this::consume));
	}

	/**
	 * Only a plate put out on the furniture is party food, the same test every other dish uses.
	 */
	@Override
	public boolean isPutOut(Level level, BlockPos pos, BlockState state) {
		return PartyArea.touchingFurniture(level, pos);
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof PlateBlockEntity be) || be.stack.isEmpty()) return 0;
		be.clear();
		return 0;
	}

}
