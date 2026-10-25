package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollNature;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollModule;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * The player-bound support ghost: it pairs, follows and is restored like any doll, but carries no
 * item, no loadout and no command surface. Its one contribution is the variant aura it provides
 * while its owner is nearby.
 *
 * @see "doc/design/doll/stray.md"
 */
public class GhostEntity extends BaseDollEntity implements GhostVariantHolder, GhostPowerHolder {

	public static final ResourceLocation TYPE = GensokyoLegacy.loc("ghost");
	public static final EntityDataAccessor<Integer> DATA_VARIANT =
			SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_POWER =
			SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.INT);

	private static final double ABILITY_RANGE = 16.0;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
	private static final RawAnimation SKILL = RawAnimation.begin().thenPlay("skill");

	public GhostEntity(EntityType<? extends GhostEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public DollNature nature() {
		return DollNature.SUMMON;
	}

	@Override
	protected void createDollModules(List<DollModule> modules) {
		super.createDollModules(modules);
		modules.add(new GhostVariantModule());
		modules.add(new GhostPowerModule());
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
	public void tick() {
		super.tick();
		if (level().isClientSide() || isRemoved()) return;
		LivingEntity owner = getOwner();
		if (owner == null || !owner.isAlive() || owner.level() != level() || owner.isSpectator()) return;
		GhostVariant variant = getVariant();
		if (getPower() < GhostPower.LEAVE_THRESHOLD && variant.handler.leavesWhenWeak()
				&& owner instanceof ServerPlayer sp) {
			leaveOwner(sp);
			return;
		}
		variant.handler.tick(this);
		if (distanceToSqr(owner) > ABILITY_RANGE * ABILITY_RANGE) return;
		variant.handler.aura(this, owner);
	}

	/**
	 * Below {@link GhostPower#LEAVE_THRESHOLD} the binding breaks: release spawns the wild
	 * counterpart at the current spot, so the power is not lost and the fan can still absorb it.
	 */
	private void leaveOwner(ServerPlayer owner) {
		if (GLMeta.DOLL.type().getOrCreate(owner).release(owner, this)) {
			owner.displayClientMessage(GLLang.Ghost.LEFT.get(), true);
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, ANIM_CONTROLLER, ANIM_TRANSITION,
				e -> e.setAndContinue(e.isMoving() ? FLY : IDLE)));
		controllers.add(new AnimationController<>(this, ONE_SHOT_CONTROLLER, ANIM_TRANSITION,
				e -> e.getController().isPlayingTriggeredAnimation() ? PlayState.CONTINUE : PlayState.STOP)
				.receiveTriggeredAnimations()
				.triggerableAnim("skill", SKILL));
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide() && id == EVENT_SKILL) {
			triggerAnim(ONE_SHOT_CONTROLLER, "skill");
			return;
		}
		super.handleEntityEvent(id);
	}

}
