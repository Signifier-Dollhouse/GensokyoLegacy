package dev.xkmc.gensokyolegacy.content.entity.characters.oni;

import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
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
	 * {@code 侧卧}, the side-lying loop, used as the outdoor idle one-shot. Played rather
	 * than looped, so she lies down, lies there for the length of one cycle and gets back
	 * up — which is about the whole of what an outdoor idle is allowed to be.
	 */
	private static final RawAnimation NAP = RawAnimation.begin().thenPlay("侧卧");

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
		if (getFlag(YoukaiFlags.FLYING)) {
			return event.setAndContinue(IDLE);
		}
		if (event.isMoving()) {
			return event.setAndContinue(WALK);
		}
		return event.setAndContinue(IDLE);
	}

	/**
	 * Only {@code 使用主手物品} and the outdoor nap are mapped. Every dialog slot resolves
	 * empty, so the dialog system plays nothing for her rather than asking GeckoLib for a
	 * clip that is not in this rig.
	 */
	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case OUTDOOR_IDLE -> Optional.of(NAP);
			case GREET, TALK_01, TALK_02, TALK_03, TALK_04, TALK_05, THINK, THINK_02,
					CONFUSED, AGREE, DECLINE -> Optional.empty();
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
