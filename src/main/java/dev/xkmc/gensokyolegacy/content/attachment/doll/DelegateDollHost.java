package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link DollHost} that keeps its ledger somewhere else and forwards the whole surface:
 * implement {@link #dolls()} and nothing else. Needed because the host is resolved by asking the
 * <b>owning entity</b>, so a character whose dolls live in a module still answers as the host.
 * <p>
 * Unlike {@link DollLedger}, which is the ledger itself and forwards only the command surface,
 * this is a pure pass-through: every answer, including the inert defaults, comes from the real host.
 *
 * @see doc/design/doll/host.md
 */
public interface DelegateDollHost extends DollHost {

	/** The host that actually holds the ledger. Never null, never a caller-supplied one. */
	DollHost dolls();

	@Override
	@Nullable
	default DollData findSummoned(UUID uuid) {
		return dolls().findSummoned(uuid);
	}

	@Override
	default void update(BaseDollEntity doll) {
		dolls().update(doll);
	}

	@Override
	@Nullable
	default DollData detach(UUID uuid) {
		return dolls().detach(uuid);
	}

	@Override
	default void onDeath(BaseDollEntity doll) {
		dolls().onDeath(doll);
	}

	@Override
	default float getFormationYaw() {
		return dolls().getFormationYaw();
	}

	@Override
	default List<DollEntity> summonedAllies(DollEntity doll) {
		return dolls().summonedAllies(doll);
	}

	@Override
	default boolean isCommandedTarget(LivingEntity target) {
		return dolls().isCommandedTarget(target);
	}

	@Override
	default Optional<DollActionType> doneType(UUID uuid) {
		return dolls().doneType(uuid);
	}

	@Override
	default void handAhead(DollEntity doll, DollAction action) {
		dolls().handAhead(doll, action);
	}

	@Override
	default boolean handOff(DollEntity doll, DollAction action) {
		return dolls().handOff(doll, action);
	}

}