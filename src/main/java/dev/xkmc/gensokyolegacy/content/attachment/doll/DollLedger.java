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
 * A {@link DollHost} that owns a real ledger and commands it: the player capability
 * ({@link DollAttachment}) and a character that conjures her own dolls
 * ({@link dev.xkmc.gensokyolegacy.content.entity.characters.magician.AliceDollHost}).
 * <p>
 * The ledger itself only has to expose its entries and its {@link DollCommander};
 * the whole {@link DollHost} command surface is wired through here, so no host
 * repeats the forwarding and no doll entity branches on the host type.
 */
public interface DollLedger extends DollHost {

	/**
	 * Every ledger entry in ledger order, summoned or parked. Never null.
	 * The commander walks this; entries without a loaded entity resolve to null.
	 */
	Collection<DollData> dolls();

	/** The command state machine driving this ledger's dolls. */
	DollCommander commands();

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
