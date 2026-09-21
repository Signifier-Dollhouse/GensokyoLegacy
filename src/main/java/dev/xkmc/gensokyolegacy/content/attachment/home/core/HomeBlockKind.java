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

	CONTAINER(HomeSearchUtil::isValidChest),
	CHAIR(HomeSearchUtil::isValidChair),
	SHELF(HomeSearchUtil::isValidShelf);

	private final BiPredicate<ServerLevel, BlockPos> validator;

	HomeBlockKind(BiPredicate<ServerLevel, BlockPos> validator) {
		this.validator = validator;
	}

	public BiPredicate<ServerLevel, BlockPos> validator() {
		return validator;
	}

	public boolean isValid(ServerLevel level, BlockPos pos) {
		return validator.test(level, pos);
	}

}
