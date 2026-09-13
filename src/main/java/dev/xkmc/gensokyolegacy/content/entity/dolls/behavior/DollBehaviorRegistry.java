package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionHandler;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Item → behavior binding: which stacks satisfy which {@link DollActionType}, at
 * what priority, and how to construct the executing behavior. Mirrors
 * MobWeaponAPI's {@code WeaponRegistry} without the dependency: a new item
 * feeding an existing action is one {@link #register} line in
 * {@link DollBehaviors}. The matched entry drives both capability checks and
 * execution — behaviors are created per execution from the available items, so
 * the doll stores none and per-run state can never go stale.
 */
public final class DollBehaviorRegistry {

	public record Entry(String id, Predicate<ItemStack> match, DollActionType type, int priority,
						Supplier<DollBehavior> factory) {
	}

	public record HandMatch(Entry entry, DollSlot hand, ItemStack stack) {
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	private DollBehaviorRegistry() {
	}

	public static void register(String id, Predicate<ItemStack> match, DollActionType type, int priority,
								Supplier<DollBehavior> factory) {
		ENTRIES.add(new Entry(id, match, type, priority, factory));
	}

	/**
	 * Best (entry, hand) match for a type across both hands, read from the
	 * authoritative ledger (never the synced mirror). Higher entry priority wins
	 * (so a laser anywhere beats hexbrew anywhere); ties prefer main hand.
	 * The matched stack is a copy.
	 */
	public static Optional<HandMatch> findHand(DollEntity doll, DollActionType type) {
		return findBest(doll, type).map(best -> new HandMatch(best.entry(), best.hand(), best.stack().copy()));
	}

	/**
	 * Constructs the behavior for a command from the currently held items. Empty
	 * when nothing held satisfies the action type.
	 */
	public static Optional<DollBehavior> createFor(DollEntity doll, DollAction action) {
		return findBest(doll, action.type()).map(best -> best.entry().factory().get());
	}

	/**
	 * Client-safe capability check against plain stacks (no doll needed): whether
	 * any registered entry for the type matches the stack. Used by the
	 * attack-glove sidebar, which reads the synced loadout mirror.
	 */
	public static boolean matches(ItemStack stack, DollActionType type) {
		if (stack.isEmpty()) return false;
		DollBehaviors.register();
		for (Entry entry : ENTRIES) {
			if (entry.type() == type && entry.match().test(stack)) return true;
		}
		return false;
	}

	/**
	 * Client-safe best stack for a type across two plain stacks (the synced
	 * loadout mirror): replicates {@link #findBest} ordering — higher entry
	 * priority wins, ties prefer main hand. Returns empty when no held stack
	 * satisfies the type. Display-only; the server revalidates via
	 * {@link #findHand} against the authoritative ledger.
	 */
	public static ItemStack findUsing(ItemStack main, ItemStack off, DollActionType type) {
		DollBehaviors.register();
		ItemStack best = ItemStack.EMPTY;
		int bestPriority = Integer.MIN_VALUE;
		for (ItemStack stack : new ItemStack[]{main, off}) {
			if (stack.isEmpty()) continue;
			for (Entry entry : ENTRIES) {
				if (entry.type() != type || !entry.match().test(stack)) continue;
				if (best.isEmpty() || entry.priority() > bestPriority) {
					best = stack;
					bestPriority = entry.priority();
				}
			}
		}
		return best;
	}

	private record BestMatch(Entry entry, DollSlot hand, ItemStack stack) {
	}

	private static Optional<BestMatch> findBest(DollEntity doll, DollActionType type) {
		DollBehaviors.register();
		@Nullable BestMatch best = null;
		for (DollSlot hand : new DollSlot[]{DollSlot.MAIN_HAND, DollSlot.OFF_HAND}) {
			ItemStack stack = doll.ledgerStack(hand);
			if (stack.isEmpty()) continue;
			for (Entry entry : ENTRIES) {
				if (entry.type() != type) continue;
				if (entry.match().test(stack) && (best == null || entry.priority() > best.entry().priority())) {
					best = new BestMatch(entry, hand, stack);
				}
			}
		}
		return Optional.ofNullable(best);
	}

}
