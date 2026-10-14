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
 * A fairy ice crystal, thrown. It bursts where it lands and the burst is cold enough to matter: it
 * hurts and freezes whatever is standing in it, puts out anything burning within {@link #FREEZE_RADIUS},
 * and turns the water and lava around it solid; {@link #fireImmune()} keeps fire from deciding where the burst lands.
 */
public class FairyIce extends ThrowableItemProjectile {

	public static final double SPLASH_RANGE = 6.0D;
	private static final double SPLASH_RANGE_SQ = 36.0D;

	/**
	 * How far the cold reaches when a crystal bursts, in blocks. Six, where the splash is four: the
	 * freezing changes the ground rather than hits somebody, so a burst is for a poured-and-frozen
	 * pond and a quenched campfire, not a weapon.
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
	 * On, and for all three reasons at once: {@code isInvulnerableTo} is true for any {@code IS_FIRE}
	 * source, {@code lavaHurt} never ignites it or ticks damage on it, and {@code baseTick} drains the
	 * fire ticks four a tick, so a connecting flame arrow is out before the next could be drawn.
	 */
	@Override
	public boolean fireImmune() {
		return true;
	}

	/**
	 * Lava contact, checked after {@code super.tick()} — the position actually reached. Vanilla
	 * traces a throwable against block colliders only ({@code ClipContext.Fluid.NONE}), so lava is
	 * transparent. Guarded on {@link #isRemoved()}: same-tick landings already burst via {@link #onHit}.
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
	 * Everything the burst does, in one place, so landing and lava contact cannot drift apart: the
	 * same splash, the same level event, the same freeze, the same disappearance.
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
	 * The cold, over every block in a sphere rather than a box: a square six would put the same burst
	 * eleven blocks away on the diagonal, further than the stated six. A plain volume scan is enough —
	 * roughly nine hundred block reads on a throw, once, most of them empty in open air.
	 */
	private void freezeAround() {
		BlockPos origin = blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-FREEZE_RADIUS, -FREEZE_RADIUS, -FREEZE_RADIUS), origin.offset(FREEZE_RADIUS, FREEZE_RADIUS, FREEZE_RADIUS))) {
			if (pos.distSqr(origin) <= FREEZE_RADIUS_SQ) freezeAt(pos);
		}
	}

	/**
	 * One block's worth of the burst. A fluid is frozen, anything else burning is put out, everything
	 * else is left alone — so the two never fight over the same position.
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
	 * "Top layer" is a block of air above and nothing else — not "see the sky", not "replaceable".
	 * That is what stops a crystal from drilling a hole through somebody's floor and filling it
	 * with obsidian.
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
