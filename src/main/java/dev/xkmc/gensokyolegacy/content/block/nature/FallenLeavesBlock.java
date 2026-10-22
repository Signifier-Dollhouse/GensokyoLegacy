package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One carpet of fallen leaves under a tree: a thin layer of leaves, which packs into a dense carpet when
 * right-clicked with itself.
 *
 * @see "doc/design/fallen_leaves.md"
 */
public class FallenLeavesBlock extends SimpleBushBlock {

	public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 2);
	/**
	 * A carpet piled up thick, the leaves lying on each other.
	 */
	public static final int DENSE = 1;
	/**
	 * A carpet of a single thin layer of leaves.
	 */
	public static final int SHALLOW = 2;
	/**
	 * Only horizontal, so a patch of leaves can be spun per block instead of tiling one orientation.
	 */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final MapCodec<FallenLeavesBlock> CODEC = simpleCodec(FallenLeavesBlock::new);
	/**
	 * How far below the tree the search for the ground reaches.
	 */
	private static final int SEARCH_DEPTH = 8;
	/**
	 * The chance that a dense carpet of another species is taken over.
	 */
	private static final float TAKEOVER_CHANCE = 0.3F;

	public FallenLeavesBlock(Properties properties) {
		super(properties, LAYER);
		this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1).setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LAYERS, FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(LAYERS, 2)
				.setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult result) {
		if (level.isClientSide) return ItemInteractionResult.CONSUME;
		if (itemStack.getItem().equals(blockState.getBlock().asItem()) && blockState.getValue(LAYERS) == 2) {
			level.setBlockAndUpdate(pos, blockState.setValue(LAYERS, 1));
			if (!player.isCreative()) itemStack.shrink(1);
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/**
	 * Let one leaf of {@code kind} fall into the carpet under a tree. The search goes down at most eight
	 * blocks for the ground; a carpet met on the way grows instead of a new one starting.
	 */
	public static void accumulate(ServerLevel level, BlockPos pos, FallenLeafKind kind, RandomSource random) {
		for (int i = 1; i <= SEARCH_DEPTH; i++) {
			BlockPos below = pos.below(i);
			BlockState state = level.getBlockState(below);
			if (state.getBlock() instanceof FallenLeavesBlock carpet) {
				carpet.accumulate(level, below, state, kind, random);
				return;
			}
			if (state.isFaceSturdy(level, below, Direction.UP)) {
				// the ground: the carpet lies right on top of it
				BlockPos spot = below.above();
				if (level.getBlockState(spot).canBeReplaced()) {
					place(level, spot, kind, random);
				}
				return;
			}
			if (!isPassable(state)) return;
		}
	}

	/**
	 * A leaf falls onto this carpet: it piles up denser, takes it over, or mixes into it.
	 */
	private void accumulate(ServerLevel level, BlockPos pos, BlockState state, FallenLeafKind kind, RandomSource random) {
		int layers = state.getValue(LAYERS);
		FallenLeafKind found = FallenLeafKind.of(this);
		if (found == kind) {
			// the same carpet piles up: a thin layer of leaves becomes a dense one
			if (layers == SHALLOW) {
				level.setBlock(pos, state.setValue(LAYERS, DENSE), Block.UPDATE_ALL);
			}
			return;
		}
		if (FallenLeafKind.mixes(found, kind)) {
			// two maples of different colours share one carpet
			BlockState mixed = FallenLeafKind.MIXED_MAPLE.fallenLeaves().defaultBlockState()
					.setValue(FACING, state.getValue(FACING));
			if (found == FallenLeafKind.MIXED_MAPLE) {
				// the carpet is already a mix of maples, the leaf only makes it denser
				if (layers == SHALLOW) {
					level.setBlock(pos, mixed.setValue(LAYERS, DENSE), Block.UPDATE_ALL);
				}
			} else {
				// another maple takes over, the carpet keeps how deep it is
				level.setBlock(pos, mixed.setValue(LAYERS, layers), Block.UPDATE_ALL);
			}
			return;
		}
		// a carpet of another species
		BlockState mine = kind.fallenLeaves().defaultBlockState().setValue(FACING, state.getValue(FACING));
		if (layers == SHALLOW) {
			level.setBlock(pos, mine.setValue(LAYERS, SHALLOW), Block.UPDATE_ALL);
		} else if (random.nextFloat() < TAKEOVER_CHANCE) {
			level.setBlock(pos, mine.setValue(LAYERS, DENSE), Block.UPDATE_ALL);
		}
	}

	private static void place(ServerLevel level, BlockPos pos, FallenLeafKind kind, RandomSource random) {
		BlockState state = kind.fallenLeaves().defaultBlockState()
				.setValue(LAYERS, SHALLOW)
				.setValue(FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
		if (!level.getBlockState(pos).isAir()) {
			level.destroyBlock(pos, true);
		}
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	/**
	 * Whether the search may pass through this block: a plant, or the leaves of the tree itself.
	 */
	private static boolean isPassable(BlockState state) {
		return state.getFluidState().isEmpty()
				&& (state.canBeReplaced() || state.getBlock() instanceof LeavesBlock);
	}

	public static void loot(RegistrateBlockLootTables tb, Block block) {
		var enchantments = tb.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
		var shovelOrSilk = MatchTool.toolMatches(ItemPredicate.Builder.item().of(ItemTags.SHOVELS))
				.or(MatchTool.toolMatches(ItemPredicate.Builder.item()
						.withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
								ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
										enchantments.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))))));
		tb.add(block, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shovelOrSilk)
						.add(LootItem.lootTableItem(block))));
	}
}
