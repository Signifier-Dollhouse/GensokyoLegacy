package dev.xkmc.gensokyolegacy.content.entity.characters.magician;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DelegateDollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.combat.YoukaiUpdateTargetTask;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.module.AbstractYoukaiModule;
import dev.xkmc.gensokyolegacy.content.entity.module.HomeModule;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.module.TalkModule;
import dev.xkmc.gensokyolegacy.content.entity.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

/**
 * Alice Margatroid, the doll master. She is a {@link DollHost} in her own right:
 * the roster, the arming and the orders live in {@link AliceDollHost}, and this class
 * exists only to <b>be</b> the host, since {@link BaseDollEntity#getHost} resolves a
 * doll's host by asking its owning entity. {@link DelegateDollHost} forwards the whole
 * surface, so only {@link #dolls()} is written here (host.md §1).
 */
@SerialClass
public class AliceEntity extends GeneralYoukaiEntity implements GeoYoukaiAnim, DelegateDollHost {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("待机");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("走路");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("坐下");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("睡觉");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");
	// the rig names its greeting plainly, like the other characters' (the clip itself
	// is still the colder variant this rig has always shipped)
	private static final RawAnimation GREET = RawAnimation.begin().thenPlay("招呼");
	private static final RawAnimation TALK_01 = RawAnimation.begin().thenPlay("交流_01");
	private static final RawAnimation TALK_02 = RawAnimation.begin().thenPlay("交流_02");
	private static final RawAnimation TALK_03 = RawAnimation.begin().thenPlay("交流_03");
	private static final RawAnimation TALK_04 = RawAnimation.begin().thenPlay("交流_04");
	private static final RawAnimation TALK_05 = RawAnimation.begin().thenPlay("交流_05");
	private static final RawAnimation THINK = RawAnimation.begin().thenPlay("思考中_01");
	private static final RawAnimation THINK_02 = RawAnimation.begin().thenPlay("思考中_02");
	private static final RawAnimation CONFUSED = RawAnimation.begin().thenPlay("疑惑");
	private static final RawAnimation AGREE = RawAnimation.begin().thenPlay("肯定");
	private static final RawAnimation DECLINE = RawAnimation.begin().thenPlay("拒绝");
	/**
	 * The idle flourish the current export added as {@code 闲置动画}. It takes the outdoor
	 * idle slot the other characters fill with a stretch or a gaze; nothing about it is
	 * Alice-specific, so there is no reason to leave the slot empty.
	 */
	private static final RawAnimation IDLE_FIDGET = RawAnimation.begin().thenPlay("闲置动画");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public AliceEntity(EntityType<? extends GeneralYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(
				new HomeModule(this),
				// she is a declared guest at Marisa's house (GLStructureGen), so she
				// needs the visit module like everyone else
				new VisitModule(this),
				new PartyModule(this),
				new TalkModule(this),
				new AliceDollHost(this)
		);
	}

	// ---------- doll host ----------
	// The state lives in the module; every host method is forwarded there by
	// DelegateDollHost. A doll resolves its host by asking its owner, so Alice
	// answers even though she keeps nothing here.

	/**
	 * Her retinue: the ledger she conjures from and hands her orders.
	 */
	@Override
	public DollHost dolls() {
		return AliceDollHost.hostOf(this);
	}

	/**
	 * She fights at range, so the stock melee-then-strafe pair is wrong for her:
	 * both close to contact, which is where her dolls' danmaku and her own
	 * incoming damage are least welcome. {@link AliceRangeTask} holds a 16-24
	 * band instead, on the full 3D vector.
	 */
	@Override
	protected void addFightTasks(TaskBoard board) {
		board.addAlways(new YoukaiUpdateTargetTask<>(), Activity.FIGHT);
		board.addAlways(new AliceRangeTask(), Activity.FIGHT);
	}

	protected <E extends AliceEntity> PlayState idleAnimController(final AnimationState<E> event) {
		if (event.getController().isPlayingTriggeredAnimation()) {
			return PlayState.CONTINUE;
		}
		if (isSleeping()) {
			return event.setAndContinue(SLEEP);
		}
		if (isPassenger()) {
			return event.setAndContinue(SIT);
		}
		if (getFlag(YoukaiFlags.FLYING)) {
			return event.setAndContinue(IDLE);
		}
		if (event.isMoving()) {
			return event.setAndContinue(WALK);
		}
		return event.setAndContinue(IDLE);
	}

	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case GREET -> Optional.of(GREET);
			case TALK_01 -> Optional.of(TALK_01);
			case TALK_02 -> Optional.of(TALK_02);
			case TALK_03 -> Optional.of(TALK_03);
			case TALK_04 -> Optional.of(TALK_04);
			case TALK_05 -> Optional.of(TALK_05);
			case THINK -> Optional.of(THINK);
			case THINK_02 -> Optional.of(THINK_02);
			case CONFUSED -> Optional.of(CONFUSED);
			case AGREE -> Optional.of(AGREE);
			case DECLINE -> Optional.of(DECLINE);
			case OUTDOOR_IDLE -> Optional.of(IDLE_FIDGET);
		};
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Wink first: controllers tick in registration order and overwrite shared
		// bones, so the always-looping blink yields to the main controller.
		controllers.add(new AnimationController<>(this, WINK_CONTROLLER, 0, e -> e.setAndContinue(BLINK)));
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
