package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.FollowStrayLeaderGoal;
import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.HoverStrollGoal;
import dev.xkmc.gensokyolegacy.content.item.doll.DollShieldItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.MagicalForestBiomeGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
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
import java.util.Optional;
import java.util.UUID;

/**
 * The wild stray doll: a hostile mob that hunts players on sight. A natural spawn of at least three
 * becomes a pack: the first member is crowned when the third spawns, carries a shield, and the rest
 * follow it within its leash. Packs cut short stay leaderless and look for a nearby leader instead.
 *
 * @see "doc/design/doll/stray.md"
 */
public class StrayDollEntity extends StrayFlyingMob implements GeoEntity {

	public static final ResourceLocation TYPE = GensokyoLegacy.loc("stray_doll");

	private static final EntityDataAccessor<Optional<UUID>> DATA_LEADER =
			SynchedEntityData.defineId(StrayDollEntity.class, EntityDataSerializers.OPTIONAL_UUID);

	private static final int LEADER_LEASH = 24;
	private static final double LEADER_LEASH_SQ = LEADER_LEASH * LEADER_LEASH;
	private static final int LEADER_SEARCH_RANGE = 8;
	private static final double LEADER_SEARCH_RANGE_SQ = LEADER_SEARCH_RANGE * LEADER_SEARCH_RANGE;
	private static final int LEADER_SEARCH_COOLDOWN = 100;
	private static final float LANCE_CHANCE = 0.5F;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("DStoy_idle");
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("DStoy_fly");

	/**
	 * Server-side resolve cache; the leader itself and an orphaned follower stay null. Clients use
	 * the synced UUID instead, because {@link #getLeader()} needs a server level.
	 */
	@Nullable
	private StrayDollEntity cachedLeader;

	/** Ticks left before a leaderless doll searches again; only counts down after a failed search. */
	private int leaderSearchCooldown;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public StrayDollEntity(EntityType<? extends StrayDollEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.ATTACK_DAMAGE, 2)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FLYING_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 16);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_LEADER, Optional.empty());
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
										MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
		GroupData group = data instanceof GroupData g ? g : new GroupData();
		group.count++;
		if (group.count == 2) {
			becomeLeader();
		}
		if (level.getBiome(blockPosition()).is(MagicalForestBiomeGen.KEY) && getRandom().nextFloat() < LANCE_CHANCE)
			setItemSlot(EquipmentSlot.MAINHAND, GLItems.DOLL_LANCE.asStack());
		return group;
	}

	/**
	 * The synced leader UUID: a follower's leader, the doll's own UUID when it leads, or null while
	 * it has no leader. The client resolves it through the loaded entities because
	 * {@link #getLeader()} needs a server level.
	 */
	@Nullable
	public UUID getLeaderUUID() {
		return entityData.get(DATA_LEADER).orElse(null);
	}

	public boolean isLeader() {
		return getUUID().equals(getLeaderUUID());
	}

	/**
	 * Resolves the leader, or null while it is the leader itself, dead or unloaded. A missing leader
	 * lifts the leash and leaves the follower acting on its own, as the pack's leader was the only
	 * thing tying it down.
	 */
	@Nullable
	public StrayDollEntity getLeader() {
		UUID id = getLeaderUUID();
		if (id == null || id.equals(getUUID())) return null;
		if (cachedLeader != null && cachedLeader.isAlive() && id.equals(cachedLeader.getUUID()))
			return cachedLeader;
		cachedLeader = null;
		if (level() instanceof ServerLevel server &&
				server.getEntity(id) instanceof StrayDollEntity leader &&
				leader != this && leader.isAlive())
			cachedLeader = leader;
		return cachedLeader;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return true;
	}

	private void becomeLeader() {
		if (isLeader()) return;
		setLeader(getUUID());
		setItemSlot(EquipmentSlot.OFFHAND, GLItems.MINI_SHIELD.asStack());
		setItemSlot(EquipmentSlot.MAINHAND, GLItems.DOLL_LANCE.asStack());
	}

	private void setLeader(@Nullable UUID leader) {
		entityData.set(DATA_LEADER, Optional.ofNullable(leader));
		cachedLeader = null;
	}

	/**
	 * A doll spawned without a pack leader looks for one nearby, backing off for
	 * {@link #LEADER_SEARCH_COOLDOWN} ticks after a failed search.
	 */
	private void tickLeaderSearch() {
		if (isLeader() || getLeaderUUID() != null) {
			leaderSearchCooldown = 0;
			return;
		}
		if (leaderSearchCooldown > 0) {
			leaderSearchCooldown--;
			return;
		}
		StrayDollEntity best = null;
		double bestSqr = LEADER_SEARCH_RANGE_SQ;
		for (StrayDollEntity other : level().getEntitiesOfClass(StrayDollEntity.class,
				getBoundingBox().inflate(LEADER_SEARCH_RANGE), e -> e != this && e.isAlive() && e.isLeader())) {
			double dist = distanceToSqr(other);
			if (dist <= bestSqr) {
				bestSqr = dist;
				best = other;
			}
		}
		if (best == null) leaderSearchCooldown = LEADER_SEARCH_COOLDOWN;
		else setLeader(best.getUUID());
	}

	/**
	 * A follower may only engage a target that is still within {@link #LEADER_LEASH} blocks of its
	 * leader. The leader itself, and a follower with no leader, has no such restriction.
	 */
	public boolean targetAllowedByLeader(@Nullable LivingEntity target) {
		if (target == null) return true;
		StrayDollEntity leader = getLeader();
		return leader == null || leader.distanceToSqr(target) <= LEADER_LEASH_SQ;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(2, new FollowStrayLeaderGoal(this));
		goalSelector.addGoal(3, new HoverStrollGoal(this, 1.0));
		goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		targetSelector.addGoal(0, new HurtByTargetGoal(this, StrayDollEntity.class).setAlertOthers());
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class,
				10, true, false, this::targetAllowedByLeader));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		LivingEntity target = getTarget();
		if (target != null && !targetAllowedByLeader(target)) setTarget(null);
		tickLeaderSearch();
	}

	/**
	 * The leader wears its mini shield in the vanilla off-hand slot rather than a doll loadout slot,
	 * so the block check reads that slot directly; facing and pierce gating stay in vanilla.
	 */
	@Override
	public boolean isBlocking() {
		return DollShieldItem.isShield(getOffhandItem()) || super.isBlocking();
	}

	@Override
	protected void hurtCurrentlyUsedShield(float damage) {
		if (DollShieldItem.isShield(getOffhandItem()))
			playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + getRandom().nextFloat() * 0.4F);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		UUID leader = getLeaderUUID();
		if (leader != null) compound.putUUID("Leader", leader);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		UUID leader = compound.hasUUID("Leader") ? compound.getUUID("Leader") : null;
		entityData.set(DATA_LEADER, Optional.ofNullable(leader));
		cachedLeader = null;
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

	private static final class GroupData implements SpawnGroupData {

		private int count;
		@Nullable
		private UUID first;

	}

}
