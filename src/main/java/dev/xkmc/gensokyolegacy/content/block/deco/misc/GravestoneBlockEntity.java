package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import dev.xkmc.gensokyolegacy.content.attachment.misc.ChunkMiscDataAttachment;
import dev.xkmc.gensokyolegacy.content.block.functional.plate.PlateBlockEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPowerHolder;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostVariant;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLCriteriaTriggers;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.tile_api.TickableBlockEntity;
import dev.xkmc.l2modularblock.type.BlockMethod;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The one food item the gravestone serves. Storage and pickup are the plate's; the difference is
 * that food handing back a container is allowed, since the gravestone displays instead of eating.
 *
 * @see "doc/design/gravestone.md"
 */
@SerialClass
public class GravestoneBlockEntity extends PlateBlockEntity implements TickableBlockEntity {

	/**
	 * Five seconds of end rod while the offering is consumed.
	 */
	public static final int RITUAL_TICKS = ChunkMiscDataAttachment.SUMMON_LOCK;

	/**
	 * The hidden saturation the offering must be worth: nutrition x modifier x 2, the value the
	 * hunger bar restores.
	 */
	public static final float MIN_SATURATION = 5;

	public static final BlockMethod BE = new BlockEntityBlockMethodImpl<GravestoneBlockEntity>(
			GLFurniture.GRAVESTONE_BE, GravestoneBlockEntity.class) {
		@Override
		public @Nullable GravestoneBlockEntity createTileEntity(BlockPos pos, BlockState state) {
			return state.getValue(GravestoneBlock.HALF) == Half.TOP ? null : super.createTileEntity(pos, state);
		}
	};

	@SerialField
	private int ritual;

	/**
	 * Who started the ritual, so the advancement fires for them when the ghost appears.
	 */
	@SerialField
	private UUID summoner = Util.NIL_UUID;

	public GravestoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public boolean busy() {
		return ritual > 0;
	}

	/**
	 * A worthy offering is worth at least {@link #MIN_SATURATION} saturation and carries no
	 * harmful effect.
	 */
	public static boolean isOffering(@Nullable FoodProperties props) {
		if (props == null || props.getNutrition() * props.getSaturationModifier() * 2 < MIN_SATURATION) return false;
		for (var effect : props.getEffects()) {
			if (effect.getFirst().getEffect().getCategory() == MobEffectCategory.HARMFUL) return false;
		}
		return true;
	}

	public void startRitual(Player player) {
		ritual = RITUAL_TICKS;
		summoner = player.getUUID();
		if (level instanceof ServerLevel sl)
			ChunkMiscDataAttachment.of(sl.getChunkAt(getBlockPos())).startSummon(sl.getGameTime());
		notifyTile();
	}

	public static boolean canSummon(ServerLevel level, BlockPos pos) {
		return ChunkMiscDataAttachment.of(level.getChunkAt(pos)).canSummon(level.getGameTime());
	}

	/**
	 * Taking the offering back during the ritual halts it; the chunk keeps only the start lock.
	 */
	@Override
	public void take(Player player) {
		// super.take syncs the tile, so the halted ritual reaches the client with the empty stack
		ritual = 0;
		summoner = Util.NIL_UUID;
		super.take(player);
	}

	private void halt() {
		if (ritual <= 0) return;
		ritual = 0;
		notifyTile();
	}

	@Override
	public void tick() {
		if (ritual <= 0 || level == null) return;
		if (stack.isEmpty()) {
			halt();
			return;
		}
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
		FoodProperties props = food.getItem().getFoodProperties();
		GhostVariant variant = props == null ? GhostVariant.CURIOS : roll(props, level.getRandom());
		Item convert = food.getItem().getCraftingRemainingItem();
		stack = props == null || convert == null ? ItemStack.EMPTY : new ItemStack(convert);
		notifyTile();
		if (level instanceof ServerLevel sl) {
			summon(sl, variant);
			var sp = sl.getServer().getPlayerList().getPlayer(summoner);
			if (sp != null)
				GLCriteriaTriggers.GRAVESTONE_SUMMON.get().trigger(sp);
			summoner = Util.NIL_UUID;
		}
	}

	private void summon(ServerLevel level, GhostVariant variant) {
		BlockPos pos = getBlockPos();
		StrayGhostEntity ghost = new StrayGhostEntity(GLEntities.STRAY_GHOST.get(), level);
		ghost.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
		ghost.setVariant(variant);
		ghost.setPower(GhostPowerHolder.wildPower(level.getRandom()));
		ghost.setPersistenceRequired();
		level.addFreshEntity(ghost);
		ChunkMiscDataAttachment.of(level.getChunkAt(pos)).finishSummon(level.getGameTime());
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0,
				pos.getZ() + 0.5, 24, 0.4, 0.6, 0.4, 0.02);
	}

	private static GhostVariant roll(FoodProperties props, RandomSource random) {
		int foodie = props.getNutrition() * 5;
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
		for (var effect : props.getEffects()) {
			if (effect.getFirst().getEffect().isBeneficial()) count++;
		}
		return count;
	}

}
