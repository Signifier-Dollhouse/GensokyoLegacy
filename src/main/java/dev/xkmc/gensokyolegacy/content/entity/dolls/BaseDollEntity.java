package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.StrayHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.FollowDollOwnerGoal;
import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.LookAtDollOwnerGoal;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollModule;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollTint;
import dev.xkmc.gensokyolegacy.content.entity.foundation.CombatToClient;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public abstract class BaseDollEntity extends DamageRefactorEntity implements OwnableEntity, DollTint {
	private static final int SYNC_INTERVAL = 10;
	private static final int INVERSE_INTERVAL = 20;

	/**
	 * The doll entity's configured default max health (see {@link #createAttributes}); used as the
	 * full-health reference for items' durability bar and summons of fresh (data-less) dolls.
	 */
	public static final float DEFAULT_MAX_HEALTH = 20;

	private UUID ownerUUID;
	private BlockPos homePos;
	public final double stopDistance = 1.0;
	public final double speedModifier = 1;

	/**
	 * Stray host, set by {@code DollEntity.becomeStray()}. While present every
	 * pairing query resolves against the detached entry, so no other stray
	 * branches are needed anywhere: sync, inverse check, damage and heal all flow
	 * through {@link DollHost} uniformly. Transient — chunk save/load delegates to
	 * it instead (see below).
	 */
	@Nullable
	private StrayHost strayHost;

	/**
	 * Movement cap: 0.5 blocks per tick (doc/design/doll/entity.md).
	 */
	public static final double MAX_SPEED = 0.5;

	/**
	 * Per-entity persistence slices. Populated from {@link #createDollModules(List)} in the
	 * base constructor; each hierarchy level appends its own through {@code super}.
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
				.add(Attributes.FOLLOW_RANGE, 48)
				.add(Attributes.FALL_DAMAGE_MULTIPLIER, 0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FollowDollOwnerGoal(this));
		this.goalSelector.addGoal(99999, new LookAtDollOwnerGoal(this));
	}

	public void setOwner(Player owner) {
		this.ownerUUID = owner.getUUID();
	}

	public boolean isOwner(Player player) {
		return ownerUUID != null && ownerUUID.equals(player.getUUID());
	}

	@Override
	public @Nullable UUID getOwnerUUID() {
		return ownerUUID;
	}

	/**
	 * Explicit doll allegiance, on top of the vanilla team check: the owner,
	 * everything the owner owns (fellow dolls, pets), and everything allied
	 * to the owner (teams and the like) all count as allies. Ownerless dolls
	 * (block-hosted, stray) band together, mirroring the danmaku friendly
	 * rule in {@link dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollDanmakuAlly}.
	 * This drives explosive-hexbrew filtering (blast + bottle pass-through).
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
		Entity owner = level() instanceof ServerLevel sl ?
				sl.getEntity(ownerUUID) : level().getPlayerByUUID(ownerUUID);
		return owner != null && owner != this && owner.isAlliedTo(other);
	}

	/**
	 * A resident doll is either owned by a player (ownerUUID set) or hosted by a
	 * controller block (homePos set, no owner). Player-owned dolls are deployed from
	 * the {@link DollAttachment} ledger; block-hosted dolls from a
	 * {@link dev.xkmc.gensokyolegacy.content.block.functional.doll.DollControllerBlockEntity}.
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
	 * Resolves the doll's host for pairing checks (§8.2). A stray doll resolves to
	 * its {@link StrayHost} first, so all pairing queries just work with no stray
	 * branches elsewhere. {@code null} means the host is unreachable (owner
	 * offline, home chunk unloaded/block gone) and the doll self-discards: its
	 * data is kept in the ledger and a fresh entity is respawned from the cached
	 * values when the host is reachable again.
	 */
	@Nullable
	public DollHost getHost() {
		if (strayHost != null) return strayHost;
		if (ownerUUID != null) {
			Player owner = level().getPlayerByUUID(ownerUUID);
			if (owner instanceof ServerPlayer sp) {
				return GLMeta.DOLL.type().getOrCreate(sp);
			}
			return null;
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
	// The entity is a projection of the owning player's DollData (doc/design/doll/pairing.md).
	// Identity between entity and DollData is the entity's own game UUID: every
	// materialization mints a fresh one, it is never stored on the item, and it
	// survives dimension changes. No separate doll uuid is kept, so the ledger
	// lookups (`level().getEntity(data.uuid)`) always hit the real entity.

	public ResourceLocation getDollTypeId() {
		return getType().builtInRegistryHolder().key().location();
	}

	/**
	 * Registers persistence slices into the given list. Subclasses override, call
	 * {@code super}, and append their own — chaining across multiple hierarchy levels.
	 * Must be static-safe (fresh instances, no instance state): it also runs inside the
	 * vanilla {@code Entity} constructor via {@code defineSynchedData}, before subclass
	 * state exists. A module with synced accessors must be registered here, or its
	 * accessors are never defined.
	 */
	protected void createDollModules(List<DollModule> modules) {
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
		// the entity is always freshly created (doSummon/spawnFromData), so its maxHealth base
		// value is already the entity's configured default (createAttributes) — nothing to reset.
		// combat is the single source of health (never null): seed the entity's CombatData from it.
		// the caller sets data.position/data.yRot beforehand — this applies them to the entity.
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
	// Damage and healing are intercepted at takeDamage and the onHeal hook instead of at
	// setCombatProgress (doc/design/doll/pairing.md): every combat change lands in DollData.combat
	// first and is only then mirrored into the entity's CombatData, so no re-entrancy
	// guard is needed. Without a paired entry the plain path is used.

	@Override
	protected void takeDamage(DamageSource source, float amount) {
		DollData data = resolveDollData();
		if (data == null) return;
		float progress = getCombatProgress();
		if (progress <= amount && preventDeath(source)) return;
		deferCombatData(data, progress - amount, false, false);
	}

	@Override
	public void onHeal(float heal) {
		DollData data = resolveDollData();
		if (data == null) return;
		// the value is applied to the entity by the parent heal() right after this hook,
		// so only the DollData copy needs writing here.
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
		// belongs to a controller block and is managed through the block (§13).
		if (player instanceof ServerPlayer sp && ownerUUID != null && (isOwner(sp) || sp.getAbilities().instabuild)) {
			DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
			att.itemize(sp, this);
		}
		return InteractionResult.CONSUME;
	}

	// ---- pairing: lazy sync + inverse check (world → host) ----

	@Override
	public void tick() {
		super.tick();
		// Movement cap: hard-clamp velocity to MAX_SPEED blocks/tick so even external
		// pushes (knockback, explosions) cannot exceed the cap (doc/design/doll/entity.md).
		Vec3 motion = getDeltaMovement();
		double speed = motion.length();
		if (speed > MAX_SPEED) setDeltaMovement(motion.scale(MAX_SPEED / speed));
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
	// Only a marker is written, so a doll stored in a chunk is detected on load and
	// discarded. All state lives in the player capability (doc/design/doll/entity.md).

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		this.ownerUUID = compound.hasUUID("OwnerUUID") ? compound.getUUID("OwnerUUID") : null;
		if (compound.contains("DollStrayData", Tag.TAG_COMPOUND)) {
			StrayHost loaded = StrayHost.load(level().registryAccess(), compound.getCompound("DollStrayData"));
			if (loaded != null) strayHost = loaded;
		}
		this.setNoGravity(true);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		if (strayHost != null && strayHost.data() != null) {
			compound.put("DollStrayData", strayHost.save(level().registryAccess()));
		} else {
			compound.putByte("DollNeverSave", (byte) 1);
		}
	}

}