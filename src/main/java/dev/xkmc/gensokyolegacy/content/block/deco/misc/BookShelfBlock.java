package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.OnReplacedBlockMethod;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import javax.annotation.Nullable;

public class BookShelfBlock implements ShapeBlockMethod, CreateBlockStateBlockMethod, DefaultStateBlockMethod,
		UseItemOnBlockMethod, OnReplacedBlockMethod {

	public static final int MAX_BOOKS = 12;

	public static final IntegerProperty BOOK_COUNT = IntegerProperty.create("book_count", 0, MAX_BOOKS);

	public static final VoxelShape[] SHAPES = new VoxelShape[4];

	static {
		for (int i = 0; i < 4; i++) {
			var dir = Direction.from2DDataValue(i);
			SHAPES[i] = new VoxelBuilder(0, 0, 6, 16, 16, 16).rotateFromNorth(dir);
		}
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BOOK_COUNT);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(BOOK_COUNT, 0);
	}

	@Override
	@Nullable
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(BlockStateProperties.HORIZONTAL_FACING).get2DDataValue()];
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
		int count = state.getValue(BOOK_COUNT);
		if (player.isShiftKeyDown()) {
			if (count == 0)
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide()) {
				player.getInventory().placeItemBackInInventory(new ItemStack(Items.BOOK));
				level.setBlock(pos, state.setValue(BOOK_COUNT, count - 1), Block.UPDATE_ALL);
			}
			return ItemInteractionResult.SUCCESS;
		}
		if (!stack.is(Items.BOOK) || count >= MAX_BOOKS)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(BOOK_COUNT, count + 1), Block.UPDATE_ALL);
			if (!player.isCreative())
				stack.shrink(1);
		}
		return ItemInteractionResult.SUCCESS;
	}

	@Override
	public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!level.isClientSide() && state.getBlock() != newState.getBlock()) {
			int count = state.getValue(BOOK_COUNT);
			if (count > 0)
				Block.popResource(level, pos, new ItemStack(Items.BOOK, count));
		}
	}

	public static void buildStates(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd) {
		pvd.horizontalBlock(ctx.get(), state -> {
			int count = state.getValue(BOOK_COUNT);
			// texture shelf_book_0 is the empty shelf; models use the base name for count 0 so the item model can parent it
			String id = count > 0 ? ctx.getName() + "_" + count : ctx.getName();
			return pvd.models().getBuilder("block/" + id)
					.parent(new ModelFile.UncheckedModelFile(GensokyoLegacy.loc("custom/furniture/book_shelf")))
					.texture("all", pvd.modLoc("block/shelf/shelf_book_" + count));
		});
	}

}
