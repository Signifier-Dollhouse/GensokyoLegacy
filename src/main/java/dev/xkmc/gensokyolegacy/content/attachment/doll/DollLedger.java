package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link DollHost} owning real entries and commands: the player capability or a character
 * that conjures her own dolls. The command surface is wired through here, so no host repeats
 * the forwarding and no doll branches on host type.
 *
 * @see "doc/design/doll/control.md"
 * @see "doc/design/doll/timed_summon.md"
 */
public interface DollLedger extends DollHost {

	/**
	 * Every ledger entry in ledger order, summoned or parked. Never null; entries without
	 * a loaded entity resolve to null.
	 */
	Collection<DollData> dolls();

	/**
	 * The command state machine driving this ledger's dolls.
	 */
	DollCommander commands();

	/**
	 * Forgets an entry for good: no item, no parked data, nothing waiting to become either. The end
	 * state both of a timed summon's death and of its lifespan running out, since it has no item form
	 * to fall back on. Entry-only — the caller disposes of the entity, and must do so
	 * <b>before</b> calling this, or the doll spends a tick alive and unpaired.
	 *
	 * @return the dropped entry, or null when this ledger holds no such entry
	 */
	@Nullable
	DollData drop(UUID uuid);

	@Override
	default float getFormationYaw() {
		return commands().getFormationYaw();
	}

	@Override
	default List<DollEntity> summonedAllies(DollEntity doll) {
		return commands().summonedAllies(doll);
	}

	@Override
	default boolean isCommandedTarget(LivingEntity target) {
		return commands().isCommandedTarget(target);
	}

	@Override
	default Optional<DollActionType> doneType(@Nullable UUID uuid) {
		return uuid == null ? Optional.empty() : commands().doneType(uuid);
	}

	@Override
	default void handAhead(DollEntity doll, DollAction action) {
		commands().handAhead(doll, action);
	}

	@Override
	default boolean handOff(DollEntity doll, DollAction action) {
		return commands().handOff(doll, action);
	}

}
