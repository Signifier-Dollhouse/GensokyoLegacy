package dev.xkmc.gensokyolegacy.content.entity.characters.oni;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
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
 * an activity rather than a fidget because a nap has to be able to end - she gets up
 * for a conversation, a fight or a party - and a one-shot animation has no way to be
 * interrupted.
 * <p>
 * Her rig is the narrowest one in the mod. It ships no {@code 眨眼} blink and no
 * {@code 交流_*} / greeting / thinking clips at all, which is why {@link #getAnim}
 * answers empty for every dialog slot and {@link #registerControllers} registers no
 * wink controller: naming a clip the rig does not have resolves it to null, the
 * controller is then handed an empty queue, and the next tick stops it without writing
 * any bones — which would pin her in the last pose written for the rest of the time.
 */
@SerialClass
public class SuikaEntity extends BossYoukaiEntity implements GeoYoukaiAnim {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("待机");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("走路");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("坐下");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("睡觉");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");
	/**
	 * {@code 侧卧}, side-lying, looped rather than played: the clip is authored lying
	 * down throughout and only wobbles a little, so it is a pose to hold and not a
	 * flourish to sit through. Held by the main controller for as long as
	 * {@link GLBrains#NAP} lasts - see {@link #idleAnimController} - rather than as a
	 * {@link YoukaiAnim} one-shot, because the nap is an activity with a length and a
	 * one-shot would end after a single 1.75s cycle.
	 */
	private static final RawAnimation NAP = RawAnimation.begin().thenLoop("侧卧");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public SuikaEntity(EntityType<? extends SuikaEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	protected <E extends SuikaEntity> PlayState idleAnimController(final AnimationState<E> event) {
		if (event.getController().isPlayingTriggeredAnimation()) {
			return PlayState.CONTINUE;
		}
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
	 * She naps about half the time she is left standing about with nothing to do.
	 */
	@Override
	public float napChance() {
		return 0.5f;
	}

	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		// 500, between PARTY(400) and VISITING(600): a conversation or a party is
		// something to get up for, and being above everything scheduled keeps a nap
		// from overriding what she would otherwise be doing. See the priority
		// scale in GLBrains.
		board.addPrioritizedActivity(GLBrains.NAP.get(), GLBrains.MEM_NAP.get(), 500);
		board.addExclusive(0, new YoukaiNapTask<>(), GLBrains.NAP.get());
	}

	/**
	 * Only {@code 使用主手物品} is mapped. Every dialog slot resolves empty, so the dialog
	 * system plays nothing for her rather than asking GeckoLib for a clip that is not in
	 * this rig, and so does {@link YoukaiAnim#OUTDOOR_IDLE}: lying down is her
	 * {@link GLBrains#NAP} activity, driven by {@link #napChance}, rather than an
	 * idle flourish fired off by the wander task.
	 */
	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case GREET, TALK_01, TALK_02, TALK_03, TALK_04, TALK_05, THINK, THINK_02,
					CONFUSED, AGREE, DECLINE, OUTDOOR_IDLE -> Optional.empty();
		};
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// No wink controller: this rig has no 眨眼 clip, and a looping controller pointed
		// at a missing one writes no bones at all. See the class comment.
		var main = new AnimationController<>(this, ANIM_CONTROLLER, 5, this::idleAnimController);
		addEatAnim(main);
		addHurtAnim(main);
		addDialogAnims(main);
		controllers.add(main);
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
