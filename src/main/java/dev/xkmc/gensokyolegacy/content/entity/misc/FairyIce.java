package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.CirnoEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * A fairy ice crystal, thrown. It bursts where it lands and the burst is cold
 * enough to matter: it hurts and freezes whatever is standing in it, puts out
 * anything burning within {@link #FREEZE_RADIUS}, and turns the water and lava
 * around it into something solid.
 * <p>
 * The crystal itself is fireproof - {@link #fireImmune()} is simply on, which is
 * what vanilla reads to decide that a fire damage source cannot hurt it and that
 * {@code lavaHurt} never ignites it. A crystal is the one thing in the game that
 * a player might reasonably want to throw into a fire, and a projectile that
 * vanishes halfway through the flight is not a tool. It also means fire never
 * gets to be the thing that decides where the burst lands: the burst is always
 * somewhere the thrower aimed it, or - see {@link #tick} - somewhere it touched
 * lava.
 * <p>
 * That last case is the reason there is a {@link #tick} override at all. Vanilla
 * traces a throwable's flight path against block colliders only, with
 * {@code ClipContext.Fluid.NONE}, so lava is transparent to it: a crystal thrown
 * across a lava lake sails straight over the lava and only bursts when it hits
 * the stone at the bottom, which is both the wrong place and several seconds too
 * late. So the fluid at the crystal's own position is checked every tick, and
 * touching lava triggers the same burst immediately. That is what makes the lava
 * rule on this thing meaningful - a crystal dropped into a lava pool is the one
 * case where you <em>want</em> the throwaway to cool the pool instead of dying
 * in it.
 */
public class FairyIce extends ThrowableItemProjectile {

	public static final double SPLASH_RANGE = 6.0D;
	private static final double SPLASH_RANGE_SQ = 36.0D;

	/**
	 * How far the cold reaches when a crystal bursts, in blocks. Six, where the
	 * splash is four: the freezing is the part that changes the ground rather than
	 * the part that hits somebody, so it is deliberately the larger of the two - it
	 * is a poured-and-frozen pond and a quenched campfire that a burst is for, not
	 * a weapon.
	 */
	public static final int FREEZE_RADIUS = 6;
	private static final int FREEZE_RADIUS_SQ = FREEZE_RADIUS * FREEZE_RADIUS;

	public FairyIce(EntityType<? extends FairyIce> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public FairyIce(Level pLevel, LivingEntity pShooter) {
		super(GLEntities.FAIRY_ICE.get(), pShooter, pLevel);
	}

	public FairyIce(Level pLevel, double pX, double pY, double pZ) {
		super(GLEntities.FAIRY_ICE.get(), pX, pY, pZ, pLevel);
	}

	protected Item getDefaultItem() {
		return GLItems.FAIRY_ICE_CRYSTAL.get();
	}

	/**
	 * On, and for all three reasons at once: {@code Entity#isInvulnerableTo} returns
	 * true for any {@code IS_FIRE} damage source, {@code Entity#lavaHurt} never
	 * ignites the crystal and never lands a lava tick of damage on it, and
	 * {@code Entity#baseTick} drains the fire ticks four a tick, so even a flame
	 * arrow that connects with it is out before the next one would have been drawn.
	 */
	@Override
	public boolean fireImmune() {
		return true;
	}

	/**
	 * Lava contact, checked against the position the crystal has just moved to.
	 * After {@code super.tick()} rather than before, so the position is the one it
	 * actually reached this tick and not the one it left; guarded on
	 * {@link #isRemoved()} because a crystal that landed on the same tick has
	 * already burst through {@link #onHit} and must not burst twice.
	 */
	@Override
	public void tick() {
		super.tick();
		if (isRemoved() || level().isClientSide) return;
		if (!level().getFluidState(blockPosition()).isEmpty()) burst();
	}

	protected void onHit(HitResult pResult) {
		super.onHit(pResult);
		burst();
	}

	/**
	 * Everything the burst does, in one place, so that landing on something and
	 * touching lava cannot drift apart: the same splash, the same level event, the
	 * same freeze, the same disappearance.
	 */
	private void burst() {
		areaDamage();
		freezeAround();
		level().levelEvent(2002, blockPosition(), 0xFFCFDDF4);
		discard();
	}

	private void areaDamage() {
		AABB aabb = getBoundingBox().inflate(SPLASH_RANGE, 2.0D, SPLASH_RANGE);
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, aabb)) {
			if (e instanceof CirnoEntity) continue;
			double d0 = distanceToSqr(e);
			if (d0 >= SPLASH_RANGE_SQ) continue;
			float dmg = e.isSensitiveToWater() ? 18 : 9;
			e.hurt(damageSources().indirectMagic(this, getOwner()), dmg);
			if (e.isOnFire() && e.isAlive()) {
				e.extinguishFire();
			}
			if (e.canFreeze()) {
				e.setTicksFrozen(360);
			}
		}
	}

	/**
	 * The cold, over every block in a sphere rather than a box: a square six would
	 * put the same burst eleven blocks away on the diagonal, which is further than
	 * the stated six and reads as the effect being bigger than the number.
	 * <p>
	 * Deliberately a plain scan of the volume rather than a shape or a block
	 * entity: it is roughly nine hundred block reads on a throw, it runs once
	 * because the crystal is gone afterwards, and every one of them is a lookup
	 * that comes back empty on the great majority of throws into open air.
	 */
	private void freezeAround() {
		BlockPos origin = blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-FREEZE_RADIUS, -FREEZE_RADIUS, -FREEZE_RADIUS), origin.offset(FREEZE_RADIUS, FREEZE_RADIUS, FREEZE_RADIUS))) {
			if (pos.distSqr(origin) <= FREEZE_RADIUS_SQ) freezeAt(pos);
		}
	}

	/**
	 * One block's worth of the burst. A fluid is frozen, anything else burning is
	 * put out, and everything else is left alone - so the two never fight over the
	 * same position and there is no ordering to get wrong.
	 */
	private void freezeAt(BlockPos pos) {
		var fluid = level().getFluidState(pos);
		if (fluid.isEmpty()) {
			dowseFire(pos);
			return;
		}
		if (fluid.is(FluidTags.WATER) && isOpenTop(pos)) {
			level().setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
			return;
		}
		if (fluid.is(FluidTags.LAVA) && isOpenTop(pos)) {
			level().setBlockAndUpdate(pos, fluid.isSource()
					? Blocks.OBSIDIAN.defaultBlockState()
					: Blocks.COBBLESTONE.defaultBlockState());
		}
	}

	/**
	 * "Top layer" is a block of air above and nothing else. Not "see the sky", not
	 * "replaceable": a single slab over the lava is enough to turn it into
	 * cobblestone, which is what stops a crystal from drilling a hole through
	 * somebody's floor and filling it with obsidian.
	 */
	private boolean isOpenTop(BlockPos pos) {
		return level().getBlockState(pos.above()).isAir();
	}

	private void dowseFire(BlockPos pos) {
		BlockState state = level().getBlockState(pos);
		if (state.is(BlockTags.FIRE)) {
			level().removeBlock(pos, false);
		} else if (AbstractCandleBlock.isLit(state)) {
			AbstractCandleBlock.extinguish(null, state, level(), pos);
		} else if (CampfireBlock.isLitCampfire(state)) {
			level().levelEvent(null, 1009, pos, 0);
			CampfireBlock.dowse(getOwner(), level(), pos, state);
			level().setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, Boolean.FALSE));
		}
	}

}
