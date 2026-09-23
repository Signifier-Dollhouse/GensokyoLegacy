package dev.xkmc.gensokyolegacy.content.entity.behavior.task.marisa;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;

public class MarisaTaskUtil {

	private static final int MAX_SURFACE_UP = 8;
	private static final int MAX_SURFACE_DOWN = 48;

	/**
	 * Outdoor working area: 12 blocks around the house on x/z, 6 on y.
	 */
	public static BoundingBox outdoorBox(BoundingBox house) {
		return new BoundingBox(
				house.minX() - 12, house.minY() - 6, house.minZ() - 12,
				house.maxX() + 12, house.maxY() + 6, house.maxZ() + 12);
	}

	/**
	 * Surface sampling for outdoor chores: pick a random x/z column in the area,
	 * start from the {@code MOTION_BLOCKING_NO_LEAVES} heightmap (which sees through
	 * leaves), climb out when buried in solid ground, then sink through air, leaves,
	 * trunks and mushroom caps down to the actual surface. Returns null when the
	 * column is not loaded.
	 */
	@Nullable
	public static BlockPos randomSurfacePos(ServerLevel level, BoundingBox area, RandomSource rand) {
		int x = rand.nextInt(area.minX(), area.maxX() + 1);
		int z = rand.nextInt(area.minZ(), area.maxZ() + 1);
		if (!level.isLoaded(new BlockPos(x, area.minY(), z))) return null;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
				x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		for (int i = 0; i < MAX_SURFACE_UP && pos.getY() < level.getMaxBuildHeight(); i++) {
			BlockState state = level.getBlockState(pos);
			if (!state.isSolidRender(level, pos) || isCanopy(state)) break;
			pos.move(Direction.UP);
		}
		for (int i = 0; i < MAX_SURFACE_DOWN && pos.getY() > level.getMinBuildHeight(); i++) {
			if (pos.getY() >= level.getMaxBuildHeight()) {
				pos.move(Direction.DOWN);
				continue;
			}
			if (!isCanopy(level.getBlockState(pos))) break;
			pos.move(Direction.DOWN);
		}
		if (!level.isLoaded(pos)) return null;
		return pos.immutable();
	}

	/**
	 * Canopy the surface scan sinks through: air, leaves, logs (including template
	 * trunks) and huge mushroom stems/caps. Small mushrooms are forage targets and
	 * do not count.
	 */
	private static boolean isCanopy(BlockState state) {
		if (state.isAir()) return true;
		if (state.is(BlockTags.LEAVES)) return true;
		if (state.is(BlockTags.LOGS) || state.is(GLTagGen.TEMPLATE_TRUNK)) return true;
		return state.getBlock() instanceof HugeMushroomBlock;
	}

	/**
	 * Insert a stack into a home chest. Drops the remainder above the drop position
	 * when the chest is missing, invalid, or full, so items are never voided.
	 */
	public static void insertIntoChest(ServerLevel level, BlockPos chest, ItemStack stack, BlockPos dropPos) {
		if (stack.isEmpty()) return;
		if (chest != null && HomeSearchUtil.isValidChest(level, chest)) {
			var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, chest, Direction.UP);
			if (cap == null && level.getBlockEntity(chest) instanceof BaseContainerBlockEntity cont) {
				cap = new InvWrapper(cont);
			}
			if (cap != null) {
				ItemStack rest = ItemHandlerHelper.insertItem(cap, stack.copy(), false);
				if (!rest.isEmpty()) {
					Block.popResource(level, dropPos, rest);
				}
				return;
			}
		}
		Block.popResource(level, dropPos, stack.copy());
	}

}
