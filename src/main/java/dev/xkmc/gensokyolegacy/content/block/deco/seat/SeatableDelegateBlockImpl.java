package dev.xkmc.gensokyolegacy.content.block.deco.seat;

import dev.xkmc.l2modularblock.core.DelegateBlockImpl;
import dev.xkmc.l2modularblock.type.BlockMethod;

public class SeatableDelegateBlockImpl extends DelegateBlockImpl implements ISeatableBlock {

	private final float offset;

	protected SeatableDelegateBlockImpl(Properties p, float offset, BlockMethod... impl) {
		super(p, impl);
		this.offset = offset;
	}

	@Override
	public float offset() {
		return offset;
	}

}
