package dev.xkmc.gensokyolegacy.content.attachment.glove;

import dev.xkmc.gensokyolegacy.content.item.common.GloveTargeting;
import dev.xkmc.l2core.capability.player.PlayerCapabilityTemplate;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server half of the glove target cache (glove.md §2): one ray-trace hint per glove, written by
 * {@code GloveTargetPacket}. Session-only, and never authority: {@link #resolve} re-checks the
 * glove's own TTL, the level, the entity and the range, and the glove re-checks what it will
 * accept ({@link GloveTargeting#acceptsTarget}), so a modified client cannot buy itself a target.
 *
 * @see "doc/design/doll/glove.md"
 */
@SerialClass
public class GloveTargetAttachment extends PlayerCapabilityTemplate<GloveTargetAttachment> {

	private record Entry(@Nullable UUID target, long time) {
	}

	/**
	 * One slot per glove item, so main hand and off hand never overwrite each other. Never
	 * serialized; {@link #mark} drops expired entries, so it cannot outlive a removed item.
	 */
	private final Map<Item, Entry> entries = new HashMap<>();

	/**
	 * Records a client trace result as this glove's hint, stamped with the level's game time.
	 * Expired entries for every glove are pruned here, on the write that runs every few ticks anyway.
	 */
	public void mark(Item glove, @Nullable UUID target, long now) {
		entries.entrySet().removeIf(e -> e.getKey() instanceof GloveTargeting g && now - e.getValue().time > g.targetTtl());
		entries.put(glove, new Entry(target, now));
	}

	/**
	 * The hinted target for this stack's glove, or null: missing, stale, dead, out-of-level and
	 * out-of-range hints all read alike. A hit restamps the timestamp, so a target the holder
	 * keeps looking at stays live however slowly they use the glove.
	 */
	@Nullable
	public LivingEntity resolve(ServerPlayer sp, ItemStack stack) {
		if (!(stack.getItem() instanceof GloveTargeting glove)) return null;
		Entry entry = entries.get(stack.getItem());
		if (entry == null || entry.target == null) return null;
		long now = sp.level().getGameTime();
		if (now - entry.time > glove.targetTtl()) return null;
		if (!(sp.serverLevel().getEntity(entry.target) instanceof LivingEntity target) || !target.isAlive()) return null;
		if (sp.distanceTo(target) > glove.targetRange()) return null;
		entries.put(stack.getItem(), new Entry(entry.target, now));
		return target;
	}

}