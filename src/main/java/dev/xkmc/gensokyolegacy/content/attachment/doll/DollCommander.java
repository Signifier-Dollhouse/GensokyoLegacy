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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Player-facing doll command logic, accessed via {@link DollAttachment#commands}.
 * Issues ONE_TIME / ITERATIVE actions, chains volleys doll-to-doll, schedules
 * heals as one-time orders, and prunes dead marks. Owns the in-flight iterative
 * set (server-only, never persisted). Ledger transitions (summon / itemize /
 * park / restore) stay on {@link DollAttachment}.
 */
public class DollCommander {

	private final DollAttachment attachment;

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
	 * Glove ray-trace target synced from the client (glove.md §2). A hint only:
	 * every use re-validates alive, range, and alliance server-side, and refreshes
	 * the timestamp on success.
	 */
	@Nullable
	public UUID gloveTarget;
	public long gloveTargetTime;

	public DollCommander(DollAttachment attachment) {
		this.attachment = attachment;
	}

	public boolean isMarked(LivingEntity target) {
		return target != null && healTargets.contains(target.getUUID());
	}

	public void tick(ServerPlayer player) {
		for (DollData data : attachment.dolls().values()) {
			if (data.isSummoned()) maybeAutoHeal(player, data);
		}
		guardIterative(player);
		if (player.level().getGameTime() % 20 == 0) pruneHealMarks(player);
	}

	/**
	 * Drops marks whose entities are gone or dead from every loaded dimension.
	 * Runs once per second; scheduling already skips such marks every tick.
	 */
	private void pruneHealMarks(ServerPlayer player) {
		var server = player.serverLevel().getServer();
		healTargets.removeIf(id -> {
			for (ServerLevel level : server.getAllLevels()) {
				if (level.getEntity(id) instanceof LivingEntity target && target.isAlive()) return false;
			}
			return true;
		});
	}

	// ---------- glove API: one command, one doll, one execution ----------

	/** Volley: the first available doll starts an iterative regular attack. */
	public boolean issueVolley(ServerPlayer player, LivingEntity target) {
		return issue(player, DollAction.iterative(DollActionType.REGULAR_ATTACK, target.getUUID()));
	}

	/** Super / suicide: exactly one available doll acts once. */
	public boolean issueOneTime(ServerPlayer player, LivingEntity target, DollActionType type) {
		return issue(player, DollAction.oneTime(type, target.getUUID()));
	}

	/** Glove super / suicide: one random available doll acts once. Returns the picked doll, or null. */
	@Nullable
	public DollEntity issueOneTimeRandom(ServerPlayer player, LivingEntity target, DollActionType type) {
		DollAction action = DollAction.oneTime(type, target.getUUID());
		long now = player.level().getGameTime();
		List<DollEntity> acceptors = new ArrayList<>();
		for (DollData data : attachment.dolls().values()) {
			if (!data.isSummoned() || data.uuid == null) continue;
			ServerLevel level = attachment.getLevel(player, data);
			if (level == null || !(level.getEntity(data.uuid) instanceof DollEntity doll)) continue;
			if (!doll.actions.canAccept(doll, action)) continue;
			acceptors.add(doll);
		}
		if (acceptors.isEmpty()) return null;
		DollEntity pick = acceptors.get(player.getRandom().nextInt(acceptors.size()));
		return pick.actions.tryStart(action, now) ? pick : null;
	}

	private boolean issue(ServerPlayer player, DollAction action) {
		long now = player.level().getGameTime();
		for (DollData data : attachment.dolls().values()) {
			if (!data.isSummoned() || data.uuid == null) continue;
			ServerLevel level = attachment.getLevel(player, data);
			if (level == null || !(level.getEntity(data.uuid) instanceof DollEntity doll)) continue;
			if (!doll.actions.canAccept(doll, action)) continue;
			if (!doll.actions.tryStart(action, now)) continue;
			if (action.mode() == DollActionMode.ITERATIVE) iterative.add(action);
			return true;
		}
		return false;
	}

	/** Stop: halt every doll and drop in-flight iterations. Returns dolls halted. */
	public int stopAll(ServerPlayer player) {
		long now = player.level().getGameTime();
		int n = 0;
		for (DollData data : attachment.dolls().values()) {
			if (!data.isSummoned() || data.uuid == null) continue;
			ServerLevel level = attachment.getLevel(player, data);
			if (level == null || !(level.getEntity(data.uuid) instanceof DollEntity doll)) continue;
			doll.actions.stop(now);
			n++;
		}
		iterative.clear();
		return n;
	}

	/**
	 * Tells the next available doll to go ahead without releasing the local wait:
	 * used when an accepted iterative action stalls before starting. The waiter
	 * keeps its own attempt (and its own abort timer); nothing is added to
	 * {@code done} here.
	 */
	public void handAhead(DollEntity from, DollAction action) {
		for (DollData data : attachment.dolls().values()) {
			if (!data.isSummoned() || data.uuid == null ||
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
		for (DollData data : attachment.dolls().values()) {
			if (!data.isSummoned() || data.uuid == null || data.uuid.equals(doll.getUUID())) continue;
			if (resolveEntity(doll, data) instanceof DollEntity ally) out.add(ally);
		}
		return out;
	}

	/**
	 * Chain-passing for iterative actions: starts the same (shared done-set) action
	 * on the next available summoned doll. Returns false when the iteration ends.
	 */
	public boolean handOff(DollEntity from, DollAction action) {
		for (DollData data : attachment.dolls().values()) {
			if (!data.isSummoned() || data.uuid == null ||
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

	@Nullable
	private Entity resolveEntity(DollEntity from, DollData data) {
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
	private void guardIterative(ServerPlayer player) {
		for (DollAction action : new ArrayList<>(iterative)) {
			if (heldBy(action, player)) continue;
			boolean reOffered = false;
			for (DollData data : attachment.dolls().values()) {
				if (!data.isSummoned() || data.uuid == null || action.done().contains(data.uuid)) continue;
				ServerLevel level = attachment.getLevel(player, data);
				if (level == null || !(level.getEntity(data.uuid) instanceof DollEntity doll)) continue;
				if (!doll.actions.canAccept(doll, action)) continue;
				if (!doll.actions.tryStart(action, player.level().getGameTime())) continue;
				reOffered = true;
				break;
			}
			if (!reOffered) iterative.remove(action);
		}
	}

	private boolean heldBy(DollAction action, ServerPlayer player) {
		for (DollData data : attachment.dolls().values()) {
			if (!data.isSummoned() || data.uuid == null) continue;
			ServerLevel level = attachment.getLevel(player, data);
			if (level == null || !(level.getEntity(data.uuid) instanceof DollEntity doll)) continue;
			if (doll.actions.holds(action)) return true;
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

	private void maybeAutoHeal(ServerPlayer player, DollData data) {
		if (data.uuid == null) return;
		ServerLevel level = attachment.getLevel(player, data);
		if (level == null || !(level.getEntity(data.uuid) instanceof DollEntity doll)) return;
		if (doll.actions.isActive()) return;
		long now = level.getGameTime();
		if (doll.actions.isSuppressed(now)) return;
		var match = DollBehaviorRegistry.findHand(doll, DollActionType.HEAL);
		if (match.isEmpty()) return;
		var setup = DollBehaviors.findHealPaper(match.get().stack());
		if (setup.isEmpty()) return;
		HealTalisman paper = setup.get().paper();
		LivingEntity target = null;
		if (paper.test(player)) target = player;
		else if (paper.test(doll)) target = doll;
		else {
			target = findInjuredAlly(doll, paper);
			if (target == null) target = findHealMark(doll, paper);
		}
		if (target == null) return;
		doll.actions.tryStart(DollAction.oneTime(DollActionType.HEAL, target.getUUID()), now);
	}

	/**
	 * Nearest injured fellow summoned doll: alongside the owner and the doll
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
