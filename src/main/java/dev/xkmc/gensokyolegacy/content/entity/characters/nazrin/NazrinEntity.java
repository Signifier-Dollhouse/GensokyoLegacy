package dev.xkmc.gensokyolegacy.content.entity.characters.nazrin;

import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.gensokyolegacy.content.item.tool.Dowser;
import dev.xkmc.gensokyolegacy.content.item.tool.DowserHalf;
import dev.xkmc.gensokyolegacy.init.registrate.GLSounds;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

/**
 * Nazrin, the dowser of Myouren Temple. Her rig ships the sleep, thinking and puzzle clips; the gift
 * reactions borrow her yes and no.
 */
@SerialClass
public class NazrinEntity extends BossYoukaiEntity implements GeoYoukaiAnim {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("sleep");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("swing");
	private static final RawAnimation GREET = RawAnimation.begin().thenPlay("hello");
	private static final RawAnimation TALK_01 = RawAnimation.begin().thenPlay("talk_01");
	private static final RawAnimation TALK_02 = RawAnimation.begin().thenPlay("talk_02");
	private static final RawAnimation TALK_03 = RawAnimation.begin().thenPlay("talk_03");
	private static final RawAnimation THINK = RawAnimation.begin().thenPlay("think_01");
	private static final RawAnimation THINK_02 = RawAnimation.begin().thenPlay("think_02");
	private static final RawAnimation PUZZLE = RawAnimation.begin().thenPlay("puzzle");
	private static final RawAnimation AGREE = RawAnimation.begin().thenPlay("agree");
	private static final RawAnimation REFUSE = RawAnimation.begin().thenPlay("refuse");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public NazrinEntity(EntityType<? extends BossYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	public SoundEvent getDialogChatSound() {
		return GLSounds.DIALOG_CHAT_YOUNG.get();
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
			case TALK_01, TALK_04 -> Optional.of(TALK_01);
			case TALK_02, TALK_05 -> Optional.of(TALK_02);
			case TALK_03 -> Optional.of(TALK_03);
			case THINK -> Optional.of(THINK);
			case THINK_02 -> Optional.of(THINK_02);
			case CONFUSED -> Optional.of(PUZZLE);
			case AGREE, LIKE -> Optional.of(AGREE);
			case DECLINE, DISLIKE -> Optional.of(REFUSE);
			case EAT -> Optional.of(EAT);
			case HURT -> Optional.of(HURT);
			default -> Optional.empty();
		};
	}

	@Override
	public ItemStack getDefaultHeldItem(EquipmentSlot slot) {
		return switch (slot) {
			case MAINHAND -> Dowser.ofHalf(DowserHalf.LEFT);
			case OFFHAND -> Dowser.ofHalf(DowserHalf.RIGHT);
			default -> ItemStack.EMPTY;
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

}
