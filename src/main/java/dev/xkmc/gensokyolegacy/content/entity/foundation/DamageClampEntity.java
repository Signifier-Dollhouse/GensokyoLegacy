package dev.xkmc.gensokyolegacy.content.entity.foundation;

import com.google.common.collect.Sets;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFeatureSet;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

@SerialClass
public class DamageClampEntity extends DamageRefactorEntity {

	protected final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);

	private boolean hurtCall = false;
	private final Set<ServerPlayer> players = Sets.newHashSet();

	protected DamageClampEntity(EntityType<? extends DamageRefactorEntity> entityType, Level level) {
		super(entityType, level);
		bossEvent.setVisible(false);
	}

	@Override
	public boolean mayBeLeashed() {
		return false;
	}

	@Override
	protected boolean canRide(Entity pVehicle) {
		return false;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}

	public YoukaiFeatureSet getFeatures() {
		return YoukaiFeatureSet.NONE;
	}

	public boolean shouldIgnore(LivingEntity e) {
		return false;
	}

	@Override
	public boolean canBeAffected(MobEffectInstance ins) {
		return !isEffectImmune() && super.canBeAffected(ins);
	}

	@Override
	protected boolean isEffectImmune() {
		return getFeatures().effectImmune();
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide()) {
			if (isEffectImmune() && !getActiveEffectsMap().isEmpty()) {
				removeAllEffects();
			}
			bossEvent.setProgress(getCombatProgress() / getMaxHealth());
		}
	}

	@Override
	public void validateData() {
		super.validateData();
		if (!level().isClientSide()) {
			double maxSpeed = getFeatures().maxSpeed();
			if (getDeltaMovement().length() > maxSpeed) {
				setDeltaMovement(getDeltaMovement().normalize().scale(maxSpeed));
			}
		}
	}

	@Override
	public boolean canSwimInFluidType(FluidType type) {
		return isEffectImmune();
	}

	@Override
	public boolean fireImmune() {
		return isEffectImmune();
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return getFeatures().damageFilter() &&
				!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
				!(source.getEntity() instanceof LivingEntity) ||
				super.isInvulnerableTo(source);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!source.is(DamageTypes.GENERIC_KILL) || source.getEntity() != null) {
			if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
					!(source.getEntity() instanceof LivingEntity) &&
					getFeatures().damageFilter())
				return false;
			if (source.getEntity() instanceof LivingEntity le) {
				if (shouldIgnore(le)) return false;
			}
		}

		hurtCall = true;
		boolean ans = super.hurt(source, amount);
		hurtCall = false;
		return ans;
	}

	@Override
	protected void actuallyHurt(DamageSource source, float amount) {
		if (!hurtCall) return;
		super.actuallyHurt(source, amount);
	}

	@Override
	protected float dynamicReductionRate() {
		if (getFeatures().limiter() <= 1)
			return 0;
		return getFeatures().dynamicReductionRate();
	}

	@Override
	protected float dynamicReductionCap() {
		return 1f / getFeatures().limiter();
	}

	// boss bar

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		var name = getDisplayName();
		if (hasCustomName() && name != null) {
			bossEvent.setName(name);
		}
	}

	public void setCustomName(@Nullable Component pName) {
		super.setCustomName(pName);
		if (pName != null) bossEvent.setName(pName);
	}

	public void startSeenByPlayer(ServerPlayer pPlayer) {
		super.startSeenByPlayer(pPlayer);
		players.add(pPlayer);
		this.bossEvent.addPlayer(pPlayer);
	}

	public void stopSeenByPlayer(ServerPlayer pPlayer) {
		super.stopSeenByPlayer(pPlayer);
		players.remove(pPlayer);
		this.bossEvent.removePlayer(pPlayer);
	}

	public Collection<ServerPlayer> getPlayers() {
		return players;
	}

}
