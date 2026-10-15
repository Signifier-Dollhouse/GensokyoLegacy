package dev.xkmc.gensokyolegacy.content.entity.youkai;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.api.IYoukaiEntity;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.spell.spellcard.SpellCardWrapper;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.fastprojectileapi.spellcircle.SpellCircleHolder;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationState;
import dev.xkmc.gensokyolegacy.content.block.deco.bed.YoukaiBedBlock;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.YoukaiNavigationControl;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageClampEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.IDimensionBoundEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.ISleepOffsetEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.facet.YoukaiAnim;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2core.base.entity.SyncedData;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SerialClass
public abstract class YoukaiEntity extends DamageClampEntity implements SpellCircleHolder, IYoukaiEntity, ISleepOffsetEntity, IDimensionBoundEntity {

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(YoukaiEntity.class, ser);
	}

	protected static final SyncedData YOUKAI_DATA = new SyncedData(YoukaiEntity::defineId);

	protected static final EntityDataAccessor<Integer> DATA_FLAGS_ID = YOUKAI_DATA.define(SyncedData.INT, 0, "youkai_flags");

	/**
	 * {@link SyncedData} ships a handful of primitives and no stack; what she is chewing rides as a
	 * plain {@link ItemStack} codec over the vanilla serializer.
	 */
	private static final SyncedData.Serializer<ItemStack> ITEM_STACK =
			new SyncedData.Simple<>(EntityDataSerializers.ITEM_STACK, ItemStack.CODEC);

	/**
	 * What she is chewing, and how much of the chew is left. Both client-visible, and both named
	 * null where the flags are not: a chew is a gesture, not a state, and a persisted countdown
	 * would outlive an unload that cuts the feeding task off before its {@code stop}.
	 * <p>
	 * They have to be synced at all because a character eats by playing {@link YoukaiAnim#EAT},
	 * which is a rig clip with no vanilla use timer behind it - nothing on the client knows the
	 * chew is running. The stack also outlives the hand on purpose: every chew empties that hand
	 * part-way through, and the crumbs have to carry on to the end.
	 */
	private static final EntityDataAccessor<ItemStack> DATA_EAT_ITEM = YOUKAI_DATA.define(ITEM_STACK, ItemStack.EMPTY, null);
	private static final EntityDataAccessor<Integer> DATA_EAT_TICKS = YOUKAI_DATA.define(SyncedData.INT, 0, null);

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

	public boolean getFlag(YoukaiFlags val) {
		int flag = 1 << val.ordinal();
		return (this.entityData.get(DATA_FLAGS_ID) & flag) != 0;
	}

	/**
	 * The bitfield is an {@code int} and has to stay one: these used to be cast to {@code byte}, silently
	 * dropping any flag past the eighth bit, which made the ninth flag and every one after it unsettable.
	 */
	public void setFlag(YoukaiFlags val, boolean enable) {
		int b0 = this.entityData.get(DATA_FLAGS_ID);
		int flag = 1 << val.ordinal();
		if (enable) {
			b0 |= flag;
		} else {
			b0 &= -1 - flag;
		}
		this.entityData.set(DATA_FLAGS_ID, b0);
	}

	// features

	@Override
	public void tick() {
		super.tick();
		combatManager.tick();
	}

	protected void customServerAiStep() {
		targets.tick();
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
		for (var e : modules) {
			e.tickClient();
		}
		crumbs();
	}

	// ---------- eating ----------

	/**
	 * Crumbs, on vanilla's own cadence: {@link #CRUMBS} every {@link #CRUMB_EVERY} ticks.
	 * Vanilla also holds them back until {@code useItem.getUseDuration() * 0.21875}, which has
	 * nothing to bite into here - a chew is a rig clip, not a use timer.
	 */
	private static final int CRUMB_EVERY = 4;
	private static final int CRUMBS = 5;

	/**
	 * Begin a chew: the dish she is eating and how many ticks of it are left.
	 * <p>
	 * Called by whoever is feeding her mouth - {@code FairyFeedTask} off a hand-out,
	 * {@code PartyContext#beginChew} off a plate - rather than off {@link YoukaiAnim#EAT},
	 * because only the caller knows what she has and how long its own chew is meant to run.
	 */
	public void startEating(ItemStack food, int ticks) {
		entityData.set(DATA_EAT_ITEM, food);
		entityData.set(DATA_EAT_TICKS, ticks);
	}

	public void stopEating() {
		entityData.set(DATA_EAT_ITEM, ItemStack.EMPTY);
		entityData.set(DATA_EAT_TICKS, 0);
	}

	/** The dish she is chewing, or empty. Only ever read for the crumbs. */
	public ItemStack getEatItem() {
		return entityData.get(DATA_EAT_ITEM);
	}

	public int getEatTicks() {
		return entityData.get(DATA_EAT_TICKS);
	}

	/**
	 * The clock, counted here rather than by the caller so the two chews cannot drift apart: each
	 * has its own behaviour driving its own gesture, and a second thing decrementing this would be
	 * a third place to keep in step. Floored, because a spent chew stays spent.
	 */
	private void tickEating() {
		int left = getEatTicks();
		if (left > 0) entityData.set(DATA_EAT_TICKS, left - 1);
	}

	/**
	 * Crumbs for as long as the chew is running, which is not the same as how long the dish was
	 * in her hand: every chew takes it out of there part-way through.
	 * <p>
	 * Client-side only. Server-side {@code addParticle} is a broadcast, so ticking this on the
	 * server would send every crumb to every watching client <em>as well</em> as the client
	 * drawing its own.
	 */
	private void crumbs() {
		int left = getEatTicks();
		if (!level().isClientSide() || left <= 0 || left % CRUMB_EVERY != 0) return;
		var food = getEatItem();
		if (food.isEmpty()) return;
		for (int i = 0; i < CRUMBS; i++) crumb(food);
	}

	/**
	 * One crumb at her mouth, thrown the way vanilla throws one: a small cone out of the eye
	 * position, which on these rigs is close enough to the mouth for the spread to read as coming
	 * from it rather than from her chest.
	 */
	private void crumb(ItemStack food) {
		var out = new Vec3((random.nextFloat() - 0.5F) * 0.2f, -random.nextFloat() * 0.2 - 0.1, 0.6)
				.xRot(-getXRot() * ((float) Math.PI / 180F))
				.yRot(-getYRot() * ((float) Math.PI / 180F))
				.add(getEyePosition());
		var vel = new Vec3((random.nextFloat() - 0.5F) * 0.1, random.nextFloat() * 0.1 + 0.1, 0.0)
				.xRot(-getXRot() * ((float) Math.PI / 180F))
				.yRot(-getYRot() * ((float) Math.PI / 180F));
		level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, food),
				out.x, out.y, out.z, vel.x, vel.y + 0.05, vel.z);
	}

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

	public Optional<Player> resolvePlayer(@Nullable Entity e) {
		if (e instanceof Player player) {
			return Optional.of(player);
		}
		if (e instanceof OwnableEntity own && level() instanceof ServerLevel sl) {
			var id = own.getOwnerUUID();
			if (id != null && sl.getEntity(id) instanceof Player player) {
				return Optional.of(player);
			}
		}
		return Optional.empty();
	}

	public Optional<CharDataHolder> getData(@Nullable Entity e) {
		return resolvePlayer(e).map(player -> CharDataHolder.get(player, this));
	}

	public <T> Optional<T> getModule(Class<T> cls) {
		return modules.getModule(cls);
	}

	@Override
	protected void actuallyHurt(DamageSource source, float amount) {
		if (spellCard != null) spellCard.hurt(cardHolder, source, amount);
		var holder = getData(source.getEntity());
		holder.ifPresent(e -> e.onHurt(source, amount));
		if (!level().isClientSide()) {
			holder.ifPresent(e -> {
				if (vanishOnDislike() && e.data().reputation < ReputationConstants.DISCARD_REP_THRESHOLD &&
						e.player() instanceof ServerPlayer sp) {
					sp.displayClientMessage(GLLang.Info.YOUKAI_AVOID.get(getDisplayName()), false);
					discard();
				}
			});
		}
		super.actuallyHurt(source, amount);
	}

	/**
	 * Whether losing the last of a player's favour makes this character give up and disappear, the
	 * {@code avoid} reaction of a youkai that will not share a world with whoever mistreated her.
	 * <p>
	 * A character whose answer to a beating is to run turns this off: the threshold would otherwise delete her
	 * on the very tick the flee begins. The reputation cost still stands, so she ends up stranger-or-worse.
	 */
	public boolean vanishOnDislike() {
		return true;
	}

	@Override
	public void die(DamageSource source) {
		boolean prev = dead;
		super.die(source);
		for (var module : modules) {
			module.onKilled();
		}
		var e = source.getEntity();
		if (!prev && dead && e instanceof LivingEntity le) {
			if (!e.isAlive() || !e.isAddedToLevel() || e.isRemoved())
				return;
			onKilledBy(le, source);
		}
	}

	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity entity) {
		getData(entity).ifPresent(CharDataHolder::onKilledByCharacter);
		return true;
	}

	protected void onKilledBy(LivingEntity le, DamageSource source) {
		getData(le).ifPresent(CharDataHolder::onKillCharacter);
	}

	public void refreshIdle() {
		noPlayerTime = 0;
	}

	// combat

	protected YoukaiCombatManager createCombatManager() {
		return new DefaultCombatManager(this, null);
	}

	protected void tickTargeting() {
		if (getTarget() == null || !getTarget().isAlive()) {
			noTargetTime++;
			boolean doHeal = getFeatures().noTargetHealing() && noTargetTime >= 20 && tickCount % 20 == 0;
			doHeal |= getCombatProgress() < getMaxHealth();
			if (doHeal && getLastHurtByMob() instanceof Player player && player.getAbilities().instabuild) {
				if (tickCount - getLastHurtByMobTimestamp() < 100) {
					doHeal = false;
				}
			}
			if (doHeal) {
				setHealth(getMaxHealth());
			}
		} else {
			noTargetTime = 0;
		}
		if (noTargetTime == 0 && getFeatures().hasBossBar()) {
			bossEvent.setVisible(true);
		}
		if (noTargetTime > 40) {
			bossEvent.setVisible(false);
		}
		noPlayerTime = 0;
	}

	protected void tickSpell() {
		if (spellCard != null) {
			if (isAggressive() && shouldShowSpellCircle()) {
				spellCard.tick(cardHolder);
			} else {
				spellCard.reset();
			}
		}
	}

	public boolean shouldIgnore(LivingEntity e) {
		if (e instanceof Player player && player.getAbilities().instabuild)
			return false;
		if (!targets.isValidTarget(e))
			return true;
		var event = new YoukaiFightEvent(this, e);
		return NeoForge.EVENT_BUS.post(event).isCanceled();
	}

	@Override
	public final boolean isInvulnerableTo(DamageSource pSource) {
		if (pSource.getEntity() instanceof LivingEntity le && shouldIgnore(le)) {
			return true;
		}
		return pSource.is(DamageTypeTags.IS_FALL) ||
				combatManager.isInvulnerableTo(pSource) ||
				super.isInvulnerableTo(pSource);
	}

	public final boolean wouldInitiateAttack(LivingEntity entity) {
		if (entity instanceof Player) return false;
		if (!targets.isValidTarget(entity)) return false;
		if (shouldIgnore(entity)) return false;
		return combatManager.targetKind(entity).initiateAttack();
	}

	public final boolean isHostileTo(LivingEntity le) {
		return targets.contains(le) || wouldInitiateAttack(le);
	}

	public final boolean shouldHurt(LivingEntity le) {
		if (shouldIgnore(le)) return false;
		if (le instanceof Player && targets.isValidTarget(le)) return true;
		return isHostileTo(le) || combatManager.targetKind(le).isPrey() || combatManager.shouldHurtInnocent(le);
	}

	public final void onDanmakuHit(LivingEntity e, IDanmakuEntity danmaku) {
		combatManager.onDanmakuHit(e, danmaku);
	}

	@Override
	public void onDanmakuImmune(LivingEntity e, IDanmakuEntity danmaku, DamageSource source) {
		combatManager.onDanmakuImmune(e, danmaku, source);
	}

	@Override
	public LivingEntity self() {
		return super.self();
	}

	@Override
	public final boolean shouldShowSpellCircle() {
		return combatManager.getSpellCircle() != null;
	}

	@Override
	public final @Nullable ResourceLocation getSpellCircle() {
		return combatManager.getSpellCircle();
	}

	@Override
	public final float getCircleSize(float pTick) {
		return combatManager.getCircleSize(pTick);
	}

	public void shoot(float dmg, int life, Vec3 vec, DyeColor color) {
		ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), this, level());
		danmaku.setItem(DanmakuItems.Bullet.CIRCLE.get(color).asStack());
		danmaku.setup(dmg, life, true, true, vec);
		level().addFreshEntity(danmaku);
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