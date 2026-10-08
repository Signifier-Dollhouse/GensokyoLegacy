package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.l2core.base.entity.SyncedData;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

/**
 * A plain fairy: no named character behind her, just the shared fairy rig. {@link FairyEntity}
 * itself stays the vanilla-placeholder mob for the three named three-star fairies, so this
 * subclass is what carries the Blockbench rig and the four texture sheets.
 */
@SerialClass
public class PlainFairyEntity extends FairyEntity implements GeoYoukaiAnim {

	/**
	 * How many sheets the rig ships, i.e. the range of {@link #DATA_VARIANT}. The four
	 * {@code fairy_*.png} sheets are the same recolour of one mesh, chosen per entity.
	 */
	public static final int VARIANT_COUNT = 4;

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("待机");
	protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("坐下");
	protected static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("睡觉");
	/**
	 * Her hovering loop, which is what an airborne fairy wants instead of standing.
	 * <p>
	 * Also covers walking: this rig has no {@code 走路}/{@code 跑步} clip, like Cirno's.
	 * It cannot stay a missing name — GeckoLib resolves an unknown clip to {@code null},
	 * {@code buildAnimationQueue} then hands the controller an empty queue, and the next
	 * {@code process()} tick stops it without writing any bones — which would pin her in
	 * the last pose written, i.e. frozen mid-hover for the whole walk, silently.
	 */
	private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("漂浮");
	private static final RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");

	/** Her own entity data, layered over the generic youkai/spell-data layers. */
	protected static final SyncedData PLAIN_FAIRY_DATA = new SyncedData(PlainFairyEntity::defineId, SPELL_DATA);

	private static final EntityDataAccessor<Integer> DATA_VARIANT = PLAIN_FAIRY_DATA.define(SyncedData.INT, 0, "variant");

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(PlainFairyEntity.class, ser);
	}

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public PlainFairyEntity(EntityType<? extends FairyEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected SyncedData data() {
		return PLAIN_FAIRY_DATA;
	}

	// ---------- texture variant ----------

	/**
	 * Which of the four sheets to draw, {@code 0..VARIANT_COUNT-1}. Cosmetics only: nothing
	 * reads it but {@link PlainFairyModel#getTextureResource}, so the variant changes no
	 * stats and no behavior. Synced and NBT-backed through {@link #PLAIN_FAIRY_DATA}, like
	 * the rest of her entity data, so it survives a save/load.
	 */
	public int getVariant() {
		return entityData.get(DATA_VARIANT);
	}

	public void setVariant(int variant) {
		entityData.set(DATA_VARIANT, Math.floorMod(variant, VARIANT_COUNT));
	}

	/**
	 * Rolls the sheet on spawn. In {@code finalizeSpawn} rather than the constructor so
	 * the roll is not re-rolled on every chunk reload, and only on the server so the
	 * client takes the synced value instead of rolling its own.
	 */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
		if (!pLevel.isClientSide()) setVariant(getRandom().nextInt(VARIANT_COUNT));
		return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
	}

	// ---------- geckolib ----------

	protected <E extends PlainFairyEntity> PlayState idleAnimController(final AnimationState<E> event) {
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
			return event.setAndContinue(FLOAT);
		}
		if (event.isMoving()) {
			return event.setAndContinue(FLOAT);
		}
		return event.setAndContinue(IDLE);
	}

	/**
	 * Only {@code 使用主手物品} is mapped: this rig has no {@code 交流_*}, greeting, agree,
	 * decline or thinking clips, so those slots resolve empty and the dialog system simply
	 * plays nothing for them rather than asking GeckoLib for a clip that is not there.
	 */
	@Override
	public Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return switch (anim) {
			case USE_MAINHAND -> Optional.of(USE_MAINHAND);
			case GREET, TALK_01, TALK_02, TALK_03, THINK, AGREE, DECLINE, OUTDOOR_IDLE -> Optional.empty();
		};
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Blink first: controllers tick in registration order and overwrite shared
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