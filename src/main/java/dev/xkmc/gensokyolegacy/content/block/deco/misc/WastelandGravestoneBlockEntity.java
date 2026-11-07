package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import dev.xkmc.gensokyolegacy.content.block.functional.plate.PlateBlockEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPowerHolder;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostVariant;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.tile_api.TickableBlockEntity;
import dev.xkmc.l2modularblock.type.BlockMethod;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The one food item the gravestone serves. Storage and pickup are the plate's; the difference is
 * that food handing back a container is allowed, since the gravestone displays instead of eating.
 *
 * @see "doc/design/gravestone.md"
 */
@SerialClass
public class WastelandGravestoneBlockEntity extends PlateBlockEntity implements TickableBlockEntity {

	/**
	 * Five seconds of end rod while the offering is consumed.
	 */
	public static final int RITUAL_TICKS = 100;

	public static final BlockMethod BE = new BlockEntityBlockMethodImpl<WastelandGravestoneBlockEntity>(
			GLFurniture.WASTELAND_GRAVESTONE_BE, WastelandGravestoneBlockEntity.class) {
		@Override
		public @Nullable WastelandGravestoneBlockEntity createTileEntity(BlockPos pos, BlockState state) {
			return state.getValue(WastelandGravestoneBlock.HALF) == Half.TOP ? null : super.createTileEntity(pos, state);
		}
	};

	@SerialField
	private int ritual;

	public WastelandGravestoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public boolean busy() {
		return ritual > 0;
	}

	public void startRitual() {
		ritual = RITUAL_TICKS;
		notifyTile();
	}

	@Override
	public void tick() {
		if (ritual <= 0 || level == null) return;
		if (level.isClientSide) {
			ritual--;
			spawnParticle();
			return;
		}
		if (--ritual > 0) return;
		finish();
	}

	private void spawnParticle() {
		if (level == null) return;
		if (level.getGameTime() % 2 != 0) return;
		RandomSource random = level.getRandom();
		var center = getBlockPos().getCenter().add(0, 1, 0);
		var dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
		var p = center.add(dir.scale(0.4));
		dir = dir.scale(-0.05);
		level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, dir.x, dir.y, dir.z);
	}

	private void finish() {
		ItemStack food = stack;
		if (food.isEmpty()) {
			notifyTile();
			return;
		}
		FoodProperties props = food.get(DataComponents.FOOD);
		GhostVariant variant = props == null ? GhostVariant.CURIOS : roll(props, level.getRandom());
		stack = props == null ? ItemStack.EMPTY
				: props.usingConvertsTo().map(ItemStack::copy).orElse(ItemStack.EMPTY);
		notifyTile();
		if (level instanceof ServerLevel sl) summon(sl, variant);
	}

	private void summon(ServerLevel level, GhostVariant variant) {
		BlockPos pos = getBlockPos();
		StrayGhostEntity ghost = new StrayGhostEntity(GLEntities.STRAY_GHOST.get(), level);
		ghost.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
		ghost.setVariant(variant);
		ghost.setPower(GhostPowerHolder.wildPower(level.getRandom()));
		ghost.setPersistenceRequired();
		level.addFreshEntity(ghost);
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0,
				pos.getZ() + 0.5, 24, 0.4, 0.6, 0.4, 0.02);
	}

	private static GhostVariant roll(FoodProperties props, RandomSource random) {
		int foodie = props.nutrition() * 5;
		int lively = beneficialEffects(props) * 10;
		int total = foodie + lively;
		if (total > 100) {
			return random.nextInt(total) < foodie ? GhostVariant.FOODIE : GhostVariant.LIVELY;
		}
		int pick = random.nextInt(100);
		if (pick < foodie) return GhostVariant.FOODIE;
		if (pick < total) return GhostVariant.LIVELY;
		return GhostVariant.CURIOS;
	}

	private static int beneficialEffects(FoodProperties props) {
		int count = 0;
		for (FoodProperties.PossibleEffect effect : props.effects()) {
			var holder = effect.effect().getEffect();
			if (holder != null && holder.value().isBeneficial()) count++;
		}
		return count;
	}

}
