package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionHandler;
import dev.xkmc.gensokyolegacy.content.entity.dolls.goals.DollCommandGoal;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class DollEntity extends BaseDollEntity
		implements DollGeo, DollLoadout, DollShield, DollDanmakuAlly, DollStray, DollTint.Impl, DollStatus {

	public static final EntityDataAccessor<Integer> DATA_COLOR =
			SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_ACTION_STATE =
			SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_ACTION_TYPE =
			SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_VALID_MASK =
			SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.INT);

	public static final EntityDataAccessor<ItemStack> DATA_MAIN_HAND =
			SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.ITEM_STACK);
	public static final EntityDataAccessor<ItemStack> DATA_OFF_HAND =
			SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.ITEM_STACK);
	public static final EntityDataAccessor<ItemStack> DATA_CLOTH =
			SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.ITEM_STACK);
	public static final EntityDataAccessor<ItemStack> DATA_CORE =
			SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.ITEM_STACK);

	public final DollActionHandler actions = new DollActionHandler();

	private DollCommandGoal commandGoal;

	public DollEntity(EntityType<? extends DollEntity> type, Level level) {
		super(type, level);
	}

	/**
	 * Whether the delegating command goal is currently executing a behavior:
	 * distinguishes attacking from preparing in the synced sidebar status.
	 */
	public boolean isExecutingCommand() {
		return commandGoal != null && commandGoal.isExecuting();
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && !isRemoved()) syncActionStatus();
	}

	@Override
	protected void createDollModules(List<DollModule> modules) {
		super.createDollModules(modules);
		// fresh per-entity instances; static-safe: also runs in Entity ctor before instance state exists
		modules.add(new DollGeo.DollGeoModule(this));
		modules.add(new DollTint.DollTintModule(this));
		modules.add(new DollLoadout.DollLoadoutModule());
		modules.add(new DollStatus.DollStatusModule());
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		commandGoal = new DollCommandGoal(this);
		goalSelector.addGoal(0, commandGoal);
	}

	/**
	 * Doll one-shot animation events (see {@code DollGeo}): consumed on the
	 * client, everything else falls through to vanilla handling.
	 */
	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide() && (id == DollGeo.EVENT_ATTACK || id == DollGeo.EVENT_BOMB ||
				id == DollGeo.EVENT_BOW || id == DollGeo.EVENT_SKILL)) {
			handleDollEvent(id);
			return;
		}
		super.handleEntityEvent(id);
	}

	// ---- Shield (vanilla super-calls stay; shield logic lives in DollShield) ----

	@Override
	public boolean isBlocking() {
		return super.isBlocking() || hasReadyShield();
	}

	@Override
	public boolean isDamageSourceBlocked(DamageSource damageSource) {
		if (!isProjectileDamage(damageSource)) return false;
		return super.isDamageSourceBlocked(damageSource);
	}

	@Override
	protected void hurtCurrentlyUsedShield(float damage) {
		wearShield();
	}

}
