package dev.xkmc.gensokyolegacy.content.block.deco;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.UseWithoutItemBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

public class CabinetBlock implements ShapeBlockMethod, UseWithoutItemBlockMethod,
		CreateBlockStateBlockMethod, DefaultStateBlockMethod {

	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	public static final VoxelShape[] SHAPES = new VoxelShape[4];

	static {
		var builder = new VoxelBuilder(0, 0, 6, 16, 16, 16);
		for (int i = 0; i < 4; i++) {
			SHAPES[i] = builder.rotateFromNorth(Direction.from2DDataValue(i));
		}
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(BlockStateProperties.HORIZONTAL_FACING).get2DDataValue()];
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide) {
			boolean open = state.getValue(OPEN);
			level.setBlockAndUpdate(pos, state.setValue(OPEN, !open));
			level.playSound(null, pos,
					open ? SoundEvents.WOODEN_DOOR_CLOSE : SoundEvents.WOODEN_DOOR_OPEN,
					SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OPEN);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(OPEN, false);
	}

	public static void buildStates(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd, String tex) {
		var closed = pvd.models().getBuilder("block/" + ctx.getName())
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/cabinet")))
				.texture("1", pvd.modLoc("block/cabinet/" + tex))
				.texture("particle", pvd.modLoc("block/cabinet/" + tex))
				.renderType("cutout");
		var open = pvd.models().getBuilder("block/" + ctx.getName() + "_open")
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/cabinet")))
				.texture("1", pvd.modLoc("block/cabinet/" + tex + "_open"))
				.texture("particle", pvd.modLoc("block/cabinet/" + tex + "_open"))
				.renderType("cutout");
		pvd.horizontalBlock(ctx.get(), state -> state.getValue(OPEN) ? open : closed);
	}

}
