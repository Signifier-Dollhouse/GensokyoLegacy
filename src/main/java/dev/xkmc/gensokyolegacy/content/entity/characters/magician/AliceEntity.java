package dev.xkmc.gensokyolegacy.content.entity.characters.magician;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.YoukaiFindPreySensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.combat.YoukaiSearchTargetTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.play.YoukaiHuntTask;
import dev.xkmc.gensokyolegacy.content.entity.module.AbstractYoukaiModule;
import dev.xkmc.gensokyolegacy.content.entity.module.HomeModule;
import dev.xkmc.gensokyolegacy.content.entity.module.TalkModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

@SerialClass
public class AliceEntity extends GeneralYoukaiEntity implements GeoYoukaiAnim {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("待机");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("走路");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("坐下");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("睡觉");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");
	// this rig ships a single, colder greeting clip instead of the neutral 招呼
	private static final RawAnimation GREET = RawAnimation.begin().thenPlay("招呼(适用于冷淡性格)");
	private static final RawAnimation TALK_01 = RawAnimation.begin().thenPlay("交流_01");
	private static final RawAnimation TALK_02 = RawAnimation.begin().thenPlay("交流_02");
	private static final RawAnimation TALK_03 = RawAnimation.begin().thenPlay("交流_03");
	private static final RawAnimation THINK = RawAnimation.begin().thenPlay("思考中");
	private static final RawAnimation AGREE = RawAnimation.begin().thenPlay("肯定");
	private static final RawAnimation DECLINE = RawAnimation.begin().thenPlay("拒绝");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public AliceEntity(EntityType<? extends GeneralYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(
				new HomeModule(this),
				new TalkModule(this),
				new AliceDollHost(this)
		);
	}

	@Override
	protected void constructTaskBoard(TaskBoard board) {
		super.constructTaskBoard(board);
		board.addExclusive(250, new YoukaiHuntTask(6), GLBrains.HUNT.get());
		board.addBehaviorActivity(YoukaiSearchTargetTask.class, GLBrains.HUNT.get());
		// the prey sensor has to keep running while the hunt is already up, or the
		// hunt drops the moment it starts
		board.addSensor(new YoukaiFindPreySensor<>(e -> e.getActivity() == Activity.PLAY
				|| e.getActivity() == GLBrains.HUNT.get()));
		board.addPrioritizedActivity(GLBrains.HUNT.get(), GLBrains.MEM_PREY.get(), 200);
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
			case THINK -> Optional.of(THINK);
			case AGREE -> Optional.of(AGREE);
			case DECLINE -> Optional.of(DECLINE);
			case OUTDOOR_IDLE -> Optional.empty();
		};
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Wink first: controllers tick in registration order and overwrite shared
		// bones, so the always-looping blink yields to the main controller.
		controllers.add(new AnimationController<>(this, WINK_CONTROLLER, 0, e -> e.setAndContinue(BLINK)));
		var main = new AnimationController<>(this, ANIM_CONTROLLER, 5, this::idleAnimController);
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
