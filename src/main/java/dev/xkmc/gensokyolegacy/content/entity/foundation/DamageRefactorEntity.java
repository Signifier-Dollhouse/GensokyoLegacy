package dev.xkmc.gensokyolegacy.content.entity.foundation;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

@SerialClass
public class DamageRefactorEntity extends PathfinderMob {

	private long antiHealDisableTimestamp;

	protected DamageRefactorEntity(EntityType<? extends DamageRefactorEntity> entityType, Level level) {
		super(entityType, level);
	}

	public boolean invalidTarget(LivingEntity e) {
		return e.isRemoved() || !e.isAlive() || !e.isAddedToLevel() || e.level() != level();
	}

	@Nullable
	@Override
	public LivingEntity getTarget() {
		LivingEntity ans = super.getTarget();
		if (ans == null || invalidTarget(ans)) return null;
		return ans;
	}

	protected boolean isProtected() {
		return isInvulnerable();
	}

	protected boolean isEffectImmune() {
		return false;
	}

	protected void postHurt(DamageSource source) {
	}

	public boolean isInvulnerableToExtra(DamageSource damage) {
		return isInvulnerable() && !damage.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	protected final void actuallyHurtImpl(DamageSource source, float amount) {
		if (isInvulnerableTo(source)) return;
		damageContainers.peek().setReduction(DamageContainer.Reduction.ARMOR, damageContainers.peek().getNewDamage() - getDamageAfterArmorAbsorb(source, damageContainers.peek().getNewDamage()));
		getDamageAfterMagicAbsorb(source, damageContainers.peek().getNewDamage());
		float damage = CommonHooks.onLivingDamagePre(this, damageContainers.peek());
		damageContainers.peek().setReduction(DamageContainer.Reduction.ABSORPTION, Math.min(getAbsorptionAmount(), damage));
		float absorbed = Math.min(damage, damageContainers.peek().getReduction(DamageContainer.Reduction.ABSORPTION));
		setAbsorptionAmount(Math.max(0, getAbsorptionAmount() - absorbed));
		float f1 = damageContainers.peek().getNewDamage();
		float f = absorbed;
		if (f > 0.0F && f < 3.4028235E37F && source.getEntity() instanceof ServerPlayer serverplayer) {
			serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0F));
		}
		if (f1 != 0.0F) {
			hurtFinal(source, f1);
		}
		CommonHooks.onLivingDamagePost(this, damageContainers.peek());
	}

	protected void hurtFinal(DamageSource source, float amount) {
		if (isInvulnerableToExtra(source)) return;
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			antiHealDisableTimestamp = level().getGameTime() + 100;
			if (isInvulnerable())
				amount = Math.max(amount, Math.max(1, getMaxHealth() * 0.01f));
		}
		getCombatTracker().recordDamage(source, amount);
		takeDamage(source, amount);
		gameEvent(GameEvent.ENTITY_DAMAGE);
		onDamageTaken(damageContainers.peek());
		postHurt(source);
	}

	protected void takeDamage(DamageSource source, float amount) {
		if (getCombatProgress() <= amount && preventDeath(source)) return;
		setCombatProgress(getCombatProgress() - amount, false, false);
	}

	protected boolean preventDeath(DamageSource source) {
		return false;
	}

	public void validateData() {
		if (getCombatProgress() > 0) {
			if (deathTime > 0) deathTime = 0;
			if (dead) dead = false;
		}
	}

	@Override
	public float getHealth() {
		return Math.min(getCombatProgress(), getMaxHealth());
	}

	@Override
	public void setHealth(float amount) {
		if (!Float.isFinite(amount)) return;
		if (level().isClientSide()) {
			setCombatProgress(amount, false, false);
		}
		float health = getCombatProgress();
		if (tickCount > 5 && amount <= health) return;
		setCombatProgress(amount, amount > health, false);
	}

	public void setCombatProgress(float amount) {
		setCombatProgress(amount, true, false);
	}

	public void setCombatProgress(float amount, boolean force, boolean repair) {
		boolean update = combatData == null || amount != combatData.amount();
		if (combatData == null) combatData = CombatData.start(this, amount);
		else combatData = combatData.set(this, amount, force, repair);
		if (!loopingSetHealth) {
			loopingSetHealth = true;
			super.setHealth(amount);
			loopingSetHealth = false;
			if (update && isAddedToLevel() && !level().isClientSide())
				CombatToClient.send(this);
		}
	}

	public float getCombatProgress() {
		if (combatData != null)
			return Math.max(super.getHealth(), combatData.amount());
		if (!level().isClientSide())
			validateCombatData();
		return super.getHealth();
	}

	public void applyData(CombatData data) {
		combatData = data;
		loopingSetHealth = true;
		super.setHealth(data.amount());
		loopingSetHealth = false;
	}

	@Override
	public void heal(float original) {
		if (level().isClientSide()) return;
		var heal = EventHooks.onLivingHeal(this, original);
		if (isEffectImmune() && level().getGameTime() > antiHealDisableTimestamp) {
			heal = Math.max(original, heal);
			if (heal <= 0) return;
		}
		float f = getCombatProgress();
		float m = getMaxHealth();
		heal = Math.min(m - f, heal);
		if (f > 0 && heal > 0) {
			setCombatProgress(f + heal, true, false);
		}
	}

	// guards

	@Override
	protected boolean isImmobile() {
		return this.getCombatProgress() <= 0.0F;
	}

	@Override
	public boolean isDeadOrDying() {
		return this.getCombatProgress() <= 0.0F;
	}

	public boolean isAlive() {
		return !this.isRemoved() && this.getCombatProgress() > 0.0F;
	}

	@Override
	protected void tickDeath() {
		if (getCombatProgress() > 0) return;
		super.tickDeath();
	}

	@Override
	public void die(DamageSource source) {
		if (getCombatProgress() > 0) return;
		if (CommonHooks.onLivingDeath(this, source)) return;
		if (isRemoved() || dead) return;
		Entity entity = source.getEntity();
		LivingEntity livingentity = this.getKillCredit();
		if (this.deathScore >= 0 && livingentity != null) {
			livingentity.awardKillScore(this, this.deathScore, source);
		}
		if (this.isSleeping()) {
			this.stopSleeping();
		}
		this.dead = true;
		this.getCombatTracker().recheckStatus();
		Level level = this.level();
		if (level instanceof ServerLevel serverlevel) {
			if (entity == null || entity.killedEntity(serverlevel, this)) {
				this.gameEvent(GameEvent.ENTITY_DIE);
				this.dropAllDeathLoot(serverlevel, source);
				this.createWitherRose(livingentity);
			}
			this.level().broadcastEntityEvent(this, (byte) 3);
		}
		this.setPose(Pose.DYING);
	}

	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
		if (getCombatProgress() > 0) return;
		super.dropAllDeathLoot(level, source);
	}

	@Override
	public void setPose(Pose pose) {
		if (getCombatProgress() > 0 && pose == Pose.DYING)
			return;
		super.setPose(pose);
	}

	@Override
	public void handleEntityEvent(byte event) {
		if (event == EntityEvent.DEATH && getCombatProgress() > 0)
			return;
		super.handleEntityEvent(event);
	}

	@MustBeInvokedByOverriders
	@Override
	public void tick() {
		validateData();
		super.tick();
		if (combatData != null)
			combatData = combatData.update(this);
		if (tickCount % 20 == 13 && isAddedToLevel() && !level().isClientSide()) {
			validateCombatData();
			CombatToClient.send(this);
		}
	}

	@Override
	public void kill() {
		if (dynamicReductionRate() > 0 && !level().isClientSide()) {
			combatData = new CombatData(0, 0);
			super.setHealth(0);
			die(damageSources().genericKill());
			CombatToClient.send(this);
			return;
		}
		super.kill();
	}

	// data

	private CombatData combatData;
	private boolean loopingSetHealth = false;

	public CombatData getCombatData() {
		return combatData;
	}

	public void validateCombatData() {
		if (loopingSetHealth) return;
		if (combatData == null) {
			combatData = CombatData.start(this, super.getHealth());
		} else {
			if (super.getHealth() < combatData.amount()) {
				loopingSetHealth = true;
				super.setHealth(combatData.amount());
				loopingSetHealth = false;
			} else combatData = combatData.set(this, super.getHealth(), true, false);
		}
	}

	protected float dynamicReductionRate() {
		return 0;
	}

	protected float dynamicReductionCap() {
		return 0.2f;
	}

	public record CombatData(float amount, float baseline) {

		public static CombatData start(DamageRefactorEntity e, float amount) {
			var rate = e.dynamicReductionRate();
			float base = rate == 0 ? 0 : amount - e.getMaxHealth() * e.dynamicReductionCap();
			return new CombatData(amount, base);
		}

		public CombatData set(DamageRefactorEntity e, float amount, boolean force, boolean boostBase) {
			var rate = e.dynamicReductionRate();
			float ans = rate > 0 && !force ? Math.max(amount, baseline) : amount;
			float base = rate > 0 && boostBase && ans > amount() ?
					Math.max(baseline + ans - amount(), ans - e.getMaxHealth() * e.dynamicReductionCap()) :
					Math.min(baseline, ans);
			return new CombatData(ans, base);
		}

		public CombatData update(DamageRefactorEntity e) {
			var rate = e.dynamicReductionRate();
			if (rate == 0) return this;
			var max = e.getMaxHealth();
			var allowed = max * e.dynamicReductionCap();
			var minBase = Math.max(0, amount - allowed);
			if (baseline <= minBase) {
				if (e.isAggressive())
					return this;
				var maxBase = Math.min(minBase, baseline + max / 1200f);
				return new CombatData(amount, maxBase);
			}
			return new CombatData(amount, Math.max(minBase, baseline - allowed / rate));
		}
	}

}