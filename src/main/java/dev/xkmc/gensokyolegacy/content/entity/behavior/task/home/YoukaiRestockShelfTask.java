package dev.xkmc.gensokyolegacy.content.entity.behavior.task.home;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.attachment.index.BedRefData;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfBlockEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Walk to a shelf in the home structure and fill it with a random item
 * from the given tag, so players can buy it. Shelves are handled as a group:
 * starting from one found shelf, all shelves up to 2 blocks away are collected
 * by BFS, then one shelf is picked at random. Empty shelves are prioritized;
 * when every shelf is stocked, restock is skipped most of the time, with a
 * configurable chance (see morichikaReplaceChance) to replace a stocked
 * shelf with a new item. Shelves owned by other players are skipped while
 * they hold an item, stock, or earnings; fully empty ones are claimed.
 */
public class YoukaiRestockShelfTask<E extends SmartYoukaiEntity> extends AbstractHomeHolderTask<E> {

	private static final int NO_SHELF_DELAY = 60;
	private static final int SKIP_DELAY = 200;
	private static final int FILL_DELAY = 100;
	private static final int REPLACE_DELAY = 12000;

	private final TagKey<Item> tag;
	private final int restockDuration;

	private BlockPos shelf;
	private ItemStack offer = ItemStack.EMPTY;
	private int stock, cost;
	private boolean replace;

	private long walkEnd, restockEnd, nextRestock;

	public YoukaiRestockShelfTask(TagKey<Item> tag) {
		this(tag, 40);
	}

	public YoukaiRestockShelfTask(TagKey<Item> tag, int restockDuration) {
		super(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
		));
		this.tag = tag;
		this.restockDuration = restockDuration;
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
		if (level.getGameTime() < nextRestock) return false;
		if (!super.checkExtraStartConditions(level, entity)) return false;
		var bed = BedRefData.of(level, entity);
		if (bed.isEmpty() || bed.get().getBedPos() == null) return false;
		var seed = home.getBlockAround(HomeBlockKind.SHELF, bed.get().getBedPos());
		if (seed == null) {
			nextRestock = level.getGameTime() + NO_SHELF_DELAY;
			return false;
		}
		var group = HomeSearchUtil.collectConnected(level, seed, HomeBlockKind.SHELF, 2);
		var rand = level.getRandom();
		var usable = group.stream().filter(p -> isAvailableForRestock(level, p)).toList();
		var empty = usable.stream().filter(p -> HomeSearchUtil.isEmptyShelf(level, p)).toList();
		if (!empty.isEmpty()) {
			shelf = empty.get(rand.nextInt(empty.size()));
			replace = false;
		} else if (!usable.isEmpty() && rand.nextFloat() < GLModConfig.SERVER.morichikaReplaceChance.get()) {
			shelf = usable.get(rand.nextInt(usable.size()));
			replace = true;
		} else {
			nextRestock = level.getGameTime() + SKIP_DELAY;
			return false;
		}
		if (!pickOffer(level)) {
			shelf = null;
			return false;
		}
		return true;
	}

	/**
	 * Shelves placed by players keep their owner. A shelf owned by someone else is
	 * left alone while it still holds an item mark, remaining stock, or uncollected
	 * earnings. A fully empty player shelf may be claimed for the shop (see doRestock).
	 */
	private static boolean isAvailableForRestock(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof ShelfBlockEntity be)) return false;
		if (be.owner.equals(Util.NIL_UUID)) return true;
		return be.stack.isEmpty() && be.stock <= 0 && be.earning <= 0;
	}

	private boolean pickOffer(ServerLevel level) {
		var holders = level.registryAccess().registryOrThrow(Registries.ITEM)
				.getTag(tag).map(e -> e.stream().toList()).orElse(List.of());
		if (holders.isEmpty()) return false;
		var rand = level.getRandom();
		var holder = holders.get(rand.nextInt(holders.size()));
		offer = new ItemStack(holder.value());
		var data = GLMeta.MORICHIKA_OFFER.get(level.registryAccess(), holder);
		if (data == null) {
			stock = 1;
			cost = 1;
		} else {
			stock = data.rollStock(rand);
			cost = data.rollPrice(rand);
		}
		return true;
	}

	@Override
	protected void start(ServerLevel level, E entity, long gameTime) {
		BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(shelf, 1, 1));
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(shelf));
		walkEnd = gameTime + 200;
		restockEnd = 0;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
		if (!home.isValid()) return false;
		if (!HomeBlockKind.SHELF.isValid(level, shelf)) return false;
		if (restockEnd == 0) {
			if (entity.distanceToSqr(shelf.getCenter()) < 4) {
				restockEnd = gameTime + restockDuration;
				return true;
			}
			return gameTime < walkEnd;
		}
		if (restockEnd == gameTime) {
			doRestock(level);
			nextRestock = restockEnd + (replace ? REPLACE_DELAY : FILL_DELAY);
			BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
			BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
			return false;
		}
		return gameTime < restockEnd;
	}

	private void doRestock(ServerLevel level) {
		if (level.getBlockEntity(shelf) instanceof ShelfBlockEntity be &&
				(replace || be.stack.isEmpty() || be.stock <= 0)) {
			be.owner = Util.NIL_UUID;
			be.restock(offer.copyWithCount(1), stock, cost);
		}
	}

	@Override
	protected void stop(ServerLevel level, E entity, long gameTime) {
		walkEnd = 0;
		restockEnd = 0;
		shelf = null;
		replace = false;
		offer = ItemStack.EMPTY;
		super.stop(level, entity, gameTime);
	}

}
