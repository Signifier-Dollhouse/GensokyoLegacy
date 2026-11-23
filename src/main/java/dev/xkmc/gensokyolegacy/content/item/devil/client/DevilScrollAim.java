package dev.xkmc.gensokyolegacy.content.item.devil.client;

import dev.xkmc.gensokyolegacy.content.item.common.client.GloveTargetCache;
import dev.xkmc.gensokyolegacy.content.item.devil.DevilScrollItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the dash aim assist: turns the local camera onto the cached target so the player
 * sees the lock. The dash itself is bent server-side; this only makes the view agree with it.
 *
 * @see "doc/design/item/devils_scroll.md"
 */
public final class DevilScrollAim {

	private DevilScrollAim() {
	}

	/**
	 * Sneak lock driver: while the player sneaks with the scroll in the main hand, the view stays
	 * locked onto the cached target. Called from the client tick, so movement follows the lock, and
	 * again from {@code RenderFrameEvent.Pre} with the frame's partial tick, because the mouse turn
	 * is applied after the tick and the target renders at an interpolated position.
	 */
	public static void lock(float partialTick) {
		Player player = Minecraft.getInstance().player;
		if (player == null || !player.isShiftKeyDown()) return;
		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() instanceof DevilScrollItem) lockView(player, stack, partialTick);
	}

	/**
	 * Locks the view onto the cached target when the same gate the server uses allows it: within
	 * {@link DevilScrollItem#AIM_COS} of the current view, or any angle while sneaking.
	 */
	public static void lockView(Player player, ItemStack stack) {
		lockView(player, stack, 1.0F);
	}

	/**
	 * The render path passes the frame's partial tick so the camera aims at where the target is
	 * drawn, not at its last tick position; the tick path uses the current position.
	 */
	public static void lockView(Player player, ItemStack stack, float partialTick) {
		if (!(stack.getItem() instanceof DevilScrollItem scroll)) return;
		LivingEntity target = GloveTargetCache.clientTarget(stack);
		if (target == null || !scroll.acceptsTarget(stack, player, target)) return;
		Vec3 to = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.5, 0)
				.subtract(player.getEyePosition(partialTick));
		if (to.lengthSqr() < 1e-6) return;
		if (!player.isShiftKeyDown() && player.getViewVector(partialTick).dot(to.normalize()) <= DevilScrollItem.AIM_COS)
			return;
		float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-to.x, to.z)));
		float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))),
				-90, 90);
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.setYHeadRot(yaw);
		// both ends of the render lerp, so the partial-tick camera does not blend a mouse delta
		// back in between the tick lock and the frame
		player.yRotO = yaw;
		player.xRotO = pitch;
	}

}
