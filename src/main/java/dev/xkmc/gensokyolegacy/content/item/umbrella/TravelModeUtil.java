package dev.xkmc.gensokyolegacy.content.item.umbrella;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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

	private static Vec3 findSafeVertical(ServerLevel level, BlockPos pos) {
		int min = level.getMinBuildHeight();
		int max = level.getMaxBuildHeight() - 1;
		if (isFitting(level, pos)) return Vec3.atBottomCenterOf(pos);
		BlockState state = level.getBlockState(pos);
		boolean isAir = state.isAir();
		if (isAir) {
			// teleporting to air: move down to first block so player stands on a block
			for (int y = pos.getY() - 1; y >= min; y--) {
				BlockPos cand = new BlockPos(pos.getX(), y, pos.getZ());
				if (isFitting(level, cand)) return Vec3.atBottomCenterOf(cand);
			}
			// fallback: search up if no ground below
			for (int y = pos.getY() + 1; y <= max; y++) {
				BlockPos cand = new BlockPos(pos.getX(), y, pos.getZ());
				if (isFitting(level, cand)) return Vec3.atBottomCenterOf(cand);
			}
		} else {
			// teleporting to solid space: move up to first 2-block space so that player stands on block
			for (int y = pos.getY() + 1; y <= max; y++) {
				BlockPos cand = new BlockPos(pos.getX(), y, pos.getZ());
				if (isFitting(level, cand)) return Vec3.atBottomCenterOf(cand);
			}
			// fallback: search down if no space above
			for (int y = pos.getY() - 1; y >= min; y--) {
				BlockPos cand = new BlockPos(pos.getX(), y, pos.getZ());
				if (isFitting(level, cand)) return Vec3.atBottomCenterOf(cand);
			}
		}
		return null;
	}

	private static boolean isFitting(ServerLevel level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		BlockState cur = level.getBlockState(pos);
		BlockState above = level.getBlockState(pos.above());
		if (below.isAir() || !below.isSolidRender(level, pos.below())) return false;
		if (!cur.isAir() || !above.isAir()) return false;
		if (!cur.getFluidState().isEmpty() || !above.getFluidState().isEmpty()) return false;
		return true;
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
}
