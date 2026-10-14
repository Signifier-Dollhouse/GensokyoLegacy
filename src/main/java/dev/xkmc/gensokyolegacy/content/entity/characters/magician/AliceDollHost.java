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
 * Alice's retinue ledger: she is a {@link DollHost} in her own right, so her dolls pair,
 * follow and fight through the same machinery as a player's. A character host has no items,
 * a roster sized by {@link Post} and orders she issues herself (host.md §2–§5).
 *
 * @see "doc/design/doll/host.md"
 */
@SerialClass
public class AliceDollHost extends AbstractYoukaiModule implements DollLedger {

	private static final ResourceLocation ID = GensokyoLegacy.loc("alice_dolls");

	/**
	 * Hard ledger bound, the top of {@link Post#COMBAT}. Only retired dolls linger in
	 * it — destroyed ones are dropped — so the live roster is really {@link #quota}.
	 */
	public static final int MAX_DOLLS = 8;

	/** Reconjure at most this often, so a doll dying never becomes a spawn loop. */
	private static final int CONJURE_INTERVAL = 20;

	/** Order pass period. The dolls' own shot cooldowns pace the actual firing. */
	private static final int COMMAND_INTERVAL = 5;

	/**
	 * Her dolls in conjuring order, created on demand up to {@link #MAX_DOLLS} times. A
	 * destroyed entry is dropped rather than repaired, so nothing is conjured twice.
	 */
	@SerialField
	private final List<DollData> dolls = new ArrayList<>();

	/** Ordinal of the {@link Post} the roster was last sized for, -1 before the first. */
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
	 * The ledger exposed on {@link AliceEntity} itself: a doll asks its owning entity for a
	 * host, and a module is not one — {@link DelegateDollHost} makes that a single method.
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
		// back to, and she has no way to take one in.
		return null;
	}

	// ---------- tick ----------

	@Override
	public void tickServer() {
		if (!(owner.level() instanceof ServerLevel sl)) return;
		if (owner.isDeadOrDying() || owner.isRemoved()) return;
		// a chunk reload can invalidate the whole roster at once, so repair and
		// re-conjure land in the same tick instead of hiding behind the conjure gate
		boolean repaired = reconcile(sl);
		commander.tick(owner);
		// restamp the follow-formation slots; the player ledger folds this into its
		// sidebar push and Alice has no sidebar
		commander.roster(owner);
		Post current = currentPost();
		// a re-roll runs before retire/conjure so both passes see the new size
		boolean resized = rosterSize(current);
		retire(quota);
		if (repaired || resized || sl.getGameTime() % CONJURE_INTERVAL == 0) conjure(sl, quota);
		arm(current == Post.COMBAT);
		if (current == Post.COMBAT && sl.getGameTime() % COMMAND_INTERVAL == 0) command();
	}

	/** Combat while hunting or fighting, outdoors while idling or playing, indoors otherwise. */
	public Post currentPost() {
		Activity act = owner.getActivity();
		if (act == Activity.FIGHT || act == GLBrains.HUNT.get()) return Post.COMBAT;
		if (act == Activity.IDLE || act == Activity.PLAY) return Post.OUTDOOR;
		return Post.INDOOR;
	}

	/**
	 * How many dolls she wants out now, rolled only on a post change, so two Alices never field identical rosters.
	 *
	 * @return whether the size changed this pass
	 */
	private boolean rosterSize(Post current) {
		if (post == current.ordinal()) return false;
		post = current.ordinal();
		quota = Math.min(current.roll(owner.getRandom()), MAX_DOLLS);
		return true;
	}

	/**
	 * Per-entry watchdog: gone entities and wandered-off dolls are parked for the next
	 * conjure, destroyed dolls are dropped outright (she conjures from air, so there is
	 * no count of dolls to respect), and a doll that lost its host is parked too — its
	 * uuid still resolves, so it counts as live and would suppress its own replacement.
	 *
	 * @return whether anything was parked or dropped, so the caller re-conjures now
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

	/** Her death takes the retinue with her at once; the ledger goes down with her entity. */
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
			// spread the fresh materialization over the ring by its own roster slot,
			// so a returning doll does not pop in on top of a live one
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

	/** Hard bound on the ledger; only a retired doll still parked can reach it. */
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
	 * The whole loadout, every pass: a weapon in the main hand exactly while she fights, a
	 * folded heal talisman in the off hand only while someone can be spent on. A doll holding
	 * a ticket or inside its {@link DollHandLock#HOLD}-tick hand lock is left alone
	 * (host.md §4.2).
	 */
	private void arm(boolean combat) {
		// whether a talisman is worth holding at all, asked once per pass so the
		// whole roster draws the same conclusion from the same moment in time
		boolean medic = combat || commander.healNeeded(owner);
		long now = owner.level().getGameTime();
		for (int slot = 0; slot < dolls.size(); slot++) {
			DollData data = dolls.get(slot);
			if (data == null || data.inventory == null) continue;
			DollEntity doll = resolve(data) instanceof DollEntity found ? found : null;
			// idle is not enough: a doll that spent something a moment ago is still
			// playing that swing, and its hands have to stay as drawn
			if (doll != null && (doll.actions.isActive() || doll.handLock.isLocked(now))) continue;
			MutableDollInventory inv = data.inventory;
			if (isTalisman(inv.get(DollSlot.MAIN_HAND))) {
				// a sticky swap left the talisman in the main hand, which the weapon
				// owns: hand it home keeping the stack, or every heal burns a talisman
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
				// the lance is bound by an exact item match, so this is an identity
				// test and not "does it hit at all"
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
	 * Hands out one attack order per free doll, in two passes: spread over the distinct mobs,
	 * then an overflow so nobody idles. Both skip a doll that cannot reach the mob — half the
	 * roster holds a lance, whose charge must start inside its engage range (host.md §5).
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
		// pass two: the mobs ran out before the dolls did, so the rest pile onto the
		// first live target — the one she is herself fixated on
		for (var entry : summoned) {
			if (entry.doll().actions.isActive()) continue;
			LivingEntity target = nextFree(entry.doll(), targets, null);
			if (target == null) continue;
			issue(entry.doll(), target, claimed);
		}
	}

	/** One-time attack order on a named target. Recorded so the doll treats it as an enemy. */
	private void issue(DollEntity doll, LivingEntity target, Set<UUID> claimed) {
		var order = DollAction.oneTime(DollActionType.REGULAR_ATTACK, target.getUUID());
		if (!commander.issueTo(doll, order)) return;
		commander.markAttackTarget(target);
		claimed.add(target.getUUID());
	}

	/**
	 * The first target this doll could be ordered onto: unclaimed ones only when
	 * {@code claimed} is given, all of them otherwise. Reach is the same filter in both.
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
	 * Whether an order on this target could run: the behavior this doll's weapon resolves to,
	 * asked whether it can close on the mob. Asked with a fresh behavior, so it leaves none.
	 */
	private static boolean canReach(DollEntity doll, LivingEntity target) {
		return DollBehaviorRegistry.createFor(doll, DollActionType.REGULAR_ATTACK)
				.map(behavior -> behavior.canReach(doll, target))
				.orElse(false);
	}

	/**
	 * Everything Alice is hostile to, her melee target first: the target container
	 * tracks non-players only, so a player she has decided to hit is added by hand.
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
	 * The live doll an entry names, looked up in the dimension the entry recorded. Null when
	 * that world is gone or the entity is not there — dolls are never chunk-serialized, so
	 * "not there" means gone. This is the <b>raw</b> lookup, host included, so {@link #reconcile}
	 * can still park a doll that lost its host; the commander's lookup is the filtered one.
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
