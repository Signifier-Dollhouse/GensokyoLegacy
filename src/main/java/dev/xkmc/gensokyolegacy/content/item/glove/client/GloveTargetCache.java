package dev.xkmc.gensokyolegacy.content.item.glove.client;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveMode;
import dev.xkmc.gensokyolegacy.content.item.glove.network.DollGloveTargetPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Client-side ray-trace target cache (glove.md §2). While the local player
 * holds the glove, the crosshair entity within 48 blocks (not behind a block)
 * is cached and re-synced to the server every few ticks; the cached target
 * glows via {@code GloveTargetGlowMixin} in the current mode's color.
 * Stale entries linger until the TTL instead of flickering on every miss —
 * the server re-validates anyway.
 *
	 * <p>The ray trace is pre-filtered by mode, mirroring the server checks in
 * {@code DollGloveHandler}: editor only sees dolls, attack modes only see
 * valid attack targets, summon sees nothing (it acts globally). Other modes
 * take any living entity.
 */
public class GloveTargetCache {

	public static final double RANGE = 48;

	private static final long TTL_MILLIS = 5000;

	@Nullable
	private static UUID target;

	private static long time;

	public static void onInventoryTick(Player player, ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != player || mc.level == null) return;
		if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) return;
		if (player.tickCount % 5 != 0) return;
		if (DollGloveItem.getMode(stack) == DollGloveMode.SUMMON) {
			// summon/recall acts globally, not on a target: drop any cached
			// target so nothing glows in this mode
			target = null;
			return;
		}
		Entity cam = mc.getCameraEntity() == null ? player : mc.getCameraEntity();
		Vec3 from = cam.getEyePosition();
		Vec3 dir = cam.getViewVector(1.0F);
		Vec3 to = from.add(dir.scale(RANGE));
		BlockHitResult block = mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cam));
		double blockDist = block.getType() == HitResult.Type.MISS ? RANGE : from.distanceTo(block.getLocation());
		AABB box = cam.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(1);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(cam, from, to, box,
				e -> e instanceof LivingEntity living && living.isAlive() && living.isPickable() && e != player &&
						testMode(DollGloveItem.getMode(stack), player, e),
				RANGE * RANGE);
		if (hit == null || from.distanceTo(hit.getLocation()) > blockDist) return;
		target = hit.getEntity().getUUID();
		time = System.currentTimeMillis();
		GensokyoLegacy.HANDLER.toServer(new DollGloveTargetPacket(target));
	}

	/**
	 * Mode pre-filter mirroring the server checks: editor commands act on
	 * dolls, attack commands on valid attack targets. Doll ownership is
	 * server-side only (the owner UUID is not synced), so the client filters
	 * by type and the server enforces ownership.
	 */
	private static boolean testMode(DollGloveMode mode, Player player, Entity e) {
		if (!(e instanceof LivingEntity living) || !living.isAlive() || !living.isPickable()) return false;
		return switch (mode) {
			case EDITOR -> e instanceof DollEntity;
			case VOLLEY, SUPER, SUICIDE -> isValidAttackTarget(player, living);
			default -> true;
		};
	}

	/** Client mirror of {@code DollGloveHandler.isValidAttackTarget}. */
	private static boolean isValidAttackTarget(Player player, LivingEntity target) {
		if (target == player) return false;
		if (target instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID())) return false;
		if (target instanceof BaseDollEntity) return false;
		return target.isAttackable() && !target.isSpectator();
	}

	/**
	 * The glove mode coloring the cached target's glow, or null when nothing
	 * is marked. Reads the currently held glove, so recoloring follows mode
	 * switches; the stale-target TTL still applies.
	 */
	@Nullable
	public static DollGloveMode markedMode(@Nullable Entity entity) {
		if (!isMarked(entity)) return null;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return null;
		ItemStack main = mc.player.getMainHandItem();
		if (main.getItem() instanceof DollGloveItem) return DollGloveItem.getMode(main);
		ItemStack off = mc.player.getOffhandItem();
		if (off.getItem() instanceof DollGloveItem) return DollGloveItem.getMode(off);
		return null;
	}

	public static boolean isMarked(@Nullable Entity entity) {
		if (entity == null || target == null) return false;
		if (!entity.getUUID().equals(target)) return false;
		return System.currentTimeMillis() - time < TTL_MILLIS;
	}

}
