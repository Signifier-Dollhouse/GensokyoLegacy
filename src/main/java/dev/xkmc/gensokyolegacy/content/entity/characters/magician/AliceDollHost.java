package dev.xkmc.gensokyolegacy.content.entity.characters.magician;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DelegateDollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollCommander;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollLedger;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollSpawn;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollState;
import dev.xkmc.gensokyolegacy.content.attachment.doll.MutableDollInventory;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviors;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollHandLock;
import dev.xkmc.gensokyolegacy.content.entity.module.AbstractYoukaiModule;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.FoldedPaperTalisman;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Alice's retinue: she is a {@link dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost}
 * in her own right, so her dolls pair, follow, and fight through exactly the same
 * machinery as a player's (doc/design/doll/pairing.md §8.2). Nothing in the doll
 * entity is aware of who holds it.
 * <p>
 * Deliberate departures from the player ledger, all of them the point of the
 * character:
 * <ul>
 *   <li><b>No items, no inventory.</b> Her dolls are conjured out of thin air and
 *       never itemize, so {@code STORED} is unused. A retired doll parks as
 *       {@code TEMP} to be conjured again when the roster grows; a
 *       <b>destroyed</b> one is dropped from the ledger outright, because there is
 *       no count of dolls she is allowed to own for a cap to protect — she simply
 *       makes another. {@code detach} is refused and {@code onDeath} does nothing.</li>
 *   <li><b>A roster, not a collection.</b> She keeps a standing set sized by what
 *       she is doing ({@link Post}), so the count is her intent rather than
 *       whatever the player happens to own.</li>
 *   <li><b>She arms and orders them.</b> A weapon exists exactly while she is
 *       fighting ({@link Post#COMBAT}) — and it is not the same weapon for all of
 *       them: the roster splits down the middle, half charging in with doll lances
 *       and half shooting from the back with star wands. Heal talismans live in the
 *       off hand only while she is fighting or someone in her retinue is hurt, and
 *       every attack is a one-time order she hands out herself, one doll per mob,
 *       never an iterative volley and never one a doll cannot reach.</li>
 * </ul>
 * <p>
 * The whole ledger lives on her entity and rides her chunk save like any other
 * module data, so an <b>unload</b> keeps the roster — dolls and all their values —
 * and the reconcile pass re-conjures them on the next tick. Dolls themselves are
 * never chunk-serialized (§5.8), so a SUMMONED entry without a live entity is by
 * definition gone. A <b>discard or a kill</b> drops the whole thing: the host
 * becomes unreachable and every doll self-discards, and her data is never written
 * back. There is nothing of hers left in the world to come back to.
 */
@SerialClass
public class AliceDollHost extends AbstractYoukaiModule implements DollLedger {

	private static final ResourceLocation ID = GensokyoLegacy.loc("alice_dolls");

	/**
	 * Hard bound on the ledger — the top of {@link Post#COMBAT}. Only retired dolls
	 * linger in it, since destroyed ones are dropped, so the live roster is really
	 * bounded by {@link #quota}.
	 */
	public static final int MAX_DOLLS = 8;

	/** Reconjure at most this often, so a doll dying never becomes a spawn loop. */
	private static final int CONJURE_INTERVAL = 20;

	/** Order pass period. The dolls' own shot cooldowns pace the actual firing. */
	private static final int COMMAND_INTERVAL = 5;

	/**
	 * Her dolls, in conjuring order. A doll is <b>created on demand</b> — the roster
	 * grows as her post demands more, rather than being conjured up front and sat
	 * idle — and is created at most {@link #MAX_DOLLS} times over, because a
	 * destroyed entry is dropped instead of being kept for repair.
	 */
	@SerialField
	private final List<DollData> dolls = new ArrayList<>();

	/** Ordinal of the {@link Post} she last rolled a roster size for, -1 before the first. */
	@SerialField
	private int post = -1;

	/** How many dolls to keep out in {@link #post}. Re-rolled only when the post changes. */
	@SerialField
	private int quota = Post.INDOOR.min;

	private final DollCommander commander = new DollCommander(this);

	private final AliceEntity owner;

	public AliceDollHost(AliceEntity owner) {
		super(ID, owner);
		this.owner = owner;
	}

	/**
	 * The retinue ledger, exposed on {@link AliceEntity} itself. The module cannot
	 * be the doll's host: {@link BaseDollEntity#getHost} resolves the host by
	 * asking the <b>owning entity</b> for a {@link DollHost}, and a module is not
	 * an entity. So Alice answers as the host and forwards to us — through
	 * {@link DelegateDollHost}, which is what makes that one method instead of ten.
	 */
	public static DollHost hostOf(AliceEntity alice) {
		return alice.getModule(AliceDollHost.class)
				.map(DollLedger.class::cast)
				.orElseThrow(() -> new IllegalStateException("Alice is missing her doll module"));
	}

	// ---------- host ----------

	@Override
	public Collection<DollData> dolls() {
		return dolls;
	}

	@Override
	public DollCommander commands() {
		return commander;
	}

	@Override
	@Nullable
	public DollData findSummoned(UUID uuid) {
		if (uuid == null) return null;
		for (DollData data : dolls) {
			if (data != null && data.isSummoned() && uuid.equals(data.uuid)) return data;
		}
		return null;
	}

	@Override
	public void update(BaseDollEntity doll) {
		DollData data = findSummoned(doll.getUUID());
		if (data != null) doll.writeValuesTo(data);
	}

	@Override
	@Nullable
	public DollData detach(UUID uuid) {
		// Alice never cuts a doll loose: a stray needs somewhere to hand its entry
		// back to, and she has no way to take one in. Only a suicide order asks for
		// this, and she issues none.
		return null;
	}

	// ---------- tick ----------

	@Override
	public void tickServer() {
		if (!(owner.level() instanceof ServerLevel sl)) return;
		if (owner.isDeadOrDying()) return;
		// a chunk reload is the one case that can invalidate the whole roster at
		// once, so repair and re-conjure land in the same tick instead of letting
		// the conjure gate below hide the repair for up to a second
		boolean repaired = reconcile(sl);
		commander.tick(owner);
		// restamp the follow-formation slots. The player ledger folds this into its
		// sidebar push; Alice has no sidebar, so she just pays for it here.
		commander.roster(owner);
		Post current = currentPost();
		// a new post re-rolls the roster size, so the retire/conjure passes below
		// always run against the size they were meant for
		boolean resized = rosterSize(current);
		retire(quota);
		if (repaired || resized || sl.getGameTime() % CONJURE_INTERVAL == 0) conjure(sl, quota);
		arm(current == Post.COMBAT);
		if (current == Post.COMBAT && sl.getGameTime() % COMMAND_INTERVAL == 0) command();
	}

	/**
	 * The post she is holding, read off the brain's active activity: hunting and
	 * fighting are combat, idling and playing are outdoors, everything else (at
	 * home, asleep, in conversation) counts as indoors.
	 */
	public Post currentPost() {
		Activity act = owner.getActivity();
		if (act == Activity.FIGHT || act == GLBrains.HUNT.get()) return Post.COMBAT;
		if (act == Activity.IDLE || act == Activity.PLAY) return Post.OUTDOOR;
		return Post.INDOOR;
	}

	/**
	 * How many dolls she wants out right now, rolling a fresh size whenever the
	 * post changes so two Alices — and two visits — do not field identical
	 * rosters. Returns whether the size changed this pass.
	 */
	private boolean rosterSize(Post current) {
		if (post == current.ordinal()) return false;
		post = current.ordinal();
		quota = Math.min(current.roll(owner.getRandom()), MAX_DOLLS);
		return true;
	}

	/**
	 * Per-entry watchdog, the counterpart of the player ledger's SUMMONED pass. An
	 * entry whose entity is gone — chunk unload, a dimension hop — is parked so the
	 * next conjure brings it back, and one that wandered off (too far, or into
	 * another world) is conjured again at her side. A <b>destroyed</b> doll is
	 * dropped from the ledger outright: she conjures dolls from air, so there is no
	 * count of how many she may have to respect and no broken one worth keeping —
	 * the conjure pass just makes another.
	 * <p>
	 * A doll that has <b>lost its host</b> is parked as well, and that is the
	 * self-healing case. It can neither follow nor be commanded, and with no paired
	 * entry the deferred damage pipeline has nothing to write, so it would otherwise
	 * sit there inert — and, because its uuid still resolves, still be counted as
	 * live, which suppresses the conjure meant to replace it. One such doll wedges
	 * the whole roster. The commander's own resolution refuses to count it, so
	 * parking it here lets the conjure pass bring a working doll back.
	 *
	 * @return whether any entry was parked or dropped, so the caller can re-conjure
	 *         immediately instead of waiting for its own interval
	 */
	private boolean reconcile(ServerLevel sl) {
		boolean repaired = false;
		for (var it = dolls.iterator(); it.hasNext(); ) {
			DollData data = it.next();
			if (data == null || !data.isSummoned()) continue;
			BaseDollEntity doll = resolve(data);
			if (doll == null) {
				data.state = DollState.TEMP;
				repaired = true;
			} else if (data.getHealth() <= 0) {
				doll.discard();
				it.remove();
				repaired = true;
			} else if (doll.level() != sl || doll.distanceTo(owner) >= PULLBACK_DISTANCE || doll.getHost() == null) {
				park(data);
				repaired = true;
			}
		}
		return repaired;
	}

	/**
	 * Her death takes the retinue with her, immediately rather than a tick later —
	 * a corpse does not keep ordering dolls about. The ledger itself is never
	 * written back: it goes down with her entity, exactly like the rest of her data.
	 */
	@Override
	public void onKilled() {
		for (DollData data : dolls) {
			if (data == null || !data.isSummoned()) continue;
			BaseDollEntity doll = resolve(data);
			if (doll != null) doll.discard();
		}
	}

	/** Parks every doll past {@code want}, newest first, so the elders keep their posts. */
	private void retire(int want) {
		int live = liveCount();
		for (int i = dolls.size() - 1; i >= 0 && live > want; i--) {
			DollData data = dolls.get(i);
			if (data == null || !data.isSummoned()) continue;
			park(data);
			live--;
		}
	}

	/** Writes the doll's values back, drops the entity, and parks the entry. */
	private void park(DollData data) {
		if (resolve(data) instanceof BaseDollEntity doll) {
			doll.writeValuesTo(data);
			doll.discard();
		}
		data.state = DollState.TEMP;
	}

	/** Brings the roster up to {@code want}, re-conjuring parked dolls before making new ones. */
	private void conjure(ServerLevel sl, int want) {
		int live = liveCount();
		while (live < want) {
			DollData data = parkedEntry();
			int slot;
			if (data == null) {
				if (atCap()) return;
				data = conjurer();
				dolls.add(data);
				slot = dolls.size() - 1;
			} else {
				slot = dolls.indexOf(data);
			}
			// spread the fresh materialization over the ring by its own slot in the
			// roster, so a returning doll does not pop in on top of a live one
			Vec3 pos = DollSpawn.ringPos(owner, slot, Math.max(1, want));
			if (pos == null) return;
			data.position = pos;
			data.yRot = owner.getYRot();
			BaseDollEntity doll = DollSpawn.materialize(sl, owner, data);
			if (doll == null) {
				data.state = DollState.TEMP;
				return;
			}
			sl.addFreshEntity(doll);
			live++;
		}
	}

	/** The first parked entry ready to come back, or null when they are all out. */
	@Nullable
	private DollData parkedEntry() {
		for (DollData data : dolls) {
			if (data == null || data.isSummoned() || data.getHealth() <= 0) continue;
			return data;
		}
		return null;
	}

	/**
	 * Hard bound on the ledger. Only ever reached by a retired doll still parked,
	 * since destroyed ones are dropped outright — the live roster is already capped
	 * by {@link #quota}, which never exceeds the top of {@link Post#COMBAT}.
	 */
	private boolean atCap() {
		return dolls.size() >= MAX_DOLLS;
	}

	/** A brand new doll entry: full health, unarmament, Alice's red. */
	private DollData conjurer() {
		DollData data = new DollData();
		data.type = DollItem.TYPE;
		data.color = DyeColor.RED;
		data.state = DollState.TEMP;
		return data;
	}

	/**
	 * A weapon in the main hand exactly while she is fighting, and a folded heal
	 * talisman in the off hand whenever there is anyone to spend it on. A doll is
	 * armed the moment combat starts and disarmed the moment it ends, so she never
	 * leaves a weapon in a doll's hand she is not paying for — and unarmed dolls
	 * cannot be handed an attack at all, since an attack order needs a
	 * {@link DollActionType#REGULAR_ATTACK} hand.
	 * <p>
	 * Which weapon is the roster's own business: the ledger slot decides, and the
	 * two halves get opposite ones. Even slots take a {@link GLItems#DOLL_LANCE},
	 * odd slots a star wand, so half the retinue charges and half shoots. The split
	 * is deliberately free of state — no assigned-weapon field to keep in step with
	 * the roster, and a doll that walks away from the fight and comes back finds
	 * the same thing in its hand. It is also the same ordering the follow
	 * formation uses, so the two weapons end up on opposite flanks of the arc over
	 * her head: the lances swing in from one side while the wands lay fire down from
	 * the other.
	 * <p>
	 * The talisman is conditional where the weapon is not, because it is not held for
	 * its own sake: it is held for {@link DollCommander}'s heal pass to spend. A doll
	 * standing in a house with nothing hurt in reach has nothing to heal, so it holds
	 * no charm — the same way it holds no weapon outside a fight. A talisman appears in
	 * the off hand as soon as she is fighting (<b>or</b> when the pass finds anyone
	 * hurt at all, which is the interesting case: it is off-duty and still patches up
	 * her dolls and herself) and leaves both hands again once nobody is hurt. Like the
	 * weapons it is hers, not loot: it goes back to the air it was folded from.
	 * <p>
	 * The two hands do not contend — {@code REGULAR_ATTACK} and {@code HEAL} are
	 * different action types, and a doll only ever holds one ticket.
	 * <p>
	 * This owns the <b>whole</b> loadout, not just the weapon, because the heal
	 * behaviour has a sticky swap: acting on an off-hand item moves it into the main
	 * hand and leaves it there — that is how a doll "decides" to act with a given
	 * hand. So a doll that has just healed is holding the talisman in the main hand
	 * and its weapon in the off hand. A pass that only policed the main hand would
	 * leave the talisman sitting there, and then refuel the off hand on top of it,
	 * which is how a doll ends up carrying two. So: hand the talisman back where it
	 * belongs — <b>keeping the stack</b> rather than burning a fresh one — and let the
	 * main hand be reclaimed for the weapon.
	 * <p>
	 * A doll holding a ticket is left completely alone. The swap is deliberate for
	 * the action in flight, and reloading the loadout out from under it would make
	 * the heal no-op on the very tick it resolves. The layout is repaired on the
	 * first idle tick after.
	 * <p>
	 * A doll that just <b>spent</b> an item is left alone too, for
	 * {@link DollHandLock#HOLD} ticks past the ticket. The one-shot animation is
	 * triggered by the action that spends the item, and heal, throw and laser all
	 * complete on the tick they fire — so a doll firing the last shot of a fight
	 * would otherwise have its wand taken away, or its talisman teleported back to
	 * the other hand, one tick into the swing that swing is playing.
	 * <p>
	 * Walks the whole ledger, not just the live dolls: a doll retired mid-fight is
	 * already parked by the time this runs, and it must not carry a weapon back out
	 * the next time she goes to the park.
	 */
	private void arm(boolean combat) {
		// whether a talisman is worth holding at all. Asked once per pass, so the
		// whole roster draws the same conclusion from the same moment in time
		boolean medic = combat || commander.healNeeded(owner);
		long now = owner.level().getGameTime();
		for (int slot = 0; slot < dolls.size(); slot++) {
			DollData data = dolls.get(slot);
			if (data == null || data.inventory == null) continue;
			DollEntity doll = resolve(data) instanceof DollEntity found ? found : null;
			// idle is not enough to touch it: a doll that spent something a moment ago
			// is still playing that swing, and its hands have to stay as drawn
			if (doll != null && (doll.actions.isActive() || doll.handLock.isLocked(now))) continue;
			MutableDollInventory inv = data.inventory;
			if (isTalisman(inv.get(DollSlot.MAIN_HAND))) {
				// a sticky swap left the talisman in the main hand, which the weapon
				// owns: hand it home keeping the stack rather than overwrite it and
				// burn a fresh one — and only when it is being kept at all
				if (medic && !DollBehaviors.isUsableHealTalisman(inv.get(DollSlot.OFF_HAND)))
					inv.set(DollSlot.OFF_HAND, inv.get(DollSlot.MAIN_HAND));
				inv.set(DollSlot.MAIN_HAND, ItemStack.EMPTY);
			}
			if (medic) {
				// refilled as it wears down: a spent talisman is no longer a heal hand,
				// and a healer that cannot heal is just a doll with a paper scrap
				if (!DollBehaviors.isUsableHealTalisman(inv.get(DollSlot.OFF_HAND)))
					inv.set(DollSlot.OFF_HAND, FoldedPaperTalisman.fold(GLTalismans.HEAL_TALISMAN.asStack()));
			} else if (isTalisman(inv.get(DollSlot.OFF_HAND))) {
				// nobody to spend it on and no fight to keep it ready for
				inv.set(DollSlot.OFF_HAND, ItemStack.EMPTY);
			}
			ItemStack main = inv.get(DollSlot.MAIN_HAND);
			if (combat) {
				ItemStack weapon = slot % 2 == 0 ? GLItems.DOLL_LANCE.asStack() : GLItems.STAR_WAND.asStack();
				// the lance needs an exact match (it is the only stack
				// DollBehaviors#doll_lance binds), so this is an identity test and not
				// "does it hit at all"
				if (!main.is(weapon.getItem())) inv.set(DollSlot.MAIN_HAND, weapon);
			} else if (!main.isEmpty()) {
				inv.set(DollSlot.MAIN_HAND, ItemStack.EMPTY);
			}
			if (doll != null) doll.syncLoadoutMirror();
		}
	}

	/** Whether a hand holds folded paper — a talisman of any kind, spent or not. */
	private static boolean isTalisman(ItemStack stack) {
		return stack.getItem() instanceof FoldedPaperTalisman;
	}

	// ---------- orders ----------

	/**
	 * Hands out one attack order per free doll, in two passes.
	 * <p>
	 * The first pass is a spread: every doll takes a mob no other doll is engaged
	 * with, so a crowd is dealt with rather than focused. The second pass is the
	 * overflow, and it is the point of the whole thing: once the mobs run out, the
	 * dolls that are still free pile onto the survivors. Without it a one-mob fight
	 * would have exactly one doll shooting and seven standing idle, which reads as
	 * the dolls not working at all.
	 * <p>
	 * Both passes skip a doll that cannot reach the mob they would be given. Half
	 * the roster holds a lance, and a charge has to <b>start</b> inside its engage
	 * range (§5.1b) — a lancer ordered onto a mob across the field would be handed a
	 * ticket its own doll throws away on the next tick, so the pass would re-offer
	 * the same dud forever while the shooter it displaced went unassigned. A doll
	 * with nothing in charge reach is simply left waiting in formation, which is
	 * also the honest answer: a lance is not a ranged weapon, and it engages when
	 * the mob comes to it.
	 * <p>
	 * Deliberately one-time rather than iterative: she re-tasks every free doll each
	 * pass instead of chaining a volley, so orders never queue behind one another and
	 * a dead target frees its doll immediately. What paces the shooting is the doll's
	 * own danmaku cooldown, so throughput is simply one shot per doll per cooldown —
	 * with a full combat roster that is a shot every half second from every doll,
	 * which is where the "non-stop" comes from.
	 */
	private void command() {
		var summoned = commander.summoned(owner);
		if (summoned.isEmpty()) return;
		List<LivingEntity> targets = validTargets();
		if (targets.isEmpty()) return;
		Set<UUID> claimed = new HashSet<>();
		for (var entry : summoned) {
			DollAction held = entry.doll().actions.getCurrent();
			if (held != null && held.target() != null) claimed.add(held.target());
		}
		// pass one: spread the roster over the distinct mobs
		for (var entry : summoned) {
			if (entry.doll().actions.isActive()) continue;
			LivingEntity target = nextFree(entry.doll(), targets, claimed);
			if (target == null) continue;
			issue(entry.doll(), target, claimed);
		}
		// pass two: nobody stands idle. The mobs ran out before the dolls did, so
		// the rest pile onto whatever is left — the first live target, which is the
		// one she is herself fixated on
		for (var entry : summoned) {
			if (entry.doll().actions.isActive()) continue;
			LivingEntity target = nextFree(entry.doll(), targets, null);
			if (target == null) continue;
			issue(entry.doll(), target, claimed);
		}
	}

	/** One-time attack order on a named target. Records it so the doll treats it as an enemy. */
	private void issue(DollEntity doll, LivingEntity target, Set<UUID> claimed) {
		var order = DollAction.oneTime(DollActionType.REGULAR_ATTACK, target.getUUID());
		if (!commander.issueTo(doll, order)) return;
		commander.markAttackTarget(target);
		claimed.add(target.getUUID());
	}

	/**
	 * The first target this doll could be ordered onto: unclaimed ones only when
	 * {@code claimed} is given, all of them otherwise. Reach is the same filter in
	 * both — see {@link #command} for why a lancer is passed over.
	 */
	@Nullable
	private static LivingEntity nextFree(DollEntity doll, List<LivingEntity> targets, @Nullable Set<UUID> claimed) {
		for (LivingEntity target : targets) {
			if (claimed != null && claimed.contains(target.getUUID())) continue;
			if (canReach(doll, target)) return target;
		}
		return null;
	}

	/**
	 * Whether an order on this target could actually run: the behavior this doll's
	 * weapon resolves to, asked whether it can close on the mob. A star wand
	 * answers for anything in shot range, a lance only inside its engage range.
	 * Asked per doll with a fresh behavior, so it costs nothing and leaves no state
	 * behind.
	 */
	private static boolean canReach(DollEntity doll, LivingEntity target) {
		return DollBehaviorRegistry.createFor(doll, DollActionType.REGULAR_ATTACK)
				.map(behavior -> behavior.canReach(doll, target))
				.orElse(false);
	}

	/**
	 * Everything Alice is actually hostile to, her melee target first. The target
	 * container tracks non-players only, so her current target is added by hand —
	 * she is perfectly willing to point the retinue at a player she has decided
	 * to hit.
	 */
	private List<LivingEntity> validTargets() {
		List<LivingEntity> out = new ArrayList<>();
		LivingEntity primary = owner.getTarget();
		if (primary != null && owner.targets.isValidTarget(primary)) out.add(primary);
		for (LivingEntity target : owner.targets.getTargets()) {
			if (!out.contains(target)) out.add(target);
		}
		return out;
	}

	private int liveCount() {
		return commander.summoned(owner).size();
	}

	/**
	 * The live doll an entry names, looked up in the dimension the entry recorded.
	 * Null when that world is gone or the entity is not there — dolls are never
	 * chunk-serialized (§5.8), so "not there" means gone rather than pending. This
	 * is the <b>raw</b> lookup, host and all: a doll that has lost its host still has
	 * to come back here, so that {@link #reconcile} can see it and park it. The
	 * commander's resolution is the filtered one, and that is what keeps a useless
	 * doll out of the live count.
	 */
	@Nullable
	private BaseDollEntity resolve(DollData data) {
		if (data.uuid == null || data.dimension == null) return null;
		var server = owner.level().getServer();
		if (server == null) return null;
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, data.dimension));
		if (level == null) return null;
		return level.getEntity(data.uuid) instanceof BaseDollEntity doll ? doll : null;
	}

	/** How many dolls Alice commands at once, by what she is doing. */
	public enum Post {
		/** At home, asleep, or talking: a couple of dolls minding the house. */
		INDOOR(1, 2),
		/** Out and about: a small escort. */
		OUTDOOR(3, 4),
		/** Hunting or fighting: the full set, all armed. */
		COMBAT(6, 8);

		private final int min, max;

		Post(int min, int max) {
			this.min = min;
			this.max = max;
		}

		public int roll(RandomSource random) {
			return min + random.nextInt(max - min + 1);
		}
	}

}
