package dev.xkmc.gensokyolegacy.content.attachment.home.core;

import dev.xkmc.gensokyolegacy.content.block.deco.cabinet.CabinetBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.ChairEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.ISeatableBlock;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyPotBlockEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class HomeSearchUtil {

	public static boolean isValidChest(ServerLevel sl, BlockPos pos) {
		var be = sl.getBlockEntity(pos);
		return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof CabinetBlockEntity;
		//|| be instanceof BasketBlockEntity; TODO other container handling
	}

	public static boolean isValidChair(ServerLevel sl, BlockPos pos) {
		return sl.getBlockState(pos).getBlock() instanceof ISeatableBlock;
	}

	public static boolean isValidShelf(ServerLevel sl, BlockPos pos) {
		return sl.getBlockEntity(pos) instanceof ShelfBlockEntity;
	}

	public static boolean isValidPot(ServerLevel sl, BlockPos pos) {
		return sl.getBlockEntity(pos) instanceof AlchemyPotBlockEntity;
	}

	public static boolean isEmptyShelf(ServerLevel sl, BlockPos pos) {
		return sl.getBlockEntity(pos) instanceof ShelfBlockEntity be &&
				(be.stack.isEmpty() || be.stock <= 0);
	}

	public static void put(ServerLevel level, BlockPos chest, Function<Boolean, ItemStack> doCraft) {
		if (!isValidChest(level, chest)) return;
		var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, chest, Direction.UP);
		if (cap == null) {
			if (level.getBlockEntity(chest) instanceof BaseContainerBlockEntity cont) {
				cap = new InvWrapper(cont);
			} else return;
		}
		if (ItemHandlerHelper.insertItem(cap, doCraft.apply(true), true).isEmpty()) {
			ItemHandlerHelper.insertItem(cap, doCraft.apply(false), false);
		}
	}

	public static void setSitting(ServerLevel level, BlockPos pos, YoukaiEntity entity) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ISeatableBlock block) {
			List<ChairEntity> seats = level.getEntitiesOfClass(ChairEntity.class, new AABB(pos));
			if (seats.isEmpty()) {
				block.sitDown(level, pos, entity);
			} else {
				var seat = seats.getFirst();
				var e = seat.getPassengers();
				if (!e.isEmpty()) {
					if (e.getFirst() instanceof Player) return;
					if (e.getFirst() instanceof YoukaiEntity) return;
				}
				seat.ejectPassengers();
				entity.startRiding(seat);
			}
		}
	}

	@Nullable
	public static BlockPos searchKind(IBlockSearchCache data, HomeBlockKind kind, MultiStructureBound rooms, ServerLevel sl, BlockPos center) {
		BoundingBox box = BoundingBox.fromCorners(
				center.offset(-kind.rxz(), -kind.ry(), -kind.rxz()),
				center.offset(kind.rxz(), kind.ry(), kind.rxz()));
		var area = rooms.intersect(box);
		if (area.isEmpty()) return null;
		var rand = sl.getRandom();
		BlockSearchCache cache = data.cache(kind);
		List<BlockPos> list = cache.pos;
		long now = sl.getGameTime();
		if (now < cache.lastSearch + Math.max(list.size(), 1) * 100L) {
			// cooling down: probe a few cached entries instead of a full validation pass
			List<BlockPos> cands = new ArrayList<>();
			for (var e : list) {
				if (!sl.isLoaded(e)) continue;
				if (area.isInside(e)) cands.add(e);
			}
			for (int i = 0; i < 3 && !cands.isEmpty(); i++) {
				var e = cands.remove(rand.nextInt(cands.size()));
				if (kind.isValid(sl, e)) return e;
				list.remove(e);
			}
			return null;
		}
		var itr = list.iterator();
		while (itr.hasNext()) {
			var e = itr.next();
			if (!sl.isLoaded(e)) continue;
			if (!kind.isValid(sl, e)) {
				itr.remove();
			}
		}
		List<BlockPos> hits = new ArrayList<>();
		for (var e : list) {
			if (!sl.isLoaded(e)) continue;
			if (area.isInside(e)) hits.add(e);
		}
		cache.lastSearch = now;
		if (!hits.isEmpty()) {
			return hits.get(rand.nextInt(hits.size()));
		}
		if (kind.scanBlockEntities()) {
			// refill the cache from block entities of loaded chunks; never forces a chunk load
			for (var b : area.boxes()) {
				int x0 = SectionPos.blockToSectionCoord(b.minX());
				int x1 = SectionPos.blockToSectionCoord(b.maxX());
				int z0 = SectionPos.blockToSectionCoord(b.minZ());
				int z1 = SectionPos.blockToSectionCoord(b.maxZ());
				for (int cx = x0; cx <= x1; cx++) {
					for (int cz = z0; cz <= z1; cz++) {
						var chunk = sl.getChunkSource().getChunkNow(cx, cz);
						if (chunk == null) continue;
						for (BlockPos bePos : chunk.getBlockEntities().keySet()) {
							if (!area.isInside(bePos)) continue;
							if (!kind.isValid(sl, bePos)) continue;
							if (!list.contains(bePos)) list.add(bePos);
							hits.add(bePos);
						}
					}
				}
			}
			if (!hits.isEmpty()) return hits.get(rand.nextInt(hits.size()));
			return null;
		}
		var pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < kind.trial(); i++) {
			area.randomPos(rand, pos);
			if (kind.isValid(sl, pos)) {
				var ans = pos.immutable();
				list.add(ans);
				return ans;
			}
		}
		return null;
	}

	/**
	 * BFS from a seed position, collecting all connected blocks of the given kind.
	 * Two blocks count as connected when they are at most {@code radius} blocks
	 * apart on each axis. Used to find a whole group of adjacent shelves from one hit.
	 */
	public static List<BlockPos> collectConnected(ServerLevel sl, BlockPos seed, HomeBlockKind kind, int radius) {
		var ans = new ArrayList<BlockPos>();
		if (!sl.isLoaded(seed) || !kind.isValid(sl, seed)) return ans;
		var visited = new LinkedHashSet<BlockPos>();
		var queue = new ArrayDeque<BlockPos>();
		visited.add(seed);
		queue.add(seed);
		var pos = new BlockPos.MutableBlockPos();
		while (!queue.isEmpty() && ans.size() < 64) {
			var cur = queue.removeFirst();
			ans.add(cur);
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) continue;
						pos.set(cur.getX() + dx, cur.getY() + dy, cur.getZ() + dz);
						if (!sl.isLoaded(pos)) continue;
						if (!kind.isValid(sl, pos)) continue;
						var imm = pos.immutable();
						if (visited.add(imm)) queue.add(imm);
					}
				}
			}
		}
		return ans;
	}

	@Nullable
	public static Vec3 getRandomPos(BoundingBox bound, YoukaiEntity e, Predicate<BlockPos> pred) {
		return getRandomPos(MultiStructureBound.of(bound), e, pred);
	}

	@Nullable
	public static Vec3 getRandomPos(MultiStructureBound rooms, YoukaiEntity e, Predicate<BlockPos> pred) {
		if (rooms.isEmpty()) return null;
		var rand = e.getRandom();
		return RandomPos.generateRandomPos(e, () -> {
			var pos = new BlockPos.MutableBlockPos();
			rooms.randomPos(rand, pos);
			var ans = new BlockPos(pos);
			if (!e.getNavigation().isStableDestination(ans)) return null;
			ans = LandRandomPos.movePosUpOutOfSolid(e, ans);
			if (ans == null || !rooms.isInside(ans) || !pred.test(ans)) return null;
			return ans;
		});
	}

}
