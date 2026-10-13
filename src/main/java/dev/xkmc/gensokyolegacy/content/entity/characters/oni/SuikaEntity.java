package dev.xkmc.gensokyolegacy.content.entity.characters.oni;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.core.YoukaiAcquireTargetTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.core.YoukaiNapTask;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

/**
 * Ibuki Suika, the oni who lives on her own at the edge of the Bamboo Forest.
 * <p>
 * She has no home in this mod: there is no bamboo forest to put one in, so like
 * {@code SakuyaEntity} she only ever turns up as a guest — see {@code GLStructureGen}
 * for the house she visits. That is why she is registered without a spawn egg.
 * <p>
 * Her content is deliberately unwritten for now: {@code SuikaQDGen} gives her the
 * greeting lines and nothing else, so she is met with an empty topic list.
 * <p>
 * She is also the only character who naps: {@link #napChance} buys her the
 * {@link GLBrains#NAP} activity, which is where her {@code 侧卧} clip is spent. It is
 * an activity rather than a fidget because a nap has to be able to end - she gets up for
 * a fight or a party - and a one-shot animation has no way to be interrupted. A
 * conversation does not get her up: she answers from where she is lying, in
 * {@link #NAP_TALK}.
 * <p>
 * Her rig is the narrowest one in the mod. It ships no {@code 眨眼} blink, no
 * greeting or thinking clips, and only two {@code 交流_*} ones, so {@link #getAnim}
 * answers empty for most dialog slots and {@link #hasBlinkAnim} says no: naming a
 * clip the rig does not have resolves it to null, and the controller handed the
 * empty queue logs "Unable to find animation" and writes nothing, every frame.
 */
@SerialClass
public class SuikaEntity extends BossYoukaiEntity implements GeoYoukaiAnim {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("待机");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("走路");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("坐下");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("睡觉");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");
	private static final RawAnimation TALK_01 = RawAnimation.begin().thenPlay("交流_01");
	private static final RawAnimation TALK_02 = RawAnimation.begin().thenPlay("交流_02");
	/**
	 * {@code 侧卧_交流}: {@link #NAP}'s lying pose with the talking motion written into
	 * it. She talks while she naps, so every dialog trigger is re-pointed at this while
	 * she is down - see {@link #syncDialogAnims}. Played, not looped, like any other
	 * dialog one-shot: the state controller holds {@code 侧卧} underneath it the whole
	 * time, and both clips are the same pose, so the hand-off at either end of the
	 * 1.75s is invisible.
	 */
	private static final RawAnimation NAP_TALK = RawAnimation.begin().thenPlay("侧卧_交流");
	/**
	 * {@code 侧卧}, side-lying, looped rather than played: the clip is authored lying
	 * down throughout and only wobbles a little, so it is a pose to hold and not a
	 * flourish to sit through. Held by the state controller for as long as
	 * {@link GLBrains#NAP} lasts - see {@link #stateAnimController} - rather than as a
	 * {@link YoukaiAnim} one-shot, because the nap is an activity with a length and a
	 * one-shot would end after a single 1.75s cycle.
	 */
	private static final RawAnimation NAP = RawAnimation.begin().thenLoop("侧卧");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	/**
	 * Whether {@link #syncDialogAnims} has already pointed the controller's dialog
	 * triggers at {@link #NAP_TALK}. Client side only, and only ever read to skip a
	 * second identical pass.
	 */
	private boolean dialogsNapping;

