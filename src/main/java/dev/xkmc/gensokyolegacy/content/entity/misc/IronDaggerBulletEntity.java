package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveRune;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveRunes;
import dev.xkmc.gensokyolegacy.content.item.tool.IronDaggerItem;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The thrown form of {@link IronDaggerItem}: an {@link ItemBulletEntity} that hands the dagger
 * back instead of just vanishing when it stops existing.
 * <p>
 * The dagger is consumed from the stack by the throw (see {@link IronDaggerItem#use}), so handing
 * it back is what makes the throw reusable instead of a lost item. A dagger with no player to
 * return to is gone for good, which is the only way to lose one.
 * <p>
 * Whether a given throw is to be handed back at all is settled by the thrower through
 * {@link #setReturnable} and then carried by the entity, rather than worked out again from the
 * item and the owner once the dagger has landed: the throw is the only moment that knows whether
 * the dagger was paid for, and a throw that was not (a creative player, or another mod clearing
 * the consume flag of the {@link DanmakuUseEvent} the throw posts) must not conjure one out of
 * thin air on the way back.
 * <p>
 * All three ways out are overridden rather than only the hit ones: landing on a block or on an
 * entity discards straight from {@code DanmakuBulletEntity}, while a dagger that flies its full
 * two seconds is erased through {@link #markErased} without ever hitting anything.
 * {@link #givenBack} keeps a dagger that goes out through both on the same tick (a hit on the
 * tick its life runs out) from being handed over twice, and disarming the {@code afterExpiry}
 * trail there keeps that same tick from spawning a successor on top of the return — which is the
 * other half of that case, and the one that used to hand out a second dagger.
 * <p>
 * A dagger can also carry a {@link DaggerGloveRune}, the glove's extra on-hit effect, which only
 * the {@link DaggerGloveItem} ever sets. It rides the bullet rather than the stack because the
 * stack is long gone by the time anything is hit: the rune's whole job is to act at the moment of
 * impact, on a dagger that is about to be handed back and may not even come back at all.
 */
@SerialClass
public class IronDaggerBulletEntity extends ItemBulletEntity {

	@SerialField
	private boolean returnable = false;
	@SerialField
	private boolean givenBack = false;
	/**
	 * Whether this dagger has already handed its return to a successor, and so must not hand it
	 * back itself. See {@link #handOffTo}.
	 */
	@SerialField
	private boolean handedOff = false;
	@SerialField
	private ResourceLocation rune = null;

	public IronDaggerBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public IronDaggerBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		super(pEntityType, pShooter, pLevel);
	}

	/**
	 * Declares up front that this dagger is one that must come back, i.e. that its throw was paid
	 * for out of the owner's stack. A dagger left unflagged is simply lost when it lands, which is
	 * what any throw that cost nothing wants.
	 */
	public void setReturnable(boolean pReturnable) {
		returnable = pReturnable;
	}

	/**
	 * Gives this dagger a {@link DaggerGloveRune} to run when it hits something, by id. Only the
	 * glove sets one; a plain {@link IronDaggerItem} throw leaves this null and the hit path does
	 * no extra work.
	 * <p>
	 * The id, not the rune itself, because the id is what survives the flight: the rune is resolved
	 * through {@link DaggerGloveRunes} at the moment of the hit, so an id whose rune has since been
	 * removed reads as no rune rather than failing on a dagger in mid-air.
	 */
	public void setRune(@Nullable ResourceLocation pRune) {
		rune = pRune;
	}

	/**
	 * Claims this dagger's return, in favour of a successor that is about to be spawned.
	 * <p>
	 * A homing shot is two entities for one spent dagger, so exactly one of them may give it back.
	 * Which one cannot be fixed up front, because the second stage only exists if the turn actually
	 * produced one: the target may have died in the ten ticks the first stage spends flying, or the
	 * level reference may be gone. Deciding returnability at throw time therefore loses the dagger
	 * whenever the second stage does not materialise.
	 * <p>
	 * So the first stage is flagged returnable like any other and gives the dagger back on every
	 * ordinary ending — block, entity, expiry, being unloaded out of a chunk. Only once it has
	 * genuinely spawned a successor does it call this, which suppresses its own return and lets the
	 * successor carry it. The net effect is that a homing shot returns exactly one dagger on every
	 * path, including the ones where the turn produced nothing.
	 *
	 * @return true if this dagger may now stop returning itself, i.e. the caller really did take
	 * over the return; false if the claim was already made, or the dagger is already gone back to
	 * its thrower, and the caller must keep its own
	 */
	public boolean handOffTo(boolean successorSpawned) {
		if (!successorSpawned || !ownsReturn()) return false;
		handedOff = true;
		return true;
	}

	/**
	 * Whether this dagger is still the one that owes the thrower a return: it was thrown for a
	 * price, and neither it nor a successor it has spawned has handed that price back yet.
	 */
	private boolean ownsReturn() {
		return returnable && !givenBack && !handedOff;
	}

	@Override
	protected void onHitBlock(BlockHitResult pResult) {
		impact(SoundEvents.TRIDENT_HIT_GROUND);
		giveBack();
		super.onHitBlock(pResult);
	}

	@Override
	public void onHitEntity(EntityHitResult pResult) {
		impact(SoundEvents.TRIDENT_HIT);
		runRune(pResult);
		giveBack();
		super.onHitEntity(pResult);
	}

	/**
	 * Runs the carried rune against whatever was hit, if there is a rune to run. The owner is
	 * passed as it is found: a dagger whose player has logged out or left the level still carries
	 * its rune, and a rune that needs an owner simply does nothing without one.
	 */
	private void runRune(EntityHitResult pResult) {
		if (rune == null || level().isClientSide) return;
		Entity entity = pResult.getEntity();
		while (entity instanceof PartEntity<?> part) entity = part.getParent();
		if (!(entity instanceof LivingEntity target)) return;
		DaggerGloveRunes.get(rune).onHit((ServerLevel) level(),
				getOwner() instanceof LivingEntity owner ? owner : null, target, this);
	}

	@Override
	public void markErased(boolean kill) {
		giveBack();
		super.markErased(kill);
	}

	/**
	 * Puts the dagger in its owner's inventory, or at their feet when there is no room for it.
	 * <p>
	 * <b>Paying the return also disarms this dagger's {@code afterExpiry} trail</b>, and that line is
	 * the whole of the anti-duplication fix. {@code ItemBulletEntity#terminate} fires the trail from
	 * {@code BaseProjectile#tick}, which resolves the move vector's hit <em>before</em> it checks the
	 * lifetime — so a dagger that hits a block or an entity on the very tick its life runs out gives
	 * itself back and is discarded, and the tick then carries straight on into {@code terminate()} with
	 * nothing left to stop it. The trail spawned a stage 2 flagged returnable next to a first stage
	 * that had already handed its dagger back, and the shot returned two out of one. Which is why it
	 * only ever showed up where there was something solid in the last leg — a corridor, a room, any
	 * wall inside the turn radius — and never in a superflat.
	 * <p>
	 * Clearing the field rather than overriding {@code terminate} to refuse: {@code terminate} short-
	 * circuits on a null {@code afterExpiry}, which every plain single/fan dagger already relies on to
	 * have no trail at all, so this is not a new assumption about the library — only the statement that
	 * a dagger which has already paid its return has no successor left to delegate it to. The trail
	 * ends with the dagger rather than outliving it, on every path out of here.
	 * <p>
	 * Nothing is handed back when there is no one to hand it to, and the dagger then vanishes as
	 * any other danmaku would: the throw was never flagged returnable (see {@link #setReturnable}),
	 * the return was claimed by a successor (see {@link #handOffTo}), the owner is not a player (a
	 * youkai or doll throwing the item as danmaku), the player is dead or has logged out, or they
	 * left the level while the dagger was still in the air. The trail is disarmed either way, because
	 * in all of those the dagger is finished and a successor could only have conjured a second one.
	 */
	private void giveBack() {
		if (givenBack || !returnable || handedOff || level().isClientSide) return;
		givenBack = true;
		afterExpiry = null;
		if (!(getOwner() instanceof Player player)) return;
		if (!player.isAlive() || player.level() != level()) return;
		ItemStack dagger = getItem();
		if (dagger.isEmpty()) return;
		if (!player.getInventory().add(dagger)) player.drop(dagger, false);
	}

	/**
	 * Plays the vanilla trident impact for whatever the dagger just struck, so that the throw
	 * reads as the weapon it imitates. Deliberately kept out of {@link #giveBack}: a dagger that is
	 * lost rather than returned still landed with a clang.
	 */
	private void impact(SoundEvent pSound) {
		if (level().isClientSide) return;
		level().playSound(null, this, pSound, SoundSource.PLAYERS, 1F, 1F);
	}

}