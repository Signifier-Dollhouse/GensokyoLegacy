package dev.xkmc.gensokyolegacy.content.block.base;

import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.PlacementBlockMethod;
import dev.xkmc.l2modularblock.mult.UseWithoutItemBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public record VariantImpl(int max, IntegerProperty variant, boolean randomPlacement) implements
		CreateBlockStateBlockMethod, DefaultStateBlockMethod, PlacementBlockMethod,
		UseWithoutItemBlockMethod {

	public VariantImpl(int max) {
		this(max, true);
	}

	public VariantImpl(int max, boolean randomPlacement) {
		this(max, IntegerProperty.create("variant", 0, max), randomPlacement);
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(variant);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(variant, 0);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			level.setBlockAndUpdate(pos, state.setValue(variant, (state.getValue(variant) + 1) % (max + 1)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockState getStateForPlacement(BlockState state, BlockPlaceContext ctx) {
		if (!randomPlacement) return state;
		long seed = state.getBlock().defaultBlockState().getSeed(ctx.getClickedPos());
		return state.setValue(variant, RandomSource.create(seed).nextInt(max + 1));
	}

}
