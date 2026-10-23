package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.content.attachment.doll.*;
import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.FollowDollOwnerGoal;
import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.LookAtDollOwnerGoal;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollModule;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollRig;
import dev.xkmc.gensokyolegacy.content.entity.foundation.CombatToClient;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.IDimensionBoundEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Foundation and pairing half of a doll: identity, movement, goals, the deferred damage
 * pipeline, the itemize interaction, the animation plumbing and the never-save guard — everything
 * every doll kind shares, and the part that keeps the ledger honest. What a kind can <i>do</i>
 * lives one layer up: {@link DollEntity} adds the loadout, the commands and the status.
 *
 * @see "doc/design/doll/entity.md"
 * @see "doc/design/doll/summon.md"
 */
public abstract class BaseDollEntity extends DamageRefactorEntity
		implements OwnableEntity, DollRig, IDimensionBoundEntity {
	private static final int SYNC_INTERVAL = 10;
	private static final int INVERSE_INTERVAL = 20;

	/**
	 * The configured default max health (see {@link #createAttributes}); the full-health
	 * reference for items' durability bar and for fresh, data-less summons.
	 */
	public static final float DEFAULT_MAX_HEALTH = 20;

	private UUID ownerUUID;
	private BlockPos homePos;
	public final double stopDistance = 1.0;
	public final double speedModifier = 1;

	/**
	 * Stray host, set by {@code DollEntity.becomeStray()}. While present every pairing query
	 * resolves against the detached entry. Transient — chunk save/load delegates to it instead.
	 */
	@Nullable
	private StrayHost strayHost;

	/**
	 * Movement cap, 0.5 blocks per tick.
	 */
	public static final double MAX_SPEED = 0.5;

	/**
	 * The cap enforced in {@link #tick()}: {@link #MAX_SPEED}, raised for the length of a burst
	 * by the behavior spending it and restored by its {@code stop}. Nothing else may touch it.
	 */
	private double speedCap = MAX_SPEED;

	public double speedCap() {
		return speedCap;
	}

	public void setSpeedCap(double speedCap) {
		this.speedCap = speedCap;
	}

	/**
	 * Per-entity persistence slices. Populated from {@link #createDollModules(List)} in
	 * the base constructor; each hierarchy level appends its own through {@code super}.
	 */
	private final List<DollModule> modules = new ArrayList<>();
	private final Map<Class<? extends DollModule>, DollModule> moduleMap = new LinkedHashMap<>();

	/**
	 * Fetches a module by exact class. Required modules are always registered in
	 * {@code createDollModules}, so absence is a programming error and throws.
	 */
	public <T extends DollModule> T getModule(Class<T> type) {
		DollModule module = moduleMap.get(type);
		if (module == null) throw new IllegalStateException("Missing doll module: " + type.getSimpleName());
		return type.cast(module);
	}

	public BaseDollEntity(EntityType<? extends BaseDollEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		this.moveControl = new DollMoveControl(this);
		this.setNoGravity(true);
		createDollModules(this.modules);
		for (DollModule module : this.modules) {
			if (this.moduleMap.putIfAbsent(module.getClass(), module) != null)
				throw new IllegalStateException("Duplicate doll module: " + module.getClass().getSimpleName());
		}
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
		nav.setCanOpenDoors(true);
		nav.setCanPassDoors(true);
		nav.setCanFloat(true);
		return nav;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH)
				.add(Attributes.ATTACK_DAMAGE, 1)
				.add(Attributes.FOLLOW_RANGE, 48)
				.add(Attributes.FALL_DAMAGE_MULTIPLIER, 0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FollowDollOwnerGoal(this));
		this.goalSelector.addGoal(99999, new LookAtDollOwnerGoal(this));
	}

	public void setOwner(LivingEntity owner) {
		this.ownerUUID = owner.getUUID();
	}

	public boolean isOwner(LivingEntity entity) {
		return ownerUUID != null && ownerUUID.equals(entity.getUUID());
	}

	@Override
	public @Nullable UUID getOwnerUUID() {
		return ownerUUID;
	}

	/**
	 * The owning living entity, or null when it is gone. Wider than
	 * {@code OwnableEntity}'s player-only default: a character can host dolls too.
	 */
	@Override
	public @Nullable LivingEntity getOwner() {
		return resolveOwner() instanceof LivingEntity le ? le : null;
	}

	/**
	 * The owner as a plain entity, resolved wherever it lives: the server-level entity index for
	 * any living owner, the player list as the only thing a client can look up.
	 */
	@Nullable
	private Entity resolveOwner() {
		if (ownerUUID == null) return null;
		if (level() instanceof ServerLevel sl) return sl.getEntity(ownerUUID);
		return level().getPlayerByUUID(ownerUUID);
	}

	/**
	 * Whether this doll is deployed from a player ledger. Only those can be recalled to an
	 * item: a character conjures her dolls out of nothing and has no inventory to hand them to.
	 */
	public boolean isPlayerOwned() {
		return ownerUUID != null && resolveOwner() instanceof ServerPlayer;
	}

	/**
	 * Explicit doll allegiance, on top of the vanilla team check: the owner, everything the
	 * owner owns and everything allied to the owner all count as allies. Ownerless dolls
	 * (block-hosted, stray) band together, mirroring {@code DollDanmakuAlly}.
	 */
	@Override
	public boolean isAlliedTo(Entity other) {
		if (other == this) return true;
		if (super.isAlliedTo(other)) return true;
		if (ownerUUID == null) {
			return other instanceof OwnableEntity own && own.getOwnerUUID() == null;
		}
		if (other.getUUID().equals(ownerUUID)) return true;
		if (other instanceof OwnableEntity own && ownerUUID.equals(own.getOwnerUUID())) return true;
		Entity owner = resolveOwner();
		return owner != null && owner != this && owner.isAlliedTo(other);
	}

	/**
	 * Whether the follow and look goals should steer this doll right now. False only while a
	 * player is driving the kind from their own hands, whose input owns its heading instead.
	 */
	public boolean followsOwner() {
		return true;
	}

	/**
	 * A resident doll is either owned by a living entity (player ledger, or a
	 * character's own host) or hosted by a controller block (homePos set, no owner).
	 */
	public void setHome(BlockPos pos) {
		this.homePos = pos.immutable();
	}

	public boolean isBlockHosted() {
		return homePos != null;
	}

	public boolean isHomedTo(BlockPos pos) {
		return homePos != null && homePos.equals(pos);
	}

	/**
	 * Resolves the doll's host for pairing checks: a stray through its {@link StrayHost}, an
	 * owned doll through its owner — a player through {@link DollAttachment}, any other living
	 * owner through the {@link DollHost} it implements, the one branch that lets a character
	 * conjure her own dolls. {@code null} means the host is unreachable and the doll
	 * self-discards, keeping its data in the ledger for a later resummon.
	 */
	@Nullable
	public DollHost getHost() {
		if (strayHost != null) return strayHost;
		if (ownerUUID != null) {
			if (!(level() instanceof ServerLevel sl)) {
				return null;
			}
			Entity owner = sl.getEntity(ownerUUID);
			if (owner == null || owner.isRemoved() || !owner.isAddedToLevel()) return null;
			if (owner instanceof ServerPlayer sp) {
				return GLMeta.DOLL.type().getOrCreate(sp);
			}
			return owner instanceof DollHost host ? host : null;
		}
		if (homePos == null || !(level() instanceof ServerLevel sl)) {
			return null;
		}
		if (!sl.isLoaded(homePos)) {
			return null;
		}
		return sl.getBlockEntity(homePos) instanceof DollHost host ? host : null;
	}

	// ---- significant-value sync ----
	// The entity is a projection of the owner's DollData (pairing.md) sharing its uuid:
	// minted fresh per materialization and never stored on the item.

	public ResourceLocation getDollTypeId() {
		return getType().builtInRegistryHolder().key().location();
	}

	/**
	 * What a doll of this kind is allowed to become. An item-backed doll by default; a summon or
	 * a gadget overrides this, and it is the only place the kind has to say so —
	 * {@link DollSpawn#materialize} stamps it onto the ledger entry, which is what every
	 * itemize / recall / loadout gate actually reads.
	 *
	 * @see "doc/design/doll/summon.md"
	 */
	public DollNature nature() {
		return DollNature.DOLL;
	}

	/**
	 * Registers persistence slices into the list; subclasses override, call {@code super} and
	 * append their own. Must be static-safe: it also runs inside the vanilla {@code Entity} ctor.
	 */
	protected void createDollModules(List<DollModule> modules) {
		// fresh per-entity instances; static-safe: also runs in Entity ctor before instance state exists
		modules.add(new DollRig.DollRigModule(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		// local list, not the field: the field does not exist yet during Entity construction
		List<DollModule> modules = new ArrayList<>();
		createDollModules(modules);
		for (DollModule module : modules)
			module.defineSynchedData(builder);
	}

	public void writeValuesTo(DollData d) {
		d.type = getDollTypeId();
		d.dimension = level().dimension().location();
		d.position = position();
		d.yRot = getYRot();
		d.customName = getCustomName();
		d.lastUpdate = level().getGameTime();
		for (DollModule module : modules)
			module.writeValuesTo(getEntityData(), d);
	}

	public void readValuesFrom(DollData d) {
		// the entity is always freshly created, so its maxHealth base is already the
		// configured default and combat is the single source of health (never null)
		applyData(d.combat);
		setCustomName(d.customName);
		if (d.position != null) setPos(d.position);
		setYRot(d.yRot);
		for (DollModule module : modules)
			module.readValuesFrom(getEntityData(), d);
	}

	@Nullable
	private DollData resolveDollData() {
		DollHost host = getHost();
		if (host == null) return null;
		DollData data = host.findSummoned(getUUID());
		return data != null && data.isSummoned() ? data : null;
	}

	// ---- deferred damage pipeline ----
	// Damage and healing are intercepted at takeDamage and the onHeal hook, so every
	// change lands in DollData.combat first and is mirrored in after: no guard needed.

	@Override
	protected void takeDamage(DamageSource source, float amount) {
		DollData data = resolveDollData();
		if (data == null) {
			// unpaired: take the plain path. Dropping the hit would read as invulnerability
			// and hide whatever fault left it unpaired
			super.takeDamage(source, amount);
			return;
		}
		float progress = getCombatProgress();
		if (progress <= amount && preventDeath(source)) return;
		deferCombatData(data, progress - amount, false, false);
	}

	@Override
	public void onHeal(float heal) {
		DollData data = resolveDollData();
		if (data == null) return;
		// the parent heal() applies the value to the entity right after this hook,
		// so only the DollData copy needs writing here
		deferCombatData(data, getCombatProgress() + heal, true, false);
	}

	@Override
	public void die(DamageSource damageSource) {
		if (!level().isClientSide()) {
			DollHost host = getHost();
			if (host != null) host.onDeath(this);
		}
		super.die(damageSource);
	}

	private void deferCombatData(DollData data, float amount, boolean force, boolean boostBase) {
		CombatData ans = data.combat.set(this, amount, force, boostBase);
		data.combat = ans;
		boolean update = getCombatData() == null || ans.amount() != getCombatData().amount();
		applyData(ans);
		if (update && isAddedToLevel() && !level().isClientSide())
			CombatToClient.send(this);
	}

	public boolean isStray() {
		return strayHost != null;
	}

	public void setStrayHost(@Nullable StrayHost host) {
		strayHost = host;
	}

	@Nullable
	public StrayHost strayHost() {
		return strayHost;
	}

	// ---- interact: modules first, empty main hand converts back to the item ----

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		for (DollModule module : modules) {
			InteractionResult result = module.interact(player, hand);
			if (result != InteractionResult.PASS) return result;
		}
		if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		if (level().isClientSide()) return InteractionResult.CONSUME;
		// only a player-owned doll converts back to the item here; a block-hosted doll
		// belongs to its controller block, and a character's dolls have no item form. A
		// kind with no item form refuses one outright, so its empty-hand poke is just a poke.
		boolean convertible = isPlayerOwned() && nature().itemizable();
		if (player instanceof ServerPlayer sp && convertible && (isOwner(sp) || sp.getAbilities().instabuild)) {
			DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
			att.itemize(sp, this);
		}
		return InteractionResult.CONSUME;
	}

	// ---- dimension-bound ----
	// A doll projects a ledger entry naming exactly one level, so a portal could strand
	// it where no ledger describes it; canUsePortal is the gate every entry point consults.

	@Override
	public boolean canUsePortal(boolean allowPassengers) {
		return false;
	}

	@Override
	public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
		return false;
	}

	// ---- never leashed, never a passenger ----

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	// ---- pairing: lazy sync + inverse check (world → host) ----

	@Override
	public void tick() {
		super.tick();
		// hard-clamp velocity after super.tick(), so even external pushes (knockback,
		// explosions) cannot exceed the cap
		Vec3 motion = getDeltaMovement();
		double speed = motion.length();
		if (speed > speedCap) setDeltaMovement(motion.scale(speedCap / speed));
		if (level().isClientSide() || isRemoved()) return;
		DollHost host = getHost();
		if (host == null) {
			discard();
			return;
		}
		if (tickCount % SYNC_INTERVAL == 0) {
			host.update(this);
		}
		if (tickCount % INVERSE_INTERVAL == 0) {
			DollData data = host.findSummoned(getUUID());
			if (data == null || !data.type.equals(getDollTypeId())) {
				GensokyoLegacy.LOGGER.warn(GLLang.Doll.RESYNC_ORPHAN.get().getString());
				discard();
			}
		}
	}

	// ---- pairing: one-shot inverse check on join/load ----

	public void onJoinLevelCheck() {
		if (level().isClientSide() || isRemoved()) return;
		DollHost host = getHost();
		if (host == null) {
			discard();
			return;
		}
		DollData data = host.findSummoned(getUUID());
		if (data == null || !data.type.equals(getDollTypeId())) {
			discard();
		}
	}

	// ---- damage / targetability ----

	@Override
	public boolean canBeSeenAsEnemy() {
		return false;
	}

	// ---- never persisted into chunks ----
	// All state lives in the ledger entry, so a doll with no entry cannot be
	// reconstructed and must never be written in the first place (entity.md §5).

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		this.ownerUUID = compound.hasUUID("OwnerUUID") ? compound.getUUID("OwnerUUID") : null;
		if (compound.contains("DollStrayData", Tag.TAG_COMPOUND)) {
			StrayHost loaded = StrayHost.load(level().registryAccess(), compound.getCompound("DollStrayData"));
			if (loaded != null) strayHost = loaded;
		}
		this.setNoGravity(true);
	}

	/**
	 * A doll is a projection of its ledger entry and must not be written into a chunk at all.
	 * A <b>stray</b> is the one exception: its entry was cut from the ledger, so the entity
	 * carries the only copy of that detached data and has to ride the chunk until it dies.
	 * <p>
	 * This has to be the real gate rather than the {@code DollNeverSave} marker alone: a
	 * load-time {@code setRemoved} lands the doll in the world anyway, because
	 * {@code EntityInLevelCallback} is attached after {@code readAdditionalSaveData} and
	 * {@code addEntityWithoutEvent} does not consult the removal reason (entity.md §5).
	 */
	@Override
	public boolean shouldBeSaved() {
		return strayHost != null && strayHost.data() != null;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		if (strayHost != null && strayHost.data() != null) {
			compound.put("DollStrayData", strayHost.save(level().registryAccess()));
		} else {
			// only reached if something forced a save on a paired doll: the marker is
			// ignored on the way back in, so it reads ownerless and both ledgers evict it
			compound.putByte("DollNeverSave", (byte) 1);
		}
	}

}