	public SuikaEntity(EntityType<? extends SuikaEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public PlayState stateAnimController(AnimationState<?> event) {
		if (isSleeping()) {
			return event.setAndContinue(SLEEP);
		}
		if (isPassenger()) {
			return event.setAndContinue(SIT);
		}
		// Above the moving test on purpose: she lies where she stopped, so a nap that
		// catches the last of a walk has to finish rather than cut back to 走路.
		if (getFlag(YoukaiFlags.NAPPING)) {
			return event.setAndContinue(NAP);
		}
		if (getFlag(YoukaiFlags.FLYING)) {
			return event.setAndContinue(IDLE);
		}
		if (event.isMoving()) {
			return event.setAndContinue(WALK);
		}
		return event.setAndContinue(IDLE);
	}

	/**
	 * The only rig in the mod that gathers, {@link GeoYoukaiAnim#GATHER}: her spell
	 * card fires it from the pull move.
	 */
	@Override
	public boolean hasGatherAnim() {
		return true;
	}

	/**
	 * No {@code 眨眼} blink: naming a clip this rig does not have leaves
	 * {@link GeoYoukaiAnim#registerControllers} with a blink controller that logs
	 * "Unable to find animation" on every frame — see the class comment.
	 */
	@Override
	public boolean hasBlinkAnim() {
		return false;
	}

	/**
	 * The nap has to reach her dialog triggers, and they live on the one-shot
	 * controller, so the swap runs here rather than in {@link #stateAnimController}.
	 */
	@Override
	public PlayState oneShotAnimController(AnimationState<?> event) {
		syncDialogAnims(event.getController());
		return GeoYoukaiAnim.super.oneShotAnimController(event);
	}

	/**
	 * While she is lying down, every dialog slot plays {@link #NAP_TALK} instead of the
	 * standing clip {@link #getAnim} names - she talks in the pose she happens to be in,
	 * and standing up to do it would be worse than nothing.
	 * <p>
	 * {@code triggerableAnim} is a map put read only when a trigger fires, so
	 * re-pointing a trigger mid-life is safe: the one-shot already playing holds its own
	 * reference, and only the next trigger sees the swap. That is why this runs from the
	 * one-shot controller's handler every frame rather than from the nap task - the task
	 * is server side, and {@link YoukaiFlags#NAPPING} is the only thing the two sides
	 * agree on.
	 * <p>
	 * Only slots {@link #getAnim} actually maps are touched, so this can never register
	 * a clip her rig does not have, and {@link YoukaiAnim#USE_MAINHAND} keeps its own
	 * clip - opening a door while lying down is a thing she does lying down.
	 */
	private void syncDialogAnims(AnimationController<?> controller) {
		boolean napping = getFlag(YoukaiFlags.NAPPING);
		if (napping == dialogsNapping) return;
		dialogsNapping = napping;
		for (var anim : YoukaiAnim.values()) {
			var clip = getAnim(anim).orElse(null);
			if (clip == null) continue;
			controller.triggerableAnim(anim.trigger(), napping && anim.isDialog() ? NAP_TALK : clip);
		}
	}

	/**
	 * She naps about half the time she is left standing about with nothing to do.
	 */
	@Override
	public float napChance() {
		return 0.5f;
	}

	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		board.addPrioritizedActivity(GLBrains.NAP.get(), GLBrains.MEM_NAP.get(), 500);
		board.addExclusive(0, new YoukaiNapTask<>(), GLBrains.NAP.get());
	}

	/**
	 * The rig's standing clips, and nothing else. Every slot her rig has no
	 * {@code 交流_*} / greeting / thinking clip for resolves empty, so the dialog system
	 * plays nothing for it rather than asking GeckoLib for a clip that is not in this rig
	 * - which is most of them, and includes {@link YoukaiAnim#OUTDOOR_IDLE}: lying down
	 * is her {@link GLBrains#NAP} activity, driven by {@link #napChance}, rather than an
	 * idle flourish fired off by the wander task.
	 * <p>
	 * This is deliberately a function of the rig alone and not of her state.
	 * {@link #syncDialogAnims} is where lying down swaps the standing clip for
	 * {@link #NAP_TALK}, so the table the server reads to decide whether a slot exists
	 * never has to agree with what the client is currently playing.
	 */
	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case TALK_01 -> Optional.of(TALK_01);
			case TALK_02 -> Optional.of(TALK_02);
			case GREET, TALK_03, TALK_04, TALK_05, THINK, THINK_02,
				 CONFUSED, AGREE, DECLINE, OUTDOOR_IDLE -> Optional.empty();
		};
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide() && handleAnimEvent(id)) return;
		super.handleEntityEvent(id);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

}
