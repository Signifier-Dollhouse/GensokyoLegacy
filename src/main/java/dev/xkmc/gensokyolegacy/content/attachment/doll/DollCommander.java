package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionMode;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehavior;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviors;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollHealBehavior;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanContext;
import dev.xkmc.gensokyolegacy.content.item.talisman.kinds.HealTalisman;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Command logic for one {@link DollLedger}: issues ONE_TIME / ITERATIVE actions, chains volleys
 * doll-to-doll, schedules heals, prunes dead marks. Server-only state, never persisted; ledger
 * transitions stay on the ledger. Every entry point takes the commanding {@link LivingEntity}.
 *
 * @see "doc/design/doll/control.md"
 */
public class DollCommander {

	private final DollLedger ledger;

	/**
	 * In-flight iterative actions. Identity set: the shared done-set mutates, so
	 * record equality is unstable.
	 */
	private final Set<DollAction> iterative = Collections.newSetFromMap(new IdentityHashMap<>());

	/**
	 * Heal-mark targets (entity uuids), session-only: forgotten on logout / death,
	 * never serialized.
	 */
	public final Set<UUID> healTargets = new LinkedHashSet<>();

	/** Commanded attack targets: doll-shot danmaku outlives the ticket that fired it, so everything
	 * uncommanded and non-hostile is spared. Transient like {@link #healTargets}, pruned per second. */
	public final Set<UUID> attackTargets = new LinkedHashSet<>();

	/** Follow-formation yaw, latched from the view yaw only when the commander moves horizontally:
	 * turning on the spot leaves every doll's slot frozen instead of tracking the cursor. */
	private float formationYaw;

	@Nullable
	private Vec3 formationAnchor;

	public DollCommander(DollLedger ledger) {
		this.ledger = ledger;
	}

	// ---------- follow formation ----------

	/**
	 * The latched follow-formation yaw, re-anchored once per tick from the commander's view yaw
	 * (see {@link #updateFormationAnchor}). Server-only.
	 */
	public float getFormationYaw() {
		return formationYaw;
	}

	private void updateFormationAnchor(LivingEntity owner) {
		var pos = owner.position();
		if (formationAnchor == null) {
			formationAnchor = pos;
			formationYaw = owner.getYRot();
			return;
		}
		double dx = pos.x - formationAnchor.x;
		double dz = pos.z - formationAnchor.z;
		if (dx * dx + dz * dz > 1.0) {
			formationAnchor = pos;
			formationYaw = owner.getYRot();
		}
	}

	// ---------- marks ----------

	public boolean isMarked(LivingEntity target) {
		return target != null && healTargets.contains(target.getUUID());
	}

	public void tick(LivingEntity owner) {
		for (DollData data : ledger.dolls()) {
			if (data.isSummoned()) maybeAutoHeal(owner, data);
		}
		lapse(owner);
		guardIterative(owner);
		if (owner.level().getGameTime() % 20 == 0) pruneHealMarks(owner);
		updateFormationAnchor(owner);
	}

	/**
	 * Counts every entry's lifespan down and drops the ones that ran out (timed_summon.md §3). Lives
	 * here because every ledger already ticks its commander, so expiry costs no per-host code, and
	 * because an entry is only ever dropped from here and from a host's own reconcile pass.
	 * <p>
	 * The countdown only runs while the host does, so a timed summon parked over an owner's logout
	 * loses no life to time it was not there.
	 */
	private void lapse(LivingEntity owner) {
		for (DollData data : new ArrayList<>(ledger.dolls())) {
			if (data == null || !data.tickLifespan()) continue;
			// entity first, entry second: the other order leaves one tick in which the doll
			// is alive and unpaired, which its own inverse check reads as an orphan
			Entity doll = resolveEntity(owner, data);
			if (doll != null) doll.discard();
			ledger.drop(data.uuid);
		}
	}

	/**
	 * Drops marks and commanded-attack records whose entity is gone or dead in every loaded
	 * dimension. Runs once per second; scheduling already skips such entries every tick.
	 */
	private void pruneHealMarks(LivingEntity owner) {
		var server = owner.level().getServer();
		if (server == null) return;
		healTargets.removeIf(id -> isGoneEverywhere(server, id));
		attackTargets.removeIf(id -> isGoneEverywhere(server, id));
	}

