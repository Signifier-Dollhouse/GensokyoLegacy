package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * One ghost variant's behaviour, carried by the enum constant like {@code HexBrewHandler}. The
 * entity never branches on the variant: it hands the tick and the aura pulse to this object.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public interface GhostVariantHandler {

	Component getDisplayName();

	/**
	 * Whether a wild stray may roll this variant.
	 */
	default boolean wild() {
		return true;
	}

	/**
	 * Whether the fan's summon-all may create this variant. All variants qualify except the
	 * haunting ghost, which is sent at a target instead.
	 */
	default boolean summonable() {
		return true;
	}

	/**
	 * Whether the bone flute may release this variant back into the wild.
	 */
	default boolean releasable() {
		return true;
	}

	/**
	 * Whether the binding breaks below {@link GhostPower#LEAVE_THRESHOLD}.
	 */
	default boolean leavesWhenWeak() {
		return true;
	}

	/**
	 * Whether the follow goals should steer this ghost right now.
	 */
	default boolean followsOwner(GhostEntity ghost) {
		return true;
	}

	/**
	 * Whether travel is suspended, leaving the ghost as a fixed centre.
	 */
	default boolean isStationary(GhostEntity ghost) {
		return false;
	}

	/**
	 * Gives this ghost a target, resetting any attack state. Only the haunting variant acts on it.
	 */
	default void hunt(GhostEntity ghost, LivingEntity target) {
	}

	/**
	 * Per-tick upkeep, called only while the owner is alive, present and not a spectator.
	 */
	default void tick(GhostEntity ghost) {
	}

	/**
	 * The aura pulse, called only while the owner is alive, present and near.
	 */
	default void aura(GhostEntity ghost, LivingEntity owner) {
	}

	/**
	 * A last-resort save when the owner dies. Returns true when this ghost consumed itself.
	 */
	default boolean wardDeath(ServerPlayer player, GhostEntity ghost, DollAttachment att) {
		return false;
	}

}
