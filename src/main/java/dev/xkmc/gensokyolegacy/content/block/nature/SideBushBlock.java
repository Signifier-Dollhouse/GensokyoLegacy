package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SideBushBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final MapCodec<SideBushBlock> CODEC = simpleCodec(SideBushBlock::new);

	private static final VoxelShape[] SHAPES = new VoxelShape[4];

	static {
		var shape = new VoxelBuilder(1, 6, 11, 14, 11, 16);
		for (int i = 0; i < 4; i++) {
			SHAPES[i] = shape.rotateFromNorth(Direction.from2DDataValue(i));
		}
	}

	private final @Nullable ItemLike drop;

	public SideBushBlock(Properties properties) {
		this(properties, null);
	}

	public SideBushBlock(Properties properties, ItemLike drop) {
		super(properties);
		this.drop = drop;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		for (Direction dir : ctx.getNearestLookingDirections()) {
			if (dir.getAxis() == Direction.Axis.Y) continue;
			BlockPos supportPos = ctx.getClickedPos().relative(dir);
			BlockState supportState = ctx.getLevel().getBlockState(supportPos);
			if (supportState.isFaceSturdy(ctx.getLevel(), supportPos, dir.getOpposite())) {
				return this.defaultBlockState().setValue(FACING, dir.getOpposite());
			}
		}
		return null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING).getOpposite();
		BlockPos supportPos = pos.relative(facing);
		BlockState supportState = level.getBlockState(supportPos);
		return supportState.isFaceSturdy(level, supportPos, facing.getOpposite());
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos) {
		return state.canSurvive(level, pos) ? super.updateShape(state, facing, facingState, level, pos, facingPos)
				: Blocks.AIR.defaultBlockState();
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(FACING).get2DDataValue()];
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return state.getFluidState().isEmpty();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return pathComputationType == PathComputationType.AIR && !this.hasCollision || super.isPathfindable(state, pathComputationType);
	}

	public static void loot(RegistrateBlockLootTables tb, Block block) {
		var shearsOrSilk = MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS))
				.or(MatchTool.toolMatches(ItemPredicate.Builder.item()
						.withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
								ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
										tb.getRegistries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
										MinMaxBounds.Ints.atLeast(1)))))));
		SideBushBlock bush = (SideBushBlock) block;
		tb.add(block, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shearsOrSilk)
						.add(LootItem.lootTableItem(block)))
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shearsOrSilk.invert())
						.add(LootItem.lootTableItem(bush.drop == null ? block.asItem() : bush.drop))));
	}

}