	private static boolean isGoneEverywhere(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(id) instanceof LivingEntity target && target.isAlive()) return false;
		}
		return true;
	}

	// ---------- glove API: one command, one doll, one execution ----------

	public boolean isCommandedTarget(LivingEntity target) {
		return target != null && attackTargets.contains(target.getUUID());
	}

	/** Volley: the first available doll starts an iterative regular attack. */
	public boolean issueVolley(LivingEntity owner, LivingEntity target) {
		attackTargets.add(target.getUUID());
		return issue(owner, DollAction.iterative(DollActionType.REGULAR_ATTACK, target.getUUID()));
	}

	/** Whether the commander has any summoned doll entity loaded nearby. */
	public boolean hasSummonedDoll(LivingEntity owner) {
		for (DollData data : ledger.dolls()) {
			if (resolveEntity(owner, data) instanceof DollEntity) return true;
		}
		return false;
	}

	/** Whether any summoned doll holds a valid weapon for the given action type (ignoring busy state). */
	public boolean hasCapableDoll(LivingEntity owner, DollActionType type) {
		for (DollData data : ledger.dolls()) {
			if (resolveEntity(owner, data) instanceof DollEntity doll &&
					DollBehaviorRegistry.findHand(doll, type).isPresent()) return true;
		}
		return false;
	}

	/** Whether any summoned doll could actually hit this target: a valid weapon for the type, and
	 * a behavior that can reach it — the reach half is what separates this from
	 * {@link #hasCapableDoll}: armed melee dolls cannot touch a mob across the field, so a host
	 * reports too far instead of ordering (control.md §5.1b). Asked per doll, fresh behavior. */
	public boolean canAttack(LivingEntity owner, LivingEntity target, DollActionType type) {
		for (DollData data : ledger.dolls()) {
			if (!(resolveEntity(owner, data) instanceof DollEntity doll)) continue;
			Optional<DollBehavior> behavior = DollBehaviorRegistry.createFor(doll, type);
			if (behavior.isPresent() && behavior.get().canReach(doll, target)) return true;
		}
		return false;
	}

	/** Super / suicide: exactly one available doll acts once. */
	public boolean issueOneTime(LivingEntity owner, LivingEntity target, DollActionType type) {
		return issue(owner, DollAction.oneTime(type, target.getUUID()));
	}

	/** Glove super / suicide: one random available doll acts once. Returns the picked doll, or null. */
	@Nullable
	public DollEntity issueOneTimeRandom(LivingEntity owner, LivingEntity target, DollActionType type) {
		attackTargets.add(target.getUUID());
		DollAction action = DollAction.oneTime(type, target.getUUID());
		long now = owner.level().getGameTime();
		List<DollEntity> acceptors = new ArrayList<>();
		for (DollData data : ledger.dolls()) {
			if (!(resolveEntity(owner, data) instanceof DollEntity doll)) continue;
			if (!doll.actions.canAccept(doll, action)) continue;
			acceptors.add(doll);
		}
		if (acceptors.isEmpty()) return null;
		DollEntity pick = acceptors.get(owner.getRandom().nextInt(acceptors.size()));
		return pick.actions.tryStart(action, now) ? pick : null;
	}

	/** A one-time order pinned to a specific doll, for a character that wants each of her dolls on
	 * a target of her own choosing. False when that doll cannot take the order. */
	public boolean issueTo(DollEntity doll, DollAction action) {
		if (!doll.actions.canAccept(doll, action)) return false;
		return doll.actions.tryStart(action, doll.level().getGameTime());
	}

	/**
	 * Records a target as commanded. Bullets outlive their ticket, so a doll that
	 * already fired at an entity keeps treating it as an enemy.
	 */
	public void markAttackTarget(LivingEntity target) {
		attackTargets.add(target.getUUID());
	}

	private boolean issue(LivingEntity owner, DollAction action) {
		long now = owner.level().getGameTime();
		for (DollData data : ledger.dolls()) {
			if (!(resolveEntity(owner, data) instanceof DollEntity doll)) continue;
			if (!doll.actions.canAccept(doll, action)) continue;
			if (!doll.actions.tryStart(action, now)) continue;
			if (action.mode() == DollActionMode.ITERATIVE) iterative.add(action);
			return true;
		}
		return false;
	}

	/** Stop: halt every doll and drop in-flight iterations (suicide dives are unstoppable once
	 * started), clear heal marks and commanded-attack records so post-stop bullets stand down. */
	public int stopAll(LivingEntity owner) {
		long now = owner.level().getGameTime();
		int n = 0;
		for (DollData data : ledger.dolls()) {
			if (!(resolveEntity(owner, data) instanceof DollEntity doll)) continue;
			DollAction held = doll.actions.getCurrent();
			if (held != null && held.type() == DollActionType.SUICIDE_ATTACK) continue;
			doll.actions.stop(now);
			n++;
		}
		iterative.clear();
		attackTargets.clear();
		healTargets.clear();
		return n;
	}

	/** Tells the next available doll to go ahead without releasing the local wait, used when an
	 * accepted iterative action stalls. The waiter keeps its own abort timer; nothing is added to
	 * {@code done} here. */
	public void handAhead(DollEntity from, DollAction action) {
		for (DollData data : ledger.dolls()) {
			if (data.uuid == null ||
					data.uuid.equals(from.getUUID()) || action.done().contains(data.uuid)) continue;
			if (!(resolveEntity(from, data) instanceof DollEntity doll)) continue;
			if (!doll.actions.canAccept(doll, action)) continue;
			if (!doll.actions.tryStart(action, from.level().getGameTime())) continue;
			iterative.add(action);
			return;
		}
	}

	/**
	 * Fellow summoned dolls of the same ledger (excluding the given one), for
	 * ally-aware firing lanes.
	 */
	public List<DollEntity> summonedAllies(DollEntity doll) {
		List<DollEntity> out = new ArrayList<>();
		for (DollData data : ledger.dolls()) {
			if (data.uuid == null || data.uuid.equals(doll.getUUID())) continue;
			if (resolveEntity(doll, data) instanceof DollEntity ally) out.add(ally);
		}
		return out;
	}

	/** Chain-passing for iterative actions: start the same action (shared done-set) on the next
	 * available doll, only while nobody else holds it — hand-ahead puts two dolls on one action,
	 * and without this guard a volley fans out instead of walking the roster (control.md §8).
	 * Returns false when the iteration ends. */
	public boolean handOff(DollEntity from, DollAction action) {
		if (heldBy(action, from)) return true;
		for (DollData data : ledger.dolls()) {
			if (data.uuid == null ||
					data.uuid.equals(from.getUUID()) || action.done().contains(data.uuid)) continue;
			if (!(resolveEntity(from, data) instanceof DollEntity doll)) continue;
			if (!doll.actions.canAccept(doll, action)) continue;
			if (!doll.actions.tryStart(action, from.level().getGameTime())) continue;
			iterative.add(action);
			return true;
		}
		iterative.remove(action);
		return false;
	}

	/**
	 * The live summoned dolls of this ledger, in ledger order, paired with their
	 * entry. Entries whose entity is not loaded right now are skipped.
	 */
	public List<Summoned> summoned(LivingEntity owner) {
		List<Summoned> out = new ArrayList<>();
		for (DollData data : ledger.dolls()) {
			if (data.isSummoned() && resolveEntity(owner, data) instanceof DollEntity doll)
				out.add(new Summoned(data, doll));
		}
		return out;
	}

	/** A materialized doll and the entry that owns it. */
	public record Summoned(DollData data, DollEntity doll) {
	}

	/**
	 * Restamps every live entry's follow-formation slot (index in ledger order, shared total) over
	 * every summoned doll whatever it can do — a gadget holds a slot like anything else — and
	 * returns the actionable ones as plain entity ids, which is what the sidebar roster rides
	 * (entity.md §9.1). Entries whose entity is not loaded right now are skipped.
	 */
	public List<RosterEntry> roster(LivingEntity owner) {
		List<DollData> live = new ArrayList<>();
		for (DollData data : ledger.dolls()) {
			if (data.isSummoned() && resolveEntity(owner, data) instanceof BaseDollEntity) live.add(data);
		}
		List<RosterEntry> next = new ArrayList<>();
		int total = Math.max(1, live.size());
		for (int i = 0; i < live.size(); i++) {
			DollData data = live.get(i);
			data.formationIndex = i;
			data.formationTotal = total;
			if (resolveEntity(owner, data) instanceof DollEntity doll)
				next.add(new RosterEntry(doll.getId(), data.uuid));
		}
		return next;
	}

	/** One materialized doll: its network id and the ledger key that pairs it. */
	public record RosterEntry(int entityId, UUID uuid) {
	}

	/**
	 * The live doll an entry names, or null when there is nothing to act on.
	 * <p>
	 * A doll whose host it cannot resolve is excluded on purpose: it can neither follow nor be commanded,
	 * and counting it live pushes the count above the roster size, suppressing the conjure that replaces it.
	 */
	@Nullable
	private Entity resolveEntity(Entity from, DollData data) {
		if (data.uuid == null || data.dimension == null) return null;
		var server = from.level().getServer();
		if (server == null) return null;
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, data.dimension));
		if (level == null) return null;
		if (!(level.getEntity(data.uuid) instanceof BaseDollEntity doll)) return null;
		return doll.getHost() == null ? null : doll;
	}

	/**
	 * Stall guard: a doll holding an iterative action that leaves SUMMONED (park / itemize) strands
	 * the chain, so orphaned actions are re-offered once per tick (control.md §8).
	 */
	private void guardIterative(LivingEntity owner) {
		for (DollAction action : new ArrayList<>(iterative)) {
			if (heldBy(action, owner)) continue;
			boolean reOffered = false;
			for (DollData data : ledger.dolls()) {
				if (data.uuid == null || action.done().contains(data.uuid)) continue;
				if (!(resolveEntity(owner, data) instanceof DollEntity doll)) continue;
				if (!doll.actions.canAccept(doll, action)) continue;
				if (!doll.actions.tryStart(action, owner.level().getGameTime())) continue;
				reOffered = true;
				break;
			}
			if (!reOffered) iterative.remove(action);
		}
	}

	private boolean heldBy(DollAction action, LivingEntity owner) {
		for (DollData data : ledger.dolls()) {
			if (resolveEntity(owner, data) instanceof DollEntity doll && doll.actions.holds(action)) return true;
		}
		return false;
	}

	/**
	 * The action type this doll already completed in a live iterative volley, if any. Only live
	 * in-flight volleys report done, so nothing needs clearing when the chain ends.
	 */
	public Optional<DollActionType> doneType(UUID uuid) {
		for (DollAction action : iterative) {
			if (action.done().contains(uuid)) return Optional.of(action.type());
		}
		return Optional.empty();
	}

	// ---------- scheduled heal: ledger-issued one-time orders, never doll-issued ----------

	/** Whether anyone the heal pass could spend a talisman on is hurt: the commander, any live doll
	 * of this ledger, or a marked entity — deliberately the set {@link #maybeAutoHeal} picks from
	 * (control.md §6). */
	public boolean healNeeded(LivingEntity owner) {
		if (HealTalisman.needsHeal(owner)) return true;
		for (DollData data : ledger.dolls()) {
			if (data.isSummoned() && resolveEntity(owner, data) instanceof DollEntity doll &&
					HealTalisman.needsHeal(doll)) return true;
		}
		if (owner.level() instanceof ServerLevel level) {
			for (UUID id : healTargets) {
				if (level.getEntity(id) instanceof LivingEntity target && HealTalisman.needsHeal(target)) return true;
			}
		}
		return false;
	}

	private void maybeAutoHeal(LivingEntity owner, DollData data) {
		if (!(resolveEntity(owner, data) instanceof DollEntity doll)) return;
		if (doll.actions.isActive()) return;
		long now = owner.level().getGameTime();
		if (doll.actions.isSuppressed(now)) return;
		var match = DollBehaviorRegistry.findHand(doll, DollActionType.HEAL);
		if (match.isEmpty()) return;
		var setup = DollBehaviors.findHealPaper(match.get().stack());
		if (setup.isEmpty()) return;
		HealTalisman paper = setup.get().paper();
		LivingEntity target = null;
		if (paper.test(owner)) target = owner;
		else if (paper.test(doll)) target = doll;
		else {
			target = findInjuredAlly(doll, paper);
			if (target == null) target = findHealMark(doll, paper);
		}
		if (target == null) return;
		doll.actions.tryStart(DollAction.auto(DollActionType.HEAL, target.getUUID()), now);
	}

	/**
	 * Nearest injured fellow summoned doll. Alongside the commander and the doll itself, fellow
	 * dolls of the same ledger are assumed default heal targets.
	 */
	@Nullable
	private LivingEntity findInjuredAlly(DollEntity doll, HealTalisman paper) {
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (DollEntity ally : summonedAllies(doll)) {
			if (!paper.test(ally)) continue;
			double dist = doll.distanceToSqr(ally);
			if (dist < bestDist) {
				bestDist = dist;
				best = ally;
			}
		}
		return best;
	}

	@Nullable
	private LivingEntity findHealMark(DollEntity doll, HealTalisman paper) {
		if (!(doll.level() instanceof ServerLevel level)) return null;
		for (UUID id : healTargets) {
			if (!(level.getEntity(id) instanceof LivingEntity target) || !target.isAlive()) continue;
			if (!paper.test(target)) continue;
			if (doll.distanceTo(target) > DollHealBehavior.MARK_RANGE) continue;
			if (new TalismanContext(target, ItemStack.EMPTY, 0, ItemStack.EMPTY, paper).isOnCooldown()) continue;
			return target;
		}
		return null;
	}

}
