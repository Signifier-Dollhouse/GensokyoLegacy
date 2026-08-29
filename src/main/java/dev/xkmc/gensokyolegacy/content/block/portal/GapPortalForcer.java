package dev.xkmc.gensokyolegacy.content.block.portal;

import dev.xkmc.gensokyolegacy.content.attachment.gap.GapMapping;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.UUID;

public class GapPortalForcer {

	private GapPortalForcer() {}

	/** Complete a pending mapping by generating the missing side. Same logic for ENTRY and EXIT. */
	public static GapMapping completePending(GapMapping pending, PortalSide missingSide, ServerLevel sourceLevel) {
		PortalSide sourceSide = missingSide == PortalSide.EXIT ? PortalSide.ENTRY : PortalSide.EXIT;
		BlockPos sourcePos = pending.posAt(sourceSide);
		ResourceLocation sourceDim = pending.dimAt(sourceSide);
		if (sourcePos == null || sourceDim == null) return pending;
		ServerLevel sourceLvl = sourceLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, sourceDim));
		if (sourceLvl == null) sourceLvl = sourceLevel;
		boolean sourceInGap = sourceDim.equals(GLDimensionGen.GAP.location());
		ServerLevel targetLevel = sourceInGap ? sourceLevel.getServer().overworld()
				: sourceLevel.getServer().getLevel(GLDimensionGen.GAP);
		if (targetLevel == null) targetLevel = sourceLevel;
		ResourceLocation targetDim = targetLevel.dimension().location();
		if (!sourceInGap) targetDim = GLDimensionGen.GAP.location();
		BlockPos targetPos = scaledPos(sourcePos, sourceLvl, targetLevel);
		return pending.with(missingSide, targetPos, targetDim);
	}

	private static BlockPos scaledPos(BlockPos sourcePos, ServerLevel sourceLevel, ServerLevel targetLevel) {
		int y = scaleY(sourcePos.getY(), sourceLevel, targetLevel);
		return new BlockPos(sourcePos.getX(), y, sourcePos.getZ());
	}

	private static int scaleY(int y, ServerLevel source, ServerLevel target) {
		int sMin = source.getMinBuildHeight();
		int sMax = source.getMaxBuildHeight();
		int tMin = target.getMinBuildHeight();
		int tMax = target.getMaxBuildHeight();
		if (sMax <= sMin) return y;
		double ratio = (double) (y - sMin) / (sMax - sMin);
		ratio = Math.clamp(ratio, 0.0, 1.0);
		return (int) (ratio * (tMax - tMin)) + tMin;
	}

	/** Place a portal at pos with 3x2x3 free space and 3x3 obsidian ground. Does not replace obstructing blocks. */
	public static void placePortal(ServerLevelAccessor level, BlockPos pos, UUID id, PortalSide side) {
		if (level.getBlockEntity(pos) instanceof GapPortalBlockEntity) return;
		if (!canPlaceAt(level, pos)) return;
		// Create 3x3 ground using obsidian (replace non-obstructing blocks)
		BlockPos groundY = pos.below();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos p = groundY.offset(dx, 0, dz);
				BlockState state = level.getBlockState(p);
				if (state.isAir() || canReplaceForGround(state, level, p)) {
					level.setBlock(p, Blocks.OBSIDIAN.defaultBlockState(), 3);
				}
			}
		}
		// Clear 3x2x3 free space (keep portal blocks)
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = 0; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos p = pos.offset(dx, dy, dz);
					if (p.equals(pos) || p.equals(pos.above())) continue;
					BlockState state = level.getBlockState(p);
					if (state.isAir()) continue;
					if (!canReplaceForClear(state, level, p)) continue;
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		// Place portal blocks (check target pos itself is replaceable and not obstructing)
		BlockState atPos = level.getBlockState(pos);
		BlockState atAbove = level.getBlockState(pos.above());
		if (!canReplaceForPortal(atPos, level, pos) || !canReplaceForPortal(atAbove, level, pos.above())) {
			return;
		}
		level.setBlock(pos, GLBlocks.GAP_PORTAL.getDefaultState(), 3);
		level.setBlock(pos.above(), GLBlocks.GAP_PORTAL.getDefaultState().setValue(BlockStateProperties.HALF, Half.TOP), 3);
		if (level.getBlockEntity(pos) instanceof GapPortalBlockEntity be) {
			be.id = id;
			be.side = side;
			be.initData(side);
		}
		if (level instanceof Level l) {
			l.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	public static boolean canPlaceAt(ServerLevelAccessor level, BlockPos pos) {
		// 3x2x3 check: any obstructing block -> fail
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = 0; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos p = pos.offset(dx, dy, dz);
					BlockState state = level.getBlockState(p);
					if (isObstructing(state, level, p)) return false;
				}
			}
		}
		// 3x3 ground check: any non-full-cube obstructing block -> fail
		BlockPos groundY = pos.below();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos p = groundY.offset(dx, 0, dz);
				BlockState state = level.getBlockState(p);
				if (isObstructing(state, level, p) && !isFullCube(state, level, p)) return false;
			}
		}
		return true;
	}

	private static boolean canReplaceForGround(BlockState state, ServerLevelAccessor level, BlockPos pos) {
		if (state.isAir()) return true;
		if (isObstructing(state, level, pos)) return false;
		return true;
	}

	private static boolean canReplaceForClear(BlockState state, ServerLevelAccessor level, BlockPos pos) {
		if (state.isAir()) return false; // already air, no need
		if (isObstructing(state, level, pos)) return false;
		return true;
	}

	private static boolean canReplaceForPortal(BlockState state, ServerLevelAccessor level, BlockPos pos) {
		if (state.isAir()) return true;
		if (isObstructing(state, level, pos)) return false;
		return true;
	}

	private static boolean isObstructing(BlockState state, ServerLevelAccessor level, BlockPos pos) {
		if (state.getDestroySpeed(level, pos) < 0) return true;
		if (level.getBlockEntity(pos) != null) return true;
		return false;
	}

	private static boolean isFullCube(BlockState state, ServerLevelAccessor level, BlockPos pos) {
		return state.isCollisionShapeFullBlock(level, pos);
	}

}
