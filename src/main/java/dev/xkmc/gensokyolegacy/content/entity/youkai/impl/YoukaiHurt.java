package dev.xkmc.gensokyolegacy.content.entity.youkai.impl;

import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * What a character does about being hit: the reputation cost, the point at which she gives up on
 * whoever mistreated her, and the kill-credit bookkeeping.
 * <p>
 * {@code actuallyHurt}, {@code die} and {@code killedEntity} stay on the entity class - vanilla
 * overrides, where a default would be shadowed - and call in here. The module loop is theirs too:
 * dispatching to modules is the base's job.
 *
 * @see "doc/design/youkai/entity.md"
 */
public interface YoukaiHurt extends YoukaiBaseImpl {

	/**
	 * The reputation half of a hit: whoever landed it is marked down, and on the server a character
	 * who has run out of favour with the last player to hit her may simply leave.
	 * <p>
	 * Runs before the vanilla super call, which is what carries the actual damage on.
	 */
	default void onDamaged(DamageSource source, float amount) {
		var self = asYoukai();
		if (self.spellCard != null) self.spellCard.hurt(self.cardHolder, source, amount);
		var holder = getData(source.getEntity());
		holder.ifPresent(e -> e.onHurt(source, amount));
		if (!self.level().isClientSide()) {
			holder.ifPresent(e -> {
				if (vanishOnDislike() && e.data().reputation < ReputationConstants.DISCARD_REP_THRESHOLD &&
						e.player() instanceof ServerPlayer sp) {
					sp.displayClientMessage(GLLang.Info.YOUKAI_AVOID.get(self.getDisplayName()), false);
					self.discard();
				}
			});
		}
	}

	/**
	 * Whether losing the last of a player's favour makes this character give up and disappear, the
	 * {@code avoid} reaction of a youkai that will not share a world with whoever mistreated her.
	 * <p>
	 * A character whose answer to a beating is to run turns this off: the threshold would otherwise delete her
	 * on the very tick the flee begins. The reputation cost still stands, so she ends up stranger-or-worse.
	 */
	default boolean vanishOnDislike() {
		return true;
	}

	/**
	 * Crediting the killer. {@code freshDeath} is her dead flag from before the vanilla death ran,
	 * folded against the one after: a second {@code die} on an already-dead character must not
	 * credit anybody twice.
	 */
	default void onDeath(boolean freshDeath, DamageSource source) {
		var e = source.getEntity();
		if (!freshDeath || !(e instanceof LivingEntity le)) return;
		if (!e.isAlive() || !e.isAddedToLevel() || e.isRemoved()) return;
		onKilledBy(le, source);
	}

	default void onKillEntity(LivingEntity entity) {
		getData(entity).ifPresent(e -> e.onKilledByCharacter());
	}

	default void onKilledBy(LivingEntity le, DamageSource source) {
		getData(le).ifPresent(e -> e.onKillCharacter());
	}

}