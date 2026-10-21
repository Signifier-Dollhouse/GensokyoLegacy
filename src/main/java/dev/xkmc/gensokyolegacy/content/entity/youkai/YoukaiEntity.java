package dev.xkmc.gensokyolegacy.content.entity.youkai;

import dev.xkmc.danmakuapi.api.IYoukaiEntity;
import dev.xkmc.danmakuapi.content.spell.spellcard.SpellCardWrapper;
import dev.xkmc.fastprojectileapi.spellcircle.SpellCircleHolder;
import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationState;
import dev.xkmc.gensokyolegacy.content.block.deco.bed.YoukaiBedBlock;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.YoukaiNavigationControl;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageClampEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.IDimensionBoundEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.ISleepOffsetEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.*;
import dev.xkmc.l2core.base.entity.SyncedData;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The foundation every character shares: synced data, the NBT contract, the module registry, and
 * the vanilla overrides that dispatch into the {@code impl/} facets. Everything a character does
 * - chewing, fighting, keeping score - lives in a facet beside this file.
 *
 * @see "doc/design/youkai/entity.md"
 */
@SerialClass
public abstract class YoukaiEntity extends DamageClampEntity implements SpellCircleHolder, IYoukaiEntity,
		ISleepOffsetEntity, IDimensionBoundEntity, YoukaiBaseImpl, YoukaiEating, YoukaiCombat, YoukaiHurt {

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(YoukaiEntity.class, ser);
	}

	protected static final SyncedData YOUKAI_DATA = new SyncedData(YoukaiEntity::defineId);

	public static final EntityDataAccessor<Integer> DATA_FLAGS_ID = YOUKAI_DATA.define(SyncedData.INT, 0, "youkai_flags");

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.4)
				.add(Attributes.FOLLOW_RANGE, 48);
	}

	@SerialField
	public final YoukaiTargetContainer targets;

	@SerialField
	public SpellCardWrapper spellCard;

	@SerialField
	protected int noTargetTime, noPlayerTime;

	protected final YoukaiSourceOverride sources = new YoukaiSourceOverride(level().registryAccess());
	protected final YoukaiModuleHolder modules = new YoukaiModuleHolder(createModules());

	public final YoukaiCardHolder cardHolder = new YoukaiCardHolder(this);
	public final YoukaiCombatManager combatManager = createCombatManager();
	public final YoukaiNavigationControl navCtrl = new YoukaiNavigationControl(this);

	public YoukaiEntity(EntityType<? extends YoukaiEntity> pEntityType, Level pLevel) {
		this(pEntityType, pLevel, 10);
	}

	public YoukaiEntity(EntityType<? extends YoukaiEntity> pEntityType, Level pLevel, int maxSize) {
		super(pEntityType, pLevel);
		this.targets = new YoukaiTargetContainer(this, maxSize);
		if (isNamed()) {
			setPersistenceRequired();
		}
	}

	protected SoundEvent getAmbientSound() {
		return SoundEvents.EMPTY;
	}

	protected SoundEvent getHurtSound(DamageSource pDamageSource) {
		return SoundEvents.EMPTY;
	}

	protected SoundEvent getDeathSound() {
		return SoundEvents.EMPTY;
	}

	// base

	protected SyncedData data() {
		return YOUKAI_DATA;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		data().register(builder);
		YoukaiEating.defineSynchedData(builder);
		// local list, not the field: the field does not exist yet during Entity construction
		for (var e : createModules()) {
			e.defineSynchedData(builder);
		}
	}

	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Age", tickCount);
		var cdc = new TagCodec(level().registryAccess());
		tag.put("auto-serial", Objects.requireNonNull(cdc.toTag(new CompoundTag(), this)));
		if (hasRestriction()) {
			var data = cdc.valueToTag(RestrictData.class, new RestrictData(getRestrictCenter(), getRestrictRadius()));
			if (data != null) tag.put("Restrict", data);
		}
		data().write(level().registryAccess(), tag, entityData);
		var moduleTag = new CompoundTag();
		for (var e : modules) {
			var val = cdc.toTag(new CompoundTag(), e);
			if (val != null) {
				moduleTag.put(e.getId().toString(), val);
			}
		}
		tag.put("YoukaiModules", moduleTag);
	}

	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		tickCount = tag.getInt("Age");
		var cdc = new TagCodec(level().registryAccess());
		if (tag.contains("auto-serial")) {
			Wrappers.run(() -> cdc.fromTag(tag.getCompound("auto-serial"), getClass(), this));
		}
		var data = tag.get("Restrict");
		if (data != null) {
			RestrictData res = cdc.valueFromTag(data, RestrictData.class);
			if (res != null) {
				restrictTo(res.center(), (int) res.radius());
			}
		}
		data().read(level().registryAccess(), tag, entityData);
		if (tag.contains("YoukaiModules")) {
			var moduleTag = tag.getCompound("YoukaiModules");
			for (var e : modules) {
				if (!moduleTag.contains(e.getId().toString())) continue;
				var val = moduleTag.getCompound(e.getId().toString());
				cdc.fromTag(val, e.getClass(), e);
			}
		}
		if (getTarget() == null || isSleeping()) {
			navCtrl.setWalking();
		}
	}

	/**
	 * Ticks since she last had a live target, the clock {@code YoukaiCombat#tickTargeting} runs on.
	 * Accessor rather than a bare field: the counter is {@code @SerialField}, so it has to stay
	 * here, and a facet in another package cannot read a protected field.
	 */
	public int noTargetTime() {
		return noTargetTime;
	}

	public void setNoTargetTime(int time) {
		noTargetTime = time;
	}

	// features

	@Override
	public void tick() {
		super.tick();
		combatManager.tick();
	}

	protected void customServerAiStep() {
		targets.tick();
		// the three faceted ticks: targeting and the spell card, then the chew clock
		tickTargeting();
		tickSpell();
		tickEating();
		if (isSleeping()) {
			// Sleep freeze: no brain behavior may move the entity while it sleeps, so drop any stale
			// navigation / control orders and kill momentum (travel/push enforce the same freeze).
			getNavigation().stop();
			navCtrl.stopMoving();
			setDeltaMovement(Vec3.ZERO);
		} else {
			navCtrl.tickMove();
		}
		for (var e : modules) {
			e.tickServer();
		}
		super.customServerAiStep();
	}

	public void setControl(MoveControl ctrl, PathNavigation nav) {
		moveControl = ctrl;
		navigation = nav;
	}

	protected List<AbstractYoukaiModule> createModules() {
		return List.of(new HomeModule(this), new VisitModule(this), new PartyModule(this),
				new GiftModule(this), new TalkModule(this));
	}

	/**
	 * Whether this character is visiting someone else's home. A visitor has no {@link HomeModule} binding,
	 * so it can neither sleep nor claim a bed, and the quest/dialog layer treats it as a guest.
	 */
	public boolean isVisiting() {
		return getFlag(YoukaiFlags.VISITING);
	}

	/**
	 * Whether this character is a guest at somebody's party. Like a visitor she has
	 * no {@link HomeModule} binding, and the quest/dialog layer treats her as a guest.
	 */
	public boolean isPartying() {
		return getFlag(YoukaiFlags.PARTYING);
	}

	/**
	 * Ticks between two hurt reactions. Danmaku lands every few ticks and a flinch restarted on each one
	 * reads as a stutter rather than a hit; the clip itself is half a second long, which sets the number.
	 */
	private static final int HURT_ANIM_COOLDOWN = 10;

	private int lastHurtAnim = -HURT_ANIM_COOLDOWN;

	@Override
	protected void postHurt(DamageSource source) {
		super.postHurt(source);
		if (tickCount - lastHurtAnim < HURT_ANIM_COOLDOWN) return;
		lastHurtAnim = tickCount;
		YoukaiAnim.HURT.play(this);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		for (var e : modules) {
			InteractionResult result = e.interact(player, hand);
			if (result != InteractionResult.PASS)
				return result;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public void handleEntityEvent(byte pId) {
		for (var e : modules) {
			if (e.handleEntityEvent(pId))
				return;
		}
		super.handleEntityEvent(pId);
	}

	@Override
	protected void tickEffects() {
		super.tickEffects();
		// vanilla calls this from baseTick on both sides; the gate is what makes tickClient
		// mean it, matching tickServer, which is server-only through isEffectiveAi()
		if (!level().isClientSide()) return;
		for (var e : modules) {
			e.tickClient();
		}
		crumbs();
	}

	// ---------- pickup ----------

	@Override
	public boolean canPickUpLoot() {
		return tickCount % 10 == 0 && modules.hasPickup();
	}

	@Override
	public boolean wantsToPickUp(ItemStack stack) {
		for (var e : modules) {
			if (e instanceof IPickupModule pick) {
				if (pick.canPickup(stack)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void pickUpItem(ItemEntity item) {
		for (var e : modules) {
			if (e instanceof IPickupModule pick) {
				if (pick.canPickup(item.getItem())) {
					pick.onPickup(item);
					if (item.isRemoved()) {
						return;
					}
				}
			}
		}
	}

	// data

	public <T> Optional<T> getModule(Class<T> cls) {
		return modules.getModule(cls);
	}

	@Override
	protected void actuallyHurt(DamageSource source, float amount) {
		onDamaged(source, amount);
		super.actuallyHurt(source, amount);
	}

	@Override
	public void die(DamageSource source) {
		boolean prev = dead;
		super.die(source);
		for (var module : modules) {
			module.onKilled();
		}
		// dead is protected, so the before/after fold happens here rather than in the facet
		onDeath(!prev && dead, source);
	}

	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity entity) {
		onKillEntity(entity);
		return true;
	}

	public void refreshIdle() {
		noPlayerTime = 0;
	}

	// combat

	protected YoukaiCombatManager createCombatManager() {
		return new DefaultCombatManager(this, null);
	}

	/**
	 * The vanilla gate, kept here because a facet default would be shadowed by
	 * {@code Entity#isInvulnerableTo} without a word. The rest of the question is
	 * {@link YoukaiCombat#invulnerableTo}.
	 */
	@Override
	public final boolean isInvulnerableTo(DamageSource pSource) {
		return invulnerableTo(pSource) || super.isInvulnerableTo(pSource);
	}

	@Override
	public LivingEntity self() {
		return super.self();
	}

	// misc

	@Nullable
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
		initSpellCard();
		return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
	}

	public void initSpellCard() {
	}

	public void startSleeping(BlockPos pos) {
		if (this.isPassenger()) {
			this.stopRiding();
		}
		// Sleep entry cancels all motion at the source: bed-approach walk/fly orders must not outlive
		// sleep onset, and stale flight state (noGravity, FLYING flag) must not carry into the pose.
		this.getNavigation().stop();
		this.navCtrl.stopMoving();
		this.navCtrl.setWalking();
		this.setPose(Pose.SLEEPING);
		this.setPosToBedImpl(pos);
		this.setSleepingPos(pos);
		this.setDeltaMovement(Vec3.ZERO);
		this.hasImpulse = true;
	}

	@Override
	public double getSleepOffset(BlockPos pos) {
		if (level().isLoaded(pos)) {
			return YoukaiBedBlock.getSleepOffset(level().getBlockState(pos));
		}
		return YoukaiBedBlock.VANILLA_SLEEP_OFFSET;
	}

	public boolean mayInteract(Player player) {
		return !isHostileTo(player) && getModule(TalkModule.class).map(e -> !e.isTalking()).orElse(true);
	}

	/**
	 * Whether an interaction belonging to {@code activity} may happen now.
	 * <p>
	 * The one-argument form is the blanket gate - hostile, or already mid-conversation. This adds the urgency
	 * question: being fed or talked to should not pull her out of a fight or a panic, and equally should not
	 * happen while she is already doing the thing asked for. The base class has no priority scale and defers.
	 *
	 * @see SmartYoukaiEntity#mayInteract(Player, Activity)
	 */
	public boolean mayInteract(Player player, Activity activity) {
		return mayInteract(player);
	}

	public ReputationState getReputation(LivingEntity le) {
		return getData(le).map(e -> e.data().getState()).orElse(ReputationState.STRANGER);
	}

	public void setTalkTo(@Nullable ServerPlayer player, int time) {
		if (player == null) return;
		getNavigation().stop();
		lookAt(player, 30, 30);
	}

	@Override
	public void travel(Vec3 vec) {
		// Sleep freeze, client-side half: client physics (gravity, move control)
		// would otherwise drift the sleeping entity between server syncs.
		if (isSleeping()) {
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		super.travel(vec);
	}

	@Override
	public void push(Entity entity) {
		if (isSleeping()) return;
		super.push(entity);
	}

	// A character is anchored to the home or visit site its index entry names, which names one level;
	// a portal would drop it where the index does not describe it, so both gates refuse.

	@Override
	public boolean canUsePortal(boolean allowPassengers) {
		return false;
	}

	@Override
	public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
		return false;
	}

	public boolean mayFly() {
		return true;
	}

	/**
	 * Whether she ever settles onto the ground. A hovering character has no walkable start node - a walk node
	 * wants solid ground under the feet and hers is a block of air - so landing her would leave her unable to
	 * path anywhere, and she would have to climb again next tick.
	 */
	public boolean mayLand() {
		return true;
	}

	public boolean vanishOutOfSight() {
		return true;
	}

	public boolean isNamed() {
		return true;
	}

	public boolean isTalkingTo(ServerPlayer sp) {
		return false;
	}

	@Override
	public Vec3 getVehicleAttachmentPoint(Entity entity) {
		// Match vanilla player sitting height so the sitting animation sits
		// on chairs instead of floating above the seat.
		return new Vec3(0, 0.5, 0);
	}

}