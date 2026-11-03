package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class BirdNestBlock extends SimpleBushBlock {

	private static final MapCodec<BirdNestBlock> CODEC = simpleCodec(BirdNestBlock::new);
	public static final BooleanProperty HAS_EGG = BooleanProperty.create("has_egg");

	public BirdNestBlock(Properties properties) {
		super(properties, Block.box(2, 0, 2, 14, 5, 14));
		registerDefaultState(stateDefinition.any().setValue(HAS_EGG, false));
	}

	@Override
	public MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HAS_EGG);
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
										   Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.EGG)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (state.getValue(HAS_EGG)) return ItemInteractionResult.CONSUME;
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(HAS_EGG, true), Block.UPDATE_ALL);
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1, 1);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!level.isClientSide() && !state.is(newState.getBlock()) && state.getValue(HAS_EGG)) {
			Block.popResource(level, pos, new ItemStack(Items.EGG));
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

}
