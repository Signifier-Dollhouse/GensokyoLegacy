package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.behavior.module.FairyFeedModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.facet.YoukaiAnim;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Two beats of eating, in the order they have to happen in: she takes the dish into her hand and
 * chews it, and only once she has actually had some of it does she reach over and hand
 * something back. Doing the gift inline at feed time would hand it over while she is still
 * mid-chew, which reads as two unrelated things happening at once rather than one gesture.
 * <p>
 * {@code FairyFeedModule} leaves the feeding - player, the dish and the gift it earned -
 * in {@link GLBrains#MEM_FEED} rather than handing anything over itself, and
 * that memory is the whole handoff. It is registered on its own
 * {@link GLBrains#FEAST} activity rather than on CORE, because eating is the
 * one thing she stops for: an activity is an exclusive set of behaviors, so
 * this is what actually keeps the stroll and stay-tasks from queueing a walk
 * target underneath her while she has something in both hands.
 * <p>
 * What she hands back is not decided here: the module rolled it when she took
 * the food, so this only has to carry it to her hand. On a plain fairy a cookie
 * bought a flower and a cake bought fairy dust; on Cirno the same two tiers buy
 * packed ice and a crystal - this task cannot tell the difference, which is the
 * point of letting {@code FairyFeedModule} own the gift.
 * <p>
 * Typed to {@link FairyEntity} rather than to the plain fairy specifically,
 * because eating is the shared fairy's rule and Cirno is fed on the same terms.
 * Every method it calls is on {@code YoukaiEntity}, so the wider bound costs
 * nothing.
 */
public class FairyFeedTask extends Behavior<FairyEntity> {

	/**
	 * How long she chews before reaching for the gift. Matched to the length of
	 * the {@code 吃} clip, which is played and held, so she is mid-chew for the
	 * whole of it.
	 */
	private static final int EAT_TICKS = 45;
	/**
	 * When the dish leaves her hand: halfway through the chew, which is where the
	 * clip brings it to her mouth. Nothing takes it out of her hand after that -
	 * she is chewing with nothing to hold, and the crumbs carry on alone.
	 */
	private static final int SWALLOWED_AT = EAT_TICKS / 2;
	/**
	 * The {@code 使用主手物品} clip runs 0.31s. The gift leaves her hand halfway
	 * through it rather than at the start, so it arrives on the arm's way out
	 * instead of teleporting out of a gesture that has not begun.
	 */
	private static final int USE_MAINHAND_TICKS = 6;
	private static final int REACH_AHEAD_TICKS = USE_MAINHAND_TICKS / 2;

	public FairyFeedTask() {
		// runs past the throw: the behavior would otherwise time out mid-gesture and
		// the gift would never leave her hand at all
		super(Map.of(GLBrains.MEM_FEED.get(), MemoryStatus.VALUE_PRESENT),
				EAT_TICKS + USE_MAINHAND_TICKS, EAT_TICKS + USE_MAINHAND_TICKS);
	}

	private long startedAt;
	private boolean swallowed;
	private boolean reached;
	private boolean thrown;

	@Override
	protected void start(ServerLevel level, FairyEntity entity, long gameTime) {
		startedAt = gameTime;
		swallowed = false;
		reached = false;
		thrown = false;
		YoukaiAnim.EAT.play(entity);
		var fed = BrainUtils.getMemory(entity, GLBrains.MEM_FEED.get());
		if (fed == null) return;
		// stop where she is. Clearing the target matters as much as stopping the
		// navigation: anything still holding a walk target would re-issue it next move
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		entity.getNavigation().stop();
		entity.navCtrl.stopMoving();
		// she eats facing you. LookAtTargetSink is a CORE behavior, so it is already
		// running in this activity and only needs somewhere to point.
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new EntityTracker(fed.player(), true));
		// the dish goes in her hand here rather than at feed time, so it can only be
		// held by a chew that is actually under way - a memory that expires unrun
		// has no task whose stop could take it back out. Vanilla equipment sync carries
		// it to the client, which is all YoukaiHeldItemLayer reads.
		entity.setItemInHand(InteractionHand.MAIN_HAND, fed.food().copy());
		entity.startEating(fed.food(), EAT_TICKS);
	}

	/**
	 * Vanilla's default is {@code false}, which stops a behavior the instant it
	 * starts - {@code tick} never runs at all. A behavior that has to live for any
	 * length of time must say so here. Nothing else gates the run: the entry
	 * condition is re-checked on every start, and {@link #stop} clears the memory.
	 */
	@Override
	protected boolean canStillUse(ServerLevel level, FairyEntity entity, long gameTime) {
		return true;
	}

	/**
	 * Five things on three latches: stand still, chew, take the dish out of her hand, reach, then
	 * let go. Each fires once, and {@code tick} runs every tick the behavior is alive, so the
	 * gesture and the gift cannot come apart.
	 */
	@Override
	protected void tick(ServerLevel level, FairyEntity entity, long gameTime) {
		holdStill(entity);
		long age = gameTime - startedAt;
		if (!swallowed && age >= SWALLOWED_AT) {
			swallowed = true;
			entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
		if (!reached && age >= EAT_TICKS) {
			reached = true;
			YoukaiAnim.USE_MAINHAND.play(entity);
		}
		if (reached && !thrown && age >= EAT_TICKS + REACH_AHEAD_TICKS) {
			thrown = true;
			throwGift(entity);
		}
	}

	/**
	 * Kill any leftover momentum. Stopping the navigation and the move control is
	 * not enough on its own: {@code travel} bleeds horizontal velocity by 9% a tick
	 * rather than dropping it, so she coasts a good four blocks before she has
	 * actually stopped. Her hover is left alone.
	 */
	private static void holdStill(FairyEntity entity) {
		var v = entity.getDeltaMovement();
		entity.setDeltaMovement(0.0, v.y, 0.0);
	}

	/**
	 * Still their enemy: what she was given was food, not a peace offering, so
	 * there is no gift at all - the stack is simply never thrown. Asked here rather
	 * than carried over the wire, since a player can drop back below the line while
	 * she is still chewing.
	 */
	private static void throwGift(FairyEntity entity) {
		var fed = BrainUtils.getMemory(entity, GLBrains.MEM_FEED.get());
		if (fed == null) return;
		entity.getData(fed.player())
                .filter(data -> FairyFeedModule.welcomed(data.data()))
				.flatMap(data -> entity.getModule(FairyFeedModule.class))
				.ifPresent(feed -> feed.throwGift(fed.player(), fed.gift()));
	}

	/**
	 * Cleared here rather than when the gift is paid, so that a second dish fed
	 * mid-chew is not swallowed by the run already in flight: this one ends and the
	 * behavior starts again for the other. The hand goes back to empty with it - a chew
	 * cut short before {@link #SWALLOWED_AT} is the only case where the dish is still in it.
	 */
	@Override
	protected void stop(ServerLevel level, FairyEntity entity, long gameTime) {
		BrainUtils.clearMemory(entity, GLBrains.MEM_FEED.get());
		entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		entity.stopEating();
	}

}