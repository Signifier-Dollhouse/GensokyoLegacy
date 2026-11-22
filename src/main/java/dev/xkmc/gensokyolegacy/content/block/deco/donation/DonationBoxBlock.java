package dev.xkmc.gensokyolegacy.content.block.deco.donation;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.mult.*;
import dev.xkmc.l2modularblock.one.EntityInsideBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

/**
 * A donation box occupies the middle and two invisible side cells; only the middle holds the block
 * entity. Right-clicking any part inserts a coin, and a coin thrown onto it is swallowed by
 * {@link #entityInside}.
 *
 * @see "doc/design/structure/donation.md"
 */
public class DonationBoxBlock implements CreateBlockStateBlockMethod, DefaultStateBlockMethod, PlacementBlockMethod,
		ShapeUpdateBlockMethod, SetPlacedByBlockMethod, SurviveBlockMethod, PlayerDestoryBlockMethod,
		UseItemOnBlockMethod, EntityInsideBlockMethod {

	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(PART, Part.MIDDLE);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockState state, BlockPlaceContext ctx) {
		for (Part part : Part.values()) {
			if (part == Part.MIDDLE) continue;
			BlockPos side = sidePos(ctx.getClickedPos(), state.getValue(FACING), part);
			if (!ctx.getLevel().getBlockState(side).canBeReplaced(ctx)) return null;
		}
		return state;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (state.getValue(PART) != Part.MIDDLE) return;
		for (Part part : Part.values()) {
			if (part == Part.MIDDLE) continue;
			level.setBlock(sidePos(pos, state.getValue(FACING), part), state.setValue(PART, part),
					Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
		if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof DonationBoxBlockEntity be) {
			be.tryLink(sl);
		}
	}

	@Override
	public BlockState updateShape(Block self, BlockState current, BlockState old, Direction dir, BlockState nState,
								  LevelAccessor level, BlockPos pos, BlockPos nPos) {
		Part part = current.getValue(PART);
		if (part == Part.MIDDLE) {
			for (Part side : Part.values()) {
				if (side == Part.MIDDLE) continue;
				// only react to the cell that actually changed, so a template can place the
				// three cells in any order without the middle breaking on the missing side
				if (sideDir(current.getValue(FACING), side) != dir) continue;
				return matches(nState, current, side) ? current : Blocks.AIR.defaultBlockState();
			}
			return current;
		}
		if (sideDir(current.getValue(FACING), part).getOpposite() != dir) return current;
		return matches(nState, current, Part.MIDDLE) ? current : Blocks.AIR.defaultBlockState();
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(PART) == Part.MIDDLE) return true;
		return matches(level.getBlockState(middlePos(pos, state)), state, Part.MIDDLE);
	}

	@Override
	public @Nullable BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && player.isCreative()) {
			BlockPos mid = middlePos(pos, state);
			BlockState midState = level.getBlockState(mid);
			if (matches(midState, state, Part.MIDDLE)) {
				for (Part part : Part.values()) {
					BlockPos target = part == Part.MIDDLE ? mid : sidePos(mid, midState.getValue(FACING), part);
					if (target.equals(pos)) continue;
					BlockState found = level.getBlockState(target);
					if (matches(found, midState, part)) removeNoDrop(level, target, found);
				}
			}
		}
		return null;
	}

	@Override
	public boolean playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
								 @Nullable BlockEntity be, ItemStack stack) {
		return false;
	}

	/**
	 * Only the middle part carries the loot, so a survival break or an explosion drops at most one.
	 */
	public static void genLoot(RegistrateBlockLootTables pvd, DelegateBlock block) {
		var helper = new LootHelper(pvd);
		var pool = LootPool.lootPool().add(helper.item(block.asItem())
				.when(helper.enumState(block, PART, Part.MIDDLE)));
		pvd.add(block, LootTable.lootTable().withPool(pvd.applyExplosionCondition(block, pool)));
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
										   Player player, InteractionHand hand, BlockHitResult hit) {
		BlockPos mid = middlePos(pos, state);
		if (!(level.getBlockEntity(mid) instanceof DonationBoxBlockEntity be)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		int coins = DonationBoxBlockEntity.coinsOf(stack);
		if (coins <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (level.isClientSide) {
			return be.linked() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (player instanceof ServerPlayer sp && be.insert(sp, coins)) {
			stack.shrink(1);
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/**
	 * A coin item entity that lands on the box overlaps a part cell, so the whole stack is consumed
	 * here instead of being scanned for by a tick handler.
	 */
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide) return;
		if (!(entity instanceof ItemEntity item)) return;
		if (item.isRemoved()) return;
		if (!(item.getOwner() instanceof ServerPlayer player)) return;
		int coins = DonationBoxBlockEntity.coinsOf(item.getItem());
		if (coins <= 0) return;
		BlockPos mid = middlePos(pos, state);
		if (!(level.getBlockEntity(mid) instanceof DonationBoxBlockEntity be)) return;
		if (be.insert(player, coins * item.getItem().getCount())) {
			item.discard();
		}
	}

	public static BlockPos middlePos(BlockPos pos, BlockState state) {
		Part part = state.getValue(PART);
		if (part == Part.MIDDLE) return pos;
		return pos.relative(sideDir(state.getValue(FACING), part).getOpposite());
	}

	public static BlockPos sidePos(BlockPos middle, Direction facing, Part part) {
		return middle.relative(sideDir(facing, part));
	}

	private static Direction sideDir(Direction facing, Part part) {
		return part == Part.LEFT ? facing.getCounterClockWise() : facing.getClockWise();
	}

	private static boolean matches(BlockState state, BlockState ref, Part part) {
		return state.is(ref.getBlock()) && state.getValue(PART) == part &&
				state.getValue(FACING) == ref.getValue(FACING);
	}

	private static void removeNoDrop(Level level, BlockPos pos, BlockState state) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		level.levelEvent(2001, pos, Block.getId(state));
	}

	public enum Part implements StringRepresentable {
		LEFT, MIDDLE, RIGHT;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

}
