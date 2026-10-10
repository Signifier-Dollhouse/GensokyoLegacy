package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import com.google.common.collect.ImmutableMap;
import dev.xkmc.gensokyolegacy.content.entity.module.FairyCakeModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * Two beats of eating, in the order they have to happen in: she chews the cake
 * first, and only once she has actually had some of it does she reach over and
 * throw something back. Doing the gift inline at feed time would hand it over
 * while she is still mid-chew, which reads as two unrelated things happening at
 * once rather than one gesture.
 * <p>
 * {@code CakeFeedModule} leaves the player in {@link GLBrains#MEM_FEED} rather
 * than throwing itself, and that memory is the whole handoff. It is registered on
 * its own {@link GLBrains#FEAST} activity rather than on CORE, because eating is
 * the one thing she stops for: an activity is an exclusive set of behaviors, so
 * this is what actually keeps the stroll and stay-tasks from queueing a walk
 * target underneath her while she has a cake in both hands.
 */
public class FairyCakeTask extends Behavior<PlainFairyEntity> {

	/**
	 * How long she chews before reaching for the flower. Matched to the length of
	 * the {@code 吃} clip, which is played and held, so she is mid-chew for the
	 * whole of it and holds the last pose if anything runs long.
	 */
	private static final int EAT_TICKS = 45;
	/**
	 * The {@code 使用主手物品} clip runs 0.31s. The flower leaves her hand halfway
	 * through it rather than at the start, so the gift arrives on the arm's way out
	 * instead of teleporting out of a gesture that has not begun - three ticks in is
	 * where the hand has actually come up.
	 */
	private static final int USE_MAINHAND_TICKS = 6;
	private static final int REACH_AHEAD_TICKS = USE_MAINHAND_TICKS / 2;

	public FairyCakeTask() {
		// runs past the throw: the behavior would otherwise time out mid-gesture and
		// the flower would never leave her hand at all
		super(Map.of(GLBrains.MEM_FEED.get(), MemoryStatus.VALUE_PRESENT),
				EAT_TICKS + USE_MAINHAND_TICKS, EAT_TICKS + USE_MAINHAND_TICKS);
	}

	private long startedAt;
	private boolean reached;
	private boolean thrown;

	@Override
	protected void start(ServerLevel level, PlainFairyEntity entity, long gameTime) {
		startedAt = gameTime;
		reached = false;
		thrown = false;
		entity.broadcastEat();
		var player = BrainUtils.getMemory(entity, GLBrains.MEM_FEED.get());
		if (player == null) return;
		// stop where she is. Clearing the target matters as much as stopping the
		// navigation: anything still holding a walk target would re-issue it on the
		// very next move task and walk off with the cake.
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		entity.getNavigation().stop();
		entity.navCtrl.stopMoving();
		// she eats facing you. LookAtTargetSink is a CORE behavior, so it is already
		// running in this activity and only needs somewhere to point.
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
	}

	/**
	 * Vanilla's default is {@code false}, which stops a behavior the instant it
	 * starts - {@code tick} never runs at all, and only {@code start} ever fires. A
	 * behavior that has to live for any length of time must say so here; every other
	 * one that does in this codebase overrides it too.
	 * <p>
	 * Nothing else gates the run: the entry condition is re-checked on every start,
	 * and {@link #stop} clears the memory that started it.
	 */
	@Override
	protected boolean canStillUse(ServerLevel level, PlainFairyEntity entity, long gameTime) {
		return true;
	}

	/**
	 * Four things on two latches: stand still, chew, reach, then let go. Each fires
	 * once, and {@code tick} runs every tick the behavior is alive - which is every
	 * tick the animation is playing - so the gesture and the gift cannot come apart.
	 */
	@Override
	protected void tick(ServerLevel level, PlainFairyEntity entity, long gameTime) {
		holdStill(entity);
		long age = gameTime - startedAt;
		if (!reached && age >= EAT_TICKS) {
			reached = true;
			YoukaiAnim.USE_MAINHAND.play(entity);
		}
		if (reached && !thrown && age >= EAT_TICKS + REACH_AHEAD_TICKS) {
			thrown = true;
			throwFlower(entity);
		}
	}

	/**
	 * Kill any leftover momentum. Stopping the navigation and the move control is
	 * not enough on its own: {@code travel} bleeds horizontal velocity by 9% a tick
	 * rather than dropping it, so without this she coasts a good four blocks before
	 * she has actually stopped. Her hover is left alone - she stays up, she just
	 * stops travelling.
	 */
	private static void holdStill(PlainFairyEntity entity) {
		var v = entity.getDeltaMovement();
		entity.setDeltaMovement(0.0, v.y, 0.0);
	}

	/**
	 * Still their enemy: the cake was food, not a peace offering, so no flower. Asked
	 * here rather than carried over the wire, since a player can drop back below the
	 * line while she is still chewing.
	 */
	private static void throwFlower(PlainFairyEntity entity) {
		var player = BrainUtils.getMemory(entity, GLBrains.MEM_FEED.get());
		if (player == null) return;
		entity.getData(player)
				.filter(data -> FairyCakeModule.welcomed(data.data()))
				.ifPresent(data -> entity.getModule(FairyCakeModule.class)
						.ifPresent(cake -> cake.throwFlower(player)));
	}

	/**
	 * Cleared here rather than when the gift is paid, so that a second cake fed
	 * mid-chew is not swallowed by the run already in flight: this one ends, the
	 * memory is still set by that second cake, and the behavior starts again for it.
	 */
	@Override
	protected void stop(ServerLevel level, PlainFairyEntity entity, long gameTime) {
		BrainUtils.clearMemory(entity, GLBrains.MEM_FEED.get());
	}

}