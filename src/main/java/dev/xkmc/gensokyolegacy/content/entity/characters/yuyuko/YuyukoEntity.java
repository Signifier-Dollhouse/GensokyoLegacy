package dev.xkmc.gensokyolegacy.content.entity.characters.yuyuko;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DelegateDollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.AbstractYoukaiModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Saigyouji Yuyuko, mistress of Hakugyokurou. She never touches the ground: her navigation stays
 * airborne and the walk clip is only ever played as a glide. Three attendant ghosts orbit her at
 * full power through {@link YuyukoGhostHost}.
 *
 * @see "doc/design/party/select.md"
 */
@SerialClass
public class YuyukoEntity extends BossYoukaiEntity implements GeoYoukaiAnim, DelegateDollHost {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	/**
	 * The rig ships one locomotion loop and it is named {@code walk}; she plays it while hovering,
	 * since she has no ground state.
	 */
	protected static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("walk");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("sleep");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("swing");
	private static final RawAnimation GREET = RawAnimation.begin().thenPlay("hello");
	private static final RawAnimation TALK_01 = RawAnimation.begin().thenPlay("talk_01");
	private static final RawAnimation TALK_02 = RawAnimation.begin().thenPlay("talk_02");
	private static final RawAnimation THINK = RawAnimation.begin().thenPlay("think_01");
	private static final RawAnimation CONFUSED = RawAnimation.begin().thenPlay("puzzle");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public YuyukoEntity(EntityType<? extends YuyukoEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		List<AbstractYoukaiModule> list = new ArrayList<>(super.createModules());
		list.add(new YuyukoGhostHost(this));
		return list;
	}

	@Override
	public DollHost dolls() {
		return getModule(YuyukoGhostHost.class)
				.orElseThrow(() -> new IllegalStateException("Yuyuko is missing her ghost module"));
	}

	// ---------- hovering ----------

	/**
	 * She is airborne whenever she is awake, and never settles onto a floor: a walk node needs
	 * solid ground and hers is a block of air.
	 */
	@Override
	public boolean mayFly() {
		return !isSleeping();
	}

	@Override
	public boolean mayLand() {
		return false;
	}

	@Override
	public void travel(Vec3 input) {
		boolean hover = !isSleeping() && !isPassenger();
		if (hover) {
			if (!isNoGravity()) setNoGravity(true);
			if (!navCtrl.isFlying()) navCtrl.setFlying();
		}
		super.travel(input);
	}

	// ---------- geckolib ----------

	public PlayState stateAnimController(AnimationState<?> event) {
		if (isSleeping()) {
			return event.setAndContinue(SLEEP);
		}
		if (isPassenger()) {
			return event.setAndContinue(SIT);
		}
		if (event.isMoving()) {
			return event.setAndContinue(FLOAT);
		}
		return event.setAndContinue(IDLE);
	}

	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case GREET -> Optional.of(GREET);
			case TALK_01, TALK_03, TALK_05 -> Optional.of(TALK_01);
			case TALK_02, TALK_04 -> Optional.of(TALK_02);
			case THINK, THINK_02 -> Optional.of(THINK);
			case CONFUSED -> Optional.of(CONFUSED);
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

}
