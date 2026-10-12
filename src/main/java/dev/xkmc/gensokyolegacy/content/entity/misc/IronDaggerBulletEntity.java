package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveItem;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveRune;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveRunes;
import dev.xkmc.gensokyolegacy.content.item.tool.IronDaggerItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The thrown form of {@link IronDaggerItem}: an {@link ItemBulletEntity} that hands the dagger
 * back instead of just vanishing when it stops existing.
 * <p>
 * Whether a given throw is to be handed back at all is settled by the thrower through
 * {@link #setReturnable} and then carried by the entity, rather than re-derived from the item and
 * the owner on landing: the throw is the only moment that knows whether the dagger was paid for,
 * and an unpaid throw must not conjure one out of thin air on the way back.
 * <p>
 * All three ways out are overridden rather than only the hit ones — landing on a block or an entity
 * discards straight from {@code DanmakuBulletEntity}, while a dagger that flies its full life is
 * erased through {@link #markErased}. {@link #givenBack} keeps a dagger that goes out through both
 * on one tick (a hit on the tick its life runs out) from being handed over twice.
 * <p>
 * A dagger can also carry a {@link DaggerGloveRune}, which only the {@link DaggerGloveItem} ever
 * sets. It rides the bullet rather than the stack because the stack is long gone by the time
 * anything is hit.
 *
 * @see doc/design/dagger_glove.md
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
	 * what any throw that cost nothing wants: a creative throw, or one a listener cleared
	 * {@link DanmakuUseEvent#consume()} on.
	 */
	public void setReturnable(boolean pReturnable) {
		returnable = pReturnable;
	}

	/**
	 * Gives this dagger a {@link DaggerGloveRune} to run when it hits something, by id; only the
	 * glove sets one. The id is stored rather than the rune, so an id whose rune has since been
	 * removed reads as no rune at the moment of the hit instead of failing on a dagger in mid-air.
	 */
	public void setRune(@Nullable ResourceLocation pRune) {
		rune = pRune;
	}

	/**
	 * Claims this dagger's return, in favour of a successor that is about to be spawned.
	 * <p>
	 * A homing shot is two entities for one spent dagger, so exactly one of them may give it back —
	 * and which one cannot be fixed up front, because the second stage only exists if the turn
	 * actually produced one. This dagger therefore returns itself on every ordinary ending and only
	 * calls this once a successor really exists (dagger_glove.md §4).
	 *
	 * @return true if this dagger may now stop returning itself; false if the claim was already
	 * taken or the dagger is already gone back to its thrower, and the caller must keep its own
	 */
	public boolean handOffTo(boolean successorSpawned) {
		if (!successorSpawned || !ownsReturn()) return false;
		handedOff = true;
		return true;
	}

	/**
	 * Whether this dagger is still the one that owes the thrower a return: it was thrown for a
	 * price, and neither it nor a successor it has spawned has handed that price back.
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

	@Override
	public void tick() {
		super.tick();
		Vec3 vec3 = this.getDeltaMovement();
		double d5 = vec3.x;
		double d6 = vec3.y;
		double d1 = vec3.z;
		for(int i = 0; i < 4; ++i) {
			this.level().addParticle(GLParticles.KNIFE.get(), this.getX() + d5 * (double)i / (double)4.0F, this.getY() + d6 * (double)i / (double)4.0F, this.getZ() + d1 * (double)i / (double)4.0F, -d5, -d6 + 0.2, -d1);
		}
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
	 * Paying the return also disarms this dagger's {@code afterExpiry} trail, and that line is the
	 * whole of the anti-duplication fix: {@code BaseProjectile#tick} resolves the move vector's hit
	 * <em>before</em> the lifetime check, so a dagger hitting on the very tick its life runs out
	 * gives itself back, is discarded, and then carries on into {@code terminate()} anyway — a
	 * returnable stage 2 next to a first stage that already paid (dagger_glove.md §4a). The trail
	 * ends with the dagger rather than outliving it, on every path out of here.
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
	 * Plays the vanilla trident impact for whatever the dagger just struck. Deliberately kept out
	 * of {@link #giveBack}: a dagger that is lost rather than returned still landed with a clang.
	 */
	private void impact(SoundEvent pSound) {
		if (level().isClientSide) return;
		level().playSound(null, this, pSound, SoundSource.PLAYERS, 1F, 1F);
	}

}