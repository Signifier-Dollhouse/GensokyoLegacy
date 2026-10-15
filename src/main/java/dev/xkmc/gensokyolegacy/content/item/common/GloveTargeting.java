package dev.xkmc.gensokyolegacy.content.item.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A glove that acts on whatever is under the holder's crosshair rather than on the entity they clicked -
 * the contract for the one shared ray-trace target cache (glove.md §2), implemented by the doll glove and
 * the dagger glove's homing mode (dagger_glove.md §2d). One client map and one server store, both keyed by
 * the glove's item, so two gloves in two hands keep two targets instead of fighting over one slot.
 * <p>
 * Everything glove-specific lives behind these four members - how far it looks, whether a wall vetoes the
 * shot, what it accepts, what colour its marker glows - so both sides run the same trace and check.
 *
 * @see "doc/design/dagger_glove.md"
 */
public interface GloveTargeting {

	/** How far this glove looks for a target, in blocks. */
	double targetRange();

	/** Whether a block standing between the holder and the candidate vetoes the shot. */
	boolean blockedByBlocks();

	/**
	 * How long a traced target stays usable after the holder last traced it, in ticks.
	 * <p>
	 * Both sides read the same number, so the outline a holder sees is exactly the window in which a use
	 * would still act on that target, and neither side can promise a target the other has already dropped.
	 */
	long targetTtl();

	/**
	 * Whether this glove, in its current mode, would act on this candidate at all. Runs on both sides: it
	 * pre-filters the client trace, and the server re-checks the hint, so the two cannot disagree.
	 */
	boolean acceptsTarget(ItemStack stack, Player holder, LivingEntity candidate);

	/**
	 * Outline tint (ARGB) for this glove's cached target while it is held, or null when it marks nothing -
	 * a mode with no target to mark, or a glove that is not held. Client-side only.
	 */
	@Nullable
	Integer targetGlow(ItemStack stack);

}