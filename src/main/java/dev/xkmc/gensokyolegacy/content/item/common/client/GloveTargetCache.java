package dev.xkmc.gensokyolegacy.content.item.common.client;

import dev.xkmc.gensokyolegacy.content.item.common.GloveTargeting;
import dev.xkmc.gensokyolegacy.content.item.common.network.GloveTargetPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side ray-trace target cache (glove.md §2), shared by every {@link GloveTargeting} glove —
 * the doll glove and the dagger glove's homing mode (dagger_glove.md §2d).
 * <p>
 * While the local player holds one, the crosshair entity within the glove's own range is cached
 * per glove item and re-synced to the server on every trace hit; the cached target glows through
 * {@code ClientGlowManager} in the held mode's colour. Stale entries linger until the TTL instead
 * of flickering on every miss — the server re-validates anyway.
 * <p>
 * One entry per glove rather than one slot for the whole player: main hand and off hand tick in
 * the same call, and a doll glove and a dagger glove trace with different ranges, different wall
 * rules and different predicates, so a shared slot would have them overwrite each other.
 * <p>
 * Driven by one client tick rather than by {@code Item#inventoryTick}, which would have to be
 * overridden identically in every glove: what feeds the cache is the same decision for all of them.
 * <p>
 * Dolls are not special-cased here. Doll hovering (gold glow, loadout overlay, editor open) runs
 * on a fresh 16-block ray trace through {@code GloveDollHover} (client) and
 * {@code DollGloveHandler.tryOpenEditor} (server) instead, and registers first in
 * {@code ClientGlowManager} (glove.md §2b).
 */
public final class GloveTargetCache {

	/** The crosshair is traced every N client ticks. */
	private static final int TRACE_INTERVAL = 5;

	/** Server ticks per second, for turning a glove's TTL into the client-side linger. */
	private static final long MILLIS_PER_TICK = 50;

	private record Entry(@Nullable UUID target, long time) {
	}

	private static final Map<Item, Entry> ENTRIES = new HashMap<>();

	private GloveTargetCache() {
	}

	/**
	 * Client tick driver: traces the crosshair once per interval for every held targeting glove.
	 * Client-side only.
	 */
	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null) return;
		if (player.tickCount % TRACE_INTERVAL != 0) return;
		trace(player, player.getMainHandItem());
		trace(player, player.getOffhandItem());
	}

	/**
	 * Traces the crosshair for one held stack and, on a hit, caches and re-syncs the target.
	 * Nothing at all happens when the stack is not a targeting glove or its mode wants no target,
	 * which is what keeps an aimed mode from ever lighting something up.
	 */
	private static void trace(Player player, ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		if (!(stack.getItem() instanceof GloveTargeting glove)) return;
		Entity cam = mc.getCameraEntity() != null ? mc.getCameraEntity() : player;
		Vec3 from = cam.getEyePosition();
		Vec3 dir = cam.getViewVector(1.0F);
		double range = glove.targetRange();
		Vec3 to = from.add(dir.scale(range));
		// whether a wall vetoes the shot is the glove's decision, not the trace's: a glove whose
		// attack turns has no reason to refuse what stands in the way
		double blockDist = Double.MAX_VALUE;
		if (glove.blockedByBlocks()) {
			BlockHitResult block = mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cam));
			if (block.getType() != HitResult.Type.MISS) blockDist = from.distanceTo(block.getLocation());
		}
		AABB box = cam.getBoundingBox().expandTowards(dir.scale(range)).inflate(1);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(cam, from, to, box,
				e -> e instanceof LivingEntity living && living.isAlive() && living.isPickable() &&
						glove.acceptsTarget(stack, player, living),
				range * range);
		if (hit == null || from.distanceTo(hit.getLocation()) > blockDist) return;
		UUID target = hit.getEntity().getUUID();
		ENTRIES.put(stack.getItem(), new Entry(target, System.currentTimeMillis()));
		GensokyoLegacy.HANDLER.toServer(new GloveTargetPacket(stack.getItem(), target));
	}

	/**
	 * The held stack whose glove marks this entity, or null. Main hand first, so a glove held
	 * there wins over the same glove in the off hand.
	 */
	@Nullable
	private static ItemStack markedStack(@Nullable Entity entity) {
		if (!(entity instanceof LivingEntity living)) return null;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return null;
		ItemStack main = mc.player.getMainHandItem();
		if (marks(main, living)) return main;
		ItemStack off = mc.player.getOffhandItem();
		return marks(off, living) ? off : null;
	}

	/**
	 * Whether this stack is a glove that marks this entity right now: it must be a targeting glove
	 * whose current mode draws a marker at all, and its own cache entry must still be inside the
	 * glove's TTL — the same window the server would still accept the hint in.
	 */
	private static boolean marks(ItemStack stack, LivingEntity target) {
		if (!(stack.getItem() instanceof GloveTargeting glove)) return false;
		if (glove.targetGlow(stack) == null) return false;
		Entry entry = ENTRIES.get(stack.getItem());
		return entry != null && entry.target != null && entry.target.equals(target.getUUID()) &&
				System.currentTimeMillis() - entry.time < glove.targetTtl() * MILLIS_PER_TICK;
	}

	/**
	 * Outline color for the cached target: the held glove's own color, or null when nothing is
	 * marked. Display-only, and read per frame, so recoloring follows mode switches while the
	 * stale-target TTL still applies.
	 */
	@Nullable
	public static Integer hoverColor(@Nullable Entity entity) {
		ItemStack stack = markedStack(entity);
		if (stack == null) return null;
		return ((GloveTargeting) stack.getItem()).targetGlow(stack);
	}

	public static boolean isMarked(@Nullable Entity entity) {
		return markedStack(entity) != null;
	}

}