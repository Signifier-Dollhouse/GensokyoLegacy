package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyGift;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.content.attachment.party.item.PartyMessage;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Every move a guest can be asked to make, over the state on {@link PartyModule}. This object
 * holds no state of its own: the durable state is serialized with the entity, which is what lets
 * a guest survive a save. No walk order is issued while she is off the ground, because these
 * entities all fly and an order given mid-air is answered by the flying one: the test is
 * {@code navCtrl.isCloseToGround}, since a walk node wants solid ground under the feet.
 *
 * @see PartyTasks for the machine these moves drive
 * @see "doc/design/party/guest.md"
 */
public class PartyContext {

	private final SmartYoukaiEntity entity;
	private final PartyModule party;
	private final ServerLevel level;
	private final PartyAttachment attachment;
	private final long gameTime;

	public PartyContext(SmartYoukaiEntity entity, ServerLevel level, @Nullable PartyAttachment attachment) {
		this.entity = entity;
		this.party = entity.getModule(PartyModule.class).orElseThrow();
		this.level = level;
		this.attachment = attachment;
		this.gameTime = level.getGameTime();
	}

	public SmartYoukaiEntity entity() {
		return entity;
	}

	public ServerLevel level() {
		return level;
	}

	public PartyModule party() {
		return party;
	}

	/** Her party this tick, or null when it has ended or its chunk has unloaded. */
	@Nullable
	public PartyAttachment attachment() {
		return attachment;
	}

	public long gameTime() {
		return gameTime;
	}

	/** Whether she is actually in a chair, which no field on the module can know. */
	public boolean seated() {
		return party.seat() != null && entity.isPassenger();
	}

	// Fields on PartyModule, read and written in place. A memory would have been the tidier
	// home, but one only survives a save if it carries a codec.

	public boolean holding() {
		return party.serve != null;
	}

	@Nullable
	public PartyServe serve() {
		return party.serve;
	}

	@Nullable
	public BlockPos dish() {
		return party.serveDish;
	}

	public void dish(@Nullable BlockPos pos) {
		party.serveDish = pos;
	}

	/** Whether the dish she committed to is close enough to take, from where she is now. */
	public boolean dishInReach() {
		var dish = dish();
		if (dish == null) return false;
		return PartyTarget.forDish(dish, standingNear(dish), seated()).inReach(this);
	}

	/** One tick of a countdown, floored at zero so a spent timer stays spent. */
	public static int step(int current) {
		return current > 1 ? current - 1 : 0;
	}

	public void hesitate(int ticks) {
		party.hesitate = Math.max(ticks, 0);
	}

	public void grab(int ticks) {
		party.grab = Math.max(ticks, 0);
	}

	public void rest(int ticks) {
		party.rest = Math.max(ticks, 0);
	}

	public boolean hesitating() {
		return party.hesitate > 0;
	}

	public int tickHesitate() {
		party.hesitate = step(party.hesitate);
		return party.hesitate;
	}

	public boolean grabbing() {
		return party.grab > 0;
	}

	public int tickGrab() {
		party.grab = step(party.grab);
		return party.grab;
	}

	public boolean chewing() {
		return party.chew > 0;
	}

	public int tickChew() {
		party.chew = step(party.chew);
		return party.chew;
	}

	public boolean resting() {
		return party.rest > 0;
	}

	public int tickRest() {
		party.rest = step(party.rest);
		return party.rest;
	}

	// The leave timer never expires on its own: armed at all *is* the departure, so zero
	// means "she has stood up and is on her way", not "over". Clearing it drops the cascade.

	public boolean leaving() {
		return party.leaveArmed;
	}

	/** Ticks left before she stands up; zero once she is already up. */
	public int leaveWait() {
		return party.leave;
	}

	/**
	 * Arm the departure, waiting {@code ticks} before she stands up. The gift is given here,
	 * not at the call sites, so a fourth way of ending a visit cannot forget it. Repeating is
	 * harmless: {@link PartyGift} is a no-op once she has given.
	 */
	public void armLeave(int ticks) {
		giveGift();
		party.leave = Math.max(ticks, 0);
		party.leaveArmed = true;
	}

