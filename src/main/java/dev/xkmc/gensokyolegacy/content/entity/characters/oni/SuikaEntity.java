package dev.xkmc.gensokyolegacy.content.entity.characters.oni;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.core.YoukaiNapTask;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.l2core.base.entity.SyncedData;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
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
 * Which nap she lies down in is {@link #getNapVariant()}, a synced integer rather
 * than a second flag, because both the clip she holds while down and the one she
 * talks from it are looked up by it - see {@link #NAP_ANIMS}. The rig ships one nap
 * today, so the number is always 0; it is a number rather than a boolean because
 * the second {@code 侧卧_*} clip a rig grows should not have to touch any of this.
 * <p>
 * {@link #NAP_TALK} and the {@code 聚集} her spell card pulls with are the reason
 * the enum has {@link YoukaiAnim#TALK_SPECIAL_01} and {@link YoukaiAnim#SKILL_01}: a
 * slot every character shares cannot be named after what she happens to do with it.
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

	private static final RawAnimation GATHER = RawAnimation.begin().thenPlay("聚集");
	private static final RawAnimation NAP_ANIM = RawAnimation.begin().thenLoop("侧卧");
	private static final RawAnimation NAP_TALK = RawAnimation.begin().thenPlay("侧卧_交流");

	protected static final SyncedData SUIKA_DATA = new SyncedData(SuikaEntity::defineId, SPELL_DATA);

	private static final EntityDataAccessor<Integer> DATA_NAP_VARIANT = SUIKA_DATA.define(SyncedData.INT, 0, "nap_variant");

	private static final RawAnimation[] NAP_ANIMS = {NAP_ANIM, NAP_ANIM};

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(SuikaEntity.class, ser);
	}

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);


	public SuikaEntity(EntityType<? extends SuikaEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	protected SyncedData data() {
		return SUIKA_DATA;
	}

	// ---------- nap variant ----------

	public int getNapVariant() {
		return Math.clamp(entityData.get(DATA_NAP_VARIANT), 0, NAP_ANIMS.length - 1);
	}

	public void setNapVariant(int variant) {
		entityData.set(DATA_NAP_VARIANT, variant);
	}

	public PlayState stateAnimController(AnimationState<?> event) {
		if (isSleeping()) {
			return event.setAndContinue(SLEEP);
		}
		if (isPassenger()) {
			return event.setAndContinue(SIT);
		}
		if (getFlag(YoukaiFlags.NAPPING)) {
			return event.setAndContinue(NAP_ANIMS[getNapVariant()]);
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
	public boolean hasBlinkAnim() {
		return false;
	}

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

	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case TALK_01, TALK_03, TALK_05 -> Optional.of(TALK_01);
			case GREET, TALK_02, TALK_04 -> Optional.of(TALK_02);
			case TALK_SPECIAL_01 -> Optional.of(NAP_TALK);
			case TALK_SPECIAL_02 -> Optional.of(NAP_TALK);
			case SKILL_01 -> Optional.of(GATHER);
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

	/**
	 * Answer from the bed rather than sitting up. Re-pointing every dialog slot is enough.
	 * Client-side, so the server's "does this slot exist" table stays a function of
	 * the rig alone and never has to agree with what is playing.
	 */
	@Override
	public void triggerAnim(YoukaiAnim anim) {
		if (anim.isDialog() && getFlag(YoukaiFlags.NAPPING)) {
			int variant = getNapVariant();
			GeoYoukaiAnim.super.triggerAnim(variant == 0 ?
					YoukaiAnim.TALK_SPECIAL_01 : YoukaiAnim.TALK_SPECIAL_02);
		} else GeoYoukaiAnim.super.triggerAnim(anim);
	}

}
