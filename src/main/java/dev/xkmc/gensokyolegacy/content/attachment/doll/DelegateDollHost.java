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
 * A {@link DollHost} that keeps its ledger somewhere else and forwards the whole
 * surface to it: implement {@link #dolls()} and nothing else.
 * <p>
 * It exists for the case {@link BaseDollEntity#getHost} creates. The host is
 * resolved by asking the <b>owning entity</b>, so a character whose dolls live in
 * a module — where module data belongs, riding her chunk save — still has to be
 * the host itself. Rather than have every such entity repeat the same ten
 * one-line forwards (and drift, or forget one), the forwards live here once.
 * {@link dev.xkmc.gensokyolegacy.content.entity.characters.magician.AliceEntity}
 * is the one that needs it today: her dolls are kept by
 * {@link dev.xkmc.gensokyolegacy.content.entity.characters.magician.AliceDollHost},
 * so she answers as the host and delegates every method to it. Without that
 * answer her dolls would find no host at all and discard themselves on the next
 * tick.
 * <p>
 * Unlike {@link DollLedger}, which is itself the ledger and forwards only the
 * command surface to its {@link DollCommander}, the delegate is a pure
 * pass-through: it holds nothing and forwards <b>everything</b>, pairing half
 * included. That is what makes it safe for a host with no state of its own —
 * every answer, and every inert default, comes from the real host.
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