	/** Arm the departure with a fresh per-guest wait, so a table does not empty at once. */
	public void armLeave() {
		armLeave(PartyTasks.unseatDelay(entity.getRandom()));
	}

	/** Count the unseat wait down, stopping at zero. */
	public void countLeave() {
		party.leave = step(party.leave);
	}

	/**
	 * Whether the host is still close enough to be worth sitting out the rest of the unseat wait
	 * for. An absent host, or one who has gone to another dimension, counts as far away: there is
	 * no one left to see her think it over.
	 */
	public boolean hostNearby() {
		var host = party.host();
		if (host == null) return false;
		var player = level.getPlayerByUUID(host);
		return player != null && player.level() == level
				&& player.distanceTo(entity) <= PartyTasks.HOST_LEAVE_DISTANCE;
	}

	// === movement ===

	/** Whether she can be told to walk anywhere at all. */
	public boolean canWalk() {
		return entity.onGround() || entity.navCtrl.isCloseToGround();
	}

	/** The standable block beside {@code subject}, or null when there is none. */
	@Nullable
	public BlockPos standingNear(BlockPos subject) {
		return PartySend.standingNear(level, subject);
	}

	/**
	 * Point her at {@code target}, issuing the walk order if she is not already walking. Issued
	 * once rather than once a tick: re-issuing restarts the pathfinding under her own feet, which
	 * is how a walk that would have succeeded ends up timing out.
	 *
	 * @return whether she is in range to act on it already
	 */
	public boolean walkTo(PartyTarget target) {
		if (target.inReach(this) && target.arrived(this)) return true;
		if (target.inReach(this)) {
			// in range but not standing on the tile: the order is what closes the gap
			order(target);
			return false;
		}
		if (!canWalk()) return false;
		order(target);
		return false;
	}

