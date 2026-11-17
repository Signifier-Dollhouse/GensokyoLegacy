package dev.xkmc.gensokyolegacy.content.entity.characters.byakuren;

import dev.xkmc.gensokyolegacy.content.entity.behavior.module.AbstractYoukaiModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.HomeModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.TalkModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

/**
 * Hijiri Byakuren, head priest of Myouren Temple. Her rig ships four talk clips, so the fifth borrows
 * the third, and it has no love or hate clip, so the gift reactions borrow her yes and no.
 */
@SerialClass
public class ByakurenEntity extends BossYoukaiEntity implements GeoYoukaiAnim {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("sleep");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("swing");
	private static final RawAnimation GREET = RawAnimation.begin().thenPlay("hello");
	private static final RawAnimation TALK_01 = RawAnimation.begin().thenPlay("talk_01");
	private static final RawAnimation TALK_02 = RawAnimation.begin().thenPlay("talk_02");
	private static final RawAnimation TALK_03 = RawAnimation.begin().thenPlay("talk_03");
	private static final RawAnimation TALK_04 = RawAnimation.begin().thenPlay("talk_04");
	private static final RawAnimation THINK = RawAnimation.begin().thenPlay("think_01");
	private static final RawAnimation THINK_02 = RawAnimation.begin().thenPlay("think_02");
	private static final RawAnimation CONFUSED = RawAnimation.begin().thenPlay("puzzle");
	private static final RawAnimation AGREE = RawAnimation.begin().thenPlay("agree");
	private static final RawAnimation DECLINE = RawAnimation.begin().thenPlay("refuse");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public ByakurenEntity(EntityType<? extends BossYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public PlayState stateAnimController(AnimationState<?> event) {
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
			case TALK_05 -> Optional.of(TALK_03);
			case THINK -> Optional.of(THINK);
			case THINK_02 -> Optional.of(THINK_02);
			case CONFUSED -> Optional.of(CONFUSED);
			case AGREE -> Optional.of(AGREE);
			case DECLINE -> Optional.of(DECLINE);
			case LIKE -> Optional.of(AGREE);
			case DISLIKE -> Optional.of(DECLINE);
			case EAT -> Optional.of(EAT);
			case HURT -> Optional.of(HURT);
			default -> Optional.empty();
		};
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide() && handleAnimEvent(id)) return;
		super.handleEntityEvent(id);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(new HomeModule(this), new VisitModule(this), new TalkModule(this));
	}

}
