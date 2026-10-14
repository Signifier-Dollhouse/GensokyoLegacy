package dev.xkmc.gensokyolegacy.content.item.umbrella;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class TravelModeUtil {

	public static final int TRAVEL_DISTANCE = 1000;
	public static final int TRAVEL_MIN_TICKS = 10;

	public static Vec3 findSafePosition(ServerLevel level, BlockPos pos) {
		Vec3 safe = findSafeVertical(level, pos);
		if (safe != null) return safe;
		int radius = 3;
		for (int r = 1; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
					BlockPos neighbor = new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
					safe = findSafeVertical(level, neighbor);
					if (safe != null) return safe;
				}
			}
		}
		return Vec3.atBottomCenterOf(pos);
	}

	@Nullable
	private static Vec3 findSafeVertical(ServerLevel level, BlockPos pos) {
		int min = level.getMinBuildHeight();
		int max = level.getMaxBuildHeight() - 1;
		if (pos.getY() < min) pos = pos.atY(min);
		if (pos.getY() > max) pos = pos.atY(max);
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		// A column is judged by three yes/no questions, and stepping one block answers two from
		// the step before, so the walk carries that pair and reads one block per candidate.
		BlockState at = level.getBlockState(pos);
		boolean below = isSupport(level.getBlockState(pos.below()));
		boolean cur = isSupport(at);
		boolean above = isSupport(level.getBlockState(pos.above()));
		if (isFitting(below, cur, above)) return Vec3.atBottomCenterOf(pos);
		if (at.isAir()) {
			// teleporting to air: move down to first block so player stands on a block; lo/hi hold
			// the support answers for this column and the one above, so only the third is new
			boolean lo = below, hi = cur;
			for (int cy = y - 1; cy >= min; cy--) {
				boolean next = isSupport(level.getBlockState(new BlockPos(x, cy - 1, z)));
				if (isFitting(next, lo, hi)) return Vec3.atBottomCenterOf(new BlockPos(x, cy, z));
				hi = lo;
				lo = next;
			}
			// fallback: search up if no ground below
			boolean upLo = cur, upHi = above;
			for (int cy = y + 1; cy <= max; cy++) {
				boolean next = isSupport(level.getBlockState(new BlockPos(x, cy + 1, z)));
				if (isFitting(upLo, upHi, next)) return Vec3.atBottomCenterOf(new BlockPos(x, cy, z));
				upLo = upHi;
				upHi = next;
			}
		} else {
			// teleporting to solid space: move up to first 2-block space so that player stands on block
			boolean lo = cur, hi = above;
			for (int cy = y + 1; cy <= max; cy++) {
				boolean next = isSupport(level.getBlockState(new BlockPos(x, cy + 1, z)));
				if (isFitting(lo, hi, next)) return Vec3.atBottomCenterOf(new BlockPos(x, cy, z));
				lo = hi;
				hi = next;
			}
			// fallback: search down if no space above
			boolean downLo = below, downHi = cur;
			for (int cy = y - 1; cy >= min; cy--) {
				boolean next = isSupport(level.getBlockState(new BlockPos(x, cy - 1, z)));
				if (isFitting(next, downLo, downHi)) return Vec3.atBottomCenterOf(new BlockPos(x, cy, z));
				downHi = downLo;
				downLo = next;
			}
		}
		return null;
	}

	private static boolean isFitting(boolean below, boolean cur, boolean above) {
		return below && !cur && !above;
	}

	private static boolean isSupport(BlockState state) {
		return state.blocksMotion() || state.getFluidState().is(Tags.Fluids.WATER);
	}

	public static Vec3 adjustToFreeSpace(ServerLevel level, LivingEntity entity, Vec3 dst) {
		var dim = entity.getDimensions(Pose.STANDING);
		if (dim.width() * dim.width() * dim.height() > 64) return dst;
		Vec3 center = dst.add(0, dim.height() / 2.0, 0);
		double xz = Math.max(0, dim.width() - 1) + 1e-6;
		double y = Math.max(0, dim.height() - 1) + 1e-6;
		VoxelShape shape = Shapes.create(AABB.ofSize(center, xz, y, xz));
		var found = level.findFreePosition(entity, shape, center, dim.width(), dim.height(), dim.width());
		if (found.isPresent()) {
			return found.get().add(0, -dim.height() / 2.0, 0);
		}
		return dst;
	}

	public static void teleportPlayer(ServerPlayer sp, ServerLevel targetLevel, Vec3 dst) {
		ServerLevel cur = sp.serverLevel();
		if (cur != targetLevel) {
			sp.teleportTo(targetLevel, dst.x, dst.y, dst.z, Set.of(), sp.getYRot(), sp.getXRot());
		} else {
			sp.teleportTo(dst.x, dst.y, dst.z);
			sp.connection.resetPosition();
		}
	}

	public static void teleportPlayer(ServerPlayer sp, ServerLevel targetLevel, Vec3 dst, float yaw, float pitch) {
		sp.setYRot(yaw);
		sp.setXRot(pitch);
		sp.yHeadRot = yaw;
		sp.yBodyRot = yaw;
		teleportPlayer(sp, targetLevel, dst);
	}

	public static void restoreOrientation(LivingEntity entity, float yaw, float pitch) {
		entity.setYRot(yaw);
		entity.setXRot(pitch);
		entity.yHeadRot = yaw;
		entity.yBodyRot = yaw;
	}
}
