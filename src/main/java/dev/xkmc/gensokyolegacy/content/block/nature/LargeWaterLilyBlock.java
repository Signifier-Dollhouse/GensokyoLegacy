package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The large water lily, in {@link #VARIANTS} looks: a bare pad, one with a bloom, one with a bud, one with a
 * seed pod. Placing one picks a look at random; right-click a placed lily to walk through them.
 */
public class LargeWaterLilyBlock extends WaterLilyBlock {

	/** How many looks the lily has; the variant property counts them. */
	public static final int VARIANTS = 7;
	public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, VARIANTS - 1);

	/** The pad of the large lily, a thin slab at the height its model draws it. */
	private static final VoxelShape PAD = Block.box(1.0, 9.0, 1.0, 15.0, 10.5, 15.0);

	private static final MapCodec<LargeWaterLilyBlock> CODEC = simpleCodec(LargeWaterLilyBlock::new);

	public LargeWaterLilyBlock(Properties properties) {
		super(properties, PAD);
	}

	@Override
	public MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(VARIANT);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null) return null;
		return state.setValue(VARIANT, context.getLevel().getRandom().nextInt(VARIANTS));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
	                                          BlockHitResult hitResult) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		int next = (state.getValue(VARIANT) + 1) % VARIANTS;
		level.setBlockAndUpdate(pos, state.setValue(VARIANT, next));
		return InteractionResult.SUCCESS;
	}

}
