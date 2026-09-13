package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One doll command. {@code done} is the shared iterative progress set: dolls of one
 * volley pass the same instance along, each adding itself on completion. It is
 * mutated, never copied — compare and dedup by {@code (type, mode, target)} only,
 * never by record equality. Empty (and unused) for {@code ONE_TIME}.
 */
public record DollAction(DollActionType type, DollActionMode mode, @Nullable UUID target, Set<UUID> done) {

	public static DollAction oneTime(DollActionType type, @Nullable UUID target) {
		return new DollAction(type, DollActionMode.ONE_TIME, target, new LinkedHashSet<>());
	}

	public static DollAction iterative(DollActionType type, @Nullable UUID target) {
		return new DollAction(type, DollActionMode.ITERATIVE, target, new LinkedHashSet<>());
	}

	public boolean sameOrder(DollAction other) {
		return type == other.type && mode == other.mode &&
				(target == null ? other.target == null : target.equals(other.target));
	}

}
