package dev.xkmc.gensokyolegacy.content.entity.behavior.task.marisa;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class MarisaTaskUtil {

	/**
	 * Outdoor working area: 12 blocks around the house on x/z, 6 on y.
	 */
	public static BoundingBox outdoorBox(BoundingBox house) {
		return new BoundingBox(
				house.minX() - 12, house.minY() - 6, house.minZ() - 12,
				house.maxX() + 12, house.maxY() + 6, house.maxZ() + 12);
	}

	public static BlockPos randomPosIn(BoundingBox box, RandomSource rand) {
		return new BlockPos(
				rand.nextInt(box.minX(), box.maxX() + 1),
				rand.nextInt(box.minY(), box.maxY() + 1),
				rand.nextInt(box.minZ(), box.maxZ() + 1));
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
