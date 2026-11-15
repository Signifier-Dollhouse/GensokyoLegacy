package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.content.item.treasure.TreasureTower;
import dev.xkmc.gensokyolegacy.content.item.treasure.TreasureTowerData;
import dev.xkmc.gensokyolegacy.content.item.treasure.TreasureTowerManager;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The tower while it is away from the hand: it flies out, beams the aimed stone, and returns.
 * A plain projectile rather than a living entity, since the owner, the stack and the spot suffice.
 *
 * @see "doc/design/item/tower.md"
 */
public class TreasureTowerEntity extends ThrowableItemProjectile {

	private enum Phase {
		OUT, CAST, RETURN
	}

	private static final double FLY_SPEED = 0.45;
	private static final double RETURN_SPEED = 0.6;
	private static final double OUT_FORWARD = 4;
	private static final double OUT_UP = 2;
	private static final double MAX_OWNER_DIST = 32;
	private static final int CAST_TIME = 10;
	private static final double BEAM_STEP = 0.25;
	private static final double BEAM_SPREAD = 0.1;
	private static final SimpleParticleType PARTICLE = ParticleTypes.HAPPY_VILLAGER;

	private Phase phase = Phase.OUT;
	private int castTicks;
	private Vec3 target = Vec3.ZERO;
	private BlockPos targetBlock = BlockPos.ZERO;
	private InteractionHand hand = InteractionHand.MAIN_HAND;
	private int slot;

	public TreasureTowerEntity(EntityType<? extends TreasureTowerEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public TreasureTowerEntity(Level level, Player owner) {
		super(GLEntities.TREASURE_TOWER.get(), owner, level);
		noPhysics = true;
	}

	/**
	 * Sends the tower out and takes the stack out of the given hand; {@link #deliver} puts it back.
	 */
	public static void launch(Level level, Player player, InteractionHand hand, ItemStack stack, BlockPos targetBlock) {
		TreasureTowerEntity entity = new TreasureTowerEntity(level, player);
		Vec3 look = flatLook(player);
		entity.target = player.position().add(look.scale(OUT_FORWARD)).add(0, OUT_UP, 0);
		entity.targetBlock = targetBlock;
		entity.hand = hand;
		entity.slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
		entity.setItem(stack);
		entity.setPos(player.getEyePosition().add(player.getLookAngle().scale(0.5)).add(0, -0.25, 0));
		level.addFreshEntity(entity);
	}

	/**
	 * The stack that pays: the offhand first, then the main hand, then the inventory in slot order.
	 * The stack must map to an ore for this very stone, and only that one stack pays.
	 */
	public static ItemStack pickGem(Player player, BlockState state, TreasureTowerData data) {
		ItemStack off = player.getOffhandItem();
		if (!off.isEmpty() && data.oreFor(state, off) != null) return off;
		ItemStack main = player.getMainHandItem();
		if (!main.isEmpty() && data.oreFor(state, main) != null) return main;
		for (ItemStack stack : player.getInventory().items)
			if (!stack.isEmpty() && data.oreFor(state, stack) != null) return stack;
		return ItemStack.EMPTY;
	}

	@Override
	protected Item getDefaultItem() {
		return GLItems.TREASURE_TOWER.get();
	}

	@Override
	public void tick() {
		baseTick();
		if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
		if (!(getOwner() instanceof Player owner) || owner.isRemoved() || owner.isDeadOrDying()
				|| owner.level() != level() || distanceToSqr(owner) > MAX_OWNER_DIST * MAX_OWNER_DIST) {
			dropAndDiscard();
			return;
		}
		switch (phase) {
			case OUT -> {
				if (moveTowards(target, FLY_SPEED)) {
					phase = Phase.CAST;
					castTicks = CAST_TIME;
					cast(server, owner);
				}
			}
			case CAST -> {
				beam(server);
				if (--castTicks <= 0) phase = Phase.RETURN;
			}
			case RETURN -> {
				if (moveTowards(handPos(owner), RETURN_SPEED)) deliver(owner);
			}
		}
	}

	private boolean moveTowards(Vec3 to, double speed) {
		Vec3 diff = to.subtract(position());
		double dist = diff.length();
		if (dist <= speed) {
			setPos(to.x, to.y, to.z);
			return true;
		}
		setPos(position().add(diff.scale(speed / dist)));
		return false;
	}

	private void cast(ServerLevel level, Player player) {
		TreasureTowerData data = TreasureTowerManager.get(level.getServer());
		ItemStack gems = pickGem(player, level.getBlockState(targetBlock), data);
		if (gems.isEmpty()) return;
		Item gem = gems.getItem();
		List<BlockPos> found = search(level, targetBlock, gem, data, gems.getCount());
		if (found.isEmpty()) return;
		ItemStack stack = getItem();
		if (SpiritualPower.get(stack) < TreasureTower.COST) {
			player.displayClientMessage(GLLang.ItemTreasureTower.NEED_POWER.get(TreasureTower.COST), true);
			return;
		}
		if (!player.hasInfiniteMaterials()) gems.shrink(found.size());
		SpiritualPower.add(stack, -TreasureTower.COST);
		setItem(stack);
		for (BlockPos pos : found) {
			Block ore = data.oreFor(level.getBlockState(pos), gem);
			if (ore != null) level.setBlockAndUpdate(pos, ore.defaultBlockState());
		}
		beam(level);
		level.playSound(null, targetBlock, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
		player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		player.swing(hand);
	}

	private void deliver(Player player) {
		ItemStack stack = getItem();
		if (!stack.isEmpty()) {
			if (player.getInventory().getItem(slot).isEmpty()) player.getInventory().setItem(slot, stack);
			else if (!player.getInventory().add(stack)) player.drop(stack, false);
		}
		discard();
	}

	private void dropAndDiscard() {
		ItemStack stack = getItem();
		if (!stack.isEmpty()) spawnAtLocation(stack);
		discard();
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

	private static Vec3 flatLook(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1e-6 ? Vec3.directionFromRotation(0, player.getYRot()) : flat.normalize();
	}

	private static Vec3 handPos(Player player) {
		return player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.35, 0);
	}

	/**
	 * The beam from the tower to the aimed block, four particles per block of distance.
	 */
	private void beam(ServerLevel level) {
		Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
		Vec3 to = Vec3.atCenterOf(targetBlock);
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

	@Override
	public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte("Phase", (byte) phase.ordinal());
		tag.putInt("CastTicks", castTicks);
		tag.putDouble("TargetX", target.x);
		tag.putDouble("TargetY", target.y);
		tag.putDouble("TargetZ", target.z);
		tag.putLong("TargetBlock", targetBlock.asLong());
		tag.putByte("Hand", (byte) hand.ordinal());
		tag.putInt("Slot", slot);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		phase = Phase.values()[Mth.clamp(tag.getByte("Phase"), 0, Phase.values().length - 1)];
		castTicks = tag.getInt("CastTicks");
		target = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
		targetBlock = BlockPos.of(tag.getLong("TargetBlock"));
		hand = InteractionHand.values()[Mth.clamp(tag.getByte("Hand"), 0, InteractionHand.values().length - 1)];
		slot = tag.getInt("Slot");
	}

}
