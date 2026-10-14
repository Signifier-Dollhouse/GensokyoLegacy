package dev.xkmc.gensokyolegacy.content.entity.characters.oni;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.core.YoukaiAcquireTargetTask;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.core.YoukaiNapTask;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

/**
 * Ibuki Suika, the oni who lives on her own at the edge of the Bamboo Forest.
 * <p>
 * She has no home in this mod — there is no bamboo forest to put one in — so like
 * {@code SakuyaEntity} she only turns up as a guest ({@code GLStructureGen}), which is why she
 * has no spawn egg. Her content is deliberately unwritten: {@code SuikaQDGen} gives her the
 * greeting lines and nothing else, so she is met with an empty topic list.
 * <p>
 * She is the only character who naps: {@link #napChance} buys her the {@link GLBrains#NAP}
 * activity, where her {@code 侧卧} clip is spent. An activity rather than a fidget because a
 * nap has to be able to end, and a one-shot animation cannot be interrupted. A conversation
 * does not get her up — she answers from where she lies, in {@link #TALK_SPECIAL_01}.
 * <p>
 * That clip and the {@code 聚集} her spell card pulls with are why the enum has
 * {@link YoukaiAnim#TALK_SPECIAL_01} and {@link YoukaiAnim#SKILL_01}: a shared slot cannot be
 * named after what one character happens to do with it.
 * <p>
 * Her rig is the narrowest in the mod: no {@code 眨眼} blink, no greeting or thinking clips,
 * only two {@code 交流_*} ones, so {@link #getAnim} answers empty for most dialog slots and
 * {@link #hasBlinkAnim} says no. A clip the rig does not have resolves to null, and the
 * controller then logs "Unable to find animation" every frame.
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
	// the generic slots: 侧卧_交流 is her talk while she is lying down, 聚集 is the
	// sweep her spell card pulls with. Neither name says what the clip is (YoukaiAnim).
	private static final RawAnimation TALK_SPECIAL_01 = RawAnimation.begin().thenPlay("侧卧_交流");
	private static final RawAnimation SKILL_01 = RawAnimation.begin().thenPlay("聚集");
	private static final RawAnimation NAP = RawAnimation.begin().thenLoop("侧卧");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);


	public SuikaEntity(EntityType<? extends SuikaEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public PlayState stateAnimController(AnimationState<?> event) {
		if (isSleeping()) {
			return event.setAndContinue(SLEEP);
		}
		if (isPassenger()) {
			return event.setAndContinue(SIT);
		}
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
			case TALK_SPECIAL_01 -> Optional.of(TALK_SPECIAL_01);
			case SKILL_01 -> Optional.of(SKILL_01);
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
	 * Answer from the bed rather than sitting up. Re-pointing every dialog slot at
	 * {@link #TALK_SPECIAL_01} is enough, since that is the only talk she does: a conversation
	 * has to outlive the nap it interrupted without her standing up in the middle of it.
	 * <p>
	 * Client-side, so the server's "does this slot exist" table stays a function of the rig alone.
	 */
	@Override
	public void triggerAnim(YoukaiAnim anim) {
		if (anim.isDialog() && getFlag(YoukaiFlags.NAPPING))
			GeoYoukaiAnim.super.triggerAnim(YoukaiAnim.TALK_SPECIAL_01);
		else GeoYoukaiAnim.super.triggerAnim(anim);
	}

}
