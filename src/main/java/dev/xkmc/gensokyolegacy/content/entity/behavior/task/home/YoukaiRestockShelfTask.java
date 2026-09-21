package dev.xkmc.gensokyolegacy.content.entity.behavior.task.home;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.attachment.index.BedRefData;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfBlockEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
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
 * when every shelf is stocked, restock is skipped at 90% chance, with 10%
 * chance to replace a stocked shelf with a new item.
 */
public class YoukaiRestockShelfTask<E extends SmartYoukaiEntity> extends AbstractHomeHolderTask<E> {

	private final TagKey<Item> tag;
	private final int minStock, maxStock, maxCost, cooldown, retryDelay;
	private final int restockDuration;

	private BlockPos shelf;
	private ItemStack offer = ItemStack.EMPTY;
	private int stock, cost;
	private boolean replace;

	private long walkEnd, restockEnd, nextRestock;

	public YoukaiRestockShelfTask(TagKey<Item> tag, int minStock, int maxStock, int maxCost, int cooldown) {
		this(tag, minStock, maxStock, maxCost, cooldown, 100, 40);
	}

	public YoukaiRestockShelfTask(TagKey<Item> tag, int minStock, int maxStock, int maxCost, int cooldown, int retryDelay, int restockDuration) {
		super(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
		));
		this.tag = tag;
		this.minStock = minStock;
		this.maxStock = maxStock;
		this.maxCost = maxCost;
		this.cooldown = cooldown;
		this.retryDelay = retryDelay;
		this.restockDuration = restockDuration;
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
		if (level.getGameTime() < nextRestock) return false;
		if (!super.checkExtraStartConditions(level, entity)) return false;
		var bed = BedRefData.of(level, entity);
		if (bed.isEmpty() || bed.get().getBedPos() == null) return false;
		var seed = home.getShelvesAround(bed.get().getBedPos());
		if (seed == null) {
			nextRestock = level.getGameTime() + retryDelay;
			return false;
		}
		var group = HomeSearchUtil.collectConnected(level, seed, HomeBlockKind.SHELF, 2);
		var rand = level.getRandom();
		var empty = group.stream().filter(p -> HomeSearchUtil.isEmptyShelf(level, p)).toList();
		if (!empty.isEmpty()) {
			shelf = empty.get(rand.nextInt(empty.size()));
			replace = false;
		} else if (!group.isEmpty() && rand.nextFloat() >= 0.9f) {
			shelf = group.get(rand.nextInt(group.size()));
			replace = true;
		} else {
			nextRestock = level.getGameTime() + retryDelay;
			return false;
		}
		if (!pickOffer(level)) {
			shelf = null;
			return false;
		}
		return true;
	}

	private boolean pickOffer(ServerLevel level) {
		var holders = level.registryAccess().registryOrThrow(Registries.ITEM)
				.getTag(tag).map(e -> e.stream().toList()).orElse(List.of());
		if (holders.isEmpty()) return false;
		var rand = level.getRandom();
		offer = new ItemStack(holders.get(rand.nextInt(holders.size())).value());
		stock = minStock + rand.nextInt(maxStock - minStock + 1);
		cost = 1 + rand.nextInt(maxCost);
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
			nextRestock = restockEnd + cooldown;
			BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
			BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
			return false;
		}
		return gameTime < restockEnd;
	}

	private void doRestock(ServerLevel level) {
		if (level.getBlockEntity(shelf) instanceof ShelfBlockEntity be &&
				(replace || be.stack.isEmpty() || be.stock <= 0)) {
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
