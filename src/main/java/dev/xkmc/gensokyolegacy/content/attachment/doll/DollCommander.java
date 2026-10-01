package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionMode;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
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
 * Command logic for one {@link DollLedger}: issues ONE_TIME / ITERATIVE actions,
 * chains volleys doll-to-doll, schedules heals as one-time orders, and prunes dead
 * marks. Owns the in-flight iterative set and the follow-formation anchor
 * (server-only, never persisted). Ledger transitions (summon / itemize / park /
 * restore) stay on the ledger itself.
 * <p>
 * Every method takes the commanding {@link LivingEntity} — a player for the glove,
 * a character for her own retinue — so the same fan-out serves both.
 */
public class DollCommander {

	private final DollLedger ledger;

	/**
	 * In-flight iterative actions. Identity set: the shared done-set mutates, so
	 * record equality is unstable.
	 */
	private final Set<DollAction> iterative = Collections.newSetFromMap(new IdentityHashMap<>());

	/**
	 * Heal-mark targets (entity uuids). Deliberately transient: marks live for the
	 * session and are forgotten on logout / death — never serialized.
	 */
	public final Set<UUID> healTargets = new LinkedHashSet<>();

	/**
	 * Commanded attack targets (entity uuids): every issued attack records its
	 * target here. Doll-shot danmaku treats these as enemies even after the issuing
	 * ticket completes (bullets outlive tickets), so everything else uncommanded
	 * and non-hostile is spared. Transient like {@link #healTargets}: dead or
	 * vanished entries are pruned once per second.
	 */
	public final Set<UUID> attackTargets = new LinkedHashSet<>();

	/**
	 * Follow-formation anchor (server-only, never serialized). The formation yaw
	 * is latched from the commander's view yaw only when the commander actually
	 * moves horizontally — looking around while standing still leaves the anchor
	 * (and therefore every doll's slot target) frozen, so dolls never swirl around
	 * to track the cursor.
	 */
	private float formationYaw;
	@Nullable
	private Vec3 formationAnchor;

	/**
	 * Glove ray-trace target synced from the client (glove.md §2). A hint only:
	 * every use re-validates alive, range, and alliance server-side, and refreshes
	 * the timestamp on success.
	 */
	@Nullable
	public UUID gloveTarget;
	public long gloveTargetTime;

	public DollCommander(DollLedger ledger) {
		this.ledger = ledger;
	}

	// ---------- follow formation ----------

	/**
	 * The latched follow-formation yaw, re-anchored once per tick from the
	 * commander's view yaw (see {@link #updateFormationAnchor}). Server-only.
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
		guardIterative(owner);
		if (owner.level().getGameTime() % 20 == 0) pruneHealMarks(owner);
		updateFormationAnchor(owner);
	}

	/**
	 * Drops heal marks and commanded-attack records whose entities are gone or
	 * dead from every loaded dimension. Runs once per second; scheduling
	 * already skips such entries every tick.
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

	/**
	 * A one-time order pinned to a specific doll — the fan-out a character uses
	 * when she wants each of her dolls on a target of her choosing rather than
	 * whichever doll happens to be free first. Returns false when that doll
	 * cannot take the order.
	 */
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

	/**
	 * Stop: halt every doll and drop in-flight iterations — except suicide dives,
	 * which are unstoppable once started. Also clears heal marks and
	 * commanded-attack records, so post-stop bullets and heals stand down too.
	 * Returns dolls halted.
	 */
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

	/**
	 * Tells the next available doll to go ahead without releasing the local wait:
	 * used when an accepted iterative action stalls before starting. The waiter
	 * keeps its own attempt (and its own abort timer); nothing is added to
	 * {@code done} here.
	 */
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

	/**
	 * Chain-passing for iterative actions: starts the same (shared done-set) action
	 * on the next available summoned doll. Returns false when the iteration ends.
	 */
	public boolean handOff(DollEntity from, DollAction action) {
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
	 * Restamps every live entry's follow-formation slot (index in ledger order,
	 * shared total) and returns the same set as plain entity ids — the sidebar
	 * roster rides it, every host that keeps a formation reads the slot cache.
	 */
	public List<RosterEntry> roster(LivingEntity owner) {
		List<RosterEntry> next = new ArrayList<>();
		for (DollData data : ledger.dolls()) {
			if (!data.isSummoned()) continue;
			if (!(resolveEntity(owner, data) instanceof DollEntity doll)) continue;
			data.formationIndex = next.size();
			next.add(new RosterEntry(doll.getId(), data.uuid));
		}
		int total = Math.max(1, next.size());
		for (RosterEntry entry : next) {
			DollData data = dataOf(entry.uuid());
			if (data != null) data.formationTotal = total;
		}
		return next;
	}

	/** One materialized doll: its network id and the ledger key that pairs it. */
	public record RosterEntry(int entityId, UUID uuid) {
	}

	@Nullable
	private DollData dataOf(@Nullable UUID uuid) {
		if (uuid == null) return null;
		for (DollData data : ledger.dolls()) {
			if (uuid.equals(data.uuid)) return data;
		}
		return null;
	}

	@Nullable
	private Entity resolveEntity(Entity from, DollData data) {
		if (data.uuid == null || data.dimension == null) return null;
		var server = from.level().getServer();
		if (server == null) return null;
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, data.dimension));
		return level == null ? null : level.getEntity(data.uuid);
	}

	/**
	 * Stall guard: a doll holding an iterative action that leaves SUMMONED (park /
	 * itemize) strands the chain, so re-offer orphaned actions once per tick.
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
	 * The action type this doll already completed in a live iterative volley, if
	 * any. Drives the done state of the attack-glove sidebar: only live
	 * in-flight volleys report done, so nothing needs clearing when the chain ends.
	 */
	public Optional<DollActionType> doneType(UUID uuid) {
		for (DollAction action : iterative) {
			if (action.done().contains(uuid)) return Optional.of(action.type());
		}
		return Optional.empty();
	}

	// ---------- scheduled heal: ledger-issued one-time orders, never doll-issued ----------

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
	 * Nearest injured fellow summoned doll: alongside the commander and the doll
	 * itself, fellow dolls of the same ledger are assumed default heal targets.
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
