package dev.xkmc.gensokyolegacy.content.attachment.home.core;

import dev.xkmc.gensokyolegacy.content.block.deco.cabinet.CabinetBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.ChairEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.ISeatableBlock;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.List;
import java.util.function.BiPredicate;
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
	public static BlockPos searchBlock(List<BlockPos> cache, BiPredicate<ServerLevel, BlockPos> validity, BoundingBox room, ServerLevel sl, BlockPos center, int rxz, int ry, int trail) {
		return searchBlock(cache, validity, MultiStructureBound.of(room), sl, center, rxz, ry, trail);
	}

	@Nullable
	public static BlockPos searchBlock(List<BlockPos> cache, BiPredicate<ServerLevel, BlockPos> validity, MultiStructureBound rooms, ServerLevel sl, BlockPos center, int rxz, int ry, int trail) {
		BoundingBox box = BoundingBox.fromCorners(center.offset(-rxz, -ry, -rxz), center.offset(rxz, ry, rxz));
		var area = rooms.intersect(box);
		if (area.isEmpty()) return null;
		cache.removeIf(e -> sl.isLoaded(e) && !validity.test(sl, e));
		for (var e : cache) {
			if (!sl.isLoaded(e)) continue;
			if (area.isInside(e)) {
				return e;
			}
		}
		var rand = sl.getRandom();
		var pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < trail; i++) {
			area.randomPos(rand, pos);
			if (validity.test(sl, pos)) {
				var ans = pos.immutable();
				cache.add(ans);
				return ans;
			}
		}
		return null;
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
