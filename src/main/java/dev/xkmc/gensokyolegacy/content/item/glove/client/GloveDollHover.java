package dev.xkmc.gensokyolegacy.content.item.glove.client;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Doll hover readout (glove.md §2b): while the local player holds the glove,
 * the doll under the crosshair (own ray trace out to
 * {@code DollGloveHandler.EDITOR_RANGE} blocks, occluded by blocks) glows
 * gold and shows its loadout overlay. This deliberately bypasses
 * {@code GloveTargetCache}: the trace runs fresh every client tick with no
 * packets and no TTL linger, and the server re-traces on every use — so what
 * the holder sees is exactly what right-click will open.
 */
public final class GloveDollHover {

	/** Gold outline for a hovered doll, in every mode (ARGB). */
	public static final int DOLL_GLOW = 0xFFAA00;

	private static int lastTick = -1;

	@Nullable
	private static DollEntity memo;

	private GloveDollHover() {
	}

	public static boolean gloveInHand(Player player) {
		if (player.getMainHandItem().getItem() instanceof DollGloveItem) return true;
		return player.getOffhandItem().getItem() instanceof DollGloveItem;
	}

	/**
	 * The doll under the crosshair while the holder carries the glove, or
	 * null. Re-traced at most once per client tick (the glow mixin queries
	 * this once per entity per frame). Client-side only.
	 */
	@Nullable
	public static DollEntity hoveredDoll() {
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || !gloveInHand(mc.player)) {
			memo = null;
			return null;
		}
		int t = mc.player.tickCount;
		if (t != lastTick) {
			lastTick = t;
			memo = traceDoll();
		}
		if (memo != null && (memo.isRemoved() || !memo.isAlive())) memo = null;
		return memo;
	}

	/** Whether the entity is the glove-hovered doll. Client-side only. */
	public static boolean isHovered(@Nullable Entity entity) {
		return entity instanceof DollEntity && hoveredDoll() == entity;
	}

	/** Gold for the glove-hovered doll, null otherwise. Client-side only. */
	@Nullable
	public static Integer hoverColor(@Nullable Entity entity) {
		return isHovered(entity) ? DOLL_GLOW : null;
	}

	/**
	 * Fresh crosshair trace for dolls, mirroring the server editor trace in
	 * {@code DollGloveHandler}: same range, same block occlusion, type-only
	 * filter (ownership is server-side, enforced on open).
	 */
	@Nullable
	private static DollEntity traceDoll() {
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return null;
		double range = DollGloveHandler.EDITOR_RANGE;
		Entity cam = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
		Vec3 from = cam.getEyePosition();
		Vec3 dir = cam.getViewVector(1.0F);
		Vec3 to = from.add(dir.scale(range));
		BlockHitResult block = mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cam));
		double blockDist = block.getType() == HitResult.Type.MISS ? range : from.distanceTo(block.getLocation());
		AABB box = cam.getBoundingBox().expandTowards(dir.scale(range)).inflate(1);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(cam, from, to, box,
				e -> e instanceof DollEntity doll && doll.isAlive() && doll.isPickable(),
				range * range);
		if (hit == null || from.distanceTo(hit.getLocation()) > blockDist) return null;
		return (DollEntity) hit.getEntity();
	}

}
