package dev.xkmc.gensokyolegacy.content.entity.characters.magician;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.YoukaiFindPreySensor;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.combat.YoukaiSearchTargetTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.combat.YoukaiUpdateTargetTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.play.YoukaiHuntTask;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Alice Margatroid, the doll master. She is a {@link DollHost} in her own right:
 * her dolls are hosted by {@link AliceDollHost}, which is where the roster, the
 * arming, and the orders live. This class exists only to <b>be</b> the host —
 * {@link BaseDollEntity#getHost} resolves a doll's host by asking its owning
 * entity, so without this every one of her dolls would find no host and discard
 * itself on the next tick.
 */
@SerialClass
public class AliceEntity extends GeneralYoukaiEntity implements GeoYoukaiAnim, DollHost {

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
	private static final RawAnimation THINK = RawAnimation.begin().thenPlay("思考中");
	// the rig also ships 深思熟虑, a longer thinking-over-it variant of the same slot.
	// Unmapped: there is only one THINK slot, and swapping would silently lengthen
	// every existing dialog's thinking beat.
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

	// ---------- doll host ----------
	// Pure delegation to the module, which is where the state actually lives. A
	// doll resolves its host by asking its owner, so Alice has to answer even
	// though she keeps nothing here.

	@Override
	@Nullable
	public DollData findSummoned(UUID uuid) {
		return dolls().findSummoned(uuid);
	}

	@Override
	public void update(BaseDollEntity doll) {
		dolls().update(doll);
	}

	@Override
	@Nullable
	public DollData detach(UUID uuid) {
		return dolls().detach(uuid);
	}

	@Override
	public float getFormationYaw() {
		return dolls().getFormationYaw();
	}

	@Override
	public List<DollEntity> summonedAllies(DollEntity doll) {
		return dolls().summonedAllies(doll);
	}

	@Override
	public boolean isCommandedTarget(LivingEntity target) {
		return dolls().isCommandedTarget(target);
	}

	@Override
	public Optional<DollActionType> doneType(UUID uuid) {
		return dolls().doneType(uuid);
	}

	@Override
	public void handAhead(DollEntity doll, DollAction action) {
		dolls().handAhead(doll, action);
	}

	@Override
	public boolean handOff(DollEntity doll, DollAction action) {
		return dolls().handOff(doll, action);
	}

	/** Her retinue: the ledger she conjures from and hands her orders. */
	private DollHost dolls() {
		return AliceDollHost.hostOf(this);
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
