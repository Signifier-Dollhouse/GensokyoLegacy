package dev.xkmc.gensokyolegacy.content.entity.dolls.client;

import dev.xkmc.gensokyolegacy.content.entity.dolls.SekibankiHeadEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.network.HeadMoveToServer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.network.HeadReleaseToServer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.network.HeadUseToServer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * The possessing client's half (gadget.md §5). The camera entity itself is the whole state: the
 * server moves it with the vanilla set-camera packet, so this only has to notice that the camera
 * is a Sekibanki head, feed it the mouse and the movement keys, report back where it ended up,
 * and swallow every click the body would otherwise answer on its own.
 */
public final class HeadPossession {

	private HeadPossession() {
	}

	/**
	 * Client tick driver. Runs on {@code ClientTickEvent.Post}, i.e. <i>after</i> the level tick in
	 * which the head flew: {@code travel} has already consumed the keys stored by the previous call
	 * and moved the head, so what is read here is the frame that just happened and not one about to.
	 * That is what lets the head report a position rather than keys.
	 */
	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.getCameraEntity() instanceof SekibankiHeadEntity head) || mc.player == null || mc.level == null) {
			return;
		}
		if (!head.isAddedToLevel() || head.isRemoved() || mc.level.getEntity(head.getId()) != head) {
			stop(mc, head);
			return;
		}
		// a break left running when the camera jumped would keep cracking on its own
		mc.gameMode.stopDestroyBlock();
		GensokyoLegacy.HANDLER.toServer(new HeadMoveToServer(head.getId(),
				head.getX(), head.getY(), head.getZ(), head.getYRot(), head.getXRot()));
	}

	/**
	 * Hands the camera to this frame's keys, so the level tick that follows flies the head on
	 * input that is current rather than one frame stale.
	 */
	public static void readInput() {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.getCameraEntity() instanceof SekibankiHeadEntity head) || mc.player == null) return;
		head.applyMove(mc.player.getYRot(), mc.player.getXRot(), mc.player.input.leftImpulse,
				mc.player.input.forwardImpulse, mc.player.input.jumping,
				mc.player.input.shiftKeyDown, mc.player.isSprinting());
	}

	/**
	 * Every click belongs to the head while the camera is in it, so the body behind cannot attack
	 * or use anything. Left click is the way back out — it has nothing else to mean here — and
	 * right click is the head's own empty-hand block interaction.
	 */
	public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.getCameraEntity() instanceof SekibankiHeadEntity head)) return;
		event.setCanceled(true);
		event.setSwingHand(false);
		if (event.isAttack()) {
			stop(mc, head);
		} else if (event.isUseItem() && mc.hitResult instanceof BlockHitResult hit) {
			GensokyoLegacy.HANDLER.toServer(new HeadUseToServer(head.getId(), hit.getBlockPos(), hit.getDirection(), hit.getLocation()));
		}
	}

	/** The body's arm would otherwise hang in mid air in front of the head's eyes. */
	public static void onRenderHand(RenderHandEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getCameraEntity() instanceof SekibankiHeadEntity) event.setCanceled(true);
	}

	/** Puts the camera back and tells the head to stop answering for it. */
	private static void stop(Minecraft mc, SekibankiHeadEntity head) {
		mc.setCameraEntity(mc.player);
		GensokyoLegacy.HANDLER.toServer(new HeadReleaseToServer(head.getId()));
	}

}