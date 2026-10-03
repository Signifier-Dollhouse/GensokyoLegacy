package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionHandler;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
 * <p>
 * The table is also the loadout's admission list: {@link #usableInHand} (what a
 * hand slot takes at all) and {@link #spendsOnUse} (whether a stack in a hand is
 * ammunition rather than one item) both read it, so a new binding unlocks its
 * item in the loadout editor without a second edit.
 * <p>
 * One binding is <b>not</b> an item: the bare-handed fallback, registered through
 * {@link #registerBareHand}. An empty stack is never an item-table match (there is
 * nothing in the hand to match), so it cannot be expressed as a predicate — it is
 * matched against the <b>empty hand itself</b>, and only after no item binding has
 * claimed the type. That is what lets a doll with nothing to fight with still answer
 * a volley, see {@link DollBehaviors#register}.
 */
public final class DollBehaviorRegistry {

	/** Id of the bare-handed binding, for symmetry with the item-table ids. */
	private static final String BARE_HAND = "barehand";

	/**
	 * One binding. {@code consumed} says the behavior spends what it holds — one per
	 * use ({@code consumeLoadoutItem(slot, 1)}) — which is what makes N in a hand N
	 * uses and therefore what lets such a stack sit above size 1 in a loadout slot.
	 * Never-consumed bindings (danmaku shots, the lance, talismans) are one item per
	 * hand, however large a stack the item itself allows.
	 */
	public record Entry(String id, Predicate<ItemStack> match, DollActionType type, int priority,
						boolean consumed, Supplier<DollBehavior> factory) {
	}

	public record HandMatch(Entry entry, DollSlot hand, ItemStack stack) {

		/**
		 * Whether this is the bare-handed fallback rather than an item: the match
		 * names the <b>empty</b> hand. Derived rather than stored, because the item
		 * table can only match a non-empty stack ({@link #findBest} skips empties),
		 * so an empty match stack is unambiguously the bare-handed binding.
		 */
		public boolean bareHand() {
			return stack.isEmpty();
		}
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	/** The one bare-handed binding, if a type registered one. At most one per type. */
	private static final Map<DollActionType, Entry> BARE_HANDS = new EnumMap<>(DollActionType.class);

	private DollBehaviorRegistry() {
	}

	public static void register(String id, Predicate<ItemStack> match, DollActionType type, int priority,
								boolean consumed, Supplier<DollBehavior> factory) {
		ENTRIES.add(new Entry(id, match, type, priority, consumed, factory));
	}

	/**
	 * Register the bare-handed fallback for an action type: a doll that holds nothing
	 * satisfying {@code type}, and has at least one hand free, acts with that hand.
	 * <p>
	 * Consulted only once the item table has come up empty for the type, so it never
	 * competes with a weapon — which is why it takes no priority. Costs nothing and is
	 * deliberately <b>not</b> in {@link #ENTRIES}: {@link #usableInHand} admits what a
	 * hand slot takes, and an empty stack is already the one thing it never admits.
	 */
	public static void registerBareHand(DollActionType type, Supplier<DollBehavior> factory) {
		BARE_HANDS.put(type, new Entry(BARE_HAND, ItemStack::isEmpty, type, 0, false, factory));
	}

	/**
	 * Best (entry, hand) match for a type across both hands, read from the
	 * authoritative ledger (never the synced mirror). Higher entry priority wins
	 * (so a laser anywhere beats hexbrew anywhere); ties prefer main hand.
	 * The matched stack is a copy.
	 * <p>
	 * When no item in either hand satisfies the type, the bare-handed fallback takes
	 * it if the type registered one and a hand is free — see {@link #registerBareHand}.
	 */
	public static Optional<HandMatch> findHand(DollEntity doll, DollActionType type) {
		return findBest(doll, type).map(best -> new HandMatch(best.entry(), best.hand(), best.stack().copy()));
	}

	/**
	 * Constructs the behavior for a command from the currently held items. Empty
	 * when nothing held satisfies the action type.
	 */
	public static Optional<DollBehavior> createFor(DollEntity doll, DollAction action) {
		return createFor(doll, action.type());
	}

	/**
	 * Constructs the behavior an action type maps to from the currently held items.
	 * Empty when nothing held satisfies it. The order-free form, for asking what a
	 * loadout could do with no command in hand — a reach check, say.
	 */
	public static Optional<DollBehavior> createFor(DollEntity doll, DollActionType type) {
		return findBest(doll, type).map(best -> best.entry().factory().get());
	}

	/**
	 * Client-safe capability check against plain stacks (no doll needed): whether
	 * any registered entry for the type matches the stack. Used by the
	 * attack-glove sidebar, which reads the synced loadout mirror.
	 */
	public static boolean matches(ItemStack stack, DollActionType type) {
		return !stack.isEmpty() && anyMatch(stack, type);
	}

	/**
	 * Whether <b>any</b> binding matches the stack, whatever the action type — the
	 * doll has something to do with it. The loadout half of the same question: a
	 * hand slot takes what {@link #usableInHand} admits, and that is every bound
	 * item.
	 */
	public static boolean matchesAny(ItemStack stack) {
		return !stack.isEmpty() && anyMatch(stack, null);
	}

	/**
	 * What a doll hand will take: anything a doll can act with — some binding
	 * matches it, whatever the action type — plus the vanilla shield, which is not a
	 * behavior but the reactive block (DollShield §5.6). Both hand slots admit the
	 * same set; only blocking reads the off hand, so a shield parked in the main
	 * hand does nothing until a sticky swap (§2) carries it across.
	 */
	public static boolean usableInHand(ItemStack stack) {
		return !stack.isEmpty() && (matchesAny(stack) || stack.getItem() instanceof ShieldItem);
	}

	/**
	 * Whether a stack in a doll hand is <b>ammunition</b> rather than one item: a
	 * binding that spends what it holds matches, so N in the slot is N uses. Only
	 * these may sit above size 1 (loadout.md §4) — a thrown hexbrew bottle, a fired
	 * laser, a detonated TNT. A talisman wears down by durability instead, and
	 * danmaku items and the lance are never spent, so those stay single.
	 */
	public static boolean spendsOnUse(ItemStack stack) {
		if (stack.isEmpty()) return false;
		DollBehaviors.register();
		for (Entry entry : ENTRIES) {
			if (entry.consumed() && entry.match().test(stack)) return true;
		}
		return false;
	}

	/**
	 * The one matching pass behind {@link #matches} / {@link #matchesAny}: a null
	 * type means any.
	 */
	private static boolean anyMatch(ItemStack stack, @Nullable DollActionType type) {
		DollBehaviors.register();
		for (Entry entry : ENTRIES) {
			if (type != null && entry.type() != type) continue;
			if (entry.match().test(stack)) return true;
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
		// Nothing held satisfies the type: fall back to the bare hand, main hand first
		// so the common case (two empty hands) needs no swap.
		Entry bareHand = BARE_HANDS.get(type);
		if (best == null && bareHand != null) {
			for (DollSlot hand : new DollSlot[]{DollSlot.MAIN_HAND, DollSlot.OFF_HAND}) {
				if (!doll.ledgerStack(hand).isEmpty()) continue;
				return Optional.of(new BestMatch(bareHand, hand, ItemStack.EMPTY));
			}
		}
		return Optional.ofNullable(best);
	}

}