	private void order(PartyTarget target) {
		var walk = target.walkTo() == null ? target.interact() : target.walkTo();
		if (walk == null) return;
		// registered here rather than in walkTo, so the stall fallback covers every order that is
		// really given, and no clock runs at all while she is too high up to be given one
		party.walkingTo(walk, gameTime);
		if (BrainUtils.getMemory(entity, MemoryModuleType.WALK_TARGET) != null) return;
		// a passenger does not walk, and the chair she is riding will not go for her
		if (entity.isPassenger()) entity.stopRiding();
		BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(walk.getCenter(), 1, 1));
		if (target.interact() != null) {
			BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET,
					new BlockPosTracker(target.interact()));
		}
	}

	/** She got where she was sent. */
	public void arrived() {
		party.arrived();
	}

	/** Whether she is close enough to the chair or the dish to act on it. */
	public boolean inReach(PartyTarget target) {
		return target.inReach(this);
	}

	/** Drop the walk order and the destination. */
	public void clearWalk() {
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
		party.arrived();
	}

	// === seating ===

	/**
	 * Ask the party for this guest's chair.
	 *
	 * @return the target, or null when every chair is taken
	 */
	@Nullable
	public PartyTarget requestSeat() {
		var seat = attachment.claimSeat(level, entity.getUUID(), party.seat());
		if (seat == null) return null;
		return PartyTarget.forSeat(seat, standingNear(seat));
	}

	/** A target for her own chair, to carry a serving back to. */
	@Nullable
	public PartyTarget herSeat() {
		var seat = party.seat();
		if (seat == null) return null;
		return PartyTarget.forSeat(seat, standingNear(seat));
	}

	/** Sit on the chair this target names, and claim it. */
	public void sit(PartyTarget target) {
		var seat = target.interact();
		if (seat == null) return;
		HomeSearchUtil.setSitting(level, seat, entity);
		party.claimSeat(seat);
		clearWalk();
		arrived();
	}

	/** Give up the chair, releasing it back to the party's seating plan. */
	public void dropSeat() {
		party.dropSeat(level);
		if (entity.isPassenger()) entity.stopRiding();
	}

	/** Tell the host, once per episode, that there is no chair for her. */
	public void reportNoSeat() {
		PartyMessage.reportLookingForSeat(level, attachment, entity);
	}

	// === eating ===

	/**
	 * Take one serving off {@code dish} and put it in her hand.
	 *
	 * @return false when the dish is gone or off the spread, so the caller draws again
	 */
	public boolean takeServing(BlockPos dish) {
		var state = level.getBlockState(dish);
		if (!PartyFoodValue.isFood(level, dish, state) || !PartyArea.isLaidOut(level, dish, state)) {
			return false;
		}
		// read before consuming: a serve's readings belong to the state it was taken from
		var serve = new PartyServe(
				PartyFoodValue.hungerPerServe(level, dish, state),
				PartyFoodValue.appealPerServe(level.registryAccess(), level, dish, state));
		var stack = servingOf(dish);
		entity.setItemInHand(InteractionHand.MAIN_HAND, stack);
		entity.swing(InteractionHand.MAIN_HAND);
		YoukaiAnim.USE_MAINHAND.play(entity);
		party.serve = serve;
		// one serve only, so the rest of the party can still reach it
		PartyFoodValue.consume(level, dish, state);
		return true;
	}

	/** The stack one serving is worth: a slice, a bowl, or a board's pair of sushi. */
	private ItemStack servingOf(BlockPos dish) {
		for (var result : PartyFoodValue.results(level, dish, level.getBlockState(dish))) {
			if (result.valid()) return result.servingCopy();
		}
		return ItemStack.EMPTY;
	}

	/** Put down whatever she is carrying. */
	public void dropServing() {
		if (!entity.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
	}

	/**
	 * Credit and put down whatever she is carrying, for a visit that ends mid-bite. The plate
	 * left a dish that is already gone, so dropping it uncredited swallows food she did eat.
	 */
	public void finishServe() {
		bankServe();
		dropServing();
	}

	/**
	 * Credit the serving she was carrying and stop carrying it. The serialized serve is what
	 * makes this safe: a guest interrupted mid-bite still has it when she comes back.
	 */
	public void bankServe() {
		var serve = serve();
		if (serve == null) return;
		if (!serve.isEmpty()) party.eat(serve.hunger(), serve.appeal(), attachment);
		party.serve = null;
	}

	/**
	 * Begin the chew: sound, animation, timer, and the plate leaving her hand part-way through.
	 * Works seated or standing — the only guest who ever chews off a chair is one whose chair
	 * was taken away while she was carrying a plate.
	 */
	public void beginChew() {
		YoukaiAnim.EAT.play(entity);
		// the plate she is carrying is the crumb: Chew empties her hand part-way through,
		// so the crumbs have to come off the synced stack rather than off the hand
		entity.startEating(entity.getMainHandItem().copy(), PartyTasks.EAT_TICKS);
		level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EAT,
				SoundSource.PLAYERS, 0.8F, 1.0F);
		party.chew = PartyTasks.EAT_TICKS;
	}

	/** Halfway through a chew, in ticks — when the plate leaves her hand. */
	public int chewDrop() {
		return PartyTasks.EAT_TICKS / 2;
	}

	/**
	 * One dish, drawn at random with probability proportional to its appeal per serve.
	 *
	 * @return the dish, or null when the spread is empty
	 */
	@Nullable
	public BlockPos drawDish() {
		var access = level.registryAccess();
		var dishes = new ArrayList<BlockPos>();
		var weights = new ArrayList<Integer>();
		long total = 0;
		for (var pos : attachment.food()) {
			var state = level.getBlockState(pos);
			if (!PartyArea.isLaidOut(level, pos, state)) continue;
			if (!PartyFoodValue.isFood(level, pos, state)) continue;
			int appeal = PartyFoodValue.appealPerServe(access, level, pos, state);
			if (appeal <= 0) continue;
			dishes.add(pos.immutable());
			weights.add(appeal);
			total += appeal;
		}
		if (total <= 0) return null;
		// RandomSource has no bounded nextLong; nextDouble() is [0,1), so this lands in
		// [0, total) without putting the bound on the int range at all.
		long roll = (long) (entity.getRandom().nextDouble() * total);
		for (int i = 0; i < dishes.size(); i++) {
			roll -= weights.get(i);
			if (roll < 0) return dishes.get(i);
		}
		return dishes.getLast();
	}

	// === leaving ===

	/** Leave a gift, once per visit. */
	public void giveGift() {
		PartyGift.give(level, entity, party);
	}

	/** Give up on the party because there is nowhere to sit. */
	public void expel() {
		PartyMessage.reportEvicted(level, attachment, entity);
		dismiss();
	}

	/**
	 * The hard exit, for the cases where there is nothing to walk away from. Distinct from
	 * {@link #finishLeaving}: this guest is gone now, rather than having walked out.
	 */
	public void dismiss() {
		clearWalk();
		finishServe();
		party.dismiss();
	}

	/** Finish leaving: come off the party's books, then restore or discard. */
	public void finishLeaving() {
		clearWalk();
		finishServe();
		party.completeLeave(level);
	}

	/**
 * Where she goes when the party is over. Asked for on every tick of the walkout, so it is
 * drawn once and kept: drawn per call, a guest re-rolls it each tick and never arrives.
 *
 * @return a destination, or null to finish leaving here and now
 */
