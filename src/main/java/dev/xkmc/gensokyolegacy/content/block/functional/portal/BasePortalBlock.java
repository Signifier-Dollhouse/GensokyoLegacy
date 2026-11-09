// Source recreated from a .class file by IntelliJ IDEA (Fernflower decompiler).

package dev.xkmc.gensokyolegacy.content.block.functional.portal;

import dev.xkmc.l2modularblock.DelegateEntityBlockImpl;
import dev.xkmc.l2modularblock.one.EntityInsideBlockMethod;
import dev.xkmc.l2modularblock.one.RenderShapeBlockMethod;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class BasePortalBlock extends DelegateEntityBlockImpl {

	public record PortalMethod() implements EntityInsideBlockMethod, RenderShapeBlockMethod {

		private static final String TICK = "gensokyolegacy:portal_tick";
		private static final String TIME = "gensokyolegacy:portal_time";

		public void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
			if (!(level instanceof ServerLevel sl) || e.isPassenger() || e.isOnPortalCooldown()) return;
			if (!(level.getBlockEntity(pos) instanceof IPortalBlockEntity be)) return;
			var data = e.getPersistentData();
			long now = level.getGameTime();
			long last = data.getLong(TICK);
			if (last == now) return;
			int time = last == now - 1 ? data.getInt(TIME) : 0;
			data.putLong(TICK, now);
			if (time < e.getPortalWaitTime()) {
				data.putInt(TIME, time + 1);
				return;
			}
			PortalDestination dest = be.getPortalDestination(sl, e, pos);
			if (dest == null) return;
			data.remove(TIME);
			data.remove(TICK);
			e.setPortalCooldown();
			dest.teleport(e);
		}

		@Override
		public RenderShape getRenderShape(BlockState p_49232_) {
			return RenderShape.ENTITYBLOCK_ANIMATED;
		}

	}

	protected BasePortalBlock(Properties p, BlockMethod... impl) {
		super(p, impl);
	}

}
