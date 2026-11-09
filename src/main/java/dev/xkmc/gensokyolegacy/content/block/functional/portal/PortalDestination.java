package dev.xkmc.gensokyolegacy.content.block.functional.portal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * 1.21's {@code DimensionTransition} plus its post-transition effects, which 1.20.1 splits between
 * {@link PortalInfo} and {@link ITeleporter}.
 */
public record PortalDestination(ServerLevel level, Vec3 pos, Vec3 speed, float yRot, float xRot) {

	public static PortalDestination of(ServerLevel level, Entity entity, Vec3 pos) {
		return new PortalDestination(level, pos, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot());
	}

	public PortalInfo info() {
		return new PortalInfo(pos, speed, yRot, xRot);
	}

	public Entity teleport(Entity entity) {
		return entity.changeDimension(level, new Teleporter(info()));
	}

	private record Teleporter(PortalInfo info) implements ITeleporter {

		@Override
		public @Nullable PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
												  Function<ServerLevel, PortalInfo> defaultPortalInfo) {
			return info;
		}

	}

}
