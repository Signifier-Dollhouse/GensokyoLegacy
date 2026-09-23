package dev.xkmc.gensokyolegacy.content.attachment.home.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiPredicate;

/**
 * Purpose of a cached block position list in home data.
 * Used as key of the generic position cache map so all
 * chair / container / shelf lists share one auto-serialized field.
 * Each kind carries its own block validator.
 */
public enum HomeBlockKind {

	CONTAINER(HomeSearchUtil::isValidChest, 3, 3, 32, true),
	CHAIR(HomeSearchUtil::isValidChair, 3, 3, 12, false),
	SHELF(HomeSearchUtil::isValidShelf, 48, 16, 64, true),
	POT(HomeSearchUtil::isValidPot, 24, 8, 64, true);

	private final BiPredicate<ServerLevel, BlockPos> validator;
	private final int rxz, ry, trial;
	private final boolean blockEntityScan;

	HomeBlockKind(BiPredicate<ServerLevel, BlockPos> validator, int rxz, int ry, int trial, boolean blockEntityScan) {
		this.validator = validator;
		this.rxz = rxz;
		this.ry = ry;
		this.trial = trial;
		this.blockEntityScan = blockEntityScan;
	}

	public BiPredicate<ServerLevel, BlockPos> validator() {
		return validator;
	}

	public boolean isValid(ServerLevel level, BlockPos pos) {
		return validator.test(level, pos);
	}

	public int rxz() {
		return rxz;
	}

	public int ry() {
		return ry;
	}

	public int trial() {
		return trial;
	}

	/**
	 * Whether targets of this kind are block entities, so a cache miss can be
	 * refilled by scanning block entities of loaded chunks instead of blind sampling.
	 */
	public boolean scanBlockEntities() {
		return blockEntityScan;
	}

}