@Nullable
public BlockPos destination() {
		var drawn = party.leaveDest;
		if (drawn != null) return drawn;
		drawn = pickDestination();
		if (drawn != null) party.leaveTo(drawn);
		return drawn;
	}

	@Nullable
	private BlockPos pickDestination() {
		var table = party.table();
		if (table == null) return null;
		if (party.origin() == PartyModule.Origin.SUMMONED) return walkOut(table);
		var key = party.originKey();
		if (key == null || !level.dimension().equals(key.getDim())) return null;
		// a visit whose window has closed: she is leaving the party, not going home from
		// it, and a long walk to a house that is no longer having her is a worse ending
		if (party.origin() == PartyModule.Origin.VISITING) {
			var visit = entity.getModule(VisitModule.class).orElse(null);
			if (visit == null || visit.remaining(level) <= 0) return null;
		}
		var home = IHomeHolder.of(level, key);
		var center = home == null ? null : home.getWanderCenter();
		return center == null ? key.pos() : center;
	}

	/**
	 * Somewhere a summoned guest walks off to before she is put away — far, because that is how
	 * far the host sees her go. Checked twice, on the roll and on the ground found for it: a
	 * search outward from a too-close roll can return a neighbouring block.
	 *
	 * @return somewhere to walk to, or null when nothing that far out is walkable
	 */
	@Nullable
	private BlockPos walkOut(BlockPos table) {
		int min = GLModConfig.SERVER.partyGuestRadius.get() + 12;
		int max = min + PartyTasks.WALK_OUT_SPREAD;
		var random = entity.getRandom();
		for (int attempt = 0; attempt < PartyTasks.WALK_OUT_TRIES; attempt++) {
			var spot = new BlockPos(table.getX() + random.nextInt(-max, max + 1),
					table.getY(), table.getZ() + random.nextInt(-max, max + 1));
			if (!farEnough(spot, table, min)) continue;
			var away = PartySend.freeSpaceNear(level, spot, 4);
			if (away == null) continue;
			var dest = BlockPos.containing(away);
			if (farEnough(dest, table, min)) return dest;
		}
		return null;
	}

	private static boolean farEnough(BlockPos dest, BlockPos table, int min) {
		long dx = (long) dest.getX() - table.getX();
		long dz = (long) dest.getZ() - table.getZ();
		return dx * dx + dz * dz >= (long) min * min;
	}
}