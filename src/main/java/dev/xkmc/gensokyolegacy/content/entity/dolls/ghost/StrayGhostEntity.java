package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayFlyingMob;
import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.HoverStrollGoal;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * The wild, ownerless ghost. It is deliberately passive — no target goals at all — and lives
 * outside the doll ledger until the bone flute binds it.
 *
 * @see "doc/design/doll/stray.md"
 */
public class StrayGhostEntity extends StrayFlyingMob implements GeoEntity, GhostVariantHolder, GhostPowerHolder {

	public static final ResourceLocation TYPE = GensokyoLegacy.loc("stray_ghost");
	public static final EntityDataAccessor<Integer> DATA_VARIANT =
			SynchedEntityData.defineId(StrayGhostEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_POWER =
			SynchedEntityData.defineId(StrayGhostEntity.class, EntityDataSerializers.INT);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public StrayGhostEntity(EntityType<? extends StrayGhostEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return StrayFlyingMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FLYING_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 16);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new HoverStrollGoal(this, 1.0));
		goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, GhostVariant.CURIOS.ordinal());
		builder.define(DATA_POWER, GhostPower.WILD_MIN);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
										MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
		if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND || reason == MobSpawnType.NATURAL) {
			setVariant(GhostVariant.random(getRandom()));
			setPower(GhostPowerHolder.wildPower(getRandom()));
		}
		return data;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Variant", getVariant().ordinal());
		tag.putInt("Power", getPower());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Variant")) setVariant(GhostVariant.byId(tag.getInt("Variant")));
		if (tag.contains("Power")) setPower(tag.getInt("Power"));
	}

	@Override
	public GhostVariant getVariant() {
		return GhostVariant.byId(getEntityData().get(DATA_VARIANT));
	}

	@Override
	public void setVariant(GhostVariant variant) {
		getEntityData().set(DATA_VARIANT, variant.ordinal());
	}

	@Override
	public int getPower() {
		return getEntityData().get(DATA_POWER);
	}

	@Override
	public void setPower(int power) {
		getEntityData().set(DATA_POWER, Mth.clamp(power, 0, GhostPower.MAX));
	}

	@Override
	public Component getName() {
		if (getCustomName() != null) return super.getName();
		return getVariant().getDisplayName();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "main", 3,
				e -> e.setAndContinue(e.isMoving() ? FLY : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

}
