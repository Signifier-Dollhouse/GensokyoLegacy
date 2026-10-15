package dev.xkmc.gensokyolegacy.content.entity.youkai.impl;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A character chewing: the dish she is eating, how much of it is left, and the crumbs.
 * <p>
 * The two accessors are defined onto the vanilla builder rather than onto the {@code SyncedData}
 * chain: both are named {@code null}, so they were never part of the NBT contract and leaving the
 * chain behind them changed nothing — {@code doc/design/youkai/entity.md} §3.
 */
public interface YoukaiEating extends YoukaiBaseImpl {

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
	EntityDataAccessor<ItemStack> DATA_EAT_ITEM = SynchedEntityData.defineId(YoukaiEntity.class,
			EntityDataSerializers.ITEM_STACK);
	EntityDataAccessor<Integer> DATA_EAT_TICKS = SynchedEntityData.defineId(YoukaiEntity.class,
			EntityDataSerializers.INT);

	/**
	 * Crumbs, on vanilla's own cadence: {@link #CRUMBS} every {@link #CRUMB_EVERY} ticks.
	 * Vanilla also holds them back until {@code useItem.getUseDuration() * 0.21875}, which has
	 * nothing to bite into here - a chew is a rig clip, not a use timer.
	 */
	int CRUMB_EVERY = 4;
	int CRUMBS = 5;

	/**
	 * Begin a chew: the dish she is eating and how many ticks of it are left.
	 * <p>
	 * Called by whoever is feeding her mouth - {@code FairyFeedTask} off a hand-out,
	 * {@code PartyContext#beginChew} off a plate - rather than off {@link YoukaiAnim#EAT}, because
	 * only the caller knows what she has and how long its own chew is meant to run.
	 */
	default void startEating(ItemStack food, int ticks) {
		getEntityData().set(DATA_EAT_ITEM, food);
		getEntityData().set(DATA_EAT_TICKS, ticks);
	}

	default void stopEating() {
		getEntityData().set(DATA_EAT_ITEM, ItemStack.EMPTY);
		getEntityData().set(DATA_EAT_TICKS, 0);
	}

	/** The dish she is chewing, or empty. Only ever read for the crumbs. */
	default ItemStack getEatItem() {
		return getEntityData().get(DATA_EAT_ITEM);
	}

	default int getEatTicks() {
		return getEntityData().get(DATA_EAT_TICKS);
	}

	/**
	 * The clock, counted here rather than by the caller so the two chews cannot drift apart: each
	 * has its own behaviour driving its own gesture, and a second thing decrementing this would be
	 * a third place to keep in step. Floored, because a spent chew stays spent.
	 */
	default void tickEating() {
		int left = getEatTicks();
		if (left > 0) getEntityData().set(DATA_EAT_TICKS, left - 1);
	}

	/**
	 * Crumbs for as long as the chew is running, which is not the same as how long the dish was
	 * in her hand: every chew takes it out of there part-way through.
	 * <p>
	 * Client-side only. {@code Level#addParticle} is an empty method that {@code ServerLevel} never
	 * overrides - broadcasting is {@code ServerLevel#sendParticles} - so a server-side run would
	 * build five throwaway vectors and hand them to a no-op.
	 */
	default void crumbs() {
		int left = getEatTicks();
		if (!asYoukai().level().isClientSide() || left <= 0 || left % CRUMB_EVERY != 0) return;
		var food = getEatItem();
		if (food.isEmpty()) return;
		for (int i = 0; i < CRUMBS; i++) crumb(food);
	}

	/**
	 * One crumb at her mouth, thrown the way vanilla throws one: a small cone out of the eye
	 * position, which on these rigs is close enough to the mouth for the spread to read as coming
	 * from it rather than from her chest.
	 */
	default void crumb(ItemStack food) {
		var self = asYoukai();
		var random = self.getRandom();
		var out = new Vec3((random.nextFloat() - 0.5F) * 0.2f, -random.nextFloat() * 0.2 - 0.1, 0.6)
				.xRot(-self.getXRot() * ((float) Math.PI / 180F))
				.yRot(-self.getYRot() * ((float) Math.PI / 180F))
				.add(self.getEyePosition());
		var vel = new Vec3((random.nextFloat() - 0.5F) * 0.1, random.nextFloat() * 0.1 + 0.1, 0.0)
				.xRot(-self.getXRot() * ((float) Math.PI / 180F))
				.yRot(-self.getYRot() * ((float) Math.PI / 180F));
		self.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, food),
				out.x, out.y, out.z, vel.x, vel.y + 0.05, vel.z);
	}

	/**
	 * Registers the pair. A static hook rather than a module, because the module list is per-entity
	 * and these two accessors are the same pair on every character.
	 */
	static void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_EAT_ITEM, ItemStack.EMPTY);
		builder.define(DATA_EAT_TICKS, 0);
	}

}