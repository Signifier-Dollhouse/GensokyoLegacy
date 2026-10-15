package dev.xkmc.gensokyolegacy.content.entity.characters.sakuya;

import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.MaidenEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

/**
 * Izayoi Sakuya, chief maid of the Scarlet Devil Mansion. She owns no home: the only place
 * she is found is someone else's, as a visit-system guest — see {@code GLStructureGen} for
 * the house and {@code SakuyaQDGen} for what she says and sells there.
 */
@SerialClass
public class SakuyaEntity extends MaidenEntity implements GeoYoukaiAnim {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("待机");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("走路");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("坐下");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");
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

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public SakuyaEntity(EntityType<? extends MaidenEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public PlayState stateAnimController(AnimationState<?> event) {
		// No 睡觉 clip in this rig, and naming a missing one pins her in the last pose
		// written: GeckoLib resolves it to null and stops the controller. 坐下 is closest.
		if (isSleeping()) {
			return event.setAndContinue(SIT);
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
		return this.geoCache;
	}

}