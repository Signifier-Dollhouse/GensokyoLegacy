package dev.xkmc.gensokyolegacy.content.item.treasure;

import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualItem;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Charges for a second, then on release turns the aimed stone and everything connected to it into
 * that gem's ore, one gem per block. Reusable: the pool and the gems are the price, the tower itself
 * never wears out.
 *
 * @see "doc/design/tower.md"
 */
public class TreasureTower extends SpiritualItem {

	public static final int MAX_POWER = 1000;
	public static final int COST = 100;
	public static final int COOLDOWN = 10;
	/**
	 * The use duration is the wind-up: the action fires when it runs out, so an early release does
	 * nothing.
	 */
	public static final int USE_DURATION = 20;
	/**
	 * The item's particle, drawn along the beam from the eye to the aimed block.
	 */
	public static final SimpleParticleType PARTICLE = ParticleTypes.HAPPY_VILLAGER;

	private static final double BEAM_STEP = 0.1;
	private static final double BEAM_SPREAD = 0.1;
	/**
	 * Beam reach: far past the arm's own range.
	 */
	private static final double REACH = 24;

	public TreasureTower(Properties properties) {
		super(properties.stacksTo(1), MAX_POWER);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, context, list, flag);
		list.add(GLLang.ItemTreasureTower.TOOLTIP.get());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_DURATION;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.SPEAR;
	}

	/**
	 * The wind-up completed: for a use this short the game finishes the use at the last tick rather
	 * than calling {@link #releaseUsing}, so both paths end here.
	 */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof Player player) fire(player, stack, level);
		return stack;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) return;
		if (getUseDuration(stack, entity) - timeLeft < USE_DURATION) return;
		fire(player, stack, level);
	}

	private void fire(Player player, ItemStack stack, Level level) {
		if (level.isClientSide) return;
		if (player.getCooldowns().isOnCooldown(this)) return;
		if (!(player.pick(REACH, 1.0F, false) instanceof BlockHitResult hit)
				|| hit.getType() != HitResult.Type.BLOCK) return;

		BlockPos target = hit.getBlockPos();
		TreasureTowerData data = TreasureTowerManager.get(level.getServer());
		ItemStack gems = pickGem(player, level.getBlockState(target), data);
		if (gems.isEmpty()) return;
		if (SpiritualPower.get(stack) < COST) {
			player.displayClientMessage(GLLang.ItemTreasureTower.NEED_POWER.get(COST), true);
			return;
		}

		Item gem = gems.getItem();
		List<BlockPos> found = search(level, target, gem, data, gems.getCount());
		if (found.isEmpty()) return;

		if (!player.hasInfiniteMaterials()) gems.shrink(found.size());
		SpiritualPower.add(stack, -COST);
		for (BlockPos pos : found) {
			Block ore = data.oreFor(level.getBlockState(pos), gem);
			if (ore != null) level.setBlockAndUpdate(pos, ore.defaultBlockState());
		}
		if (level instanceof ServerLevel sl) beam(sl, player, Vec3.atCenterOf(target));
		level.playSound(null, target, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
		player.swing(player.getUsedItemHand());
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, COOLDOWN);
	}

	/**
	 * The gem to spend: the offhand first, then the main hand, then the inventory in slot order. The
	 * stack must map to an ore for this very stone, so a diamond and an emerald in the pack still
	 * do the right thing. Only this one stack pays, so its count is also how far the search may go.
	 */
	private static ItemStack pickGem(Player player, BlockState state, TreasureTowerData data) {
		ItemStack off = player.getOffhandItem();
		if (!off.isEmpty() && data.oreFor(state, off) != null) return off;
		ItemStack main = player.getMainHandItem();
		if (!main.isEmpty() && data.oreFor(state, main) != null) return main;
		for (ItemStack stack : player.getInventory().items)
			if (!stack.isEmpty() && data.oreFor(state, stack) != null) return stack;
		return ItemStack.EMPTY;
	}

	/**
	 * The connected run of blocks this gem converts, nearest first, at most {@code limit} of them.
	 */
	private static List<BlockPos> search(Level level, BlockPos from, Item gem, TreasureTowerData data, int limit) {
		List<BlockPos> ans = new ArrayList<>();
		if (data.oreFor(level.getBlockState(from), gem) == null) return ans;
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(from);
		queue.add(from);
		while (!queue.isEmpty() && ans.size() < limit) {
			BlockPos pos = queue.poll();
			ans.add(pos);
			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if (seen.contains(next) || !level.isLoaded(next)) continue;
				if (data.oreFor(level.getBlockState(next), gem) == null) continue;
				seen.add(next);
				queue.add(next);
			}
		}
		return ans;
	}

	/**
	 * The beam from the eye to the target, ten particles per block of distance with a small spread.
	 */
	private static void beam(ServerLevel level, Player player, Vec3 to) {
		Vec3 from = player.getEyePosition();
		int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) / BEAM_STEP));
		RandomSource random = level.getRandom();
		for (int i = 0; i <= steps; i++) {
			Vec3 at = from.lerp(to, (double) i / steps);
			level.sendParticles(PARTICLE,
					at.x + spread(random), at.y + spread(random), at.z + spread(random),
					1, 0, 0, 0, 0);
		}
	}

	private static double spread(RandomSource random) {
		return (random.nextDouble() * 2 - 1) * BEAM_SPREAD;
	}

}
